package mega.privacy.android.app.lollipop.megachat.calls;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.nio.ByteBuffer;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatVideoListenerInterface;


public class RemoteCameraCallFullScreenFragment extends Fragment implements MegaChatVideoListenerInterface, View.OnClickListener {

    int width = 0;
    int height = 0;
    Bitmap bitmap;
    MegaChatApiAndroid megaChatApi;
    Context context;
    long chatId;
    long userHandle;

    public SurfaceView remoteFullScreenSurfaceView;
    MegaSurfaceRenderer remoteRenderer;

    public static RemoteCameraCallFullScreenFragment newInstance(long chatId, long userHandle) {
        log("newInstance");
        RemoteCameraCallFullScreenFragment f = new RemoteCameraCallFullScreenFragment();

        Bundle args = new Bundle();
        args.putLong("chatId", chatId);
        args.putLong("userHandle",userHandle);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");
        if (megaChatApi == null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }

        Bundle args = getArguments();
        this.chatId = args.getLong("chatId", -1);
        this.userHandle = args.getLong("userHandle", -1);
        super.onCreate(savedInstanceState);
        log("after onCreate called super");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        if (!isAdded()) {
            return null;
        }

        View v = inflater.inflate(R.layout.fragment_remote_camera_call_full_screen, container, false);

        remoteFullScreenSurfaceView = (SurfaceView)v.findViewById(R.id.surface_remote_video);
        remoteFullScreenSurfaceView.setOnClickListener(this);
        remoteFullScreenSurfaceView.setZOrderMediaOverlay(true);
        SurfaceHolder remoteSurfaceHolder = remoteFullScreenSurfaceView.getHolder();
        remoteSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        remoteRenderer = new MegaSurfaceRenderer(remoteFullScreenSurfaceView);
        megaChatApi.addChatRemoteVideoListener(chatId, userHandle, this);

        return v;
    }

    @Override
    public void onChatVideoData(MegaChatApiJava api, long chatid, int width, int height, byte[] byteBuffer){
        log("onChatVideoData");

        if((width == 0) || (height == 0)){
            return;
        }

        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;

            SurfaceHolder holder = remoteFullScreenSurfaceView.getHolder();
            if (holder != null) {
                int viewWidth = remoteFullScreenSurfaceView.getWidth();
                int viewHeight = remoteFullScreenSurfaceView.getHeight();
                if ((viewWidth != 0) && (viewHeight != 0)) {
                    int holderWidth = viewWidth < width ? viewWidth : width;
                    int holderHeight = holderWidth * viewHeight / viewWidth;
                    if (holderHeight > viewHeight) {
                        holderHeight = viewHeight;
                        holderWidth = holderHeight * viewWidth / viewHeight;
                    }
                    this.bitmap = remoteRenderer.CreateBitmap(width, height);
                    holder.setFixedSize(holderWidth, holderHeight);
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
            remoteRenderer.DrawBitmap(false);
        }
    }



    @Override
    public void onAttach(Context context) {
        log("onAttach");

        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDestroy(){
        log("onDestroy");

        megaChatApi.removeChatVideoListener(chatId, userHandle, this);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        log("onResume");
        this.width=0;
        this.height=0;
        super.onResume();
    }

    public void setVideoFrame(boolean visible){
        log("setVideoFrame");
        if(visible){
            remoteFullScreenSurfaceView.setVisibility(View.VISIBLE);
        }
        else{
            remoteFullScreenSurfaceView.setVisibility(View.GONE);
        }
    }

    private static void log(String log) {
        Util.log("RemoteCameraCallFullScreenFragment", log);
    }

    @Override
    public void onClick(View v) {
        ((ChatCallActivity)context).remoteCameraClick();
    }
}
