package mega.privacy.mobile.home.presentation.recents.bucket.model

import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * UI state for RecentsBucketScreen
 *
 * @param nodeUiItems list of node UI items in the bucket
 * @param isLoading true if loading
 * @param fileCount number of files in the bucket
 * @param timestamp timestamp of the bucket
 * @param parentFolderName localized name of the parent folder
 */
data class RecentsBucketUiState(
    val nodeUiItems: List<NodeUiItem<TypedNode>> = emptyList(),
    val isMediaBucket: Boolean = false,
    val isLoading: Boolean = true,
    val fileCount: Int = 0,
    val timestamp: Long = 0L,
    val parentFolderName: LocalizedText = LocalizedText.Literal(""),
    val nodeSourceType: NodeSourceType
)