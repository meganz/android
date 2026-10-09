package mega.privacy.android.domain.usecase.folderpreference

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.node.HandleToBase64UseCase
import javax.inject.Inject

/**
 * Get the sort order currently applied to a cloud folder: its per-folder sort order when per-folder
 * sorting is in use, otherwise the global cloud sort order.
 */
class GetFolderSortOrderUseCase @Inject constructor(
    private val monitorFolderSortOrderUseCase: MonitorFolderSortOrderUseCase,
    private val handleToBase64UseCase: HandleToBase64UseCase,
    private val getCloudSortOrder: GetCloudSortOrder,
) {
    /**
     * @param folderId the folder whose sort order to resolve
     */
    suspend operator fun invoke(folderId: NodeId): SortOrder =
        monitorFolderSortOrderUseCase(
            folderKey = handleToBase64UseCase(folderId.longValue),
            orElse = flow { emit(getCloudSortOrder()) },
        ).first()
}
