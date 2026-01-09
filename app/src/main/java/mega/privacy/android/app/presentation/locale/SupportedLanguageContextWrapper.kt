package mega.privacy.android.app.presentation.locale

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
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
             * On Android 13+ (API 33+), per-app language preferences are handled by the system.
             * No manual locale filtering is needed - just wrap the context without modification.
             *
             * On Android 12 and below, filter unsupported locales to prevent a bug where
             * selecting a non-supported locale and then a supported locale causes the order of locales
             * to get randomly flipped, resulting in mixed language resources.
             */
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // Android 12 and below: Apply locale filtering
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
            // Android 13+: Let the system handle locales via per-app language preferences
            return SupportedLanguageContextWrapper(context)
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