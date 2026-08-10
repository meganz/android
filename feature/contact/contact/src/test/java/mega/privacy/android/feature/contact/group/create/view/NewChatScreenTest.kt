package mega.privacy.android.feature.contact.group.create.view

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.feature.contact.group.create.model.CreateChatUiState
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NewChatScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that the fab is hidden until a contact is selected`() {
        setScreen(dataState(contact(1L, "Alice")))

        composeTestRule.onNodeWithTag(NEW_CHAT_NEXT_FAB_TAG).assertIsNotDisplayed()

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()

        composeTestRule.onNodeWithTag(NEW_CHAT_NEXT_FAB_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that selecting a single contact confirms a one to one chat without showing settings`() {
        var oneToOneHandle: Long? = null
        var groupReported = false
        setScreen(
            dataState(contact(1L, "Alice"), contact(2L, "Bob")),
            onConfirmOneToOne = { oneToOneHandle = it },
            onConfirmGroup = { _, _, _, _, _, _ -> groupReported = true },
        )

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onNodeWithTag(NEW_CHAT_NEXT_FAB_TAG).performClick()

        composeTestRule.onNodeWithTag(NEW_CHAT_SETTINGS_TAG).assertIsNotDisplayed()
        assertThat(oneToOneHandle).isEqualTo(1L)
        assertThat(groupReported).isFalse()
    }

    @Test
    fun `test that selecting two contacts advances to the settings step`() {
        setScreen(dataState(contact(1L, "Alice"), contact(2L, "Bob")))

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[1].performClick()
        composeTestRule.onNodeWithTag(NEW_CHAT_NEXT_FAB_TAG).performClick()

        composeTestRule.onNodeWithTag(NEW_CHAT_SETTINGS_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that confirming from the settings step reports a group with the selected handles`() {
        var groupHandles: Set<Long>? = null
        var oneToOneReported = false
        setScreen(
            dataState(contact(1L, "Alice"), contact(2L, "Bob")),
            onConfirmOneToOne = { oneToOneReported = true },
            onConfirmGroup = { handles, _, _, _, _, _ -> groupHandles = handles },
        )

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[1].performClick()
        composeTestRule.onNodeWithTag(NEW_CHAT_NEXT_FAB_TAG).performClick()
        composeTestRule.onNodeWithTag(NEW_CHAT_CONFIRM_FAB_TAG).performClick()

        assertThat(groupHandles).containsExactly(1L, 2L)
        assertThat(oneToOneReported).isFalse()
    }

    @Test
    fun `test that tapping the remove icon reports the participant handle`() {
        val removed = mutableListOf<Long>()
        composeTestRule.setContent {
            SettingsStep(
                contacts = listOf(contact(1L, "Alice"), contact(2L, "Bob")).toImmutableSet(),
                selectedHandles = setOf(1L, 2L).toImmutableSet(),
                selectedCount = 2,
                tagPrefix = NEW_CHAT_TAG_PREFIX,
                onConfirm = { _, _, _, _, _ -> },
                onRemoveParticipant = { removed.add(it) },
                allowGroupImageSelection = false,
                onBack = {},
            )
        }

        composeTestRule.onNodeWithTag(NEW_CHAT_SETTINGS_LIST_TAG)
            .performScrollToNode(hasText("Alice"))
        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_REMOVE, useUnmergedTree = true)[0]
            .performSemanticsAction(SemanticsActions.OnClick)

        assertThat(removed).containsExactly(1L)
    }

    @Test
    fun `test that selected contacts remain listed in the settings step when a search filters them out of the visible list`() {
        setSearchableScreen(listOf(contact(1L, "Alice"), contact(2L, "Bob")))

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[1].performClick()

        composeTestRule.onNode(searchActionMatcher).performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("zzz")

        composeTestRule.onNodeWithTag(NEW_CHAT_NEXT_FAB_TAG).performClick()

        composeTestRule.onNodeWithTag(NEW_CHAT_SETTINGS_LIST_TAG)
            .performScrollToNode(hasText("Alice"))
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithTag(NEW_CHAT_SETTINGS_LIST_TAG)
            .performScrollToNode(hasText("Bob"))
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun `test that advancing to the settings step clears the active search`() {
        setSearchableScreen(listOf(contact(1L, "Alice"), contact(2L, "Bob")))

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[1].performClick()

        composeTestRule.onNode(searchActionMatcher).performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("zzz")

        // The search hides every contact before advancing.
        composeTestRule.onNodeWithTag(NEW_CHAT_EMPTY_TAG).assertIsDisplayed()

        composeTestRule.onNodeWithTag(NEW_CHAT_NEXT_FAB_TAG).performClick()
        composeTestRule.onNodeWithTag(NEW_CHAT_SETTINGS_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Navigation Icon").performClick()

        // Advancing cleared the search, so returning shows the full list again.
        composeTestRule.onNodeWithTag(NEW_CHAT_EMPTY_TAG).assertIsNotDisplayed()
        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW).assertCountEquals(2)
    }

    private fun setScreen(
        state: CreateChatUiState,
        onSearchQueryChange: (String?) -> Unit = {},
        onConfirmOneToOne: (Long) -> Unit = {},
        onConfirmGroup: (Set<Long>, String?, Boolean, Boolean, Boolean, String?) -> Unit =
            { _, _, _, _, _, _ -> },
        onBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            NewChatScreen(
                state = state,
                onSearchQueryChange = onSearchQueryChange,
                onConfirmOneToOne = onConfirmOneToOne,
                onConfirmGroup = onConfirmGroup,
                onBack = onBack,
            )
        }
    }

    /**
     * Hosts [NewChatScreen] with a stateful contacts list that filters like the real ViewModel: the
     * visible contacts shrink to those whose display name matches the query, while selection is
     * retained by the Compose selection state across the filter.
     */
    private fun setSearchableScreen(contacts: List<ContactItemUiState>) {
        composeTestRule.setContent {
            var state by remember {
                mutableStateOf<CreateChatUiState>(
                    CreateChatUiState.Data(
                        contacts = contacts.toImmutableList(),
                        query = null,
                        allowGroupImageSelection = true,
                    ),
                )
            }
            NewChatScreen(
                state = state,
                onSearchQueryChange = { query ->
                    val visible = if (query.isNullOrBlank()) {
                        contacts
                    } else {
                        contacts.filter { it.displayName.contains(query, ignoreCase = true) }
                    }
                    state = CreateChatUiState.Data(
                        contacts = visible.toImmutableList(),
                        query = query,
                        allowGroupImageSelection = true,
                    )
                },
                onConfirmOneToOne = {},
                onConfirmGroup = { _, _, _, _, _, _ -> },
                onBack = {},
            )
        }
    }

    private fun dataState(vararg contacts: ContactItemUiState) =
        CreateChatUiState.Data(
            contacts = contacts.toList().toImmutableList(),
            query = null,
            allowGroupImageSelection = true,
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

    /**
     * Matches the search action icon button in [mega.android.core.ui.components.toolbar.MegaSearchTopAppBar].
     * Both the action button and the search field's leading icon carry the "Search" content
     * description, so [Role.Button] singles out the clickable action that toggles search mode.
     */
    private val searchActionMatcher =
        hasContentDescription("Search") and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    private companion object {
        const val CONTACT_ITEM_VIEW_ROW = "contact_item_view:row"
        const val CONTACT_ITEM_VIEW_REMOVE = "contact_item_view:remove"
        const val NEW_CHAT_NEXT_FAB_TAG = "$NEW_CHAT_TAG_PREFIX$NEXT_FAB_SUFFIX"
        const val NEW_CHAT_SETTINGS_TAG = "$NEW_CHAT_TAG_PREFIX$SETTINGS_SUFFIX"
        const val NEW_CHAT_SETTINGS_LIST_TAG = "$NEW_CHAT_TAG_PREFIX$SETTINGS_LIST_SUFFIX"
        const val NEW_CHAT_CONFIRM_FAB_TAG = "$NEW_CHAT_TAG_PREFIX$CONFIRM_FAB_SUFFIX"
        const val NEW_CHAT_EMPTY_TAG = "$NEW_CHAT_TAG_PREFIX$EMPTY_SUFFIX"
    }
}
