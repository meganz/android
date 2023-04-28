package test.mega.privacy.android.app.presentation.folderlink

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.palm.composestateevents.triggered
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.folderlink.model.FolderLinkState
import mega.privacy.android.app.presentation.folderlink.view.FolderLinkView
import mega.privacy.android.app.presentation.testpassword.view.Constants
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.fromId
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class FolderLinkViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(uiState: FolderLinkState = FolderLinkState()) {
        composeTestRule.setContent {
            FolderLinkView(
                state = uiState,
                onBackPressed = { },
                onShareClicked = { },
                onMoreClicked = { },
                stringUtilWrapper = mock(),
                onMenuClick = { },
                onItemClicked = { },
                onLongClick = { },
                onChangeViewTypeClick = { },
                onSortOrderClick = { },
                onSelectAllActionClicked = { },
                onClearAllActionClicked = { },
                onSaveToDeviceClicked = { },
                onImportClicked = { },
                onOpenFile = { },
                onResetOpenFile = { },
                onDownloadNode = { },
                onResetDownloadNode = { },
                onSelectImportLocation = { },
                onResetSelectImportLocation = { },
                onResetSnackbarMessage = { },
                emptyViewString = stringResource(id = R.string.file_browser_empty_folder)
            )
        }
    }

    @Test
    fun `test that toolbar title has correct initial value`() {
        setComposeContent(FolderLinkState())
        composeTestRule.onNodeWithText("MEGA - ${fromId(R.string.general_loading)}")
            .assertIsDisplayed()
    }

    @Test
    fun `test that toolbar title has correct value when nodes are fetched`() {
        val title = "Folder Title"
        setComposeContent(FolderLinkState(isNodesFetched = true, title = title))
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `test that correct toolbar is displayed when node is selected`() {
        val title = "1 selected"
        setComposeContent(
            FolderLinkState(isNodesFetched = true, title = title, selectedNodeCount = 1)
        )
        composeTestRule.onNodeWithContentDescription(fromId(R.string.general_back_button))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(fromId(R.string.label_more))
            .assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(fromId(R.string.general_share))
            .assertDoesNotExist()
    }

    @Test
    fun `test that empty view is displayed when nodes list is empty`() {
        setComposeContent(FolderLinkState(isNodesFetched = true, nodesList = listOf()))
        composeTestRule.onNodeWithText(R.string.file_browser_empty_folder).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Folder").assertIsDisplayed()
    }

    @Test
    fun `test that nodes view is displayed when nodes are fetched`() {
        val nodeName = "Folder1"
        val node = mock<TypedFolderNode>()
        whenever(node.name).thenReturn(nodeName)
        setComposeContent(
            FolderLinkState(
                isNodesFetched = true,
                nodesList = listOf(NodeUIItem(node, isSelected = false, isInvisible = false))
            )
        )
        composeTestRule.onNodeWithText(nodeName).assertIsDisplayed()
    }

    @Test
    fun `test that import and save to device buttons are shown`() {
        val nodeName = "Folder1"
        val node = mock<TypedFolderNode>()
        whenever(node.name).thenReturn(nodeName)
        setComposeContent(
            FolderLinkState(
                isNodesFetched = true,
                nodesList = listOf(NodeUIItem(node, isSelected = false, isInvisible = false)),
                hasDbCredentials = true
            )
        )
        composeTestRule.onNodeWithText(R.string.add_to_cloud).assertIsDisplayed()
        composeTestRule.onNodeWithText(R.string.general_save_to_device).assertIsDisplayed()
    }

    @Test
    fun `test that snackbar is shown shown`() {
        val nodeName = "Folder1"
        val node = mock<TypedFolderNode>()
        whenever(node.name).thenReturn(nodeName)
        setComposeContent(
            FolderLinkState(
                isNodesFetched = true,
                nodesList = listOf(NodeUIItem(node, isSelected = false, isInvisible = false)),
                snackbarMessageContent = triggered("Test")
            )
        )
        composeTestRule.onNodeWithTag(Constants.SNACKBAR_TAG).assertIsDisplayed()
    }
}