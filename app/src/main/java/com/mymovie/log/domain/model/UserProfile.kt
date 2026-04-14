package com.mymovie.log.domain.model

data class UserProfile(
    val id: String,
    val email: String,
    val displayName: String?,
    val avatarUrl: String?
)
