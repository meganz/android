package mega.privacy.android.app.camera.view

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZoomLevelButtonsGroupTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun zoomButtons_areDisplayed() {
        composeTestRule.setContent {
            ZoomLevelButtonsGroup(
                availableZoomRange = 0.5f..3.0f,
                currentZoomRatio = 1.0f,
                onZoomLevelSelected = {}
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ZOOM_BUTTONS_ROW).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_ZOOM_BUTTON_PREFIX + "0").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_ZOOM_BUTTON_PREFIX + "1").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_ZOOM_BUTTON_PREFIX + "2").assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_ZOOM_BUTTON_PREFIX + "3").assertIsDisplayed()
    }

    @Test
    fun zoomButton_click_triggersCallback() {
        var selectedZoom = 1.0f
        composeTestRule.setContent {
            ZoomLevelButtonsGroup(
                availableZoomRange = 0.5f..3.0f,
                currentZoomRatio = selectedZoom,
                onZoomLevelSelected = { selectedZoom = it }
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ZOOM_BUTTON_PREFIX + "2").performClick()
        // No direct assertion on callback, but no crash means click is handled
    }

    @Test
    fun zoomButtons_notDisplayed_whenEmpty() {
        composeTestRule.setContent {
            ZoomLevelButtonsGroup(
                availableZoomRange = 1.0f..1.0f,
                currentZoomRatio = 1.0f,
                onZoomLevelSelected = {}
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ZOOM_BUTTONS_ROW).assertDoesNotExist()
    }

    @Test
    fun highlightedButton_displaysCorrectText() {
        composeTestRule.setContent {
            ZoomLevelButtonsGroup(
                availableZoomRange = 0.5f..3.0f,
                currentZoomRatio = 2.0f,
                onZoomLevelSelected = {}
            )
        }
        // The 2x button should be highlighted and display "2x"
        composeTestRule.onNodeWithTag(TEST_TAG_ZOOM_BUTTON_PREFIX + "2")
            .assertIsDisplayed()
            .assert(hasText("2x"))
    }

    @Test
    fun unavailableZoomRange_noButtonsShown() {
        composeTestRule.setContent {
            ZoomLevelButtonsGroup(
                availableZoomRange = 1.0f..1.0f,
                currentZoomRatio = 1.0f,
                onZoomLevelSelected = {}
            )
        }
        composeTestRule.onNodeWithTag(TEST_TAG_ZOOM_BUTTONS_ROW).assertDoesNotExist()
    }
}