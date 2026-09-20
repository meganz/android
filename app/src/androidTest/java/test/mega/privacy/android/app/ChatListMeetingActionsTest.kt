package mega.privacy.android.app

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
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaChatListItem
import mega.privacy.android.data.test.stub.StubMegaChatRoom
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.navigation.destination.ChatListNavKey
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
 * Full-app instrumented test for the meeting creation actions restored in AND-24991.
 *
 * The chat list FAB is tab-aware: on the Chats tab it opens the new-chat flow, on the Meetings tab
 * it opens a bottom sheet offering Start meeting now / Join meeting / Schedule meeting. This test
 * walks that journey end to end — including the tab switch itself, where each tab must list only
 * its own rooms — rotating the device while the sheet is open to prove the sheet and the selected
 * tab survive the activity recreation rotation causes.
 *
 * Built on the `:data-test` fake-SDK framework following [ChatListFabAndEmptyStateTest]: the whole
 * app runs as in production with only the SDK gateways faked; the UI is driven with UiAutomator, and
 * Compose test tags are matched as resource ids because MegaActivity sets
 * `testTagsAsResourceId = true`.
 *
 * "Start meeting now" and "Schedule meeting" are deliberately not followed through: both leave the
 * Navigation3 shell for `MeetingActivity` / `CreateScheduledMeetingActivity`, legacy
 * `PasscodeActivity` screens that cannot run under the Hilt test application. The navigation events
 * they emit are covered by `ChatListViewModelTest` instead.
 *
 * The single continuous journey also doubles as the MR demo recording for AND-24991.
 */
@HiltAndroidTest
class ChatListMeetingActionsTest {

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

    private val startMeetingLabel: String
        get() = targetContext.getString(sharedR.string.chat_meeting_actions_start_now)

    private val joinMeetingLabel: String
        get() = targetContext.getString(sharedR.string.chat_meeting_actions_join)

    private val scheduleMeetingLabel: String
        get() = targetContext.getString(sharedR.string.chat_meeting_actions_schedule)

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
        fakeFlags.set(ApiFeatures.ContactComposeFeature, true)

