package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;
import java.nio.ByteBuffer;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.GroupCallAdapter;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatVideoListenerInterface;


public class GroupCallListener implements MegaChatVideoListenerInterface {

    Context context;
    GroupCallAdapter.ViewHolderGroupCall holder;
    int width,height;
    Bitmap bitmap;

    public GroupCallListener(Context context, GroupCallAdapter.ViewHolderGroupCall holder) {
        this.context = context;
        this.holder = holder;

    }

    public GroupCallListener(Context context) {
        this.context = context;
    }


    @Override
    public void onChatVideoData(MegaChatApiJava api, long chatid, int width, int height, byte[] byteBuffer) {
        Log.d("*******","**** onChatVideoData()");
        if((width == 0) || (height == 0)){
            return;
        }

        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;

            SurfaceHolder Sholder = holder.localFullScreenSurfaceView.getHolder();
            if (Sholder != null) {
                int viewWidth = holder.localFullScreenSurfaceView.getWidth();
                int viewHeight = holder.localFullScreenSurfaceView.getHeight();
                if ((viewWidth != 0) && (viewHeight != 0)) {
                    int holderWidth = viewWidth < width ? viewWidth : width;
                    int holderHeight = holderWidth * viewHeight / viewWidth;
                    if (holderHeight > viewHeight) {
                        holderHeight = viewHeight;
                        holderWidth = holderHeight * viewWidth / viewHeight;
                    }
                    this.bitmap = holder.localRenderer.CreateBitmap(width, height);
                    Sholder.setFixedSize(holderWidth, holderHeight);
                }
                else{
                    this.width = -1;
                    this.height = -1;
                }
            }
        }

        if (bitmap != null) {
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer));

            // Instead of using this WebRTC renderer, we should probably draw the image by ourselves.
            // The renderer has been modified a bit and an update of WebRTC could break our app
            holder.localRenderer.DrawBitmap(false);
        }
    }
}

