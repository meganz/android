package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageNotSentBottomSheetTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that the header is displayed`() {
        initComposeRule()
        composeRule.onNodeWithTag(TEST_TAG_SEND_ERROR_HEADER).assertIsDisplayed()
    }

    @Test
    fun `test that actions are displayed`() {
        val tag1 = "box1"
        val tag2 = "box2"
        val actions = listOf<@Composable () -> Unit>(@Composable {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .testTag(tag1)
            )
        }, @Composable {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .testTag(tag2)
            )
        })
        initComposeRule(actions)
        composeRule.onNodeWithTag(tag1).assertIsDisplayed()
        composeRule.onNodeWithTag(tag2).assertIsDisplayed()
    }

    private fun initComposeRule(actions: List<@Composable () -> Unit> = emptyList<@Composable () -> Unit>()) {
        composeRule.setContent {
            MessageNotSentBottomSheet(
                actions = actions,
            )
        }
    }
}