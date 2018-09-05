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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.webrtc.Logging;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

// The following four imports are needed saveBitmapToJPEG which
// is for debug only
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;

import org.webrtc.Logging;

import mega.privacy.android.app.R;

public class MegaSurfaceRendererGroup implements SurfaceHolder.Callback {

    private final static String TAG = "WEBRTC";

    // the bitmap used for drawing.
    private Bitmap bitmap = null;
    private ByteBuffer byteBuffer = null;
    private SurfaceHolder surfaceHolder;
    // Rect of the source bitmap to draw
    private Rect srcRect = new Rect();
    // Rect of the destination canvas to draw to
    private Rect dstRect = new Rect();
    private RectF dstRectf = new RectF();
    Paint paint;
    PorterDuffXfermode modesrcover;
    PorterDuffXfermode modesrcin;
    int surfaceWidth = 0;
    int surfaceHeight = 0;


    public MegaSurfaceRendererGroup(SurfaceView view) {
        Log.d("MegaSurfaceRenderer","MegaSurfaceRenderer() ");

//        this.surf = view;
        surfaceHolder = view.getHolder();
        if(surfaceHolder == null)
            return;
        surfaceHolder.addCallback(this);
        paint = new Paint();
        modesrcover = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
        modesrcin = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
    }

    // surfaceChanged and surfaceCreated share this function
    private void changeDestRect(int dstWidth, int dstHeight) {
        Log.d("SurfaceRendererGroup","changeDestRect(): dstWidth = "+dstWidth+", dstHeight = "+dstHeight);
        surfaceWidth = dstWidth;
        surfaceHeight = dstHeight;
        dstRect.top = 0;
        dstRect.left = 0;
        dstRect.right = dstWidth;
        dstRect.bottom = dstHeight;
        dstRectf = new RectF(dstRect);

//        adjustAspectRatio();
    }

//    private void adjustAspectRatio() {
//        if (bitmap != null && dstRect.height() != 0) {
//            float srcaspectratio = (float) bitmap.getWidth() / bitmap.getHeight();
//            dstRect.top = 0;
//            dstRect.left = 0;
//            dstRect.right = surfaceWidth;
//            dstRect.bottom = (int)(surfaceWidth/srcaspectratio);
//            dstRectf = new RectF(dstRect);
//        }
//    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("MegaSurfaceRenderer","surfaceCreated()");

        Canvas canvas = surfaceHolder.lockCanvas();
        if(canvas != null) {
            Rect dst = surfaceHolder.getSurfaceFrame();
            if(dst != null) {
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
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int in_width, int in_height) {
        Log.d("MegaSurfaceRenderer","surfaceChanged(): in_width = "+in_width+", in_height = "+in_height);

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
        Log.d("MegaSurfaceRenderer","surfaceDestroyed(): ");

        Logging.d(TAG, "ViESurfaceRenderer::surfaceDestroyed");
        bitmap = null;
        byteBuffer = null;
    }

    public Bitmap CreateBitmap(int width, int height) {
        Log.d("MegaSurfaceRenderer","CreateBitmap(): width = "+width+", height = "+height);

        Logging.d(TAG, "CreateByteBitmap " + width + ":" + height);
        if (bitmap == null) {
            try {
                android.os.Process.setThreadPriority(
                        android.os.Process.THREAD_PRIORITY_DISPLAY);
            }
            catch (Exception e) {
            }
        }

        if(height == width){
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            srcRect.top = 0;
            srcRect.bottom = height;
            srcRect.left = 0;
            srcRect.right = width;

        }else if(height > width){
            int newHeight = width;
            bitmap = Bitmap.createBitmap(width, newHeight, Bitmap.Config.ARGB_8888);
            srcRect.top = 0;
            srcRect.bottom = newHeight;
            srcRect.left = 0;
            srcRect.right = width;
//            float decrease = height - newHeight;
//            int decreaseHalf = (int) (decrease/2);
//
//            srcRect.top = decreaseHalf;
//            srcRect.bottom = (height - decreaseHalf);
//            srcRect.left = 0;
//            srcRect.right = width;
        }else{
            int newWidth = height;
            bitmap = Bitmap.createBitmap(newWidth, height, Bitmap.Config.ARGB_8888);
            srcRect.left = 0;
            srcRect.right = newWidth;
            srcRect.top = 0;
            srcRect.bottom = height;
//            float decrease = width - newWidth;
//            int decreaseHalf = (int) (decrease/2);
//
//            srcRect.left = decreaseHalf;
//            srcRect.right = (width - decreaseHalf);
//            srcRect.top = 0;
//            srcRect.bottom = height;
        }

//        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//        srcRect.left = 0;
//        srcRect.top = 0;
//        srcRect.bottom = height;
//        srcRect.right = width;
//        adjustAspectRatio();

        return bitmap;
    }

    public void DrawBitmap(boolean flag) {

        if(bitmap == null)
            return;

        if (surfaceHolder == null){
            return;
        }

        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas != null) {
            canvas.scale(-1, 1);
            canvas.translate(-canvas.getWidth(), 0);
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

}
