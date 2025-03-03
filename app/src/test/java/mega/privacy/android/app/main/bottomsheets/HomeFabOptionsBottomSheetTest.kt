package mega.privacy.android.app.main.bottomsheets

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.core.test.AnalyticsTestRule
import mega.privacy.android.shared.resources.R as sharedResR
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import mega.privacy.mobile.analytics.event.HomeNewTextFileMenuToolbarEvent
import mega.privacy.mobile.analytics.event.HomeUploadFilesMenuToolbarEvent
import mega.privacy.mobile.analytics.event.HomeUploadFolderMenuToolbarEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1080dp-h1920dp")
class HomeFabOptionsBottomSheetTest {

    var composeRule = createComposeRule()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeRule)

    @Test
    fun `test that all items are displayed`() {
        initComposeRule()
        composeRule.onNodeWithText(R.string.context_upload).assertExists().assertIsDisplayed()
        composeRule.onNodeWithText(R.string.upload_files).assertExists().assertIsDisplayed()
        composeRule.onNodeWithText(R.string.upload_folder).assertExists().assertIsDisplayed()
        composeRule.onNodeWithText(R.string.menu_scan_document).assertExists().assertIsDisplayed()
        composeRule.onNodeWithText(R.string.menu_take_picture).assertExists().assertIsDisplayed()
        composeRule.onNodeWithText(R.string.action_create_txt).assertExists().assertIsDisplayed()
        composeRule.onNodeWithText(sharedResR.string.settings_section_sync).assertExists()
            .assertIsDisplayed()
        composeRule.onNodeWithText(sharedResR.string.device_center_sync_add_new_syn_button_option)
            .assertExists().assertIsDisplayed()
        composeRule.onNodeWithText(sharedResR.string.device_center_sync_add_new_backup_button_option)
            .assertExists().assertIsDisplayed()
        composeRule.onNodeWithText(R.string.section_chat).assertExists().assertIsDisplayed()
        composeRule.onNodeWithText(R.string.fab_label_new_chat).assertExists().assertIsDisplayed()
    }

    @Test
    fun `test that click the Upload files option sends the right analytics tracker event`() {
        initComposeRule()
        composeRule.onNodeWithText(R.string.upload_files).assertExists().assertIsDisplayed()
            .performClick()
        assertThat(analyticsRule.events).contains(HomeUploadFilesMenuToolbarEvent)
    }

    @Test
    fun `test that click the Upload folder option sends the right analytics tracker event`() {
        initComposeRule()
        composeRule.onNodeWithText(R.string.upload_folder).assertExists().assertIsDisplayed()
            .performClick()
        assertThat(analyticsRule.events).contains(HomeUploadFolderMenuToolbarEvent)
    }

    @Test
    fun `test that click the Create new text file option sends the right analytics tracker event`() {
        initComposeRule()
        composeRule.onNodeWithText(R.string.action_create_txt).assertExists().assertIsDisplayed()
            .performClick()
        assertThat(analyticsRule.events).contains(HomeNewTextFileMenuToolbarEvent)
    }

    private fun initComposeRule() {
        composeRule.setContent {
            HomeFabOptionsBottomSheet(
                onUploadFilesClicked = {},
                onUploadFolderClicked = {},
                onScanDocumentClicked = {},
                onCaptureClicked = {},
                onCreateNewTextFileClicked = {},
                onAddNewSyncClicked = {},
                onAddNewBackupClicked = {},
                onNewChatClicked = {},
            )
        }
    }
}