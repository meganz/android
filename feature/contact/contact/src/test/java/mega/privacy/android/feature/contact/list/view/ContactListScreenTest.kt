package mega.privacy.android.feature.contact.list.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.feature.contact.list.model.ContactListUiState
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.mobile.analytics.event.AddContactFABEvent
import mega.privacy.mobile.analytics.event.ContactItemAvatarSelectedEvent
import mega.privacy.mobile.analytics.event.ContactItemSelectedEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactListScreenTest {

    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    @Test
    fun `test that loading indicator is displayed when state is Loading`() {
        setScreen(ContactListUiState.Loading)
        composeTestRule.onNodeWithTag(CONTACT_LIST_LOADING_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CONTACT_LIST_LAZY_COLUMN_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that fab is displayed and triggers onAddContactClick when state is Data`() {
        var addClicked = false
        setScreen(dataState(), onAddContactClick = { addClicked = true })

        composeTestRule.onNodeWithTag(CONTACT_LIST_FAB_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CONTACT_LIST_FAB_TAG).performClick()
        assert(addClicked)
    }

    @Test
    fun `test that tapping fab tracks AddContactFABEvent`() {
        setScreen(dataState())

        composeTestRule.onNodeWithTag(CONTACT_LIST_FAB_TAG).performClick()

        assertThat(analyticsRule.events).contains(AddContactFABEvent)
    }

    @Test
    fun `test that group headers render for each contact initial`() {
        val contacts = mapOf(
            "A" to listOf(contact(1L, "Alice")),
            "B" to listOf(contact(2L, "Bob")),
        )
        setScreen(dataState(contacts = contacts))

        composeTestRule.onNodeWithTag(contactGroupHeaderTag("A")).assertIsDisplayed()
        composeTestRule.onNodeWithTag(contactGroupHeaderTag("B")).assertIsDisplayed()
    }

    @Test
    fun `test that requests action shows badge when there are incoming requests`() {
        setScreen(dataState(incomingRequestCount = 4))
        composeTestRule
            .onNodeWithTag("$CONTACT_LIST_REQUESTS_ACTION_TAG:badge", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `test that requests action has no badge when there are no incoming requests`() {
        setScreen(dataState(incomingRequestCount = 0))
        composeTestRule
            .onAllNodesWithTag("$CONTACT_LIST_REQUESTS_ACTION_TAG:badge", useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun `test that requests action click invokes onRequestsClick`() {
        var clicked = false
        setScreen(dataState(), onRequestsClick = { clicked = true })

        composeTestRule.onNodeWithTag(CONTACT_LIST_REQUESTS_ACTION_TAG).performClick()
        assert(clicked)
    }

    @Test
    fun `test that groups action click invokes onGroupsClick`() {
        var clicked = false
        setScreen(dataState(), onGroupsClick = { clicked = true })

        composeTestRule.onNodeWithTag(CONTACT_LIST_GROUPS_ACTION_TAG).performClick()
        assert(clicked)
    }

    @Test
    fun `test that contact row click invokes onContactClick with handle`() {
        var receivedHandle: Long? = null
        val contacts = mapOf("A" to listOf(contact(42L, "Alice")))
        setScreen(
            state = dataState(contacts = contacts),
            onContactClick = { receivedHandle = it },
        )

        composeTestRule.onAllNodesWithTag("contact_item_view:row")[0].performClick()
        assert(receivedHandle == 42L)
    }

    @Test
    fun `test that contact row click tracks ContactItemSelectedEvent`() {
        val contacts = mapOf("A" to listOf(contact(42L, "Alice")))
        setScreen(state = dataState(contacts = contacts))

        composeTestRule.onAllNodesWithTag("contact_item_view:row")[0].performClick()

        assertThat(analyticsRule.events).contains(ContactItemSelectedEvent)
    }

    @Test
    fun `test that contact avatar click tracks ContactItemAvatarSelectedEvent`() {
        val contacts = mapOf("A" to listOf(contact(42L, "Alice")))
        setScreen(state = dataState(contacts = contacts))

        composeTestRule.onAllNodesWithTag("contact_item_view:avatar_click")[0].performClick()

        assertThat(analyticsRule.events).contains(ContactItemAvatarSelectedEvent)
    }

    @Test
    fun `test that recently added row is shown when there are recently added contacts`() {
        setScreen(
            dataState(
                recentlyAddedContacts = listOf(contact(7L, "New")).toImmutableList()
            )
        )
        composeTestRule.onNodeWithTag(CONTACT_LIST_RECENT_HEADER_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CONTACT_LIST_RECENT_ROW_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that empty state is displayed when there are no contacts`() {
        setScreen(dataState(contacts = emptyMap()))
        composeTestRule.onNodeWithTag(CONTACT_LIST_EMPTY_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that bottom sheet appears when more is tapped on a contact`() {
        val contacts = mapOf("A" to listOf(contact(1L, "Alice", email = "a@test.com")))
        setScreen(dataState(contacts = contacts))

        composeTestRule.onAllNodesWithTag("contact_item_view:more")[0].performClick()
        composeTestRule.onNodeWithTag(CONTACT_ACTIONS_SHEET_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that bottom sheet send message invokes onContactClick`() {
        var receivedHandle: Long? = null
        val contacts = mapOf("A" to listOf(contact(11L, "Alice", email = "a@test.com")))
        setScreen(
            dataState(contacts = contacts),
            onContactClick = { receivedHandle = it },
        )

        composeTestRule.onAllNodesWithTag("contact_item_view:more")[0].performClick()
        composeTestRule.onNodeWithTag(CONTACT_ACTION_SEND_MESSAGE_TAG).performClick()
        composeTestRule.waitForIdle()
        assert(receivedHandle == 11L)
    }

    @Test
    fun `test that search action toggles into search mode`() {
        setScreen(dataState())
        composeTestRule.onNode(searchActionMatcher).performClick()
        composeTestRule.onNodeWithContentDescription("Navigation Icon").assertIsDisplayed()
    }

    @Test
    fun `test that typing in search input fires onSearchQueryChange with text`() {
        var lastQuery: String? = null
        setScreen(dataState(), onSearchQueryChange = { lastQuery = it })

        composeTestRule.onNode(searchActionMatcher).performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("ali")

        assert(lastQuery == "ali")
    }

    /**
     * Matches the search action icon button in [mega.android.core.ui.components.toolbar.MegaSearchTopAppBar].
     * Both the action button and the search field's leading icon carry the "Search" content
     * description, so the [Role.Button] is used to single out the clickable action.
     */
    private val searchActionMatcher =
        hasContentDescription("Search") and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    private fun setScreen(
        state: ContactListUiState,
        onSearchQueryChange: (String?) -> Unit = {},
        onContactClick: (Long) -> Unit = {},
        onContactInfoClick: (String) -> Unit = {},
        onAddContactClick: () -> Unit = {},
        onRequestsClick: () -> Unit = {},
        onGroupsClick: () -> Unit = {},
        onStartCall: (Long, Boolean, Boolean) -> Unit = { _, _, _ -> },
        onRemoveContact: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            ContactListScreen(
                state = state,
                onSearchQueryChange = onSearchQueryChange,
                onContactClick = onContactClick,
                onContactInfoClick = onContactInfoClick,
                onAddContactClick = onAddContactClick,
                onRequestsClick = onRequestsClick,
                onGroupsClick = onGroupsClick,
                onStartCall = onStartCall,
                onRemoveContact = onRemoveContact,
                onChatEventConsumed = {},
                onCallEventConsumed = {},
                onNavigateToChat = {},
                onStartCallTriggered = {},
            )
        }
    }

    private fun dataState(
        contacts: Map<String, List<ContactItemUiState>> = mapOf("A" to listOf(contact(1L))),
        recentlyAddedContacts: ImmutableList<ContactItemUiState> = emptyList<ContactItemUiState>().toImmutableList(),
        incomingRequestCount: Int = 0,
    ) = ContactListUiState.Data(
        contacts = contacts,
        recentlyAddedContacts = recentlyAddedContacts,
        incomingRequestCount = incomingRequestCount,
        openChatEvent = consumed(),
        startCallEvent = consumed(),
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
}
