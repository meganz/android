package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import android.content.Intent;

import mega.privacy.android.app.activities.GetLinkActivity;
import mega.privacy.android.app.interfaces.GetLinkInterface;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

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
        Intent shareIntent) {
        super(context);

        this.numberExport = numberExport;
        this.pendingExport = numberExport;
        this.exportedLinks = exportedLinks;
        this.shareIntent = shareIntent;
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

        if (exportedLinks != null) {
            if (e.getErrorCode() != API_OK) {
                numberError++;
            } else {
                exportedLinks.append(request.getLink())
                    .append("\n\n");
            }

            pendingExport--;
            if (pendingExport == 0) {
                if (numberError < numberExport && shareIntent != null) {
                    startShareIntent(context, shareIntent, exportedLinks.toString());
                }
                if (numberError != 0) {
                    logError(numberError + " errors exporting nodes");
                    showSnackbar(context, context.getResources()
                        .getQuantityString(R.plurals.context_link_export_error, numberExport));
                }
            }
            return;
        }

        if (e.getErrorCode() == MegaError.API_OK && request.getLink() != null) {
            if (shareIntent != null) {
                startShareIntent(context, shareIntent, request.getLink());
            } else if (context instanceof GetLinkActivity) {
                ((GetLinkActivity) context).setLink();
            }
        } else {
            logError("Error exporting node: " + e.getErrorString());

            if (context instanceof GetLinkActivity
                    && e.getErrorCode() != MegaError.API_EBUSINESSPASTDUE) {
                ((GetLinkActivity) context).showSnackbar(SNACKBAR_TYPE,
                        context.getString(R.string.context_no_link), MEGACHAT_INVALID_HANDLE);
            }
        }
    }
}
