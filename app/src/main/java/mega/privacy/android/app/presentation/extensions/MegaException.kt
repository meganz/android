package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaError

/**
 * Gets the identifier of the translated error string from errorCode.
 *
 * @return The translated string id.
 */
internal fun MegaException.getErrorStringId(): Int =
    when (errorCode) {
        MegaError.API_OK -> R.string.api_ok
        MegaError.API_EINTERNAL -> R.string.api_einternal
        MegaError.API_EARGS -> R.string.api_eargs
        MegaError.API_EAGAIN -> R.string.api_eagain
        MegaError.API_ERATELIMIT -> R.string.api_eratelimit
        MegaError.API_EFAILED -> R.string.api_efailed
        MegaError.API_ETOOMANY ->
            when (errorString) {
                "Terms of Service breached" -> R.string.api_etoomany_ec_download
                "Too many concurrent connections or transfers" -> R.string.api_etoomay
                else -> R.string.payment_egeneric_api_error_unknown
            }

        MegaError.API_ERANGE -> R.string.api_erange
        MegaError.API_EEXPIRED -> R.string.api_eexpired
        MegaError.API_ENOENT -> R.string.api_enoent
        MegaError.API_ECIRCULAR ->
            when (errorString) {
                "Upload produces recursivity" -> R.string.api_ecircular_ec_upload
                "Circular linkage detected" -> R.string.api_ecircular
                else -> R.string.payment_egeneric_api_error_unknown
            }

        MegaError.API_EACCESS -> R.string.api_eaccess
        MegaError.API_EEXIST -> R.string.api_eexist
        MegaError.API_EINCOMPLETE -> R.string.api_eincomplete
        MegaError.API_EKEY -> R.string.api_ekey
        MegaError.API_ESID -> R.string.api_esid
        MegaError.API_EBLOCKED ->
            when (errorString) {
                "File removed as it violated our Terms of Service" -> R.string.error_download_takendown_file
                "Blocked" -> R.string.api_eblocked
                else -> R.string.payment_egeneric_api_error_unknown
            }

        MegaError.API_EOVERQUOTA -> R.string.api_eoverquota
        MegaError.API_ETEMPUNAVAIL -> R.string.api_etempunavail
        MegaError.API_ETOOMANYCONNECTIONS -> R.string.api_etoomanyconnections
        MegaError.API_EWRITE -> R.string.api_ewrite
        MegaError.API_EREAD -> R.string.api_eread
        MegaError.API_EAPPKEY -> R.string.api_eappkey
        MegaError.API_ESSL -> R.string.api_essl
        MegaError.API_EGOINGOVERQUOTA -> R.string.api_egoingoverquota
        MegaError.API_EMFAREQUIRED -> R.string.api_emfarequired
        MegaError.API_EMASTERONLY -> R.string.api_emasteronly
        MegaError.API_EBUSINESSPASTDUE -> R.string.api_ebusinesspastdue
        MegaError.PAYMENT_ECARD -> R.string.payment_ecard
        MegaError.PAYMENT_EBILLING -> R.string.payment_ebilling
        MegaError.PAYMENT_EFRAUD -> R.string.payment_efraud
        MegaError.PAYMENT_ETOOMANY -> R.string.payment_etoomay
        MegaError.PAYMENT_EBALANCE -> R.string.payment_ebalance
        MegaError.PAYMENT_EGENERIC -> R.string.payment_egeneric_api_error_unknown
        else ->
            if (errorCode > 0) {
                R.string.api_error_http
            } else {
                R.string.payment_egeneric_api_error_unknown
            }
    }

/**
 * Gets the identifier of the translated error string from chat errorCode.
 *
 * @return The translated string id.
 */
internal fun MegaException.getChatErrorStringId(): Int =
    when (errorCode) {
        MegaChatError.ERROR_OK -> R.string.error_ok
        MegaChatError.ERROR_ARGS -> R.string.error_args
        MegaChatError.ERROR_ACCESS -> R.string.error_access
        MegaChatError.ERROR_NOENT -> R.string.error_noent
        MegaChatError.ERROR_EXIST -> R.string.error_exist
        MegaChatError.ERROR_TOOMANY -> R.string.error_toomany
        MegaChatError.ERROR_UNKNOWN -> R.string.error_unknown
        else -> R.string.error_unknown
    }
