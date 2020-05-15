package mega.privacy.android.app.lollipop.megachat.calls;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.fragments.BaseFragment;
import mega.privacy.android.app.lollipop.listeners.GroupCallListener;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatSession;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.px2dp;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class FragmentPeerSelected extends BaseFragment implements View.OnClickListener {

    private GroupCallListener listener = null;
    private MegaChatRoom chatRoom;
    private long chatId;
    private long peerid;
    private long clientid;
    private View contentView;
    private RelativeLayout videoLayout;
    private RelativeLayout avatarLayout;
    private RoundedImageView avatarImage;
    private RelativeLayout muteLayout;

    public static FragmentPeerSelected newInstance(long chatId, long peerid, long clientid) {
        logDebug("Chat ID: " + chatId);

        FragmentPeerSelected f = new FragmentPeerSelected();
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

        this.chatRoom = megaChatApi.getChatRoom(chatId);
        if (chatRoom == null || megaChatApi.getChatCall(chatId) == null)
            return;

        if (peerid == INVALID_CALL_PEER_ID) {
            this.peerid = chatRoom.getPeerHandle(0);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (!isAdded()) {
            return null;
        }

        contentView = inflater.inflate(R.layout.fragment_camera_full_screen_big, container, false);
        videoLayout = contentView.findViewById(R.id.video_layout);
        videoLayout.setOnClickListener(this);
        videoLayout.setVisibility(View.GONE);
        avatarLayout = contentView.findViewById(R.id.avatar_layout);
        avatarLayout.setOnClickListener(this);
        avatarLayout.setVisibility(View.GONE);
        avatarImage = contentView.findViewById(R.id.avatar_image);
        avatarImage.setAlpha(1f);
        muteLayout = contentView.findViewById(R.id.mute_layout);

        setAvatarPeerSelected(peerid);
        checkValues(peerid, clientid);

        return contentView;
    }

    /**
     * Method to get the default avatar and image if it has one, in group calls.
     *
     * @param peerId The selected participant.
     */
    private void setAvatarPeerSelected(long peerId) {
        if (peerid != peerId)
            return;

        chatRoom = megaChatApi.getChatRoom(chatId);

        /*Default Avatar*/
        Bitmap defaultBitmap = getDefaultAvatarCall(chatRoom, peerid, false, true);
        avatarImage.setImageBitmap(defaultBitmap);

        /*Avatar*/
        Bitmap bitmap = getImageAvatarCall(context, chatRoom, peerid);
        if (bitmap != null) {
            avatarImage.setImageBitmap(bitmap);
        }
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
     * Method for updating the video status.
     *
     * @param peerId   Peer ID.
     * @param clientId Client ID.
     */
    public void checkValues(long peerId, long clientId) {
        MegaChatCall callChat = ((ChatCallActivity) context).getCall();

        if (callChat == null || peerId != peerid || clientId != clientid || isItMe(chatId, peerId, clientId))
            return;

        MegaChatSession session = ((ChatCallActivity) context).getSessionCall(peerId, clientId);

        if (session != null && session.hasVideo() && !callChat.isOnHold() && !((ChatCallActivity) context).isSessionOnHold()) {
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
        if (videoLayout == null || videoLayout.getVisibility() == View.VISIBLE)
            return;

        hideAvatar();

        if (listener == null) {
            videoLayout.removeAllViews();

            TextureView myTexture = new TextureView(context);
            myTexture.setLayoutParams(new RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
            myTexture.setAlpha(1.0f);
            myTexture.setRotation(0);

            listener = new GroupCallListener(context, myTexture, peerid, clientid, chatId, ((ChatCallActivity) context).getNumParticipants());
            if (isItMe(chatId, peerid, clientid)) {
                megaChatApi.addChatLocalVideoListener(chatId, listener);
            } else {
                megaChatApi.addChatRemoteVideoListener(chatId, peerid, clientid, listener);
            }

            videoLayout.addView(listener.getSurfaceView());

        } else {
            if (videoLayout.getChildCount() > 0 && !videoLayout.getChildAt(0).equals(listener.getSurfaceView())) {
                videoLayout.removeAllViews();
                if (listener.getSurfaceView().getParent() != null && listener.getSurfaceView().getParent().getParent() != null) {
                    ((ViewGroup) listener.getSurfaceView().getParent()).removeView(listener.getSurfaceView());
                }
                videoLayout.addView(listener.getSurfaceView());
            } else if (videoLayout.getChildCount() == 0) {
                if (listener.getSurfaceView() != null && listener.getSurfaceView().getParent().getParent() != null) {
                    ((ViewGroup) listener.getSurfaceView().getParent()).removeView(listener.getSurfaceView());
                }
                videoLayout.addView(listener.getSurfaceView());
            }

            listener.setHeight(0);
            listener.setWidth(0);
        }

        videoLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Method for deactivating the video.
     */
    private void deactivateVideo() {
        if (videoLayout == null || listener == null || videoLayout.getVisibility() == View.GONE) {
            return;
        }

        logDebug("Removing suface view");
        videoLayout.setVisibility(View.GONE);

        if (listener != null) {
            logDebug("Removing remote video listener");
            if (isItMe(chatId, peerid, clientid)) {
                megaChatApi.removeChatVideoListener(chatId, -1, -1, listener);
            } else {
                megaChatApi.removeChatVideoListener(chatId, peerid, clientid, listener);
            }
            if (videoLayout.getChildCount() > 0) {
                videoLayout.removeAllViews();
            }
            if (listener.getSurfaceView().getParent() != null && listener.getSurfaceView().getParent().getParent() != null) {
                ((ViewGroup) listener.getSurfaceView().getParent()).removeView(listener.getSurfaceView());
            }
            listener = null;
        }
    }

    /**
     * Method to show the avatar.
     */
    private void showAvatar() {
        if (avatarLayout == null)
            return;

        deactivateVideo();
        showOnHoldImage();
    }

    /**
     * Method to show the call on hold image.
     */
    public void showOnHoldImage() {
        avatarLayout.setVisibility(View.VISIBLE);
        MegaChatCall call = ((ChatCallActivity) context).getCall();
        MegaChatSession session = ((ChatCallActivity) context).getSessionCall(peerid, clientid);

        if ((call != null && call.isOnHold()) || (session != null && session.isOnHold())) {
            avatarImage.setAlpha(0.5f);
        } else {
            avatarImage.setAlpha(1f);
        }
    }

    /**
     * Method to hide the avatar.
     */
    private void hideAvatar() {
        if (avatarLayout.getVisibility() == View.GONE)
            return;

        avatarImage.setAlpha(1f);
        avatarLayout.setVisibility(View.GONE);
    }

    /**
     * Method to show the mute icon.
     */
    public void showMuteIcon(long peerid, long clientid) {
        if (peerid != this.peerid || clientid != clientid || muteLayout == null)
            return;

        MegaChatCall call = ((ChatCallActivity) context).getCall();
        MegaChatSession session = ((ChatCallActivity) context).getSessionCall(peerid, clientid);

        boolean isShouldShown = call != null && session != null && !call.isOnHold() && !session.isOnHold() && !session.hasAudio();

        if (isShouldShown) {

            RelativeLayout.LayoutParams paramsMicroSurface = new RelativeLayout.LayoutParams(muteLayout.getLayoutParams());
            paramsMicroSurface.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            paramsMicroSurface.addRule(RelativeLayout.ALIGN_PARENT_TOP);

            paramsMicroSurface.setMargins(0, px2dp(16, outMetrics), px2dp(16, outMetrics), 0);
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
    public void changePeerSelected(long newChatId, long callId, long newPeerId, long newClientId) {
        if ((newPeerId == peerid && newClientId == clientid) || ((ChatCallActivity) context).getCall().getId() != callId)
            return;

        deactivateVideo();
        avatarImage.setImageBitmap(null);

        this.peerid = newPeerId;
        this.clientid = newClientId;
        if (newChatId != chatId) {
            this.chatId = newChatId;
            this.chatRoom = megaChatApi.getChatRoom(chatId);
        }

        setAvatarPeerSelected(peerid);
        checkValues(newPeerId, newClientId);
    }

    /**
     * Method to destroy the surfaceView.
     */
    public void removeSurfaceView() {
        videoLayout.setVisibility(View.GONE);

        if (listener != null) {
            logDebug("Removing remote video listener");
            if (isItMe(chatId, peerid, clientid)) {
                megaChatApi.removeChatVideoListener(chatId, -1, -1, listener);
            } else {
                megaChatApi.removeChatVideoListener(chatId, peerid, clientid, listener);
            }

            if (videoLayout.getChildCount() != 0) {
                videoLayout.removeAllViews();
                videoLayout.removeAllViewsInLayout();
            }
            if (listener.getSurfaceView().getParent() != null && listener.getSurfaceView().getParent().getParent() != null) {
                ((ViewGroup) listener.getSurfaceView().getParent()).removeView(listener.getSurfaceView());
            }
            listener = null;
        }
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
        avatarImage.setImageBitmap(null);
        super.onDestroy();
    }
}
