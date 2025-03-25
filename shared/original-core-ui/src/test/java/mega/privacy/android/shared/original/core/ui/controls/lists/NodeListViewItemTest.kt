package mega.privacy.android.shared.original.core.ui.controls.lists

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class NodeListViewItemTest {


    @get:Rule
    val composeTestRule = createComposeRule()


    private fun setContent(
        title: String = "Title",
        subtitle: String = "Subtitle",
        icon: Int = R.drawable.ic_info,
        accessPermissionIcon: Int? = null,
        showOffline: Boolean = false,
        showVersion: Boolean = false,
        showLink: Boolean = false,
        isTakenDown: Boolean = false,
        showFavourite: Boolean = false,
        onMoreClicked: (() -> Unit)? = null,
        description: String? = null,
        highlightText: String = "",
        tags: List<String>? = null,
    ) {
        composeTestRule.setContent {
            Scaffold {
                Text(text = "Empty screen", modifier = Modifier.padding(it))
                NodeListViewItem(
                    title = title,
                    subtitle = subtitle,
                    icon = icon,
                    accessPermissionIcon = accessPermissionIcon,
                    showOffline = showOffline,
                    showVersion = showVersion,
                    labelColor = MegaOriginalTheme.colors.indicator.pink,
                    showLink = showLink,
                    isTakenDown = isTakenDown,
                    showFavourite = showFavourite,
                    onMoreClicked = onMoreClicked,
                    description = description,
                    highlightText = highlightText,
                    tags = tags
                )
            }
        }
    }

    @Test
    fun `test that node list view item displays items correctly when only title and subtitle is provided`() {
        // Start the app
        setContent()

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
        setContent(accessPermissionIcon = R.drawable.ic_favorite,
            showOffline = true,
            showVersion = true,
            showLink = true,
            isTakenDown = true,
            showFavourite = true,
            onMoreClicked = {})

        // Check that the text is displayed
        composeTestRule.onNodeWithText("Empty screen").assertIsDisplayed()

        // Check that the list item title is displayed
        composeTestRule.onNodeWithText("Title").assertIsDisplayed()

        // Check that the list item sub title is displayed
        composeTestRule.onNodeWithText("Subtitle").assertIsDisplayed()

        //Check that label is displayed
        composeTestRule.onNodeWithTag(LABEL_TAG, useUnmergedTree = true).assertIsDisplayed()

        //Check that offline icon is displayed
        composeTestRule.onNodeWithTag(OFFLINE_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()

        //Check that version icon is displayed
        composeTestRule.onNodeWithTag(VERSION_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()

        //Check that favourite icon is displayed
        composeTestRule.onNodeWithTag(FAVOURITE_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()

        //Check that link icon is displayed
        composeTestRule.onNodeWithTag(LINK_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()

        //Check that permission icon is displayed
        composeTestRule.onNodeWithTag(PERMISSION_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()

        //Check that taken down icon is displayed
        composeTestRule.onNodeWithTag(TAKEN_DOWN_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()

    }

    @Test
    fun `test that node description is displayed when it matches search query`() {
        setContent(
            description = "Camera Uploads",
            highlightText = "camera",
            tags = listOf("camerauploads", "myuploads")
        )
        composeTestRule.onNodeWithTag(DESCRIPTION_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that node description is not displayed when it does not match search query`() {
        setContent(
            description = "Camera Uploads",
            highlightText = "video",
            tags = listOf("camerauploads", "myuploads")
        )
        composeTestRule.onNodeWithTag(DESCRIPTION_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `test that node tags are displayed when they match search query`() {
        setContent(
            description = "Camera Uploads",
            highlightText = "camerauploads",
            tags = listOf("camerauploads", "myuploads")
        )
        composeTestRule.onNodeWithTag(TAGS_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that node tags are not displayed when they do not match search query`() {
        setContent(
            description = "CameraUploads",
            highlightText = "video",
            tags = listOf("camerauploads", "myuploads")
        )
        composeTestRule.onNodeWithTag(TAGS_TAG, useUnmergedTree = true).assertDoesNotExist()
    }
}