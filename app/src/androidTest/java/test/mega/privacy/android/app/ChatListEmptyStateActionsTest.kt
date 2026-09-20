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
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaUser
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented test for the chat list's empty-state calls to action — the parity gap that
 * shipped unnoticed because no end-to-end test followed the buttons through.
 *
 * Both empty Chats-tab variants are walked: without any MEGA contact the primary action invites a
 * friend and must open the invite-contacts screen, and with a contact it must open the new-chat
 * flow. The empty Meetings tab offers its own pair of actions instead (start now / schedule).
 *
 * Built on the `:data-test` fake-SDK framework following [ChatListFabAndEmptyStateTest]: the whole
 * app runs as in production with only the SDK gateways faked; whether the user "has any contact"
 * is decided by `MonitorHasAnyContactUseCase` reading `MegaApiGateway.getContacts`, which is
 * seeded per test. The UI is driven with UiAutomator; Compose test tags are matched as resource
 * ids because MegaActivity sets `testTagsAsResourceId = true`.
 *
 * Of the two meeting actions only "Schedule meeting" is followed through, to
 * `CreateScheduledMeetingActivity` — the screen [ScheduleMeetingTest] drives from the legacy chat
 * list, reached here from the new one. "Start meeting now" is asserted as offered only: it leaves
 * for `MeetingActivity`, which cannot run under the Hilt test application.
 */
@HiltAndroidTest
class ChatListEmptyStateActionsTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

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
        fakeFlags.set(ApiFeatures.ContactComposeFeature, true)

        // No chats seeded -> both tabs render their empty state.
        fakeMegaChat.resetToDefaults()

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
        fakeMegaApi.resetToDefaults()
    }

    @Test
    fun emptyChatsTabInviteFriendActionOpensTheInviteContactsScreen() {
        // The fake reports no contacts by default, so the empty state offers the invite action.
        launchChatList()

        awaitObject(By.res(CHAT_LIST_EMPTY_TAG), LAUNCH_TIMEOUT, "chat-empty-state")
        val inviteLabel = targetContext.getString(sharedR.string.chat_recent_invite_friend)
        awaitObject(By.text(inviteLabel), LOAD_TIMEOUT, "invite-friend-label")

        awaitObject(By.res(CHAT_LIST_EMPTY_INVITE_TAG), LOAD_TIMEOUT, "invite-friend-action").click()
        awaitObject(By.res(INVITE_CONTACTS_SCREEN_TAG), LOAD_TIMEOUT, "invite-contacts-screen")
    }

    @Test
    fun emptyChatsTabNewChatActionOpensTheNewChatFlowAndMeetingsTabOffersItsOwnActions() {
        fakeMegaApi.stub(MegaApiGateway::getContacts) { listOf(StubMegaUser(CONTACT_EMAIL, CONTACT_HANDLE)) }

        launchChatList()

        // With a contact, the empty Chats tab offers the new-chat action instead of the invite one.
        awaitObject(By.res(CHAT_LIST_EMPTY_TAG), LAUNCH_TIMEOUT, "chat-empty-state")
        awaitObject(By.res(CHAT_LIST_EMPTY_NEW_CHAT_TAG), LOAD_TIMEOUT, "new-chat-action").click()
        val pickerTitle = targetContext.getString(sharedR.string.new_chat_title)
        awaitObject(By.text(pickerTitle), LOAD_TIMEOUT, "new-chat-picker")

        // Back returns to the empty chat list, from where the Meetings tab is reachable.
        device.pressBack()
        awaitObject(By.res(CHAT_LIST_EMPTY_TAG), LOAD_TIMEOUT, "chat-empty-state-restored")

        val meetingsTab = targetContext.getString(sharedR.string.chat_tab_meetings_title)
        device.findObject(By.text(meetingsTab)).click()
        awaitObject(By.res(MEETING_LIST_EMPTY_TAG), LOAD_TIMEOUT, "meeting-empty-state")
        val meetingEmptyTitle =
            targetContext.getString(sharedR.string.meeting_recent_list_empty_title)
        awaitObject(By.text(meetingEmptyTitle), LOAD_TIMEOUT, "meeting-empty-title")
        awaitObject(By.res(MEETING_LIST_EMPTY_START_TAG), LOAD_TIMEOUT, "meeting-start-action")
        awaitObject(By.res(MEETING_LIST_EMPTY_SCHEDULE_TAG), LOAD_TIMEOUT, "meeting-schedule-action")
    }

    @Test
    fun emptyMeetingsTabScheduleActionOpensTheScheduleMeetingScreen() {
        launchChatList()

        awaitObject(By.res(CHAT_LIST_EMPTY_TAG), LAUNCH_TIMEOUT, "chat-empty-state")
        val meetingsTab = targetContext.getString(sharedR.string.chat_tab_meetings_title)
        device.findObject(By.text(meetingsTab)).click()
        awaitObject(By.res(MEETING_LIST_EMPTY_TAG), LOAD_TIMEOUT, "meeting-empty-state")

        awaitObject(By.res(MEETING_LIST_EMPTY_SCHEDULE_TAG), LOAD_TIMEOUT, "meeting-schedule-action")
            .click()
        awaitObject(By.desc(SCHEDULE_CLOSE_BUTTON_DESC), LAUNCH_TIMEOUT, "schedule-meeting-screen")

        // The schedule screen is a separate activity that confirms before closing; pop it through
        // that confirmation so the shared process is left on the chat list.
        device.findObject(By.desc(SCHEDULE_CLOSE_BUTTON_DESC)).click()
        awaitObject(By.res(DISCARD_CONFIRM_BUTTON_TAG), LOAD_TIMEOUT, "discard-meeting-dialog")
            .click()
        awaitObject(By.res(MEETING_LIST_EMPTY_TAG), LOAD_TIMEOUT, "meeting-empty-state-restored")
    }

    private fun launchChatList() {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ChatListNavKey(showMeetingTab = false)),
        )
        ActivityScenario.launch<MegaActivity>(intent)
    }

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

        const val CONTACT_EMAIL = "contact@mega.co.nz"
        const val CONTACT_HANDLE = 4_401L

        /** Mirror ChatListContent test tags. */
        const val CHAT_LIST_EMPTY_TAG = "chat_list_content:chat_empty"
        const val CHAT_LIST_EMPTY_NEW_CHAT_TAG = "chat_list_content:chat_empty_new_chat"
        const val CHAT_LIST_EMPTY_INVITE_TAG = "chat_list_content:chat_empty_invite_friend"
        const val MEETING_LIST_EMPTY_TAG = "chat_list_content:meeting_empty"
        const val MEETING_LIST_EMPTY_START_TAG = "chat_list_content:meeting_empty_start"
        const val MEETING_LIST_EMPTY_SCHEDULE_TAG = "chat_list_content:meeting_empty_schedule"

        /** Mirrors INVITE_CONTACTS_SCREEN_TAG of the feature/contact invite-contacts screen. */
        const val INVITE_CONTACTS_SCREEN_TAG = "invite_contacts_screen"

        /** Mirrors the close affordance of CreateScheduledMeetingActivity. See ScheduleMeetingTest. */
        const val SCHEDULE_CLOSE_BUTTON_DESC = "Cancel schedule meeting button"

        /** Mirrors MegaAlertDialog's confirm button, used by the discard-meeting confirmation. */
        const val DISCARD_CONFIRM_BUTTON_TAG = "mega_alert_dialog:button_confirm"
    }
}
