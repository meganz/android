package mega.privacy.android.app.listeners;

import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.content.Context;

import java.util.HashMap;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity;
import mega.privacy.android.app.main.megachat.MegaChatParticipant;
import mega.privacy.android.app.main.megachat.chatAdapters.MegaChatAdapter;
import mega.privacy.android.domain.entity.ChatRequest;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;
import timber.log.Timber;

public class GetPeerAttributesListener extends ChatBaseListener {

    private HashMap<Integer, MegaChatParticipant> participantRequests;

    private boolean isChatMessageRequest;
    private MegaChatAdapter.ViewHolderMessageChat holder;
    private MegaChatAdapter adapter;

    private ChatRequest request;

    public GetPeerAttributesListener(Context context) {
        super(context);
    }

    /**
     * Constructor used to request for participants' attributes from GroupChatInfoActivity.
     *
     * @param context             current Context
     * @param participantRequests HashMap<Integer, MegaChatParticipant> in which the keys are the positions in adapter
     *                            and the values the participants of the chat to check
     */
    public GetPeerAttributesListener(Context context, HashMap<Integer, MegaChatParticipant> participantRequests) {
        super(context);

        this.participantRequests = new HashMap<>(participantRequests);
    }

    /**
     * Constructor used to request for participant's attributes from a chat message.
     *
     * @param context current Context
     * @param holder  item view to update in the adapter
     * @param adapter adapter in which the message has to be updated
     */
    public GetPeerAttributesListener(Context context, MegaChatAdapter.ViewHolderMessageChat holder, MegaChatAdapter adapter) {
        super(context);

        this.holder = holder;
        this.adapter = adapter;
        isChatMessageRequest = true;
    }

    /**
     * Constructor used to request for participant's attributes from a chat message notification.
     *
     * @param context current Context
     * @param request ChatRequest which contains the push notification to update
     */
    public GetPeerAttributesListener(Context context, ChatRequest request) {
        super(context);

        this.request = request;
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() != MegaChatRequest.TYPE_GET_PEER_ATTRIBUTES) return;

        if (e.getErrorCode() == MegaChatError.ERROR_OK) {
            MegaHandleList handleList = request.getMegaHandleList();
            long chatHandle = request.getChatHandle();

            if (chatHandle != INVALID_HANDLE && handleList != null) {
                if (this.request != null) {
                    updateNotificationName();
                } else if (context instanceof GroupChatInfoActivity) {
                    ((GroupChatInfoActivity) context).updateParticipants(chatHandle, participantRequests, handleList);
                } else if (context instanceof ChatActivity) {
                    if (isChatMessageRequest) {
                        updateMessage(api.getChatRoom(chatHandle), handleList.get(0));
                    } else {
                        ((ChatActivity) context).updateCustomSubtitle(chatHandle, handleList);
                    }
                }
            } else {
                Timber.e("Error asking for user attributes. Chat handle: %s handleList: %s", chatHandle, handleList == null ? "NULL" : "not null");
            }
        } else {
            Timber.e("Error asking for user attributes: %s", e.getErrorString());
        }
    }

    /**
     * Updates a message with the user's attributes received in the request.
     *
     * @param chat       MegaChatRoom in which the message has to be updated
     * @param peerHandle user's identifier whose attributes has been requested
     */
    private void updateMessage(MegaChatRoom chat, long peerHandle) {
        if (adapter == null || chat == null || adapter.getChatRoom() == null
                || adapter.getChatRoom().getChatId() != chat.getChatId()
                || holder == null || holder.getUserHandle() != peerHandle) {
            Timber.w("Message cannot be updated due to some error.");
            return;
        }

        new ChatController(context).setNonContactAttributesInDB(peerHandle);
        adapter.notifyItemChanged(holder.getAdapterPosition());
    }

    /**
     * Updates a notification with the user's attributes reveived in the request.
     */
    private void updateNotificationName() {
        ChatAdvancedNotificationBuilder notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(context);
        notificationBuilder.setIsUpdatingUserName();
        notificationBuilder.generateChatNotification(request);
    }
}
