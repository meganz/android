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
import mega.privacy.android.data.test.stub.StubMegaChatScheduledMeeting
import mega.privacy.android.data.test.stub.StubMegaChatScheduledRules
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatScheduledRules
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.time.Instant
import javax.inject.Inject

/**
 * Full-app instrumented test for the chat-list meetings-tab decorations added in AND-24789.
 *
 * With [ApiFeatures.CustomisableBottomNavigation] on, the new chat list is opened on the Meetings
 * tab seeded with two pending scheduled meetings — a one-off (upcoming) and a weekly recurring one.
 * The tab must then show the "Today" section header, the "Upcoming meeting" / "Recurring meeting"
 * subtitles, and the recurring icon.
 *
 * Built on the `:data-test` fake-SDK framework following [ChatListSelectionTest]: the whole app
 * runs as in production — real navigation, ViewModels, the chat repository and the
 * `GetChatsUseCase` MEETINGS pipeline — with only the SDK gateways faked. Each meeting needs a
 * [StubMegaChatRoom] (isMeeting), a matching [StubMegaChatListItem] and a
 * [StubMegaChatScheduledMeeting] seeded into [FakeMegaChatApiGateway.chatState]; occurrences are
 * not modelled, so the domain falls back to the scheduled meeting's own start / end times. The UI
 * is driven with UiAutomator; Compose test tags are matched as resource ids because MegaActivity
 * sets `testTagsAsResourceId = true`.
 *
 * The single continuous journey also doubles as the MR demo recording for AND-24789.
 */
@HiltAndroidTest
class ChatListMeetingsDecorationsTest {

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
        seedMeetings()
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
    fun meetingsTabShowsHeadersAndPendingMeetingDecorations() {
        // Enter the new chat list on the Meetings tab through the production intent seam.
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ChatListNavKey(showMeetingTab = true)),
        )
        ActivityScenario.launch<MegaActivity>(intent)

        // Both meetings are scheduled today, so the first pending meeting carries the "Today" header.
        awaitObject(By.res(SECTION_HEADER_TAG), LAUNCH_TIMEOUT, "section-header")
        val todayHeader = targetContext.getString(sharedR.string.meetings_list_header_today)
        awaitObject(By.text(todayHeader), LOAD_TIMEOUT, "today-header")
        Thread.sleep(2_000)

        // The one-off meeting shows the upcoming label; the recurring one shows the recurring
        // label and the recurring icon.
        val upcomingLabel =
            targetContext.getString(sharedR.string.meetings_list_upcoming_meeting_label)
        val recurringLabel =
            targetContext.getString(sharedR.string.meetings_list_recurring_meeting_label)
        awaitObject(By.text(upcomingLabel), LOAD_TIMEOUT, "upcoming-label")
        awaitObject(By.text(recurringLabel), LOAD_TIMEOUT, "recurring-label")
        awaitObject(By.res(RECURRING_ICON_TAG), LOAD_TIMEOUT, "recurring-icon")
        Thread.sleep(2_000)
    }

    private fun seedMeetings() {
        val now = Instant.now().epochSecond
        seedMeeting(
            chatId = FIRST_CHAT_ID,
            schedId = FIRST_SCHED_ID,
            title = UPCOMING_TITLE,
            startDateTime = now + ONE_HOUR,
            endDateTime = now + TWO_HOURS,
            rules = null,
        )
        seedMeeting(
            chatId = FIRST_CHAT_ID + 1,
            schedId = FIRST_SCHED_ID + 1,
            title = RECURRING_TITLE,
            startDateTime = now + THREE_HOURS,
            endDateTime = now + FOUR_HOURS,
            rules = StubMegaChatScheduledRules(freq = MegaChatScheduledRules.FREQ_WEEKLY),
        )
    }

    private fun seedMeeting(
        chatId: Long,
        schedId: Long,
        title: String,
        startDateTime: Long,
        endDateTime: Long,
        rules: MegaChatScheduledRules?,
    ) {
        fakeMegaChat.chatState.chatRooms[chatId] = StubMegaChatRoom(
            chatId = chatId,
            title = title,
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
            peers = listOf(PEER_HANDLE_BASE + chatId to MegaChatRoom.PRIV_STANDARD),
            isGroup = true,
            isMeeting = true,
            isActive = true,
            isArchived = false,
        )
        fakeMegaChat.chatState.chatListItems[chatId] = StubMegaChatListItem(
            chatId = chatId,
            title = title,
            lastMessage = "",
            lastTimestamp = startDateTime,
            isGroup = true,
            isActive = true,
        )
        fakeMegaChat.chatState.addScheduledMeeting(
            chatId,
            StubMegaChatScheduledMeeting(
                chatId = chatId,
                schedId = schedId,
                parentSchedId = -1L,
                title = title,
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                rules = rules,
            ),
        )
    }

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
        const val LAUNCH_TIMEOUT = 60_000L
        const val LOAD_TIMEOUT = 30_000L

        const val FIRST_CHAT_ID = 7_001L
        const val FIRST_SCHED_ID = 8_001L
        const val PEER_HANDLE_BASE = 9_001L

        const val ONE_HOUR = 3_600L
        const val TWO_HOURS = 7_200L
        const val THREE_HOURS = 10_800L
        const val FOUR_HOURS = 14_400L

        const val UPCOMING_TITLE = "Design sync"
        const val RECURRING_TITLE = "Weekly standup"

        /** Mirror ChatListContent / ChatRoomItemView test tags. */
        const val SECTION_HEADER_TAG = "chat_list_content:section_header"
        const val RECURRING_ICON_TAG = "chat_room_item:recurring_icon"
    }
}
