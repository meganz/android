package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import android.content.Intent;

import java.util.ArrayList;

import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

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

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
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

        if (request != null && request.getLink() != null) {
            if (shareIntent == null)
                return;

            if (e.getErrorCode() != API_OK) {
                numberError++;
            } else if (exportedLinks != null) {
                exportedLinks.append(request.getLink())
                        .append("\n\n");
            }

            if (numberError != 0) {
                logError(numberError + " errors exporting nodes");
                showSnackbar(context, context.getResources()
                        .getQuantityString(R.plurals.context_link_export_error, numberExport));
                return;
            }

            if (exportedLinks == null) {
                startShareIntent(context, shareIntent, request.getLink());
                return;
            }

            pendingExport--;

            if (pendingExport == 0 && numberError < numberExport) {
                startShareIntent(context, shareIntent, exportedLinks.toString());
            }

            return;
        }

        if (e.getErrorCode() == MegaError.API_OK && request.getLink() != null) {
            if (shareIntent != null) {
                startShareIntent(context, shareIntent, request.getLink());
            }
        } else {
            logError("Error exporting node: " + e.getErrorString());

            ChatController chatC = new ChatController(context);
            if(messages != null && !messages.isEmpty()){
                logDebug("Several nodes to import to MEGA and export");
                chatC.importNode(messageId, chatId, true);
            }else{
                logDebug("One node to import to MEGA and export");
                chatC.importNode(messageId, chatId, true);
            }
        }
    }
}
