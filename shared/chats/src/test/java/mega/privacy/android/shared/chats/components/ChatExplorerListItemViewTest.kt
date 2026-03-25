package mega.privacy.android.shared.chats.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.icon.pack.IconPack
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ChatExplorerListItemViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testPeerPrimary = Color(0xFF6200EE)
    private val testPeerSecondary = Color(0xFF00D5E2)

    private fun setContent(
        title: String? = "Test Title",
        subtitle: String? = null,
        isSelected: Boolean = false,
        isEnabled: Boolean = true,
        hasAvatarIcon: Boolean = false,
        isHint: Boolean = false,
        avatarColor: Color? = testPeerPrimary,
        avatarSecondaryColor: Color? = null,
        contactAvatarFile: File? = null,
        icon: ImageVector? = null,
        onItemClicked: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AndroidThemeForPreviews {
                ChatExplorerListItemView(
                    isSelected = isSelected,
                    isEnabled = isEnabled,
                    title = title,
                    subtitle = subtitle,
                    isHint = isHint,
                    hasAvatarIcon = hasAvatarIcon,
                    contactAvatarFile = contactAvatarFile,
                    icon = icon,
                    avatarColor = avatarColor,
                    avatarSecondaryColor = avatarSecondaryColor,
                    onItemClicked = onItemClicked,
                )
            }
        }
    }

    @Test
    fun `test that chat list item displays title correctly`() {
        setContent()

        composeTestRule.onNodeWithTag(TITLE_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that basic chat list item displays subtitle correctly`() {
        setContent(subtitle = "subtitle")

        composeTestRule.onNodeWithTag(SUBTITLE_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that title is not shown when null`() {
        setContent(title = null)
        composeTestRule.onNodeWithTag(TITLE_TAG, useUnmergedTree = true).assertIsNotDisplayed()
    }

    @Test
    fun `test that subtitle is not shown when null`() {
        setContent(subtitle = null)
        composeTestRule.onNodeWithTag(SUBTITLE_TAG, useUnmergedTree = true).assertIsNotDisplayed()
    }

    @Test
    fun `test that note to self hint icon is displayed when isHint is true`() {
        setContent(
            isHint = true,
            icon = IconPack.Medium.Thin.Outline.FileText,
        )

        composeTestRule.onNodeWithTag(NOTE_TO_SELF_HINT_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that leading profile icon is displayed for group or meeting style row`() {
        setContent(
            icon = IconPack.Medium.Thin.Solid.Video,
            hasAvatarIcon = true,
        )

        composeTestRule.onNodeWithTag(MEETING_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that leading profile icon is displayed for note to self non hint row`() {
        setContent(
            isHint = false,
            hasAvatarIcon = true,
            icon = IconPack.Medium.Thin.Outline.FileText,
        )

        composeTestRule.onNodeWithTag(MEETING_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that contact avatar is displayed for peer style row`() {
        setContent(
            hasAvatarIcon = false,
            isHint = false,
            avatarColor = Color(0xFFFF8989),
            avatarSecondaryColor = Color(0xFFFF5252),
        )

        composeTestRule.onNodeWithTag(CONTACT_AVATAR_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that isHint true with null icon does not show hint icon tag`() {
        setContent(isHint = true, icon = null)
        composeTestRule.onNodeWithTag(NOTE_TO_SELF_HINT_ICON_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that hasAvatarIcon true with null icon does not show leading profile icon`() {
        setContent(
            hasAvatarIcon = true,
            icon = null,
            avatarColor = Color(0xFFFF8989),
        )
        composeTestRule.onNodeWithTag(MEETING_ICON_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that disabled row invoke onItemClicked when clicked`() {
        val onItemClicked = mock<() -> Unit>()
        setContent(isEnabled = false, onItemClicked = onItemClicked)
        composeTestRule.onNodeWithTag(TITLE_TAG, useUnmergedTree = true).performClick()
    }

    @Test
    fun `test that checkbox shows check icon when item is selected`() {
        setContent(isSelected = true)
        composeTestRule.onNodeWithContentDescription("check icon", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that checkbox does not show check icon when item is not selected`() {
        setContent(isSelected = false)
        composeTestRule.onNodeWithContentDescription("check icon", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that onItemClicked is invoked when row is clicked`() {
        val onItemClicked = mock<() -> Unit>()
        setContent(onItemClicked = onItemClicked)
        composeTestRule.onNodeWithTag(TITLE_TAG, useUnmergedTree = true).performClick()
        verify(onItemClicked).invoke()
    }

    @Test
    fun `test that onItemClicked is invoked when subtitle is clicked`() {
        val onItemClicked = mock<() -> Unit>()
        setContent(subtitle = "Subtitle", onItemClicked = onItemClicked)
        composeTestRule.onNodeWithTag(SUBTITLE_TAG, useUnmergedTree = true).performClick()
        verify(onItemClicked).invoke()
    }

    @Test
    fun `test that note to self hint item shows localized title and hint icon`() {
        setContent(
            isHint = true,
            isSelected = false,
            isEnabled = true,
            icon = IconPack.Medium.Thin.Outline.FileText
        )
        composeTestRule.onNodeWithTag(NOTE_TO_SELF_HINT_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that note to self non hint item shows localized title and profile icon`() {
        setContent(
            isHint = false,
            isSelected = false,
            isEnabled = true,
            hasAvatarIcon = true,
            icon = IconPack.Medium.Thin.Outline.FileText
        )
        composeTestRule.onNodeWithTag(MEETING_ICON_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that note to self item does not show subtitle`() {
        setContent(
            isHint = true,
            isSelected = false,
            isEnabled = true,
        )
        composeTestRule.onNodeWithTag(SUBTITLE_TAG, useUnmergedTree = true).assertIsNotDisplayed()
    }

    @Test
    fun `test that meeting item shows title, subtitle and leading icon`() {
        setContent(
            title = "chat title",
            subtitle = "3 participants",
            hasAvatarIcon = true,
            isSelected = false,
            isEnabled = true,
            avatarColor = testPeerPrimary,
            avatarSecondaryColor = testPeerSecondary,
            icon = IconPack.Medium.Thin.Solid.Video,
        )
        composeTestRule.onNodeWithTag(TITLE_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SUBTITLE_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(MEETING_ICON_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that one to one chat item shows name, subtitle and leading icon`() {
        setContent(
            title = "name",
            subtitle = "Away",
            hasAvatarIcon = false,
            isSelected = false,
            isEnabled = true,
            avatarColor = testPeerPrimary,
            avatarSecondaryColor = testPeerSecondary,
        )
        composeTestRule.onNodeWithTag(TITLE_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SUBTITLE_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CONTACT_AVATAR_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
