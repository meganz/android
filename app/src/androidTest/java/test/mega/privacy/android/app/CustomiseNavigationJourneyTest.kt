package mega.privacy.android.app

import androidx.test.uiautomator.By
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.destination.CustomiseNavigationNavKey
import org.junit.Test

/**
 * Journey 3 — customisation round trip on the Customise navigation screen: toggle Offline on,
 * Media off and make Chat the first item → Save → the bottom bar renders the saved arrangement.
 *
 * The screen is launched with the same [CustomiseNavigationNavKey] extra-destination intent the
 * Settings row fires in production (see [launchCustomiseNavigationScreen] for why the legacy
 * Settings screen itself cannot be driven on the fake-SDK framework).
 *
 * Reordering is achieved through the toggles' deterministic append order (disable the default
 * items, then enable Chat / Home / Offline in the target order) rather than a UiAutomator drag on
 * the reorder handle: the reorderable list requires a long-press-then-move gesture whose timing
 * is not reliably reproduced by injected drags, while toggling appends items deterministically
 * and exercises the same pending-selection state that dragging mutates.
 */
@HiltAndroidTest
class CustomiseNavigationJourneyTest : BottomNavigationTestBase() {

    override fun seedFeatureState() {
        flagSeeder.seed(ApiFeatures.CustomisableBottomNavigation, enabled = true)
        runBlocking { setCustomiseNavigationTooltipShownUseCase() }
    }

    @Test
    fun customiseArrangementSavesToBar() {
        launchCustomiseNavigationScreen()

        listOf(HOME_ID, DRIVE_ID, MEDIA_ID).forEach(::toggleNavigationItem)
        listOf(CHAT_ID, HOME_ID, OFFLINE_ID).forEach(::toggleNavigationItem)

        click(CUSTOMISE_SAVE_BUTTON_TAG, "save-button")
        awaitAnalyticsEvent("CustomiseNavigationSaveButtonPressed")

        awaitObject(By.res(CHAT_NAV_ITEM_TAG), LOAD_TIMEOUT, "chat-nav-item")
        assertBarOrder(
            CHAT_NAV_ITEM_TAG,
            HOME_NAV_ITEM_TAG,
            OFFLINE_NAV_ITEM_TAG,
            MENU_NAV_ITEM_TAG,
        )
        assertNotPresent(By.res(DRIVE_NAV_ITEM_TAG), "drive-nav-item")
        assertNotPresent(By.res(MEDIA_NAV_ITEM_TAG), "media-nav-item")
    }
}
