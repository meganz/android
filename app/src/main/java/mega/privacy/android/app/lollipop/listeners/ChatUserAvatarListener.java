package mega.privacy.android.app.lollipop.listeners;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;

import java.io.File;

import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class ChatUserAvatarListener implements MegaRequestListenerInterface {

    Context context;
    RecyclerView.ViewHolder holder;
    RecyclerView.Adapter adapter;

    public ChatUserAvatarListener(Context context, RecyclerView.ViewHolder holder, RecyclerView.Adapter adapter) {
        this.context = context;

        if(adapter instanceof MegaListChatLollipopAdapter){
            this.holder = (MegaListChatLollipopAdapter.ViewHolderChatList) holder;
            this.adapter = (MegaListChatLollipopAdapter) adapter;
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart()");
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish(): "+e.getErrorCode());
        if (e.getErrorCode() == MegaError.API_OK){
            boolean avatarExists = false;

            if(holder instanceof MegaListChatLollipopAdapter.ViewHolderChatList){
                if(((MegaListChatLollipopAdapter.ViewHolderNormalChatList)holder)!=null && ((MegaListChatLollipopAdapter.ViewHolderNormalChatList)holder).getContactMail()!=null && request.getEmail()!=null){
                    if (((MegaListChatLollipopAdapter.ViewHolderNormalChatList)holder).getContactMail().compareTo(request.getEmail()) == 0){
                        File avatar = null;
                        if (context.getExternalCacheDir() != null){
                            avatar = new File(context.getExternalCacheDir().getAbsolutePath(), ((MegaListChatLollipopAdapter.ViewHolderNormalChatList)holder).getContactMail() + ".jpg");
                        }
                        else{
                            avatar = new File(context.getCacheDir().getAbsolutePath(), ((MegaListChatLollipopAdapter.ViewHolderNormalChatList)holder).getContactMail() + ".jpg");
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
                                    ((MegaListChatLollipopAdapter.ViewHolderNormalChatList)holder).setImageView(bitmap);
                                }
                            }
                        }
                    }
                }
                else{
                    log("Adapter cannot be updated - null");
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