package mega.privacy.android.core.ui.controls.chat.messages.file

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileNoPreviewMessageViewTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that file type show correctly`() {
        val fileName = "my pdf.pdf"
        initComposeRuleContent(
            fileName = fileName,
            fileSize = "30 MB",
        )
        composeRule.onNodeWithTag(FILE_MESSAGE_VIEW_FILE_NAME_TEST_TAG, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText(fileName).assertExists()
    }

    @Test
    fun `test that file size show correctly`() {
        val fileSize = "30 MB"
        initComposeRuleContent(
            fileName = "my pdf.pdf",
            fileSize = fileSize,
        )
        composeRule.onRoot(useUnmergedTree = true)
        composeRule.onNodeWithTag(FILE_MESSAGE_VIEW_FILE_SIZE_TEST_TAG, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText(fileSize).assertExists()
    }

    @Test
    fun `test that file type icon show correctly`() {
        initComposeRuleContent(
            fileName = "my pdf.pdf",
            fileSize = "30 MB",
        )
        composeRule.onRoot(useUnmergedTree = true)
        composeRule.onNodeWithTag(FILE_MESSAGE_VIEW_FILE_TYPE_ICON_TEST_TAG, useUnmergedTree = true)
            .assertExists()
    }

    private fun initComposeRuleContent(
        fileName: String,
        fileSize: String,
    ) {
        composeRule.setContent {
            FileNoPreviewMessageView(
                isMe = true,
                fileName = fileName,
                fileSize = fileSize,
                fileTypeResId = R.drawable.ic_check_circle,
            )
        }
    }
}