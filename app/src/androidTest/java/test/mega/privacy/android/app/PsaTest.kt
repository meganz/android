package mega.privacy.android.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.appstate.global.initialisation.postlogin.PsaInitialiser
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaRequest
import mega.privacy.android.domain.entity.featureflag.MiscLoadedState
import mega.privacy.android.domain.usecase.SetHomeConfigurationTooltipShownUseCase
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.setting.BroadcastMiscStateUseCase
import nz.mega.sdk.MegaRequest
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented port of the AAT feature `inn.psa` — the PSA (public service announcement)
 * scenarios. Built on the `:data-test` fake-SDK framework and following [HomeScreenTest] /
 * [SettingsOptions1Test]: the whole app runs as in production with only the SDK gateways faked and
 * the UI is driven with UiAutomator.
 *
 * PSAs are gateway-fed end to end, so the whole chain runs unmodified: the announcement the fake
 * replies with to `MegaApiGateway.getPsa` is mapped, rendered, and dismissed back through
 * `MegaApiGateway.setPsaHandled`.
 *
 * What makes the announcement appear is worth stating, because it is not the Home screen opening:
 *
 * - The PSA is fetched by [PsaInitialiser], a post-login initialiser. Post-login units run once the
 *   app has fetched nodes after a login; a test that boots straight into a logged-in session never
 *   reaches that phase, so the test runs that one production unit itself. Its siblings are left out
 *   deliberately — several of them (What's New, subscription offer, onboarding) emit navigation
 *   events of their own that would overlay the PSA.
 * - The fetch is gated behind the SDK's `EVENT_MISC_FLAGS_READY`, which the app turns into
 *   [MiscLoadedState.FlagsReady]; only then does `MonitorPsaUseCase` refresh. The fake cannot raise
 *   SDK events, so the test broadcasts the same state the event handler would.
 *
 * With both in place the announcement shows on launch, so the AAT home-button-and-return dance
 * (which is how the Appium tests coax a PSA out of a real backend) is not reproduced.
 *
 * The announcement renders as a modal bottom sheet, which is a separate window where Compose test
 * tags are not exposed as resource ids, so its content is matched by visible text.
 *
 * Ported: inn.psa01 (a standard PSA shows), inn.psa02 (Dismiss removes it for good),
 * inn.psa03 (the positive button opens the announcement's link).
 */
@HiltAndroidTest
class PsaTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    @Inject
    lateinit var setHomeConfigurationTooltipShownUseCase: SetHomeConfigurationTooltipShownUseCase

    @Inject
    lateinit var broadcastMiscStateUseCase: BroadcastMiscStateUseCase

    @Inject
    lateinit var psaInitialiser: PsaInitialiser

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private val rootHandle get() = fakeMegaApi.nodeTree.rootNode.handle

    private val postLoginScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var scenario: ActivityScenario<MegaActivity>? = null

    @Before
    fun setUp() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        hiltRule.inject()

        // A physical device that has gone to sleep stops the launched activity straight away, so
        // nothing is ever drawn to assert against.
        device.wakeUp()
        runCatching { device.executeShellCommand("wm dismiss-keyguard") }

        instrumentation.uiAutomation.grantRuntimePermission(
            targetContext.packageName,
            android.Manifest.permission.POST_NOTIFICATIONS,
        )

        // Boot the test process through the production initialiser units. See CloudDriveUploadTest.
        TestAppBoot.runCoreInitializers()

        // Cloud Drive children may be resolved on start; answer through the fake node tree.
        fakeMegaApi.stub(MegaApiGateway::getChildren) {
            fakeMegaApi.nodeTree.childrenOf(rootHandle)
        }

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)

            // The one-time "Customise Home" onboarding tooltip is a popup that covers part of the
            // Home screen, so on a fresh install it can swallow taps meant for what it covers.
            setHomeConfigurationTooltipShownUseCase()
        }
    }

    @After
    fun tearDown() {
        postLoginScope.cancel()
        scenario?.close()
    }

    // region tests

    /** inn.psa01 — a standard PSA is shown once the app has launched. */
    @Test
    fun standardPsaIsShown() {
        showStandardPsa()

        awaitObject(By.text(PSA_TITLE), LOAD_TIMEOUT, "psa-title")
        awaitObject(By.text(PSA_TEXT), LOAD_TIMEOUT, "psa-body")
        awaitObject(By.text(PSA_POSITIVE_TEXT), LOAD_TIMEOUT, "psa-positive-button")
        awaitObject(By.text(dismissText()), LOAD_TIMEOUT, "psa-dismiss-button")
    }

    /**
     * inn.psa02 — Dismiss tells the SDK the announcement was handled, closes it, and it does not
     * come back.
     */
    @Test
    fun dismissRemovesPsaAndMarksItHandled() {
        showStandardPsa()
        awaitObject(By.text(PSA_TITLE), LOAD_TIMEOUT, "psa-title")

        clickText(dismissText(), "psa-dismiss-button")

        awaitInvocations("setPsaHandled($PSA_ID)") {
            fakeMegaApi.invocationsOf(MegaApiGateway::setPsaHandled)
                .filter { it.arguments.firstOrNull() == PSA_ID }
        }
        awaitGone(By.text(PSA_TITLE), "psa-dismissed")
        awaitStaysGone(By.text(PSA_TITLE), "psa-does-not-return")
    }

    /**
     * inn.psa03 — the positive button opens the announcement's link. The redesigned app hands the
     * link to the platform (a custom tab, falling back to the browser) rather than rendering a
     * detail screen of its own, so the outgoing intent is what the scenario asserts; tapping it
     * also marks the announcement as handled.
     */
    @Test
    fun positiveButtonOpensPsaLink() {
        Intents.init()
        try {
            intending(hasAction(Intent.ACTION_VIEW)).respondWith(
                Instrumentation.ActivityResult(Activity.RESULT_OK, null),
            )

            showStandardPsa()
            awaitObject(By.text(PSA_TITLE), LOAD_TIMEOUT, "psa-title")

            clickText(PSA_POSITIVE_TEXT, "psa-positive-button")

            awaitIntended(
                "psa-link",
                allOf(hasAction(Intent.ACTION_VIEW), hasData(PSA_LINK)),
            )
            awaitInvocations("setPsaHandled($PSA_ID)") {
                fakeMegaApi.invocationsOf(MegaApiGateway::setPsaHandled)
                    .filter { it.arguments.firstOrNull() == PSA_ID }
            }
        } finally {
            Intents.release()
        }
    }

    // endregion

    // region psa helpers

    /**
     * Seeds a standard announcement, launches the app, and drives the two things the fetch waits
     * for: the post-login unit that monitors announcements, and the misc-flags-ready state the
     * monitor gates its first refresh on.
     */
    private fun showStandardPsa() {
        fakeMegaApi.stubRequest(
            MegaApiGateway::getPsa,
            request = StubMegaRequest(
                type = MegaRequest.TYPE_GET_PSA,
                number = PSA_ID.toLong(),
                name = PSA_TITLE,
                text = PSA_TEXT,
                password = PSA_POSITIVE_TEXT,
                link = PSA_LINK,
            ),
        )

        launchHome()

        postLoginScope.launch { psaInitialiser(SESSION, true) }
        runBlocking { broadcastMiscStateUseCase(MiscLoadedState.FlagsReady) }
    }

    private fun dismissText() = str("general_dismiss_dialog")

    // endregion

    // region navigation & UiAutomator helpers

    /** Launches the logged-in app; the redesigned Home is the default start screen. */
    private fun launchHome() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)
        awaitObject(By.res(HOME_APP_BAR_TAG), LAUNCH_TIMEOUT, "home-app-bar")
    }

    private fun str(name: String): String {
        val id = targetContext.resources.getIdentifier(name, "string", targetContext.packageName)
        require(id != 0) { "string resource not found: $name" }
        return targetContext.getString(id)
    }

    private fun clickText(text: String, name: String) {
        val selector = By.text(text)
        awaitObject(selector, LOAD_TIMEOUT, name)
        device.findObject(selector).click()
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

    /** Fails if [selector] reappears within [REAPPEARANCE_WINDOW_MS]. */
    private fun awaitStaysGone(selector: BySelector, name: String) {
        if (!device.wait(Until.hasObject(selector), REAPPEARANCE_WINDOW_MS)) return
        dumpHierarchy(name)
        throw AssertionError("$name ($selector) came back; hierarchy in logcat tag UiDump")
    }

    /**
     * Gateway commands are dispatched from a `viewModelScope.launch`, so their invocations are not
     * recorded synchronously after the UI action that triggers them. Poll [supplier] until at least
     * one invocation matches, or fail with what was actually recorded.
     */
    private fun <T> awaitInvocations(name: String, supplier: () -> List<T>): List<T> {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            val matches = supplier()
            if (matches.isNotEmpty()) return matches
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for an invocation of $name; " +
                    "recorded api=${fakeMegaApi.invocations.map { it.methodName }}",
        )
    }

    /** Polls Espresso-Intents until every [matchers] entry has been intended, like [awaitObject]. */
    private fun awaitIntended(name: String, vararg matchers: org.hamcrest.Matcher<Intent>) {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        var lastError: AssertionError? = null
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                matchers.forEach { Intents.intended(it) }
                return
            } catch (e: AssertionError) {
                lastError = e
                Thread.sleep(POLL_INTERVAL_MS)
            }
        }
        dumpHierarchy(name)
        throw AssertionError("Expected intent for $name not observed", lastError)
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
        const val REAPPEARANCE_WINDOW_MS = 5_000L
        const val POLL_INTERVAL_MS = 100L

        const val SESSION = "psa-test-session"

        const val PSA_ID = 8001
        const val PSA_TITLE = "Scheduled maintenance"
        const val PSA_TEXT = "MEGA will be unavailable for a short period this weekend."
        const val PSA_POSITIVE_TEXT = "Learn more"

        /**
         * A non-MEGA host on purpose: MEGA links are treated as deeplinks and rewritten with a
         * `noplans` parameter, neither of which this scenario is about.
         */
        const val PSA_LINK = "https://psa.example.com/maintenance"

        /** Mirrors HOME_MAIN_APP_BAR_TAG of the redesigned HomeScreen. */
        const val HOME_APP_BAR_TAG = "home_screen:main_app_bar"
    }
}
