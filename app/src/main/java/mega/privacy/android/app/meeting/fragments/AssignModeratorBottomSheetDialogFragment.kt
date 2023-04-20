package mega.privacy.android.app.meeting.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetAssignModeratorBinding

/**
 * The fragment shows two options for moderator when the moderator leave the meeting
 *
 * LEAVE MEETING or ASSIGN MODERATOR
 */
class AssignModeratorBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: BottomSheetAssignModeratorBinding
    private var callBackAssignModerator: (() -> Unit)? = null
    private var callBackLeaveMeeting: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(requireContext(),
            R.style.BottomSheetFragmentWithTransparentBackground).apply {
            setCanceledOnTouchOutside(true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = BottomSheetAssignModeratorBinding.inflate(layoutInflater, container, false)

        binding.assignModerator.setOnClickListener { assignModerator() }
        binding.leaveAnyway.setOnClickListener { leaveAnyway() }
        return binding.root
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog ?: return
        BottomSheetBehavior.from(dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)).state =
            BottomSheetBehavior.STATE_EXPANDED
    }

    /**
     * Assign moderator listener will close the page and open assign moderator activity
     */
    private fun assignModerator() {
        dismissAllowingStateLoss()
        callBackAssignModerator?.invoke()
    }

    /**
     * Leave anyway listener, will leave meeting directly
     */
    private fun leaveAnyway() {
        dismissAllowingStateLoss()
        callBackLeaveMeeting?.invoke()
    }

    /**
     * Set the call back for clicking leave meeting option
     *
     * @param leaveMeetingModerator call back
     */
    fun setLeaveMeetingCallBack(leaveMeetingModerator: () -> Unit) {
        callBackLeaveMeeting = leaveMeetingModerator
    }

    /**
     * Set the call back for clicking assign moderator option
     *
     * @param showAssignModeratorFragment call back
     */
    fun setAssignModeratorCallBack(showAssignModeratorFragment: () -> Unit) {
        callBackAssignModerator = showAssignModeratorFragment
    }
}