package com.mettyoung.fitbro

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * Drives a suspend [block] to completion synchronously. Test doubles in this module
 * never actually suspend (no real dispatchers / async), so the coroutine finishes
 * before startCoroutine returns. Surfaces any failure via the continuation.
 */
fun runSync(block: suspend () -> Unit) {
    var failure: Throwable? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result ->
        failure = result.exceptionOrNull()
    })
    failure?.let { throw it }
}
