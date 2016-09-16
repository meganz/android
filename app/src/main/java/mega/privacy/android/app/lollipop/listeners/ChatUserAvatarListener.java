package mega.privacy.android.app.lollipop.listeners;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

import mega.privacy.android.app.lollipop.adapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class ChatUserAvatarListener implements MegaRequestListenerInterface {

    Context context;
    MegaListChatLollipopAdapter.ViewHolderChatList holder;
    MegaListChatLollipopAdapter adapter;

    public ChatUserAvatarListener(Context context, MegaListChatLollipopAdapter.ViewHolderChatList holder, MegaListChatLollipopAdapter adapter) {
        this.context = context;
        this.holder = holder;
        this.adapter = adapter;
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart()");
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish()");
        if (e.getErrorCode() == MegaError.API_OK){
            boolean avatarExists = false;

            if (holder.getContactMail().compareTo(request.getEmail()) == 0){
                File avatar = null;
                if (context.getExternalCacheDir() != null){
                    avatar = new File(context.getExternalCacheDir().getAbsolutePath(), holder.getContactMail() + ".jpg");
                }
                else{
                    avatar = new File(context.getCacheDir().getAbsolutePath(), holder.getContactMail() + ".jpg");
                }
                Bitmap bitmap = null;
                if (avatar.exists()){
                    if (avatar.length() > 0){
                        BitmapFactory.Options bOpts = new BitmapFactory.Options();
                        bOpts.inPurgeable = true;
                        bOpts.inInputShareable = true;
                        bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                        if (bitmap == null) {
                            avatar.delete();
                        }
                        else{
                            holder.setImageView(bitmap);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
        log("onRequestTemporaryError");
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        // TODO Auto-generated method stub
    }

    private static void log(String log) {
        Util.log("ChatUserAvatarListener", log);
    }

}