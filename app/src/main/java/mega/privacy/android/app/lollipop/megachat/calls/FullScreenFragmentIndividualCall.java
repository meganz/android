package mega.privacy.android.app.lollipop.megachat.calls;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.fragment.app.Fragment;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatSession;

import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class FullScreenFragmentIndividualCall extends Fragment implements View.OnClickListener {

    private ImageView avatarImageOnHold;
    private IndividualCallListener listener = null;
    private Context context;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private DatabaseHandler dbH;

    private DisplayMetrics outMetrics;
    private View contentView;

    private Bitmap bitmap;
    private long chatId;
    private long peerid;
    private long clientid;
    private boolean isSmallCamera;

    private SurfaceView surfaceView = null;
    private RelativeLayout avatarLayout;
    private RelativeLayout avatarBackground;
    private RoundedImageView avatarImage;
    private MegaChatCall call;
    private MegaChatRoom chatRoom;

    public static FullScreenFragmentIndividualCall newInstance(long chatId, long peerid, long clientid) {
        logDebug("Chat ID: " + chatId);

        FullScreenFragmentIndividualCall f = new FullScreenFragmentIndividualCall();
        Bundle args = new Bundle();
        args.putLong(CHAT_ID, chatId);
        args.putLong(PEER_ID, peerid);
        args.putLong(CLIENT_ID, clientid);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle args = getArguments();
        this.chatId = args.getLong(CHAT_ID, MEGACHAT_INVALID_HANDLE);
        this.peerid = args.getLong(PEER_ID, INVALID_CALL_PEER_ID);
        this.clientid = args.getLong(CLIENT_ID, INVALID_CALL_CLIENT_ID);
        this.isSmallCamera = false;
        megaApi = MegaApplication.getInstance().getMegaApi();
        megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        this.chatRoom = megaChatApi.getChatRoom(chatId);
        this.call = megaChatApi.getChatCall(chatId);

        if (chatRoom == null || call == null)
            return;

        if(peerid == INVALID_CALL_PEER_ID){
            this.peerid = chatRoom.getPeerHandle(0);
        }

        logDebug("Chat ID: " + chatId);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (!isAdded())
            return null;

        contentView = inflater.inflate(R.layout.full_screen_individual_call, container, false);
        surfaceView = contentView.findViewById(R.id.video);
        surfaceView.setOnClickListener(this);
        surfaceView.setVisibility(View.GONE);
        avatarLayout = contentView.findViewById(R.id.avatar_layout);
        avatarLayout.setOnClickListener(this);
        avatarLayout.setVisibility(View.GONE);
        avatarBackground = contentView.findViewById(R.id.avatar_background);
        avatarImage = contentView.findViewById(R.id.avatar_image);
        avatarImageOnHold = contentView.findViewById(R.id.avatar_image_on_hold);
        avatarImageOnHold.setVisibility(View.GONE);
        avatarImage.setAlpha(1f);
        init();

        return contentView;
    }

    private MegaChatSession getSession() {
        if (isItMe(chatId, peerid, clientid))
            return null;

        return call.getMegaChatSession(peerid, clientid);
    }

    private void init(){
        updateAvatar(peerid);
        checkValues(peerid, clientid, null);
    }

    public void checkValues(long peerId, long clientId, MegaChatSession session){
        MegaChatCall callChat = ((ChatCallActivity)context).getCall();
        if(callChat == null)
            return;

        if(peerId != peerid || clientId != clientid)
            return;

        if(isItMe(chatId, peerId, clientId)){
            if (callChat.hasLocalVideo() && !callChat.isOnHold() && !((ChatCallActivity)context).isSessionOnHold()) {
                activateVideo();
                return;
            }

            showAvatar();
            return;
        }

        if (session != null && session.hasVideo() && !callChat.isOnHold() && !((ChatCallActivity)context).isSessionOnHold()) {
            activateVideo();
            return;
        }

        showAvatar();
    }

    private void activateVideo() {
        if(surfaceView == null || surfaceView.getVisibility() == View.VISIBLE)
            return;

        hideAvatar();
        if(listener == null) {
            listener = new IndividualCallListener(context, surfaceView, peerid, clientid, chatId, outMetrics);
            if (isItMe(chatId, peerid, clientid)) {
                megaChatApi.addChatLocalVideoListener(chatId, listener);
            } else {
                megaChatApi.addChatRemoteVideoListener(chatId, peerid, clientid, listener);
            }
        }else{
            listener.setHeight(0);
            listener.setWidth(0);
        }

        surfaceView.setVisibility(View.VISIBLE);
    }

    private void deactivateVideo() {
        if(surfaceView == null || listener == null || surfaceView.getVisibility() == View.GONE) {
            return;
        }
        if (isItMe(chatId, peerid, clientid)) {
            megaChatApi.removeChatVideoListener(chatId, -1, -1, listener);
        } else {
            megaChatApi.removeChatVideoListener(chatId, peerid, clientid, listener);
        }

        listener = null;
        surfaceView.setVisibility(View.GONE);
    }

    private void updateAvatar(long newPeerId) {

        if(peerid == newPeerId)
            return;

        chatRoom = megaChatApi.getChatRoom(chatId);

        /*Default Avatar*/
        Bitmap defaultBitmap = getDefaultAvatarCall(chatRoom, peerid, true, true);
        avatarImage.setImageBitmap(defaultBitmap);

        /*Avatar*/
        Bitmap bitmap = getImageAvatarCall(context, chatRoom, peerid);
        if (bitmap != null) {
            avatarImage.setImageBitmap(bitmap);
        }
    }

    public void setAvatar(long peerId, Bitmap bitmap) {
        if (!isItMe(chatId, peerid, clientid) && peerId == chatRoom.getPeerHandle(0) && bitmap != null && avatarImage != null) {
            avatarImage.setImageBitmap(bitmap);
        }
    }

    private void showAvatar() {
        if(avatarLayout == null)
            return;

        deactivateVideo();
        avatarLayout.setVisibility(View.VISIBLE);

        if ((call != null && call.isOnHold()) || (getSession() != null && getSession().isOnHold())) {
            avatarImageOnHold.setVisibility(View.VISIBLE);
            avatarImage.setAlpha(0.5f);
        } else {
            avatarImageOnHold.setVisibility(View.GONE);
            avatarImage.setAlpha(1f);
        }
    }

    private void hideAvatar() {
        avatarImageOnHold.setVisibility(View.GONE);
        avatarImage.setAlpha(1f);
        avatarLayout.setVisibility(View.GONE);
    }

    public void changeUser(long newChatId, long callId, long newPeerId, long newClientId, MegaChatSession session) {
        logDebug(" Current peerI: "+peerid+", new peerId = "+newPeerId+". Current clientId: "+clientid+", new clientId = "+newClientId);

        if (newPeerId == peerid && newClientId == clientid)
            return;

        //Remove values
        deactivateVideo();

        this.peerid = newPeerId;
        this.clientid = newClientId;
        this.isSmallCamera = false;
        if (newChatId != chatId) {
            this.chatId = newChatId;
            this.chatRoom = megaChatApi.getChatRoom(chatId);
        }
        if (call.getId() != callId) {
            this.call = megaChatApi.getChatCall(chatId);
        }

        updateAvatar(peerid);
        checkValues(peerid, clientid, session);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDestroy() {
        removeSurfaceView();
        this.avatarImage.setImageBitmap(null);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        if(listener != null){
            listener.setHeight(0);
            listener.setWidth(0);
        }
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        ((ChatCallActivity) context).remoteCameraClick(isSmallCamera);
    }

    private void removeSurfaceView() {
        if(surfaceView != null) {
            if (surfaceView.getParent() != null && surfaceView.getParent().getParent() != null) {
                logDebug("Removing suface view");
                ((ViewGroup) surfaceView.getParent()).removeView(surfaceView);
            }
            surfaceView.setVisibility(View.GONE);
        }

        logDebug("Removing remote video listener");
        if (isItMe(chatId, peerid, clientid)) {
            megaChatApi.removeChatVideoListener(chatId, -1, -1, this);
        } else {
            megaChatApi.removeChatVideoListener(chatId, peerid, clientid, this);
        }
        listener = null;
    }
}
