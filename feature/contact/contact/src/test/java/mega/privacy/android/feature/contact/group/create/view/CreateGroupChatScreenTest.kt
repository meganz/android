package mega.privacy.android.feature.contact.group.create.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.feature.contact.group.create.model.CreateChatUiState
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateGroupChatScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that the shimmer loading view is displayed when state is Loading`() {
        setScreen(CreateChatUiState.Loading)

        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_LOADING_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_LIST_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that the empty view is displayed when there are no contacts`() {
        setScreen(
            CreateChatUiState.Data(
                contacts = persistentListOf(),
                query = null,
                allowGroupImageSelection = true,
            )
        )

        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_EMPTY_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the next fab is hidden until a contact is selected`() {
        setScreen(dataState(contact(1L, "Alice")))

        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_NEXT_FAB_TAG).assertIsNotDisplayed()

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()

        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_NEXT_FAB_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the next fab is shown with no selection when empty group is allowed`() {
        setScreen(dataState(contact(1L, "Alice")), allowEmptyGroup = true)

        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_NEXT_FAB_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that tapping next with no selection advances to settings when empty group is allowed`() {
        setScreen(dataState(contact(1L, "Alice")), allowEmptyGroup = true)

        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_NEXT_FAB_TAG).performClick()

        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_SETTINGS_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that tapping next advances to the settings step`() {
        setScreen(dataState(contact(1L, "Alice")))

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_NEXT_FAB_TAG).performClick()

        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_SETTINGS_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the chat link toggle is hidden when EKR is enabled`() {
        goToSettings(contact(1L, "Alice"))

        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_CHAT_LINK_TAG).assertIsDisplayed()

        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_EKR_TAG).performClick()

        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_CHAT_LINK_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that the settings step lists the selected contacts under a participant count header`() {
        goToSettings(contact(1L, "Alice"), contact(2L, "Bob"))

        val expectedHeader = composeTestRule.activity.resources.getQuantityString(
            sharedR.plurals.general_number_participants,
            2,
            2,
        )
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_SELECTED_CONTACTS_TAG)
            .assertTextEquals(expectedHeader)

        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_SETTINGS_LIST_TAG)
            .performScrollToNode(hasText("Alice"))
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()

        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_SETTINGS_LIST_TAG)
            .performScrollToNode(hasText("Bob"))
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun `test that the settings step lists only the selected contacts`() {
        setScreen(dataState(contact(1L, "Alice"), contact(2L, "Bob")))

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_NEXT_FAB_TAG).performClick()

        val expectedHeader = composeTestRule.activity.resources.getQuantityString(
            sharedR.plurals.general_number_participants,
            1,
            1,
        )
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_SELECTED_CONTACTS_TAG)
            .assertTextEquals(expectedHeader)

        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_SETTINGS_LIST_TAG)
            .performScrollToNode(hasText("Alice"))
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Bob").assertCountEquals(0)
    }

    @Test
    fun `test that confirming reports the selected handles and the chosen settings`() {
        var result: Result? = null
        setScreen(
            dataState(contact(1L, "Alice")),
            onConfirm = { handles, title, isEkr, isChatLink, allowAdd, _ ->
                result = Result(handles, title, isEkr, isChatLink, allowAdd)
            },
        )

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_NEXT_FAB_TAG).performClick()
        composeTestRule.onNode(hasImeAction(ImeAction.Done)).performTextInput("My group")
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_CONFIRM_FAB_TAG).performClick()

        assertThat(result).isNotNull()
        assertThat(result?.handles).containsExactly(1L)
        assertThat(result?.title).isEqualTo("My group")
        assertThat(result?.isEkr).isFalse()
        assertThat(result?.isChatLink).isFalse()
        // allow-add-participants defaults to on.
        assertThat(result?.allowAdd).isTrue()
    }

    @Test
    fun `test that enabling EKR forces the chat link off in the reported settings`() {
        var result: Result? = null
        setScreen(
            dataState(contact(1L, "Alice")),
            onConfirm = { handles, title, isEkr, isChatLink, allowAdd, _ ->
                result = Result(handles, title, isEkr, isChatLink, allowAdd)
            },
        )

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_NEXT_FAB_TAG).performClick()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_EKR_TAG).performClick()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_CONFIRM_FAB_TAG).performClick()

        assertThat(result?.isEkr).isTrue()
        assertThat(result?.isChatLink).isFalse()
    }

    @Test
    fun `test that a blank group name is reported as null`() {
        var result: Result? = null
        setScreen(
            dataState(contact(1L, "Alice")),
            onConfirm = { handles, title, isEkr, isChatLink, allowAdd, _ ->
                result = Result(handles, title, isEkr, isChatLink, allowAdd)
            },
        )

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_NEXT_FAB_TAG).performClick()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_CONFIRM_FAB_TAG).performClick()

        assertThat(result?.title).isNull()
    }

    @Test
    fun `test that enabling get chat link with a blank name blocks confirm and shows an inline error`() {
        var result: Result? = null
        setScreen(
            dataState(contact(1L, "Alice")),
            onConfirm = { handles, title, isEkr, isChatLink, allowAdd, _ ->
                result = Result(handles, title, isEkr, isChatLink, allowAdd)
            },
        )

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_NEXT_FAB_TAG).performClick()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_CHAT_LINK_TAG).performClick()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_CONFIRM_FAB_TAG).performClick()

        assertThat(result).isNull()
        val error = composeTestRule.activity
            .getString(sharedR.string.create_group_chat_link_requires_name_error)
        composeTestRule.onNodeWithText(error).assertIsDisplayed()
    }

    @Test
    fun `test that entering a name clears the chat link error and allows confirm`() {
        var result: Result? = null
        setScreen(
            dataState(contact(1L, "Alice")),
            onConfirm = { handles, title, isEkr, isChatLink, allowAdd, _ ->
                result = Result(handles, title, isEkr, isChatLink, allowAdd)
            },
        )

        composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[0].performClick()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_NEXT_FAB_TAG).performClick()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_CHAT_LINK_TAG).performClick()
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_CONFIRM_FAB_TAG).performClick()

        assertThat(result).isNull()

        composeTestRule.onNode(hasImeAction(ImeAction.Done)).performTextInput("My group")
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_CONFIRM_FAB_TAG).performClick()

        assertThat(result).isNotNull()
        assertThat(result?.isChatLink).isTrue()
        assertThat(result?.title).isEqualTo("My group")
    }

    private fun goToSettings(vararg contacts: ContactItemUiState) {
        setScreen(dataState(*contacts))
        contacts.indices.forEach { index ->
            composeTestRule.onAllNodesWithTag(CONTACT_ITEM_VIEW_ROW)[index].performClick()
        }
        composeTestRule.onNodeWithTag(CREATE_GROUP_CHAT_NEXT_FAB_TAG).performClick()
    }

    private fun setScreen(
        state: CreateChatUiState,
        allowEmptyGroup: Boolean = false,
        onSearchQueryChange: (String?) -> Unit = {},
        onConfirm: (Set<Long>, String?, Boolean, Boolean, Boolean, String?) -> Unit =
            { _, _, _, _, _, _ -> },
        onBack: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CreateGroupChatScreen(
                state = state,
                allowEmptyGroup = allowEmptyGroup,
                onSearchQueryChange = onSearchQueryChange,
                onConfirm = onConfirm,
                onBack = onBack,
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

    private data class Result(
        val handles: Set<Long>,
        val title: String?,
        val isEkr: Boolean,
        val isChatLink: Boolean,
        val allowAdd: Boolean,
    )

    private companion object {
        const val CONTACT_ITEM_VIEW_ROW = "contact_item_view:row"
    }
}
