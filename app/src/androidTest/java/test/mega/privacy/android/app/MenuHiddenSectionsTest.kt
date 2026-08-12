package mega.privacy.android.app

import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.domain.entity.preference.NavigationItemsPreference
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.domain.usecase.preference.SetNavigationItemsPreferenceUseCase
import mega.privacy.android.navigation.destination.CustomiseNavigationNavKey
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented test for AND-24371 — sections hidden from the customised bottom
 * navigation bar surface as Menu rows.
 *
 * With [ApiFeatures.CustomisableBottomNavigation] enabled and a saved
 * [NavigationItemsPreference] of only Home and Drive, every other enabled main navigation
 * section is hidden from the bar. The Menu must then:
 *
 * 1. Show a dynamically-derived row for each hidden section that has no static Menu row
 *    (Media and Favourites here), anchored between the Chat and Device centre rows. Hidden
 *    sections that already have a static row (Chat, Shared items, Offline files) must not be
 *    duplicated.
 * 2. Open a hidden section pushed on top of the Menu's own back stack when its row is tapped,
 *    with a back affordance in the section's top bar (a bar section shows no navigation icon),
 *    returning to the Menu when tapped.
 * 3. React live to the preference: removing a section from the bar on the Customise navigation
 *    screen (toggle off → Save) moves it into the Menu the moment the preference is saved,
 *    with no relaunch.
 *
 * Each test exercises its whole journey against a single activity launch — instrumented
 * launches are expensive (the orchestrator restarts the process per test), and a continuous
 * flow also records cleanly as an MR demo clip.
 *
 * Built on the `:data-test` fake-SDK framework following [RenameNodeTest]: the whole app runs
 * as in production — real navigation, reconciler, ViewModels, preference persistence — with
 * only the SDK gateways faked and the flag forced through [FakeFeatureFlagValueProvider]. The
 * UI is driven with UiAutomator; Compose test tags are matched as resource ids because
 * MegaActivity sets `testTagsAsResourceId = true`.
 */
