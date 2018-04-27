package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;

//Listener for  multi attach
public class MultipleAttachChatListener implements MegaChatRequestListenerInterface {

    Context context;
    boolean sendMultipleFiles;

    public MultipleAttachChatListener(Context context, long chatId) {
        super();
        this.context = context;
        this.chatId = chatId;
        this.sendMultipleFiles = false;
    }

    public MultipleAttachChatListener(Context context, long chatId, boolean sendMultipleFiles) {
        super();
        this.context = context;
        this.chatId = chatId;
        this.sendMultipleFiles = sendMultipleFiles;
    }

    int counter = 0;
    int error = 0;
    int max_items = 0;
    long chatId = -1;

    private static void log(String log) {
        Util.log("MultipleAttachChatListener", log);
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
            log("Attach node error: "+e.getErrorString()+"__"+e.getErrorCode());
        }

        int requestType = request.getType();
        log("Counter on RequestFinish: "+counter);
        log("Error on RequestFinish: "+error);

        if(counter==0){
            int success = max_items - error;

            if(context instanceof ChatActivityLollipop){
                if(success>0){
                    if(max_items==1){
                        ((ChatActivityLollipop) context).openChatAfterForward(chatId);
                    }
                    else{
                        ((ChatActivityLollipop) context).openChatAfterForward(chatId);
                    }
                }
                else{
                    ((ChatActivityLollipop) context).showSnackbar(context.getString(R.string.messages_forwarded_error));
                }
            }
            else if(context instanceof ManagerActivityLollipop){
                if(success>0){
                    if(max_items==1){
                        ((ManagerActivityLollipop) context).openChat(chatId);
                    }
                    else{
                        if(sendMultipleFiles){
                            ((ManagerActivityLollipop) context).showSnackbar((context.getResources().getQuantityString(R.plurals.files_send_to_chat_success, 10)));
                        }
                        else{
                            ((ManagerActivityLollipop) context).showSnackbar((context.getResources().getQuantityString(R.plurals.files_send_to_chat_success, 1)));
                        }
                    }
                }
                else{
                    ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.files_send_to_chat_error));
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("Counter on onRequestTemporaryError: "+counter);
    }
};
