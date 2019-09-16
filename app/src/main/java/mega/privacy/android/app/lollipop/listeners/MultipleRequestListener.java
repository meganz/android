package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megaachievements.AchievementsActivity;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ContactAttachmentActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.DBUtil;
import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;

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
    int max_items = 0;
    int actionListener = -1;
    String message;

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

        LogUtil.logWarning("Counter: " + counter);
//			MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
//			if(node!=null){
//				log("onRequestTemporaryError: "+node.getName());
//			}
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

        counter++;
        if(counter>max_items){
            max_items=counter;
        }
        LogUtil.logDebug("Counter: " + counter);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

        counter--;
        if (e.getErrorCode() != MegaError.API_OK){
            error++;
        }
        int requestType = request.getType();
        LogUtil.logDebug("Counter: " + counter);
        LogUtil.logDebug("Error: " + error);
//			MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
//			if(node!=null){
//				log("onRequestTemporaryError: "+node.getName());
//			}
        if(counter==0){
            switch (requestType) {
                case  MegaRequest.TYPE_MOVE:{
                    if (actionListener== Constants.MULTIPLE_SEND_RUBBISH){
                        LogUtil.logDebug("Move to rubbish request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_moved_to_rubbish, max_items-error) + context.getString(R.string.number_incorrectly_moved_to_rubbish, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_moved_to_rubbish, max_items);
                        }
                        if(context instanceof ManagerActivityLollipop) {
                            ManagerActivityLollipop managerActivity = (ManagerActivityLollipop) context;
                            managerActivity.refreshAfterMovingToRubbish();
                            DBUtil.resetAccountDetailsTimeStamp(context);
                        }
                        else {
                            ((ContactFileListActivityLollipop) context).refreshAfterMovingToRubbish();
                        }
                    }
                    else if (actionListener== Constants.MULTIPLE_RESTORED_FROM_RUBBISH){
                        LogUtil.logDebug("Restore nodes from rubbish request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_restored_from_rubbish, max_items-error) + context.getString(R.string.number_incorrectly_restored_from_rubbish, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_restored_from_rubbish, max_items);
                        }

                        ManagerActivityLollipop managerActivity = (ManagerActivityLollipop) context;
                        managerActivity.refreshAfterMovingToRubbish();
                        DBUtil.resetAccountDetailsTimeStamp(context);
                    }
                    else{
                        LogUtil.logDebug("Move nodes request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_moved, max_items-error) + context.getString(R.string.number_incorrectly_moved, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_moved, max_items);
                        }
                        ((ManagerActivityLollipop) context).refreshAfterMoving();
                    }
                    break;
                }
                case MegaRequest.TYPE_REMOVE:{
                    LogUtil.logDebug("Remove multi request finish");
                    if (actionListener==Constants.MULTIPLE_LEAVE_SHARE){
                        LogUtil.logDebug("Leave multi share");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_leaved, max_items-error) + context.getString(R.string.number_no_leaved, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_leaved, max_items);
                        }
                    }
                    else{
                        LogUtil.logDebug("Multi remove");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_removed, max_items-error) + context.getString(R.string.number_no_removed, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_removed, max_items);
                        }

                        ManagerActivityLollipop managerActivity = (ManagerActivityLollipop) context;
                        managerActivity.refreshAfterRemoving();
                        DBUtil.resetAccountDetailsTimeStamp(context);
                    }

                    break;
                }
                case MegaRequest.TYPE_REMOVE_CONTACT:{
                    LogUtil.logDebug("Multi contact remove request finish");
                    if(error>0){
                        message = context.getString(R.string.number_contact_removed, max_items-error) + context.getString(R.string.number_contact_not_removed, error);
                    }
                    else{
                        message = context.getString(R.string.number_contact_removed, max_items);
                    }

                    ((ManagerActivityLollipop) context).updateContactsView(true, false, false);
                    break;
                }
                case MegaRequest.TYPE_COPY:{
                    if (actionListener==Constants.MULTIPLE_CONTACTS_SEND_INBOX){
                        LogUtil.logDebug("Send to inbox multiple contacts request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_sent, max_items-error) + context.getString(R.string.number_no_sent, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_sent, max_items);
                        }
                    }
                    else if (actionListener==Constants.MULTIPLE_FILES_SEND_INBOX){
                        LogUtil.logDebug("Send to inbox multiple files request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_sent_multifile, max_items-error) + context.getString(R.string.number_no_sent_multifile, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_sent_multifile, max_items);
                        }
                    }
                    else if(actionListener==Constants.MULTIPLE_CHAT_IMPORT){
                        //Many files shared with one contacts
                        if(error>0){
                            message = context.getString(R.string.number_correctly_imported_from_chat, max_items-error) + context.getString(R.string.number_no_imported_from_chat, error);
                        }
                        else{
                            message = context.getString(R.string.import_success_message);
                        }
                    }
                    else{
                        LogUtil.logDebug("Copy request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_copied, max_items-error) + context.getString(R.string.number_no_copied, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_copied, max_items);
                        }

                        DBUtil.resetAccountDetailsTimeStamp(context);
                    }
                    break;
                }
                case MegaRequest.TYPE_INVITE_CONTACT:{

                    if(request.getNumber()==MegaContactRequest.INVITE_ACTION_REMIND){
                        LogUtil.logDebug("Remind contact request finished");
                        message = context.getString(R.string.number_correctly_reinvite_contact_request, max_items);
                    }
                    else if(request.getNumber()==MegaContactRequest.INVITE_ACTION_DELETE){
                        LogUtil.logDebug("Delete contact request finished");
                        if(error>0){
                            message = context.getString(R.string.number_no_delete_contact_request, max_items-error, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_delete_contact_request, max_items);
                        }
                    }
                    else if (request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD){
                        LogUtil.logDebug("Invite contact request finished");
                        if(error>0){
                            message = context.getString(R.string.number_no_invite_contact_request, max_items-error, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_invite_contact_request, max_items);
                        }
                    }
                    break;
                }
                case MegaRequest.TYPE_REPLY_CONTACT_REQUEST:{
                    LogUtil.logDebug("Multiple reply request sent");

                    if(error>0){
                        message = context.getString(R.string.number_incorrectly_invitation_reply_sent, max_items-error, error);
                    }
                    else{
                        message = context.getString(R.string.number_correctly_invitation_reply_sent, max_items);
                    }
                    break;
                }
                case MegaRequest.TYPE_SHARE:{
                    LogUtil.logDebug("Multiple share request finished");
                    if(actionListener==Constants.MULTIPLE_REMOVE_SHARING_CONTACTS){
                        if(error>0){
                            message = context.getString(R.string.context_no_removed_sharing_contacts);
                        }
                        else{
                            message = context.getString(R.string.context_correctly_removed_sharing_contacts);
                        }
                    }
                    else if(actionListener==Constants.MULTIPLE_CONTACTS_SHARE){
                        //TODO change UI
                        //One file shared with many contacts
                        if(error>0){
                            message = context.getString(R.string.number_contact_file_shared_correctly, max_items-error) + context.getString(R.string.number_contact_file_not_shared_, error);
                        }
                        else{
                            message = context.getString(R.string.number_contact_file_shared_correctly, max_items);
                        }
                    }
                    else if(actionListener==Constants.MULTIPLE_FILE_SHARE){
                        //Many files shared with one contacts
                        if(error>0){
                            message = context.getString(R.string.number_correctly_shared, max_items-error) + context.getString(R.string.number_no_shared, error);
                        }
                        else{
                            message = context.getString(R.string.context_correctly_shared);
                        }
                    }
                    else{
                        if(error>0){
                            if(request.getAccess()== MegaShare.ACCESS_UNKNOWN){
                                message = context.getString(R.string.context_no_shared_number_removed, error);
                            }
                            else{
                                message = context.getString(R.string.context_no_shared_number, error);
                            }
                        }
                        else{
                            if(request.getAccess()==MegaShare.ACCESS_UNKNOWN){
                                message = context.getString(R.string.context_correctly_shared_removed);
                            }
                            else{
                                message = context.getString(R.string.context_correctly_shared);
                            }
                        }
                    }
                }
                default:
                    break;
            }
            if(context instanceof ManagerActivityLollipop){
                ((ManagerActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, message, -1);
            }
            else if(context instanceof ChatActivityLollipop){
                ((ChatActivityLollipop) context).removeProgressDialog();
                ((ChatActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, message, -1);
            }
            else if(context instanceof ContactAttachmentActivityLollipop){
                ((ContactAttachmentActivityLollipop) context).showSnackbar(message);
            }
            else if(context instanceof AchievementsActivity){
                ((AchievementsActivity) context).showInviteConfirmationDialog();
            }
            else if(context instanceof AddContactActivityLollipop){
                ((AddContactActivityLollipop) context).showSnackbar(message);
            }
            else if(context instanceof NodeAttachmentHistoryActivity){
                ((NodeAttachmentHistoryActivity) context).removeProgressDialog();
                ((NodeAttachmentHistoryActivity) context).showSnackbar(Constants.SNACKBAR_TYPE, message);
            }
        }
    }
}
