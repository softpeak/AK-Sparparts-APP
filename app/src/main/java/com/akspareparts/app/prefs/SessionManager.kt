package com.akspareparts.app.prefs

import android.content.Context

/** Wraps SharedPreferences for login session persistence. */
class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("ak_session", Context.MODE_PRIVATE)

    val isLoggedIn: Boolean get() = prefs.getBoolean(KEY_LOGGED_IN, false)
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

    companion object {
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULLNAME = "fullname"
    }
}
