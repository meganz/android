package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.sync.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
@RunWith(AndroidJUnit4::class)
internal class SyncEmptyScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that all sync empty screen components are visible`() {
        composeTestRule.setContent {
            SyncEmptyScreen({})
        }

        composeTestRule.onNodeWithTag(TAG_SYNC_EMPTY_SCREEN_TOOLBAR)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_SYNC_EMPTY_SCREEN_ILLUSTRATION)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_SYNC_EMPTY_ONBOARDING_TITLE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("You will need to set up a local folder on your \n" + "device that would pair with a chosen folder on \n" + "your Cloud Drive.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.start_screen_setting))
            .assertIsDisplayed()
    }
}