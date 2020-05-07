package mega.privacy.android.app.lollipop.megachat.calls;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.nio.ByteBuffer;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatVideoListenerInterface;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.VideoCaptureUtils.*;

public class RemoteCameraCallFullScreenFragment extends Fragment implements MegaChatVideoListenerInterface, View.OnClickListener {

    private SurfaceView fullScreenSurfaceView = null;
    private int width = 0;
    private int height = 0;
    private Bitmap bitmap;
    private MegaChatApiAndroid megaChatApi;
    private Context context;
    private long chatId;
    private long peerid;
    private long clientid;
    private MegaSurfaceRenderer megaSurfaceRender;

    public static RemoteCameraCallFullScreenFragment newInstance(long chatId, long peerid, long clientid) {
        logDebug("Chat ID: " + chatId);
        RemoteCameraCallFullScreenFragment f = new RemoteCameraCallFullScreenFragment();
        Bundle args = new Bundle();
        args.putLong(CHAT_ID, chatId);
        args.putLong(PEER_ID, peerid);
        args.putLong(CLIENT_ID, clientid);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (megaChatApi == null) {
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }

        Bundle args = getArguments();
        this.chatId = args.getLong(CHAT_ID, -1);
        this.peerid = args.getLong(PEER_ID, -1);
        this.clientid = args.getLong(CLIENT_ID, -1);


        logDebug("Chat ID: " + chatId);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (!isAdded())
            return null;

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        View v = inflater.inflate(R.layout.fragment_remote_camera_call_full_screen, container, false);
        fullScreenSurfaceView = v.findViewById(R.id.surface_remote_video);
        if (isItMe(chatId, peerid, clientid)) {
            logDebug("**************** CREATE VIEW MEEEEE");
            fullScreenSurfaceView.setOnClickListener(null);
            fullScreenSurfaceView.setZOrderMediaOverlay(true);
        } else {
            logDebug("**************** CREATE VIEW CONTACT");
            fullScreenSurfaceView.setOnClickListener(this);
            fullScreenSurfaceView.setZOrderOnTop(false);
            fullScreenSurfaceView.setZOrderMediaOverlay(false);
        }

        SurfaceHolder remoteSurfaceHolder = fullScreenSurfaceView.getHolder();
        remoteSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        megaSurfaceRender = new MegaSurfaceRenderer(fullScreenSurfaceView, false, metrics);
        logDebug("Adding video listener");

        if (isItMe(chatId, peerid, clientid)) {
            megaChatApi.addChatLocalVideoListener(chatId, this);
        } else {
            megaChatApi.addChatRemoteVideoListener(chatId, peerid, clientid, this);
        }

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
                    this.bitmap = megaSurfaceRender.createBitmap(width, height);
                    holder.setFixedSize(holderWidth, holderHeight);
                } else {
                    this.width = -1;
                    this.height = -1;
                }
            }

        }
        if (bitmap == null) return;
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer));
        if (isItMe(chatId, peerid, clientid)) {
            if (isVideoAllowed()) {
                megaSurfaceRender.drawBitmap( true);
            }
        } else {
            megaSurfaceRender.drawBitmap( false);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDestroy() {
        this.removeSurfaceView();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        this.width = 0;
        this.height = 0;
        fullScreenSurfaceView.setVisibility(View.VISIBLE);
        super.onResume();
    }

    public void removeSurfaceView() {
        if (fullScreenSurfaceView.getParent() != null && fullScreenSurfaceView.getParent().getParent() != null) {
            logDebug("Removing suface view");
            ((ViewGroup) fullScreenSurfaceView.getParent()).removeView(fullScreenSurfaceView);
        }
        fullScreenSurfaceView.setVisibility(View.GONE);
        logDebug("Removing remote video listener");

        if (isItMe(chatId, peerid, clientid)) {
            megaChatApi.removeChatVideoListener(chatId, -1, -1, this);
        } else {
            megaChatApi.removeChatVideoListener(chatId, peerid, clientid, this);
        }
    }

    @Override
    public void onClick(View v) {
        ((ChatCallActivity) context).remoteCameraClick();
    }
}
