package mega.privacy.android.app

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaChatCall
import mega.privacy.android.data.test.stub.StubMegaChatListItem
import mega.privacy.android.data.test.stub.StubMegaChatRoom
import mega.privacy.android.data.test.stub.StubMegaHandleList
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.navigation.destination.ChatListNavKey
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatRoom
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented test for what a chat-list row itself does: opening the conversation it
 * stands for, and advertising a call that is running in it.
 *
 * Tapping a row navigates through `ShowChatMessagesNavKey`, which hands off to the legacy
 * [mega.privacy.android.app.presentation.meeting.chat.ChatActivity] chat room; back must land on
 * the chat list again. Seeding an in-progress call for the same room drives the production
 * `GetChatsUseCase` call pipeline so the row renders its ongoing-call indicator — the per-row
 * counterpart of the global banner covered by [OngoingCallBannerInstrumentedTest].
 *
 * Built on the `:data-test` fake-SDK framework following [ChatListSelectionTest]: the whole app
 * runs as in production with only the SDK gateways faked; the UI is driven with UiAutomator, and
 * Compose test tags are matched as resource ids because both MegaActivity and the chat room set
 * `testTagsAsResourceId = true`. The rendered message list inside the chat room is not asserted
 * here — [ChatRoomTest] owns the chat-room journeys and documents their exclusions.
 */
@HiltAndroidTest
class ChatListRowNavigationTest {

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
        seedChatRoom()
        fakeMegaChat.clearInvocations()

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            setCustomiseNavigationTooltipShownUseCase()
        }
    }

    @After
    fun tearDown() {
        fakeFlags.clear()
        fakeMegaChat.resetToDefaults()
    }

    @Test
    fun tappingAChatRowOpensItsChatRoomAndBackReturnsToTheList() {
        launchChatList()

        awaitObject(chatRow(CHAT_TITLE), LAUNCH_TIMEOUT, "chat-row").click()

        // The chat room renders for the tapped chat: its app bar carries the room title and its
        // message input bar is up.
        awaitObject(
            By.res(CHAT_APP_BAR_TITLE_TAG).text(CHAT_TITLE),
            LAUNCH_TIMEOUT,
            "chat-room-app-bar-title",
        )
        awaitObject(By.res(CHAT_TEXT_FIELD_TAG), LOAD_TIMEOUT, "chat-room-input-bar")

        device.pressBack()
        awaitObject(By.res(CHAT_LIST_SCREEN_TAG), LOAD_TIMEOUT, "chat-list-restored")
        awaitObject(chatRow(CHAT_TITLE), LOAD_TIMEOUT, "chat-row-restored")
    }

    @Test
    fun aCallRunningInAChatShowsTheOngoingCallIndicatorOnItsRow() {
        stubOngoingCall()

        launchChatList()

        awaitObject(chatRow(CHAT_TITLE), LAUNCH_TIMEOUT, "chat-row")
        awaitObject(
            By.res(CHAT_ROOM_ITEM_TAG).hasDescendant(By.res(CHAT_ROOM_ITEM_CALL_TAG)),
            LOAD_TIMEOUT,
            "ongoing-call-indicator",
        )
    }

    private fun launchChatList() {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ChatListNavKey(showMeetingTab = false)),
        )
        ActivityScenario.launch<MegaActivity>(intent)
    }

    /**
     * Reports an in-progress call in the seeded room so `GetChatsUseCase` resolves the row's
     * current call status to a started call. See [OngoingCallBannerInstrumentedTest].
     */
    private fun stubOngoingCall() {
        fakeMegaChat.stub(
            MegaChatApiGateway::getChatCalls,
            matcher = { it.firstOrNull() == MegaChatCall.CALL_STATUS_IN_PROGRESS },
        ) { StubMegaHandleList(listOf(CHAT_ID)) }

        fakeMegaChat.stub(
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

    private fun seedChatRoom() {
        fakeMegaChat.chatState.chatRooms[CHAT_ID] = StubMegaChatRoom(
            chatId = CHAT_ID,
            title = CHAT_TITLE,
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
            peers = listOf(PEER_HANDLE to MegaChatRoom.PRIV_STANDARD),
            isGroup = true,
            isActive = true,
            isArchived = false,
        )
        fakeMegaChat.chatState.chatListItems[CHAT_ID] = StubMegaChatListItem(
            chatId = CHAT_ID,
            title = CHAT_TITLE,
            lastMessage = "Message in $CHAT_TITLE",
            lastTimestamp = BASE_TIMESTAMP,
            isGroup = true,
            isActive = true,
        )
    }

    private fun chatRow(title: String): BySelector =
        By.res(CHAT_ROOM_ITEM_TAG).hasDescendant(By.text(title))

    private fun awaitObject(selector: BySelector, timeout: Long, name: String): UiObject2 {
        if (device.wait(Until.hasObject(selector), timeout)) return device.findObject(selector)
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump"
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

        const val CHAT_ID = 5_401L
        const val CALL_ID = 6_401L
        const val PEER_HANDLE = 7_401L
        const val BASE_TIMESTAMP = 1_700_000_000L
        const val CHAT_TITLE = "Design Team"

        /** Mirror ChatListScreen / ChatRoomItemView test tags. */
        const val CHAT_LIST_SCREEN_TAG = "chat_list_screen:screen"
        const val CHAT_ROOM_ITEM_TAG = "chat_room_item:row"
        const val CHAT_ROOM_ITEM_CALL_TAG = "chat_room_item:call_indicator"

        /** Mirror the chat room's app bar title and CHAT_TEXT_FIELD_TEXT_TAG input field. */
        const val CHAT_APP_BAR_TITLE_TAG = "mega_app_bar_title:text"
        const val CHAT_TEXT_FIELD_TAG = "chat_text_field"
    }
}
