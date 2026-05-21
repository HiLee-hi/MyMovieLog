package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetCurrentUserIdUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): String =
        repository.currentUser.first()?.id ?: ""
}
