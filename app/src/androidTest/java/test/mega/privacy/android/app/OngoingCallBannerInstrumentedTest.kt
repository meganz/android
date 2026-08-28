package mega.privacy.android.app

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaChatCall
import mega.privacy.android.data.test.stub.StubMegaHandleList
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.feature.chat.components.RETURN_TO_CALL_BANNER_TAG
import nz.mega.sdk.MegaChatCall
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented test for AND-24635 — the global return-to-call banner (AND-24598 / W1) in
 * the Navigation3 app shell.
 *
 * Runs the whole app against the `:data-test` fake SDK: an in-progress chat call is stubbed on
 * [FakeMegaChatApiGateway] so the production `MonitorOngoingCallUseCase` path emits a live call and
 * the banner renders. The UI is driven with UiAutomator; Compose test tags are matched as resource
 * ids because MegaActivity sets `testTagsAsResourceId = true`.
 */
@HiltAndroidTest
class OngoingCallBannerInstrumentedTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeChatApi: FakeMegaChatApiGateway

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

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

        TestAppBoot.runCoreInitializers()

        // Persist a logged-in session through the app's real credentials path so MegaNavDisplay
        // renders the main graph (and thus the shell overlay) rather than the login screen.
        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
    }

    @After
    fun tearDown() {
        fakeChatApi.resetToDefaults()
    }

    @Test
    fun ongoingCallShowsReturnToCallBanner() {
        stubOngoingCall()

        ActivityScenario.launch(MegaActivity::class.java)

        awaitObject(By.res(RETURN_TO_CALL_BANNER_TAG), LAUNCH_TIMEOUT, "return-to-call-banner")
    }

    @Test
    fun noOngoingCallHidesReturnToCallBanner() {
        // No call is stubbed, so the fake gateway's defaults report no call in progress and
        // MonitorOngoingCallUseCase emits null — the banner must never appear.
        ActivityScenario.launch(MegaActivity::class.java)

        // The shell has rendered once the main navigation bar is present; with no ongoing call the
        // banner must be absent.
        awaitObject(By.res(MAIN_NAVIGATION_BAR_TAG), LAUNCH_TIMEOUT, "main-navigation-bar")
        assertThat(device.hasObject(By.res(RETURN_TO_CALL_BANNER_TAG))).isFalse()
    }

    /**
     * Stubs an in-progress, not-on-hold call so `GetChatCallInProgress` (which scans Initial →
     * Connecting → Joining → InProgress) resolves it and `MonitorOngoingCallUseCase` emits a
     * non-null `ChatCall` on collection.
     */
    private fun stubOngoingCall() {
        fakeChatApi.stub(
            MegaChatApiGateway::getChatCalls,
            matcher = { it.firstOrNull() == MegaChatCall.CALL_STATUS_IN_PROGRESS },
        ) { StubMegaHandleList(listOf(CHAT_ID)) }

        fakeChatApi.stub(
            MegaChatApiGateway::getChatCall,
            matcher = { it.firstOrNull() == CHAT_ID },
        ) {
            StubMegaChatCall(
                callId = CALL_ID,
                chatId = CHAT_ID,
                status = MegaChatCall.CALL_STATUS_IN_PROGRESS,
                isOnHold = false,
            )
        }
    }

    /**
     * Waits for [selector]; on timeout, dumps the window hierarchy to logcat (tag `UiDump`,
     * chunked so it survives the post-test uninstall) before failing.
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
        const val CHAT_ID = 123L
        const val CALL_ID = 456L

        /** Mirrors MAIN_NAVIGATION_BAR_TEST_TAG in :core:navigation-snowflake-components. */
        const val MAIN_NAVIGATION_BAR_TAG = "main_navigation:navigation_bar"
    }
}
