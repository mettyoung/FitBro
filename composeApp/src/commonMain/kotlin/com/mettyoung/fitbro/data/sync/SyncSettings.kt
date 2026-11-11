package com.mettyoung.fitbro.data.sync

interface SyncSettings {
    fun getSyncUrl(): String
    fun setSyncUrl(url: String)

    fun getSyncToken(): String
    fun setSyncToken(token: String)

    /** Last cloud server_seq applied locally; null until the first successful sync. */
    fun getSyncCursor(): Long?
    fun setSyncCursor(cursor: Long?)

    fun getLastSyncedAt(): Long?
    fun setLastSyncedAt(epochMs: Long)
}
