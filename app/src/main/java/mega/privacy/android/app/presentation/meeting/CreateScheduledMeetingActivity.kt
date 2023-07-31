package mega.privacy.android.app.presentation.meeting

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
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
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.presentation.extensions.changeStatusBarColor
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.model.ScheduleMeetingAction
import mega.privacy.android.app.presentation.meeting.view.CreateScheduledMeetingView
import mega.privacy.android.app.presentation.meeting.view.CustomRecurrenceView
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.meeting.EndsRecurrenceOption
import mega.privacy.android.domain.entity.meeting.RecurrenceDialogOption
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Activity which shows scheduled meeting info screen.
 *
 * @property passCodeFacade [PasscodeCheck]
 * @property getThemeMode   [GetThemeMode]
 */
@AndroidEntryPoint
class CreateScheduledMeetingActivity : PasscodeActivity(), SnackbarShower {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<CreateScheduledMeetingViewModel>()
    private val scheduledMeetingManagementViewModel by viewModels<ScheduledMeetingManagementViewModel>()

    private lateinit var addContactLauncher: ActivityResultLauncher<Intent?>

    private var materialTimePicker: MaterialTimePicker? = null
    private var materialDatePicker: MaterialDatePicker<Long>? = null

    internal companion object {
        const val CREATE_SCHEDULED_MEETING_TAG = "createScheduledMeetingTag"
        const val CUSTOM_RECURRENCE_TAG = "customRecurrenceTag"
    }

    private lateinit var navController: NavHostController

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        collectFlows()

