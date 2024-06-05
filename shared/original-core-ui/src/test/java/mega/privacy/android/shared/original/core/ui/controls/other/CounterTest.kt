package mega.privacy.android.shared.original.core.ui.controls.other

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CounterTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that counter shows the specified text`() {
        with(composeRule) {
            val expected = "Some text"
            setContent {
                Counter(text = expected)
            }
            onNodeWithTag(COUNTER_TEXT_TAG).apply {
                assertIsDisplayed()
                assertTextEquals(expected)
            }
        }
    }
}