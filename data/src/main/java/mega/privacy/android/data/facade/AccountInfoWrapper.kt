package mega.privacy.android.data.facade

import nz.mega.sdk.MegaRequest

/**
 * Account info wrapper to create an interface to Account information functionality
 *
 */
interface AccountInfoWrapper {
    /**
     * Storage capacity used as a formatted string
     */
    val storageCapacityUsedAsFormattedString: String

    /**
     * Handle account detail
     * Legacy support, it will remove after refactor done
     *
     * @param request
     */
    suspend fun handleAccountDetail(request: MegaRequest)

    /**
     * Resets account info.
     */
    suspend fun resetAccountInfo()
}
