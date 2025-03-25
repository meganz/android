package mega.privacy.android.domain.usecase.favourites

import mega.privacy.android.domain.entity.favourite.FavouriteSortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.UnTypedNode
import javax.inject.Inject

/**
 * Use case interface for adding favourites
 */
class SortFavouritesUseCase @Inject constructor(
    private val getFavouriteSortOrderUseCase: GetFavouriteSortOrderUseCase,
) {

    /**
     * Invoke
     * @param nodes the list of nodes to be sorted.
     * @param order the sort order.
     */
    suspend operator fun invoke(
        nodes: List<UnTypedNode>,
        order: FavouriteSortOrder? = null,
    ): List<UnTypedNode> {
        val sortOrder = order ?: getFavouriteSortOrderUseCase()
        return nodes.sortedWith { item1, item2 ->
            if (sortOrder.sortDescending) {
                item2.compareTo(item1, sortOrder)
            } else {
                item1.compareTo(item2, sortOrder)
            }
        }
    }

    private fun UnTypedNode.compareTo(other: UnTypedNode, order: FavouriteSortOrder): Int {
        return when (this) {
            is FileNode -> this.compareToFile(other, order)
            is FolderNode -> this.compareToFolder(other, order)
        }
    }

    private fun FolderNode.compareToFolder(
        other: UnTypedNode,
        order: FavouriteSortOrder,
    ): Int {
        val otherFolder = other as? FolderNode ?: return compareFolderToFile(order)
        return if (order is FavouriteSortOrder.Label) label.compareTo(otherFolder.label) else name.compareTo(
            otherFolder.name
        )
    }

    private fun FileNode.compareToFile(
        other: UnTypedNode,
        order: FavouriteSortOrder,
    ): Int {
        val otherFile = other as? FileNode ?: return compareFileToFolder(order)
        return when (order) {
            FavouriteSortOrder.Label -> label.compareTo(otherFile.label)
            is FavouriteSortOrder.ModifiedDate -> modificationTime.compareTo(otherFile.modificationTime)
            is FavouriteSortOrder.Name -> name.compareTo(otherFile.name)
            is FavouriteSortOrder.Size -> size.compareTo(otherFile.size)
        }
    }

    private fun compareFolderToFile(order: FavouriteSortOrder) = if (order.sortDescending) 1 else -1

    private fun compareFileToFolder(order: FavouriteSortOrder) = if (order.sortDescending) -1 else 1
}