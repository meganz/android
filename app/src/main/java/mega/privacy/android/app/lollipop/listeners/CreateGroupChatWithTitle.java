package mega.privacy.android.app.lollipop.listeners;


import android.content.Context;

import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;

public class CreateGroupChatWithTitle implements MegaChatRequestListenerInterface {

    Context context;
    String title;

    public CreateGroupChatWithTitle(Context context, String title) {
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
                log("Chat created - set title");
                api.setChatTitle(request.getChatHandle(), title, this);
            }
            else{
                if(context instanceof ManagerActivityLollipop){
                    ((ManagerActivityLollipop) context).onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle(), false);
                }
                else if(context instanceof FileExplorerActivityLollipop){
                    ((FileExplorerActivityLollipop) context).onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle());
                }
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_EDIT_CHATROOM_NAME) {
            log("Change title");
            if(context instanceof ManagerActivityLollipop){
                ((ManagerActivityLollipop) context).onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle(), true);
            }
            else if(context instanceof FileExplorerActivityLollipop){
                ((FileExplorerActivityLollipop) context).onRequestFinishCreateChat(e.getErrorCode(), request.getChatHandle());
            }

        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    private static void log(String log) {
        Util.log("CreateGroupChatWithTitle", log);
    }
}
