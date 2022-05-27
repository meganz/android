package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get the rubbish bin node
 */
interface GetRubbishBinNode {
    /**
     * Get the rubbish bin node
     *
     * @return A node corresponding to the rubbish bin node, null if cannot be retrieved
     */
    operator fun invoke(): MegaNode?
}