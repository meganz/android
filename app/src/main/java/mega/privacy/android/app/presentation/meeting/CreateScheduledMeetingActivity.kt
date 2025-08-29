package mega.privacy.android.app.presentation.meeting

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingActivity.DatePickerType.END_DATE
import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingActivity.DatePickerType.START_DATE
import mega.privacy.android.app.presentation.meeting.CreateScheduledMeetingActivity.DatePickerType.UNTIL
import mega.privacy.android.app.presentation.meeting.model.ScheduleMeetingAction
import mega.privacy.android.app.presentation.meeting.view.CreateScheduledMeetingView
import mega.privacy.android.app.presentation.meeting.view.CustomRecurrenceView
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CHAT_ID
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.meeting.EndsRecurrenceOption
import mega.privacy.android.domain.entity.meeting.RecurrenceDialogOption
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingType
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.CreateMeetingMaxDurationReachedEvent
import mega.privacy.mobile.analytics.event.EditMeetingMaxDurationReachedEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingCreateConfirmButtonEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingSettingEnableMeetingLinkButtonEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingSettingEnableOpenInviteButtonEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingSettingRecurrenceButtonEvent
import mega.privacy.mobile.analytics.event.ScheduledMeetingSettingSendCalendarInviteButtonEvent
import mega.privacy.mobile.analytics.event.WaitingRoomEnableButtonEvent
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * Activity which shows scheduled meeting info screen.
 *
 * @property passCodeFacade         [PasscodeCheck]
 * @property monitorThemeModeUseCase           [MonitorThemeModeUseCase]
 */
@AndroidEntryPoint
class CreateScheduledMeetingActivity : PasscodeActivity(), SnackbarShower {

    private enum class DatePickerType { START_DATE, END_DATE, UNTIL }

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var megaNavigator: MegaNavigator

    private val viewModel by viewModels<CreateScheduledMeetingViewModel>()
    private val scheduledMeetingManagementViewModel by viewModels<ScheduledMeetingManagementViewModel>()

    private lateinit var addContactLauncher: ActivityResultLauncher<Intent>

    private var materialTimePicker: MaterialTimePicker? = null
    private var materialDatePicker: MaterialDatePicker<Long>? = null

    internal companion object {
        const val CREATE_SCHEDULED_MEETING_TAG = "createScheduledMeetingTag"
        const val CUSTOM_RECURRENCE_TAG = "customRecurrenceTag"
        const val MEETING_LINK_CREATED_TAG = "meetingLinkCreatedTag"
        const val MEETING_LINK_TAG = "meetingLinkTag"
        const val MEETING_TITLE_TAG = "meetingTitleTag"
    }

    private lateinit var navController: NavHostController

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainerWrapper.setPasscodeCheck(passCodeFacade)
        enableEdgeToEdge()

        collectFlows()

        val chatId = intent.getLongExtra(
            CHAT_ID,
            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
        )

        viewModel.getChatRoom(chatId = chatId)
        scheduledMeetingManagementViewModel.setChatId(newChatId = chatId)

        setContent { MainComposeView() }

        addContactLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                viewModel.allowAddParticipantsOption()

