package mega.privacy.android.app.lollipop.megachat.calls;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.ArrayList;
import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView;
import mega.privacy.android.app.components.OnSwipeTouchListener;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.fcm.IncomingCallService;
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;

import mega.privacy.android.app.lollipop.controllers.ChatController;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatSession;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.IncomingCallNotification.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.VideoCaptureUtils.*;
import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class ChatCallActivity extends BaseActivity implements MegaChatRequestListenerInterface, MegaRequestListenerInterface, View.OnClickListener, KeyEvent.Callback {

    final private static int MIN_PEERS_LIST = 7;
    final private static int ARROW_ANIMATION = 250;
    final private static int INFO_ANIMATION = 4000;
    final private static int MOVE_ANIMATION = 500;
    final private static int ALPHA_ANIMATION = 600;
    final private static int ALPHA_ARROW_ANIMATION = 1000;
    final private static int DURATION_TOOLBAR_ANIMATION = 500;
    final private static int NECESSARY_CHANGE_OF_SIZES = 3;
    final private static int TYPE_JOIN = 1;
    final private static int TYPE_LEFT = -1;
    private final static int TITLE_TOOLBAR = 250;
    private final static String SMALL_FRAGMENT = "cameraFragmentSmall";
    private final static String FULLSCREEN_FRAGMENT = "cameraFragmentFullScreen";
    private final static String PEERSELECTED_FRAGMENT = "cameraFragmentPeerSelected";
    private static final int TIMEOUT = 5000;
    private long chatId;
    private MegaChatRoom chat;
    private MegaChatCall callChat;
    private long chatIdToHang = MEGACHAT_INVALID_HANDLE;
    private boolean answerWithVideo = false;
    private float widthScreenPX;

    private InfoPeerGroupCall peerSelected;
    private ArrayList<InfoPeerGroupCall> peersOnCall = new ArrayList<>();

    private Animation shake;

    private Toolbar tB;
    private ActionBar aB;
    private MenuItem cameraSwapMenuItem;
    private EmojiTextView titleToolbar;
    private TextView subtitleToobar;
    private Chronometer callInProgressChrono;
    private RelativeLayout mutateContactCallLayout;
    private EmojiTextView mutateCallText;
    private RelativeLayout mutateOwnCallLayout;
    private RelativeLayout callOnHoldLayout;
    private EmojiTextView callOnHoldText;
    private LinearLayout linearParticipants;
    private TextView participantText;
    private EmojiTextView infoUsersBar;
    private RelativeLayout reconnectingLayout;
    private RelativeLayout anotherCallLayout;
    private RelativeLayout anotherCallLayoutLayer;
    private EmojiTextView anotherCallTitle;
    private TextView anotherCallSubtitle;
    private TextView reconnectingText;

    private RelativeLayout smallElementsIndividualCallLayout;
    private RelativeLayout bigElementsIndividualCallLayout;
    private RelativeLayout bigElementsGroupCallLayout;

    private RelativeLayout recyclerViewLayout;
    private CustomizedGridCallRecyclerView recyclerView;
    private GroupCallAdapter adapterGrid;

    private RelativeLayout bigRecyclerViewLayout;
    private RecyclerView bigRecyclerView;
    private LinearLayoutManager layoutManager;
    private GroupCallAdapter adapterList;

    private RelativeLayout fragmentContainer;

    private FloatingActionButton onHoldFAB;
    private FloatingActionButton videoFAB;
    private FloatingActionButton microFAB;
    private FloatingActionButton rejectFAB;
    private FloatingActionButton hangFAB;
    private FloatingActionButton speakerFAB;
    private FloatingActionButton answerCallFAB;

    private LinearLayout linearFAB;
    private RelativeLayout relativeCall;
    private LinearLayout linearArrowCall;
    private ImageView firstArrowCall;
    private ImageView secondArrowCall;
    private ImageView thirdArrowCall;
    private ImageView fourArrowCall;
    private RelativeLayout relativeVideo;
    private LinearLayout linearArrowVideo;
    private ImageView firstArrowVideo;
    private ImageView secondArrowVideo;
    private ImageView thirdArrowVideo;
    private ImageView fourArrowVideo;
    private Handler handlerArrow1, handlerArrow2, handlerArrow3, handlerArrow4, handlerArrow5, handlerArrow6;

    private boolean isManualMode = false;
    private int statusBarHeight = 0;
    private int totalVideosAllowed;
    private boolean inTemporaryState = false;
    private FragmentIndividualCall cameraFragmentSmall;
    private ViewGroup smallCameraLayout;
    private FrameLayout smallFragmentContainer;

    private FragmentIndividualCall cameraFragmentFullScreen;
    private ViewGroup fullScreenCameraLayout;
    private FrameLayout fullScreenFragmentContainer;

    private FragmentPeerSelected cameraFragmentPeerSelected;
    private ViewGroup peerSelectedCameraLayout;
    private FrameLayout peerSelectedFragmentContainer;
    private CountDownTimer countDownTimer;
    private ChatController chatC;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        logDebug("onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.call_action, menu);
        cameraSwapMenuItem = menu.findItem(R.id.cab_menu_camera_swap);
        cameraSwapMenuItem.setEnabled(true);
        cameraSwapMenuItem.setIcon(mutateIcon(this, R.drawable.ic_camera_swap, R.color.background_chat));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        cameraSwapMenuItem.setVisible(isNecessaryToShowSwapCameraOption());
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Method for determining whether to display the camera switching icon.
     *
     * @return True, if it is. False, if not.
     */
    private boolean isNecessaryToShowSwapCameraOption() {
        if (callChat == null)
            return false;

        int callStatus = callChat.getStatus();
        return callChat.getStatus() != MegaChatCall.CALL_STATUS_RING_IN &&
                callStatus >= MegaChatCall.CALL_STATUS_HAS_LOCAL_STREAM &&
                (callStatus <= MegaChatCall.CALL_STATUS_IN_PROGRESS || callStatus == MegaChatCall.CALL_STATUS_RECONNECTING) &&
                callChat.hasLocalVideo() &&
                !callChat.isOnHold();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        logDebug("onOptionsItemSelected");
        app.sendSignalPresenceActivity();
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            case R.id.cab_menu_camera_swap:{
                swapCamera(new ChatChangeVideoStreamListener(getApplicationContext()));
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Check the audio and video values in local and remote.
     *
     * @param session The session set up with a specific user.
     */
    public void updateAVFlags(MegaChatSession session) {
        if (!chat.isGroup()) {
            updateLocalAV();
            updateRemoteAV(session);
        }

        updateSubTitle();
    }

    /**
     * Method for painting the initial UI.
     *
     * @param chatId Identifier of the chat room to which the call belongs.
     */
    private void initialUI(long chatId) {
        logDebug("Initializing call UI");
        //Contact's avatar
        if (chatId == MEGACHAT_INVALID_HANDLE || megaChatApi == null) return;

        chat = megaChatApi.getChatRoom(chatId);
        callChat = megaChatApi.getChatCall(chatId);
        if (callChat == null) {
            finishActivity();
            return;
        }

        clearIncomingCallNotification(chatId);

        titleToolbar.setText(getTitleChat(chat));
        updateSubTitle();

        if (chat.isGroup()) {
            smallElementsIndividualCallLayout.setVisibility(View.GONE);
            bigElementsIndividualCallLayout.setVisibility(View.GONE);
            bigElementsGroupCallLayout.setVisibility(View.VISIBLE);
        } else {
            smallElementsIndividualCallLayout.setVisibility(View.VISIBLE);
            bigElementsIndividualCallLayout.setVisibility(View.VISIBLE);
            bigElementsGroupCallLayout.setVisibility(View.GONE);
            bigRecyclerView.setVisibility(View.GONE);
            bigRecyclerViewLayout.setVisibility(View.GONE);
        }

        checkInitialCallStatus();
        showInitialFABConfiguration();
        checkCallOnHold();
        updateAnotherCallOnHoldBar(callChat.getChatid());
    }

    /**
     * Method for creating the small camera in individual calls.
     */
    private void createSmallFragment() {
        if (getCall() == null || callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT || callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN || cameraFragmentSmall != null)
            return;

        cameraFragmentSmall = FragmentIndividualCall.newInstance(chatId, megaChatApi.getMyUserHandle(), megaChatApi.getMyClientidHandle(chatId), true);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.small_camera_fragment, cameraFragmentSmall, SMALL_FRAGMENT);
        ft.commitNowAllowingStateLoss();

        smallCameraLayout.setVisibility(View.VISIBLE);
        smallFragmentContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Method for removing the small camera in individual calls
     */
    private void removeSmallFragment() {
        if (cameraFragmentSmall == null)
            return;

        cameraFragmentSmall.onDestroy();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.remove(cameraFragmentSmall);
        cameraFragmentSmall = null;
    }

    /**
     * Method for creating the full screen camera in individual calls.
     */
    private void createFullScreenFragment() {

        if (getCall() == null || cameraFragmentFullScreen != null)
            return;

        long peerId;
        long clientId;
        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
            peerId = megaChatApi.getMyUserHandle();
            clientId = megaChatApi.getMyClientidHandle(chatId);
        } else {
            peerId = callChat.getSessionsPeerid().get(0);
            clientId = callChat.getSessionsClientid().get(0);
        }
        cameraFragmentFullScreen = FragmentIndividualCall.newInstance(chatId, peerId, clientId, false);

        FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
        ftFS.replace(R.id.full_screen_fragment, cameraFragmentFullScreen, FULLSCREEN_FRAGMENT);
        ftFS.commitNowAllowingStateLoss();

        fullScreenCameraLayout.setVisibility(View.VISIBLE);
        fullScreenFragmentContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Method for removing the full screen camera in individual calls
     */
    private void removeFullScreenFragment() {
        if (cameraFragmentFullScreen == null)
            return;

        cameraFragmentFullScreen.onDestroy();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.remove(cameraFragmentFullScreen);
        cameraFragmentFullScreen = null;
    }

    /**
     * Method for creating the selected participant's fragment.
     */
    private void createPeerSelectedFragment() {
        if (getCall() == null || !chat.isGroup() || cameraFragmentPeerSelected != null || peerSelected == null)
            return;

        cameraFragmentPeerSelected = FragmentPeerSelected.newInstance(chatId, peerSelected.getPeerId(), peerSelected.getClientId());
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_peer_selected, cameraFragmentPeerSelected, PEERSELECTED_FRAGMENT);
        ft.commitNowAllowingStateLoss();

        peerSelectedFragmentContainer.setVisibility(View.VISIBLE);
        peerSelectedCameraLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Method for removing the selected participant's fragment.
     */
    private void removePeerSelectedFragment() {
        if (cameraFragmentPeerSelected == null)
            return;

        cameraFragmentPeerSelected.onDestroy();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.remove(cameraFragmentPeerSelected);
        cameraFragmentPeerSelected = null;
        peerSelectedCameraLayout.setVisibility(View.GONE);
    }

    /**
     * Check the initial state of the call and update UI.
     */
    private void checkInitialCallStatus() {
        if (chatId == MEGACHAT_INVALID_HANDLE || megaChatApi == null || getCall() == null)
            return;

        chat = megaChatApi.getChatRoom(chatId);
        int callStatus = callChat.getStatus();
        createSmallFragment();
        createFullScreenFragment();

        if ((callStatus >= MegaChatCall.CALL_STATUS_REQUEST_SENT &&
                callStatus <= MegaChatCall.CALL_STATUS_IN_PROGRESS) ||
                (callStatus >= MegaChatCall.CALL_STATUS_USER_NO_PRESENT &&
                        callStatus <= MegaChatCall.CALL_STATUS_RECONNECTING)) {

            MegaApplication.setCallLayoutStatus(chatId, true);
        }

        if (callStatus == MegaChatCall.CALL_STATUS_RING_IN) {
            displayLinearFAB(true);
            checkOutgoingOrIncomingCall();
            return;
        }

        displayLinearFAB(false);
        if (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
            checkOutgoingOrIncomingCall();
        } else if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS || callStatus == MegaChatCall.CALL_STATUS_RECONNECTING || callStatus == MegaChatCall.CALL_STATUS_JOINING) {
            checkCurrentParticipants();
            updateSubTitle(); }
        if ((callStatus >= MegaChatCall.CALL_STATUS_REQUEST_SENT && callStatus <= MegaChatCall.CALL_STATUS_IN_PROGRESS) || callStatus == MegaChatCall.CALL_STATUS_RECONNECTING) {
            if (callStatus == MegaChatCall.CALL_STATUS_RECONNECTING) {
                showReconnecting();
            }
        }

        updateAVFlags(getSessionIndividualCall(callChat));
        updateLocalSpeakerStatus();
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        logDebug("Action: " + getIntent().getAction());
        if (extras == null)
            return;

        long newChatId = extras.getLong(CHAT_ID, MEGACHAT_INVALID_HANDLE);
        if (megaChatApi == null) {
            return;
        }

        if (chatId != MEGACHAT_INVALID_HANDLE && chatId == newChatId) {
            logDebug("Same call");
            chat = megaChatApi.getChatRoom(chatId);
            checkInitialCallStatus();

        } else if (newChatId != MEGACHAT_INVALID_HANDLE) {
            logDebug("Different call");
            refreshUI();
            chatId = newChatId;
            initialUI(chatId);
        }
    }

    /**
     * Refresh the UI, when a different calls starts.
     */
    private void refreshUI() {
        removeSmallFragment();
        removeFullScreenFragment();
        removePeerSelectedFragment();
        anotherCallLayout.setVisibility(View.GONE);
        callOnHoldLayout.setVisibility(View.GONE);
        mutateOwnCallLayout.setVisibility(View.GONE);
        mutateContactCallLayout.setVisibility(View.GONE);
        reconnectingLayout.setVisibility(View.GONE);
        mutateContactCallLayout.setVisibility(View.GONE);
        infoUsersBar.setVisibility(View.GONE);
    }

    /**
     * Method for controlling changes in the call.
     */
    private BroadcastReceiver chatCallUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;

            long chatIdReceived = intent.getLongExtra(UPDATE_CHAT_CALL_ID, MEGACHAT_INVALID_HANDLE);
            if (chatIdReceived != getCurrentChatid()) {
                logWarning("Call in different chat");
                long callIdReceived = intent.getLongExtra(UPDATE_CALL_ID, MEGACHAT_INVALID_HANDLE);
                if (callChat != null && callIdReceived != callChat.getId() && (intent.getAction().equals(ACTION_CALL_STATUS_UPDATE) || intent.getAction().equals(ACTION_CHANGE_CALL_ON_HOLD))) {
                    checkAnotherCallOnHold();
                }
                return;
            }

            long callIdReceived  = intent.getLongExtra(UPDATE_CALL_ID, MEGACHAT_INVALID_HANDLE);
            if (callIdReceived == MEGACHAT_INVALID_HANDLE) {
                logWarning("Call recovered is incorrect");
                return;
            }

            updateCall(callIdReceived);
            if (intent.getAction().equals(ACTION_CALL_STATUS_UPDATE)) {
                int callStatus = intent.getIntExtra(UPDATE_CALL_STATUS, INVALID_CALL_STATUS);
                logDebug("The call status is "+callStatusToString(callStatus)+".  Call id "+callChat);
                if (callStatus != INVALID_CALL_STATUS) {
                    switch (callStatus) {
                        case MegaChatCall.CALL_STATUS_HAS_LOCAL_STREAM:
                            updateLocalAV();
                            break;
                        case MegaChatCall.CALL_STATUS_IN_PROGRESS:
                            checkInProgressCall();
                            break;
                        case MegaChatCall.CALL_STATUS_JOINING:
                            break;
                        case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:
                        case MegaChatCall.CALL_STATUS_DESTROYED:
                            checkTerminatingCall();
                            break;
                        case MegaChatCall.CALL_STATUS_USER_NO_PRESENT:
                            checkUserNoPresentInCall();
                            break;
                        case MegaChatCall.CALL_STATUS_RECONNECTING:
                            checkReconnectingCall();
                            break;
                    }
                }
            }

            if (intent.getAction().equals(ACTION_CHANGE_CALL_ON_HOLD)) {
                checkCallOnHold();
            }

            if (intent.getAction().equals(ACTION_CHANGE_LOCAL_AVFLAGS)) {
                updateLocalAV();
            }

            if (intent.getAction().equals(ACTION_CHANGE_COMPOSITION)) {
                int typeChange = intent.getIntExtra(TYPE_CHANGE_COMPOSITION, 0);
                long peerIdReceived = intent.getLongExtra(UPDATE_PEER_ID, MEGACHAT_INVALID_HANDLE);
                long clientIdReceived = intent.getLongExtra(UPDATE_CLIENT_ID, MEGACHAT_INVALID_HANDLE);
                checkCompositionChanges(typeChange, peerIdReceived, clientIdReceived);
            }
        }
    };

    /**
     * Method for controlling changes in sessions.
     */
    private BroadcastReceiver chatSessionUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;

            long chatIdReceived = intent.getLongExtra(UPDATE_CHAT_CALL_ID, MEGACHAT_INVALID_HANDLE);
            if (chatIdReceived != getCurrentChatid()) {
                logWarning("Call in different chat");
                long callIdReceived = intent.getLongExtra(UPDATE_CALL_ID, MEGACHAT_INVALID_HANDLE);
                if (callChat != null && callIdReceived != callChat.getId() && (intent.getAction().equals(ACTION_SESSION_STATUS_UPDATE) || intent.getAction().equals(ACTION_CHANGE_SESSION_ON_HOLD))) {
                    checkAnotherCallOnHold();
                }
                return;
            }

            long callId = intent.getLongExtra(UPDATE_CALL_ID, MEGACHAT_INVALID_HANDLE);
            if (callId == MEGACHAT_INVALID_HANDLE) {
                logWarning("Call recovered is incorrect");
                return;
            }

            updateCall(callId);
            if(!intent.getAction().equals(ACTION_UPDATE_CALL)){

                long peerId = intent.getLongExtra(UPDATE_PEER_ID, MEGACHAT_INVALID_HANDLE);
                long clientId = intent.getLongExtra(UPDATE_CLIENT_ID, MEGACHAT_INVALID_HANDLE);
                MegaChatSession session = getSessionCall(peerId, clientId);

                if (intent.getAction().equals(ACTION_CHANGE_REMOTE_AVFLAGS)) {
                    updateRemoteAV(session);
                }

                if (intent.getAction().equals(ACTION_CHANGE_AUDIO_LEVEL)) {
                    checkAudioLevel(session);
                }

                if (intent.getAction().equals(ACTION_CHANGE_NETWORK_QUALITY)) {
                    checkNetworkQuality(session);
                }

                if (intent.getAction().equals(ACTION_CHANGE_SESSION_ON_HOLD)) {
                    logDebug("The session on hold change");
                    if(chat.isGroup()){
                        checkSessionOnHold(peerId, clientId);
                    }else{
                        checkCallOnHold();
                    }
                }

                if (intent.getAction().equals(ACTION_SESSION_STATUS_UPDATE)) {

                    int sessionStatus = intent.getIntExtra(UPDATE_SESSION_STATUS, INVALID_CALL_STATUS);
                    logDebug("The session status changed to "+sessionStatusToString(sessionStatus));

                    if (sessionStatus == MegaChatSession.SESSION_STATUS_DESTROYED) {
                        int termCode = intent.getIntExtra(UPDATE_SESSION_TERM_CODE, -1);
                        if (termCode == MegaChatCall.TERM_CODE_ERROR) {
                            checkReconnectingCall();
                            return;
                        }

                        if (termCode == MegaChatCall.TERM_CODE_USER_HANGUP) {
                            checkHangCall(callId);
                        }
                    }

                    if (sessionStatus == MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
                        if(cameraFragmentFullScreen != null){
                            cameraFragmentFullScreen.changeUser(chatId, callId, peerId, clientId);
                        }
                        hideReconnecting();
                        updateAVFlags(session);
                        updateSubtitleNumberOfVideos();
                    }
                }
            }
        }
    };

    private BroadcastReceiver proximitySensorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null)
                return;

            boolean isNear = intent.getBooleanExtra(UPDATE_PROXIMITY_SENSOR_STATUS, false);
            boolean realStatus = MegaApplication.getVideoStatus(chatId);
            if (!realStatus) {
                inTemporaryState = false;
            } else if (isNear) {
                inTemporaryState = true;
                megaChatApi.disableVideo(chatId, ChatCallActivity.this);
            } else {
                inTemporaryState = false;
                megaChatApi.enableVideo(chatId, ChatCallActivity.this);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cancelIncomingCallNotification(this);
        setContentView(R.layout.activity_calls_chat);
        app.setShowPinScreen(true);
        chatC = new ChatController(this);
        statusBarHeight = getStatusBarHeight();

        widthScreenPX = getOutMetrics().widthPixels;

        if (megaChatApi != null) {
            megaChatApi.retryPendingConnections(false, null);
        }

        if (megaApi == null || megaApi.getRootNode() == null || megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR) {
            logWarning("Refresh session - sdk || karere");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        fragmentContainer = findViewById(R.id.file_info_fragment_container);

        tB = findViewById(R.id.call_toolbar);
        if (tB == null) {
            logWarning("Toolbar is Null");
            return;
        }
        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);
        aB.setTitle(null);
        aB.setSubtitle(null);

        titleToolbar = tB.findViewById(R.id.title_toolbar);
        titleToolbar.setText(" ");
        titleToolbar.setMaxWidthEmojis(px2dp(TITLE_TOOLBAR, getOutMetrics()));

        subtitleToobar = tB.findViewById(R.id.subtitle_toolbar);
        callInProgressChrono = tB.findViewById(R.id.simple_chronometer);
        linearParticipants = tB.findViewById(R.id.ll_participants);
        participantText = tB.findViewById(R.id.participants_text);
        linearParticipants.setVisibility(View.GONE);
        totalVideosAllowed = megaChatApi.getMaxVideoCallParticipants();
        callOnHoldLayout = findViewById(R.id.call_on_hold_layout);
        callOnHoldText = findViewById(R.id.call_on_hold_text);
        callOnHoldLayout.setVisibility(View.GONE);
        mutateOwnCallLayout = findViewById(R.id.mutate_own_call);
        mutateOwnCallLayout.setVisibility(View.GONE);
        mutateContactCallLayout = findViewById(R.id.mutate_contact_call);
        mutateContactCallLayout.setVisibility(View.GONE);
        mutateCallText = findViewById(R.id.text_mutate_contact_call);
        smallElementsIndividualCallLayout = findViewById(R.id.small_elements_individual_call);
        smallElementsIndividualCallLayout.setVisibility(View.GONE);
        bigElementsIndividualCallLayout = findViewById(R.id.big_elements_individual_call);
        bigElementsIndividualCallLayout.setVisibility(View.GONE);
        bigElementsIndividualCallLayout.setOnClickListener(this);
        linearFAB = findViewById(R.id.linear_buttons);
        displayLinearFAB(false);
        infoUsersBar = findViewById(R.id.info_users_bar);
        infoUsersBar.setVisibility(View.GONE);
        reconnectingLayout = findViewById(R.id.reconnecting_layout);
        reconnectingLayout.setVisibility(View.GONE);
        reconnectingText = findViewById(R.id.reconnecting_text);

        anotherCallLayout = findViewById(R.id.another_call_layout);
        anotherCallLayout.setOnClickListener(this);
        anotherCallLayoutLayer = findViewById(R.id.another_call_layout_layer);
        anotherCallTitle = findViewById(R.id.another_call_title);
        anotherCallSubtitle = findViewById(R.id.another_call_subtitle);
        anotherCallLayout.setVisibility(View.GONE);

        isManualMode = false;

        relativeCall = findViewById(R.id.relative_answer_call_fab);
        relativeCall.requestLayout();
        relativeCall.setVisibility(View.GONE);

        linearArrowCall = findViewById(R.id.linear_arrow_call);
        linearArrowCall.setVisibility(View.GONE);
        firstArrowCall = findViewById(R.id.first_arrow_call);
        secondArrowCall = findViewById(R.id.second_arrow_call);
        thirdArrowCall = findViewById(R.id.third_arrow_call);
        fourArrowCall = findViewById(R.id.four_arrow_call);

        relativeVideo = findViewById(R.id.relative_video_fab);
        relativeVideo.requestLayout();
        relativeVideo.setVisibility(View.GONE);

        linearArrowVideo = findViewById(R.id.linear_arrow_video);
        linearArrowVideo.setVisibility(View.GONE);
        firstArrowVideo = findViewById(R.id.first_arrow_video);
        secondArrowVideo = findViewById(R.id.second_arrow_video);
        thirdArrowVideo = findViewById(R.id.third_arrow_video);
        fourArrowVideo = findViewById(R.id.four_arrow_video);

        answerCallFAB = findViewById(R.id.answer_call_fab);
        enableFab(answerCallFAB);
        answerCallFAB.hide();

        onHoldFAB = findViewById(R.id.on_hold_fab);
        onHoldFAB.setOnClickListener(this);
        enableFab(onHoldFAB);
        onHoldFAB.hide();

        videoFAB = findViewById(R.id.video_fab);
        videoFAB.setOnClickListener(this);
        enableFab(videoFAB);
        videoFAB.hide();

        rejectFAB = findViewById(R.id.reject_fab);
        rejectFAB.setOnClickListener(this);
        enableFab(rejectFAB);
        rejectFAB.hide();

        speakerFAB = findViewById(R.id.speaker_fab);
        speakerFAB.setOnClickListener(this);
        enableFab(speakerFAB);
        speakerFAB.hide();

        microFAB = findViewById(R.id.micro_fab);
        microFAB.setOnClickListener(this);
        enableFab(microFAB);
        microFAB.hide();

        hangFAB = findViewById(R.id.hang_fab);
        hangFAB.setOnClickListener(this);
        enableFab(hangFAB);
        hangFAB.hide();

        shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        //Cameras in Group call
        bigElementsGroupCallLayout = findViewById(R.id.big_elements_group_call);
        bigElementsGroupCallLayout.setVisibility(View.GONE);

        //Recycler View for 1-6 peers
        recyclerViewLayout = findViewById(R.id.rl_recycler_view);
        recyclerViewLayout.setPadding(0, 0, 0, 0);
        recyclerViewLayout.setVisibility(View.GONE);
        recyclerView = findViewById(R.id.recycler_view_cameras);
        recyclerView.setPadding(0, 0, 0, 0);
        recyclerView.setColumnWidth((int) widthScreenPX);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setVisibility(View.GONE);

        //Big elements group calls
        peerSelectedCameraLayout = findViewById(R.id.peer_selected_layout);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) peerSelectedCameraLayout.getLayoutParams();
        params.setMargins(0, getActionBarHeight(this), 0, 0);
        peerSelectedCameraLayout.setLayoutParams(params);
        peerSelectedFragmentContainer = findViewById(R.id.fragment_peer_selected);
        peerSelectedFragmentContainer.setVisibility(View.GONE);
        peerSelectedCameraLayout.setVisibility(View.GONE);

        //Recycler View for 7-8 peers (because 9-10 without video)
        bigRecyclerViewLayout = findViewById(R.id.rl_big_recycler_view);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        bigRecyclerView = findViewById(R.id.big_recycler_view_cameras);
        bigRecyclerView.setLayoutManager(layoutManager);
        placeCarouselParticipants(true);
        bigRecyclerView.setVisibility(View.GONE);
        bigRecyclerViewLayout.setVisibility(View.GONE);

        /*Small*/
        smallCameraLayout = findViewById(R.id.small_camera_parent);
        smallFragmentContainer = findViewById(R.id.small_camera_fragment);
        RelativeLayout.LayoutParams paramsSmall = (RelativeLayout.LayoutParams) smallFragmentContainer.getLayoutParams();
        paramsSmall.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        paramsSmall.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        smallFragmentContainer.setLayoutParams(paramsSmall);
        smallFragmentContainer.setOnTouchListener(new OnDragTouchListener(smallFragmentContainer, smallCameraLayout));
        smallCameraLayout.setVisibility(View.GONE);
        smallFragmentContainer.setVisibility(View.GONE);

        /*FullScreen*/
        fullScreenCameraLayout = findViewById(R.id.full_screen_parent);
        fullScreenFragmentContainer = findViewById(R.id.full_screen_fragment);
        RelativeLayout.LayoutParams paramsFS = (RelativeLayout.LayoutParams) fullScreenFragmentContainer.getLayoutParams();
        paramsFS.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        paramsFS.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        fullScreenFragmentContainer.setLayoutParams(paramsFS);
        fullScreenCameraLayout.setVisibility(View.GONE);
        fullScreenFragmentContainer.setVisibility(View.GONE);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            chatId = extras.getLong(CHAT_ID, MEGACHAT_INVALID_HANDLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = this.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD) {
                requestWindowFeature(Window.FEATURE_NO_TITLE);
                this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            videoFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_video_off));
            speakerFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            speakerFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_speaker_off));
            onHoldFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            onHoldFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_call_hold_fab));
            microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
            microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_record_audio_w));

            initialUI(chatId);
        }

        IntentFilter filterCall = new IntentFilter(ACTION_UPDATE_CALL);
        filterCall.addAction(ACTION_CALL_STATUS_UPDATE);
        filterCall.addAction(ACTION_CHANGE_LOCAL_AVFLAGS);
        filterCall.addAction(ACTION_CHANGE_COMPOSITION);
        filterCall.addAction(ACTION_CHANGE_CALL_ON_HOLD);
        registerReceiver(chatCallUpdateReceiver, filterCall);

        IntentFilter filterSession = new IntentFilter(ACTION_UPDATE_CALL);
        filterSession.addAction(ACTION_SESSION_STATUS_UPDATE);
        filterSession.addAction(ACTION_CHANGE_REMOTE_AVFLAGS);
        filterSession.addAction(ACTION_CHANGE_AUDIO_LEVEL);
        filterSession.addAction(ACTION_CHANGE_NETWORK_QUALITY);
        filterSession.addAction(ACTION_CHANGE_SESSION_ON_HOLD);
        registerReceiver(chatSessionUpdateReceiver, filterSession);

        IntentFilter filterProximitySensor = new IntentFilter(BROADCAST_ACTION_INTENT_PROXIMITY_SENSOR);
        registerReceiver(proximitySensorReceiver, filterProximitySensor);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        logDebug("Type: " + request.getType());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestUpdate");
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        logDebug("Type: " + request.getType());
        if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER && e.getErrorCode() != MegaError.API_OK) {

            Bitmap avatar = getImageAvatarCall(chat, chat.getPeerHandle(0));
            if (avatar == null) {
                logWarning("No avatar found, no change needed");
                return;
            }

            if (!chat.isGroup()) {
                if (chat.getPeerEmail(0).compareTo(request.getEmail()) == 0) {
                    if (cameraFragmentFullScreen != null) {
                        cameraFragmentFullScreen.setAvatar(chat.getPeerHandle(0), avatar);
                    }
                }
            } else if (lessThanSevenParticipants() && adapterGrid != null) {
                adapterGrid.updateAvatarImage(request.getEmail());

            } else if (!lessThanSevenParticipants() && adapterList != null) {
                adapterList.updateAvatarImage(request.getEmail());
                if (chat != null && peerSelected != null && chat.getPeerEmailByHandle(peerSelected.getPeerId()) != null && chat.getPeerEmailByHandle(peerSelected.getPeerId()).compareTo(request.getEmail()) == 0) {
                    if (cameraFragmentPeerSelected != null) {
                        cameraFragmentPeerSelected.setAvatar(peerSelected.getPeerId(), avatar);
                    }
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
    }

    /**
     * Method to know if the action bar is being displayed.
     *
     * @return True if it's visible. False if it's hidden.
     */
    public boolean isActionBarShowing() {
        return aB.isShowing();
    }

    /**
     * Method to hide the action bar.
     */
    private void hideActionBar() {
        if (aB == null || !isActionBarShowing()) return;
        if (tB == null) {
            aB.hide();
            return;
        }
        tB.animate().translationY(-220).setDuration(DURATION_TOOLBAR_ANIMATION).withEndAction(() -> {
            aB.hide();
            if (checkIfNecessaryUpdateMuteIcon()) {
                adapterGrid.updateMuteIcon();
            }
        }).start();
    }

    /**
     * Method to show the action bar.
     */
    private void showActionBar() {
        if (aB == null || isActionBarShowing()) return;
        if (tB == null) {
            aB.show();
            return;
        }

        tB.animate().translationY(0).setDuration(DURATION_TOOLBAR_ANIMATION).withEndAction(() -> {
            aB.show();
            if (checkIfNecessaryUpdateMuteIcon()) {
                adapterGrid.updateMuteIcon();
            }
        }).start();
    }

    /**
     * Method for finding out if the muted icon needs to be updated.
     *
     * @return True, if it needs to be updated. False, if not.
     */
    private boolean checkIfNecessaryUpdateMuteIcon() {
        return chat.isGroup() && adapterGrid != null && (peersOnCall.size() == 2 || peersOnCall.size() == 5 || peersOnCall.size() == MAX_PARTICIPANTS_GRID);
    }

    /**
     * Method to hide the FAB buttons.
     */
    private void hideFABs() {
        videoFAB.hide();
        linearArrowVideo.setVisibility(View.GONE);
        relativeVideo.setVisibility(View.GONE);
        microFAB.hide();
        speakerFAB.hide();
        onHoldFAB.hide();
        rejectFAB.hide();
        answerCallFAB.hide();
        hangFAB.hide();

        linearArrowCall.setVisibility(View.GONE);
        relativeCall.setVisibility(View.GONE);
        placeCarouselParticipants(false);
    }

    /**
     * Method for resetting the height and width of videos in group calls.
     */
    private void restoreHeightAndWidth() {
        if (peersOnCall == null || peersOnCall.isEmpty()) return;
        logDebug("restoreHeightAndWidth");
        for (InfoPeerGroupCall peer : peersOnCall) {
            if (peer.getListener() == null) break;
            if (peer.getListener().getHeight() != 0) {
                peer.getListener().setHeight(0);
            }
            if (peer.getListener().getWidth() != 0) {
                peer.getListener().setWidth(0);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        app.unregisterProximitySensor();
    }

    @Override
    protected void onResume() {
        super.onResume();
        stopService(new Intent(this, IncomingCallService.class));
        restoreHeightAndWidth();
        app.startProximitySensor();
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        sendSignalPresence();
    }

    private void destroyAdapter() {
        if (lessThanSevenParticipants() && adapterGrid != null) {
            adapterGrid.onDestroy();
            adapterGrid = null;
        }
        if (!lessThanSevenParticipants() && adapterList != null) {
            adapterList.onDestroy();
            adapterList = null;
        }
    }

    @Override
    public void onDestroy() {
        app.unregisterProximitySensor();

        removeFullScreenFragment();
        removeSmallFragment();
        removePeerSelectedFragment();

        clearHandlers();
        activateChrono(false, callInProgressChrono, callChat);
        restoreHeightAndWidth();

        peerSelected = null;
        if (adapterList != null) {
            adapterList.updateMode(false);
        }
        isManualMode = false;
        destroyAdapter();

        peersOnCall.clear();
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
        if (bigRecyclerView != null) {
            bigRecyclerView.setAdapter(null);
        }

        unregisterReceiver(chatCallUpdateReceiver);
        unregisterReceiver(chatSessionUpdateReceiver);
        unregisterReceiver(proximitySensorReceiver);

        super.onDestroy();
    }

    private void finishActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.finishAndRemoveTask();
        } else {
            super.finish();
        }
    }

    @Override
    public void onBackPressed() {
        logDebug("onBackPressed");
        super.onBackPressed();
        finishActivity();
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
        logDebug("Type: " + request.getType());
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {
        logDebug("Type: " + request.getType());
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        logDebug("Type: " + request.getType());

        if (request.getType() == MegaChatRequest.TYPE_HANG_CHAT_CALL) {
            logDebug("TYPE_HANG_CHAT_CALL");
            if (getCall() == null) return;

            if(chatIdToHang == chatId){
                app.setSpeakerStatus(callChat.getChatid(), false);
                finishActivity();
            }else{
                app.setSpeakerStatus(callChat.getChatid(), answerWithVideo);
                megaChatApi.answerChatCall(chatId, answerWithVideo, ChatCallActivity.this);
                clearAnimations();
            }
        } else if (request.getType() == MegaChatRequest.TYPE_ANSWER_CHAT_CALL) {

            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                if (request.getFlag() == true) {
                    logDebug("Ok answer with video");
                } else {
                    logDebug("Ok answer with NO video - ");
                }

                updateLocalAV();
            } else {
                logWarning("Error call: " + e.getErrorString());

                if (e.getErrorCode() == MegaChatError.ERROR_TOOMANY) {
                    showErrorAlertDialogGroupCall(getString(R.string.call_error_too_many_participants), true, this);
                } else {
                    if (getCall() == null) return;
                    app.setSpeakerStatus(callChat.getChatid(), false);
                    finishActivity();
                }
            }
        } else if (request.getType() == MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL) {

            if (e.getErrorCode() != MegaChatError.ERROR_OK) {
                logWarning("Error changing audio or video: " + e.getErrorString());
                if (e.getErrorCode() == MegaChatError.ERROR_TOOMANY) {
                    showSnackbar(getString(R.string.call_error_too_many_video));
                }
            }
        } else if (request.getType() == MegaChatRequest.TYPE_SET_CALL_ON_HOLD) {
            if (e.getErrorCode() == MegaChatError.ERROR_NOENT) {
                logWarning("Error. No calls in this chat " + e.getErrorString());
            } else if (e.getErrorCode() == MegaChatError.ERROR_ACCESS) {
                logWarning("Error. The call is not in progress " + e.getErrorString());
                showSnackbar(getString(R.string.call_error_call_on_hold));
            } else if (e.getErrorCode() == MegaChatError.ERROR_ARGS) {
                logWarning("Error. The call was already in that state " + e.getErrorString());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
    }

    /**
     * Method for hidding the reconnect call bar.
     */
    public void hideReconnecting() {
        if (!reconnectingLayout.isShown()) return;
        logDebug("Hidding Reconnecting bar and showing You are back bar");
        reconnectingLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.accentColor));
        reconnectingText.setText(getString(R.string.connected_message));
        reconnectingLayout.setAlpha(1);
        reconnectingLayout.setVisibility(View.VISIBLE);
        reconnectingLayout.animate()
                .alpha(0f)
                .setDuration(INFO_ANIMATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        reconnectingLayout.setVisibility(View.GONE);
                    }
                });
        updateSubTitle();
    }

    /**
     * Method for displaying the reconnect call bar.
     */
    private void showReconnecting() {
        reconnectingLayout.clearAnimation();
        if (reconnectingLayout.isShown() && !reconnectingText.getText().equals(getString(R.string.connected_message)))
            return;

        reconnectingLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.reconnecting_bar));
        reconnectingText.setText(getString(R.string.reconnecting_message));
        reconnectingLayout.setVisibility(View.VISIBLE);
        reconnectingLayout.setAlpha(1);
    }

    private void connectingCall() {
        subtitleToobar.setVisibility(View.VISIBLE);
        activateChrono(false, callInProgressChrono, callChat);
        subtitleToobar.setText(getString(R.string.chat_connecting));
    }

    private void updateInfoUsersBar(String text) {
        logDebug("updateInfoUsersBar");
        infoUsersBar.setText(text);
        infoUsersBar.setBackgroundColor(ContextCompat.getColor(this, R.color.accentColor));
        infoUsersBar.setAlpha(1);
        infoUsersBar.setVisibility(View.VISIBLE);
        infoUsersBar.animate().alpha(0).setDuration(INFO_ANIMATION);
    }

    private void sendSignalPresence() {
        if (getCall() == null) return;
        if (callChat.getStatus() != MegaChatCall.CALL_STATUS_IN_PROGRESS && callChat.getStatus() != MegaChatCall.CALL_STATUS_REQUEST_SENT)
            return;

        app.sendSignalPresenceActivity();
    }

    /**
     * Method for positioning the fab buttons.
     *
     * @param isIncomingCall True, if the call is incoming and the arrows should be displayed. False, if not.
     */
    private void displayLinearFAB(boolean isIncomingCall) {
        logDebug("displayLinearFAB");
        RelativeLayout.LayoutParams layoutParams;
        if (isIncomingCall) {
            layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        } else {
            layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        }
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        linearFAB.setLayoutParams(layoutParams);
        linearFAB.requestLayout();
        linearFAB.setOrientation(LinearLayout.HORIZONTAL);
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");
        if (getCall() == null) return;

        switch (v.getId()) {
            case R.id.another_call_layout:
                returnToAnotherCall();
                break;

            case R.id.big_elements_individual_call:
                remoteCameraClick();
                break;

            case R.id.video_fab:
                logDebug("Video FAB");
                if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                    if (canNotJoinCall(this, callChat, chat)) break;

                    displayLinearFAB(false);
                    checkAnswerCall(true);

                } else if (callChat.hasLocalVideo()) {
                    megaChatApi.disableVideo(chatId, this);
                } else {
                    logDebug("Enable Video");
                    megaChatApi.enableVideo(chatId, this);
                }
                sendSignalPresence();
                break;

            case R.id.micro_fab:
                logDebug("Click on micro fab");
                if (callChat.hasLocalAudio()) {
                    megaChatApi.disableAudio(chatId, this);
                } else {
                    megaChatApi.enableAudio(chatId, this);
                }
                sendSignalPresence();
                break;

            case R.id.speaker_fab:
                logDebug("Click on speaker fab");
                if (app.getSpeakerStatus(callChat.getChatid())) {
                    app.setSpeakerStatus(callChat.getChatid(), false);
                } else {
                    app.setSpeakerStatus(callChat.getChatid(), true);
                }
                updateLocalSpeakerStatus();
                sendSignalPresence();
                break;

            case R.id.on_hold_fab:
                logDebug("Click on call on hold fab");
                MegaChatCall callOnHold = getAnotherCallOnHold(callChat.getId());
                if (callOnHold == null) {
                    if (getCall() == null)
                        break;

                    if (callChat.isOnHold()) {
                        checkAnotherCallActive();
                    } else {
                        megaChatApi.setCallOnHold(chatId, true, this);
                        sendSignalPresence();
                    }
                } else {
                    returnToAnotherCallOnHold(callOnHold);
                }
                break;

            case R.id.reject_fab:
            case R.id.hang_fab:
                logDebug("Click on reject fab or hang fab");
                chatIdToHang = chatId;
                megaChatApi.hangChatCall(chatId, this);
                sendSignalPresence();
                break;

            case R.id.answer_call_fab:
                logDebug("Click on answer fab");
                if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                    if (canNotJoinCall(this, callChat, chat)) break;

                    displayLinearFAB(false);
                    checkAnswerCall(false);
                }
                sendSignalPresence();
                break;

        }
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
            if (!hasCameraPermission) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                return false;
            }
            boolean hasRecordAudioPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
            if (!hasRecordAudioPermission) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO);
                return false;
            }
        }
        return true;
    }

    /**
     * Method for updating FAB buttons.
     */
    private void showInitialFABConfiguration() {
        if (getCall() == null)
            return;

        if(!checkPermissions()){
            withoutCallPermissions();
            answerCallFAB.hide();
            microFAB.hide();
            videoFAB.hide();
            rejectFAB.hide();
            onHoldFAB.hide();
            return;
        }
        logDebug("Call Status "+callStatusToString(callChat.getStatus()));

        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
            relativeCall.setVisibility(View.VISIBLE);
            answerCallFAB.show();
            linearArrowCall.setVisibility(View.GONE);
            relativeVideo.setVisibility(View.VISIBLE);
            videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
            videoFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_videocam_white));
            if(!videoFAB.isShown()) videoFAB.show();

            linearArrowVideo.setVisibility(View.GONE);
            rejectFAB.show();
            speakerFAB.hide();
            onHoldFAB.hide();
            microFAB.hide();
            hangFAB.hide();

            if (callChat.hasVideoInitialCall()) {
                displayLinearFAB(true);

                answerCallFAB.setOnClickListener(this);
                videoFAB.setOnClickListener(null);
                linearArrowVideo.setVisibility(View.VISIBLE);

                videoFAB.startAnimation(shake);

                animationAlphaArrows(fourArrowVideo);
                handlerArrow1 = new Handler();
                handlerArrow1.postDelayed(new Runnable() {
                    public void run() {
                        animationAlphaArrows(thirdArrowVideo);
                        handlerArrow2 = new Handler();
                        handlerArrow2.postDelayed(new Runnable() {
                            public void run() {
                                animationAlphaArrows(secondArrowVideo);
                                handlerArrow3 = new Handler();
                                handlerArrow3.postDelayed(new Runnable() {
                                    public void run() {
                                        animationAlphaArrows(firstArrowVideo);
                                    }
                                }, ARROW_ANIMATION);
                            }
                        }, ARROW_ANIMATION);
                    }
                }, ARROW_ANIMATION);

                videoFAB.setOnTouchListener(new OnSwipeTouchListener(this) {
                    public void onSwipeTop() {
                        videoFAB.clearAnimation();
                        animationButtons(true);
                    }
                });

            } else {
                displayLinearFAB(true);

                linearArrowCall.setVisibility(View.VISIBLE);

                answerCallFAB.startAnimation(shake);

                animationAlphaArrows(fourArrowCall);
                handlerArrow4 = new Handler();
                handlerArrow4.postDelayed(new Runnable() {
                    public void run() {
                        animationAlphaArrows(thirdArrowCall);
                        handlerArrow5 = new Handler();
                        handlerArrow5.postDelayed(new Runnable() {
                            public void run() {
                                animationAlphaArrows(secondArrowCall);
                                handlerArrow6 = new Handler();
                                handlerArrow6.postDelayed(new Runnable() {
                                    public void run() {
                                        animationAlphaArrows(firstArrowCall);
                                    }
                                }, ARROW_ANIMATION);
                            }
                        }, ARROW_ANIMATION);
                    }
                }, ARROW_ANIMATION);

                answerCallFAB.setOnTouchListener(new OnSwipeTouchListener(this) {
                    public void onSwipeTop() {
                        answerCallFAB.clearAnimation();
                        animationButtons(false);
                    }
                });
            }

        } else if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT || callChat.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS || callChat.getStatus() == MegaChatCall.CALL_STATUS_JOINING || callChat.getStatus() == MegaChatCall.CALL_STATUS_RECONNECTING) {

            if (!microFAB.isShown()) microFAB.show();
            updateMicroFABStatus();

            if (!speakerFAB.isShown()) speakerFAB.show();
            updateLocalSpeakerStatus();

            if (!onHoldFAB.isShown()) onHoldFAB.show();
            updateOnHoldFABStatus();

            if (!videoFAB.isShown()) videoFAB.show();
            updateVideoFABStatus();

            if(!hangFAB.isShown()) hangFAB.show();

            rejectFAB.hide();
            answerCallFAB.hide();

            relativeVideo.setVisibility(View.VISIBLE);
            linearArrowVideo.setVisibility(View.GONE);
            relativeCall.setVisibility(View.INVISIBLE);
            linearArrowCall.setVisibility(View.GONE);
        }

        placeCarouselParticipants(true);
    }

    private void animationButtons(final boolean isVideo) {
        logDebug("isVideo: " + isVideo);

        TranslateAnimation translateAnim = new TranslateAnimation(0, 0, 0, -380);
        translateAnim.setDuration(MOVE_ANIMATION);
        translateAnim.setFillAfter(true);
        translateAnim.setFillBefore(true);
        translateAnim.setRepeatCount(0);

        AlphaAnimation alphaAnim = new AlphaAnimation(1.0f, 0.0f);
        alphaAnim.setDuration(ALPHA_ANIMATION);
        alphaAnim.setFillAfter(true);
        alphaAnim.setFillBefore(true);
        alphaAnim.setRepeatCount(0);

        AnimationSet s = new AnimationSet(false);//false means don't share interpolators
        s.addAnimation(translateAnim);
        s.addAnimation(alphaAnim);

        if (!isVideo) {
            answerCallFAB.startAnimation(s);
        } else {
            videoFAB.startAnimation(s);
        }

        firstArrowVideo.clearAnimation();
        secondArrowVideo.clearAnimation();
        thirdArrowVideo.clearAnimation();
        fourArrowVideo.clearAnimation();
        linearArrowVideo.setVisibility(View.GONE);

        translateAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                displayLinearFAB(false);
                if (!isVideo) {
                    answerCallFAB.hide();
                    answerCall(false);
                } else {
                    videoFAB.hide();
                    answerCall(true);
                }
            }
        });
    }

    /**
     * Method for positioning the carousel of participants.
     *
     * @param isAlignBotton True, if it must be placed below the selected video. False, if it must be placed above.
     */
    private void placeCarouselParticipants(boolean isAlignBotton) {

        if (bigRecyclerViewLayout == null || bigRecyclerView == null || peerSelectedCameraLayout == null)
            return;

        RelativeLayout.LayoutParams bigRecyclerViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        bigRecyclerViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        if (isAlignBotton) {
            bigRecyclerViewParams.addRule(RelativeLayout.ALIGN_BOTTOM, peerSelectedCameraLayout.getId());
            bigRecyclerViewParams.addRule(RelativeLayout.BELOW, 0);

        } else {
            bigRecyclerViewParams.addRule(RelativeLayout.ALIGN_BOTTOM, 0);
            bigRecyclerViewParams.addRule(RelativeLayout.BELOW, peerSelectedCameraLayout.getId());
        }

        bigRecyclerViewLayout.setLayoutParams(bigRecyclerViewParams);
        bigRecyclerViewLayout.requestLayout();
    }

    /**
     * Check local audio and video values.
     */
    private void updateLocalAV() {
        if (getCall() != null && callChat.getStatus() != MegaChatCall.CALL_STATUS_RING_IN) {
            updateLocalVideoStatus();
            updateLocalAudioStatus();
            updateSubtitleNumberOfVideos();
        }
    }

    /**
     * Update the local video.
     */
    private void updateLocalVideoStatus() {
        if (getCall() == null)
            return;

        boolean isVideoOn = callChat.hasLocalVideo();
        if (!inTemporaryState) {
            app.setVideoStatus(callChat.getChatid(), isVideoOn);
        }

        if(!videoFAB.isShown()) videoFAB.show();
        updateVideoFABStatus();

        if (!chat.isGroup()) {
            if (cameraFragmentFullScreen != null) {
                cameraFragmentFullScreen.checkValues(megaChatApi.getMyUserHandle(), megaChatApi.getMyClientidHandle(chatId));
            }

            if (cameraFragmentSmall != null) {
                cameraFragmentSmall.checkValues(megaChatApi.getMyUserHandle(), megaChatApi.getMyClientidHandle(chatId));
            }

        } else {

            if (peersOnCall.isEmpty())
                return;

            for (int i = 0; i < peersOnCall.size(); i++) {
                InfoPeerGroupCall peerInfo = peersOnCall.get(i);
                if (isItMe(chatId, peerInfo.getPeerId(), peerInfo.getClientId())) {
                    if (isVideoOn == peerInfo.isVideoOn())
                        break;

                    peerInfo.setVideoOn(isVideoOn);
                    updateChangesVideo(i);
                    break;
                }
            }
        }

        invalidateOptionsMenu();
        checkTypeCall();
    }

    /**
     * Update the local audio.
     */
    private void updateLocalAudioStatus() {
        if (getCall() == null) return;

        logDebug("Call Status " + callStatusToString(callChat.getStatus()));
        boolean isAudioOn = callChat.hasLocalAudio();
        updateMicroFABStatus();

        if (!chat.isGroup()) {
            //Individual call
            refreshOwnMicro();
            if (cameraFragmentSmall != null) {
                cameraFragmentSmall.showMuteIcon(megaChatApi.getMyUserHandle(), megaChatApi.getMyClientidHandle(chatId));
            }

        } else {
            //Group call
            if (peersOnCall.isEmpty()) return;

            int position = peersOnCall.size() - 1;
            InfoPeerGroupCall infoPeer = peersOnCall.get(position);

            if (isAudioOn == infoPeer.isAudioOn())
                return;

            infoPeer.setAudioOn(isAudioOn);
            updateChangesAudio(position);
        }
    }

    /**
     * Update video FAB status depending on the state of the local video.
     */
    private void updateVideoFABStatus() {
        if (callChat.hasLocalVideo()) {
            //Enable video FAB
            videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
            videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_white));
        } else {
            //Disable video FAB
            videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_off));
        }
    }

    /**
     * Method for updating speaker status, ON or OFF..
     */
    private void updateLocalSpeakerStatus() {
        if (getCall() == null || (!statusCallInProgress(callChat.getStatus()) && callChat.getStatus() != MegaChatCall.CALL_STATUS_RING_IN))
            return;

        boolean isSpeakerOn = MegaApplication.getSpeakerStatus(callChat.getChatid());
        app.updateSpeakerStatus(isSpeakerOn, callChat.getStatus(), callChat.getChatid());


        if (isSpeakerOn) {
            speakerFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
            speakerFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_speaker_on));
        } else {
            speakerFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            speakerFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_speaker_off));
        }
    }

    /**
     * Update micro FAB status depending on the state of the local audio.
     */
    private void updateMicroFABStatus() {
        if (callChat.hasLocalAudio()) {
            //Enable video FAB
            microFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
            microFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_record_audio_w));
        } else {
            //Disable video FAB
            microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            microFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_mic_off));
        }
    }

    /**
     * Method to control when a call waiting is switched to active.
     */
    private void checkAnotherCallActive() {
        ArrayList<Long> chatsIDsWithCallActive = getCallsParticipating();

        if (chatsIDsWithCallActive != null || !chatsIDsWithCallActive.isEmpty()) {
            for (Long anotherChatId : chatsIDsWithCallActive) {
                if (callChat.getChatid() != anotherChatId && megaChatApi.getChatCall(anotherChatId) != null && !megaChatApi.getChatCall(anotherChatId).isOnHold()) {
                    megaChatApi.setCallOnHold(anotherChatId, true, this);
                    break;
                }
            }
        }

        megaChatApi.setCallOnHold(chatId, false, this);
        sendSignalPresence();
    }

    /**
     * Method for returning to a call on hold and activating it. The call will be put on hold.
     */
    private void returnToAnotherCallOnHold(MegaChatCall callOnHold) {
        if (callOnHold == null) {
            logWarning("There is no other call on hold");
            return;
        }

        MegaChatRoom chatCallOnHold = megaChatApi.getChatRoom(callOnHold.getChatid());
        if(chatCallOnHold == null){
            logWarning("The chats does not exist");
            return;
        }

        megaChatApi.setCallOnHold(chatId, true, this);
        megaChatApi.setCallOnHold(chatCallOnHold.getChatId(), false, this);

        sendSignalPresence();

        MegaApplication.setSpeakerStatus(chatCallOnHold.getChatId(), false);
        MegaApplication.setShowPinScreen(false);
        Intent intentOpenCall = new Intent(this, ChatCallActivity.class);
        intentOpenCall.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentOpenCall.putExtra(CHAT_ID, chatCallOnHold.getChatId());
        startActivity(intentOpenCall);
    }

    /**
     * Method for returning to a call on hold or call active.
     */
    private void returnToAnotherCall() {
        if (getCall() == null)
            return;

        ArrayList<Long> chatsIDsWithCallActive = getCallsParticipating();
        if (chatsIDsWithCallActive == null || chatsIDsWithCallActive.isEmpty())
            return;

        for (Long anotherChatId : chatsIDsWithCallActive) {
            if (callChat.getChatid() != anotherChatId) {
                logDebug("Returning to another call");
                MegaApplication.setSpeakerStatus(anotherChatId, false);
                MegaApplication.setShowPinScreen(false);
                Intent intentOpenCall = new Intent(this, ChatCallActivity.class);
                intentOpenCall.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intentOpenCall.putExtra(CHAT_ID, anotherChatId);
                startActivity(intentOpenCall);
                return;
            }
        }
    }

    /**
     * Update the bar if there are calls on hold in other chats or not.
     *
     * @param currentChatId The current chat ID.
     */
    private void updateAnotherCallOnHoldBar(long currentChatId) {
        ArrayList<Long> chatsIDsWithCallActive = getCallsParticipating();
        if (chatsIDsWithCallActive == null || chatsIDsWithCallActive.isEmpty()) {
            anotherCallLayout.setVisibility(View.GONE);
            return;
        }

        if (getCall() != null && callChat.isOnHold()) {
            for (Long anotherChatId : chatsIDsWithCallActive) {
                if (anotherChatId != currentChatId) {
                    MegaChatCall call = megaChatApi.getChatCall(anotherChatId);
                    if (!call.isOnHold()) {
                        anotherCallLayout(call.isOnHold(), anotherChatId);
                        return;
                    }
                }
            }
        } else {
            for (Long anotherChatId : chatsIDsWithCallActive) {
                if (anotherChatId != currentChatId) {
                    MegaChatCall call = megaChatApi.getChatCall(anotherChatId);
                    if (call.isOnHold()) {
                        anotherCallLayout(call.isOnHold(), anotherChatId);
                        return;
                    }
                }
            }
        }
        anotherCallLayout.setVisibility(View.GONE);
    }

    private void anotherCallLayout(boolean isOnHold, long anotherChatId) {
        MegaChatRoom anotherChat = megaChatApi.getChatRoom(anotherChatId);
        if (!anotherChat.isGroup()) {
            MegaChatCall anotherCall = megaChatApi.getChatCall(anotherChatId);
            if (anotherCall != null) {
                MegaChatSession sessionAnotherChat = anotherCall.getMegaChatSession(anotherCall.getSessionsPeerid().get(0), anotherCall.getSessionsClientid().get(0));
                if (sessionAnotherChat != null && sessionAnotherChat.isOnHold()) {
                    isOnHold = true;
                }
            }
        }
        anotherCallTitle.setText(anotherChat.getTitle());
        anotherCallSubtitle.setText(getString(isOnHold ? R.string.call_on_hold : R.string.call_in_progress_layout));
        anotherCallSubtitle.setAlpha(1f);
        if (isOnHold) {
            anotherCallLayout.setAlpha(0.9f);
            anotherCallLayoutLayer.setBackgroundColor(ContextCompat.getColor(this, R.color.transparent_black2));
        } else {
            anotherCallLayout.setAlpha(1f);
            anotherCallLayoutLayer.setBackgroundColor(Color.TRANSPARENT);
        }
        anotherCallLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Update the button if there are calls on hold in other chats or not.
     *
     * @param callOnHold The call on hold.
     */
    private void updateOnHoldFabButton(MegaChatCall callOnHold) {
        if (callOnHold != null) {
            onHoldFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.disable_fab_chat_call)));
            onHoldFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_call_swap));
        } else {
            onHoldFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_call_hold_fab));
            onHoldFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(callChat.isOnHold() ||
                    isSessionOnHold(chat.getChatId()) ? R.color.accentColor : R.color.disable_fab_chat_call)));
        }
    }

    /**
     * Update pause FAB status depending on the state of the call.
     */
    private void updateOnHoldFABStatus() {

        if (getCall() == null) {
            return;
        }

        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT ||
                callChat.getStatus() == MegaChatCall.CALL_STATUS_RECONNECTING ||
                (!callChat.isOnHold() && isSessionOnHold(chat.getChatId()) && !chat.isGroup())) {
            disableFab(onHoldFAB);
        } else {
            enableFab(onHoldFAB);
        }

        updateOnHoldFabButton(getAnotherCallOnHold(callChat.getId()));
        updateFABwithCallOnHold();
    }

    private void disableFab(final FloatingActionButton fab) {
        fab.setEnabled(false);
        fab.setAlpha(0.5f);
    }

    private void enableFab(final FloatingActionButton fab) {
        fab.setEnabled(true);
        fab.setAlpha(1f);
    }

    /**
     * Method to enable or disable the buttons depending on the call on hold.
     */
    private void updateFABwithCallOnHold() {
        if (callChat.isOnHold()) {
            disableFab(speakerFAB);
            disableFab(videoFAB);
            disableFab(microFAB);
        } else {
            enableFab(microFAB);
            enableFab(speakerFAB);
            if (!chat.isGroup() && isSessionOnHold(chat.getChatId())) {
                disableFab(videoFAB);
            } else {
                enableFab(videoFAB);
            }
        }
    }

    /**
     * Check if is an audio call.
     */
    private void checkTypeCall() {
        if (isOnlyAudioCall()) {
            showActionBar();
            showInitialFABConfiguration();
        }
    }

    /**
     * Check remote audio and video values.
     *
     * @param session of user that has changed its values.
     */
    public void updateRemoteAV(MegaChatSession session) {
        if (getCall() == null || session == null)
            return;

        updateRemoteAudioStatus(session);
        updateRemoteVideoStatus(session);
        updateSubtitleNumberOfVideos();
    }

    /**
     * Update the remote audio.
     *
     * @param session The session.
     */
    private void updateRemoteAudioStatus(MegaChatSession session) {
        if (chat.isGroup()) {
            for (int i = 0; i < peersOnCall.size(); i++) {
                if (peersOnCall.get(i).getPeerId() == session.getPeerid() && peersOnCall.get(i).getClientId() == session.getClientid()) {
                    if ((session.hasAudio() && peersOnCall.get(i).isAudioOn()) || (!session.hasAudio() && !peersOnCall.get(i).isAudioOn()))
                        break;

                    peersOnCall.get(i).setAudioOn(session.hasAudio());
                    updateChangesAudio(i);
                    if (cameraFragmentPeerSelected != null && !lessThanSevenParticipants() && peerSelected != null && peerSelected.getPeerId() == session.getPeerid() && peerSelected.getClientId() == session.getClientid()) {
                        cameraFragmentPeerSelected.showMuteIcon(peerSelected.getPeerId(), peerSelected.getClientId());
                    }
                    break;
                }
            }
        } else {
            refreshContactMicro(session);
        }
    }

    /**
     * Method for updating the contact muted call bar.
     */
    private void refreshContactMicro(MegaChatSession session) {
        if (session == null || chat.isGroup()) {
            mutateContactCallLayout.setVisibility(View.GONE);
            return;
        }

        logDebug("Session status is " + sessionStatusToString(session.getStatus()));
        if (session.isOnHold() || callChat.isOnHold() || session.getStatus() == MegaChatSession.SESSION_STATUS_INITIAL || session.hasAudio()) {
            mutateContactCallLayout.setVisibility(View.GONE);
        }else{
            String name = chatC.getParticipantFirstName(chat.getPeerHandle(0));
            if (isTextEmpty(name)) {
                if (megaChatApi != null) {
                    name = megaChatApi.getContactEmail(callChat.getSessionsPeerid().get(0));
                }
                if (name == null) {
                    name = " ";
                }
            }
            mutateCallText.setText(getString(R.string.muted_contact_micro, name));
            mutateContactCallLayout.setVisibility(View.VISIBLE);
        }
        refreshOwnMicro();
    }

    /**
     * Update the remote video.
     *
     * @param session The session.
     */
    private void updateRemoteVideoStatus(MegaChatSession session) {
        if (chat.isGroup()) {
            updateRemoteVideoGroupCall(session);
        } else {
            updateRemoteVideoIndividualCall(session);
        }
        checkTypeCall();
    }

    /**
     * Update the remote video in a individual call.
     *
     * @param session The session.
     */
    private void updateRemoteVideoIndividualCall(MegaChatSession session) {
        if (cameraFragmentFullScreen != null) {
            cameraFragmentFullScreen.checkValues(session.getPeerid(), session.getClientid());
        }
        if (!callChat.hasLocalVideo() && cameraFragmentSmall != null) {
            cameraFragmentSmall.checkIndividualAudioCall();
        }
    }

    /**
     * Update the remote video of a participant in a group call.
     *
     * @param session The session of participant.
     */
    private void updateRemoteVideoGroupCall(MegaChatSession session) {
        for (int i = 0; i < peersOnCall.size(); i++) {
            if (peersOnCall.get(i).getPeerId() == session.getPeerid() && peersOnCall.get(i).getClientId() == session.getClientid()) {
                if ((session.hasVideo() && peersOnCall.get(i).isVideoOn()) || (!session.hasVideo() && !peersOnCall.get(i).isVideoOn()))
                    break;

                peersOnCall.get(i).setVideoOn(session.hasVideo());
                updateChangesVideo(i);

                if (!lessThanSevenParticipants() && peerSelected != null && peerSelected.getPeerId() == session.getPeerid() && peerSelected.getClientId() == session.getClientid()) {
                    updateParticipantSelectedStatus();
                }
                break;
            }
        }
    }

    /**
     * Method for finding out if there are less than 7 participants in the group call.
     *
     * @return True, if there's less. False, if there's more.
     */
    private boolean lessThanSevenParticipants() {
        return chat != null && chat.isGroup() && peersOnCall != null && peersOnCall.size() <= MAX_PARTICIPANTS_GRID;
    }

    private void updateChangesAudio(int position) {
        if (lessThanSevenParticipants() && adapterGrid != null) {
            adapterGrid.updateParticipantAudio(position);
            return;
        }
        if (!lessThanSevenParticipants() && adapterList != null) {
            adapterList.updateParticipantAudio(position);
            return;
        }
        updateUI();
    }

    /**
     * Method to update the group call UI if there are changes in a participant's video.
     *
     * @param position The position of the participant in the array.
     */
    private void updateChangesVideo(int position) {
        if (lessThanSevenParticipants() && adapterGrid != null) {
            adapterGrid.notifyItemChanged(position);
            adapterGrid.updateAvatarsPosition();
            return;
        }
        if (!lessThanSevenParticipants() && adapterList != null) {
            adapterList.notifyItemChanged(position);
            adapterList.updateAvatarsPosition();
            return;
        }
        updateUI();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA:
            case RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showInitialFABConfiguration();
                } else {
                    withoutCallPermissions();
                }
                break;
        }
    }

    /**
     * Method showing only the hang up button because you do not have the necessary permissions.
     */
    private void withoutCallPermissions() {
        hangFAB.show();
        displayLinearFAB(false);
    }

    /**
     * Method for finding out if a group call is audio-only.
     *
     * @return True if it's audio only. False if it's video.
     */
    private boolean isOnlyAudioCall() {
        return getCall() != null && callChat.getNumParticipants(MegaChatCall.VIDEO) <= 0;
    }

    /**
     * Method to hide or show the buttons when clicked on the screen.
     */
    public void remoteCameraClick() {
        stopCountDownTimer();

        if (getCall() == null || (callChat.getStatus() != MegaChatCall.CALL_STATUS_IN_PROGRESS && callChat.getStatus() != MegaChatCall.CALL_STATUS_JOINING && callChat.getStatus() != MegaChatCall.CALL_STATUS_RECONNECTING))
            return;

        if (callChat.getStatus() != MegaChatCall.CALL_STATUS_IN_PROGRESS && callChat.getStatus() != MegaChatCall.CALL_STATUS_JOINING && callChat.getStatus() != MegaChatCall.CALL_STATUS_RECONNECTING) {
            if (!aB.isShowing()) {
                showActionBar();
                showInitialFABConfiguration();
            }
            return;
        }

        if (aB.isShowing()) {
            if (isOnlyAudioCall())
                return;

            hideActionBar();
            hideFABs();
            return;
        }

        showActionBar();
        showInitialFABConfiguration();
    }

    /**
     * Method to control when a participant is touched to select him..
     *
     * @param peer The participant touched.
     */
    public void itemClicked(InfoPeerGroupCall peer) {
        logDebug("userSelected: -> (peerId = " + peer.getPeerId() + ", clientId = " + peer.getClientId() + ")");
        if (peerSelected.getPeerId() == peer.getPeerId() && peerSelected.getClientId() == peer.getClientId()) {

            //I touched the same user that is now in big fragment:
            if (isManualMode) {
                isManualMode = false;
                logDebug("Manual mode - False");
            } else {
                isManualMode = true;
                logDebug("Manual mode - True");
            }
            if (adapterList == null || peersOnCall.isEmpty())
                return;

            adapterList.updateMode(isManualMode);
            for (int i = 0; i < peersOnCall.size(); i++) {
                if (peersOnCall.get(i).getPeerId() == peer.getPeerId() && peersOnCall.get(i).getClientId() == peer.getClientId()) {
                    peersOnCall.get(i).setGreenLayer(true);
                } else if (peersOnCall.get(i).hasGreenLayer()) {
                    peersOnCall.get(i).setGreenLayer(false);
                }
                adapterList.updatePeerSelected(i);
            }

        } else if (isItMe(chatId, peer.getPeerId(), peer.getClientId())) {
            //Me
            logDebug("Click myself - do nothing");
        } else {
            //contact
            if (!isManualMode) {
                isManualMode = true;
                if (adapterList != null) {
                    adapterList.updateMode(true);
                }
                logDebug("Manual mode - True");
            }
            peerSelected = peer;
            updateParticipantSelected();
        }
    }

    /**
     * Method for answering a call.
     *
     * @param isVideoCall True, if it's a video call. False, if it's an audio call.
     */
    private void answerCall(boolean isVideoCall) {
        logDebug("answerCall");
        if(getCall() == null)
            return;

        checkAnswerCall(isVideoCall);
        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }
    }

    private void checkAnswerCall(boolean isVideoCall){
        long chatIdOfCallActive = existsAnotherCall(chatId);
        if(chatIdOfCallActive == chatId) {
            app.setSpeakerStatus(callChat.getChatid(), isVideoCall);
            megaChatApi.answerChatCall(chatId, isVideoCall, this);
            clearAnimations();
        }else{
            chatIdToHang = chatIdOfCallActive;
            answerWithVideo = isVideoCall;
            megaChatApi.hangChatCall(chatIdOfCallActive, this);
        }
    }

    private void clearAnimations(){
        clearHandlers();
        answerCallFAB.clearAnimation();
        videoFAB.clearAnimation();
    }

    /**
     * Show the arrow animations.
     * @param arrow The ImageView.
     */
    private void animationAlphaArrows(final ImageView arrow) {
        logDebug("animationAlphaArrows");

        AlphaAnimation alphaAnimArrows = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimArrows.setDuration(ALPHA_ARROW_ANIMATION);
        alphaAnimArrows.setFillAfter(true);
        alphaAnimArrows.setFillBefore(true);
        alphaAnimArrows.setRepeatCount(Animation.INFINITE);
        arrow.startAnimation(alphaAnimArrows);
    }

    /**
     * Method for updating the subtitle.
     */
    private void updateSubTitle() {

        if (getCall() == null) return;
        logDebug("Call Status: "+callStatusToString(callChat.getStatus()));

        switch (callChat.getStatus()){
            case MegaChatCall.CALL_STATUS_RECONNECTING:{
                activateChrono(false, callInProgressChrono, callChat);
                subtitleToobar.setVisibility(View.GONE);
                return;
            }

            case MegaChatCall.CALL_STATUS_REQUEST_SENT:{
                subtitleToobar.setVisibility(View.VISIBLE);
                activateChrono(false, callInProgressChrono, callChat);
                subtitleToobar.setText(getString(R.string.outgoing_call_starting));
                return;
            }

            case MegaChatCall.CALL_STATUS_RING_IN:{
                subtitleToobar.setVisibility(View.VISIBLE);
                activateChrono(false, callInProgressChrono, callChat);
                subtitleToobar.setText(getString(R.string.incoming_call_starting));
                return;
            }

            case MegaChatCall.CALL_STATUS_IN_PROGRESS:
            case MegaChatCall.CALL_STATUS_JOINING:{
                if (chat.isGroup()) {
                    boolean isInProgress = false;
                    MegaHandleList listPeerids = callChat.getSessionsPeerid();
                    MegaHandleList listClientids = callChat.getSessionsClientid();
                    for (int i = 0; i < listPeerids.size(); i++) {
                        MegaChatSession userSession = callChat.getMegaChatSession(listPeerids.get(i), listClientids.get(i));
                        if (userSession != null && userSession.getStatus() == MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
                            isInProgress = true;
                            break;
                        }
                    }
                    if (isInProgress) {
                        logDebug("Session in progress");
                        subtitleToobar.setVisibility(View.GONE);
                        activateChrono(true, callInProgressChrono, callChat);
                        return;
                    }

                    logWarning("Error getting the session of the user or session not in progress");
                    connectingCall();
                    return;
                }

                logDebug("Individual call in progress");
                linearParticipants.setVisibility(View.GONE);
                MegaChatSession userSession = callChat.getMegaChatSession(callChat.getSessionsPeerid().get(0), callChat.getSessionsClientid().get(0));
                if (userSession == null) {
                    logWarning("User session is null");
                    connectingCall();
                    return;
                }

                if (userSession.getStatus() == MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
                    subtitleToobar.setVisibility(View.GONE);
                    activateChrono(true, callInProgressChrono, callChat);
                    return;
                }
            }
        }

        subtitleToobar.setVisibility(View.GONE);
        activateChrono(false, callInProgressChrono, callChat);
    }

    /**
     * Method for updating the subtitle of the group call.
     */
    private void updateSubtitleNumberOfVideos() {
        if (chat == null || getCall() == null)
            return;

        if (!chat.isGroup() || !statusCallInProgress(callChat.getStatus())) {
            linearParticipants.setVisibility(View.GONE);
            return;
        }

        logDebug("Updating the number of participants with video on");
        int usersWithVideo = callChat.getNumParticipants(MegaChatCall.VIDEO);
        if (usersWithVideo <= 0) {
            linearParticipants.setVisibility(View.GONE);
            return;
        }

        if (totalVideosAllowed == 0 && megaChatApi != null) {
            totalVideosAllowed = megaChatApi.getMaxVideoCallParticipants();
        }
        if (totalVideosAllowed == 0) {
            linearParticipants.setVisibility(View.GONE);
            return;
        }

        participantText.setText(usersWithVideo + "/" + totalVideosAllowed);
        linearParticipants.setVisibility(View.VISIBLE);
        return;
    }

    /**
     * Method for cleaning several views.
     */
    private void clearViews() {
        destroyAdapter();

        recyclerView.setAdapter(null);
        recyclerView.setVisibility(View.GONE);
        recyclerViewLayout.setVisibility(View.GONE);

        bigRecyclerView.setAdapter(null);
        bigRecyclerView.setVisibility(View.GONE);
        bigRecyclerViewLayout.setVisibility(View.GONE);

        removePeerSelectedFragment();
    }

    /**
     * Method for updating the layer of the selected participant.
     *
     * @param position Participant's position in adapter.
     */
    private void updateGreenLayer(int position) {
        if (lessThanSevenParticipants() || adapterList == null)
            return;

        adapterList.updatePeerSelected(position);
    }

    /**
     * Method for updating the status of the selected participant.
     */
    private void updateParticipantSelectedStatus() {
        if (cameraFragmentPeerSelected != null && peerSelected != null) {
            cameraFragmentPeerSelected.checkValues(peerSelected.getPeerId(), peerSelected.getClientId());
        }
    }

    /**
     * Method for updating the selected participant.
     */
    private void updateParticipantSelected() {
        logDebug("Call status: "+callStatusToString(callChat.getStatus()));

        if (!statusCallInProgress(callChat.getStatus())) {
            if (peerSelected != null)
                return;

            if (peersOnCall.isEmpty()) {
                removePeerSelectedFragment();
                return;
            }
            peerSelected = peersOnCall.get((peersOnCall.size()) - 1);

        } else {

            if (peersOnCall.isEmpty() || (peerSelected == null && isManualMode))
                return;

            if (peerSelected == null) {
                peerSelected = peersOnCall.get(0);
                updateParticipantSelectedLayer(peerSelected, false);
            } else if (peersOnCall.contains(peerSelected)) {
                updateParticipantSelectedLayer(peerSelected, false);
            } else {
                peerSelected = peersOnCall.get(0);
                updateParticipantSelectedLayer(peerSelected, true);
            }
        }

        if (cameraFragmentPeerSelected != null) {
            cameraFragmentPeerSelected.changePeerSelected(chatId, callChat.getId(), peerSelected.getPeerId(), peerSelected.getClientId());
        } else {
            createPeerSelectedFragment();
        }
    }

    /**
     * Method for updating the layer of the participant selected.
     *
     * @param peerSelected     Peer ID of participant selected.
     * @param updateManualMode Participant selection mode.
     */
    private void updateParticipantSelectedLayer(InfoPeerGroupCall peerSelected, boolean updateManualMode){
        for(InfoPeerGroupCall peer:peersOnCall) {
            if (peerSelected.getPeerId() == peer.getPeerId() && peerSelected.getClientId() == peer.getClientId()) {
                if(updateManualMode){
                    isManualMode = false;
                    if (adapterList != null) {
                        adapterList.updateMode(false);
                    }
                }
                if (!peer.hasGreenLayer()) {
                    peer.setGreenLayer(true);
                    updateGreenLayer(peersOnCall.indexOf(peer));
                }
            } else if (peer.hasGreenLayer()) {
                peer.setGreenLayer(false);
                updateGreenLayer(peersOnCall.indexOf(peer));
            }
        }
    }

    /**
     * Method to update the selected participant's interface when the call is on hold.
     */
    private void updateParticipantSelectedInCallOnHold(){
        if(peerSelected == null)
            return;

        MegaChatSession session = getSessionCall(peerSelected.getPeerId(), peerSelected.getClientId());
        if (callChat.isOnHold() || session != null && session.isOnHold()) {
            if (callChat.isOnHold()) {
                callOnHoldText.setText(getString(R.string.call_on_hold));
            } else if (session.isOnHold()) {
                callOnHoldText.setText(getString(R.string.session_on_hold, peerSelected.getName()));
            }
            callOnHoldLayout.setVisibility(View.VISIBLE);
        } else {
            callOnHoldLayout.setVisibility(View.GONE);
        }

        if (cameraFragmentPeerSelected != null) {
            cameraFragmentPeerSelected.showOnHoldImage(peerSelected.getPeerId(), peerSelected.getClientId());
            cameraFragmentPeerSelected.showMuteIcon(peerSelected.getPeerId(), peerSelected.getClientId());
        }
    }
    private void clearHandlers() {
        logDebug("clearHandlers");
        if (handlerArrow1 != null) {
            handlerArrow1.removeCallbacksAndMessages(null);
        }
        if (handlerArrow2 != null) {
            handlerArrow2.removeCallbacksAndMessages(null);
        }
        if (handlerArrow3 != null) {
            handlerArrow3.removeCallbacksAndMessages(null);
        }
        if (handlerArrow4 != null) {
            handlerArrow4.removeCallbacksAndMessages(null);
        }
        if (handlerArrow5 != null) {
            handlerArrow5.removeCallbacksAndMessages(null);
        }
        if (handlerArrow6 != null) {
            handlerArrow6.removeCallbacksAndMessages(null);
        }
        activateChrono(false, callInProgressChrono, callChat);
    }

    /**
     * Method for obtaining the name of a participant.
     *
     * @param peerid Peer ID.
     * @return The name of this participant.
     */
    private String getName(long peerid) {
        if (peerid == megaChatApi.getMyUserHandle()) {
            return megaChatApi.getMyFullname();
        }

        return chatC.getParticipantFirstName(peerid);
    }

    /**
     * Method for obtaining the mail of a participant.
     *
     * @param peerid Peer ID.
     * @return The mail of this participant.
     */
    private String getEmail(long peerid) {
        return getUserMailCall(chat, peerid);
    }

    public void showSnackbar(String s) {
        logDebug("showSnackbar: " + s);
        showSnackbar(fragmentContainer, s);
    }

    /**
     * Hide or show the muted call bar.
     *
     * @param option True, if it must be shown. False, if it must be hidden.
     */
    private void checkMutateOwnCallLayout(int option) {
        if ( mutateOwnCallLayout == null || mutateOwnCallLayout.getVisibility() == option)
            return;

        mutateOwnCallLayout.setVisibility(option);
    }

    /**
     * Method for updating the own muted call bar.
     */
    public void refreshOwnMicro() {
        if (chat.isGroup() || getCall() == null){
            checkMutateOwnCallLayout(View.GONE);
            return;
        }

        boolean shoudShown = !callChat.isOnHold() && !isSessionOnHold(chat.getChatId()) && !callChat.hasLocalAudio();

        if (!shoudShown || mutateContactCallLayout.getVisibility() == View.VISIBLE) {
            checkMutateOwnCallLayout(View.GONE);
            return;
        }
        checkMutateOwnCallLayout(View.VISIBLE);
    }

    /**
     * Method to get the chat ID of this call.
     *
     * @return The chat ID.
     */
    public long getCurrentChatid() {
        return chatId;
    }

    /**
     * Method for updating the call.
     *
     * @param callId Call ID.
     */
    private void updateCall(long callId) {
        this.callChat = megaChatApi.getChatCallByCallId(callId);
    }

    /**
     * Method to get the call.
     *
     * @return The current call.
     */
    public MegaChatCall getCall() {
        if (megaChatApi == null) return null;
        return callChat = megaChatApi.getChatCall(chatId);
    }

    /**
     * Method to get number of participants.
     *
     * @return number of paticipants.
     */
    public int getNumParticipants() {
        if(peersOnCall == null)
            return (int) MEGACHAT_INVALID_HANDLE;

        return peersOnCall.size();
    }

    /**
     * Method to get the session with a participant in a group call.
     *
     * @param peerid   Peer ID of this session.
     * @param clientid Client ID of this session.
     * @return The session with this participant.
     */
    public MegaChatSession getSessionCall(long peerid, long clientid) {
        if(callChat == null)
            return null;

        return callChat.getMegaChatSession(peerid, clientid);
    }

    /**
     * Method to know if the call is in progress.
     *
     * @param callStatus The current call status.
     * @return True if it is. False if it is not.
     */
    private boolean statusCallInProgress(int callStatus) {
        return callStatus != MegaChatCall.CALL_STATUS_RING_IN &&
                (callStatus < MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION ||
                        callStatus > MegaChatCall.CALL_STATUS_USER_NO_PRESENT);
    }

    /**
     * Check whether or not there are calls on hold in other chats.
     */
    private void checkAnotherCallOnHold() {
        MegaChatCall callOnHold = getAnotherCallOnHold(callChat.getId());

        updateOnHoldFabButton(callOnHold);
        updateAnotherCallOnHoldBar(callChat.getChatid());
    }

    /**
     * Check for changes in call composition received in CHANGE_TYPE_CALL_COMPOSITION.
     *
     * @param typeChange       Indicates if the change is TYPE_JOIN, TYPE_LEFT or no change.
     * @param peerIdReceived   Identifier of the user to whom the change is applied.
     * @param clientIdReceived Identifier of the client to whom the change is applied.
     */
    private void checkCompositionChanges(int typeChange, long peerIdReceived, long clientIdReceived) {
        if (getCall() == null || !chat.isGroup() || reconnectingLayout.isShown())
            return;

        logDebug("The type of changes in composition is " + typeChange);
        if (typeChange == TYPE_JOIN) {
            userJoined(peerIdReceived, clientIdReceived);
        } else if (typeChange == TYPE_LEFT) {
            userLeft(peerIdReceived, clientIdReceived);
        } else {
            return;
        }

        updateSubTitle();
        updateSubtitleNumberOfVideos();
        checkTypeCall();
    }

    /**
     * Perform the necessary actions when the status of the call is CALL_STATUS_REQUEST_SENT or CALL_STATUS_RING_IN.
     */
    private void checkOutgoingOrIncomingCall() {
        if (chat.isGroup()) {
            checkCurrentParticipants();
            updateSubTitle();
        } else {
            updateSubtitleNumberOfVideos();
        }
    }

    /**
     * Perform the necessary actions when the status of the call is CALL_STATUS_IN_PROGRESS.
     */
    private void checkInProgressCall() {
        if (getCall() == null)
            return;

        if (reconnectingLayout.isShown()) {
            hideReconnecting();
            checkCurrentParticipants();
            updateSubTitle();
            return;
        }

        if (!chat.isGroup()) {
            createSmallFragment();
            createFullScreenFragment();
            updateAVFlags(getSessionIndividualCall(callChat));
        }

        answerCallFAB.setOnTouchListener(null);
        videoFAB.setOnTouchListener(null);
        videoFAB.setOnClickListener(this);

        showInitialFABConfiguration();
        updateSubtitleNumberOfVideos();

        updateLocalSpeakerStatus();
        stopCountDownTimer();

        countDownTimer = new CountDownTimer(TIMEOUT, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                remoteCameraClick();
            }
        }.start();
    }

    /**
     * Perform the necessary actions when the status of the session is SESSION_STATUS_DESTROYED and the term code is TERM_CODE_USER_HANGUP.
     */
    private void checkHangCall(long callId){
        MegaChatCall call = megaChatApi.getChatCallByCallId(callId);
        if(call != null && statusCallInProgress(call.getStatus()) && call.getStatus() != MegaChatCall.CALL_STATUS_RECONNECTING){
            return;
        }

        checkTerminatingCall();
    }

    /**
     * Perform the necessary actions when the call is over.
     */
    private void checkTerminatingCall() {
        ArrayList<Long> calls = getCallsParticipating();
        MegaApplication.setCallLayoutStatus(chatId, false);

        if(calls == null || calls.isEmpty()){
            clearHandlers();
            MegaApplication.setSpeakerStatus(chatId, false);
            finishActivity();
        }else{
            for(Long chatId:calls) {
                MegaApplication.setSpeakerStatus(chatId, false);
                MegaApplication.setShowPinScreen(false);
                Intent intentOpenCall = new Intent(this, ChatCallActivity.class);
                intentOpenCall.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intentOpenCall.putExtra(CHAT_ID, chatId);
                startActivity(intentOpenCall);
                break;
            }
        }
    }

    /**
     * Perform the necessary actions when the status of the call is CALL_STATUS_USER_NO_PRESENT.
     */
    private void checkUserNoPresentInCall() {
        if (getCall() == null || reconnectingLayout.isShown())
            return;

        clearHandlers();
    }

    /**
     * Perform the necessary actions when the status of the call is CALL_STATUS_RECONNECTING.
     */
    private void checkReconnectingCall() {
        if (getCall() == null || (chat.isGroup() && callChat.getStatus() != MegaChatCall.CALL_STATUS_RECONNECTING) || reconnectingLayout.isShown())
            return;

        activateChrono(false, callInProgressChrono, callChat);
        subtitleToobar.setVisibility(View.GONE);
        showReconnecting();
    }

    /**
     * Method to know if the call is audio only.
     *
     * @return True if it is an audio call. False if it is a video call.
     */
    public boolean isIndividualAudioCall() {
        if (chat.isGroup())
            return true;

        MegaChatSession session = getSessionIndividualCall(callChat);
        if (session != null && session.isOnHold() && MegaApplication.wasLocalVideoEnable())
            return false;

        return session == null || (!callChat.hasLocalVideo() && !session.hasVideo());
    }

    /**
     * Make the necessary actions when the session is on hold in group calls.
     *
     * @param peerid   Peer ID.
     * @param clientid Client ID.
     */
    private void checkSessionOnHold(long peerid, long clientid) {
        if (getCall() == null)
            return;

        if (lessThanSevenParticipants() && adapterGrid != null) {
            adapterGrid.updateSessionOnHold(peerid, clientid);
        } else if (!lessThanSevenParticipants() && adapterList != null) {
            adapterList.updateSessionOnHold(peerid, clientid);
            if (peerSelected != null && peerid == peerSelected.getPeerId() && clientid == peerSelected.getClientId()) {
                updateParticipantSelectedInCallOnHold();
            }
        }
    }

    /**
     * Perform the necessary actions when the call is on hold.
     */
    private void checkCallOnHold() {
        if (getCall() == null)
            return;

        updateLocalVideoStatus();

        if (chat.isGroup()) {
            if (callChat.isOnHold()) {
                callOnHoldText.setText(getString(R.string.call_on_hold));
                callOnHoldLayout.setVisibility(View.VISIBLE);
            } else {
                callOnHoldLayout.setVisibility(View.GONE);
            }

            if (lessThanSevenParticipants() && adapterGrid != null) {
                adapterGrid.updateCallOnHold();
            } else if (!lessThanSevenParticipants() && adapterList != null) {
                adapterList.updateCallOnHold();
                if (peerSelected != null) {
                    updateParticipantSelectedStatus();
                }
            }
        } else {
            MegaChatSession session = getSessionIndividualCall(callChat);
            if (session != null) {
                updateRemoteVideoStatus(session);
            }

            if (callChat.isOnHold() || isSessionOnHold(chat.getChatId())) {
                callOnHoldText.setText(getString(R.string.call_on_hold));
                callOnHoldLayout.setVisibility(View.VISIBLE);
                checkMutateOwnCallLayout(View.GONE);

                if (mutateContactCallLayout.getVisibility() == View.VISIBLE) {
                    mutateContactCallLayout.setVisibility(View.GONE);
                }
            } else {
                callOnHoldLayout.setVisibility(View.GONE);
                refreshContactMicro(session);
            }
        }

        updateOnHoldFABStatus();
        invalidateOptionsMenu();
    }

    /**
     * Check for changes in the network quality received in CHANGE_TYPE_SESSION_NETWORK_QUALITY.
     *
     * @param session of a user in which the change has occurred.
     */
    private void checkNetworkQuality(MegaChatSession session) {
        if (!chat.isGroup() || peersOnCall.isEmpty() || session == null || session.getStatus() != MegaChatSession.SESSION_STATUS_IN_PROGRESS)
            return;

        logDebug("Network quality changed");
        int qualityLevel = session.getNetworkQuality();
        for (int i = 0; i < peersOnCall.size(); i++) {
            if (peersOnCall.get(i).getPeerId() == session.getPeerid() && peersOnCall.get(i).getClientId() == session.getClientid()) {
                if (qualityLevel == 0 && peersOnCall.get(i).isGoodQuality()) {
                    //Bad quality
                    peersOnCall.get(i).setGoodQuality(false);
                }

                if (qualityLevel > 0 && !peersOnCall.get(i).isGoodQuality()) {
                    //Good quality
                    peersOnCall.get(i).setGoodQuality(true);
                }

                if (lessThanSevenParticipants() && adapterGrid != null) {
                    adapterGrid.updateParticipantQuality(i);
                } else if (!lessThanSevenParticipants() && adapterList != null) {
                    adapterList.updateParticipantQuality(i);
                } else {
                    updateUI();
                }
                break;
            }
        }

        int participantsWithPoorConnection = 1;
        int totalParticipants = peersOnCall.size();
        for (InfoPeerGroupCall participant : peersOnCall) {
            if ((participant.getPeerId() != megaChatApi.getMyUserHandle() || participant.getClientId() != megaChatApi.getMyClientidHandle(chatId)) && !participant.isGoodQuality()) {
                participantsWithPoorConnection++;
            }
        }

        if (participantsWithPoorConnection == totalParticipants) {
            if (reconnectingLayout.getVisibility() != View.VISIBLE) {
                reconnectingLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.reconnecting_bar));
                reconnectingText.setText(getString(R.string.poor_internet_connection_message));
                reconnectingLayout.setVisibility(View.VISIBLE);
                reconnectingLayout.setAlpha(1);
            }
        } else if (reconnectingLayout.getVisibility() == View.VISIBLE && reconnectingText.getText().equals(getString(R.string.poor_internet_connection_message))) {
            reconnectingLayout.setVisibility(View.GONE);
        }
    }

    /**
     * Check for changes in the audio level received in CHANGE_TYPE_SESSION_AUDIO_LEVEL.
     *
     * @param session of a user in which the change has occurred.
     */
    private void checkAudioLevel(MegaChatSession session) {
        if (!chat.isGroup() || peersOnCall.isEmpty() || lessThanSevenParticipants() || isManualMode || session == null || !session.getAudioDetected())
            return;

        for (InfoPeerGroupCall peer: peersOnCall) {
            int position = peersOnCall.indexOf(peer);
            if (peersOnCall.get(position).getPeerId() == session.getPeerid() && peersOnCall.get(position).getClientId() == session.getClientid()) {
                peerSelected = peer;
                updateParticipantSelected();
                break;
            }
        }
    }

    /**
     * Check the current participants in the call and update UI.
     */
    private void checkCurrentParticipants() {
        if (megaChatApi == null || getCall() == null || peersOnCall == null || callChat.getPeeridParticipants() == null)
            return;
        logDebug("Checking the current participants in the call. Call status: " + callStatusToString(callChat.getStatus()));
        boolean changes = false;

        if (!peersOnCall.isEmpty()) peersOnCall.clear();

        //Check the participants to be added
        for (int i = 0; i < callChat.getPeeridParticipants().size(); i++) {
            boolean peerContain = false;
            long userPeerid = callChat.getPeeridParticipants().get(i);
            long userClientid = callChat.getClientidParticipants().get(i);

            for (InfoPeerGroupCall peer : peersOnCall) {
                if (peer.getPeerId() == userPeerid && peer.getClientId() == userClientid) {
                    peerContain = true;
                    break;
                }
            }

            if (!peerContain) {
                addContactIntoArray(userPeerid, userClientid);
                changes = true;
            }
        }

        if (changes) updateUI();

        if (peersOnCall.isEmpty() || !statusCallInProgress(callChat.getStatus())) return;

        logDebug("Update Video&Audio local&remote");
        updateSubTitle();

        for (int i = 0; i < peersOnCall.size(); i++) {
            if (isItMe(chatId, peersOnCall.get(i).getPeerId(), peersOnCall.get(i).getClientId())) {
                updateLocalAV();
            } else {
                updateRemoteAV(callChat.getMegaChatSession(peersOnCall.get(i).getPeerId(), peersOnCall.get(i).getClientId()));
            }
        }
    }


    /**
     * Check when the number of participants changes in the call and update UI.
     */
    private void checkParticipantChanges(boolean isAdded, int posRemoved, int posInserted) {
        logDebug("Checking for changes in the number of participants");
        if ((lessThanSevenParticipants() && adapterGrid == null) ||
                (lessThanSevenParticipants() && !isAdded && peersOnCall.size() == MAX_PARTICIPANTS_GRID) ||
                (!lessThanSevenParticipants() && adapterList == null) ||
                (!lessThanSevenParticipants() && isAdded && peersOnCall.size() == MIN_PEERS_LIST)) {
            updateUI();
            return;
        }

        if (lessThanSevenParticipants()) {
            recyclerView.getRecycledViewPool().clear();
            if (peersOnCall.size() < NECESSARY_CHANGE_OF_SIZES) {
                if (isAdded) {
                    adapterGrid.notifyItemInserted(posInserted);
                } else {
                    adapterGrid.notifyItemRemoved(posRemoved);
                }
                adapterGrid.notifyDataSetChanged();

            } else if (peersOnCall.size() == NECESSARY_CHANGE_OF_SIZES) {
                if (isAdded) {
                    adapterGrid.notifyItemInserted(posInserted);
                    adapterGrid.notifyDataSetChanged();

                } else {
                    adapterGrid.notifyItemRemoved(posRemoved);
                    adapterGrid.notifyItemRangeChanged(posRemoved, peersOnCall.size());
                }
            } else {
                int rangeToUpdate;
                if (isAdded) {
                    adapterGrid.notifyItemInserted(posInserted);
                    rangeToUpdate = posInserted - 1;
                } else {
                    adapterGrid.notifyItemRemoved(posRemoved);
                    rangeToUpdate = posRemoved;
                }
                adapterGrid.notifyItemRangeChanged(rangeToUpdate, peersOnCall.size());
            }

            updateRecyclerView();
            adapterGrid.updateAvatarsPosition();
        } else {
            int posUpdated;
            bigRecyclerView.getRecycledViewPool().clear();
            if (isAdded) {
                posUpdated = posInserted - 1;
                adapterList.notifyItemInserted(posUpdated);
            } else {
                posUpdated = posRemoved;
                adapterList.notifyItemRemoved(posUpdated);
            }
            adapterList.notifyItemRangeChanged(posUpdated, peersOnCall.size());
            adapterList.updateAvatarsPosition();
            updateParticipantSelected();
        }
    }

    /**
     * Method for updating the margins and number of columns of the recycler view.
     */
    private void updateRecyclerView() {
        int height = getActionBarHeight(this);
        int marginTop = height;
        if (peersOnCall.size() < NECESSARY_CHANGE_OF_SIZES) {
            marginTop = 0;
        } else if (peersOnCall.size() == NECESSARY_CHANGE_OF_SIZES || peersOnCall.size() == 4) {
            marginTop = height + px2dp(60, getOutMetrics());
        }

        recyclerViewLayout.setPadding(0, marginTop, 0, 0);
        if (peersOnCall.size() < NECESSARY_CHANGE_OF_SIZES) {
            recyclerView.setColumnWidth((int) widthScreenPX);
        } else {
            recyclerView.setColumnWidth((int) widthScreenPX / 2);
        }
    }

    /**
     *  Update the adapter depends of the number of participants.
     */
    private void updateUI() {
        if (getCall() == null) return;
        if (peersOnCall.isEmpty()) {
            clearViews();
            return;
        }
        logDebug("Updating the UI, number of participants = " + peersOnCall.size());
        if (!statusCallInProgress(callChat.getStatus())) {
            linearParticipants.setVisibility(View.GONE);
        }

        if (lessThanSevenParticipants()) {
            destroyAdapter();
            removePeerSelectedFragment();

            peerSelectedCameraLayout.setVisibility(View.GONE);
            bigRecyclerView.setAdapter(null);
            bigRecyclerView.setVisibility(View.GONE);
            bigRecyclerViewLayout.setVisibility(View.GONE);
            recyclerViewLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.VISIBLE);

            updateRecyclerView();

            if (adapterGrid == null) {
                logDebug("Need to create the adapter");
                recyclerView.setAdapter(null);
                adapterGrid = new GroupCallAdapter(this, recyclerView, peersOnCall, chatId);
                recyclerView.setAdapter(adapterGrid);
            } else {
                logDebug("Notify of changes");
                recyclerView.getRecycledViewPool().clear();
                adapterGrid.notifyDataSetChanged();
                adapterGrid.updateAvatarsPosition();
            }

            if (statusCallInProgress(callChat.getStatus())) {
                adapterGrid.updateMuteIcon();
            }
        } else {

            destroyAdapter();
            recyclerView.setAdapter(null);
            recyclerView.setVisibility(View.GONE);
            recyclerViewLayout.setVisibility(View.GONE);
            bigRecyclerViewLayout.setVisibility(View.VISIBLE);
            bigRecyclerView.setVisibility(View.VISIBLE);

            if (adapterList == null) {
                logDebug("Need to create the adapter");
                bigRecyclerView.setAdapter(null);
                adapterList = new GroupCallAdapter(this, bigRecyclerView, peersOnCall, chatId);
                bigRecyclerView.setAdapter(adapterList);
            } else {
                logDebug("Notify of changes");
                bigRecyclerView.getRecycledViewPool().clear();
                adapterList.notifyDataSetChanged();
                adapterList.updateAvatarsPosition();
            }

            if (statusCallInProgress(callChat.getStatus())) {
                adapterList.updateMuteIcon();
            }

            updateParticipantSelected();
        }
    }

    /**
     * Method for updating the UI when a user joined in the group call.
     *
     * @param userPeerId   Peer ID.
     * @param userClientId Client ID.
     */
    private void userJoined(long userPeerId, long userClientId) {
        logDebug("Participant joined the group call");

        if (peersOnCall.isEmpty()) {
            addContactIntoArray(userPeerId, userClientId);
            return;
        }

        boolean containsUser = false;
        for (InfoPeerGroupCall peer : peersOnCall) {
            if (peer.getPeerId() == userPeerId && peer.getClientId() == userClientId) {
                containsUser = true;
                break;
            }
        }

        if (!containsUser) {
            addContactIntoArray(userPeerId, userClientId);
            int callStatus = callChat.getStatus();
            if ((statusCallInProgress(callStatus) && callStatus != MegaChatCall.CALL_STATUS_RECONNECTING) && (userPeerId != megaChatApi.getMyUserHandle() || userClientId != megaChatApi.getMyClientidHandle(chatId))) {
                updateInfoUsersBar(getString(R.string.contact_joined_the_call, getName(userPeerId)));
            }

            checkParticipantChanges(true, -1, (peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1)));

            if (statusCallInProgress(callStatus)) {
                updateRemoteAV(callChat.getMegaChatSession(userPeerId, userClientId));
            }
        }
    }

    /**
     * Method for updating the UI when a user leaves the group call.
     *
     * @param userPeerId   Peer ID.
     * @param userClientId Client ID.
     */
    private void userLeft(long userPeerId, long userClientId) {
        logDebug("Participant left the group call");

        if (peersOnCall.isEmpty())
            return;

        for (InfoPeerGroupCall peer : peersOnCall) {
            if (peer.getPeerId() == userPeerId && peer.getClientId() == userClientId) {
                int callStatus = callChat.getStatus();

                if ((statusCallInProgress(callStatus) && callStatus != MegaChatCall.CALL_STATUS_RECONNECTING) && (userPeerId != megaChatApi.getMyUserHandle() || userClientId != megaChatApi.getMyClientidHandle(chatId))) {
                    updateInfoUsersBar(getString(R.string.contact_left_the_call, getName(userPeerId)));
                }

                int position = peersOnCall.indexOf(peer);
                removeContact(peer);
                checkParticipantChanges(false, position, -1);
                break;

            }
        }
    }

    /**
     * Method for adding a participant to the array.
     *
     * @param userPeerid   Peer ID.
     * @param userClientid Client ID.
     */
    private void addContactIntoArray(long userPeerid, long userClientid) {
        if (getCall() == null) return;

        if (isItMe(chatId, userPeerid, userClientid)) {
            InfoPeerGroupCall myPeer = new InfoPeerGroupCall(userPeerid, userClientid, getName(userPeerid), getEmail(userPeerid), callChat.hasLocalVideo(), callChat.hasLocalAudio(), false, true, null);
            peersOnCall.add(myPeer);
            logDebug("I've been added to the array");
        } else {
            InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userPeerid, userClientid, getName(userPeerid), getEmail(userPeerid));
            peersOnCall.add((peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1)), userPeer);
            logDebug("Participant has been added to the array");
        }

        checkAudioLevelMonitor();
    }

    /**
     * Method for Removing a Participant from the Array.
     *
     * @param peer The participant.
     */
    private void removeContact(InfoPeerGroupCall peer) {
        if (isItMe(chatId, peer.getPeerId(), peer.getClientId()))
            return;

        logDebug("Participant has been removed from the array");
        peersOnCall.remove(peer);
        checkAudioLevelMonitor();
    }

    /**
     * Method to enable or disable the audio level monitor.
     */
    private void checkAudioLevelMonitor() {
        if (peersOnCall.size() >= MIN_PEERS_LIST) {
            if (!megaChatApi.isAudioLevelMonitorEnabled(chatId)) {
                megaChatApi.enableAudioLevelMonitor(true, chatId);
            }
        } else if (megaChatApi.isAudioLevelMonitorEnabled(chatId)) {
            megaChatApi.enableAudioLevelMonitor(false, chatId);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                app.muteOrUnmute(false);
                return false;

            case KeyEvent.KEYCODE_VOLUME_DOWN:
                app.muteOrUnmute(true);
                return false;

            default:
                return super.dispatchKeyEvent(event);
        }
    }

    /**
     * Stop the countdown timer.
     */
    private void stopCountDownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }
}
