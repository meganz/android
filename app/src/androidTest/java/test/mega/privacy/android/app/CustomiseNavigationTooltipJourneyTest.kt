package mega.privacy.android.app

import androidx.test.uiautomator.By
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Test

/**
 * Journey 2 — with `CustomisableBottomNavigation` enabled and a fresh (never-shown) state, the
 * one-time onboarding tooltip appears on landing, its Explore action opens the Customise
 * navigation screen, and once shown it never reappears — neither after navigating back nor after
 * recreating the activity (the shown state is persisted the moment it is displayed).
 *
 * The tooltip renders in a Popup window, which does not expose Compose test tags as resource
 * ids, so it is asserted and driven by its texts.
 */
@HiltAndroidTest
class CustomiseNavigationTooltipJourneyTest : BottomNavigationTestBase() {

    override fun seedFeatureState() {
        flagSeeder.seed(ApiFeatures.CustomisableBottomNavigation, enabled = true)
    }

    @Test
    fun tooltipShowsOnceAndExploreOpensCustomiseScreen() {
        launchApp()

        awaitObject(By.text(tooltipBodyText()), LAUNCH_TIMEOUT, "customise-tooltip")
        awaitAnalyticsEvent("CustomiseNavigationTooltipDisplayed")

        clickAndAwait(
            clickSelector = By.text(exploreText()),
            expectedSelector = By.res(CUSTOMISE_SAVE_BUTTON_TAG),
            name = "tooltip-explore-button",
        )
        awaitAnalyticsEvent("CustomiseNavigationTooltipExploreButtonPressed")

        device.pressBack()
        awaitObject(By.res(HOME_NAV_ITEM_TAG), LOAD_TIMEOUT, "home-nav-item")
        assertNotPresent(By.text(tooltipBodyText()), "tooltip-after-back")

        scenario?.recreate()
        awaitObject(By.res(HOME_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "home-nav-item-after-recreate")
        assertNotPresent(By.text(tooltipBodyText()), "tooltip-after-recreate")
    }

    private fun exploreText() =
        targetContext.getString(sharedR.string.settings_customise_navigation_tooltip_action)
}
