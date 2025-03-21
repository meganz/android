package mega.privacy.android.domain.exception.account

import mega.privacy.android.domain.exception.MegaException

/**
 * Account existed exception
 *
 */
class AccountExistedException(
    errorCode: Int,
    errorString: String? = null,
) : MegaException(errorCode, errorString)