package mega.privacy.android.app.main.listeners;

import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import mega.privacy.android.app.main.megachat.chatAdapters.MegaChipChatExplorerAdapter;
import mega.privacy.android.app.main.megachat.chatAdapters.MegaListChatAdapter;
import mega.privacy.android.app.main.megachat.chatAdapters.MegaListChatExplorerAdapter;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import timber.log.Timber;

public class ChatUserAvatarListener implements MegaRequestListenerInterface {

    Context context;
    RecyclerView.ViewHolder holder;

    public ChatUserAvatarListener(Context context, RecyclerView.ViewHolder holder) {
        this.context = context;
        this.holder = holder;
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        Timber.d("onRequestStart()");
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        Timber.d("Error code: %s", e.getErrorCode());

        if (e.getErrorCode() == MegaError.API_OK) {
            Bitmap bitmap;
            String email;
            if (holder instanceof MegaListChatAdapter.ViewHolderChatList) {
                MegaListChatAdapter.ViewHolderNormalChatList viewHolder = (MegaListChatAdapter.ViewHolderNormalChatList) holder;
                if (viewHolder != null && viewHolder.getContactMail() != null && request.getEmail() != null) {
                    email = viewHolder.getContactMail();
                    if (email.compareTo(request.getEmail()) == 0) {
                        bitmap = getBitmap(email);
                        if (bitmap != null) {
                            viewHolder.setImageView(bitmap);
                        }
                    }
                } else {
                    Timber.w("Adapter cannot be updated - null");
                }
            } else if (holder instanceof MegaListChatExplorerAdapter.ViewHolderChatExplorerList) {
                MegaListChatExplorerAdapter.ViewHolderChatExplorerList viewHolder = (MegaListChatExplorerAdapter.ViewHolderChatExplorerList) holder;
                if (viewHolder != null && viewHolder.getEmail() != null && request.getEmail() != null) {
                    email = viewHolder.getEmail();
                    if (email.compareTo(request.getEmail()) == 0) {
                        bitmap = getBitmap(email);
                        if (bitmap != null) {
                            viewHolder.setAvatarImage(bitmap);
                        }
                    }
                } else {
                    Timber.w("Adapter cannot be updated - null");
                }
            } else if (holder instanceof MegaChipChatExplorerAdapter.ViewHolderChips) {
                MegaChipChatExplorerAdapter.ViewHolderChips viewHolder = (MegaChipChatExplorerAdapter.ViewHolderChips) holder;
                if (viewHolder != null && viewHolder.getEmail() != null && request.getEmail() != null) {
                    email = viewHolder.getEmail();
                    if (email.compareTo(request.getEmail()) == 0) {
                        bitmap = getBitmap(email);
                        if (bitmap != null) {
                            viewHolder.setAvatar(bitmap);
                        }
                    }
                } else {
                    Timber.w("Adapter cannot be updated - null");
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        Timber.w("onRequestTemporaryError");
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    private Bitmap getBitmap(String email) {
        File avatar = buildAvatarFile(email + ".jpg");
        Bitmap bitmap;
        if (isFileAvailable(avatar)) {
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                if (bitmap == null) {
                    avatar.delete();
                } else {
                    return bitmap;
                }
            }
        }

        return null;
    }
}