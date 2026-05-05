package com.mettyoung.fitbro.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class OAuthCallback(val code: String, val state: String)

object OAuthCallbackHandler {
    private val _callbacks = MutableSharedFlow<OAuthCallback>(extraBufferCapacity = 1)
    val callbacks = _callbacks.asSharedFlow()

    fun deliver(code: String, state: String) {
        _callbacks.tryEmit(OAuthCallback(code, state))
    }
}
