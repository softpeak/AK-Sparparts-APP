package com.akspareparts.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akspareparts.app.AKApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val application = app as AKApplication
    private val repo = application.repository
    private val session = application.session

    private val _loggedIn = MutableStateFlow(session.isLoggedIn)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val fullName: String? get() = session.fullName

    fun login(username: String, password: String) {
        _error.value = null
        viewModelScope.launch {
            val user = repo.login(username.trim(), password.trim())
            if (user != null) {
                session.saveSession(user.username, user.fullName)
                _loggedIn.value = true
            } else {
                _error.value = "Invalid username or password"
            }
        }
    }

    fun logout() {
        session.logout()
        _loggedIn.value = false
    }

    fun clearError() { _error.value = null }
}
