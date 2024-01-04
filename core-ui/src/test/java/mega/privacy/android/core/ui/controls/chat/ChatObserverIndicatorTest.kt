package mega.privacy.android.core.ui.controls.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatObserverIndicatorTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that observer indicator shows correctly`() {
        val numObservers = "2"
        with(composeRule) {
            setContent {
                ChatObserverIndicator(numObservers)
            }
            onNodeWithTag(TEST_TAG_OBSERVER_INDICATOR).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_OBSERVER_ICON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_OBSERVER_NUMBER).assertIsDisplayed()
            onNodeWithText(numObservers).assertIsDisplayed()
        }
    }
}