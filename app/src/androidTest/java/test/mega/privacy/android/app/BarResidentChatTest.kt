package mega.privacy.android.app

import androidx.test.uiautomator.By
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.preference.NavigationItemsPreference
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Test

/**
 * Full-app instrumented test for AND-24955 — the Chat section resident in the customised bottom
 * navigation bar is a bar root: its top bar shows no back affordance and the Menu shows no Chat
 * row (the static row is filtered while its section sits in the bar).
 *
 * Built on the `:data-test` fake-SDK framework via [BottomNavigationTestBase]: the whole app runs
 * as in production with only the SDK gateways faked, [ApiFeatures.CustomisableBottomNavigation]
 * seeded through the remote-flag path, and the UI driven with UiAutomator. The back affordance is
 * the top bar's "Navigation Icon" content description; its absence is only asserted once the top
 * bar itself is confirmed on screen, so the check cannot pass against an unsettled screen.
 */
@HiltAndroidTest
class BarResidentChatTest : BottomNavigationTestBase() {

    private val chatLabel get() = targetContext.getString(sharedR.string.general_chat)

    private val deviceCentreLabel
        get() = targetContext.getString(sharedR.string.general_section_device_centre)

    override fun seedFeatureState() {
        flagSeeder.seed(ApiFeatures.CustomisableBottomNavigation, enabled = true)
        runBlocking {
            setCustomiseNavigationTooltipShownUseCase()
            setNavigationItemsPreferenceUseCase(
                NavigationItemsPreference(
                    orderedVisibleItemIds = listOf(HOME_ID, DRIVE_ID, CHAT_ID),
                )
            )
        }
    }

    @Test
    fun chatSectionInTheBarShowsNoBackAffordanceAndHasNoMenuRow() {
        launchApp()

        // Chat sits in the customised bar; entering it lands on the chat list as a bar root.
        awaitObject(By.res(CHAT_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "chat-nav-item")
        clickAndAwait(By.res(CHAT_NAV_ITEM_TAG), By.res(CHAT_LIST_SCREEN_TAG), "chat-nav-item")

        // The bar root's top bar renders with no back affordance.
        awaitObject(By.res(CHAT_LIST_SEARCH_BAR_TAG), LOAD_TIMEOUT, "chat-top-bar")
        assertNotPresent(backArrow, "chat-back-arrow")

        // The Menu shows no Chat row while Chat is in the bar; the anchor row below Chat's slot
        // is scrolled into view first so the absence is asserted against composed rows.
        openMenu()
        scrollMenuUntil(By.text(deviceCentreLabel), "device-centre-row")
        assertNotPresent(menuAccountRow(chatLabel), "chat-menu-row")
    }
}
