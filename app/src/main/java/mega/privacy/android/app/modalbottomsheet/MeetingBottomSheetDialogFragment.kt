package mega.privacy.android.app.modalbottomsheet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import kotlinx.android.synthetic.main.bottom_sheet_meeting.view.*
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetMeetingBinding
import mega.privacy.android.app.lollipop.controllers.ContactController

class MeetingBottomSheetDialogFragment : BaseBottomSheetDialogFragment(), View.OnClickListener {
    private var cC: ContactController? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cC = ContactController(context)
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val binding = BottomSheetMeetingBinding.inflate(LayoutInflater.from(context), null, false)
        contentView = binding.root;
        mainLinearLayout = binding.root.meeting_bottom_sheet
        items_layout = binding.root.meeting_item
        binding.ivStartMeeting.setOnClickListener(this)
        binding.ivJoinMeeting.setOnClickListener(this)
        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, true)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.iv_start_meeting -> {
            }
            R.id.iv_join_meeting -> {
            }
        }
        setStateBottomSheetBehaviorHidden()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }
}