package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;

//Listener for  multi attach
public class MultipleForwardChatListener implements MegaChatRequestListenerInterface {

    Context context;

    public MultipleForwardChatListener(Context context, long chatId) {
        super();
        this.context = context;
        this.chatId = chatId;
    }

    int counter = 0;
    int error = 0;
    int max_items = 0;
    long chatId = -1;

    private static void log(String log) {
        Util.log("MultipleForwardChatListener", log);
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

        if(counter==0){
            int success = max_items - error;
            if(success>0){
                if(context instanceof ChatActivityLollipop){
                    if(max_items==1){
                        ((ChatActivityLollipop) context).openChatAfterForward(chatId);
                    }
                    else{
                        ((ChatActivityLollipop) context).openChatAfterForward(chatId);
                    }
                }
            }
            else{
                ((ChatActivityLollipop) context).showSnackbar(context.getString(R.string.messages_forwarded_error));
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("Counter on onRequestTemporaryError: "+counter);
    }
};
