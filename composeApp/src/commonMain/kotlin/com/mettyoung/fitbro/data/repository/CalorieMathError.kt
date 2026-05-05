package com.mettyoung.fitbro.data.repository

sealed class CalorieMathError {
    data class InvalidInput(val field: String, val reason: String) : CalorieMathError()
}

sealed class CalorieResult<out T> {
    data class Success<T>(val value: T) : CalorieResult<T>()
    data class Failure(val error: CalorieMathError) : CalorieResult<Nothing>()
}
