package mega.privacy.android.app.meeting.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.activityViewModels
import mega.privacy.android.app.databinding.BottomSheetEndMeetingBinding
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil
import nz.mega.sdk.MegaChatApiJava

class EndMeetingBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {
    private lateinit var binding: BottomSheetEndMeetingBinding
    private val sharedViewModel:InMeetingViewModel by activityViewModels()
    private var chatId: Long? = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    private var callBack: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chatId = it.getLong(Constants.CHAT_ID, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
        }

        if (chatId == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            LogUtil.logError("Error. Chat doesn't exist")
            return
        }

        chatId?.let { sharedViewModel.setChatId(it) }
        
        if (sharedViewModel.getCall() == null) {
            LogUtil.logError("Error. Call doesn't exist")
            return
        }
    }

    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        binding =
            BottomSheetEndMeetingBinding.inflate(LayoutInflater.from(context), null, false).apply {
                lifecycleOwner = this@EndMeetingBottomSheetDialogFragment
            }

        binding.assignModerator.setOnClickListener { assignModerator() }
        binding.leaveAnyway.setOnClickListener { leaveAnyway() }
        dialog.setContentView(binding.root)
    }

    private fun assignModerator() {
        dismiss()
        callBack?.invoke()
    }

    private fun leaveAnyway() {
        sharedViewModel.leaveMeeting()
        requireActivity().finish()
    }

    fun setAssignCallBack(showAssignModeratorFragment: () -> Unit) {
        callBack = showAssignModeratorFragment
    }

    companion object {
        fun newInstance(chatId: Long): EndMeetingBottomSheetDialogFragment {
            return EndMeetingBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(Constants.CHAT_ID, chatId)
                }
            }
        }
    }
}