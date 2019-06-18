package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaParticipantsChatLollipopAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.CacheFolderManager.isFileAvailable;

public class ChatParticipantAvatarListener implements MegaRequestListenerInterface {

    Context context;
    MegaParticipantsChatLollipopAdapter.ViewHolderParticipantsList holder;
    MegaParticipantsChatLollipopAdapter adapter;

    public ChatParticipantAvatarListener(Context context, MegaParticipantsChatLollipopAdapter.ViewHolderParticipantsList holder, MegaParticipantsChatLollipopAdapter adapter) {
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

            if (holder.contactMail != null) {
                if (holder.contactMail.compareTo(request.getEmail()) == 0){
                    File avatar = buildAvatarFile(context, holder.contactMail + ".jpg");
                    Bitmap bitmap = null;
                    if (isFileAvailable(avatar)){
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
            else{
                if (holder.userHandle.compareTo(request.getEmail()) == 0){
                    File avatar = buildAvatarFile(context, holder.userHandle + ".jpg");
                    Bitmap bitmap = null;
                    if (isFileAvailable(avatar)){
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
                else{
                    log("Handle do not match");
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api,
                                        MegaRequest request, MegaError e) {
        log("onRequestTemporaryError");
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        // TODO Auto-generated method stub
    }

    private static void log(String log) {
        Util.log("ChatParticipantAvatarListener", log);
    }

}
