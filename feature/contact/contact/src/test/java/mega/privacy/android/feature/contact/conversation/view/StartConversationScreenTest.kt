package mega.privacy.android.feature.contact.conversation.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.feature.contact.conversation.model.NoteToSelfUiState
import mega.privacy.android.feature.contact.group.create.model.CreateChatUiState
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.mobile.analytics.event.CreateNoteToSelfButtonPressedEvent
import mega.privacy.mobile.analytics.event.GroupChatPressedEvent
import mega.privacy.mobile.analytics.event.InviteContactsPressedEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartConversationScreenTest {

    private val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    @Test
    fun `test that loading view is shown when contacts are loading`() {
        setScreen(contactsState = CreateChatUiState.Loading)

        composeTestRule.onNodeWithTag(START_CONVERSATION_LOADING_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(START_CONVERSATION_LIST_TAG).assertCountEquals(0)
    }

    @Test
    fun `test that new group chat and invite rows are shown when there are contacts`() {
        setScreen(contactsState = dataState(contact(1L, "Alice")))

        composeTestRule.onNodeWithTag(START_CONVERSATION_NEW_GROUP_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(START_CONVERSATION_INVITE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that invite row is hidden and invite empty state shown when there are no contacts`() {
        setScreen(contactsState = dataState())

        composeTestRule.onNodeWithTag(START_CONVERSATION_NEW_GROUP_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(START_CONVERSATION_INVITE_TAG).assertCountEquals(0)
        composeTestRule.onNodeWithTag(START_CONVERSATION_EMPTY_CONTACTS_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that search is offered when there are contacts`() {
        setScreen(contactsState = dataState(contact(1L, "Alice")))

        composeTestRule.onAllNodes(searchActionMatcher).assertCountEquals(1)
    }

    @Test
    fun `test that search is not offered when there are no contacts`() {
        setScreen(contactsState = dataState())

        composeTestRule.onAllNodes(searchActionMatcher).assertCountEquals(0)
    }

    @Test
    fun `test that action rows are hidden when searching`() {
        setScreen(contactsState = dataState(contact(1L, "Alice")), searchActive = true)

        composeTestRule.onAllNodesWithTag(START_CONVERSATION_NEW_GROUP_TAG).assertCountEquals(0)
        composeTestRule.onAllNodesWithTag(START_CONVERSATION_INVITE_TAG).assertCountEquals(0)
    }

    @Test
    fun `test that empty search state is shown when searching finds no contacts`() {
        setScreen(contactsState = dataState(), searchActive = true)

        composeTestRule.onNodeWithTag(START_CONVERSATION_EMPTY_SEARCH_TAG).assertIsDisplayed()
        composeTestRule.onAllNodesWithTag(START_CONVERSATION_NOTE_TO_SELF_TAG).assertCountEquals(0)
        composeTestRule.onAllNodesWithTag(START_CONVERSATION_EMPTY_CONTACTS_TAG).assertCountEquals(0)
    }

    @Test
    fun `test that search field stays visible when searching finds no contacts`() {
        setScreen(contactsState = dataState(), searchActive = true)

        composeTestRule.onAllNodes(hasSetTextAction()).assertCountEquals(1)
    }

    @Test
    fun `test that note to self row is shown when the note to self chat is available`() {
        setScreen(
            contactsState = dataState(contact(1L, "Alice")),
            noteToSelfState = NoteToSelfUiState.Data(chatId = 10L, isHint = true),
        )

        composeTestRule.onNodeWithTag(START_CONVERSATION_NOTE_TO_SELF_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that note to self row is hidden when the note to self chat is loading`() {
        setScreen(
            contactsState = dataState(contact(1L, "Alice")),
            noteToSelfState = NoteToSelfUiState.Loading,
        )

        composeTestRule.onAllNodesWithTag(START_CONVERSATION_NOTE_TO_SELF_TAG).assertCountEquals(0)
    }

    @Test
    fun `test that contacts are grouped under their initial letter headers`() {
        setScreen(
            contactsState = dataState(
                contact(1L, "Alice"),
                contact(2L, "amy"),
                contact(3L, "Bob"),
            ),
        )

        composeTestRule.onAllNodesWithTag(startConversationHeaderTag("A")).assertCountEquals(1)
        scrollTo(hasTestTag(startConversationHeaderTag("B")))
        composeTestRule.onNodeWithTag(startConversationHeaderTag("B")).assertIsDisplayed()
    }

    @Test
    fun `test that tapping a contact invokes onContactClick with its handle`() {
        var clickedHandle: Long? = null
        setScreen(
            contactsState = dataState(contact(1L, "Alice"), contact(2L, "Bob")),
            onContactClick = { clickedHandle = it },
        )

        scrollTo(hasText("Bob"))
        composeTestRule.onNode(hasTestTag(CONTACT_ITEM_VIEW_ROW) and hasText("Bob")).performClick()

        assertThat(clickedHandle).isEqualTo(2L)
    }

    @Test
    fun `test that tapping new group chat invokes the callback and tracks the event`() {
        var clicked = false
        setScreen(
            contactsState = dataState(contact(1L, "Alice")),
            onNewGroupChatClick = { clicked = true },
        )

        composeTestRule.onNodeWithTag(START_CONVERSATION_NEW_GROUP_TAG).performClick()

        assertThat(clicked).isTrue()
        assertThat(analyticsRule.events).contains(GroupChatPressedEvent)
    }

    @Test
    fun `test that tapping invite invokes the callback and tracks the event`() {
        var clicked = false
        setScreen(
            contactsState = dataState(contact(1L, "Alice")),
            onInviteClick = { clicked = true },
        )

        composeTestRule.onNodeWithTag(START_CONVERSATION_INVITE_TAG).performClick()

        assertThat(clicked).isTrue()
        assertThat(analyticsRule.events).contains(InviteContactsPressedEvent)
    }

    @Test
    fun `test that tapping the empty state invite button invokes the invite callback`() {
        var clicked = false
        setScreen(contactsState = dataState(), onInviteClick = { clicked = true })

        scrollTo(hasTestTag(START_CONVERSATION_EMPTY_INVITE_TAG))
        composeTestRule.onNodeWithTag(START_CONVERSATION_EMPTY_INVITE_TAG).performClick()

        assertThat(clicked).isTrue()
        assertThat(analyticsRule.events).contains(InviteContactsPressedEvent)
    }

    @Test
    fun `test that tapping note to self invokes the callback with its chat id and tracks the event`() {
        var clickedChatId: Long? = null
        setScreen(
            contactsState = dataState(contact(1L, "Alice")),
            noteToSelfState = NoteToSelfUiState.Data(chatId = 10L, isHint = false),
            onNoteToSelfClick = { clickedChatId = it },
        )

        composeTestRule.onNodeWithTag(START_CONVERSATION_NOTE_TO_SELF_TAG).performClick()

        assertThat(clickedChatId).isEqualTo(10L)
        assertThat(analyticsRule.events).contains(CreateNoteToSelfButtonPressedEvent)
    }

    private fun setScreen(
        contactsState: CreateChatUiState,
        noteToSelfState: NoteToSelfUiState = NoteToSelfUiState.Data(chatId = 10L, isHint = true),
        searchActive: Boolean = false,
        onContactClick: (Long) -> Unit = {},
        onNewGroupChatClick: () -> Unit = {},
        onInviteClick: () -> Unit = {},
        onNoteToSelfClick: (Long) -> Unit = {},
    ) {
        composeTestRule.setContent {
            StartConversationScreenContent(
                contactsState = contactsState,
                noteToSelfState = noteToSelfState,
                searchActive = searchActive,
                searchText = "",
                onSearchActiveChange = {},
                onSearchTextChange = {},
                onContactClick = onContactClick,
                onNewGroupChatClick = onNewGroupChatClick,
                onInviteClick = onInviteClick,
                onNoteToSelfClick = onNoteToSelfClick,
                onBack = {},
            )
        }
    }

    private fun scrollTo(matcher: SemanticsMatcher) {
        composeTestRule.onNodeWithTag(START_CONVERSATION_LIST_TAG).performScrollToNode(matcher)
    }

    private fun dataState(vararg contacts: ContactItemUiState) = CreateChatUiState.Data(
        contacts = contacts.toList().toImmutableList(),
        query = null,
        allowGroupImageSelection = false,
    )

    private fun contact(handle: Long, name: String) = ContactItemUiState(
        handle = handle,
        displayName = name,
        status = ContactItemStatus.Online,
        lastSeen = null,
        avatar = AvatarData.Initials(initials = name.take(1), avatarColor = Color.Blue),
        isVerified = false,
        email = "$name@example.com",
    )

    private val searchActionMatcher =
        hasContentDescription("Search") and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    private companion object {
        const val CONTACT_ITEM_VIEW_ROW = "contact_item_view:row"
    }
}
