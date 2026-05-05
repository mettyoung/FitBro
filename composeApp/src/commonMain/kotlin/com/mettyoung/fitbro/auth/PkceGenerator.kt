package com.mettyoung.fitbro.auth

import kotlin.random.Random

data class PkceChallenge(
    val codeVerifier: String,
    val codeChallenge: String,
    val codeChallengeMethod: String = "S256"
)

expect fun sha256Bytes(input: ByteArray): ByteArray

expect fun currentTimeMs(): Long

fun generatePkce(): PkceChallenge {
    val verifier = buildString {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        repeat(64) { append(chars[Random.nextInt(chars.length)]) }
    }
    val hash = sha256Bytes(verifier.encodeToByteArray())
    val challenge = base64UrlEncode(hash)
    return PkceChallenge(codeVerifier = verifier, codeChallenge = challenge)
}

private fun base64UrlEncode(input: ByteArray): String {
    val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val sb = StringBuilder()
    var i = 0
    while (i < input.size) {
        val b0 = input[i].toInt() and 0xFF
        val b1 = if (i + 1 < input.size) input[i + 1].toInt() and 0xFF else 0
        val b2 = if (i + 2 < input.size) input[i + 2].toInt() and 0xFF else 0
        sb.append(table[(b0 shr 2) and 0x3F])
        sb.append(table[((b0 shl 4) or (b1 shr 4)) and 0x3F])
        sb.append(if (i + 1 < input.size) table[((b1 shl 2) or (b2 shr 6)) and 0x3F] else '=')
        sb.append(if (i + 2 < input.size) table[b2 and 0x3F] else '=')
        i += 3
    }
    return sb.toString()
        .replace('+', '-')
        .replace('/', '_')
        .trimEnd('=')
}
