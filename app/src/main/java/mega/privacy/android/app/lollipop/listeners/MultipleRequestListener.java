package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.ChatActivity;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.DBUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.StringResourcesUtils.*;
import static mega.privacy.android.app.utils.Util.showSnackbar;

//Listener for  multiselect
public class MultipleRequestListener implements MegaRequestListenerInterface {

    Context context;

    public MultipleRequestListener(int action, Context context) {
        super();
        this.actionListener = action;
        this.context = context;
    }

    int counter = 0;
    int error = 0;
    int errorBusiness = 0;
    int errorExist= 0;
    int max_items = 0;
    int actionListener = -1;
    String message;

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        logWarning("Counter: " + counter);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

        counter++;
        if(counter>max_items){
            max_items=counter;
        }
        logDebug("Counter: " + counter);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

        counter--;
        if (e.getErrorCode() != MegaError.API_OK){
            if (e.getErrorCode() == MegaError.API_EMASTERONLY) {
                errorBusiness++;
            } else if (e.getErrorCode() == MegaError.API_EEXIST) {
                errorExist++;
            }

            error++;
        }
        int requestType = request.getType();
        logDebug("Counter: " + counter);
        logDebug("Error: " + error);
        if(counter==0){
            switch (requestType) {
                case  MegaRequest.TYPE_MOVE:{
                    if (actionListener == MULTIPLE_SEND_RUBBISH) {
                        logDebug("Move to rubbish request finished");
                        int success_items = max_items - error;
                        if (error > 0 && (success_items > 0)) {
                            if (error == 1 && (success_items == 1)) {
                                message = getString(R.string.node_correctly_and_node_incorrectly_moved_to_rubbish);
                            } else if (error == 1) {
                                message = getString(R.string.nodes_correctly_and_node_incorrectly_moved_to_rubbish, success_items);
                            } else if (success_items == 1) {
                                message = getString(R.string.node_correctly_and_nodes_incorrectly_moved_to_rubbish, error);
                            } else {
                                message = getString(R.string.nodes_correctly_and_nodes_incorrectly_moved_to_rubbish, success_items, error);
                            }
                        } else if (error > 0) {
                            message = getQuantityString(R.plurals.number_incorrectly_moved_to_rubbish, error, error);
                        } else {
                            message = getQuantityString(R.plurals.number_correctly_moved_to_rubbish, success_items, success_items);
                        }
                    }
                    break;
                }
                case MegaRequest.TYPE_REMOVE:{
                    logDebug("Remove multi request finish");
                    if (actionListener==MULTIPLE_LEAVE_SHARE){
                        logDebug("Leave multi share");
                        if(error>0){
                            if (error == errorBusiness) {
                                message = e.getErrorString();
                            } else {
                                message = context.getString(R.string.number_correctly_leaved, max_items - error) + context.getString(R.string.number_no_leaved, error);
                            }
                        }
                        else {
                            message = context.getString(R.string.number_correctly_leaved, max_items);
                        }
                    }

                    break;
                }
                case MegaRequest.TYPE_REMOVE_CONTACT:{
                    logDebug("Multi contact remove request finish");
                    if(error>0){
                        message = context.getString(R.string.number_contact_removed, max_items-error) + context.getString(R.string.number_contact_not_removed, error);
                    }
                    else{
                        message = context.getString(R.string.number_contact_removed, max_items);
                    }

                    break;
                }
                case MegaRequest.TYPE_COPY:{
                    if (actionListener==MULTIPLE_CONTACTS_SEND_INBOX){
                        logDebug("Send to inbox multiple contacts request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_sent, max_items-error) + context.getString(R.string.number_no_sent, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_sent, max_items);
                        }
                    }
                    else if (actionListener==MULTIPLE_FILES_SEND_INBOX){
                        logDebug("Send to inbox multiple files request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_sent_multifile, max_items-error) + context.getString(R.string.number_no_sent_multifile, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_sent_multifile, max_items);
                        }
                    }
                    else if(actionListener==MULTIPLE_CHAT_IMPORT){
                        //Many files shared with one contacts
                        if(error>0){
                            message = context.getString(R.string.number_correctly_imported_from_chat, max_items-error) + context.getString(R.string.number_no_imported_from_chat, error);
                        }
                        else{
                            message = context.getString(R.string.import_success_message);
                        }
                    }
                    else{
                        logDebug("Copy request finished");
                        if(error>0){
                            if (e.getErrorCode() == MegaError.API_EOVERQUOTA && api.isForeignNode(request.getParentHandle())) {
                                showForeignStorageOverQuotaWarningDialog(context);
                            }

                            message = context.getString(R.string.number_correctly_copied, max_items-error) + context.getString(R.string.number_no_copied, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_copied, max_items);
                        }

                        resetAccountDetailsTimeStamp();
                    }
                    break;
                }
                case MegaRequest.TYPE_INVITE_CONTACT:{

                    if(request.getNumber()==MegaContactRequest.INVITE_ACTION_REMIND){
                        logDebug("Remind contact request finished");
                        message = context.getString(R.string.number_correctly_reinvite_contact_request, max_items);
                    }
                    else if(request.getNumber()==MegaContactRequest.INVITE_ACTION_DELETE){
                        logDebug("Delete contact request finished");
                        if(error>0){
                            message = context.getString(R.string.number_no_delete_contact_request, max_items-error, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_delete_contact_request, max_items);
                        }
                    }
                    else if (request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD){
                        logDebug("Invite contact request finished");
                        if (errorExist > 0) {
                            message = getString(R.string.number_existing_invite_contact_request, errorExist);
                            int success = max_items - error;

                            if (success > 0) {
                                message += "\n" + getString(R.string.number_correctly_invite_contact_request, success);
                            }
                        } else if (error > 0) {
                            message = getString(R.string.number_no_invite_contact_request, max_items - error, error);
                        } else {
                            message = getString(R.string.number_correctly_invite_contact_request, max_items);
                        }
                    }
                    break;
                }
                case MegaRequest.TYPE_REPLY_CONTACT_REQUEST:{
                    logDebug("Multiple reply request sent");

                    if(error>0){
                        message = context.getString(R.string.number_incorrectly_invitation_reply_sent, max_items-error, error);
                    }
                    else{
                        message = context.getString(R.string.number_correctly_invitation_reply_sent, max_items);
                    }
                    break;
                }
                default:
                    break;
            }

            if (context instanceof ChatActivity) {
                ((ChatActivity) context).removeProgressDialog();
            } else if (context instanceof NodeAttachmentHistoryActivity) {
                ((NodeAttachmentHistoryActivity) context).removeProgressDialog();
            }

            showSnackbar(context, message);
        }
    }
}
