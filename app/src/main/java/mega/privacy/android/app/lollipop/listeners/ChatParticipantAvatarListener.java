package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import java.io.File;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaParticipantsChatLollipopAdapter;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ChatParticipantAvatarListener implements MegaRequestListenerInterface {

    Context context;
    MegaParticipantsChatLollipopAdapter.ViewHolderParticipantsList holder;
    MegaParticipantsChatLollipopAdapter adapter;
    ImageView imageView;
    private String userEmail;

    public ChatParticipantAvatarListener(Context context, MegaParticipantsChatLollipopAdapter.ViewHolderParticipantsList holder, MegaParticipantsChatLollipopAdapter adapter) {
        this.context = context;
        this.holder = holder;
        this.adapter = adapter;
    }
    public ChatParticipantAvatarListener(Context context, final ImageView imageView, String userEmail) {
        this.context = context;
        this.imageView = imageView;
        this.userEmail = userEmail;
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestStart()");
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        logDebug("onRequestFinish()");
        if (e.getErrorCode() == MegaError.API_OK){

            if(context instanceof GroupChatInfoActivityLollipop){
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
                        logWarning("Handle do not match");
                    }
                }

            }else{
                if (userEmail.compareTo(request.getEmail()) == 0) {
                    File avatar = buildAvatarFile(context, userEmail + ".jpg");
                    Bitmap bitmap;
                    if (isFileAvailable(avatar) && avatar.length() > 0) {
                        BitmapFactory.Options bOpts = new BitmapFactory.Options();
                        bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                        if (bitmap == null) {
                            avatar.delete();
                            return;
                        }
                        imageView.setImageBitmap(bitmap);
                    }
                }
            }

        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        logWarning("onRequestTemporaryError");
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        // TODO Auto-generated method stub
    }
}
