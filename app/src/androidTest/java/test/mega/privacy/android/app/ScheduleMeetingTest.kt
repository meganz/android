package mega.privacy.android.app

import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.navigation.destination.ChatListNavKey
import nz.mega.sdk.MegaChatScheduledRules
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Full-app instrumented port of the AAT smoke scenario sm16 tc04 "Schedule a meeting"
 * (TestRail 997113).
 *
 * The journey is the production one: the chat list opens on the Meetings tab, its empty state
 * offers "Schedule meeting", and the resulting `CreateScheduledMeetingActivity` is asserted row by
 * row — close and save actions, title field, start and end date-time, recurrence (defaulting to
 * "Never"), meeting link, add participants, send calendar invitation, waiting room, allow
 * non-hosts and add description. A title is typed, the recurrence dialogue is opened and asserted,
 * "every day" is picked, the recurrence summary becomes "Daily", and save is confirmed to reach the
 * SDK as a `createChatroomAndSchedMeeting` carrying that title and a daily rule.
 *
 * Built on the `:data-test` fake-SDK framework following [ChatListMeetingsDecorationsTest]: the
 * whole app runs as in production with only the SDK gateways faked, and the UI is driven with
 * UiAutomator.
 *
 * The AAT tail — the created meeting appearing in the Meetings tab with a recurring icon and a
 * "daily" subtitle — is **not** reproduced. The fake's `createChatroomAndSchedMeeting` records the
 * call and completes it successfully, but it creates neither a chat room nor a scheduled meeting in
 * `FakeChatState`, so nothing can come back through `getScheduledMeetingsByChat`. Reproducing it
 * needs the fake to materialise a room, list item and scheduled meeting from the create arguments
 * and to return the new chat handle on the request.
 */
