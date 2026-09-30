package mega.privacy.android.app

import android.content.Intent
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
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaChatMessage
import mega.privacy.android.data.test.stub.StubMegaChatRoom
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.data.test.stub.StubMegaNodeList
import mega.privacy.android.domain.usecase.GetStorageDownloadDefaultPathUseCase
import mega.privacy.android.domain.usecase.SetAskForDownloadLocationUseCase
import mega.privacy.android.domain.usecase.SetDownloadLocationUseCase
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
 * Saving a chat attachment to the device raises the "download started" snackbar on the chat room's
 * Material 2 host.
 *
 * [mega.privacy.android.app.presentation.meeting.chat.view.ChatView] is one of the legacy Material 2
 * screens that hands its own `scaffoldState.snackbarHostState` to `StartTransferComponent`, so this
 * journey is the one that exercises the Material 2 seam rather than the Material 3 path every other
 * caller takes.
 *
 * Built on the `:data-test` fake-SDK harness following [ChatRoomTest] and [FileFolderMenuTest]: the
 * whole app runs as in production with only the SDK gateways faked, the UI is driven with
 * UiAutomator, and Compose test tags are matched as resource ids.
 *
 * The holds are deliberate — they keep the journey legible when it is screen-recorded for a merge
 * request, and cost a few seconds per CI run.
 */
@HiltAndroidTest
class ChatAttachmentDownloadSnackbarTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var fakeMegaChat: FakeMegaChatApiGateway

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    @Inject
    lateinit var setDownloadLocationUseCase: SetDownloadLocationUseCase

    @Inject
    lateinit var setAskForDownloadLocationUseCase: SetAskForDownloadLocationUseCase

    @Inject
    lateinit var getStorageDownloadDefaultPathUseCase: GetStorageDownloadDefaultPathUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private val rootHandle get() = fakeMegaApi.nodeTree.rootNode.handle

    private var scenario: ActivityScenario<*>? = null

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

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            // Without a settled destination the download opens the folder picker instead of
            // starting, and the snackbar never fires. It must be the default path specifically:
            // ShouldAskDownloadDestinationUseCase rejects any other location that has no persisted
            // SAF permission, and asks again.
            setDownloadLocationUseCase(getStorageDownloadDefaultPathUseCase())
            setAskForDownloadLocationUseCase(false)
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        fakeMegaChat.resetToDefaults()
    }

    @Test
    fun saveToDeviceShowsTheDownloadStartedSnackbar() {
        seedChatRoomWithAttachment()

        scenario = ActivityScenario.launch<ChatActivity>(
            Intent(targetContext, ChatActivity::class.java)
                .putExtra(ChatNavKey.LEGACY_CHAT_ID, CHAT_ID),
        )

        awaitObject(By.text(CONTACT_NAME), LAUNCH_TIMEOUT, "chat-room-loaded")
        awaitObject(By.text(FILE_NAME), LOAD_TIMEOUT, "attachment-message-row")
        hold()

        device.findObject(By.text(FILE_NAME)).longClick()
        awaitObject(By.res(SAVE_TO_DEVICE_SHEET_ITEM), LOAD_TIMEOUT, "message-options-sheet")
        hold()

        device.findObject(By.res(SAVE_TO_DEVICE_SHEET_ITEM)).click()

        val started = targetContext.getString(sharedR.string.transfers_download_started_snackbar)
        awaitObject(By.text(started), LOAD_TIMEOUT, "download-started-snackbar")
        hold(FINAL_HOLD_MS)
    }

    private fun seedChatRoomWithAttachment() {
        val node = StubMegaNode(
            handle = FILE_HANDLE,
            name = FILE_NAME,
            parentHandle = rootHandle,
            isFolder = false,
            size = FILE_SIZE,
            modificationTime = System.currentTimeMillis() / 1000,
            owner = fakeMegaApi.account.myUserHandle,
        )
        fakeMegaApi.nodeTree.addNode(node, parentHandle = rootHandle)

        fakeMegaChat.chatState.chatRooms[CHAT_ID] = StubMegaChatRoom(
            chatId = CHAT_ID,
            title = CONTACT_NAME,
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
            peers = listOf(PEER_HANDLE to MegaChatRoom.PRIV_STANDARD),
            isGroup = false,
            isActive = true,
        )

        fakeMegaChat.chatState.addChatMessage(
            CHAT_ID,
            StubMegaChatMessage(
                msgId = ATTACHMENT_MSG_ID,
                userHandle = PEER_HANDLE,
                type = MegaChatMessage.TYPE_NODE_ATTACHMENT,
                status = MegaChatMessage.STATUS_DELIVERED,
                timestamp = System.currentTimeMillis() / 1000 - MESSAGE_AGE_SECONDS,
                megaNodeList = StubMegaNodeList(listOf(node)),
            ),
        )
    }

    /** Leaves the current state on screen long enough for a viewer to register it. */
    private fun hold(millis: Long = HOLD_MS) = Thread.sleep(millis)

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

    private companion object {
        const val LAUNCH_TIMEOUT = 60_000L
        const val LOAD_TIMEOUT = 30_000L
        const val HOLD_MS = 1_500L
        const val FINAL_HOLD_MS = 2_500L

        const val CHAT_ID = 1_001L
        const val PEER_HANDLE = 2_002L
        const val CONTACT_NAME = "and25085 contact"
        const val ATTACHMENT_MSG_ID = 10L
        const val MESSAGE_AGE_SECONDS = 3_600L

        const val FILE_HANDLE = 100L
        const val FILE_NAME = "and25085-attachment.txt"
        const val FILE_SIZE = 1_024L

        /** Mirrors MessageAction's bottomSheetItemTestTag for SaveToDeviceMessageAction. */
        const val SAVE_TO_DEVICE_SHEET_ITEM = "chat_message_options_sheet:save_to_device"
    }
}
