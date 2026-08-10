package mega.privacy.android.feature.contact.group.view

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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.palm.composestateevents.consumed
import mega.privacy.android.feature.contact.group.model.ContactGroupItem
import mega.privacy.android.feature.contact.group.model.ContactGroupUiState
import mega.privacy.android.shared.contact.model.AvatarData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactGroupsScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that loading view is displayed when state is Loading`() {
        setScreen(ContactGroupUiState.Loading)
        composeTestRule.onNodeWithTag(CONTACT_GROUPS_LOADING_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CONTACT_GROUPS_LAZY_COLUMN_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that fab is hidden when state is Loading`() {
        setScreen(ContactGroupUiState.Loading)
        composeTestRule.onNodeWithTag(CONTACT_GROUPS_FAB_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that fab is displayed and triggers onCreateGroupClick when state is Data`() {
        var createClicked = false
        setScreen(dataState(), onCreateGroupClick = { createClicked = true })

        composeTestRule.onNodeWithTag(CONTACT_GROUPS_FAB_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CONTACT_GROUPS_FAB_TAG).performClick()
        assert(createClicked)
    }

    @Test
    fun `test that empty state is displayed when there are no groups`() {
        setScreen(dataState(groups = emptyList()))
        composeTestRule.onNodeWithTag(CONTACT_GROUPS_EMPTY_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that group rows render for each group`() {
        setScreen(dataState(groups = listOf(group(1L, "Alpha"), group(2L, "Beta"))))
        composeTestRule.onNodeWithTag(contactGroupRowTag(1L)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(contactGroupRowTag(2L)).assertIsDisplayed()
    }

    @Test
    fun `test that group row click invokes onGroupClick with chat id`() {
        var receivedChatId: Long? = null
        setScreen(
            state = dataState(groups = listOf(group(42L, "Alpha"))),
            onGroupClick = { receivedChatId = it },
        )

        composeTestRule.onNodeWithTag(contactGroupRowTag(42L)).performClick()
        assert(receivedChatId == 42L)
    }

    @Test
    fun `test that private indicator is shown for private groups only`() {
        setScreen(
            dataState(
                groups = listOf(
                    group(1L, "Private", isPrivate = true),
                    group(2L, "Public", isPrivate = false),
                )
            )
        )
        composeTestRule
            .onNodeWithTag(contactGroupPrivateIconTag(1L), useUnmergedTree = true)
            .assertExists()
        composeTestRule
            .onAllNodesWithTag(contactGroupPrivateIconTag(2L), useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun `test that search action toggles into search mode`() {
        setScreen(dataState())
        composeTestRule.onNode(searchActionMatcher).performClick()
        composeTestRule.onNode(hasSetTextAction()).assertIsDisplayed()
    }

    @Test
    fun `test that typing in search input fires onSearchQueryChange with text`() {
        var lastQuery: String? = null
        setScreen(dataState(), onSearchQueryChange = { lastQuery = it })

        composeTestRule.onNode(searchActionMatcher).performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("alp")

        assert(lastQuery == "alp")
    }

    /**
     * Matches the search action icon button in [mega.android.core.ui.components.toolbar.MegaSearchTopAppBar].
     */
    private val searchActionMatcher =
        hasContentDescription("Search") and
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    private fun setScreen(
        state: ContactGroupUiState,
        onSearchQueryChange: (String?) -> Unit = {},
        onGroupClick: (Long) -> Unit = {},
        onCreateGroupClick: () -> Unit = {},
        onNavigateToChat: (Long) -> Unit = {},
        onBackClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ContactGroupsScreen(
                state = state,
                onSearchQueryChange = onSearchQueryChange,
                onGroupClick = onGroupClick,
                onCreateGroupClick = onCreateGroupClick,
                onGroupChatCreatedConsumed = {},
                onNavigateToChat = onNavigateToChat,
                onBackClick = onBackClick,
            )
        }
    }

    private fun dataState(
        groups: List<ContactGroupItem> = listOf(group(1L, "Alpha")),
    ) = ContactGroupUiState.Data(
        groups = groups,
        groupChatCreated = consumed(),
    )

    private fun group(
        chatId: Long,
        name: String = "Group $chatId",
        isPrivate: Boolean = false,
    ) = ContactGroupItem(
        chatId = chatId,
        name = name,
        avatarData = listOf(
            AvatarData.Initials(
                initials = name.first().toString(),
                avatarColor = Color(0xFF2E7D32),
            ),
        ),
        isPrivate = isPrivate,
    )
}
