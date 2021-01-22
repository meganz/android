package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import android.content.Intent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.utils.Constants;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import mega.privacy.android.app.activities.GetLinkActivity;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;
import static nz.mega.sdk.MegaError.*;
import static nz.mega.sdk.MegaRequest.*;

public class ExportListener extends BaseListener {
    private Intent shareIntent;

    private boolean removeExport;
    private int numberRemove;
    private int pendingRemove;
    private int numberError;

    private int numberExport;
    private int pendingExport;
    private StringBuilder exportedLinks;
    private long messageId = MEGACHAT_INVALID_HANDLE;
    private long chatId = MEGACHAT_INVALID_HANDLE;
    private ArrayList<AndroidMegaChatMessage> messages;
    final private HashMap<Long, Long> msgIdNodeHandle = new HashMap<>();

    public ExportListener(Context context) {
        super(context);
    }

    /**
     * Constructor used for the purpose of launch a view intent to share content through the link created when the request finishes
     *
     * @param context     current Context
     * @param shareIntent Intent to share the content
     */
    public ExportListener(Context context, Intent shareIntent) {
        super(context);
        this.shareIntent = shareIntent;
    }

    /**
     * Constructor used for the purpose of launch a view intent to share content through the link created when the request finishes
     *
     * @param context     current Context
     * @param shareIntent Intent to share the content
     */
    public ExportListener(Context context, Intent shareIntent, long messageId, long chatId){
        super(context);
        this.shareIntent = shareIntent;
        this.messageId = messageId;
        this.chatId = chatId;
        this.numberExport = 1;
        this.pendingExport = numberExport;
    }

    /**
     * Constructor used for the purpose of remove links of one or more nodes
     *
     * @param context       current Context
     * @param removeExport  true if the request is to remove links
     * @param numberRemove  number of nodes to remove the link
     */
    public ExportListener(Context context, boolean removeExport, int numberRemove) {
        super(context);
        this.removeExport = removeExport;
        this.numberRemove = this.pendingRemove = numberRemove;
    }

    /**
     * Constructor used for the purpose of export multiple nodes, then combine links with
     * already exported nodes, then share those links.
     *
     * @param context       current Context
     * @param numberExport  number of nodes to remove the link
     * @param exportedLinks links of already exported nodes
     * @param shareIntent Intent to share the content
     */
    public ExportListener(Context context, int numberExport, StringBuilder exportedLinks,
                          Intent shareIntent, ArrayList<AndroidMegaChatMessage> messages, long chatId) {
        super(context);
        this.shareIntent = shareIntent;
        this.messages = messages;
        this.chatId = chatId;
        this.numberExport = numberExport;
        this.pendingExport = numberExport;
        this.exportedLinks = exportedLinks;

        for(AndroidMegaChatMessage msg: messages){
            long msgId = msg.getMessage().getMsgId();
            long nodeHandle = msg.getMessage().getMegaNodeList().get(0).getHandle();
            msgIdNodeHandle.put(nodeHandle, msgId);
        }
    }

    /**
     * Constructor used for the purpose of export multiple nodes, then combine links with
     * already exported nodes, then share those links.
     *
     * @param context       current Context
     * @param numberExport  number of nodes to remove the link
     * @param exportedLinks links of already exported nodes
     * @param shareIntent Intent to share the content
     */
    public ExportListener(Context context, int numberExport, StringBuilder exportedLinks,
        Intent shareIntent) {
        super(context);
        this.shareIntent = shareIntent;
        this.numberExport = numberExport;
        this.pendingExport = numberExport;
        this.exportedLinks = exportedLinks;
    }

    /**
     * Method for updating the handle of the node associated with a message.
     * This is necessary when a node is to be shared and it is necessary to import the node into the cloud and use that node to get a share link.
     *
     * @param msgID         Message ID.
     * @param newNodeHandle node Handle of a node imported.
     */
    public void updateNodeHandle(long msgID, long newNodeHandle) {
        boolean isFoundAndRemoved = getKeyByValueAndRemoveIt(msgIdNodeHandle, msgID);
        if (isFoundAndRemoved) {
            msgIdNodeHandle.put(newNodeHandle, msgID);
        }
    }

    /**
     * Method to display a snackbar when all nodes have not been imported correctly.
     */
    public void errorImportingNodes() {
        numberError++;
        pendingExport--;
        if (pendingExport == 0) {
            logError(numberExport + " errors exporting nodes");
            showSnackbar(context, context.getResources()
                    .getQuantityString(R.plurals.context_link_export_error, numberExport));
        }
    }

    /**
     * Method to get the key in Map with the value and remove this entry.
     *
     * @param map   The Map.
     * @param value The value.
     * @param <T>   First param of the Map.
     * @param <E>   Second param of the Map.
     * @return True, if it has been found and removed. False, if not.
     */
    public static <T, E> boolean getKeyByValueAndRemoveIt(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                map.remove(entry.getKey());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestFinish(@NotNull MegaApiJava api, MegaRequest request, @NotNull MegaError e) {
        if (request.getType() != TYPE_EXPORT) return;

        if (removeExport) {
            pendingRemove--;

            if (e.getErrorCode() != API_OK) {
                numberError++;
            }

            if (pendingRemove == 0) {
                if (numberError > 0) {
                    logError("Removing link error");
                    showSnackbar(context, context.getResources().getQuantityString(R.plurals.context_link_removal_error, numberRemove));
                } else {
                    showSnackbar(context, context.getResources().getQuantityString(R.plurals.context_link_removal_success, numberRemove));
                }
            }

            return;
        }

        if (request == null)
            return;

        if (request.getLink() != null) {
            logDebug("The link is not null");

            if (e.getErrorCode() != API_OK) {
                numberError++;
            } else if (exportedLinks != null) {
                exportedLinks.append(request.getLink())
                        .append("\n\n");
            }

            if (numberError > 0) {
                logError(numberError + " errors exporting nodes");
                showSnackbar(context, context.getResources()
                        .getQuantityString(R.plurals.context_link_export_error, numberExport));
                return;
            }

            if (exportedLinks == null) {
                logDebug("Start share one item");
                if (shareIntent != null) {
                    startShareIntent(context, shareIntent, request.getLink());
                } else if (context instanceof GetLinkActivity) {
                    ((GetLinkActivity) context).setLink();
                }
                return;
            }

            pendingExport--;
            if (shareIntent != null && pendingExport == 0 && numberError < numberExport) {
                logDebug("Start share several items");
                startShareIntent(context, shareIntent, exportedLinks.toString());
            }
            return;
        }

        logError("Error exporting node: " + e.getErrorString());

        if (context instanceof GetLinkActivity
                && e.getErrorCode() != MegaError.API_EBUSINESSPASTDUE) {
            ((GetLinkActivity) context).showSnackbar(SNACKBAR_TYPE,
                    context.getString(R.string.context_no_link), MEGACHAT_INVALID_HANDLE);
        }

        ChatController chatC = new ChatController(context);
        if (messages == null || messages.isEmpty()) {
            logDebug("One node to import to MEGA and then share");
            chatC.setExportListener(null);
        } else {
            messageId = msgIdNodeHandle.get(request.getNodeHandle());
            logDebug("Several nodes to import to MEGA and then share");
            chatC.setExportListener(this);
        }

        chatC.importNode(messageId, chatId, Constants.IMPORT_TO_SHARE_OPTION);
    }
}
