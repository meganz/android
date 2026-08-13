package mega.privacy.android.app

import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.presentation.meeting.chat.ChatActivity
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.state.FakeChatState
import mega.privacy.android.data.test.stub.StubMegaChatRoom
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaChatRoom
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented port of the AAT feature `sm14 ChatRoom` — the one-to-one chat-room
 * scenarios that assert the chat-room layout / options, sending messages, and the clear / archive /
 * mute actions. It is built on the `:data-test` fake-SDK framework and follows [CloudDriveUploadTest]
 * and [TransferManagerTest]: the whole app runs as in production with only the SDK gateways faked,
 * the UI is driven with UiAutomator (not a Compose rule), and Compose test tags are matched as
 * resource ids because the chat screen sets `testTagsAsResourceId = true`.
 *
 * How the chat room is opened: rather than navigating via the (flag-gated, legacy) Contacts list, a
 * one-to-one chat room is seeded directly into the fake chat state ([FakeChatState.chatRooms]) and
 * [ChatActivity] is launched with the seeded chat id ([ChatNavKey.LEGACY_CHAT_ID] extra). The real
 * `ChatViewModel` loads the room through the gateway (`getChatRoom` → repository → fake chat state),
 * so the app bar, options menu and input bar render exactly as in production.
 *
 * What is asserted per scenario is the user-visible outcome where the fake reproduces it faithfully
 * (the app-bar title and call icons, the options-menu items, the clear-history confirmation dialog,
 * the mute dialog) and, for the fire-and-forget chat gateway commands that the fake does not
 * simulate a follow-up event for (clear history, archive, send message), that the app invoked the
 * matching gateway method. Own-message send is asserted through the gateway call rather than real
 * delivery.
 *
 * Two gotchas learned from earlier ports are applied: modal / popup surfaces (the options overflow
 * dropdown, the confirmation and mute dialogs) are driven by `By.text` where they do not carry
 * `testTagsAsResourceId`, and gateway commands dispatched from `viewModelScope.launch` are polled
 * with [awaitInvocations] rather than asserted synchronously.
 *
 * Scenarios that require rendered message rows from the message-list paging pipeline (tc02 forward,
 * tc03 copy & re-send, tc06 select & forward multiple, tc10 react with emoji) or the out-of-process
 * / external chat-picker activity are intentionally left in AAT — they cannot be reproduced cleanly
 * against the current gateway-level fakes.
 */
