package mega.privacy.android.app

import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.flags.RemoteFeatureFlagSeeder
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.domain.usecase.preference.SetNavigationItemsPreferenceUseCase
import mega.privacy.android.navigation.destination.CustomiseNavigationNavKey
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.After
import org.junit.Before
import org.junit.Rule
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Shared plumbing for the bottom-navigation customisation journeys
 * (`ApiFeatures.CustomisableBottomNavigation`), following the established full-app instrumented
 * pattern of [CloudDriveUploadTest] / [TransferManagerTest]: the whole app runs as in production
 * with only the SDK gateways faked, [MegaActivity] is driven with UiAutomator, and Compose test
 * tags are matched as resource ids in the main window. Popup windows (the customise tooltip) do
 * not expose test tags as resource ids and are driven by text instead.
 *
 * Subclasses seed their flag / preference state in [seedFeatureState], which runs before boot and
 * before any activity launch so the first (process-cached) flag resolution already sees the
 * seeded value — see [RemoteFeatureFlagSeeder] for the resolution path.
 */
abstract class BottomNavigationTestBase {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var flagSeeder: RemoteFeatureFlagSeeder

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    @Inject
    lateinit var setNavigationItemsPreferenceUseCase: SetNavigationItemsPreferenceUseCase

    @Inject
    lateinit var setCustomiseNavigationTooltipShownUseCase: SetCustomiseNavigationTooltipShownUseCase

    protected val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    protected val targetContext get() = instrumentation.targetContext

    protected val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    protected var scenario: ActivityScenario<MegaActivity>? = null

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

        // Seed flag / preference state before the boot initialisers and before any activity so
        // nothing resolves (and process-caches) the flag ahead of the seeded value.
        seedFeatureState()

        // Boot the test process through the production initialiser units. See CloudDriveUploadTest.
        TestAppBoot.runCoreInitializers()
        fakeMegaApi.clearInvocations()

