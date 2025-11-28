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
class RecentsHiddenViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that hidden message is displayed`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsHiddenView(
                    onShowActivityClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_HIDDEN_TEXT_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that show activity button is displayed`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsHiddenView(
                    onShowActivityClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_HIDDEN_BUTTON_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onShowActivityClick is called when show activity button is clicked`() {
        var clicked = false

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsHiddenView(
                    onShowActivityClicked = { clicked = true }
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_HIDDEN_BUTTON_TEST_TAG, useUnmergedTree = true)
            .performClick()

        assertThat(clicked).isTrue()
    }
}

