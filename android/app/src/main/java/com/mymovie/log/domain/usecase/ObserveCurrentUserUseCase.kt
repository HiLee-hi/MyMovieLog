package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.model.UserProfile
import com.mymovie.log.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCurrentUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Flow<UserProfile?> =
        repository.currentUser
}
