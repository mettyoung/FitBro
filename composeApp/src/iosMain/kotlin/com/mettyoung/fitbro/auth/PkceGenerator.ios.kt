package com.mettyoung.fitbro.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.posix.gettimeofday
import platform.posix.timeval

@OptIn(ExperimentalForeignApi::class)
actual fun sha256Bytes(input: ByteArray): ByteArray {
    val inputU = input.toUByteArray()
    val result = UByteArray(CC_SHA256_DIGEST_LENGTH.toInt())
    inputU.usePinned { pinnedInput ->
        result.usePinned { pinnedResult ->
            CC_SHA256(pinnedInput.addressOf(0), input.size.toUInt(), pinnedResult.addressOf(0))
        }
    }
    return result.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
actual fun currentTimeMs(): Long = memScoped {
    val tv = alloc<timeval>()
    gettimeofday(tv.ptr, null)
    tv.tv_sec * 1000L + tv.tv_usec / 1000L
}
