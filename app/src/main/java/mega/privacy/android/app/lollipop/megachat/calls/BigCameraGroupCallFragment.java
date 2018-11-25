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
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatVideoListenerInterface;

public class BigCameraGroupCallFragment extends Fragment implements MegaChatVideoListenerInterface {

    int width = 0;
    int height = 0;
    Bitmap bitmap;
    MegaChatApiAndroid megaChatApi;
    Context context;
    long chatId;
    Long userHandle;

    public SurfaceView fullScreenSurfaceView;
    MegaSurfaceRendererGroup renderer;

    public static BigCameraGroupCallFragment newInstance(long chatId, long userHandle) {
        log("newInstance");
        BigCameraGroupCallFragment f = new BigCameraGroupCallFragment();

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
//        this.width = 0;
//        this.height = 0;
        super.onCreate(savedInstanceState);
        log("after onCreate called super");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        if (!isAdded()) {
            return null;
        }

        View v = inflater.inflate(R.layout.fragment_local_camera_call_full_screen, container, false);

        fullScreenSurfaceView = (SurfaceView)v.findViewById(R.id.surface_local_video);
        fullScreenSurfaceView.setZOrderMediaOverlay(true);
        SurfaceHolder localSurfaceHolder = fullScreenSurfaceView.getHolder();
        localSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        renderer = new MegaSurfaceRendererGroup(fullScreenSurfaceView, userHandle);

        if(userHandle.equals(megaChatApi.getMyUserHandle())){
            log("onCreateView() addChatLocalVideoListener chatId: "+chatId);
            megaChatApi.addChatLocalVideoListener(chatId, this);
        }else{
            log("onCreateView() addChatRemoteVideoListener chatId: "+chatId);
            megaChatApi.addChatRemoteVideoListener(chatId, userHandle, this);
        }

        return v;
    }

    @Override
    public void onChatVideoData(MegaChatApiJava api, long chatid, int width, int height, byte[] byteBuffer) {
        if((width == 0) || (height == 0)){
            return;
        }

        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;

            SurfaceHolder holder = fullScreenSurfaceView.getHolder();
            if (holder != null) {
                int viewWidth = fullScreenSurfaceView.getWidth();
                int viewHeight = fullScreenSurfaceView.getHeight();
                if ((viewWidth != 0) && (viewHeight != 0)) {
                    int holderWidth = viewWidth < width ? viewWidth : width;
                    int holderHeight = holderWidth * viewHeight / viewWidth;
                    if (holderHeight > viewHeight) {
                        holderHeight = viewHeight;
                        holderWidth = holderHeight * viewWidth / viewHeight;
                    }
                    this.bitmap = renderer.CreateBitmap(width, height);
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
            renderer.DrawBitmap(false);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDestroy(){
        log("onDestroy");
        if(fullScreenSurfaceView.getParent()!=null){
            if(fullScreenSurfaceView.getParent().getParent()!=null){
                log("onDestroy() removeView chatId: "+chatId);
                ((ViewGroup)fullScreenSurfaceView.getParent()).removeView(fullScreenSurfaceView);
            }
        }
        fullScreenSurfaceView.setVisibility(View.GONE);
        if(userHandle.equals(megaChatApi.getMyUserHandle())){
            log("onDestroy() removeChatVideoListener (LOCAL) chatId: "+chatId);
            megaChatApi.removeChatVideoListener(chatId, -1, this);
        }else{
            log("onDestroy() removeChatVideoListener (REMOTE) chatId: "+chatId);
            megaChatApi.removeChatVideoListener(chatId, userHandle, this);
        }
        super.onDestroy();
    }
    @Override
    public void onResume() {
        log("onResume");
        this.width=0;
        this.height=0;
        fullScreenSurfaceView.setVisibility(View.VISIBLE);

        super.onResume();
    }

    public void removeSurfaceView(){
        log("removeSurfaceView()");
        if(fullScreenSurfaceView.getParent()!=null){
            if(fullScreenSurfaceView.getParent().getParent()!=null){
                log("removeSurfaceView() removeView chatId: "+chatId);
                ((ViewGroup)fullScreenSurfaceView.getParent()).removeView(fullScreenSurfaceView);
            }
        }
        fullScreenSurfaceView.setVisibility(View.GONE);
        if(userHandle.equals(megaChatApi.getMyUserHandle())){
            log("removeSurfaceView() removeChatVideoListener (LOCAL) chatId: "+chatId);
            megaChatApi.removeChatVideoListener(chatId, -1, this);
        }else{
            log("removeSurfaceView() removeChatVideoListener (REMOTE) chatId: "+chatId);
            megaChatApi.removeChatVideoListener(chatId, userHandle, this);
        }
    }

    private static void log(String log) {
        Util.log("LocalCameraCallFullScreenFragment", log);
    }
}
