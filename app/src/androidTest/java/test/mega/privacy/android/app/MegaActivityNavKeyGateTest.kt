package mega.privacy.android.app

import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.uiautomator.By
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.destination.CustomiseNavigationNavKey
import org.junit.Test

/**
 * Verifies that navigation destinations carried in the "navKeys" intent extra are honoured only
 * when the intent reaches the non-exported entry alias, and ignored when an external caller aims
 * them at the exported [MegaActivity] component directly.
 *
 * The Customise navigation screen is a convenient observable destination; the same gate protects
 * every destination, including the chat-join key described in the reporting ticket.
 */
@HiltAndroidTest
class MegaActivityNavKeyGateTest : BottomNavigationTestBase() {

    override fun seedFeatureState() {
        flagSeeder.seed(ApiFeatures.CustomisableBottomNavigation, enabled = true)
        runBlocking {
            setCustomiseNavigationTooltipShownUseCase()
        }
    }

    @Test
    fun navKeysAimedAtExportedComponentAreIgnored() {
        val intent = Intent().apply {
            component = ComponentName(targetContext, MegaActivity::class.java.name)
            putParcelableArrayListExtra(NAV_KEYS_EXTRA, arrayListOf(CustomiseNavigationNavKey()))
        }

        scenario = ActivityScenario.launch(intent)

        awaitObject(By.res(MENU_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "home-after-external-intent")
        assertNotPresent(
            By.res(CUSTOMISE_SAVE_BUTTON_TAG),
            "customise-screen-from-external-navkeys",
        )
    }

    @Test
    fun navKeysFromInternalEntryStillNavigate() {
        launchCustomiseNavigationScreen()
    }

    private companion object {
        const val NAV_KEYS_EXTRA = "navKeys"
    }
}
