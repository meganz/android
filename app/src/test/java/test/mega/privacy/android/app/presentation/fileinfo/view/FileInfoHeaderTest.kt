package test.mega.privacy.android.app.presentation.fileinfo.view

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
    fun `test that preview is not shown if preview uri is null`() {
        composeTestRule.setContent {
            FileInfoHeader(
                iconResource = null,
                accessPermissionDescription = null,
                title = "Title",
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_PREVIEW).assertDoesNotExist()
    }

    @Test
    fun `test that icon is shown if icon resource is set and preview uri is null`() {
        composeTestRule.setContent {
            FileInfoHeader(
                iconResource = android.R.drawable.ic_menu_more,
                accessPermissionDescription = null,
                title = "Title",
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ICON).assertExists()
    }

    @Test
    fun `test that icon is not shown if icon resource is null`() {
        composeTestRule.setContent {
            FileInfoHeader(
                iconResource = null,
                accessPermissionDescription = null,
                title = "Title",
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ICON).assertDoesNotExist()
    }

    @Test
    fun `test that access permission text is shown if access permission description is set and icon resource is not null`() {
        composeTestRule.setContent {
            FileInfoHeader(
                iconResource = android.R.drawable.ic_menu_more,
                accessPermissionDescription = android.R.string.ok,
                title = "Title",
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ACCESS).assertExists()
    }

    @Test
    fun `test that access permission text is not shown if access permission description is null`() {
        composeTestRule.setContent {
            FileInfoHeader(
                iconResource = null,
                accessPermissionDescription = null,
                title = "Title",
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ACCESS).assertDoesNotExist()
    }

}