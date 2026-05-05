package com.mettyoung.fitbro.auth

interface TokenStorage {
    fun saveToken(token: OAuthToken)
    fun loadToken(): OAuthToken?
    fun clearToken()
}

expect fun createTokenStorage(): TokenStorage
