package mega.privacy.android.app.presentation.contact.invite

import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.main.InvitationContactInfo
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT
import mega.privacy.android.app.main.InvitationContactInfo.Companion.TYPE_PHONE_CONTACT_HEADER
import mega.privacy.android.app.main.model.InviteContactUiState
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class InviteContactScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var mockContextCompat: MockedStatic<ContextCompat>

    @Before
    fun setUp() {
        mockContextCompat = mockStatic(ContextCompat::class.java)
    }

    @After
    fun tearDown() {
        mockContextCompat.close()
    }

    @Test
    fun `test that the floating action button is clickable when enabled`() {
        with(composeRule) {
            val onInviteContactClick = mock<() -> Unit>()
            setScreen(
                uiState = InviteContactUiState(
                    query = "email@email.com",
                    selectedContactInformation = listOf(
                        InvitationContactInfo(
                            id = 1L,
                            name = "name 1"
                        ),
                        InvitationContactInfo(
                            id = 2L,
                            name = "name 2"
                        )
                    )
                ),
                onInviteContactClick = onInviteContactClick
            )

            onNodeWithTag(INVITE_CONTACT_FAB_TAG).performClick()

            verify(onInviteContactClick).invoke()
        }
    }

    @Test
    fun `test that the floating action button is not clickable when disabled`() {
        with(composeRule) {
            val onInviteContactClick = mock<() -> Unit>()
            setScreen(onInviteContactClick = onInviteContactClick)

            onNodeWithTag(INVITE_CONTACT_FAB_TAG).performClick()

            verify(onInviteContactClick, never()).invoke()
        }
    }

    @Test
    fun `test that the contacts are initialized when the permission is granted`() {
        with(composeRule) {
            val onInitializeContacts = mock<() -> Unit>()

            setScreen(onInitializeContacts = onInitializeContacts)

            verify(onInitializeContacts).invoke()
        }
    }

    @Test
    fun `test that the user is navigated back when the toolbar's back navigation is clicked`() {
        with(composeRule) {
            val onBackPressed = mock<() -> Unit>()
            setScreen(onBackPressed = onBackPressed)

            onNodeWithTag("appbar:button_back").performClick()

            verify(onBackPressed).invoke()
        }
    }

    @Test
    fun `test that the total selected contacts are displayed in the toolbar`() {
        with(composeRule) {
            setScreen(
                uiState = InviteContactUiState(
                    selectedContactInformation = listOf(
                        InvitationContactInfo(
                            id = 1L,
                            name = "name 1"
                        ),
                        InvitationContactInfo(
                            id = 2L,
                            name = "name 2"
                        )
                    )
                )
            )

            val text = context.resources.getQuantityString(
                R.plurals.general_selection_num_contacts,
                2,
                2
            )
            onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the selected contacts are displayed as chips`() {
        val firstContact = InvitationContactInfo(
            id = 1L,
            name = "name 1"
        )
        val secondContact = InvitationContactInfo(
            id = 2L,
            displayInfo = "name 2"
        )
        with(composeRule) {
            setScreen(
                uiState = InviteContactUiState(
                    selectedContactInformation = listOf(
                        firstContact,
                        secondContact
                    )
                )
            )

            onNodeWithTag(SELECTED_CONTACT_CHIP_TAG + firstContact.getContactName()).assertIsDisplayed()
            onNodeWithTag(SELECTED_CONTACT_CHIP_TAG + secondContact.getContactName()).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the search query is set to an empty string when the input query is a valid email and the last character is a white-space`() {
        with(composeRule) {
            var updatedQuery: String? = null
            setScreen(onSearchQueryChange = { updatedQuery = it })

            onNodeWithTag("generic_text_field:text_field").performTextInput("email@email.com ")

            assertThat(updatedQuery).isEmpty()
        }
    }

    @Test
    fun `test that the search query is set to an empty string when the input query is a valid phone number and the last character is a white-space`() {
        with(composeRule) {
            var updatedQuery: String? = null
            setScreen(onSearchQueryChange = { updatedQuery = it })

            onNodeWithTag("generic_text_field:text_field").performTextInput("08123123123 ")

            assertThat(updatedQuery).isEmpty()
        }
    }

    @Test
    fun `test that the default contact list body is displayed the first time the user opens the screen in his or her whole life`() {
        with(composeRule) {
            denyPermission()

            setScreen()

            onNodeWithTag(DEFAULT_BODY_LOADING_TEXT_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the loading body is displayed when loading`() {
        with(composeRule) {
            setScreen(uiState = InviteContactUiState(isLoading = true))

            onNodeWithTag(CONTACT_LIST_LOADING_TEXT_TAG).assertIsDisplayed()
            onNodeWithTag(CIRCULAR_LOADING_INDICATOR_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the empty result body is displayed when the list of contacts is empty`() {
        with(composeRule) {
            setScreen(uiState = InviteContactUiState(areContactsInitialized = true))

            onNodeWithTag(NO_CONTACTS_TEXT_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the list of contacts is displayed when the list isn't empty`() {
        val header = InvitationContactInfo(
            type = TYPE_PHONE_CONTACT_HEADER
        )
        val firstContact = InvitationContactInfo(
            id = 1L,
            name = "name 1",
            type = TYPE_PHONE_CONTACT
        )
        val secondContact = InvitationContactInfo(
            id = 2L,
            displayInfo = "name 2",
            type = TYPE_PHONE_CONTACT
        )
        with(composeRule) {
            setScreen(
                uiState = InviteContactUiState(
                    areContactsInitialized = true,
                    filteredContacts = listOf(
                        header,
                        firstContact,
                        secondContact
                    )
                )
            )

            // Header item
            onNodeWithTag(PHONE_CONTACTS_HEADER_TEXT_TAG).assertIsDisplayed()
            // First Contact
            onNodeWithTag(CONTACT_LIST_BODY_TAG)
                .performScrollToNode(hasText(firstContact.getContactName()))
                .assertIsDisplayed()
            onNodeWithTag(CONTACT_LIST_BODY_TAG)
                .performScrollToNode(hasText(firstContact.displayInfo))
                .assertIsDisplayed()
            // Second Contact
            onNodeWithTag(CONTACT_LIST_BODY_TAG)
                .performScrollToNode(hasText(secondContact.getContactName()))
                .assertIsDisplayed()
            onNodeWithTag(CONTACT_LIST_BODY_TAG)
                .performScrollToNode(hasText(secondContact.displayInfo))
                .assertIsDisplayed()
        }
    }

    private fun denyPermission() {
        whenever(
            ContextCompat.checkSelfPermission(
                any(),
                any()
            )
        ) doReturn PackageManager.PERMISSION_DENIED
    }

    private fun ComposeContentTestRule.setScreen(
        uiState: InviteContactUiState = InviteContactUiState(),
        onBackPressed: () -> Unit = {},
        onInitializeContacts: () -> Unit = {},
        onShareContactLink: (contactLink: String) -> Unit = {},
        onOpenPersonalQRCode: () -> Unit = {},
        onSearchQueryChange: (query: String) -> Unit = {},
        onInviteContactClick: () -> Unit = {},
        onScanQRCodeClick: () -> Unit = {},
        onAddContactInfo: (query: String, type: Int) -> Unit = { _, _ -> },
        onContactListItemClick: (contactInfo: InvitationContactInfo) -> Unit = {},
        onDoneImeActionClick: () -> Unit = {},
        onContactChipClick: (contactInfo: InvitationContactInfo) -> Unit = {},
    ) {
        setContent {
            InviteContactScreen(
                uiState = uiState,
                isDarkMode = false,
                onBackPressed = onBackPressed,
                onInitializeContacts = onInitializeContacts,
                onShareContactLink = onShareContactLink,
                onOpenPersonalQRCode = onOpenPersonalQRCode,
                onSearchQueryChange = onSearchQueryChange,
                onInviteContactClick = onInviteContactClick,
                onScanQRCodeClick = onScanQRCodeClick,
                onAddContactInfo = onAddContactInfo,
                onContactListItemClick = onContactListItemClick,
                onDoneImeActionClick = onDoneImeActionClick,
                onContactChipClick = onContactChipClick,
            )
        }
    }
}
