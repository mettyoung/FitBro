package com.mettyoung.fitbro.data.food

import com.mettyoung.fitbro.util.currentEpochMs
import com.mettyoung.fitbro.util.hmacSha1
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.util.encodeBase64
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlin.math.roundToInt
import kotlin.random.Random

private val fatSecretJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private inline fun <reified T> JsonElement.toList(): List<T> = when (this) {
    is JsonArray -> mapNotNull { runCatching { fatSecretJson.decodeFromJsonElement<T>(it) }.getOrNull() }
    is JsonObject -> runCatching { listOf(fatSecretJson.decodeFromJsonElement<T>(this)) }.getOrDefault(emptyList())
    else -> emptyList()
}

private fun percentEncode(s: String): String = buildString {
    s.forEach { c ->
        if (c.isLetterOrDigit() || c == '-' || c == '_' || c == '.' || c == '~') {
            append(c)
        } else {
            c.toString().encodeToByteArray().forEach { b ->
                append('%')
                append(b.toInt().and(0xff).toString(16).uppercase().padStart(2, '0'))
            }
        }
    }
}

private const val CONSUMER_KEY = "913a84a2b4824f3984bd6c530026dde9"
private const val CONSUMER_SECRET = "b3219fbded874a8dabef205d9c6481dd"
private const val API_URL = "https://platform.fatsecret.com/rest/server.api"

private fun generateNonce(): String =
    (1..16).map { Random.nextInt(36).toString(36) }.joinToString("")

private data class SignedRequest(
    val requestParams: Map<String, String>,
    val authorizationHeader: String
)

private fun buildSignedRequest(
    method: String,
    baseUrl: String,
    requestParams: Map<String, String>
): SignedRequest {
    val timestamp = (currentEpochMs() / 1000).toString()
    val nonce = generateNonce()

    val oauthParams = mapOf(
        "oauth_consumer_key" to CONSUMER_KEY,
        "oauth_nonce" to nonce,
        "oauth_signature_method" to "HMAC-SHA1",
        "oauth_timestamp" to timestamp,
        "oauth_version" to "1.0"
    )

    val allParams = (oauthParams + requestParams)
        .entries
        .sortedWith(compareBy({ it.key }, { it.value }))
        .joinToString("&") { "${percentEncode(it.key)}=${percentEncode(it.value)}" }

    val baseString = "${method.uppercase()}&${percentEncode(baseUrl)}&${percentEncode(allParams)}"
    val signingKey = "${percentEncode(CONSUMER_SECRET)}&"

    val hmacBytes = hmacSha1(signingKey.encodeToByteArray(), baseString.encodeToByteArray())

    val signature = hmacBytes.encodeBase64().replace("\r", "").replace("\n", "").trim()

    val oauthWithSig = oauthParams + mapOf("oauth_signature" to signature)
    val headerValue = "OAuth " + oauthWithSig.entries
        .sortedBy { it.key }
        .joinToString(", ") { "${percentEncode(it.key)}=\"${percentEncode(it.value)}\"" }

    return SignedRequest(requestParams, headerValue)
}

private fun Map<String, String>.toFormBody(): String =
    entries.joinToString("&") { "${percentEncode(it.key)}=${percentEncode(it.value)}" }

class FatSecretFoodDataSource : FoodDataSource {

    override val supportsBarcode = true
    override val supportsFoodDetail = true

    private val httpClient = HttpClient {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }

    override suspend fun search(query: String): FoodResult<List<FoodSearchResult>> {
        return try {
            val requestParams = mapOf(
                "method" to "foods.search",
                "search_expression" to query,
                "format" to "json",
                "max_results" to "20"
            )
            val sr = buildSignedRequest("POST", API_URL, requestParams)
            val response = httpClient.post(API_URL) {
                header("Authorization", sr.authorizationHeader)
                setBody(TextContent(sr.requestParams.toFormBody(), ContentType.Application.FormUrlEncoded))
            }
            val raw = response.bodyAsText()
            println("FatSecret search [${response.status}]: ${raw.take(500)}")
            val body = fatSecretJson.decodeFromString<FatSecretSearchResponse>(raw)
            val foods = body.foods?.food?.toList<FatSecretFood>() ?: emptyList()
            val results = foods.mapNotNull { it.toFoodSearchResult() }
            if (results.isEmpty()) FoodResult.Failure(FoodError.EmptyResults)
            else FoodResult.Success(results)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("FatSecret search error: ${e::class.simpleName}: ${e.message}")
            FoodResult.Failure(FoodError.NetworkError(e.message ?: "Network error"))
        }
    }

