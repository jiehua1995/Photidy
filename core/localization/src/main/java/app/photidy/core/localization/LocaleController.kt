package app.photidy.core.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.photidy.core.model.AppLanguage

/**
 * App-level language switching backed by AppCompat's per-app locale API.
 * On Android 13+ this delegates to the system per-app language setting.
 */
object LocaleController {

    fun apply(language: AppLanguage) {
        val locales = when (language) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.CHINESE -> LocaleListCompat.forLanguageTags("zh-CN")
            AppLanguage.CHINESE_TRADITIONAL -> LocaleListCompat.forLanguageTags("zh-TW")
            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
            AppLanguage.GERMAN -> LocaleListCompat.forLanguageTags("de")
            AppLanguage.JAPANESE -> LocaleListCompat.forLanguageTags("ja")
            AppLanguage.KOREAN -> LocaleListCompat.forLanguageTags("ko")
            AppLanguage.SPANISH -> LocaleListCompat.forLanguageTags("es")
            AppLanguage.FRENCH -> LocaleListCompat.forLanguageTags("fr")
            AppLanguage.ITALIAN -> LocaleListCompat.forLanguageTags("it")
        }
        if (AppCompatDelegate.getApplicationLocales() != locales) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }
}
