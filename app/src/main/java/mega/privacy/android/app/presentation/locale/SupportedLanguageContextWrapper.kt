package mega.privacy.android.app.presentation.locale

import android.content.Context
import android.content.ContextWrapper
import android.os.LocaleList
import java.util.Locale

/**
 * Supported language context wrapper
 *
 * @constructor
 *
 * @param base
 */
class SupportedLanguageContextWrapper private constructor(base: Context?) : ContextWrapper(base) {

    companion object {
        private val supportedLanguages = setOf(
            "en",
            "ar",
            "de",
            "es",
            "fr",
            "in",
            "id",
            "it",
            "ja",
            "ko",
            "nl",
            "pl",
            "pt",
            "ro",
            "ru",
            "th",
            "vi",
            "zh",
            "tr"
        )

        /**
         * Static constructor for SupportedLanguageContextWrapper
         *
         * @param context base context to wrap
         * @return wrapped context
         */
        fun wrap(context: Context?): SupportedLanguageContextWrapper {
            if (context == null) {
                return SupportedLanguageContextWrapper(null)
            }
            /**
             * Fix a bug where having multiple locales in system settings causes the order of locales
             * to get randomly flipped, resulting in mixed language resources.
             */
            val configuration = context.resources.configuration
            val userLocales = configuration.locales
            val supportedLocaleList = getSupportedLocales(userLocales)
            val resolvedLocale = supportedLocaleList.firstOrNull() ?: Locale("en")

            Locale.setDefault(resolvedLocale)

            // Create a new configuration with the resolved locale
            val newConfig = android.content.res.Configuration(configuration)
            newConfig.setLocales(LocaleList(resolvedLocale))

            val newContext = context.createConfigurationContext(newConfig)
            return SupportedLanguageContextWrapper(newContext)
        }

        private fun getSupportedLocales(
            locales: LocaleList,
        ) = (0 until locales.size())
            .mapNotNull { i ->
                locales[i].takeIf { locale -> supportedLanguages.contains(locale.language) }
            }
            .toTypedArray()
    }
}