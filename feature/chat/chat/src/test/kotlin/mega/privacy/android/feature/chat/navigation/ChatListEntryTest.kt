package mega.privacy.android.feature.chat.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatListEntryTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that CreateNewChatRequestEffect opens the new chat flow when it is requested`() {
        var requests = 0
        composeRule.setContent {
            CreateNewChatRequestEffect(requested = true, onRequestNewChat = { requests++ })
        }
        composeRule.waitForIdle()

        assertThat(requests).isEqualTo(1)
    }

    @Test
    fun `test that CreateNewChatRequestEffect does not open the new chat flow when it is not requested`() {
        var requests = 0
        composeRule.setContent {
            CreateNewChatRequestEffect(requested = false, onRequestNewChat = { requests++ })
        }
        composeRule.waitForIdle()

        assertThat(requests).isEqualTo(0)
    }

    @Test
    fun `test that CreateNewChatRequestEffect does not open the new chat flow again after state restoration`() {
        var requests = 0
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            CreateNewChatRequestEffect(requested = true, onRequestNewChat = { requests++ })
        }
        composeRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertThat(requests).isEqualTo(1)
    }
}
