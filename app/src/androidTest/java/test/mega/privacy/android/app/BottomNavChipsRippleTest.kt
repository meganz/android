package mega.privacy.android.app

import android.os.SystemClock
import dagger.hilt.android.testing.HiltAndroidTest
import androidx.test.uiautomator.By
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.preference.NavigationItemsPreference
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Test

/**
 * Journey 6 — the Home shortcuts chips mirror the navigation preference: a chip is hidden while
 * its section is in the bottom bar (a redundant shortcut) and returns once it is not. The Audio
 * chip has no bar equivalent and is always shown.
 *
 * Chips expose their label as content description (and some labels equal bar item labels), so
 * chip presence is asserted by whether the description exists outside the navigation bar bounds. The second phase sets the preference back to the default
 * arrangement (there is no API to clear the preference; the default ids hide no chips, which is
 * the same reconciliation path) and recreates the activity to re-read it.
 */
@HiltAndroidTest
class BottomNavChipsRippleTest : BottomNavigationTestBase() {

    override fun seedFeatureState() {
        flagSeeder.seed(ApiFeatures.CustomisableBottomNavigation, enabled = true)
        runBlocking {
            setCustomiseNavigationTooltipShownUseCase()
            setNavigationItemsPreferenceUseCase(
                NavigationItemsPreference(listOf(HOME_ID, FAVOURITES_ID, OFFLINE_ID)),
            )
        }
    }

    @Test
    fun chipsForSectionsInBarAreHiddenAndReturnWhenRemoved() {
        launchApp()

        awaitObject(By.desc(audioChipText()), LAUNCH_TIMEOUT, "audio-chip")
        assertBarOrder(
            HOME_NAV_ITEM_TAG,
            FAVOURITES_NAV_ITEM_TAG,
            OFFLINE_NAV_ITEM_TAG,
            MENU_NAV_ITEM_TAG,
        )
        assertTextOnlyInsideNavigationBar(favouritesChipText(), "favourites-chip-hidden")
        assertTextOnlyInsideNavigationBar(offlineChipText(), "offline-chip-hidden")

        runBlocking {
            setNavigationItemsPreferenceUseCase(
                NavigationItemsPreference(listOf(HOME_ID, DRIVE_ID, MEDIA_ID)),
            )
        }
        scenario?.recreate()

        awaitObject(By.desc(audioChipText()), LAUNCH_TIMEOUT, "audio-chip-after-reset")
        awaitTextOutsideNavigationBar(favouritesChipText(), "favourites-chip-returned")
        awaitTextOutsideNavigationBar(offlineChipText(), "offline-chip-returned")
    }

    /** Asserts every occurrence of [text] lies within the navigation bar (i.e. no chip). */
    private fun assertTextOnlyInsideNavigationBar(text: String, name: String) {
        val barBounds = navigationBarBounds()
        val outside = device.findObjects(By.desc(text))
            .filterNot { barBounds.contains(it.visibleBounds) }
        if (outside.isNotEmpty()) {
            throw AssertionError(
                "Expected no '$text' chip outside the navigation bar for $name but found ${outside.size} at " +
                        outside.map { it.visibleBounds },
            )
        }
    }

    /**
     * Awaits an occurrence of [text] outside the navigation bar, swiping the chips row towards
     * its end in case the chip is beyond the horizontally scrollable viewport.
     */
    private fun awaitTextOutsideNavigationBar(text: String, name: String) {
        val barBounds = navigationBarBounds()
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        var swipes = 0
        while (SystemClock.uptimeMillis() < deadline) {
            val found = device.findObjects(By.desc(text))
                .any { !barBounds.contains(it.visibleBounds) }
            if (found) return
            if (swipes < MAX_SCROLL_SWIPES) {
                device.findObject(By.desc(audioChipText()))?.visibleBounds?.let { chipBounds ->
                    device.swipe(
                        (device.displayWidth * 0.8).toInt(),
                        chipBounds.centerY(),
                        (device.displayWidth * 0.2).toInt(),
                        chipBounds.centerY(),
                        20,
                    )
                    swipes++
                }
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError("Timed out waiting for '$text' outside the navigation bar ($name)")
    }

    private fun audioChipText() =
        targetContext.getString(sharedR.string.home_screen_audios_chip_title)

    private fun favouritesChipText() =
        targetContext.getString(sharedR.string.video_section_title_favourite_playlist)

    private fun offlineChipText() =
        targetContext.getString(sharedR.string.section_saved_for_offline_new)
}
