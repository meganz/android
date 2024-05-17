package mega.privacy.android.shared.original.core.ui.controls.lists

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class RecentActionListViewItemTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val firstLineText = "First line text"
    private val parentFolderName = "Parent folder"
    private val time = "12:00 PM"
    private val icon = IconPackR.drawable.ic_folder_incoming_medium_solid
    private val actionIcon = IconPackR.drawable.ic_arrow_ascending_medium_regular_outline
    private val shareIcon = IconPackR.drawable.ic_share_network_medium_regular_outline

    @Test
    fun `test that recent action list item is displayed correctly when all information provided`() {
        composeRule.setContent {
            RecentActionListViewItem(
                firstLineText = firstLineText,
                icon = icon,
                parentFolderName = parentFolderName,
                time = time,
                actionIcon = actionIcon,
                onItemClick = {},
                onMenuClick = {}
            )
        }

        composeRule.onNodeWithTag(FIRST_LINE_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(FOLDER_NAME_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(TIME_TEST_TAG, true).assertTextEquals(time)
        composeRule.onNodeWithTag(ICON_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(ACTION_ICON_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(MENU_TEST_TAG, true).assertExists()
    }

    @Test
    fun `test that favorite icon is displayed when isFavorite is true`() {
        composeRule.setContent {
            RecentActionListViewItem(
                firstLineText = firstLineText,
                icon = icon,
                parentFolderName = parentFolderName,
                time = time,
                actionIcon = actionIcon,
                isFavourite = true,
                onItemClick = {},
                onMenuClick = {}
            )
        }

        composeRule.onNodeWithTag(FAVORITE_TEST_TAG, true).assertExists()
    }

    @Test
    fun `test that share icon is displayed when shareIcon is provided`() {
        composeRule.setContent {
            RecentActionListViewItem(
                firstLineText = firstLineText,
                icon = icon,
                parentFolderName = parentFolderName,
                time = time,
                actionIcon = actionIcon,
                shareIcon = shareIcon,
                onItemClick = {},
                onMenuClick = {}
            )
        }

        composeRule.onNodeWithTag(SHARES_ICON_TEST_TAG, true).assertExists()
    }

    @Test
    fun `test that menu button is not displayed when showMenuButton is false`() {
        composeRule.setContent {
            RecentActionListViewItem(
                firstLineText = firstLineText,
                icon = icon,
                parentFolderName = parentFolderName,
                time = time,
                actionIcon = actionIcon,
                showMenuButton = false,
                onItemClick = {},
                onMenuClick = {}
            )
        }

        composeRule.onNodeWithTag(MENU_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that view is displayed with default values when some parameters are not provided`() {
        composeRule.setContent {
            RecentActionListViewItem(
                firstLineText = firstLineText,
                onItemClick = {},
                onMenuClick = {},
                time = time,
                parentFolderName = parentFolderName,
                actionIcon = actionIcon
            )
        }

        composeRule.onNodeWithTag(ICON_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(ACTION_ICON_TEST_TAG, true).assertExists()
        composeRule.onNodeWithTag(MENU_TEST_TAG, true).assertExists()
    }
}
