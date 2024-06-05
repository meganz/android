package mega.privacy.android.shared.original.core.ui.controls.layouts

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.shared.original.core.ui.controls.other.COUNTER_TEXT_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FastScrollLazyColumnTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that content is shown`() {
        with(composeRule) {
            val expectedText = "test"
            setContent {
                FastScrollLazyColumn(totalItems = 10, tooltipText = { "" }) {
                    item {
                        Text(expectedText)
                    }
                }
            }
            onNodeWithText(expectedText).assertIsDisplayed()
        }
    }

    @Test
    fun `test that thumb icon is shown when the list is scrolled`() {
        with(composeRule) {
            val items = (0..1000).map { it }
            setContent {
                FastScrollLazyColumn(
                    totalItems = items.size,
                    tooltipText = { "$it" },
                    modifier = Modifier.size(700.dp)
                ) {
                    itemsIndexed(items = items) { index, _ ->
                        Text("$index", modifier = Modifier.testTag("$index"))
                    }
                }
            }
            onNodeWithTag(THUMB_TAG).assertDoesNotExist()
            onNodeWithTag(LAZY_COLUMN_TAG).performTouchInput {
                down(center)
                moveBy(Offset(0f, -200f)) //simulate swipe but without touch up event
            }
            onNodeWithTag(THUMB_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that tooltip icon is shown when the thumb is pressed`() {
        with(composeRule) {
            val items = (0..1000).map { it }
            setContent {
                FastScrollLazyColumn(
                    totalItems = items.size,
                    tooltipText = { "$it" },
                    modifier = Modifier.size(700.dp)
                ) {
                    itemsIndexed(items = items) { index, _ ->
                        Text("$index", modifier = Modifier.testTag("$index"))
                    }
                }
            }
            onNodeWithTag(LAZY_COLUMN_TAG).performTouchInput {
                down(center)
                moveBy(Offset(0f, -200f)) //simulate swipe but without touch up event
            }
            onNodeWithTag(COUNTER_TEXT_TAG).assertIsNotDisplayed()
            onNodeWithTag(THUMB_TAG).performTouchInput {
                up()
                down(center)
            }
            onNodeWithTag(COUNTER_TEXT_TAG).assertIsDisplayed()
        }
    }
}