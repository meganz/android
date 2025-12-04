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
class RecentsEmptyViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that empty message is displayed`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsEmptyView(
                    onUploadClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_EMPTY_TEXT_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that upload button is displayed`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsEmptyView(
                    onUploadClicked = {}
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_UPLOAD_BUTTON_TEST_TAG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onUploadClicked is called when upload button is clicked`() {
        var clicked = false

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsEmptyView(
                    onUploadClicked = { clicked = true }
                )
            }
        }

        composeRule.onNodeWithTag(RECENTS_UPLOAD_BUTTON_TEST_TAG, useUnmergedTree = true)
            .performClick()

        assertThat(clicked).isTrue()
    }
}

