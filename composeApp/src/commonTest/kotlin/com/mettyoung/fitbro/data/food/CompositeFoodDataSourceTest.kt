package com.mettyoung.fitbro.data.food

import com.mettyoung.fitbro.data.model.CustomFood
import com.mettyoung.fitbro.data.repository.CustomFoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.mettyoung.fitbro.runSync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class StubCustomFoodRepository(private val foods: List<CustomFood>) : CustomFoodRepository {
    override fun getAllCustomFoods(): Flow<List<CustomFood>> = flowOf(foods)
    override suspend fun searchCustomFoods(query: String): List<CustomFood> =
        foods.filter { it.name.contains(query, ignoreCase = true) }
    override suspend fun getCustomFood(id: Long): CustomFood? = foods.firstOrNull { it.id == id }
    override suspend fun createCustomFood(food: CustomFood): Long = 0
    override suspend fun updateCustomFood(food: CustomFood) {}
    override suspend fun deleteCustomFood(id: Long) {}
}

/** Configurable fake of the remote (FatSecret) source. */
private class FakeRemoteFoodDataSource(
    override val supportsBarcode: Boolean = true,
    private val searchResult: FoodResult<List<FoodSearchResult>>,
    private val detail: FoodDetail? = null,
    private val barcodeDetail: FoodDetail? = null
) : FoodDataSource {
    override val supportsFoodDetail = true
    var detailRequestedId: String? = null

    override suspend fun search(query: String) = searchResult
    override suspend fun getFoodDetail(foodId: String): FoodResult<FoodDetail> {
        detailRequestedId = foodId
        return detail?.let { FoodResult.Success(it) } ?: FoodResult.Failure(FoodError.EmptyResults)
    }
    override suspend fun searchByBarcode(barcode: String): FoodResult<FoodDetail> =
        barcodeDetail?.let { FoodResult.Success(it) } ?: FoodResult.Failure(FoodError.EmptyResults)
}

private fun custom(id: Long, name: String) = CustomFood(
    id = id, name = name, calories = 100.0, proteinG = 1.0, carbG = 2.0, fatG = 3.0, servingSizeG = 50.0
)

private fun remoteHit(name: String) = FoodSearchResult(name, brand = null, foodId = "9001", displayText = "x")

class CompositeFoodDataSourceTest {

    private fun composite(
        customFoods: List<CustomFood>,
        remote: FakeRemoteFoodDataSource
    ) = CompositeFoodDataSource(
        local = CustomFoodDataSource(StubCustomFoodRepository(customFoods)),
        remote = remote
    )

    @Test
    fun search_merges_custom_first_then_remote() = runSync {
        val remote = FakeRemoteFoodDataSource(
            searchResult = FoodResult.Success(listOf(remoteHit("Rice (brand)")))
        )
        val ds = composite(listOf(custom(1, "Rice bowl")), remote)

        val merged = assertIs<FoodResult.Success<List<FoodSearchResult>>>(ds.search("rice")).value
        assertEquals(2, merged.size)
        assertEquals("custom:1", merged[0].foodId) // custom first
        assertEquals("9001", merged[1].foodId)
    }

    @Test
    fun search_returns_custom_only_when_remote_fails() = runSync {
        val remote = FakeRemoteFoodDataSource(
            searchResult = FoodResult.Failure(FoodError.NetworkError("offline"))
        )
        val ds = composite(listOf(custom(1, "Rice bowl")), remote)

        val merged = assertIs<FoodResult.Success<List<FoodSearchResult>>>(ds.search("rice")).value
        assertEquals(1, merged.size)
        assertEquals("custom:1", merged[0].foodId)
    }

    @Test
    fun search_propagates_remote_failure_when_nothing_matches() = runSync {
        val remote = FakeRemoteFoodDataSource(
            searchResult = FoodResult.Failure(FoodError.NetworkError("offline"))
        )
        val ds = composite(emptyList(), remote)

        val failure = assertIs<FoodResult.Failure>(ds.search("rice"))
        assertIs<FoodError.NetworkError>(failure.error)
    }

    @Test
    fun getFoodDetail_routes_custom_ids_locally() = runSync {
        val remote = FakeRemoteFoodDataSource(searchResult = FoodResult.Failure(FoodError.EmptyResults))
        val ds = composite(listOf(custom(7, "Soup")), remote)

        val detail = assertIs<FoodResult.Success<FoodDetail>>(ds.getFoodDetail("custom:7")).value
        assertEquals("Soup", detail.name)
        assertEquals(null, remote.detailRequestedId) // remote never consulted
    }

    @Test
    fun getFoodDetail_routes_non_custom_ids_to_remote() = runSync {
        val remoteDetail = FoodDetail("9001", "Remote Food", null, emptyList(), "FatSecret")
        val remote = FakeRemoteFoodDataSource(
            searchResult = FoodResult.Failure(FoodError.EmptyResults),
            detail = remoteDetail
        )
        val ds = composite(emptyList(), remote)

        val detail = assertIs<FoodResult.Success<FoodDetail>>(ds.getFoodDetail("9001")).value
        assertEquals("Remote Food", detail.name)
        assertEquals("9001", remote.detailRequestedId)
    }

    @Test
    fun barcode_delegates_to_remote() = runSync {
        val barcodeDetail = FoodDetail("9001", "Scanned", null, emptyList(), "FatSecret")
        val remote = FakeRemoteFoodDataSource(
            searchResult = FoodResult.Failure(FoodError.EmptyResults),
            barcodeDetail = barcodeDetail
        )
        val ds = composite(emptyList(), remote)

        assertTrue(ds.supportsBarcode)
        val detail = assertIs<FoodResult.Success<FoodDetail>>(ds.searchByBarcode("12345")).value
        assertEquals("Scanned", detail.name)
    }
}
