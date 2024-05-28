package mega.privacy.android.app.presentation.meeting.view.dialog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.WaitingRoomManagementViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import nz.mega.sdk.MegaChatApiJava
import javax.inject.Inject

/**
 * Users In Waiting Room Dialog Fragment
 * Necessary to display the compose dialogue in ChatActivity.java
 */
@AndroidEntryPoint
@Deprecated("Use only in case of Java code. . Use UsersInWaitingRoomDialog compose view instead.")
class UsersInWaitingRoomDialogFragment : DialogFragment() {
    private val viewModel: WaitingRoomManagementViewModel by viewModels()

    /**
     * GetThemeMode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * On create view
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                OriginalTempTheme(isDark = isDark) {
                    UsersInWaitingRoomDialog(
                        onSeeWaitingRoomClick = {
                            val chatId = viewModel.state.value.chatId
                            MegaApplication.getInstance().openCallService(chatId)

                            val intent =
                                Intent(requireContext(), MeetingActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    action = MeetingActivity.MEETING_ACTION_IN
                                    putExtra(MeetingActivity.MEETING_CHAT_ID, chatId)
                                    putExtra(MeetingActivity.MEETING_BOTTOM_PANEL_EXPANDED, true)
                                }
                            startActivity(intent)
                            dismissAllowingStateLoss()
                        },
                        onDismiss = {
                            dismissAllowingStateLoss()
                        },
                    )

                    DenyEntryToCallDialog(
                        onDismiss = { dismissAllowingStateLoss() }
                    )
                }
            }
        }
    }

    /**
     * On view created
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.collectFlow(viewModel.state) { state ->
            state.snackbarString?.let {
                showMessage(it)
                dismissAllowingStateLoss()
            }
        }
    }

    /**
     * Show snackbar
     *
     * @param message Text shown in the snackbar
     */
    private fun showMessage(message: String) {
        (activity as? BaseActivity)?.showSnackbar(
            Constants.SNACKBAR_TYPE,
            message,
            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
        )
    }

    companion object {
        /**
         * New instance
         */
        fun newInstance() = UsersInWaitingRoomDialogFragment()
    }
}