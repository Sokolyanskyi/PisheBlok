package com.pisheblok.app.auth

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

data class AuthUser(val uid: String, val email: String)

data class AuthState(
    val cloudReady: Boolean,
    val user: AuthUser?,
)

/**
 * Wraps FirebaseAuth. When Firebase keys are missing ([cloudReady] == false)
 * the app runs in local-only mode: user stays null and the sync is disabled.
 */
class AuthManager(app: Application) {

    val cloudReady: Boolean = FirebaseApp.getApps(app).isNotEmpty()

    private val auth: FirebaseAuth? = if (cloudReady) FirebaseAuth.getInstance() else null

    private val _state = MutableStateFlow(currentState())
    val state: StateFlow<AuthState> = _state

    init {
        auth?.addAuthStateListener { fb ->
            _state.value = AuthState(cloudReady, fb.currentUser?.toAuthUser())
        }
    }

    private fun currentState(): AuthState =
        AuthState(cloudReady, auth?.currentUser?.toAuthUser())

    private fun FirebaseUser.toAuthUser() = AuthUser(uid, email ?: "")

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth!!.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun register(email: String, password: String): Result<Unit> = runCatching {
        auth!!.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth!!.sendPasswordResetEmail(email.trim()).await()
    }

    fun signOut() {
        auth?.signOut()
    }
}
