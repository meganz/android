package mega.privacy.mobile.home.presentation.recents.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.mobile.analytics.event.RecentsEmptyStateUploadButtonPressedEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecentsEmptyViewTest {

    private val composeRule = createComposeRule()

    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeRule)

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

    @Test
    fun `test that RecentsEmptyStateUploadButtonPressedEvent is tracked when upload button is clicked`() {
        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsEmptyView(
                    onUploadClicked = {}
                )
            }
        }

        analyticsRule.events.clear()

        composeRule.onNodeWithTag(RECENTS_UPLOAD_BUTTON_TEST_TAG, useUnmergedTree = true)
            .performClick()

        assertThat(analyticsRule.events).contains(RecentsEmptyStateUploadButtonPressedEvent)
        assertThat(analyticsRule.events).hasSize(1)
    }

    @Test
    fun `test that RecentsEmptyStateUploadButtonPressedEvent is tracked before onUploadClicked is called`() {
        var uploadClickedCalled = false
        var wasEventTrackedBeforeCallback = false

        composeRule.setContent {
            AndroidThemeForPreviews {
                RecentsEmptyView(
                    onUploadClicked = {
                        wasEventTrackedBeforeCallback = analyticsRule.events.contains(
                            RecentsEmptyStateUploadButtonPressedEvent
                        )
                        uploadClickedCalled = true
                    }
                )
            }
        }

        analyticsRule.events.clear()

        composeRule.onNodeWithTag(RECENTS_UPLOAD_BUTTON_TEST_TAG, useUnmergedTree = true)
            .performClick()

        assertThat(uploadClickedCalled).isTrue()
        assertThat(wasEventTrackedBeforeCallback).isTrue()
    }
}

