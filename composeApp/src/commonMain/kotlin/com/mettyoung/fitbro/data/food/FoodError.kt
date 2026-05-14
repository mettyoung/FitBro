package com.mettyoung.fitbro.data.food

sealed class FoodError {
    data class NetworkError(val message: String) : FoodError()
    data object ParseError : FoodError()
    data object EmptyResults : FoodError()
    data object NotSupported : FoodError()
}
