package mega.privacy.android.app.lollipop.megachat.calls;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.nio.ByteBuffer;

import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatVideoListenerInterface;

import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.VideoCaptureUtils.*;

public class IndividualCallListener implements MegaChatVideoListenerInterface {

    private Context context;
    private int width;
    private int height;
    private SurfaceView surfaceView;
    private boolean isLocal;
    private MegaSurfaceRenderer renderer;
    private SurfaceHolder surfaceHolder;
    private Bitmap bitmap;

    public IndividualCallListener(Context context, SurfaceView video, long peerid, long clientid, long chatId, DisplayMetrics outMetrics, boolean isSmallCamera) {
        this.context = context;
        this.width = 0;
        this.height = 0;
        this.surfaceView = video;
        this.isLocal = isItMe(chatId, peerid, clientid);
        if (!isSmallCamera && !isLocal) {
            this.surfaceView.setZOrderOnTop(false);
            this.surfaceView.setZOrderMediaOverlay(false);
        } else if (!isSmallCamera && isLocal) {
            this.surfaceView.setZOrderMediaOverlay(true);
        } else if (isSmallCamera && isLocal) {
            this.surfaceView.setZOrderMediaOverlay(true);
        }
        surfaceHolder = this.surfaceView.getHolder();
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        this.renderer = new MegaSurfaceRenderer(this.surfaceView, isSmallCamera, outMetrics);
    }

    @Override
    public void onChatVideoData(MegaChatApiJava api, long chatid, int width, int height, byte[] byteBuffer) {

        if ((width == 0) || (height == 0)) {
            return;
        }

        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            SurfaceHolder holder = surfaceView.getHolder();
            if (holder != null) {
                int viewWidth = surfaceView.getWidth();
                int viewHeight = surfaceView.getHeight();
                if (viewWidth != 0 && viewHeight != 0) {
                    int holderWidth = viewWidth < width ? viewWidth : width;
                    int holderHeight = holderWidth * viewHeight / viewWidth;
                    if (holderHeight > viewHeight) {
                        holderHeight = viewHeight;
                        holderWidth = holderHeight * viewWidth / viewHeight;
                    }
                    this.bitmap = renderer.createBitmap(width, height);
                    holder.setFixedSize(holderWidth, holderHeight);
                } else {
                    this.width = -1;
                    this.height = -1;
                }
            }
        }

        if (bitmap == null)
            return;

        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer));
        if (isVideoAllowed()) {
            renderer.drawBitmap(isLocal);
        }
    }

    public MegaSurfaceRenderer getRenderer() {
        return renderer;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }


}

