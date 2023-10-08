package mega.privacy.android.feature.devicecenter.ui.lists.loading

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [DeviceCenterLoadingItem]
 */
@RunWith(AndroidJUnit4::class)
internal class DeviceCenterLoadingItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the loading item is displayed`() {
        composeTestRule.setContent { DeviceCenterLoadingItem() }
        composeTestRule.onNodeWithTag(DEVICE_CENTER_LOADING_ITEM).assertIsDisplayed()
    }
}