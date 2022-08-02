package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Authorize and return a MegaNode can be downloaded with any instance of MegaApi
 */
fun interface AuthorizeNode {
    /**
     * Authorize and return a MegaNode can be downloaded with any instance of MegaApi
     *
     * @param handle the handle of the node to authorize
     * @return a MegaNode that can be downloaded with any instance of MegaApi,
     *         null if can't be authorized
     */
    suspend operator fun invoke(handle: Long): MegaNode?
}