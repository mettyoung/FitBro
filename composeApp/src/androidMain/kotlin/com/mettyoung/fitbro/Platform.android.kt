package com.mettyoung.fitbro

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val healthNutritionSourceName: String = "Health Connect"
}

actual fun getPlatform(): Platform = AndroidPlatform()
