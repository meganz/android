package mega.privacy.android.feature.devicecenter.ui.lists.loading

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [DeviceCenterLoadingScreen]
 */
@RunWith(AndroidJUnit4::class)
internal class DeviceCenterLoadingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the loading screen is displayed`() {
        composeTestRule.setContent { DeviceCenterLoadingScreen() }
        composeTestRule.onNodeWithTag(DEVICE_CENTER_LOADING_SCREEN).assertIsDisplayed()
    }
}