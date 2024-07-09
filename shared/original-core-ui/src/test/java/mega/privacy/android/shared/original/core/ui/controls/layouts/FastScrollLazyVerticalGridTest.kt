package mega.privacy.android.shared.original.core.ui.controls.layouts

import THUMB_TAG
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FastScrollLazyVerticalGridTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that content is shown`() {
        with(composeRule) {
            val expectedText = "test"
            setContent {
                FastScrollLazyVerticalGrid(
                    totalItems = 10,
                    columns = GridCells.Fixed(1),
                    tooltipText = { "" }
                ) {
                    items(1) {
                        Text(expectedText)
                    }
                }
            }
            onNodeWithText(expectedText).assertIsDisplayed()
        }
    }

    @Test
    fun `test that thumb icon is shown when the grid is scrolled`() {
        with(composeRule) {
            val items = (0..1000).map { it }
            setContent {
                FastScrollLazyVerticalGrid(
                    totalItems = items.size,
                    columns = GridCells.Fixed(1),
                    tooltipText = { "$it" },
                    modifier = Modifier.size(700.dp)
                ) {
                    itemsIndexed(items) { index, _ ->
                        Text("$index", modifier = Modifier.testTag("$index"))
                    }
                }
            }
            onNodeWithTag(THUMB_TAG).assertDoesNotExist()
            onNodeWithTag(LAZY_GRID_TAG).performTouchInput {
                down(center)
                moveBy(Offset(0f, -200f)) // Simulate a scroll
            }
            onNodeWithTag(THUMB_TAG).assertIsDisplayed()
        }
    }
}