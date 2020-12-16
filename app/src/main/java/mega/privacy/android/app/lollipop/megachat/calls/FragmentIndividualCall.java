package mega.privacy.android.app.lollipop.megachat.calls;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.fragments.BaseFragment;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatSession;

import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class FragmentIndividualCall extends BaseFragment implements View.OnClickListener {

    private IndividualCallListener listener = null;
    private MegaChatRoom chatRoom;
    private long chatId;
    private long peerid;
    private long clientid;

    private boolean isSmallCamera;

    private View contentView;
    private SurfaceView surfaceView = null;
    private RelativeLayout avatarLayout;
    private RoundedImageView avatarImage;

    private RelativeLayout muteLayout;
    private RelativeLayout videoLayout;
    private ImageView avatarImageOnHold;

    public static FragmentIndividualCall newInstance(long chatId, long peerid, long clientid, boolean isSmallCamera) {
        logDebug("Chat ID: " + chatId);

        FragmentIndividualCall f = new FragmentIndividualCall();
        Bundle args = new Bundle();
        args.putLong(CHAT_ID, chatId);
        args.putLong(PEER_ID, peerid);
        args.putLong(CLIENT_ID, clientid);
        args.putBoolean(TYPE_CAMERA, isSmallCamera);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle args = getArguments();
        this.chatId = args.getLong(CHAT_ID, MEGACHAT_INVALID_HANDLE);
        this.peerid = args.getLong(PEER_ID, MEGACHAT_INVALID_HANDLE);
        this.clientid = args.getLong(CLIENT_ID, MEGACHAT_INVALID_HANDLE);
        this.isSmallCamera = args.getBoolean(TYPE_CAMERA, false);

        this.chatRoom = megaChatApi.getChatRoom(chatId);
        if (chatRoom == null || megaChatApi.getChatCall(chatId) == null)
            return;

        if (peerid == MEGACHAT_INVALID_HANDLE) {
            this.peerid = chatRoom.getPeerHandle(0);
        }

        logDebug("Chat ID: " + chatId);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (!isAdded())
            return null;

        if (isSmallCamera) {
            contentView = inflater.inflate(R.layout.fragment_local_camera_call, container, false);
            videoLayout = contentView.findViewById(R.id.video_layout);
            surfaceView = contentView.findViewById(R.id.video);
            surfaceView.setVisibility(View.GONE);
            videoLayout.setVisibility(View.GONE);
            avatarLayout = contentView.findViewById(R.id.avatar_layout);
            avatarLayout.setVisibility(View.GONE);
            avatarImage = contentView.findViewById(R.id.avatar_image);
            avatarImage.setAlpha(1f);
            muteLayout = contentView.findViewById(R.id.mute_layout);
            muteLayout.setVisibility(View.GONE);
        } else {
            contentView = inflater.inflate(R.layout.full_screen_individual_call, container, false);
            surfaceView = contentView.findViewById(R.id.video);
            surfaceView.setOnClickListener(this);
            surfaceView.setVisibility(View.GONE);
            avatarLayout = contentView.findViewById(R.id.avatar_layout);
            avatarLayout.setOnClickListener(this);
            avatarLayout.setVisibility(View.GONE);
            avatarImage = contentView.findViewById(R.id.avatar_image);
            avatarImage.setAlpha(1f);

            avatarImageOnHold = contentView.findViewById(R.id.avatar_image_on_hold);
            avatarImageOnHold.setVisibility(View.GONE);
        }

        updateAvatar(peerid);
        checkValues(peerid, clientid);

        return contentView;
    }

    /**
     * Method for loading the corresponding avatar.
     *
     * @param newPeerId Peer ID.
     */
    private void updateAvatar(long newPeerId) {
        if (peerid != newPeerId || avatarImage == null) {
            logError("Error updating the avatar");
            return;
        }

        chatRoom = megaChatApi.getChatRoom(chatId);

        /*Avatar*/
        Bitmap bitmap = getImageAvatarCall(chatRoom, peerid);
        avatarImage.setImageBitmap(bitmap != null ? bitmap :
                getDefaultAvatarCall(context, chatRoom, peerid));
    }

    /**
     * Method for updating the video status.
     *
     * @param peerId   Peer ID.
     * @param clientId Client ID.
     */
    public void checkValues(long peerId, long clientId) {
        MegaChatCall callChat = ((ChatCallActivity) context).getCall();
        if (callChat == null || peerId != peerid || clientId != clientid) {
            logError("Error checking values");
            return;
        }

        if (isItMe(chatId, peerId, clientId)) {
            if (callChat.hasLocalVideo() && !callChat.isOnHold() && !isSessionOnHold(chatId)) {
                activateVideo();
                return;
            }

            showAvatar();
            return;
        }

        MegaChatSession session = ((ChatCallActivity) context).getSessionCall(peerId, clientId);

        if (session != null && session.hasVideo() && !callChat.isOnHold() && !isSessionOnHold(chatId)) {
            activateVideo();
            return;
        }

        showAvatar();
        showMuteIcon(peerid, clientid);
    }

    /**
     * Method for activating the video.
     */
    private void activateVideo() {
        if (surfaceView == null || surfaceView.getVisibility() == View.VISIBLE) {
            logError("Error activating video");
            return;
        }

        hideAvatar();

        if (listener == null) {
            listener = new IndividualCallListener(context, surfaceView, peerid, clientid, chatId, outMetrics, isSmallCamera);
            if (isItMe(chatId, peerid, clientid)) {
                megaChatApi.addChatLocalVideoListener(chatId, listener);
            } else {
                megaChatApi.addChatRemoteVideoListener(chatId, peerid, clientid, listener);
            }
        } else {
            listener.setHeight(0);
            listener.setWidth(0);
        }

        if (isSmallCamera && videoLayout != null) {
            videoLayout.setVisibility(View.VISIBLE);
        }
        surfaceView.setVisibility(View.VISIBLE);
    }

    /**
     * Method for deactivating the video.
     */
    private void deactivateVideo() {
        if (surfaceView == null || listener == null || surfaceView.getVisibility() == View.GONE) {
            logError("Error deactivating video");
            return;
        }

        logDebug("Removing suface view");
        surfaceView.setVisibility(View.GONE);
        if (isSmallCamera && videoLayout != null) {
            videoLayout.setVisibility(View.GONE);
        }

        removeChatVideoListener();

        if (isSmallCamera) {
            checkIndividualAudioCall();
        }
    }

    /**
     * Method for removing the video listener.
     */
    private void removeChatVideoListener() {
        if (listener == null)
            return;

        logDebug("Removing remote video listener");
        if (isItMe(chatId, peerid, clientid)) {
            megaChatApi.removeChatVideoListener(chatId, MEGACHAT_INVALID_HANDLE, MEGACHAT_INVALID_HANDLE, listener);
        } else {
            megaChatApi.removeChatVideoListener(chatId, peerid, clientid, listener);
        }
        listener = null;
    }

    /**
     * Method for updating the muted call bar on individual calls.
     */
    public void checkIndividualAudioCall() {
        if (avatarLayout == null || !isSmallCamera) {
            logError("Error checking if is only audio call");
            return;
        }

        if (((ChatCallActivity) context).isIndividualAudioCall()) {
            hideAvatar();
        } else {
            avatarLayout.setVisibility(View.VISIBLE);
        }

        showMuteIcon(peerid, clientid);
    }

    /**
     * Method to add the bitmap to the avatar.
     */
    public void setAvatar(long peerId, Bitmap bitmap) {
        if (!isItMe(chatId, peerid, clientid) && peerId == peerid && bitmap != null && avatarImage != null) {
            avatarImage.setImageBitmap(bitmap);
        }
    }

    /**
     * Method to show the avatar.
     */
    private void showAvatar() {
        deactivateVideo();
        if (isSmallCamera) {
            checkIndividualAudioCall();
            return;
        }
        showOnHoldImage();
    }

    /**
     * Method to show the call on hold image.
     */
    public void showOnHoldImage() {
        if (avatarLayout == null || isSmallCamera) {
            logError("Error showing the avatar");
            return;
        }

        avatarLayout.setVisibility(View.VISIBLE);
        MegaChatCall call = ((ChatCallActivity) context).getCall();
        MegaChatSession session = ((ChatCallActivity) context).getSessionCall(peerid, clientid);

        if ((call != null && call.isOnHold()) || (session != null && session.isOnHold())) {
            avatarImageOnHold.setVisibility(View.VISIBLE);
            avatarImage.setAlpha(0.5f);
        } else {
            avatarImageOnHold.setVisibility(View.GONE);
            avatarImage.setAlpha(1f);
        }
    }

    /**
     * Method to hide the avatar.
     */
    private void hideAvatar() {
        if (avatarLayout == null || avatarLayout.getVisibility() == View.GONE) {
            logError("Error hidding the avatar");
            return;
        }

        if (!isSmallCamera) {
            avatarImageOnHold.setVisibility(View.GONE);
            avatarImage.setAlpha(1f);
        }

        avatarLayout.setVisibility(View.GONE);
    }

    /**
     * Method to show the mute icon.
     */
    public void showMuteIcon(long peerid, long clientid) {
        if (!isSmallCamera || peerid != this.peerid || clientid != clientid || muteLayout == null)
            return;

        MegaChatCall call = ((ChatCallActivity) context).getCall();

        boolean isShouldShown = call != null && !call.isOnHold() &&
                !isSessionOnHold(call.getChatid()) && !call.hasLocalAudio() &&
                (surfaceView.getVisibility() == View.VISIBLE || avatarLayout.getVisibility() == View.VISIBLE);

        if (isShouldShown) {
            int marginTop;
            RelativeLayout.LayoutParams paramsMicroSurface = new RelativeLayout.LayoutParams(muteLayout.getLayoutParams());
            if (surfaceView.getVisibility() == View.VISIBLE) {
                paramsMicroSurface.addRule(RelativeLayout.ALIGN_RIGHT, videoLayout.getId());
                paramsMicroSurface.addRule(RelativeLayout.ALIGN_TOP, videoLayout.getId());
                marginTop = dp2px(12, outMetrics);
            } else {
                paramsMicroSurface.addRule(RelativeLayout.ALIGN_RIGHT, avatarLayout.getId());
                paramsMicroSurface.addRule(RelativeLayout.ALIGN_TOP, avatarLayout.getId());
                marginTop = dp2px(3, outMetrics);

            }
            paramsMicroSurface.setMargins(0, marginTop, dp2px(4, outMetrics), 0);
            muteLayout.setLayoutParams(paramsMicroSurface);
            muteLayout.setVisibility(View.VISIBLE);

            return;
        }

        muteLayout.setVisibility(View.GONE);
    }

    /**
     * Method for changing the user being displayed.
     *
     * @param newChatId   Chat ID.
     * @param callId      Call ID.
     * @param newPeerId   Peer ID.
     * @param newClientId Client ID.
     */
    public void changeUser(long newChatId, long callId, long newPeerId, long newClientId) {

        if (isSmallCamera || (newPeerId == peerid && newClientId == clientid) || ((ChatCallActivity) context).getCall().getId() != callId) {
            logError("Error changing the user");
            return;
        }

        deactivateVideo();

        this.peerid = newPeerId;
        this.clientid = newClientId;
        this.isSmallCamera = false;
        if (newChatId != chatId) {
            this.chatId = newChatId;
            this.chatRoom = megaChatApi.getChatRoom(chatId);
        }

        updateAvatar(peerid);
        checkValues(peerid, clientid);
    }

    /**
     * Method to destroy the surfaceView.
     */
    private void removeSurfaceView() {
        if (surfaceView != null) {
            if (surfaceView.getParent() != null && surfaceView.getParent().getParent() != null) {
                logDebug("Removing suface view");
                ((ViewGroup) surfaceView.getParent()).removeView(surfaceView);
            }

            surfaceView.setVisibility(View.GONE);
            if (isSmallCamera) {
                videoLayout.setVisibility(View.GONE);
            }
        }

        removeChatVideoListener();
    }

    @Override
    public void onClick(View v) {
        ((ChatCallActivity) context).remoteCameraClick();
    }

    @Override
    public void onResume() {
        if (listener != null) {
            listener.setHeight(0);
            listener.setWidth(0);
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        removeSurfaceView();
        if (avatarImage != null) {
            avatarImage.setImageBitmap(null);
        }
        super.onDestroy();
    }
}
