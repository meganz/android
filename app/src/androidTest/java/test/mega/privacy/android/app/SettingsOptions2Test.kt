package mega.privacy.android.app

import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaError
import mega.privacy.android.data.test.stub.StubMegaRequest
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import nz.mega.sdk.MegaError
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented port of the AAT feature `sm21 SettingOptions_2` — the gateway-clean
 * scenarios of the second Settings smoke feature. Built on the `:data-test` fake-SDK framework and
 * following [SettingsOptions1Test]: the whole app runs as in production with only the SDK gateways
 * faked, the UI is driven with UiAutomator, legacy preference rows are matched by their visible
 * title text (resolved from string resources), and Compose test tags are matched as resource ids
 * where the screen sets `testTagsAsResourceId = true`.
 *
 * Ported: tc01 (start screen selection), tc02 (recovery key copy), tc03+tc04 (passcode enable
 * journey and options sheet), tc05 (change password entry UI), tc06 (2FA setup with wrong code),
 * tc10 (report issue submit), tc16 (app version taps toggle chat logging), tc17 (delete account
 * dialog).
 *
 * Skipped: the tc02 save-to-file branch (legacy in-app `FileStorageActivity` picker gated on the
 * WRITE_EXTERNAL_STORAGE permission flow — not cleanly stubbable), the tc04 relock-after-timeout
 * tail (requires 20s of real backgrounding), tc07 (the Advanced sub-menu with "Don't use HTTP" no
 * longer exists in the app — the Transfers settings screen only exposes connection counts and the
 * `setting_title_use_https_only` string is unreferenced by any screen), and tc08/tc09/tc11–tc15
 * (real web pages, app-store hand-offs and cookie web links — out of scope).
 */
@HiltAndroidTest
class SettingsOptions2Test {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

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

        // Pre-grant notifications so the boot permission launcher returns synchronously instead of
        // showing a system dialog.
        runCatching {
            instrumentation.uiAutomation.grantRuntimePermission(
                targetContext.packageName,
                android.Manifest.permission.POST_NOTIFICATIONS,
            )
        }

        // Boot the test process through the production initialiser units. See CloudDriveUploadTest.
        TestAppBoot.runCoreInitializers()

        // Home may resolve Cloud Drive children on start; resolve them through the fake node tree.
        fakeMegaApi.stub(MegaApiGateway::getChildren) {
            fakeMegaApi.nodeTree.childrenOf(rootHandle)
        }

        fakeMegaApi.clearInvocations()

        // Persist a logged-in session through the app's real credentials path; account details are
        // fetched eagerly because delete-account visibility waits for a non-empty account.
        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
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

    /** tc01 — choosing a start screen option updates the shown selection on Settings. */
    @Test
    fun startScreenSelectionUpdatesSummary() {
        openSettings()

        val startScreen = str("start_screen_setting")
        scrollToText(startScreen)
        awaitObject(By.text(str("general_section_home")), LOAD_TIMEOUT, "start-screen-summary-home")
        clickText(startScreen, "start-screen-row")

        awaitObject(By.text(str("configure_start_screen")), LOAD_TIMEOUT, "start-screen-picker")
        val drive = str("general_drive")
        clickText(drive, "drive-option")
        device.pressBack()

        scrollToText(startScreen)
        awaitObject(By.text(drive), LOAD_TIMEOUT, "start-screen-summary-drive")
    }

    /** tc02 — copying the recovery key requests the master key and confirms the copy. */
    @Test
    fun recoveryKeyCopyShowsConfirmation() {
        openSettings()

        val recoveryKey = str("settings_recovery_key_title")
        scrollToText(recoveryKey)
        clickText(recoveryKey, "recovery-key-row")

        awaitObject(By.text(str("backup_title")), LOAD_TIMEOUT, "recovery-key-screen")
        clickText(str("context_copy"), "copy-button")

        awaitObject(By.text(str("copy_MK_confirmation")), LOAD_TIMEOUT, "copy-confirmation")
        awaitInvocations("getExportMasterKey") {
            fakeMegaApi.invocations.filter { it.methodName == "getExportMasterKey" }
        }
        device.pressBack()
        awaitGone(By.text(str("copy_MK_confirmation")), "copy-confirmation-dismissed")
    }

