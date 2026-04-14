package com.mymovie.log.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.domain.repository.AuthRepository
import com.mymovie.log.domain.repository.MovieRecordRepository
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val movieRecordRepository: MovieRecordRepository
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepository.currentUser
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        observeAuthAndSync()
    }

    private fun observeAuthAndSync() {
        viewModelScope.launch {
            isLoggedIn
                .collect { loggedIn ->
                    if (loggedIn) {
                        AppLogger.i("APP_VM", "Login detected → starting syncFromRemote")
                        syncFromRemote()
                    } else {
                        AppLogger.i("APP_VM", "Logout detected → clearing local cache")
                        clearLocalCache()
                    }
                }
        }
    }

    private fun syncFromRemote() {
        viewModelScope.launch {
            runCatching { movieRecordRepository.syncFromRemote() }
                .onSuccess { AppLogger.i("APP_VM", "syncFromRemote completed") }
                .onFailure { AppLogger.e("APP_VM", "syncFromRemote failed: ${it.message}", it) }
        }
    }

    private fun clearLocalCache() {
        viewModelScope.launch {
            runCatching { movieRecordRepository.clearLocalCache() }
                .onSuccess { AppLogger.i("APP_VM", "clearLocalCache completed") }
                .onFailure { AppLogger.e("APP_VM", "clearLocalCache failed: ${it.message}", it) }
        }
    }
}
