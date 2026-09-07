package mega.privacy.android.app

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaError
import mega.privacy.android.data.test.stub.StubMegaUser
import mega.privacy.android.data.test.stub.StubMegaUserAlert
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.SetHomeConfigurationTooltipShownUseCase
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaUserAlert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented port of the AAT feature `sm02 Check the menu options` — every scenario
 * opens one destination from the app menu and asserts that the destination renders. Built on the
 * `:data-test` fake-SDK framework and following [HomeScreenTest] / [MenuHiddenSectionsTest]: the
 * whole app runs as in production with only the SDK gateways faked, the UI is driven with
 * UiAutomator (not a Compose rule), and Compose test tags are matched as resource ids because
 * [MegaActivity] sets `testTagsAsResourceId = true`.
 *
 * The AAT scenarios navigate through the legacy navigation drawer, which no longer exists — there
 * is no `DrawerLayout` and no `ManagerActivity` on develop. The destinations survive as rows on the
 * Menu bottom-navigation tab (`MenuAccountItems`), so each scenario keeps its intent and re-points
 * its locators at that tab. Notifications has no Menu row at all: it is reached through the bell
 * action in the Menu's own top bar, which is where this suite opens it.
 *
 * The feature's Background — the account entry showing the avatar and email, plus a Storage
 * entry — is asserted on the Menu tab itself before every scenario navigates on.
 *
 * Ported: tc02 (Contacts), tc03 (Notifications), tc04 (Transfers), tc05 (Offline),
 * tc06 (Rubbish Bin), tc07 (Settings).
 *
 * Adaptations and skips:
 * - tc01 (My Account) is not ported: [HomeScreenTest.myAccountOpensFromAccountAvatarRow] already
 *   opens My Account from the same Menu account row and asserts the account details it checks.
 * - tc04 is kept even though [TransferManagerTest] covers the Transfer Manager, because that suite
 *   only ever reaches it from the toolbar transfers widget with transfers in flight — the Menu
 *   route and the "No transfers" empty state are asserted only here.
 * - tc07's AAT checklist is stale — it asserts an "Advanced" row and a "Send feedback" row that no
 *   longer exist, and misses rows added since. What is asserted here is what `preferences.xml`
 *   renders for a free account on default feature flags, top to bottom. "Customise navigation" and
 *   "Sorting and view mode" stay hidden behind [ApiFeatures.CustomisableBottomNavigation] and
 *   [ApiFeatures.SortingAndViewMode], and "Show hidden items" renders for paid plans only, so none
 *   of the three is part of this baseline.
 * - tc08 (Upgrade account) is not ported: the plan list is priced through Google Play, not the SDK,
 *   so the screen renders its error state under the fakes.
 */
