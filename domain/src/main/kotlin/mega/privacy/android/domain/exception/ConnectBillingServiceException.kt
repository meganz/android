package mega.privacy.android.domain.exception

/**
 * Connect billing service exception
 *
 * @param code
 */
class ConnectBillingServiceException(code: Int, errorString: String) :
    MegaException(code, errorString)