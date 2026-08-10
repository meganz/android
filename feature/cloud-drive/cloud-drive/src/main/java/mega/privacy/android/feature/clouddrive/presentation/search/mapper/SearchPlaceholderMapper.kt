package mega.privacy.android.feature.clouddrive.presentation.search.mapper

import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * Mapper to convert search parameters to search placeholder text
 */
class SearchPlaceholderMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param nodeSourceType the source type of the search
     * @param nodeName the node name (optional)
     * @return LocalizedText representing the search placeholder
     */
    operator fun invoke(
        nodeSourceType: NodeSourceType,
        nodeName: String?,
    ) = if (!nodeName.isNullOrEmpty()) {
        LocalizedText.StringRes(
            resId = sharedR.string.search_placeholder_folder,
            formatArgs = listOf(nodeName)
        )
    } else {
        LocalizedText.StringRes(getSectionNameRes(nodeSourceType))
    }

    private fun getSectionNameRes(nodeSourceType: NodeSourceType) = when (nodeSourceType) {
        NodeSourceType.CLOUD_DRIVE -> sharedR.string.search_placeholder_cloud_drive
        NodeSourceType.RUBBISH_BIN -> sharedR.string.search_placeholder_rubbish_bin
        NodeSourceType.HOME -> sharedR.string.search_placeholder_cloud_drive
        NodeSourceType.INCOMING_SHARES -> sharedR.string.search_placeholder_incoming_shares
        NodeSourceType.OUTGOING_SHARES -> sharedR.string.search_placeholder_outgoing_shares
        NodeSourceType.LINKS -> sharedR.string.search_placeholder_links
        NodeSourceType.FAVOURITES -> sharedR.string.search_placeholder_favourites
        NodeSourceType.DOCUMENTS -> sharedR.string.search_placeholder_documents
        NodeSourceType.AUDIO -> sharedR.string.search_placeholder_audios
        NodeSourceType.VIDEOS -> sharedR.string.search_placeholder_videos
        NodeSourceType.BACKUPS -> sharedR.string.search_placeholder_backups
        else -> sharedR.string.search_bar_placeholder_text
    }
}

