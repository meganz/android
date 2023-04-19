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

        private val supportedLanguages = listOf(
            "en",
            "ar",
            "de",
            "es",
            "fr",
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
                val locales = configuration.locales
                LocaleList(*getSupportedLocales(locales))
                    .takeUnless { it.isEmpty }
                    ?.let {
                        Locale.setDefault(it.get(0))
                        configuration.setLocales(it)
                    } ?: run {
                    // If only unsupported locale is set in phone setting and app-specific setting
                    // set default locale to English.
                    val defaultLocale = Locale("en")
                    Locale.setDefault(defaultLocale)
                    configuration.setLocales(LocaleList(defaultLocale))
                }
            }
            return SupportedLanguageContextWrapper(context)
        }

        private fun getSupportedLocales(
            locales: LocaleList,
        ) = (0 until locales.size()).mapNotNull { i ->
            locales[i].takeIf { locale -> supportedLanguages.contains(locale.language) }
        }.toTypedArray()
    }

}