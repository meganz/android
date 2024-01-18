package test.mega.privacy.android.app.presentation.meeting.chat.view.message.link

import androidx.activity.ComponentActivity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.meeting.chat.view.message.link.FileLinkMessageView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileLinkMessageViewTest {
    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that text shows correctly when link is valid`() {
        initComposeRuleContent(
            fileName = "File name",
            fileSize = 1024,
            link = "https://mega.nz/file/1234567890#1234567890",
        )
        composeTestRule.onNodeWithText("File name").assertExists()
        composeTestRule.onNodeWithText(FileSizeStringMapper(composeTestRule.activity)(1024))
            .assertExists()
        composeTestRule.onNodeWithText("mega.nz").assertExists()
    }

    private fun initComposeRuleContent(
        fileName: String,
        fileSize: Long,
        link: String,
    ) {
        composeTestRule.setContent {
            FileLinkMessageView(
                fileIcon = painterResource(id = R.drawable.ic_rich_link),
                fileName = fileName,
                fileSize = fileSize,
                link = link,
            )
        }
    }
}