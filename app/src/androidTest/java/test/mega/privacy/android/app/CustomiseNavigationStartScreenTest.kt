package mega.privacy.android.app

import androidx.test.uiautomator.By
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.preference.NavigationItemsPreference
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import org.junit.Test

/**
 * Journey 4 — the first item of the saved navigation preference is the start screen: with a
 * preference that puts Chat first, a fresh launch lands directly on the (Compose) chat list.
 *
 * The preference is seeded through [setNavigationItemsPreferenceUseCase] rather than the UI so
 * this journey only measures the launch behaviour; the UI path that produces the same
 * preference is covered end to end by [CustomiseNavigationJourneyTest].
 */
@HiltAndroidTest
class CustomiseNavigationStartScreenTest : BottomNavigationTestBase() {

    override fun seedFeatureState() {
        flagSeeder.seed(ApiFeatures.CustomisableBottomNavigation, enabled = true)
        runBlocking {
            setCustomiseNavigationTooltipShownUseCase()
            setNavigationItemsPreferenceUseCase(
                NavigationItemsPreference(listOf(CHAT_ID, HOME_ID, DRIVE_ID)),
            )
        }
    }

    @Test
    fun landingScreenIsChatListWhenChatIsFirst() {
        launchApp()

        awaitObject(By.res(CHAT_LIST_SCREEN_TAG), LAUNCH_TIMEOUT, "chat-list-landing")
        assertBarOrder(
            CHAT_NAV_ITEM_TAG,
            HOME_NAV_ITEM_TAG,
            DRIVE_NAV_ITEM_TAG,
            MENU_NAV_ITEM_TAG,
        )
    }
}
