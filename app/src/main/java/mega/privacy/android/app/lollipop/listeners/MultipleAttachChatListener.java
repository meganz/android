package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import java.util.ArrayList;

import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.R;
import mega.privacy.android.app.audioplayer.AudioPlayerActivity;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

//Listener for  multi attach
public class MultipleAttachChatListener implements MegaChatRequestListenerInterface {

    Context context;
    int counter = 0;
    int error = 0;
    int max_items = 0;
    long chatId = -1;

    public MultipleAttachChatListener(Context context, long chatId, int counter) {
        super();
        this.context = context;
        this.chatId = chatId;
        this.counter = counter;
        this.max_items = counter;
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
        logDebug("onRequestStart");
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        counter--;
        if (e.getErrorCode() != MegaError.API_OK){
            error++;
            logError("Attach node error: " + e.getErrorString() + "__" + e.getErrorCode());
        } else if (context instanceof ChatActivityLollipop) {
            ((ChatActivityLollipop) context).sendMessageToUI(new AndroidMegaChatMessage(request.getMegaChatMessage()));
        }

        logDebug("Counter: " + counter);
        logDebug("Error: " + error);

        if(counter==0){
            int success = max_items - error;

            if(context instanceof ManagerActivityLollipop){
                if(success>0){
                    if(chatId==-1){
                        ((ManagerActivityLollipop) context).showSnackbar(MESSAGE_SNACKBAR_TYPE, null, -1);
                    }
                    else{
                        ((ManagerActivityLollipop) context).showSnackbar(MESSAGE_SNACKBAR_TYPE, null, chatId);
                    }
                }
                else{
                    ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.files_send_to_chat_error), -1);
                }
            }
            else if(context instanceof ContactInfoActivityLollipop){
                if(success>0){
                   ((ContactInfoActivityLollipop) context).showSnackbar(MESSAGE_SNACKBAR_TYPE, null, chatId);
                }
                else{
                    ((ContactInfoActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.files_send_to_chat_error), -1);
                }
            }
            else if (context instanceof FullScreenImageViewerLollipop) {
                if(success>0){
                    if(chatId==-1){
                        ((FullScreenImageViewerLollipop) context).showSnackbar(MESSAGE_SNACKBAR_TYPE, null, -1);
                    }
                    else{
                        ((FullScreenImageViewerLollipop) context).showSnackbar(MESSAGE_SNACKBAR_TYPE, null, chatId);
                    }
                }
                else{
                    ((FullScreenImageViewerLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.files_send_to_chat_error), -1);
                }
            }
            else if (context instanceof AudioVideoPlayerLollipop) {
                if(success>0){
                    if(chatId==-1){
                        ((AudioVideoPlayerLollipop) context).showSnackbar(MESSAGE_SNACKBAR_TYPE, null, -1);
                    }
                    else{
                        ((AudioVideoPlayerLollipop) context).showSnackbar(MESSAGE_SNACKBAR_TYPE, null, chatId);
                    }
                }
                else{
                    ((AudioVideoPlayerLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.files_send_to_chat_error), -1);
                }
            }
            else if (context instanceof PdfViewerActivityLollipop) {
                if(success>0){
                    if(chatId==-1){
                        ((PdfViewerActivityLollipop) context).showSnackbar(MESSAGE_SNACKBAR_TYPE, null, -1);
                    }
                    else{
                        ((PdfViewerActivityLollipop) context).showSnackbar(MESSAGE_SNACKBAR_TYPE, null, chatId);
                    }
                }
                else{
                    ((PdfViewerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.files_send_to_chat_error), -1);
                }
            }
            else if (context instanceof FileInfoActivityLollipop) {
                if(success>0){
                    if(chatId==-1){
                        ((FileInfoActivityLollipop) context).showSnackbar(MESSAGE_SNACKBAR_TYPE, null, -1);
                    }
                    else{
                        ((FileInfoActivityLollipop) context).showSnackbar(MESSAGE_SNACKBAR_TYPE, null, chatId);
                    }
                }
                else{
                    ((FileInfoActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.files_send_to_chat_error), -1);
                }
            } else if (context instanceof ChatActivityLollipop) {
                if (success > 0) {
                    ((ChatActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getResources().getQuantityString(R.plurals.files_send_to_chat_success, success, success), -1);

                } else {
                    ((ChatActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.files_send_to_chat_error), -1);
                }
            } else if (context instanceof AudioPlayerActivity) {
                if(success>0) {
                    if(chatId==-1) {
                        ((AudioPlayerActivity) context).showSnackbar(MESSAGE_SNACKBAR_TYPE, null,
                                MEGACHAT_INVALID_HANDLE);
                    } else {
                        ((AudioPlayerActivity) context).showSnackbar(MESSAGE_SNACKBAR_TYPE, null,
                                chatId);
                    }
                } else {
                    ((AudioPlayerActivity) context).showSnackbar(SNACKBAR_TYPE,
                            context.getString(R.string.files_send_to_chat_error),
                            MEGACHAT_INVALID_HANDLE);
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        logWarning("Counter: " + counter);
    }
};
