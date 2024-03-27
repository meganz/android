package mega.privacy.android.core.ui.controls.camera

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.core.R

@RunWith(AndroidJUnit4::class)
class CameraPreviewBottomBarTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that button is displayed`() {
        composeTestRule.setContent {
            CameraPreviewBottomBar(onSendVideo = {})
        }

        composeTestRule.onNodeWithTag(TEST_TAG_CAMERA_PREVIEW_BOTTOM_BAR_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.context_send))
            .assertIsDisplayed()
    }
}