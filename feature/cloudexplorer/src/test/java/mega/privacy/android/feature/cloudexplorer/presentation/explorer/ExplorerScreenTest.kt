package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodeExplorerUiState
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.NodesExplorerViewModel
import mega.privacy.android.feature.cloudexplorer.presentation.nodesexplorer.nodeExplorerDataState
import mega.privacy.android.navigation.destination.CopyNavKey
import mega.privacy.android.shared.nodes.components.previewdata.LocalNodeHeaderPreviewData
import mega.privacy.android.shared.nodes.model.NodeHeaderItemUiState
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
internal class ExplorerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    private val viewModel = mock<NodesExplorerViewModel>()

    private fun viewModelStoreOwner(coordinator: ExplorerViewModel): ViewModelStoreOwner {
        val store = mock<ViewModelStore> {
            on { get(argThat<String> { contains(NodesExplorerViewModel::class.java.canonicalName.orEmpty()) }) } doReturn viewModel
            on { get(argThat<String> { contains(ExplorerViewModel::class.java.canonicalName.orEmpty()) }) } doReturn coordinator
        }
        return mock { on { viewModelStore } doReturn store }
    }

    @Test
    fun `test that the action buttons are shown when no action is processing`() {
        setContent(isProcessingAction = false)

        composeTestRule.onNodeWithTag(ACTION_BUTTONS_VIEW_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the action buttons are hidden while an action is processing`() {
        setContent(isProcessingAction = true)

        composeTestRule.onNodeWithTag(ACTION_BUTTONS_VIEW_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the folder name is shown as the title in inner navigation`() {
        setContent(
            uiState = nodeExplorerDataState(
                folderName = LocalizedText.Literal(FOLDER_NAME),
                isRoot = false,
            ),
        )

        composeTestRule.onNodeWithText(FOLDER_NAME).assertIsDisplayed()
    }

    @Test
    fun `test that clicking the action button in folder picker mode picks the current folder`() {
        var pickedFolderId: NodeId? = null
        setContent(
            nodeExplorerId = CURRENT_FOLDER_ID,
            onFolderPicked = { pickedFolderId = it },
        )

        composeTestRule.onNodeWithText(actionLabel(sharedR.string.general_copy)).performClick()

        assertThat(pickedFolderId).isEqualTo(CURRENT_FOLDER_ID)
    }

    @Test
    fun `test that clicking the action button does not pick the folder when offline`() {
        var pickedFolderId: NodeId? = null
        setContent(
            uiState = dataState(currentFolderId = CURRENT_FOLDER_ID),
            isConnected = false,
            onFolderPicked = { pickedFolderId = it },
        )

        composeTestRule.onNodeWithText(actionLabel(sharedR.string.general_copy)).performClick()

        assertThat(pickedFolderId).isNull()
    }

    @Test
    fun `test that clicking the cancel button closes the explorer`() {
        var closed = false
        setContent(onCloseExplorerScreen = { closed = true })

        composeTestRule.onNodeWithText(actionLabel(sharedR.string.general_dialog_cancel_button))
            .performClick()

        assertThat(closed).isTrue()
    }

    @Test
    fun `test that the action button is disabled when the current folder is the disabled target`() {
        setContent(
            nodeExplorerId = CURRENT_FOLDER_ID,
            disabledTargetId = CURRENT_FOLDER_ID,
        )

        composeTestRule.onNodeWithText(actionLabel(sharedR.string.general_copy)).assertIsNotEnabled()
    }

    @Test
    fun `test that the action button is disabled when picker restrictions disallow picking`() {
        setContent(
            nodeExplorerId = CURRENT_FOLDER_ID,
            pickerRestrictions = ExplorerPickerRestrictions(isPickEnabled = false),
        )

        composeTestRule.onNodeWithText(actionLabel(sharedR.string.general_copy))
            .assertIsNotEnabled()
    }

    @Test
    fun `test that the action button is enabled when picker restrictions allow picking`() {
        setContent(
            nodeExplorerId = CURRENT_FOLDER_ID,
            disabledTargetId = CURRENT_FOLDER_ID,
            pickerRestrictions = ExplorerPickerRestrictions(isPickEnabled = true),
        )

        composeTestRule.onNodeWithText(actionLabel(sharedR.string.general_copy)).assertIsEnabled()
    }

    private fun setContent(
        uiState: NodeExplorerUiState = dataState(),
        nodeExplorerId: NodeId = NodeId(-1),
        isProcessingAction: Boolean = false,
        disabledTargetId: NodeId? = null,
        pickerRestrictions: ExplorerPickerRestrictions? = null,
        isConnected: Boolean = true,
        onFolderPicked: (NodeId) -> Unit = {},
        onCloseExplorerScreen: () -> Unit = {},
    ) {
        whenever(viewModel.uiState).thenReturn(MutableStateFlow(uiState))
        val owner = viewModelStoreOwner(stubExplorerViewModel(isConnected = isConnected))

        composeTestRule.setContent {
            AndroidThemeForPreviews {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides owner,
                    LocalNodeHeaderPreviewData provides NodeHeaderItemUiState.Data(
                        viewType = ViewType.LIST,
                        nodeSortConfiguration = NodeSortConfiguration.default,
                    ),
                ) {
                    ExplorerScreen(
                        explorerMode = ExplorerMode.Copy,
                        startNavKey = CopyNavKey(emptyList()),
                        isInnerNavigation = true,
                        nodeExplorerId = nodeExplorerId,
                        nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                        onCloseExplorerScreen = onCloseExplorerScreen,
                        onNavigateBack = {},
                        onNavigate = {},
                        isProcessingAction = isProcessingAction,
                        disabledTargetId = disabledTargetId,
                        pickerRestrictions = pickerRestrictions,
                        onFolderPicked = onFolderPicked,
                    )
                }
            }
        }
    }

    private fun dataState(
        currentFolderId: NodeId = NodeId(-1),
    ) = nodeExplorerDataState(
        currentFolderId = currentFolderId,
        isRoot = false,
    )

    private fun actionLabel(resId: Int): String =
        ApplicationProvider.getApplicationContext<Context>().getString(resId)

    private companion object {
        const val FOLDER_NAME = "Test folder"
        val CURRENT_FOLDER_ID = NodeId(99)
    }
}
