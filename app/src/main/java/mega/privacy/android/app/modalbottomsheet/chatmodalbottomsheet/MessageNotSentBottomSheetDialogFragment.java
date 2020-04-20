package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNodeList;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class MessageNotSentBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaChatRoom selectedChat;
    private AndroidMegaChatMessage selectedMessage;
    private MegaChatMessage originalMsg;
    private long chatId;
    private long messageId;

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

        MegaChatMessage messageMega = megaChatApi.getManualSendingMessage(chatId, messageId);
        if (messageMega != null) {
            selectedMessage = new AndroidMegaChatMessage(messageMega);
        }

        selectedChat = megaChatApi.getChatRoom(chatId);
        logDebug("Chat ID: " + chatId + ", Message ID: " + messageId);
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

        titleSlidingPanel.setText(getString(R.string.title_message_not_sent_options));

        LinearLayout separator = contentView.findViewById(R.id.separator);

        if (selectedMessage != null && selectedChat != null) {
            if (selectedMessage.getMessage().isEdited()) {
                originalMsg = megaChatApi.getMessage(selectedChat.getChatId(), selectedMessage.getMessage().getTempId());
                if (originalMsg != null && originalMsg.isEditable() && (selectedChat.getOwnPrivilege() == MegaChatRoom.PRIV_STANDARD || selectedChat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR)) {
                    optionRetryLayout.setVisibility(View.VISIBLE);
                    optionRetryLayout.setOnClickListener(this);
                    separator.setVisibility(View.VISIBLE);
                } else {
                    optionRetryLayout.setVisibility(View.GONE);
                    separator.setVisibility(View.GONE);
                }
            } else {
                if ((selectedChat.getOwnPrivilege() == MegaChatRoom.PRIV_STANDARD) || (selectedChat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR)) {
                    optionRetryLayout.setVisibility(View.VISIBLE);
                    optionRetryLayout.setOnClickListener(this);
                    separator.setVisibility(View.VISIBLE);
                } else {
                    optionRetryLayout.setVisibility(View.GONE);
                    separator.setVisibility(View.GONE);
                }
            }
        }

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false);
    }

    @Override
    public void onClick(View v) {
        if (selectedMessage == null && selectedChat == null) {
            logWarning("Chat or message are NULL");
            return;
        }

        switch (v.getId()) {
            case R.id.msg_not_sent_retry_layout:
                if (selectedMessage.getMessage().getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT) {
                    MegaNodeList nodeList = selectedMessage.getMessage().getMegaNodeList();
                    if (nodeList != null) {
                        long nodeHandle = nodeList.get(0).getHandle();

                        ((ChatActivityLollipop) context).removeMsgNotSent();
                        megaChatApi.removeUnsentMessage(selectedChat.getChatId(), selectedMessage.getMessage().getRowId());

                        ((ChatActivityLollipop) context).retryNodeAttachment(nodeHandle);
                    } else {
                        logWarning("Error the nodeList cannot be recovered");
                    }
                } else if (selectedMessage.getMessage().getType() == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
                    long userCount = selectedMessage.getMessage().getUsersCount();

                    MegaHandleList handleList = MegaHandleList.createInstance();

                    for (int i = 0; i < userCount; i++) {
                        long handle = selectedMessage.getMessage().getUserHandle(i);
                        handleList.addMegaHandle(handle);
                    }

                    ((ChatActivityLollipop) context).removeMsgNotSent();
                    megaChatApi.removeUnsentMessage(selectedChat.getChatId(), selectedMessage.getMessage().getRowId());

                    ((ChatActivityLollipop) context).retryContactAttachment(handleList);
                } else {
                    ((ChatActivityLollipop) context).removeMsgNotSent();
                    megaChatApi.removeUnsentMessage(selectedChat.getChatId(), selectedMessage.getMessage().getRowId());

                    if (selectedMessage.getMessage().isEdited()) {
                        logDebug("Message is edited --> edit");
                        if (originalMsg != null) {
                            ((ChatActivityLollipop) context).editMessageMS(selectedMessage.getMessage().getContent(), originalMsg);
                        }
                    } else {
                        logDebug("Message NOT edited --> send");
                        ((ChatActivityLollipop) context).sendMessage(selectedMessage.getMessage().getContent());
                    }
                }

                break;

            case R.id.msg_not_sent_delete_layout:
                ((ChatActivityLollipop) context).removeMsgNotSent();
                megaChatApi.removeUnsentMessage(selectedChat.getChatId(), selectedMessage.getMessage().getRowId());
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
