package mega.privacy.android.app.usecase.exception

import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatError.*
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.*

/**
 * Class to manage MegaApi and MegaChatApi errors.
 *
 * @property errorCode      Error code.
 * @property errorMessage   Error message string.
 * @property isChatError    Flag to check if error is coming from megaChatApi.
 */
sealed class MegaException constructor(
    val errorCode: Int,
    val errorMessage: String,
    val isChatError: Boolean = false
) : RuntimeException("$errorCode: $errorMessage") {

    /**
     * Gets the translated string of the error.
     *
     * @return  Translated error string
     */
    fun getTranslatedErrorString(): String =
        if (isChatError) {
            StringResourcesUtils.getTranslatedChatErrorString(errorCode)
        } else {
            StringResourcesUtils.getTranslatedErrorString(errorCode, errorMessage)
        }
}

// Generic Errors
class SuccessMegaException(errorMessage: String, isChatError: Boolean): MegaException(API_OK, errorMessage, isChatError)
class HttpMegaException(errorCode: Int, errorMessage: String, isChatError: Boolean): MegaException(errorCode, errorMessage, isChatError)
class UnknownMegaException(errorCode: Int, errorMessage: String, isChatError: Boolean): MegaException(errorCode, errorMessage, isChatError)

// MegaApi Errors
class InternalMegaException(errorMessage: String): MegaException(API_EINTERNAL, errorMessage, false)
class BadArgumentMegaException(errorMessage: String): MegaException(API_EARGS, errorMessage, false)
class RequestFailedMegaException(errorMessage: String): MegaException(API_EAGAIN, errorMessage, false)
class RateLimitMegaException(errorMessage: String): MegaException(API_ERATELIMIT, errorMessage, false)
class PermanentlyFailMegaException(errorMessage: String): MegaException(API_EFAILED, errorMessage, false)
class TooManyRequestsMegaException(errorMessage: String): MegaException(API_ETOOMANY, errorMessage, false)
class ResourceOutOfRangeMegaException(errorMessage: String): MegaException(API_ERANGE, errorMessage, false)
class ResourceExpiredMegaException(errorMessage: String): MegaException(API_EEXPIRED, errorMessage, false)
class ResourceDoesNotExistMegaException(errorMessage: String): MegaException(API_ENOENT, errorMessage, false)
class CircularLinkageMegaException(errorMessage: String): MegaException(API_ECIRCULAR, errorMessage, false)
class AccessDeniedMegaException(errorMessage: String): MegaException(API_EACCESS, errorMessage, false)
class ResourceAlreadyExistsMegaException(errorMessage: String): MegaException(API_EEXIST, errorMessage, false)
class RequestIncompleteMegaException(errorMessage: String): MegaException(API_EINCOMPLETE, errorMessage, false)
class CryptographicMegaException(errorMessage: String): MegaException(API_EKEY, errorMessage, false)
class BadSessionIdMegaException(errorMessage: String): MegaException(API_ESID, errorMessage, false)
class ResourceAdministrativelyBlockedMegaException(errorMessage: String): MegaException(API_EBLOCKED, errorMessage, false)
class QuotaExceededMegaException(errorMessage: String): MegaException(API_EOVERQUOTA, errorMessage, false)
class ResourceTemporarilyNotAvailableMegaException(errorMessage: String): MegaException(API_ETEMPUNAVAIL, errorMessage, false)
class TooManyConnectionsMegaException(errorMessage: String): MegaException(API_ETOOMANYCONNECTIONS, errorMessage, false)
class FileCouldNotBeWrittenMegaException(errorMessage: String): MegaException(API_EWRITE, errorMessage, false)
class FileCouldNotBeReadMegaException(errorMessage: String): MegaException(API_EREAD, errorMessage, false)
class InvalidApplicationKeyMegaException(errorMessage: String): MegaException(API_EAPPKEY, errorMessage, false)
class InvalidSslKeyMegaException(errorMessage: String): MegaException(API_ESSL, errorMessage, false)
class NotEnoughQuotaMegaException(errorMessage: String): MegaException(API_EGOINGOVERQUOTA, errorMessage, false)
class MultiFactorAuthenticationRequiredMegaException(errorMessage: String): MegaException(API_EMFAREQUIRED, errorMessage, false)
class AccessDeniedForSubUsersMegaException(errorMessage: String): MegaException(API_EMASTERONLY, errorMessage, false)
class BusinessAccountExpiredMegaException(errorMessage: String): MegaException(API_EBUSINESSPASTDUE, errorMessage, false)
class OverDiskQuotaPaywallMegaException(errorMessage: String): MegaException(API_EPAYWALL, errorMessage, false)

// MegaApi Payment Errors
class CreditCardRejectedPaymentMegaException(errorMessage: String): MegaException(PAYMENT_ECARD, errorMessage, false)
class BillingPaymentMegaException(errorMessage: String): MegaException(PAYMENT_EBILLING, errorMessage, false)
class FraudProtectionRejectedPaymentMegaException(errorMessage: String): MegaException(PAYMENT_EFRAUD, errorMessage, false)
class TooManyRequestsPaymentPaymentMegaException(errorMessage: String): MegaException(PAYMENT_ETOOMANY, errorMessage, false)
class BalancePaymentMegaException(errorMessage: String): MegaException(PAYMENT_EBALANCE, errorMessage, false)
class GenericPaymentMegaException(errorMessage: String): MegaException(PAYMENT_EGENERIC, errorMessage, false)

