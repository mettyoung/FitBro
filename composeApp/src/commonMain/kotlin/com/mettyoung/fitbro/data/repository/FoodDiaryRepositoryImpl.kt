package com.mettyoung.fitbro.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.mettyoung.fitbro.data.db.FitBroDatabase
import com.mettyoung.fitbro.data.model.DailyMacroTotals
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.util.currentEpochMs
import com.mettyoung.fitbro.util.newUuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FoodDiaryRepositoryImpl(
    private val database: FitBroDatabase,
    private val now: () -> Long = ::currentEpochMs
) : FoodDiaryRepository, FoodDiarySyncStore {

    private val queries get() = database.foodDiaryQueries

    /** Must be called inside a transaction alongside the mutation it records (FR-11). */
    private fun enqueue(uuid: String, deleted: Boolean, at: Long) {
        queries.enqueueOutbox(uuid = uuid, deleted = if (deleted) 1L else 0L, queued_at = at)
    }

    private fun enqueueId(id: Long, at: Long) {
        queries.uuidForId(id).executeAsOneOrNull()?.let { enqueue(it, deleted = false, at = at) }
    }

    override fun getEntriesForDate(date: String): Flow<List<FoodDiaryEntry>> =
        database.foodDiaryQueries.getEntriesByDate(date)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun addEntry(entry: FoodDiaryEntry): Long = withContext(Dispatchers.Default) {
        val at = now()
        val uuid = entry.uuid.ifBlank { newUuid() }
        database.foodDiaryQueries.transactionWithResult {
            val nextSortOrder = database.foodDiaryQueries
                .maxSortOrderForDateMeal(entry.date, entry.mealType)
                .executeAsOne() + 1
            database.foodDiaryQueries.insertEntry(
                date = entry.date,
                mealType = entry.mealType,
                foodName = entry.foodName,
                brandName = entry.brandName,
                calories = entry.calories,
                proteinG = entry.proteinG,
                carbG = entry.carbG,
                fatG = entry.fatG,
                servingSizeG = entry.servingSizeG,
                servingUnit = entry.servingUnit,
                food_id = entry.foodId,
                sortOrder = nextSortOrder,
                serving_id = entry.servingId,
                serving_quantity = entry.servingQuantity,
                uuid = uuid,
                updated_at = at
            )
            enqueue(uuid, deleted = false, at = at)
            database.foodDiaryQueries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun updateEntry(entry: FoodDiaryEntry): Unit = withContext(Dispatchers.Default) {
        val at = now()
        queries.transaction {
            queries.updateEntry(
                foodName = entry.foodName,
                brandName = entry.brandName,
                calories = entry.calories,
                proteinG = entry.proteinG,
                carbG = entry.carbG,
                fatG = entry.fatG,
                servingSizeG = entry.servingSizeG,
                servingUnit = entry.servingUnit,
                food_id = entry.foodId,
                serving_id = entry.servingId,
                serving_quantity = entry.servingQuantity,
                updated_at = at,
                id = entry.id
            )
            enqueueId(entry.id, at)
        }
    }

    override suspend fun deleteEntry(id: Long): Unit = withContext(Dispatchers.Default) {
        val at = now()
        queries.transaction {
            val uuid = queries.uuidForId(id).executeAsOneOrNull()
            queries.deleteEntry(id)
            if (uuid != null) enqueue(uuid, deleted = true, at = at)
        }
    }

    override suspend fun reorderMeal(
        date: String,
        mealType: String,
        orderedIds: List<Long>
    ): Unit = withContext(Dispatchers.Default) {
        val at = now()
        database.foodDiaryQueries.transaction {
            orderedIds.forEachIndexed { index, id ->
                database.foodDiaryQueries.updateSortOrder(sortOrder = index.toLong(), updated_at = at, id = id)
                enqueueId(id, at)
            }
        }
    }

    override suspend fun moveEntryToPosition(
        date: String,
        movedId: Long,
        targetMeal: String,
        targetOrderedIds: List<Long>,
        sourceMeal: String,
        sourceOrderedIds: List<Long>
    ): Unit = withContext(Dispatchers.Default) {
        val at = now()
        database.foodDiaryQueries.transaction {
            targetOrderedIds.forEachIndexed { index, id ->
                if (id == movedId) {
                    database.foodDiaryQueries.moveEntryToMeal(
                        mealType = targetMeal,
                        sortOrder = index.toLong(),
                        updated_at = at,
                        id = id
                    )
                } else {
                    database.foodDiaryQueries.updateSortOrder(sortOrder = index.toLong(), updated_at = at, id = id)
                }
                enqueueId(id, at)
            }
            sourceOrderedIds.forEachIndexed { index, id ->
                database.foodDiaryQueries.updateSortOrder(sortOrder = index.toLong(), updated_at = at, id = id)
                enqueueId(id, at)
            }
        }
    }

    override fun getRecentFoods(limit: Int): Flow<List<FoodDiaryEntry>> =
        database.foodDiaryQueries.recentFoods(limit.toLong())
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain().copy(id = 0, sortOrder = 0) } }

    override suspend fun copyMealToDate(
        sourceDate: String,
        mealType: String,
        targetDate: String
    ): Unit = withContext(Dispatchers.Default) {
        val at = now()
        database.foodDiaryQueries.transaction {
            val sources = database.foodDiaryQueries
                .getEntriesByDateAndMeal(sourceDate, mealType)
                .executeAsList()
            if (sources.isEmpty()) return@transaction
            var nextSortOrder = database.foodDiaryQueries
                .maxSortOrderForDateMeal(targetDate, mealType)
                .executeAsOne() + 1
            sources.forEach { row ->
                val uuid = newUuid()
                database.foodDiaryQueries.insertEntry(
                    date = targetDate,
                    mealType = mealType,
                    foodName = row.foodName,
                    brandName = row.brandName,
                    calories = row.calories,
                    proteinG = row.proteinG,
                    carbG = row.carbG,
                    fatG = row.fatG,
                    servingSizeG = row.servingSizeG,
                    servingUnit = row.servingUnit,
                    food_id = row.food_id,
                    sortOrder = nextSortOrder,
                    serving_id = row.serving_id,
                    serving_quantity = row.serving_quantity,
                    uuid = uuid,
                    updated_at = at
                )
                enqueue(uuid, deleted = false, at = at)
                nextSortOrder++
            }
        }
    }

    override fun getDailyTotals(date: String): Flow<DailyMacroTotals> =
        database.foodDiaryQueries.getDailyTotals(date)
            .asFlow()
            .mapToOne(Dispatchers.Default)
            .map { row ->
                DailyMacroTotals(
                    date = date,
                    calories = row.calories ?: 0.0,
                    proteinG = row.proteinG ?: 0.0,
                    carbG = row.carbG ?: 0.0,
                    fatG = row.fatG ?: 0.0
                )
            }

    override fun getDailyTotalsForRange(startDate: String, endDate: String): Flow<List<DailyMacroTotals>> =
        database.foodDiaryQueries.getDailyTotalsForDateRange(startDate, endDate)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    DailyMacroTotals(
                        date = row.date,
                        calories = row.calories ?: 0.0,
                        proteinG = row.proteinG ?: 0.0,
                        carbG = row.carbG ?: 0.0,
                        fatG = row.fatG ?: 0.0
                    )
                }
            }

    override suspend fun pendingChanges(): List<PendingChange> = withContext(Dispatchers.Default) {
        queries.transactionWithResult {
            queries.listOutbox().executeAsList().map { row ->
                val entry = if (row.deleted == 1L) null else queries.getByUuid(row.uuid).executeAsOneOrNull()?.toDomain()
                PendingChange(uuid = row.uuid, queuedAt = row.queued_at, entry = entry)
            }
        }
    }

    override suspend fun markPushed(changes: List<PendingChange>): Unit = withContext(Dispatchers.Default) {
        queries.transaction {
            changes.forEach { queries.deleteOutboxIfUnchanged(uuid = it.uuid, queued_at = it.queuedAt) }
        }
    }

    override suspend fun enqueueAll(): Unit = withContext(Dispatchers.Default) {
        val at = now()
        queries.transaction {
            queries.getAllEntries().executeAsList().forEach { enqueue(it.uuid, deleted = false, at = at) }
        }
    }

    // FR-12: last-write-wins; never touches the outbox, so pulled changes aren't echoed back.
    override suspend fun applyRemoteChanges(changes: List<RemoteChange>): List<FoodDiaryEntry> =
        withContext(Dispatchers.Default) {
            queries.transactionWithResult {
                changes.mapNotNull { change ->
                    val local = queries.getByUuid(change.uuid).executeAsOneOrNull()
                    val pending = queries.outboxEntryFor(change.uuid).executeAsOneOrNull()
                    val localVersion = maxOf(local?.updated_at ?: Long.MIN_VALUE, pending?.queued_at ?: Long.MIN_VALUE)
                    if (change.updatedAt <= localVersion) return@mapNotNull null
                    when (change) {
                        is RemoteChange.Delete -> {
                            queries.deleteByUuid(change.uuid)
                            null
                        }
                        is RemoteChange.Upsert -> {
                            val e = change.entry
                            if (local == null) {
                                queries.insertEntry(
                                    date = e.date, mealType = e.mealType, foodName = e.foodName, brandName = e.brandName,
                                    calories = e.calories, proteinG = e.proteinG, carbG = e.carbG, fatG = e.fatG,
                                    servingSizeG = e.servingSizeG, servingUnit = e.servingUnit, food_id = e.foodId,
                                    sortOrder = e.sortOrder, serving_id = e.servingId, serving_quantity = e.servingQuantity,
                                    uuid = e.uuid, updated_at = e.updatedAt
                                )
                            } else {
                                queries.updateFromRemote(
                                    date = e.date, mealType = e.mealType, foodName = e.foodName, brandName = e.brandName,
                                    calories = e.calories, proteinG = e.proteinG, carbG = e.carbG, fatG = e.fatG,
                                    servingSizeG = e.servingSizeG, servingUnit = e.servingUnit, food_id = e.foodId,
                                    sortOrder = e.sortOrder, serving_id = e.servingId, serving_quantity = e.servingQuantity,
                                    updated_at = e.updatedAt, uuid = e.uuid
                                )
                            }
                            queries.getByUuid(e.uuid).executeAsOne().toDomain()
                        }
                    }
                }
            }
        }
}

private fun com.mettyoung.fitbro.data.db.FoodDiaryEntry.toDomain() = FoodDiaryEntry(
    id = id,
    date = date,
    mealType = mealType,
    foodName = foodName,
    brandName = brandName,
    calories = calories,
    proteinG = proteinG,
    carbG = carbG,
    fatG = fatG,
    servingSizeG = servingSizeG,
    servingUnit = servingUnit,
    foodId = food_id,
    sortOrder = sortOrder,
    servingId = serving_id,
    servingQuantity = serving_quantity,
    uuid = uuid,
    updatedAt = updated_at
)
