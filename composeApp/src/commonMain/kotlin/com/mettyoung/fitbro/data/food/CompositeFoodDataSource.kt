package com.mettyoung.fitbro.data.food

/**
 * Fans search out across multiple [FoodDataSource]s and merges the results, with
 * [local] (custom foods) shown first. Detail lookups are routed by food-id prefix:
 * `custom:` ids go to [local], everything else to [remote]. Barcode scanning is
 * delegated to [remote] (the only source that supports it).
 */
class CompositeFoodDataSource(
    private val local: CustomFoodDataSource,
    private val remote: FoodDataSource
) : FoodDataSource {

    override val supportsBarcode = remote.supportsBarcode
    override val supportsFoodDetail = true

    override suspend fun search(query: String): FoodResult<List<FoodSearchResult>> {
        val localResults = (local.search(query) as? FoodResult.Success)?.value.orEmpty()
        val remoteResult = remote.search(query)
        val remoteResults = (remoteResult as? FoodResult.Success)?.value.orEmpty()

        val merged = localResults + remoteResults
        if (merged.isNotEmpty()) return FoodResult.Success(merged)

        // Nothing matched anywhere — surface the remote error (network vs empty) if any.
        return remoteResult as? FoodResult.Failure ?: FoodResult.Failure(FoodError.EmptyResults)
    }

    override suspend fun getFoodDetail(foodId: String): FoodResult<FoodDetail> =
        if (CustomFoodDataSource.owns(foodId)) local.getFoodDetail(foodId)
        else remote.getFoodDetail(foodId)

    override suspend fun searchByBarcode(barcode: String): FoodResult<FoodDetail> =
        if (remote.supportsBarcode) remote.searchByBarcode(barcode)
        else FoodResult.Failure(FoodError.NotSupported)
}
