package mega.privacy.android.feature.chat.list.view

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.reflect.KClass
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.list.model.ChatListUiState
import mega.privacy.android.navigation.contract.state.LocalTopLevelNavKeyClass
import mega.privacy.android.navigation.destination.ChatListNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatListScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val navigationIconContentDescription = "Navigation Icon"

    private object OtherSectionNavKey : NavKey

    private fun setupComposeContent(topLevelNavKeyClass: KClass<out NavKey>?) {
        composeRule.setContent {
            CompositionLocalProvider(LocalTopLevelNavKeyClass provides topLevelNavKeyClass) {
                AndroidThemeForPreviews {
                    TestChatListScreen()
                }
            }
        }
    }

    @Composable
    private fun TestChatListScreen() {
        ChatListScreen(
            uiState = ChatListUiState.Loading,
            showMeetingTab = false,
            onBackClick = {},
            onItemClick = {},
            onNewChatClick = {},
            onArchivedClick = {},
            onSearchQueryChange = {},
        )
    }

    @Test
    fun `test that no back button is shown when the chat list is the navigation bar destination`() {
        setupComposeContent(topLevelNavKeyClass = ChatListNavKey::class)

        composeRule.onNodeWithContentDescription(navigationIconContentDescription)
            .assertDoesNotExist()
    }

    @Test
    fun `test that a back button is shown when another section is the navigation bar destination`() {
        setupComposeContent(topLevelNavKeyClass = OtherSectionNavKey::class)

        composeRule.onNodeWithContentDescription(navigationIconContentDescription)
            .assertIsDisplayed()
    }

    @Test
    fun `test that a back button is shown when hosted outside the main navigation display`() {
        setupComposeContent(topLevelNavKeyClass = null)

        composeRule.onNodeWithContentDescription(navigationIconContentDescription)
            .assertIsDisplayed()
    }
}
