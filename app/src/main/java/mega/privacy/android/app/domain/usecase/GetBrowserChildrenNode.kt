package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get children nodes of the browser parent handle
 */
fun interface GetBrowserChildrenNode {
    /**
     * Get children nodes of the browser parent handle
     *
     * @param parentHandle
     * @return Children nodes of the parent handle, null if cannot be retrieved
     */
    suspend operator fun invoke(parentHandle: Long): List<MegaNode>?
}