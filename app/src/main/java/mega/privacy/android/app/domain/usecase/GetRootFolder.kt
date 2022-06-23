package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get the root node
 */
fun interface GetRootFolder {
    /**
     * Get the root node
     *
     * @return A node corresponding to the root node, null if cannot be retrieved
     */
    suspend operator fun invoke(): MegaNode?
}