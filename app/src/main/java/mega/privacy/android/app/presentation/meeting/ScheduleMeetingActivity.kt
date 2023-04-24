package mega.privacy.android.app.presentation.meeting

import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.presentation.extensions.changeStatusBarColor
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.model.ScheduleMeetingAction
import mega.privacy.android.app.presentation.meeting.view.ScheduleMeetingView
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Activity which shows scheduled meeting info screen.
 *
 * @property passCodeFacade [PasscodeCheck]
 * @property getThemeMode   [GetThemeMode]
 */
@AndroidEntryPoint
class ScheduleMeetingActivity : PasscodeActivity(), SnackbarShower {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<ScheduleMeetingViewModel>()

    private lateinit var addContactLauncher: ActivityResultLauncher<Intent?>

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { (openAddContact, openInfoScreen) ->
                    openInfoScreen?.let {
                        viewModel.openInfo(null)
                        openScheduleMeetingInfo(it)
                    }

                    openAddContact?.let { shouldOpen ->
                        if (shouldOpen) {
                            viewModel.removeAddContact()
                            Timber.d("Open Invite participants screen")
                            addContactLauncher.launch(
                                Intent(
                                    this@ScheduleMeetingActivity,
                                    AddContactActivity::class.java
                                )
                                    .putExtra(
                                        Constants.INTENT_EXTRA_KEY_CONTACT_TYPE,
                                        Constants.CONTACT_TYPE_MEGA
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
        }

        setContent { ScheduleMeetingComposeView() }

        addContactLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

    @Composable
    private fun ScheduleMeetingComposeView() {
        val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
        val isDark = themeMode.isDarkMode()
        val uiState by viewModel.state.collectAsState()
        AndroidTheme(isDark = isDark) {
            ScheduleMeetingView(
                state = uiState,
                onButtonClicked = ::onActionTap,
                onDiscardClicked = { viewModel.onDiscardMeetingTap() },
                onAcceptClicked = { viewModel.onScheduleMeetingTap() },
                onStartTimeClicked = { showTimePicker(true) },
                onStartDateClicked = { showDatePicker(true) },
                onEndTimeClicked = { showTimePicker(false) },
                onEndDateClicked = { showDatePicker(false) },
                onScrollChange = { scrolled -> this.changeStatusBarColor(scrolled, isDark) },
                onDismiss = { viewModel.dismissDialog() },
                onSnackbarShown = viewModel::snackbarShown,
                onDiscardMeetingDialog = { finish() },
                onDescriptionValueChange = { text -> viewModel.onDescriptionChange(text) },
                onTitleValueChange = { text -> viewModel.onTitleChange(text) },
            )
        }
    }

    /**
     * Open chat room
     *
     * @param chatId Chat id.
     */
    private fun openScheduleMeetingInfo(chatId: Long) {
        val intentOpenChat = Intent(this, ScheduledMeetingInfoActivity::class.java).apply {
            putExtra(Constants.CHAT_ID, chatId)
            putExtra(Constants.SCHEDULED_MEETING_ID, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
            putExtra(Constants.SCHEDULED_MEETING_CREATED, true)
        }
        finish()
        this.startActivity(intentOpenChat)
    }

    private fun showDatePicker(isStart: Boolean) {
        val currentState = viewModel.state.value
        val currentDate = if (isStart) currentState.startDate else currentState.endDate
        val dateValidator = if (isStart) {
            DateValidatorPointForward.now()
        } else {
            DateValidatorPointForward.from(currentState.startDate.toEpochMilli())
        }

        MaterialDatePicker.Builder.datePicker()
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .setSelection(currentDate.toEpochMilli())
            .setCalendarConstraints(
                CalendarConstraints.Builder().setValidator(dateValidator).build()
            )
            .build()
            .apply {
                addOnPositiveButtonClickListener { selection ->
                    val updatedDate = Instant.ofEpochMilli(selection)
                    if (isStart) {
                        viewModel.setStartDate(updatedDate)
                    } else {
                        viewModel.setEndDate(updatedDate)
                    }
                }
                show(supportFragmentManager, "DatePicker")
            }
    }

    private fun showTimePicker(isStart: Boolean) {
        val currentState = viewModel.state.value
        val currentDate = (if (isStart) currentState.startDate else currentState.endDate)
            .atZone(ZoneId.systemDefault())

        MaterialTimePicker.Builder()
            .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
            .setTimeFormat(
                if (DateFormat.is24HourFormat(this))
                    TimeFormat.CLOCK_24H
                else
                    TimeFormat.CLOCK_12H
            )
            .setHour(currentDate.hour)
            .setMinute(currentDate.minute)
            .build()
            .apply {
                addOnPositiveButtonClickListener {
                    val updatedDate = currentDate.withHour(hour).withMinute(minute).toInstant()
                    if (isStart) {
                        viewModel.setStartDate(updatedDate)
                    } else {
                        viewModel.setEndDate(updatedDate)
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
            ScheduleMeetingAction.Recurrence -> Timber.d("Recurrence option")
            ScheduleMeetingAction.MeetingLink -> viewModel.onMeetingLinkTap()
            ScheduleMeetingAction.AddParticipants -> viewModel.onAddParticipantsTap()
            ScheduleMeetingAction.SendCalendarInvite -> viewModel.onSendCalendarInviteTap()
            ScheduleMeetingAction.AllowNonHostAddParticipants -> viewModel.onAllowNonHostAddParticipantsTap()
            ScheduleMeetingAction.AddDescription -> viewModel.onAddDescriptionTap()
        }
    }
}
