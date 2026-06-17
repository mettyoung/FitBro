package com.mettyoung.fitbro.data.repository

import com.mettyoung.fitbro.data.model.CardioSession
import kotlinx.coroutines.flow.Flow

interface CardioRepository {
    suspend fun logSession(date: String, minutes: Int, note: String?)
    suspend fun updateSession(id: Long, date: String, minutes: Int, note: String?)
    suspend fun deleteSession(id: Long)
    fun sessionsForRange(startDate: String, endDate: String): Flow<List<CardioSession>>
}
