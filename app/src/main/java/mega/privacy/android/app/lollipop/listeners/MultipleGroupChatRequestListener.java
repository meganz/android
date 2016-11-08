package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;

//Listener for  multiselect
public class MultipleGroupChatRequestListener implements MegaChatRequestListenerInterface {

    Context context;

    public MultipleGroupChatRequestListener(Context context) {
        super();
        this.context = context;
    }

    int counter = 0;
    int error = 0;
    int max_items = 0;
    String message;

    private static void log(String log) {
        Util.log("MultipleGroupChatRequestListener", log);
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
        counter++;
        if(counter>max_items){
            max_items=counter;
        }
        log("Counter on RequestStart: "+counter);
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        counter--;
        if (e.getErrorCode() != MegaError.API_OK){
            error++;
        }
        int requestType = request.getType();
        log("Counter on RequestFinish: "+counter);
        log("Error on RequestFinish: "+error);
//			MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
//			if(node!=null){
//				log("onRequestTemporaryError: "+node.getName());
//			}
        if(counter==0){
            switch (requestType) {

                case MegaRequest.TYPE_INVITE_CONTACT:{

                    if(request.getNumber()==MegaContactRequest.INVITE_ACTION_REMIND){
                        log("remind contact request finished");
                        message = context.getString(R.string.number_correctly_reinvite_contact_request, max_items);
                    }
                    else if(request.getNumber()==MegaContactRequest.INVITE_ACTION_DELETE){
                        log("delete contact request finished");
                        if(error>0){
                            message = context.getString(R.string.number_no_delete_contact_request, max_items-error, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_delete_contact_request, max_items);
                        }
                    }
                    else if (request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD){
                        log("invite contact request finished");
                        if(error>0){
                            message = context.getString(R.string.number_no_invite_contact_request, max_items-error, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_invite_contact_request, max_items);
                        }
                    }
                    break;
                }

                default:
                    break;
            }
            ((ManagerActivityLollipop) context).showSnackbar(message);
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("Counter on onRequestTemporaryError: "+counter);
    }
};
