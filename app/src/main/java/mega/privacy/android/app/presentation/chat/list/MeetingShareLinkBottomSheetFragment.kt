package mega.privacy.android.app.presentation.chat.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.chat.list.view.MeetingLinkView
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingManagementViewModel
import mega.privacy.android.app.presentation.meeting.model.ShareLinkOption
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * BottomSheetFragment to show the Meeting Share Link options
 */
@AndroidEntryPoint
class MeetingShareLinkBottomSheetFragment : BottomSheetDialogFragment() {
    companion object {
        /**
         * Tag to identify the fragment
         */
        const val TAG = "MeetingShareLinkBottomSheetFragment"

        /**
         * Create a new instance of MeetingShareLinkBottomSheetFragment
         */
        fun newInstance(): MeetingShareLinkBottomSheetFragment {
            return MeetingShareLinkBottomSheetFragment()
        }
    }

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val scheduledMeetingManagementViewModel by viewModels<ScheduledMeetingManagementViewModel>(
        { requireParentFragment() })

    /**
     * Create the view for the Meeting Share Link BottomSheet
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            val mode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            OriginalTempTheme(isDark = mode.isDarkMode()) {
                MeetingLinkView(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    onSendLinkToChat = {
                        scheduledMeetingManagementViewModel.onMeetingLinkShare(ShareLinkOption.SendLinkToChat)
                        dismissAllowingStateLoss()
                    },
                    onShareLink = {
                        scheduledMeetingManagementViewModel.onMeetingLinkShare(ShareLinkOption.ShareLink)
                        dismissAllowingStateLoss()
                    },
                )
            }
        }
    }

    /**
     * Custom show method to avoid showing the same dialog multiple times
     */
    fun show(manager: FragmentManager) {
        if (manager.findFragmentByTag(TAG) == null) super.show(
            manager,
            TAG
        )
    }

}
