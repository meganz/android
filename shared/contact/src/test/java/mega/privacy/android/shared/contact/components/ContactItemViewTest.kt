package mega.privacy.android.shared.contact.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class ContactItemViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onClick = mock<() -> Unit>()
    private val onLongClick = mock<() -> Unit>()
    private val onAvatarClick = mock<() -> Unit>()
    private val onMoreClicked = mock<() -> Unit>()

    private val contact = ContactItemUiState(
        handle = 1L,
        displayName = "Alice Anderson",
        status = ContactItemStatus.Online,
        lastSeen = null,
        avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
        isVerified = false,
    )

    private fun setContent(
        contact: ContactItemUiState = this.contact,
        onClick: (() -> Unit)? = this.onClick,
        onLongClick: (() -> Unit)? = this.onLongClick,
        onAvatarClick: (() -> Unit)? = this.onAvatarClick,
        onMoreClicked: (() -> Unit)? = this.onMoreClicked,
        inSelectionMode: Boolean = false,
    ) {
        composeTestRule.setContent {
            ContactItemView(
                contactItemUiState = contact,
                onClick = onClick,
                onLongClick = onLongClick,
                onAvatarClick = onAvatarClick,
                onMoreClicked = onMoreClicked,
                inSelectionMode = inSelectionMode,
            )
        }
    }

    @Test
    fun `test that onClick is fired when row body is clicked`() {
        setContent()
        composeTestRule.onNodeWithTag(CONTACT_ITEM_VIEW_ROW).performClick()
        verify(onClick).invoke()
        verifyNoInteractions(onLongClick)
        verifyNoInteractions(onAvatarClick)
        verifyNoInteractions(onMoreClicked)
    }

    @Test
    fun `test that onLongClick is fired when row body is long clicked`() {
        setContent()
        composeTestRule.onNodeWithTag(CONTACT_ITEM_VIEW_ROW)
            .performTouchInput { longClick() }
        verify(onLongClick).invoke()
        verifyNoInteractions(onClick)
        verifyNoInteractions(onAvatarClick)
        verifyNoInteractions(onMoreClicked)
    }

    @Test
    fun `test that onMoreClicked is fired when kebab is clicked`() {
        setContent()
        composeTestRule.onNodeWithTag(CONTACT_ITEM_VIEW_MORE).performClick()
        verify(onMoreClicked).invoke()
        verifyNoInteractions(onClick)
        verifyNoInteractions(onLongClick)
        verifyNoInteractions(onAvatarClick)
    }

    @Test
    fun `test that kebab is not rendered when onMoreClicked is null`() {
        setContent(onMoreClicked = null)
        composeTestRule.onNodeWithTag(CONTACT_ITEM_VIEW_MORE).assertDoesNotExist()
    }

    @Test
    fun `test that onAvatarClick is fired when avatar is clicked and onAvatarClick is set`() {
        setContent()
        composeTestRule.onNodeWithTag(CONTACT_ITEM_VIEW_AVATAR_CLICK).performClick()
        verify(onAvatarClick).invoke()
        verifyNoInteractions(onClick)
        verifyNoInteractions(onLongClick)
        verifyNoInteractions(onMoreClicked)
    }

    @Test
    fun `test that the avatar clickable wrapper is not rendered when onAvatarClick is null`() {
        setContent(onAvatarClick = null)
        composeTestRule.onNodeWithTag(CONTACT_ITEM_VIEW_AVATAR_CLICK).assertDoesNotExist()
    }

    @Test
    fun `test that verified badge is rendered when isVerified is true`() {
        setContent(contact = contact.copy(isVerified = true))
        composeTestRule.onNodeWithTag(CONTACT_ITEM_VIEW_VERIFIED_BADGE, useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `test that verified badge is not rendered when isVerified is false`() {
        setContent(contact = contact.copy(isVerified = false))
        composeTestRule.onNodeWithTag(CONTACT_ITEM_VIEW_VERIFIED_BADGE, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that checkbox is shown when inSelectionMode is true`() {
        setContent(inSelectionMode = true)
        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_CHECKBOX, useUnmergedTree = true)
            .onFirst()
            .assertExists()
    }

    @Test
    fun `test that kebab is hidden when inSelectionMode is true`() {
        setContent(inSelectionMode = true)
        composeTestRule.onNodeWithTag(CONTACT_ITEM_VIEW_MORE, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_CHECKBOX, useUnmergedTree = true)
            .onFirst()
            .assertExists()
    }

    @Test
    fun `test that checkbox is not shown when inSelectionMode is false`() {
        setContent(inSelectionMode = false)
        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_CHECKBOX, useUnmergedTree = true)
            .assertCountEquals(0)
    }
}
