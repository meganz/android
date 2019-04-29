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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
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
import java.util.ArrayList;
import java.util.List;

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
import android.view.TextureView;

import org.webrtc.Logging;

import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;

public class MegaSurfaceRendererGroup implements TextureView.SurfaceTextureListener{

    private final static String TAG = "WEBRTC";

    // the bitmap used for drawing.
    private Bitmap bitmap = null;
    private ByteBuffer byteBuffer = null;
    private SurfaceTexture surfaceHolder;

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
    long peerId;
    long clientId;
    private TextureView myTexture = null;

    protected List<MegaSurfaceRendererGroupListener> listeners;


    public MegaSurfaceRendererGroup(TextureView view, long peerId, long clientId) {
        log("MegaSurfaceRendererGroup(): ");

        this.myTexture = view;
        myTexture.setSurfaceTextureListener(this);
        bitmap = myTexture.getBitmap();
        paint = new Paint();
        modesrcover = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
        modesrcin = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
        this.peerId = peerId;
        this.clientId = clientId;
        listeners = new ArrayList<MegaSurfaceRendererGroupListener>();
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
        log("adjustAspectRatio()");
        if (bitmap != null && dstRect.height() != 0) {
            dstRect.top = 0;
            dstRect.left = 0;
            dstRect.right = surfaceWidth;
            dstRect.bottom = surfaceHeight;
        }
    }

    public Bitmap CreateBitmap(int width, int height) {
        log("CreateBitmap(): width = "+width+", height = "+height);
        Logging.d(TAG, "CreateByteBitmap " + width + ":" + height);
        if (bitmap == null) {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
            }catch (Exception e) {}
        }

        if(height == width){
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            srcRect.top = 0;
            srcRect.bottom = height;
            srcRect.left = 0;
            srcRect.right = width;
            log(" CreateBitmap(): width == height. sRect(T "+srcRect.top+" -B "+srcRect.bottom+")(L "+srcRect.left+" - R "+srcRect.right+")");

        }else if(height > width){
            bitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888);
            srcRect.top = 0;
            srcRect.bottom = width;
            srcRect.left = 0;
            srcRect.right = width;
            log("CreateBitmap(): height > width. sRect(T "+srcRect.top+" -B "+srcRect.bottom+")(L "+srcRect.left+" - R "+srcRect.right+")");

        }else{
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            srcRect.left = 0;
            srcRect.right = height;
            srcRect.top = 0;
            srcRect.bottom = height;
            log("CreateBitmap(): height < width. sRect(T "+srcRect.top+" -B "+srcRect.bottom+")(L "+srcRect.left+" - R "+srcRect.right+")");
        }
        adjustAspectRatio();
        return bitmap;
    }

    public void DrawBitmap(boolean flag, boolean isLocal) {
        if(bitmap == null){
            return;
        }
        if (myTexture == null){
            return;
        }
        Canvas canvas = myTexture.lockCanvas();
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
            myTexture.unlockCanvasAndPost(canvas);
        }
    }

    private void notifyStateToAll() {
        for(MegaSurfaceRendererGroupListener listener : listeners)
            notifyState(listener);
    }

    public void addListener(MegaSurfaceRendererGroupListener l) {
        listeners.add(l);
        notifyState(l);
    }

    private void notifyState(MegaSurfaceRendererGroupListener listener) {
        if(listener == null)
            return;
        listener.resetSize(peerId, clientId);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int in_width, int in_height) {
        log("onSurfaceTextureAvailable()");
        Bitmap textureViewBitmap = myTexture.getBitmap();
        Canvas canvas = new Canvas(textureViewBitmap);
        if(canvas != null) {
            notifyStateToAll();
            changeDestRect(in_width, in_height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int in_width, int in_height) {
        log("onSurfaceTextureSizeChanged(): in_width = "+in_width+", in_height = "+in_height);
        changeDestRect(in_width, in_height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        log("onSurfaceTextureDestroyed() -> surfaceWidth = 0 && surfaceHeight = 0");
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
//        if(byteBuffer == null)
//            return;
//        byteBuffer.rewind();
//        bitmap.copyPixelsFromBuffer(byteBuffer);
//        DrawBitmap(false);
//    }

    private static void log(String log) {
        Util.log("MegaSurfaceRendererGroup", log);
    }

}
