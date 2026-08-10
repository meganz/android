package mega.privacy.mobile.home.presentation.home.widget.recents.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentsWidgetHeaderTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that title is displayed`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsWidgetHeader(
                    onOptionsClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(TITLE_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that menu button is displayed`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsWidgetHeader(
                    onOptionsClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_MENU_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that onOptionsClicked is called when menu button is clicked`() {
        var clicked = false

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsWidgetHeader(
                    onOptionsClicked = { clicked = true }
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_MENU_TEST_TAG).performClick()

        assertThat(clicked).isTrue()
    }
}