    /**
     * tc03 + tc04 — enabling the passcode walks the PIN setup journey (with the passcode options
     * sheet rendering its 4-digit/6-digit/alphanumeric choices) and ends with the passcode enabled
     * and its options tiles shown.
     *
     * Carries the demo-clip camera holds: short pauses on each surface, ending on a 2s hold.
     */
    @Test
    fun passcodeEnableJourneyShowsOptionsAndEnabledState() {
        openSettings()

        val passcodeLock = str("settings_passcode_lock_switch")
        scrollToText(passcodeLock)
        clickText(passcodeLock, "passcode-lock-row")

        awaitObject(By.res(ENABLE_PASSCODE_TILE), LOAD_TIMEOUT, "enable-passcode-tile")
        Thread.sleep(1_000)
        click(By.res(ENABLE_PASSCODE_TILE), LOAD_TIMEOUT, "enable-passcode-tile")
        dismissAutofillIfShown()

        // Legacy PasscodeLockActivity in set mode.
        awaitObject(By.text(str("unlock_pin_title")), LOAD_TIMEOUT, "enter-passcode-title")
        awaitObject(By.res(viewId("pass_first_input")), LOAD_TIMEOUT, "passcode-first-input")
        Thread.sleep(1_500)

        // tc03 — the passcode options sheet renders its type choices.
        click(By.res(viewId("passcode_options_button")), LOAD_TIMEOUT, "passcode-options-button")
        awaitObject(By.text(str("four_pin_lock")), LOAD_TIMEOUT, "four-digits-option")
        awaitObject(By.text(str("six_pin_lock")), LOAD_TIMEOUT, "six-digits-option")
        awaitObject(By.text(str("AN_pin_lock")), LOAD_TIMEOUT, "alphanumeric-option")
        Thread.sleep(1_500)
        device.pressBack()
        awaitGone(By.text(str("AN_pin_lock")), "passcode-options-dismissed")

        // tc04 — set and confirm a 4-digit passcode.
        enterPasscode("1111")
        awaitObject(By.text(str("unlock_pin_title_2")), LOAD_TIMEOUT, "reenter-passcode-title")
        Thread.sleep(1_000)
        enterPasscode("1111")

        // Back on the Compose passcode settings screen: enabled state and the options tiles.
        awaitObject(By.res(CHANGE_PASSCODE_TILE), LOAD_TIMEOUT, "change-passcode-tile")
        awaitObject(By.res(REQUIRE_PASSCODE_TILE), LOAD_TIMEOUT, "require-passcode-tile")
        // The Change and Require tiles awaited above render only while the passcode is enabled
        // (PasscodeSettingsView gates them on state.isEnabled), so they are the enabled-state
        // proof. The tile's own MegaSwitch is display-only and exposes no checked semantics, and
        // its 1dp divider never surfaces to UiAutomator.
        Thread.sleep(2_000)
    }

    /** tc05 — the Change password screen renders its entry UI. */
    @Test
    fun changePasswordScreenRendersEntryUi() {
        openSettings()

        val changePassword = str("my_account_change_password")
        scrollToText(changePassword)
        clickText(changePassword, "change-password-row")

        awaitObject(
            By.text(str("my_account_change_password_newPassword1")),
            LOAD_TIMEOUT,
            "new-password-field",
        )
        awaitObject(
            By.text(str("my_account_change_password_newPassword2")),
            LOAD_TIMEOUT,
            "confirm-password-field",
        )
    }

    /** tc06 — the 2FA setup journey shows the seed, and a wrong code shows the invalid-code error. */
    @Test
    fun twoFactorSetupWrongCodeShowsError() {
        fakeMegaApi.stubRequest(
            MegaApiGateway::getMultiFactorAuthCode,
            request = StubMegaRequest(text = TWO_FA_SEED),
        )
        fakeMegaApi.stubRequest(
            MegaApiGateway::enableMultiFactorAuth,
            request = StubMegaRequest(flag = true),
            error = StubMegaError(MegaError.API_EFAILED),
        )

        openSettings()

        val twoFactor = str("settings_2fa")
        scrollToText(twoFactor)
        clickText(twoFactor, "two-factor-row")

        awaitObject(By.text(str("title_2fa")), LOAD_TIMEOUT, "two-factor-initialisation-screen")
        clickText(str("button_setup_2fa"), "begin-setup-button")

        awaitObject(By.res(SEED_BOX_TAG), LOAD_TIMEOUT, "seed-box")
        clickText(str("general_next"), "next-button")

        awaitObject(
            By.text(str("multi_factor_auth_login_verification_content")),
            LOAD_TIMEOUT,
            "authentication-screen",
        )
        // The pin field requests focus itself; wait for it before injecting key events.
        device.wait(Until.hasObject(By.focused(true)), LOAD_TIMEOUT)
        repeat(6) { device.pressKeyCode(KeyEvent.KEYCODE_1) }

        awaitObject(By.text(str("pin_error_2fa")), LOAD_TIMEOUT, "invalid-code-error")
        awaitInvocations("enableMultiFactorAuth") {
            fakeMegaApi.invocations.filter { it.methodName == "enableMultiFactorAuth" }
        }
    }

