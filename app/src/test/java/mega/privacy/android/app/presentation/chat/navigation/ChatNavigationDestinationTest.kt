package mega.privacy.android.app.presentation.chat.navigation

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.ChatActivity
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_ACTION
import mega.privacy.android.app.presentation.meeting.chat.model.EXTRA_LINK
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.feature.chat.navigation.CHAT_ROOM_ENTRY_TAG
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagResolver
import mega.privacy.android.navigation.contract.featureflag.LocalFeatureFlagResolver
import mega.privacy.android.navigation.contract.transparent.TransparentMetadata
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class ChatNavigationDestinationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val navigationHandler = mock<NavigationHandler>()
    private val removeDestination = mock<() -> Unit>()

    private val underTest: (NavKey) -> NavEntry<NavKey> = entryProvider {
        chatLegacyDestination(removeDestination = removeDestination)
        showChatMessagesDestination(navigationHandler = navigationHandler)
    }

    private val chatId = 123L

    @Test
    fun `test that chatLegacyDestination is not transparent when registered`() {
        val entry = underTest(ChatNavKey(chatId = chatId))

        assertThat(entry.metadata).doesNotContainKey(TransparentMetadata.KEY)
    }

    @Test
    fun `test that showChatMessagesDestination is not transparent when registered`() {
        val entry = underTest(ShowChatMessagesNavKey(chatId = chatId))

        assertThat(entry.metadata).doesNotContainKey(TransparentMetadata.KEY)
    }

    @Test
    fun `test that chatLegacyDestination launches ChatActivity and removes itself when the flag is disabled`() {
        val key = ChatNavKey(chatId = chatId, action = ACTION, link = LINK)

        setContent(key, isFlagEnabled = false)

        val intent = nextStartedActivity()
        assertThat(intent.component?.className).isEqualTo(ChatActivity::class.java.name)
        assertThat(intent.action).isEqualTo(ACTION)
        assertThat(intent.getLongExtra(ChatNavKey.LEGACY_CHAT_ID, -1L)).isEqualTo(chatId)
        assertThat(intent.getStringExtra(EXTRA_ACTION)).isEqualTo(ACTION)
        assertThat(intent.getStringExtra(EXTRA_LINK)).isEqualTo(LINK)
        verify(removeDestination).invoke()
        composeTestRule.onNodeWithTag(CHAT_ROOM_ENTRY_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that chatLegacyDestination shows the chat room when the flag is enabled`() {

        setContent(ChatNavKey(chatId = chatId), isFlagEnabled = true)

        composeTestRule.onNodeWithTag(CHAT_ROOM_ENTRY_TAG).assertIsDisplayed()
        assertThat(nextStartedActivityOrNull()).isNull()
        verify(removeDestination, never()).invoke()
    }

    @Test
    fun `test that showChatMessagesDestination launches ChatActivity and removes itself when the flag is disabled`() {
        val key = ShowChatMessagesNavKey(chatId = chatId, openFromList = true)

        setContent(key, isFlagEnabled = false)

        val intent = nextStartedActivity()
        assertThat(intent.component?.className).isEqualTo(ChatActivity::class.java.name)
        assertThat(intent.action).isEqualTo(Constants.ACTION_CHAT_SHOW_MESSAGES)
        assertThat(intent.getLongExtra(ChatNavKey.LEGACY_CHAT_ID, -1L)).isEqualTo(chatId)
        assertThat(intent.getBooleanExtra(ChatActivity.EXTRA_OPEN_FROM_LIST, false)).isTrue()
        verify(navigationHandler).remove(key)
        composeTestRule.onNodeWithTag(CHAT_ROOM_ENTRY_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that showChatMessagesDestination shows the chat room when the flag is enabled`() {
        val key = ShowChatMessagesNavKey(chatId = chatId)

        setContent(key, isFlagEnabled = true)

        composeTestRule.onNodeWithTag(CHAT_ROOM_ENTRY_TAG).assertIsDisplayed()
        assertThat(nextStartedActivityOrNull()).isNull()
        verify(navigationHandler, never()).remove(key)
    }

    private fun setContent(key: NavKey, isFlagEnabled: Boolean) {
        val entry = underTest(key)
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalFeatureFlagResolver provides FeatureFlagResolver { feature ->
                    feature == ApiFeatures.SingleActivityChatRoom && isFlagEnabled
                }
            ) {
                entry.Content()
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun nextStartedActivityOrNull(): Intent? =
        shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity

    private fun nextStartedActivity(): Intent = requireNotNull(nextStartedActivityOrNull())

    private companion object {
        const val ACTION = "action"
        const val LINK = "https://mega.nz/chat/link"
    }
}
