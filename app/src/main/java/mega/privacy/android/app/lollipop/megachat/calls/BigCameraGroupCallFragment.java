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
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

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

    public TextureView myTexture;
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
        super.onCreate(savedInstanceState);
        log("after onCreate called super");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        if (!isAdded()) {
            return null;
        }

        View v = inflater.inflate(R.layout.fragment_camera_full_screen_big, container, false);
        myTexture = (TextureView) v.findViewById(R.id.texture_view_video);
        myTexture.setAlpha(1.0f);
        myTexture.setRotation(0);
        myTexture.setVisibility(View.VISIBLE);
        this.width = 0;
        this.height = 0;
        renderer = new MegaSurfaceRendererGroup(myTexture, userHandle);


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

                int viewWidth = myTexture.getWidth();
                int viewHeight = myTexture.getHeight();
                if ((viewWidth != 0) && (viewHeight != 0)) {
                    int holderWidth = viewWidth < width ? viewWidth : width;
                    int holderHeight = holderWidth * viewHeight / viewWidth;
                    if (holderHeight > viewHeight) {
                        holderHeight = viewHeight;
                        holderWidth = holderHeight * viewWidth / viewHeight;
                    }
                    this.bitmap = renderer.CreateBitmap(width, height);
                }else{
                    this.width = -1;
                    this.height = -1;
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
        if(myTexture.getParent()!=null){
            if(myTexture.getParent().getParent()!=null){
                log("onDestroy() removeView chatId: "+chatId);
                ((ViewGroup)myTexture.getParent()).removeView(myTexture);
            }
        }
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
        super.onResume();
    }

    private static void log(String log) {
        Util.log("BigCameraGroupCallFragment", log);
    }
}
