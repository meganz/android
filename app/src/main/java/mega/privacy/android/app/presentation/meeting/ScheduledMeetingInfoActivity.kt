package mega.privacy.android.app.presentation.meeting

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import mega.privacy.android.app.presentation.extensions.changeStatusBarColor
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoAction
import mega.privacy.android.app.presentation.meeting.view.ScheduledMeetingInfoView
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.presentation.theme.AndroidTheme
import javax.inject.Inject

/**
 * Activity which shows scheduled meeting info screen.
 *
 * @property passCodeFacade [PasscodeCheck]
 * @property getThemeMode   [GetThemeMode]
 */
@AndroidEntryPoint
class ScheduledMeetingInfoActivity : ComponentActivity() {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<ScheduledMeetingInfoViewModel>()

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect {
                }
            }
        }

        setContent { ScheduledMeetingInfoView() }
    }

    @Composable
    private fun ScheduledMeetingInfoView() {
        val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
        val isDark = themeMode.isDarkMode()
        val uiState by viewModel.state.collectAsState()

        AndroidTheme(isDark = isDark) {
            ScheduledMeetingInfoView(
                state = uiState,
                onButtonClicked = ::onActionTap,
                onEditClicked = viewModel::onEditTap,
                onAddParticipantsClicked = viewModel::onAddParticipantsTap,
                onSeeMoreClicked = viewModel::onSeeMoreTap,
                onLeaveGroupClicked = viewModel::onLeaveGroupTap,
                onParticipantClicked = viewModel::onParticipantTap,
                onBackPressed = { finish() },
                onScrollChange = { scrolled -> this.changeStatusBarColor(scrolled, isDark) },
            )
        }
    }

    private fun onActionTap(action: ScheduledMeetingInfoAction) {
        when (action) {
            ScheduledMeetingInfoAction.MeetingLink -> viewModel.onMeetingLinkTap()
            ScheduledMeetingInfoAction.ShareMeetingLink -> viewModel.onShareMeetingLinkTap()
            ScheduledMeetingInfoAction.ChatNotifications -> viewModel.onChatNotificationsTap()
            ScheduledMeetingInfoAction.AllowNonHostAddParticipants -> viewModel.onAllowAddParticipantsTap()
            ScheduledMeetingInfoAction.ShareFiles -> viewModel.onSharedFilesTap()
            ScheduledMeetingInfoAction.ManageChatHistory -> viewModel.onManageChatHistoryTap()
            ScheduledMeetingInfoAction.EnableEncryptedKeyRotation -> viewModel.onEnableEncryptedKeyRotationTap()
        }
    }
}