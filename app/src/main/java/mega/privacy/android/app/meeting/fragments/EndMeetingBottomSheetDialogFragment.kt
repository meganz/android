package mega.privacy.android.app.meeting.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetEndMeetingBinding
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber

/**
 * The fragment shows two options for moderator when the moderator leave the meeting
 */
class EndMeetingBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: BottomSheetEndMeetingBinding
    private val sharedViewModel: InMeetingViewModel by activityViewModels()
    private var chatId: Long? = MegaChatApiJava.MEGACHAT_INVALID_HANDLE
    private var callBack: (() -> Unit)? = null
    private var callBackLeaveMeeting: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            chatId = it.getLong(Constants.CHAT_ID, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
        }

        if (chatId == MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
            Timber.e("Error. Chat doesn't exist")
            return
        }

        chatId?.let { sharedViewModel.setChatId(it) }

        if (sharedViewModel.getCall() == null) {
            Timber.e("Error. Call doesn't exist")
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

    override fun onStart() {
        super.onStart()

        val dialog = dialog ?: return
        BottomSheetBehavior.from(dialog.findViewById(R.id.design_bottom_sheet)).state =
            BottomSheetBehavior.STATE_EXPANDED
    }

    /**
     * Assign moderator listener will close the page and open assign moderator activity
     */
    private fun assignModerator() {
        dismiss()
        callBack?.invoke()
    }

    /**
     * Leave anyway listener, will leave meeting directly
     */
    private fun leaveAnyway() {
        dismiss()
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