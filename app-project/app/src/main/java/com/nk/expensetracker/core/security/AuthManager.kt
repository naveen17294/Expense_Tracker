package com.nk.expensetracker.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "security_prefs")

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
        private val IS_PIN_ENABLED_KEY = booleanPreferencesKey("is_pin_enabled")
        private const val SALT = "nk_expense_tracker_salt_2026"
    }

    val isPinEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_PIN_ENABLED_KEY] ?: false
    }

    suspend fun setPin(pin: String) {
        val hash = hashPin(pin)
        dataStore.edit { preferences ->
            preferences[PIN_HASH_KEY] = hash
            preferences[IS_PIN_ENABLED_KEY] = true
        }
    }

    suspend fun removePin() {
        dataStore.edit { preferences ->
            preferences.remove(PIN_HASH_KEY)
            preferences[IS_PIN_ENABLED_KEY] = false
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        var savedHash: String? = null
        dataStore.edit { preferences ->
            savedHash = preferences[PIN_HASH_KEY]
        }
        
        if (savedHash == null) return false
        
        return hashPin(pin) == savedHash
    }

    private fun hashPin(pin: String): String {
        val saltedPin = pin + SALT
        val bytes = MessageDigest.getInstance("SHA-256").digest(saltedPin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
