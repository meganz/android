package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChipChatExplorerAdapter;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaListChatExplorerAdapter;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaListChatLollipopAdapter;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ChatUserAvatarListener implements MegaRequestListenerInterface {

    Context context;
    RecyclerView.ViewHolder holder;

    public ChatUserAvatarListener(Context context, RecyclerView.ViewHolder holder) {
        this.context = context;
        this.holder = holder;
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestStart()");
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        logDebug("Error code: " + e.getErrorCode());

        if (e.getErrorCode() == MegaError.API_OK){
            Bitmap bitmap;
            String email;
            if(holder instanceof MegaListChatLollipopAdapter.ViewHolderChatList){
                MegaListChatLollipopAdapter.ViewHolderNormalChatList viewHolder = (MegaListChatLollipopAdapter.ViewHolderNormalChatList) holder;
                if(viewHolder !=null && viewHolder.getContactMail()!=null && request.getEmail()!=null){
                    email = viewHolder.getContactMail();
                    if (email.compareTo(request.getEmail()) == 0){
                        bitmap = getBitmap(email);
                        if (bitmap != null) {
                            viewHolder.setImageView(bitmap);
                        }
                    }
                }
                else{
                    logWarning("Adapter cannot be updated - null");
                }
            }
            else if (holder instanceof MegaListChatExplorerAdapter.ViewHolderChatExplorerList) {
                MegaListChatExplorerAdapter.ViewHolderChatExplorerList viewHolder = (MegaListChatExplorerAdapter.ViewHolderChatExplorerList) holder;
                if(viewHolder !=null && viewHolder.getEmail()!=null && request.getEmail()!=null){
                    email = viewHolder.getEmail();
                    if (email.compareTo(request.getEmail()) == 0){
                        bitmap = getBitmap(email);
                        if (bitmap != null) {
                            viewHolder.setAvatarImage(bitmap);
                        }
                    }
                }
                else{
                    logWarning("Adapter cannot be updated - null");
                }
            }
            else if (holder instanceof MegaChipChatExplorerAdapter.ViewHolderChips) {
                MegaChipChatExplorerAdapter.ViewHolderChips viewHolder = (MegaChipChatExplorerAdapter.ViewHolderChips) holder;
                if(viewHolder !=null && viewHolder.getEmail()!=null && request.getEmail()!=null){
                    email = viewHolder.getEmail();
                    if (email.compareTo(request.getEmail()) == 0){
                        bitmap = getBitmap(email);
                        if (bitmap != null) {
                            viewHolder.setAvatar(bitmap);
                        }
                    }
                }
                else{
                    logWarning("Adapter cannot be updated - null");
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api,MegaRequest request, MegaError e) {
        logWarning("onRequestTemporaryError");
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        // TODO Auto-generated method stub
    }

    private Bitmap getBitmap(String email) {
        File avatar = buildAvatarFile(context, email + ".jpg");
        Bitmap bitmap;
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
                    return bitmap;
                }
            }
        }

        return null;
    }
}