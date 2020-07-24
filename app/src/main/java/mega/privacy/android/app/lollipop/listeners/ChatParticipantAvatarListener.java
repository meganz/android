package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;
import java.io.File;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaParticipantsChatLollipopAdapter;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.AvatarUtil.getImageAvatar;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.getCircleBitmap;

public class ChatParticipantAvatarListener implements MegaRequestListenerInterface {

    Context context;
    MegaParticipantsChatLollipopAdapter.ViewHolderParticipantsList holder;
    MegaParticipantsChatLollipopAdapter adapter;
    private ImageView imageView;
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
        if (e.getErrorCode() == MegaError.API_OK) {
            if (context instanceof GroupChatInfoActivityLollipop) {
                if (holder.contactMail != null) {
                    if (holder.contactMail.compareTo(request.getEmail()) == 0) {
                        Bitmap bitmap = getImageAvatar(holder.contactMail);
                        if (bitmap != null) {
                            holder.setImageView(bitmap);
                        }
                    }
                } else if (holder.userHandle.compareTo(request.getEmail()) == 0) {
                    Bitmap bitmap = getImageAvatar(holder.userHandle);
                    if (bitmap != null) {
                        holder.setImageView(bitmap);
                    }
                } else {
                    logWarning("Handle do not match");
                }
            } else if (userEmail.compareTo(request.getEmail()) == 0) {
                Bitmap bitmap = getImageAvatar(userEmail);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
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
