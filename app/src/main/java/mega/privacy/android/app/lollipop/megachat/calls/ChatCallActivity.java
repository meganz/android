package mega.privacy.android.app.lollipop.megachat.calls;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.CustomizedGridCallRecyclerView;
import mega.privacy.android.app.components.OnSwipeTouchListener;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.fcm.IncomingCallService;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.CallNonContactNameListener;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.GroupCallAdapter;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
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
import static mega.privacy.android.app.utils.ChatUtil.showErrorAlertDialogGroupCall;
import static mega.privacy.android.app.utils.FileUtils.*;

public class ChatCallActivity extends BaseActivity implements MegaChatRequestListenerInterface, MegaChatCallListenerInterface, MegaRequestListenerInterface, View.OnClickListener, SensorEventListener, KeyEvent.Callback {

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
    private float widthScreenPX, heightScreenPX;
    private long chatId;
    private MegaChatRoom chat;
    private MegaChatCall callChat;
    private MegaChatApiAndroid megaChatApi = null;
    private Display display;
    private DisplayMetrics outMetrics;
    private Toolbar tB;
    private TextView titleToolbar;
    private TextView subtitleToobar;
    private Chronometer callInProgressChrono;
    private RelativeLayout mutateContactCallLayout;
    private TextView mutateCallText;
    private RelativeLayout mutateOwnCallLayout;
    private LinearLayout linearParticipants;
    private TextView participantText;
    private TextView infoUsersBar;
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
    private TextView myInitialLetter;
    private RelativeLayout contactAvatarLayout;
    private RoundedImageView contactImage;
    private TextView contactInitialLetter;
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
    private TextView avatarBigCameraGroupCallInitialLetter;
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
    private SensorManager mSensorManager = null;
    private Sensor mSensor;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private int field = 0x00000020;

    public static void log(String message) {
        Util.log("ChatCallActivity", message);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        log("onPrepareOptionsMenu");
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                onBackPressed();
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
        log("updateScreenStatus:");
        if (chatId == -1 || megaChatApi == null || getCall() == null) return;
        chat = megaChatApi.getChatRoom(chatId);
        int callStatus = callChat.getStatus();

        if (chat.isGroup()) {
            log("updateScreenStatus:Group");
            clearArrayList(peersBeforeCall);

            if (callStatus == MegaChatCall.CALL_STATUS_RING_IN) {
                displayLinearFAB(true);
            } else if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS || callStatus == MegaChatCall.CALL_STATUS_JOINING || callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                displayLinearFAB(false);
            }

            checkParticipants();
            updateSubtitleNumberOfVideos();

        } else {
            log("updateScreenStatus:Individual");

            if (callStatus == MegaChatCall.CALL_STATUS_RING_IN || callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS || callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                if (callStatus == MegaChatCall.CALL_STATUS_RING_IN) {
                    displayLinearFAB(true);
                } else {
                    displayLinearFAB(false);
                }
                setProfileAvatar(megaChatApi.getMyUserHandle());
                setProfileAvatar(chat.getPeerHandle(0));
            }
            updateLocalAV();
            updateRemoteAV(-1, -1);
        }

        updateSubTitle();
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        log("onNewIntent");

        Bundle extras = intent.getExtras();
        log(getIntent().getAction());
        if (extras != null) {
            long newChatId = extras.getLong(Constants.CHAT_ID, -1);
            if (megaChatApi == null) return;

            if (chatId != -1 && chatId == newChatId) {
                log("onNewIntent: same call");
                chat = megaChatApi.getChatRoom(chatId);
                updateScreenStatus();
                updateLocalSpeakerStatus();

            } else {
                if (newChatId == -1) return;

                log("onNewIntent: different call");
                chatId = newChatId;
                chat = megaChatApi.getChatRoom(chatId);
                callChat = megaChatApi.getChatCall(chatId);
                titleToolbar.setText(chat.getTitle());
                updateScreenStatus();
                updateLocalSpeakerStatus();

                if (getCall() == null || callChat.getStatus() != MegaChatCall.CALL_STATUS_IN_PROGRESS)
                    return;

                log("onNewIntent:Start call Service");
                Intent intentService = new Intent(this, CallService.class);
                intentService.putExtra(Constants.CHAT_ID, callChat.getChatid());
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
        log("onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_calls_chat);
        MegaApplication.setShowPinScreen(true);

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }

        widthScreenPX = outMetrics.widthPixels;
        heightScreenPX = outMetrics.heightPixels - statusBarHeight;

        MegaApplication app = (MegaApplication) getApplication();

        if (megaApi == null) {
            megaApi = app.getMegaApi();
        }
        if (megaApi != null) {
            megaApi.retryPendingConnections();
        }
        if (megaChatApi == null) {
            megaChatApi = app.getMegaChatApi();
        }
        if (megaChatApi != null) {
            megaChatApi.retryPendingConnections(false, null);
        }

        if (megaApi == null || megaApi.getRootNode() == null || megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR) {
            log("Refresh session - sdk || karere");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra("visibleFragment", Constants.LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        megaChatApi.addChatCallListener(this);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        try {
            field = PowerManager.class.getClass().getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
        } catch (Throwable ignored) {
        }

        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(field, getLocalClassName());

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        fragmentContainer = findViewById(R.id.file_info_fragment_container);

        tB = findViewById(R.id.call_toolbar);
        if (tB == null) {
            log("Toolbar is Null");
            return;
        }
        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setTitle(null);
        aB.setSubtitle(null);

        titleToolbar = tB.findViewById(R.id.title_toolbar);
        titleToolbar.setText(" ");
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
        answerCallFAB.setVisibility(View.GONE);

        videoFAB = findViewById(R.id.video_fab);
        videoFAB.setOnClickListener(this);
        videoFAB.setVisibility(View.GONE);

        rejectFAB = findViewById(R.id.reject_fab);
        rejectFAB.setOnClickListener(this);
        rejectFAB.setVisibility(View.GONE);

        speakerFAB = findViewById(R.id.speaker_fab);
        speakerFAB.setOnClickListener(this);
        speakerFAB.setVisibility(View.GONE);

        microFAB = findViewById(R.id.micro_fab);
        microFAB.setOnClickListener(this);
        microFAB.setVisibility(View.GONE);

        hangFAB = findViewById(R.id.hang_fab);
        hangFAB.setOnClickListener(this);
        hangFAB.setVisibility(View.GONE);

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
            myInitialLetter = findViewById(R.id.call_chat_my_image_initial_letter);
            contactAvatarLayout = findViewById(R.id.call_chat_contact_image_rl);
            contactAvatarLayout.setOnClickListener(this);
            contactAvatarLayout.setVisibility(View.GONE);
            contactImage = findViewById(R.id.call_chat_contact_image);
            contactInitialLetter = findViewById(R.id.call_chat_contact_image_initial_letter);

            videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            videoFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_video_off));

            //Contact's avatar
            chatId = extras.getLong(Constants.CHAT_ID, -1);
            if (chatId == -1 || megaChatApi == null) return;

            chat = megaChatApi.getChatRoom(chatId);
            callChat = megaChatApi.getChatCall(chatId);
            if (callChat == null) {
                megaChatApi.removeChatCallListener(this);
                ((MegaApplication) getApplication()).setSpeakerStatus(callChat.getChatid(), false);
                finishActivity();
                return;
            }

            log("Start call Service");
            Intent intentService = new Intent(this, CallService.class);
            intentService.putExtra(Constants.CHAT_ID, callChat.getChatid());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.startForegroundService(intentService);
            } else {
                this.startService(intentService);
            }

            ((MegaApplication) getApplication()).checkAudioManager();

            int callStatus = callChat.getStatus();
            log("The status of the callChat is: " + callStatus);
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
                log("onCreate:RING_IN");
                if (chat.isGroup()) {
                    log("onCreate:RING_IN:group");
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
                    log("onCreate:RING_IN:individual");
                    setAvatarLayout();
                }

            } else if ((callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS) || (callStatus == MegaChatCall.CALL_STATUS_JOINING)) {
                log("onCreate:IN_PROGRESS||JOINING");
                if (!chat.isGroup()) {
                    log("onCreate:IN_PROGRESS||JOINING:individual");
                    setAvatarLayout();
                }

                updateScreenStatus();
                updateLocalSpeakerStatus();

            } else if (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                log("onCreate:REQUEST_SENT");
                updateLocalSpeakerStatus();
                if (chat.isGroup()) {
                    log("onCreate:REQUEST_SENT:group");
                    clearArrays();
                    addMeIntoArray();
                    updatePeers();

                } else {
                    log("onCreate:REQUEST_SENT:individual");
                    setAvatarLayout();
                }
                updateLocalAV();

            } else {
                log("onCreate:other status: " + callStatus);
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
        log("I added in peersOnCall");

    }

