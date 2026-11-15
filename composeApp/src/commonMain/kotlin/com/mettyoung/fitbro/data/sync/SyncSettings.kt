package com.mettyoung.fitbro.data.sync

/**
 * Canonical form of a user-typed Worker URL: trimmed, no trailing slash, and `https://` when no
 * scheme was given (a bare host would otherwise resolve as a path on localhost). Blank stays blank.
 */
fun normalizeSyncUrl(raw: String): String {
    val url = raw.trim().trimEnd('/')
    if (url.isEmpty()) return url
    return if (url.contains("://")) url else "https://$url"
}

/** Pasted tokens often carry a trailing newline, which is an illegal HTTP header character. */
fun normalizeSyncToken(raw: String): String = raw.trim()

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
