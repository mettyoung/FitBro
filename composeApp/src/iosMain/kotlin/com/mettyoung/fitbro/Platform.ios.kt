package com.mettyoung.fitbro

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val healthNutritionSourceName: String = "Apple Health"
}

actual fun getPlatform(): Platform = IOSPlatform()