    private void addContactIntoArray(long userPeerid, long userClientid) {
        if (getCall() == null) return;

        InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userPeerid, userClientid, getName(userPeerid), false, false, false, true, null);

        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN || (callChat.getStatus() >= MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION && callChat.getStatus() <= MegaChatCall.CALL_STATUS_USER_NO_PRESENT)) {
            peersBeforeCall.add((peersBeforeCall.size() == 0 ? 0 : (peersBeforeCall.size() - 1)), userPeer);
            log(userPeer.getPeerId() + " added in peersBeforeCall");
        } else {
            peersOnCall.add((peersOnCall.size() == 0 ? 0 : (peersOnCall.size() - 1)), userPeer);
            log(userPeer.getPeerId() + " added in peersOnCall");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        log("onKeyDown");
        ((MegaApplication) getApplication()).checkAudioManager();
        if (getCall() == null) return true;
        int value;

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP: {
                try {
                    if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                        value = AudioManager.STREAM_RING;
                    } else if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                        value = AudioManager.STREAM_MUSIC;
                    } else {
                        value = AudioManager.STREAM_VOICE_CALL;
                    }
                    ((MegaApplication) getApplication()).setAudioManagerValues(value, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                    return true;

                } catch (SecurityException e) {
                    return super.onKeyDown(keyCode, event);
                }

            }
            case KeyEvent.KEYCODE_VOLUME_DOWN: {
                try {
                    if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                        value = AudioManager.STREAM_RING;
                    } else if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                        value = AudioManager.STREAM_MUSIC;
                    } else {
                        value = AudioManager.STREAM_VOICE_CALL;
                    }
                    ((MegaApplication) getApplication()).setAudioManagerValues(value, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                    return true;

                } catch (SecurityException e) {
                    return super.onKeyDown(keyCode, event);
                }
            }
            default: {
                return super.onKeyDown(keyCode, event);
            }
        }
    }
    private void setAvatarLayout() {
        log("setAvatarLayout");
        setProfileAvatar(megaChatApi.getMyUserHandle());
        setProfileAvatar(chat.getPeerHandle(0));
        myAvatarLayout.setVisibility(View.VISIBLE);
        contactAvatarLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart: " + request.getType());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        log("onRequestUpdate");
    }

    @SuppressLint("NewApi")
    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish: type: " + request.getType());

        if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER && e.getErrorCode() != MegaError.API_OK) {
            log("onRequestFinish:TYPE_GET_ATTR_USER: OK");

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

            log("onRequestFinish:Avatar found it");
            if (chat.isGroup()) {
                avatarBigCameraGroupCallInitialLetter.setVisibility(View.GONE);
                avatarBigCameraGroupCallImage.setVisibility(View.VISIBLE);
                avatarBigCameraGroupCallImage.setImageBitmap(bitmap);
                return;
            }

            if (getCall() == null) return;

            if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                myImage.setImageBitmap(bitmap);
                myInitialLetter.setVisibility(View.GONE);
                return;
            }
            contactImage.setImageBitmap(bitmap);
            contactInitialLetter.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
    }

    private Bitmap profileAvatar(long peerId, String peerEmail) {
        log("profileAvatar:peerId: " + peerId);
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

    /*Individual Call: Profile*/
    private void setProfileAvatar(long peerId) {
        log("setProfileAvatar:peerId: " + peerId);

        String email;
        String name;
        if (peerId == megaChatApi.getMyUserHandle()) {
            email = megaChatApi.getMyEmail();
            name = megaChatApi.getMyFullname();
        } else {
            email = chat.getPeerEmail(0);
            name = chat.getPeerFullname(0);
        }

        Bitmap bitmap = profileAvatar(peerId, email);
        if (bitmap == null) {
            createDefaultAvatar(peerId, name);
            return;
        }

        if (getCall() == null) return;

        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
            if (peerId == megaChatApi.getMyUserHandle()) {
                contactImage.setImageBitmap(bitmap);
                contactImage.setVisibility(View.VISIBLE);
                contactInitialLetter.setVisibility(View.GONE);
            } else {
                myImage.setImageBitmap(bitmap);
                myImage.setVisibility(View.VISIBLE);
                myInitialLetter.setVisibility(View.GONE);
            }
            return;
        }

        if (peerId == megaChatApi.getMyUserHandle()) {
            myImage.setImageBitmap(bitmap);
            myImage.setVisibility(View.VISIBLE);
            myInitialLetter.setVisibility(View.GONE);
        } else {
            contactImage.setImageBitmap(bitmap);
            contactImage.setVisibility(View.VISIBLE);
            contactInitialLetter.setVisibility(View.GONE);
        }
    }

    /*Group call: Profile peer selected*/
    public void setProfilePeerSelected(long peerId, String fullName, String peerEmail) {
        log("setProfilePeerSelected:peerId: " + peerId);

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
        if (peerEmail == null) return;
        Bitmap bitmap = profileAvatar(peerId, peerEmail);
        if (bitmap == null) {
            createDefaultAvatarPeerSelected(peerId, fullName, peerEmail);
            return;
        }
        avatarBigCameraGroupCallInitialLetter.setVisibility(View.GONE);
        avatarBigCameraGroupCallImage.setVisibility(View.VISIBLE);
        avatarBigCameraGroupCallImage.setImageBitmap(bitmap);
    }

    private Bitmap defaultAvatar(long peerId) {
        log("defaultAvatar:peerId: " + peerId);

        Bitmap defaultAvatar = Bitmap.createBitmap(outMetrics.widthPixels, outMetrics.widthPixels, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(Color.TRANSPARENT);
        String color = megaApi.getUserAvatarColor(MegaApiAndroid.userHandleToBase64(peerId));
        if (color != null) {
            p.setColor(Color.parseColor(color));
        } else {
            p.setColor(ContextCompat.getColor(this, R.color.lollipop_primary_color));
        }

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight()) {
            radius = defaultAvatar.getWidth() / 2;
        } else {
            radius = defaultAvatar.getHeight() / 2;
        }
        c.drawCircle(defaultAvatar.getWidth() / 2, defaultAvatar.getHeight() / 2, radius, p);
        return defaultAvatar;
    }

    /*Individual Call: default Avatar*/
    private void createDefaultAvatar(long peerId, String peerName) {
        log("createDefaultAvatar:peerId: " + peerId);
        if (getCall() == null) return;

        Bitmap defaultAvatar = defaultAvatar(peerId);
        String firstLetter = peerName.charAt(0) + "";
        firstLetter = firstLetter.toUpperCase(Locale.getDefault());

        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
            if (peerId == megaChatApi.getMyUserHandle()) {
                contactImage.setImageBitmap(defaultAvatar);
                putAttributesTextView(contactInitialLetter, BIG_LETTER_SIZE, firstLetter);
            } else {
                myImage.setImageBitmap(defaultAvatar);
                putAttributesTextView(myInitialLetter, SMALL_LETTER_SIZE, firstLetter);
            }
            return;
        }

        if (peerId == megaChatApi.getMyUserHandle()) {
            myImage.setImageBitmap(defaultAvatar);
            putAttributesTextView(myInitialLetter, SMALL_LETTER_SIZE, firstLetter);
        } else {
            contactImage.setImageBitmap(defaultAvatar);
            putAttributesTextView(contactInitialLetter, BIG_LETTER_SIZE, firstLetter);
        }

    }

    /*Group call: default avatar of peer selected*/
    private void createDefaultAvatarPeerSelected(long peerId, String peerName, String peerEmail) {
        log("createDefaultAvatarPeerSelected:peerId: " + peerId);
        avatarBigCameraGroupCallImage.setVisibility(View.VISIBLE);
        avatarBigCameraGroupCallImage.setImageBitmap(defaultAvatar(peerId));

        if (peerName != null && peerName.trim().length() > 0) {
            String firstLetter = peerName.charAt(0) + "";
            firstLetter = firstLetter.toUpperCase(Locale.getDefault());
            avatarBigCameraGroupCallInitialLetter.setText(firstLetter);
            avatarBigCameraGroupCallInitialLetter.setTextColor(Color.WHITE);
            avatarBigCameraGroupCallInitialLetter.setVisibility(View.VISIBLE);
            return;
        }

        if (peerEmail != null && peerEmail.length() > 0) {
            String firstLetter = peerEmail.charAt(0) + "";
            firstLetter = firstLetter.toUpperCase(Locale.getDefault());
            avatarBigCameraGroupCallInitialLetter.setText(firstLetter);
            avatarBigCameraGroupCallInitialLetter.setTextColor(Color.WHITE);
            avatarBigCameraGroupCallInitialLetter.setVisibility(View.VISIBLE);
        }
    }

    private void putAttributesTextView(TextView tv, float size, String text) {
        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(Color.WHITE);
        tv.setVisibility(View.VISIBLE);
    }

    private void hideActionBar() {
        if (aB == null || !aB.isShowing()) return;
        log("hideActionBar");
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
        log("showActionBar");
        aB.show();
        if (tB == null) return;
        tB.animate().translationY(0).setDuration(800L).start();
    }

    private void hideFABs() {
        log("hideFABs");
        statusButton(videoFAB, View.GONE);
        linearArrowVideo.setVisibility(View.GONE);
        relativeVideo.setVisibility(View.GONE);
        statusButton(microFAB, View.GONE);
        statusButton(speakerFAB, View.GONE);
        statusButton(rejectFAB, View.GONE);
        statusButton(hangFAB, View.GONE);
        statusButton(answerCallFAB, View.GONE);
        linearArrowCall.setVisibility(View.GONE);
        relativeCall.setVisibility(View.GONE);
        displayedBigRecyclerViewLayout(false);
    }

    private void restoreHeightAndWidth() {
        if (peersOnCall.isEmpty()) return;
        log("restoreHeightAndWidth");
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
        MegaApplication.activityPaused();
        super.onPause();
        if (mSensorManager == null) return;
        mSensorManager.unregisterListener(this);
        mSensorManager = null;
    }

    @Override
    protected void onResume() {
        log("onResume");
        super.onResume();
        stopService(new Intent(this, IncomingCallService.class));
        restoreHeightAndWidth();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        ((MegaApplication) getApplication()).checkAudioManager();

        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        MegaApplication.activityResumed();

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
        log("onDestroy");
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        clearHandlers();
        ChatUtil.activateChrono(false, callInProgressChrono, callChat);
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
        recyclerView.setAdapter(null);
        bigRecyclerView.setAdapter(null);
        super.onDestroy();
    }

    private void finishActivity() {
        log("finishActivity");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.finishAndRemoveTask();
        } else {
            super.finish();
        }
    }

    @Override
    public void onBackPressed() {
        log("onBackPressed");
        super.onBackPressed();
        finishActivity();
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {
        log("onRequestStart: " + request.getType());
    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {
        log("onRequestUpdate: " + request.getType());
    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish: " + request.getType());

        if (request.getType() == MegaChatRequest.TYPE_HANG_CHAT_CALL) {
            log("onRequestFinish: TYPE_HANG_CHAT_CALL");
            if (mSensorManager != null) {
                mSensorManager.unregisterListener(this);
            }

            if (getCall() == null) return;
            ((MegaApplication) getApplication()).setSpeakerStatus(callChat.getChatid(), false);
            finishActivity();

        } else if (request.getType() == MegaChatRequest.TYPE_ANSWER_CHAT_CALL) {

            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                showInitialFABConfiguration();

                if (request.getFlag() == true) {
                    log("Ok answer with video");
                } else {
                    log("Ok answer with NO video - ");
                }
                updateLocalAV();
            } else {
                log("Error call: " + e.getErrorString());

                if (e.getErrorCode() == MegaChatError.ERROR_TOOMANY) {
                    showErrorAlertDialogGroupCall(getString(R.string.call_error_too_many_participants), true, this);
                } else {
                    if (getCall() == null) return;
                    ((MegaApplication) getApplication()).setSpeakerStatus(callChat.getChatid(), false);
                    finishActivity();
                }
            }
        } else if (request.getType() == MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL) {

            if (e.getErrorCode() != MegaChatError.ERROR_OK) {
                log("Error changing audio or video: " + e.getErrorString());
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
        log("onChatCallUpdate:chatId: " + chatId);

        if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {
            int callStatus = call.getStatus();
            log("CHANGE_TYPE_STATUS -> status: " + callStatus);

            switch (callStatus) {
                case MegaChatCall.CALL_STATUS_JOINING:
                case MegaChatCall.CALL_STATUS_IN_PROGRESS: {

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
                    clearHandlers();
                    stopSpeakerAudioManger();
                    ((MegaApplication) getApplication()).setSpeakerStatus(callChat.getChatid(), false);
                    finishActivity();
                    break;
                }
                case MegaChatCall.CALL_STATUS_USER_NO_PRESENT: {
                    clearHandlers();
                    break;
                }
                default: {
                    break;
                }
            }

        } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_SESSION_STATUS)) {
            log("onChatCallUpdate:CHANGE_TYPE_SESSION_STATUS");

            if (chat.isGroup()) {
                log("onChatCallUpdate:CHANGE_TYPE_SESSION_STATUS:group");

                clearArrayList(peersBeforeCall);

                long userPeerId = call.getPeerSessionStatusChange();
                long userClientId = call.getClientidSessionStatusChange();

                MegaChatSession userSession = call.getMegaChatSession(userPeerId, userClientId);
                if (userSession == null) return;

                log("CHANGE_TYPE_SESSION_STATUS: " + userSession.getStatus());
                if (userSession.getStatus() == MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
                    log("CHANGE_TYPE_SESSION_STATUS:IN_PROGRESS");

                    //contact joined the group call
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
                                    recyclerViewLayout.setPadding(0, Util.scaleWidthPx(136, outMetrics), 0, 0);
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
                    log("CHANGE_TYPE_SESSION_STATUS:DESTROYED");
                    //contact left the group call
                    if (peersOnCall.isEmpty()) return;

                    for (int i = 0; i < peersOnCall.size(); i++) {
                        if (peersOnCall.get(i).getPeerId() == userPeerId && peersOnCall.get(i).getClientId() == userClientId) {
                            log("CHANGE_TYPE_SESSION_STATUS:DESTROYED " + peersOnCall.get(i).getPeerId() + " removed from peersOnCall");
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
                                                recyclerViewLayout.setPadding(0, Util.scaleWidthPx(136, outMetrics), 0, 0);
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
                } else {
                    log("CHANGE_TYPE_SESSION_STATUS:OTHER = " + userSession.getStatus());
                }
            } else {
                log("onChatCallUpdate:CHANGE_TYPE_SESSION_STATUS:individual");

                if (call.getPeerSessionStatusChange() == call.getSessionsPeerid().get(0) && call.getClientidSessionStatusChange() == call.getSessionsClientid().get(0)) {
                    updateSubTitle();
                }
                updateRemoteAV(-1, -1);
                updateLocalAV();
            }


        } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_REMOTE_AVFLAGS)) {
            log("CHANGE_TYPE_SESSION_STATUS:REMOTE_AVFLAGS");

            if (chat.isGroup()) {
                updateRemoteAV(call.getPeerSessionStatusChange(), call.getClientidSessionStatusChange());
            } else if ((call.getPeerSessionStatusChange() == call.getSessionsPeerid().get(0)) && (call.getClientidSessionStatusChange() == call.getSessionsClientid().get(0))) {
                updateRemoteAV(-1, -1);
            }
            updateSubtitleNumberOfVideos();

        } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS)) {
            log("CHANGE_TYPE_SESSION_STATUS:LOCAL_AVFLAGS");
            updateLocalAV();
            updateSubtitleNumberOfVideos();

        } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION)) {
            log("CHANGE_TYPE_SESSION_STATUS:COMPOSITION: status -> " + call.getStatus());

            if (call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN || call.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                log("CHANGE_TYPE_SESSION_STATUS:COMPOSITION RING_IN || USER_NO_PRESENT -> TotalParticipants: " + call.getPeeridParticipants().size());
                checkParticipants();
            }
            updateSubTitle();
            updateSubtitleNumberOfVideos();

        } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_SESSION_AUDIO_LEVEL)) {
            log("CHANGE_TYPE_SESSION_STATUS:CHANGE_TYPE_SESSION_AUDIO_LEVEL");
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
            log("audio detected: " + peerSelected.getPeerId());
            updateUserSelected();

        } else if (call.hasChanged(MegaChatCall.CHANGE_TYPE_SESSION_NETWORK_QUALITY)) {
            log("CHANGE_TYPE_SESSION_STATUS:NETWORK_QUALITY");

            if (!chat.isGroup() || peersOnCall.isEmpty()) return;
            clearArrayList(peersBeforeCall);

            long userPeerid = call.getPeerSessionStatusChange();
            long userClientid = call.getClientidSessionStatusChange();
            MegaChatSession userSession = call.getMegaChatSession(userPeerid, userClientid);
            if (userSession == null || userSession.getStatus() != MegaChatSession.SESSION_STATUS_IN_PROGRESS)
                return;

            log("CHANGE_TYPE_SESSION_STATUS:NETWORK_QUALITY:IN_PROGRESS");
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
            log("other call.getChanges(): " + call.getChanges());
        }
    }

    private void updateInfoUsersBar(String text) {
        log("updateInfoUsersBar");
        infoUsersBar.setText(text);
        infoUsersBar.setBackgroundColor(ContextCompat.getColor(this, R.color.accentColor));
        infoUsersBar.setAlpha(1);
        infoUsersBar.setVisibility(View.VISIBLE);
        infoUsersBar.animate().alpha(0).setDuration(INFO_ANIMATION);
    }

    private void stopSpeakerAudioManger() {
        if (rtcAudioManager == null) return;
        log("stopSpeakerAudioManger");

        try {
            rtcAudioManager.stop();
            rtcAudioManager = null;
        } catch (Exception e) {
            log("Exception stopping speaker audio manager");
        }
    }

    private void sendSignalPresence() {
        if (getCall() == null) return;
        if (callChat.getStatus() != MegaChatCall.CALL_STATUS_IN_PROGRESS && callChat.getStatus() != MegaChatCall.CALL_STATUS_REQUEST_SENT)
            return;
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();
    }

    private void displayLinearFAB(boolean isMatchParent) {
        log("displayLinearFAB");
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
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_CALL_LOG}, Constants.WRITE_LOG);
//            }
//        }
//    }

    @Override
    public void onClick(View v) {
        log("onClick");
        if (getCall() == null) return;

        switch (v.getId()) {
            case R.id.call_chat_contact_image_rl:
            case R.id.parent_layout_big_camera_group_call: {
                remoteCameraClick();
                break;
            }
            case R.id.video_fab: {
                log("onClick video FAB");
                if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                    displayLinearFAB(false);
                    megaChatApi.answerChatCall(chatId, true, this);
                    clearHandlers();
                    answerCallFAB.clearAnimation();
                    videoFAB.clearAnimation();

                } else if (callChat.hasLocalVideo()) {
                    log(" disableVideo");
                    megaChatApi.disableVideo(chatId, this);
                } else {
                    log(" enableVideo");
                    megaChatApi.enableVideo(chatId, this);
                }
                sendSignalPresence();
                break;
            }
            case R.id.micro_fab: {
                log("Click on micro fab");
                if (callChat.hasLocalAudio()) {
                    megaChatApi.disableAudio(chatId, this);
                } else {
                    megaChatApi.enableAudio(chatId, this);
                }
                sendSignalPresence();
                break;
            }
            case R.id.speaker_fab: {
                log("Click on speaker fab");
                if (((MegaApplication) getApplication()).getSpeakerStatus(callChat.getChatid())) {
                    ((MegaApplication) getApplication()).setSpeakerStatus(callChat.getChatid(), false);
                } else {
                    ((MegaApplication) getApplication()).setSpeakerStatus(callChat.getChatid(), true);
                }
                updateLocalSpeakerStatus();
                sendSignalPresence();

                break;
            }
            case R.id.reject_fab:
            case R.id.hang_fab: {
                log("Click on reject fab or hang fab");
                megaChatApi.hangChatCall(chatId, this);
                sendSignalPresence();
                break;
            }
            case R.id.answer_call_fab: {
                log("Click on answer fab");
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
        log("checkPermissions:Camera && Audio");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
            if (!hasCameraPermission) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, Constants.REQUEST_CAMERA);
                return false;
            }
            boolean hasRecordAudioPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
            if (!hasRecordAudioPermission) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, Constants.RECORD_AUDIO);
                return false;
            }
            return true;
        }
        return true;
    }

    private void statusButton(FloatingActionButton button, int visibility) {
        if (button.getVisibility() == visibility) return;
        log("statusButton");

        if (visibility == View.VISIBLE) {
            button.show();
        }
        if (visibility == View.GONE) {
            button.hide();
        }
        button.setVisibility(visibility);
    }

    private void showInitialFABConfiguration() {
        log("showInitialFABConfiguration");
        if (getCall() == null) return;

        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
            log("showInitialFABConfiguration:RING_IN");

            relativeCall.setVisibility(View.VISIBLE);
            statusButton(answerCallFAB, View.VISIBLE);
            linearArrowCall.setVisibility(View.GONE);
            relativeVideo.setVisibility(View.VISIBLE);
            statusButton(videoFAB, View.VISIBLE);
            videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
            videoFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_videocam_white));

            linearArrowVideo.setVisibility(View.GONE);

            statusButton(rejectFAB, View.VISIBLE);
            statusButton(speakerFAB, View.INVISIBLE);
            statusButton(microFAB, View.INVISIBLE);
            statusButton(hangFAB, View.GONE);

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

        } else if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT || callChat.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
            log("showInitialFABConfiguration:REQUEST_SENT || IN_PROGRESS");

            relativeVideo.setVisibility(View.VISIBLE);
            statusButton(speakerFAB, View.VISIBLE);
            statusButton(microFAB, View.VISIBLE);
            statusButton(videoFAB, View.VISIBLE);
            statusButton(hangFAB, View.VISIBLE);
            linearArrowVideo.setVisibility(View.GONE);
            statusButton(answerCallFAB, View.INVISIBLE);
            relativeCall.setVisibility(View.INVISIBLE);
            linearArrowCall.setVisibility(View.GONE);
            statusButton(rejectFAB, View.INVISIBLE);

        }

        displayedBigRecyclerViewLayout(true);
    }

    private void animationButtons(final boolean isVideo) {
        log("animationButtons:isVideo: " + isVideo);

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
                    answerCallFAB.setVisibility(View.INVISIBLE);
                    answerCall(false);
                } else {
                    videoFAB.setVisibility(View.INVISIBLE);
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
        log("updateLocalVideoStatus");
        if (getCall() == null) return;

        int callStatus = callChat.getStatus();
        if (chat.isGroup()) {
            if (callChat.hasLocalVideo()) {
                log("updateLocalVideoStatus:group:Video local connected");
                videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_white));
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
                log("onUpdateLocalVideoStatus:group:Video local NOT connected");
                videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_off));
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
            log("updateLocalVideoStatus:individual");
            if (callChat.hasLocalVideo()) {
                log("updateLocalVideoStatus:Video local connected");
                videoFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
                videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_videocam_white));

                if (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                    log("callStatus: CALL_STATUS_REQUEST_SENT");
                    optionsLocalCameraFragmentFS(true);
                } else if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                    log("callStatus: CALL_STATUS_IN_PROGRESS");
                    optionsLocalCameraFragment(true);
                }
            } else {
                log("updateLocalVideoStatus:Video local NOT connected");
                videoFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                videoFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_video_off));

                if (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                    log("callStatus: CALL_STATUS_REQUEST_SENT");
                    optionsLocalCameraFragmentFS(false);
                } else if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                    log("callStatus: CALL_STATUS_IN_PROGRESS ");
                    optionsLocalCameraFragment(false);
                }
            }
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
        log("updateLocalAudioStatus");
        if (getCall() == null) return;

        if (chat.isGroup()) {
            int position;
            if (callChat.hasLocalAudio()) {
                log("updateLocalAudioStatus:group:Audio local connected");
                microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
                microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_record_audio_w));
                if (peersOnCall.isEmpty()) return;
                position = peersOnCall.size() - 1;
                if (peersOnCall.get(position).isAudioOn()) return;
                peersOnCall.get(position).setAudioOn(true);
            } else {
                log("updateLocalAudioStatus:group:Audio local NOT connected");
                microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_off));
                if (peersOnCall.isEmpty()) return;
                position = peersOnCall.size() - 1;
                if (!peersOnCall.get(position).isAudioOn()) return;
                peersOnCall.get(position).setAudioOn(false);
            }
            updateChangesAudio(position);
        } else {
            if (callChat.hasLocalAudio()) {
                log("updateLocalAudioStatus:individual:Audio local connected");
                microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accentColor)));
                microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_record_audio_w));
            } else {
                log("updateLocalAudioStatus:individual:Audio local NOT connected");
                microFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
                microFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_off));
            }
            refreshOwnMicro();
        }
    }

    private void updateLocalSpeakerStatus() {
        log("updateLocalSpeakerStatus");
        if (rtcAudioManager == null) {
            rtcAudioManager = AppRTCAudioManager.create(getApplicationContext(), ((MegaApplication) getApplication()).getSpeakerStatus(callChat.getChatid()));
            rtcAudioManager.start(null);
        }
        log("updateLocalSpeakerStatus enable speaker");
        rtcAudioManager.activateSpeaker(((MegaApplication) getApplication()).getSpeakerStatus(callChat.getChatid()));
        if (((MegaApplication) getApplication()).getSpeakerStatus(callChat.getChatid())) {
            speakerFAB.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.accentColor)));
            speakerFAB.setImageDrawable(getResources().getDrawable(R.drawable.ic_speaker_on));
        } else {
            speakerFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.disable_fab_chat_call)));
            speakerFAB.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_speaker_off));
        }
        ((MegaApplication) getApplication()).audioManagerStatus(callChat);

    }


    private void updateRemoteVideoStatus(long userPeerId, long userClientId) {
        log("updateRemoteVideoStatus (peerid = " + userPeerId + ", clientid = " + userClientId + ")");
        if (getCall() == null) return;

        if (chat.isGroup()) {
            MegaChatSession userSession = callChat.getMegaChatSession(userPeerId, userClientId);
            if (userSession == null || peersOnCall.isEmpty()) return;

            if (userSession.hasVideo()) {
                log("updateRemoteVideoStatus: Contact -> Video remote connected");
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
                log("updateRemoteVideoStatus: Contact -> Video remote NO connected");
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
            log("updateRemoteVideoStatus:individual");
            MegaChatSession userSession = callChat.getMegaChatSession(callChat.getSessionsPeerid().get(0), callChat.getSessionsClientid().get(0));
            if (userSession == null) return;
            if (isRemoteVideo == REMOTE_VIDEO_NOT_INIT) {
                if (userSession.hasVideo()) {
                    log("updateRemoteVideoStatus:REMOTE_VIDEO_NOT_INIT Contact Video remote connected");
                    optionsRemoteCameraFragmentFS(true);
                } else {
                    log("updateRemoteVideoStatus:REMOTE_VIDEO_NOT_INIT Contact Video remote NOT connected");
                    optionsRemoteCameraFragmentFS(false);
                }
            } else {
                if (isRemoteVideo == REMOTE_VIDEO_ENABLED && !userSession.hasVideo()) {
                    log("updateRemoteVideoStatus:REMOTE_VIDEO_ENABLED Contact Video remote connected");
                    optionsRemoteCameraFragmentFS(false);

                } else if ((isRemoteVideo == REMOTE_VIDEO_DISABLED) && userSession.hasVideo()) {
                    log("updateRemoteVideoStatus:REMOTE_VIDEO_DISABLED Contact Video remote connected");
                    optionsRemoteCameraFragmentFS(true);
                }
            }
        }
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
        log("updateRemoteAudioStatus (peerid = " + userPeerId + ", clientid = " + userClientId + ")");
        if (getCall() == null) return;

        if (chat.isGroup()) {
            MegaChatSession userSession = callChat.getMegaChatSession(userPeerId, userClientId);
            if (userSession == null || peersOnCall.isEmpty()) return;

            if (userSession.hasAudio()) {
                log("updateRemoteAudioStatus:group Contact -> Audio remote connected");
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
                log("updateRemoteAudioStatus: Contact -> Audio remote NO connected");
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
            log("updateRemoteAudioStatus:individual");
            refreshContactMicro();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        log("onRequestPermissionsResult");

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.REQUEST_CAMERA: {
                log("REQUEST_CAMERA");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPermissions()) {
//                        checkPermissionsWriteLog();
                        showInitialFABConfiguration();
                    }
                } else {
                    rejectFAB.setVisibility(View.VISIBLE);
                }
                break;
            }
            case Constants.RECORD_AUDIO: {
                log("RECORD_AUDIO");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPermissions()) {
//                        checkPermissionsWriteLog();
                        showInitialFABConfiguration();
                    }
                } else {
                    rejectFAB.setVisibility(View.VISIBLE);
                }
                break;
            }
            case Constants.WRITE_LOG: {
                log("WRITE_LOG");
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    checkPermissionsWriteLog();
//                }
                break;
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values[0] == 0 || !wakeLock.isHeld()) {
            wakeLock.acquire();
        } else if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    public void remoteCameraClick() {
        log("remoteCameraClick");
        if (getCall() == null || callChat.getStatus() != MegaChatCall.CALL_STATUS_IN_PROGRESS)
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

        log("itemClicked:userSelected: -> (peerId = " + peer.getPeerId() + ", clientId = " + peer.getClientId() + ")");
        if (peerSelected.getPeerId() == peer.getPeerId() && peerSelected.getClientId() == peer.getClientId()) {
            //I touched the same user that is now in big fragment:
            if (isManualMode) {
                isManualMode = false;
                log("manual mode - False");
            } else {
                isManualMode = true;
                log("manual mode - True");
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
            log("itemClicked:Click myself - do nothing");
        } else {
            //contact
            if (!isManualMode) {
                isManualMode = true;
                if (adapterList != null) {
                    adapterList.updateMode(true);
                }
                log("manual mode - True");
            }
            peerSelected = peer;
            updateUserSelected();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }



    private void answerCall(boolean isVideoCall) {
        log("answerCall");
        clearHandlers();
        if (megaChatApi == null) return;

        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }
        ((MegaApplication) getApplication()).setSpeakerStatus(callChat.getChatid(), isVideoCall);
        megaChatApi.answerChatCall(chatId, isVideoCall, this);
    }

    private void animationAlphaArrows(final ImageView arrow) {
        log("animationAlphaArrows");

        AlphaAnimation alphaAnimArrows = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimArrows.setDuration(ALPHA_ARROW_ANIMATION);
        alphaAnimArrows.setFillAfter(true);
        alphaAnimArrows.setFillBefore(true);
        alphaAnimArrows.setRepeatCount(Animation.INFINITE);
        arrow.startAnimation(alphaAnimArrows);
    }

    private void updateSubTitle() {
        log("updateSubTitle");

        if (getCall() == null) return;

        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
            log("updateSubTitle:REQUEST_SENT");
            subtitleToobar.setVisibility(View.VISIBLE);
            ChatUtil.activateChrono(false, callInProgressChrono, callChat);
            subtitleToobar.setText(getString(R.string.outgoing_call_starting));
            return;
        }

        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
            log("*updateSubTitle:RING_IN");
            subtitleToobar.setVisibility(View.VISIBLE);
            ChatUtil.activateChrono(false, callInProgressChrono, callChat);
            subtitleToobar.setText(getString(R.string.incoming_call_starting));
            return;

        }
        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS || callChat.getStatus() == MegaChatCall.CALL_STATUS_JOINING) {
            log("updateSubTitle:IN_PROGRESS || JOINING -- " + callChat.getStatus());
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
                    log("session in progress");
                    subtitleToobar.setVisibility(View.GONE);
                    ChatUtil.activateChrono(true, callInProgressChrono, callChat);
                    return;
                }

                log("Error getting the session of the user or session not in progress ");
                subtitleToobar.setText(getString(R.string.chat_connecting));
                subtitleToobar.setVisibility(View.VISIBLE);
                ChatUtil.activateChrono(false, callInProgressChrono, callChat);
                return;
            }

            log("updateSubTitle:individual call in progress");

            linearParticipants.setVisibility(View.GONE);
            MegaChatSession userSession = callChat.getMegaChatSession(callChat.getSessionsPeerid().get(0), callChat.getSessionsClientid().get(0));
            if (userSession == null) {
                log("updateSubTitle: userSession == null");
                subtitleToobar.setText(getString(R.string.chat_connecting));
                subtitleToobar.setVisibility(View.VISIBLE);
                ChatUtil.activateChrono(false, callInProgressChrono, callChat);
                return;
            }

            if (userSession.getStatus() == MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
                log("session in progress");
                subtitleToobar.setVisibility(View.GONE);
                ChatUtil.activateChrono(true, callInProgressChrono, callChat);
                return;
            }
        }
        subtitleToobar.setVisibility(View.GONE);
        ChatUtil.activateChrono(false, callInProgressChrono, callChat);
    }

    private void updateSubtitleNumberOfVideos() {
        log("updateSubtitleNumberOfVideos");
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
        log("updatePeers");
        if (getCall() == null) return;

        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN || (callChat.getStatus() >= MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION && callChat.getStatus() <= MegaChatCall.CALL_STATUS_USER_NO_PRESENT)) {
            //I'M NOT IN THE CALL
            log("updatePeers:incoming call: peersBeforeCall");
            clearArrayList(peersOnCall);

            linearParticipants.setVisibility(View.GONE);

            if (peersBeforeCall.isEmpty()) {
                resetPeers();
                return;
            }
            if (!notYetJoinedTheCall) {
                notYetJoinedTheCall = true;
            }

            log("updatePeers:incoming call: peersBeforeCall not empty");
            if (peersBeforeCall.size() <= MAX_PEERS_GRID) {
                log("updatePeers:peersBeforeCall GRID ");
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
                        recyclerViewLayout.setPadding(0, Util.scaleWidthPx(136, outMetrics), 0, 0);
                    } else {
                        recyclerViewLayout.setPadding(0, 0, 0, 0);
                    }
                    recyclerView.setColumnWidth((int) widthScreenPX / 2);
                }

                if (adapterGrid == null) {
                    log("create adapter(1º)");
                    recyclerView.setAdapter(null);
                    adapterGrid = new GroupCallAdapter(this, recyclerView, peersBeforeCall, chatId, true);
                    recyclerView.setAdapter(adapterGrid);
                } else {
                    log("notifyDataSetChanged");
                    adapterGrid.notifyDataSetChanged();
                }

            } else {
                log("updatePeers:peersBeforeCall LIST ");
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
                    log("create adapter(1º)");
                    bigRecyclerView.setAdapter(null);
                    adapterList = new GroupCallAdapter(this, bigRecyclerView, peersBeforeCall, chatId, false);
                    bigRecyclerView.setAdapter(adapterList);
                } else {
                    log("notifyDataSetChanged");
                    adapterList.notifyDataSetChanged();
                }
                updateUserSelected();
            }

        } else {
            //I'M IN THE CALL.
            log("updatePeers:in progress call");
            clearArrayList(peersBeforeCall);

            if (peersOnCall.isEmpty()) {
                resetPeers();
                return;
            }

            if (peersOnCall.size() <= MAX_PEERS_GRID) {
                log("updatePeers:peersOnCall GRID ");
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
                        recyclerViewLayout.setPadding(0, Util.scaleWidthPx(136, outMetrics), 0, 0);
                    } else {
                        recyclerViewLayout.setPadding(0, 0, 0, 0);
                    }
                    recyclerView.setColumnWidth((int) widthScreenPX / 2);
                }

                if (adapterGrid == null) {
                    log("create adapter(1º)");
                    recyclerView.setAdapter(null);
                    adapterGrid = new GroupCallAdapter(this, recyclerView, peersOnCall, chatId, true);
                    recyclerView.setAdapter(adapterGrid);

                } else {
                    if (notYetJoinedTheCall) {
                        log("create adapter(2º)");
                        destroyAdapter(false);
                        recyclerView.setAdapter(null);
                        adapterGrid = new GroupCallAdapter(this, recyclerView, peersOnCall, chatId, true);
                        recyclerView.setAdapter(adapterGrid);
                    } else {
                        log("notifyDataSetChanged");
                        adapterGrid.notifyDataSetChanged();
                    }
                }

            } else {
                log("updatePeers:peersOnCall LIST ");
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
                    log("create adapter(1º)");
                    bigRecyclerView.setAdapter(null);
                    adapterList = new GroupCallAdapter(this, bigRecyclerView, peersOnCall, chatId, false);
                    bigRecyclerView.setAdapter(adapterList);
                } else {
                    if (notYetJoinedTheCall) {
                        log("create adapter(2º)");
                        destroyAdapter(true);
                        bigRecyclerView.setAdapter(null);
                        adapterList = new GroupCallAdapter(this, bigRecyclerView, peersOnCall, chatId, false);
                        bigRecyclerView.setAdapter(adapterList);
                    } else {
                        log("notifyDataSetChanged");
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
        log("updateUserSelected");
        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN || (callChat.getStatus() >= MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION && callChat.getStatus() <= MegaChatCall.CALL_STATUS_USER_NO_PRESENT)) {
            //I'M NOT IN THE CALL
            log("updateUserSelected: INCOMING");
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
            log("updateUserSelected: IN PROGRESS");

            if (peersOnCall.isEmpty()) return;

            if (peerSelected == null) {
                log("updateUserSelected:peerSelected == null");

                if (isManualMode) return;

                int position = 0;
                peerSelected = peersOnCall.get(position);
                log("updateUserSelected:InProgress - new peerSelected (peerId = " + peerSelected.getPeerId() + ", clientId = " + peerSelected.getClientId() + ")");
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
                log("updateUserSelected:peerSelected != null");

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
                    log("updateUserSelected:InProgress - new peerSelected (peerId = " + peerSelected.getPeerId() + ", clientId = " + peerSelected.getClientId() + ")");
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

                    log("updateUserSelected:InProgress - peerSelected (peerId = " + peerSelected.getPeerId() + ", clientId = " + peerSelected.getClientId() + ")");
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
        log("createBigFragment()");
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
        log("createBigAvatar()");
        removeBigFragment();

        fragmentBigCameraGroupCall.setVisibility(View.GONE);
        avatarBigCameraGroupCallImage.setImageBitmap(null);
        setProfilePeerSelected(peerSelected.getPeerId(), peerSelected.getName(), null);
        parentBigCameraGroupCall.setVisibility(View.VISIBLE);
        avatarBigCameraGroupCallLayout.setVisibility(View.VISIBLE);
    }

    private void clearSurfacesViews() {
        log("clearSurfacesViews");
        removeLocalCameraFragment();
        removeLocalCameraFragmentFS();
        removeRemoteCameraFragmentFS();
        removeBigFragment();
    }

    private void clearHandlers() {
        log("clearHandlers");
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
        ChatUtil.activateChrono(false, callInProgressChrono, callChat);
    }

    public void showSnackbar(String s) {
        log("showSnackbar: " + s);
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
        log("updateNonContactName: Email found it");
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
        log("checkParticipants");
        if (getCall() == null) return;

        if (callChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN || (callChat.getStatus() >= MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION && callChat.getStatus() <= MegaChatCall.CALL_STATUS_USER_NO_PRESENT)) {
            log("checkParticipants: RING_IN || TERMINATING_USER_PARTICIPATION || USER_NO_PRESENT");
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
                            log("checkParticipants " + peersBeforeCall.get(i).getPeerId() + " removed from peersBeforeCall");
                            peersBeforeCall.remove(i);
                            changes = true;
                        }
                    }
                }

                if (changes) {
                    updatePeers();
                }
            } else {
                log("peersBeforeCall is empty -> add all");
                for (int i = 0; i < callChat.getPeeridParticipants().size(); i++) {
                    long userPeerid = callChat.getPeeridParticipants().get(i);
                    long userClientid = callChat.getClientidParticipants().get(i);
                    InfoPeerGroupCall userPeer = new InfoPeerGroupCall(userPeerid, userClientid, getName(userPeerid), false, false, false, true, null);
                    log("checkParticipants " + userPeer.getPeerId() + " added in peersBeforeCall");
                    addContactIntoArray(userPeerid, userClientid);
                }
                updatePeers();
            }

        } else {
            log("checkParticipants: IN PROGRESS || JOINING");

            if (megaChatApi == null || callChat.getPeeridParticipants().size() <= 0) return;


            //peersOnCall
            if (!peersOnCall.isEmpty()) {

                boolean changes = false;
                //Get all participant and check it (some will be added and others will be removed)
                for (int i = 0; i < callChat.getPeeridParticipants().size(); i++) {
                    boolean peerContain = false;
                    long userPeerid = callChat.getPeeridParticipants().get(i);
                    long userClientid = callChat.getClientidParticipants().get(i);

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
                    for (int j = 0; j < callChat.getPeeridParticipants().size(); j++) {
                        long userPeerid = callChat.getPeeridParticipants().get(j);
                        long userClientid = callChat.getClientidParticipants().get(j);
                        if (peersOnCall.get(i).getPeerId() == userPeerid && peersOnCall.get(i).getClientId() == userClientid) {
                            peerContained = true;
                            break;
                        }
                    }
                    if (!peerContained) {
                        log(peersOnCall.get(i).getPeerId() + " removed of peersOnCall");
                        peersOnCall.remove(i);
                        changes = true;
                    }
                }

                if (changes) updatePeers();

            } else {
                //peersOnCall empty
                log("Add all participants ");
                addMeIntoArray();
                for (int i = 0; i < callChat.getPeeridParticipants().size(); i++) {
                    long userPeerid = callChat.getPeeridParticipants().get(i);
                    long userClientid = callChat.getClientidParticipants().get(i);

                    if (userPeerid != megaChatApi.getMyUserHandle() || userClientid != megaChatApi.getMyClientidHandle(chatId)) {
                        MegaChatSession sessionPeer = callChat.getMegaChatSession(userPeerid, userClientid);
                        if (sessionPeer == null) break;
                        if (sessionPeer.getStatus() <= MegaChatSession.SESSION_STATUS_IN_PROGRESS) {
                            addContactIntoArray(userPeerid, userClientid);
                        }
                    }
                }
                updatePeers();

            }
            if (peersOnCall.isEmpty()) return;

            log("checkParticipants update Video&Audio local&remoto");
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
        log("refreshOwnMicro");

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
        log("refreshContactMicro");
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
