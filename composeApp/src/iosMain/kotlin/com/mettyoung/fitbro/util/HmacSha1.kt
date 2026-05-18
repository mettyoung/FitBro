package com.mettyoung.fitbro.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.CC_SHA1_DIGEST_LENGTH
import platform.CoreCrypto.kCCHmacAlgSHA1

@OptIn(ExperimentalForeignApi::class)
actual fun hmacSha1(key: ByteArray, data: ByteArray): ByteArray {
    val result = UByteArray(CC_SHA1_DIGEST_LENGTH)
    result.usePinned { pinnedResult ->
        key.toUByteArray().usePinned { pinnedKey ->
            data.toUByteArray().usePinned { pinnedData ->
                CCHmac(
                    kCCHmacAlgSHA1,
                    pinnedKey.addressOf(0),
                    key.size.toULong(),
                    pinnedData.addressOf(0),
                    data.size.toULong(),
                    pinnedResult.addressOf(0)
                )
            }
        }
    }
    return result.toByteArray()
}
