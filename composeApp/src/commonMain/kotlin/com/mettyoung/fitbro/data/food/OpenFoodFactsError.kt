package com.mettyoung.fitbro.data.food

sealed class OpenFoodFactsError {
    data class NetworkError(val message: String) : OpenFoodFactsError()
    data object ParseError : OpenFoodFactsError()
    data object EmptyResults : OpenFoodFactsError()
}
