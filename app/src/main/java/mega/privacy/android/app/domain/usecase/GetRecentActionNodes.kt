package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.fragments.homepage.NodeItem
import nz.mega.sdk.MegaRecentActionBucket

/**
 * Get the nodes from recent action bucket
 */
fun interface GetRecentActionNodes {

    /**
     * Get the nodes from recent action bucket
     *
     * @param bucket the bucket containing the nodes
     * @return a list of node item contained in the bucket
     */
    suspend operator fun invoke(bucket: MegaRecentActionBucket): List<NodeItem>
}