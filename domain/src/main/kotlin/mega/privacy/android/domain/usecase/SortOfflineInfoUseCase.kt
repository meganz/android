package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject

/**
 * Use case to sort offline information
 */
class SortOfflineInfoUseCase @Inject constructor(
    private val getOfflineSortOrder: GetOfflineSortOrder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Invoke a sort operation on the given list of offline information. The sorting is done
     * based on whether the item is a folder or file (folders first), followed by the
     * user-defined sort order.
     *
     * @param offlineInfoList List of offline information to be sorted
     * @return Sorted list of offline information
     */
    suspend operator fun invoke(
        offlineInfoList: List<OfflineFileInformation>,
    ): List<OfflineFileInformation> = withContext(ioDispatcher) {
        offlineInfoList.sortedWith(
            compareByDescending<OfflineFileInformation> { it.isFolder }.thenBy(getOfflineSortOrder())
        )
    }

    private fun Comparator<OfflineFileInformation>.thenBy(
        sortOrder: SortOrder,
    ): Comparator<OfflineFileInformation> {
        return when (sortOrder) {
            SortOrder.ORDER_DEFAULT_ASC -> thenBy { it.name.lowercase() }
            SortOrder.ORDER_DEFAULT_DESC -> thenByDescending { it.name.lowercase() }
            SortOrder.ORDER_SIZE_ASC -> thenBy { it.totalSize }
            SortOrder.ORDER_SIZE_DESC -> thenByDescending { it.totalSize }
            SortOrder.ORDER_MODIFICATION_ASC -> thenBy { it.lastModifiedTime }
            SortOrder.ORDER_MODIFICATION_DESC -> thenByDescending { it.lastModifiedTime }
            else -> thenBy { it.name.lowercase() }
        }
    }
}