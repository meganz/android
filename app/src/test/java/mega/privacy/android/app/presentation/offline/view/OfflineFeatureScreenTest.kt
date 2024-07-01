package mega.privacy.android.app.presentation.offline.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.offline.offlinecompose.model.OfflineUiState
import mega.privacy.android.shared.original.core.ui.controls.banners.TEST_TAG_WARNING_BANNER_CLOSE
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@RunWith(AndroidJUnit4::class)
class OfflineFeatureScreenTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that OfflineFeatureScreen displays OfflineLoadingView when loading`() {
        val uiState = OfflineUiState(isLoading = true)
        composeRule.setContent {
            OfflineFeatureScreen(
                uiState = uiState,
                fileTypeIconMapper = mock(),
                onCloseWarningClick = {},
                onOfflineItemClicked = {},
                onItemLongClicked = { },
                onOptionClicked = {}
            )
        }

        composeRule.onNodeWithTag(OFFLINE_LOADING_VIEW_TEST_TAG, true).assertIsDisplayed()
    }

    @Test
    fun `test that OfflineFeatureScreen displays OfflineEmptyView when loaded and list is empty`() {
        val uiState = OfflineUiState(isLoading = false, offlineNodes = emptyList())
        composeRule.setContent {
            OfflineFeatureScreen(
                uiState = uiState,
                fileTypeIconMapper = mock(),
                onCloseWarningClick = {},
                onOfflineItemClicked = {},
                onItemLongClicked = { },
                onOptionClicked = {}
            )
        }

        composeRule.onNodeWithTag(OFFLINE_EMPTY_IMAGE_TEST_TAG, true).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_EMPTY_TEXT_TEST_TAG, true).assertIsDisplayed()
    }

    @Test
    fun `test that OfflineFeatureScreen displays warning banner when showOfflineWarning is true and isLoading is false`() {
        val uiState =
            OfflineUiState(
                isLoading = false,
                offlineNodes = emptyList(),
                showOfflineWarning = true,
            )
        composeRule.setContent {
            OfflineFeatureScreen(
                uiState = uiState,
                fileTypeIconMapper = mock(),
                onCloseWarningClick = {},
                onOfflineItemClicked = {},
                onItemLongClicked = { },
                onOptionClicked = {}
            )
        }

        composeRule.onNodeWithTag(TEST_TAG_WARNING_BANNER_CLOSE, true).assertIsDisplayed()
    }
}