                if (result.resultCode == RESULT_OK) {
                    result.data?.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
                        ?.let { contactsData ->
                            viewModel.addContactsSelected(contactsData)
                        }
                } else {
                    Timber.e("Error adding participants")
                    viewModel.setOnAddingParticipantsConsumed()
                }
            }
    }

    private fun collectFlows() {
        collectFlow(viewModel.state) { (_, openAddContact, _, finish) ->
            if (finish) {
                finishCreateScheduledMeeting(RESULT_OK)
            }

            openAddContact?.let { shouldOpen ->
                if (shouldOpen) {
                    viewModel.setOnOpenAddContactConsumed()
                    Timber.d("Open Invite participants screen")
                    addContactLauncher.launch(
                        Intent(
                            this@CreateScheduledMeetingActivity,
                            AddContactActivity::class.java
                        )
                            .putExtra(
                                Constants.INTENT_EXTRA_KEY_CONTACT_TYPE,
                                Constants.CONTACT_TYPE_MEGA
                            )
                            .putStringArrayListExtra(
                                Constants.INTENT_EXTRA_KEY_CONTACTS_SELECTED,
                                viewModel.getEmails()
                            )
                            .putExtra(Constants.INTENT_EXTRA_KEY_CHAT, true)
                            .putExtra(
                                Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE,
                                getString(R.string.add_participants_menu_item)
                            )
                    )
                }
            }
        }
    }

    /**
     * Finish create scheduled meeting with result
     *
     * @param result    Result: RESULT_CANCELED or RESULT_OK
     */
    private fun finishCreateScheduledMeeting(result: Int) {
        setResult(
            result,
            Intent().apply {
                putExtra(
                    CHAT_ID,
                    viewModel.state.value.chatIdToOpenInfoScreen
                )
                Timber.d("Chat id ${viewModel.state.value.chatIdToOpenInfoScreen}")
                putExtra(MEETING_TITLE_TAG, viewModel.state.value.meetingTitle)
                Timber.d("Meeting title ${viewModel.state.value.meetingTitle}")
                putExtra(
                    MEETING_LINK_CREATED_TAG,
                    viewModel.state.value.meetingLink?.isNotBlank() == true
                )
                putExtra(
                    MEETING_LINK_TAG,
                    viewModel.state.value.meetingLink
                )
                Timber.d("Meeting link ${viewModel.state.value.meetingLink}")
            }
        )
        finish()
    }

    /**
     * Open compose view
     */
    @Composable
    fun MainComposeView() {
        val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val isDark = themeMode.isDarkMode()
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        val managementState by scheduledMeetingManagementViewModel.state.collectAsStateWithLifecycle()
        navController = rememberNavController()

        OriginalTheme(isDark = isDark) {
            NavHost(
                navController = navController,
                startDestination = CREATE_SCHEDULED_MEETING_TAG
            ) {
                composable(CREATE_SCHEDULED_MEETING_TAG) {
                    CreateScheduledMeetingView(
                        state = uiState,
                        managementState = managementState,
                        onButtonClicked = ::onActionTap,
                        onDiscardClicked = viewModel::onDiscardMeetingTap,
                        onAcceptClicked = ::onScheduleMeetingTap,
                        onStartTimeClicked = { showTimePicker(true) },
                        onStartDateClicked = { showDatePicker(START_DATE) },
                        onEndTimeClicked = { showTimePicker(false) },
                        onEndDateClicked = { showDatePicker(END_DATE) },
                        onDismiss = viewModel::dismissDialog,
                        onResetSnackbarMessage = viewModel::onSnackbarMessageConsumed,
                        onDiscardMeetingDialog = { finishCreateScheduledMeeting(RESULT_CANCELED) },
                        onDescriptionValueChange = viewModel::onDescriptionChange,
                        onTitleValueChange = viewModel::onTitleChange,
                        onCloseWarningClicked = scheduledMeetingManagementViewModel::closeWaitingRoomWarning,
                        onUpgradeNowClicked = ::openUpgradeAccount,
                        onRecurrenceDialogOptionClicked = { optionSelected ->
                            viewModel.dismissDialog()
                            when {
                                optionSelected == RecurrenceDialogOption.Custom -> {
                                    viewModel.setInitialCustomRules()
                                    navController.navigate(CUSTOM_RECURRENCE_TAG)
                                }

                                optionSelected != RecurrenceDialogOption.Customised -> {
                                    viewModel.onDefaultRecurrenceOptionTap(optionSelected)
                                }
                            }
                        }
                    )
                }
                composable(CUSTOM_RECURRENCE_TAG) {
                    CustomRecurrenceView(
                        state = uiState,
                        onAcceptClicked = {
                            viewModel.onAcceptClicked()
                            navController.navigate(CREATE_SCHEDULED_MEETING_TAG)
                        },
                        onRejectClicked = {
                            viewModel.onRejectClicked()
                            navController.navigate(CREATE_SCHEDULED_MEETING_TAG)
                        },
                        onFrequencyTypeChanged = viewModel::onFrequencyTypeChanged,
                        onIntervalChanged = viewModel::onIntervalChanged,
                        onFocusChanged = viewModel::onFocusChanged,
                        onWeekdaysClicked = viewModel::onWeekdaysOptionTap,
                        onDayClicked = viewModel::onDayClicked,
                        onMonthlyRadioButtonClicked = viewModel::onMonthlyRadioButtonClicked,
                        onEndsRadioButtonClicked = viewModel::onEndsRadioButtonClicked,
                        onMonthWeekDayChanged = viewModel::onMonthWeekDayChanged,
                        onWeekOfMonthChanged = viewModel::onWeekOfMonthChanged,
                        onDateClicked = {
                            viewModel.onEndsRadioButtonClicked(EndsRecurrenceOption.CustomDate)
                            showDatePicker(UNTIL)
                        },
                        onMonthDayChanged = viewModel::onMonthDayChanged,
                    )
                }
            }
        }
    }

    /**
     * Tap on Schedule meeting option
     */
    private fun onScheduleMeetingTap() {
        Analytics.tracker.trackEvent(ScheduledMeetingCreateConfirmButtonEvent)
        viewModel.onScheduleMeetingTap()
    }

    /**
     * Open upgrade account screen
     */
    private fun openUpgradeAccount() {
        Analytics.tracker.trackEvent(if (viewModel.state.value.type == ScheduledMeetingType.Edition) EditMeetingMaxDurationReachedEvent else CreateMeetingMaxDurationReachedEvent)
        megaNavigator.openUpgradeAccount(context = this,)
    }

    /**
     * Show date picker dialog
     *
     * @param type  [DatePickerType]
     */
    private fun showDatePicker(type: DatePickerType) {
        if (materialTimePicker != null || materialDatePicker != null)
            return

        val currentState = viewModel.state.value
        val currentDate = when (type) {
            START_DATE -> currentState.startDate
            END_DATE -> currentState.endDate
            UNTIL -> currentState.customRecurrenceState.endDateOccurrenceOption
        }

        val dateValidator = if (type == END_DATE) {
            DateValidatorPointForward.from(currentState.startDate.getTruncatedUtcTimeInMillis())
        } else {
            DateValidatorPointForward.now()
        }

        materialDatePicker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.MaterialCalendarTheme)
            .setPositiveButtonText(
                getString(
                    if (currentState.type == ScheduledMeetingType.Creation)
                        sharedResR.string.general_ok
                    else
                        R.string.meetings_edit_scheduled_meeting_occurrence_dialog_confirm_button
                )
            )
            .setNegativeButtonText(getString(sharedR.string.general_dialog_cancel_button))
            .setTitleText(getString(R.string.meetings_schedule_meeting_calendar_select_date_label))
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .setSelection(currentDate.getTruncatedUtcTimeInMillis())
            .setCalendarConstraints(
                CalendarConstraints.Builder().setValidator(dateValidator).build()
            )
            .build()
            .apply {
                addOnDismissListener { materialDatePicker = null }
                addOnPositiveButtonClickListener { selection ->
                    val selectedDate = Instant.ofEpochMilli(selection)
                        .atZone(ZoneId.systemDefault())
                        .withHour(currentDate.hour)
                        .withMinute(currentDate.minute)

                    Timber.d("Selected until date $selectedDate")

                    when (type) {
                        START_DATE -> viewModel.onStartDateTimeTap(selectedDate)
                        END_DATE -> viewModel.onEndDateTimeTap(selectedDate)
                        UNTIL -> viewModel.onUntilDateTap(selectedDate)
                    }
                }
                show(supportFragmentManager, "DatePicker")
            }
    }

    /**
     * Show time picker
     *
     * @param isStart
     */
    private fun showTimePicker(isStart: Boolean) {
        if (materialTimePicker != null || materialDatePicker != null)
            return

        val currentState = viewModel.state.value
        val currentDate = if (isStart) currentState.startDate else currentState.endDate

        val localTime = currentDate.withZoneSameInstant(ZoneId.systemDefault())

        materialTimePicker = MaterialTimePicker.Builder()
            .setTheme(R.style.MaterialTimerTheme)
            .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
            .setHour(localTime.hour)
            .setMinute(localTime.minute)
            .setTimeFormat(
                if (viewModel.is24HourFormat)
                    TimeFormat.CLOCK_24H
                else
                    TimeFormat.CLOCK_12H
            )
            .setPositiveButtonText(getString(sharedResR.string.general_ok))
            .setNegativeButtonText(getString(sharedR.string.general_dialog_cancel_button))
            .setTitleText(getString(R.string.meetings_schedule_meeting_enter_time_title_dialog))
            .build()
            .apply {
                addOnDismissListener {
                    materialTimePicker = null
                }
                addOnPositiveButtonClickListener {
                    var selectedTime = localTime
                        .withHour(hour)
                        .withMinute(minute)
                    Timber.d("Selected time in picker $selectedTime")

                    if (currentState.type == ScheduledMeetingType.Edition) {
                        selectedTime = selectedTime.withZoneSameLocal(localTime.zone)
                    }
                    Timber.d("Selected time $selectedTime")

                    if (isStart) {
                        viewModel.onStartDateTimeTap(selectedTime)
                    } else {
                        viewModel.onEndDateTimeTap(selectedTime)
                    }
                }
                show(supportFragmentManager, "TimePicker")
            }
    }

    /**
     * Tap in a button action
     */
    private fun onActionTap(action: ScheduleMeetingAction) {
        when (action) {
            ScheduleMeetingAction.Recurrence -> onRecurrenceTap()
            ScheduleMeetingAction.EndRecurrence -> {
                viewModel.setInitialCustomRules()
                navController.navigate(CUSTOM_RECURRENCE_TAG)
            }

            ScheduleMeetingAction.MeetingLink -> onMeetingLinkTap()
            ScheduleMeetingAction.AddParticipants -> viewModel.onAddParticipantsTap()
            ScheduleMeetingAction.SendCalendarInvite -> onSendCalendarInviteTap()
            ScheduleMeetingAction.AllowNonHostAddParticipants -> onAllowNonHostAddParticipantsTap()
            ScheduleMeetingAction.AddDescription -> viewModel.onAddDescriptionTap()
            ScheduleMeetingAction.WaitingRoom -> onWaitingRoomTap()
        }
    }

    /**
     * Tap recurring meeting button
     */
    private fun onRecurrenceTap() {
        Analytics.tracker.trackEvent(ScheduledMeetingSettingRecurrenceButtonEvent)
        viewModel.onRecurrenceTap()
    }

    /**
     * Tap to enable or disable meeting link option
     */
    private fun onMeetingLinkTap() {
        if (!viewModel.state.value.enabledMeetingLinkOption) {
            Analytics.tracker.trackEvent(ScheduledMeetingSettingEnableMeetingLinkButtonEvent)
        }
        viewModel.onMeetingLinkTap()
    }

    /**
     * Tap to enable or disable send calendar invite option
     */
    private fun onSendCalendarInviteTap() {
        if (!viewModel.state.value.enabledSendCalendarInviteOption) {
            Analytics.tracker.trackEvent(ScheduledMeetingSettingSendCalendarInviteButtonEvent)
        }
        viewModel.onSendCalendarInviteTap()
    }

    /**
     * Tap to enable or disable allow non-hosts to add participants option
     */
    private fun onAllowNonHostAddParticipantsTap() {
        Analytics.tracker.trackEvent(ScheduledMeetingSettingEnableOpenInviteButtonEvent)
        viewModel.onAllowNonHostAddParticipantsTap()
    }

    /**
     * Tap to enable or disable waiting room option
     */
    private fun onWaitingRoomTap() {
        if (!viewModel.state.value.enabledWaitingRoomOption) {
            Analytics.tracker.trackEvent(WaitingRoomEnableButtonEvent)
        }
        viewModel.onWaitingRoomTap()
    }

    /**
     * Given a [ZonedDateTime], get truncated UTC time in millis required for [MaterialDatePicker]
     *
     * @return  Epoch time in millis
     */
    private fun ZonedDateTime.getTruncatedUtcTimeInMillis(): Long = toLocalDateTime()
        .truncatedTo(ChronoUnit.DAYS)
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli()
}
