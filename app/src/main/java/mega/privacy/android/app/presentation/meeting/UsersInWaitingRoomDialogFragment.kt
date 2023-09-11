package mega.privacy.android.app.presentation.meeting

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
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.view.UsersInWaitingRoomDialog
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaChatApiJava
import javax.inject.Inject

/**
 * Users In Waiting Room Dialog Fragment
 * Necessary to display the compose dialogue in ChatActivity.java
 */
@AndroidEntryPoint
class UsersInWaitingRoomDialogFragment : DialogFragment() {
    private val viewModel: WaitingRoomManagementViewModel by viewModels()

    /**
     * GetThemeMode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val isDark = themeMode.isDarkMode()
                AndroidTheme(isDark = isDark) {
                    UsersInWaitingRoomDialog(
                        state = state,
                        onAdmitClick = {
                            viewModel.admitUsersClick()
                        },
                        onSeeWaitingRoomClick = {
                            viewModel.setShowParticipantsInWaitingRoomDialogConsumed()
                            viewModel.setShowDenyParticipantDialogConsumed()
                            dismissAllowingStateLoss()
                        },
                        onDismiss = {
                            viewModel.setShowParticipantsInWaitingRoomDialogConsumed()
                            dismissAllowingStateLoss()
                        },
                    )
                }
            }
        }
    }

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
        fun newInstance() = UsersInWaitingRoomDialogFragment()
    }
}