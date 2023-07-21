package test.mega.privacy.android.app.presentation.twofactorauthentication.view

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.InitialisationScreen
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.LOCK_IMAGE_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.fromId
import test.mega.privacy.android.app.hasDrawable

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class InitialisationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setupRule() {
        composeRule.setContent {
            InitialisationScreen(onNextClicked = { })
        }
    }

    @Test
    fun `test that title is shown`() {
        setupRule()
        composeRule.onNodeWithText(fromId(R.string.title_2fa)).assertIsDisplayed()
    }

    @Test
    fun `test that description is shown`() {
        setupRule()
        composeRule.onNodeWithText(fromId(R.string.two_factor_authentication_explain))
            .assertIsDisplayed()
    }

    @Test
    fun `test that Begin Setup button is shown`() {
        setupRule()
        composeRule.onNodeWithText(fromId(R.string.button_setup_2fa)).assertIsDisplayed()
    }

    @Test
    fun `test that Begin Setup button is clickable`() {
        setupRule()
        composeRule.onNodeWithText(fromId(R.string.button_setup_2fa)).assertHasClickAction()
    }

    @Test
    fun `test that Lock Image is visible`() {
        setupRule()
        composeRule.onNodeWithTag(LOCK_IMAGE_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that Lock Image contains the right drawable`() {
        setupRule()
        composeRule.onNode(hasDrawable(R.drawable.ic_2fa)).assertIsDisplayed()
    }
}