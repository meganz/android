package mega.privacy.android.app.lollipop.megachat.calls;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
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
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView;
import mega.privacy.android.app.components.OnSwipeTouchListener;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.fcm.IncomingCallService;
import mega.privacy.android.app.interfaces.OnProximitySensorListener;
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.CallNonContactNameListener;
import mega.privacy.android.app.lollipop.megachat.AppRTCAudioManager;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.GroupCallAdapter;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatCallListenerInterface;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatSession;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.IncomingCallNotification.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.VideoCaptureUtils.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;

public class ChatCallActivity extends BaseActivity implements MegaChatRequestListenerInterface, MegaChatCallListenerInterface, MegaRequestListenerInterface, View.OnClickListener, KeyEvent.Callback {

    final private static int REMOTE_VIDEO_NOT_INIT = -1;
    final private static int REMOTE_VIDEO_ENABLED = 1;
    final private static int REMOTE_VIDEO_DISABLED = 0;
    final private static int BIG_LETTER_SIZE = 60;
    final private static int SMALL_LETTER_SIZE = 40;
    final private static int MIN_PEERS_LIST = 7;
    final private static int MAX_PEERS_GRID = 6;
    final private static int ARROW_ANIMATION = 250;
    final private static int INFO_ANIMATION = 4000;
    final private static int MOVE_ANIMATION = 500;
    final private static int ALPHA_ANIMATION = 600;
    final private static int ALPHA_ARROW_ANIMATION = 1000;
    final private static int NECESSARY_CHANGE_OF_SIZES = 4;
    private final static int TITLE_TOOLBAR = 250;
    private float widthScreenPX, heightScreenPX;
    private long chatId;
    private MegaChatRoom chat;
    private MegaChatCall callChat;
    private MegaChatApiAndroid megaChatApi = null;
    private Display display;
    private DisplayMetrics outMetrics;
    private Toolbar tB;
    private EmojiTextView titleToolbar;
    private TextView subtitleToobar;
    private Chronometer callInProgressChrono;
    private RelativeLayout mutateContactCallLayout;
    private EmojiTextView mutateCallText;
    private RelativeLayout mutateOwnCallLayout;
    private LinearLayout linearParticipants;
    private TextView participantText;
    private EmojiTextView infoUsersBar;
    private RelativeLayout reconnectingLayout;
    private TextView reconnectingText;
    private ActionBar aB;
    private boolean avatarRequested = false;
    private ArrayList<InfoPeerGroupCall> peersOnCall = new ArrayList<>();
    private ArrayList<InfoPeerGroupCall> peersBeforeCall = new ArrayList<>();
    private RelativeLayout smallElementsIndividualCallLayout;
    private RelativeLayout bigElementsIndividualCallLayout;
    private RelativeLayout bigElementsGroupCallLayout;
    private RelativeLayout recyclerViewLayout;
    private CustomizedGridCallRecyclerView recyclerView;
    private RelativeLayout bigRecyclerViewLayout;
    private LinearLayoutManager layoutManager;
    private RecyclerView bigRecyclerView;
    private GroupCallAdapter adapterGrid;
    private GroupCallAdapter adapterList;
    private int isRemoteVideo = REMOTE_VIDEO_NOT_INIT;
    private RelativeLayout myAvatarLayout;
    private RoundedImageView myImage;
    private RelativeLayout contactAvatarLayout;
    private RoundedImageView contactImage;
    private RelativeLayout fragmentContainer;
    private int totalVideosAllowed = 0;
    private FloatingActionButton videoFAB;
    private FloatingActionButton microFAB;
    private FloatingActionButton rejectFAB;
    private FloatingActionButton hangFAB;
    private FloatingActionButton speakerFAB;
    private FloatingActionButton answerCallFAB;
    private boolean notYetJoinedTheCall = true;
    private FrameLayout fragmentContainerLocalCamera;
    private FrameLayout fragmentContainerLocalCameraFS;
    private FrameLayout fragmentContainerRemoteCameraFS;
    private ViewGroup parentLocal;
    private ViewGroup parentLocalFS;
    private ViewGroup parentRemoteFS;
    private FrameLayout fragmentBigCameraGroupCall;
    private ImageView microFragmentBigCameraGroupCall;
    private ViewGroup parentBigCameraGroupCall;
    private RelativeLayout avatarBigCameraGroupCallLayout;
    private ImageView avatarBigCameraGroupCallMicro;
    private RoundedImageView avatarBigCameraGroupCallImage;
    private EmojiTextView avatarBigCameraGroupCallInitialLetter;
    private AppRTCAudioManager rtcAudioManager = null;
    private Animation shake;

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
    private InfoPeerGroupCall peerSelected = null;
    private boolean isManualMode = false;
    private int statusBarHeight = 0;
    private MegaApiAndroid megaApi = null;
    private Handler handlerArrow1, handlerArrow2, handlerArrow3, handlerArrow4, handlerArrow5, handlerArrow6;
    private LocalCameraCallFragment localCameraFragment = null;
    private LocalCameraCallFullScreenFragment localCameraFragmentFS = null;
    private RemoteCameraCallFullScreenFragment remoteCameraFragmentFS = null;
    private BigCameraGroupCallFragment bigCameraGroupCallFragment = null;
    private MenuItem cameraSwapMenuItem;
    private MegaApplication application =  MegaApplication.getInstance();
    private boolean inTemporaryState = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        logDebug("onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.call_action, menu);
        cameraSwapMenuItem = menu.findItem(R.id.cab_menu_camera_swap);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(isNecessaryToShowSwapCameraOption()){
            cameraSwapMenuItem.setVisible(true);
            if(callChat.hasLocalVideo()){
                cameraSwapMenuItem.setEnabled(true);
                cameraSwapMenuItem.setIcon(mutateIcon(this, R.drawable.ic_camera_swap, R.color.background_chat));
            }else{
                cameraSwapMenuItem.setEnabled(false);
                cameraSwapMenuItem.setIcon(mutateIcon(this, R.drawable.ic_camera_swap, R.color.white_50_opacity));
            }
        }else{
            cameraSwapMenuItem.setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private boolean isNecessaryToShowSwapCameraOption(){
        if(callChat == null) return false;
        int callStatus = callChat.getStatus();
        if(callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN || callStatus < MegaChatCall.CALL_STATUS_HAS_LOCAL_STREAM || (callStatus > MegaChatCall.CALL_STATUS_IN_PROGRESS && callStatus != MegaChatCall.CALL_STATUS_RECONNECTING)) return false;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        logDebug("onOptionsItemSelected");
        application.sendSignalPresenceActivity();
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

    private void clearArrayList(ArrayList<InfoPeerGroupCall> array) {
        if (array == null || array.isEmpty()) return;
        array.clear();
    }

    private void clearArrays() {
        clearArrayList(peersBeforeCall);
        clearArrayList(peersOnCall);
    }

    private void updateScreenStatus() {
        logDebug("updateScreenStatus:");
        if (chatId == -1 || megaChatApi == null || getCall() == null) return;
        chat = megaChatApi.getChatRoom(chatId);
        int callStatus = callChat.getStatus();

        if (chat.isGroup()) {
            logDebug("Group");
            clearArrayList(peersBeforeCall);

            if (callStatus == MegaChatCall.CALL_STATUS_RING_IN) {
                displayLinearFAB(true);
            } else if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS || callStatus == MegaChatCall.CALL_STATUS_JOINING || callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT || callStatus == MegaChatCall.CALL_STATUS_RECONNECTING) {
                displayLinearFAB(false);
                if (callStatus == MegaChatCall.CALL_STATUS_RECONNECTING && !reconnectingLayout.isShown()) {
                    showReconnecting();
                }
            }

            checkParticipants();
            updateSubtitleNumberOfVideos();

        } else {
            logDebug("Individual");

            if (callStatus == MegaChatCall.CALL_STATUS_RING_IN || callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS || callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT || callStatus == MegaChatCall.CALL_STATUS_RECONNECTING) {
                if (callStatus == MegaChatCall.CALL_STATUS_RING_IN) {
                    displayLinearFAB(true);
                } else {
                    displayLinearFAB(false);
                }
                setProfileAvatar(megaChatApi.getMyUserHandle());
                setProfileAvatar(chat.getPeerHandle(0));
                if (callStatus == MegaChatCall.CALL_STATUS_RECONNECTING && !reconnectingLayout.isShown()) {
                    showReconnecting();
                }
            }
            updateLocalAV();
            updateRemoteAV(-1, -1);
        }

        updateSubTitle();
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        logDebug("onNewIntent");

        Bundle extras = intent.getExtras();
        logDebug("Action: " + getIntent().getAction());
        if (extras != null) {
            long newChatId = extras.getLong(CHAT_ID, -1);
            if (megaChatApi == null) return;

            if (chatId != -1 && chatId == newChatId) {
                logDebug("Same call");
                chat = megaChatApi.getChatRoom(chatId);
                updateScreenStatus();
                updateLocalSpeakerStatus();

            } else {
                if (newChatId == -1) return;

                logDebug("Different call");
                chatId = newChatId;
                chat = megaChatApi.getChatRoom(chatId);
                callChat = megaChatApi.getChatCall(chatId);
                titleToolbar.setText(chat.getTitle());
                updateScreenStatus();
                updateLocalSpeakerStatus();

                if (getCall() == null || callChat.getStatus() != MegaChatCall.CALL_STATUS_IN_PROGRESS)
                    return;

                logDebug("Start call Service");
                Intent intentService = new Intent(this, CallService.class);
                intentService.putExtra(CHAT_ID, callChat.getChatid());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.startForegroundService(intentService);
                } else {
                    this.startService(intentService);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logDebug("onCreate");
        super.onCreate(savedInstanceState);
        cancelIncomingCallNotification(this);
        setContentView(R.layout.activity_calls_chat);
        application.setShowPinScreen(true);

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        widthScreenPX = outMetrics.widthPixels;
        heightScreenPX = outMetrics.heightPixels - statusBarHeight;

        if (megaApi == null) {
            megaApi = application.getMegaApi();
        }
        if (megaApi != null) {
            megaApi.retryPendingConnections();
        }
        if (megaChatApi == null) {
            megaChatApi = application.getMegaChatApi();
        }
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
        megaChatApi.addChatCallListener(this);

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
        titleToolbar.setMaxWidthEmojis(px2dp(TITLE_TOOLBAR, outMetrics));

        subtitleToobar = tB.findViewById(R.id.subtitle_toolbar);
        callInProgressChrono = tB.findViewById(R.id.simple_chronometer);
        linearParticipants = tB.findViewById(R.id.ll_participants);
        participantText = tB.findViewById(R.id.participants_text);
        linearParticipants.setVisibility(View.GONE);
        totalVideosAllowed = megaChatApi.getMaxVideoCallParticipants();
        mutateOwnCallLayout = findViewById(R.id.mutate_own_call);
        mutateOwnCallLayout.setVisibility(View.GONE);
        mutateContactCallLayout = findViewById(R.id.mutate_contact_call);
        mutateContactCallLayout.setVisibility(View.GONE);
        mutateCallText = findViewById(R.id.text_mutate_contact_call);
        smallElementsIndividualCallLayout = findViewById(R.id.small_elements_individual_call);
        smallElementsIndividualCallLayout.setVisibility(View.GONE);
        bigElementsIndividualCallLayout = findViewById(R.id.big_elements_individual_call);
        bigElementsIndividualCallLayout.setVisibility(View.GONE);
        linearFAB = findViewById(R.id.linear_buttons);
        displayLinearFAB(false);
        infoUsersBar = findViewById(R.id.info_users_bar);
        infoUsersBar.setVisibility(View.GONE);
        reconnectingLayout = findViewById(R.id.reconnecting_layout);
        reconnectingLayout.setVisibility(View.GONE);
        reconnectingText = findViewById(R.id.reconnecting_text);

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
        answerCallFAB.hide();
        videoFAB = findViewById(R.id.video_fab);
        videoFAB.setOnClickListener(this);
        videoFAB.hide();
        rejectFAB = findViewById(R.id.reject_fab);
        rejectFAB.setOnClickListener(this);
        rejectFAB.hide();
        speakerFAB = findViewById(R.id.speaker_fab);
        speakerFAB.setOnClickListener(this);
        speakerFAB.hide();
        microFAB = findViewById(R.id.micro_fab);
        microFAB.setOnClickListener(this);
        microFAB.hide();
        hangFAB = findViewById(R.id.hang_fab);
        hangFAB.setOnClickListener(this);
        hangFAB.hide();
        shake = AnimationUtils.loadAnimation(this, R.anim.shake);

        //Cameras in Group call
        bigElementsGroupCallLayout = findViewById(R.id.big_elements_group_call);
        bigElementsGroupCallLayout.setVisibility(View.GONE);

        //Recycler View for 1-6 peers
        recyclerViewLayout = findViewById(R.id.rl_recycler_view);
        recyclerViewLayout.setVisibility(View.GONE);
        recyclerView = findViewById(R.id.recycler_view_cameras);
        recyclerView.setPadding(0, 0, 0, 0);
        recyclerView.setColumnWidth((int) widthScreenPX);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setVisibility(View.GONE);

        //Big elements group calls
        parentBigCameraGroupCall = findViewById(R.id.parent_layout_big_camera_group_call);
        ViewGroup.LayoutParams paramsBigCameraGroupCall = parentBigCameraGroupCall.getLayoutParams();
        if (widthScreenPX < heightScreenPX) {
            paramsBigCameraGroupCall.width = (int) widthScreenPX;
            paramsBigCameraGroupCall.height = (int) widthScreenPX;
        } else {
            paramsBigCameraGroupCall.width = (int) heightScreenPX;
            paramsBigCameraGroupCall.height = (int) heightScreenPX;
        }

        parentBigCameraGroupCall.setLayoutParams(paramsBigCameraGroupCall);
        parentBigCameraGroupCall.setOnClickListener(this);

        fragmentBigCameraGroupCall = findViewById(R.id.fragment_big_camera_group_call);
        fragmentBigCameraGroupCall.setVisibility(View.GONE);
        microFragmentBigCameraGroupCall = findViewById(R.id.micro_fragment_big_camera_group_call);
        microFragmentBigCameraGroupCall.setVisibility(View.GONE);

        avatarBigCameraGroupCallLayout = findViewById(R.id.rl_avatar_big_camera_group_call);
        avatarBigCameraGroupCallMicro = findViewById(R.id.micro_avatar_big_camera_group_call);
        avatarBigCameraGroupCallImage = findViewById(R.id.image_big_camera_group_call);
        avatarBigCameraGroupCallInitialLetter = findViewById(R.id.initial_letter_big_camera_group_call);

        avatarBigCameraGroupCallMicro.setVisibility(View.GONE);
        avatarBigCameraGroupCallLayout.setVisibility(View.GONE);
        parentBigCameraGroupCall.setVisibility(View.GONE);

        //Recycler View for 7-8 peers (because 9-10 without video)
        bigRecyclerViewLayout = findViewById(R.id.rl_big_recycler_view);
        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        bigRecyclerView = findViewById(R.id.big_recycler_view_cameras);
        bigRecyclerView.setLayoutManager(layoutManager);
        displayedBigRecyclerViewLayout(true);
        bigRecyclerView.setVisibility(View.GONE);
        bigRecyclerViewLayout.setVisibility(View.GONE);

        //Local camera small
        parentLocal = findViewById(R.id.parent_layout_local_camera);
        fragmentContainerLocalCamera = findViewById(R.id.fragment_container_local_camera);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) fragmentContainerLocalCamera.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        fragmentContainerLocalCamera.setLayoutParams(params);
        fragmentContainerLocalCamera.setOnTouchListener(new OnDragTouchListener(fragmentContainerLocalCamera, parentLocal));
        parentLocal.setVisibility(View.GONE);
        fragmentContainerLocalCamera.setVisibility(View.GONE);

        //Local camera Full Screen
        parentLocalFS = findViewById(R.id.parent_layout_local_camera_FS);
        fragmentContainerLocalCameraFS = findViewById(R.id.fragment_container_local_cameraFS);
        RelativeLayout.LayoutParams paramsFS = (RelativeLayout.LayoutParams) fragmentContainerLocalCameraFS.getLayoutParams();
        paramsFS.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        paramsFS.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        fragmentContainerLocalCameraFS.setLayoutParams(paramsFS);
        parentLocalFS.setVisibility(View.GONE);
        fragmentContainerLocalCameraFS.setVisibility(View.GONE);

        //Remote camera Full Screen
        parentRemoteFS = findViewById(R.id.parent_layout_remote_camera_FS);
        fragmentContainerRemoteCameraFS = findViewById(R.id.fragment_container_remote_cameraFS);
        RelativeLayout.LayoutParams paramsRemoteFS = (RelativeLayout.LayoutParams) fragmentContainerRemoteCameraFS.getLayoutParams();
        paramsRemoteFS.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        paramsRemoteFS.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        fragmentContainerRemoteCameraFS.setLayoutParams(paramsRemoteFS);
        fragmentContainerRemoteCameraFS.setOnTouchListener(new OnDragTouchListener(fragmentContainerRemoteCameraFS, parentRemoteFS));
        parentRemoteFS.setVisibility(View.GONE);
        fragmentContainerRemoteCameraFS.setVisibility(View.GONE);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
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

            myAvatarLayout = findViewById(R.id.call_chat_my_image_rl);
            myAvatarLayout.setVisibility(View.GONE);
            myImage = findViewById(R.id.call_chat_my_image);
            contactAvatarLayout = findViewById(R.id.call_chat_contact_image_rl);
            contactAvatarLayout.setOnClickListener(this);
            contactAvatarLayout.setVisibility(View.GONE);
            contactImage = findViewById(R.id.call_chat_contact_image);
            videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            videoFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_video_off));
            speakerFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            speakerFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_speaker_off));
            microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_off));

            //Contact's avatar
            chatId = extras.getLong(CHAT_ID, -1);
            if (chatId == -1 || megaChatApi == null) return;

            chat = megaChatApi.getChatRoom(chatId);
            callChat = megaChatApi.getChatCall(chatId);
            if (callChat == null) {
                megaChatApi.removeChatCallListener(this);
                finishActivity();
                return;
            }

            logDebug("Start call Service");
            Intent intentService = new Intent(this, CallService.class);
            intentService.putExtra(CHAT_ID, callChat.getChatid());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.startForegroundService(intentService);
            } else {
                this.startService(intentService);
            }
            application.createChatAudioManager();

            int callStatus = callChat.getStatus();
            logDebug("The status of the callChat is: " + callStatusToString(callStatus)+", Chat is a Group: "+chat.isGroup());
            titleToolbar.setText(chat.getTitle());
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

            if (callStatus == MegaChatCall.CALL_STATUS_RING_IN) {
                if (chat.isGroup()) {
                    clearArrays();
                    if (callChat.getPeeridParticipants().size() > 0) {
                        for (int i = 0; i < callChat.getPeeridParticipants().size(); i++) {
                            long userPeerid = callChat.getPeeridParticipants().get(i);
                            long userClientid = callChat.getClientidParticipants().get(i);
                            addContactIntoArray(userPeerid, userClientid);
                        }
                        updatePeers();
                    }

                } else {
                    setAvatarLayout();
                }

            } else if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS || callStatus == MegaChatCall.CALL_STATUS_JOINING || callStatus == MegaChatCall.CALL_STATUS_RECONNECTING) {
                if (!chat.isGroup()) {
                    setAvatarLayout();
                }

                updateScreenStatus();
                updateLocalSpeakerStatus();

            } else if (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                updateLocalSpeakerStatus();
                if (chat.isGroup()) {
                    clearArrays();
                    addMeIntoArray();
                    updatePeers();

                } else {
                    setAvatarLayout();
                }
                updateLocalAV();

            }



        }
        if (checkPermissions()) {
//            checkPermissionsWriteLog();
            showInitialFABConfiguration();
        }
    }

    private void addMeIntoArray() {
        InfoPeerGroupCall myPeer = new InfoPeerGroupCall(megaChatApi.getMyUserHandle(), megaChatApi.getMyClientidHandle(chatId), megaChatApi.getMyFullname(), callChat.hasLocalVideo(), callChat.hasLocalAudio(), false, true, null);
        peersOnCall.add(myPeer);
        logDebug("I have been added to the array of peers");

    }

    private void addContactIntoArray(long userPeerid, long userClientid) {
        if (getCall() == null) return;

        InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userPeerid, userClientid, getName(userPeerid), false, false, false, true, null);

        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN || (callChat.getStatus() >= MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION && callChat.getStatus() <= MegaChatCall.CALL_STATUS_USER_NO_PRESENT)) {
            peersBeforeCall.add((peersBeforeCall.size() == 0 ? 0 : (peersBeforeCall.size() - 1)), userPeer);
            logDebug("Participant: Peer id " + userPeer.getPeerId() + "and Client id " + userPeer.getClientId() + " added in the array of peers when I'm not in the call");
        } else {
            peersOnCall.add((peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1)), userPeer);
            logDebug("Participant: Peer id " + userPeer.getPeerId() + "and Client id " + userPeer.getClientId() + " added in the array of peers when I'm in the call");
        }
    }

    private void setAvatarLayout() {
        logDebug("setAvatarLayout");
        setProfileAvatar(megaChatApi.getMyUserHandle());
        setProfileAvatar(chat.getPeerHandle(0));
        myAvatarLayout.setVisibility(View.VISIBLE);
        contactAvatarLayout.setVisibility(View.VISIBLE);
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
            logDebug("TYPE_GET_ATTR_USER: OK");

            File avatar = buildAvatarFile(this, request.getEmail() + ".jpg");
            if (!isFileAvailable(avatar) || avatar.length() <= 0) return;

            BitmapFactory.Options bOpts = new BitmapFactory.Options();
            bOpts.inPurgeable = true;
            bOpts.inInputShareable = true;
            Bitmap bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
            if (bitmap == null) {
                avatar.delete();
                return;
            }

            logDebug("Avatar found it");
            if (chat.isGroup()) {
                avatarBigCameraGroupCallInitialLetter.setVisibility(View.GONE);
                avatarBigCameraGroupCallImage.setVisibility(View.VISIBLE);
                avatarBigCameraGroupCallImage.setImageBitmap(bitmap);
                return;
            }

            if (getCall() == null) return;

            if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                myImage.setImageBitmap(bitmap);
                return;
            }
            contactImage.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
    }

    private Bitmap profileAvatar(long peerId, String peerEmail) {
        logDebug("peerId: " + peerId);
        File avatar = null;
        Bitmap bitmap;

        if (!TextUtils.isEmpty(peerEmail)) {
            avatar = buildAvatarFile(this, peerEmail + ".jpg");
        }
        if (!isFileAvailable(avatar) || avatar.length() <= 0) {
            if (peerId != megaChatApi.getMyUserHandle() && !avatarRequested) {
                avatarRequested = true;
                megaApi.getUserAvatar(peerEmail, buildAvatarFile(this, peerEmail + ".jpg").getAbsolutePath(), this);
            }
            return null;
        }

        BitmapFactory.Options bOpts = new BitmapFactory.Options();
        bOpts.inPurgeable = true;
        bOpts.inInputShareable = true;
        bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
        if (bitmap == null && peerId != megaChatApi.getMyUserHandle()) {
            avatar.delete();
            if (!avatarRequested) {
                avatarRequested = true;
                megaApi.getUserAvatar(peerEmail, buildAvatarFile(this, peerEmail + ".jpg").getAbsolutePath(), this);
            }
        }
        return bitmap;
    }

    private void setBitmap(Bitmap bitmap, long peerId){
        if (getCall() == null) return;
        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
            if (peerId == megaChatApi.getMyUserHandle()) {
                contactImage.setImageBitmap(bitmap);
                contactImage.setVisibility(View.VISIBLE);
            } else {
                myImage.setImageBitmap(bitmap);
                myImage.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (peerId == megaChatApi.getMyUserHandle()) {
            myImage.setImageBitmap(bitmap);
            myImage.setVisibility(View.VISIBLE);
        } else {
            contactImage.setImageBitmap(bitmap);
            contactImage.setVisibility(View.VISIBLE);
        }
    }

    /*Individual Call: Profile*/
    private void setProfileAvatar(long peerId) {
        logDebug("peerId: " + peerId);

        String email;
        String name;
        if (peerId == megaChatApi.getMyUserHandle()) {
            email = megaChatApi.getMyEmail();
            name = megaChatApi.getMyFullname();
        } else {
            email = chat.getPeerEmail(0);
            name = chat.getPeerFullname(0);
        }
        /*Default Avatar*/
        Bitmap defaultBitmapAvatar = getDefaultAvatar(this, getColorAvatar(this, megaApi, peerId), name, AVATAR_SIZE, true);
        setBitmap(defaultBitmapAvatar, peerId);

        /*Avatar*/
        Bitmap bitmap = profileAvatar(peerId, email);
        if (bitmap == null) return;
        setBitmap(bitmap, peerId);
    }

    /*Group call: Profile peer selected*/
    public void setProfilePeerSelected(long peerId, String fullName, String peerEmail) {
        logDebug("peerId: " + peerId);

        if (peerId == megaChatApi.getMyUserHandle()) {
            //My peer, other client
            peerEmail = megaChatApi.getMyEmail();
        } else if (peerEmail == null || peerId != peerSelected.getPeerId()) {
            //Contact
            peerEmail = megaChatApi.getContactEmail(peerId);
            if (peerEmail == null) {
                CallNonContactNameListener listener = new CallNonContactNameListener(this, peerId, true, fullName);
                megaChatApi.getUserEmail(peerId, listener);
            }
        }
        String avatarLetter = null;
        if(fullName != null){
            avatarLetter = fullName;
        }else if(peerEmail != null){
            avatarLetter = peerEmail;
        }

        /*Default Avatar*/
        avatarBigCameraGroupCallImage.setImageBitmap(getDefaultAvatar(this, getColorAvatar(this, megaApi, peerId), avatarLetter, BIG_LETTER_SIZE, true));
        /*Avatar*/
        Bitmap bitmap = profileAvatar(peerId, peerEmail);
        if (bitmap == null) return;
        avatarBigCameraGroupCallImage.setVisibility(View.VISIBLE);
        avatarBigCameraGroupCallImage.setImageBitmap(bitmap);
    }

    private void hideActionBar() {
        if (aB == null || !aB.isShowing()) return;
        if (tB == null) {
            aB.hide();
            return;
        }
        tB.animate().translationY(-220).setDuration(800L).withEndAction(new Runnable() {
            @Override
            public void run() {
                aB.hide();
            }
        }).start();
    }

    private void showActionBar() {
        if (aB == null || aB.isShowing()) return;
        aB.show();
        if (tB == null) return;
        tB.animate().translationY(0).setDuration(800L).start();
    }

    private void hideFABs() {
        videoFAB.hide();
        linearArrowVideo.setVisibility(View.GONE);
        relativeVideo.setVisibility(View.GONE);
        microFAB.hide();
        speakerFAB.hide();
        rejectFAB.hide();
        answerCallFAB.hide();
        hangFAB.hide();

        linearArrowCall.setVisibility(View.GONE);
        relativeCall.setVisibility(View.GONE);
        displayedBigRecyclerViewLayout(false);
    }

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
        if(rtcAudioManager!=null){
            rtcAudioManager.unregisterProximitySensor();
        }
    }

    @Override
    protected void onResume() {
        logDebug("onResume");
        super.onResume();
        stopService(new Intent(this, IncomingCallService.class));
        restoreHeightAndWidth();
        if (rtcAudioManager != null) {
            rtcAudioManager.startProximitySensor();
        }
        application.createChatAudioManager();
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        sendSignalPresence();
    }

    private void destroyAdapter(boolean isList) {
        if (isList && adapterList != null) {
            adapterList.onDestroy();
            adapterList = null;
            return;
        }

        if (!isList && adapterGrid != null) {
            adapterGrid.onDestroy();
            adapterGrid = null;
        }
    }

    private void destroyAdapters() {
        destroyAdapter(true);
        destroyAdapter(false);
    }

    @Override
    public void onDestroy() {
        logDebug("onDestroy");
        if(rtcAudioManager!=null){
           rtcAudioManager.unregisterProximitySensor();
        }
        clearHandlers();
        activateChrono(false, callInProgressChrono, callChat);
        restoreHeightAndWidth();

        if (megaChatApi != null) {
            megaChatApi.removeChatCallListener(this);
        }

        clearSurfacesViews();
        peerSelected = null;
        if (adapterList != null) {
            adapterList.updateMode(false);
        }
        isManualMode = false;
        destroyAdapters();

        clearArrays();
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
        if (bigRecyclerView != null) {
            bigRecyclerView.setAdapter(null);
        }
        super.onDestroy();
    }

    private void finishActivity() {
        logDebug("finishActivity");

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
            application.setSpeakerStatus(callChat.getChatid(), false);
            finishActivity();

        } else if (request.getType() == MegaChatRequest.TYPE_ANSWER_CHAT_CALL) {

            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                showInitialFABConfiguration();

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
                    application.setSpeakerStatus(callChat.getChatid(), false);
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
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
    }

    @Override
    public void onChatCallUpdate(MegaChatApiJava api, MegaChatCall call) {

        if (call.getChatid() != chatId) return;

        this.callChat = call;
        logDebug("Chat Id: " + chatId+", Call Status: "+callStatusToString(call.getStatus()));

        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {
            int callStatus = call.getStatus();
            logDebug("CHANGE_TYPE_STATUS" + callStatusToString(callStatus)+", group chat "+chat.isGroup());

            switch (callStatus) {
                case MegaChatCall.CALL_STATUS_HAS_LOCAL_STREAM: {
                    updateLocalAV();
                    invalidateOptionsMenu();
                    break;
                }
                case MegaChatCall.CALL_STATUS_JOINING:
                case MegaChatCall.CALL_STATUS_IN_PROGRESS: {
                    if (reconnectingLayout.isShown()) {
                        hideReconnecting();
                        break;
                    }

                    if (chat.isGroup()) {
                        checkParticipants();
                    } else {
                        setProfileAvatar(megaChatApi.getMyUserHandle());
                        setProfileAvatar(chat.getPeerHandle(0));
                        removeLocalCameraFragmentFS();
                        parentLocalFS.setVisibility(View.GONE);
                        fragmentContainerLocalCameraFS.setVisibility(View.GONE);

                        updateLocalAV();
                        updateRemoteAV(-1, -1);
                    }

                    answerCallFAB.setOnTouchListener(null);
                    videoFAB.setOnTouchListener(null);
                    videoFAB.setOnClickListener(this);
                    showInitialFABConfiguration();
                    updateSubTitle();
                    updateSubtitleNumberOfVideos();
                    updateLocalSpeakerStatus();
                    break;
                }
                case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:
                case MegaChatCall.CALL_STATUS_DESTROYED: {
                    if(callStatus == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION && reconnectingLayout.isShown()){
                        break;
                    }

                    clearHandlers();
                    stopSpeakerAudioManger();
                    application.setSpeakerStatus(callChat.getChatid(), false);
                    finishActivity();
                    break;
                }
                case MegaChatCall.CALL_STATUS_USER_NO_PRESENT: {
                    if (reconnectingLayout.isShown()){
                        break;
                    }

                    clearHandlers();
                    break;
                }
                case MegaChatCall.CALL_STATUS_RECONNECTING: {
                    if (!reconnectingLayout.isShown()) {
                        updateSubTitle();
                        showReconnecting();
                    }
                    break;
                }
                default: {
                    break;
                }
            }

        } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_SESSION_STATUS)) {
            logDebug("CHANGE_TYPE_SESSION_STATUS. Group chat "+chat.isGroup());

            if (chat.isGroup()) {
                clearArrayList(peersBeforeCall);

                long userPeerId = call.getPeerSessionStatusChange();
                long userClientId = call.getClientidSessionStatusChange();

                MegaChatSession userSession = call.getMegaChatSession(userPeerId, userClientId);
                if (userSession == null) return;

                logDebug("User session status : " + callStatusToString(userSession.getStatus()));
                if (userSession.getStatus() == MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
                    //Contact joined the group call
                    boolean peerContain = false;
                    if (peersOnCall.isEmpty()) {
                        checkParticipants();
                        return;
                    }

                    for (InfoPeerGroupCall peerOnCall : peersOnCall) {
                        if (peerOnCall.getPeerId() == userPeerId && peerOnCall.getClientId() == userClientId) {
                            peerContain = true;
                            break;
                        }
                    }

                    if (!peerContain) {
                        addContactIntoArray(userPeerId, userClientId);
                        updateInfoUsersBar(getString(R.string.contact_joined_the_call, getName(userPeerId)));

                        if (peersOnCall.size() <= MAX_PEERS_GRID) {

                            if (adapterGrid == null) {
                                updatePeers();
                            } else {
                                if (peersOnCall.size() < NECESSARY_CHANGE_OF_SIZES) {
                                    recyclerViewLayout.setPadding(0, 0, 0, 0);
                                    recyclerView.setColumnWidth((int) widthScreenPX);
                                    int posInserted = (peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1));
                                    adapterGrid.notifyItemInserted(posInserted);
                                    adapterGrid.notifyDataSetChanged();
                                } else if (peersOnCall.size() == NECESSARY_CHANGE_OF_SIZES) {
                                    recyclerViewLayout.setPadding(0, scaleWidthPx(136, outMetrics), 0, 0);
                                    recyclerView.setColumnWidth((int) widthScreenPX / 2);
                                    adapterGrid.notifyItemInserted(peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1));
                                    adapterGrid.notifyDataSetChanged();
                                } else {
                                    recyclerViewLayout.setPadding(0, 0, 0, 0);
                                    recyclerView.setColumnWidth((int) widthScreenPX / 2);
                                    int posInserted = (peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1));
                                    adapterGrid.notifyItemInserted(posInserted);
                                    adapterGrid.notifyItemRangeChanged((posInserted - 1), peersOnCall.size());
                                }
                            }
                        } else {
                            if (adapterList == null) {
                                updatePeers();
                            } else {
                                if (peersOnCall.size() == MIN_PEERS_LIST) {
                                    updatePeers();
                                } else {
                                    int posInserted = (peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1));
                                    adapterList.notifyItemInserted(posInserted);
                                    adapterList.notifyItemRangeChanged((posInserted - 1), peersOnCall.size());
                                    updateUserSelected();
                                }
                            }
                        }
                    }
                    updateSubTitle();
                    updateRemoteAV(userPeerId, userClientId);
                    updateLocalAV();

                } else if (userSession.getStatus() == MegaChatSession.SESSION_STATUS_DESTROYED) {
                    //contact left the group call
                    if (peersOnCall.isEmpty()) return;

                    for (int i = 0; i < peersOnCall.size(); i++) {
                        if (peersOnCall.get(i).getPeerId() == userPeerId && peersOnCall.get(i).getClientId() == userClientId) {
                            logDebug("Participant: Peer Id " + peersOnCall.get(i).getPeerId() + " and Client Id "+peersOnCall.get(i).getClientId()+" removed from array of peers");
                            updateInfoUsersBar(getString(R.string.contact_left_the_call, peersOnCall.get(i).getName()));
                            peersOnCall.remove(i);
                            if (peersOnCall.isEmpty()) break;
                            if (peersOnCall.size() <= MAX_PEERS_GRID) {
                                if (adapterGrid == null) {
                                    updatePeers();
                                } else {
                                    if (peersOnCall.size() == MAX_PEERS_GRID) {
                                        updatePeers();
                                    } else {
                                        if (peersOnCall.size() < NECESSARY_CHANGE_OF_SIZES) {
                                            recyclerViewLayout.setPadding(0, 0, 0, 0);
                                            recyclerView.setColumnWidth((int) widthScreenPX);
                                            adapterGrid.notifyItemRemoved(i);
                                            adapterGrid.notifyDataSetChanged();
                                        } else {
                                            if (peersOnCall.size() == NECESSARY_CHANGE_OF_SIZES) {
                                                recyclerViewLayout.setPadding(0, scaleWidthPx(136, outMetrics), 0, 0);
                                                recyclerView.setColumnWidth((int) widthScreenPX / 2);
                                            } else {
                                                recyclerViewLayout.setPadding(0, 0, 0, 0);
                                                recyclerView.setColumnWidth((int) widthScreenPX / 2);
                                            }
                                            adapterGrid.notifyItemRemoved(i);
                                            adapterGrid.notifyItemRangeChanged(i, peersOnCall.size());
                                        }
                                    }
                                }
                            } else {
                                if (adapterList == null) {
                                    updatePeers();
                                } else {
                                    adapterList.notifyItemRemoved(i);
                                    adapterList.notifyItemRangeChanged(i, peersOnCall.size());
                                    updateUserSelected();
                                }
                            }
                            break;
                        }
                    }
                    updateLocalAV();
                }
            } else {
                if (call.getPeerSessionStatusChange() == call.getSessionsPeerid().get(0) && call.getClientidSessionStatusChange() == call.getSessionsClientid().get(0)) {
                    updateSubTitle();
                }
                updateRemoteAV(-1, -1);
                updateLocalAV();
            }


        } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_REMOTE_AVFLAGS)) {
            logDebug("CHANGE_TYPE_REMOTE_AVFLAGS");

            if (chat.isGroup()) {
                updateRemoteAV(call.getPeerSessionStatusChange(), call.getClientidSessionStatusChange());
            } else if ((call.getPeerSessionStatusChange() == call.getSessionsPeerid().get(0)) && (call.getClientidSessionStatusChange() == call.getSessionsClientid().get(0))) {
                updateRemoteAV(-1, -1);
            }
            updateSubtitleNumberOfVideos();

        } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS)) {
            logDebug("CHANGE_TYPE_LOCAL_AVFLAGS");
            updateLocalAV();
            updateSubtitleNumberOfVideos();
            invalidateOptionsMenu();

        } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION)) {
            logDebug("CHANGE_TYPE_CALL_COMPOSITION: Call Status " + call.getStatus()+", Number of participants "+call.getPeeridParticipants().size());

            if (call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN || call.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                logDebug("CHANGE_TYPE_SESSION_STATUS:COMPOSITION RING_IN || USER_NO_PRESENT -> TotalParticipants: " + call.getPeeridParticipants().size());
                checkParticipants();
            }
            updateSubTitle();
            updateSubtitleNumberOfVideos();

        } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_SESSION_AUDIO_LEVEL)) {
            logDebug("CHANGE_TYPE_SESSION_AUDIO_LEVEL");
            if (peersOnCall.isEmpty() || peersOnCall.size() <= MAX_PEERS_GRID || isManualMode)
                return;

            long userPeerid = call.getPeerSessionStatusChange();
            long userClientid = call.getClientidSessionStatusChange();
            MegaChatSession userSession = call.getMegaChatSession(userPeerid, userClientid);
            if (userSession == null) return;

            boolean userHasAudio = userSession.getAudioDetected();
            if (!userHasAudio) return;
            //The user is talking
            int position = -1;
            for (int i = 0; i < peersOnCall.size(); i++) {
                if (peersOnCall.get(i).getPeerId() == userPeerid && peersOnCall.get(i).getClientId() == userClientid) {
                    position = i;
                }
            }

            if (position == -1) return;
            peerSelected = adapterList.getNodeAt(position);
            logDebug("Audio detected: Participant " + peerSelected.getPeerId()+"");
            updateUserSelected();

        } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_SESSION_NETWORK_QUALITY)) {
            logDebug("CHANGE_TYPE_SESSION_STATUS:NETWORK_QUALITY");

            if (!chat.isGroup() || peersOnCall.isEmpty()) return;
            clearArrayList(peersBeforeCall);

            long userPeerid = call.getPeerSessionStatusChange();
            long userClientid = call.getClientidSessionStatusChange();
            MegaChatSession userSession = call.getMegaChatSession(userPeerid, userClientid);
            if (userSession == null || userSession.getStatus() != MegaChatSession.SESSION_STATUS_IN_PROGRESS)
                return;

            logDebug("CHANGE_TYPE_SESSION_STATUS:NETWORK_QUALITY:IN_PROGRESS");
            int qualityLevel = userSession.getNetworkQuality();

            for (int i = 0; i < peersOnCall.size(); i++) {
                if (peersOnCall.get(i).getPeerId() == userPeerid && peersOnCall.get(i).getClientId() == userClientid) {
                    if (qualityLevel < 2 && peersOnCall.get(i).isGoodQuality()) {
                        //Bad quality
                        peersOnCall.get(i).setGoodQuality(false);
                    }

                    if (qualityLevel >= 2 && !peersOnCall.get(i).isGoodQuality()) {
                        //Good quality
                        peersOnCall.get(i).setGoodQuality(true);
                    }

                    if (peersOnCall.size() <= MAX_PEERS_GRID && adapterGrid != null) {
                        adapterGrid.changesInQuality(i, null);
                    } else if (peersOnCall.size() >= MIN_PEERS_LIST && adapterList != null) {
                        adapterList.changesInQuality(i, null);
                    } else {
                        updatePeers();
                    }
                }
            }
        } else {
            logDebug("Other call.getChanges(): " + call.getChanges());
        }
    }

    private void hideReconnecting() {
        if (!reconnectingLayout.isShown()) return;

        logDebug("Hidden Reconnecting Layout and Shown You are back Layout");
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

    private void showReconnecting() {
        if (reconnectingLayout.isShown()) return;

        logDebug("Shown Reconnecting Layout");
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

    private void stopSpeakerAudioManger() {
        if (rtcAudioManager == null) return;
        logDebug("stopSpeakerAudioManger");

        try {
            rtcAudioManager.stop();
            rtcAudioManager = null;
        } catch (Exception e) {
            logError("Exception stopping speaker audio manager", e);
        }
    }

    private void sendSignalPresence() {
        if (getCall() == null) return;
        if (callChat.getStatus() != MegaChatCall.CALL_STATUS_IN_PROGRESS && callChat.getStatus() != MegaChatCall.CALL_STATUS_REQUEST_SENT)
            return;
        application.sendSignalPresenceActivity();
    }

    private void displayLinearFAB(boolean isMatchParent) {
        logDebug("displayLinearFAB");
        RelativeLayout.LayoutParams layoutParams;
        if (isMatchParent) {
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

//    public void checkPermissionsWriteLog(){
//        log("checkPermissionsWriteLog()");
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            boolean hasWriteLogPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALL_LOG) == PackageManager.PERMISSION_GRANTED);
//            if (!hasWriteLogPermission) {
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CALL_LOG}, WRITE_LOG);
//            }
//        }
//    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");
        if (getCall() == null) return;

        switch (v.getId()) {
            case R.id.call_chat_contact_image_rl:
            case R.id.parent_layout_big_camera_group_call: {
                remoteCameraClick();
                break;
            }
            case R.id.video_fab: {
                logDebug("Video FAB");
                if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                    displayLinearFAB(false);
                    megaChatApi.answerChatCall(chatId, true, this);
                    clearHandlers();
                    answerCallFAB.clearAnimation();
                    videoFAB.clearAnimation();

                } else if (callChat.hasLocalVideo()) {
                    logDebug("Disable Video");
                    megaChatApi.disableVideo(chatId, this);
                } else {
                    logDebug("Enable Video");
                    megaChatApi.enableVideo(chatId, this);
                }
                sendSignalPresence();
                break;
            }
            case R.id.micro_fab: {
                logDebug("Click on micro fab");
                if (callChat.hasLocalAudio()) {
                    megaChatApi.disableAudio(chatId, this);
                } else {
                    megaChatApi.enableAudio(chatId, this);
                }
                sendSignalPresence();
                break;
            }
            case R.id.speaker_fab: {
                logDebug("Click on speaker fab");
                if (application.getSpeakerStatus(callChat.getChatid())) {
                    application.setSpeakerStatus(callChat.getChatid(), false);
                } else {
                    application.setSpeakerStatus(callChat.getChatid(), true);
                }
                updateLocalSpeakerStatus();
                sendSignalPresence();

                break;
            }
            case R.id.reject_fab:
            case R.id.hang_fab: {
                logDebug("Click on reject fab or hang fab");
                megaChatApi.hangChatCall(chatId, this);
                sendSignalPresence();
                break;
            }
            case R.id.answer_call_fab: {
                logDebug("Click on answer fab");
                if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                    displayLinearFAB(false);
                    megaChatApi.answerChatCall(chatId, false, this);
                    clearHandlers();
                    answerCallFAB.clearAnimation();
                    videoFAB.clearAnimation();
                }
                sendSignalPresence();
                break;
            }
        }
    }

    private boolean checkPermissions() {
        logDebug("Camera && Audio");
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
            return true;
        }
        return true;
    }

    private void showInitialFABConfiguration() {
        if (getCall() == null) return;

        logDebug("Call Status "+callStatusToString(callChat.getStatus()));
        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
            relativeCall.setVisibility(View.VISIBLE);
            answerCallFAB.show();
            linearArrowCall.setVisibility(View.GONE);
            relativeVideo.setVisibility(View.VISIBLE);
            if(videoFAB.isShown()) videoFAB.hide();
            videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
            videoFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_videocam_white));
            videoFAB.show();

            linearArrowVideo.setVisibility(View.GONE);
            rejectFAB.show();
            speakerFAB.hide();
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

            if(microFAB.isShown()) microFAB.hide();
            if (callChat.hasLocalAudio()) {
                microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
                microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_record_audio_w));
            } else {
                microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_off));
            }
            microFAB.show();


            if(speakerFAB.isShown()) speakerFAB.hide();
            if (application.getSpeakerStatus(callChat.getChatid())) {
                speakerFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                speakerFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_speaker_on));
            } else {
                speakerFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                speakerFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_speaker_off));
            }
            speakerFAB.show();


            if(videoFAB.isShown()) videoFAB.hide();
            if (callChat.hasLocalVideo()) {
                videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_white));
            } else {
                videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_off));
            }
            videoFAB.show();

            if(hangFAB.isShown()) hangFAB.hide();
            hangFAB.show();

            rejectFAB.hide();
            answerCallFAB.hide();

            relativeVideo.setVisibility(View.VISIBLE);
            linearArrowVideo.setVisibility(View.GONE);
            relativeCall.setVisibility(View.INVISIBLE);
            linearArrowCall.setVisibility(View.GONE);
        }

        displayedBigRecyclerViewLayout(true);
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

    private void displayedBigRecyclerViewLayout(boolean isAlignBotton) {

        if (bigRecyclerViewLayout == null || bigRecyclerView == null || parentBigCameraGroupCall == null)
            return;
        RelativeLayout.LayoutParams bigRecyclerViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        bigRecyclerViewParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        if (isAlignBotton) {
            bigRecyclerViewParams.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.parent_layout_big_camera_group_call);
            bigRecyclerViewParams.addRule(RelativeLayout.BELOW, 0);

        } else {
            bigRecyclerViewParams.addRule(RelativeLayout.ALIGN_BOTTOM, 0);
            bigRecyclerViewParams.addRule(RelativeLayout.BELOW, R.id.parent_layout_big_camera_group_call);
        }
        bigRecyclerViewLayout.setLayoutParams(bigRecyclerViewParams);
        bigRecyclerViewLayout.requestLayout();
    }


    private void updateLocalVideoStatus() {
        if (getCall() == null) return;
        int callStatus = callChat.getStatus();
        logDebug("Call Status "+callStatus);
        boolean isVideoOn = callChat.hasLocalVideo();
        if(!inTemporaryState){
            application.setVideoStatus(callChat.getChatid(), isVideoOn);
        }
        if (chat.isGroup()) {
            if (callChat.hasLocalVideo()) {
                logDebug("group:Video local connected");
                if(videoFAB.isShown()) {
                    videoFAB.hide();
                    videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                    videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_white));
                    videoFAB.show();
                }

                if (peersOnCall.isEmpty()) return;
                for (int i = 0; i < peersOnCall.size(); i++) {
                    if (peersOnCall.get(i).getPeerId() == megaChatApi.getMyUserHandle() && peersOnCall.get(i).getClientId() == megaChatApi.getMyClientidHandle(chatId)) {
                        if (peersOnCall.get(i).isVideoOn()) break;
                        peersOnCall.get(i).setVideoOn(true);
                        updateChangesVideo(i);
                        break;
                    }
                }
            } else {
                logDebug("group:Video local NOT connected");
                if(videoFAB.isShown()) {
                    videoFAB.hide();
                    videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                    videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_off));
                    videoFAB.show();
                }

                if (peersOnCall.isEmpty()) return;
                for (int i = 0; i < peersOnCall.size(); i++) {
                    if (peersOnCall.get(i).getPeerId() == megaChatApi.getMyUserHandle() && peersOnCall.get(i).getClientId() == megaChatApi.getMyClientidHandle(chatId)) {
                        if (!peersOnCall.get(i).isVideoOn()) break;
                        peersOnCall.get(i).setVideoOn(false);
                        updateChangesVideo(i);
                        break;
                    }
                }
            }

        } else {
            logDebug("individual");
            if (callChat.hasLocalVideo()) {
                logDebug("Video local connected");
                if(videoFAB.isShown()) {
                    videoFAB.hide();
                    videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                    videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_white));
                    videoFAB.show();
                }

                if (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                    logDebug("callStatus: CALL_STATUS_REQUEST_SENT");
                    optionsLocalCameraFragmentFS(true);
                } else if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                    logDebug("callStatus: CALL_STATUS_IN_PROGRESS");
                    optionsLocalCameraFragment(true);
                }
            } else {
                logDebug("Video local NOT connected");
                if(videoFAB.isShown()) {
                    videoFAB.hide();
                    videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                    videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_off));
                    videoFAB.show();
                }

                if (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                    logDebug("callStatus: CALL_STATUS_REQUEST_SENT");
                    optionsLocalCameraFragmentFS(false);
                } else if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                    logDebug("callStatus: CALL_STATUS_IN_PROGRESS ");
                    optionsLocalCameraFragment(false);
                }
            }
        }
        checkTypeCall();
    }

    private void checkTypeCall(){
        if(isOnlyAudioCall()){
            showActionBar();
            showInitialFABConfiguration();
        }
    }

    private void optionsLocalCameraFragment(boolean isNecessaryCreate) {
        if (isNecessaryCreate) {
            if (localCameraFragment == null) {
                localCameraFragment = LocalCameraCallFragment.newInstance(chatId);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container_local_camera, localCameraFragment, "localCameraFragment");
                ft.commitNowAllowingStateLoss();
            }
            myAvatarLayout.setVisibility(View.GONE);
            parentLocal.setVisibility(View.VISIBLE);
            fragmentContainerLocalCamera.setVisibility(View.VISIBLE);
            return;
        }
        removeLocalCameraFragment();
        parentLocal.setVisibility(View.GONE);
        fragmentContainerLocalCamera.setVisibility(View.GONE);
        myAvatarLayout.setVisibility(View.VISIBLE);
    }

    private void removeLocalCameraFragment() {
        if (localCameraFragment == null) return;
        localCameraFragment.removeSurfaceView();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.remove(localCameraFragment);
        localCameraFragment = null;
    }

    private void optionsLocalCameraFragmentFS(boolean isNecessaryCreate) {
        if (isNecessaryCreate) {
            if (localCameraFragmentFS == null) {
                localCameraFragmentFS = LocalCameraCallFullScreenFragment.newInstance(chatId);
                FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
                ftFS.replace(R.id.fragment_container_local_cameraFS, localCameraFragmentFS, "localCameraFragmentFS");
                ftFS.commitNowAllowingStateLoss();
            }
            contactAvatarLayout.setVisibility(View.GONE);
            parentLocalFS.setVisibility(View.VISIBLE);
            fragmentContainerLocalCameraFS.setVisibility(View.VISIBLE);
            return;
        }
        removeLocalCameraFragmentFS();
        parentLocalFS.setVisibility(View.GONE);
        fragmentContainerLocalCameraFS.setVisibility(View.GONE);
        contactAvatarLayout.setVisibility(View.VISIBLE);
    }

    private void removeLocalCameraFragmentFS() {
        if (localCameraFragmentFS == null) return;
        localCameraFragmentFS.removeSurfaceView();
        FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
        ftFS.remove(localCameraFragmentFS);
        localCameraFragmentFS = null;
    }

    private void updateLocalAV() {
        updateLocalVideoStatus();
        updateLocalAudioStatus();
    }

    private void updateRemoteAV(long peerId, long clientId) {
        updateRemoteVideoStatus(peerId, clientId);
        updateRemoteAudioStatus(peerId, clientId);
    }

    private void updateChangesAudio(int position) {
        if (peersOnCall.size() <= MAX_PEERS_GRID && adapterGrid != null) {
            adapterGrid.changesInAudio(position, null);
            return;
        }
        if (peersOnCall.size() >= MIN_PEERS_LIST && adapterList != null) {
            adapterList.changesInAudio(position, null);
            return;
        }
        updatePeers();
    }

    private void updateChangesVideo(int position) {
        if (peersOnCall.size() <= MAX_PEERS_GRID && adapterGrid != null) {
            adapterGrid.notifyItemChanged(position);
            return;
        }
        if (peersOnCall.size() >= MIN_PEERS_LIST && adapterList != null) {
            adapterList.notifyItemChanged(position);
            return;
        }
        updatePeers();
    }

    private void updateLocalAudioStatus() {
        if (getCall() == null) return;
        logDebug("Call Status "+callStatusToString(callChat.getStatus()));
        if (chat.isGroup()) {
            int position;
            if (callChat.hasLocalAudio()) {
                logDebug("group:Audio local connected");
                if(microFAB.isShown()) {
                    microFAB.hide();
                    microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
                    microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_record_audio_w));
                    microFAB.show();
                }
                if (peersOnCall.isEmpty()) return;
                position = peersOnCall.size() - 1;
                if (peersOnCall.get(position).isAudioOn()) return;
                peersOnCall.get(position).setAudioOn(true);
            } else {
                logDebug("group:Audio local NOT connected");
                if(microFAB.isShown()) {
                    microFAB.hide();
                    microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                    microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_off));
                    microFAB.show();
                }
                if (peersOnCall.isEmpty()) return;
                position = peersOnCall.size() - 1;
                if (!peersOnCall.get(position).isAudioOn()) return;
                peersOnCall.get(position).setAudioOn(false);
            }
            updateChangesAudio(position);
        } else {
            if (callChat.hasLocalAudio()) {
                logDebug("individual:Audio local connected");
                if(microFAB.isShown()) {
                    microFAB.hide();
                    microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
                    microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_record_audio_w));
                    microFAB.show();
                }
            } else {
                logDebug("individual:Audio local NOT connected");
                if(microFAB.isShown()) {
                    microFAB.hide();
                    microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                    microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_off));
                    microFAB.show();
                }
            }
            refreshOwnMicro();
        }
    }

    private void createAppRTCAudioManager(boolean isSpeakerOn){
        rtcAudioManager = AppRTCAudioManager.create(this, isSpeakerOn);
        rtcAudioManager.setOnProximitySensorListener(new OnProximitySensorListener() {
            @Override
            public void needToUpdate(boolean isNear) {
                boolean realStatus = application.getVideoStatus(callChat.getChatid());
                if(!realStatus){
                    inTemporaryState = false;
                }else if(isNear){
                    inTemporaryState = true;
                    megaChatApi.disableVideo(chatId, ChatCallActivity.this);
                }else{
                    inTemporaryState = false;
                    megaChatApi.enableVideo(chatId, ChatCallActivity.this);
                }
            }
        });
    }

    private void updateLocalSpeakerStatus() {
        boolean isSpeakerOn = application.getSpeakerStatus(callChat.getChatid());
        if(rtcAudioManager == null){
            createAppRTCAudioManager(isSpeakerOn);
        }else{
            rtcAudioManager.updateSpeakerStatus(isSpeakerOn);
        }

        speakerFAB.hide();
        if (isSpeakerOn) {
            speakerFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
            speakerFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_speaker_on));
        } else {
            speakerFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            speakerFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_speaker_off));
        }

        speakerFAB.show();
        application.setAudioManagerValues(callChat);
    }

    private void updateRemoteVideoStatus(long userPeerId, long userClientId) {
        logDebug("(peerid = " + userPeerId + ", clientid = " + userClientId + ")");
        if (getCall() == null) return;

        if (chat.isGroup()) {
            MegaChatSession userSession = callChat.getMegaChatSession(userPeerId, userClientId);
            if (userSession == null || peersOnCall.isEmpty()) return;

            if (userSession.hasVideo()) {
                logDebug("Contact -> Video remote connected");
                for (int i = 0; i < peersOnCall.size(); i++) {

                    if (peersOnCall.get(i).getPeerId() == userPeerId && peersOnCall.get(i).getClientId() == userClientId) {
                        if (peersOnCall.get(i).isVideoOn()) break;
                        peersOnCall.get(i).setVideoOn(true);
                        updateChangesVideo(i);
                        if (peersOnCall.size() >= MIN_PEERS_LIST && peerSelected != null && peerSelected.getPeerId() == userPeerId && peerSelected.getClientId() == userClientId) {
                            createBigFragment(peerSelected.getPeerId(), peerSelected.getClientId());
                            avatarBigCameraGroupCallMicro.setVisibility(View.GONE);
                            if (peerSelected.isAudioOn()) {
                                microFragmentBigCameraGroupCall.setVisibility(View.GONE);
                            } else {
                                microFragmentBigCameraGroupCall.setVisibility(View.VISIBLE);
                            }
                        }
                        break;
                    }
                }

            } else {
                logDebug("Contact -> Video remote NO connected");
                for (int i = 0; i < peersOnCall.size(); i++) {
                    if (peersOnCall.get(i).getPeerId() == userPeerId && peersOnCall.get(i).getClientId() == userClientId) {
                        if (!peersOnCall.get(i).isVideoOn()) break;
                        peersOnCall.get(i).setVideoOn(false);
                        updateChangesVideo(i);
                        if (peersOnCall.size() >= MIN_PEERS_LIST && peerSelected != null && peerSelected.getPeerId() == userPeerId && peerSelected.getClientId() == userClientId) {
                            createBigAvatar();
                            microFragmentBigCameraGroupCall.setVisibility(View.GONE);
                            if (peerSelected.isAudioOn()) {
                                avatarBigCameraGroupCallMicro.setVisibility(View.GONE);
                            } else {
                                avatarBigCameraGroupCallMicro.setVisibility(View.VISIBLE);
                            }
                        }
                        break;
                    }
                }
            }

        } else {
            logDebug("Individual");
            MegaChatSession userSession = callChat.getMegaChatSession(callChat.getSessionsPeerid().get(0), callChat.getSessionsClientid().get(0));
            if (userSession == null) return;
            if (isRemoteVideo == REMOTE_VIDEO_NOT_INIT) {
                if (userSession.hasVideo()) {
                    logDebug("REMOTE_VIDEO_NOT_INIT Contact Video remote connected");
                    optionsRemoteCameraFragmentFS(true);
                } else {
                    logDebug("REMOTE_VIDEO_NOT_INIT Contact Video remote NOT connected");
                    optionsRemoteCameraFragmentFS(false);
                }
            } else {
                if (isRemoteVideo == REMOTE_VIDEO_ENABLED && !userSession.hasVideo()) {
                    logDebug("REMOTE_VIDEO_ENABLED Contact Video remote connected");
                    optionsRemoteCameraFragmentFS(false);

                } else if ((isRemoteVideo == REMOTE_VIDEO_DISABLED) && userSession.hasVideo()) {
                    logDebug("REMOTE_VIDEO_DISABLED Contact Video remote connected");
                    optionsRemoteCameraFragmentFS(true);
                }
            }
        }
        checkTypeCall();
    }

    private void optionsRemoteCameraFragmentFS(boolean isNecessaryCreate) {
        if (isNecessaryCreate) {
            isRemoteVideo = REMOTE_VIDEO_ENABLED;
            if (remoteCameraFragmentFS == null) {
                remoteCameraFragmentFS = RemoteCameraCallFullScreenFragment.newInstance(chatId, callChat.getSessionsPeerid().get(0), callChat.getSessionsClientid().get(0));
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container_remote_cameraFS, remoteCameraFragmentFS, "remoteCameraFragmentFS");
                ft.commitNowAllowingStateLoss();
            }
            contactAvatarLayout.setVisibility(View.GONE);
            parentRemoteFS.setVisibility(View.VISIBLE);
            fragmentContainerRemoteCameraFS.setVisibility(View.VISIBLE);
            return;
        }

        isRemoteVideo = REMOTE_VIDEO_DISABLED;
        removeRemoteCameraFragmentFS();
        contactAvatarLayout.setVisibility(View.VISIBLE);
        parentRemoteFS.setVisibility(View.GONE);
        fragmentContainerRemoteCameraFS.setVisibility(View.GONE);
    }

    private void removeRemoteCameraFragmentFS() {
        if (remoteCameraFragmentFS == null) return;
        remoteCameraFragmentFS.removeSurfaceView();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.remove(remoteCameraFragmentFS);
        remoteCameraFragmentFS = null;
    }

    private void updateRemoteAudioStatus(long userPeerId, long userClientId) {
        logDebug("(peerid = " + userPeerId + ", clientid = " + userClientId + ")");
        if (getCall() == null) return;

        if (chat.isGroup()) {
            MegaChatSession userSession = callChat.getMegaChatSession(userPeerId, userClientId);
            if (userSession == null || peersOnCall.isEmpty()) return;

            if (userSession.hasAudio()) {
                logDebug("group Contact -> Audio remote connected");
                for (int i = 0; i < peersOnCall.size(); i++) {
                    if (peersOnCall.get(i).getPeerId() == userPeerId && peersOnCall.get(i).getClientId() == userClientId) {
                        if (peersOnCall.get(i).isAudioOn()) break;
                        peersOnCall.get(i).setAudioOn(true);
                        updateChangesAudio(i);
                        if (peersOnCall.size() >= MIN_PEERS_LIST && peerSelected != null && peerSelected.getPeerId() == userPeerId && peerSelected.getClientId() == userClientId) {
                            avatarBigCameraGroupCallMicro.setVisibility(View.GONE);
                            microFragmentBigCameraGroupCall.setVisibility(View.GONE);
                        }
                        break;
                    }
                }
            } else {
                logDebug("Contact -> Audio remote NO connected");
                for (int i = 0; i < peersOnCall.size(); i++) {
                    if (peersOnCall.get(i).getPeerId() == userPeerId && peersOnCall.get(i).getClientId() == userClientId) {
                        if (!peersOnCall.get(i).isAudioOn()) break;
                        peersOnCall.get(i).setAudioOn(false);
                        updateChangesAudio(i);
                        if (peersOnCall.size() >= MIN_PEERS_LIST && peerSelected != null && peerSelected.getPeerId() == userPeerId && peerSelected.getClientId() == userClientId) {
                            if (peerSelected.isVideoOn()) {
                                avatarBigCameraGroupCallMicro.setVisibility(View.GONE);
                                microFragmentBigCameraGroupCall.setVisibility(View.VISIBLE);
                            } else {
                                avatarBigCameraGroupCallMicro.setVisibility(View.VISIBLE);
                                microFragmentBigCameraGroupCall.setVisibility(View.GONE);
                            }
                        }
                        break;
                    }
                }
            }
        } else {
            logDebug("Individual");
            refreshContactMicro();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        logDebug("onRequestPermissionsResult");

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA: {
                logDebug("REQUEST_CAMERA");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPermissions()) {
//                        checkPermissionsWriteLog();
                        showInitialFABConfiguration();
                    }
                } else {
                    rejectFAB.show();
                }
                break;
            }
            case RECORD_AUDIO: {
                logDebug("RECORD_AUDIO");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPermissions()) {
//                        checkPermissionsWriteLog();
                        showInitialFABConfiguration();
                    }
                } else {
                    rejectFAB.show();
                }
                break;
            }
            case WRITE_LOG: {
                logDebug("WRITE_LOG");
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    checkPermissionsWriteLog();
//                }
                break;
            }
        }
    }

    private boolean isOnlyAudioCall() {
        if(callChat == null || callChat.getNumParticipants(MegaChatCall.VIDEO) > 0) return false;
        return true;
    }

    public void remoteCameraClick() {
        logDebug("remoteCameraClick");
        if (getCall() == null || (callChat.getStatus() != MegaChatCall.CALL_STATUS_IN_PROGRESS && callChat.getStatus() != MegaChatCall.CALL_STATUS_JOINING && callChat.getStatus() != MegaChatCall.CALL_STATUS_RECONNECTING) || isOnlyAudioCall())
            return;

        if (aB.isShowing()) {
            hideActionBar();
            hideFABs();
            return;
        }

        showActionBar();
        showInitialFABConfiguration();
    }

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
            if (adapterList == null || peersOnCall.isEmpty()) return;
            adapterList.updateMode(isManualMode);
            for (int i = 0; i < peersOnCall.size(); i++) {
                if (peersOnCall.get(i).getPeerId() == peer.getPeerId() && peersOnCall.get(i).getClientId() == peer.getClientId()) {
                    peersOnCall.get(i).setGreenLayer(true);
                } else if (peersOnCall.get(i).hasGreenLayer()) {
                    peersOnCall.get(i).setGreenLayer(false);
                }
                adapterList.changesInGreenLayer(i, null);
            }

        } else if (peer.getPeerId() == megaChatApi.getMyUserHandle() && peer.getClientId() == megaChatApi.getMyClientidHandle(chatId)) {
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
            updateUserSelected();
        }
    }

    private void answerCall(boolean isVideoCall) {
        logDebug("answerCall");
        clearHandlers();
        if (megaChatApi == null) return;

        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }
        application.setSpeakerStatus(callChat.getChatid(), isVideoCall);
        megaChatApi.answerChatCall(chatId, isVideoCall, this);
    }

    private void animationAlphaArrows(final ImageView arrow) {
        logDebug("animationAlphaArrows");

        AlphaAnimation alphaAnimArrows = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimArrows.setDuration(ALPHA_ARROW_ANIMATION);
        alphaAnimArrows.setFillAfter(true);
        alphaAnimArrows.setFillBefore(true);
        alphaAnimArrows.setRepeatCount(Animation.INFINITE);
        arrow.startAnimation(alphaAnimArrows);
    }

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
                    logWarning("userSession is null");
                    connectingCall();
                    return;
                }

                if (userSession.getStatus() == MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
                    logDebug("Session in progress");
                    subtitleToobar.setVisibility(View.GONE);
                    activateChrono(true, callInProgressChrono, callChat);
                    return;
                }
            }
        }

        subtitleToobar.setVisibility(View.GONE);
        activateChrono(false, callInProgressChrono, callChat);
    }

    private void updateSubtitleNumberOfVideos() {
        logDebug("updateSubtitleNumberOfVideos");
        if (chat == null) return;
        if (!chat.isGroup()) {
            linearParticipants.setVisibility(View.GONE);
            return;
        }

        if (getCall() == null) return;
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

    private void updatePeers() {
        logDebug("updatePeers");
        if (getCall() == null) return;

        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN || (callChat.getStatus() >= MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION && callChat.getStatus() <= MegaChatCall.CALL_STATUS_USER_NO_PRESENT)) {
            //I'M NOT IN THE CALL
            logDebug("incoming call: peersBeforeCall");
            clearArrayList(peersOnCall);

            linearParticipants.setVisibility(View.GONE);

            if (peersBeforeCall.isEmpty()) {
                resetPeers();
                return;
            }
            if (!notYetJoinedTheCall) {
                notYetJoinedTheCall = true;
            }

            logDebug("Incoming call: peersBeforeCall not empty");
            if (peersBeforeCall.size() <= MAX_PEERS_GRID) {
                logDebug("peersBeforeCall GRID ");
                //1-6 users

                destroyAdapter(true);

                avatarBigCameraGroupCallLayout.setVisibility(View.GONE);

                bigRecyclerView.setAdapter(null);
                bigRecyclerView.setVisibility(View.GONE);
                bigRecyclerViewLayout.setVisibility(View.GONE);

                parentBigCameraGroupCall.setOnClickListener(null);
                parentBigCameraGroupCall.setVisibility(View.GONE);

                recyclerViewLayout.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.VISIBLE);

                if (peersBeforeCall.size() < NECESSARY_CHANGE_OF_SIZES) {
                    recyclerViewLayout.setPadding(0, 0, 0, 0);
                    recyclerView.setColumnWidth((int) widthScreenPX);
                } else {
                    if (peersBeforeCall.size() == NECESSARY_CHANGE_OF_SIZES) {
                        recyclerViewLayout.setPadding(0, scaleWidthPx(136, outMetrics), 0, 0);
                    } else {
                        recyclerViewLayout.setPadding(0, 0, 0, 0);
                    }
                    recyclerView.setColumnWidth((int) widthScreenPX / 2);
                }

                if (adapterGrid == null) {
                    logDebug("Create adapter");
                    recyclerView.setAdapter(null);
                    adapterGrid = new GroupCallAdapter(this, recyclerView, peersBeforeCall, chatId, true);
                    recyclerView.setAdapter(adapterGrid);
                } else {
                    logDebug("notifyDataSetChanged");
                    adapterGrid.notifyDataSetChanged();
                }

            } else {
                logDebug("peersBeforeCall LIST ");
                //7 + users

                destroyAdapter(false);

                recyclerView.setAdapter(null);
                recyclerView.setVisibility(View.GONE);
                recyclerViewLayout.setVisibility(View.GONE);

                parentBigCameraGroupCall.setOnClickListener(this);
                parentBigCameraGroupCall.setVisibility(View.VISIBLE);

                bigRecyclerViewLayout.setVisibility(View.VISIBLE);
                bigRecyclerView.setVisibility(View.VISIBLE);

                if (adapterList == null) {
                    logDebug("Create adapter");
                    bigRecyclerView.setAdapter(null);
                    adapterList = new GroupCallAdapter(this, bigRecyclerView, peersBeforeCall, chatId, false);
                    bigRecyclerView.setAdapter(adapterList);
                } else {
                    logDebug("notifyDataSetChanged");
                    adapterList.notifyDataSetChanged();
                }
                updateUserSelected();
            }

        } else {
            //I'M IN THE CALL.
            logDebug("In progress call");
            clearArrayList(peersBeforeCall);

            if (peersOnCall.isEmpty()) {
                resetPeers();
                return;
            }

            if (peersOnCall.size() <= MAX_PEERS_GRID) {
                logDebug("peersOnCall GRID ");
                //1-6 users
                destroyAdapter(true);

                removeBigFragment();

                avatarBigCameraGroupCallLayout.setVisibility(View.GONE);
                bigRecyclerView.setAdapter(null);
                bigRecyclerView.setVisibility(View.GONE);
                bigRecyclerViewLayout.setVisibility(View.GONE);

                parentBigCameraGroupCall.setOnClickListener(null);
                parentBigCameraGroupCall.setVisibility(View.GONE);

                recyclerViewLayout.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.VISIBLE);

                if (peersOnCall.size() < NECESSARY_CHANGE_OF_SIZES) {
                    recyclerViewLayout.setPadding(0, 0, 0, 0);
                    recyclerView.setColumnWidth((int) widthScreenPX);
                } else {
                    if (peersOnCall.size() == NECESSARY_CHANGE_OF_SIZES) {
                        recyclerViewLayout.setPadding(0, scaleWidthPx(136, outMetrics), 0, 0);
                    } else {
                        recyclerViewLayout.setPadding(0, 0, 0, 0);
                    }
                    recyclerView.setColumnWidth((int) widthScreenPX / 2);
                }

                if (adapterGrid == null) {
                    logDebug("Create adapter");
                    recyclerView.setAdapter(null);
                    adapterGrid = new GroupCallAdapter(this, recyclerView, peersOnCall, chatId, true);
                    recyclerView.setAdapter(adapterGrid);

                } else {
                    if (notYetJoinedTheCall) {
                        logDebug("Create adapter");
                        destroyAdapter(false);
                        recyclerView.setAdapter(null);
                        adapterGrid = new GroupCallAdapter(this, recyclerView, peersOnCall, chatId, true);
                        recyclerView.setAdapter(adapterGrid);
                    } else {
                        logDebug("notifyDataSetChanged");
                        adapterGrid.notifyDataSetChanged();
                    }
                }

            } else {
                logDebug("peersOnCall LIST ");
                //7 + users
                destroyAdapter(false);

                recyclerView.setAdapter(null);
                recyclerView.setVisibility(View.GONE);
                recyclerViewLayout.setVisibility(View.GONE);

                parentBigCameraGroupCall.setOnClickListener(this);
                parentBigCameraGroupCall.setVisibility(View.VISIBLE);

                bigRecyclerViewLayout.setVisibility(View.VISIBLE);
                bigRecyclerView.setVisibility(View.VISIBLE);

                if (adapterList == null) {
                    logDebug("Create adapter");
                    bigRecyclerView.setAdapter(null);
                    adapterList = new GroupCallAdapter(this, bigRecyclerView, peersOnCall, chatId, false);
                    bigRecyclerView.setAdapter(adapterList);
                } else {
                    if (notYetJoinedTheCall) {
                        logDebug("Create adapter");
                        destroyAdapter(true);
                        bigRecyclerView.setAdapter(null);
                        adapterList = new GroupCallAdapter(this, bigRecyclerView, peersOnCall, chatId, false);
                        bigRecyclerView.setAdapter(adapterList);
                    } else {
                        logDebug("notifyDataSetChanged");
                        adapterList.notifyDataSetChanged();
                    }
                }
                updateUserSelected();
            }
            if (notYetJoinedTheCall) {
                notYetJoinedTheCall = false;
            }

        }

    }

    private void resetPeers() {
        destroyAdapters();

        recyclerView.setAdapter(null);
        recyclerView.setVisibility(View.GONE);
        recyclerViewLayout.setVisibility(View.GONE);

        bigRecyclerView.setAdapter(null);
        bigRecyclerView.setVisibility(View.GONE);
        bigRecyclerViewLayout.setVisibility(View.GONE);

        parentBigCameraGroupCall.setOnClickListener(null);
        parentBigCameraGroupCall.setVisibility(View.GONE);
    }

    private void updateGreenLayer(int position) {
        if (adapterList == null) return;
        adapterList.changesInGreenLayer(position, null);
    }

    private void updateStatusUserSelected() {
        if (peerSelected.isVideoOn()) {
            //Video ON
            createBigFragment(peerSelected.getPeerId(), peerSelected.getClientId());
            avatarBigCameraGroupCallMicro.setVisibility(View.GONE);
            if (peerSelected.isAudioOn()) {
                microFragmentBigCameraGroupCall.setVisibility(View.GONE);
            } else {
                microFragmentBigCameraGroupCall.setVisibility(View.VISIBLE);
            }
        } else {
            //Video OFF
            createBigAvatar();
            microFragmentBigCameraGroupCall.setVisibility(View.GONE);
            if (peerSelected.isAudioOn()) {
                avatarBigCameraGroupCallMicro.setVisibility(View.GONE);
            } else {
                avatarBigCameraGroupCallMicro.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateUserSelected() {
        logDebug("updateUserSelected");
        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN || (callChat.getStatus() >= MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION && callChat.getStatus() <= MegaChatCall.CALL_STATUS_USER_NO_PRESENT)) {
            //I'M NOT IN THE CALL
            logDebug("INCOMING");
            if (peerSelected != null) return;

            parentBigCameraGroupCall.setVisibility(View.VISIBLE);

            removeBigFragment();
            fragmentBigCameraGroupCall.setVisibility(View.GONE);

            //Create Avatar, get the last peer of peersBeforeCall
            if (peersBeforeCall.isEmpty()) {
                avatarBigCameraGroupCallLayout.setVisibility(View.GONE);
                return;
            }

            avatarBigCameraGroupCallLayout.setVisibility(View.VISIBLE);
            InfoPeerGroupCall peerTemp = peersBeforeCall.get((peersBeforeCall.size()) - 1);
            setProfilePeerSelected(peerTemp.getPeerId(), peerTemp.getName(), null);

        } else {
            //I'M IN THE CALL
            logDebug("IN PROGRESS");

            if (peersOnCall.isEmpty()) return;

            if (peerSelected == null) {
                logWarning("peerSelected == null");

                if (isManualMode) return;

                int position = 0;
                peerSelected = peersOnCall.get(position);
                logDebug("InProgress - new peerSelected (peerId = " + peerSelected.getPeerId() + ", clientId = " + peerSelected.getClientId() + ")");
                for (int i = 0; i < peersOnCall.size(); i++) {
                    if (i == position) {
                        if (!peersOnCall.get(i).hasGreenLayer()) {
                            peersOnCall.get(i).setGreenLayer(true);
                            updateGreenLayer(i);
                        }
                    } else {
                        if (peersOnCall.get(i).hasGreenLayer()) {
                            peersOnCall.get(i).setGreenLayer(false);
                            updateGreenLayer(i);
                        }
                    }
                }
            } else {
                logDebug("peerSelected != null");

                //find if peerSelected is removed:
                boolean peerContained = false;
                for (int i = 0; i < peersOnCall.size(); i++) {
                    if (peersOnCall.get(i).getPeerId() == peerSelected.getPeerId() && peersOnCall.get(i).getClientId() == peerSelected.getClientId()) {
                        peerContained = true;
                        break;
                    }
                }
                if (!peerContained) {
                    //it was removed
                    int position = 0;
                    peerSelected = peersOnCall.get(position);
                    logDebug("InProgress - new peerSelected (peerId = " + peerSelected.getPeerId() + ", clientId = " + peerSelected.getClientId() + ")");
                    for (int i = 0; i < peersOnCall.size(); i++) {
                        if (i == position) {
                            isManualMode = false;
                            if (adapterList != null) {
                                adapterList.updateMode(false);
                            }
                            if (!peersOnCall.get(i).hasGreenLayer()) {
                                peersOnCall.get(i).setGreenLayer(true);
                                updateGreenLayer(i);
                            }
                        } else {
                            if (peersOnCall.get(i).hasGreenLayer()) {
                                peersOnCall.get(i).setGreenLayer(false);
                                updateGreenLayer(i);
                            }
                        }
                    }
                } else {

                    logDebug("InProgress - peerSelected (peerId = " + peerSelected.getPeerId() + ", clientId = " + peerSelected.getClientId() + ")");
                    for (int i = 0; i < peersOnCall.size(); i++) {
                        if (peersOnCall.get(i).getPeerId() == peerSelected.getPeerId() && peersOnCall.get(i).getClientId() == peerSelected.getClientId()) {
                            peersOnCall.get(i).setGreenLayer(true);
                            updateGreenLayer(i);
                        } else {
                            if (peersOnCall.get(i).hasGreenLayer()) {
                                peersOnCall.get(i).setGreenLayer(false);
                                updateGreenLayer(i);
                            }
                        }
                    }
                }
            }
            updateStatusUserSelected();
        }
    }

    private void createBigFragment(long peerId, long clientId) {
        logDebug("createBigFragment()");
        removeBigFragment();

        bigCameraGroupCallFragment = BigCameraGroupCallFragment.newInstance(chatId, peerId, clientId);
        FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
        ftFS.replace(R.id.fragment_big_camera_group_call, bigCameraGroupCallFragment, "bigCameraGroupCallFragment");
        ftFS.commitNowAllowingStateLoss();

        fragmentBigCameraGroupCall.setVisibility(View.VISIBLE);
        parentBigCameraGroupCall.setVisibility(View.VISIBLE);
        avatarBigCameraGroupCallLayout.setVisibility(View.GONE);
    }

    private void removeBigFragment() {
        if (bigCameraGroupCallFragment == null) return;
        bigCameraGroupCallFragment.removeSurfaceView();
        FragmentTransaction ftFS = getSupportFragmentManager().beginTransaction();
        ftFS.remove(bigCameraGroupCallFragment);
        bigCameraGroupCallFragment = null;

    }

    private void createBigAvatar() {
        logDebug("createBigAvatar()");
        removeBigFragment();

        fragmentBigCameraGroupCall.setVisibility(View.GONE);
        avatarBigCameraGroupCallImage.setImageBitmap(null);
        setProfilePeerSelected(peerSelected.getPeerId(), peerSelected.getName(), null);
        parentBigCameraGroupCall.setVisibility(View.VISIBLE);
        avatarBigCameraGroupCallLayout.setVisibility(View.VISIBLE);
    }

    private void clearSurfacesViews() {
        logDebug("clearSurfacesViews");
        removeLocalCameraFragment();
        if(parentLocal != null && fragmentContainerLocalCamera != null) {
            parentLocal.setVisibility(View.GONE);
            fragmentContainerLocalCamera.setVisibility(View.GONE);
        }

        removeLocalCameraFragmentFS();
        if(parentLocalFS != null && fragmentContainerLocalCameraFS != null) {
            parentLocalFS.setVisibility(View.GONE);
            fragmentContainerLocalCameraFS.setVisibility(View.GONE);
        }

        removeRemoteCameraFragmentFS();
        if(parentRemoteFS != null && fragmentContainerRemoteCameraFS != null) {
            parentRemoteFS.setVisibility(View.GONE);
            fragmentContainerRemoteCameraFS.setVisibility(View.GONE);
        }

        removeBigFragment();
        if(fragmentBigCameraGroupCall != null) {
            fragmentBigCameraGroupCall.setVisibility(View.GONE);
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

    public void showSnackbar(String s) {
        logDebug("showSnackbar: " + s);
        showSnackbar(fragmentContainer, s);
    }

    private String getName(long peerid) {
        String name = " ";
        if (megaChatApi == null || chat == null) return name;

        if (peerid == megaChatApi.getMyUserHandle()) {
            name = megaChatApi.getMyFullname();
            if (name == null) name = megaChatApi.getMyEmail();

        } else {
            name = chat.getPeerFullnameByHandle(peerid);
            if (name == null) {
                name = megaChatApi.getContactEmail(peerid);
                if (name == null) {
                    CallNonContactNameListener listener = new CallNonContactNameListener(this, peerid, false, name);
                    megaChatApi.getUserEmail(peerid, listener);
                }
            }
        }
        return name;
    }

    public void updateNonContactName(long peerid, String peerEmail) {
        logDebug("Email found it");
        if (!peersBeforeCall.isEmpty()) {
            for (InfoPeerGroupCall peer : peersBeforeCall) {
                if (peerid == peer.getPeerId()) {
                    peer.setName(peerEmail);
                }
            }
        }
        if (!peersOnCall.isEmpty()) {
            for (InfoPeerGroupCall peer : peersOnCall) {
                if (peerid == peer.getPeerId()) {
                    peer.setName(peerEmail);
                }
            }
        }
    }

    private void checkParticipants() {
        if (getCall() == null) return;
        logDebug("Call status: "+callChat.getStatus());

        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN || (callChat.getStatus() >= MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION && callChat.getStatus() <= MegaChatCall.CALL_STATUS_USER_NO_PRESENT)) {
            //peersBeforeCall
            if (megaChatApi == null || callChat.getPeeridParticipants().size() <= 0) return;
            boolean isMe = false;
            for (int i = 0; i < callChat.getPeeridParticipants().size(); i++) {
                long userPeerid = callChat.getPeeridParticipants().get(i);
                long userClientid = callChat.getClientidParticipants().get(i);
                if (userPeerid == megaChatApi.getMyUserHandle() && userClientid == megaChatApi.getMyClientidHandle(chatId)) {
                    isMe = true;
                    break;
                }
            }

            if (isMe) return;

            if (!peersBeforeCall.isEmpty()) {
                logDebug("Call status: "+callChat.getStatus()+" there are "+peersBeforeCall.size()+" participants");

                boolean changes = false;
                for (int i = 0; i < callChat.getPeeridParticipants().size(); i++) {
                    boolean peerContain = false;
                    long userPeerid = callChat.getPeeridParticipants().get(i);
                    long userClientid = callChat.getClientidParticipants().get(i);

                    for (InfoPeerGroupCall peerBeforeCall : peersBeforeCall) {
                        if ((peerBeforeCall.getPeerId() == userPeerid) && (peerBeforeCall.getClientId() == userClientid)) {
                            peerContain = true;
                            break;
                        }
                    }
                    if (!peerContain) {
                        addContactIntoArray(userPeerid, userClientid);
                        changes = true;
                    }
                }

                if ((peersBeforeCall != null) && (peersBeforeCall.size() > 0)) {
                    for (int i = 0; i < peersBeforeCall.size(); i++) {
                        boolean peerContained = false;
                        for (int j = 0; j < callChat.getPeeridParticipants().size(); j++) {
                            long userPeerid = callChat.getPeeridParticipants().get(j);
                            long userClientid = callChat.getClientidParticipants().get(j);
                            if ((peersBeforeCall.get(i).getPeerId() == userPeerid) && (peersBeforeCall.get(i).getClientId() == userClientid)) {
                                peerContained = true;
                            }
                        }
                        if (!peerContained) {
                            logDebug(peersBeforeCall.get(i).getPeerId() + " removed from peersBeforeCall");
                            peersBeforeCall.remove(i);
                            changes = true;
                        }
                    }
                }

                if (changes) {
                    updatePeers();
                }
            } else {
                logDebug("Call status: "+callChat.getStatus()+", the array of participants is empty, all participants will be added");
                for (int i = 0; i < callChat.getPeeridParticipants().size(); i++) {
                    long userPeerid = callChat.getPeeridParticipants().get(i);
                    long userClientid = callChat.getClientidParticipants().get(i);
                    addContactIntoArray(userPeerid, userClientid);
                }
                updatePeers();
            }

        } else {
            if (megaChatApi == null || callChat.getSessionsPeerid().size() <= 0) return;


            //peersOnCall
            if (!peersOnCall.isEmpty()) {
                logDebug("Call status: "+callChat.getStatus()+" there are "+peersOnCall.size()+" participants");

                boolean changes = false;
                //Get all participant and check it (some will be added and others will be removed)
                for (int i = 0; i < callChat.getSessionsPeerid().size(); i++) {
                    boolean peerContain = false;
                    long userPeerid = callChat.getSessionsPeerid().get(i);
                    long userClientid = callChat.getSessionsClientid().get(i);

                    for (InfoPeerGroupCall peerOnCall : peersOnCall) {
                        if (peerOnCall.getPeerId() == userPeerid && peerOnCall.getClientId() == userClientid) {
                            peerContain = true;
                            break;
                        }
                    }
                    if (!peerContain) {
                        if (userPeerid == megaChatApi.getMyUserHandle() && userClientid == megaChatApi.getMyClientidHandle(chatId)) {
                            //me
                            addMeIntoArray();
                            changes = true;

                        } else {
                            //contact
                            MegaChatSession sessionPeer = callChat.getMegaChatSession(userPeerid, userClientid);
                            if (sessionPeer == null) return;
                            if (sessionPeer.getStatus() <= MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
                                addContactIntoArray(userPeerid, userClientid);
                                changes = true;
                            }
                        }
                    }
                }

                for (int i = 0; i < peersOnCall.size(); i++) {
                    boolean peerContained = false;
                    for (int j = 0; j < callChat.getSessionsPeerid().size(); j++) {
                        long userPeerid = callChat.getSessionsPeerid().get(j);
                        long userClientid = callChat.getSessionsClientid().get(j);
                        if (peersOnCall.get(i).getPeerId() == userPeerid && peersOnCall.get(i).getClientId() == userClientid) {
                            peerContained = true;
                            break;
                        }
                    }

                    if (!peerContained && (peersOnCall.get(i).getPeerId() != megaChatApi.getMyUserHandle() || peersOnCall.get(i).getClientId() != megaChatApi.getMyClientidHandle(chatId))) {
                        logDebug("Participant: Peer id " + peersOnCall.get(i).getPeerId() + "and Client id " + peersOnCall.get(i).getClientId() + " removed from the array of peers when I'm in the call");
                        peersOnCall.remove(i);
                        changes = true;
                    }
                }

                if (changes) updatePeers();

            } else {
                //peersOnCall empty
                logDebug("Call status: "+callChat.getStatus()+", the array of participants is empty, all sessions will be added");
                addMeIntoArray();
                for (int i = 0; i < callChat.getSessionsPeerid().size(); i++) {
                    long userPeerid = callChat.getSessionsPeerid().get(i);
                    long userClientid = callChat.getSessionsClientid().get(i);
                    MegaChatSession sessionPeer = callChat.getMegaChatSession(userPeerid, userClientid);
                    if (sessionPeer == null) continue;
                    if (sessionPeer.getStatus() <= MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
                        addContactIntoArray(userPeerid, userClientid);
                    }
                }
                updatePeers();

            }
            if (peersOnCall.isEmpty()) return;

            logDebug("Update Video&Audio local&remote");
            updateSubTitle();
            for (int i = 0; i < peersOnCall.size(); i++) {
                if (peersOnCall.get(i).getPeerId() == megaChatApi.getMyUserHandle() && peersOnCall.get(i).getClientId() == megaChatApi.getMyClientidHandle(chatId)) {
                    updateLocalAV();
                } else {
                    updateRemoteAV(peersOnCall.get(i).getPeerId(), peersOnCall.get(i).getClientId());
                }
            }
        }
    }

    private MegaChatCall getCall() {
        if (callChat != null) return callChat;
        if (megaChatApi == null) return null;
        callChat = megaChatApi.getChatCall(chatId);
        return callChat;
    }

    private void localCameraFragmentShowMicro(boolean showIt) {
        if (localCameraFragment == null) return;
        localCameraFragment.showMicro(showIt);
    }

    private void checkMutateOwnCallLayout(int option) {
        if (mutateOwnCallLayout.getVisibility() == option) return;
        mutateOwnCallLayout.setVisibility(option);
    }

    public void refreshOwnMicro() {
        logDebug("refreshOwnMicro");

        if (chat.isGroup() || getCall() == null) return;

        if (callChat.hasLocalAudio() || !callChat.hasLocalVideo()) {
            localCameraFragmentShowMicro(false);
        } else {
            localCameraFragmentShowMicro(true);
        }

        if (callChat.hasLocalAudio() || (callChat.hasLocalVideo() || mutateContactCallLayout.getVisibility() == View.VISIBLE)) {
            checkMutateOwnCallLayout(View.GONE);
            return;
        }
        checkMutateOwnCallLayout(View.VISIBLE);
    }

    private void refreshContactMicro() {
        logDebug("refreshContactMicro");
        if (chat.isGroup() || getCall() == null) return;

        MegaChatSession userSession = callChat.getMegaChatSession(callChat.getSessionsPeerid().get(0), callChat.getSessionsClientid().get(0));
        if (userSession == null) return;
        if (userSession.getStatus() == MegaChatSession.SESSION_STATUS_INITIAL || userSession.hasAudio()) {
            mutateContactCallLayout.setVisibility(View.GONE);
        } else {
            String name = chat.getPeerFirstname(0);
            if ((name == null) || (name == " ")) {
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
}
