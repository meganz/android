package mega.privacy.android.app

import androidx.test.uiautomator.By
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import org.junit.Test

/**
 * Journey 1 — with `CustomisableBottomNavigation` disabled the app keeps its default navigation:
 * the bottom bar shows Home / Drive / Media / Menu only and no onboarding tooltip appears on
 * landing.
 *
 * The Settings side of this journey (the "Start screen" row shown instead of "Customise
 * navigation") is not driven here: the legacy `SettingsActivity` reads `MegaApiAndroid.rootNode`
 * directly (bypassing `MegaApiGateway`) in `BaseActivity.shouldRefreshSessionDueToSDK` and
 * redirect-loops on the fake-SDK framework; that row logic is unit-tested in
 * `SettingsViewModelTest`.
 */
@HiltAndroidTest
class BottomNavDefaultFlagOffTest : BottomNavigationTestBase() {

    override fun seedFeatureState() {
        flagSeeder.seed(ApiFeatures.CustomisableBottomNavigation, enabled = false)
    }

    @Test
    fun defaultBarWithoutCustomisationEntryPoints() {
        launchApp()

        awaitObject(By.res(HOME_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "home-nav-item")
        assertBarOrder(
            HOME_NAV_ITEM_TAG,
            DRIVE_NAV_ITEM_TAG,
            MEDIA_NAV_ITEM_TAG,
            MENU_NAV_ITEM_TAG,
        )
        assertNotPresent(By.res(CHAT_NAV_ITEM_TAG), "chat-nav-item")
        assertNotPresent(By.res(OFFLINE_NAV_ITEM_TAG), "offline-nav-item")
        assertNotPresent(By.text(tooltipBodyText()), "customise-tooltip")
    }
}
