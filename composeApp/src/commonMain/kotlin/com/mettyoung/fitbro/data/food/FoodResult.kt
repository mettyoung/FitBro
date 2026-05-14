package com.mettyoung.fitbro.data.food

sealed class FoodResult<out T> {
    data class Success<T>(val value: T) : FoodResult<T>()
    data class Failure(val error: FoodError) : FoodResult<Nothing>()
}
