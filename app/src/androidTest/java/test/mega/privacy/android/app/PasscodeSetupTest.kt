package mega.privacy.android.app

import android.content.Intent
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
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.usecase.MonitorPasscodeLockPreferenceUseCase
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.passcode.DisablePasscodeUseCase
import mega.privacy.android.domain.usecase.passcode.EnableBiometricsUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeTypeUseCase
import mega.privacy.android.domain.usecase.passcode.SetPasscodeTimeoutUseCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Instrumented coverage of the Compose set-passcode journey that replaced the legacy passcode
 * Activity. Built on the same `:data-test` fake-SDK + UiAutomator harness as [SettingsOptions2Test]:
 * the whole app runs as in production with only the SDK gateways faked, and the setup screen's
 * Compose test tags are matched as resource ids because it sets `testTagsAsResourceId = true`.
 *
 * The passcode itself is not SDK-backed, so its state is seeded and torn down through the domain
 * use cases rather than the fake gateway, and the require-passcode timeout is pinned to
 * [PasscodeTimeout.Immediate] so the relock scenarios do not need real backgrounding time.
 *
 * Not covered: the biometric toggle is driven through the platform `BiometricPrompt`, which cannot
 * be answered from the test process, so [changingThePasscodeKeepsBiometricsEnabled] switches
 * biometrics on through its use case and asserts the type that survives the change rather than
 * tapping the tile.
 */
