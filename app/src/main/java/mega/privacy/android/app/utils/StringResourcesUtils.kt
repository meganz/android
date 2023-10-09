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
            MegaError.API_OK -> context.getString(R.string.api_ok)
            MegaError.API_EINTERNAL -> context.getString(R.string.api_einternal)
            MegaError.API_EARGS -> context.getString(R.string.api_eargs)
            MegaError.API_EAGAIN -> context.getString(R.string.api_eagain)
            MegaError.API_ERATELIMIT -> context.getString(R.string.api_eratelimit)
            MegaError.API_EFAILED -> context.getString(R.string.api_efailed)
            MegaError.API_ETOOMANY -> when (errorString) {
                "Terms of Service breached" -> context.getString(R.string.api_etoomany_ec_download)
                "Too many concurrent connections or transfers" -> context.getString(R.string.api_etoomay)
                else -> errorString
            }
            MegaError.API_ERANGE -> context.getString(R.string.api_erange)
            MegaError.API_EEXPIRED -> context.getString(R.string.api_eexpired)
            MegaError.API_ENOENT -> context.getString(R.string.api_enoent)
            MegaError.API_ECIRCULAR -> when (errorString) {
                "Upload produces recursivity" -> context.getString(R.string.api_ecircular_ec_upload)
                "Circular linkage detected" -> context.getString(R.string.api_ecircular)
                else -> errorString
            }
            MegaError.API_EACCESS -> context.getString(R.string.api_eaccess)
            MegaError.API_EEXIST -> context.getString(R.string.api_eexist)
            MegaError.API_EINCOMPLETE -> context.getString(R.string.api_eincomplete)
            MegaError.API_EKEY -> context.getString(R.string.api_ekey)
            MegaError.API_ESID -> context.getString(R.string.api_esid)
            MegaError.API_EBLOCKED -> when (errorString) {
                "File removed as it violated our Terms of Service" -> context.getString(R.string.error_download_takendown_file)
                "Blocked" -> context.getString(R.string.api_eblocked)
                else -> errorString
            }
            MegaError.API_EOVERQUOTA -> context.getString(R.string.api_eoverquota)
            MegaError.API_ETEMPUNAVAIL -> context.getString(R.string.api_etempunavail)
            MegaError.API_ETOOMANYCONNECTIONS -> context.getString(R.string.api_etoomanyconnections)
            MegaError.API_EWRITE -> context.getString(R.string.api_ewrite)
            MegaError.API_EREAD -> context.getString(R.string.api_eread)
            MegaError.API_EAPPKEY -> context.getString(R.string.api_eappkey)
            MegaError.API_ESSL -> context.getString(R.string.api_essl)
            MegaError.API_EGOINGOVERQUOTA -> context.getString(R.string.api_egoingoverquota)
            MegaError.API_EMFAREQUIRED -> context.getString(R.string.api_emfarequired)
            MegaError.API_EMASTERONLY -> context.getString(R.string.api_emasteronly)
            MegaError.API_EBUSINESSPASTDUE -> context.getString(R.string.api_ebusinesspastdue)
            MegaError.PAYMENT_ECARD -> context.getString(R.string.payment_ecard)
            MegaError.PAYMENT_EBILLING -> context.getString(R.string.payment_ebilling)
            MegaError.PAYMENT_EFRAUD -> context.getString(R.string.payment_efraud)
            MegaError.PAYMENT_ETOOMANY -> context.getString(R.string.payment_etoomay)
            MegaError.PAYMENT_EBALANCE -> context.getString(R.string.payment_ebalance)
            MegaError.PAYMENT_EGENERIC -> if (errorCode > 0) {
                context.getString(R.string.api_error_http)
            } else {
                context.getString(R.string.payment_egeneric_api_error_unknown)
            }
            else -> if (errorCode > 0) {
                context.getString(R.string.api_error_http)
            } else {
                context.getString(R.string.payment_egeneric_api_error_unknown)
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
            MegaChatError.ERROR_OK -> context.getString(R.string.error_ok)
            MegaChatError.ERROR_ARGS -> context.getString(R.string.error_args)
            MegaChatError.ERROR_ACCESS -> context.getString(R.string.error_access)
            MegaChatError.ERROR_NOENT -> context.getString(R.string.error_noent)
            MegaChatError.ERROR_EXIST -> context.getString(R.string.error_exist)
            MegaChatError.ERROR_TOOMANY -> context.getString(R.string.error_toomany)
            MegaChatError.ERROR_UNKNOWN -> context.getString(R.string.error_unknown)
            else -> context.getString(R.string.error_unknown)
        }
    }
}