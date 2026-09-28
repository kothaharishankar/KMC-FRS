package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.core.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean(Constants.KEY_THEME_DARK, false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.KEY_THEME_DARK, enabled).apply()
        _isDarkMode.value = enabled
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(Constants.KEY_JWT_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(Constants.KEY_JWT_TOKEN, null)
    }

    fun saveUserSession(userId: String, name: String, role: String) {
        prefs.edit()
            .putString(Constants.KEY_USER_ID, userId)
            .putString(Constants.KEY_USER_NAME, name)
            .putString(Constants.KEY_USER_ROLE, role)
            .apply()
    }

    fun getUserSession(): Triple<String?, String?, String?> {
        val id = prefs.getString(Constants.KEY_USER_ID, null)
        val name = prefs.getString(Constants.KEY_USER_NAME, null)
        val role = prefs.getString(Constants.KEY_USER_ROLE, null)
        return Triple(id, name, role)
    }

    // --- Specific methods matching StorageService requirement ---
    fun saveSession(token: String, empId: String, name: String, role: String) {
        prefs.edit()
            .putString(Constants.KEY_JWT_TOKEN, token)
            .putString(Constants.KEY_USER_ID, empId)
            .putString(Constants.KEY_USER_NAME, name)
            .putString(Constants.KEY_USER_ROLE, role)
            .apply()
    }

    fun getToken(): String? = getAuthToken()

    fun getRole(): String? = prefs.getString(Constants.KEY_USER_ROLE, null)

    fun hasActiveSession(): Boolean = getAuthToken() != null && getRole() != null

    fun clearSession() {
        prefs.edit()
            .remove(Constants.KEY_JWT_TOKEN)
            .remove(Constants.KEY_USER_ID)
            .remove(Constants.KEY_USER_NAME)
            .remove(Constants.KEY_USER_ROLE)
            .apply()
    }
}
