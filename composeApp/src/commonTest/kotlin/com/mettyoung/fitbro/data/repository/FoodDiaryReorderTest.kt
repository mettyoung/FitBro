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
