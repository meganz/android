package mega.privacy.android.app.presentation.login.onboarding.view

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.login.onboarding.model.TourUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.util.ReflectionHelpers


@RunWith(AndroidJUnit4::class)
class TourScreenKtTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that join a meeting as guest button is displayed`() {
        with(composeRule) {
            setScreen()
            onNodeWithTag(JOIN_A_MEETING_AS_GUEST_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that login button is displayed`() {
        with(composeRule) {
            setScreen()
            onNodeWithTag(BUTTON_LOGIN_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that create account button is displayed`() {
        with(composeRule) {
            setScreen()
            onNodeWithTag(BUTTON_CREATE_ACCOUNT_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that tour title text for all pages exists`() {
        with(composeRule) {
            setScreen()
            for (index in 0 until 5) {
                onNodeWithTag("${TEXT_TOUR_TITLE_TAG}_$index").assertExists()
            }
        }
    }

    @Test
    fun `test that tour description text for all pages exists`() {
        with(composeRule) {
            setScreen()
            for (index in 0 until 5) {
                onNodeWithTag("${TEXT_TOUR_DESCRIPTION_TAG}_$index").assertExists()
            }
        }
    }


    @Test
    fun `test that tour image resource for all pages exists`() {
        with(composeRule) {
            setScreen()
            for (index in 0 until 5) {
                onNodeWithTag("${IMAGE_RESOURCE_TOUR_TAG}_$index").assertExists()
            }
        }
    }

    @Test
    fun `test that tour image background for all pages exists`() {
        with(composeRule) {
            setScreen()
            for (index in 0 until 5) {
                onNodeWithTag("${IMAGE_BACKGROUND_TOUR_TAG}_$index").assertExists()
            }
        }
    }

    @Test
    fun `test that the correct meeting link is returned when opening the link`() {
        val meetingLink = "https://mega.co.nz/meetingLink"
        with(composeRule) {
            var actualLink: String? = null
            setScreen(
                uiState = TourUiState(
                    meetingLink = meetingLink,
                    shouldOpenLink = true
                ),
                onOpenLink = { actualLink = it }
            )

            Truth.assertThat(actualLink).isEqualTo(meetingLink)
        }
    }

    @Test
    fun `test that the dialog is displayed when the bluetooth permission has been granted for Android S`() {
        with(composeRule) {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 31)
            Mockito.mockStatic(ContextCompat::class.java).use {
                it.`when`<Int> {
                    ContextCompat.checkSelfPermission(
                        mock(),
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    )
                } doReturn PackageManager.PERMISSION_DENIED
                setScreen()

                onNodeWithTag(JOIN_A_MEETING_AS_GUEST_TAG).performClick()

                onNodeWithTag(PASTE_MEETING_LINK_DIALOG_TAG).assertIsDisplayed()
            }
        }
    }

    @Test
    fun `test that the dialog is not displayed when the bluetooth permission hasn't been granted for Android S`() {
        with(composeRule) {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 31)
            setScreen()

            onNodeWithTag(JOIN_A_MEETING_AS_GUEST_TAG).performClick()

            onNodeWithTag(PASTE_MEETING_LINK_DIALOG_TAG).assertIsNotDisplayed()
        }
    }

    @Test
    fun `test that the dialog is displayed when without asking the bluetooth permission for Android below S version`() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 30)
        with(composeRule) {
            setScreen()

            onNodeWithTag(JOIN_A_MEETING_AS_GUEST_TAG).performClick()

            onNodeWithTag(PASTE_MEETING_LINK_DIALOG_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that login button click action is triggered`() {
        val loginClick: () -> Unit = mock {}
        with(composeRule) {
            setScreen(onLoginClick = loginClick)
            onNodeWithTag(BUTTON_LOGIN_TAG).performClick()
        }
        verify(loginClick).invoke()
    }

    @Test
    fun `test that create account button click action is triggered`() {
        val createAccountClick: () -> Unit = mock {}
        with(composeRule) {
            setScreen(onCreateAccountClick = createAccountClick)
            onNodeWithTag(BUTTON_CREATE_ACCOUNT_TAG).performClick()
        }
        verify(createAccountClick).invoke()
    }

    private fun ComposeContentTestRule.setScreen(
        uiState: TourUiState = TourUiState(),
        onLoginClick: () -> Unit = {},
        onCreateAccountClick: () -> Unit = {},
        onMeetingLinkChange: (String) -> Unit = {},
        onConfirmMeetingLinkClick: () -> Unit = {},
        onOpenLink: (meetingLink: String) -> Unit = {},
        onClearLogoutProgressFlag: () -> Unit = {},
    ) {
        setContent {
            TourScreen(
                uiState = uiState,
                onLoginClick = onLoginClick,
                onCreateAccountClick = onCreateAccountClick,
                onMeetingLinkChange = onMeetingLinkChange,
                onConfirmMeetingLinkClick = onConfirmMeetingLinkClick,
                onOpenLink = onOpenLink,
                onClearLogoutProgressFlag = onClearLogoutProgressFlag
            )
        }
    }
}
