package mega.privacy.android.app

import androidx.test.uiautomator.By
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.preference.NavigationItemsPreference
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import org.junit.Test

/**
 * Journey 7 — Reset to default: starting from a customised preference (seeded through
 * [setNavigationItemsPreferenceUseCase]; the UI path that produces it is covered by
 * [CustomiseNavigationJourneyTest]), the customise screen's "Reset to default" restores the
 * default pending arrangement and Save returns the bottom bar to Home / Drive / Media / Menu.
 *
 * The screen is launched with the production extra-destination intent — see
 * [launchCustomiseNavigationScreen] for why the legacy Settings screen itself cannot be driven
 * on the fake-SDK framework.
 */
@HiltAndroidTest
class CustomiseNavigationResetTest : BottomNavigationTestBase() {

    override fun seedFeatureState() {
        flagSeeder.seed(ApiFeatures.CustomisableBottomNavigation, enabled = true)
        runBlocking {
            setCustomiseNavigationTooltipShownUseCase()
            setNavigationItemsPreferenceUseCase(
                NavigationItemsPreference(listOf(CHAT_ID, HOME_ID, OFFLINE_ID)),
            )
        }
    }

    @Test
    fun resetToDefaultRestoresDefaultBar() {
        launchCustomiseNavigationScreen()

        click(CUSTOMISE_RESET_BUTTON_TAG, "reset-button")
        awaitAnalyticsEvent("CustomiseNavigationResetButtonPressed")

        click(CUSTOMISE_SAVE_BUTTON_TAG, "save-button")
        awaitAnalyticsEvent("CustomiseNavigationSaveButtonPressed")

        awaitObject(By.res(DRIVE_NAV_ITEM_TAG), LOAD_TIMEOUT, "drive-nav-item")
        assertBarOrder(
            HOME_NAV_ITEM_TAG,
            DRIVE_NAV_ITEM_TAG,
            MEDIA_NAV_ITEM_TAG,
            MENU_NAV_ITEM_TAG,
        )
        assertNotPresent(By.res(CHAT_NAV_ITEM_TAG), "chat-nav-item-after-reset")
        assertNotPresent(By.res(OFFLINE_NAV_ITEM_TAG), "offline-nav-item-after-reset")
    }
}
