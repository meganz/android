package mega.privacy.android.app

import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaRequest
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupCameraUploadsSettingUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented port of the AAT feature `sm20 SettingOptions_1` — the *Portable* scenarios
 * that assert the Settings UI reflecting and persisting setting changes. Built on the `:data-test`
 * fake-SDK framework and following [CloudDriveUploadTest] / [TransferManagerTest]: the whole app runs
 * as in production with only the SDK gateways faked, the UI is driven with UiAutomator (not a Compose
 * rule), and Compose test tags are matched as resource ids where the screen sets
 * `testTagsAsResourceId = true`.
 *
 * Two constraints shape which sm20 scenarios are ported here:
 *
 * 1. **Only screens that reach the SDK through `MegaApiGateway` / `MegaChatApiGateway` are
 *    fake-backed.** The legacy Chat settings screen and the legacy rubbish-bin "empty" action call
 *    `MegaChatApiAndroid` / `MegaApiAndroid` (and legacy `NodeController`) directly, so the fake
 *    cannot intercept them; per `TEST-PLACEMENT-POLICY.md` those stay in AAT until refactored behind
 *    the gateway. See the class-level deferral list in the MR description.
 * 2. **Legacy `PreferenceFragment` screens carry no Compose test tags**, so their rows are driven by
 *    visible title text (`By.text`, resolved from the app's string resources). The Compose Camera
 *    Uploads screen and its radio dialogs are fully tagged, so those are driven by `By.res` — the
 *    dialog options are exposed as resource ids because the dialog sets `testTagsAsResourceId = true`
 *    on its own window.
 *
 * Ported: tc01 (theme), tc03 (CU how-to/file-upload options), tc04 (CU keep-file-names + secondary
 * media), tc13 (calls sound notifications), tc15 (clear offline files), tc20 (file-versioning
 * toggle). The remaining sm20 scenarios are deferred with reasons in the MR description.
 */
@HiltAndroidTest
class SettingsOptions1Test {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    @Inject
    lateinit var setupCameraUploadsSettingUseCase: SetupCameraUploadsSettingUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private val rootHandle get() = fakeMegaApi.nodeTree.rootNode.handle

    private var scenario: ActivityScenario<MegaActivity>? = null

    @Before
    fun setUp() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        hiltRule.inject()

        // Pre-grant the permissions the settings flows request so their launchers return
        // synchronously instead of showing a system dialog (notifications for boot; media for
        // Camera Uploads).
        listOf(
            android.Manifest.permission.POST_NOTIFICATIONS,
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.READ_MEDIA_VIDEO",
        ).forEach { permission ->
            runCatching {
                instrumentation.uiAutomation.grantRuntimePermission(
                    targetContext.packageName,
                    permission,
                )
            }
        }

        // Boot the test process through the production initialiser units. See CloudDriveUploadTest.
        TestAppBoot.runCoreInitializers()

        // Home may resolve Cloud Drive children on start; resolve them through the fake node tree.
        fakeMegaApi.stub(MegaApiGateway::getChildren) {
            fakeMegaApi.nodeTree.childrenOf(rootHandle)
        }

        // File versioning is read from the request flag; report it enabled so the versioning toggle
        // starts checked and tc20 can exercise the disable-confirmation path.
        fakeMegaApi.stubRequest(
            MegaApiGateway::getFileVersionsOption,
            request = StubMegaRequest(flag = true),
        )

        fakeMegaApi.clearInvocations()

        // Persist a logged-in session through the app's real credentials path and seed Camera
        // Uploads as enabled so the CU option tiles (tc03/tc04) render without the enable flow's
        // media-permission prompt.
        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            setupCameraUploadsSettingUseCase(true)
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        // Pop any legacy sub-activities / dialogs left on the stack so tests stay isolated in the
        // shared process.
        repeat(4) { runCatching { device.pressBack() } }
    }

    // region tests

    /** tc01 — choosing a colour theme updates the shown selection (Dark, then Light). */
    @Test
    fun themeSwitchUpdatesShownSelection() {
        openSettings()

        val colourTheme = str("set_color_theme_label")
        val dark = str("theme_dark")
        val light = str("theme_light")

        scrollToText(colourTheme)
        clickText(colourTheme, "colour-theme-row")
        clickText(dark, "dark-option")
        awaitObject(By.text(dark), LOAD_TIMEOUT, "theme-summary-dark")

        scrollToText(colourTheme)
        clickText(colourTheme, "colour-theme-row-reopen")
        clickText(light, "light-option")
        awaitObject(By.text(light), LOAD_TIMEOUT, "theme-summary-light")
    }

    /** tc03 — Camera Uploads how-to-upload and file-upload options update the shown value. */
    @Test
    fun cameraUploadsHowToAndFileUploadOptions() {
        openCameraUploadsSettings()

        click(By.res(HOW_TO_UPLOAD_TILE), LOAD_TIMEOUT, "how-to-upload-tile")
        click(By.res(HOW_TO_UPLOAD_OPTION_WIFI_OR_MOBILE), LOAD_TIMEOUT, "how-to-upload-option")
        awaitObject(By.text(str("cam_sync_data")), LOAD_TIMEOUT, "how-to-upload-summary")

        click(By.res(FILE_UPLOAD_TILE), LOAD_TIMEOUT, "file-upload-tile")
        click(By.res(FILE_UPLOAD_OPTION_VIDEOS_ONLY), LOAD_TIMEOUT, "file-upload-option")
        awaitObject(
            By.text(str("settings_camera_upload_only_videos")),
            LOAD_TIMEOUT,
            "file-upload-summary",
        )
    }

    /** tc04 — Camera Uploads keep-file-names toggles, and secondary media uploads can be enabled. */
    @Test
    fun cameraUploadsKeepFileNamesAndSecondaryMedia() {
        openCameraUploadsSettings()

        awaitObject(By.res(KEEP_FILE_NAMES_CHECKBOX), LOAD_TIMEOUT, "keep-file-names-checkbox")
        val initiallyChecked = device.findObject(By.res(KEEP_FILE_NAMES_CHECKBOX)).isChecked
        device.findObject(By.res(KEEP_FILE_NAMES_CHECKBOX)).click()
        awaitChecked(KEEP_FILE_NAMES_CHECKBOX, expected = !initiallyChecked, name = "keep-file-names")

        // Secondary media uploads start disabled; the row offers to enable, then flips to "Disable".
        click(By.text(str("settings_secondary_upload_on")), LOAD_TIMEOUT, "enable-secondary-media")
        awaitObject(
            By.text(str("settings_secondary_upload_off")),
            LOAD_TIMEOUT,
            "secondary-media-enabled",
        )
    }

    /** tc13 — the Calls settings screen shows the sound-notifications option. */
    @Test
    fun callsSettingsShowsSoundNotifications() {
        openSettings()

        val calls = str("settings_calls_title")
        scrollToText(calls)
        clickText(calls, "calls-row")

        awaitObject(
            By.text(str("settings_calls_sound_notifications_title")),
            LOAD_TIMEOUT,
            "sound-notifications-label",
        )
    }

    /** tc15 — clearing offline files shows the confirmation dialog and completes. */
    @Test
    fun clearOfflineFilesConfirmationFlow() {
        openFileManagementSettings()

        val clearOffline = str("settings_advanced_features_offline")
        scrollToText(clearOffline)
        clickText(clearOffline, "clear-offline-row")

        val confirmation = str("clear_offline_confirmation")
        awaitObject(By.text(confirmation), LOAD_TIMEOUT, "clear-offline-dialog")
        clickText(str("general_clear"), "clear-offline-confirm")
        awaitGone(By.text(confirmation), "clear-offline-dialog-dismissed")
    }

    /** tc20 — disabling file versioning confirms and requests the SDK to disable it. */
    @Test
    fun fileVersioningDisableRequestsGateway() {
        openFileManagementSettings()

        val versioning = str("settings_enable_file_versioning_title")
        scrollToText(versioning)
        clickText(versioning, "file-versioning-row")

        awaitObject(By.text(str("disable_versioning_label")), LOAD_TIMEOUT, "disable-versioning-dialog")
        clickText(str("verify_2fa_subtitle_diable_2fa"), "disable-versioning-confirm")

        awaitInvocations("setFileVersionsOption") {
            fakeMegaApi.invocations.filter { it.methodName == "setFileVersionsOption" }
        }
    }

    // endregion

    // region navigation helpers

    /** Launches the logged-in app and opens the legacy Settings screen via the Menu tab. */
    private fun openSettings() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)

        click(By.res(MENU_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "menu-nav-item")

        val settings = str("general_settings")
        scrollToText(settings)
        clickText(settings, "settings-menu-item")

        // The Appearance "Colour theme" row is near the top of the settings list; use it as the
        // "settings screen is up" signal.
        awaitObject(By.text(str("set_color_theme_label")), LOAD_TIMEOUT, "settings-screen")
    }

    private fun openFileManagementSettings() {
        openSettings()
        val fileManagement = str("settings_file_management_category")
        scrollToText(fileManagement)
        clickText(fileManagement, "file-management-row")
    }

    private fun openCameraUploadsSettings() {
        openSettings()
        val cameraUploads = str("section_photo_sync")
        scrollToText(cameraUploads)
        clickText(cameraUploads, "camera-uploads-row")
        // The option tiles render because Camera Uploads is seeded enabled in setUp().
        awaitObject(By.res(HOW_TO_UPLOAD_TILE), LOAD_TIMEOUT, "camera-uploads-options")
    }

    // endregion

    // region invocation helpers

    /**
     * Preference / gateway writes are dispatched from background scopes, so invocations are not
     * recorded synchronously after the UI action. Poll [supplier] until at least one match, or fail
     * with what was actually recorded. Mirrors [TransferManagerTest.awaitInvocations].
     */
    private fun <T> awaitInvocations(
        name: String,
        timeoutMs: Long = LOAD_TIMEOUT,
        supplier: () -> List<T>,
    ): List<T> {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            val matches = supplier()
            if (matches.isNotEmpty()) return matches
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Timed out after ${timeoutMs}ms waiting for an invocation of $name; " +
                    "recorded methods=${fakeMegaApi.invocations.map { it.methodName }}",
        )
    }

    // endregion

    // region UiAutomator helpers

    private fun str(name: String): String {
        val id = targetContext.resources.getIdentifier(name, "string", targetContext.packageName)
        require(id != 0) { "string resource not found: $name" }
        return targetContext.getString(id)
    }

    private fun click(selector: BySelector, timeout: Long, name: String) {
        awaitObject(selector, timeout, name)
        device.findObject(selector).click()
    }

    private fun clickText(text: String, name: String) = click(By.text(text), LOAD_TIMEOUT, name)

    private fun awaitChecked(tag: String, expected: Boolean, name: String) {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            if (device.findObject(By.res(tag))?.isChecked == expected) return
            Thread.sleep(POLL_INTERVAL_MS)
        }
        dumpHierarchy(name)
        throw AssertionError("Timed out waiting for $name checkbox to be checked=$expected")
    }

    /**
     * Scrolls the first scrollable container until [text] is on screen. No-op when it is already
     * visible or nothing is scrollable — legacy preference lists and the Compose menu list both
     * expose a scrollable container.
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

    private fun awaitGone(selector: BySelector, name: String) {
        if (device.wait(Until.gone(selector), LOAD_TIMEOUT)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name ($selector) to disappear; hierarchy in logcat tag UiDump",
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
        const val LAUNCH_TIMEOUT = 30_000L
        const val LOAD_TIMEOUT = 15_000L
        const val POLL_INTERVAL_MS = 100L
        const val MAX_SCROLL_SWIPES = 12

        /** Main navigation item test tag for the Menu tab (`MenuHomeScreen` destination). */
        const val MENU_NAV_ITEM_TAG = "main_navigation:navigation_item_MenuHomeScreen"

        /** Camera Uploads option tile tags (Compose, `testTagsAsResourceId`). */
        const val HOW_TO_UPLOAD_TILE = "how_to_upload_tile:generic_two_line_list_item"
        const val FILE_UPLOAD_TILE = "file_upload_tile:generic_two_line_list_item"
        const val KEEP_FILE_NAMES_CHECKBOX = "keep_file_names_tile:mega_checkbox"

        /**
         * Radio-dialog option tags. The dialog sets `testTagsAsResourceId = true` on its own window,
         * so these resolve by resource id even though the dialog renders in a separate window. The
         * suffix is the option enum's name.
         */
        const val HOW_TO_UPLOAD_OPTION_WIFI_OR_MOBILE =
            "confirmation_dialog_with_radio_buttons:text_option_WIFI_OR_MOBILE_DATA"
        const val FILE_UPLOAD_OPTION_VIDEOS_ONLY =
            "confirmation_dialog_with_radio_buttons:text_option_VideosOnly"
    }
}
