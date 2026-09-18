package mega.privacy.android.app

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaChatListItem
import mega.privacy.android.data.test.stub.StubMegaChatRoom
import mega.privacy.android.domain.entity.passcode.PasscodeTimeout
import mega.privacy.android.domain.entity.passcode.PasscodeType
import mega.privacy.android.domain.entity.passcode.SetPasscodeRequest
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.passcode.MonitorPasscodeLockStateUseCase
import mega.privacy.android.domain.usecase.passcode.SetPasscodeTimeoutUseCase
import mega.privacy.android.domain.usecase.passcode.SetPasscodeUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import nz.mega.sdk.MegaChatRoom
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented regression test for AND-23510 — "incorrect landing page when using the
 * chat shortcut when passcode is turned on".
 *
 * The launcher chat shortcut starts [MegaActivity] with `ACTION_SHORTCUT_CHAT`, which the intent
 * handler turns into a queued `ChatListNavKey`. With a passcode set to an immediate timeout, that
 * destination is queued while the app is locked, so the journey covers the whole path: shortcut
 * intent -> passcode lock -> unlock -> landing destination. The default navigation lands on Home,
 * so ending up on the chat list can only come from the shortcut's destination surviving the lock.
 *
 * [passcodeGatesAPlainLaunchAndUnlocksToTheDefaultLandingScreen] is the baseline for the same
 * seeded state without the shortcut, so a failure of the shortcut journey is attributable to the
 * shortcut and not to the passcode seeding or the unlock step.
 *
 * Built on the `:data-test` fake-SDK framework following [ChatListArchivedNavigationTest]: the
 * whole app runs as in production and only the SDK gateways are faked. The passcode is configured
 * through the app's own domain use cases and locked through [PasscodeRepository] — the same state
 * the production lifecycle observer writes when the app is resumed with an immediate timeout — and
 * it is unlocked by entering the PIN in the real passcode screen. The UI is driven with
 * UiAutomator; Compose test tags are matched as resource ids.
 */
