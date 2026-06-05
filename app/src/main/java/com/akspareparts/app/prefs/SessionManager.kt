package com.akspareparts.app.prefs

import android.content.Context

/** Wraps SharedPreferences for login session + API key persistence. */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("ak_session", Context.MODE_PRIVATE)

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED_IN, false)
        private set(v) = prefs.edit().putBoolean(KEY_LOGGED_IN, v).apply()

    val username: String? get() = prefs.getString(KEY_USERNAME, null)
    val fullName: String? get() = prefs.getString(KEY_FULLNAME, null)

    fun saveSession(username: String, fullName: String) {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_USERNAME, username)
            .putString(KEY_FULLNAME, fullName)
            .apply()
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_LOGGED_IN)
            .remove(KEY_USERNAME)
            .remove(KEY_FULLNAME)
            .apply()
    }

    // Anthropic API key (one-time prompt after first login)
    var apiKey: String?
        get() = prefs.getString(KEY_API_KEY, null)
        set(v) = prefs.edit().putString(KEY_API_KEY, v).apply()

    val hasApiKey: Boolean get() = !apiKey.isNullOrBlank()

    companion object {
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULLNAME = "fullname"
        private const val KEY_API_KEY = "anthropic_api_key"
    }
}
