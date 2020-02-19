package mega.privacy.android.app.lollipop.megachat.calls;
/*
 *  Copyright (c) 2012 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.view.TextureView;

import org.webrtc.Logging;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.VideoCaptureUtils.*;

// The following four imports are needed saveBitmapToJPEG which
// is for debug only

public class MegaSurfaceRendererGroup implements TextureView.SurfaceTextureListener {

    private final static String TAG = "WEBRTC";
    protected List<MegaSurfaceRendererGroupListener> listeners;
    private Paint paint;
    private PorterDuffXfermode modesrcover;
    private PorterDuffXfermode modesrcin;
    private int surfaceWidth = 0;
    private int surfaceHeight = 0;
    private long peerId;
    private long clientId;
    // the bitmap used for drawing.
    private Bitmap bitmap = null;
    private ByteBuffer byteBuffer = null;
    private SurfaceTexture surfaceHolder;
    // Rect of the source bitmap to draw
    private Rect srcRect = new Rect();
    // Rect of the destination canvas to draw to
    private Rect dstRect = new Rect();
    private RectF dstRectf = new RectF();
    private TextureView myTexture = null;


    public MegaSurfaceRendererGroup(TextureView view, long peerId, long clientId) {
        logDebug("MegaSurfaceRendererGroup()");

        this.myTexture = view;
        myTexture.setSurfaceTextureListener(this);
        bitmap = myTexture.getBitmap();
        paint = new Paint();
        modesrcover = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
        modesrcin = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
        this.peerId = peerId;
        this.clientId = clientId;
        listeners = new ArrayList<>();
    }

    // surfaceChanged and surfaceCreated share this function
    private void changeDestRect(int dstWidth, int dstHeight) {
        logDebug("dstWidth = " + dstWidth + ", dstHeight = " + dstHeight);
        surfaceWidth = dstWidth;
        surfaceHeight = dstHeight;
        dstRect.top = 0;
        dstRect.left = 0;
        dstRect.right = dstWidth;
        dstRect.bottom = dstHeight;
        dstRectf = new RectF(dstRect);
        adjustAspectRatio();
    }

    private void adjustAspectRatio() {
        logDebug("adjustAspectRatio()");
        if (bitmap != null && dstRect.height() != 0) {
            dstRect.top = 0;
            dstRect.left = 0;
            dstRect.right = surfaceWidth;
            dstRect.bottom = surfaceHeight;
        }
    }

    public Bitmap createBitmap(int width, int height) {
        logDebug("width = " + width + ", height = " + height);
        Logging.d(TAG, "CreateByteBitmap " + width + ":" + height);
        if (bitmap == null) {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
            } catch (Exception e) {
            }
        }

        if (height == width) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            srcRect.top = 0;
            srcRect.bottom = height;
            srcRect.left = 0;
            srcRect.right = width;
            logDebug("width == height. sRect(T " + srcRect.top + " -B " + srcRect.bottom + ")(L " + srcRect.left + " - R " + srcRect.right + ")");

        } else if (height > width) {
            bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
            srcRect.top = 0;
            srcRect.bottom = width;
            srcRect.left = 0;
            srcRect.right = width;
            logDebug("height > width. sRect(T " + srcRect.top + " -B " + srcRect.bottom + ")(L " + srcRect.left + " - R " + srcRect.right + ")");

        } else {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            srcRect.left = 0;
            srcRect.right = height;
            srcRect.top = 0;
            srcRect.bottom = height;
            logDebug("height < width. sRect(T " + srcRect.top + " -B " + srcRect.bottom + ")(L " + srcRect.left + " - R " + srcRect.right + ")");
        }
        adjustAspectRatio();
        return bitmap;
    }

    public void drawBitmap(boolean isLocal) {
        if (bitmap == null || myTexture == null) return;

        Canvas canvas = myTexture.lockCanvas();
        if (canvas == null) return;
        if (isLocal && isFrontCameraInUse()) {
            canvas.scale(-1, 1);
            canvas.translate(-canvas.getWidth(), 0);
        }
        canvas.drawBitmap(bitmap, srcRect, dstRect, null);
        myTexture.unlockCanvasAndPost(canvas);

    }

    private void notifyStateToAll() {
        for (MegaSurfaceRendererGroupListener listener : listeners)
            notifyState(listener);
    }

    public void addListener(MegaSurfaceRendererGroupListener l) {
        listeners.add(l);
        notifyState(l);
    }

    private void notifyState(MegaSurfaceRendererGroupListener listener) {
        if (listener == null)
            return;
        listener.resetSize(peerId, clientId);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int in_width, int in_height) {
        logDebug("onSurfaceTextureAvailable()");
        Bitmap textureViewBitmap = myTexture.getBitmap();
        Canvas canvas = new Canvas(textureViewBitmap);
        if (canvas == null) return;
        notifyStateToAll();
        changeDestRect(in_width, in_height);

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int in_width, int in_height) {
        logDebug("in_width = " + in_width + ", in_height = " + in_height);
        changeDestRect(in_width, in_height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        logDebug("surfaceWidth = 0 && surfaceHeight = 0");
        bitmap = null;
        byteBuffer = null;
        surfaceWidth = 0;
        surfaceHeight = 0;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    public interface MegaSurfaceRendererGroupListener {
        void resetSize(long peerId, long clientId);
    }

}