@HiltAndroidTest
class PasscodeSetupTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    @Inject
    lateinit var disablePasscodeUseCase: DisablePasscodeUseCase

    @Inject
    lateinit var setPasscodeTimeoutUseCase: SetPasscodeTimeoutUseCase

    @Inject
    lateinit var enableBiometricsUseCase: EnableBiometricsUseCase

    @Inject
    lateinit var monitorPasscodeTypeUseCase: MonitorPasscodeTypeUseCase

    @Inject
    lateinit var monitorPasscodeLockPreferenceUseCase: MonitorPasscodeLockPreferenceUseCase

    @Inject
    lateinit var passcodeRepository: PasscodeRepository

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

        runCatching {
            instrumentation.uiAutomation.grantRuntimePermission(
                targetContext.packageName,
                android.Manifest.permission.POST_NOTIFICATIONS,
            )
        }

        TestAppBoot.runCoreInitializers()

        fakeMegaApi.stub(MegaApiGateway::getChildren) {
            fakeMegaApi.nodeTree.childrenOf(rootHandle)
        }

        fakeMegaApi.clearInvocations()

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            // The passcode and its lock state outlive an Activity, so anything left behind by
            // another test would lock this one out before it reached its own scenario.
            disablePasscodeUseCase()
            passcodeRepository.setLocked(false)
            setPasscodeTimeoutUseCase(PasscodeTimeout.Immediate)
        }
    }

    @After
    fun tearDown() {
        runCatching { device.setOrientationNatural() }
        runBlocking {
            runCatching {
                disablePasscodeUseCase()
                passcodeRepository.setLocked(false)
            }
        }
        scenario?.close()
        repeat(4) { runCatching { device.pressBack() } }
    }

    // region tests

    /**
     * AND-22071 — cycling through the passcode formats before entering anything must not leave a
     * stale first entry behind, so the confirmation of an untouched 4 digit passcode matches.
     */
    @Test
    fun switchingPasscodeTypesDoesNotStrandAStaleFirstEntry() {
        openPasscodeSettings()
        startPasscodeSetup()

        selectPasscodeType("AN_pin_lock")
        awaitObject(By.res(SETUP_PASSCODE_PASSWORD_FIELD), LOAD_TIMEOUT, "alphanumeric-field")
        selectPasscodeType("six_pin_lock")
        awaitObject(By.res(SETUP_PASSCODE_FIELD), LOAD_TIMEOUT, "six-digit-field")
        selectPasscodeType("four_pin_lock")
        awaitObject(By.res(SETUP_PASSCODE_FIELD), LOAD_TIMEOUT, "four-digit-field")

        typePasscode("1234")
        awaitReEntryRound()
        assertNoMismatchError()
        typePasscode("1234")

        awaitPasscodeEnabledSettings()
    }

    /** A genuine mismatch reports it, and the retry that matches still saves the passcode. */
    @Test
    fun mismatchedConfirmationShowsTheErrorAndAllowsARetry() {
        openPasscodeSettings()
        startPasscodeSetup()

        typePasscode("1234")
        awaitReEntryRound()
        typePasscode("4321")

        awaitObject(By.res(SETUP_PASSCODE_MISMATCH_ERROR), LOAD_TIMEOUT, "mismatch-error")

        typePasscode("1234")

        awaitPasscodeEnabledSettings()
    }

    /** Changing the passcode of a biometric-backed passcode keeps biometrics switched on. */
    @Test
    fun changingThePasscodeKeepsBiometricsEnabled() {
        openPasscodeSettings()
        setPasscodeThroughTheUi("1234")
        runBlocking { enableBiometricsUseCase() }

        click(By.res(CHANGE_PASSCODE_TILE), LOAD_TIMEOUT, "change-passcode-tile")
        dismissAutofillIfShown()

        awaitObject(By.text(str("reset_pin_title")), LOAD_TIMEOUT, "change-passcode-title")
        typePasscode("5678")
        awaitObject(By.text(str("reset_pin_title_2")), LOAD_TIMEOUT, "change-passcode-reenter")
        typePasscode("5678")

        awaitPasscodeEnabledSettings()
        assertThat(currentPasscodeType()).isInstanceOf(PasscodeType.Biometric::class.java)
    }

    /** Rotating mid-journey keeps the user on the round they had reached, with an empty field. */
    @Test
    fun rotatingKeepsTheConfirmationRound() {
        openPasscodeSettings()
        startPasscodeSetup()

        typePasscode("1234")
        awaitReEntryRound()

        device.setOrientationLeft()

        awaitReEntryRound()
        // A first entry surviving the rotation inside the field would make this four digit
        // confirmation over-long and never complete.
        typePasscode("1234")

        device.setOrientationNatural()
        awaitPasscodeEnabledSettings()
    }

    /** A newly set 4 digit passcode locks the app on return and unlocks with the same digits. */
    @Test
    fun aNewPasscodeUnlocksTheAppAfterBackgrounding() {
        openPasscodeSettings()
        setPasscodeThroughTheUi("1234")

        returnToMainNavigation()
        backgroundAndReturn()

        awaitUnlockScreen()
        typeKeys("1234")
        awaitUnlocked()
    }

    /** A 6 digit passcode can be set and then unlocks the app. */
    @Test
    fun aSixDigitPasscodeCanBeSetAndUnlocksTheApp() {
        openPasscodeSettings()
        startPasscodeSetup()

        selectPasscodeType("six_pin_lock")
        typePasscode("123456")
        awaitReEntryRound()
        typePasscode("123456")
        awaitPasscodeEnabledSettings()

        returnToMainNavigation()
        backgroundAndReturn()

        awaitUnlockScreen()
        typeKeys("123456")
        awaitUnlocked()
    }

    /** An alphanumeric passcode can be set and then unlocks the app. */
    @Test
    fun anAlphanumericPasscodeCanBeSetAndUnlocksTheApp() {
        openPasscodeSettings()
        startPasscodeSetup()

        selectPasscodeType("AN_pin_lock")
        typePassword(ALPHANUMERIC_PASSCODE)
        awaitReEntryRound()
        typePassword(ALPHANUMERIC_PASSCODE)
        awaitPasscodeEnabledSettings()

        returnToMainNavigation()
        backgroundAndReturn()

        awaitUnlockScreen()
        typeUnlockPassword(ALPHANUMERIC_PASSCODE)
        awaitUnlocked()
    }

    /** Turning the passcode lock off stops the app asking for a passcode again. */
    @Test
    fun disablingThePasscodeStopsTheUnlockPrompt() {
        openPasscodeSettings()
        setPasscodeThroughTheUi("1234")

        click(By.res(ENABLE_PASSCODE_TILE), LOAD_TIMEOUT, "enable-passcode-tile")
        awaitGone(By.res(CHANGE_PASSCODE_TILE), "change-passcode-tile-gone")
        assertThat(passcodeIsEnabled()).isFalse()

        returnToMainNavigation()
        backgroundAndReturn()

        awaitUnlocked()
        assertThat(device.hasObject(By.text(str("unlock_pin_title")))).isFalse()
    }

    // endregion

    // region passcode journey helpers

    /** Launches the logged-in app and opens the Compose Passcode lock settings screen. */
    private fun openPasscodeSettings() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)

        click(By.res(MENU_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "menu-nav-item")
        awaitObject(By.res(MENU_ACCOUNT_ITEM_TAG), LOAD_TIMEOUT, "menu-account-item")

        val settings = str("general_settings")
        scrollToText(settings)
        clickText(settings, "settings-menu-item")
        awaitObject(By.text(str("set_color_theme_label")), LOAD_TIMEOUT, "settings-screen")

        val passcodeLock = str("settings_passcode_lock_switch")
        scrollToText(passcodeLock)
        clickText(passcodeLock, "passcode-lock-row")
        awaitObject(By.res(ENABLE_PASSCODE_TILE), LOAD_TIMEOUT, "enable-passcode-tile")
    }

    private fun startPasscodeSetup() {
        click(By.res(ENABLE_PASSCODE_TILE), LOAD_TIMEOUT, "enable-passcode-tile")
        dismissAutofillIfShown()
        awaitObject(By.text(str("unlock_pin_title")), LOAD_TIMEOUT, "set-passcode-title")
    }

    /** Opens the passcode options sheet and picks the format labelled by [labelResource]. */
    private fun selectPasscodeType(labelResource: String) {
        click(By.res(SETUP_PASSCODE_OPTIONS_BUTTON), LOAD_TIMEOUT, "passcode-options-button")
        awaitObject(By.text(str("AN_pin_lock")), LOAD_TIMEOUT, "passcode-options-sheet")
        clickText(str(labelResource), "passcode-type-option")
        awaitGone(By.text(str("AN_pin_lock")), "passcode-options-dismissed")
    }

    /** Walks the two entry rounds of the setup screen and returns to the settings screen. */
    private fun setPasscodeThroughTheUi(passcode: String) {
        startPasscodeSetup()
        typePasscode(passcode)
        awaitReEntryRound()
        typePasscode(passcode)
        awaitPasscodeEnabledSettings()
    }

    private fun typePasscode(digits: String) {
        awaitObject(By.res(SETUP_PASSCODE_FIELD), LOAD_TIMEOUT, "setup-passcode-field")
        typeKeys(digits)
    }

    /**
     * Fills a free form passcode field and submits it with the IME action the field listens for.
     */
    private fun typePassword(password: String) {
        awaitObject(By.res(SETUP_PASSCODE_PASSWORD_FIELD), LOAD_TIMEOUT, "password-field")
        typeKeys(password)
        device.pressKeyCode(KeyEvent.KEYCODE_ENTER)
    }

    /**
     * The unlock screen does not expose its test tags as resource ids, so its free form field is
     * reached through the focus it requests for itself.
     */
    private fun typeUnlockPassword(password: String) {
        typeKeys(password)
        device.pressKeyCode(KeyEvent.KEYCODE_ENTER)
    }

    /** Types [value] as hardware key events into whichever field currently holds focus. */
    private fun typeKeys(value: String) {
        device.wait(Until.hasObject(By.focused(true)), LOAD_TIMEOUT)
        value.forEach { device.pressKeyCode(keyCodeOf(it)) }
    }

    private fun keyCodeOf(character: Char) = when (character) {
        in '0'..'9' -> KeyEvent.KEYCODE_0 + (character - '0')
        in 'a'..'z' -> KeyEvent.KEYCODE_A + (character - 'a')
        else -> throw IllegalArgumentException("No key code mapped for '$character'")
    }

    private fun awaitReEntryRound() {
        awaitObject(By.text(str("unlock_pin_title_2")), LOAD_TIMEOUT, "re-enter-passcode-title")
        awaitGone(By.res(SETUP_PASSCODE_OPTIONS_BUTTON), "passcode-options-button-gone")
    }

    private fun assertNoMismatchError() {
        assertThat(device.hasObject(By.res(SETUP_PASSCODE_MISMATCH_ERROR))).isFalse()
    }

    /**
     * The Change and Require tiles render only while the passcode is enabled
     * (`PasscodeSettingsView` gates them on `state.isEnabled`), so they are the enabled-state proof.
     */
    private fun awaitPasscodeEnabledSettings() {
        awaitObject(By.res(CHANGE_PASSCODE_TILE), LOAD_TIMEOUT, "change-passcode-tile")
        awaitObject(By.res(REQUIRE_PASSCODE_TILE), LOAD_TIMEOUT, "require-passcode-tile")
    }

    /** Backs out of the settings stack until the main navigation is showing again. */
    private fun returnToMainNavigation() {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            if (device.hasObject(By.res(MENU_NAV_ITEM_TAG))) return
            device.pressBack()
            device.wait(Until.hasObject(By.res(MENU_NAV_ITEM_TAG)), BACK_STEP_TIMEOUT)
        }
        dumpHierarchy("main-navigation")
        throw AssertionError("Timed out backing out to the main navigation; hierarchy in logcat tag UiDump")
    }

    private fun backgroundAndReturn() {
        device.pressHome()
        device.wait(Until.gone(By.pkg(targetContext.packageName).depth(0)), LOAD_TIMEOUT)

        val launchIntent = requireNotNull(
            targetContext.packageManager.getLaunchIntentForPackage(targetContext.packageName)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        targetContext.startActivity(launchIntent)

        device.wait(Until.hasObject(By.pkg(targetContext.packageName).depth(0)), LAUNCH_TIMEOUT)
    }

    /**
     * The unlock screen shares its title with the first round of the setup screen, so it is
     * identified by that title appearing without the setup screen's options button.
     */
    private fun awaitUnlockScreen() {
        awaitObject(By.text(str("unlock_pin_title")), LOAD_TIMEOUT, "unlock-screen")
        assertThat(device.hasObject(By.res(SETUP_PASSCODE_OPTIONS_BUTTON))).isFalse()
    }

    private fun awaitUnlocked() =
        awaitObject(By.res(MENU_NAV_ITEM_TAG), LOAD_TIMEOUT, "main-navigation")

    private fun currentPasscodeType() = runBlocking { monitorPasscodeTypeUseCase().first() }

    private fun passcodeIsEnabled() =
        runBlocking { monitorPasscodeLockPreferenceUseCase().first() }

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
        const val BACK_STEP_TIMEOUT = 2_000L
        const val MAX_SCROLL_SWIPES = 12

        /** Cancel button of the Google Play services password-autofill overlay. */
        const val AUTOFILL_DISMISS_BUTTON = "android:id/autofill_dialog_no"

        /** Main navigation item test tag for the Menu tab (`MenuHomeScreen` destination). */
        const val MENU_NAV_ITEM_TAG = "main_navigation:navigation_item_MenuHomeScreen"

        /** Mirror MenuHomeScreenTestTags — the Menu's async account list. */
        const val MENU_ACCOUNT_ITEM_TAG = "menu_home_screen:account_item"

        /** Passcode settings tile tags (Compose, `testTagsAsResourceId`). */
        const val ENABLE_PASSCODE_TILE = "enable_passcode_tile:generic_two_line_list_item"
        const val CHANGE_PASSCODE_TILE = "change_passcode_tile:generic_two_line_list_item"
        const val REQUIRE_PASSCODE_TILE = "require_passcode_tile:generic_two_line_list_item"

        /** Mirror the internal SETUP_PASSCODE_* tags of the core-ui set-passcode screen. */
        const val SETUP_PASSCODE_FIELD = "setup_passcode_screen:passcode_field"
        const val SETUP_PASSCODE_PASSWORD_FIELD = "setup_passcode_screen:password_field"
        const val SETUP_PASSCODE_MISMATCH_ERROR = "setup_passcode_screen:text_mismatch_error"
        const val SETUP_PASSCODE_OPTIONS_BUTTON = "setup_passcode_screen:button_passcode_options"

        const val ALPHANUMERIC_PASSCODE = "mega1234"
    }
}