@HiltAndroidTest
class MenuHiddenSectionsTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    @Inject
    lateinit var setNavigationItemsPreferenceUseCase: SetNavigationItemsPreferenceUseCase

    @Inject
    lateinit var setCustomiseNavigationTooltipShownUseCase: SetCustomiseNavigationTooltipShownUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private val chatLabel get() = targetContext.getString(sharedR.string.general_chat)

    private val mediaLabel get() = targetContext.getString(sharedR.string.media_feature_title)

    private val favouritesLabel
        get() = targetContext.getString(sharedR.string.video_section_title_favourite_playlist)

    private val deviceCentreLabel
        get() = targetContext.getString(sharedR.string.general_section_device_centre)

    @Before
    fun setUp() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        hiltRule.inject()

        instrumentation.uiAutomation.grantRuntimePermission(
            targetContext.packageName,
            android.Manifest.permission.POST_NOTIFICATIONS,
        )

        // Boot the test process through the production initialiser units. See CloudDriveUploadTest.
        TestAppBoot.runCoreInitializers()

        fakeFlags.set(ApiFeatures.CustomisableBottomNavigation, true)

        // Persist a logged-in session through the app's real credentials path. The one-time
        // customise-navigation onboarding tooltip is marked as already shown so its popup can
        // never intercept the navigation bar taps these tests make.
        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            setCustomiseNavigationTooltipShownUseCase()
        }
    }

    @After
    fun tearDown() {
        fakeFlags.clear()
    }

    @Test
    fun hiddenSectionsAppearAsMenuRowsAndOpenPushedWithBackReturningToTheMenu() {
        // A customised bar of only Home and Drive, saved through the production preference
        // path — the same preference the Customise navigation settings screen writes. Every
        // other enabled section (Media, Chat, Favourites, Shared items, Offline files) is
        // thereby hidden.
        runBlocking {
            setNavigationItemsPreferenceUseCase(
                NavigationItemsPreference(orderedVisibleItemIds = listOf("home", "drive"))
            )
        }

        ActivityScenario.launch(MegaActivity::class.java)

        // The customised bar renders Home, Drive and the pinned Menu — Media is hidden.
        awaitObject(By.res(MENU_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "menu-nav-item")
        assertThat(device.hasObject(By.res(HOME_NAV_ITEM_TAG))).isTrue()
        assertThat(device.hasObject(By.res(DRIVE_NAV_ITEM_TAG))).isTrue()
        assertThat(device.hasObject(By.res(MEDIA_NAV_ITEM_TAG))).isFalse()
        Thread.sleep(1_000)

        device.findObject(By.res(MENU_NAV_ITEM_TAG)).click()
        awaitObject(By.res(MENU_TOOLBAR_TAG), LOAD_TIMEOUT, "menu-toolbar")

        // The hidden sections without a static Menu row appear as rows in the account section.
        scrollUntilObject(By.text(deviceCentreLabel), "device-centre-row")
        awaitObject(By.text(mediaLabel), LOAD_TIMEOUT, "media-row")
        awaitObject(By.text(favouritesLabel), LOAD_TIMEOUT, "favourites-row")

        // Anchored in place: Chat (static) → Media → Favourites → Device centre (static).
        val chatTop = device.findObject(By.text(chatLabel)).visibleBounds.top
        val mediaTop = device.findObject(By.text(mediaLabel)).visibleBounds.top
        val favouritesTop = device.findObject(By.text(favouritesLabel)).visibleBounds.top
        val deviceCentreTop = device.findObject(By.text(deviceCentreLabel)).visibleBounds.top
        assertThat(chatTop).isLessThan(mediaTop)
        assertThat(mediaTop).isLessThan(favouritesTop)
        assertThat(favouritesTop).isLessThan(deviceCentreTop)

        // Hidden sections with a static row are not duplicated: exactly one Chat row.
        assertThat(device.findObjects(By.text(chatLabel))).hasSize(1)

        // Hold so a human watching the run can see the derived rows before navigating on.
        Thread.sleep(2_000)

        // Open the hidden Media section from its dynamically-derived Menu row.
        device.findObject(By.text(mediaLabel)).click()

        // Media opens pushed on the Menu's back stack, so its top bar shows a back affordance
        // (rendered by a bar section's top bar as AppBarNavigationType.None before this change).
        awaitObject(BACK_ARROW, LOAD_TIMEOUT, "media-back-arrow")
        Thread.sleep(2_000)
        device.findObject(BACK_ARROW).click()

        // Back lands on the Menu again, not on another section's root.
        awaitObject(By.res(MENU_TOOLBAR_TAG), LOAD_TIMEOUT, "menu-toolbar-after-back")
        Thread.sleep(2_000)
    }

    @Test
    fun sectionRemovedFromTheNavigationBarViaCustomiseNavigationMovesToTheMenu() {
        // The saved arrangement includes Media, so it starts on the bar. Four items are
        // selected so that removing one keeps the selection at the allowed minimum of
        // MinSelectableNavigationItems.
        runBlocking {
            setNavigationItemsPreferenceUseCase(
                NavigationItemsPreference(
                    orderedVisibleItemIds = listOf("home", "drive", "media", "chat")
                )
            )
        }

        ActivityScenario.launch(MegaActivity::class.java)

        // Media starts in the navigation bar…
        awaitObject(By.res(MENU_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "menu-nav-item")
        awaitObject(By.res(MEDIA_NAV_ITEM_TAG), LOAD_TIMEOUT, "media-nav-item")
        Thread.sleep(1_000)

        // …so the Menu has no Media row (scrolled past its would-be anchor to prove absence).
        device.findObject(By.res(MENU_NAV_ITEM_TAG)).click()
        awaitObject(By.res(MENU_TOOLBAR_TAG), LOAD_TIMEOUT, "menu-toolbar")
        scrollUntilObject(By.text(deviceCentreLabel), "device-centre-row")
        // Scoped to account rows — the bar's own "Media" label is still on screen.
        assertThat(device.hasObject(mediaMenuRow())).isFalse()
        Thread.sleep(1_500)

        // Remove Media from the bar through Customise navigation. The screen is entered with
        // the same production intent its Settings entry fires — the legacy Settings screen
        // itself (a BaseActivity) cannot run under the Hilt test application.
        targetContext.startActivity(
            MegaActivity.getIntentWithExtraDestinations(
                targetContext,
                listOf(CustomiseNavigationNavKey),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )

        awaitObject(By.res(SAVE_BUTTON_TAG), LOAD_TIMEOUT, "customise-save-button")
        awaitObject(By.res(MEDIA_ITEM_ROW_TAG), LOAD_TIMEOUT, "customise-media-row")
        Thread.sleep(1_000)
        device.findObject(By.res(MEDIA_ITEM_ROW_TAG))
            .findObject(By.checkable(true))
            .click()
        Thread.sleep(1_500)
        device.findObject(By.res(SAVE_BUTTON_TAG)).click()

        // Save pops straight back to the Menu; the bar reacts to the preference immediately —
        // Media is gone from it…
        awaitObject(By.res(MENU_TOOLBAR_TAG), LOAD_TIMEOUT, "menu-toolbar-after-save")
        assertThat(device.hasObject(By.res(MEDIA_NAV_ITEM_TAG))).isFalse()
        Thread.sleep(1_000)

        // …and a Media row has appeared in the Menu, which opens the section pushed with back.
        scrollToTop()
        scrollUntilObject(mediaMenuRow(), "media-menu-row")
        Thread.sleep(2_000)
        device.findObject(mediaMenuRow()).click()
        awaitObject(BACK_ARROW, LOAD_TIMEOUT, "media-back-arrow")
        Thread.sleep(2_000)
    }

    /** A Menu account row titled "Media", as opposed to the bar's own "Media" label. */
    private fun mediaMenuRow(): BySelector =
        By.res(ACCOUNT_ITEM_TAG).hasDescendant(By.text(mediaLabel))

    /** Scrolls the visible scrollable list back to its top. */
    private fun scrollToTop() {
        repeat(MAX_SCROLL_ATTEMPTS) {
            val list = device.findObject(By.scrollable(true)) ?: return
            if (!list.scroll(Direction.UP, 1f)) return
        }
    }

    /**
     * Scrolls the Menu's account list until [selector] is present. The list is a LazyColumn, so
     * rows below the visible area are not composed and cannot be matched by UiAutomator until
     * they are scrolled into view. Small scroll steps keep the anchor rows (Chat … Device
     * centre) visible together for the ordering assertion.
     */
    private fun scrollUntilObject(selector: BySelector, name: String) {
        repeat(MAX_SCROLL_ATTEMPTS) {
            if (device.hasObject(selector)) return
            val list = device.findObject(By.scrollable(true)) ?: return@repeat
            list.setGestureMargin(list.visibleBounds.height() / 4)
            list.scroll(Direction.DOWN, SCROLL_PERCENT)
        }
        awaitObject(selector, LOAD_TIMEOUT, name)
    }

    /**
     * Waits for [selector]; on timeout, writes the window hierarchy to logcat (tag `UiDump`,
     * chunked — it survives the post-test uninstall) before failing.
     */
    private fun awaitObject(selector: BySelector, timeout: Long, name: String) {
        if (device.wait(Until.hasObject(selector), timeout)) return
        val stream = ByteArrayOutputStream()
        device.dumpWindowHierarchy(stream)
        stream.toString("UTF-8").chunked(3000).forEachIndexed { index, chunk ->
            Log.d("UiDump", "[$name#$index] $chunk")
        }
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump"
        )
    }

    private companion object {
        const val LAUNCH_TIMEOUT = 30_000L
        const val LOAD_TIMEOUT = 15_000L
        const val MAX_SCROLL_ATTEMPTS = 20
        const val SCROLL_PERCENT = 0.3f

        /** Main navigation item test tags, derived from each item's NavKey class name. */
        const val HOME_NAV_ITEM_TAG = "main_navigation:navigation_item_Home"
        const val DRIVE_NAV_ITEM_TAG = "main_navigation:navigation_item_DriveSyncNavKey"
        const val MEDIA_NAV_ITEM_TAG = "main_navigation:navigation_item_MediaMainNavKey"
        const val MENU_NAV_ITEM_TAG = "main_navigation:navigation_item_MenuHomeScreen"

        /** Mirror MenuHomeScreenTestTags. */
        const val MENU_TOOLBAR_TAG = "menu_home_screen:toolbar"
        const val ACCOUNT_ITEM_TAG = "menu_home_screen:account_item"

        /** Mirror CustomiseNavigationScreenTestTags. */
        const val SAVE_BUTTON_TAG = "customise_navigation_screen:button_save"
        const val MEDIA_ITEM_ROW_TAG = "customise_navigation_screen:item_media"

        /** The MegaTopAppBar back affordance rendered by AppBarNavigationType.Back. */
        val BACK_ARROW: BySelector = By.desc("Navigation Icon")
    }
}