@HiltAndroidTest
class ScheduleMeetingTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

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

    private var scenario: ActivityScenario<MegaActivity>? = null

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

        fakeMegaChat.resetToDefaults()
        fakeMegaChat.clearInvocations()

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            setCustomiseNavigationTooltipShownUseCase()
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        // Pop the schedule-meeting activity and any dialog it left so tests stay isolated in the
        // shared process.
        repeat(4) { runCatching { device.pressBack() } }
        fakeMegaChat.resetToDefaults()
    }

    @Test
    fun scheduleADailyMeetingFromTheMeetingsTab() {
        openMeetingsTab()

        click(
            By.text(string(R.string.chat_schedule_meeting)),
            LOAD_TIMEOUT,
            "schedule-meeting-button",
        )

        awaitObject(By.desc(CLOSE_BUTTON_DESC), LOAD_TIMEOUT, "schedule-meeting-screen")
        hideKeyboard()

        assertScreenRows()

        setMeetingTitle(MEETING_TITLE)
        hideKeyboard()

        assertRecurrenceDialogue()

        click(By.res(recurrenceOptionTag(EVERY_DAY_OPTION)), LOAD_TIMEOUT, "every-day-option")
        awaitLabel(dailySummary(), "recurrence-summary-daily")

        click(By.desc(SAVE_BUTTON_DESC), LOAD_TIMEOUT, "save-button")
        assertMeetingCreated()
    }

    /**
     * Opens the chat list on the Meetings tab through the production intent seam. With
     * `CustomisableBottomNavigation` off — its shipped default — the destination hands over to the
     * legacy chat list, which is the surface carrying the "Schedule meeting" empty-state action.
     */
    private fun openMeetingsTab() {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ChatListNavKey(showMeetingTab = true)),
        )
        scenario = ActivityScenario.launch(intent)
        awaitObject(
            By.text(string(R.string.chat_schedule_meeting)),
            LAUNCH_TIMEOUT,
            "meetings-tab-empty-state",
        )
    }

    private fun assertScreenRows() {
        awaitObject(By.desc(SAVE_BUTTON_DESC), LOAD_TIMEOUT, "save-button")
        awaitLabel(string(R.string.meetings_schedule_meeting_name_hint), "meeting-title-placeholder")

        assertDateAndTimeRows()

        assertActionRow(
            iconDescription = "Recurrence icon",
            title = string(R.string.meetings_schedule_meeting_recurrence_label),
            subtitle = string(R.string.meetings_schedule_meeting_recurrence_never_label),
        )
        assertActionRow(
            iconDescription = "MeetingLink icon",
            title = string(R.string.meeting_link),
            subtitle = string(R.string.scheduled_meetings_share_meeting_link_panel_title),
        )
        assertActionRow(
            iconDescription = "AddParticipants icon",
            title = string(R.string.add_participants_menu_item),
            subtitle = null,
        )
        assertActionRow(
            iconDescription = "SendCalendarInvite icon",
            title = string(R.string.meetings_schedule_meeting_send_calendar_invite_label),
            subtitle = string(R.string.scheduled_meetings_send_calendar_invite_panel_title),
        )
        assertActionRow(
            iconDescription = "WaitingRoom icon",
            title = string(R.string.meetings_schedule_meeting_waiting_room_label),
            subtitle = string(R.string.meetings_schedule_meeting_waiting_room_description),
        )
        assertActionRow(
            iconDescription = "AllowNonHostAddParticipants icon",
            title = string(R.string.chat_group_chat_info_allow_non_host_participants_option),
            subtitle = null,
        )
        assertActionRow(
            iconDescription = "AddDescription icon",
            title = string(R.string.meetings_schedule_meeting_add_description_label),
            subtitle = null,
        )
    }

    /**
     * The start and end rows each render a date and a time, formatted in the device locale and
     * zone. Asserting the shape rather than the rendered instant keeps the test out of the
     * locale / timezone / clock-rollover flakiness that absolute dates invite.
     */
    private fun assertDateAndTimeRows() {
        // Scoped to the app package so the status-bar clock does not count as a meeting time.
        val dates = By.pkg(targetContext.packageName).text(DATE_PATTERN)
        val times = By.pkg(targetContext.packageName).text(TIME_PATTERN)
        awaitObject(dates, LOAD_TIMEOUT, "meeting-dates")
        awaitObject(times, LOAD_TIMEOUT, "meeting-times")
        assertThat(device.findObjects(dates)).hasSize(2)
        assertThat(device.findObjects(times)).hasSize(2)
    }

    private fun assertActionRow(iconDescription: String, title: String, subtitle: String?) {
        scrollToLabel(title)
        awaitLabel(iconDescription, "row-icon-$iconDescription")
        awaitLabel(title, "row-title-$title")
        subtitle?.let { awaitLabel(it, "row-subtitle-$title") }
    }

    private fun assertRecurrenceDialogue() {
        scrollToLabel(string(R.string.meetings_schedule_meeting_recurrence_label))
        click(
            By.textContains(string(R.string.meetings_schedule_meeting_recurrence_label)),
            LOAD_TIMEOUT,
            "recurrence-row",
        )

        awaitObject(By.res(DIALOG_TITLE_TAG), LOAD_TIMEOUT, "recurrence-dialogue")
        listOf(NEVER_OPTION, EVERY_DAY_OPTION, EVERY_WEEK_OPTION, EVERY_MONTH_OPTION, CUSTOM_OPTION)
            .forEach { option ->
                awaitObject(By.res(recurrenceOptionTag(option)), LOAD_TIMEOUT, "option-$option")
            }
        awaitObject(By.res(DIALOG_CANCEL_BUTTON_TAG), LOAD_TIMEOUT, "recurrence-dialogue-cancel")
    }

    private fun assertMeetingCreated() {
        val invocations = awaitInvocations("createChatroomAndSchedMeeting") {
            fakeMegaChat.invocationsOf(MegaChatApiGateway::createChatroomAndSchedMeeting)
        }
        assertThat(invocations).hasSize(1)

        val arguments = invocations.single().arguments
        assertThat(arguments[ARG_IS_MEETING]).isEqualTo(true)
        assertThat(arguments[ARG_PUBLIC_CHAT]).isEqualTo(true)
        assertThat(arguments[ARG_TITLE]).isEqualTo(MEETING_TITLE)

        val rules = arguments[ARG_RULES] as MegaChatScheduledRules
        assertThat(rules.freq()).isEqualTo(MegaChatScheduledRules.FREQ_DAILY)
        assertThat(rules.interval()).isEqualTo(1)
    }

    private fun setMeetingTitle(title: String) {
        awaitObject(By.clazz(EDIT_TEXT_CLASS), LOAD_TIMEOUT, "meeting-title-field")
        device.findObject(By.clazz(EDIT_TEXT_CLASS)).text = title
        awaitLabel(title, "meeting-title-typed")
    }

    /**
     * The title field takes initial focus on creation, so the IME covers most of the form. Back is
     * consumed by the IME while it is up; the guard covers the case where it is already down and
     * back reaches the screen's discard handler instead.
     */
    private fun hideKeyboard() {
        device.wait(Until.hasObject(By.clazz(EDIT_TEXT_CLASS).focused(true)), LOAD_TIMEOUT)
        Thread.sleep(IME_SETTLE_MS)
        device.pressBack()
        val discardTitle =
            By.text(string(R.string.meetings_schedule_meeting_discard_meeting_dialog_title))
        if (device.wait(Until.hasObject(discardTitle), SHORT_TIMEOUT)) {
            click(
                By.text(string(R.string.meetings_schedule_meeting_discard_meeting_dialog_keep_editing_option)),
                LOAD_TIMEOUT,
                "keep-editing",
            )
        }
    }

    /**
     * Every row on this screen is a single clickable node, so Compose merges its icon description
     * and its texts into one accessibility node whose content lands in either `text` or
     * `contentDescription` depending on what the row carries. Matching on both, as a substring,
     * keeps the assertions readable without depending on how a given row happens to merge.
     */
    private fun awaitLabel(label: String, name: String) {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            if (hasLabel(label)) return
            Thread.sleep(POLL_INTERVAL_MS)
        }
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name (\"$label\"); hierarchy in logcat tag UiDump",
        )
    }

    private fun hasLabel(label: String): Boolean =
        device.hasObject(By.textContains(label)) || device.hasObject(By.descContains(label))

    private fun scrollToLabel(label: String) {
        if (hasLabel(label)) return
        runCatching {
            UiScrollable(UiSelector().scrollable(true))
                .apply { setMaxSearchSwipes(MAX_SCROLL_SWIPES) }
                .scrollIntoView(UiSelector().textContains(label))
        }
    }

    /**
     * Gateway commands are dispatched from `viewModelScope`, so they are not recorded synchronously
     * after the tap. Poll [supplier] until at least one match, or fail with what was recorded.
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
                    "recorded chat methods=${fakeMegaChat.invocations.map { it.methodName }}",
        )
    }

    private fun click(selector: BySelector, timeout: Long, name: String) {
        awaitObject(selector, timeout, name)
        device.findObject(selector).click()
    }

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

    private fun string(id: Int): String = targetContext.getString(id)

    private fun dailySummary(): String = targetContext.resources.getQuantityString(
        R.plurals.meetings_schedule_meeting_recurrence_every_number_of_days_label,
        1,
        1,
    )

    private fun recurrenceOptionTag(option: String) = "$DIALOG_OPTION_TAG_PREFIX$option"

    private companion object {
        const val LAUNCH_TIMEOUT = 60_000L
        const val LOAD_TIMEOUT = 20_000L
        const val SHORT_TIMEOUT = 2_000L
        const val IME_SETTLE_MS = 1_000L
        const val POLL_INTERVAL_MS = 100L
        const val MAX_SCROLL_SWIPES = 12

        const val MEETING_TITLE = "sm16tc04meeting"

        const val EDIT_TEXT_CLASS = "android.widget.EditText"

        /** Mirror the app-bar icon descriptions in CreateScheduledMeetingView. */
        const val CLOSE_BUTTON_DESC = "Cancel schedule meeting button"
        const val SAVE_BUTTON_DESC = "Accept schedule meeting button"

        /** Mirror ConfirmationDialogWithRadioButtons test tags. */
        const val DIALOG_TITLE_TAG = "confirmation_dialog_with_radio_buttons:text_title"
        const val DIALOG_CANCEL_BUTTON_TAG = "confirmation_dialog_with_radio_buttons:button_cancel"
        const val DIALOG_OPTION_TAG_PREFIX = "confirmation_dialog_with_radio_buttons:text_option_"

        /** Mirror RecurrenceDialogOption entry names, which the option tags are built from. */
        const val NEVER_OPTION = "Never"
        const val EVERY_DAY_OPTION = "EveryDay"
        const val EVERY_WEEK_OPTION = "EveryWeek"
        const val EVERY_MONTH_OPTION = "EveryMonth"
        const val CUSTOM_OPTION = "Custom"

        /** Positions in the recorded `createChatroomAndSchedMeeting` argument list. */
        const val ARG_IS_MEETING = 1
        const val ARG_PUBLIC_CHAT = 2
        const val ARG_TITLE = 3
        const val ARG_RULES = 12

        /** "E',' d MMM',' yyyy" and "HH:mm" / "hh:mma", as formatted by CreateScheduledMeetingView. */
        val DATE_PATTERN: Pattern = Pattern.compile("[\\p{L}.]+, \\d{1,2} [\\p{L}.]+, \\d{4}")
        val TIME_PATTERN: Pattern = Pattern.compile("\\d{1,2}:\\d{2} ?([AaPp][Mm])?")
    }
}
