package mega.privacy.android.app

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipboardManager
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
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
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.state.FakeChatState
import mega.privacy.android.data.test.stub.StubMegaChatMessage
import mega.privacy.android.data.test.stub.StubMegaChatRoom
import mega.privacy.android.data.test.stub.StubMegaHandleList
import mega.privacy.android.data.test.stub.StubMegaStringList
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaChatMessage
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
 * the mute dialog, rendered message rows) and, for the fire-and-forget chat gateway commands that
 * the fake does not simulate a follow-up event for (clear history, archive), that the app invoked
 * the matching gateway method.
 *
 * Message rows render through the app's real paging pipeline: history is seeded via
 * [FakeChatState.addChatMessage] and delivered by the fake's `loadMessages`, own sends echo an
 * in-flight message that the app stores itself, so sent / forwarded / re-sent messages are asserted
 * as rendered rows. That unblocks the scenarios that need rendered rows: tc02 forward, tc03 copy &
 * re-send, tc06 select & forward multiple, and tc10 react with emoji. The chat picker used by
 * forward is a separate legacy activity that still talks to the SDK directly, so — like the file
 * picker in [CloudDriveUploadTest] — it is stubbed with Espresso-Intents to immediately return the
 * current chat as the forward target; everything after the picker result runs the production
 * forward path. The reaction confirmation that the real SDK pushes after `addReaction` is emitted
 * through the fake (`OnReactionUpdate` plus stubbed reaction reads) so the reaction chip renders.
 *
 * Two gotchas learned from earlier ports are applied: modal / popup surfaces (the options overflow
 * dropdown, the confirmation and mute dialogs, the message-options sheet) are driven by `By.text`
 * where they do not carry `testTagsAsResourceId`, and gateway commands dispatched from
 * `viewModelScope.launch` are polled with [awaitInvocations] rather than asserted synchronously.
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

    /**
     * tc02 — forwarding a message to the same chat re-sends its content through the chat gateway
     * and renders it as a second message row. The chat-picker activity is stubbed with
     * Espresso-Intents to return the current chat as the target.
     */
    @Test
    fun forwardMessageToSameChat() {
        seedPeerMessage(FORWARD_MSG_ID, FORWARD_MESSAGE)
        withStubbedChatPicker {
            openChatRoom()

            longClickText(FORWARD_MESSAGE, "seeded-message-row")
            clickText(forwardText(), "forward-option")

            awaitSendMessageInvocation(FORWARD_MESSAGE)
            awaitObjectCount(By.text(FORWARD_MESSAGE), 2, "forwarded-message-row")
        }
    }

    /**
     * tc03 — copying a message puts its content on the clipboard, and re-sending the copied text
     * renders it as a second message row.
     */
    @Test
    fun copyMessageAndResend() {
        seedPeerMessage(COPY_MSG_ID, COPY_MESSAGE)
        openChatRoom()

        longClickText(COPY_MESSAGE, "seeded-message-row")
        clickText(copyText(), "copy-option")
        awaitClipboard(COPY_MESSAGE)

        sendMessageAndAssertRenderedRow(COPY_MESSAGE, expectedRowCount = 2)
    }

    /**
     * tc06 — selecting two messages via select mode and forwarding them re-sends both contents
     * through the chat gateway and renders each as a second row.
     */
    @Test
    fun selectAndForwardMultipleMessages() {
        seedPeerMessage(MULTI_MSG_ID_1, MULTI_MESSAGE_1)
        seedPeerMessage(MULTI_MSG_ID_2, MULTI_MESSAGE_2)
        withStubbedChatPicker {
            openChatRoom()

            longClickText(MULTI_MESSAGE_1, "first-message-row")
            clickText(selectText(), "select-option")
            clickText(MULTI_MESSAGE_2, "second-message-row")
            click(TOOLBAR_FORWARD_TAG, "toolbar-forward-action")

            awaitSendMessageInvocation(MULTI_MESSAGE_1)
            awaitSendMessageInvocation(MULTI_MESSAGE_2)
            awaitObjectCount(By.text(MULTI_MESSAGE_1), 2, "first-forwarded-row")
            awaitObjectCount(By.text(MULTI_MESSAGE_2), 2, "second-forwarded-row")
        }
    }

    /**
     * tc10 — reacting to a message with an emoji requests the reaction through the chat gateway
     * and, once the SDK-side confirmation is simulated, renders the reaction chip on the row.
     */
    @Test
    fun reactToMessageWithEmoji() {
        seedPeerMessage(REACT_MSG_ID, REACT_MESSAGE)
        openChatRoom()

        longClickText(REACT_MESSAGE, "seeded-message-row")
        clickText(THUMBS_UP_REACTION, "thumbs-up-reaction")

        awaitInvocations("addReaction($CHAT_ID, $REACT_MSG_ID, $THUMBS_UP_REACTION)") {
            fakeMegaChat.invocations.filter {
                it.methodName == "addReaction" &&
                        it.arguments.getOrNull(0) == CHAT_ID &&
                        it.arguments.getOrNull(1) == REACT_MSG_ID &&
                        it.arguments.getOrNull(2) == THUMBS_UP_REACTION
            }
        }

        // The real SDK confirms the reaction with an onReactionUpdate callback and serves the
        // reaction reads; simulate both so the chip renders on the message row.
        fakeMegaChat.stubResult(
            MegaChatApiGateway::getMessageReactions,
            StubMegaStringList(listOf(THUMBS_UP_REACTION)),
        )
        fakeMegaChat.stubResult(MegaChatApiGateway::getMessageReactionCount, 1)
        fakeMegaChat.stubResult(
            MegaChatApiGateway::getReactionUsers,
            StubMegaHandleList(listOf(fakeMegaChat.chatState.myUserHandle)),
        )
        runBlocking {
            fakeMegaChat.emitChatRoomUpdate(
                CHAT_ID,
                ChatRoomUpdate.OnReactionUpdate(REACT_MSG_ID, THUMBS_UP_REACTION, 1),
            )
        }

        awaitObject(By.res(REACTION_CHIP_TAG), LOAD_TIMEOUT, "reaction-chip")
    }

    /** tc11 — sending an emoji sends it through the chat gateway and renders the message row. */
    @Test
    fun sendEmojiMessage() {
        openChatRoom()
        sendMessageAndAssertRenderedRow(EMOJI_MESSAGE)
    }

    /** tc12 — sending a text message sends it through the chat gateway and renders the row. */
    @Test
    fun sendTextMessage() {
        openChatRoom()
        sendMessageAndAssertRenderedRow(TEXT_MESSAGE)
    }

    // endregion

    // region shared scenario bodies

    /**
     * Type [message] into the chat input, send it, and assert both the gateway call and the
     * rendered message row. The row assertion only counts matches after the send call cleared
     * the input field, so the input's own text cannot satisfy it: [expectedRowCount] rows must
     * match once the field is empty again.
     */
    private fun sendMessageAndAssertRenderedRow(message: String, expectedRowCount: Int = 1) {
        awaitObject(By.res(CHAT_TEXT_FIELD_TAG), LOAD_TIMEOUT, "chat-input-field")
        device.findObject(By.res(CHAT_TEXT_FIELD_TAG)).text = message

        click(SEND_ICON_TAG, "send-icon")

        awaitSendMessageInvocation(message)
        awaitGone(By.res(CHAT_TEXT_FIELD_TAG).text(message), "chat-input-cleared")
        awaitObjectCount(By.text(message), expectedRowCount, "sent-message-row")
    }

    private fun awaitSendMessageInvocation(message: String) {
        awaitInvocations("sendMessage($CHAT_ID, \"$message\")") {
            fakeMegaChat.invocations.filter {
                it.methodName == "sendMessage" &&
                        it.arguments.getOrNull(0) == CHAT_ID &&
                        it.arguments.getOrNull(1) == message
            }
        }
    }

    /**
     * The forward target picker ([mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerActivity])
     * is a legacy activity that talks to the SDK directly, so it cannot run against the fakes;
     * stub its result with Espresso-Intents as if the user had picked this same chat.
     */
    private fun withStubbedChatPicker(block: () -> Unit) {
        Intents.init()
        try {
            intending(hasAction(Constants.ACTION_FORWARD_MESSAGES)).respondWith(
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().putExtra(Constants.SELECTED_CHATS, longArrayOf(CHAT_ID)),
                ),
            )
            block()
        } finally {
            Intents.release()
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

    /** Seed a seen peer text message into the room's history rendered by the paging pipeline. */
    private fun seedPeerMessage(msgId: Long, text: String) {
        fakeMegaChat.chatState.addChatMessage(
            CHAT_ID,
            StubMegaChatMessage(
                msgId = msgId,
                userHandle = PEER_HANDLE,
                type = MegaChatMessage.TYPE_NORMAL,
                status = MegaChatMessage.STATUS_SEEN,
                timestamp = System.currentTimeMillis() / 1000 - MESSAGE_AGE_SECONDS + msgId,
                content = text,
                isDeletable = true,
            ),
        )
    }

    // endregion

    // region string resolution (options / dialogs are matched by text)

    private fun infoText() = targetContext.getString(R.string.general_info)
    private fun archiveText() = targetContext.getString(R.string.general_archive)
    private fun muteText() = targetContext.getString(R.string.general_mute)
    private fun muteDialogTitle() =
        targetContext.getString(R.string.title_dialog_mute_chatroom_notifications)

    private fun clearText() = targetContext.getString(sharedR.string.general_clear)
    private fun okText() = targetContext.getString(sharedR.string.general_ok)
    private fun forwardText() = targetContext.getString(R.string.forward_menu_item)
    private fun copyText() = targetContext.getString(R.string.context_copy)
    private fun selectText() = targetContext.getString(R.string.general_select)

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

    private fun longClickText(text: String, name: String) {
        awaitObject(By.text(text), LOAD_TIMEOUT, name)
        device.findObject(By.text(text)).longClick()
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
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name ($selector) to be gone; " +
                    "hierarchy in logcat tag UiDump",
        )
    }

    private fun awaitObjectCount(selector: BySelector, count: Int, name: String) {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            if (device.findObjects(selector).size >= count) return
            Thread.sleep(POLL_INTERVAL_MS)
        }
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $count objects matching $name " +
                    "($selector), found ${device.findObjects(selector).size}; " +
                    "hierarchy in logcat tag UiDump",
        )
    }

    /**
     * Clipboard reads are only allowed for the focused app, so read it on the activity's main
     * thread and poll: the copy action runs in a composable trigger one frame after the click.
     */
    private fun awaitClipboard(expected: String) {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        var lastSeen: String? = null
        while (SystemClock.uptimeMillis() < deadline) {
            scenario?.onActivity { activity ->
                lastSeen = activity.getSystemService(ClipboardManager::class.java)
                    ?.primaryClip?.getItemAt(0)?.text?.toString()
            }
            if (lastSeen == expected) return
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for clipboard to contain \"$expected\"; " +
                    "last seen: $lastSeen",
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
        const val FORWARD_MESSAGE = "sm14tc02 fwd"
        const val COPY_MESSAGE = "sm14tc03 copy"
        const val MULTI_MESSAGE_1 = "sm14tc06 first"
        const val MULTI_MESSAGE_2 = "sm14tc06 second"
        const val REACT_MESSAGE = "sm14tc10 react"
        const val FORWARD_MSG_ID = 10L
        const val COPY_MSG_ID = 20L
        const val MULTI_MSG_ID_1 = 30L
        const val MULTI_MSG_ID_2 = 31L
        const val REACT_MSG_ID = 40L
        const val MESSAGE_AGE_SECONDS = 3_600L

        /** THUMBS_UP_REACTION of AddReactionsSheetItem; matched as text in the options sheet. */
        const val THUMBS_UP_REACTION = "👍"

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

        /** Mirrors ForwardMessageAction.toolbarMenuItemTestTag in the select-mode toolbar. */
        const val TOOLBAR_FORWARD_TAG = "chat_message_toolbar:action_forward"

        /** Mirrors TEST_TAG_CHAT_MESSAGE_REACTION_CHIP of ReactionChip. */
        const val REACTION_CHIP_TAG = "chat_message_reaction:reaction_chip"
    }
}
