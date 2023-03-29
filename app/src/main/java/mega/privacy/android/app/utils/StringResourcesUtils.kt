package mega.privacy.android.app.utils

import android.content.Context
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaError

/**
 * Deprecated string resource util class
 */
object StringResourcesUtils {


    private val context: Context by lazy { getInstance() }

    /**
     * Return the string value associated with a particular resource ID in the current app language.
     * If something fails return the original string in English.
     *
     * @param resId The resource identifier of the desired string.
     * @return The desired string.
     */
    @JvmStatic
    @Deprecated("Use {@link android.content.Context#getString(int)} instead.")
    fun getString(resId: Int) = context.getString(resId)

    /**
     * Return the string value associated with a particular resource ID in the current app language,
     * substituting the format arguments as defined in Formatter and String.format(String, Object...).
     * If something fails return the original string in English.
     *
     * @param resId      The resource identifier of the desired string.
     * @param formatArgs The format arguments that will be used for substitution.
     * @return The desired string.
     */
    @JvmStatic
    @Deprecated("Use {@link android.content.Context#getString(int, Object...)} instead.")
    fun getString(resId: Int, vararg formatArgs: Any?) =
        context.getString(resId, *formatArgs)

    /**
     * Returns the string necessary for grammatically correct pluralization of the given
     * resource ID for the given quantity in the current app language.
     * If something fails return the original string in English.
     *
     * @param resId    The resource identifier of the desired string
     * @param quantity The number used to get the correct string for the current language's plural rules.
     * @return The desired string.
     */
    @JvmStatic
    @Deprecated("Use {@link android.content.res.Resources#getQuantityString(int, int)} instead.")
    fun getQuantityString(resId: Int, quantity: Int) =
        context.resources.getQuantityString(resId, quantity)

    /**
     * Returns the string necessary for grammatically correct pluralization of the given resource ID
     * for the given quantity in the current app language and substituting the format arguments as
     * defined in Formatter and String.format(String, Object...).
     * If something fails return the original string in English.
     *
     * @param resId      The resource identifier of the desired string.
     * @param quantity   The number used to get the correct string for the current language's plural rules.
     * @param formatArgs The format arguments that will be used for substitution.
     * @return The desired string.
     */
    @JvmStatic
    @Deprecated("Use {@link android.content.res.Resources#getQuantityString(int, int, Object...)} instead.")
    fun getQuantityString(resId: Int, quantity: Int, vararg formatArgs: Any?) =
        context.resources.getQuantityString(resId, quantity, *formatArgs)

    /**
     * Gets the translated string of an error received in a request.
     *
     * @param error MegaError received in the request
     * @return The translated string
     */
    @JvmStatic
    @Deprecated(
        message = "MegaError should not be thrown in Presentation module, but instead mapped in Data module. For now however extension methods can be used",
        replaceWith = ReplaceWith("mega.privacy.android.app.presentation.extensions.MegaExceptionKt#getErrorStringId(MegaException)")
    )
    fun getTranslatedErrorString(error: MegaError): String {
        return getTranslatedErrorString(error.errorCode, error.errorString)
    }

