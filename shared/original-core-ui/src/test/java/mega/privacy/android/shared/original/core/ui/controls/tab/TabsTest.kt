package mega.privacy.android.shared.original.core.ui.controls.tab

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TabsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val title1 = "Title 1"
    private val title2 = "Title 2"
    private val tag1 = "tag1"
    private val tag2 = "tag2"
    private val view1 = "view1"
    private val view2 = "view2"

    @Test
    fun `test that Tabs displays correctly a two text cells view`() {
        with(composeTestRule) {
            setContent {
                Tabs(
                    cells = persistentListOf(
                        TextCell(
                            text = title1,
                            tag = tag1,
                        ) { Tab1() },
                        TextCell(
                            text = title2,
                            tag = tag2,
                        ) { Tab2() }
                    )
                )
            }

            onNodeWithText(title1).isDisplayed()
            onNodeWithTag(tag1).isDisplayed()
            onNodeWithTag(view1).isDisplayed()
            onNodeWithText(title2).isDisplayed()
            onNodeWithText(tag2).isDisplayed()
            onNodeWithTag(view2).isDisplayed()
        }
    }

    @Composable
    private fun Tab1() {
        Column(modifier = Modifier.testTag(view1)) {}
    }

    @Composable
    private fun Tab2() {
        Column(modifier = Modifier.testTag(view2)) {}
    }
}