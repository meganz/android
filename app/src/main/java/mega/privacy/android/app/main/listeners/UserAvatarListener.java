package mega.privacy.android.app.main.listeners;

import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import java.io.File;

import mega.privacy.android.app.main.adapters.LastContactsAdapter;
import mega.privacy.android.app.main.adapters.MegaContactsAdapter;
import mega.privacy.android.app.utils.ThumbnailUtils;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import timber.log.Timber;

public class UserAvatarListener implements MegaRequestListenerInterface {

    Context context;
    MegaContactsAdapter.ViewHolderContacts holder;

    public UserAvatarListener(Context context, MegaContactsAdapter.ViewHolderContacts holder) {
        this.context = context;
        this.holder = holder;
    }

    @Override
    public void onRequestStart(@NonNull MegaApiJava api, @NonNull MegaRequest request) {
    }

    @Override
    public void onRequestFinish(@NonNull MegaApiJava api, @NonNull MegaRequest request, @NonNull MegaError e) {
        if (holder == null || holder.contactMail == null) {
            Timber.w("Holder or mail is NULL");
            return;
        }

        if (e.getErrorCode() == MegaError.API_OK) {
            if (holder.contactMail.compareTo(request.getEmail()) == 0) {
                File avatar = buildAvatarFile(holder.contactMail + ".jpg");
                Bitmap bitmap = null;
                if (isFileAvailable(avatar)) {
                    if (avatar.length() > 0) {
                        BitmapFactory.Options bOpts = new BitmapFactory.Options();
                        bOpts.inPurgeable = true;
                        bOpts.inInputShareable = true;
                        bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                        if (bitmap == null) {
                            avatar.delete();
                        } else {
                            if (holder instanceof MegaContactsAdapter.ViewHolderContactsGrid) {
                                bitmap = ThumbnailUtils.getRoundedRectBitmap(context, bitmap, 3);
                                ((MegaContactsAdapter.ViewHolderContactsGrid) holder).imageView.setImageBitmap(bitmap);
                            } else if (holder instanceof MegaContactsAdapter.ViewHolderContactsList) {
                                ((MegaContactsAdapter.ViewHolderContactsList) holder).imageView.setImageBitmap(bitmap);
                            } else if (holder instanceof LastContactsAdapter.ViewHolder) {
                                ((LastContactsAdapter.ViewHolder) holder).avatarImage.setImageBitmap(bitmap);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(@NonNull MegaApiJava api, @NonNull MegaRequest request, @NonNull MegaError e) {
    }

    @Override
    public void onRequestUpdate(@NonNull MegaApiJava api, @NonNull MegaRequest request) {
    }

}