@HiltAndroidTest
class MenuOptionsTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    @Inject
    lateinit var setHomeConfigurationTooltipShownUseCase: SetHomeConfigurationTooltipShownUseCase

    @Inject
    lateinit var setCustomiseNavigationTooltipShownUseCase: SetCustomiseNavigationTooltipShownUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private val contact = StubMegaUser(email = CONTACT_EMAIL, handle = CONTACT_HANDLE)

    private var scenario: ActivityScenario<MegaActivity>? = null

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

        // The Contacts destination is gated: with the flag off it launches the legacy
        // ContactsActivity, which cannot run under the Hilt test application.
        fakeFlags.set(ApiFeatures.ContactComposeFeature, true)

        // Home resolves Cloud Drive children on start; answer through the fake node tree.
        fakeMegaApi.stub(MegaApiGateway::getChildren) {
            fakeMegaApi.nodeTree.childrenOf(fakeMegaApi.nodeTree.rootNode.handle)
        }

        // The contact list renders its empty state — and with it neither the Requests nor the
        // Groups entry — until there is at least one visible contact.
        fakeMegaApi.stub(MegaApiGateway::getContacts) { listOf(contact) }
        fakeMegaApi.stub(MegaApiGateway::getContact) { contact }
        // No alias is set, so the alias read must fail cleanly for the contact to resolve.
        fakeMegaApi.stubRequest(
            MegaApiGateway::getUserAlias,
            error = StubMegaError(MegaError.API_ENOENT),
        )

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)

            // The one-time onboarding tooltips are popups that cover the screens this suite taps
            // through, so on a fresh install they swallow the taps meant for them.
            setHomeConfigurationTooltipShownUseCase()
            setCustomiseNavigationTooltipShownUseCase()
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        fakeFlags.clear()
    }

    // region tests

    /** tc02 — the Contacts row opens the contact list with its Requests and Groups entries. */
    @Test
    fun contactsOpensFromTheMenu() {
        openFromMenu(str("general_section_contacts"), "contacts-menu-row")

        awaitObject(
            By.res(CONTACT_LIST_REQUESTS_ACTION_TAG),
            LOAD_TIMEOUT,
            "contacts-requests-entry",
        )
        awaitObject(By.res(CONTACT_LIST_GROUPS_ACTION_TAG), LOAD_TIMEOUT, "contacts-groups-entry")
        awaitObject(By.text(CONTACT_EMAIL), LOAD_TIMEOUT, "contacts-seeded-contact")
    }

    /**
     * tc03 — the bell action opens Notifications, listing the pending contact request seeded as a
     * user alert. AAT reaches the same list by having a contact request waiting on the account.
     */
    @Test
    fun notificationsOpensFromTheMenu() {
        fakeMegaApi.stub(MegaApiGateway::getUserAlerts) {
            listOf(
                StubMegaUserAlert(
                    id = ALERT_ID,
                    type = MegaUserAlert.TYPE_INCOMINGPENDINGCONTACT_REQUEST,
                    userHandle = CONTACT_HANDLE,
                    email = CONTACT_EMAIL,
                    timestamp = System.currentTimeMillis() / 1000,
                ),
            )
        }

        openMenu()
        click(By.res(MENU_NOTIFICATION_ICON_TAG), "notifications-bell")

        awaitObject(By.res(NOTIFICATION_TOP_BAR_TAG), LOAD_TIMEOUT, "notifications-top-bar")
        awaitObject(By.res(NOTIFICATION_LIST_TAG), LOAD_TIMEOUT, "notifications-list")
        awaitObject(
            By.text(str("title_contact_request_notification")),
            LOAD_TIMEOUT,
            "notifications-contact-request-row",
        )
    }

    /** tc04 — the Transfers row opens the Transfer Manager on its "No transfers" empty state. */
    @Test
    fun transfersOpensFromTheMenu() {
        openFromMenu(str("general_section_transfers"), "transfers-menu-row")

        awaitObject(By.res(TRANSFERS_VIEW_TAG), LOAD_TIMEOUT, "transfers-view")
        awaitObject(By.res(TRANSFERS_EMPTY_VIEW_TAG), LOAD_TIMEOUT, "transfers-empty-view")
    }

    /** tc05 — the Offline files row opens the Offline screen. */
    @Test
    fun offlineOpensFromTheMenu() {
        openFromMenu(str("general_section_offline_files"), "offline-menu-row")

        awaitObject(By.res(OFFLINE_EMPTY_TOP_APP_BAR_TAG), LOAD_TIMEOUT, "offline-top-bar")
        awaitObject(By.res(OFFLINE_EMPTY_TAG), LOAD_TIMEOUT, "offline-empty-view")
        awaitObject(By.text(str("offline_screen_title")), LOAD_TIMEOUT, "offline-title")
    }

    /** tc06 — the Rubbish bin row opens the Rubbish bin. */
    @Test
    fun rubbishBinOpensFromTheMenu() {
        openFromMenu(str("general_section_rubbish_bin"), "rubbish-bin-menu-row")

        awaitObject(By.res(RUBBISH_BIN_APP_BAR_TAG), LOAD_TIMEOUT, "rubbish-bin-top-bar")
        awaitObject(By.res(RUBBISH_BIN_EMPTY_VIEW_TAG), LOAD_TIMEOUT, "rubbish-bin-empty-view")
    }

    /** tc07 — the Settings row opens Settings with every category and row it renders today. */
    @Test
    fun settingsOpensFromTheMenu() {
        openFromMenu(str("general_settings"), "settings-menu-row")

        awaitObject(preferenceTitle(str("set_color_theme_label")), LOAD_TIMEOUT, "settings-screen")

        SETTINGS_SURFACE.forEach { name ->
            val title = str(name)
            scrollToText(title)
            awaitObject(settingsEntry(name, title), LOAD_TIMEOUT, "settings-entry-$name")
        }
    }

    // endregion

    // region navigation helpers

    /**
     * Launches the logged-in app and opens the Menu tab, asserting the feature Background: the
     * account entry carrying the avatar and the account email, and the Storage entry.
     */
    private fun openMenu() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)

        click(By.res(MENU_NAV_ITEM_TAG), "menu-nav-item", LAUNCH_TIMEOUT)
        awaitObject(By.res(MENU_TOOLBAR_TAG), LOAD_TIMEOUT, "menu-toolbar")

        awaitObject(By.res(MENU_AVATAR_TAG), LOAD_TIMEOUT, "menu-avatar")
        awaitObject(
            By.res(MENU_MY_ACCOUNT_ITEM_TAG).hasDescendant(By.text(fakeMegaApi.account.email)),
            LOAD_TIMEOUT,
            "menu-account-email",
        )
        awaitObject(
            menuRow(str("account_cancel_account_screen_plan_storage")),
            LOAD_TIMEOUT,
            "menu-storage-row",
        )
    }

    /** Opens the Menu tab and taps the account row titled [label]. */
    private fun openFromMenu(label: String, name: String) {
        openMenu()
        val row = menuRow(label)
        scrollUntilObject(row, name)
        click(row, name)
    }

    /** A Menu account row titled [label], as opposed to a navigation bar label of the same name. */
    private fun menuRow(label: String): BySelector =
        By.res(MENU_ACCOUNT_ITEM_TAG).hasDescendant(By.text(label))

    // endregion

    // region UiAutomator helpers

    private fun str(name: String): String {
        val id = targetContext.resources.getIdentifier(name, "string", targetContext.packageName)
        require(id != 0) { "string resource not found: $name" }
        return targetContext.getString(id)
    }

    /**
     * Matches the title of a preference row. Scoping to the row's own text view keeps a match off
     * the toolbar, which carries the same title as the Menu row that opened the screen.
     */
    private fun preferenceTitle(title: String): BySelector =
        By.res(PREFERENCE_TITLE_ID).text(title)

    private fun settingsEntry(name: String, title: String): BySelector =
        if (name in SETTINGS_ENTRIES_WITHOUT_TITLE_ID) By.text(title) else preferenceTitle(title)

    private fun click(selector: BySelector, name: String, timeout: Long = LOAD_TIMEOUT) {
        awaitObject(selector, timeout, name)
        device.findObject(selector).click()
    }

    /**
     * Scrolls the Menu's account list until [selector] is present. The list is a LazyColumn, so
     * rows below the visible area are not composed and cannot be matched until scrolled into view.
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
     * Scrolls the first scrollable container until [text] is on screen. No-op when it is already
     * visible or nothing is scrollable.
     */
    private fun scrollToText(text: String) {
        if (device.hasObject(By.text(text))) return
        runCatching {
            UiScrollable(UiSelector().scrollable(true))
                .apply { setMaxSearchSwipes(MAX_SCROLL_SWIPES) }
                .scrollTextIntoView(text)
        }
    }

    private fun awaitObject(selector: BySelector, timeout: Long, name: String) {
        if (device.wait(Until.hasObject(selector), timeout)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump",
        )
    }

    private fun dumpHierarchy(name: String) {
        val stream = ByteArrayOutputStream()
        device.dumpWindowHierarchy(stream)
        stream.toString("UTF-8").chunked(3000).forEachIndexed { index, chunk ->
            Log.d("UiDump", "[$name#$index] $chunk")
        }
    }

    // endregion

    private companion object {
        const val LAUNCH_TIMEOUT = 60_000L
        const val LOAD_TIMEOUT = 30_000L
        const val MAX_SCROLL_ATTEMPTS = 20
        const val MAX_SCROLL_SWIPES = 16
        const val SCROLL_PERCENT = 0.3f

        const val CONTACT_EMAIL = "sm02.contact@mega.co.nz"
        const val CONTACT_HANDLE = 2001L
        const val ALERT_ID = 1L

        /** Main navigation item test tag for the Menu tab (`MenuHomeScreen` destination). */
        const val MENU_NAV_ITEM_TAG = "main_navigation:navigation_item_MenuHomeScreen"

        /** Mirror MenuHomeScreenUiTestTags. */
        const val MENU_TOOLBAR_TAG = "menu_home_screen:toolbar"
        const val MENU_MY_ACCOUNT_ITEM_TAG = "menu_home_screen:my_account_item"
        const val MENU_ACCOUNT_ITEM_TAG = "menu_home_screen:account_item"
        const val MENU_AVATAR_TAG = "menu_home_screen:avatar"
        const val MENU_NOTIFICATION_ICON_TAG = "menu_home_screen:notification_icon"

        /** Mirror CONTACT_LIST_REQUESTS_ACTION_TAG / CONTACT_LIST_GROUPS_ACTION_TAG. */
        const val CONTACT_LIST_REQUESTS_ACTION_TAG = "contact_list_screen:requests_action"
        const val CONTACT_LIST_GROUPS_ACTION_TAG = "contact_list_screen:groups_action"

        /** Mirror NOTIFICATION_TOP_BAR_M3_TEST_TAG / NOTIFICATION_LIST_VIEW_M3_TEST_TAG. */
        const val NOTIFICATION_TOP_BAR_TAG = "notification_top_bar_test_tag"
        const val NOTIFICATION_LIST_TAG = "notification_list_view_test_tag"

        /** Mirror TEST_TAG_TRANSFERS_VIEW / TEST_TAG_EMPTY_TRANSFERS_VIEW. */
        const val TRANSFERS_VIEW_TAG = "transfers_view"
        const val TRANSFERS_EMPTY_VIEW_TAG = "transfers_view:empty"

        /** Mirror OFFLINE_SCREEN_EMPTY_TOP_APP_BAR_TAG / OFFLINE_SCREEN_EMPTY_TAG. */
        const val OFFLINE_EMPTY_TOP_APP_BAR_TAG = "offline_screen:empty_top_app_bar"
        const val OFFLINE_EMPTY_TAG = "offline_screen:empty"

        /** Mirror RUBBISH_BIN_MAIN_APP_BAR_TAG / NODES_EMPTY_VIEW_VISIBLE. */
        const val RUBBISH_BIN_APP_BAR_TAG = "rubbish_bin_screen:main_app_bar"
        const val RUBBISH_BIN_EMPTY_VIEW_TAG = "rubbish_bin_screen:empty_view"

        /** Title text view of an androidx preference row. */
        const val PREFERENCE_TITLE_ID = "android:id/title"

        /**
         * Entries that surface to UiAutomator without an [PREFERENCE_TITLE_ID] node, so they are
         * matched on their text alone: the category headers, whose `my_category_preference`
         * layout is a bare title TextView, and Delete account, which renders through
         * `cancel_account_preferences`.
         */
        val SETTINGS_ENTRIES_WITHOUT_TITLE_ID = setOf(
            "settings_appearance",
            "settings_features",
            "settings_storage",
            "user_interface_setting",
            "settings_media",
            "settings_security_options_title",
            "settings_help",
            "settings_about",
            "settings_delete_account",
        )

        /**
         * Every category header and preference row `preferences.xml` renders for a free account on
         * default feature flags, in the order they appear on screen.
         */
        val SETTINGS_SURFACE = listOf(
            "settings_appearance",
            "set_color_theme_label",
            "settings_features",
            "section_photo_sync",
            "settings_section_sync",
            "section_chat",
            "settings_calls_title",
            "general_section_transfers",
            "settings_storage",
            "download_location",
            "settings_file_management_category",
            "user_interface_setting",
            "start_screen_setting",
            "hide_recent_setting",
            "settings_media_discovery_sub_folder_title",
            "settings_media",
            "settings_media_audio_files",
            "settings_security_options_title",
            "settings_recovery_key_title",
            "settings_passcode_lock_switch",
            "my_account_change_password",
            "settings_2fa",
            "section_qr_code",
            "settings_help",
            "general_help_centre",
            "settings_help_rate_app",
            "settings_help_report_issue",
            "settings_about",
            "settings_about_privacy_policy",
            "settings_about_cookie_policy",
            "settings_about_cookie_settings",
            "settings_about_terms_of_service",
            "settings_about_code_link_title",
            "settings_about_sdk_version",
            "settings_about_karere_version",
            "settings_about_app_version",
            "settings_delete_account",
        )
    }
}
