package mega.privacy.android.feature.clouddrive.presentation.shares.contactshares

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.core.nodecomponents.action.MultiNodeActionHandler
import mega.privacy.android.core.nodecomponents.model.NodeActionState
import mega.privacy.android.core.transfers.widget.TransfersToolabarWidgetUiState
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidgetViewModel
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.clouddrive.presentation.shares.contactshares.model.ContactSharedFoldersUiState
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model.IncomingSharesAction
import mega.privacy.android.feature.clouddrive.presentation.shares.incomingshares.model.IncomingSharesUiState
import mega.privacy.android.feature.transfers.components.widget.TransfersToolbarWidgetStatus
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.nodes.components.SORT_ORDER_TAG
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.NodeUiItem
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(AndroidJUnit4::class)
class ContactSharedFoldersScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val analyticsRule = AnalyticsTestRule(mock<AnalyticsTracker>())

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val navigationIconContentDescription = "Navigation Icon"

    private val emptyStateTitle = context.getString(sharedR.string.shares_screen_incoming_empty)
        .replace("[B]", "").replace("[/B]", "")
        .replace("[A]", "").replace("[/A]", "")

    private val transfersToolbarWidgetViewModel = mock<TransfersToolbarWidgetViewModel> {
        on { state } doReturn MutableStateFlow(
            TransfersToolabarWidgetUiState(
                status = TransfersToolbarWidgetStatus.Transferring,
                isUserLoggedIn = true,
            )
        )
    }

    private val viewModelStore = mock<ViewModelStore> {
        on {
            get(argThat<String> {
                contains(TransfersToolbarWidgetViewModel::class.java.canonicalName.orEmpty())
            })
        } doReturn transfersToolbarWidgetViewModel
    }

    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { viewModelStore } doReturn viewModelStore
    }

    @Test
    fun `test that app bar shows the contact name when it is resolved`() {
        setupComposeContent(contactName = "John Doe")

        composeRule.onNodeWithTag(CONTACT_SHARED_FOLDERS_APP_BAR_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("John Doe").assertIsDisplayed()
    }

    @Test
    fun `test that app bar shows the email when the contact name is not resolved`() {
        setupComposeContent(contactName = null, email = "john@mega.co.nz")

        composeRule.onNodeWithText("john@mega.co.nz").assertIsDisplayed()
    }

    @Test
    fun `test that onBack is invoked when the navigation icon is clicked`() {
        val onBack = mock<() -> Unit>()
        setupComposeContent(onBack = onBack)

        composeRule.onNodeWithContentDescription(navigationIconContentDescription).performClick()

        verify(onBack).invoke()
    }

    @Test
    fun `test that onNavigateToTransfers is invoked when the transfers widget is clicked`() {
        val onNavigateToTransfers = mock<() -> Unit>()
        setupComposeContent(onNavigateToTransfers = onNavigateToTransfers)

        composeRule.onNodeWithTag(CONTACT_SHARED_FOLDERS_TRANSFERS_WIDGET_TAG).performClick()

        verify(onNavigateToTransfers).invoke()
    }

    @Test
    fun `test that empty state is shown when there are no shared folders`() {
        setupComposeContent(items = emptyList(), isLoading = false)

        composeRule.onNodeWithText(emptyStateTitle).assertIsDisplayed()
    }

    @Test
    fun `test that empty state is not shown while loading`() {
        setupComposeContent(items = emptyList(), isLoading = true)

        composeRule.onNodeWithText(emptyStateTitle).assertDoesNotExist()
    }

    @Test
    fun `test that shared folders are shown when items are loaded`() {
        setupComposeContent(
            items = listOf(
                createNodeUiItem(id = 1L, name = "Shared folder 1"),
                createNodeUiItem(id = 2L, name = "Shared folder 2"),
            ),
            isLoading = false,
        )

        composeRule.onNodeWithTag(CONTACT_SHARED_FOLDERS_CONTENT_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Shared folder 1").assertIsDisplayed()
        composeRule.onNodeWithText("Shared folder 2").assertIsDisplayed()
    }

    @Test
    fun `test that ItemClicked action is emitted when an item is clicked`() {
        val onIncomingShareAction = mock<(IncomingSharesAction) -> Unit>()
        val item = createNodeUiItem(id = 1L, name = "Shared folder 1")
        setupComposeContent(
            items = listOf(item),
            isLoading = false,
            onIncomingShareAction = onIncomingShareAction,
        )

        composeRule.onNodeWithText("Shared folder 1").performClick()

        verify(onIncomingShareAction).invoke(IncomingSharesAction.ItemClicked(item))
    }

    @Test
    fun `test that ItemLongClicked action is emitted when an item is long clicked`() {
        val onIncomingShareAction = mock<(IncomingSharesAction) -> Unit>()
        val item = createNodeUiItem(id = 1L, name = "Shared folder 1")
        setupComposeContent(
            items = listOf(item),
            isLoading = false,
            onIncomingShareAction = onIncomingShareAction,
        )

        composeRule.onNodeWithText("Shared folder 1").performTouchInput { longClick() }

        verify(onIncomingShareAction).invoke(IncomingSharesAction.ItemLongClicked(item))
    }

    @Test
    fun `test that onShowNodeOptions is invoked with the node id when the item menu is clicked`() {
        val onShowNodeOptions = mock<(NodeId) -> Unit>()
        setupComposeContent(
            items = listOf(createNodeUiItem(id = 1L, name = "Shared folder 1")),
            isLoading = false,
            onShowNodeOptions = onShowNodeOptions,
        )

        composeRule.onNodeWithContentDescription("More", useUnmergedTree = true).performClick()

        verify(onShowNodeOptions).invoke(NodeId(1L))
    }

    @Test
    fun `test that selection mode app bar replaces the default app bar when an item is selected`() {
        setupComposeContent(
            items = listOf(createNodeUiItem(id = 1L, name = "Shared folder 1", isSelected = true)),
            isLoading = false,
        )

        composeRule.onNodeWithTag(CONTACT_SHARED_FOLDERS_SELECTION_MODE_APP_BAR_TAG)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(CONTACT_SHARED_FOLDERS_APP_BAR_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that selection mode app bar shows the selected items count`() {
        setupComposeContent(
            items = listOf(
                createNodeUiItem(id = 1L, name = "Shared folder 1", isSelected = true),
                createNodeUiItem(id = 2L, name = "Shared folder 2", isSelected = true),
                createNodeUiItem(id = 3L, name = "Shared folder 3", isSelected = false),
            ),
            isLoading = false,
        )

        composeRule.onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun `test that onSelectAllClicked is invoked when select all is clicked`() {
        val onSelectAllClicked = mock<() -> Unit>()
        setupComposeContent(
            items = listOf(
                createNodeUiItem(id = 1L, name = "Shared folder 1", isSelected = true),
                createNodeUiItem(id = 2L, name = "Shared folder 2", isSelected = false),
            ),
            isLoading = false,
            onSelectAllClicked = onSelectAllClicked,
        )

        composeRule.onNodeWithTag(CommonMenuAction.SelectAll.testTag).performClick()

        verify(onSelectAllClicked).invoke()
    }

    @Test
    fun `test that select all is not offered when all items are selected`() {
        setupComposeContent(
            items = listOf(createNodeUiItem(id = 1L, name = "Shared folder 1", isSelected = true)),
            isLoading = false,
        )

        composeRule.onNodeWithTag(CommonMenuAction.SelectAll.testTag).assertDoesNotExist()
    }

    @Test
    fun `test that onCancelSelectionClicked is invoked when the close icon is clicked`() {
        val onCancelSelectionClicked = mock<() -> Unit>()
        setupComposeContent(
            items = listOf(createNodeUiItem(id = 1L, name = "Shared folder 1", isSelected = true)),
            isLoading = false,
            onCancelSelectionClicked = onCancelSelectionClicked,
        )

        composeRule.onNodeWithContentDescription(navigationIconContentDescription).performClick()

        verify(onCancelSelectionClicked).invoke()
    }

    @Test
    fun `test that selection mode bottom bar is shown when an item is selected`() {
        setupComposeContent(
            items = listOf(createNodeUiItem(id = 1L, name = "Shared folder 1", isSelected = true)),
            isLoading = false,
            nodeActionState = NodeActionState(
                visibleActions = listOf(CommonMenuAction.More),
                availableActions = listOf(CommonMenuAction.More),
            ),
        )

        composeRule.onNodeWithTag(CONTACT_SHARED_FOLDERS_SELECTION_MODE_BOTTOM_BAR_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that selection mode bottom bar is not shown when no item is selected`() {
        setupComposeContent(
            items = listOf(createNodeUiItem(id = 1L, name = "Shared folder 1", isSelected = false)),
            isLoading = false,
            nodeActionState = NodeActionState(
                visibleActions = listOf(CommonMenuAction.More),
                availableActions = listOf(CommonMenuAction.More),
            ),
        )

        composeRule.onNodeWithTag(CONTACT_SHARED_FOLDERS_SELECTION_MODE_BOTTOM_BAR_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun `test that sort bottom sheet is shown when the sort order is clicked`() {
        setupComposeContent(
            items = listOf(createNodeUiItem(id = 1L, name = "Shared folder 1")),
            isLoading = false,
        )

        composeRule.onNodeWithTag(SORT_ORDER_TAG, useUnmergedTree = true).performClick()

        composeRule.onNodeWithText(context.getString(sharedR.string.action_sort_by_header))
            .assertIsDisplayed()
    }

    private fun createNodeUiItem(
        id: Long,
        name: String,
        isSelected: Boolean = false,
    ): NodeUiItem<TypedNode> {
        val node = mock<TypedFolderNode> {
            on { this.id } doReturn NodeId(id)
            on { this.name } doReturn name
        }
        return NodeUiItem(
            node = node,
            isSelected = isSelected,
            title = LocalizedText.Literal(name),
            iconRes = iconPackR.drawable.ic_folder_users_medium_solid,
        )
    }

    private fun setupComposeContent(
        email: String = "john@mega.co.nz",
        contactName: String? = "John Doe",
        items: List<NodeUiItem<TypedNode>> = emptyList(),
        isLoading: Boolean = false,
        nodeActionState: NodeActionState = NodeActionState(),
        onBack: () -> Unit = {},
        onNavigateToTransfers: () -> Unit = {},
        onShowNodeOptions: (NodeId) -> Unit = {},
        onSelectAllClicked: () -> Unit = {},
        onCancelSelectionClicked: () -> Unit = {},
        onIncomingShareAction: (IncomingSharesAction) -> Unit = {},
        setSortOrder: (NodeSortConfiguration) -> Unit = {},
    ) {
        val uiState = ContactSharedFoldersUiState(
            contactName = contactName,
            shares = IncomingSharesUiState(
                isLoading = isLoading,
                items = items,
            ),
        )
        composeRule.setContent {
            CompositionLocalProvider(
                LocalActivity provides null,
                LocalViewModelStoreOwner provides viewModelStoreOwner,
            ) {
                AndroidThemeForPreviews {
                    ContactSharedFoldersScreen(
                        email = email,
                        navigationHandler = mock(),
                        onBack = onBack,
                        onNavigateToTransfers = onNavigateToTransfers,
                        onShowNodeOptions = onShowNodeOptions,
                        onTransfer = {},
                        uiState = uiState,
                        nodeActionState = nodeActionState,
                        selectionModeActionHandler = MultiNodeActionHandler { _, _ -> },
                        onSelectAllClicked = onSelectAllClicked,
                        onCancelSelectionClicked = onCancelSelectionClicked,
                        onIncomingShareAction = onIncomingShareAction,
                        setSortOrder = setSortOrder,
                    )
                }
            }
        }
    }
}
