package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;

//Listener for  multi attach
public class MultipleAttachChatListener implements MegaChatRequestListenerInterface {

    Context context;
    boolean sendMultipleFiles;
    int counter = 0;
    int error = 0;
    int max_items = 0;
    long chatId = -1;

    public MultipleAttachChatListener(Context context, long chatId, boolean sendMultipleFiles, int counter) {
        super();
        this.context = context;
        this.chatId = chatId;
        this.counter = counter;
        this.max_items = counter;
        this.sendMultipleFiles = sendMultipleFiles;
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
        LogUtil.logDebug("onRequestStart");
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        counter--;
        if (e.getErrorCode() != MegaError.API_OK){
            error++;
            LogUtil.logError("Attach node error: " + e.getErrorString() + "__" + e.getErrorCode());
        }

        LogUtil.logDebug("Counter: " + counter);
        LogUtil.logDebug("Error: " + error);

        if(counter==0){
            int success = max_items - error;

            if(context instanceof ManagerActivityLollipop){
                if(success>0){
                    if(chatId==-1){
                        if(sendMultipleFiles){
                            ((ManagerActivityLollipop) context).showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, context.getResources().getQuantityString(R.plurals.files_send_to_chat_success, 10), -1);
                        }
                        else{
                            ((ManagerActivityLollipop) context).showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, context.getResources().getQuantityString(R.plurals.files_send_to_chat_success, 1), -1);
                        }
                    }
                    else{
                        ((ManagerActivityLollipop) context).showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, null, chatId);
                    }
                }
                else{
                    ((ManagerActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.files_send_to_chat_error), -1);
                }
            }
            else if(context instanceof ContactInfoActivityLollipop){
                if(success>0){
                   ((ContactInfoActivityLollipop) context).showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, null, chatId);
                }
                else{
                    ((ContactInfoActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.files_send_to_chat_error), -1);
                }
            }
            else if (context instanceof FullScreenImageViewerLollipop) {
                if(success>0){
                    if(chatId==-1){
                        ((FullScreenImageViewerLollipop) context).showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, context.getResources().getQuantityString(R.plurals.files_send_to_chat_success, 1), -1);
                    }
                    else{
                        ((FullScreenImageViewerLollipop) context).showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, null, chatId);
                    }
                }
                else{
                    ((FullScreenImageViewerLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.files_send_to_chat_error), -1);
                }
            }
            else if (context instanceof AudioVideoPlayerLollipop) {
                if(success>0){
                    if(chatId==-1){
                        ((AudioVideoPlayerLollipop) context).showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, context.getResources().getQuantityString(R.plurals.files_send_to_chat_success, 1), -1);
                    }
                    else{
                        ((AudioVideoPlayerLollipop) context).showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, null, chatId);
                    }
                }
                else{
                    ((AudioVideoPlayerLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.files_send_to_chat_error), -1);
                }
            }
            else if (context instanceof PdfViewerActivityLollipop) {
                if(success>0){
                    if(chatId==-1){
                        ((PdfViewerActivityLollipop) context).showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, context.getResources().getQuantityString(R.plurals.files_send_to_chat_success, 1), -1);
                    }
                    else{
                        ((PdfViewerActivityLollipop) context).showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, null, chatId);
                    }
                }
                else{
                    ((PdfViewerActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.files_send_to_chat_error), -1);
                }
            }
            else if (context instanceof FileInfoActivityLollipop) {
                if(success>0){
                    if(chatId==-1){
                        ((FileInfoActivityLollipop) context).showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, context.getResources().getQuantityString(R.plurals.files_send_to_chat_success, 1), -1);
                    }
                    else{
                        ((FileInfoActivityLollipop) context).showSnackbar(Constants.MESSAGE_SNACKBAR_TYPE, null, chatId);
                    }
                }
                else{
                    ((FileInfoActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.files_send_to_chat_error), -1);
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        LogUtil.logWarning("Counter: " + counter);
    }
};
