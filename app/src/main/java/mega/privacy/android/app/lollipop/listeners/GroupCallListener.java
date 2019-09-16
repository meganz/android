package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.TextureView;

import java.nio.ByteBuffer;

import mega.privacy.android.app.lollipop.megachat.calls.MegaSurfaceRendererGroup;
import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatVideoListenerInterface;

public class GroupCallListener implements MegaChatVideoListenerInterface {

    Context context;
    int width;
    int height;
    Bitmap bitmap;
    TextureView myTexture = null;
    boolean isLocal;
    MegaSurfaceRendererGroup localRenderer = null;

    public GroupCallListener(Context context, TextureView myTexture, long peerid, long clientid, boolean isLocal) {
        LogUtil.logDebug("GroupCallListener");
        this.context = context;
        this.width = 0;
        this.height = 0;
        this.myTexture = myTexture;
        this.isLocal = isLocal;
        this.localRenderer = new MegaSurfaceRendererGroup(myTexture, peerid, clientid);
    }


    @Override
    public void onChatVideoData(MegaChatApiJava api, long chatid, int width, int height, byte[] byteBuffer) {

        if((width == 0) || (height == 0)){
            return;
        }

        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;

            int viewWidth = myTexture.getWidth();
            int viewHeight = myTexture.getHeight();

            if ((viewWidth != 0) && (viewHeight != 0)) {
                int holderWidth = viewWidth < width ? viewWidth : width;
                int holderHeight = holderWidth * viewHeight / viewWidth;
                if (holderHeight > viewHeight) {

                    holderHeight = viewHeight;
                    holderWidth = holderHeight * viewWidth / viewHeight;
                }
                this.bitmap = localRenderer.CreateBitmap(width, height);
            }else{
                this.width = -1;
                this.height = -1;
            }

        }

        if (bitmap != null) {
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer));
            // Instead of using this WebRTC renderer, we should probably draw the image by ourselves.
            // The renderer has been modified a bit and an update of WebRTC could break our app
            localRenderer.DrawBitmap(false, isLocal);
        }
    }

    public TextureView getSurfaceView() {
        return myTexture;
    }

    public MegaSurfaceRendererGroup getLocalRenderer() {
        return localRenderer;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}

