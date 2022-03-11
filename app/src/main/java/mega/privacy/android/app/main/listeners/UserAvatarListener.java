package mega.privacy.android.app.main.listeners;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

import mega.privacy.android.app.main.adapters.ContactsHorizontalAdapter;
import mega.privacy.android.app.main.adapters.LastContactsAdapter;
import mega.privacy.android.app.main.adapters.MegaContactsAdapter;
import mega.privacy.android.app.utils.ThumbnailUtils;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class UserAvatarListener implements MegaRequestListenerInterface {
    
    Context context;
    MegaContactsAdapter.ViewHolderContacts holder;
    
    public UserAvatarListener(Context context, MegaContactsAdapter.ViewHolderContacts holder) {
        this.context = context;
        this.holder = holder;
    }
    
    public UserAvatarListener(Context context) {
        this.context = context;
    }
    
    @Override
    public void onRequestStart(MegaApiJava api,MegaRequest request) {
    }
    
    @Override
    public void onRequestFinish(MegaApiJava api,MegaRequest request,MegaError e) {
        if (holder == null || holder.contactMail == null) {
            logWarning("Holder or mail is NULL");
            return;
        }

        if (e.getErrorCode() == MegaError.API_OK) {
            if (holder.contactMail.compareTo(request.getEmail()) == 0) {
                File avatar = buildAvatarFile(context, holder.contactMail + ".jpg");
                Bitmap bitmap = null;
                if (isFileAvailable(avatar)) {
                    if (avatar.length() > 0) {
                        BitmapFactory.Options bOpts = new BitmapFactory.Options();
                        bOpts.inPurgeable = true;
                        bOpts.inInputShareable = true;
                        bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(),bOpts);
                        if (bitmap == null) {
                            avatar.delete();
                        } else {
                            if (holder instanceof MegaContactsAdapter.ViewHolderContactsGrid) {
                                bitmap = ThumbnailUtils.getRoundedRectBitmap(context,bitmap,3);
                                ((MegaContactsAdapter.ViewHolderContactsGrid)holder).imageView.setImageBitmap(bitmap);
                            } else if (holder instanceof MegaContactsAdapter.ViewHolderContactsList) {
                                ((MegaContactsAdapter.ViewHolderContactsList)holder).imageView.setImageBitmap(bitmap);
                            } else if (holder instanceof LastContactsAdapter.ViewHolder) {
                                ((LastContactsAdapter.ViewHolder)holder).avatarImage.setImageBitmap(bitmap);
                            } else if (holder instanceof ContactsHorizontalAdapter.ContactViewHolder) {
                                ((ContactsHorizontalAdapter.ContactViewHolder) holder).avatar.setImageBitmap(bitmap);
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void onRequestTemporaryError(MegaApiJava api,MegaRequest request,MegaError e) {
    }
    
    @Override
    public void onRequestUpdate(MegaApiJava api,MegaRequest request) {
    }
    
}

