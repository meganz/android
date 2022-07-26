package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get a list with all public links
 */
fun interface GetPublicLinks {
    /**
     * Get a list with all public links
     *
     * @param parentHandle
     ** @return Children nodes of the parent handle
     *          Root list of public nodes if parent handle is invalid
     *          null if parent node cannot be retrieved
     */
    suspend operator fun invoke(parentHandle: Long): List<MegaNode>?
}