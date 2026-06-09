package com.mettyoung.fitbro.data.repository

import com.mettyoung.fitbro.data.model.DailyMacroTotals
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.MealType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * In-memory fake mirroring FoodDiaryRepositoryImpl's sortOrder contract:
 * - addEntry assigns sortOrder = max(sortOrder for date+meal) + 1
 * - getEntriesForDate orders by (mealType, sortOrder, id)
 * - reorderMeal rewrites sortOrder = index for the supplied id order
 *
 * The real impl delegates the same semantics to SQLDelight; this guards the
 * contract that US-002's drag UI and daily-total math depend on.
 */
private class FakeFoodDiaryRepository : FoodDiaryRepository {
    private val rows = MutableStateFlow<List<FoodDiaryEntry>>(emptyList())
    private var nextId = 1L

    override fun getEntriesForDate(date: String): Flow<List<FoodDiaryEntry>> =
        rows.map { all ->
            all.filter { it.date == date }
                .sortedWith(compareBy({ it.mealType }, { it.sortOrder }, { it.id }))
        }

    override suspend fun addEntry(entry: FoodDiaryEntry): Long {
        val maxOrder = rows.value
            .filter { it.date == entry.date && it.mealType == entry.mealType }
            .maxOfOrNull { it.sortOrder } ?: -1L
        val id = nextId++
        rows.value = rows.value + entry.copy(id = id, sortOrder = maxOrder + 1)
        return id
    }

    override suspend fun updateEntry(entry: FoodDiaryEntry) {
        rows.value = rows.value.map { if (it.id == entry.id) entry else it }
    }

    override suspend fun deleteEntry(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    override suspend fun reorderMeal(date: String, mealType: String, orderedIds: List<Long>) {
        rows.value = rows.value.map { row ->
            val idx = orderedIds.indexOf(row.id)
            if (row.date == date && row.mealType == mealType && idx >= 0) {
                row.copy(sortOrder = idx.toLong())
            } else row
        }
    }

    override suspend fun moveEntryToPosition(
        date: String,
        movedId: Long,
        targetMeal: String,
        targetOrderedIds: List<Long>,
        sourceMeal: String,
        sourceOrderedIds: List<Long>
    ) {
        rows.value = rows.value.map { row ->
            val tIdx = targetOrderedIds.indexOf(row.id)
            val sIdx = sourceOrderedIds.indexOf(row.id)
            when {
                row.id == movedId -> row.copy(
                    mealType = targetMeal,
                    sortOrder = targetOrderedIds.indexOf(movedId).toLong()
                )
                tIdx >= 0 -> row.copy(sortOrder = tIdx.toLong())
                sIdx >= 0 -> row.copy(sortOrder = sIdx.toLong())
                else -> row
            }
        }
    }

    override fun getRecentFoods(limit: Int): Flow<List<FoodDiaryEntry>> =
        rows.map { all ->
            all.groupBy { row ->
                row.foodId
                    ?: (row.foodName.trim().lowercase() + "|" + (row.brandName ?: "").trim().lowercase())
            }
                .map { (_, group) -> group.maxBy { it.id } }
                .sortedByDescending { it.id }
                .take(limit)
                .map { it.copy(id = 0, sortOrder = 0) }
        }

    override suspend fun copyMealToDate(sourceDate: String, mealType: String, targetDate: String) {
        val sources = rows.value
            .filter { it.date == sourceDate && it.mealType == mealType }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
        if (sources.isEmpty()) return
        var nextSortOrder = (rows.value
            .filter { it.date == targetDate && it.mealType == mealType }
            .maxOfOrNull { it.sortOrder } ?: -1L) + 1
        val copies = sources.map { row ->
            row.copy(id = nextId++, date = targetDate, sortOrder = nextSortOrder++)
        }
        rows.value = rows.value + copies
    }

    override fun getDailyTotals(date: String): Flow<DailyMacroTotals> =
        rows.map { all ->
            val d = all.filter { it.date == date }
            DailyMacroTotals(
                date = date,
                calories = d.sumOf { it.calories },
                proteinG = d.sumOf { it.proteinG },
                carbG = d.sumOf { it.carbG },
                fatG = d.sumOf { it.fatG }
            )
        }

    override fun getDailyTotalsForRange(startDate: String, endDate: String): Flow<List<DailyMacroTotals>> =
        throw NotImplementedError()
}

class FoodDiaryReorderTest {

