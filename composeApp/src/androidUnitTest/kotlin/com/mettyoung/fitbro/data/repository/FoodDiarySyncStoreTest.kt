package com.mettyoung.fitbro.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.mettyoung.fitbro.data.db.FitBroDatabase
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.MealType
import com.mettyoung.fitbro.data.sync.toDto
import com.mettyoung.fitbro.data.sync.toRemoteChange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FoodDiarySyncStoreTest {
    private var clock = 1_000L
    private lateinit var repo: FoodDiaryRepositoryImpl

    @BeforeTest
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FitBroDatabase.Schema.create(driver)
        repo = FoodDiaryRepositoryImpl(FitBroDatabase(driver), now = { clock })
    }

    private fun entry(name: String, meal: String = MealType.BREAKFAST, uuid: String = "") = FoodDiaryEntry(
        date = "2026-11-10",
        mealType = meal,
        foodName = name,
        calories = 100.0,
        proteinG = 10.0,
        carbG = 10.0,
        fatG = 2.0,
        servingSizeG = 100.0,
        servingUnit = "g",
        uuid = uuid
    )

    private suspend fun entries() = repo.getEntriesForDate("2026-11-10").first()

    @Test
    fun everyMutationIsQueued() = runBlocking {
        val id = repo.addEntry(entry("a"))
        val uuid = entries().single().uuid
        assertTrue(uuid.isNotBlank())
        assertEquals(listOf(uuid), repo.pendingChanges().map { it.uuid })

        repo.markPushed(repo.pendingChanges())
        clock = 2_000
        repo.updateEntry(entries().single().copy(foodName = "a2"))
        val update = repo.pendingChanges().single()
        assertEquals("a2", update.entry?.foodName)
        assertEquals(2_000L, update.entry?.updatedAt)

        repo.markPushed(repo.pendingChanges())
        clock = 3_000
        repo.deleteEntry(id)
        val tombstone = repo.pendingChanges().single()
        assertEquals(uuid, tombstone.uuid)
        assertNull(tombstone.entry)
        assertEquals(true, tombstone.toDto().deleted)
    }

    @Test
    fun markPushedKeepsChangesMadeDuringThePush() = runBlocking {
        repo.addEntry(entry("a"))
        val snapshot = repo.pendingChanges()
        clock = 2_000
        repo.updateEntry(entries().single().copy(foodName = "edited mid-push"))
        repo.markPushed(snapshot)
        assertEquals("edited mid-push", repo.pendingChanges().single().entry?.foodName)
    }

    @Test
    fun copyMealGivesCopiesNewUuids() = runBlocking {
        repo.addEntry(entry("a"))
        repo.copyMealToDate("2026-11-10", MealType.BREAKFAST, "2026-11-11")
        val uuids = repo.pendingChanges().map { it.uuid }
        assertEquals(2, uuids.toSet().size)
    }

    @Test
    fun remoteChangesApplyWithLastWriteWinsAndAreNotEchoed() = runBlocking {
        val remote = entry("from grok", uuid = "r1").copy(updatedAt = 5_000)
        val applied = repo.applyRemoteChanges(listOf(RemoteChange.Upsert(remote)))
        assertEquals(listOf("r1"), applied.map { it.uuid })
        assertEquals("from grok", entries().single().foodName)
        assertTrue(repo.pendingChanges().isEmpty(), "pulled changes must not be queued for push")

        // Older or equal versions are ignored.
        val stale = repo.applyRemoteChanges(listOf(RemoteChange.Upsert(remote.copy(foodName = "stale", updatedAt = 5_000))))
        assertTrue(stale.isEmpty())
        assertEquals("from grok", entries().single().foodName)

        // A newer version updates in place, keeping the local id.
        val localId = entries().single().id
        repo.applyRemoteChanges(listOf(RemoteChange.Upsert(remote.copy(foodName = "newer", updatedAt = 6_000))))
        assertEquals(localId to "newer", entries().single().let { it.id to it.foodName })

        repo.applyRemoteChanges(listOf(RemoteChange.Delete("r1", 7_000)))
        assertTrue(entries().isEmpty())
    }

    @Test
    fun remoteEditDoesNotResurrectAPendingLocalDelete() = runBlocking {
        val id = repo.addEntry(entry("a", uuid = "u1"))
        clock = 9_000
        repo.deleteEntry(id)
        repo.applyRemoteChanges(listOf(RemoteChange.Upsert(entry("remote edit", uuid = "u1").copy(updatedAt = 8_000))))
        assertTrue(entries().isEmpty())
    }

    @Test
    fun localEditNewerThanRemoteWins() = runBlocking {
        repo.addEntry(entry("local", uuid = "u1"))
        clock = 9_000
        repo.updateEntry(entries().single().copy(foodName = "local newer"))
        repo.applyRemoteChanges(listOf(RemoteChange.Upsert(entry("remote", uuid = "u1").copy(updatedAt = 8_000))))
        assertEquals("local newer", entries().single().foodName)
    }

    @Test
    fun enqueueAllBackfillsEveryEntry() = runBlocking {
        repo.addEntry(entry("a"))
        repo.addEntry(entry("b", meal = MealType.LUNCH))
        repo.markPushed(repo.pendingChanges())
        repo.enqueueAll()
        assertEquals(2, repo.pendingChanges().size)
    }

    @Test
    fun wireFormatRoundTrips() = runBlocking {
        repo.addEntry(entry("a"))
        val dto = repo.pendingChanges().single().toDto()
        val back = dto.toRemoteChange() as RemoteChange.Upsert
        val original = entries().single()
        assertEquals(original.copy(id = 0), back.entry)
    }
}
