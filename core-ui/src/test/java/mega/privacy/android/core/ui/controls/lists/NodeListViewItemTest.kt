package mega.privacy.android.core.ui.controls.lists

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class NodeListViewItemTest {


    @get:Rule
    val composeTestRule = createComposeRule()


    private fun setContentWithoutAnyIcons() {
        composeTestRule.setContent {
            Scaffold {
                Text(text = "Empty screen", modifier = Modifier.padding(it))
                NodeLisViewItem(title = "Title", subtitle = "Subtitle", icon = R.drawable.ic_info)
            }
        }
    }

    private fun setContentWithAllIcons() {
        composeTestRule.setContent {
            Scaffold {
                Text(text = "Empty screen", modifier = Modifier.padding(it))
                NodeLisViewItem(
                    title = "Title",
                    subtitle = "Subtitle",
                    icon = R.drawable.ic_info,
                    accessPermissionIcon = R.drawable.ic_favorite,
                    showOffline = true,
                    showVersion = true,
                    labelColor = Color.Blue,
                    showLink = true,
                    showFavourite = true,
                    onMoreClicked = {},
                )
            }
        }
    }

    @Test
    fun `test that node list view item displays items correctly when only title and subtitle is provided`() {
        // Start the app
        setContentWithoutAnyIcons()

        // Check that the text is displayed
        composeTestRule.onNodeWithText("Empty screen").assertIsDisplayed()

        // Check that the list item title is displayed
        composeTestRule.onNodeWithText("Title").assertIsDisplayed()

        // Check that the list item sub title is displayed
        composeTestRule.onNodeWithText("Subtitle").assertIsDisplayed()

        //Check that label is not displayed
        composeTestRule.onNodeWithTag(LABEL_TAG).assertDoesNotExist()

        //Check that offline icon is not displayed
        composeTestRule.onNodeWithTag(OFFLINE_ICON_TAG).assertDoesNotExist()

        //Check that version icon is not displayed
        composeTestRule.onNodeWithTag(VERSION_ICON_TAG).assertDoesNotExist()

        //Check that favourite icon is not displayed
        composeTestRule.onNodeWithTag(FAVOURITE_ICON_TAG).assertDoesNotExist()

        //Check that link icon is not displayed
        composeTestRule.onNodeWithTag(LINK_ICON_TAG).assertDoesNotExist()

        //Check that permission icon is not displayed
        composeTestRule.onNodeWithTag(PERMISSION_ICON_TAG).assertDoesNotExist()

    }

    @Test
    fun `test that  in node list view item displays all icons when correct inputs are provided`() {
        // Start the app
        setContentWithAllIcons()

        // Check that the text is displayed
        composeTestRule.onNodeWithText("Empty screen").assertIsDisplayed()

        // Check that the list item title is displayed
        composeTestRule.onNodeWithText("Title").assertIsDisplayed()

        // Check that the list item sub title is displayed
        composeTestRule.onNodeWithText("Subtitle").assertIsDisplayed()

        //Check that label is displayed
        composeTestRule.onNodeWithTag(LABEL_TAG).assertIsDisplayed()

        //Check that offline icon is displayed
        composeTestRule.onNodeWithTag(OFFLINE_ICON_TAG).assertIsDisplayed()

        //Check that version icon is displayed
        composeTestRule.onNodeWithTag(VERSION_ICON_TAG).assertIsDisplayed()

        //Check that favourite icon is displayed
        composeTestRule.onNodeWithTag(FAVOURITE_ICON_TAG).assertIsDisplayed()

        //Check that link icon is displayed
        composeTestRule.onNodeWithTag(LINK_ICON_TAG).assertIsDisplayed()

        //Check that permission icon is displayed
        composeTestRule.onNodeWithTag(PERMISSION_ICON_TAG).assertIsDisplayed()

    }
}