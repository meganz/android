package mega.privacy.android.app.presentation.offline.offlinefileinfocompose.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_AVAILABLE_OFFLINE_SWITCH
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_ICON
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_PREVIEW
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.model.OfflineFileInfoUiState
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.app.onNodeWithText


@RunWith(AndroidJUnit4::class)
class OfflineFileInfoScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that preview is shown if thumbnail is set`() {
        composeTestRule.setContent {
            val uiState = OfflineFileInfoUiState(
                offlineFileInformation = OfflineFileInformation(
                    handle = "1",
                    path = "/path",
                    lastModifiedTime = 1000L,
                    thumbnail = "/path"
                ),
                isLoading = false
            )
            OfflineFileInfoScreen(
                uiState = uiState,
                onBackPressed = { },
                onRemoveFromOffline = { }
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_PREVIEW, true).assertExists()
    }

    @Test
    fun `test that icon and folder info views are shown if node type is folder`() {
        composeTestRule.setContent {
            val uiState = OfflineFileInfoUiState(
                offlineFileInformation = OfflineFileInformation(
                    thumbnail = null,
                    isFolder = true,
                    handle = "1",
                    path = "/path",
                    lastModifiedTime = 1000L,
                ),
                isLoading = false
            )
            OfflineFileInfoScreen(
                uiState = uiState,
                onBackPressed = { },
                onRemoveFromOffline = { }
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ICON, true).assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_PREVIEW, true).assertDoesNotExist()
    }

    @Test
    fun `test that alert dialog is shown when remove from offline switch is clicked`() {
        composeTestRule.setContent {
            val uiState = OfflineFileInfoUiState(
                offlineFileInformation = OfflineFileInformation(
                    thumbnail = null,
                    isFolder = true,
                    handle = "1",
                    path = "/path",
                    lastModifiedTime = 1000L,
                ),
                isLoading = false
            )
            OfflineFileInfoScreen(
                uiState = uiState,
                onBackPressed = { },
                onRemoveFromOffline = { }
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_AVAILABLE_OFFLINE_SWITCH, true).performClick()
        composeTestRule.onNodeWithText(R.string.confirmation_delete_from_save_for_offline)
            .assertExists()
    }
}