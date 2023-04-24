package test.mega.privacy.android.app.presentation.fileinfo.view.sharedinfo

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.contact.view.contactItemForPreviews
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_CONTACT_ITEM_SHARED
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_SHARES_HEADER
import mega.privacy.android.app.presentation.fileinfo.view.TEST_TAG_SHOW_MORE
import mega.privacy.android.app.presentation.fileinfo.view.sharedinfo.MAX_CONTACTS_TO_SHOW
import mega.privacy.android.app.presentation.fileinfo.view.sharedinfo.SharedInfoView
import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.domain.entity.shares.AccessPermission
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class SharedInfoViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun contacts(number: Int) = List(number) {
        mock<ContactPermission> {
            on { accessPermission }.thenReturn(AccessPermission.READWRITE)
            on { contactItem }.thenReturn(contactItemForPreviews)
        }
    }

    @Test
    fun `test that the list of limited contacts is shown when expanded is true`() {
        composeTestRule.setContent {
            SharedInfoView(
                contacts = contacts(MAX_CONTACTS_TO_SHOW + 1),
                expanded = true,
                onHeaderClick = {},
                onContactClick = {},
                onContactLongClick = {},
                onMoreOptionsClick = {},
                onShowMoreContactsClick = {})
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
                onHeaderClick = {},
                onContactClick = {},
                onContactLongClick = {},
                onMoreOptionsClick = {},
                onShowMoreContactsClick = {})
        }
        composeTestRule.onNodeWithTag(TEST_TAG_SHOW_MORE, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `test that show more contacts is not shown when there are 5 contacts`() {
        composeTestRule.setContent {
            SharedInfoView(
                contacts = contacts(MAX_CONTACTS_TO_SHOW),
                expanded = true,
                onHeaderClick = {},
                onContactClick = {},
                onContactLongClick = {},
                onMoreOptionsClick = {},
                onShowMoreContactsClick = {})
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
                onHeaderClick = {},
                onContactClick = {},
                onContactLongClick = {},
                onMoreOptionsClick = {},
                onShowMoreContactsClick = {})
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
                onHeaderClick = onHeaderClick,
                onContactClick = {},
                onContactLongClick = {},
                onMoreOptionsClick = {},
                onShowMoreContactsClick = {}
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_SHARES_HEADER).performClick()
        verify(onHeaderClick).invoke()
    }
}