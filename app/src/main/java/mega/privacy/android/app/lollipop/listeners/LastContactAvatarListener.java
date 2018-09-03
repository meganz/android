package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;

import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

public class LastContactAvatarListener implements MegaRequestListenerInterface {
    
    private static final int PIXELS = 3;
    private static final String SUFFIX = ".jpg";
    
    private Context context;
    
    private String contactEmail;
    
    private ImageView avatarImage;
    
    public LastContactAvatarListener(Context context,String contactEmail,ImageView avatarImage) {
        this.context = context;
        this.contactEmail = contactEmail;
        this.avatarImage = avatarImage;
    }
    
    @Override
    public void onRequestFinish(MegaApiJava api,MegaRequest request,MegaError e) {
        Log.e("@#@",getClass().getSimpleName());
        if (e.getErrorCode() == MegaError.API_OK) {
            if (contactEmail.compareTo(request.getEmail()) == 0) {
                File avatar;
                if (context.getExternalCacheDir() != null) {
                    avatar = new File(context.getExternalCacheDir().getAbsolutePath(),contactEmail + SUFFIX);
                } else {
                    avatar = new File(context.getCacheDir().getAbsolutePath(),contactEmail + SUFFIX);
                }
                Bitmap bitmap;
                if (avatar.exists()) {
                    if (avatar.length() > 0) {
                        BitmapFactory.Options bOpts = new BitmapFactory.Options();
                        bOpts.inPurgeable = true;
                        bOpts.inInputShareable = true;
                        bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(),bOpts);
                        if (bitmap == null) {
                            avatar.delete();
                        } else {
                            bitmap = ThumbnailUtilsLollipop.getRoundedRectBitmap(context,bitmap,PIXELS);
                            avatarImage.setImageBitmap(bitmap);
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void onRequestStart(MegaApiJava api,MegaRequest request) {
    
    }
    
    @Override
    public void onRequestUpdate(MegaApiJava api,MegaRequest request) {
    
    }
    
    @Override
    public void onRequestTemporaryError(MegaApiJava api,MegaRequest request,MegaError e) {
    
    }
}
