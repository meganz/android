package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.model.ChatAttachmentUiEntity
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.model.NodeAttachmentBottomSheetUiState
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeAttachmentBottomSheetContentTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that node view is shown`() {
        composeRule.setContent {
            val uiState = NodeAttachmentBottomSheetUiState(
                isOnline = false,
                item = ChatAttachmentUiEntity(
                    nodeId = NodeId(123),
                    name = "Title",
                    size = 1230,
                    thumbnailPath = null,
                    isAvailableOffline = true,
                    isInAnonymousMode = false
                ),
                isLoading = false
            )
            NodeAttachmentBottomSheetContent(
                uiState = uiState,
                fileTypeIconMapper = FileTypeIconMapper(),
                onAvailableOfflineChecked = { _, _ -> },
                onSaveToDeviceClicked = {},
                onImportClicked = {}
            )
        }
        composeRule.onNodeWithTag(NODE_VIEW_TEST_TAG, true).assertIsDisplayed()
        composeRule.onNodeWithTag(ADD_TO_CLOUD_DRIVE_ACTION_TEST_TAG, true).assertIsDisplayed()
        composeRule.onNodeWithTag(SAVE_TO_DEVICE_ACTION_TEST_TAG, true).assertIsDisplayed()
        composeRule.onNodeWithTag(AVAILABLE_OFFLINE_ACTION_TEST_TAG, true).assertIsDisplayed()
    }

    @Test
    fun `test that import and offline actions are not shown when chat is in anonymous mode`() {
        composeRule.setContent {
            val uiState = NodeAttachmentBottomSheetUiState(
                isOnline = false,
                item = ChatAttachmentUiEntity(
                    nodeId = NodeId(123),
                    name = "Title",
                    size = 1230,
                    thumbnailPath = null,
                    isAvailableOffline = true,
                    isInAnonymousMode = true
                ),
                isLoading = false
            )
            NodeAttachmentBottomSheetContent(
                uiState = uiState,
                fileTypeIconMapper = FileTypeIconMapper(),
                onAvailableOfflineChecked = { _, _ -> },
                onSaveToDeviceClicked = {},
                onImportClicked = {}
            )
        }
        composeRule.onNodeWithTag(ADD_TO_CLOUD_DRIVE_ACTION_TEST_TAG, true).assertIsNotDisplayed()
        composeRule.onNodeWithTag(AVAILABLE_OFFLINE_ACTION_TEST_TAG, true).assertIsNotDisplayed()
    }
}