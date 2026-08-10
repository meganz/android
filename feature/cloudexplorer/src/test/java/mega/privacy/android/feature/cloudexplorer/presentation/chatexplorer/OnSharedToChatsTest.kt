package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.navigation.destination.ChatNavKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class OnSharedToChatsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that sharing to a single chat navigates to it and closes the explorer`() {
        var navigated: NavKey? = null
        var closed = false
        val handler = handler(onNavigate = { navigated = it }, onClose = { closed = true })

        composeTestRule.runOnIdle { handler(listOf(CHAT_ID)) }

        assertThat(navigated).isEqualTo(ChatNavKey(chatId = CHAT_ID))
        assertThat(closed).isTrue()
    }

    @Test
    fun `test that sharing to no chats does nothing`() {
        var navigated: NavKey? = null
        var closed = false
        val handler = handler(onNavigate = { navigated = it }, onClose = { closed = true })

        composeTestRule.runOnIdle { handler(emptyList()) }

        assertThat(navigated).isNull()
        assertThat(closed).isFalse()
    }

    private fun handler(
        onNavigate: (NavKey) -> Unit,
        onClose: () -> Unit,
    ): (List<Long>) -> Unit {
        lateinit var captured: (List<Long>) -> Unit
        composeTestRule.setContent {
            captured = rememberOnSharedToChats(
                onNavigate = onNavigate,
                onCloseExplorerScreen = onClose,
            )
        }
        return captured
    }

    private companion object {
        const val CHAT_ID = 42L
    }
}
