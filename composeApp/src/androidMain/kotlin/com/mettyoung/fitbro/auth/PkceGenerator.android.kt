package com.mettyoung.fitbro.auth

import java.security.MessageDigest

actual fun sha256Bytes(input: ByteArray): ByteArray =
    MessageDigest.getInstance("SHA-256").digest(input)

actual fun currentTimeMs(): Long = System.currentTimeMillis()
