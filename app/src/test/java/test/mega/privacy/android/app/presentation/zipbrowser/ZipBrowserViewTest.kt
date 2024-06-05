package test.mega.privacy.android.app.presentation.zipbrowser

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.zipbrowser.model.ZipInfoUiEntity
import mega.privacy.android.app.presentation.zipbrowser.view.ZIP_BROWSER_ITEM_DIVIDER_TEST_TAG
import mega.privacy.android.app.presentation.zipbrowser.view.ZIP_BROWSER_LIST_TEST_TAG
import mega.privacy.android.app.presentation.zipbrowser.view.ZIP_BROWSER_TOP_BAR_TEST_TAG
import mega.privacy.android.app.presentation.zipbrowser.view.ZipBrowserView
import mega.privacy.android.domain.entity.zipbrowser.ZipEntryType
import mega.privacy.android.icon.pack.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZipBrowserViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testParentFolderName = "ZIP zipFolder"

    private fun setComposeContent(
        items: List<ZipInfoUiEntity> = emptyList(),
        parentFolderName: String = testParentFolderName,
        folderDepth: Int = 0,
        modifier: Modifier = Modifier,
        onItemClicked: (ZipInfoUiEntity) -> Unit = {},
        onBackPressed: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ZipBrowserView(
                items = items,
                parentFolderName = parentFolderName,
                folderDepth = folderDepth,
                modifier = modifier,
                onItemClicked = onItemClicked,
                onBackPressed = onBackPressed
            )
        }
    }

    @Test
    fun `test that the UIs are displayed correctly when the items are empty`() {
        setComposeContent()

        composeTestRule.onNodeWithTag(ZIP_BROWSER_TOP_BAR_TEST_TAG, true).assertIsDisplayed()
        composeTestRule.onNodeWithText(text = testParentFolderName, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(ZIP_BROWSER_LIST_TEST_TAG, true).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(ZIP_BROWSER_ITEM_DIVIDER_TEST_TAG, true)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that the UIs are displayed correctly when the items are not empty`() {
        val fileName = "zip file.txt"
        val folderName = "zip folder"
        val fileInfo = "100 MB"
        val folderInfo = "10 files"
        val testItems = listOf(
            ZipInfoUiEntity(
                icon = R.drawable.ic_folder_medium_solid,
                name = folderName,
                path = "zipFolder/",
                info = folderInfo,
                zipEntryType = ZipEntryType.Folder
            ),
            ZipInfoUiEntity(
                icon = R.drawable.ic_text_medium_solid,
                name = fileName,
                path = "zipFolder/zip file.txt",
                info = fileInfo,
                zipEntryType = ZipEntryType.File
            )
        )
        setComposeContent(items = testItems)

        composeTestRule.onNodeWithTag(ZIP_BROWSER_TOP_BAR_TEST_TAG, true).assertIsDisplayed()
        composeTestRule.onNodeWithText(text = testParentFolderName, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(ZIP_BROWSER_LIST_TEST_TAG, true).assertIsDisplayed()
        composeTestRule.onNodeWithText(text = folderName, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(text = folderInfo, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(text = fileName, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(text = fileInfo, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(ZIP_BROWSER_ITEM_DIVIDER_TEST_TAG + "0", true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(ZIP_BROWSER_ITEM_DIVIDER_TEST_TAG + "1", true)
            .assertIsDisplayed()
    }


}