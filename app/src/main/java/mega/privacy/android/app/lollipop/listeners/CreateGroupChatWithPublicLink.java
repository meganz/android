package mega.privacy.android.app.lollipop.listeners;


import android.content.Context;

import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;

public class CreateGroupChatWithPublicLink implements MegaChatRequestListenerInterface {

    Context context;
    String title;

    public CreateGroupChatWithPublicLink(Context context, String title) {
        this.context = context;
        this.title = title;
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish: "+e.getErrorCode()+"_"+request.getRequestString());

        if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                log("Chat created - get link");
                api.createChatLink(request.getChatHandle(), this);
            }
            else{
                if(context instanceof ManagerActivityLollipop){
                    ((ManagerActivityLollipop) context).onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle(), false);
                }
                else if(context instanceof FileExplorerActivityLollipop){
                    ((FileExplorerActivityLollipop) context).onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle(), false);
                }
                else if(context instanceof ChatExplorerActivity){
                    ((ChatExplorerActivity) context).onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle(), false);
                }
            }
        }
        else if (request.getType() == MegaChatRequest.TYPE_CHAT_LINK_HANDLE) {
            log("MegaChatRequest.TYPE_CHAT_LINK_HANDLE finished!!!");
            if (request.getFlag() == false) {
               if (request.getNumRetry() == 1) {
                   log("Chat link exported!");

                   if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                       if(request.getText()!=null){
                           if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                               android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                               clipboard.setText(request.getText());
                           } else {
                               android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                               android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", request.getText());
                               clipboard.setPrimaryClip(clip);
                           }
                       }
                   }

                   if(context instanceof ManagerActivityLollipop){
                       ((ManagerActivityLollipop) context).onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle(), true);
                   }
                   else if(context instanceof FileExplorerActivityLollipop){
                       ((FileExplorerActivityLollipop) context).onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle(), true);
                   }
                   else if(context instanceof ChatExplorerActivity){
                       ((ChatExplorerActivity) context).onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle(), true);
                   }
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    private static void log(String log) {
        Util.log("CreateGroupChatWithPublicLink", log);
    }
}
