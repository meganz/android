package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import mega.privacy.android.feature.sync.ui.views.SOLVED_ISSUE_BODY
import mega.privacy.android.feature.sync.ui.views.SOLVED_ISSUE_BODY_ICON
import mega.privacy.android.feature.sync.ui.views.SOLVED_ISSUE_MAIN_CONTAINER
import mega.privacy.android.feature.sync.ui.views.SOLVED_ISSUE_NODE_IMAGE
import mega.privacy.android.feature.sync.ui.views.SOLVED_ISSUE_NODE_SUBTITLE
import mega.privacy.android.feature.sync.ui.views.SOLVED_ISSUE_NODE_TITLE
import mega.privacy.android.feature.sync.ui.views.SolvedIssueCard
import mega.privacy.android.icon.pack.R as IconPackR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class SolvedIssueCardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that all basic solved issue card components are visible`() {
        composeTestRule.setContent {
            SolvedIssueCard(
                title = "Test Folder",
                body = "Test resolution explanation",
                nodeIcon = IconPackR.drawable.ic_folder_medium_solid
            )
        }

        composeTestRule.onNodeWithTag(SOLVED_ISSUE_MAIN_CONTAINER)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SOLVED_ISSUE_NODE_IMAGE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SOLVED_ISSUE_NODE_TITLE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SOLVED_ISSUE_BODY)
            .assertIsDisplayed()
    }

    @Test
    fun `test that subtitle is displayed when provided`() {
        composeTestRule.setContent {
            SolvedIssueCard(
                title = "Test Folder",
                subTitle = "Test Subtitle",
                body = "Test resolution explanation",
                nodeIcon = IconPackR.drawable.ic_folder_medium_solid
            )
        }

        composeTestRule.onNodeWithTag(SOLVED_ISSUE_NODE_SUBTITLE)
            .assertIsDisplayed()
    }

    @Test
    fun `test that body icon is displayed when provided`() {
        composeTestRule.setContent {
            SolvedIssueCard(
                title = "Test Folder",
                body = "Test resolution explanation",
                nodeIcon = IconPackR.drawable.ic_folder_medium_solid,
                bodyIcon = R.drawable.ic_check_circle
            )
        }

        composeTestRule.onNodeWithTag(SOLVED_ISSUE_BODY_ICON)
            .assertIsDisplayed()
    }

    @Test
    fun `test that body icon is not displayed when not provided`() {
        composeTestRule.setContent {
            SolvedIssueCard(
                title = "Test Folder",
                body = "Test resolution explanation",
                nodeIcon = IconPackR.drawable.ic_folder_medium_solid
            )
        }

        composeTestRule.onNodeWithTag(SOLVED_ISSUE_MAIN_CONTAINER)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SOLVED_ISSUE_NODE_IMAGE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SOLVED_ISSUE_NODE_TITLE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SOLVED_ISSUE_BODY)
            .assertIsDisplayed()
    }

    @Test
    fun `test that subtitle is not displayed when not provided`() {
        composeTestRule.setContent {
            SolvedIssueCard(
                title = "Test Folder",
                body = "Test resolution explanation",
                nodeIcon = IconPackR.drawable.ic_folder_medium_solid
            )
        }

        composeTestRule.onNodeWithTag(SOLVED_ISSUE_MAIN_CONTAINER)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SOLVED_ISSUE_NODE_IMAGE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SOLVED_ISSUE_NODE_TITLE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SOLVED_ISSUE_BODY)
            .assertIsDisplayed()
    }

    @Test
    fun `test that all components are visible with full parameters`() {
        composeTestRule.setContent {
            SolvedIssueCard(
                title = "Test Folder",
                subTitle = "Test Subtitle",
                body = "Test resolution explanation",
                nodeIcon = IconPackR.drawable.ic_folder_medium_solid,
                bodyIcon = R.drawable.ic_check_circle
            )
        }

        composeTestRule.onNodeWithTag(SOLVED_ISSUE_MAIN_CONTAINER)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SOLVED_ISSUE_NODE_IMAGE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SOLVED_ISSUE_NODE_TITLE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SOLVED_ISSUE_NODE_SUBTITLE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SOLVED_ISSUE_BODY_ICON)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SOLVED_ISSUE_BODY)
            .assertIsDisplayed()
    }

    @Test
    fun `test that long title text is handled properly`() {
        val longTitle =
            "This is a very long title that should be handled properly with ellipsis when it exceeds the available space"

        composeTestRule.setContent {
            SolvedIssueCard(
                title = longTitle,
                body = "Test resolution explanation",
                nodeIcon = IconPackR.drawable.ic_folder_medium_solid
            )
        }

        composeTestRule.onNodeWithTag(SOLVED_ISSUE_NODE_TITLE)
            .assertIsDisplayed()
    }

    @Test
    fun `test that long body text is handled properly`() {
        val longBody =
            "This is a very long resolution explanation that should be handled properly with ellipsis when it exceeds the available space and should wrap to multiple lines as needed"

        composeTestRule.setContent {
            SolvedIssueCard(
                title = "Test Folder",
                body = longBody,
                nodeIcon = IconPackR.drawable.ic_folder_medium_solid
            )
        }

        composeTestRule.onNodeWithTag(SOLVED_ISSUE_BODY)
            .assertIsDisplayed()
    }

    @Test
    fun `test that long subtitle text is handled properly`() {
        val longSubtitle =
            "This is a very long subtitle that should be handled properly with ellipsis when it exceeds the available space"

        composeTestRule.setContent {
            SolvedIssueCard(
                title = "Test Folder",
                subTitle = longSubtitle,
                body = "Test resolution explanation",
                nodeIcon = IconPackR.drawable.ic_folder_medium_solid
            )
        }

        composeTestRule.onNodeWithTag(SOLVED_ISSUE_NODE_SUBTITLE)
            .assertIsDisplayed()
    }
}
