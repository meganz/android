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


public class LocalCameraCallFragment extends Fragment implements MegaChatVideoListenerInterface {

    int width = 0;
    int height = 0;
    Bitmap bitmap;
    MegaChatApiAndroid megaChatApi;
    Context context;
    long chatId;

    SurfaceView localSurfaceView = null;
    MegaSurfaceRenderer localRenderer;

    public static LocalCameraCallFragment newInstance(long chatId) {
        log("#### newInstance: cID: "+chatId);
        LocalCameraCallFragment f = new LocalCameraCallFragment();

        Bundle args = new Bundle();
        args.putLong("chatId", chatId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate cID: "+chatId);
        if (megaChatApi == null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
        }
        this.width = 0;
        this.height = 0;
        this.localSurfaceView = null;
        Bundle args = getArguments();
        this.chatId = args.getLong("chatId", -1);

        super.onCreate(savedInstanceState);
        log("after onCreate called super");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (!isAdded()) {
            return null;
        }

        View v = inflater.inflate(R.layout.fragment_local_camera_call, container, false);
        this.width = 0;
        this.height = 0;
        localSurfaceView = (SurfaceView)v.findViewById(R.id.surface_local_video);

        localSurfaceView.setZOrderOnTop(true);
        SurfaceHolder localSurfaceHolder = localSurfaceView.getHolder();
        localSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        localRenderer = new MegaSurfaceRenderer(localSurfaceView);

        megaChatApi.addChatLocalVideoListener(chatId, this);

        return v;
    }

    @Override
    public void onChatVideoData(MegaChatApiJava api, long chatid, int width, int height, byte[] byteBuffer) {

        if((width == 0) || (height == 0)){
            return;
        }

        if (this.width != width || this.height != height)
        {
            this.width = width;
            this.height = height;

            SurfaceHolder holder = localSurfaceView.getHolder();
            if (holder != null) {
                int viewWidth = localSurfaceView.getWidth();
                int viewHeight = localSurfaceView.getHeight();
                if ((viewWidth != 0) && (viewHeight != 0)) {
                    int holderWidth = viewWidth < width ? viewWidth : width;
                    int holderHeight = holderWidth * viewHeight / viewWidth;
                    if (holderHeight > viewHeight) {
                        holderHeight = viewHeight;
                        holderWidth = holderHeight * viewWidth / viewHeight;
                    }
                    this.bitmap = localRenderer.CreateBitmap(width, height);
                    holder.setFixedSize(holderWidth, holderHeight);
                }
                else{
                    this.width = -1;
                    this.height = -1;
                }
            }
        }

        if (bitmap != null){
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer));
            localRenderer.DrawBitmap(true);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDestroy(){
        log("onDestroy");

        if(localSurfaceView.getParent()!=null){
            if(localSurfaceView.getParent().getParent()!=null){
                ((ViewGroup)localSurfaceView.getParent()).removeView(localSurfaceView);
            }else{
                ((ViewGroup)localSurfaceView.getParent()).removeAllViewsInLayout();
            }
        }
        megaChatApi.removeChatVideoListener(chatId, -1, this);
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
        log("setVideoFrame: "+visible);
        if(visible){
            localSurfaceView.setVisibility(View.VISIBLE);
        }
        else{
            localSurfaceView.setVisibility(View.GONE);
        }
    }

    private static void log(String log) {
        Util.log("LocalCameraCallFragment", log);
    }
}
