package test.mega.privacy.android.app.presentation.meeting.chat.view.message.link

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper
import mega.privacy.android.app.presentation.meeting.chat.view.message.link.FolderLinkContent
import mega.privacy.android.app.presentation.meeting.chat.view.message.link.FolderLinkMessageView
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.domain.entity.FolderInfo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderLinkMessageViewTest {
    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that folder name shows correctly`() {
        val folderName = "Folder name"
        initComposeRuleContent(
            content = FolderLinkContent(
                folderInfo = FolderInfo(
                    folderName = folderName,
                    numFolders = 0,
                    numFiles = 0,
                    currentSize = 0L,
                    numVersions = 0,
                    versionsSize = 0L,
                ),
                link = "https://mega.nz/folder/1234567890#1234567890",
            )
        )
        composeTestRule.onNodeWithText(folderName).assertExists()
    }

    @Test
    fun `test that number of folders and files shows correctly`() {
        val numFolders = 3
        val numFiles = 24
        val currentSize = 1234567890L
        initComposeRuleContent(
            content = FolderLinkContent(
                folderInfo = FolderInfo(
                    folderName = "",
                    numFolders = numFolders,
                    numFiles = numFiles,
                    currentSize = currentSize,
                    numVersions = 0,
                    versionsSize = 0L,
                ),
                link = "https://mega.nz/folder/1234567890#1234567890",
            )
        )
        val text = TextUtil.getFolderInfo(numFolders, numFiles, composeTestRule.activity) +
                "\n${FileSizeStringMapper(composeTestRule.activity)(currentSize)}"
        composeTestRule.onNodeWithText(text).assertExists()
    }

    @Test
    fun `test that authority shows correctly`() {
        val link = "https://mega.nz/folder/1234567890#1234567890"
        initComposeRuleContent(
            content = FolderLinkContent(
                folderInfo = FolderInfo(
                    folderName = "",
                    numFolders = 0,
                    numFiles = 0,
                    currentSize = 0L,
                    numVersions = 0,
                    versionsSize = 0L,
                ),
                link = link,
            )
        )
        composeTestRule.onNodeWithText(Uri.parse(link).authority.orEmpty()).assertExists()
    }

    private fun initComposeRuleContent(content: FolderLinkContent) {
        composeTestRule.setContent {
            FolderLinkMessageView(
                linkContent = content
            )
        }
    }
}