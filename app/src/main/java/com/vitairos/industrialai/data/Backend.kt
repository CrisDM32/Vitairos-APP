package com.vitairos.industrialai.data

import com.vitairos.industrialai.BuildConfig
import com.vitairos.industrialai.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class ApiError(val code: Int, message: String) : IOException(message)
data class Session(val token: String, val expiresAt: String, val profile: DemoUser)
data class Catalog(val reportTypes: List<String>, val processes: List<String>, val areas: List<String>, val shifts: List<String>)

private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
private fun JSONObject.profile() = DemoUser(getString("email"), getString("name"), Role.valueOf(getString("role")))
private fun JSONObject.optional(key: String) = if (isNull(key)) null else getString(key)
private fun localTime(value: String) = OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

class Backend {
    private val identity = BuildConfig.IDENTITY_URL.trimEnd('/') + "/api/v1"
    private val reports = BuildConfig.REPORTS_URL.trimEnd('/') + "/api/v1"

    private suspend fun request(method: String, url: String, token: String? = null, body: JSONObject? = null,
                                upload: Attachment? = null, download: File? = null): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 6000
            connection.readTimeout = 25000
            token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            if (upload != null) {
                val file = File(upload.path)
                if (!file.isFile) throw IOException("Falta el archivo ${upload.name}")
                val boundary = UUID.randomUUID().toString()
                connection.doOutput = true
                connection.setChunkedStreamingMode(16384)
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.outputStream.use { out ->
                    val name = upload.name.map { if (it in listOf('"','\r','\n','\\')) '_' else it }.joinToString("")
                    out.write("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"$name\"\r\nContent-Type: application/octet-stream\r\n\r\n".toByteArray())
                    file.inputStream().use { it.copyTo(out) }
                    out.write("\r\n--$boundary--\r\n".toByteArray())
                }
            } else if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type","application/json; charset=UTF-8")
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw ApiError(code, runCatching { JSONObject(error).optional("detail") }.getOrNull() ?: "HTTP $code")
            }
            if (download != null) {
                connection.inputStream.use { input -> download.outputStream().use { input.copyTo(it) } }
                ""
            } else connection.inputStream.bufferedReader().use { it.readText() }
        } finally { connection.disconnect() }
    }

    suspend fun login(email: String, password: String): Session {
        val json = JSONObject(request("POST","$identity/sessions",body=JSONObject().put("email",email.trim()).put("password",password)))
        return Session(json.getString("accessToken"),json.getString("expiresAt"),json.getJSONObject("user").profile())
    }
    suspend fun logout(token: String) { request("DELETE","$identity/sessions/current",token) }
    suspend fun me(token: String) = JSONObject(request("GET","$identity/me",token)).profile()
    suspend fun catalog(token: String): Catalog {
        val json = JSONObject(request("GET","$reports/catalog",token))
        fun values(key: String) = json.getJSONArray(key).let { a -> (0 until a.length()).map { a.getString(it) } }
        return Catalog(values("reportTypes"), values("processes"), values("areas"), values("shifts"))
    }
    suspend fun currentWeather(token: String, latitude: Double, longitude: Double) =
        JSONObject(request("GET","$reports/weather/current?latitude=$latitude&longitude=$longitude",token))

    suspend fun ids(token: String) = JSONArray(request("GET","$reports/reports",token)).objects().map { it.getString("id") }
    suspend fun detail(token: String, id: String, local: Report?) =
        decode(request("GET","$reports/reports/$id",token),local)

    private fun decode(raw: String, local: Report?): Report {
        val json = JSONObject(raw)
        val r = json.getJSONObject("report")
        val files = json.getJSONArray("attachments").objects().map { a ->
            val id = a.getString("id")
            Attachment(id,a.getString("name"),a.getDouble("sizeMb"),AttachmentKind.valueOf(a.getString("kind")),
                local?.attachments?.find { it.id == id }?.path.orEmpty())
        }
        return Report(r.getString("id"),r.getString("type"),r.getString("process"),r.getString("area"),
            r.getString("shift"),r.getString("notes"),r.getString("user"),localTime(r.getString("createdAt")),
            ReportStatus.valueOf(r.getString("status")),r.getInt("attempts"),files,
            json.getJSONArray("timeline").objects().map { TimelineEvent(localTime(it.getString("time")),it.getString("text")) },
            r.optional("supervisorNote"),r.optDouble("latitude").takeIf { it.isFinite() },
            r.optDouble("longitude").takeIf { it.isFinite() },weather=json.optJSONObject("weather")?.toString())
    }

    suspend fun save(token: String, r: Report, submit: Boolean): Report {
        val url = "$reports/reports/${r.id}"
        val body = JSONObject().put("type",r.type).put("process",r.process).put("area",r.area).put("shift",r.shift)
            .put("notes",r.notes).put("latitude",r.latitude).put("longitude",r.longitude)
        val draft = try { decode(request("PUT",url,token,body),r) } catch (e: ApiError) {
            if (e.code != 409 || !submit) throw e
            val existing = detail(token,r.id,r)
            if (existing.status == ReportStatus.SINCRONIZADO) return existing
            throw e
        }
        val remoteIds = draft.attachments.map { it.id }.toSet()
        for (id in remoteIds - r.attachments.map { it.id }.toSet()) request("DELETE","$url/attachments/$id",token)
        for (file in r.attachments) {
            if (file.path.isNotBlank() || file.id !in remoteIds) request("PUT","$url/attachments/${file.id}",token,upload=file)
        }
        return if (submit) decode(request("PUT","$url/submission",token),r) else detail(token,r.id,r)
    }
    suspend fun delete(token: String, id: String) {
        try { request("DELETE","$reports/reports/$id",token) } catch (e: ApiError) { if (e.code != 404) throw e }
    }
    suspend fun correct(token: String, r: Report, note: String) =
        decode(request("PUT","$reports/reports/${r.id}/correction",token,JSONObject().put("note",note)),r)

    suspend fun download(token: String, reportId: String, a: Attachment, dir: File): File = withContext(Dispatchers.IO) {
        val file = File.createTempFile("adjunto_", "." + a.name.substringAfterLast('.'), dir)
        try {
            request("GET","$reports/reports/$reportId/attachments/${a.id}",token,download=file)
            file
        } catch (e: Exception) { file.delete(); throw e }
    }
}
