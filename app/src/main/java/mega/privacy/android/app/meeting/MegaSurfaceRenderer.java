/*
 *  Copyright (c) 2012 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package mega.privacy.android.app.meeting;

// The following four imports are needed saveBitmapToJPEG which
// is for debug only

import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.VideoCaptureUtils.isFrontCameraInUse;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MegaSurfaceRenderer implements Callback, TextureView.SurfaceTextureListener {

    private Paint paint;
    private PorterDuffXfermode modesrcover;
    private PorterDuffXfermode modesrcin;
    private int surfaceWidth = 0;
    private int surfaceHeight = 0;
    private static final int CORNER_RADIUS = 20;
    private static final int VISIBLE = 255;

    // the bitmap used for drawing.
    private Bitmap bitmap = null;
    private SurfaceHolder surfaceHolder;
    // Rect of the source bitmap to draw
    private final Rect srcRect = new Rect();
    // Rect of the destination canvas to draw to
    private final Rect dstRect = new Rect();
    private RectF dstRectf = new RectF();
    private boolean isSmallCamera;
    private DisplayMetrics outMetrics;
    private long peerId = MEGACHAT_INVALID_HANDLE;
    private long clientId = MEGACHAT_INVALID_HANDLE;
    private TextureView myTexture = null;
    protected List<MegaSurfaceRendererListener> listeners;

    private int alpha = VISIBLE;

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public MegaSurfaceRenderer(SurfaceView view, boolean isSmallCamera, DisplayMetrics outMetrics) {
        surfaceHolder = view.getHolder();
        if (surfaceHolder == null)
            return;

        surfaceHolder.addCallback(this);
        paint = new Paint();
        modesrcover = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
        modesrcin = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
        this.isSmallCamera = isSmallCamera;
        this.outMetrics = outMetrics;
    }

    public MegaSurfaceRenderer(TextureView view, long peerId, long clientId) {
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
        if (bitmap != null && dstRect.height() != 0) {
            dstRect.top = 0;
            dstRect.left = 0;
            dstRect.right = surfaceWidth;
            dstRect.bottom = surfaceHeight;

            dstRectf = new RectF(dstRect);
            float srcaspectratio = (float) bitmap.getWidth() / bitmap.getHeight();
            float dstaspectratio = (float) dstRect.width() / dstRect.height();
            if (srcaspectratio != 0 && dstaspectratio != 0) {

                if (isSmallCamera) {
                    if (srcaspectratio > dstaspectratio) {
                        float newHeight = dstRect.width() / srcaspectratio;
                        float decrease = dstRect.height() - newHeight;
                        dstRect.top += decrease / 2;
                        dstRect.bottom -= decrease / 2;
                        dstRectf = new RectF(dstRect);
                    } else {
                        float newWidth = dstRect.height() * srcaspectratio;
                        float decrease = dstRect.width() - newWidth;
                        dstRect.left += decrease / 2;
                        dstRect.right -= decrease / 2;
                        dstRectf = new RectF(dstRect);
                    }
                } else {
                    if (srcaspectratio > dstaspectratio) {
                        float newWidth = dstRect.height() * srcaspectratio;
                        float decrease = dstRect.width() - newWidth;
                        dstRect.left += decrease / 2;
                        dstRect.right -= decrease / 2;
                        dstRectf = new RectF(dstRect);
                    } else {
                        float newHeight = dstRect.width() / srcaspectratio;
                        float decrease = dstRect.height() - newHeight;
                        dstRect.top += decrease / 2;
                        dstRect.bottom -= decrease / 2;
                        dstRectf = new RectF(dstRect);
                    }
                }
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null) return;
        Rect dst = surfaceHolder.getSurfaceFrame();
        if (dst != null) {
            changeDestRect(dst.right - dst.left, dst.bottom - dst.top);
        }

        Timber.d("Surface created");
        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int in_width, int in_height) {
        changeDestRect(in_width, in_height);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Timber.d("Surface destroyed");
        bitmap = null;
        surfaceWidth = 0;
        surfaceHeight = 0;
    }

    public Bitmap createBitmap(int width, int height) {
        if (bitmap == null) {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
            } catch (Exception e) {
                Timber.e(e);
            }
        }

        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        srcRect.left = 0;
        srcRect.top = 0;
        srcRect.bottom = height;
        srcRect.right = width;
        adjustAspectRatio();
        return bitmap;
    }

    /**
     * Draw video frames.
     *
     * @param isLocal Indicates if the frames are from the local camera.
     */
    public void drawBitmap(boolean isLocal, boolean isGroup) {
        if (bitmap == null || (isGroup && myTexture == null) || (!isGroup && surfaceHolder == null))
            return;

        Canvas canvas = isGroup ? myTexture.lockCanvas() : surfaceHolder.lockCanvas();

        if (canvas == null) return;
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);


        if (isLocal && isFrontCameraInUse()) {
            canvas.scale(-1, 1);
            canvas.translate(-canvas.getWidth(), 0);
        }
        if (isSmallCamera) {
            paint.reset();
            paint.setAlpha(alpha);
            paint.setXfermode(modesrcover);
            canvas.drawRoundRect(dstRectf, dp2px(CORNER_RADIUS, outMetrics), dp2px(CORNER_RADIUS, outMetrics), paint);
            paint.setXfermode(modesrcin);
        } else {
            paint = null;
        }
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint);

        if (isGroup) {
            myTexture.unlockCanvasAndPost(canvas);
        } else {
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    private void notifyStateToAll() {
        for (MegaSurfaceRendererListener listener : listeners) {
            notifyState(listener);
        }
    }

    public void addListener(MegaSurfaceRendererListener l) {
        listeners.add(l);
    }

    private void notifyState(MegaSurfaceRendererListener listener) {
        if (listener == null)
            return;

        listener.resetSize(peerId, clientId);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int in_width, int in_height) {
        Bitmap textureViewBitmap = myTexture.getBitmap();
        if (textureViewBitmap == null) return;

        Timber.d("TextureView Available");
        notifyStateToAll();
        changeDestRect(in_width, in_height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int in_width, int in_height) {
        changeDestRect(in_width, in_height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Timber.d("TextureView destroyed");
        bitmap = null;
        surfaceWidth = 0;
        surfaceHeight = 0;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    public interface MegaSurfaceRendererListener {
        void resetSize(long peerId, long clientId);
    }

    /**
     * Get the width of the surface view
     *
     * @return the width
     */
    public int getSurfaceWidth() {
        return surfaceWidth;
    }

    /**
     * Get the height of the surface view
     *
     * @return the height
     */
    public int getSurfaceHeight() {
        return surfaceHeight;
    }
}
