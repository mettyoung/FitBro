package com.mettyoung.fitbro.data.food

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class UsdaFoodDataSource : FoodDataSource {

    override val supportsBarcode = false

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    override suspend fun search(query: String): FoodResult<List<FoodSearchResult>> {
        return try {
            val response = httpClient.get("https://api.nal.usda.gov/fdc/v1/foods/search") {
                parameter("query", query)
                parameter("pageSize", 20)
                parameter("dataType", "SR Legacy,Foundation")
                parameter("api_key", API_KEY)
            }
            val body = response.body<UsdaSearchResponse>()
            val results = body.foods
                ?.mapNotNull { it.toFoodSearchResult() }
                ?: emptyList()
            if (results.isEmpty()) {
                FoodResult.Failure(FoodError.EmptyResults)
            } else {
                FoodResult.Success(results)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FoodResult.Failure(FoodError.NetworkError(e.message ?: "Network error"))
        }
    }

    override suspend fun searchByBarcode(barcode: String): FoodResult<FoodSearchResult> =
        FoodResult.Failure(FoodError.NotSupported)

    companion object {
        private const val API_KEY = "DEMO_KEY"
    }
}

@Serializable
private data class UsdaSearchResponse(
    val foods: List<UsdaFood>? = null
)

@Serializable
private data class UsdaFood(
    val description: String? = null,
    val brandOwner: String? = null,
    val servingSize: Double? = null,
    val foodNutrients: List<UsdaNutrient> = emptyList()
) {
    fun toFoodSearchResult(): FoodSearchResult? {
        val name = description?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return FoodSearchResult(
            name = name,
            brand = brandOwner?.trim()?.takeIf { it.isNotBlank() },
            caloriesPer100g = nutrientValue("Energy", unitName = "KCAL"),
            proteinPer100g = nutrientValue("Protein"),
            carbPer100g = nutrientValue("Carbohydrate, by difference"),
            fatPer100g = nutrientValue("Total lipid (fat)"),
            servingSizeG = servingSize
        )
    }

    private fun nutrientValue(name: String, unitName: String? = null): Double =
        foodNutrients.firstOrNull { n ->
            n.nutrientName == name && (unitName == null || n.unitName == unitName)
        }?.value ?: 0.0
}

@Serializable
private data class UsdaNutrient(
    val nutrientName: String? = null,
    val unitName: String? = null,
    val value: Double? = null
)
