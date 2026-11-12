package com.mymovie.log.data.repository

import com.mymovie.log.domain.model.UserProfile
import com.mymovie.log.domain.repository.AuthRepository
import com.mymovie.log.util.AppLogger
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth
) : AuthRepository {

    override val currentUser: Flow<UserProfile?> = auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> {
                val user = status.session.user
                AppLogger.i("REPO_AUTH", "Session authenticated: userId=${user?.id?.let { AppLogger.shortId(it) }}")
                user?.let {
                    UserProfile(
                        id = it.id,
                        email = it.email ?: "",
                        displayName = it.userMetadata?.get("full_name")?.toString()?.trim('"'),
                        avatarUrl = it.userMetadata?.get("avatar_url")?.toString()?.trim('"')
                    )
                }
            }
            else -> {
                AppLogger.d("REPO_AUTH", "Session status: ${status::class.simpleName}")
                null
            }
        }
    }

    override suspend fun signInWithEmail(email: String, password: String) {
        AppLogger.i("REPO_AUTH", "Email sign-in attempt: ${AppLogger.maskEmail(email)}")
        try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            AppLogger.i("REPO_AUTH", "Email sign-in success")
        } catch (e: Exception) {
            AppLogger.e("REPO_AUTH", "Email sign-in failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String) {
        AppLogger.i("REPO_AUTH", "Email sign-up attempt: ${AppLogger.maskEmail(email)}")
        try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            AppLogger.i("REPO_AUTH", "Email sign-up success")
        } catch (e: Exception) {
            AppLogger.e("REPO_AUTH", "Email sign-up failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun signInWithGoogle() {
        AppLogger.i("REPO_AUTH", "Google sign-in attempt")
        try {
            auth.signInWith(Google)
            AppLogger.i("REPO_AUTH", "Google sign-in success")
        } catch (e: Exception) {
            AppLogger.e("REPO_AUTH", "Google sign-in failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun signOut() {
        AppLogger.i("REPO_AUTH", "Sign-out attempt")
        try {
            auth.signOut()
            AppLogger.i("REPO_AUTH", "Sign-out success")
        } catch (e: Exception) {
            AppLogger.e("REPO_AUTH", "Sign-out failed: ${e.message}", e)
            throw e
        }
    }

    override fun isLoggedIn(): Boolean =
        auth.sessionStatus.value is SessionStatus.Authenticated
}
