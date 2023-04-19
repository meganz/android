package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.MESSAGE_ID;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import mega.privacy.android.app.R;
import mega.privacy.android.app.main.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNodeList;
import timber.log.Timber;

public class MessageNotSentBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaChatRoom selectedChat;
    private AndroidMegaChatMessage selectedMessage;
    private MegaChatMessage originalMsg;
    private long chatId;
    private long messageId;

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

        MegaChatMessage messageMega = megaChatApi.getManualSendingMessage(chatId, messageId);
        if (messageMega != null) {
            selectedMessage = new AndroidMegaChatMessage(messageMega);
        }

        selectedChat = megaChatApi.getChatRoom(chatId);
        Timber.d("Chat ID: %d, Message ID: %d", chatId, messageId);

        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        if (selectedMessage == null && selectedChat == null) {
            Timber.w("Chat or message are NULL");
            return;
        }

        int id = v.getId();
        if (id == R.id.msg_not_sent_retry_layout) {
            if (selectedMessage.getMessage().getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT) {
                MegaNodeList nodeList = selectedMessage.getMessage().getMegaNodeList();
                if (nodeList != null) {
                    long nodeHandle = nodeList.get(0).getHandle();

                    ((ChatActivity) requireActivity()).removeMsgNotSent();
                    megaChatApi.removeUnsentMessage(selectedChat.getChatId(), selectedMessage.getMessage().getRowId());

                    ((ChatActivity) requireActivity()).retryNodeAttachment(nodeHandle);
                } else {
                    Timber.w("Error the nodeList cannot be recovered");
                }
            } else if (selectedMessage.getMessage().getType() == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
                long userCount = selectedMessage.getMessage().getUsersCount();

                MegaHandleList handleList = MegaHandleList.createInstance();

                for (int i = 0; i < userCount; i++) {
                    long handle = selectedMessage.getMessage().getUserHandle(i);
                    handleList.addMegaHandle(handle);
                }

                ((ChatActivity) requireActivity()).removeMsgNotSent();
                megaChatApi.removeUnsentMessage(selectedChat.getChatId(), selectedMessage.getMessage().getRowId());

                ((ChatActivity) requireActivity()).retryContactAttachment(handleList);
            } else {
                ((ChatActivity) requireActivity()).removeMsgNotSent();
                megaChatApi.removeUnsentMessage(selectedChat.getChatId(), selectedMessage.getMessage().getRowId());

                if (selectedMessage.getMessage().isEdited()) {
                    Timber.d("Message is edited --> edit");
                    if (originalMsg != null) {
                        ((ChatActivity) requireActivity()).editMessageMS(selectedMessage.getMessage().getContent(), originalMsg);
                    }
                } else {
                    Timber.d("Message NOT edited --> send");
                    ((ChatActivity) requireActivity()).sendMessage(selectedMessage.getMessage().getContent());
                }
            }
        } else if (id == R.id.msg_not_sent_delete_layout) {
            ((ChatActivity) requireActivity()).removeMsgNotSent();
            megaChatApi.removeUnsentMessage(selectedChat.getChatId(), selectedMessage.getMessage().getRowId());
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
