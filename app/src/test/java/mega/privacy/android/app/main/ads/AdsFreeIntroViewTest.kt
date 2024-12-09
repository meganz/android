package mega.privacy.android.app.main.ads

import mega.privacy.android.shared.resources.R as sharedR
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class AdsFreeIntroViewTest {

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that ads free intro view shows correctly`() {
        initAdsFreeIntroView()

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.payment_ads_free_intro_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.payment_ads_free_intro_description))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.payment_ads_free_intro_generous_storage_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.payment_ads_free_intro_generous_storage_description))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.payment_ads_free_intro_transfer_sharing_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.payment_ads_free_intro_transfer_sharing_description))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.payment_ads_free_intro_additional_security_label))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.payment_ads_free_intro_additional_security_description))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(ADS_FREE_IMAGE_TEST_TAG)
            .assertExists()
    }

    private fun initAdsFreeIntroView() {
        composeTestRule.setContent {
            AdsFreeIntroView(
                onDismiss = {}
            )
        }
    }
}