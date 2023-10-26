package test.mega.privacy.android.app.upgradeAccount

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.upgradeAccount.view.FEATURE_TITLE
import mega.privacy.android.app.upgradeAccount.view.IMAGE_TAG
import mega.privacy.android.app.upgradeAccount.view.PRO_PLAN_TEXT
import mega.privacy.android.app.upgradeAccount.view.PRO_PLAN_TITLE
import mega.privacy.android.app.upgradeAccount.view.SECURITY_DESCRIPTION_ROW
import mega.privacy.android.app.upgradeAccount.view.SKIP_BUTTON
import mega.privacy.android.app.upgradeAccount.view.STORAGE_DESCRIPTION_ROW
import mega.privacy.android.app.upgradeAccount.view.TRANSFER_DESCRIPTION_ROW
import mega.privacy.android.app.upgradeAccount.view.VIEW_PRO_PLAN_BUTTON
import mega.privacy.android.app.upgradeAccount.view.VariantAOnboardingDialogView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class VariantAOnboardingDialogViewTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that image is shown`() {
        composeRule.setContent {
            VariantAOnboardingDialogView(onSkipPressed = {}, onViewPlansPressed = {})
        }
        composeRule.onNodeWithTag(IMAGE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that pro plan row is shown correctly`() {
        composeRule.setContent {
            VariantAOnboardingDialogView(onSkipPressed = {}, onViewPlansPressed = {})
        }
        composeRule.onNodeWithTag(PRO_PLAN_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(PRO_PLAN_TEXT).assertIsDisplayed()
    }

    @Test
    fun `test that title for features is shown correctly`() {
        composeRule.setContent {
            VariantAOnboardingDialogView(onSkipPressed = {}, onViewPlansPressed = {})
        }
        composeRule.onNodeWithTag(FEATURE_TITLE).assertIsDisplayed()
    }

    @Test
    fun `test that storage row is displayed`() {
        composeRule.setContent {
            VariantAOnboardingDialogView(onSkipPressed = {}, onViewPlansPressed = {})
        }
        composeRule.onNodeWithTag(STORAGE_DESCRIPTION_ROW).assertIsDisplayed()
    }

    @Test
    fun `test that transfer row is displayed`() {
        composeRule.setContent {
            VariantAOnboardingDialogView(onSkipPressed = {}, onViewPlansPressed = {})
        }
        composeRule.onNodeWithTag(TRANSFER_DESCRIPTION_ROW).assertIsDisplayed()
    }

    @Test
    fun `test that security row is displayed`() {
        composeRule.setContent {
            VariantAOnboardingDialogView(onSkipPressed = {}, onViewPlansPressed = {})
        }
        composeRule.onNodeWithTag(SECURITY_DESCRIPTION_ROW).assertIsDisplayed()
    }

    @Test
    fun `test that skip button is shown correctly`() {
        composeRule.setContent {
            VariantAOnboardingDialogView(onSkipPressed = {}, onViewPlansPressed = {})
        }
        composeRule.onNodeWithTag(SKIP_BUTTON).assertIsDisplayed()
    }

    @Test
    fun `test that view pro plan button is shown correctly`() {
        composeRule.setContent {
            VariantAOnboardingDialogView(onSkipPressed = {}, onViewPlansPressed = {})
        }
        composeRule.onNodeWithTag(VIEW_PRO_PLAN_BUTTON).assertIsDisplayed()
    }
}