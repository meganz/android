package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
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

                case MegaChatRequest.TYPE_INVITE_TO_CHATROOM:{

                    log("invite to chatRoom request finished");
                    if(error>0){
                        message = context.getString(R.string.number_no_add_participant_request, max_items-error, error);
                    }
                    else{
                        message = context.getString(R.string.number_correctly_add_participant, max_items);
                    }
                    break;
                }

                default:
                    break;
            }

            if(context instanceof GroupChatInfoActivityLollipop){
                ((GroupChatInfoActivityLollipop) context).setParticipants();
                ((GroupChatInfoActivityLollipop) context).showSnackbar(message);
            }
            else if(context instanceof ChatActivityLollipop){
                ((ChatActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, message, -1);
            }

        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("Counter on onRequestTemporaryError: "+counter);
    }
};
