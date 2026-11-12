package com.mymovie.log.domain.repository

import com.mymovie.log.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<UserProfile?>
    suspend fun signInWithEmail(email: String, password: String)
    suspend fun signUpWithEmail(email: String, password: String)
    suspend fun signInWithGoogle()
    suspend fun signOut()
    fun isLoggedIn(): Boolean
}
