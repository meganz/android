package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoPlayerGestureSliderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that percentage text shows 0 percent when value is 0`() {
        composeRule.setContent {
            VideoPlayerGestureSlider(value = 0f, type = GestureSliderType.Volume)
        }
        composeRule.onNodeWithText("0%").assertIsDisplayed()
    }

    @Test
    fun `test that percentage text shows 50 percent when value is 0 point 5`() {
        composeRule.setContent {
            VideoPlayerGestureSlider(value = 0.5f, type = GestureSliderType.Volume)
        }
        composeRule.onNodeWithText("50%").assertIsDisplayed()
    }

    @Test
    fun `test that percentage text shows 100 percent when value is 1`() {
        composeRule.setContent {
            VideoPlayerGestureSlider(value = 1f, type = GestureSliderType.Volume)
        }
        composeRule.onNodeWithText("100%").assertIsDisplayed()
    }

    @Test
    fun `test that brightness icon is shown for brightness type`() {
        composeRule.setContent {
            VideoPlayerGestureSlider(value = 0.5f, type = GestureSliderType.Brightness)
        }
        composeRule.onNodeWithContentDescription(SLIDER_CONTENT_DESC_BRIGHTNESS).assertIsDisplayed()
    }

    @Test
    fun `test that volume muted icon is shown when value is 0`() {
        composeRule.setContent {
            VideoPlayerGestureSlider(value = 0f, type = GestureSliderType.Volume)
        }
        composeRule.onNodeWithContentDescription(SLIDER_CONTENT_DESC_VOLUME_MUTED)
            .assertIsDisplayed()
    }

    @Test
    fun `test that volume low icon is shown when value is less than 0 point 2`() {
        composeRule.setContent {
            VideoPlayerGestureSlider(value = 0.1f, type = GestureSliderType.Volume)
        }
        composeRule.onNodeWithContentDescription(SLIDER_CONTENT_DESC_VOLUME_LOW).assertIsDisplayed()
    }

    @Test
    fun `test that volume medium icon is shown when value is between 0 point 2 and 0 point 5`() {
        composeRule.setContent {
            VideoPlayerGestureSlider(value = 0.3f, type = GestureSliderType.Volume)
        }
        composeRule.onNodeWithContentDescription(SLIDER_CONTENT_DESC_VOLUME_MEDIUM)
            .assertIsDisplayed()
    }

    @Test
    fun `test that volume high icon is shown when value is 0 point 5 or above`() {
        composeRule.setContent {
            VideoPlayerGestureSlider(value = 0.8f, type = GestureSliderType.Volume)
        }
        composeRule.onNodeWithContentDescription(SLIDER_CONTENT_DESC_VOLUME_HIGH)
            .assertIsDisplayed()
    }
}
