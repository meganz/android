package mega.privacy.android.app.presentation.locale

import android.content.Context
import android.content.ContextWrapper
import android.os.LocaleList

/**
 * Supported language context wrapper
 *
 * @constructor
 *
 * @param base
 */
class SupportedLanguageContextWrapper(base: Context?) : ContextWrapper(base) {
    private val supportedLanguages = listOf(
        "en",
        "ar",
        "de",
        "es",
        "fr",
        "id",
        "jt",
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

    override fun attachBaseContext(newBase: Context?) {
        /**
         * When selecting a non supported locale and then a supported locale in the language settings
         * causes a strange error in which the order of the two locales get randomly flipped.
         * This causes some resources to be loaded in the supported language and others in the
         * default language. I don't know what causes that to happen, but this code removes
         * unsupported locales from the configuration as a measure to prevent the strange behaviour.
         **/


        newBase?.resources?.configuration?.let { configuration ->
            val locales = configuration.locales
            LocaleList(*getSupportedLocales(locales, supportedLanguages))
                .takeUnless { it.isEmpty }
                ?.let {
                    configuration.setLocales(it)
                }
        }
        super.attachBaseContext(newBase)
    }

    private fun getSupportedLocales(
        locales: LocaleList,
        supportedLanguages: List<String>,
    ) = (0 until locales.size()).mapNotNull { i ->
        locales[i].takeIf { locale -> supportedLanguages.contains(locale.language) }
    }.toTypedArray()
}