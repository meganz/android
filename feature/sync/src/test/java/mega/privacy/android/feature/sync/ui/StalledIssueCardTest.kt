package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.sync.ui.views.StalledIssueCard
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_STALLED_ISSUE_CARD_BUTTON_MORE
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_STALLED_ISSUE_CARD_ICON_NODE_THUMBNAIL
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_STALLED_ISSUE_CARD_TEXT_CONFLICT_NAME
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_STALLED_ISSUE_CARD_TEXT_NODE_NAME
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_STALLED_ISSUE_CARD_TEXT_NODE_PATH
import mega.privacy.android.icon.pack.R as IconPackR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class StalledIssueCardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that all sync empty screen components are visible`() {
        composeTestRule.setContent {
            StalledIssueCard(
                nodeName = "test folder",
                conflictName = "Name conflict",
                nodePath = "/test/path",
                icon = IconPackR.drawable.ic_folder_medium_solid,
                moreClicked = {},
                shouldShowMoreIcon = true
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_STALLED_ISSUE_CARD_ICON_NODE_THUMBNAIL)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_STALLED_ISSUE_CARD_BUTTON_MORE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_STALLED_ISSUE_CARD_TEXT_CONFLICT_NAME)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_STALLED_ISSUE_CARD_TEXT_NODE_NAME)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_STALLED_ISSUE_CARD_TEXT_NODE_PATH)
            .assertIsDisplayed()
    }
}
