package com.example.data

import kotlinx.coroutines.flow.Flow

class FocusRepository(private val focusSessionDao: FocusSessionDao) {
    val allSessions: Flow<List<FocusSession>> = focusSessionDao.getAllSessions()

    suspend fun insertSession(session: FocusSession) {
        focusSessionDao.insertSession(session)
    }

    suspend fun clearAll() {
        focusSessionDao.clearAll()
    }
}
