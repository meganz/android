package mega.privacy.android.app.listeners;

import android.content.Context;

import java.util.HashMap;

import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.MegaChatParticipant;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatLollipopAdapter;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaHandleList;

import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class GetPeerAttributesListener extends ChatBaseListener {

    private HashMap<Integer, MegaChatParticipant> participantRequests;

    private boolean isChatMessageRequest;
    private MegaChatLollipopAdapter.ViewHolderMessageChat holder;
    private MegaChatLollipopAdapter adapter;

    public GetPeerAttributesListener(Context context) {
        super(context);
    }

    /**
     * Constructor used to request for participants' attributes from GroupChatInfoActivityLollipop.
     *
     * @param context               current Context
     * @param participantRequests   HashMap<Integer, MegaChatParticipant> in which the keys are the positions in adapter
     *                              and the values the participants of the chat to check
     */
    public GetPeerAttributesListener(Context context, HashMap<Integer, MegaChatParticipant> participantRequests) {
        super(context);

        this.participantRequests = participantRequests;
    }

    /**
     * Constructor used to request for participant's attributes from a chat message.
     *
     * @param context   current Context
     * @param holder    item view to update in the adapter
     * @param adapter   adapter in which the message has to be updated
     */
    public GetPeerAttributesListener(Context context, MegaChatLollipopAdapter.ViewHolderMessageChat holder, MegaChatLollipopAdapter adapter) {
        super(context);

        this.holder = holder;
        this.adapter = adapter;
        isChatMessageRequest = true;
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() != MegaChatRequest.TYPE_GET_PEER_ATTRIBUTES) return;

        if (e.getErrorCode() == MegaChatError.ERROR_OK) {
            MegaHandleList handleList = request.getMegaHandleList();
            long chatHandle = request.getChatHandle();

            if (chatHandle != INVALID_HANDLE && handleList != null) {
                if (context instanceof GroupChatInfoActivityLollipop) {
                    ((GroupChatInfoActivityLollipop) context).updateParticipants(chatHandle, participantRequests, handleList);
                } else if (context instanceof ChatActivityLollipop) {
                    if (isChatMessageRequest) {
                        updateMessage(api.getChatRoom(chatHandle), handleList.get(0));
                    } else {
                        ((ChatActivityLollipop) context).updateCustomSubtitle(chatHandle, handleList);
                    }
                }
            } else {
                logError("Error asking for user attributes. Chat handle: " + chatHandle + " handleList: " + handleList == null ? "NULL" : "not null");
            }
        } else {
            logError("Error asking for user attributes: " + e.getErrorString());
        }
    }

    /**
     * Updates a message with the user's attributes received in the request.
     *
     * @param chat          MegaChatRoom in which the message has to be updated
     * @param peerHandle    user's identifier whose attributes has been requested
     */
    private void updateMessage(MegaChatRoom chat, long peerHandle) {
        if (adapter == null || chat == null || adapter.getChatRoom() == null
                || adapter.getChatRoom().getChatId() != chat.getChatId()
                || holder == null || holder.getUserHandle() != peerHandle) {
            logWarning("Message cannot be updated due to some error.");
            return;
        }

        adapter.setChatRoom(chat);
        new ChatController(context).setNonContactAttributesInDB(chat, peerHandle);
        adapter.notifyItemChanged(holder.getAdapterPosition());
    }
}
