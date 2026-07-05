package com.asmr.player.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.asmr.player.data.settings.SettingsKeys
import com.asmr.player.data.settings.settingsDataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val appLanguage: Flow<AppLanguage> = context.settingsDataStore.data.map { prefs ->
        AppLanguage.fromWireValue(prefs[SettingsKeys.APP_LANGUAGE])
    }

    suspend fun getAppLanguage(): AppLanguage =
        AppLanguage.fromWireValue(context.settingsDataStore.data.first()[SettingsKeys.APP_LANGUAGE])

    suspend fun setAppLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.APP_LANGUAGE] = language.wireValue
        }
        applyLanguage(language)
    }

    fun applyLanguage(language: AppLanguage = AppLanguage.System) {
        val localeList = when (language) {
            AppLanguage.System -> LocaleListCompat.getEmptyLocaleList()
            else -> {
                val locale = language.toLocale() ?: return
                LocaleListCompat.create(locale)
            }
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun wrapContext(base: Context, language: AppLanguage): Context {
        val locale = language.toLocale() ?: return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