        val chatId = intent.getLongExtra(
            Constants.CHAT_ID,
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
                }
            }
    }

    private fun collectFlows() {
        collectFlow(viewModel.state) { (_, openAddContact, chatIdToOpenInfoScreen, finish) ->
            if (finish) {
                finishCreateScheduledMeeting(RESULT_OK)
            }

            chatIdToOpenInfoScreen?.let {
                viewModel.setOnOpenInfoConsumed()
                Timber.d("Open Scheduled meeting info screen")
                openScheduledMeetingInfo(it)
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

        collectFlow(scheduledMeetingManagementViewModel.state) { (_, _, _, _, _, _, _, _, _, meetingLink) ->
            viewModel.updateInitialChatValues(
                !meetingLink.isNullOrEmpty()
            )
        }
    }

    private fun finishCreateScheduledMeeting(result: Int) {
        setResult(result)
        finish()
    }

    /**
     * Open compose view
     */
    @Composable
    fun MainComposeView() {
        val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val isDark = themeMode.isDarkMode()
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        navController = rememberNavController()

        AndroidTheme(isDark = isDark) {
            NavHost(
                navController = navController,
                startDestination = CREATE_SCHEDULED_MEETING_TAG
            ) {
                composable(CREATE_SCHEDULED_MEETING_TAG) {
                    CreateScheduledMeetingView(
                        state = uiState,
                        onButtonClicked = ::onActionTap,
                        onDiscardClicked = viewModel::onDiscardMeetingTap,
                        onAcceptClicked = viewModel::onScheduleMeetingTap,
                        onStartTimeClicked = { showTimePicker(true) },
                        onStartDateClicked = { showDatePicker(true) },
                        onEndTimeClicked = { showTimePicker(false) },
                        onEndDateClicked = { showDatePicker(false) },
                        onScrollChange = { scrolled ->
                            this@CreateScheduledMeetingActivity.changeStatusBarColor(
                                scrolled,
                                isDark
                            )
                        },
                        onDismiss = viewModel::dismissDialog,
                        onResetSnackbarMessage = viewModel::onSnackbarMessageConsumed,
                        onDiscardMeetingDialog = { finishCreateScheduledMeeting(RESULT_CANCELED) },
                        onDescriptionValueChange = viewModel::onDescriptionChange,
                        onTitleValueChange = viewModel::onTitleChange,
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
                        onScrollChange = { scrolled ->
                            this@CreateScheduledMeetingActivity.changeStatusBarColor(
                                scrolled,
                                isDark
                            )
                        },
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
                            showUntilDatePicker()
                        },
                        onMonthDayChanged = viewModel::onMonthDayChanged,
                    )
                }
            }
        }
    }

    /**
     * Open chat room
     *
     * @param chatId Chat id.
     */
    private fun openScheduledMeetingInfo(chatId: Long) {
        val intentOpenChat = Intent(this, ScheduledMeetingInfoActivity::class.java).apply {
            putExtra(Constants.CHAT_ID, chatId)
            putExtra(Constants.SCHEDULED_MEETING_ID, -1)
            putExtra(Constants.SCHEDULED_MEETING_CREATED, true)
        }
        finish()
        this.startActivity(intentOpenChat)
    }

    /**
     * Show until date picker
     */
    private fun showUntilDatePicker() {
        if (materialDatePicker != null)
            return

        val currentState = viewModel.state.value
        val currentDate = currentState.customRecurrenceState.endDateOccurrenceOption

        materialDatePicker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.MaterialCalendarTheme)
            .setPositiveButtonText(getString(R.string.general_ok))
            .setNegativeButtonText(getString(R.string.button_cancel))
            .setTitleText(getString(R.string.meetings_schedule_meeting_calendar_select_date_label))
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .setSelection(currentDate.toInstant().toEpochMilli())
            .setCalendarConstraints(
                CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now()).build()
            )
            .build()
            .apply {
                addOnDismissListener {
                    materialDatePicker = null
                }
                addOnPositiveButtonClickListener { selection ->
                    viewModel.onUntilDateTap(
                        Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC)
                    )
                }
                show(supportFragmentManager, "DatePicker")
            }
    }

    /**
     * Show date picker
     *
     * @param isStart
     */
    private fun showDatePicker(isStart: Boolean) {
        if (materialTimePicker != null || materialDatePicker != null)
            return

        val currentState = viewModel.state.value
        val currentDate = if (isStart) currentState.startDate else currentState.endDate
        val dateValidator = if (isStart) {
            DateValidatorPointForward.now()
        } else {
            DateValidatorPointForward.from(currentState.startDate.toInstant().toEpochMilli())
        }

        materialDatePicker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.MaterialCalendarTheme)
            .setPositiveButtonText(getString(R.string.general_ok))
            .setNegativeButtonText(getString(R.string.button_cancel))
            .setTitleText(getString(R.string.meetings_schedule_meeting_calendar_select_date_label))
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .setSelection(currentDate.toInstant().toEpochMilli())
            .setCalendarConstraints(
                CalendarConstraints.Builder().setValidator(dateValidator).build()
            )
            .build()
            .apply {
                addOnDismissListener {
                    materialDatePicker = null
                }
                addOnPositiveButtonClickListener { selection ->
                    val selectedDate = Instant.ofEpochMilli(selection)
                        .atZone(ZoneOffset.UTC)
                        .withHour(currentDate.hour)
                        .withMinute(currentDate.minute)

                    if (isStart) {
                        viewModel.onStartDateTimeTap(selectedDate)
                    } else {
                        viewModel.onEndDateTimeTap(selectedDate)
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
            .setPositiveButtonText(getString(R.string.general_ok))
            .setNegativeButtonText(getString(R.string.button_cancel))
            .setTitleText(getString(R.string.meetings_schedule_meeting_enter_time_title_dialog))
            .build()
            .apply {
                addOnDismissListener {
                    materialTimePicker = null
                }
                addOnPositiveButtonClickListener {
                    val selectedTime = localTime
                        .withHour(hour)
                        .withMinute(minute)
                        .withZoneSameInstant(ZoneOffset.UTC)
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
            ScheduleMeetingAction.Recurrence -> viewModel.onRecurrenceTap()
            ScheduleMeetingAction.EndRecurrence -> {
                viewModel.setInitialCustomRules()
                navController.navigate(CUSTOM_RECURRENCE_TAG)
            }

            ScheduleMeetingAction.MeetingLink -> viewModel.onMeetingLinkTap()
            ScheduleMeetingAction.AddParticipants -> viewModel.onAddParticipantsTap()
            ScheduleMeetingAction.SendCalendarInvite -> viewModel.onSendCalendarInviteTap()
            ScheduleMeetingAction.AllowNonHostAddParticipants -> viewModel.onAllowNonHostAddParticipantsTap()
            ScheduleMeetingAction.AddDescription -> viewModel.onAddDescriptionTap()
        }
    }
}