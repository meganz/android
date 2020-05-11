package mega.privacy.android.app.lollipop.megachat.calls;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import java.nio.ByteBuffer;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatVideoListenerInterface;

import static mega.privacy.android.app.utils.LogUtil.*;

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
    private int numParticipants;
    private MegaSurfaceRendererGroup renderer;

    public static BigCameraGroupCallFragment newInstance(long chatId, long peerId, long cliendId, int numParticipants) {
        logDebug("newInstance");
        BigCameraGroupCallFragment f = new BigCameraGroupCallFragment();

        Bundle args = new Bundle();
        args.putLong("chatId", chatId);
        args.putLong("peerId", peerId);
        args.putLong("cliendId", cliendId);
        args.putInt("numParticipants", numParticipants);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        logDebug("onCreate");
        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
        }

        Bundle args = getArguments();
        this.chatId = args.getLong("chatId", -1);
        this.peerId = args.getLong("peerId", -1);
        this.cliendId = args.getLong("cliendId", -1);
        this.numParticipants = args.getInt("numParticipants", -1);

        super.onCreate(savedInstanceState);
        logDebug("After onCreate called super");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logDebug("onCreateView");

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
        renderer = new MegaSurfaceRendererGroup(myTexture, peerId, cliendId, numParticipants);
        logDebug("addChatRemoteVideoListener chatId: " + chatId + " ( peerId = " + peerId + ", clientId = " + cliendId + ")");
        megaChatApi.addChatRemoteVideoListener(chatId, peerId, cliendId, this);
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
                this.bitmap = renderer.createBitmap(width, height);
            } else {
                this.width = -1;
                this.height = -1;
            }
        }
        if (bitmap == null) return;
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(byteBuffer));
        renderer.drawBitmap(false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDestroy() {
        logDebug("onDestroy");
        removeSurfaceView();
        super.onDestroy();
    }

    public void removeSurfaceView() {
        logDebug("removeSurfaceView()");
        if (myTexture.getParent() != null && myTexture.getParent().getParent() != null) {
            logDebug("removeView chatId: " + chatId);
            ((ViewGroup) myTexture.getParent()).removeView(myTexture);
        }
        if (megaChatApi == null) return;
        logDebug("removeChatVideoListener chatId: " + chatId + " ( peerId = " + peerId + ", clientId = " + cliendId + ")");
        megaChatApi.removeChatVideoListener(chatId, peerId, cliendId, this);

    }

    @Override
    public void onResume() {
        logDebug("onResume");
        super.onResume();
    }
}
