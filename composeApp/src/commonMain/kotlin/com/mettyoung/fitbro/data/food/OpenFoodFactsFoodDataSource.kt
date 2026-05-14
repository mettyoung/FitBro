package com.mettyoung.fitbro.data.food

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class OpenFoodFactsFoodDataSource : FoodDataSource {

    override val supportsBarcode = true

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
            val response = httpClient.get("https://world.openfoodfacts.org/cgi/search.pl") {
                parameter("search_terms", query)
                parameter("action", "process")
                parameter("json", "1")
                parameter("page_size", "20")
                header("User-Agent", "FitBro/1.0 (Android; emmettyoung92@gmail.com)")
            }
            val body = response.body<OpenFoodFactsResponse>()
            val results = body.products
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

    override suspend fun searchByBarcode(barcode: String): FoodResult<FoodSearchResult> {
        return try {
            val response = httpClient.get("https://world.openfoodfacts.org/api/v2/product/$barcode.json") {
                parameter("fields", "product_name,brands,serving_size,serving_quantity,nutriments")
                header("User-Agent", "FitBro/1.0 (Android; emmettyoung92@gmail.com)")
            }
            val body = response.body<OpenFoodFactsBarcodeResponse>()
            val result = body.product?.toFoodSearchResult()
            if (result != null) {
                FoodResult.Success(result)
            } else {
                FoodResult.Failure(FoodError.EmptyResults)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FoodResult.Failure(FoodError.NetworkError(e.message ?: "Network error"))
        }
    }
}

@Serializable
private data class OpenFoodFactsResponse(
    val products: List<OpenFoodFactsProduct>? = null
)

@Serializable
private data class OpenFoodFactsBarcodeResponse(
    val product: OpenFoodFactsProduct? = null,
    val status: Int? = null
)

@Serializable
private data class OpenFoodFactsProduct(
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    @SerialName("serving_quantity") val servingQuantity: String? = null,
    val nutriments: OpenFoodFactsNutriments? = null
) {
    fun toFoodSearchResult(): FoodSearchResult? {
        val name = productName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val servingSizeG = servingQuantity?.toDoubleOrNull()
        val servingDescription = servingSize?.trim()?.takeIf { it.isNotBlank() }
            ?: servingSizeG?.let { "${it.toInt()}g" }
        return FoodSearchResult(
            name = name,
            brand = brands?.trim()?.takeIf { it.isNotBlank() },
            caloriesPer100g = nutriments?.energyKcal100g ?: 0.0,
            proteinPer100g = nutriments?.proteins100g ?: 0.0,
            carbPer100g = nutriments?.carbohydrates100g ?: 0.0,
            fatPer100g = nutriments?.fat100g ?: 0.0,
            servingSizeG = servingSizeG,
            servingDescription = servingDescription,
            source = "OpenFoodFacts"
        )
    }
}

@Serializable
private data class OpenFoodFactsNutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null
)