@HiltAndroidTest
class ChatShortcutPasscodeLandingTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

    @Inject
    lateinit var fakeMegaChat: FakeMegaChatApiGateway

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    @Inject
    lateinit var setCustomiseNavigationTooltipShownUseCase: SetCustomiseNavigationTooltipShownUseCase

    @Inject
    lateinit var setPasscodeUseCase: SetPasscodeUseCase

    @Inject
    lateinit var setPasscodeTimeoutUseCase: SetPasscodeTimeoutUseCase

    @Inject
    lateinit var passcodeRepository: PasscodeRepository

    @Inject
    lateinit var monitorPasscodeLockStateUseCase: MonitorPasscodeLockStateUseCase

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

        fakeFlags.set(ApiFeatures.CustomisableBottomNavigation, true)

        fakeMegaChat.resetToDefaults()
        seedRoom(CHAT_ID, CHAT_TITLE)
        fakeMegaChat.clearInvocations()

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            setCustomiseNavigationTooltipShownUseCase()

            setPasscodeUseCase(SetPasscodeRequest(PasscodeType.Pin(PASSCODE.length), PASSCODE))
            setPasscodeTimeoutUseCase(PasscodeTimeout.Immediate)
            passcodeRepository.setPasscodeEnabled(true)
            passcodeRepository.setFailedAttempts(0)
            passcodeRepository.setLocked(true)
        }
    }

    /**
     * The launched scenarios are deliberately neither held nor closed. The shortcut intent's action
     * is consumed — set to null — once it has been handled, so `ActivityScenario` stops matching the
     * activity's lifecycle callbacks against the launch intent and `close()` would block until its
     * own timeout before failing, even though the activity is destroyed. Per-test process isolation
     * tears the activity down instead.
     */
    @After
    fun tearDown() {
        runBlocking {
            passcodeRepository.setLocked(false)
            passcodeRepository.setPasscodeEnabled(false)
            passcodeRepository.setPasscode(null)
            passcodeRepository.setPasscodeType(null)
            passcodeRepository.setPasscodeTimeOut(null)
            passcodeRepository.setFailedAttempts(0)
        }
        fakeFlags.clear()
        fakeMegaChat.resetToDefaults()
    }

    @Test
    fun chatShortcutLandsOnChatListAfterUnlockingThePasscode() {
        val intent = Intent(targetContext, MegaActivity::class.java).apply {
            action = Constants.ACTION_SHORTCUT_CHAT
        }
        ActivityScenario.launch<MegaActivity>(intent)

        awaitPasscodeScreen()
        assertThat(device.hasObject(By.res(CHAT_LIST_SCREEN_TAG))).isFalse()

        enterPasscode()

        awaitObject(By.res(CHAT_LIST_SCREEN_TAG), LOAD_TIMEOUT, "chat-list-landing")
        awaitObject(chatRow(CHAT_TITLE), LOAD_TIMEOUT, "chat-row")
    }

    @Test
    fun passcodeGatesAPlainLaunchAndUnlocksToTheDefaultLandingScreen() {
        ActivityScenario.launch(MegaActivity::class.java)

        awaitPasscodeScreen()

        enterPasscode()

        awaitObject(By.res(HOME_APP_BAR_TAG), LOAD_TIMEOUT, "home-landing")
    }

    /**
     * Waits for the passcode screen and reports the live lock state when it never arrives, so a
     * failure separates "the app was never locked" from "something was shown over the lock".
     */
    private fun awaitPasscodeScreen() {
        if (device.wait(Until.hasObject(By.res(PASSCODE_FIELD_TAG)), LAUNCH_TIMEOUT)) return
        throw AssertionError(
            "Timed out after ${LAUNCH_TIMEOUT}ms waiting for the passcode screen; the passcode " +
                    "lock state is ${lockState()}${describe("passcode-screen")}",
        )
    }

    private fun enterPasscode() {
        awaitObject(By.res(PASSCODE_FIELD_TAG), LOAD_TIMEOUT, "passcode-field").text = PASSCODE
        awaitGone(By.res(PASSCODE_FIELD_TAG), UNLOCK_TIMEOUT, "passcode-screen")
    }

    private fun lockState(): Boolean =
        runBlocking { monitorPasscodeLockStateUseCase().first() }

    private fun seedRoom(chatId: Long, title: String) {
        fakeMegaChat.chatState.chatRooms[chatId] = StubMegaChatRoom(
            chatId = chatId,
            title = title,
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
            peers = listOf(PEER_HANDLE to MegaChatRoom.PRIV_STANDARD),
            isGroup = true,
            isActive = true,
        )
        fakeMegaChat.chatState.chatListItems[chatId] = StubMegaChatListItem(
            chatId = chatId,
            title = title,
            lastMessage = "",
            lastTimestamp = BASE_TIMESTAMP,
            isGroup = true,
            isActive = true,
        )
    }

    private fun chatRow(title: String): BySelector =
        By.res(CHAT_ROOM_ITEM_TAG).hasDescendant(By.text(title))

    private fun awaitObject(selector: BySelector, timeout: Long, name: String): UiObject2 {
        if (!device.wait(Until.hasObject(selector), timeout)) {
            throw AssertionError(
                "Timed out after ${timeout}ms waiting for $name ($selector)${describe(name)}",
            )
        }
        return device.findObject(selector)
    }

    private fun awaitGone(selector: BySelector, timeout: Long, name: String) {
        if (device.wait(Until.gone(selector), timeout)) return
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector) to " +
                    "disappear${describe(name)}",
        )
    }

    /**
     * Logs the full window hierarchy and returns a compact summary of it — one line per node that
     * carries a resource id or text — so a failure names the screen that was reached instead of
     * only the selector that was missing.
     */
    private fun describe(name: String): String {
        val stream = ByteArrayOutputStream()
        device.dumpWindowHierarchy(stream)
        val xml = stream.toString("UTF-8")
        xml.chunked(3000).forEachIndexed { index, chunk ->
            Timber.d("[$name#$index] $chunk")
        }
        val summary = NODE_PATTERN.findAll(xml)
            .mapNotNull { match ->
                val id = match.value.attribute("resource-id")
                val text = match.value.attribute("text")
                if (id.isEmpty() && text.isEmpty()) null else "$id|$text"
            }
            .distinct()
            .joinToString(separator = "\n")
        return "; screen was:\n$summary"
    }

    private fun String.attribute(name: String) =
        Regex("$name=\"([^\"]*)\"").find(this)?.groupValues?.get(1).orEmpty()

    private companion object {
        const val LAUNCH_TIMEOUT = 60_000L
        const val UNLOCK_TIMEOUT = 30_000L
        const val LOAD_TIMEOUT = 30_000L

        const val CHAT_ID = 8_101L
        const val PEER_HANDLE = 9_101L
        const val BASE_TIMESTAMP = 1_700_000_000L

        const val CHAT_TITLE = "Design Team"
        const val PASSCODE = "1234"

        private val NODE_PATTERN = Regex("<node[^>]*>")

        /** Mirrors the PASSCODE_FIELD_TAG of the passcode unlock screen. */
        const val PASSCODE_FIELD_TAG = "passcode_dialog:passcode_field"

        /** Mirror the ChatListScreen and ChatRoomItem test tags of the Compose chat list. */
        const val CHAT_LIST_SCREEN_TAG = "chat_list_screen:screen"
        const val CHAT_ROOM_ITEM_TAG = "chat_room_item:row"

        /** Mirrors HOME_MAIN_APP_BAR_TAG of the redesigned HomeScreen. */
        const val HOME_APP_BAR_TAG = "home_screen:main_app_bar"
    }
}
