package com.mettyoung.fitbro.auth

sealed class OAuthError {
    data class NetworkError(val cause: Exception) : OAuthError()
    data class HttpError(val statusCode: Int, val message: String) : OAuthError()
    data object TokenExpired : OAuthError()
    data object NotAuthenticated : OAuthError()
    data class StorageError(val cause: Exception) : OAuthError()
    data class Unknown(val cause: Exception) : OAuthError()
}

sealed class OAuthResult<out T> {
    data class Success<T>(val value: T) : OAuthResult<T>()
    data class Failure(val error: OAuthError) : OAuthResult<Nothing>()
}