    private fun entry(name: String, meal: String = MealType.BREAKFAST, cal: Double = 100.0) =
        FoodDiaryEntry(
            date = "2026-06-06",
            mealType = meal,
            foodName = name,
            calories = cal,
            proteinG = 1.0,
            carbG = 2.0,
            fatG = 3.0,
            servingSizeG = 100.0,
            servingUnit = "g"
        )

    @Test
    fun addEntryAssignsIncrementingSortOrderPerMeal() = runSync {
        val repo = FakeFoodDiaryRepository()
        repo.addEntry(entry("a"))
        repo.addEntry(entry("b"))
        repo.addEntry(entry("c", meal = MealType.LUNCH))

        val entries = repo.getEntriesForDateOnce()
        val breakfast = entries.filter { it.mealType == MealType.BREAKFAST }
        assertEquals(listOf(0L, 1L), breakfast.map { it.sortOrder })
        // Lunch sortOrder restarts at 0 independent of breakfast.
        val lunch = entries.first { it.mealType == MealType.LUNCH }
        assertEquals(0L, lunch.sortOrder)
    }

    @Test
    fun reorderMealPersistsNewOrderAndPreservesTotals() = runSync {
        val repo = FakeFoodDiaryRepository()
        val id1 = repo.addEntry(entry("a", cal = 100.0))
        val id2 = repo.addEntry(entry("b", cal = 200.0))
        val id3 = repo.addEntry(entry("c", cal = 300.0))

        repo.reorderMeal("2026-06-06", MealType.BREAKFAST, listOf(id3, id1, id2))

        val ordered = repo.getEntriesForDateOnce().map { it.foodName }
        assertEquals(listOf("c", "a", "b"), ordered)

        // Daily totals must be unchanged by reordering.
        val totals = repo.getDailyTotalsOnce()
        assertEquals(600.0, totals.calories, 0.001)
    }

    @Test
    fun moveEntryToPositionTransfersMealAtIndex() = runSync {
        val repo = FakeFoodDiaryRepository()
        val l1 = repo.addEntry(entry("lunch1", meal = MealType.LUNCH, cal = 100.0))
        val l2 = repo.addEntry(entry("lunch2", meal = MealType.LUNCH, cal = 200.0))
        val d1 = repo.addEntry(entry("dinner1", meal = MealType.DINNER, cal = 300.0))
        val d2 = repo.addEntry(entry("dinner2", meal = MealType.DINNER, cal = 400.0))

        // Drag lunch1 into dinner, inserted between dinner1 and dinner2.
        repo.moveEntryToPosition(
            date = "2026-06-06",
            movedId = l1,
            targetMeal = MealType.DINNER,
            targetOrderedIds = listOf(d1, l1, d2),
            sourceMeal = MealType.LUNCH,
            sourceOrderedIds = listOf(l2)
        )

        val dinner = repo.getEntriesForDateOnce().filter { it.mealType == MealType.DINNER }
        assertEquals(listOf("dinner1", "lunch1", "dinner2"), dinner.map { it.foodName })
        assertEquals(listOf(0L, 1L, 2L), dinner.map { it.sortOrder })
        // Lunch compacted to just lunch2 at order 0.
        val lunch = repo.getEntriesForDateOnce().filter { it.mealType == MealType.LUNCH }
        assertEquals(listOf("lunch2"), lunch.map { it.foodName })
        assertEquals(0L, lunch.single().sortOrder)
        // Daily totals unchanged by the move.
        assertEquals(1000.0, repo.getDailyTotalsOnce().calories, 0.001)
    }

