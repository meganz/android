package mega.privacy.android.app.presentation.meeting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.presentation.extensions.changeStatusBarColor
import mega.privacy.android.app.presentation.extensions.getEndZoneDateTime
import mega.privacy.android.app.presentation.extensions.getStartZoneDateTime
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.app.presentation.meeting.view.RecurringMeetingInfoView
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import javax.inject.Inject

/**
 * Activity which shows occurrences of recurring meeting.
 *
 * @property getFeatureFlagValueUseCase [GetFeatureFlagValueUseCase]
 * @property getThemeMode               [GetThemeMode]
 */
@AndroidEntryPoint
class RecurringMeetingInfoActivity : PasscodeActivity() {
    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    @Inject
    lateinit var getThemeMode: GetThemeMode
    private val viewModel by viewModels<RecurringMeetingInfoViewModel>()
    private val scheduledMeetingManagementViewModel by viewModels<ScheduledMeetingManagementViewModel>()
    private var materialDatePicker: MaterialDatePicker<Long>? = null
    private var materialTimePicker: MaterialTimePicker? = null

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        collectFlows()

        val chatId = intent.getLongExtra(Constants.CHAT_ID, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
        viewModel.setChatId(newChatId = chatId)
        scheduledMeetingManagementViewModel.setChatId(newChatId = chatId)

        setContent { View() }
    }


    private fun collectFlows() {
        collectFlow(viewModel.state) { (finish) ->
            if (finish) {
                Timber.d("Finish activity")
                finish()
            }


        }

        collectFlow(scheduledMeetingManagementViewModel.state) { (finish) ->
            if (finish) {
                Timber.d("Finish activity")
                finish()
            }
        }
    }

    override fun onDestroy() {
        with(scheduledMeetingManagementViewModel) {
            stopMonitoringLoadMessages()
            setOnChatIdConsumed()
            setOnChatRoomItemConsumed()
        }
        super.onDestroy()
    }

    @Composable
    private fun View() {
        val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
        val isDark = themeMode.isDarkMode()
        val uiState by viewModel.state.collectAsStateWithLifecycle()
        val managementState by scheduledMeetingManagementViewModel.state.collectAsStateWithLifecycle()
        AndroidTheme(isDark = isDark) {
            RecurringMeetingInfoView(
                state = uiState,
                managementState = managementState,
                onScrollChange = { scrolled -> this.changeStatusBarColor(scrolled, isDark) },
                onBackPressed = { finish() },
                onOccurrenceClicked = { occurrence ->
                    if (managementState.chatRoom?.ownPrivilege == ChatRoomPermission.Moderator) {
                        scheduledMeetingManagementViewModel.let {
                            it.onOccurrenceTap(occurrence)
                            managementState.chatId?.let { chatId ->
                                it.monitorLoadedMessages(chatId)
                            }
                        }
                    }
                },
                onSeeMoreClicked = { viewModel.onSeeMoreOccurrencesTap() },
                onCancelOccurrenceClicked = {
                    scheduledMeetingManagementViewModel.let {
                        if (uiState.occurrencesList.size == 1) {
                            managementState.chatId?.let { chatId ->
                                it.checkIfIsChatHistoryEmpty(chatId)
                            }
                        }
                        it.onCancelOccurrenceTap()
                    }
                },
                onEditOccurrenceClicked = { scheduledMeetingManagementViewModel.onEditOccurrenceTap() },
                onConsumeSelectOccurrenceEvent = { scheduledMeetingManagementViewModel.onConsumeSelectOccurrenceEvent() },
                onResetSnackbarMessage = scheduledMeetingManagementViewModel::onSnackbarMessageConsumed,
                onCancelOccurrence = {
                    managementState.selectedOccurrence?.let { occurrence ->
                        scheduledMeetingManagementViewModel.cancelOccurrence(occurrence)
                    }
                    onDismissDialog()
                },
                onCancelOccurrenceAndMeeting = {
                    scheduledMeetingManagementViewModel.cancelOccurrenceAndScheduledMeeting()
                    onDismissDialog()
                },
                onEditOccurrence = {
                    scheduledMeetingManagementViewModel.onUpdateScheduledMeetingOccurrenceTap()
                    onDismissDialog()
                },
                onDismissDialog = ::onDismissDialog,
                onDateTap = ::showDatePicker,
                onStartTimeTap = { showTimePicker(true) },
                onEndTimeTap = { showTimePicker(false) },
            )
        }
    }

