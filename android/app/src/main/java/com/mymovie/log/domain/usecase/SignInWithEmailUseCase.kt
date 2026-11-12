package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithEmailUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String) =
        repository.signInWithEmail(email, password)
}