    /** tc10 — submitting a report shows the received-report confirmation. */
    @Test
    fun reportIssueSubmitShowsConfirmation() {
        openSettings()

        val reportIssue = str("settings_help_report_issue")
        scrollToText(reportIssue)
        clickText(reportIssue, "report-issue-row")

        awaitObject(
            By.text(str("settings_help_report_issue_instructions")),
            LOAD_TIMEOUT,
            "report-issue-screen",
        )

        val descriptionField = By.clazz("android.widget.EditText")
        click(descriptionField, LOAD_TIMEOUT, "describe-issue-input")
        device.findObject(descriptionField).text = REPORT_DESCRIPTION

        click(
            By.desc(str("settings_help_report_issue_submit_menu_action")),
            LOAD_TIMEOUT,
            "submit-report-action",
        )

        awaitObject(
            By.text(str("settings_help_report_issue_success")),
            LOAD_TIMEOUT,
            "report-confirmation",
        )
        awaitInvocations("createSupportTicket") {
            fakeMegaApi.invocations.filter { it.methodName == "createSupportTicket" }
        }
    }

    /** tc16 — tapping the app version five times toggles chat-message debugging with a snackbar. */
    @Test
    fun appVersionFiveTapsToggleChatLogging() {
        openSettings()

        val appVersion = str("settings_about_app_version")
        scrollToText(appVersion)

        repeat(5) { clickText(appVersion, "app-version-row") }
        val enabled = By.text(str("show_info_chat_msg_enabled"))
        awaitObject(enabled, LOAD_TIMEOUT, "logging-enabled-snackbar")
        // Let the snackbar clear so it cannot swallow the next round of taps.
        awaitGone(enabled, "logging-enabled-snackbar-gone")

        repeat(5) { clickText(appVersion, "app-version-row") }
        awaitObject(
            By.text(str("show_info_chat_msg_disabled")),
            LOAD_TIMEOUT,
            "logging-disabled-snackbar",
        )
    }

    /** tc17 — the delete-account popup shows its warning and dismisses safely. */
    @Test
    fun deleteAccountDialogShowsAndDismisses() {
        openSettings()

        val deleteAccount = str("settings_delete_account")
        scrollToText(deleteAccount)
        clickText(deleteAccount, "delete-account-row")

        val warning = By.text(str("delete_account_text"))
        awaitObject(warning, LOAD_TIMEOUT, "delete-account-dialog")
        awaitObject(By.text(str("general_dismiss_dialog")), LOAD_TIMEOUT, "dismiss-action")

        clickText(str("general_dismiss_dialog"), "delete-account-dismiss")
        awaitGone(warning, "delete-account-dialog-dismissed")
    }

    // endregion

    // region navigation helpers

