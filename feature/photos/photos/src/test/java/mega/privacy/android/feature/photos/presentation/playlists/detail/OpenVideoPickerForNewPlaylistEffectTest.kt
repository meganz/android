package mega.privacy.android.feature.photos.presentation.playlists.detail

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
class OpenVideoPickerForNewPlaylistEffectTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the video picker opens when the playlist was newly created`() {
        var openCount = 0

        composeTestRule.setContent {
            OpenVideoPickerForNewPlaylistEffect(
                isNewlyCreated = true,
                onOpenVideoPicker = { openCount++ },
            )
        }
        composeTestRule.waitForIdle()

        assertThat(openCount).isEqualTo(1)
    }

    @Test
    fun `test that the video picker does not open when the playlist was not newly created`() {
        var openCount = 0

        composeTestRule.setContent {
            OpenVideoPickerForNewPlaylistEffect(
                isNewlyCreated = false,
                onOpenVideoPicker = { openCount++ },
            )
        }
        composeTestRule.waitForIdle()

        assertThat(openCount).isEqualTo(0)
    }

    @Test
    fun `test that the video picker does not open again after the state is restored`() {
        var openCount = 0
        val restorationTester = StateRestorationTester(composeTestRule)

        restorationTester.setContent {
            OpenVideoPickerForNewPlaylistEffect(
                isNewlyCreated = true,
                onOpenVideoPicker = { openCount++ },
            )
        }
        composeTestRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        assertThat(openCount).isEqualTo(1)
    }
}
