package test.mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.fileinfo.view.FileInfoHeader
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_ACCESS
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_ICON
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_PREVIEW
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileInfoHeaderTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that preview is shown if preview uri is set`() {
        composeTestRule.setContent {
            FileInfoHeader(
                previewUri = "something",
                iconResource = null,
                accessPermissionDescription = null,
                backgroundAlpha = 1f,
                tintColor = Color.White,
                title = "Title",
                titleAlpha = 1f,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_PREVIEW).assertExists()
    }

    @Test
    fun `test that preview is not shown if preview uri is null`() {
        composeTestRule.setContent {
            FileInfoHeader(
                previewUri = null,
                iconResource = null,
                accessPermissionDescription = null,
                backgroundAlpha = 1f,
                tintColor = Color.White,
                title = "Title",
                titleAlpha = 1f,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_PREVIEW).assertDoesNotExist()
    }

    @Test
    fun `test that icon is shown if icon resource is set and preview uri is null`() {
        composeTestRule.setContent {
            FileInfoHeader(
                previewUri = null,
                iconResource = android.R.drawable.ic_menu_more,
                accessPermissionDescription = null,
                backgroundAlpha = 1f,
                tintColor = Color.White,
                title = "Title",
                titleAlpha = 1f,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ICON).assertExists()
    }

    @Test
    fun `test that icon is not shown if icon resource is null`() {
        composeTestRule.setContent {
            FileInfoHeader(
                previewUri = null,
                iconResource = null,
                accessPermissionDescription = null,
                backgroundAlpha = 1f,
                tintColor = Color.White,
                title = "Title",
                titleAlpha = 1f,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ICON).assertDoesNotExist()
    }

    @Test
    fun `test that icon is not shown if icon resource is set but preview uri is not null`() {
        composeTestRule.setContent {
            FileInfoHeader(
                previewUri = "something",
                iconResource = android.R.drawable.ic_menu_more,
                accessPermissionDescription = null,
                backgroundAlpha = 1f,
                tintColor = Color.White,
                title = "Title",
                titleAlpha = 1f,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ICON).assertDoesNotExist()
    }

    @Test
    fun `test that access permission test is shown if access permission description is set`() {
        composeTestRule.setContent {
            FileInfoHeader(
                previewUri = null,
                iconResource = null,
                accessPermissionDescription = android.R.string.ok,
                backgroundAlpha = 1f,
                tintColor = Color.White,
                title = "Title",
                titleAlpha = 1f,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ACCESS).assertExists()
    }

    @Test
    fun `test that access permission test is not shown if access permission description is null`() {
        composeTestRule.setContent {
            FileInfoHeader(
                previewUri = null,
                iconResource = null,
                accessPermissionDescription = null,
                backgroundAlpha = 1f,
                tintColor = Color.White,
                title = "Title",
                titleAlpha = 1f,
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ACCESS).assertDoesNotExist()
    }

}