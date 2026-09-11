package mega.privacy.android.app

import androidx.test.uiautomator.By
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.entity.preference.NavigationItemsPreference
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Test

/**
 * Full-app instrumented test for AND-24955 — the static Chat Menu row tracks whether the Chat
 * section is resident in the customised bottom navigation bar:
 *
 * 1. With Chat hidden from the bar, the Menu keeps its static Chat row, which opens the chat
 *    list pushed on the Menu's back stack — with a back affordance that returns to the Menu.
 * 2. With Chat resident in the bar, the static Chat row is filtered from the Menu.
 *
 * This is the static-row complement of [MenuHiddenSectionsTest], which covers the
 * dynamically-derived rows of sections without a static Menu row. Built on the `:data-test`
 * fake-SDK framework via [BottomNavigationTestBase]; the navigation preference is seeded per
 * test through the production preference path before the single activity launch.
 */
@HiltAndroidTest
class MenuChatRowRoundTripTest : BottomNavigationTestBase() {

    private val chatLabel get() = targetContext.getString(sharedR.string.general_chat)

    private val deviceCentreLabel
        get() = targetContext.getString(sharedR.string.general_section_device_centre)

    override fun seedFeatureState() {
        flagSeeder.seed(ApiFeatures.CustomisableBottomNavigation, enabled = true)
        runBlocking { setCustomiseNavigationTooltipShownUseCase() }
    }

    private fun seedBarPreference(vararg itemIds: String) = runBlocking {
        setNavigationItemsPreferenceUseCase(
            NavigationItemsPreference(orderedVisibleItemIds = itemIds.toList())
        )
    }

    @Test
    fun chatMenuRowOpensTheChatListPushedWithBackReturningToTheMenu() {
        seedBarPreference(HOME_ID, DRIVE_ID, MEDIA_ID)

        launchApp()
        awaitObject(By.res(MENU_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "menu-nav-item")
        openMenu()

        // Chat is hidden from the bar, so its static Menu row is kept and opens the chat list
        // pushed on the Menu's own back stack — with a back affordance (a bar root shows none).
        scrollMenuUntil(menuAccountRow(chatLabel), "chat-menu-row")
        clickAndAwait(menuAccountRow(chatLabel), By.res(CHAT_LIST_SCREEN_TAG), "chat-menu-row")
        awaitObject(backArrow, LOAD_TIMEOUT, "chat-back-arrow")

        // Back lands on the Menu again, not on another section's root.
        clickAndAwait(backArrow, By.res(MENU_TOOLBAR_TAG), "chat-back-arrow")
    }

    @Test
    fun chatMenuRowIsAbsentWhenChatIsResidentInTheBar() {
        seedBarPreference(HOME_ID, DRIVE_ID, CHAT_ID)

        launchApp()
        awaitObject(By.res(CHAT_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "chat-nav-item")
        openMenu()

        // The anchor row below Chat's slot is scrolled into view first so the absence is
        // asserted against composed rows, not an area the LazyColumn has not composed yet.
        scrollMenuUntil(By.text(deviceCentreLabel), "device-centre-row")
        assertNotPresent(menuAccountRow(chatLabel), "chat-menu-row")
    }
}
