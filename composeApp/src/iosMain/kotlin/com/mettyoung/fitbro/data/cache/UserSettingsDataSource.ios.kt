package com.mettyoung.fitbro.data.cache

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

actual fun createUserSettingsDataSource(): UserSettingsDataSource =
    UserSettingsDataSourceImpl(NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults))
