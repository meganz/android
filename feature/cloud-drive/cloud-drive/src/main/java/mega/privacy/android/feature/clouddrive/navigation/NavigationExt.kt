package mega.privacy.android.feature.clouddrive.navigation

import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.destination.LegacySearchNavKey
import mega.privacy.android.navigation.destination.SearchNavKey

/**
 * Temporary extension function to get the appropriate [NavKey] for search based on the feature flag.
 * @param folderId The ID of the folder to search within.
 * @param isRevampEnabled Flag indicating whether the revamp feature is enabled.
 * @return The appropriate [NavKey] for search.
 */
fun NodeSourceType.getSearchNavKey(
    folderId: Long,
    isRevampEnabled: Boolean,
) = if (isRevampEnabled) {
    SearchNavKey(
        parentHandle = folderId,
        nodeSourceType = this
    )
} else {
    LegacySearchNavKey(
        parentHandle = folderId,
        nodeSourceType = this
    )
}