        fakeMegaChat.resetToDefaults()
        seedRooms()

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            setCustomiseNavigationTooltipShownUseCase()
        }
    }

    @After
    fun tearDown() {
        runCatching { device.setOrientationNatural() }
        fakeFlags.clear()
        fakeMegaChat.resetToDefaults()
    }

    @Test
    fun meetingFabOpensActionsSheet() {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ChatListNavKey(showMeetingTab = false)),
        )
        ActivityScenario.launch<MegaActivity>(intent)

        // Chats tab: only the non-meeting room is listed and the FAB opens the new-chat flow.
        awaitObject(chatRow(CHAT_TITLE), LAUNCH_TIMEOUT, "chat-row")
        awaitObject(By.res(CHAT_LIST_FAB_TAG), LAUNCH_TIMEOUT, "new-chat-fab")
        awaitGone(chatRow(MEETING_TITLE), "meeting-row-hidden-on-chats-tab")

        // Switching to Meetings swaps both the listed rooms and the FAB.
        val meetingsTab = targetContext.getString(sharedR.string.chat_tab_meetings_title)
        device.findObject(By.text(meetingsTab)).click()
        awaitObject(chatRow(MEETING_TITLE), LOAD_TIMEOUT, "meeting-row")
        awaitGone(chatRow(CHAT_TITLE), "chat-row-hidden-on-meetings-tab")
        awaitObject(By.res(CHAT_LIST_MEETING_FAB_TAG), LOAD_TIMEOUT, "meeting-fab")
        awaitGone(By.res(CHAT_LIST_FAB_TAG), "new-chat-fab-hidden")

        // The meeting FAB opens the actions sheet.
        device.findObject(By.res(CHAT_LIST_MEETING_FAB_TAG)).click()
        awaitSheetActions("meeting-actions-sheet")

        // Rotating recreates the activity; the sheet and the selected tab must come back.
        device.setOrientationLeft()
        device.waitForIdle()
        awaitSheetActions("meeting-actions-sheet-landscape")

        device.setOrientationNatural()
        device.waitForIdle()
        awaitSheetActions("meeting-actions-sheet-portrait")

        // Join meeting opens the dialog asking for a meeting link.
        device.findObject(By.text(joinMeetingLabel)).click()
        val dialogTitle = targetContext.getString(sharedR.string.chat_join_meeting_dialog_title)
        awaitObject(By.text(dialogTitle), LOAD_TIMEOUT, "join-meeting-dialog-title")
        val linkHint = targetContext.getString(sharedR.string.chat_join_meeting_dialog_hint)
        awaitObject(By.text(linkHint), LOAD_TIMEOUT, "join-meeting-dialog-hint")

        // Cancelling returns to the Meetings tab with its meeting FAB.
        val cancel = targetContext.getString(sharedR.string.general_dialog_cancel_button)
        device.findObject(By.text(cancel)).click()
        awaitGone(By.text(dialogTitle), "join-meeting-dialog-dismissed")
        awaitObject(By.res(CHAT_LIST_MEETING_FAB_TAG), LOAD_TIMEOUT, "meeting-fab-restored")
    }

    /**
     * The sheet's rows are matched by label rather than by test tag: modal bottom sheets and
     * dialogs compose in their own window, which does not inherit MegaActivity's
     * `testTagsAsResourceId`, so their tags never surface as resource ids to UiAutomator.
     */
    private fun awaitSheetActions(name: String) {
        awaitObject(By.text(startMeetingLabel), LOAD_TIMEOUT, "$name-start")
        awaitObject(By.text(joinMeetingLabel), LOAD_TIMEOUT, "$name-join")
        awaitObject(By.text(scheduleMeetingLabel), LOAD_TIMEOUT, "$name-schedule")
    }

    private fun seedRooms() {
        seedRoom(FIRST_CHAT_ID, CHAT_TITLE, isMeeting = false)
        seedRoom(FIRST_CHAT_ID + 1, MEETING_TITLE, isMeeting = true)
        seedRoom(FIRST_CHAT_ID + 2, "Design review", isMeeting = true)
    }

    private fun chatRow(title: String): BySelector =
        By.res(CHAT_ROOM_ITEM_TAG).hasDescendant(By.text(title))

    private fun seedRoom(chatId: Long, title: String, isMeeting: Boolean) {
        fakeMegaChat.chatState.chatRooms[chatId] = StubMegaChatRoom(
            chatId = chatId,
            title = title,
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
            peers = listOf(PEER_HANDLE_BASE + chatId to MegaChatRoom.PRIV_STANDARD),
            isGroup = true,
            isMeeting = isMeeting,
            isActive = true,
            isArchived = false,
        )
        fakeMegaChat.chatState.chatListItems[chatId] = StubMegaChatListItem(
            chatId = chatId,
            title = title,
            lastMessage = "Message in $title",
            lastTimestamp = BASE_TIMESTAMP - chatId,
            isGroup = true,
            isActive = true,
        )
    }

    private fun awaitObject(selector: BySelector, timeout: Long, name: String) {
        if (device.wait(Until.hasObject(selector), timeout)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump"
        )
    }

    private fun awaitGone(selector: BySelector, name: String) {
        if (device.wait(Until.gone(selector), LOAD_TIMEOUT)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name to disappear ($selector); " +
                    "hierarchy in logcat tag UiDump"
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

        const val FIRST_CHAT_ID = 5_100L
        const val PEER_HANDLE_BASE = 9_100L
        const val BASE_TIMESTAMP = 1_749_000_000L

        const val CHAT_TITLE = "Team standup"
        const val MEETING_TITLE = "Weekly sync"

        /** Mirror ChatListScreen / ChatRoomItemView test tags. */
        const val CHAT_ROOM_ITEM_TAG = "chat_room_item:row"
        const val CHAT_LIST_FAB_TAG = "chat_list_screen:new_chat_fab"
        const val CHAT_LIST_MEETING_FAB_TAG = "chat_list_screen:new_meeting_fab"
    }
}
