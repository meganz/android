package mega.privacy.android.app.domain.usecase

import nz.mega.sdk.MegaNode

/**
 * Get the rubbish bin node
 */
interface GetRubbishBinFolder {
    /**
     * Get the rubbish bin node
     *
     * @return A node corresponding to the rubbish bin node, null if cannot be retrieved
     */
    suspend operator fun invoke(): MegaNode?
}