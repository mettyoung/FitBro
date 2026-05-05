package com.mettyoung.fitbro.data.cache

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

actual fun createCacheDataSource(): CacheDataSource =
    CacheDataSourceImpl(NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults))
