package com.mettyoung.fitbro.data.sync

import kotlin.test.Test
import kotlin.test.assertEquals

class SyncSettingsNormalizationTest {

    @Test
    fun bareHostGetsHttpsScheme() {
        assertEquals("https://fitbro.example.workers.dev", normalizeSyncUrl("fitbro.example.workers.dev"))
    }

    @Test
    fun existingSchemeIsKept() {
        assertEquals("http://10.0.2.2:8787", normalizeSyncUrl("http://10.0.2.2:8787"))
        assertEquals("https://fitbro.example.workers.dev", normalizeSyncUrl("https://fitbro.example.workers.dev"))
    }

    @Test
    fun whitespaceAndTrailingSlashesAreStripped() {
        assertEquals("https://fitbro.example.workers.dev", normalizeSyncUrl("  https://fitbro.example.workers.dev//\n"))
    }

    @Test
    fun blankUrlStaysBlank() {
        assertEquals("", normalizeSyncUrl("   "))
    }

    @Test
    fun tokenIsTrimmed() {
        assertEquals("abc123", normalizeSyncToken(" abc123\n"))
    }
}
