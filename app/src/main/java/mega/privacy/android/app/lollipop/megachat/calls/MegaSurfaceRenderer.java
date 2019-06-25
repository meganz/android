/*
 *  Copyright (c) 2012 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package mega.privacy.android.app.lollipop.megachat.calls;

// The following four imports are needed saveBitmapToJPEG which
// is for debug only

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import org.webrtc.Logging;

import java.nio.ByteBuffer;

import mega.privacy.android.app.utils.Util;

public class MegaSurfaceRenderer implements Callback {

    private final static String TAG = "WEBRTC";
    private Paint paint;
    private PorterDuffXfermode modesrcover;
    private PorterDuffXfermode modesrcin;
    private int surfaceWidth = 0;
    private int surfaceHeight = 0;
    // the bitmap used for drawing.
    private Bitmap bitmap = null;
    private ByteBuffer byteBuffer = null;
    private SurfaceHolder surfaceHolder;
    // Rect of the source bitmap to draw
    private Rect srcRect = new Rect();
    // Rect of the destination canvas to draw to
    private Rect dstRect = new Rect();
    private RectF dstRectf = new RectF();


    public MegaSurfaceRenderer(SurfaceView view) {
        log("MegaSurfaceRenderer() ");
        surfaceHolder = view.getHolder();
        if (surfaceHolder == null)
            return;
        surfaceHolder.addCallback(this);
        paint = new Paint();
        modesrcover = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
        modesrcin = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
    }

    private static void log(String log) {
        Util.log("MegaSurfaceRendererGroup", log);
    }

    // surfaceChanged and surfaceCreated share this function
    private void changeDestRect(int dstWidth, int dstHeight) {
        log("changeDestRect(): dstWidth = " + dstWidth + ", dstHeight = " + dstHeight);
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
        log("adjustAspectRatio(): ");

        if (bitmap != null && dstRect.height() != 0) {
            dstRect.top = 0;
            dstRect.left = 0;
            dstRect.right = surfaceWidth;
            dstRect.bottom = surfaceHeight;

            dstRectf = new RectF(dstRect);
            float srcaspectratio = (float) bitmap.getWidth() / bitmap.getHeight();
            float dstaspectratio = (float) dstRect.width() / dstRect.height();

            if (srcaspectratio != 0 && dstaspectratio != 0) {
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
            }
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        log("surfaceCreated(): ");

        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null) return;
        Rect dst = surfaceHolder.getSurfaceFrame();
        if (dst != null) {
            changeDestRect(dst.right - dst.left, dst.bottom - dst.top);
            Logging.d(TAG, "ViESurfaceRender::surfaceCreated" +
                    " dst.left:" + dst.left +
                    " dst.top:" + dst.top +
                    " dst.right:" + dst.right +
                    " dst.bottom:" + dst.bottom +
                    " srcRect.left:" + srcRect.left +
                    " srcRect.top:" + srcRect.top +
                    " srcRect.right:" + srcRect.right +
                    " srcRect.bottom:" + srcRect.bottom +
                    " dstRect.left:" + dstRect.left +
                    " dstRect.top:" + dstRect.top +
                    " dstRect.right:" + dstRect.right +
                    " dstRect.bottom:" + dstRect.bottom);
        }
        surfaceHolder.unlockCanvasAndPost(canvas);

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int in_width, int in_height) {
        log("surfaceChanged(): in_width = " + in_width + ", in_height = " + in_height);

        Logging.d(TAG, "ViESurfaceRender::surfaceChanged");
        changeDestRect(in_width, in_height);

        Logging.d(TAG, "ViESurfaceRender::surfaceChanged" +
                " in_width:" + in_width + " in_height:" + in_height +
                " srcRect.left:" + srcRect.left +
                " srcRect.top:" + srcRect.top +
                " srcRect.right:" + srcRect.right +
                " srcRect.bottom:" + srcRect.bottom +
                " dstRect.left:" + dstRect.left +
                " dstRect.top:" + dstRect.top +
                " dstRect.right:" + dstRect.right +
                " dstRect.bottom:" + dstRect.bottom);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        log("surfaceDestroyed():");

        Logging.d(TAG, "ViESurfaceRenderer::surfaceDestroyed");
        bitmap = null;
        byteBuffer = null;
    }

    public Bitmap CreateBitmap(int width, int height) {
        log("CreateBitmap(): width = " + width + ", height = " + height);

        Logging.d(TAG, "CreateByteBitmap " + width + ":" + height);
        if (bitmap == null) {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
            } catch (Exception e) {
            }
        }
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        srcRect.left = 0;
        srcRect.top = 0;
        srcRect.bottom = height;
        srcRect.right = width;

        log("CreateBitmap(): sRect(T " + srcRect.top + " -B " + srcRect.bottom + ")(L " + srcRect.left + " - R " + srcRect.right + ")");
        adjustAspectRatio();

        return bitmap;
    }

    public void DrawBitmap(boolean flag, boolean isLocal) {
        if (bitmap == null || surfaceHolder == null)
            return;

        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null) return;
        if (isLocal) {
            canvas.scale(-1, 1);
            canvas.translate(-canvas.getWidth(), 0);
        }
        if (flag) {
            paint.reset();
            paint.setXfermode(modesrcover);
            canvas.drawRoundRect(dstRectf, 20, 20, paint);
            paint.setXfermode(modesrcin);
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint);
        } else {
            canvas.drawBitmap(bitmap, srcRect, dstRect, null);
        }
        surfaceHolder.unlockCanvasAndPost(canvas);
    }
}
