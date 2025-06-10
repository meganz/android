package mega.privacy.android.domain.usecase.sortorder

import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetLinksSortOrder
import mega.privacy.android.domain.usecase.GetOthersSortOrder
import javax.inject.Inject

/**
 * Use case to get the sort order based on the node source type.
 */
class GetSortOrderByNodeSourceTypeUseCase @Inject constructor(
    private val getCloudSortOrder: GetCloudSortOrder,
    private val getLinksSortOrder: GetLinksSortOrder,
    private val getOthersSortOrder: GetOthersSortOrder,
) {

    /**
     * Gets the sort order based on the node source.
     *
     * @param nodeSource The source of the node.
     * @return The sort order for the specified node source.
     */
    suspend operator fun invoke(nodeSource: NodeSourceType): SortOrder =
        when (nodeSource) {
            NodeSourceType.LINKS -> getLinksSortOrder()
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
                -> getCloudSortOrder()
        }
}