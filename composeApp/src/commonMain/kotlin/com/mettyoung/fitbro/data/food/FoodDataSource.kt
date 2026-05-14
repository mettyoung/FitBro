package com.mettyoung.fitbro.data.food

interface FoodDataSource {
    val supportsBarcode: Boolean
    suspend fun search(query: String): FoodResult<List<FoodSearchResult>>
    suspend fun searchByBarcode(barcode: String): FoodResult<FoodSearchResult>
}
