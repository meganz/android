package mega.privacy.android.app.presentation.meeting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.extensions.changeStatusBarColor
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

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { (finish) ->
                    if (finish) {
                        Timber.d("Finish activity")
                        finish()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                scheduledMeetingManagementViewModel.state.collect { (finish) ->
                    if (finish) {
                        Timber.d("Finish activity")
                        finish()
                    }
                }
            }
        }

        val chatId = intent.getLongExtra(Constants.CHAT_ID, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
        viewModel.setChatId(newChatId = chatId)
        scheduledMeetingManagementViewModel.setChatId(newChatId = chatId)

        setContent { View() }
    }

    override fun onDestroy() {
        scheduledMeetingManagementViewModel.let {
            it.stopMonitoringLoadMessages()
            it.setOnChatIdConsumed()
        }
        super.onDestroy()
    }

    @Composable
    private fun View() {
        val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
        val isDark = themeMode.isDarkMode()
        val uiState by viewModel.state.collectAsState()
        val managementState by scheduledMeetingManagementViewModel.state.collectAsState()
        val isCancelSchedMeetingEnabled = runBlocking {
            getFeatureFlagValueUseCase(AppFeatures.CancelSchedMeeting)
        }
        AndroidTheme(isDark = isDark) {
            RecurringMeetingInfoView(
                state = uiState,
                managementState = managementState,
                onScrollChange = { scrolled -> this.changeStatusBarColor(scrolled, isDark) },
                onBackPressed = { finish() },
                onOccurrenceClicked = { occurrence ->
                    if (managementState.chatRoom?.ownPrivilege == ChatRoomPermission.Moderator && isCancelSchedMeetingEnabled) {
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
                onConsumeSelectOccurrenceEvent = { scheduledMeetingManagementViewModel.onConsumeSelectOccurrenceEvent() },
                onResetSnackbarMessage = scheduledMeetingManagementViewModel::onSnackbarMessageConsumed,
                onCancelOccurrence = {
                    managementState.selectedOccurrence?.let { occurrence ->
                        scheduledMeetingManagementViewModel.cancelOccurrence(occurrence)
                    }
                    onDismissDialog()
                },
                onCancelOccurrenceAndMeeting = {
                    managementState.selectedOccurrence?.let { occurrence ->
                        scheduledMeetingManagementViewModel.cancelOccurrenceAndArchiveMeeting(
                            occurrence
                        )
                    }
                    onDismissDialog()
                },
                onDismissDialog = ::onDismissDialog,
            )
        }
    }

    private fun onDismissDialog() {
        scheduledMeetingManagementViewModel.let {
            it.setOnChatHistoryEmptyConsumed()
            it.onResetSelectedOccurrence()
        }
    }
}