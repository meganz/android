package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;
import java.util.ArrayList;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_ERROR_COPYING_NODES;
import static mega.privacy.android.app.constants.BroadcastConstants.ERROR_MESSAGE_TEXT;
import static mega.privacy.android.app.utils.ChatUtil.shareNodeFromChat;
import static mega.privacy.android.app.utils.Constants.MULTIPLE_FORWARD_MESSAGES;
import static mega.privacy.android.app.utils.Constants.MULTIPLE_IMPORT_CONTACT_MESSAGES;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.Util.showSnackbar;

public class CopyListener extends BaseListener {

    private int counter;
    private int error = 0;
    private final int actionListener;
    private final long chatId;
    private final ArrayList<MegaChatMessage> messagesSelected;
    private final ChatController chatC;
    private ExportListener exportListener;

    public CopyListener(int action, ArrayList<MegaChatMessage> messagesSelected, int counter, Context context, ChatController chatC, long chatId) {
        super(context);
        this.actionListener = action;
        this.context = context;
        this.counter = counter;
        this.messagesSelected = messagesSelected;
        this.chatC = chatC;
        this.chatId = chatId;
    }

    public CopyListener(int action, ArrayList<MegaChatMessage> messagesSelected, int counter, Context context, ChatController chatC, long chatId, ExportListener exportListener) {
        super(context);
        this.actionListener = action;
        this.context = context;
        this.counter = counter;
        this.messagesSelected = messagesSelected;
        this.chatC = chatC;
        this.chatId = chatId;
        this.exportListener = exportListener;
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_COPY) return;

        counter--;
        if (e.getErrorCode() != MegaError.API_OK) {
            logError("Error copying");
            error++;
        }

        if(counter != 0){
            return;
        }

        switch (actionListener) {
            case MULTIPLE_FORWARD_MESSAGES:
                if (error > 0) {
                    String message = getQuantityString(R.plurals.error_forwarding_messages, error);
                    Intent intent = new Intent(BROADCAST_ACTION_ERROR_COPYING_NODES);
                    intent.putExtra(ERROR_MESSAGE_TEXT, message);
                    MegaApplication.getInstance().sendBroadcast(intent);
                } else {
                    logDebug("Forward message");
                    chatC.forwardMessages(messagesSelected, chatId);
                }
                break;
            case MULTIPLE_IMPORT_CONTACT_MESSAGES:
                if (error > 0) {
                    if (exportListener != null) {
                        exportListener.errorImportingNodes();
                    }else{
                        showSnackbar(context, getQuantityString(R.plurals.context_link_export_error, error));
                    }
                } else {
                    MegaNode node = api.getNodeByHandle(request.getNodeHandle());
                    if (node == null) {
                        logWarning("Node is NULL");
                        return;
                    }

                    if (exportListener != null) {
                        exportListener.updateNodeHandle(messagesSelected.get(0).getMsgId(), node.getHandle());
                        logDebug("Export Node");
                        api.exportNode(node, exportListener);
                    } else {
                        logDebug("Share Node");
                        shareNodeFromChat(context, node, chatId, messagesSelected.get(0).getMsgId());
                    }
                }
                break;
        }
    }
}
