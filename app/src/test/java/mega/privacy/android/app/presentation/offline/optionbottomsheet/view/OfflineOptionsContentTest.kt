package mega.privacy.android.app.presentation.offline.optionbottomsheet.view


import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.offline.optionbottomsheet.model.OfflineOptionsUiState
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentActionsEmptyViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that node view is shown`() {
        composeRule.setContent {
            val nodeInfo = OtherOfflineNodeInformation(
                id = 1,
                handle = "1",
                parentId = 0,
                path = "/path",
                name = "Title",
                isFolder = false,
                lastModifiedTime = 1000L
            )

            val uiState = OfflineOptionsUiState(
                nodeId = NodeId(1),
                offlineFileInformation = OfflineFileInformation(
                    nodeInfo = nodeInfo,
                    thumbnail = null
                ),
                isOnline = true,
                isLoading = false
            )
            OfflineOptionsContent(
                uiState = uiState,
                fileTypeIconMapper = FileTypeIconMapper(),
                onRemoveFromOfflineClicked = {},
                onOpenInfoClicked = {},
                onOpenWithClicked = {},
                onSaveToDeviceClicked = {},
                onShareNodeClicked = {}
            )
        }
        composeRule.onNodeWithTag(NODE_VIEW_TEST_TAG, true).assertIsDisplayed()
        composeRule.onNodeWithTag(INFO_ACTION_TEST_TAG, true).assertIsDisplayed()
        composeRule.onNodeWithTag(OPEN_WITH_ACTION_TEST_TAG, true).assertIsDisplayed()
        composeRule.onNodeWithTag(SHARE_ACTION_TEST_TAG, true).assertIsDisplayed()
        composeRule.onNodeWithTag(SAVE_TO_DEVICE_ACTION_TEST_TAG, true).assertIsDisplayed()
        composeRule.onNodeWithTag(REMOVE_FROM_OFFLINE_ACTION_TEST_TAG, true).assertIsDisplayed()
    }

    @Test
    fun `test that open with action is not shown when node is not a folder`() {
        composeRule.setContent {
            val nodeInfo = OtherOfflineNodeInformation(
                id = 1,
                handle = "1",
                parentId = 0,
                path = "/path",
                name = "Title",
                isFolder = true,
                lastModifiedTime = 1000L
            )

            val uiState = OfflineOptionsUiState(
                nodeId = NodeId(1),
                isOnline = true,
                offlineFileInformation = OfflineFileInformation(
                    nodeInfo = nodeInfo,
                    thumbnail = null
                ),
                isLoading = false
            )
            OfflineOptionsContent(
                uiState = uiState,
                fileTypeIconMapper = FileTypeIconMapper(),
                onRemoveFromOfflineClicked = {},
                onOpenInfoClicked = {},
                onOpenWithClicked = {},
                onSaveToDeviceClicked = {},
                onShareNodeClicked = {}
            )
        }
        composeRule.onNodeWithTag(OPEN_WITH_ACTION_TEST_TAG, true).assertIsNotDisplayed()
        composeRule.onNodeWithTag(SHARE_ACTION_TEST_TAG, true).assertIsDisplayed()
    }

    @Test
    fun `test that share action is not shown when node is a folder but device is not online`() {
        composeRule.setContent {
            val nodeInfo = OtherOfflineNodeInformation(
                id = 1,
                handle = "1",
                parentId = 0,
                path = "/path",
                name = "Title",
                isFolder = true,
                lastModifiedTime = 1000L
            )

            val uiState = OfflineOptionsUiState(
                nodeId = NodeId(1),
                offlineFileInformation = OfflineFileInformation(
                    nodeInfo = nodeInfo,
                    thumbnail = null
                ),
                isLoading = false
            )
            OfflineOptionsContent(
                uiState = uiState,
                fileTypeIconMapper = FileTypeIconMapper(),
                onRemoveFromOfflineClicked = {},
                onOpenInfoClicked = {},
                onOpenWithClicked = {},
                onSaveToDeviceClicked = {},
                onShareNodeClicked = {}
            )
        }
        composeRule.onNodeWithTag(SHARE_ACTION_TEST_TAG, true).assertIsNotDisplayed()
    }

}