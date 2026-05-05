package com.mettyoung.fitbro.data.cronometer

import com.mettyoung.fitbro.auth.CronometerOAuthConfig
import com.mettyoung.fitbro.auth.OAuthRepository
import com.mettyoung.fitbro.auth.OAuthResult
import com.mettyoung.fitbro.data.model.DailyIntake
import com.mettyoung.fitbro.data.model.Metabolism
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CronometerDataSourceImpl(
    private val oauthRepository: OAuthRepository
) : CronometerDataSource {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override suspend fun fetchDailyIntake(startDate: String, endDate: String): ApiResult<List<DailyIntake>> {
        val token = getValidAccessToken() ?: return ApiResult.Failure(CronometerApiError.Unauthorized)
        return try {
            val response = httpClient.get("${CronometerOAuthConfig.API_BASE_URL}/nutrition/") {
                header("Authorization", "Bearer $token")
                parameter("start_date", startDate)
                parameter("end_date", endDate)
            }
            when {
                response.status == HttpStatusCode.TooManyRequests ->
                    ApiResult.Failure(CronometerApiError.RateLimited)
                response.status == HttpStatusCode.Unauthorized ->
                    ApiResult.Failure(CronometerApiError.Unauthorized)
                !response.status.isSuccess() ->
                    ApiResult.Failure(CronometerApiError.HttpError(response.status.value, response.status.description))
                else -> {
                    val body = response.body<NutritionResponse>()
                    ApiResult.Success(body.toDailyIntakeList())
                }
            }
        } catch (e: Exception) {
            ApiResult.Failure(CronometerApiError.NetworkError(e))
        }
    }

    override suspend fun fetchMetabolism(startDate: String, endDate: String): ApiResult<List<Metabolism>> {
        val token = getValidAccessToken() ?: return ApiResult.Failure(CronometerApiError.Unauthorized)
        return try {
            val response = httpClient.get("${CronometerOAuthConfig.API_BASE_URL}/metabolics/") {
                header("Authorization", "Bearer $token")
                parameter("start_date", startDate)
                parameter("end_date", endDate)
            }
            when {
                response.status == HttpStatusCode.TooManyRequests ->
                    ApiResult.Failure(CronometerApiError.RateLimited)
                response.status == HttpStatusCode.Unauthorized ->
                    ApiResult.Failure(CronometerApiError.Unauthorized)
                !response.status.isSuccess() ->
                    ApiResult.Failure(CronometerApiError.HttpError(response.status.value, response.status.description))
                else -> {
                    val body = response.body<MetabolicsResponse>()
                    ApiResult.Success(body.toMetabolismList())
                }
            }
        } catch (e: Exception) {
            ApiResult.Failure(CronometerApiError.NetworkError(e))
        }
    }

    private suspend fun getValidAccessToken(): String? {
        return when (val result = oauthRepository.getValidToken()) {
            is OAuthResult.Success -> result.value.accessToken
            is OAuthResult.Failure -> null
        }
    }
}

@Serializable
private data class NutritionDay(
    @SerialName("date") val date: String,
    @SerialName("energy") val energy: Double = 0.0
)

@Serializable
private data class NutritionResponse(
    @SerialName("data") val data: List<NutritionDay> = emptyList()
) {
    fun toDailyIntakeList(): List<DailyIntake> = data.map {
        DailyIntake(date = it.date, totalCalories = it.energy)
    }
}

@Serializable
private data class MetabolicDay(
    @SerialName("date") val date: String,
    @SerialName("bmr") val bmr: Double = 0.0,
    @SerialName("tef") val tef: Double = 0.0
)

@Serializable
private data class MetabolicsResponse(
    @SerialName("data") val data: List<MetabolicDay> = emptyList()
) {
    fun toMetabolismList(): List<Metabolism> = data.map {
        Metabolism(date = it.date, bmr = it.bmr, tef = it.tef)
    }
}