    /**
     * Gets the translated string of a Mega error.
     *
     * @param errorCode MegaChat error code
     * @return The translated string
     */
    @JvmStatic
    @Deprecated(
        message = "MegaError should not be thrown in Presentation module, but instead mapped in Data module. For now however extension methods can be used",
        replaceWith = ReplaceWith("mega.privacy.android.app.presentation.extensions.MegaExceptionKt#getErrorStringId(MegaException)")
    )
    fun getTranslatedErrorString(errorCode: Int, errorString: String): String {
        return when (errorCode) {
            MegaError.API_OK -> getString(R.string.api_ok)
            MegaError.API_EINTERNAL -> getString(R.string.api_einternal)
            MegaError.API_EARGS -> getString(R.string.api_eargs)
            MegaError.API_EAGAIN -> getString(R.string.api_eagain)
            MegaError.API_ERATELIMIT -> getString(R.string.api_eratelimit)
            MegaError.API_EFAILED -> getString(R.string.api_efailed)
            MegaError.API_ETOOMANY -> if (errorString == "Terms of Service breached") {
                getString(R.string.api_etoomany_ec_download)
            } else if (errorString == "Too many concurrent connections or transfers") {
                getString(R.string.api_etoomay)
            } else {
                errorString
            }
            MegaError.API_ERANGE -> getString(R.string.api_erange)
            MegaError.API_EEXPIRED -> getString(R.string.api_eexpired)
            MegaError.API_ENOENT -> getString(R.string.api_enoent)
            MegaError.API_ECIRCULAR -> if (errorString == "Upload produces recursivity") {
                getString(R.string.api_ecircular_ec_upload)
            } else if (errorString == "Circular linkage detected") {
                getString(R.string.api_ecircular)
            } else {
                errorString
            }
            MegaError.API_EACCESS -> getString(R.string.api_eaccess)
            MegaError.API_EEXIST -> getString(R.string.api_eexist)
            MegaError.API_EINCOMPLETE -> getString(R.string.api_eincomplete)
            MegaError.API_EKEY -> getString(R.string.api_ekey)
            MegaError.API_ESID -> getString(R.string.api_esid)
            MegaError.API_EBLOCKED -> if (errorString == "Not accessible due to ToS/AUP violation" || errorString == "Blocked") {
                getString(R.string.error_download_takendown_file)
            } else {
                errorString
            }
            MegaError.API_EOVERQUOTA -> getString(R.string.api_eoverquota)
            MegaError.API_ETEMPUNAVAIL -> getString(R.string.api_etempunavail)
            MegaError.API_ETOOMANYCONNECTIONS -> getString(R.string.api_etoomanyconnections)
            MegaError.API_EWRITE -> getString(R.string.api_ewrite)
            MegaError.API_EREAD -> getString(R.string.api_eread)
            MegaError.API_EAPPKEY -> getString(R.string.api_eappkey)
            MegaError.API_ESSL -> getString(R.string.api_essl)
            MegaError.API_EGOINGOVERQUOTA -> getString(R.string.api_egoingoverquota)
            MegaError.API_EMFAREQUIRED -> getString(R.string.api_emfarequired)
            MegaError.API_EMASTERONLY -> getString(R.string.api_emasteronly)
            MegaError.API_EBUSINESSPASTDUE -> getString(R.string.api_ebusinesspastdue)
            MegaError.PAYMENT_ECARD -> getString(R.string.payment_ecard)
            MegaError.PAYMENT_EBILLING -> getString(R.string.payment_ebilling)
            MegaError.PAYMENT_EFRAUD -> getString(R.string.payment_efraud)
            MegaError.PAYMENT_ETOOMANY -> getString(R.string.payment_etoomay)
            MegaError.PAYMENT_EBALANCE -> getString(R.string.payment_ebalance)
            MegaError.PAYMENT_EGENERIC -> if (errorCode > 0) {
                getString(R.string.api_error_http)
            } else {
                getString(R.string.payment_egeneric_api_error_unknown)
            }
            else -> if (errorCode > 0) {
                getString(R.string.api_error_http)
            } else {
                getString(R.string.payment_egeneric_api_error_unknown)
            }
        }
    }

    /**
     * Gets the translated string of an error received in a request.
     *
     * @param error MegaChatError received in the request
     * @return The translated string
     */
    @JvmStatic
    @Deprecated(
        message = "MegaChatError should not be thrown in Presentation module, but instead mapped in Data module. For now however extension methods can be used",
        replaceWith = ReplaceWith("mega.privacy.android.app.presentation.extensions.MegaExceptionKt#getChatErrorStringId(MegaException)")
    )
    fun getTranslatedErrorString(error: MegaChatError): String {
        return getTranslatedChatErrorString(error.errorCode)
    }

    /**
     * Gets the translated string of a MegaChat error code.
     *
     * @param errorCode MegaChat error code
     * @return The translated string
     */
    @JvmStatic
    @Deprecated(
        message = "MegaChatError should not be thrown in Presentation module, but instead mapped in Data module. For now however extension methods can be used",
        replaceWith = ReplaceWith("mega.privacy.android.app.presentation.extensions.MegaExceptionKt#getChatErrorStringId(MegaException)")
    )
    fun getTranslatedChatErrorString(errorCode: Int): String {
        return when (errorCode) {
            MegaChatError.ERROR_OK -> getString(R.string.error_ok)
            MegaChatError.ERROR_ARGS -> getString(R.string.error_args)
            MegaChatError.ERROR_ACCESS -> getString(R.string.error_access)
            MegaChatError.ERROR_NOENT -> getString(R.string.error_noent)
            MegaChatError.ERROR_EXIST -> getString(R.string.error_exist)
            MegaChatError.ERROR_TOOMANY -> getString(R.string.error_toomany)
            MegaChatError.ERROR_UNKNOWN -> getString(R.string.error_unknown)
            else -> getString(R.string.error_unknown)
        }
    }
}