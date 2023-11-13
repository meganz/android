package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.MsgNotSentBottomSheetBinding
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.chat.PendingMessageState
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatRoom
import timber.log.Timber

/**
 * BottomSheetDialog to show options for pendin messages
 */
class PendingMessageBottomSheetDialogFragment : BaseBottomSheetDialogFragment(),
    View.OnClickListener {
    private var selectedChat: MegaChatRoom? = null
    private var chatId: Long = 0
    private var messageId: Long = 0
    private var isUploadingMessage = false
    private lateinit var binding: MsgNotSentBottomSheetBinding

    /**
     * on create view
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = MsgNotSentBottomSheetBinding.inflate(inflater, container, false)
        contentView = binding.root
        itemsLayout = binding.itemsLayout
        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(Constants.CHAT_ID, MegaApiJava.INVALID_HANDLE)
            messageId = savedInstanceState.getLong(Constants.MESSAGE_ID, MegaApiJava.INVALID_HANDLE)
        } else {
            chatId = (requireActivity() as ChatActivity).idChat
            messageId = (requireActivity() as ChatActivity).selectedMessageId
        }
        Timber.d("Chat ID: %dMessage ID: %d", chatId, messageId)
        selectedChat = megaChatApi.getChatRoom(chatId)
        return contentView
    }

    /**
     * on view created
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val titleSlidingPanel = binding.msgNotSentTitleText
        val optionRetryLayout = binding.msgNotSentRetryLayout
        val optionDeleteLayout = binding.msgNotSentDeleteLayout
        optionDeleteLayout.setOnClickListener(this)
        val separator = binding.separator
        val pMsg = dbH.findPendingMessageById(messageId)
        isUploadingMessage =
            pMsg != null && pMsg.state != PendingMessageState.ERROR_UPLOADING.value && pMsg.state != PendingMessageState.ERROR_ATTACHING.value
        if (isUploadingMessage) {
            if (dbH.transferQueueStatus) {
                titleSlidingPanel.setText(R.string.attachment_uploading_state_paused)
                val resumeText = binding.msgNotSentRetryText
                resumeText.setText(R.string.option_resume_transfers)
                val resumeIcon = binding.msgNotSentRetryImage
                resumeIcon.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_resume_transfers
                    )
                )
                resumeIcon.alpha = 1f
                optionRetryLayout.setOnClickListener(this)
                val deleteText = binding.msgNotSentDeleteText
                deleteText.setText(R.string.option_cancel_transfer)
            } else {
                optionRetryLayout.visibility = View.GONE
                titleSlidingPanel.text = getString(R.string.title_message_uploading_options)
                separator.visibility = View.GONE
            }
        } else {
            titleSlidingPanel.text = getString(R.string.title_message_not_sent_options)
            if (selectedChat?.ownPrivilege == MegaChatRoom.PRIV_STANDARD || selectedChat?.ownPrivilege == MegaChatRoom.PRIV_MODERATOR) {
                optionRetryLayout.setOnClickListener(this)
            } else {
                optionRetryLayout.visibility = View.GONE
                separator.visibility = View.GONE
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * on click
     */
    override fun onClick(v: View) {
        val id = v.id
        if (id == R.id.msg_not_sent_retry_layout) {
            if (dbH.transferQueueStatus && isUploadingMessage) {
                megaApi.pauseTransfers(false)
                (requireActivity() as ChatActivity).updatePausedUploadingMessages()
            } else {
                (requireActivity() as ChatActivity).retryPendingMessage(messageId)
            }
        } else if (id == R.id.msg_not_sent_delete_layout) {
            (requireActivity() as ChatActivity).removePendingMsg(messageId)
        }
        setStateBottomSheetBehaviorHidden()
    }

    /**
     * on save instance state
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(Constants.CHAT_ID, chatId)
        outState.putLong(Constants.MESSAGE_ID, messageId)
    }
}