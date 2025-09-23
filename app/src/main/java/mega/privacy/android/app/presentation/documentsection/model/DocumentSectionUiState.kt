package mega.privacy.android.app.presentation.documentsection.model

import mega.privacy.android.app.presentation.validator.toolbaractions.model.SelectedNode
import mega.privacy.android.app.presentation.validator.toolbaractions.modifier.ToolbarActionsModifierItem
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * The ui state for the document section
 *
 * @property allDocuments the all document items
 * @property currentViewType the current view type
 * @property sortOrder the sort order of audio items
 * @property isLoading if is loading data
 * @property selectedNodes the selected document nodes
 * @property actionMode if list is in action mode or not
 * @property scrollToTop if need to scroll to top
 * @property accountType the account type
 * @property isHiddenNodesOnboarded if is hidden nodes onboarded
 * @property isBusinessAccountExpired if the business account is expired
 * @property hiddenNodeEnabled if hidden node is enabled
 */
data class DocumentSectionUiState(
    val allDocuments: List<DocumentUiEntity> = emptyList(),
    val currentViewType: ViewType = ViewType.LIST,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val isLoading: Boolean = true,
    val selectedNodes: List<SelectedNode> = emptyList(),
    val actionMode: Boolean = false,
    val scrollToTop: Boolean = false,
    val accountType: AccountType? = null,
    val isHiddenNodesOnboarded: Boolean = false,
    val isBusinessAccountExpired: Boolean = false,
    val hiddenNodeEnabled: Boolean = false,
    val toolbarActionsModifierItem: ToolbarActionsModifierItem.DocumentSection? = null,
)
