package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

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

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
        counter++;
        if(counter>max_items){
            max_items=counter;
        }
        logDebug("Counter: " + counter);
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
        logDebug("Counter: " + counter);
        logDebug("Error: " + error);
//			MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
//			if(node!=null){
//				log("onRequestTemporaryError: "+node.getName());
//			}
        if(counter==0){
            switch (requestType) {

                case MegaChatRequest.TYPE_INVITE_TO_CHATROOM:{

                    logDebug("Invite to chatRoom request finished");
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
                ((GroupChatInfoActivityLollipop) context).updateParticipants();
                ((GroupChatInfoActivityLollipop) context).showSnackbar(message);
            }
            else if(context instanceof ChatActivityLollipop){
                ((ChatActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, message, -1);
            }

        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        logWarning("Counter: " + counter);
    }
};
