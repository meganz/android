package mega.privacy.android.app.presentation.fileinfo.view.sharedinfo

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_CONTACT_ITEM_SHARED
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_SHARES_HEADER
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_SHOW_MORE
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.contact.model.ContactPermissionUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SharedInfoViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    val contact = ContactItemUiState(
        handle = 2L,
        displayName = "Bob Brown",
        status = ContactItemStatus.Away,
        lastSeen = 65535,
        avatar = AvatarData.Initials(initials = "B", avatarColor = Color(0xFF1565C0)),
        isVerified = false,
    )

    private fun contacts(number: Int) = List(number) {
        mock<ContactPermissionUiState> {
            on { permission }.thenReturn(AccessPermission.READWRITE)
            on { email }.thenReturn("contactItemForPreviews@mega.co.nz")
            on { contactItemUiState }.thenReturn(contact)
        }
    }

    @Test
    fun `test that the list of limited contacts is shown when expanded is true`() {
        composeTestRule.setContent {
            SharedInfoView(
                contacts = contacts(MAX_CONTACTS_TO_SHOW + 1),
                expanded = true,
                selectedContacts = emptyList(),
                onHeaderClick = {},
                onContactClick = {},
                onContactLongClick = {},
                onContactMoreOptionsClick = {},
                onShowMoreContactsClick = {}
            )
        }
        composeTestRule.onAllNodesWithTag(TEST_TAG_CONTACT_ITEM_SHARED)
            .assertCountEquals(MAX_CONTACTS_TO_SHOW)
    }

    @Test
    fun `test that show more contacts is shown when there are more than 5 contacts`() {
        composeTestRule.setContent {
            SharedInfoView(
                contacts = contacts(MAX_CONTACTS_TO_SHOW + 1),
                expanded = true,
                selectedContacts = emptyList(),
                onHeaderClick = {},
                onContactClick = {},
                onContactLongClick = {},
                onContactMoreOptionsClick = {},
                onShowMoreContactsClick = {},
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_SHOW_MORE, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `test that show more contacts is not shown when there are 5 contacts`() {
        composeTestRule.setContent {
            SharedInfoView(
                contacts = contacts(MAX_CONTACTS_TO_SHOW),
                expanded = true,
                selectedContacts = emptyList(),
                onHeaderClick = {},
                onContactClick = {},
                onContactLongClick = {},
                onContactMoreOptionsClick = {},
                onShowMoreContactsClick = {},
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_SHOW_MORE, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that list of contacts is not shown when expanded is false`() {
        composeTestRule.setContent {
            SharedInfoView(
                contacts = contacts(MAX_CONTACTS_TO_SHOW),
                expanded = false,
                selectedContacts = emptyList(),
                onHeaderClick = {},
                onContactClick = {},
                onContactLongClick = {},
                onContactMoreOptionsClick = {},
                onShowMoreContactsClick = {},
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_CONTACT_ITEM_SHARED).assertDoesNotExist()
    }

    @Test
    fun `test that on header click event is fired when header is clicked`() {
        val onHeaderClick = mock<() -> Unit>()
        composeTestRule.setContent {
            SharedInfoView(
                contacts = contacts(MAX_CONTACTS_TO_SHOW),
                expanded = false,
                selectedContacts = emptyList(),
                onHeaderClick = onHeaderClick,
                onContactClick = {},
                onContactLongClick = {},
                onContactMoreOptionsClick = {},
                onShowMoreContactsClick = {}
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_SHARES_HEADER).performClick()
        verify(onHeaderClick).invoke()
    }
}