package mega.privacy.android.domain.usecase.sortorder

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrderUseCase
import mega.privacy.android.domain.usecase.GetOfflineSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import javax.inject.Inject

/**
 * Use case to get the sort order based on the node source type.
 */
class GetSortOrderByNodeSourceTypeUseCase @Inject constructor(
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getLinksSortOrderUseCase: GetLinksSortOrderUseCase,
    private val getOthersSortOrder: GetOthersSortOrder,
    private val getOfflineSortOrder: GetOfflineSortOrder,
) {

    /**
     * Gets the sort order based on the node source.
     *
     * @param nodeSource The source of the node.
     * @return The sort order for the specified node source.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend operator fun invoke(
        nodeSource: NodeSourceType,
        isSingleActivityEnabled: Boolean = true,
    ): SortOrder =
        when (nodeSource) {
            NodeSourceType.LINKS -> getLinksSortOrderUseCase()
            NodeSourceType.INCOMING_SHARES -> getOthersSortOrder()
            NodeSourceType.CLOUD_DRIVE,
            NodeSourceType.HOME,
            NodeSourceType.RUBBISH_BIN,
            NodeSourceType.BACKUPS,
            NodeSourceType.DOCUMENTS,
            NodeSourceType.AUDIO,
            NodeSourceType.FAVOURITES,
            NodeSourceType.OUTGOING_SHARES,
            NodeSourceType.OTHER,
            NodeSourceType.VIDEOS,
            NodeSourceType.SEARCH,
            NodeSourceType.VIDEO_PLAYLISTS,
            NodeSourceType.RECENTS_BUCKET,
            NodeSourceType.FOLDER_LINK,
            NodeSourceType.FILE_LINK,
            NodeSourceType.VIDEO_RECENTLY_WATCHED,
            NodeSourceType.CHAT,
            NodeSourceType.VIDEO_PLAYER_DEFAULT,
            NodeSourceType.VIDEO_PLAYER_IMAGE_VIEWER,
            NodeSourceType.VIDEO_PLAYER_VERSIONS,
            NodeSourceType.CONTINUE_WHERE_LEFT_OFF,
                -> getCloudSortOrder()

            NodeSourceType.OFFLINE -> getOfflineSortOrder()
            NodeSourceType.TIMELINE -> SortOrder.ORDER_MODIFICATION_DESC
        }
}
