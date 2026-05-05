package com.mettyoung.fitbro

import android.app.Application
import android.content.Context

object AndroidAppContext {
    private lateinit var _context: Context

    fun init(context: Context) {
        _context = context.applicationContext
    }

    val context: Context get() = _context
}

class FitBroApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContext.init(this)
    }
}