    @Test
    fun moveEntryToPositionHandlesIntraMealReorder() = runSync {
        val repo = FakeFoodDiaryRepository()
        val a = repo.addEntry(entry("a"))
        val b = repo.addEntry(entry("b"))
        val c = repo.addEntry(entry("c"))

        // Same meal: move c to front. sourceOrderedIds empty since source == target.
        repo.moveEntryToPosition(
            date = "2026-06-06",
            movedId = c,
            targetMeal = MealType.BREAKFAST,
            targetOrderedIds = listOf(c, a, b),
            sourceMeal = MealType.BREAKFAST,
            sourceOrderedIds = emptyList()
        )

        assertEquals(listOf("c", "a", "b"), repo.getEntriesForDateOnce().map { it.foodName })
    }

    @Test
    fun getRecentFoodsDedupsOrdersAndLimits() = runSync {
        val repo = FakeFoodDiaryRepository()
        // "a" logged twice (last serving wins), plus b and c.
        repo.addEntry(entry("a", cal = 100.0))
        repo.addEntry(entry("b", cal = 200.0))
        repo.addEntry(entry("a", cal = 150.0)) // newer log of same food
        repo.addEntry(entry("c", cal = 300.0))

        val recent = repo.getRecentFoods(20).first()
        // Distinct foods only: a, b, c (one row for a).
        assertEquals(listOf("c", "a", "b"), recent.map { it.foodName })
        // Most-recent serving of "a" is the 150-cal one.
        assertEquals(150.0, recent.first { it.foodName == "a" }.calories, 0.001)
        // Detached templates.
        assertEquals(listOf(0L, 0L, 0L), recent.map { it.id })

        // Limit respected.
        assertEquals(listOf("c", "a"), repo.getRecentFoods(2).first().map { it.foodName })
    }

    @Test
    fun copyMealToDateAppendsAfterExistingTargetEntries() = runSync {
        val repo = FakeFoodDiaryRepository()
        val src = "2026-06-06"
        val tgt = "2026-06-07"
        // Source Lunch: two foods.
        repo.addEntry(entry("s1", meal = MealType.LUNCH, cal = 100.0).copy(date = src))
        repo.addEntry(entry("s2", meal = MealType.LUNCH, cal = 200.0).copy(date = src))
        // Target Lunch already has one entry that must stay first.
        repo.addEntry(entry("existing", meal = MealType.LUNCH, cal = 50.0).copy(date = tgt))

        repo.copyMealToDate(src, MealType.LUNCH, tgt)

        val targetLunch = repo.getEntriesForDate(tgt).first()
            .filter { it.mealType == MealType.LUNCH }
        assertEquals(listOf("existing", "s1", "s2"), targetLunch.map { it.foodName })
        assertEquals(listOf(0L, 1L, 2L), targetLunch.map { it.sortOrder })

        // Source slot unchanged.
        val srcLunch = repo.getEntriesForDate(src).first()
            .filter { it.mealType == MealType.LUNCH }
        assertEquals(listOf("s1", "s2"), srcLunch.map { it.foodName })
    }

    @Test
    fun copyMealToDateNoOpWhenSourceEmpty() = runSync {
        val repo = FakeFoodDiaryRepository()
        repo.addEntry(entry("x", meal = MealType.DINNER).copy(date = "2026-06-07"))
        repo.copyMealToDate("2026-06-06", MealType.DINNER, "2026-06-07")
        val dinner = repo.getEntriesForDate("2026-06-07").first()
            .filter { it.mealType == MealType.DINNER }
        assertEquals(listOf("x"), dinner.map { it.foodName })
    }

    private suspend fun FoodDiaryRepository.getEntriesForDateOnce(): List<FoodDiaryEntry> =
        getEntriesForDate("2026-06-06").first()

    private suspend fun FoodDiaryRepository.getDailyTotalsOnce(): DailyMacroTotals =
        getDailyTotals("2026-06-06").first()
}

/**
 * Runs a suspend block synchronously. The fake repo never truly suspends
 * (StateFlow emits its current value immediately), so the coroutine completes
 * before startCoroutine returns. Surfaces any failure via the continuation.
 */
private fun runSync(block: suspend () -> Unit) {
    var failure: Throwable? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result ->
        failure = result.exceptionOrNull()
    })
    failure?.let { throw it }
}
