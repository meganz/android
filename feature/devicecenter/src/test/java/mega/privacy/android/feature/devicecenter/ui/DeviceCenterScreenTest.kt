package mega.privacy.android.feature.devicecenter.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [DeviceCenterScreen]
 */
@RunWith(AndroidJUnit4::class)
internal class DeviceCenterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that all device center components are displayed`() {
        composeTestRule.setContent { DeviceCenterScreen() }
        composeTestRule.onNodeWithTag(TAG_DEVICE_CENTER_SCREEN).assertIsDisplayed()
    }
}