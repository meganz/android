package mega.privacy.android.app.lollipop.megachat.calls;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.TextureView;
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

    private TextureView myTexture;
    private int width = 0;
    private int height = 0;
    private Bitmap bitmap;
    private MegaChatApiAndroid megaChatApi;
    private Context context;
    private long chatId;
    private long peerId;
    private long cliendId;
    private MegaSurfaceRendererGroup renderer;

    public static BigCameraGroupCallFragment newInstance(long chatId, long peerId, long cliendId) {
        log("newInstance");
        BigCameraGroupCallFragment f = new BigCameraGroupCallFragment();

        Bundle args = new Bundle();
        args.putLong("chatId", chatId);
        args.putLong("peerId", peerId);
        args.putLong("cliendId", cliendId);

        f.setArguments(args);
        return f;
    }

    private static void log(String log) {
        Util.log("BigCameraGroupCallFragment", log);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        log("onCreate");
        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
        }

        Bundle args = getArguments();
        this.chatId = args.getLong("chatId", -1);
        this.peerId = args.getLong("peerId", -1);
        this.cliendId = args.getLong("cliendId", -1);

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
        myTexture = v.findViewById(R.id.texture_view_video);
        myTexture.setAlpha(1.0f);
        myTexture.setRotation(0);
        myTexture.setVisibility(View.VISIBLE);
        this.width = 0;
        this.height = 0;
        renderer = new MegaSurfaceRendererGroup(myTexture, peerId, cliendId);

        if (peerId == megaChatApi.getMyUserHandle() && cliendId == megaChatApi.getMyClientidHandle(chatId)) {
            log("onCreateView() addChatLocalVideoListener  (LOCAL)  chatId: " + chatId);
            megaChatApi.addChatLocalVideoListener(chatId, this);
        } else {
            log("onCreateView() addChatRemoteVideoListener chatId: " + chatId + " ( peerId = " + peerId + ", clientId = " + cliendId + ")");
            megaChatApi.addChatRemoteVideoListener(chatId, peerId, cliendId, this);
        }
        return v;
    }

    @Override
    public void onChatVideoData(MegaChatApiJava api, long chatid, int width, int height, byte[] byteBuffer) {
        if ((width == 0) || (height == 0)) return;

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
            } else {
                this.width = -1;
                this.height = -1;
            }
        }

        if (bitmap == null) return;
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer));
        renderer.DrawBitmap(false, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        removeSurfaceView();
        super.onDestroy();
    }

    public void removeSurfaceView() {
        log("removeSurfaceView()");
        if (myTexture.getParent() != null && myTexture.getParent().getParent() != null) {
            log("removeSurfaceView() removeView chatId: " + chatId);
            ((ViewGroup) myTexture.getParent()).removeView(myTexture);
        }

        if (megaChatApi == null) return;
        if (peerId == megaChatApi.getMyUserHandle() && cliendId == megaChatApi.getMyClientidHandle(chatId)) {
            log("removeSurfaceView() removeChatVideoListener (LOCAL) chatId: " + chatId);
            megaChatApi.removeChatVideoListener(chatId, -1, -1, this);
        } else {
            log("removeSurfaceView() removeChatVideoListener chatId: " + chatId + " ( peerId = " + peerId + ", clientId = " + cliendId + ")");
            megaChatApi.removeChatVideoListener(chatId, peerId, cliendId, this);
        }
    }

    @Override
    public void onResume() {
        log("onResume");
        super.onResume();
    }
}
