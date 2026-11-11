package com.mettyoung.fitbro.data.sync

import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.repository.PendingChange
import com.mettyoung.fitbro.data.repository.RemoteChange
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed interface SyncResult<out T> {
    data class Success<T>(val value: T) : SyncResult<T>
    sealed interface Failure : SyncResult<Nothing> {
        data object NotConfigured : Failure
        data object Unauthorized : Failure
        data class Http(val status: Int) : Failure
        data class Network(val cause: Throwable) : Failure
        data class Unexpected(val cause: Throwable) : Failure
    }
}

data class PullPage(val changes: List<RemoteChange>, val cursor: Long)

data class WeekdayGoals(val calories: Double, val proteinG: Double, val carbG: Double, val fatG: Double)

/** Wire format shared with mcp-server/src/sync.ts. Null fields mean "tombstone" when deleted = true. */
@Serializable
internal data class ChangeDto(
    val uuid: String,
    val updatedAt: Long,
    val deleted: Boolean,
    val date: String? = null,
    val mealType: String? = null,
    val foodName: String? = null,
    val brandName: String? = null,
    val calories: Double? = null,
    val proteinG: Double? = null,
    val carbG: Double? = null,
    val fatG: Double? = null,
    val servingSizeG: Double? = null,
    val servingUnit: String? = null,
    val foodId: String? = null,
    val servingId: String? = null,
    val servingQuantity: Double? = null,
    val sortOrder: Long? = null
)

@Serializable
internal data class PushBody(val changes: List<ChangeDto>)

@Serializable
internal data class PullResponse(val changes: List<ChangeDto>, val cursor: Long)

@Serializable
internal data class GoalsDto(val calories: Double, val proteinG: Double, val carbG: Double, val fatG: Double)

@Serializable
internal data class SettingsBody(val goalsByWeekday: Map<String, GoalsDto>)

internal fun PendingChange.toDto(): ChangeDto {
    val e = entry ?: return ChangeDto(uuid = uuid, updatedAt = queuedAt, deleted = true)
    return ChangeDto(
        uuid = e.uuid,
        updatedAt = e.updatedAt,
        deleted = false,
        date = e.date,
        mealType = e.mealType,
        foodName = e.foodName,
        brandName = e.brandName,
        calories = e.calories,
        proteinG = e.proteinG,
        carbG = e.carbG,
        fatG = e.fatG,
        servingSizeG = e.servingSizeG,
        servingUnit = e.servingUnit,
        foodId = e.foodId,
        servingId = e.servingId,
        servingQuantity = e.servingQuantity,
        sortOrder = e.sortOrder
    )
}

/** Returns null for a malformed non-deleted row rather than failing the whole page. */
internal fun ChangeDto.toRemoteChange(): RemoteChange? {
    if (deleted) return RemoteChange.Delete(uuid, updatedAt)
    return RemoteChange.Upsert(
        FoodDiaryEntry(
            date = date ?: return null,
            mealType = mealType ?: return null,
            foodName = foodName ?: return null,
            brandName = brandName,
            calories = calories ?: return null,
            proteinG = proteinG ?: return null,
            carbG = carbG ?: return null,
            fatG = fatG ?: return null,
            servingSizeG = servingSizeG ?: return null,
            servingUnit = servingUnit ?: return null,
            foodId = foodId,
            sortOrder = sortOrder ?: 0,
            servingId = servingId,
            servingQuantity = servingQuantity,
            uuid = uuid,
            updatedAt = updatedAt
        )
    )
}

class SyncApi(private val settings: SyncSettings) {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
    }

    private fun configured(): Pair<String, String>? {
        val url = settings.getSyncUrl()
        val token = settings.getSyncToken()
        return if (url.isBlank() || token.isBlank()) null else url to token
    }

    private suspend fun <T> call(
        block: suspend (baseUrl: String, token: String) -> HttpResponse,
        parse: suspend (HttpResponse) -> T
    ): SyncResult<T> {
        val (baseUrl, token) = configured() ?: return SyncResult.Failure.NotConfigured
        return try {
            val response = block(baseUrl, token)
            when {
                response.status.value == 401 -> SyncResult.Failure.Unauthorized
                !response.status.isSuccess() -> SyncResult.Failure.Http(response.status.value)
                else -> SyncResult.Success(parse(response))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SyncResult.Failure.Network(e)
        }
    }

    suspend fun push(changes: List<PendingChange>): SyncResult<Unit> = call(
        block = { baseUrl, token ->
            client.post("$baseUrl/sync/push") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(PushBody(changes.map { it.toDto() }))
            }
        },
        parse = { }
    )

    suspend fun pull(since: Long): SyncResult<PullPage> = call(
        block = { baseUrl, token ->
            client.get("$baseUrl/sync/pull") {
                bearerAuth(token)
                parameter("since", since)
            }
        },
        parse = { response ->
            val body = response.body<PullResponse>()
            PullPage(body.changes.mapNotNull { it.toRemoteChange() }, body.cursor)
        }
    )

    /** Keys are weekdays 0 = Monday … 6 = Sunday. */
    suspend fun putGoals(goalsByWeekday: Map<Int, WeekdayGoals>): SyncResult<Unit> = call(
        block = { baseUrl, token ->
            client.put("$baseUrl/sync/settings") {
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(SettingsBody(goalsByWeekday.entries.associate { (day, g) ->
                    day.toString() to GoalsDto(g.calories, g.proteinG, g.carbG, g.fatG)
                }))
            }
        },
        parse = { }
    )
}
