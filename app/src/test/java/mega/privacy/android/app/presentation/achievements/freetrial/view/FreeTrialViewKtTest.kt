package mega.privacy.android.app.presentation.achievements.freetrial.view

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FreeTrialViewKtTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        isReceivedAward: Boolean = false,
        installButtonClicked: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            FreeTrialView(
                icon = iconPackR.drawable.ic_mega_vpn_free_trial,
                freeTrialText = stringResource(
                    sharedR.string.text_start_mega_vpn_free_trial,
                    "5 GB"
                ),
                installButtonText = sharedR.string.button_text_install_mega_vpn,
                howItWorksText = stringResource(
                    sharedR.string.text_how_it_works_mega_vpn_free_trial,
                    "5 GB"
                ),
                isReceivedAward = isReceivedAward,
                installButtonClicked = installButtonClicked
            )
        }
    }

    @Test
    fun `test that all views displayed as expected when isReceivedAward is false`() {
        setComposeContent()

        listOf(
            FreeTrialViewTestTags.TOOLBAR,
            FreeTrialViewTestTags.IMAGE_MAIN,
            FreeTrialViewTestTags.DESCRIPTION,
            FreeTrialViewTestTags.INSTALL_APP_BUTTON,
            FreeTrialViewTestTags.HOW_IT_WORKS_TITLE,
            FreeTrialViewTestTags.HOW_IT_WORKS_DESCRIPTION
        ).forEach {
            composeTestRule.onNodeWithTag(testTag = it, useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun `test that all views displayed as expected when isReceivedAward is true`() {
        setComposeContent(isReceivedAward = true)

        listOf(
            FreeTrialViewTestTags.TOOLBAR,
            FreeTrialViewTestTags.CHECK_ICON,
            FreeTrialViewTestTags.IMAGE_MAIN,
            FreeTrialViewTestTags.DESCRIPTION,
            FreeTrialViewTestTags.INSTALL_APP_BUTTON,
            FreeTrialViewTestTags.HOW_IT_WORKS_DESCRIPTION
        ).forEach {
            composeTestRule.onNodeWithTag(testTag = it, useUnmergedTree = true).assertIsDisplayed()
        }
    }

    @Test
    fun `test that installButtonClicked is invoked as expected`() {
        val installButtonClicked = mock<() -> Unit>()

        setComposeContent(installButtonClicked = installButtonClicked)

        composeTestRule.onNodeWithTag(
            testTag = FreeTrialViewTestTags.INSTALL_APP_BUTTON,
            useUnmergedTree = true
        ).performClick()
        verify(installButtonClicked).invoke()
    }
}