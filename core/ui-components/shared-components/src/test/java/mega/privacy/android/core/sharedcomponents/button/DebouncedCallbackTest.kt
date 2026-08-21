package mega.privacy.android.core.sharedcomponents.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DebouncedCallbackTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setKeyedContent(onEvent: (String) -> Unit) {
        composeTestRule.setContent {
            val callback = rememberDebouncedCallback(
                key = { value: String -> value },
                onEvent = onEvent,
            )
            Column {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("a")
                        .clickable { callback("a") },
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("b")
                        .clickable { callback("b") },
                )
            }
        }
    }

    @Test
    fun `test that onEvent is invoked once per debounce window`() {
        val received = mutableListOf<String>()
        setKeyedContent { received += it }

        repeat(3) { composeTestRule.onNodeWithTag("a").performClick() }
        assertThat(received).containsExactly("a")

        composeTestRule.mainClock.advanceTimeBy(DEFAULT_DEBOUNCE_DURATION.inWholeMilliseconds + 1)
        composeTestRule.onNodeWithTag("a").performClick()

        assertThat(received).containsExactly("a", "a")
    }

    @Test
    fun `test that an event with a different key is not dropped`() {
        val received = mutableListOf<String>()
        setKeyedContent { received += it }

        composeTestRule.onNodeWithTag("a").performClick()
        composeTestRule.onNodeWithTag("b").performClick()

        assertThat(received).containsExactly("a", "b").inOrder()
    }
}
