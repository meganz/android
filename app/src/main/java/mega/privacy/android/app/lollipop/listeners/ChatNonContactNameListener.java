package mega.privacy.android.app.lollipop.listeners;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

import mega.privacy.android.app.lollipop.adapters.MegaChatLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class ChatNonContactNameListener implements MegaChatRequestListenerInterface {

    Context context;
    MegaChatLollipopAdapter.ViewHolderMessageChatList holder;
    MegaChatLollipopAdapter adapter;
    boolean isUserHandle;

    public ChatNonContactNameListener(Context context, MegaChatLollipopAdapter.ViewHolderMessageChatList holder, MegaChatLollipopAdapter adapter, boolean isUserHandle) {
        this.context = context;
        this.holder = holder;
        this.adapter = adapter;
        this.isUserHandle = isUserHandle;
    }

    private static void log(String log) {
        Util.log("ChatNonContactNameListener", log);
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish()");
        if (e.getErrorCode() == MegaError.API_OK){
            if(request.getType()==MegaChatRequest.TYPE_GET_FIRSTNAME){
                log("Update firstname: "+holder.getCurrentPosition());
                if(isUserHandle){
                    if(holder.getUserHandle()==request.getUserHandle()){
                        holder.setFirstNameText(request.getText());
                        holder.setFirstNameReceived();
                        holder.setNameNonContact();
                    }
                    else{
                        log("Not match!!");
                    }
                }
                else{
                    holder.setFirstNameText(request.getText());
                    holder.setFirstNameReceived();
                    holder.setNameNonContact();
                }

            }
            else if(request.getType()==MegaChatRequest.TYPE_GET_LASTNAME){
                log("Update lastname: "+holder.getCurrentPosition());
                if(isUserHandle) {
                    if (holder.getUserHandle() == request.getUserHandle()) {
                        holder.setLastNameText(request.getText());
                        holder.setLastNameReceived();
                        holder.setNameNonContact();
                    } else {
                        log("Not match!!");
                    }
                }
                else{
                    holder.setLastNameText(request.getText());
                    holder.setLastNameReceived();
                    holder.setNameNonContact();
                }
            }

        }
        else{
            log("Error asking for name!");
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }
}