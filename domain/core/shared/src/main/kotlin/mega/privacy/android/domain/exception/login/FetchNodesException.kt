package mega.privacy.android.domain.exception.login

import mega.privacy.android.domain.exception.MegaException

/**
 * Login exception.
 */
sealed class FetchNodesException : RuntimeException("FetchNodesException")

/**
 * Access error.
 *
 * @property megaException Exception required for getting the translated error string.
 */
class FetchNodesErrorAccess(val megaException: MegaException) : FetchNodesException()

/**
 * Blocked account.
 */
class FetchNodesBlockedAccount : FetchNodesException()

/**
 * Other error.
 *
 * @property megaException Exception required for getting the translated error string.
 */
class FetchNodesUnknownStatus(val megaException: MegaException) : FetchNodesException()
