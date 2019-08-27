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


public class RemoteCameraCallFullScreenFragment extends Fragment implements MegaChatVideoListenerInterface, View.OnClickListener {

    private SurfaceView remoteFullScreenSurfaceView;
    private int width = 0;
    private int height = 0;
    private Bitmap bitmap;
    private MegaChatApiAndroid megaChatApi;
    private Context context;
    private long chatId;
    private long peerid;
    private long clientid;
    private MegaSurfaceRenderer remoteRenderer;

    public static RemoteCameraCallFullScreenFragment newInstance(long chatId, long peerid, long clientid) {
        log("newInstance() chatId: " + chatId);

        RemoteCameraCallFullScreenFragment f = new RemoteCameraCallFullScreenFragment();
        Bundle args = new Bundle();
        args.putLong("chatId", chatId);
        args.putLong("peerid", peerid);
        args.putLong("clientid", clientid);
        f.setArguments(args);
        return f;
    }

    private static void log(String log) {
        Util.log("RemoteCameraCallFullScreenFragment", log);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        log("onCreate");
        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
        }

        Bundle args = getArguments();
        this.chatId = args.getLong("chatId", -1);
        this.peerid = args.getLong("peerid", -1);
        this.clientid = args.getLong("clientid", -1);

        log("onCreate() chatId: " + chatId);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (!isAdded()) {
            return null;
        }

        View v = inflater.inflate(R.layout.fragment_remote_camera_call_full_screen, container, false);
        remoteFullScreenSurfaceView = v.findViewById(R.id.surface_remote_video);
        remoteFullScreenSurfaceView.setOnClickListener(this);
        remoteFullScreenSurfaceView.setZOrderOnTop(false);
        remoteFullScreenSurfaceView.setZOrderMediaOverlay(false);
        SurfaceHolder remoteSurfaceHolder = remoteFullScreenSurfaceView.getHolder();
        remoteSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        remoteRenderer = new MegaSurfaceRenderer(remoteFullScreenSurfaceView);
        log("onCreateView() addChatRemoteVideoListener chatId: " + chatId);
        megaChatApi.addChatRemoteVideoListener(chatId, peerid, clientid, this);

        return v;
    }

    @Override
    public void onChatVideoData(MegaChatApiJava api, long chatid, int width, int height, byte[] byteBuffer) {

        if ((width == 0) || (height == 0)) {
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
                } else {
                    this.width = -1;
                    this.height = -1;
                }
            }
        }
        if (bitmap == null) return;
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer));
        remoteRenderer.DrawBitmap(false, false);

    }

    @Override
    public void onAttach(Context context) {
        log("onAttach");
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDestroy() {
        log("onDestroy");
        removeSurfaceView();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        log("onResume");
        this.width = 0;
        this.height = 0;
        remoteFullScreenSurfaceView.setVisibility(View.VISIBLE);

        super.onResume();
    }

    public void removeSurfaceView() {
        log("removeSurfaceView()");
        if (remoteFullScreenSurfaceView.getParent() != null && remoteFullScreenSurfaceView.getParent().getParent() != null) {
            log("removeSurfaceView() removeView chatId: " + chatId);
            ((ViewGroup) remoteFullScreenSurfaceView.getParent()).removeView(remoteFullScreenSurfaceView);
        }
        remoteFullScreenSurfaceView.setVisibility(View.GONE);
        log("removeSurfaceView() removeChatVideoListener (REMOTE) chatId: " + chatId);
        megaChatApi.removeChatVideoListener(chatId, peerid, clientid, this);
    }

    @Override
    public void onClick(View v) {
        ((ChatCallActivity) context).remoteCameraClick();
    }
}
