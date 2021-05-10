package mega.privacy.android.app.meeting.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.view.LayoutInflater
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetEndMeetingBinding
import mega.privacy.android.app.meeting.activity.AssignModeratorActivity
import mega.privacy.android.app.meeting.fragments.EndMeetingBottomSheetDialogViewModel.Companion.ASSIGN_MODERATOR
import mega.privacy.android.app.meeting.fragments.EndMeetingBottomSheetDialogViewModel.Companion.END_MEETING_FOR_ALL
import mega.privacy.android.app.meeting.fragments.EndMeetingBottomSheetDialogViewModel.Companion.LEAVE_ANYWAY
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.LogUtil

class EndMeetingBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {
    private lateinit var binding: BottomSheetEndMeetingBinding
    private val viewModel: EndMeetingBottomSheetDialogViewModel by viewModels()
    private val sharedViewModel:InMeetingViewModel by activityViewModels()

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        viewModel.clickEvent.observe(this, {
            when (it) {
                LEAVE_ANYWAY -> leaveAnyway()
                END_MEETING_FOR_ALL -> askConfirmationEndMeetingForAll()
                ASSIGN_MODERATOR -> assignModerator()
            }
            dismiss()
        })

        binding =
            BottomSheetEndMeetingBinding.inflate(LayoutInflater.from(context), null, false).apply {
                lifecycleOwner = this@EndMeetingBottomSheetDialogFragment
                viewmodel = viewModel
            }

        dialog.setContentView(binding.root)
    }

    private fun assignModerator() {
        activity?.startActivity(Intent(requireActivity(), AssignModeratorActivity::class.java).apply {

        })
    }

    private fun leaveAnyway() {
        sharedViewModel.leaveMeeting()
        requireActivity().finish()
    }

    private fun askConfirmationEndMeetingForAll() {
        LogUtil.logDebug("askConfirmationEndMeeting")

        MaterialAlertDialogBuilder(requireContext()).apply {
            setTitle(getString(R.string.end_meeting_for_all_dialog_title))
            setMessage(getString(R.string.end_meeting_dialog_message))
            setPositiveButton(R.string.general_ok) { dialog, _ ->
                run {
                    dialog.dismiss()
                    endMeetingForAll()
                }
            }
            setNegativeButton(R.string.general_cancel) { dialog, _ -> dialog.dismiss() }
            show()
        }
    }

    private fun endMeetingForAll() {

    }

    companion object {
        fun newInstance(): EndMeetingBottomSheetDialogFragment {
            return EndMeetingBottomSheetDialogFragment()
        }
    }
}