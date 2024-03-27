package mega.privacy.android.core.ui.controls.camera

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraTimerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that camera timer shows correctly`() {
        val testTime = "00:00:00"

        composeTestRule.setContent {
            CameraTimer(formattedTime = testTime)
        }

        composeTestRule.onNodeWithText(testTime).assertIsDisplayed()
    }
}