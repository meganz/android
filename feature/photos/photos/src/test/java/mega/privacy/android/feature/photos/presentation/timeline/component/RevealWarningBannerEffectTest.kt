package mega.privacy.android.feature.photos.presentation.timeline.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class RevealWarningBannerEffectTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the banner is not revealed on the first composition`() {
        var revealCount = 0

        composeTestRule.setContent {
            RevealWarningBannerEffect(revealRequest = 3, onReveal = { revealCount++ })
        }
        composeTestRule.waitForIdle()

        assertThat(revealCount).isEqualTo(0)
    }

    @Test
    fun `test that the banner is revealed when the request goes up`() {
        var revealCount = 0
        var request by mutableIntStateOf(0)

        composeTestRule.setContent {
            RevealWarningBannerEffect(revealRequest = request, onReveal = { revealCount++ })
        }
        composeTestRule.waitForIdle()

        request = 1
        composeTestRule.waitForIdle()
        request = 2
        composeTestRule.waitForIdle()

        assertThat(revealCount).isEqualTo(2)
    }

    @Test
    fun `test that the banner is revealed when the request count restarts after process death`() {
        var revealCount = 0
        var request by mutableIntStateOf(0)
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            RevealWarningBannerEffect(revealRequest = request, onReveal = { revealCount++ })
        }
        composeTestRule.waitForIdle()
        request = 1
        composeTestRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        request = 0
        composeTestRule.waitForIdle()
        request = 1
        composeTestRule.waitForIdle()

        assertThat(revealCount).isEqualTo(2)
    }

    @Test
    fun `test that the banner is not revealed again after the state is restored`() {
        var revealCount = 0
        var request by mutableIntStateOf(0)
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            RevealWarningBannerEffect(revealRequest = request, onReveal = { revealCount++ })
        }
        composeTestRule.waitForIdle()
        request = 1
        composeTestRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        assertThat(revealCount).isEqualTo(1)
    }
}
