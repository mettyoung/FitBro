package com.mettyoung.fitbro.data.food

import com.mettyoung.fitbro.data.model.CustomFood
import com.mettyoung.fitbro.data.repository.CustomFoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.mettyoung.fitbro.runSync
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** In-memory CustomFoodRepository mirroring the SQLDelight impl's search/get contract. */
private class FakeCustomFoodRepository(
    seed: List<CustomFood> = emptyList()
) : CustomFoodRepository {
    private val rows = MutableStateFlow(seed)
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0L) + 1

    override fun getAllCustomFoods(): Flow<List<CustomFood>> = rows

    override suspend fun searchCustomFoods(query: String): List<CustomFood> =
        rows.value.filter {
            it.name.contains(query, ignoreCase = true) ||
                (it.brandName?.contains(query, ignoreCase = true) == true)
        }.sortedBy { it.name }

    override suspend fun getCustomFood(id: Long): CustomFood? = rows.value.firstOrNull { it.id == id }

    override suspend fun createCustomFood(food: CustomFood): Long {
        val id = nextId++
        rows.value = rows.value + food.copy(id = id)
        return id
    }

    override suspend fun updateCustomFood(food: CustomFood) {
        rows.value = rows.value.map { if (it.id == food.id) food else it }
    }

    override suspend fun deleteCustomFood(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }
}

private fun chicken(id: Long = 1) = CustomFood(
    id = id,
    name = "Grilled Chicken",
    brandName = "MyKitchen",
    calories = 165.0,
    proteinG = 31.0,
    carbG = 0.0,
    fatG = 3.6,
    servingSizeG = 100.0
)

class CustomFoodDataSourceTest {

    @Test
    fun search_returns_prefixed_food_id_and_custom_label() = runSync {
        val ds = CustomFoodDataSource(FakeCustomFoodRepository(listOf(chicken())))

        val result = assertIs<FoodResult.Success<List<FoodSearchResult>>>(ds.search("chicken"))
        val item = result.value.single()

        assertEquals("Grilled Chicken", item.name)
        assertEquals("MyKitchen", item.brand)
        assertEquals("custom:1", item.foodId)
        assertTrue(item.displayText.contains("Custom"), "displayText should mark it custom: ${item.displayText}")
        assertTrue(item.displayText.contains("100g"))
        assertTrue(item.displayText.contains("165kcal"))
    }

    @Test
    fun search_empty_when_no_match() = runSync {
        val ds = CustomFoodDataSource(FakeCustomFoodRepository(listOf(chicken())))
        assertIs<FoodResult.Failure>(ds.search("pizza"))
    }

    @Test
    fun getFoodDetail_builds_single_gram_serving() = runSync {
        val ds = CustomFoodDataSource(FakeCustomFoodRepository(listOf(chicken())))

        val detail = assertIs<FoodResult.Success<FoodDetail>>(ds.getFoodDetail("custom:1")).value
        assertEquals("custom:1", detail.foodId)
        assertEquals("Custom", detail.source)

        val serving = detail.servings.single()
        assertEquals(100.0, serving.metricAmount)
        assertEquals("g", serving.metricUnit)
        assertEquals(165.0, serving.calories)
        assertEquals(31.0, serving.proteinG)
        assertEquals(0.0, serving.carbG)
        assertEquals(3.6, serving.fatG)
    }

    @Test
    fun getFoodDetail_fails_for_unknown_or_foreign_id() = runSync {
        val ds = CustomFoodDataSource(FakeCustomFoodRepository(listOf(chicken())))
        assertIs<FoodResult.Failure>(ds.getFoodDetail("custom:999"))
        assertIs<FoodResult.Failure>(ds.getFoodDetail("12345")) // FatSecret-style id, not ours
    }

    @Test
    fun owns_only_custom_prefixed_ids() {
        assertTrue(CustomFoodDataSource.owns("custom:5"))
        assertTrue(!CustomFoodDataSource.owns("5"))
    }

    @Test
    fun update_is_reflected_in_detail_and_preserves_id() = runSync {
        val repo = FakeCustomFoodRepository(listOf(chicken()))
        val ds = CustomFoodDataSource(repo)

        repo.updateCustomFood(chicken().copy(name = "Roast Chicken", calories = 200.0))

        val detail = assertIs<FoodResult.Success<FoodDetail>>(ds.getFoodDetail("custom:1")).value
        assertEquals("custom:1", detail.foodId)
        assertEquals("Roast Chicken", detail.name)
        assertEquals(200.0, detail.servings.single().calories)
    }

    @Test
    fun delete_removes_food_from_search_and_detail() = runSync {
        val repo = FakeCustomFoodRepository(listOf(chicken()))
        val ds = CustomFoodDataSource(repo)

        repo.deleteCustomFood(1)

        assertIs<FoodResult.Failure>(ds.search("chicken"))
        assertIs<FoodResult.Failure>(ds.getFoodDetail("custom:1"))
    }
}
