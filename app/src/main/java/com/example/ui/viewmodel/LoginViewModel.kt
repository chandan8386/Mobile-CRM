package com.example.ui.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val user: FirebaseUser?) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

class LoginViewModel(
    private val auth: FirebaseAuth = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        // Fallback placeholder if Firebase is not yet initialized in environment
        FirebaseAuth.getInstance()
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    init {
        try {
            _currentUser.value = auth.currentUser
            auth.addAuthStateListener { firebaseAuth ->
                _currentUser.value = firebaseAuth.currentUser
                if (firebaseAuth.currentUser != null) {
                    _uiState.value = LoginUiState.Success(firebaseAuth.currentUser)
                }
            }
        } catch (_: Exception) {
            // Ignore if Firebase uninitialized
        }
    }

    /**
     * Initiates Google Sign-In via Android Jetpack CredentialManager.
     */
    fun signInWithGoogle(context: Context, serverClientId: String = "") {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetSignInWithGoogleOption.Builder(
                    serverClientId = serverClientId.ifBlank { "dummy-server-client-id.apps.googleusercontent.com" }
                ).build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                val credential = result.credential
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    firebaseAuthWithGoogle(idToken)
                } else {
                    _uiState.value = LoginUiState.Error("Unsupported credential type received.")
                }
            } catch (e: GetCredentialCancellationException) {
                _uiState.value = LoginUiState.Idle
            } catch (e: GetCredentialException) {
                _uiState.value = LoginUiState.Error(e.message ?: "Google Sign-In failed.")
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.localizedMessage ?: "Authentication error occurred.")
            }
        }
    }

    private suspend fun firebaseAuthWithGoogle(idToken: String) {
        suspendCancellableCoroutine { continuation ->
            try {
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(authCredential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            _currentUser.value = user
                            _uiState.value = LoginUiState.Success(user)
                            continuation.resume(Unit)
                        } else {
                            val errorMsg = task.exception?.localizedMessage ?: "Firebase authentication failed."
                            _uiState.value = LoginUiState.Error(errorMsg)
                            continuation.resume(Unit)
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.localizedMessage ?: "Failed to authenticate credential.")
                continuation.resume(Unit)
            }
        }
    }

    /**
     * Email and password sign-in for sales representatives and enterprise admins.
     */
    fun signInWithEmailPassword(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter both email and password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            _currentUser.value = user
                            _uiState.value = LoginUiState.Success(user)
                        } else {
                            _uiState.value = LoginUiState.Error(
                                task.exception?.localizedMessage ?: "Invalid email or password."
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.localizedMessage ?: "Authentication service error.")
            }
        }
    }

    /**
     * Signs out the current user and resets state.
     */
    fun signOut() {
        try {
            auth.signOut()
        } catch (_: Exception) {}
        _currentUser.value = null
        _uiState.value = LoginUiState.Idle
    }

    fun clearError() {
        _uiState.value = LoginUiState.Idle
    }
}
