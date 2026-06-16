package com.mettyoung.fitbro.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.mettyoung.fitbro.data.db.FitBroDatabase
import com.mettyoung.fitbro.data.model.CardioSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CardioRepositoryImpl(private val database: FitBroDatabase) : CardioRepository {

    override suspend fun logSession(date: String, minutes: Int, note: String?): Unit =
        withContext(Dispatchers.Default) {
            database.cardioSessionQueries.insertSession(
                date = date,
                minutes = minutes.toLong(),
                note = note
            )
        }

    override suspend fun updateSession(id: Long, date: String, minutes: Int, note: String?): Unit =
        withContext(Dispatchers.Default) {
            database.cardioSessionQueries.updateSession(
                date = date,
                minutes = minutes.toLong(),
                note = note,
                id = id
            )
        }

    override suspend fun deleteSession(id: Long): Unit = withContext(Dispatchers.Default) {
        database.cardioSessionQueries.deleteSession(id)
    }

    override fun sessionsForRange(startDate: String, endDate: String): Flow<List<CardioSession>> =
        database.cardioSessionQueries.getSessionsForRange(startDate, endDate)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }
}

private fun com.mettyoung.fitbro.data.db.CardioSession.toDomain() = CardioSession(
    id = id,
    date = date,
    minutes = minutes.toInt(),
    note = note
)
