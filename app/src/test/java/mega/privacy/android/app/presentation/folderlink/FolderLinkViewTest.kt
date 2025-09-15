package mega.privacy.android.app.presentation.folderlink

import androidx.compose.material.rememberScaffoldState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.palm.composestateevents.triggered
import mega.privacy.android.app.R
import mega.privacy.android.app.fromId
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.folderlink.model.FolderLinkState
import mega.privacy.android.app.presentation.folderlink.view.Constants.APPBAR_MORE_OPTION_TAG
import mega.privacy.android.app.presentation.folderlink.view.Constants.BOTTOM_SHEET_SAVE
import mega.privacy.android.app.presentation.folderlink.view.Constants.IMPORT_BUTTON_TAG
import mega.privacy.android.app.presentation.folderlink.view.Constants.SAVE_BUTTON_TAG
import mega.privacy.android.app.presentation.folderlink.view.FolderLinkView
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.legacy.core.ui.controls.lists.MEDIA_DISCOVERY_TAG
import mega.privacy.android.shared.original.core.ui.controls.snackbars.SNACKBAR_TEST_TAG
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
@Ignore("AndroidViewBinding can't inflate in the compose bom 2025.04.01")
class FolderLinkViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        uiState: FolderLinkState = FolderLinkState(),
    ) {
        composeTestRule.setContent {
            FolderLinkView(
                state = uiState,
                scaffoldState = rememberScaffoldState(),
                onBackPressed = { },
                onShareClicked = { },
                onMoreOptionClick = { },
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
                onSelectImportLocation = { },
                onResetSelectImportLocation = { },
                onResetSnackbarMessage = { },
                onResetMoreOptionNode = { },
                onResetOpenMoreOption = { },
                onStorageStatusDialogDismiss = { },
                onStorageDialogActionButtonClick = { },
                onStorageDialogAchievementButtonClick = { },
                emptyViewString = stringResource(id = R.string.file_browser_empty_folder),
                onLinkClicked = { },
                onDisputeTakeDownClicked = { },
                onEnterMediaDiscoveryClick = { },
                fileTypeIconMapper = FileTypeIconMapper(),
                onTransferWidgetClick = { },
                request = null
            )
        }
    }

    @Test
    fun `test that toolbar title has correct initial value`() {
        setComposeContent(uiState = FolderLinkState())
        composeTestRule.onNodeWithText("MEGA - ${fromId(R.string.general_loading)}")
            .assertIsDisplayed()
    }

    @Test
    fun `test that toolbar title has correct value when nodes are fetched`() {
        val title = "Folder Title"
        setComposeContent(uiState = FolderLinkState(isNodesFetched = true, title = title))
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `test that correct toolbar is displayed when node is selected`() {
        val title = "1 selected"
        setComposeContent(
            uiState = FolderLinkState(isNodesFetched = true, title = title, selectedNodeCount = 1)
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
        setComposeContent(uiState = FolderLinkState(isNodesFetched = true, nodesList = listOf()))
        composeTestRule.onNodeWithText(R.string.file_browser_empty_folder).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Folder").assertIsDisplayed()
    }


    @Test
    fun `test that media discovery option is shown`() {
        val nodeName = "Folder1"
        val node = mock<TypedFolderNode>()
        whenever(node.name).thenReturn(nodeName)
        setComposeContent(
            uiState = FolderLinkState(
                isNodesFetched = true,
                nodesList = listOf(NodeUIItem(node, isSelected = false, isInvisible = false)),
                hasDbCredentials = true,
                hasMediaItem = true
            )
        )
        composeTestRule.onNodeWithTag(MEDIA_DISCOVERY_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that nodes view is displayed when nodes are fetched`() {
        val nodeName = "Folder1"
        val node = mock<TypedFolderNode>()
        whenever(node.name).thenReturn(nodeName)
        setComposeContent(
            uiState = FolderLinkState(
                isNodesFetched = true,
                nodesList = listOf(
                    NodeUIItem(
                        node,
                        isSelected = false,
                        isInvisible = false
                    )
                )
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
            uiState = FolderLinkState(
                isNodesFetched = true,
                nodesList = listOf(
                    NodeUIItem(
                        node,
                        isSelected = false,
                        isInvisible = false
                    )
                ),
                hasDbCredentials = true
            )
        )
        composeTestRule.onNodeWithTag(IMPORT_BUTTON_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that snackbar is shown shown`() {
        val nodeName = "Folder1"
        val node = mock<TypedFolderNode>()
        whenever(node.name).thenReturn(nodeName)
        setComposeContent(
            uiState = FolderLinkState(
                isNodesFetched = true,
                nodesList = listOf(
                    NodeUIItem(
                        node,
                        isSelected = false,
                        isInvisible = false
                    )
                ),
                snackbarMessageContent = triggered("Test")
            )
        )
        composeTestRule.onNodeWithTag(SNACKBAR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that screen should not go back when folder link bottom sheet is visible and back action triggered`() {
        val nodeName = "Folder1"
        val node = mock<TypedFolderNode>()
        whenever(node.name).thenReturn(nodeName)
        setComposeContent(
            uiState = FolderLinkState(
                isNodesFetched = true,
                nodesList = listOf(
                    NodeUIItem(
                        node,
                        isSelected = false,
                        isInvisible = false
                    )
                ),
                hasDbCredentials = true,
                openMoreOption = triggered
            )
        )
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_SAVE).assertIsDisplayed()
        Espresso.pressBack()
        composeTestRule.onNodeWithTag(BOTTOM_SHEET_SAVE).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(APPBAR_MORE_OPTION_TAG).assertIsDisplayed()
    }
}