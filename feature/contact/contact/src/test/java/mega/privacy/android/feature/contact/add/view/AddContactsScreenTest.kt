package mega.privacy.android.feature.contact.add.view

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.feature.contact.add.model.AddContactUiState
import mega.privacy.android.feature.contact.add.model.PhoneContactsSection
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddContactsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that the shimmer loading view is displayed when state is Loading`() {
        setScreen(AddContactUiState.Loading)

        composeTestRule.onNodeWithTag(ADD_CONTACTS_LOADING_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ADD_CONTACTS_LIST_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that the empty view is displayed when there are no contacts`() {
        setScreen(dataState())

        composeTestRule.onNodeWithTag(ADD_CONTACTS_EMPTY_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the contact list is displayed when there are contacts`() {
        setScreen(dataState(contact(1L, "Alice"), contact(2L, "Bob")))

        composeTestRule.onNodeWithTag(ADD_CONTACTS_LIST_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW).assertCountEquals(2)
    }

    @Test
    fun `test that the fab is hidden until a contact is selected`() {
        setScreen(dataState(contact(1L, "Alice")))

        composeTestRule.onNodeWithTag(ADD_CONTACTS_FAB_TAG).assertIsNotDisplayed()

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()

        composeTestRule.onNodeWithTag(ADD_CONTACTS_FAB_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the title reflects the selection count`() {
        setScreen(dataState(contact(1L, "Alice"), contact(2L, "Bob")))

        composeTestRule.onNodeWithText("Send contacts").assertIsDisplayed()

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onNodeWithText("1 selected").assertIsDisplayed()

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[1].performClick()
        composeTestRule.onNodeWithText("2 selected").assertIsDisplayed()
    }

    @Test
    fun `test that confirming reports the selected handles`() {
        var confirmedHandles: Set<Long>? = null
        setScreen(
            dataState(contact(1L, "Alice"), contact(2L, "Bob")),
            onConfirm = { handles, _ -> confirmedHandles = handles },
        )

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onNodeWithTag(ADD_CONTACTS_FAB_TAG).performClick()

        assertThat(confirmedHandles).containsExactly(1L)
    }

    @Test
    fun `test that the phone section header is not displayed when the section is Hidden`() {
        setScreen(dataState(contact(1L, "Alice")))

        composeTestRule.onNodeWithTag(PHONE_SECTION_HEADER_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that the phone section header is displayed when the section is present`() {
        setScreen(dataState(contact(1L, "Alice"), phoneSection = PhoneContactsSection.PermissionRequired))

        composeTestRule.onNodeWithTag(PHONE_SECTION_HEADER_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the allow access CTA is shown only when the section is expanded`() {
        setScreen(dataState(phoneSection = PhoneContactsSection.PermissionRequired))

        composeTestRule.onNodeWithTag(PHONE_SECTION_ALLOW_ACCESS_TAG).assertIsNotDisplayed()

        composeTestRule.onNodeWithTag(PHONE_SECTION_HEADER_TAG).performClick()

        composeTestRule.onNodeWithTag(PHONE_SECTION_ALLOW_ACCESS_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the select phone contacts CTA is shown when expanded and picker is available`() {
        setScreen(
            dataState(
                phoneSection = PhoneContactsSection.PickerAvailable(persistentListOf()),
            )
        )

        composeTestRule.onNodeWithTag(PHONE_SECTION_HEADER_TAG).performClick()

        composeTestRule.onNodeWithTag(PHONE_SECTION_SELECT_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that expanding a Loaded section renders phone contact rows`() {
        setScreen(
            dataState(
                phoneSection = PhoneContactsSection.Loaded(
                    persistentListOf(phoneContact("pa@test.com"), phoneContact("pb@test.com")),
                ),
            )
        )

        composeTestRule.onNodeWithTag(PHONE_SECTION_HEADER_TAG).performClick()

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW).assertCountEquals(2)
    }

    @Test
    fun `test that clicking a phone contact row selects it and shows the fab`() {
        setScreen(
            dataState(
                phoneSection = PhoneContactsSection.Loaded(
                    persistentListOf(phoneContact("pa@test.com")),
                ),
            )
        )

        composeTestRule.onNodeWithTag(ADD_CONTACTS_FAB_TAG).assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(PHONE_SECTION_HEADER_TAG).performClick()
        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()

        composeTestRule.onNodeWithTag(ADD_CONTACTS_FAB_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that selection is retained when a selected contact is filtered out`() {
        val state = mutableStateOf(dataState(contact(1L, "Alice"), contact(2L, "Bob")))
        composeTestRule.setContent {
            AddContactsScreen(
                state = state.value,
                onSearchQueryChange = {},
                onConfirm = { _, _ -> },
                onBack = {},
            )
        }

        // Select Alice (the first row) -> FAB appears.
        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onNodeWithTag(ADD_CONTACTS_FAB_TAG).assertIsDisplayed()

        // Filter Alice out of the visible list.
        composeTestRule.runOnIdle { state.value = dataState(contact(2L, "Bob")) }

        // Selection is held independently of the list, so the FAB stays visible.
        composeTestRule.onNodeWithTag(ADD_CONTACTS_FAB_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the user limit warning is displayed when showUserLimitWarning is true`() {
        setScreen(dataState(contact(1L, "Alice"), showUserLimitWarning = true))

        composeTestRule.onNodeWithTag(ADD_CONTACTS_USER_LIMIT_WARNING_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the user limit warning is not displayed when showUserLimitWarning is false`() {
        setScreen(dataState(contact(1L, "Alice")))

        composeTestRule.onNodeWithTag(ADD_CONTACTS_USER_LIMIT_WARNING_TAG).assertIsNotDisplayed()
    }

    private fun setScreen(
        state: AddContactUiState,
        onSearchQueryChange: (String?) -> Unit = {},
        onConfirm: (Set<Long>, Set<String>) -> Unit = { _, _ -> },
        onBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            AddContactsScreen(
                state = state,
                onSearchQueryChange = onSearchQueryChange,
                onConfirm = onConfirm,
                onBack = onBack,
            )
        }
    }

    private fun dataState(
        vararg contacts: ContactItemUiState,
        showUserLimitWarning: Boolean = false,
        phoneSection: PhoneContactsSection = PhoneContactsSection.Hidden,
    ) = AddContactUiState.Data(
        contacts = contacts.toList().toImmutableList(),
        query = null,
        showUserLimitWarning = showUserLimitWarning,
        phoneContactsSection = phoneSection,
        phoneContactsPickedEvent = consumed(),
    )

    private fun phoneContact(email: String) = ContactItemUiState(
        handle = -1L,
        displayName = email,
        status = ContactItemStatus.Unknown,
        lastSeen = null,
        avatar = AvatarData.Initials(initials = email.first().uppercase(), avatarColor = Color.Gray),
        isVerified = false,
        email = email,
    )

    private fun contact(
        handle: Long,
        displayName: String = "Contact $handle",
        email: String = "$handle@test.com",
    ) = ContactItemUiState(
        handle = handle,
        displayName = displayName,
        status = ContactItemStatus.Online,
        lastSeen = null,
        avatar = AvatarData.Initials(
            initials = displayName.first().toString(),
            avatarColor = Color(0xFF2E7D32),
        ),
        isVerified = false,
        email = email,
    )

    private companion object {
        const val CONTACT_ITEM_VIEW_ROW = "contact_item_view:row"
        const val PHONE_SECTION_HEADER_TAG = "add_contacts_screen:phone_section_header"
        const val PHONE_SECTION_ALLOW_ACCESS_TAG = "add_contacts_screen:phone_section_allow_access"
        const val PHONE_SECTION_SELECT_TAG = "add_contacts_screen:phone_section_select"
    }
}
