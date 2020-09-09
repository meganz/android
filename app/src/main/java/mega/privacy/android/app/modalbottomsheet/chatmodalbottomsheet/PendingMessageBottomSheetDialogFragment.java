package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.PendingMessageSingle;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaTransfer;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class PendingMessageBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaChatRoom selectedChat;
    private long chatId;
    private long messageId;

    private boolean isUploadingMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            chatId = savedInstanceState.getLong(CHAT_ID, INVALID_HANDLE);
            messageId = savedInstanceState.getLong(MESSAGE_ID, INVALID_HANDLE);
        } else {
            chatId = ((ChatActivityLollipop) context).idChat;
            messageId = ((ChatActivityLollipop) context).selectedMessageId;
        }

        logDebug("Chat ID: " + chatId + "Message ID: " + messageId);
        selectedChat = megaChatApi.getChatRoom(chatId);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.msg_not_sent_bottom_sheet, null);
        mainLinearLayout = contentView.findViewById(R.id.msg_not_sent_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        TextView titleSlidingPanel = contentView.findViewById(R.id.msg_not_sent_title_text);
        LinearLayout optionRetryLayout = contentView.findViewById(R.id.msg_not_sent_retry_layout);
        LinearLayout optionDeleteLayout = contentView.findViewById(R.id.msg_not_sent_delete_layout);
        optionDeleteLayout.setOnClickListener(this);

        LinearLayout separator = contentView.findViewById(R.id.separator);

        PendingMessageSingle pMsg = dbH.findPendingMessageById(messageId);
        isUploadingMessage = pMsg != null && pMsg.getState() == PendingMessageSingle.STATE_UPLOADING;

        if (isUploadingMessage) {
            if (megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)) {
                titleSlidingPanel.setText(R.string.attachment_uploading_state_paused);
                TextView resumeText = optionRetryLayout.findViewById(R.id.msg_not_sent_retry_text);
                resumeText.setText(R.string.option_resume_transfers);
                ImageView resumeIcon = optionRetryLayout.findViewById(R.id.msg_not_sent_retry_image);
                resumeIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_resume_transfers));
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

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.msg_not_sent_retry_layout:
                if (megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD) && isUploadingMessage) {
                    megaApi.pauseTransfers(false);
                    ((ChatActivityLollipop) context).updatePausedUploadingMessages();
                } else {
                    ((ChatActivityLollipop) context).retryPendingMessage(messageId);
                }
                break;

            case R.id.msg_not_sent_delete_layout:
                ((ChatActivityLollipop) context).removePendingMsg(messageId);
                break;
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