// MegaChatApi Errors
class UnknownMegaChatException(errorMessage: String): MegaException(ERROR_UNKNOWN, errorMessage, true)
class BadArgumentMegaChatException(errorMessage: String): MegaException(ERROR_ARGS, errorMessage, true)
class TooManyUsesMegaChatException(errorMessage: String): MegaException(ERROR_TOOMANY, errorMessage, true)
class ResourceDoesNotExistMegaChatException(errorMessage: String): MegaException(ERROR_NOENT, errorMessage, true)
class AccessDeniedMegaChatException(errorMessage: String): MegaException(ERROR_ACCESS, errorMessage, true)
class ResourceAlreadyExistsMegaChatException(errorMessage: String): MegaException(ERROR_EXIST, errorMessage, true)

/**
 * Converts MegaError to MegaException
 *
 * @return MegaException
 */
fun MegaError.toMegaException(): MegaException =
    when (errorCode) {
        API_OK -> SuccessMegaException(errorString, false)
        API_EINTERNAL -> InternalMegaException(errorString)
        API_EARGS -> BadArgumentMegaException(errorString)
        API_EAGAIN -> RequestFailedMegaException(errorString)
        API_ERATELIMIT -> RateLimitMegaException(errorString)
        API_EFAILED -> PermanentlyFailMegaException(errorString)
        API_ETOOMANY -> TooManyRequestsMegaException(errorString)
        API_ERANGE -> ResourceOutOfRangeMegaException(errorString)
        API_EEXPIRED -> ResourceExpiredMegaException(errorString)
        API_ENOENT -> ResourceDoesNotExistMegaException(errorString)
        API_ECIRCULAR -> CircularLinkageMegaException(errorString)
        API_EACCESS -> AccessDeniedMegaException(errorString)
        API_EEXIST -> ResourceAlreadyExistsMegaException(errorString)
        API_EINCOMPLETE -> RequestIncompleteMegaException(errorString)
        API_EKEY -> CryptographicMegaException(errorString)
        API_ESID -> BadSessionIdMegaException(errorString)
        API_EBLOCKED -> ResourceAdministrativelyBlockedMegaException(errorString)
        API_EOVERQUOTA -> QuotaExceededMegaException(errorString)
        API_ETEMPUNAVAIL -> ResourceTemporarilyNotAvailableMegaException(errorString)
        API_ETOOMANYCONNECTIONS -> TooManyConnectionsMegaException(errorString)
        API_EWRITE -> FileCouldNotBeWrittenMegaException(errorString)
        API_EREAD -> FileCouldNotBeReadMegaException(errorString)
        API_EAPPKEY -> InvalidApplicationKeyMegaException(errorString)
        API_ESSL -> InvalidSslKeyMegaException(errorString)
        API_EGOINGOVERQUOTA -> NotEnoughQuotaMegaException(errorString)
        API_EMFAREQUIRED -> MultiFactorAuthenticationRequiredMegaException(errorString)
        API_EMASTERONLY -> AccessDeniedForSubUsersMegaException(errorString)
        API_EBUSINESSPASTDUE -> BusinessAccountExpiredMegaException(errorString)
        API_EPAYWALL -> OverDiskQuotaPaywallMegaException(errorString)
        PAYMENT_ECARD -> CreditCardRejectedPaymentMegaException(errorString)
        PAYMENT_EBILLING -> BillingPaymentMegaException(errorString)
        PAYMENT_EFRAUD -> FraudProtectionRejectedPaymentMegaException(errorString)
        PAYMENT_ETOOMANY -> TooManyRequestsPaymentPaymentMegaException(errorString)
        PAYMENT_EBALANCE -> BalancePaymentMegaException(errorString)
        PAYMENT_EGENERIC -> GenericPaymentMegaException(errorString)
        else -> {
            if (errorCode > 0) {
                HttpMegaException(errorCode, errorString, false)
            } else {
                UnknownMegaException(errorCode, errorString, false)
            }
        }
    }

/**
 * Converts MegaChatError to MegaException
 *
 * @return MegaException
 */
fun MegaChatError.toMegaException(): MegaException =
    when (errorCode) {
        ERROR_OK -> SuccessMegaException(errorString, true)
        ERROR_UNKNOWN -> UnknownMegaChatException(errorString)
        ERROR_ARGS -> BadArgumentMegaChatException(errorString)
        ERROR_TOOMANY -> TooManyUsesMegaChatException(errorString)
        ERROR_NOENT -> ResourceDoesNotExistMegaChatException(errorString)
        ERROR_ACCESS -> AccessDeniedMegaChatException(errorString)
        ERROR_EXIST -> ResourceAlreadyExistsMegaChatException(errorString)
        else -> {
            if (errorCode > 0) {
                HttpMegaException(errorCode, errorString, true)
            } else {
                UnknownMegaException(errorCode, errorString, true)
            }
        }
    }