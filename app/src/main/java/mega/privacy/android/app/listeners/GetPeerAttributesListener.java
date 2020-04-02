package mega.privacy.android.app.listeners;

import android.content.Context;

import java.util.HashMap;

import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.MegaChatParticipant;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaHandleList;

import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

public class GetPeerAttributesListener extends ChatBaseListener {

    private HashMap<Integer, MegaChatParticipant> participantRequests;

    public GetPeerAttributesListener(Context context) {
        super(context);
    }

    /**
     * Constructor used to ask for participants' attributes from GroupChatInfoActivityLollipop.
     *
     * @param context               current Context
     * @param participantRequests   HashMap<Integer, MegaChatParticipant> in which the keys are the positions in adapter
     *                              and the values the participants of the chat to check
     */
    public GetPeerAttributesListener(Context context, HashMap<Integer, MegaChatParticipant> participantRequests) {
        super(context);

        this.participantRequests = participantRequests;
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
                    ((ChatActivityLollipop) context).updateCustomSubtitle(chatHandle, handleList);
                }
            } else {
                logError("Error asking for user attributes. Chat handle: " + chatHandle + " handleList: " + handleList == null ? "NULL" : "not null");
            }
        } else {
            logError("Error asking for user attributes: " + e.getErrorString());
        }
    }
}
