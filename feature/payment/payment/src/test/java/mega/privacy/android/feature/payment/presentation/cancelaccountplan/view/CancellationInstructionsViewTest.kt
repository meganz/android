package mega.privacy.android.feature.payment.presentation.cancelaccountplan.view

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.payment.presentation.cancelaccountplan.model.CancellationInstructionsType
import mega.privacy.android.feature.payment.presentation.cancelaccountplan.view.instructionscreens.APPLE_INSTRUCTIONS_VIEW_TEST_TAG
import mega.privacy.android.feature.payment.presentation.cancelaccountplan.view.instructionscreens.CancellationInstructionsView
import mega.privacy.android.feature.payment.presentation.cancelaccountplan.view.instructionscreens.WEB_CANCELLATION_INSTRUCTIONS_VIEW_TEST_TAG
import mega.privacy.android.feature.payment.presentation.cancelaccountplan.view.instructionscreens.WEB_REACTIVATION_INSTRUCTIONS_VIEW_TEST_TAG
import mega.privacy.android.feature.payment.presentation.cancelaccountplan.view.instructionscreens.WebInstructionsViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class CancellationInstructionsViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val webInstructionsViewModel = mock<WebInstructionsViewModel> {
        on { domainName } doReturn "mega.nz"
        on { megaUrl } doReturn "https://mega.nz/"
    }

    private val viewModelStore = mock<ViewModelStore> {
        on { get(argThat<String> { contains(WebInstructionsViewModel::class.java.canonicalName.orEmpty()) }) } doReturn webInstructionsViewModel
    }
    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { this.viewModelStore } doReturn viewModelStore
    }

    @Test
    fun `test that instructions details view is showing the Apple instructions view`() {
        composeTestRule.setContent {
            CancellationInstructionsView(
                instructionsType = CancellationInstructionsType.AppStore,
                onMegaUrlClicked = { },
                onCancelSubsFromOtherDeviceClicked = { },
                onBackPressed = { },
                isAccountReactivationNeeded = false
            )
        }
        composeTestRule.onNodeWithTag(WEB_CANCELLATION_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(WEB_REACTIVATION_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(APPLE_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that instructions details view is showing the webclient cancellation instructions view`() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                CancellationInstructionsView(
                    instructionsType = CancellationInstructionsType.WebClient,
                    onMegaUrlClicked = { },
                    onCancelSubsFromOtherDeviceClicked = { },
                    onBackPressed = { },
                    isAccountReactivationNeeded = false
                )
            }
        }
        composeTestRule.onNodeWithTag(APPLE_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(WEB_REACTIVATION_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(WEB_CANCELLATION_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that instructions details view is showing the webclient reactivation instructions view`() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                CancellationInstructionsView(
                    instructionsType = CancellationInstructionsType.WebClient,
                    onMegaUrlClicked = { },
                    onCancelSubsFromOtherDeviceClicked = { },
                    onBackPressed = { },
                    isAccountReactivationNeeded = true
                )
            }
        }
        composeTestRule.onNodeWithTag(APPLE_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(WEB_CANCELLATION_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(WEB_REACTIVATION_INSTRUCTIONS_VIEW_TEST_TAG)
            .assertIsDisplayed()
    }
}
