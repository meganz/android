package mega.privacy.android.app.listeners;

import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;

import android.content.Context;

import java.util.HashMap;

import mega.privacy.android.app.main.megachat.GroupChatInfoActivity;
import mega.privacy.android.app.main.megachat.MegaChatParticipant;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaHandleList;
import timber.log.Timber;

public class GetPeerAttributesListener extends ChatBaseListener {

    private HashMap<Integer, MegaChatParticipant> participantRequests;
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

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        if (request.getType() != MegaChatRequest.TYPE_GET_PEER_ATTRIBUTES) return;

        if (e.getErrorCode() == MegaChatError.ERROR_OK) {
            MegaHandleList handleList = request.getMegaHandleList();
            long chatHandle = request.getChatHandle();

            if (chatHandle != INVALID_HANDLE && handleList != null) {
                if (context instanceof GroupChatInfoActivity) {
                    ((GroupChatInfoActivity) context).updateParticipants(chatHandle, participantRequests, handleList);
                }
            } else {
                Timber.e("Error asking for user attributes. Chat handle: %s handleList: %s", chatHandle, handleList == null ? "NULL" : "not null");
            }
        } else {
            Timber.e("Error asking for user attributes: %s", e.getErrorString());
        }
    }
}
