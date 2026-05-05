package com.mettyoung.fitbro.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OAuthToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_at_ms") val expiresAtMs: Long,
    @SerialName("token_type") val tokenType: String = "Bearer"
) {
    fun isExpired(currentTimeMs: Long): Boolean = currentTimeMs >= expiresAtMs - 60_000L
}
