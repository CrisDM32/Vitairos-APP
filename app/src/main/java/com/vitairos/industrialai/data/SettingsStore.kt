package com.vitairos.industrialai.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private val token = stringPreferencesKey("bearer")
    private val expires = stringPreferencesKey("expires")
    private val email = stringPreferencesKey("session_email")
    private val name = stringPreferencesKey("profile_name")
    private val role = stringPreferencesKey("profile_role")
    private val offline = booleanPreferencesKey("demo_offline")

    suspend fun session(): Session? {
        val p = context.dataStore.data.first()
        return Session(p[token] ?: return null, p[expires] ?: return null,
            com.vitairos.industrialai.model.DemoUser(p[email] ?: return null, p[name] ?: return null,
                com.vitairos.industrialai.model.Role.valueOf(p[role] ?: return null)))
    }
    suspend fun setSession(s: Session?) = context.dataStore.edit {
        if (s == null) {
            it.remove(token)
            it.remove(expires)
            it.remove(email)
            it.remove(name)
            it.remove(role)
        } else {
            it[token] = s.token
            it[expires] = s.expiresAt
            it[email] = s.profile.email
            it[name] = s.profile.name
            it[role] = s.profile.role.name
        }
    }
    suspend fun offline() = context.dataStore.data.first()[offline] ?: false
    suspend fun setDemoOffline(value: Boolean) = context.dataStore.edit { it[offline] = value }
}
