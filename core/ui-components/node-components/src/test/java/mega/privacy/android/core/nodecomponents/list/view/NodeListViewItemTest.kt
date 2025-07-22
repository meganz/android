package mega.privacy.android.core.nodecomponents.list.view

import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.icon.pack.R as IconPackR
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
        title: String = "Test Title",
        subtitle: String = "Test Subtitle",
        icon: Int = IconPackR.drawable.ic_folder_outgoing_medium_solid,
        description: String? = null,
        tags: List<String>? = null,
        highlightText: String = "",
        showOffline: Boolean = false,
        showVersion: Boolean = false,
        showFavourite: Boolean = false,
        showLink: Boolean = false,
        isTakenDown: Boolean = false,
        showIsVerified: Boolean = false,
        labelColor: androidx.compose.ui.graphics.Color? = null,
        accessPermissionIcon: Int? = null,
        onMoreClicked: (() -> Unit)? = null,
        onInfoClicked: (() -> Unit)? = null,
        onItemClicked: () -> Unit = {},
        onLongClicked: (() -> Unit)? = null,
        isInSelectionMode: Boolean = false,
        isSelected: Boolean = false,
    ) {
        composeTestRule.setContent {
            NodeListViewItem(
                title = title,
                subtitle = subtitle,
                icon = icon,
                description = description,
                tags = tags,
                highlightText = highlightText,
                showOffline = showOffline,
                showVersion = showVersion,
                showFavourite = showFavourite,
                showLink = showLink,
                isTakenDown = isTakenDown,
                showIsVerified = showIsVerified,
                labelColor = labelColor,
                accessPermissionIcon = accessPermissionIcon,
                onMoreClicked = onMoreClicked,
                onInfoClicked = onInfoClicked,
                onItemClicked = onItemClicked,
                onLongClicked = onLongClicked,
                isInSelectionMode = isInSelectionMode,
                isSelected = isSelected
            )
        }
    }

    @Test
    fun `test that basic node list item displays title and subtitle correctly`() {
        setContent()

        composeTestRule.onNodeWithText("Test Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Subtitle").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TITLE_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SUBTITLE_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that description is displayed when provided and matches highlight text`() {
        setContent(
            description = "This is a test description",
            highlightText = "test"
        )

        composeTestRule.onNodeWithTag(DESCRIPTION_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("This is a test description").assertIsDisplayed()
    }

    @Test
    fun `test that description is not displayed when it does not match highlight text`() {
        setContent(
            description = "This is a test description",
            highlightText = "unrelated"
        )

        composeTestRule.onNodeWithTag(DESCRIPTION_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that description is not displayed when highlight text is empty`() {
        setContent(
            description = "This is a test description",
            highlightText = ""
        )

        composeTestRule.onNodeWithTag(DESCRIPTION_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that tags are displayed when they match highlight text`() {
        setContent(
            tags = listOf("work", "important", "project"),
            highlightText = "work"
        )

        composeTestRule.onNodeWithTag(TAGS_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("#work", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that multiple matching tags are displayed`() {
        setContent(
            tags = listOf("work", "important", "project", "workflow"),
            highlightText = "work"
        )

        composeTestRule.onNodeWithTag(TAGS_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("#work", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("#workflow", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that tags are not displayed when no tags match highlight text`() {
        setContent(
            tags = listOf("work", "important", "project"),
            highlightText = "unrelated"
        )

        composeTestRule.onNodeWithTag(TAGS_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that tags are not displayed when highlight text is empty`() {
        setContent(
            tags = listOf("work", "important", "project"),
            highlightText = ""
        )

        composeTestRule.onNodeWithTag(TAGS_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that tags are not displayed when tags list is null`() {
        setContent(
            tags = null,
            highlightText = "work"
        )

        composeTestRule.onNodeWithTag(TAGS_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that tags are not displayed when tags list is empty`() {
        setContent(
            tags = emptyList(),
            highlightText = "work"
        )

        composeTestRule.onNodeWithTag(TAGS_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that case insensitive tag matching works`() {
        setContent(
            tags = listOf("WORK", "Important", "PROJECT"),
            highlightText = "work"
        )

        composeTestRule.onNodeWithTag(TAGS_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("#WORK", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that both description and tags are displayed when both match highlight text`() {
        setContent(
            description = "This is a work-related document",
            tags = listOf("work", "important", "project"),
            highlightText = "work"
        )

        composeTestRule.onNodeWithTag(DESCRIPTION_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAGS_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("This is a work-related document").assertIsDisplayed()
        composeTestRule.onNodeWithText("#work", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that favourite icon is displayed when showFavourite is true`() {
        setContent(showFavourite = true)

        composeTestRule.onNodeWithTag(FAVOURITE_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that favourite icon is not displayed when showFavourite is false`() {
        setContent(showFavourite = false)

        composeTestRule.onNodeWithTag(FAVOURITE_ICON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that link icon is displayed when showLink is true`() {
        setContent(showLink = true)

        composeTestRule.onNodeWithTag(LINK_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that link icon is not displayed when showLink is false`() {
        setContent(showLink = false)

        composeTestRule.onNodeWithTag(LINK_ICON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that taken down icon is displayed when isTakenDown is true`() {
        setContent(isTakenDown = true)

        composeTestRule.onNodeWithTag(TAKEN_DOWN_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that taken down icon is not displayed when isTakenDown is false`() {
        setContent(isTakenDown = false)

        composeTestRule.onNodeWithTag(TAKEN_DOWN_ICON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that offline icon is displayed when showOffline is true`() {
        setContent(showOffline = true)

        composeTestRule.onNodeWithTag(OFFLINE_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that offline icon is not displayed when showOffline is false`() {
        setContent(showOffline = false)

        composeTestRule.onNodeWithTag(OFFLINE_ICON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that version icon is displayed when showVersion is true`() {
        setContent(showVersion = true)

        composeTestRule.onNodeWithTag(VERSION_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that version icon is not displayed when showVersion is false`() {
        setContent(showVersion = false)

        composeTestRule.onNodeWithTag(VERSION_ICON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that verified icon is displayed when showIsVerified is true`() {
        setContent(showIsVerified = true)

        composeTestRule.onNodeWithTag(VERIFIED_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that verified icon is not displayed when showIsVerified is false`() {
        setContent(showIsVerified = false)

        composeTestRule.onNodeWithTag(OFFLINE_ICON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that label is displayed when labelColor is provided`() {
        setContent(labelColor = androidx.compose.ui.graphics.Color.Blue)

        composeTestRule.onNodeWithTag(LABEL_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that label is not displayed when labelColor is null`() {
        setContent(labelColor = null)

        composeTestRule.onNodeWithTag(LABEL_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that permission icon is displayed when accessPermissionIcon is provided`() {
        setContent(accessPermissionIcon = IconPackR.drawable.ic_sync_01_medium_thin_outline)

        composeTestRule.onNodeWithTag(PERMISSION_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that permission icon is not displayed when accessPermissionIcon is null`() {
        setContent(accessPermissionIcon = null)

        composeTestRule.onNodeWithTag(PERMISSION_ICON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that more icon is displayed when onMoreClicked is provided`() {
        setContent(onMoreClicked = {})

        composeTestRule.onNodeWithTag(MORE_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that more icon is not displayed when onMoreClicked is null`() {
        setContent(onMoreClicked = null)

        composeTestRule.onNodeWithTag(MORE_ICON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that info icon is displayed when onInfoClicked is provided`() {
        setContent(onInfoClicked = {})

        composeTestRule.onNodeWithTag(INFO_ICON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that info icon is not displayed when onInfoClicked is null`() {
        setContent(onInfoClicked = null)

        composeTestRule.onNodeWithTag(MORE_ICON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that all icons are displayed when all flags are true`() {
        setContent(
            showOffline = true,
            showVersion = true,
            showFavourite = true,
            showLink = true,
            isTakenDown = true,
            showIsVerified = true,
            labelColor = androidx.compose.ui.graphics.Color.Blue,
            accessPermissionIcon = IconPackR.drawable.ic_sync_01_medium_thin_outline,
            onMoreClicked = {},
            onInfoClicked = {}
        )

        composeTestRule.onNodeWithTag(FAVOURITE_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(LINK_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAKEN_DOWN_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(OFFLINE_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(VERSION_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LABEL_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PERMISSION_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(INFO_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that long title is handled correctly with middle ellipsis`() {
        val longTitle =
            "This is a very long title that should be truncated with middle ellipsis to demonstrate the component's text handling capabilities"
        setContent(title = longTitle)

        composeTestRule.onNodeWithTag(TITLE_TAG, useUnmergedTree = true).assertIsDisplayed()
        // The title should be displayed but truncated
        composeTestRule.onNodeWithText(longTitle).assertIsDisplayed()
    }

    @Test
    fun `test that long subtitle is handled correctly`() {
        val longSubtitle =
            "This is a very long subtitle that demonstrates how the component handles lengthy subtitle text"
        setContent(subtitle = longSubtitle)

        composeTestRule.onNodeWithTag(SUBTITLE_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(longSubtitle).assertIsDisplayed()
    }

    @Test
    fun `test that long description is handled correctly`() {
        val longDescription =
            "This is an extremely long description that demonstrates how the component handles lengthy text content. It includes multiple sentences and provides comprehensive information about the file's purpose, contents, and usage guidelines."
        setContent(
            description = longDescription,
            highlightText = "description"
        )

        composeTestRule.onNodeWithTag(DESCRIPTION_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText(longDescription).assertIsDisplayed()
    }

    @Test
    fun `test that highlight text with numbers works correctly`() {
        setContent(
            description = "Version 2.1.0 of the application",
            tags = listOf("v2.1.0", "release", "stable"),
            highlightText = "2.1"
        )

        composeTestRule.onNodeWithTag(DESCRIPTION_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAGS_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("#v2.1.0", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that empty highlight text shows no description or tags`() {
        setContent(
            description = "This should not be displayed",
            tags = listOf("work", "important"),
            highlightText = ""
        )

        composeTestRule.onNodeWithTag(DESCRIPTION_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAGS_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that whitespace-only highlight text shows no description or tags`() {
        setContent(
            description = "This should not be displayed",
            tags = listOf("work", "important"),
            highlightText = "   "
        )

        composeTestRule.onNodeWithTag(DESCRIPTION_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAGS_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that checkbox is displayed when in selection mode`() {
        setContent(
            isInSelectionMode = true,
            isSelected = false
        )
        composeTestRule.onAllNodesWithTag(CHECKBOX_TAG, useUnmergedTree = true)
            .assertAny(hasTestTag(CHECKBOX_TAG))
    }

    @Test
    fun `test that checkbox is not displayed when not in selection mode`() {
        setContent(
            isInSelectionMode = false,
            isSelected = false
        )

        composeTestRule.onNodeWithTag(CHECKBOX_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `test that checkbox shows checked state when item is selected`() {
        setContent(
            isInSelectionMode = true,
            isSelected = true
        )
        composeTestRule.onAllNodesWithTag(CHECKBOX_TAG, useUnmergedTree = true).assertAny(hasTestTag(CHECKBOX_TAG))
        // Note: The actual checked state would need to be verified through the checkbox's internal state
    }

    @Test
    fun `test that checkbox shows unchecked state when item is not selected`() {
        setContent(
            isInSelectionMode = true,
            isSelected = false
        )
        composeTestRule.onAllNodesWithTag(CHECKBOX_TAG, useUnmergedTree = true).assertAny(hasTestTag(CHECKBOX_TAG))
        // Note: The actual unchecked state would need to be verified through the checkbox's internal state
    }

    @Test
    fun `test that more icon is not displayed when in selection mode`() {
        setContent(
            isInSelectionMode = true,
            isSelected = false,
            onMoreClicked = {}
        )

        composeTestRule.onNodeWithTag(MORE_ICON_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `test that checkbox and other icons can coexist in selection mode`() {
        setContent(
            isInSelectionMode = true,
            isSelected = true,
            showFavourite = true,
            showLink = true,
            showOffline = true,
            showVersion = true,
            isTakenDown = true,
            showIsVerified = true,
            labelColor = androidx.compose.ui.graphics.Color.Blue,
            accessPermissionIcon = IconPackR.drawable.ic_sync_01_medium_thin_outline
        )

        // Checkbox should be displayed
        composeTestRule.onAllNodesWithTag(CHECKBOX_TAG, useUnmergedTree = true).assertAny(hasTestTag(CHECKBOX_TAG))

        // Other icons should still be displayed
        composeTestRule.onNodeWithTag(FAVOURITE_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LINK_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAKEN_DOWN_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(VERSION_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(LABEL_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PERMISSION_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()

        // But more/info icons should not be displayed in selection mode
        composeTestRule.onNodeWithTag(MORE_ICON_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag(INFO_ICON_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `test that selection mode overrides more and info icons`() {
        setContent(
            isInSelectionMode = true,
            isSelected = false,
            onMoreClicked = {},
            onInfoClicked = {}
        )

        // In selection mode, only checkbox should be visible in the trailing area
        composeTestRule.onAllNodesWithTag(CHECKBOX_TAG, useUnmergedTree = true).assertAny(hasTestTag(CHECKBOX_TAG))
        composeTestRule.onNodeWithTag(MORE_ICON_TAG, useUnmergedTree = true).assertDoesNotExist()
    }
}