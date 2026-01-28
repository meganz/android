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
            /**
             * When selecting a non supported locale and then a supported locale in the language settings
             * causes a strange error in which the order of the two locales get randomly flipped.
             * This causes some resources to be loaded in the supported language and others in the
             * default language. I don't know what causes that to happen, but this code removes
             * unsupported locales from the configuration as a measure to prevent the strange behaviour.
             **/
            context?.resources?.configuration?.let { configuration ->
                val userLocales = configuration.locales
                val supportedLocaleList = getSupportedLocales(userLocales)
                val resolvedLocale = supportedLocaleList.firstOrNull() ?: Locale("en")

                Locale.setDefault(resolvedLocale)
                configuration.setLocales(LocaleList(resolvedLocale))
            }

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