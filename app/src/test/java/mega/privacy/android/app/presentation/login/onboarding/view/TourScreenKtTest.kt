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
import org.robolectric.util.ReflectionHelpers


@RunWith(AndroidJUnit4::class)
class TourScreenKtTest {

    @get:Rule
    val composeRule = createComposeRule()

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
