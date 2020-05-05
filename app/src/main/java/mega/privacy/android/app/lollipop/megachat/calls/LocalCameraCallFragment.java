package mega.privacy.android.app.lollipop.megachat.calls;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.nio.ByteBuffer;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatVideoListenerInterface;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.VideoCaptureUtils.*;

public class LocalCameraCallFragment extends Fragment implements MegaChatVideoListenerInterface {

    private SurfaceView localSurfaceView = null;
    private int width = 0;
    private int height = 0;
    private Bitmap bitmap;
    private MegaChatApiAndroid megaChatApi;
    private Context context;
    private long chatId;
    private MegaSurfaceRenderer localRenderer;
    private ImageView microIcon;

    public static LocalCameraCallFragment newInstance(long chatId) {
        logDebug("Chat ID "+chatId);
        LocalCameraCallFragment f = new LocalCameraCallFragment();
        Bundle args = new Bundle();
        args.putLong(CHAT_ID, chatId);
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
        logDebug("Chat ID: " + chatId);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (!isAdded()) {
            return null;
        }

        View v = inflater.inflate(R.layout.fragment_local_camera_call, container, false);
        localSurfaceView = v.findViewById(R.id.surface_local_video);
        localSurfaceView.setZOrderMediaOverlay(true);
        SurfaceHolder localSurfaceHolder = localSurfaceView.getHolder();
        localSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        localRenderer = new MegaSurfaceRenderer(localSurfaceView);
        microIcon = v.findViewById(R.id.micro_surface_view);
        microIcon.setVisibility(View.GONE);
        ((ChatCallActivity) context).refreshOwnMicro();
        logDebug("Adding local video listener");
        megaChatApi.addChatLocalVideoListener(chatId, this);

        return v;
    }

    @Override
    public void onChatVideoData(MegaChatApiJava api, long chatid, int width, int height, byte[] byteBuffer) {
        if ((width == 0) || (height == 0)) return;

        if (this.width != width || this.height != height) {
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
                    this.bitmap = localRenderer.createBitmap(width, height);
                    holder.setFixedSize(holderWidth, holderHeight);
                } else {
                    this.width = -1;
                    this.height = -1;
                }
            }

        }

        if (bitmap == null) return;

        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer));
        if (isVideoAllowed()) {
            localRenderer.drawBitmap(true, true);
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
    public void onDestroy() {
        this.removeSurfaceView();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        this.width = 0;
        this.height = 0;
        localSurfaceView.setVisibility(View.VISIBLE);
        super.onResume();
    }

    public void showMicro(boolean isShouldShown) {
        if (microIcon == null) return;
        if (isShouldShown) {
            microIcon.setVisibility(View.VISIBLE);
            return;
        }
        microIcon.setVisibility(View.GONE);
    }

    public void removeSurfaceView() {
        if (microIcon != null) {
            microIcon.setVisibility(View.GONE);
        }
        if (localSurfaceView.getParent() != null && localSurfaceView.getParent().getParent() != null) {
            logDebug("Removing suface view");
            ((ViewGroup) localSurfaceView.getParent()).removeView(localSurfaceView);
        }
        localSurfaceView.setVisibility(View.GONE);

        logDebug("Removing local video listener");
        megaChatApi.removeChatVideoListener(chatId, -1, -1, this);
    }
}
