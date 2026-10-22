package com.vitairos.industrialai.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class Settings(val sessionEmail: String?, val demoOffline: Boolean, val simulateServerError: Boolean)

/** Preferencias simples. Nunca se guarda la contraseña: solo el correo de la sesión ficticia. */
class SettingsStore(private val context: Context) {
    private val emailKey = stringPreferencesKey("session_email")
    private val offlineKey = booleanPreferencesKey("demo_offline")
    private val errorKey = booleanPreferencesKey("simulate_error")

    private fun Preferences.toSettings() = Settings(this[emailKey], this[offlineKey] ?: false, this[errorKey] ?: false)

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun load(): Settings = settings.first()

    suspend fun setSession(email: String?) = context.dataStore.edit { if (email == null) it.remove(emailKey) else it[emailKey] = email }
    suspend fun setDemoOffline(value: Boolean) = context.dataStore.edit { it[offlineKey] = value }
    suspend fun setSimulateError(value: Boolean) = context.dataStore.edit { it[errorKey] = value }
}
