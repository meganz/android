package mega.privacy.android.app.main.listeners;


import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

import mega.privacy.android.app.main.adapters.ShareContactsHeaderAdapter;
import mega.privacy.android.app.utils.ThumbnailUtils;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class UserAvatarListenerShare implements MegaRequestListenerInterface {

    Context context;
    ShareContactsHeaderAdapter.ViewHolderShareContacts holder;

    public UserAvatarListenerShare(Context context, ShareContactsHeaderAdapter.ViewHolderShareContacts holder) {
        this.context = context;
        this.holder = holder;
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

        if (e.getErrorCode() == MegaError.API_OK){

            if (holder.mail.compareTo(request.getEmail()) == 0){
                File avatar = buildAvatarFile(holder.mail + ".jpg");
                Bitmap bitmap = null;
                if (isFileAvailable(avatar)) {
                    if (avatar.length() > 0){
                        BitmapFactory.Options bOpts = new BitmapFactory.Options();
                        bOpts.inPurgeable = true;
                        bOpts.inInputShareable = true;
                        bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                        if (bitmap == null) {
                            avatar.delete();
                        }
                        else{
                            bitmap = ThumbnailUtils.getRoundedRectBitmap(context, bitmap, 3);
                            holder.avatar.setImageBitmap(bitmap);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
    }

}

