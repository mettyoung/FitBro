package com.mettyoung.fitbro.data.food

interface OpenFoodFactsDataSource {
    suspend fun search(query: String): FoodResult<List<FoodSearchResult>>
    suspend fun searchByBarcode(barcode: String): FoodResult<FoodSearchResult>
}
