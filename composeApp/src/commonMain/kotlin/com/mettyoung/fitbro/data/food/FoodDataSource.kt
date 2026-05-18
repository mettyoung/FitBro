package com.mettyoung.fitbro.data.food

interface FoodDataSource {
    val supportsBarcode: Boolean
    val supportsFoodDetail: Boolean get() = false
    suspend fun search(query: String): FoodResult<List<FoodSearchResult>>
    suspend fun searchByBarcode(barcode: String): FoodResult<FoodDetail> =
        FoodResult.Failure(FoodError.EmptyResults)
    suspend fun getFoodDetail(foodId: String): FoodResult<FoodDetail> =
        FoodResult.Failure(FoodError.EmptyResults)
}