@HiltAndroidTest
class ChatRoomTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaChat: FakeMegaChatApiGateway

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private var scenario: ActivityScenario<ChatActivity>? = null

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

        // The fake chat gateway is a shared singleton across tests in the process; start each test
        // from a pristine fake, then seed the one-to-one chat room this suite opens.
        fakeMegaChat.resetToDefaults()
        fakeMegaChat.chatState.chatRooms[CHAT_ID] = oneToOneChatRoom()
        fakeMegaChat.clearInvocations()

        // Persist a logged-in session through the app's real credentials path; account details are
        // fetched eagerly because account-detail dependent features wait for a non-empty value.
        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        fakeMegaChat.resetToDefaults()
    }

    // region tests

    /**
     * tc01 — the one-to-one chat room shows its title, the audio/video call icons, and the
     * three-dot options menu offers Info / Clear / Archive / Mute.
     */
    @Test
    fun oneToOneChatRoomShowsLayoutAndOptions() {
        openChatRoom()

        awaitObject(By.text(CONTACT_NAME), LOAD_TIMEOUT, "chat-title")
        awaitObject(By.res(AUDIO_CALL_ACTION_TAG), LOAD_TIMEOUT, "audio-call-action")
        awaitObject(By.res(VIDEO_CALL_ACTION_TAG), LOAD_TIMEOUT, "video-call-action")

        openOptionsMenu()
        awaitObject(By.text(infoText()), LOAD_TIMEOUT, "info-option")
        awaitObject(By.text(clearText()), LOAD_TIMEOUT, "clear-option")
        awaitObject(By.text(archiveText()), LOAD_TIMEOUT, "archive-option")
        awaitObject(By.text(muteText()), LOAD_TIMEOUT, "mute-option")
    }

    /**
     * tc04 — clearing the history requests it through the chat gateway (via a confirmation dialog),
     * and archiving the chat requests the archive through the chat gateway.
     */
    @Test
    fun clearHistoryThenArchiveChat() {
        openChatRoom()

        openOptionsMenu()
        clickText(clearText(), "clear-option")
        awaitObject(By.res(CLEAR_CONFIRMATION_DIALOG_TAG), LOAD_TIMEOUT, "clear-confirmation-dialog")
        clickText(clearText(), "clear-confirm")
        awaitInvocations("clearChatHistory($CHAT_ID)") {
            fakeMegaChat.invocations.filter {
                it.methodName == "clearChatHistory" && it.arguments.getOrNull(0) == CHAT_ID
            }
        }

        openOptionsMenu()
        clickText(archiveText(), "archive-option")
        awaitInvocations("archiveChat($CHAT_ID, archive=true)") {
            fakeMegaChat.invocations.filter {
                it.methodName == "archiveChat" &&
                        it.arguments.getOrNull(0) == CHAT_ID &&
                        it.arguments.getOrNull(1) == true
            }
        }
    }

    /**
     * tc05 — muting via the three-dot menu opens the mute-options dialog ("Mute chat notifications
     * for…") with a confirm action.
     */
    @Test
    fun muteChatViaContextMenu() {
        openChatRoom()

        openOptionsMenu()
        clickText(muteText(), "mute-option")

        awaitObject(By.text(muteDialogTitle()), LOAD_TIMEOUT, "mute-dialog-title")
        awaitObject(By.text(okText()), LOAD_TIMEOUT, "mute-dialog-ok-button")
    }

    /** tc11 — sending an emoji requests the send through the chat gateway. */
    @Test
    fun sendEmojiMessage() {
        sendMessageAndAssertGatewayCall(EMOJI_MESSAGE)
    }

    /** tc12 — sending a text message requests the send through the chat gateway. */
    @Test
    fun sendTextMessage() {
        sendMessageAndAssertGatewayCall(TEXT_MESSAGE)
    }

    // endregion

    // region shared scenario bodies

    private fun sendMessageAndAssertGatewayCall(message: String) {
        openChatRoom()

        awaitObject(By.res(CHAT_TEXT_FIELD_TAG), LOAD_TIMEOUT, "chat-input-field")
        device.findObject(By.res(CHAT_TEXT_FIELD_TAG)).text = message

        click(SEND_ICON_TAG, "send-icon")

        awaitInvocations("sendMessage($CHAT_ID, \"$message\")") {
            fakeMegaChat.invocations.filter {
                it.methodName == "sendMessage" &&
                        it.arguments.getOrNull(0) == CHAT_ID &&
                        it.arguments.getOrNull(1) == message
            }
        }
    }

    // endregion

    // region navigation & seeding helpers

    private fun openChatRoom() {
        val intent = Intent(targetContext, ChatActivity::class.java)
            .putExtra(ChatNavKey.LEGACY_CHAT_ID, CHAT_ID)
        scenario = ActivityScenario.launch(intent)
        awaitObject(By.text(CONTACT_NAME), LAUNCH_TIMEOUT, "chat-room-loaded")
    }

    private fun openOptionsMenu() = click(SHOW_MORE_TAG, "options-overflow-button")

    private fun oneToOneChatRoom() = StubMegaChatRoom(
        chatId = CHAT_ID,
        title = CONTACT_NAME,
        ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
        peers = listOf(PEER_HANDLE to MegaChatRoom.PRIV_STANDARD),
        isGroup = false,
        isActive = true,
        isArchived = false,
    )

    // endregion

    // region string resolution (options / dialogs are matched by text)

    private fun infoText() = targetContext.getString(R.string.general_info)
    private fun archiveText() = targetContext.getString(R.string.general_archive)
    private fun muteText() = targetContext.getString(R.string.general_mute)
    private fun muteDialogTitle() =
        targetContext.getString(R.string.title_dialog_mute_chatroom_notifications)

    private fun clearText() = targetContext.getString(sharedR.string.general_clear)
    private fun okText() = targetContext.getString(sharedR.string.general_ok)

    // endregion

    // region UiAutomator helpers

    private fun click(tag: String, name: String) {
        awaitObject(By.res(tag), LOAD_TIMEOUT, name)
        device.findObject(By.res(tag)).click()
    }

    private fun clickText(text: String, name: String) {
        awaitObject(By.text(text), LOAD_TIMEOUT, name)
        device.findObject(By.text(text)).click()
    }

    private fun awaitObject(selector: BySelector, timeout: Long, name: String) {
        if (device.wait(Until.hasObject(selector), timeout)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump",
        )
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
                    "recorded methods=${fakeMegaChat.invocations.map { it.methodName }}",
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
        const val POLL_INTERVAL_MS = 100L

        const val CHAT_ID = 1_001L
        const val PEER_HANDLE = 2_002L
        const val CONTACT_NAME = "sm14 contact"
        const val TEXT_MESSAGE = "sm14tc12 msg"
        const val EMOJI_MESSAGE = "🙂"

        /** Mirrors TAG_MENU_ACTIONS_SHOW_MORE of the app bar overflow button. */
        const val SHOW_MORE_TAG = "menuActionsShowMore"

        /** Mirrors ChatRoomMenuAction.TEST_TAG_AUDIO_CALL_ACTION / _VIDEO_CALL_ACTION. */
        const val AUDIO_CALL_ACTION_TAG = "chat_view:action_chat_audio_call"
        const val VIDEO_CALL_ACTION_TAG = "chat_view:action_chat_video_call"

        /** Mirrors TEST_TAG_CLEAR_CHAT_CONFIRMATION_DIALOG. */
        const val CLEAR_CONFIRMATION_DIALOG_TAG = "chat_view:dialog_chat_clear:history"

        /** Mirrors CHAT_TEXT_FIELD_TEXT_TAG / TEST_TAG_SEND_ICON of the chat input toolbar. */
        const val CHAT_TEXT_FIELD_TAG = "chat_text_field"
        const val SEND_ICON_TAG = "chat_input_text_toolbar:send_icon"
    }
}
