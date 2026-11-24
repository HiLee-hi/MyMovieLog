package com.mymovie.log.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.R
import com.mymovie.log.domain.model.UserProfile
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.usecase.GetRecordsUseCase
import com.mymovie.log.domain.usecase.ObserveCurrentUserUseCase
import com.mymovie.log.domain.usecase.SignInWithEmailUseCase
import com.mymovie.log.domain.usecase.SignInWithGoogleUseCase
import com.mymovie.log.domain.usecase.SignOutUseCase
import com.mymovie.log.domain.usecase.SignUpWithEmailUseCase
import com.mymovie.log.presentation.ui.UiText
import com.mymovie.log.presentation.ui.toUiText
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EmailAuthUiState {
    object Idle : EmailAuthUiState()
    object Loading : EmailAuthUiState()
    object Success : EmailAuthUiState()
    object EmailVerificationRequired : EmailAuthUiState() // waiting for email verification
    data class Error(val message: UiText) : EmailAuthUiState()
}

data class ProfileRecordCounts(val watched: Int = 0, val wishlist: Int = 0)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    observeCurrentUserUseCase: ObserveCurrentUserUseCase,
    getRecordsUseCase: GetRecordsUseCase,
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    val currentUser: StateFlow<UserProfile?> = observeCurrentUserUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recordCounts: StateFlow<ProfileRecordCounts> = getRecordsUseCase()
        .map { records ->
            ProfileRecordCounts(
                watched = records.count { it.status == WatchStatus.WATCHED },
                wishlist = records.count { it.status == WatchStatus.WISHLIST }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileRecordCounts())

    private val _emailAuthUiState = MutableStateFlow<EmailAuthUiState>(EmailAuthUiState.Idle)
    val emailAuthUiState: StateFlow<EmailAuthUiState> = _emailAuthUiState.asStateFlow()

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _emailAuthUiState.value = EmailAuthUiState.Loading
            AppLogger.i("VM_PROFILE", "Email sign-in requested: ${AppLogger.maskEmail(email)}")
            runCatching { signInWithEmailUseCase(email, password) }
                .onSuccess {
                    AppLogger.i("VM_PROFILE", "Email sign-in success")
                    _emailAuthUiState.value = EmailAuthUiState.Success
                }
                .onFailure { e ->
                    AppLogger.e("VM_PROFILE", "Email sign-in failed: ${e.message}", e)
                    _emailAuthUiState.value = EmailAuthUiState.Error(
                        e.toUiText(R.string.error_sign_in_failed)
                    )
                }
        }
    }

    fun signUpWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _emailAuthUiState.value = EmailAuthUiState.Loading
            AppLogger.i("VM_PROFILE", "Email sign-up requested: ${AppLogger.maskEmail(email)}")
            runCatching { signUpWithEmailUseCase(email, password) }
                .onSuccess {
                    AppLogger.i("VM_PROFILE", "Email sign-up success — verification required")
                    // not Success, email still needs verifying
                    _emailAuthUiState.value = EmailAuthUiState.EmailVerificationRequired
                }
                .onFailure { e ->
                    AppLogger.e("VM_PROFILE", "Email sign-up failed: ${e.message}", e)
                    _emailAuthUiState.value = EmailAuthUiState.Error(
                        e.toUiText(R.string.error_sign_up_failed)
                    )
                }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            AppLogger.i("VM_PROFILE", "Google sign-in requested")
            runCatching { signInWithGoogleUseCase() }
                .onSuccess { AppLogger.i("VM_PROFILE", "Google sign-in success") }
                .onFailure { AppLogger.e("VM_PROFILE", "Google sign-in failed: ${it.message}", it) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            AppLogger.i("VM_PROFILE", "Sign-out requested")
            runCatching { signOutUseCase() }
                .onSuccess { AppLogger.i("VM_PROFILE", "Sign-out success") }
                .onFailure { AppLogger.e("VM_PROFILE", "Sign-out failed: ${it.message}", it) }
        }
    }

    fun resetEmailAuthState() {
        _emailAuthUiState.value = EmailAuthUiState.Idle
    }
}
