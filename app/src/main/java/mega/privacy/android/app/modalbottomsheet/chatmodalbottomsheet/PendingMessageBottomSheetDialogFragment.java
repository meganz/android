package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;


import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.MESSAGE_ID;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import mega.privacy.android.app.R;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import mega.privacy.android.domain.entity.chat.PendingMessage;
import mega.privacy.android.domain.entity.chat.PendingMessageState;
import nz.mega.sdk.MegaChatRoom;
import timber.log.Timber;

public class PendingMessageBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaChatRoom selectedChat;
    private long chatId;
    private long messageId;

    private boolean isUploadingMessage;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(getContext(), R.layout.msg_not_sent_bottom_sheet, null);
        itemsLayout = contentView.findViewById(R.id.items_layout);

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_HANDLE);
            messageId = savedInstanceState.getLong(MESSAGE_ID, INVALID_HANDLE);
        } else {
            chatId = ((ChatActivity) requireActivity()).idChat;
            messageId = ((ChatActivity) requireActivity()).selectedMessageId;
        }

        Timber.d("Chat ID: %dMessage ID: %d", chatId, messageId);
        selectedChat = megaChatApi.getChatRoom(chatId);

        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView titleSlidingPanel = contentView.findViewById(R.id.msg_not_sent_title_text);
        LinearLayout optionRetryLayout = contentView.findViewById(R.id.msg_not_sent_retry_layout);
        LinearLayout optionDeleteLayout = contentView.findViewById(R.id.msg_not_sent_delete_layout);
        optionDeleteLayout.setOnClickListener(this);

        LinearLayout separator = contentView.findViewById(R.id.separator);

        PendingMessage pMsg = dbH.findPendingMessageById(messageId);
        isUploadingMessage = pMsg != null && pMsg.getState() != PendingMessageState.ERROR_UPLOADING.getValue() && pMsg.getState() != PendingMessageState.ERROR_ATTACHING.getValue();

        if (isUploadingMessage) {
            if (dbH.getTransferQueueStatus()) {
                titleSlidingPanel.setText(R.string.attachment_uploading_state_paused);
                TextView resumeText = optionRetryLayout.findViewById(R.id.msg_not_sent_retry_text);
                resumeText.setText(R.string.option_resume_transfers);
                ImageView resumeIcon = optionRetryLayout.findViewById(R.id.msg_not_sent_retry_image);
                resumeIcon.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_resume_transfers));
                resumeIcon.setAlpha(1F);
                optionRetryLayout.setOnClickListener(this);
                TextView deleteText = optionDeleteLayout.findViewById(R.id.msg_not_sent_delete_text);
                deleteText.setText(R.string.option_cancel_transfer);
            } else {
                optionRetryLayout.setVisibility(View.GONE);
                titleSlidingPanel.setText(getString(R.string.title_message_uploading_options));
                separator.setVisibility(View.GONE);
            }
        } else {
            titleSlidingPanel.setText(getString(R.string.title_message_not_sent_options));
            if ((selectedChat.getOwnPrivilege() == MegaChatRoom.PRIV_STANDARD) || (selectedChat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR)) {
                optionRetryLayout.setOnClickListener(this);
            } else {
                optionRetryLayout.setVisibility(View.GONE);
                separator.setVisibility(View.GONE);
            }
        }

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.msg_not_sent_retry_layout) {
            if (dbH.getTransferQueueStatus() && isUploadingMessage) {
                megaApi.pauseTransfers(false);
                ((ChatActivity) requireActivity()).updatePausedUploadingMessages();
            } else {
                ((ChatActivity) requireActivity()).retryPendingMessage(messageId);
            }
        } else if (id == R.id.msg_not_sent_delete_layout) {
            ((ChatActivity) requireActivity()).removePendingMsg(messageId);
        }

        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CHAT_ID, chatId);
        outState.putLong(MESSAGE_ID, messageId);
    }
}