    /**
     * Show date picker
     */
    private fun showDatePicker() {
        if (materialTimePicker != null || materialDatePicker != null)
            return

        val currentState = scheduledMeetingManagementViewModel.state.value
        currentState.editedOccurrence?.let { occurrence ->
            occurrence.getStartZoneDateTime()?.let { currentDate ->
                val dateValidator = DateValidatorPointForward.now()
                val milliseconds = currentDate.toInstant().toEpochMilli()
                val selection = milliseconds + TimeZone.getDefault().getOffset(milliseconds)

                materialDatePicker = MaterialDatePicker.Builder.datePicker()
                    .setTheme(R.style.MaterialCalendarTheme)
                    .setPositiveButtonText(getString(R.string.meetings_edit_scheduled_meeting_occurrence_dialog_confirm_button))
                    .setNegativeButtonText(getString(R.string.button_cancel))
                    .setTitleText(getString(R.string.meetings_update_scheduled_meeting_occurrence_calendar_dialog_title))
                    .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                    .setSelection(selection)
                    .setCalendarConstraints(
                        CalendarConstraints.Builder().setValidator(dateValidator).build()
                    )
                    .build()
                    .apply {
                        addOnDismissListener {
                            materialDatePicker = null
                        }
                        addOnPositiveButtonClickListener { selection ->
                            val selectedDate =
                                ZonedDateTime.from(
                                    Instant.ofEpochMilli(selection).atZone(ZoneId.systemDefault())
                                )

                            scheduledMeetingManagementViewModel.onNewStartDate(selectedDate)
                        }
                        show(supportFragmentManager, "DatePicker")
                    }
            }

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

        val currentState = scheduledMeetingManagementViewModel.state.value
        currentState.editedOccurrence?.let { occurrence ->
            val currentDate =
                if (isStart) occurrence.getStartZoneDateTime() else occurrence.getEndZoneDateTime()

            currentDate?.let { date ->
                val hourFormatter =
                    DateTimeFormatter
                        .ofPattern("HH")
                        .withZone(ZoneId.systemDefault())

                val minuteFormatter =
                    DateTimeFormatter
                        .ofPattern("mm")
                        .withZone(ZoneId.systemDefault())

                val hourText = hourFormatter.format(date)
                val minuteText = minuteFormatter.format(date)

                materialTimePicker = MaterialTimePicker.Builder()
                    .setTheme(R.style.MaterialTimerTheme)
                    .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
                    .setTimeFormat(
                        if (viewModel.is24HourFormat)
                            TimeFormat.CLOCK_24H
                        else
                            TimeFormat.CLOCK_12H
                    )
                    .setHour(hourText.toInt())
                    .setMinute(minuteText.toInt())
                    .setPositiveButtonText(getString(R.string.general_ok))
                    .setNegativeButtonText(getString(R.string.button_cancel))
                    .setTitleText(getString(R.string.meetings_schedule_meeting_enter_time_title_dialog))
                    .build()
                    .apply {
                        addOnDismissListener {
                            materialTimePicker = null
                        }
                        addOnPositiveButtonClickListener {
                            val selectedTime = currentDate.withHour(hour).withMinute(minute)
                            if (isStart) {
                                scheduledMeetingManagementViewModel.onNewStartTime(selectedTime)
                            } else {
                                scheduledMeetingManagementViewModel.onNewEndTime(selectedTime)
                            }
                        }
                        show(supportFragmentManager, "TimePicker")
                    }
            }
        }
    }

    private fun onDismissDialog() {
        scheduledMeetingManagementViewModel.let {
            it.setOnChatHistoryEmptyConsumed()
            it.onResetSelectedOccurrence()
        }
    }
}