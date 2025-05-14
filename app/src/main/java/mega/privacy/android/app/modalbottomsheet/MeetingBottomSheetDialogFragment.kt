package mega.privacy.android.app.modalbottomsheet

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetMeetingSimpleBinding
import mega.privacy.android.app.interfaces.MeetingBottomSheetDialogActionListener
import mega.privacy.mobile.analytics.event.ScheduleMeetingMenuItemEvent

@AndroidEntryPoint
class MeetingBottomSheetDialogFragment : BottomSheetDialogFragment(), View.OnClickListener {

    companion object {
        const val TAG = "MeetingBottomSheetDialog"

        @JvmStatic
        fun newInstance(): MeetingBottomSheetDialogFragment = MeetingBottomSheetDialogFragment()
    }

    private var listener: MeetingBottomSheetDialogActionListener? = null

    /**
     * onScheduleMeeting callback
     */
    var onScheduleMeeting: (() -> Unit)? = null

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        val binding =
            BottomSheetMeetingSimpleBinding.inflate(LayoutInflater.from(context), null, false)
        binding.btnScheduleMeeting.setOnClickListener(this)
        binding.btnStartMeeting.setOnClickListener(this)
        binding.btnJoinMeeting.setOnClickListener(this)

        dialog.setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog ?: return
        BottomSheetBehavior.from(dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)).state =
            BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_start_meeting -> {
                listener?.onCreateMeeting()
                dismiss()
            }

            R.id.btn_join_meeting -> {
                listener?.onJoinMeeting()
                dismiss()
            }

            R.id.btn_schedule_meeting -> {
                Analytics.tracker.trackEvent(ScheduleMeetingMenuItemEvent)
                onScheduleMeeting?.invoke()
                dismiss()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as MeetingBottomSheetDialogActionListener
    }
}
