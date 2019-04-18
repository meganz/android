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
import mega.privacy.android.app.utils.Util;

public class MegaSurfaceRenderer implements Callback {

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


    public MegaSurfaceRenderer(SurfaceView view) {
        log("MegaSurfaceRenderer() ");

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
        log("changeDestRect(): dstWidth = "+dstWidth+", dstHeight = "+dstHeight);
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
        log("surfaceChanged(): in_width = "+in_width+", in_height = "+in_height);

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
        log("CreateBitmap(): width = "+width+", height = "+height);


        Logging.d(TAG, "CreateByteBitmap " + width + ":" + height);
        if (bitmap == null) {
            try {
                android.os.Process.setThreadPriority(
                    android.os.Process.THREAD_PRIORITY_DISPLAY);
            }
            catch (Exception e) {
            }
        }
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        srcRect.left = 0;
        srcRect.top = 0;
        srcRect.bottom = height;
        srcRect.right = width;

        log("CreateBitmap(): sRect(T "+srcRect.top+" -B "+srcRect.bottom+")(L "+srcRect.left+" - R "+srcRect.right+")");


        adjustAspectRatio();

        return bitmap;
    }

    public ByteBuffer CreateByteBuffer(int width, int height) {
        log("CreateByteBuffer(): width = "+width+", height = "+height);

        Logging.d(TAG, "CreateByteBuffer " + width + ":" + height);
        if (bitmap == null) {
            bitmap = CreateBitmap(width, height);
            byteBuffer = ByteBuffer.allocateDirect(width * height * 2);
        }
        return byteBuffer;
    }

    // It saves bitmap data to a JPEG picture, this function is for debug only.
    private void saveBitmapToJPEG(int width, int height) {
        log("saveBitmapToJPEG(): width = "+width+", height = "+height);

        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteOutStream);

        try{
            FileOutputStream output = new FileOutputStream(String.format("/sdcard/render_%d.jpg", System.currentTimeMillis()));
            output.write(byteOutStream.toByteArray());
            output.flush();
            output.close();
        }
        catch (FileNotFoundException e) {
        }
        catch (IOException e) {
        }
    }

//    public void DrawByteBuffer() {
//        log("DrawByteBuffer(): ");
//
//        if(byteBuffer == null)
//            return;
//        byteBuffer.rewind();
//        bitmap.copyPixelsFromBuffer(byteBuffer);
//        DrawBitmap(false);
//    }

    public void DrawBitmap(boolean flag, boolean isLocal) {

        if(bitmap == null)
            return;

        if (surfaceHolder == null){
            return;
        }
        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas != null) {
            if(isLocal){
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

    private static void log(String log) {
        Util.log("MegaSurfaceRendererGroup", log);
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.BLUE);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

}
