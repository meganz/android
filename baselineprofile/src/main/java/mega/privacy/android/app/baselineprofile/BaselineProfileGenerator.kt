package mega.privacy.android.app.baselineprofile

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * This test class generates a basic startup baseline profile for the target package.
 *
 * We recommend you start with this but add important user flows to the profile to improve their performance.
 * Refer to the [baseline profile documentation](https://d.android.com/topic/performance/baselineprofiles)
 * for more information.
 *
 * You can run the generator with the Generate Baseline Profile run configuration,
 * or directly with `generateBaselineProfile` Gradle task:
 * ```
 * ./gradlew --no-daemon :app:generateReleaseBaselineProfile -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
 * ```
 * The run configuration runs the Gradle task and applies filtering to run only the generators.
 *
 * Check [documentation](https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args)
 * for more information about available instrumentation arguments.
 *
 * After you run the generator, you can verify the improvements running the [StartupBenchmarks] benchmark.
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    /**
     * Generate baseline profile for the target package.
     * For mega app make sure you choose gmsNonMinifiedRelease variant
     * For TEST_ACCOUNT_USER_NAME and TEST_ACCOUNT_PASSWORD, you can use your own test account and update in the local.properties file
     */
    @Test
    fun generate() {
        rule.collect(TARGET_PACKAGE) {
            // This block defines the app's critical user journey: cold start, log in and land on
            // the home screens, which is what the vast majority of sessions execute.
            pressHome()
            startActivityAndWait()

            if (awaitTourScreen()) {
                logIn()
            }
            waitForHomeScreens()
        }
    }

    /**
     * Waits for the cold start to settle and reports whether it landed on the tour screen.
     *
     * The collector replays this block several times and only kills the process in between, so
     * the session survives and only the first pass starts logged out; every later pass restores
     * it and goes straight to fetching the content.
     */
    private fun MacrobenchmarkScope.awaitTourScreen(): Boolean {
        val deadline = SystemClock.uptimeMillis() + APP_START_TIMEOUT_MS
        while (SystemClock.uptimeMillis() < deadline) {
            if (device.hasAnyObject(TOUR_LOG_IN_BUTTON)) return true
            if (device.hasAnyObject(
                    FETCH_NODES_PROGRESS,
                    UPGRADE_MAYBE_LATER_BUTTON,
                    PERMISSIONS_SCREEN_PORTRAIT,
                    PERMISSIONS_SCREEN_LANDSCAPE,
                    HOME_MAIN_APP_BAR,
                    MAIN_NAVIGATION_BAR,
                    MAIN_NAVIGATION_RAIL,
                )
            ) return false
            device.waitForWindowUpdate(TARGET_PACKAGE, POLL_TIMEOUT_MS)
        }
        error(
            "Timed out after ${APP_START_TIMEOUT_MS}ms before the app left the splash screen. " +
                    "Visible test tags: ${device.visibleResourceNames()}"
        )
    }

    private fun MacrobenchmarkScope.logIn() {
        require(BuildConfig.TEST_ACCOUNT_USER_NAME.isNotEmpty()) { "Please put test_account_username in local.properties" }
        require(BuildConfig.TEST_ACCOUNT_PASSWORD.isNotEmpty()) { "Please put test_account_password in local.properties" }

        device.awaitObject(TOUR_LOG_IN_BUTTON).click()

        device.fillInputField(LOGIN_EMAIL_INPUT, BuildConfig.TEST_ACCOUNT_USER_NAME)
        device.fillInputField(
            resourceName = LOGIN_PASSWORD_INPUT,
            value = BuildConfig.TEST_ACCOUNT_PASSWORD,
            isMasked = true,
        )
        device.dismissAutofillDialog()
        device.awaitObject(LOGIN_BUTTON).click()
    }

    /**
     * Types [value] into the core UI input field tagged [resourceName].
     *
     * The tag belongs to the field's wrapper, so the editable node has to be looked up beneath it;
     * `UiObject2.setText` only logs when it is handed a node that cannot take text, which would
     * otherwise leave the credentials empty and the journey stuck on the login screen.
     */
    private fun UiDevice.fillInputField(
        resourceName: String,
        value: String,
        isMasked: Boolean = false,
    ) {
        require(value.none { it.isWhitespace() }) { "Credentials must not contain whitespace." }

        dismissAutofillDialog()
        editableField(resourceName).click()
        // Focusing the field is what summons the picker, so it has to go before typing too.
        dismissAutofillDialog()

        // Typing through the input service reaches the focused field even when the accessibility
        // set-text action is refused, which happens while a system window owns the hierarchy.
        typeIntoFocusedField(value)
        waitForIdle()

        val typed = editableField(resourceName).text
        check(!typed.isNullOrEmpty()) { "Typing into '$resourceName' had no effect." }
        // A masked field reports its bullets rather than its value, so it can only be checked
        // for emptiness.
        check(isMasked || typed == value) {
            "'$resourceName' holds ${typed.length} characters after typing ${value.length}."
        }
    }

    private fun typeIntoFocusedField(value: String) {
        val output = InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .executeShellCommand("input text $value")
        // Draining the descriptor waits for the injection to finish. Returning early leaves key
        // events in flight, which then land on top of whatever the field is set to next.
        ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
    }

    private fun UiDevice.editableField(resourceName: String) =
        awaitObject(resourceName).findObject(By.res(TEXT_FIELD_EDITABLE))
            ?: error("'$resourceName' has no editable field to type into.")

    /**
     * Closes the platform autofill picker, which the login fields' autofill content types summon
     * on focus. It becomes the active window, so while it is up the app's own nodes cannot be
     * found at all, let alone tapped.
     */
    private fun UiDevice.dismissAutofillDialog() {
        val dismiss = wait(
            Until.findObject(By.res(AUTOFILL_DIALOG_DISMISS)),
            AUTOFILL_DIALOG_TIMEOUT_MS,
        ) ?: return
        dismiss.click()
        waitForIdle()
    }

    /**
     * Waits until the home screens shell is on screen, clearing anything the post login
     * initialisers put in front of it on the way.
     */
    private fun MacrobenchmarkScope.waitForHomeScreens() {
        val deadline = SystemClock.uptimeMillis() + HOME_TIMEOUT_MS
        while (SystemClock.uptimeMillis() < deadline) {
            // The navigation bar is hidden while a promotional sheet is up, so the landing
            // screen's own app bar is what confirms the journey arrived.
            if (device.hasAnyObject(HOME_MAIN_APP_BAR, MAIN_NAVIGATION_BAR, MAIN_NAVIGATION_RAIL)) {
                device.waitForIdle()
                return
            }
            if (device.currentPackageName != TARGET_PACKAGE) {
                // Something took the app out of the foreground; bring it back rather than
                // tapping at whatever now owns the screen.
                startActivityAndWait()
                continue
            }
            dismissPostLoginInterstitial()
        }
        error(
            "Timed out after ${HOME_TIMEOUT_MS}ms before reaching the home screens. " +
                    "Foreground package: ${device.currentPackageName}. " +
                    "Visible test tags: ${device.visibleResourceNames()}"
        )
    }

    /**
     * Clears one thing standing between the cold start and the landing screen.
     *
     * Post login initialisers queue an upsell, the onboarding permission pages and the What's New
     * sheet at a higher priority than the landing screen, so the journey stalls on whichever of
     * them this session happens to get. All three have to be dismissed through their own button:
     * the sheet opts out of back and outside-click dismissal, and popping the permission entry
     * with back leaves it on screen.
     */
    private fun MacrobenchmarkScope.dismissPostLoginInterstitial() {
        val dismissable = device.findObject(By.res(UPGRADE_MAYBE_LATER_BUTTON))
            ?: device.findLabelled(WHATS_NEW_DISMISS_BUTTON)
            ?: device.findLabelled(PERMISSION_SKIP_BUTTON)
        if (dismissable != null) {
            dismissable.click()
            device.waitForIdle()
            return
        }
        // Nothing to act on yet: the app is most likely still fetching nodes.
        device.waitForWindowUpdate(TARGET_PACKAGE, POLL_TIMEOUT_MS)
    }

    private fun UiDevice.findLabelled(label: String) =
        findObject(By.pkg(TARGET_PACKAGE).clickable(true).text(label))
            ?: findObject(By.pkg(TARGET_PACKAGE).text(label))

    private fun UiDevice.awaitObject(
        resourceName: String,
        timeoutMs: Long = SCREEN_TIMEOUT_MS,
    ): UiObject2 = wait(Until.findObject(By.res(resourceName)), timeoutMs)
        ?: error(
            "Timed out after ${timeoutMs}ms waiting for '$resourceName'. " +
                    "Visible test tags: ${visibleResourceNames()}"
        )

    private fun UiDevice.hasAnyObject(vararg resourceNames: String) =
        resourceNames.any { hasObject(By.res(it)) }

    private fun UiDevice.visibleResourceNames() =
        findObjects(By.res(ANY_RESOURCE_NAME))
            .mapNotNull { it.resourceName }
            .distinct()
            .take(MAX_REPORTED_RESOURCE_NAMES)
            .joinToString()

    private companion object {
        const val TARGET_PACKAGE = "mega.privacy.android.app"

        const val TOUR_LOG_IN_BUTTON = "usp_carousel_screen:log_in_button"
        const val LOGIN_EMAIL_INPUT = "login_screen:email_input"
        const val LOGIN_PASSWORD_INPUT = "login_screen:password_input"
        const val LOGIN_BUTTON = "login_screen:login_button"

        // The editable node core UI nests inside every text input field wrapper.
        const val TEXT_FIELD_EDITABLE = "base_text_field:outlined_text_field"

        // Framework id of the autofill picker's "No thanks" button.
        const val AUTOFILL_DIALOG_DISMISS = "android:id/autofill_dialog_no"

        const val UPGRADE_MAYBE_LATER_BUTTON =
            "choose_account_screen_top_bar:maybe_later_button"
        const val PERMISSIONS_SCREEN_PORTRAIT = "new_permission_screen_portrait"
        const val PERMISSIONS_SCREEN_LANDSCAPE = "new_permission_screen_landscape"
        const val FETCH_NODES_PROGRESS = "FETCH_NODES_PROGRESS"
        const val MAIN_NAVIGATION_BAR = "main_navigation:navigation_bar"
        const val MAIN_NAVIGATION_RAIL = "main_navigation:navigation_rail"
        const val HOME_MAIN_APP_BAR = "home_screen:main_app_bar"

        // Neither the What's New sheet nor the permission pages tag their buttons, so they can
        // only be reached by label. Both labels are English-only here: the sheet's is hardcoded
        // in V16_1_WhatsNewDetail, and the device running this is expected to be in English.
        const val WHATS_NEW_DISMISS_BUTTON = "Dismiss"
        const val PERMISSION_SKIP_BUTTON = "Skip for now"

        const val SCREEN_TIMEOUT_MS = 10_000L
        const val POLL_TIMEOUT_MS = 1_000L
        const val AUTOFILL_DIALOG_TIMEOUT_MS = 2_000L

        // A cold start boots the SDK and opens the database before anything is drawn.
        const val APP_START_TIMEOUT_MS = 60_000L

        // Fetching the nodes of a real account dominates this budget.
        const val HOME_TIMEOUT_MS = 120_000L

        const val MAX_REPORTED_RESOURCE_NAMES = 30
        val ANY_RESOURCE_NAME: Pattern = Pattern.compile(".+")
    }
}
