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
            val partition = folder.files.partition { !it.isFolder }
            val childFiles = partition.first.toMutableList()
            val childFolder = partition.second.toMutableList()
            when (order) {
                SortOrder.ORDER_DEFAULT_ASC -> {
                    childFolder.sortBy { item -> item.name }
                    childFiles.sortBy { item -> item.name }
                }

                SortOrder.ORDER_DEFAULT_DESC -> {
                    childFolder.sortByDescending { item -> item.name }
                    childFiles.sortByDescending { item -> item.name }
                }

                SortOrder.ORDER_MODIFICATION_ASC -> {
                    childFolder.sortBy { item -> item.name }
                    childFiles.sortBy { item -> item.lastModified }
                }

                SortOrder.ORDER_MODIFICATION_DESC -> {
                    childFolder.sortBy { item -> item.name }
                    childFiles.sortByDescending { item -> item.lastModified }
                }

                SortOrder.ORDER_SIZE_ASC -> {
                    childFolder.sortBy { item -> item.name }
                    childFiles.sortBy { item -> item.size }
                }

                SortOrder.ORDER_SIZE_DESC -> {
                    childFolder.sortBy { item -> item.name }
                    childFiles.sortByDescending { item -> item.size }
                }

                else -> Unit
            }

            childFiles to childFolder
        }
}