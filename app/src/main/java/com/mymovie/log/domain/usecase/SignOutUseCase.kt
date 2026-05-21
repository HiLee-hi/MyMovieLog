package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.repository.AuthRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() =
        repository.signOut()
}