        // Persist a logged-in session through the app's real credentials path; account details are
        // fetched eagerly because account-detail dependent features wait for a non-empty value.
        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
    }

    /** Per-journey flag / preference seeding, executed before boot and any activity launch. */
    protected abstract fun seedFeatureState()

    @After
    fun tearDown() {
        scenario?.close()
    }

    // region navigation helpers

    protected fun launchApp() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)
    }

    /**
     * Launches [MegaActivity] with [CustomiseNavigationNavKey] as an extra destination — the
     * same intent the Settings screen's "Customise navigation" row fires in production.
     *
     * The row itself cannot be driven in-process: the legacy `SettingsActivity` reads
     * `MegaApiAndroid.rootNode` directly in `BaseActivity.shouldRefreshSessionDueToSDK`
     * (bypassing `MegaApiGateway`, so the fake cannot intercept it) and redirect-loops on the
     * fake-SDK framework. Its row visibility / summary logic is unit-tested in
     * `SettingsViewModelTest`.
     */
    protected fun launchCustomiseNavigationScreen() {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(CustomiseNavigationNavKey),
        )
        scenario = ActivityScenario.launch(intent)
        awaitObject(By.res(CUSTOMISE_SAVE_BUTTON_TAG), LAUNCH_TIMEOUT, "customise-screen")
    }

    /**
     * Toggles a customise-screen navigation item row by clicking the row's Toggle (the only
     * checkable node inside the row); rows themselves are not clickable on that screen.
     */
    protected fun toggleNavigationItem(id: String) {
        val rowTag = navigationItemRowTag(id)
        scrollToRes(rowTag, "navigation-item-row-$id")
        val toggle = device.findObject(By.res(rowTag)).findObject(By.checkable(true))
        if (toggle == null) {
            dumpHierarchy("navigation-item-toggle-$id")
            throw AssertionError("No checkable toggle inside row $rowTag; hierarchy in logcat tag UiDump")
        }
        toggle.click()
    }

    /**
     * Asserts the navigation bar renders exactly [expectedTags] left to right (the pinned Menu
     * item is part of [expectedTags] when asserted).
     */
    protected fun assertBarOrder(vararg expectedTags: String) {
        val lefts = expectedTags.map { tag ->
            awaitObject(By.res(tag), LOAD_TIMEOUT, "bar-item-$tag")
            device.findObject(By.res(tag)).visibleBounds.left
        }
        assertThat(lefts).isInOrder()
    }

    protected fun navigationBarBounds(): Rect {
        awaitObject(By.res(NAVIGATION_BAR_TAG), LOAD_TIMEOUT, "navigation-bar")
        return device.findObject(By.res(NAVIGATION_BAR_TAG)).visibleBounds
    }

    // endregion

    // region UiAutomator helpers

    protected fun click(tag: String, name: String) {
        awaitObject(By.res(tag), LOAD_TIMEOUT, name)
        device.findObject(By.res(tag)).click()
    }

    protected fun clickText(text: String, name: String) {
        awaitObject(By.text(text), LOAD_TIMEOUT, name)
        device.findObject(By.text(text)).click()
    }

    /**
     * Clicks [clickSelector] and waits for [expectedSelector] to confirm the resulting screen
     * transition, retrying the click when it does not: a tap injected while the target surface
     * is still animating in can land on stale coordinates and be dropped.
     */
    protected fun clickAndAwait(
        clickSelector: BySelector,
        expectedSelector: BySelector,
        name: String,
        attempts: Int = NAVIGATION_CLICK_ATTEMPTS,
    ) {
        repeat(attempts) {
            awaitObject(clickSelector, LOAD_TIMEOUT, name)
            device.waitForIdle()
            device.findObject(clickSelector)?.click()
            if (device.wait(Until.hasObject(expectedSelector), NAVIGATION_SETTLE_MS)) return
        }
        dumpHierarchy(name)
        throw AssertionError(
            "Clicking $name ($clickSelector) $attempts times never showed $expectedSelector; " +
                    "hierarchy in logcat tag UiDump",
        )
    }

    protected fun awaitObject(selector: BySelector, timeout: Long, name: String): UiObject2 {
        if (!device.wait(Until.hasObject(selector), timeout)) {
            dumpHierarchy(name)
            throw AssertionError(
                "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump",
            )
        }
        return device.findObject(selector)
    }

    protected fun awaitGone(selector: BySelector, name: String) {
        if (device.wait(Until.gone(selector), LOAD_TIMEOUT)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name ($selector) to disappear; hierarchy in logcat tag UiDump",
        )
    }

    /**
     * Asserts [selector] does not appear within [settleMs]. Used for negative assertions (no
     * tooltip, no settings row) once the surrounding screen is confirmed visible.
     */
    protected fun assertNotPresent(selector: BySelector, name: String, settleMs: Long = ABSENCE_SETTLE_MS) {
        if (device.wait(Until.hasObject(selector), settleMs)) {
            dumpHierarchy(name)
            throw AssertionError("Expected $name ($selector) to be absent but it appeared; hierarchy in logcat tag UiDump")
        }
    }

    /** Scrolls the current screen up (content down) until [text] is visible. */
    protected fun scrollToText(text: String, name: String) =
        scrollTo(By.text(text), name)

    /** Scrolls the current screen up (content down) until the [tag] resource id is visible. */
    protected fun scrollToRes(tag: String, name: String) =
        scrollTo(By.res(tag), name)

    private fun scrollTo(selector: BySelector, name: String) {
        repeat(MAX_SCROLL_SWIPES) {
            if (device.hasObject(selector)) return
            val width = device.displayWidth
            val height = device.displayHeight
            device.swipe(width / 2, (height * 0.7).toInt(), width / 2, (height * 0.3).toInt(), 20)
            device.waitForIdle()
        }
        awaitObject(selector, LOAD_TIMEOUT, name)
    }

    private fun dumpHierarchy(name: String) {
        val stream = ByteArrayOutputStream()
        device.dumpWindowHierarchy(stream)
        stream.toString("UTF-8").chunked(3000).forEachIndexed { index, chunk ->
            Log.d("UiDump", "[$name#$index] $chunk")
        }
    }

    // endregion

    // region fake-gateway verification helpers

    /**
     * Gateway commands (including analytics `sendEvent`) are dispatched from coroutine scopes, so
     * their invocations are not recorded synchronously after the UI action that triggers them.
     * Poll [supplier] until at least one invocation matches, or fail with what was recorded.
     */
    protected fun <T> awaitInvocations(name: String, supplier: () -> List<T>): List<T> {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            val matches = supplier()
            if (matches.isNotEmpty()) return matches
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for an invocation of $name; " +
                    "recorded methods=${fakeMegaApi.invocations.map { it.methodName }}",
        )
    }

    /**
     * Awaits the analytics event [eventName] reaching the (fake) SDK. Analytics flows through a
     * fake-visible seam: `Analytics.tracker` → analytics SDK → `SendUserJourneyEventUseCase` →
     * `StatisticsRepository` → `MegaApiGateway.sendEvent(eventId, message, …)`, where `message`
     * is `"<event name> <event data json>"`.
     */
    protected fun awaitAnalyticsEvent(eventName: String) {
        awaitInvocations("sendEvent($eventName)") {
            fakeMegaApi.invocations.filter {
                it.methodName == "sendEvent" &&
                        (it.arguments.getOrNull(1) as? String)?.startsWith("$eventName ") == true
            }
        }
    }

    // endregion

    // region shared strings

    protected fun tooltipBodyText(): String =
        targetContext.getString(sharedR.string.settings_customise_navigation_tooltip_body)

    // endregion

    protected companion object {
        const val LAUNCH_TIMEOUT = 30_000L
        const val LOAD_TIMEOUT = 15_000L
        const val POLL_INTERVAL_MS = 100L
        const val ABSENCE_SETTLE_MS = 3_000L
        const val NAVIGATION_SETTLE_MS = 5_000L
        const val NAVIGATION_CLICK_ATTEMPTS = 3
        const val MAX_SCROLL_SWIPES = 5

        /** Main navigation bar and per-item tags ("main_navigation:navigation_item_<NavKey>"). */
        const val NAVIGATION_BAR_TAG = "main_navigation:navigation_bar"
        const val HOME_NAV_ITEM_TAG = "main_navigation:navigation_item_Home"
        const val DRIVE_NAV_ITEM_TAG = "main_navigation:navigation_item_DriveSyncNavKey"
        const val MEDIA_NAV_ITEM_TAG = "main_navigation:navigation_item_MediaMainNavKey"
        const val MENU_NAV_ITEM_TAG = "main_navigation:navigation_item_MenuHomeScreen"
        const val CHAT_NAV_ITEM_TAG = "main_navigation:navigation_item_ChatListNavKey"
        const val OFFLINE_NAV_ITEM_TAG = "main_navigation:navigation_item_OfflineNavKey"
        const val FAVOURITES_NAV_ITEM_TAG = "main_navigation:navigation_item_FavouritesNavKey"

        /** Mirrors CustomiseNavigationScreenTestTags. */
        const val CUSTOMISE_SAVE_BUTTON_TAG = "customise_navigation_screen:button_save"
        const val CUSTOMISE_RESET_BUTTON_TAG = "customise_navigation_screen:button_reset"

        /** Mirrors CHAT_LIST_SCREEN_TAG of the Compose chat list. */
        const val CHAT_LIST_SCREEN_TAG = "chat_list_screen:screen"

        /** Navigation item ids as persisted in NavigationItemsPreference. */
        const val HOME_ID = "home"
        const val DRIVE_ID = "drive"
        const val MEDIA_ID = "media"
        const val CHAT_ID = "chat"
        const val OFFLINE_ID = "offline"
        const val FAVOURITES_ID = "favourites"

        fun navigationItemRowTag(id: String) = "customise_navigation_screen:item_$id"
    }
}
