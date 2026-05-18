package com.mettyoung.fitbro.data.food

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
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
                parameter("dataType", "SR Legacy")
                parameter("dataType", "Foundation")
                parameter("dataType", "Survey (FNDDS)")
                parameter("dataType", "Branded")
                parameter("api_key", API_KEY)
            }
            if (response.status == HttpStatusCode.TooManyRequests) {
                return FoodResult.Failure(FoodError.NetworkError("USDA API rate limit exceeded. Wait an hour or use a free API key from api.data.gov"))
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
    val brandName: String? = null,
    val servingSize: Double? = null,
    val servingSizeUnit: String? = null,
    val householdServingFullText: String? = null,
    val foodNutrients: List<UsdaNutrient> = emptyList()
) {
    fun toFoodSearchResult(): FoodSearchResult? {
        val name = description?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val servingDescription = if (servingSize != null) {
            val household = householdServingFullText?.trim()?.takeIf { it.isNotBlank() }
            if (household != null) "$household - ${servingSize.toInt()}g"
            else {
                val unit = servingSizeUnit?.trim()?.takeIf { it.isNotBlank() } ?: "g"
                "${servingSize.toInt()} $unit"
            }
        } else null
        return FoodSearchResult(
            name = name,
            brand = (brandName ?: brandOwner)?.trim()?.takeIf { it.isNotBlank() },
            caloriesPer100g = nutrientValue("Energy", unitName = "KCAL"),
            proteinPer100g = nutrientValue("Protein"),
            carbPer100g = nutrientValue("Carbohydrate, by difference"),
            fatPer100g = nutrientValue("Total lipid (fat)"),
            servingSizeG = servingSize,
            servingDescription = servingDescription,
            source = "USDA"
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
