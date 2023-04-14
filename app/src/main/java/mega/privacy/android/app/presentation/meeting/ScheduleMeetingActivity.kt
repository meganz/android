package mega.privacy.android.app.presentation.meeting

import android.content.Intent
import android.os.Bundle
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.presentation.extensions.changeStatusBarColor
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.model.ScheduleMeetingAction
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import timber.log.Timber
import javax.inject.Inject
import mega.privacy.android.app.presentation.meeting.view.ScheduleMeetingView
import mega.privacy.android.app.utils.Constants

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
                viewModel.state.collect { (_, _, _, _, _, _, _, _, _, _, _, _, openAddContact) ->
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
                onAcceptClicked = { },
                onStartTimeClicked = { },
                onStartDateClicked = { },
                onEndTimeClicked = { },
                onEndDateClicked = { },
                onScrollChange = { scrolled -> this.changeStatusBarColor(scrolled, isDark) },
                onDismiss = { viewModel.dismissDialog() },
                onSnackbarShown = viewModel::snackbarShown,
                onDiscardMeetingDialog = { finish() },
            )
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
            ScheduleMeetingAction.SendCalendarInvite -> Timber.d("Send calendar invite option")
            ScheduleMeetingAction.AllowNonHostAddParticipants -> viewModel.onAllowNonHostAddParticipantsTap()
            ScheduleMeetingAction.AddDescription -> Timber.d("Add description option")
        }
    }
}
