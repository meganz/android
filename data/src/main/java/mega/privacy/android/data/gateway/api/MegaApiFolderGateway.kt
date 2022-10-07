package mega.privacy.android.data.gateway.api

import nz.mega.sdk.MegaNode

/**
 * Mega api folder gateway
 *
 * The gateway interface to the Mega Api folder functionality
 */
interface MegaApiFolderGateway {
    /**
     * Authentication token that can be used to identify the user account.
     */
    var accountAuth: String

    /**
     * Authorize and return a MegaNode can be downloaded with any instance of MegaApi
     *
     * @param handle the handle of the node to authorize
     * @return a MegaNode that can be downloaded with any instance of MegaApi,
     *         null if can't be authorized
     */
    suspend fun authorizeNode(handle: Long): MegaNode?

}