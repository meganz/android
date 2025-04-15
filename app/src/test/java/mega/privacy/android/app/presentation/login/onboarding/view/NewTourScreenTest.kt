package mega.privacy.android.app.presentation.login.onboarding.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.login.onboarding.view.TourTestTags.CREATE_ACCOUNT_BUTTON
import mega.privacy.android.app.presentation.login.onboarding.view.TourTestTags.LOG_IN_BUTTON
import mega.privacy.android.app.presentation.login.onboarding.view.TourTestTags.ONBOARDING_IMAGE
import mega.privacy.android.app.presentation.login.onboarding.view.TourTestTags.ONBOARDING_SUBTITLE
import mega.privacy.android.app.presentation.login.onboarding.view.TourTestTags.ONBOARDING_TITLE
import mega.privacy.android.app.presentation.login.onboarding.view.TourTestTags.PAGER_INDICATOR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class NewTourScreenTest {

    @get:Rule
    val composeRule = createComposeRule()


    @Test
    fun `test that tour onboarding title text for first page exists`() {
        with(composeRule) {
            setScreen()
            onNodeWithTag("${ONBOARDING_TITLE}_0").assertExists()
        }
    }

    @Test
    fun `test that onboarding sub title text for first page exists`() {
        with(composeRule) {
            setScreen()
            onNodeWithTag("${ONBOARDING_SUBTITLE}_0").assertExists()
        }
    }


    @Test
    fun `test that onboarding image for first page exists`() {
        with(composeRule) {
            setScreen()
            onNodeWithTag("${ONBOARDING_IMAGE}_0").assertExists()
        }
    }

    @Test
    fun `test that pager indicator is displayed`() {
        with(composeRule) {
            setScreen()
            onNodeWithTag(PAGER_INDICATOR).assertIsDisplayed()
        }
    }

    @Test
    fun `test that create account button is displayed`() {
        with(composeRule) {
            setScreen()
            onNodeWithTag(CREATE_ACCOUNT_BUTTON).assertIsDisplayed()
        }
    }

    @Test
    fun `test that login button is displayed`() {
        with(composeRule) {
            setScreen()
            onNodeWithTag(LOG_IN_BUTTON).assertIsDisplayed()
        }
    }

    @Test
    fun `test that onLoginClick is invoked when login button is clicked`() {
        val loginClick: () -> Unit = mock {}
        with(composeRule) {
            setScreen(onLoginClick = loginClick)
            onNodeWithTag(LOG_IN_BUTTON).performClick()
        }
        verify(loginClick).invoke()
    }

    @Test
    fun `test that onCreateAccountClick is invoked when create account button is clicked`() {
        val createAccountClick: () -> Unit = mock {}
        with(composeRule) {
            setScreen(onCreateAccountClick = createAccountClick)
            onNodeWithTag(CREATE_ACCOUNT_BUTTON).performClick()
        }
        verify(createAccountClick).invoke()
    }

    private fun ComposeContentTestRule.setScreen(
        onLoginClick: () -> Unit = {},
        onCreateAccountClick: () -> Unit = {},
    ) {
        setContent {
            NewTourScreen(
                onLoginClick = onLoginClick,
                onCreateAccountClick = onCreateAccountClick
            )
        }
    }
}