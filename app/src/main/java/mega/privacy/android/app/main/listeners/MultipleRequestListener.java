package mega.privacy.android.app.main.listeners;

import static mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog;
import static mega.privacy.android.app.utils.Constants.MULTIPLE_LEAVE_SHARE;
import static mega.privacy.android.app.utils.Constants.MULTIPLE_SEND_RUBBISH;
import static mega.privacy.android.app.utils.DBUtil.resetAccountDetailsTimeStamp;
import static mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString;
import static mega.privacy.android.app.utils.StringResourcesUtils.getString;
import static mega.privacy.android.app.utils.Util.showSnackbar;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.main.megachat.ChatActivity;
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import timber.log.Timber;

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
    int errorExist = 0;
    int max_items = 0;
    int actionListener = -1;
    String message;

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {


    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        Timber.w("Counter: %s", counter);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

        counter++;
        if (counter > max_items) {
            max_items = counter;
        }
        Timber.d("Counter: %s", counter);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

        counter--;
        if (e.getErrorCode() != MegaError.API_OK) {
            if (e.getErrorCode() == MegaError.API_EMASTERONLY) {
                errorBusiness++;
            } else if (e.getErrorCode() == MegaError.API_EEXIST) {
                errorExist++;
            }

            error++;
        }
        int requestType = request.getType();
        Timber.d("Counter: %s", counter);
        Timber.d("Error: %s", error);
        if (counter == 0) {
            switch (requestType) {
                case MegaRequest.TYPE_MOVE: {
                    if (actionListener == MULTIPLE_SEND_RUBBISH) {
                        Timber.d("Move to rubbish request finished");
                        int success_items = max_items - error;
                        if (error > 0 && (success_items > 0)) {
                            if (error == 1) {
                                message = getQuantityString(R.plurals.nodes_correctly_and_node_incorrectly_moved_to_rubbish, success_items, success_items);
                            } else if (success_items == 1) {
                                message = getQuantityString(R.plurals.node_correctly_and_nodes_incorrectly_moved_to_rubbish, error, error);
                            } else {
                                message = getQuantityString(R.plurals.number_correctly_moved_to_rubbish, success_items, success_items)
                                        + ". " + getQuantityString(R.plurals.number_incorrectly_moved_to_rubbish, error, error);
                            }
                        } else if (error > 0) {
                            message = getQuantityString(R.plurals.number_incorrectly_moved_to_rubbish, error, error);
                        } else {
                            message = getQuantityString(R.plurals.number_correctly_moved_to_rubbish, success_items, success_items);
                        }
                    }
                    break;
                }
                case MegaRequest.TYPE_REMOVE: {
                    Timber.d("Remove multi request finish");
                    if (actionListener == MULTIPLE_LEAVE_SHARE) {
                        Timber.d("Leave multi share");
                        if (error > 0) {
                            if (error == errorBusiness) {
                                message = e.getErrorString();
                            } else {
                                message = context.getString(R.string.number_correctly_leaved, max_items - error) + context.getString(R.string.number_no_leaved, error);
                            }
                        } else {
                            message = context.getString(R.string.number_correctly_leaved, max_items);
                        }
                    }

                    break;
                }
                case MegaRequest.TYPE_COPY: {
                    Timber.d("Copy request finished");
                    if (error > 0) {
                        if (e.getErrorCode() == MegaError.API_EOVERQUOTA && api.isForeignNode(request.getParentHandle())) {
                            showForeignStorageOverQuotaWarningDialog(context);
                        }

                        message = context.getString(R.string.number_correctly_copied, max_items - error) + context.getString(R.string.number_no_copied, error);
                    } else {
                        message = context.getString(R.string.number_correctly_copied, max_items);
                    }

                    resetAccountDetailsTimeStamp();
                    break;
                }
                case MegaRequest.TYPE_INVITE_CONTACT: {

                    if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD) {
                        Timber.d("Invite contact request finished");
                        if (errorExist > 0) {
                            message = getString(R.string.number_existing_invite_contact_request, errorExist);
                            int success = max_items - error;

                            if (success > 0) {
                                message += "\n" + getQuantityString(R.plurals.number_correctly_invite_contact_request, success, success);
                            }
                        } else if (error > 0) {
                            message = getString(R.string.number_no_invite_contact_request, max_items - error, error);
                        } else {
                            message = getQuantityString(R.plurals.number_correctly_invite_contact_request, max_items, max_items);
                        }
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
