package mega.privacy.android.domain.usecase.file

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.document.DocumentEntity
import mega.privacy.android.domain.entity.document.DocumentFolder
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetOfflineSortOrder
import javax.inject.Inject

/**
 * Apply sort order to document folder use case
 *
 */
class ApplySortOrderToDocumentFolderUseCase @Inject constructor(
    private val getOfflineSortOrder: GetOfflineSortOrder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Invoke
     *
     * @param folder
     */
    suspend operator fun invoke(folder: DocumentFolder): Pair<List<DocumentEntity>, List<DocumentEntity>> =
        withContext(ioDispatcher) {
            val order = getOfflineSortOrder()
            val partition = folder.files.toList().partition { !it.isFolder }
            val childFiles = partition.first
            val childFolder = partition.second
            return@withContext when (order) {
                SortOrder.ORDER_DEFAULT_ASC -> {
                    childFiles.sortedBy { item -> item.name } to childFolder.sortedBy { item -> item.name }
                }

                SortOrder.ORDER_DEFAULT_DESC -> {
                    childFiles.sortedByDescending { item -> item.name } to childFolder.sortedByDescending { item -> item.name }
                }

                SortOrder.ORDER_MODIFICATION_ASC -> {
                    childFiles.sortedBy { item -> item.lastModified } to childFolder.sortedBy { item -> item.name }
                }

                SortOrder.ORDER_MODIFICATION_DESC -> {
                    childFiles.sortedByDescending { item -> item.lastModified } to childFolder.sortedBy { item -> item.name }
                }

                SortOrder.ORDER_SIZE_ASC -> {
                    childFiles.sortedBy { item -> item.size } to childFolder.sortedBy { item -> item.name }
                }

                SortOrder.ORDER_SIZE_DESC -> {
                    childFiles.sortedByDescending { item -> item.size } to childFolder.sortedBy { item -> item.name }
                }

                else -> {
                    childFiles to childFolder
                }
            }
        }
}