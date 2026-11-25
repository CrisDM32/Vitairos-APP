package com.vitairos.industrialai

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vitairos.industrialai.model.ReportStatus
import com.vitairos.industrialai.ui.ReportsViewModel
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Assume.assumeNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReportIntegrationTest {
    @Test fun offlineSubmissionAndSupervisorReview() {
        val args = InstrumentationRegistry.getArguments()
        val operator = args.getString("operatorPassword")
        val supervisor = args.getString("supervisorPassword")
        assumeNotNull(operator, supervisor)
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        runBlocking {
            withContext(Dispatchers.Main) {
                val vm = ReportsViewModel(app)
                suspend fun until(condition: () -> Boolean) = withTimeout(60000) {
                    while (!condition()) delay(100)
                }
                suspend fun login(role: String, password: String) {
                    val result = CompletableDeferred<String?>()
                    vm.login("$role@vitairos.demo",password) { result.complete(it) }
                    assertNull(result.await())
                }
                until { vm.ready }
                login("operador",operator!!)
                vm.changeDemoOffline(true)
                vm.startNewDraft()
                vm.updateDraft { it.copy(type="Reporte de turno v1",process="Molienda",area="Planta",
                    shift="Día administrativo",latitude=-33.45,longitude=-70.66) }
                val file = File.createTempFile("turno_",".pdf",app.filesDir)
                file.writeText("%PDF-1.4\n%%EOF")
                assertNull(vm.addAttachment(file.name,file.length()/1048576.0,path=file.path))
                val id = vm.saveDraft(true)
                assertEquals(ReportStatus.EN_COLA,vm.report(id)?.status)
                vm.changeDemoOffline(false)
                until { vm.report(id)?.status == ReportStatus.SINCRONIZADO }
                val weather = vm.report(id)?.weather
                vm.logout()
                login("supervisor",supervisor!!)
                until { vm.report(id) != null }
                val remote = vm.report(id)!!
                assertEquals(weather,remote.weather)
                assertEquals("", remote.attachments.single().path)
                vm.download(id,remote.attachments.single())
                until { vm.report(id)!!.attachments.single().path.isNotEmpty() }
                assertArrayEquals(file.readBytes(),File(vm.report(id)!!.attachments.single().path).readBytes())
                vm.correct(vm.report(id)!!,"Precisar turno")
                until { vm.report(id)?.status == ReportStatus.REQUIERE_CORRECCION }
                vm.logout()
                login("operador",operator)
                until { vm.report(id)?.supervisorNote == "Precisar turno" }
                vm.startEdit(vm.report(id)!!)
                vm.updateDraft { it.copy(notes="Turno confirmado") }
                vm.saveDraft(true)
                until { vm.report(id)?.status == ReportStatus.SINCRONIZADO }
                assertEquals("Turno confirmado",vm.report(id)?.notes)
                assertEquals(weather,vm.report(id)?.weather)
                vm.logout()
            }
        }
    }
}
