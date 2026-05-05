package com.mettyoung.fitbro.data.cronometer

sealed class CronometerApiError {
    data class NetworkError(val cause: Exception) : CronometerApiError()
    data class HttpError(val statusCode: Int, val message: String) : CronometerApiError()
    data object RateLimited : CronometerApiError()
    data object Unauthorized : CronometerApiError()
    data class ParseError(val cause: Exception) : CronometerApiError()
}

sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Failure(val error: CronometerApiError) : ApiResult<Nothing>()
}