    /** Launches the logged-in app and opens the legacy Settings screen via the Menu tab. */
    private fun openSettings() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)

        click(By.res(MENU_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "menu-nav-item")

        // The Menu rows load asynchronously; scrolling before the account list is composed strands
        // the Settings row off screen.
        awaitObject(By.res(MENU_ACCOUNT_ITEM_TAG), LOAD_TIMEOUT, "menu-account-item")

        val settings = str("general_settings")
        scrollToText(settings)
        clickText(settings, "settings-menu-item")

        // The Appearance "Colour theme" row is near the top of the settings list; use it as the
        // "settings screen is up" signal.
        awaitObject(By.text(str("set_color_theme_label")), LOAD_TIMEOUT, "settings-screen")
    }

    /**
     * Types [digits] into the legacy passcode inputs by setting each field's text directly, which
     * is independent of the fields' auto-advancing focus.
     */
    private fun enterPasscode(digits: String) {
        digits.forEachIndexed { index, digit ->
            val selector = By.res(viewId(PASSCODE_INPUT_IDS[index]))
            awaitObject(selector, LOAD_TIMEOUT, "passcode-input-$index")
            device.findObject(selector).text = digit.toString()
        }
    }

    // endregion

    // region invocation helpers

    /**
     * Preference / gateway writes are dispatched from background scopes, so invocations are not
     * recorded synchronously after the UI action. Poll [supplier] until at least one match, or fail
     * with what was actually recorded. Mirrors [SettingsOptions1Test.awaitInvocations].
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

    /** Fully-qualified resource id for a legacy view id in the app package. */
    private fun viewId(name: String): String = "${targetContext.packageName}:id/$name"

    private fun click(selector: BySelector, timeout: Long, name: String) {
        awaitObject(selector, timeout, name)
        device.findObject(selector).click()
    }

    private fun clickText(text: String, name: String) = click(By.text(text), LOAD_TIMEOUT, name)

    /**
     * On real devices Google's password autofill can overlay a "Use your saved password?" dialog
     * the moment a PIN/password field gains focus, blocking the screen underneath. Dismiss it if
     * it appears; on devices without saved credentials this is a short no-op wait.
     */
    private fun dismissAutofillIfShown() {
        if (device.wait(Until.hasObject(By.res(AUTOFILL_DISMISS_BUTTON)), 2_000)) {
            device.findObject(By.res(AUTOFILL_DISMISS_BUTTON)).click()
            device.wait(Until.gone(By.res(AUTOFILL_DISMISS_BUTTON)), LOAD_TIMEOUT)
        }
    }


    /**
     * Scrolls the first scrollable container until [text] is on screen. Retries because some rows
     * (delete account, 2FA) appear only once their async visibility state lands.
     */
    private fun scrollToText(text: String) {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            if (device.hasObject(By.text(text))) return
            runCatching {
                UiScrollable(UiSelector().scrollable(true))
                    .apply { setMaxSearchSwipes(MAX_SCROLL_SWIPES) }
                    .scrollTextIntoView(text)
            }
            if (device.hasObject(By.text(text))) return
            Thread.sleep(POLL_INTERVAL_MS)
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

        /** Cancel button of the Google Play services password-autofill overlay. */
        const val AUTOFILL_DISMISS_BUTTON = "android:id/autofill_dialog_no"
        const val MAX_SCROLL_SWIPES = 12

        /** Main navigation item test tag for the Menu tab (`MenuHomeScreen` destination). */
        const val MENU_NAV_ITEM_TAG = "main_navigation:navigation_item_MenuHomeScreen"

        /** Mirror MenuHomeScreenTestTags — the Menu's async account list. */
        const val MENU_ACCOUNT_ITEM_TAG = "menu_home_screen:account_item"

        /** Passcode settings tile tags (Compose, `testTagsAsResourceId`). */
        const val ENABLE_PASSCODE_TILE = "enable_passcode_tile:generic_two_line_list_item"
        const val CHANGE_PASSCODE_TILE = "change_passcode_tile:generic_two_line_list_item"
        const val REQUIRE_PASSCODE_TILE = "require_passcode_tile:generic_two_line_list_item"

        /** 2FA setup screen seed box tag (Compose, `testTagsAsResourceId`). */
        const val SEED_BOX_TAG = "authentication_setup_screen:mega_seed_box_codes"

        /** Legacy PasscodeLockActivity digit inputs, in entry order. */
        val PASSCODE_INPUT_IDS = listOf(
            "pass_first_input",
            "pass_second_input",
            "pass_third_input",
            "pass_fourth_input",
        )

        /** Base32-looking seed delivered by the stubbed `getMultiFactorAuthCode` request. */
        /** Exactly 52 base32 chars — the setup screen substrings the seed into 13 groups of 4. */
        const val TWO_FA_SEED = "MFRGGZDFMZTWQ2LKNNWG23TPOBYXE43UOVWXI2LOM4QGCZLMMFRG"

        const val REPORT_DESCRIPTION =
            "Automated test feedback provided, please ignore this instrumented report."
    }
}
