package test.mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.fileinfo.model.FileInfoViewState
import mega.privacy.android.app.presentation.fileinfo.view.FileInfoScreen
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_ICON
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_PREVIEW
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileInfoScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that preview is shown if preview uri is set`() {
        composeTestRule.setContent {
            val snackBarHostState = remember { SnackbarHostState() }
            val viewState = FileInfoViewState(previewUriString = "something", hasPreview = true)
            FileInfoScreen(
                viewState = viewState,
                snackBarHostState = snackBarHostState,
                onBackPressed = { },
                onTakeDownLinkClick = {},
                onLocationClick = { },
                availableOfflineChanged = {},
                onVersionsClick = { },
                onSharedWithContactClick = {},
                onSharedWithContactSelected = {},
                onSharedWithContactUnselected = {},
                onSharedWithContactMoreOptionsClick = {},
                onShowMoreSharedWithContactsClick = {},
                onPublicLinkCopyClick = { },
                onMenuActionClick = {},
                statusBarHeight = 0f,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_PREVIEW, true).assertExists()
    }

    @Test
    fun `test that icon is not shown if icon resource is set but has preview`() {
        composeTestRule.setContent {
            val snackBarHostState = remember { SnackbarHostState() }
            val viewState = FileInfoViewState(
                previewUriString = "something",
                hasPreview = true,
                iconResource = android.R.drawable.ic_menu_more
            )
            FileInfoScreen(
                viewState = viewState,
                snackBarHostState = snackBarHostState,
                onBackPressed = { },
                onTakeDownLinkClick = {},
                onLocationClick = { },
                availableOfflineChanged = {},
                onVersionsClick = { },
                onSharedWithContactClick = {},
                onSharedWithContactSelected = {},
                onSharedWithContactUnselected = {},
                onSharedWithContactMoreOptionsClick = {},
                onShowMoreSharedWithContactsClick = {},
                onPublicLinkCopyClick = { },
                onMenuActionClick = {},
                statusBarHeight = 0f,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ICON, true).assertDoesNotExist()
    }

}