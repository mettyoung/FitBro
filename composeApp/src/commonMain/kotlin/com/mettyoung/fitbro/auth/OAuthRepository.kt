package com.mettyoung.fitbro.auth

interface OAuthRepository {
    fun buildAuthUrl(state: String): Pair<String, PkceChallenge>
    suspend fun exchangeCode(code: String, codeVerifier: String): OAuthResult<OAuthToken>
    suspend fun refreshToken(): OAuthResult<OAuthToken>
    suspend fun getValidToken(): OAuthResult<OAuthToken>
    fun isAuthenticated(): Boolean
    fun signOut()
}
