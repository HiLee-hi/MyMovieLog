package com.mymovie.log.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.domain.model.UserProfile
import com.mymovie.log.domain.repository.AuthRepository
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EmailAuthUiState {
    object Idle : EmailAuthUiState()
    object Loading : EmailAuthUiState()
    object Success : EmailAuthUiState()                  // sign-in succeeded
    object EmailVerificationRequired : EmailAuthUiState() // awaiting email verification after sign-up
    data class Error(val message: String) : EmailAuthUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<UserProfile?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _emailAuthUiState = MutableStateFlow<EmailAuthUiState>(EmailAuthUiState.Idle)
    val emailAuthUiState: StateFlow<EmailAuthUiState> = _emailAuthUiState.asStateFlow()

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _emailAuthUiState.value = EmailAuthUiState.Loading
            AppLogger.i("VM_PROFILE", "Email sign-in requested: ${AppLogger.maskEmail(email)}")
            runCatching { authRepository.signInWithEmail(email, password) }
                .onSuccess {
                    AppLogger.i("VM_PROFILE", "Email sign-in success")
                    _emailAuthUiState.value = EmailAuthUiState.Success
                }
                .onFailure { e ->
                    AppLogger.e("VM_PROFILE", "Email sign-in failed: ${e.message}", e)
                    _emailAuthUiState.value = EmailAuthUiState.Error(
                        e.message ?: "로그인에 실패했습니다"
                    )
                }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _emailAuthUiState.value = EmailAuthUiState.Loading
            AppLogger.i("VM_PROFILE", "Email sign-up requested: ${AppLogger.maskEmail(email)}")
            runCatching { authRepository.signUpWithEmail(email, password) }
                .onSuccess {
                    AppLogger.i("VM_PROFILE", "Email sign-up success — verification required")
                    // Use a distinct state instead of Success because email verification is still required
                    _emailAuthUiState.value = EmailAuthUiState.EmailVerificationRequired
                }
                .onFailure { e ->
                    AppLogger.e("VM_PROFILE", "Email sign-up failed: ${e.message}", e)
                    _emailAuthUiState.value = EmailAuthUiState.Error(
                        e.message ?: "회원가입에 실패했습니다"
                    )
                }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            AppLogger.i("VM_PROFILE", "Google sign-in requested")
            runCatching { authRepository.signInWithGoogle() }
                .onSuccess { AppLogger.i("VM_PROFILE", "Google sign-in success") }
                .onFailure { AppLogger.e("VM_PROFILE", "Google sign-in failed: ${it.message}", it) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            AppLogger.i("VM_PROFILE", "Sign-out requested")
            runCatching { authRepository.signOut() }
                .onSuccess { AppLogger.i("VM_PROFILE", "Sign-out success") }
                .onFailure { AppLogger.e("VM_PROFILE", "Sign-out failed: ${it.message}", it) }
        }
    }

    fun resetEmailAuthState() {
        _emailAuthUiState.value = EmailAuthUiState.Idle
    }
}
