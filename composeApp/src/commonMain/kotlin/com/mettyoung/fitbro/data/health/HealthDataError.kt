package com.mettyoung.fitbro.data.health

sealed class HealthDataError {
    data object PermissionDenied : HealthDataError()
    data object NotAvailable : HealthDataError()
    data class QueryError(val cause: Exception) : HealthDataError()
}

sealed class HealthResult<out T> {
    data class Success<T>(val value: T) : HealthResult<T>()
    data class Failure(val error: HealthDataError) : HealthResult<Nothing>()
}