    override suspend fun getFoodDetail(foodId: String): FoodResult<FoodDetail> {
        return try {
            val params = mapOf(
                "method" to "food.get.v4",
                "food_id" to foodId,
                "format" to "json"
            )
            val sr = buildSignedRequest("POST", API_URL, params)
            val response = httpClient.post(API_URL) {
                header("Authorization", sr.authorizationHeader)
                setBody(TextContent(sr.requestParams.toFormBody(), ContentType.Application.FormUrlEncoded))
            }
            val detail = fatSecretJson.decodeFromString<FatSecretFoodDetailResponse>(response.bodyAsText()).food?.toFoodDetail()
            if (detail != null) FoodResult.Success(detail)
            else FoodResult.Failure(FoodError.EmptyResults)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FoodResult.Failure(FoodError.NetworkError(e.message ?: "Network error"))
        }
    }

    override suspend fun searchByBarcode(barcode: String): FoodResult<FoodDetail> {
        return try {
            val idParams = mapOf(
                "method" to "food.find_id_for_barcode",
                "barcode" to barcode,
                "format" to "json"
            )
            val idSr = buildSignedRequest("POST", API_URL, idParams)
            val idResponse = httpClient.post(API_URL) {
                header("Authorization", idSr.authorizationHeader)
                setBody(TextContent(idSr.requestParams.toFormBody(), ContentType.Application.FormUrlEncoded))
            }
            val foodId = fatSecretJson.decodeFromString<FatSecretBarcodeResponse>(idResponse.bodyAsText()).foodId?.value
                ?: return FoodResult.Failure(FoodError.EmptyResults)
            getFoodDetail(foodId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FoodResult.Failure(FoodError.NetworkError(e.message ?: "Network error"))
        }
    }

}

@Serializable
private data class FatSecretSearchResponse(
    val foods: FatSecretFoodList? = null
)

@Serializable
private data class FatSecretFoodList(
    val food: JsonElement? = null
)

@Serializable
private data class FatSecretBarcodeResponse(
    @SerialName("food_id") val foodId: FatSecretFoodIdValue? = null
)

@Serializable
private data class FatSecretFoodIdValue(
    val value: String? = null
)

@Serializable
private data class FatSecretFoodDetailResponse(
    val food: FatSecretFood? = null
)

@Serializable
private data class FatSecretFood(
    @SerialName("food_id") val foodId: String? = null,
    @SerialName("food_name") val foodName: String? = null,
    @SerialName("brand_name") val brandName: String? = null,
    @SerialName("food_description") val foodDescription: String? = null,
    val servings: FatSecretServings? = null
) {
    fun toFoodDetail(): FoodDetail? {
        val id = foodId ?: return null
        val name = foodName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val servingList = servings?.serving?.toList<FatSecretServing>() ?: emptyList()
        return FoodDetail(
            foodId = id,
            name = name,
            brand = brandName?.trim()?.takeIf { it.isNotBlank() },
            servings = servingList.map { it.toServingOption() },
            source = "FatSecret"
        )
    }

    // Only called from foods.search — no servings on search results, only food_description
    fun toFoodSearchResult(): FoodSearchResult? {
        val name = foodName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val desc = foodDescription ?: return null
        return FoodSearchResult(
            name = name,
            brand = brandName?.trim()?.takeIf { it.isNotBlank() },
            foodId = foodId,
            displayText = parseDisplayText(desc)
        )
    }

    // Parses "Per 100g - Calories: 77kcal | ..." → "100g - 77kcal"
    private fun parseDisplayText(desc: String): String {
        val match = Regex("Per (.+?) - Calories: (\\d+(?:\\.\\d+)?)kcal").find(desc)
            ?: return desc.take(60)
        val serving = match.groupValues[1]
        val kcal = match.groupValues[2].toDoubleOrNull()?.roundToInt()?.toString()
            ?: match.groupValues[2]
        return "$serving - ${kcal}kcal"
    }
}

@Serializable
private data class FatSecretServings(
    val serving: JsonElement? = null
)

@Serializable
private data class FatSecretServing(
    @SerialName("serving_id") val servingId: String? = null,
    @SerialName("serving_description") val servingDescription: String? = null,
    @SerialName("metric_serving_amount") val metricServingAmount: String? = null,
    @SerialName("metric_serving_unit") val metricServingUnit: String? = null,
    val calories: String? = null,
    val fat: String? = null,
    val carbohydrate: String? = null,
    val protein: String? = null
) {
    fun toServingOption(): ServingOption {
        val metricAmt = metricServingAmount?.toDoubleOrNull()
        val desc = servingDescription?.trim() ?: "Serving"
        val description = if (metricAmt != null && metricServingUnit != null && !desc.contains(metricServingUnit)) {
            "$desc - ${metricAmt.roundToInt()}$metricServingUnit"
        } else {
            desc
        }
        return ServingOption(
            servingId = servingId,
            description = description,
            metricAmount = metricAmt,
            metricUnit = metricServingUnit,
            calories = calories?.toDoubleOrNull() ?: 0.0,
            proteinG = protein?.toDoubleOrNull() ?: 0.0,
            carbG = carbohydrate?.toDoubleOrNull() ?: 0.0,
            fatG = fat?.toDoubleOrNull() ?: 0.0
        )
    }
}
