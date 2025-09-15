package mega.privacy.android.app.presentation.rubbishbin.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType

/**
 *  @property currentHandle The current Handle
 *  @property rubbishBinHandle rubbish bin folder handle
 *  @property parentHandle parent handle of the current node
 *  @property nodeList List of [NodeUIItem]
 *  @property selectedFileNodes number of selected file [NodeUIItem] on Compose
 *  @property selectedFolderNodes number of selected folder [NodeUIItem] on Compose
 *  @property isInSelection to identify if [NodeUIItem] list is in navigation state
 *  @property currentViewType ViewType The current ViewType used by the UI
 *  @property sortOrder [SortOrder] of current list
 *  @property isPendingRefresh
 *  @property selectedNodes List of selected [TypedNode]
 *  @property restoreType Determines the specific "Restore" behavior
 *  @property accountType
 *  @property isBusinessAccountExpired
 *  @property hiddenNodeEnabled
 *  @property openedFolderNodeHandles List of opened folder node handles
 */
data class LegacyRubbishBinState(
    val rubbishBinHandle: Long = -1L,
    val currentHandle: Long = -1L,
    val parentHandle: Long? = null,
    val nodeList: List<NodeUIItem<TypedNode>> = emptyList(),
    val selectedFileNodes: Int = 0,
    val selectedFolderNodes: Int = 0,
    val isInSelection: Boolean = false,
    val currentViewType: ViewType = ViewType.LIST,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val selectedNodes: List<TypedNode> = emptyList(),
    val isPendingRefresh: Boolean = false,
    val restoreType: RestoreType? = null,
    val accountType: AccountType? = null,
    val isBusinessAccountExpired: Boolean = false,
    val hiddenNodeEnabled: Boolean = false,
    val openedFolderNodeHandles: List<Long> = listOf(-1L),
    val isLoading: Boolean = true,
    val resetScrollPositionEvent: StateEvent = consumed,
) {
    val isRootDirectory get() = currentHandle == -1L || currentHandle == rubbishBinHandle
}
