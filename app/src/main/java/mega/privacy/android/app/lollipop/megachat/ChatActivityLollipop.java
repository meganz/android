package mega.privacy.android.app.lollipop.megachat;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.TimeZone;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.components.BubbleDrawable;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.NpaLinearLayoutManager;
import mega.privacy.android.app.components.twemoji.EmojiEditText;
import mega.privacy.android.app.components.twemoji.EmojiKeyboard;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.components.twemoji.EmojiUtilsShortcodes;
import mega.privacy.android.app.components.twemoji.OnPlaceButtonListener;
import mega.privacy.android.app.components.voiceClip.OnBasketAnimationEnd;
import mega.privacy.android.app.components.voiceClip.OnRecordClickListener;
import mega.privacy.android.app.components.voiceClip.OnRecordListener;
import mega.privacy.android.app.components.voiceClip.RecordButton;
import mega.privacy.android.app.components.voiceClip.RecordView;
import mega.privacy.android.app.interfaces.MyChatFilesExisitListener;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.DownloadableActivity;
import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.ChatLinkInfoListener;
import mega.privacy.android.app.lollipop.listeners.CreateChatToPerformActionListener;
import mega.privacy.android.app.lollipop.listeners.MultipleForwardChatProcessor;
import mega.privacy.android.app.lollipop.listeners.MultipleGroupChatRequestListener;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListener;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatLollipopAdapter;
import mega.privacy.android.app.lollipop.tasks.FilePrepareTask;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.AttachmentUploadBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ContactAttachmentBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.MessageNotSentBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.NodeAttachmentBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.PendingMessageBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.SendAttachmentChatBottomSheetDialogFragment;
import mega.privacy.android.app.utils.TimeUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatCallListenerInterface;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatGeolocation;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatRoomListenerInterface;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop.*;
import static mega.privacy.android.app.lollipop.megachat.AndroidMegaRichLinkMessage.*;
import static mega.privacy.android.app.lollipop.megachat.MapsActivity.*;
import static mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class ChatActivityLollipop extends DownloadableActivity implements MegaChatCallListenerInterface, MegaChatRequestListenerInterface, MegaRequestListenerInterface, MegaChatListenerInterface, MegaChatRoomListenerInterface,  View.OnClickListener, MyChatFilesExisitListener<ArrayList<AndroidMegaChatMessage>> {

    public MegaChatLollipopAdapter.ViewHolderMessageChat holder_imageDrag;
    public int position_imageDrag = -1;
    private static final String PLAYING = "isAnyPlaying";
    private static final String ID_VOICE_CLIP_PLAYING = "idMessageVoicePlaying";
    private static final String PROGRESS_PLAYING = "progressVoicePlaying";
    private static final String MESSAGE_HANDLE_PLAYING = "messageHandleVoicePlaying";
    private static final String USER_HANDLE_PLAYING = "userHandleVoicePlaying";

    private final static int NUMBER_MESSAGES_TO_LOAD = 20;
    private final static int NUMBER_MESSAGES_BEFORE_LOAD = 8;

    private final static int ROTATION_PORTRAIT = 0;
    private final static int ROTATION_LANDSCAPE = 1;
    private final static int ROTATION_REVERSE_PORTRAIT = 2;
    private final static int ROTATION_REVERSE_LANDSCAPE = 3;
    private final static int TITLE_TOOLBAR_PORT = 140;
    private final static int TITLE_TOOLBAR_LAND = 250;
    private final static int TITLE_TOOLBAR_IND_PORT = 100;

    public static int MEGA_FILE_LINK = 1;
    public static int MEGA_FOLDER_LINK = 2;
    public static int MEGA_CHAT_LINK = 3;

    private final static int SHOW_WRITING_LAYOUT = 1;
    private final static int SHOW_JOIN_LAYOUT = 2;
    private final static int SHOW_NOTHING_LAYOUT = 3;
    private final static int INITIAL_PRESENCE_STATUS = -55;
    private final static int RECORD_BUTTON_SEND = 1;
    private final static int RECORD_BUTTON_ACTIVATED = 2;
    private final static int RECORD_BUTTON_DEACTIVATED = 3;

    private final static int PADDING_BUBBLE = 25;
    private final static int CORNER_RADIUS_BUBBLE = 30;
    private final static int MAX_WIDTH_BUBBLE = 350;
    private final static int MARGIN_BUTTON_DEACTIVATED = 48;
    private final static int MARGIN_BUTTON_ACTIVATED = 24;
    private final static int MARGIN_BOTTOM = 80;
    private final static int DURATION_BUBBLE = 4000;

    private final static int TYPE_MESSAGE_JUMP_TO_LEAST = 0;
    private final static int TYPE_MESSAGE_NEW_MESSAGE = 1;

    private int currentRecordButtonState = 0;
    private String mOutputFilePath;
    private int keyboardHeight;
    private int marginBottomDeactivated;
    private int marginBottomActivated;
    boolean newVisibility = false;
    boolean getMoreHistory=false;
    int minutesLastGreen = -1;
    boolean isLoadingHistory = false;
    private AlertDialog errorOpenChatDialog;
    long numberToLoad = -1;

    private android.support.v7.app.AlertDialog downloadConfirmationDialog;
    private AlertDialog chatAlertDialog;

    ProgressDialog dialog;
    ProgressDialog statusDialog;

    boolean retryHistory = false;

    public long lastIdMsgSeen = -1;
    public long generalUnreadCount = -1;
    boolean lastSeenReceived = false;
    int positionToScroll = -1;
    public int positionNewMessagesLayout = -1;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    Handler handlerReceive;
    Handler handlerSend;
    Handler handlerKeyboard;
    Handler handlerEmojiKeyboard;

    private TextView emptyTextView;
    private ImageView emptyImageView;
    private RelativeLayout emptyLayout;

    boolean pendingMessagesLoaded = false;

    public boolean activityVisible = false;
    boolean setAsRead = false;

    boolean isOpeningChat = true;

    int selectedPosition;
    public long selectedMessageId = -1;
    MegaChatRoom chatRoom;

    public long idChat;

    boolean noMoreNoSentMessages = false;

    public int showRichLinkWarning = RICH_WARNING_TRUE;

    private BadgeDrawerArrowDrawable badgeDrawable;

    ChatController chatC;
    boolean scrollingUp = false;

    long myUserHandle;

    ActionBar aB;
    Toolbar tB;
    RelativeLayout toolbarElementsInside;

    private EmojiTextView titleToolbar;
    private MarqueeTextView individualSubtitleToobar;
    private EmojiTextView groupalSubtitleToolbar;
    LinearLayout subtitleCall;
    Chronometer subtitleChronoCall;
    LinearLayout participantsLayout;
    TextView participantsText;
    ImageView iconStateToolbar;

    ImageView privateIconToolbar;

    float density;
    DisplayMetrics outMetrics;
    Display display;

    boolean editingMessage = false;
    MegaChatMessage messageToEdit = null;

    CoordinatorLayout fragmentContainer;
    RelativeLayout writingContainerLayout;
    RelativeLayout writingLayout;

    RelativeLayout joinChatLinkLayout;
    Button joinButton;

    RelativeLayout chatRelativeLayout;
    RelativeLayout userTypingLayout;
    TextView userTypingText;
    boolean sendIsTyping=true;
    long userTypingTimeStamp = -1;
    private ImageButton keyboardTwemojiButton;
    ImageButton mediaButton;
    ImageButton pickFileStorageButton;
    ImageButton pickAttachButton;

    private EmojiKeyboard emojiKeyboard;
    private RelativeLayout rLKeyboardTwemojiButton;

    RelativeLayout rLMediaButton;
    RelativeLayout rLPickFileStorageButton;
    RelativeLayout rLPickAttachButton;

    RelativeLayout callInProgressLayout;
    TextView callInProgressText;
    Chronometer callInProgressChrono;

    boolean startVideo = false;

    private EmojiEditText textChat;
    ImageButton sendIcon;
    RelativeLayout messagesContainerLayout;

    RelativeLayout observersLayout;
    TextView observersNumberText;

    RecyclerView listView;
    NpaLinearLayoutManager mLayoutManager;

    ChatActivityLollipop chatActivity;

    MenuItem importIcon;
    MenuItem callMenuItem;
    MenuItem videoMenuItem;
    MenuItem inviteMenuItem;
    MenuItem clearHistoryMenuItem;
    MenuItem contactInfoMenuItem;
    MenuItem leaveMenuItem;
    MenuItem archiveMenuItem;

    String intentAction;
    MegaChatLollipopAdapter adapter;
    int stateHistory;

    DatabaseHandler dbH = null;

    FrameLayout fragmentContainerFileStorage;
    RelativeLayout fileStorageLayout;
    private ChatFileStorageFragment fileStorageF;

    private ArrayList<AndroidMegaChatMessage> messages = new ArrayList<>();
    private ArrayList<AndroidMegaChatMessage> bufferMessages = new ArrayList<>();
    private ArrayList<AndroidMegaChatMessage> bufferSending = new ArrayList<>();
    private ArrayList<MessageVoiceClip> messagesPlaying =  new ArrayList<>();

    RelativeLayout messageJumpLayout;
    TextView messageJumpText;
    boolean isHideJump = false;
    int typeMessageJump = 0;
    boolean visibilityMessageJump=false;
    boolean isTurn = false;
    Handler handler;

    private AlertDialog locationDialog;
    private boolean isLocationDialogShown = false;
    private RelativeLayout inputTextLayout;
    private LinearLayout separatorOptions;

    /*Voice clips*/
    private String outputFileVoiceNotes = null;
    private String outputFileName = "";
    private RelativeLayout recordLayout;
    private RelativeLayout recordButtonLayout;
    private RecordButton recordButton;
    private MediaRecorder myAudioRecorder = null;
    private LinearLayout bubbleLayout;
    private TextView bubbleText;
    private RecordView recordView;
    private FrameLayout fragmentVoiceClip;
    private RelativeLayout voiceClipLayout;

    private boolean isShareLinkDialogDismissed = false;

    private ActionMode actionMode;

    // Data being stored when My Chat Files folder does not exist
    private ArrayList<AndroidMegaChatMessage> preservedMessagesSelected;
    // The flag to indicate whether forwarding message is on going
    private boolean isForwardingMessage = false;

    private BottomSheetDialogFragment bottomSheetDialogFragment;

    @Override
    public void storedUnhandledData(ArrayList<AndroidMegaChatMessage> preservedData) {
        this.preservedMessagesSelected = preservedData;
    }

    @Override
    public void handleStoredData() {
        forwardMessages(preservedMessagesSelected);
        preservedMessagesSelected = null;
    }

    private class UserTyping {
        MegaChatParticipant participantTyping;
        long timeStampTyping;

        public UserTyping(MegaChatParticipant participantTyping) {
            this.participantTyping = participantTyping;
        }

        public MegaChatParticipant getParticipantTyping() {
            return participantTyping;
        }

        public void setParticipantTyping(MegaChatParticipant participantTyping) {
            this.participantTyping = participantTyping;
        }

        public long getTimeStampTyping() {
            return timeStampTyping;
        }

        public void setTimeStampTyping(long timeStampTyping) {
            this.timeStampTyping = timeStampTyping;
        }
    }

    private BroadcastReceiver voiceclipDownloadedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                long nodeHandle = intent.getLongExtra(EXTRA_NODE_HANDLE, 0);
                int resultTransfer = intent.getIntExtra(EXTRA_RESULT_TRANSFER,0);
                if(adapter!=null){
                    adapter.finishedVoiceClipDownload(nodeHandle, resultTransfer);
                }
            }
        }
    };

    private BroadcastReceiver dialogConnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logDebug("Network broadcast received on chatActivity!");
            if (intent != null){
                showConfirmationConnect();
            }
        }
    };

    private BroadcastReceiver chatArchivedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            String title = intent.getStringExtra(CHAT_TITLE);
            sendBroadcastChatArchived(title);
        }
    };

    ArrayList<UserTyping> usersTyping;
    List<UserTyping> usersTypingSync;

    public void openMegaLink(String url, boolean isFile){
        logDebug("url: " + url + ", isFile: " + isFile);
        if(isFile){
            Intent openFileIntent = new Intent(this, FileLinkActivityLollipop.class);
            openFileIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openFileIntent.setAction(ACTION_OPEN_MEGA_LINK);
            openFileIntent.setData(Uri.parse(url));
            startActivity(openFileIntent);
        }else{
            Intent openFolderIntent = new Intent(this, FolderLinkActivityLollipop.class);
            openFolderIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            openFolderIntent.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);
            openFolderIntent.setData(Uri.parse(url));
            startActivity(openFolderIntent);
        }
    }

    public void showMessageInfo(int positionInAdapter){
        logDebug("showMessageInfo");
        int position = positionInAdapter-1;

        if(position<messages.size()) {
            AndroidMegaChatMessage androidM = messages.get(position);
            StringBuilder messageToShow = new StringBuilder("");
            String token = FirebaseInstanceId.getInstance().getToken();
            if(token!=null){
                messageToShow.append("FCM TOKEN: " +token);
            }
            messageToShow.append("\nCHAT ID: " + MegaApiJava.userHandleToBase64(idChat));
            messageToShow.append("\nMY USER HANDLE: " +MegaApiJava.userHandleToBase64(megaChatApi.getMyUserHandle()));
            if(androidM!=null){
                MegaChatMessage m = androidM.getMessage();
                if(m!=null){
                    messageToShow.append("\nMESSAGE TYPE: " +m.getType());
                    messageToShow.append("\nMESSAGE TIMESTAMP: " +m.getTimestamp());
                    messageToShow.append("\nMESSAGE USERHANDLE: " +MegaApiJava.userHandleToBase64(m.getUserHandle()));
                    messageToShow.append("\nMESSAGE ID: " +MegaApiJava.userHandleToBase64(m.getMsgId()));
                    messageToShow.append("\nMESSAGE TEMP ID: " +MegaApiJava.userHandleToBase64(m.getTempId()));
                }
            }
            Toast.makeText(this, messageToShow, Toast.LENGTH_SHORT).show();
        }
    }

    public void showGroupInfoActivity(){
        logDebug("showGroupInfoActivity");
        if(chatRoom.isGroup()){
            Intent i = new Intent(this, GroupChatInfoActivityLollipop.class);
            i.putExtra("handle", chatRoom.getChatId());
            this.startActivity(i);
        }else{
            Intent i = new Intent(this, ContactInfoActivityLollipop.class);
            i.putExtra("handle", chatRoom.getChatId());
            this.startActivity(i);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logDebug("onCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        if (megaApi == null) {
            MegaApplication app = (MegaApplication) getApplication();
            megaApi = app.getMegaApi();
        }

        if (megaChatApi == null) {
            MegaApplication app = (MegaApplication) getApplication();
            megaChatApi = app.getMegaChatApi();
        }

        if (megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR || megaChatApi.getInitState() == MegaChatApi.INIT_NOT_DONE) {
            logDebug("Refresh session - karere");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra("visibleFragment", LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        megaChatApi.addChatListener(this);
        megaChatApi.addChatCallListener(this);

        dbH = DatabaseHandler.getDbHandler(this);

        handler = new Handler();

        chatActivity = this;
        chatC = new ChatController(chatActivity);

        LocalBroadcastManager.getInstance(this).registerReceiver(dialogConnectReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE_DIALOG));
        LocalBroadcastManager.getInstance(this).registerReceiver(voiceclipDownloadedReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_VOICE_CLIP_DOWNLOADED));
        LocalBroadcastManager.getInstance(this).registerReceiver(chatArchivedReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_CHAT_ARCHIVED_GROUP));

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));

        setContentView(R.layout.activity_chat);
        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        //Set toolbar
        tB = findViewById(R.id.toolbar_chat);

        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);
        aB.setTitle(null);
        aB.setSubtitle(null);
        tB.setOnClickListener(this);

        toolbarElementsInside = tB.findViewById(R.id.toolbar_elements_inside);
        titleToolbar = tB.findViewById(R.id.title_toolbar);
        iconStateToolbar = tB.findViewById(R.id.state_icon_toolbar);
        privateIconToolbar = tB.findViewById(R.id.private_icon_toolbar);

        individualSubtitleToobar = tB.findViewById(R.id.individual_subtitle_toolbar);
        groupalSubtitleToolbar = tB.findViewById(R.id.groupal_subtitle_toolbar);

        subtitleCall = tB.findViewById(R.id.subtitle_call);
        subtitleChronoCall = tB.findViewById(R.id.chrono_call);
        participantsLayout = tB.findViewById(R.id.ll_participants);
        participantsText = tB.findViewById(R.id.participants_text);

        textChat = findViewById(R.id.edit_text_chat);
        textChat.setVisibility(View.VISIBLE);
        textChat.setEnabled(true);

        emptyLayout = findViewById(R.id.empty_messages_layout);
        emptyTextView = findViewById(R.id.empty_text_chat_recent);
        emptyImageView = findViewById(R.id.empty_image_view_chat);

        fragmentContainer = findViewById(R.id.fragment_container_chat);
        writingContainerLayout = findViewById(R.id.writing_container_layout_chat_layout);
        inputTextLayout = findViewById(R.id.write_layout);
        separatorOptions = findViewById(R.id.separator_layout_options);

        titleToolbar.setText("");
        individualSubtitleToobar.setText("");
        individualSubtitleToobar.setVisibility(View.GONE);
        groupalSubtitleToolbar.setText("");
        groupalSubtitleToolbar.setVisibility(View.GONE);
        subtitleCall.setVisibility(View.GONE);
        subtitleChronoCall.setVisibility(View.GONE);
        participantsLayout.setVisibility(View.GONE);
        iconStateToolbar.setVisibility(View.GONE);
        privateIconToolbar.setVisibility(View.GONE);
        badgeDrawable = new BadgeDrawerArrowDrawable(getSupportActionBar().getThemedContext());
        updateNavigationToolbarIcon();

        joinChatLinkLayout = findViewById(R.id.join_chat_layout_chat_layout);
        joinButton = findViewById(R.id.join_button);
        joinButton.setOnClickListener(this);

        messageJumpLayout = findViewById(R.id.message_jump_layout);
        messageJumpText = findViewById(R.id.message_jump_text);
        messageJumpLayout.setVisibility(View.GONE);
        writingLayout = findViewById(R.id.writing_linear_layout_chat);

        rLKeyboardTwemojiButton = findViewById(R.id.rl_keyboard_twemoji_chat);
        rLMediaButton = findViewById(R.id.rl_media_icon_chat);
        rLPickFileStorageButton = findViewById(R.id.rl_pick_file_storage_icon_chat);
        rLPickAttachButton = findViewById(R.id.rl_attach_icon_chat);

        keyboardTwemojiButton = findViewById(R.id.keyboard_twemoji_chat);
        mediaButton = findViewById(R.id.media_icon_chat);
        pickFileStorageButton = findViewById(R.id.pick_file_storage_icon_chat);
        pickAttachButton = findViewById(R.id.pick_attach_chat);


        keyboardHeight = outMetrics.heightPixels / 2 - getActionBarHeight(this, getResources());
        marginBottomDeactivated = px2dp(MARGIN_BUTTON_DEACTIVATED, outMetrics);
        marginBottomActivated = px2dp(MARGIN_BUTTON_ACTIVATED, outMetrics);

        callInProgressLayout = findViewById(R.id.call_in_progress_layout);
        callInProgressLayout.setVisibility(View.GONE);
        callInProgressText = findViewById(R.id.call_in_progress_text);
        callInProgressChrono = findViewById(R.id.call_in_progress_chrono);
        callInProgressChrono.setVisibility(View.GONE);

        enableButton(rLKeyboardTwemojiButton, keyboardTwemojiButton);
        enableButton(rLMediaButton, mediaButton);
        enableButton(rLPickAttachButton, pickAttachButton);
        enableButton(rLPickFileStorageButton, pickFileStorageButton);

        messageJumpLayout.setOnClickListener(this);

        fragmentContainerFileStorage = findViewById(R.id.fragment_container_file_storage);
        fileStorageLayout = findViewById(R.id.relative_layout_file_storage);
        fileStorageLayout.setVisibility(View.GONE);
        pickFileStorageButton.setImageResource(R.drawable.ic_b_select_image);

        chatRelativeLayout  = findViewById(R.id.relative_chat_layout);

        sendIcon = findViewById(R.id.send_message_icon_chat);
        sendIcon.setOnClickListener(this);
        sendIcon.setEnabled(true);

        //Voice clip elements
        voiceClipLayout =  findViewById(R.id.voice_clip_layout);
        fragmentVoiceClip = findViewById(R.id.fragment_voice_clip);
        recordLayout = findViewById(R.id.layout_button_layout);
        recordButtonLayout = findViewById(R.id.record_button_layout);
        recordButton = findViewById(R.id.record_button);
        recordButton.setEnabled(true);
        recordButton.setHapticFeedbackEnabled(true);
        recordView = findViewById(R.id.record_view);
        recordView.setVisibility(View.GONE);
        bubbleLayout = findViewById(R.id.bubble_layout);
        BubbleDrawable myBubble = new BubbleDrawable(BubbleDrawable.CENTER, ContextCompat.getColor(this,R.color.turn_on_notifications_text));
        myBubble.setCornerRadius(CORNER_RADIUS_BUBBLE);
        myBubble.setPointerAlignment(BubbleDrawable.RIGHT);
        myBubble.setPadding(PADDING_BUBBLE, PADDING_BUBBLE, PADDING_BUBBLE, PADDING_BUBBLE);
        bubbleLayout.setBackground(myBubble);
        bubbleLayout.setVisibility(View.GONE);
        bubbleText = findViewById(R.id.bubble_text);
        bubbleText.setMaxWidth(px2dp(MAX_WIDTH_BUBBLE, outMetrics));
        recordButton.setRecordView(recordView);
        myAudioRecorder = new MediaRecorder();
        showInputText();

        //Input text:
        handlerKeyboard = new Handler();
        handlerEmojiKeyboard = new Handler();

        emojiKeyboard = findViewById(R.id.emojiView);
        emojiKeyboard.init(this, textChat, keyboardTwemojiButton);
        emojiKeyboard.setListenerActivated(true);

        observersLayout = findViewById(R.id.observers_layout);
        observersNumberText = findViewById(R.id.observers_text);

        textChat.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s != null && !s.toString().isEmpty()) {
                    sendIcon.setEnabled(true);
                    sendIcon.setImageDrawable(ContextCompat.getDrawable(chatActivity, R.drawable.ic_send_black));
                    textChat.setHint(" ");
                    textChat.setMinLines(1);
                    textChat.setMaxLines(5);
                    sendIcon.setVisibility(View.VISIBLE);
                    currentRecordButtonState = 0;
                    recordLayout.setVisibility(View.GONE);
                    recordButtonLayout.setVisibility(View.GONE);
                } else {
                    refreshTextInput();
                }

                if (getCurrentFocus() == textChat) {
                    // is only executed if the EditText was directly changed by the user
                    if (sendIsTyping) {
                        logDebug("textChat:TextChangedListener:onTextChanged:sendIsTyping:sendTypingNotification");
                        sendIsTyping = false;
                        megaChatApi.sendTypingNotification(chatRoom.getChatId());

                        int interval = 4000;
                        Runnable runnable = new Runnable() {
                            public void run() {
                                sendIsTyping = true;
                            }
                        };
                        handlerSend = new Handler();
                        handlerSend.postDelayed(runnable, interval);
                    }

                    if (megaChatApi.isSignalActivityRequired()) {
                        megaChatApi.signalPresenceActivity();
                    }
                } else {
                    logDebug("textChat:TextChangedListener:onTextChanged:nonFocusTextChat:sendStopTypingNotification");
                    if (chatRoom != null) {
                        megaChatApi.sendStopTypingNotification(chatRoom.getChatId());
                    }
                }
            }
        });

        textChat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //Hide fileStorageLayout
                hideFileStorage();
                showLetterKB();
                return false;
            }
        });

        textChat.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Hide fileStorageLayout
                hideFileStorage();
                showLetterKB();
                return false;
            }
        });

        textChat.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //Hide fileStorageLayout
                    hideFileStorage();
                    showLetterKB();
                }
                return false;
            }
        });

        /*
        *If the recording button (an arrow) is clicked, the recording will be sent to the chat
        */
        recordButton.setOnRecordClickListener(new OnRecordClickListener() {
            @Override
            public void onClick(View v) {
                logDebug("recordButton.setOnRecordClickListener:onClick");
                recordButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                sendRecording();
            }
        });


        /*
         *Events of the recording
         */
        recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                logDebug("recordView.setOnRecordListener:onStart");
                if (participatingInACall(megaChatApi)) {
                    showSnackbar(SNACKBAR_TYPE, context.getString(R.string.not_allowed_recording_voice_clip), -1);
                    return;
                }
                if (!isAllowedToRecord()) return;
                prepareRecording();
            }

            @Override
            public void onLessThanSecond() {
                logDebug("recordView.setOnRecordListener:onLessThanSecond");
                if (!isAllowedToRecord()) return;
                showBubble();
            }

            @Override
            public void onCancel() {
                logDebug("recordView.setOnRecordListener:onCancel");
                recordButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                cancelRecording();
            }

            @Override
            public void onLock() {
                logDebug("recordView.setOnRecordListener:onLock");
                recordButtonStates(RECORD_BUTTON_SEND);
            }

            @Override
            public void onFinish(long recordTime) {
                logDebug("recordView.setOnRecordListener:onFinish");
                recordButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                sendRecording();
            }

            @Override
            public void finishedSound() {
                logDebug("recordView.setOnRecordListener:finishedSound");
                if (!isAllowedToRecord()) return;
                startRecording();
            }
        });

        recordView.setOnBasketAnimationEndListener(new OnBasketAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                logDebug("recordView.setOnBasketAnimationEndListener:onAnimationEnd");
                recordButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                cancelRecording();
            }

            @Override
            public void deactivateRecordButton() {
                logDebug("recordView.setOnBasketAnimationEndListener:desactivateRecordButton");
                hideChatOptions();
                recordView.setVisibility(View.VISIBLE);
                recordLayout.setVisibility(View.VISIBLE);
                recordButtonLayout.setVisibility(View.VISIBLE);
                recordButton.activateOnTouchListener(false);
                recordButtonDeactivated(true);
                placeRecordButton(RECORD_BUTTON_DEACTIVATED);
            }
        });


        emojiKeyboard.setOnPlaceButtonListener(new OnPlaceButtonListener() {
            @Override
            public void needToPlace() {
                logDebug("needTOPlaced");
                if(sendIcon.getVisibility() != View.VISIBLE){
                    recordLayout.setVisibility(View.VISIBLE);
                    recordButtonLayout.setVisibility(View.VISIBLE);
                }
                recordView.setVisibility(View.INVISIBLE);
                recordButton.activateOnTouchListener(true);
                placeRecordButton(RECORD_BUTTON_DEACTIVATED);
            }
        });

        messageJumpLayout.setOnClickListener(this);

        listView = findViewById(R.id.messages_chat_list_view);
        listView.setClipToPadding(false);

        listView.setNestedScrollingEnabled(false);
        ((SimpleItemAnimator) listView.getItemAnimator()).setSupportsChangeAnimations(false);

        mLayoutManager = new NpaLinearLayoutManager(this);
        mLayoutManager.setStackFromEnd(true);
        listView.setLayoutManager(mLayoutManager);

        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // Get the first visible item

                if(!messages.isEmpty()){
                    int lastPosition = messages.size()-1;
                    AndroidMegaChatMessage msg = messages.get(lastPosition);

                    while (!msg.isUploading() && msg.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL) {
                        lastPosition--;
                        msg = messages.get(lastPosition);
                    }
                    if (lastPosition == (messages.size() - 1)) {
                        //Scroll to end
                        if ((messages.size() - 1) == (mLayoutManager.findLastVisibleItemPosition() - 1)) {
                            hideMessageJump();
                        } else if ((messages.size() - 1) > (mLayoutManager.findLastVisibleItemPosition() - 1)) {
                            if (newVisibility) {
                                showJumpMessage();
                            }
                        }
                    } else {
                        lastPosition++;
                        if (lastPosition == (mLayoutManager.findLastVisibleItemPosition() - 1)) {
                            hideMessageJump();
                        } else if (lastPosition != (mLayoutManager.findLastVisibleItemPosition() - 1)) {
                            if (newVisibility) {
                                showJumpMessage();
                            }
                        }
                    }


                }

                if (stateHistory != MegaChatApi.SOURCE_NONE) {
                    if (dy > 0) {
                        // Scrolling up
                        scrollingUp = true;
                    } else {
                        // Scrolling down
                        scrollingUp = false;
                    }

                    if (!scrollingUp) {
                        int pos = mLayoutManager.findFirstVisibleItemPosition();
                        if (pos <= NUMBER_MESSAGES_BEFORE_LOAD && getMoreHistory) {
                            logDebug("DE->loadMessages:scrolling up");
                            isLoadingHistory = true;
                            stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
                            positionToScroll = -1;
                            getMoreHistory = false;
                        }
                    }
                }
            }
        });

        messagesContainerLayout = findViewById(R.id.message_container_chat_layout);

        userTypingLayout = findViewById(R.id.user_typing_layout);
        userTypingLayout.setVisibility(View.GONE);
        userTypingText = findViewById(R.id.user_typing_text);

        initAfterIntent(getIntent(), savedInstanceState);

        logDebug("FINISH on Create");
    }

    private boolean isAllowedToRecord() {
        logDebug("isAllowedToRecord ");
        if (participatingInACall(megaChatApi)) return false;
        if (!checkPermissionsVoiceClip()) return false;
        return true;
    }

    private void showLetterKB() {
        if (emojiKeyboard == null || emojiKeyboard.getLetterKeyboardShown()) return;
        emojiKeyboard.showLetterKeyboard();
    }

    private void hideFileStorage() {
        if ((!fileStorageLayout.isShown())) return;
        showInputText();
        fileStorageLayout.setVisibility(View.GONE);
        pickFileStorageButton.setImageResource(R.drawable.ic_b_select_image);
        placeRecordButton(RECORD_BUTTON_DEACTIVATED);
        if (fileStorageF == null) return;
        fileStorageF.clearSelections();
        fileStorageF.hideMultipleSelect();
    }

    /*
     * Hide input text when file storage is shown
     */

    private void hideInputText(){
        inputTextLayout.setVisibility(View.GONE);
        separatorOptions.setVisibility(View.VISIBLE);
        voiceClipLayout.setVisibility(View.GONE);
       if(emojiKeyboard!=null)
        emojiKeyboard.hideKeyboardFromFileStorage();
    }

    /*
     * Show input text when file storage is hidden
     */

    private void showInputText(){
        inputTextLayout.setVisibility(View.VISIBLE);
        separatorOptions.setVisibility(View.GONE);
        voiceClipLayout.setVisibility(View.VISIBLE);

    }

    public void initAfterIntent(Intent newIntent, Bundle savedInstanceState){
        logDebug("initAfterIntent");

        if (newIntent != null){
            logDebug("Intent is not null");
            intentAction = newIntent.getAction();
            if (intentAction != null){

                if (intentAction.equals(ACTION_OPEN_CHAT_LINK) || intentAction.equals(ACTION_JOIN_OPEN_CHAT_LINK)){
                    String link = newIntent.getDataString();
                    megaChatApi.openChatPreview(link, this);
                }
                else{

                    long newIdChat = newIntent.getLongExtra("CHAT_ID", -1);

                    if(idChat != newIdChat){
                        megaChatApi.closeChatRoom(idChat, this);
                        idChat = newIdChat;
                    }
                    myUserHandle = megaChatApi.getMyUserHandle();

                    if(savedInstanceState!=null) {
                        logDebug("Bundle is NOT NULL");
                        selectedMessageId = savedInstanceState.getLong("selectedMessageId", -1);
                        logDebug("Handle of the message: " + selectedMessageId);
                        selectedPosition = savedInstanceState.getInt("selectedPosition", -1);
                        isHideJump = savedInstanceState.getBoolean("isHideJump",false);
                        typeMessageJump = savedInstanceState.getInt("typeMessageJump",-1);
                        visibilityMessageJump = savedInstanceState.getBoolean("visibilityMessageJump",false);
                        mOutputFilePath = savedInstanceState.getString("mOutputFilePath");
                        isShareLinkDialogDismissed = savedInstanceState.getBoolean("isShareLinkDialogDismissed", false);
                        isLocationDialogShown = savedInstanceState.getBoolean("isLocationDialogShown", false);

                        if(visibilityMessageJump){
                            if(typeMessageJump == TYPE_MESSAGE_NEW_MESSAGE){
                                messageJumpText.setText(getResources().getString(R.string.message_new_messages));
                                messageJumpLayout.setVisibility(View.VISIBLE);
                            }else if(typeMessageJump == TYPE_MESSAGE_JUMP_TO_LEAST){
                                messageJumpText.setText(getResources().getString(R.string.message_jump_latest));
                                messageJumpLayout.setVisibility(View.VISIBLE);
                            }
                        }

                        lastIdMsgSeen = savedInstanceState.getLong("lastMessageSeen",-1);
                        if(lastIdMsgSeen != -1){
                            isTurn = true;
                        }
                        generalUnreadCount = savedInstanceState.getLong("generalUnreadCount",-1);

                        boolean isPlaying = savedInstanceState.getBoolean(PLAYING, false);
                        if (isPlaying) {
                            long idMessageVoicePlaying = savedInstanceState.getLong(ID_VOICE_CLIP_PLAYING, -1);
                            long messageHandleVoicePlaying = savedInstanceState.getLong(MESSAGE_HANDLE_PLAYING, -1);
                            long userHandleVoicePlaying = savedInstanceState.getLong(USER_HANDLE_PLAYING, -1);
                            int progressVoicePlaying = savedInstanceState.getInt(PROGRESS_PLAYING, 0);

                            if (!messagesPlaying.isEmpty()) {
                                for (MessageVoiceClip m : messagesPlaying) {
                                    m.getMediaPlayer().release();
                                    m.setMediaPlayer(null);
                                }
                                messagesPlaying.clear();
                            }

                            MessageVoiceClip messagePlaying = new MessageVoiceClip(idMessageVoicePlaying, userHandleVoicePlaying, messageHandleVoicePlaying);
                            messagePlaying.setProgress(progressVoicePlaying);
                            messagePlaying.setPlayingWhenTheScreenRotated(true);
                            messagesPlaying.add(messagePlaying);

                        }
                    }

                    String text = null;
                    if (intentAction.equals(ACTION_CHAT_SHOW_MESSAGES)) {
                        logDebug("ACTION_CHAT_SHOW_MESSAGES");
                        isOpeningChat = true;

                        int errorCode = newIntent.getIntExtra("PUBLIC_LINK", 1);
                        if (savedInstanceState == null) {
                            text = newIntent.getStringExtra("showSnackbar");
                            if (text == null) {
                                if (errorCode != 1) {
                                    if (errorCode == MegaChatError.ERROR_OK) {
                                        text = getString(R.string.chat_link_copied_clipboard);
                                    }
                                    else {
                                        logDebug("initAfterIntent:publicLinkError:errorCode");
                                        text = getString(R.string.general_error) + ": " + errorCode;
                                    }
                                }
                            }
                        }
                        else if (errorCode != 1 && errorCode == MegaChatError.ERROR_OK && !isShareLinkDialogDismissed) {
                                text = getString(R.string.chat_link_copied_clipboard);
                        }
                    }
                    showChat(text);
                }
            }
        }
        else{
            logWarning("INTENT is NULL");
        }
    }

    private void initializeInputText() {
        hideKeyboard();
        setChatSubtitle();

        ChatItemPreferences prefs = dbH.findChatPreferencesByHandle(Long.toString(idChat));
        if (prefs != null) {
            String written = prefs.getWrittenText();
            if (!TextUtils.isEmpty(written)) {
                textChat.setText(written);
                sendIcon.setVisibility(View.VISIBLE);
                sendIcon.setEnabled(true);
                sendIcon.setImageDrawable(ContextCompat.getDrawable(chatActivity, R.drawable.ic_send_black));
                textChat.setHint(" ");
                textChat.setMinLines(1);
                textChat.setMaxLines(5);

                currentRecordButtonState = 0;
                recordLayout.setVisibility(View.GONE);
                recordButtonLayout.setVisibility(View.GONE);
                return;
            }
        } else {
            prefs = new ChatItemPreferences(Long.toString(idChat), Boolean.toString(true), "");
            dbH.setChatItemPreferences(prefs);
        }
        refreshTextInput();
    }

    private SpannableStringBuilder transformEmojis(String textToTransform, float sizeText){
        CharSequence text = textToTransform == null ? "" : textToTransform;
        String resultText = EmojiUtilsShortcodes.emojify(text.toString());
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(resultText);
        EmojiManager.getInstance().replaceWithImages(this, spannableStringBuilder, sizeText, sizeText);
        return spannableStringBuilder;
    }

    private void refreshTextInput() {
        recordButtonStates(RECORD_BUTTON_DEACTIVATED);
        sendIcon.setVisibility(View.GONE);
        sendIcon.setEnabled(false);
        sendIcon.setImageDrawable(ContextCompat.getDrawable(chatActivity, R.drawable.ic_send_trans));
        if (chatRoom != null) {
            megaChatApi.sendStopTypingNotification(chatRoom.getChatId());
            String title;
            if (chatRoom.hasCustomTitle()) {
                title = getString(R.string.type_message_hint_with_customized_title, chatRoom.getTitle());
            } else {
                title = getString(R.string.type_message_hint_with_default_title, chatRoom.getTitle());
            }
            textChat.setHint(transformEmojis(title, textChat.getTextSize()));
        }

        textChat.setMinLines(1);
        textChat.setMaxLines(1);
    }

    public void showChat(String textSnackbar){
        if(idChat!=-1) {
            //Recover chat
            logDebug("Recover chat with id: " + idChat);
            chatRoom = megaChatApi.getChatRoom(idChat);
            if(chatRoom==null){
                logError("Chatroom is NULL - finish activity!!");
                finish();
            }

            initializeInputText();
            megaChatApi.closeChatRoom(idChat, this);
            boolean result = megaChatApi.openChatRoom(idChat, this);

            logDebug("Result of open chat: " + result);
            if(result){
                MegaApplication.setClosedChat(false);
            }

            if(!result){
                logError("Error on openChatRoom");
                if(errorOpenChatDialog==null){
                    android.support.v7.app.AlertDialog.Builder builder;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
                    }
                    else{
                        builder = new AlertDialog.Builder(this);
                    }
                    builder.setTitle(getString(R.string.chat_error_open_title));
                    builder.setMessage(getString(R.string.chat_error_open_message));

                    builder.setPositiveButton(getString(R.string.cam_sync_ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    finish();
                                }
                            }
                    );
                    errorOpenChatDialog = builder.create();
                    errorOpenChatDialog.show();
                }
            }
            else {
                int chatConnection = megaChatApi.getChatConnectionState(idChat);
                logDebug("Chat connection (" + idChat + ") is: " + chatConnection);
                if (adapter == null) {
                    adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, messagesPlaying, listView);
                    adapter.setHasStableIds(true);
                    listView.setAdapter(adapter);
                }

                setPreviewersView();
                titleToolbar.setText(chatRoom.getTitle());
                setChatSubtitle();

                if (!chatRoom.isPublic()) {
                    privateIconToolbar.setVisibility(View.VISIBLE);
                }
                else {
                    privateIconToolbar.setVisibility(View.GONE);
                }

                isOpeningChat = true;

                String textToShowB = String.format(getString(R.string.chat_loading_messages));

                try {
                    textToShowB = textToShowB.replace("[A]", "<font color=\'#7a7a7a\'>");
                    textToShowB = textToShowB.replace("[/A]", "</font>");
                    textToShowB = textToShowB.replace("[B]", "<font color=\'#000000\'>");
                    textToShowB = textToShowB.replace("[/B]", "</font>");
                } catch (Exception e) {
                }
                Spanned resultB = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    resultB = Html.fromHtml(textToShowB, Html.FROM_HTML_MODE_LEGACY);
                } else {
                    resultB = Html.fromHtml(textToShowB);
                }

                emptyScreen(resultB.toString());

                if(textSnackbar!=null){
                    String chatLink = getIntent().getStringExtra("CHAT_LINK");
                    if (chatLink != null && !isShareLinkDialogDismissed) {
                        showShareChatLinkDialog(this, chatRoom, chatLink);
                    }
                    else {
                        showSnackbar(SNACKBAR_TYPE, textSnackbar, -1);
                    }
                }

                loadHistory();
                logDebug("On create: stateHistory: " + stateHistory);
                if (isLocationDialogShown) {
                    showSendLocationDialog();
                }
            }
        }
        else{
            logError("Chat ID -1 error");
        }

        logDebug("FINISH on Create");
    }

    private void emptyScreen(String text){
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            emptyImageView.setImageResource(R.drawable.chat_empty_landscape);
        } else {
            emptyImageView.setImageResource(R.drawable.ic_empty_chat_list);
        }

        emptyTextView.setText(text);
        emptyTextView.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.VISIBLE);

        chatRelativeLayout.setVisibility(View.GONE);
    }

    public void removeChatLink(){
        logDebug("removeChatLink");
        megaChatApi.removeChatLink(idChat, this);
    }

    public void loadHistory(){
        logDebug("loadHistory");

        isLoadingHistory = true;

        long unreadCount = chatRoom.getUnreadCount();
        if (unreadCount == 0) {
            if(!isTurn) {
                lastIdMsgSeen = -1;
                generalUnreadCount = -1;
                stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
                numberToLoad=NUMBER_MESSAGES_TO_LOAD;
            }else{
                if (generalUnreadCount < 0) {
                    logDebug("loadMessages " + chatRoom.getUnreadCount());
                    long unreadAbs = Math.abs(generalUnreadCount);
                    numberToLoad =  (int) unreadAbs+NUMBER_MESSAGES_TO_LOAD;
                    stateHistory = megaChatApi.loadMessages(idChat, (int) numberToLoad);
                }
                else{
                    logDebug("loadMessages " + chatRoom.getUnreadCount());
                    numberToLoad =  (int) generalUnreadCount+NUMBER_MESSAGES_TO_LOAD;
                    stateHistory = megaChatApi.loadMessages(idChat, (int) numberToLoad);
                }
            }
            lastSeenReceived = true;
            logDebug("loadMessages:unread is 0");
        } else {
            if(!isTurn){
                lastIdMsgSeen = megaChatApi.getLastMessageSeenId(idChat);
                generalUnreadCount = unreadCount;
            }
            else{
                logDebug("Do not change lastSeenId --> rotating screen");
            }

            if (lastIdMsgSeen != -1) {
                logDebug("lastSeenId: " + lastIdMsgSeen);
            } else {
                logError("Error:InvalidLastMessage");
            }

            lastSeenReceived = false;
            if (unreadCount < 0) {
                logDebug("loadMessages " + chatRoom.getUnreadCount());
                long unreadAbs = Math.abs(unreadCount);
                numberToLoad =  (int) unreadAbs+NUMBER_MESSAGES_TO_LOAD;
                stateHistory = megaChatApi.loadMessages(idChat, (int) numberToLoad);
            }
            else{
                logDebug("loadMessages " + chatRoom.getUnreadCount());
                numberToLoad =  (int) unreadCount+NUMBER_MESSAGES_TO_LOAD;
                stateHistory = megaChatApi.loadMessages(idChat, (int) numberToLoad);
            }
        }
        logDebug("END:numberToLoad: " + numberToLoad);
    }

    private void setSubtitleVisibility() {
        if (chatRoom.isGroup()) {
            individualSubtitleToobar.setVisibility(View.GONE);
            groupalSubtitleToolbar.setVisibility(View.VISIBLE);
            iconStateToolbar.setVisibility(View.GONE);
        }
        else {
            individualSubtitleToobar.setVisibility(View.VISIBLE);
            groupalSubtitleToolbar.setVisibility(View.GONE);
        }
        subtitleCall.setVisibility(View.GONE);
    }

    private void setPreviewGroupalSubtitle () {
        long participants = chatRoom.getPeerCount();
        if (participants > 0) {
            groupalSubtitleToolbar.setVisibility(View.VISIBLE);
            groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.number_of_participants, participants)));
        }
        else {
            groupalSubtitleToolbar.setVisibility(View.GONE);
        }
    }

    public void setChatSubtitle(){
        logDebug("setChatSubtitle");
        if(chatRoom==null){
            return;
        }
        int width;
        if(isScreenInPortrait(this)){
            if(isGroup()) {
                width = scaleWidthPx(TITLE_TOOLBAR_PORT, outMetrics);
            }else {
                width = scaleWidthPx(TITLE_TOOLBAR_IND_PORT, outMetrics);
            }
        }else{
            width = scaleWidthPx(TITLE_TOOLBAR_LAND, outMetrics);
        }
        titleToolbar.setMaxWidthEmojis(width);
        titleToolbar.setTypeEllipsize(TextUtils.TruncateAt.END);

        setSubtitleVisibility();

        if (chatC.isInAnonymousMode() && megaChatApi.getChatConnectionState(idChat)==MegaChatApi.CHAT_CONNECTION_ONLINE) {
            logDebug("Is preview");
            setPreviewGroupalSubtitle();
            tB.setOnClickListener(this);
            setBottomLayout(SHOW_JOIN_LAYOUT);

        }else if(megaChatApi.getConnectionState()!=MegaChatApi.CONNECTED||megaChatApi.getChatConnectionState(idChat)!=MegaChatApi.CHAT_CONNECTION_ONLINE) {
            logDebug("Chat not connected ConnectionState: " + megaChatApi.getConnectionState() + " ChatConnectionState: " + megaChatApi.getChatConnectionState(idChat));
            tB.setOnClickListener(this);
            if (chatRoom.isPreview()) {
                logDebug("Chat not connected: is preview");
                setPreviewGroupalSubtitle();
                setBottomLayout(SHOW_NOTHING_LAYOUT);
            } else {
                logDebug("Chat not connected: is not preview");
                if (chatRoom.isGroup()) {
                    groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.invalid_connection_state)));
                } else {
                    individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.invalid_connection_state)));
                }

                int permission = chatRoom.getOwnPrivilege();
                logDebug("Check permissions");
                if ((permission == MegaChatRoom.PRIV_RO) || (permission == MegaChatRoom.PRIV_RM)) {
                    setBottomLayout(SHOW_NOTHING_LAYOUT);
                } else {
                    setBottomLayout(SHOW_WRITING_LAYOUT);
                }
            }
        }else{
            logDebug("Karere connection state: " + megaChatApi.getConnectionState());
            logDebug("Chat connection state: " + megaChatApi.getChatConnectionState(idChat));

            int permission = chatRoom.getOwnPrivilege();
            if (chatRoom.isGroup()) {
                tB.setOnClickListener(this);
                if(chatRoom.isPreview()){
                    logDebug("Is preview");
                    setPreviewGroupalSubtitle();
                    if (getIntent() != null && getIntent().getAction() != null && getIntent().getAction().equals(ACTION_JOIN_OPEN_CHAT_LINK)) {
                        setBottomLayout(SHOW_NOTHING_LAYOUT);
                    }else {
                        setBottomLayout(SHOW_JOIN_LAYOUT);
                    }

                    return;
                }
                else {
                    logDebug("Check permissions group chat");
                    if (permission == MegaChatRoom.PRIV_RO) {
                        logDebug("Permission RO");
                        setBottomLayout(SHOW_NOTHING_LAYOUT);

                        if (chatRoom.isArchived()) {
                            logDebug("Chat is archived");
                            groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.archived_chat)));
                        } else {
                            groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.observer_permission_label_participants_panel)));
                        }
                    }else if (permission == MegaChatRoom.PRIV_RM) {
                        logDebug("Permission RM");
                        setBottomLayout(SHOW_NOTHING_LAYOUT);

                        if (chatRoom.isArchived()) {
                            logDebug("Chat is archived");
                            groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.archived_chat)));
                        }
                        else if (!chatRoom.isActive()) {
                            groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.inactive_chat)));
                        }
                        else {
                            groupalSubtitleToolbar.setText(null);
                            groupalSubtitleToolbar.setVisibility(View.GONE);
                        }
                    }
                    else{
                        logDebug("Permission: " + permission);

                        setBottomLayout(SHOW_WRITING_LAYOUT);

                        if(chatRoom.isArchived()){
                            logDebug("Chat is archived");
                            groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.archived_chat)));
                        }
                        else if(chatRoom.hasCustomTitle()){
                            setCustomSubtitle();
                        }
                        else{
                            long participantsLabel = chatRoom.getPeerCount()+1; //Add one to include me
                            groupalSubtitleToolbar.setText(adjustForLargeFont(getResources().getQuantityString(R.plurals.subtitle_of_group_chat, (int) participantsLabel, participantsLabel)));
                        }
                    }
                }
            }
            else{
                logDebug("Check permissions one to one chat");
                if(permission==MegaChatRoom.PRIV_RO) {
                    logDebug("Permission RO");

                    if(megaApi!=null){
                        if(megaApi.getRootNode()!=null){
                            long chatHandle = chatRoom.getChatId();
                            MegaChatRoom chat = megaChatApi.getChatRoom(chatHandle);
                            long userHandle = chat.getPeerHandle(0);
                            String userHandleEncoded = MegaApiAndroid.userHandleToBase64(userHandle);
                            MegaUser user = megaApi.getContact(userHandleEncoded);

                            if(user!=null && user.getVisibility() == MegaUser.VISIBILITY_VISIBLE){
                                tB.setOnClickListener(this);
                            }
                            else{
                                tB.setOnClickListener(null);
                            }
                        }
                    }
                    else{
                        tB.setOnClickListener(null);
                    }
                    setBottomLayout(SHOW_NOTHING_LAYOUT);

                    if(chatRoom.isArchived()){
                        logDebug("Chat is archived");
                        individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.archived_chat)));
                    }
                    else{
                        individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.observer_permission_label_participants_panel)));
                    }
                }
                else if(permission==MegaChatRoom.PRIV_RM) {
                    tB.setOnClickListener(this);

                    logDebug("Permission RM");
                    setBottomLayout(SHOW_NOTHING_LAYOUT);

                    if(chatRoom.isArchived()){
                        logDebug("Chat is archived");
                        individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.archived_chat)));
                    }
                    else if(!chatRoom.isActive()){
                        individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.inactive_chat)));
                    }
                    else{
                        individualSubtitleToobar.setText(null);
                        individualSubtitleToobar.setVisibility(View.GONE);
                    }
                }
                else{
                    tB.setOnClickListener(this);

                    long userHandle = chatRoom.getPeerHandle(0);
                    setStatus(userHandle);
                    setBottomLayout(SHOW_WRITING_LAYOUT);
                }
            }
        }
    }

    public void setBottomLayout(int show) {
        if (show == SHOW_JOIN_LAYOUT) {
            writingContainerLayout.setVisibility(View.GONE);
            joinChatLinkLayout.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messagesContainerLayout.getLayoutParams();
            params.addRule(RelativeLayout.ABOVE, R.id.join_chat_layout_chat_layout);
            messagesContainerLayout.setLayoutParams(params);
            fragmentVoiceClip.setVisibility(View.GONE);
        }else if (show == SHOW_NOTHING_LAYOUT) {
            writingContainerLayout.setVisibility(View.GONE);
            joinChatLinkLayout.setVisibility(View.GONE);
            fragmentVoiceClip.setVisibility(View.GONE);
        }else{
            writingContainerLayout.setVisibility(View.VISIBLE);
            joinChatLinkLayout.setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messagesContainerLayout.getLayoutParams();
            params.addRule(RelativeLayout.ABOVE, R.id.writing_container_layout_chat_layout);
            messagesContainerLayout.setLayoutParams(params);
            fragmentVoiceClip.setVisibility(View.VISIBLE);
        }
    }

    public void setCustomSubtitle(){
        logDebug("setCustomSubtitle");

        long participantsCount = chatRoom.getPeerCount();
        StringBuilder customSubtitle = new StringBuilder("");
        for(int i=0;i<participantsCount;i++) {

            if(i!=0){
                customSubtitle.append(", ");
            }

            String participantName = chatRoom.getPeerFirstname(i);
            if(participantName==null){
                //Get the lastname
                String participantLastName = chatRoom.getPeerLastname(i);
                if(participantLastName==null){
                    //Get the email
                    String participantEmail = chatRoom.getPeerEmail(i);
                    customSubtitle.append(participantEmail);
                }
                else{
                    if(participantLastName.trim().isEmpty()){
                        //Get the email
                        String participantEmail = chatRoom.getPeerEmail(i);
                        customSubtitle.append(participantEmail);
                    }
                    else{
                        //Append last name to the title
                        customSubtitle.append(participantLastName);
                    }
                }
            }
            else{
                if(participantName.trim().isEmpty()){
                    //Get the lastname
                    String participantLastName = chatRoom.getPeerLastname(i);
                    if(participantLastName==null){
                        //Get the email
                        String participantEmail = chatRoom.getPeerEmail(i);
                        customSubtitle.append(participantEmail);
                    }
                    else{
                        if(participantLastName.trim().isEmpty()){
                            //Get the email
                            String participantEmail = chatRoom.getPeerEmail(i);
                            customSubtitle.append(participantEmail);
                        }
                        else{
                            //Append last name to the title
                            customSubtitle.append(participantLastName);
                        }
                    }
                }
                else{
                    //Append first name to the title
                    customSubtitle.append(participantName);
                }
            }
        }
        if (customSubtitle.toString().trim().isEmpty()){
            groupalSubtitleToolbar.setText(null);
            groupalSubtitleToolbar.setVisibility(View.GONE);
        }
        else {
            groupalSubtitleToolbar.setText(adjustForLargeFont(customSubtitle.toString()));
        }
    }

    public void setLastGreen(String date){
        individualSubtitleToobar.setText(date);
        individualSubtitleToobar.isMarqueeIsNecessary(this);
        if(subtitleCall.getVisibility()!=View.VISIBLE && groupalSubtitleToolbar.getVisibility()!=View.VISIBLE){
            individualSubtitleToobar.setVisibility(View.VISIBLE);
        }
    }

    public void requestLastGreen(int state){
        logDebug("State: " + state);

        if(chatRoom!=null && !chatRoom.isGroup() && !chatRoom.isArchived()){
            if(state == INITIAL_PRESENCE_STATUS){
                state = megaChatApi.getUserOnlineStatus(chatRoom.getPeerHandle(0));
            }

            if(state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID){
                logDebug("Request last green for user");
                megaChatApi.requestLastGreen(chatRoom.getPeerHandle(0), this);
            }
        }
    }

    public void setStatus(long userHandle){

        iconStateToolbar.setVisibility(View.GONE);

        if(megaChatApi.getConnectionState()!=MegaChatApi.CONNECTED){
            logWarning("Chat not connected");
            individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.invalid_connection_state)));
        }
        else if(chatRoom.isArchived()){
            logDebug("Chat is archived");
            individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.archived_chat)));
        }
        else if(!chatRoom.isGroup()){
            int state = megaChatApi.getUserOnlineStatus(userHandle);

            if(state == MegaChatApi.STATUS_ONLINE){
                logDebug("This user is connected");
                individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.online_status)));
                iconStateToolbar.setVisibility(View.VISIBLE);
                iconStateToolbar.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_online));

            }
            else if(state == MegaChatApi.STATUS_AWAY){
                logDebug("This user is away");
                individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.away_status)));
                iconStateToolbar.setVisibility(View.VISIBLE);
                iconStateToolbar.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_away));

            }
            else if(state == MegaChatApi.STATUS_BUSY){
                logDebug("This user is busy");
                individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.busy_status)));
                iconStateToolbar.setVisibility(View.VISIBLE);
                iconStateToolbar.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_busy));

            }
            else if(state == MegaChatApi.STATUS_OFFLINE){
                logDebug("This user is offline");
                individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.offline_status)));
                iconStateToolbar.setVisibility(View.VISIBLE);
                iconStateToolbar.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_offline));

            }
            else if(state == MegaChatApi.STATUS_INVALID){
                logWarning("INVALID status: " + state);
                individualSubtitleToobar.setText(null);
                individualSubtitleToobar.setVisibility(View.GONE);
            }
            else{
                logDebug("This user status is: " + state);
                individualSubtitleToobar.setText(null);
                individualSubtitleToobar.setVisibility(View.GONE);
            }
        }
    }

    public int compareTime(AndroidMegaChatMessage message, AndroidMegaChatMessage previous){
        return compareTime(message.getMessage().getTimestamp(), previous.getMessage().getTimestamp());
    }

    public int compareTime(long timeStamp, AndroidMegaChatMessage previous){
        return compareTime(timeStamp, previous.getMessage().getTimestamp());
    }

    public int compareTime(long timeStamp, long previous){
        if(previous!=-1){

            Calendar cal = calculateDateFromTimestamp(timeStamp);
            Calendar previousCal =  calculateDateFromTimestamp(previous);

            TimeUtils tc = new TimeUtils(TIME);

            int result = tc.compare(cal, previousCal);
            logDebug("RESULTS compareTime: " + result);
            return result;
        }
        else{
            logWarning("return -1");
            return -1;
        }
    }

    public int compareDate(AndroidMegaChatMessage message, AndroidMegaChatMessage previous){
        return compareDate(message.getMessage().getTimestamp(), previous.getMessage().getTimestamp());
    }

    public int compareDate(long timeStamp, AndroidMegaChatMessage previous){
        return compareDate(timeStamp, previous.getMessage().getTimestamp());
    }

    public int compareDate(long timeStamp, long previous){
        logDebug("compareDate");

        if(previous!=-1){
            Calendar cal = calculateDateFromTimestamp(timeStamp);
            Calendar previousCal =  calculateDateFromTimestamp(previous);

            TimeUtils tc = new TimeUtils(DATE);

            int result = tc.compare(cal, previousCal);
            logDebug("RESULTS compareDate: "+result);
            return result;
        }
        else{
            logWarning("return -1");
            return -1;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        logDebug("onCreateOptionsMenuLollipop");
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_action, menu);

        callMenuItem = menu.findItem(R.id.cab_menu_call_chat);
        videoMenuItem = menu.findItem(R.id.cab_menu_video_chat);
        inviteMenuItem = menu.findItem(R.id.cab_menu_invite_chat);
        clearHistoryMenuItem = menu.findItem(R.id.cab_menu_clear_history_chat);
        contactInfoMenuItem = menu.findItem(R.id.cab_menu_contact_info_chat);
        leaveMenuItem = menu.findItem(R.id.cab_menu_leave_chat);
        archiveMenuItem = menu.findItem(R.id.cab_menu_archive_chat);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        logDebug("onPrepareOptionsMenu");

        if(chatRoom!=null){
            callMenuItem.setEnabled(false);
            callMenuItem.setIcon(mutateIcon(this, R.drawable.ic_phone_white, R.color.white_50_opacity));
            if (chatRoom.isGroup()) {
                videoMenuItem.setVisible(false);
            }else{
                videoMenuItem.setEnabled(false);
                videoMenuItem.setIcon(mutateIcon(this, R.drawable.ic_videocam_white, R.color.white_50_opacity));
            }


            if ((chatRoom.isPreview()) || (megaChatApi.getConnectionState() != MegaChatApi.CONNECTED)
                        || (megaChatApi.getChatConnectionState(idChat) != MegaChatApi.CHAT_CONNECTION_ONLINE)) {
                logDebug("chatRoom.isPreview || megaChatApi.getConnectionState() " + megaChatApi.getConnectionState() +
                        " || megaChatApi.getChatConnectionState(idChat) "+(megaChatApi.getChatConnectionState(idChat)));

                leaveMenuItem.setVisible(false);
                clearHistoryMenuItem.setVisible(false);
                inviteMenuItem.setVisible(false);
                contactInfoMenuItem.setVisible(false);
                archiveMenuItem.setVisible(false);
            }else {
                if(megaChatApi.getNumCalls() <= 0){
                    callMenuItem.setEnabled(true);
                    callMenuItem.setIcon(mutateIcon(this, R.drawable.ic_phone_white, R.color.background_chat));

                    if (chatRoom.isGroup()) {
                        videoMenuItem.setVisible(false);
                    }else{
                        videoMenuItem.setEnabled(true);
                        videoMenuItem.setIcon(mutateIcon(this, R.drawable.ic_videocam_white, R.color.background_chat));
                    }

                }else{
                    if( megaChatApi!=null && !participatingInACall(megaChatApi) && !megaChatApi.hasCallInChatRoom(chatRoom.getChatId())){
                        callMenuItem.setEnabled(true);
                        callMenuItem.setIcon(mutateIcon(this, R.drawable.ic_phone_white, R.color.background_chat));

                        if (chatRoom.isGroup()) {
                            videoMenuItem.setVisible(false);
                        }else{
                            videoMenuItem.setEnabled(true);
                            videoMenuItem.setIcon(mutateIcon(this, R.drawable.ic_videocam_white, R.color.background_chat));
                        }
                    }
                }

                archiveMenuItem.setVisible(true);
                if(chatRoom.isArchived()){
                    archiveMenuItem.setTitle(getString(R.string.general_unarchive));
                }
                else{
                    archiveMenuItem.setTitle(getString(R.string.general_archive));
                }

                int permission = chatRoom.getOwnPrivilege();
                logDebug("Permission in the chat: " + permission);
                if (chatRoom.isGroup()) {

                    if (permission == MegaChatRoom.PRIV_MODERATOR) {

                        inviteMenuItem.setVisible(true);

                        int lastMessageIndex = messages.size() - 1;
                        if (lastMessageIndex >= 0) {
                            AndroidMegaChatMessage lastMessage = messages.get(lastMessageIndex);
                            if (!lastMessage.isUploading()) {
                                if (lastMessage.getMessage().getType() == MegaChatMessage.TYPE_TRUNCATE) {
                                    logDebug("Last message is TRUNCATE");
                                    clearHistoryMenuItem.setVisible(false);
                                } else {
                                    logDebug("Last message is NOT TRUNCATE");
                                    clearHistoryMenuItem.setVisible(true);
                                }
                            } else {
                                logDebug("Last message is UPLOADING");
                                clearHistoryMenuItem.setVisible(true);
                            }
                        }
                        else {
                            clearHistoryMenuItem.setVisible(false);
                        }

                        leaveMenuItem.setVisible(true);
                    } else if (permission == MegaChatRoom.PRIV_RM) {
                        logDebug("Group chat PRIV_RM");
                        leaveMenuItem.setVisible(false);
                        clearHistoryMenuItem.setVisible(false);
                        inviteMenuItem.setVisible(false);
                        callMenuItem.setVisible(false);
                        videoMenuItem.setVisible(false);
                    } else if (permission == MegaChatRoom.PRIV_RO) {
                        logDebug("Group chat PRIV_RO");
                        leaveMenuItem.setVisible(true);
                        clearHistoryMenuItem.setVisible(false);
                        inviteMenuItem.setVisible(false);
                        callMenuItem.setVisible(false);
                        videoMenuItem.setVisible(false);
                    } else if(permission == MegaChatRoom.PRIV_STANDARD){
                        logDebug("Group chat PRIV_STANDARD");
                        leaveMenuItem.setVisible(true);
                        clearHistoryMenuItem.setVisible(false);
                        inviteMenuItem.setVisible(false);
                    }else{
                        logDebug("Permission: " + permission);
                        leaveMenuItem.setVisible(true);
                        clearHistoryMenuItem.setVisible(false);
                        inviteMenuItem.setVisible(false);
                    }

                    contactInfoMenuItem.setTitle(getString(R.string.group_chat_info_label));
                    contactInfoMenuItem.setVisible(true);
                }
                else {
                    inviteMenuItem.setVisible(false);
                    if (permission == MegaChatRoom.PRIV_RO) {
                        clearHistoryMenuItem.setVisible(false);
                        contactInfoMenuItem.setVisible(false);
                        callMenuItem.setVisible(false);
                        videoMenuItem.setVisible(false);
                    } else {
                        clearHistoryMenuItem.setVisible(true);
                        contactInfoMenuItem.setTitle(getString(R.string.contact_properties_activity));
                        contactInfoMenuItem.setVisible(true);
                    }
                    leaveMenuItem.setVisible(false);
                }
            }

        }else{
            logWarning("Chatroom NULL on create menu");
            leaveMenuItem.setVisible(false);
            callMenuItem.setVisible(false);
            videoMenuItem.setVisible(false);
            clearHistoryMenuItem.setVisible(false);
            inviteMenuItem.setVisible(false);
            contactInfoMenuItem.setVisible(false);
            archiveMenuItem.setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    void ifAnonymousModeLogin(boolean pendingJoin) {
        if(chatC.isInAnonymousMode()){
            Intent loginIntent = new Intent(this, LoginActivityLollipop.class);
            loginIntent.putExtra("visibleFragment",  LOGIN_FRAGMENT);
            if (pendingJoin && getIntent() != null && getIntent().getDataString() != null) {
                loginIntent.setAction(ACTION_JOIN_OPEN_CHAT_LINK);
                loginIntent.setData(Uri.parse(getIntent().getDataString()));
                loginIntent.putExtra("idChatToJoin", idChat);
                closeChat(true);
            }
            startActivity(loginIntent);
        }
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        logDebug("onOptionsItemSelected");

        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home: {
                closeChat(true);
                ifAnonymousModeLogin(false);
                break;
            }
            case R.id.cab_menu_call_chat:{
                if(recordView.isRecordingNow()) break;

                startVideo = false;
                if(checkPermissionsCall()){
                    startCall();
                }
                break;
            }
            case R.id.cab_menu_video_chat:{
                logDebug("cab_menu_video_chat");
                if(recordView.isRecordingNow()) break;

                startVideo = true;
                if(checkPermissionsCall()){
                    startCall();
                }
                break;
            }
            case R.id.cab_menu_invite_chat:{
                if(recordView.isRecordingNow()) break;

                chooseAddParticipantDialog();
                break;
            }
            case R.id.cab_menu_contact_info_chat:{
                if(recordView.isRecordingNow()) break;

                if(chatRoom.isGroup()){
                    Intent i = new Intent(this, GroupChatInfoActivityLollipop.class);
                    i.putExtra("handle", chatRoom.getChatId());
                    this.startActivity(i);
                }
                else{
                    Intent i = new Intent(this, ContactInfoActivityLollipop.class);
                    i.putExtra("handle", chatRoom.getChatId());
                    this.startActivity(i);
                }
                break;
            }
            case R.id.cab_menu_clear_history_chat:{
                if(recordView.isRecordingNow()) break;

                logDebug("Clear history selected!");
                showConfirmationClearChat(chatRoom);
                break;
            }
            case R.id.cab_menu_leave_chat:{
                if(recordView.isRecordingNow()) break;

                logDebug("Leave selected!");
                showConfirmationLeaveChat(chatRoom);
                break;
            }
            case R.id.cab_menu_archive_chat:{
                if(recordView.isRecordingNow()) break;

                logDebug("Archive/unarchive selected!");
                ChatController chatC = new ChatController(chatActivity);
                chatC.archiveChat(chatRoom);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     *Prepare recording
     */
    public void prepareRecording() {
        logDebug("prepareRecording");
        recordView.playSound(TYPE_START_RECORD);
        stopReproductions();
    }

    /*
     * Start recording
     */
    public void startRecording(){
        logDebug("startRecording() with Permissions");

        long timeStamp = System.currentTimeMillis() / 1000;
        outputFileName = "/note_voice" + getVoiceClipName(timeStamp);
        File vcFile = buildVoiceClipFile(this, outputFileName);
        outputFileVoiceNotes = vcFile.getAbsolutePath();
        if (outputFileVoiceNotes == null) return;
        if (myAudioRecorder == null) myAudioRecorder = new MediaRecorder();
        try {
            myAudioRecorder.reset();
            myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            myAudioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            myAudioRecorder.setAudioEncodingBitRate(50000);
            myAudioRecorder.setAudioSamplingRate(44100);
            myAudioRecorder.setAudioChannels(1);
            myAudioRecorder.setOutputFile(outputFileVoiceNotes);
            myAudioRecorder.prepare();

        } catch (IOException e) {
            controlErrorRecording();
            e.printStackTrace();
            return;
        }
        myAudioRecorder.start();
        setRecordingNow(true);
        recordView.startRecordingTime();
    }

    public static String getVoiceClipName(long timestamp) {
        logDebug("timestamp: " + timestamp);
        //Get date time:
        try {
            Calendar calendar = Calendar.getInstance();
            TimeZone tz = TimeZone.getDefault();
            calendar.setTimeInMillis(timestamp * 1000L);
            calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            return sdf.format(calendar.getTime()) + ".m4a";

        } catch (Exception e) {
            logError("Error getting the voice clip name", e);
        }

        return null;
    }

    private void controlErrorRecording() {
        outputFileVoiceNotes = null;
        outputFileName = null;
        setRecordingNow(false);

        if (myAudioRecorder == null) return;
        myAudioRecorder.reset();
        myAudioRecorder.release();
        myAudioRecorder = null;
        textChat.requestFocus();
    }

    /*
     * Cancel recording and reset the audio recorder
     */
    private void cancelRecording() {
        logDebug("cancelRecording");
        if (!isRecordingNow() || myAudioRecorder == null) return;
        try {
            myAudioRecorder.stop();
            myAudioRecorder.reset();
            myAudioRecorder = null;
            ChatController.deleteOwnVoiceClip(this, outputFileName);
            outputFileVoiceNotes = null;
            setRecordingNow(false);
            textChat.requestFocus();

        } catch (RuntimeException stopException) {
            logError("Error canceling a recording", stopException);
            ChatController.deleteOwnVoiceClip(this, outputFileName);
            controlErrorRecording();

        }
    }

    /*
     * Stop the Record and send it to the chat
     */
    private void sendRecording() {
        logDebug("sendRecording");
        if ((!recordView.isRecordingNow()) || (myAudioRecorder == null)) return;

        try {
            myAudioRecorder.stop();
            recordView.playSound(TYPE_END_RECORD);
            setRecordingNow(false);
            uploadPictureOrVoiceClip(outputFileVoiceNotes);
            outputFileVoiceNotes = null;
            textChat.requestFocus();
        } catch (RuntimeException ex) {
            controlErrorRecording();
        }
    }

    /*
     *Hide chat options while recording
     */

    private void hideChatOptions(){
        logDebug("hideChatOptions");
        textChat.setVisibility(View.INVISIBLE);
        sendIcon.setVisibility(View.GONE);
        disableButton(rLKeyboardTwemojiButton, keyboardTwemojiButton);
        disableButton(rLMediaButton, mediaButton);
        disableButton(rLPickAttachButton, pickAttachButton);
        disableButton(rLPickFileStorageButton, pickFileStorageButton);
    }

    private void disableButton(final  RelativeLayout layout, final  ImageButton button){
        logDebug("disableButton");
        layout.setOnClickListener(null);
        button.setOnClickListener(null);
        button.setVisibility(View.INVISIBLE);
    }

    /*
     *Show chat options when not being recorded
     */
    private void showChatOptions(){
        logDebug("showChatOptions");
        textChat.setVisibility(View.VISIBLE);
        enableButton(rLKeyboardTwemojiButton, keyboardTwemojiButton);
        enableButton(rLMediaButton, mediaButton);
        enableButton(rLPickAttachButton, pickAttachButton);
        enableButton(rLPickFileStorageButton, pickFileStorageButton);
    }

    private void enableButton(RelativeLayout layout, ImageButton button){
        logDebug("enableButton");
        layout.setOnClickListener(this);
        button.setOnClickListener(this);
        button.setVisibility(View.VISIBLE);
    }

    /*
     *Record button deactivated or ready to send
     */
    private void recordButtonDeactivated(boolean isDeactivated) {
        logDebug("isDeactivated: " + isDeactivated);
        recordView.showLock(false);
        recordButtonLayout.setBackground(null);
        sendIcon.setVisibility(View.GONE);
        recordButton.setVisibility(View.VISIBLE);

        if(isDeactivated){
            recordButton.activateOnClickListener(false);
            recordButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_vc_off));
            recordButton.setColorFilter(null);
            return;
        }
        recordButton.activateOnTouchListener(false);
        recordButton.activateOnClickListener(true);
        recordButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_send_white));
        recordButton.setColorFilter(ContextCompat.getColor(context, R.color.accentColor));
    }

    /*
     *Update the record button view depending on the state the recording is in
     */
    private void recordButtonStates(int recordButtonState){
        logDebug("recordButtonState: " + recordButtonState);

        if(currentRecordButtonState == recordButtonState) return;

        currentRecordButtonState = recordButtonState;
        recordLayout.setVisibility(View.VISIBLE);
        recordButtonLayout.setVisibility(View.VISIBLE);
        if((currentRecordButtonState == RECORD_BUTTON_SEND) || (currentRecordButtonState == RECORD_BUTTON_ACTIVATED)){
            logDebug("SEND||ACTIVATED");
            recordView.setVisibility(View.VISIBLE);
            hideChatOptions();
            if(recordButtonState == RECORD_BUTTON_SEND){
                recordButtonDeactivated(false);
            }else{
                recordButtonLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.recv_bg_mic));
                recordButton.activateOnTouchListener(true);
                recordButton.activateOnClickListener(false);
                recordButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_vc_on));
                recordButton.setColorFilter(null);
            }

        }else if(currentRecordButtonState == RECORD_BUTTON_DEACTIVATED){
            logDebug("DESACTIVATED");
            showChatOptions();
            recordView.setVisibility(View.GONE);
            recordButton.activateOnTouchListener(true);
            recordButtonDeactivated(true);
        }
        placeRecordButton(currentRecordButtonState);
    }

    public void showBubble() {
        logDebug("showBubble");
        recordView.playSound(TYPE_ERROR_RECORD);
        bubbleLayout.setAlpha(1);
        bubbleLayout.setVisibility(View.VISIBLE);
        bubbleLayout.animate().alpha(0).setDuration(DURATION_BUBBLE);
        cancelRecording();
    }
    /*
    *Place the record button with the corresponding margins
    */
    public void placeRecordButton(int recordButtonState) {
        logDebug("recordButtonState: " + recordButtonState);
        int marginBottomVoicleLayout;
        recordView.recordButtonTranslation(recordButtonLayout,0,0);
        if(fileStorageLayout != null && fileStorageLayout.isShown() ||
                emojiKeyboard != null && emojiKeyboard.getEmojiKeyboardShown()) {
            marginBottomVoicleLayout = keyboardHeight + marginBottomDeactivated;
        }
        else {
            marginBottomVoicleLayout = marginBottomDeactivated;
        }

        int value = 0;
        int marginBottom = marginBottomVoicleLayout;
        int marginRight = 0;
        if(recordButtonState == RECORD_BUTTON_SEND || recordButtonState == RECORD_BUTTON_DEACTIVATED) {
            logDebug("SEND||DESACTIVATED");
            value = MARGIN_BUTTON_DEACTIVATED;
            if(recordButtonState == RECORD_BUTTON_DEACTIVATED) {
                logDebug("DESACTIVATED");
                marginRight = px2dp(14, outMetrics);
            }
        }
        else if(recordButtonState == RECORD_BUTTON_ACTIVATED) {
            logDebug("ACTIVATED");
            value = MARGIN_BOTTOM;
            if(fileStorageLayout != null && fileStorageLayout.isShown() ||
                    emojiKeyboard != null && emojiKeyboard.getEmojiKeyboardShown()) {
                marginBottom = keyboardHeight+marginBottomActivated;
            }
            else {
                marginBottom = marginBottomActivated;
            }
        }
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) recordButtonLayout.getLayoutParams();
        params.height = px2dp(value, outMetrics);
        params.width = px2dp(value, outMetrics);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.setMargins(0, 0, marginRight, marginBottom);
        recordButtonLayout.setLayoutParams(params);

        FrameLayout.LayoutParams paramsRecordView = (FrameLayout.LayoutParams) recordView.getLayoutParams();
        paramsRecordView.setMargins(0,0,0, marginBottomVoicleLayout);
        recordView.setLayoutParams(paramsRecordView);
    }

    public boolean isRecordingNow(){
        return recordView.isRecordingNow();
    }

    /*
     * Know if you're recording right now
     */
    public void setRecordingNow(boolean recordingNow) {
        logDebug("recordingNow: " + recordingNow);
        recordView.setRecordingNow(recordingNow);
        if (recordView.isRecordingNow()) {
            recordButtonStates(RECORD_BUTTON_ACTIVATED);
            int screenRotation = display.getRotation();
            switch (screenRotation) {
                case ROTATION_PORTRAIT: {
                    lockOrientationPortrait(this);
                    break;
                }
                case ROTATION_LANDSCAPE: {
                    lockOrientationLandscape(this);
                    break;
                }
                case ROTATION_REVERSE_PORTRAIT: {
                    lockOrientationReversePortrait(this);
                }
                case ROTATION_REVERSE_LANDSCAPE: {
                    lockOrientationReverseLandscape(this);
                    break;
                }
                default: {
                    unlockOrientation(this);
                    break;
                }
            }
            if (emojiKeyboard != null) emojiKeyboard.setListenerActivated(false);
            return;
        }

        unlockOrientation(this);
        recordButtonStates(RECORD_BUTTON_DEACTIVATED);
        if (emojiKeyboard != null) emojiKeyboard.setListenerActivated(true);
    }

    private void startCall(){
        logDebug("startCall ");
        stopReproductions();
        hideKeyboard();

        if(megaChatApi == null) return;

        MegaChatCall callInThisChat = megaChatApi.getChatCall(chatRoom.getChatId());

        if(callInThisChat != null){
            logDebug("There is a call in this chat");

            if (participatingInACall(megaChatApi)) {
                long chatIdCallInProgress = getChatCallInProgress(megaChatApi);
                if (chatIdCallInProgress == chatRoom.getChatId()) {
                    logDebug("I'm participating in the call of this chat");
                    returnCall(this, megaChatApi);
                    return;
                }

                logDebug("I'm participating in another call from another chat");
                showConfirmationToJoinCall(chatRoom);
                return;
            }

            if (callInThisChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                logDebug("The call in this chat is Ring in");
                ((MegaApplication) getApplication()).setSpeakerStatus(chatRoom.getChatId(), false);
                MegaApplication.setShowPinScreen(false);
                Intent intent = new Intent(this, ChatCallActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(CHAT_ID, idChat);
                startActivity(intent);
                return;
            }

            if (callInThisChat.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                logDebug("The call in this chat is In progress, but I do not participate");
                ((MegaApplication) getApplication()).setSpeakerStatus(chatRoom.getChatId(), startVideo);
                megaChatApi.startChatCall(idChat, startVideo, this);
            }
            return;

        }

        if (!participatingInACall(megaChatApi)) {
            logDebug("There is not a call in this chat and I am not in another call");
            MegaApplication.setCallLayoutStatus(idChat, false);
            ((MegaApplication) getApplication()).setSpeakerStatus(chatRoom.getChatId(), startVideo);
            megaChatApi.startChatCall(idChat, startVideo, this);
        }

    }

    private boolean checkPermissions(String permission, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        boolean hasPermission = (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED);

        if (!hasPermission) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            return false;
        }

        return true;
    }

    private boolean checkPermissionsVoiceClip() {
        logDebug("checkPermissionsVoiceClip()");
        return checkPermissions(Manifest.permission.RECORD_AUDIO, RECORD_VOICE_CLIP);
    }

    private boolean checkPermissionsCall() {
        logDebug("checkPermissionsCall");
        return checkPermissions(Manifest.permission.CAMERA, REQUEST_CAMERA)
                && checkPermissions(Manifest.permission.RECORD_AUDIO, RECORD_AUDIO);
    }

    private boolean checkPermissionsTakePicture() {
        logDebug("checkPermissionsTakePicture");
        return checkPermissions(Manifest.permission.CAMERA, REQUEST_CAMERA_TAKE_PICTURE)
                && checkPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_WRITE_STORAGE_TAKE_PICTURE);
    }

    private boolean checkPermissionsReadStorage() {
        logDebug("checkPermissionsReadStorage");
        return checkPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_READ_STORAGE);
    }

    private boolean checkPermissionWriteStorage(int code) {
        logDebug("checkPermissionsWriteStorage :" + code);
        return checkPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, code);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        logDebug("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) return;
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                logDebug("REQUEST_WRITE_STORAGE");
                //After storage authorization, resume unfinished download
                if (checkPermissionWriteStorage(REQUEST_WRITE_STORAGE)) {
                    ArrayList<MegaNodeList> list = new ArrayList<>();
                    for (int i = 0; i < preservedMessagesSelected.size(); i++) {
                        MegaNodeList megaNodeList = preservedMessagesSelected.get(i).getMessage().getMegaNodeList();
                        list.add(megaNodeList);
                    }
                    chatC.prepareForChatDownload(list);
                    preservedMessagesSelected = null;
                }
                break;
            }
            case REQUEST_WRITE_STORAGE_OFFLINE: {
                logDebug("REQUEST_WRITE_STORAGE");
                //After storage authorization, resume unfinished offline download
                if (checkPermissionWriteStorage(REQUEST_WRITE_STORAGE_OFFLINE)) {
                    chatC.saveForOfflineWithAndroidMessages(preservedMessagesSelected, chatRoom);
                    preservedMessagesSelected = null;
                }
                break;
            }
            case REQUEST_CAMERA:
            case RECORD_AUDIO:{
                logDebug("REQUEST_CAMERA || RECORD_AUDIO");
                if (checkPermissionsCall()) {
                    startCall();
                }
                break;
            }
            case REQUEST_CAMERA_TAKE_PICTURE:
            case REQUEST_WRITE_STORAGE_TAKE_PICTURE:{
                logDebug("REQUEST_CAMERA_TAKE_PICTURE || REQUEST_WRITE_STORAGE_TAKE_PICTURE");
                if (checkPermissionsTakePicture()) {
                    takePicture();
                }
                break;
            }
            case RECORD_VOICE_CLIP:
            case REQUEST_STORAGE_VOICE_CLIP:{
                logDebug("RECORD_VOICE_CLIP || REQUEST_STORAGE_VOICE_CLIP");
                if (checkPermissionsVoiceClip()) {
                   cancelRecording();
                }
                break;
            }
            case REQUEST_READ_STORAGE:{
                if (checkPermissionsReadStorage()) {
                    this.attachFromFileStorage();
                }
                break;
            }
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                    intent.putExtra(EDITING_MESSAGE, editingMessage);
                    if (messageToEdit != null) {
                        intent.putExtra(MSG_ID, messageToEdit.getMsgId());
                    }
                    startActivityForResult(intent, REQUEST_CODE_SEND_LOCATION);
                }
                break;
            }
        }
    }

    public void chooseAddParticipantDialog(){
        logDebug("chooseAddContactDialog");

        if(megaApi!=null && megaApi.getRootNode()!=null){
            ArrayList<MegaUser> contacts = megaApi.getContacts();
            if(contacts==null){
                showSnackbar(SNACKBAR_TYPE, getString(R.string.no_contacts_invite), -1);
            }
            else {
                if(contacts.isEmpty()){
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.no_contacts_invite), -1);
                }
                else{
                    Intent in = new Intent(this, AddContactActivityLollipop.class);
                    in.putExtra("contactType", CONTACT_TYPE_MEGA);
                    in.putExtra("chat", true);
                    in.putExtra("chatId", idChat);
                    in.putExtra("aBtitle", getString(R.string.add_participants_menu_item));
                    startActivityForResult(in, REQUEST_ADD_PARTICIPANTS);
                }
            }
        }
        else{
            logWarning("Online but not megaApi");
            showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
        }
    }

    public void chooseContactsDialog(){
        logDebug("chooseContactsDialog");

        if(megaApi!=null && megaApi.getRootNode()!=null){
            ArrayList<MegaUser> contacts = megaApi.getContacts();
            if(contacts==null){
                showSnackbar(SNACKBAR_TYPE, getString(R.string.no_contacts_invite), -1);
            }
            else {
                if(contacts.isEmpty()){
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.no_contacts_invite), -1);
                }
                else{
                    Intent in = new Intent(this, AddContactActivityLollipop.class);
                    in.putExtra("contactType", CONTACT_TYPE_MEGA);
                    in.putExtra("chat", true);
                    in.putExtra("aBtitle", getString(R.string.add_contacts));
                    startActivityForResult(in, REQUEST_SEND_CONTACTS);
                }
            }
        }
        else{
            logWarning("Online but not megaApi");
            showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
        }
    }

    public void disablePinScreen(){
        logDebug("disablePinScreen");
        MegaApplication.setShowPinScreen(false);
    }

    public void showProgressForwarding(){
        logDebug("showProgressForwarding");

        statusDialog = new ProgressDialog(this);
        statusDialog.setMessage(getString(R.string.general_forwarding));
        statusDialog.show();
    }

    private void stopReproductions(){
        if(adapter!=null){
            adapter.stopAllReproductionsInProgress();
        }
    }

    public void forwardMessages(ArrayList<AndroidMegaChatMessage> messagesSelected){
        logDebug("forwardMessages");
        //Prevent trigger multiple forwarding messages screens in multiple clicks
        if (isForwardingMessage) {
            logDebug("Forwarding message is on going");
            return;
        }

        if (existsMyChatFiles(messagesSelected, megaApi, this, this)) {
            stopReproductions();
            chatC.prepareAndroidMessagesToForward(messagesSelected, idChat);
            isForwardingMessage = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        logDebug("resultCode: " + resultCode);
        if (requestCode == REQUEST_ADD_PARTICIPANTS && resultCode == RESULT_OK) {
            if (intent == null) {
                logWarning("Return.....");
                return;
            }

            final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);
            MultipleGroupChatRequestListener multipleListener = null;

            if (contactsData != null) {

                if (contactsData.size() == 1) {
                    MegaUser user = megaApi.getContact(contactsData.get(0));
                    if (user != null) {
                        megaChatApi.inviteToChat(chatRoom.getChatId(), user.getHandle(), MegaChatPeerList.PRIV_STANDARD, this);
                    }
                } else {
                    logDebug("Add multiple participants " + contactsData.size());
                    multipleListener = new MultipleGroupChatRequestListener(this);
                    for (int i = 0; i < contactsData.size(); i++) {
                        MegaUser user = megaApi.getContact(contactsData.get(i));
                        if (user != null) {
                            megaChatApi.inviteToChat(chatRoom.getChatId(), user.getHandle(), MegaChatPeerList.PRIV_STANDARD, multipleListener);
                        }
                    }
                }
            }
        }
        else if (requestCode == REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
            if(!isOnline(this) || megaApi==null) {
                removeProgressDialog();
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                return;
            }

            final long toHandle = intent.getLongExtra("IMPORT_TO", 0);

            final long[] importMessagesHandles = intent.getLongArrayExtra("HANDLES_IMPORT_CHAT");

            importNodes(toHandle, importMessagesHandles);
        }
        else if (requestCode == REQUEST_SEND_CONTACTS && resultCode == RESULT_OK) {
            final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);
            if (contactsData != null) {
                MegaHandleList handleList = MegaHandleList.createInstance();
                for(int i=0; i<contactsData.size();i++){
                    MegaUser user = megaApi.getContact(contactsData.get(i));
                    if (user != null) {
                        handleList.addMegaHandle(user.getHandle());

                    }
                }
                retryContactAttachment(handleList);
            }
        }
        else if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
            if (intent == null) {
                logWarning("Return.....");
                return;
            }

            long handles[] = intent.getLongArrayExtra(NODE_HANDLES);
            logDebug("Number of files to send: " + handles.length);

            chatC.checkIfNodesAreMineAndAttachNodes(handles, idChat);
        }
        else if (requestCode == REQUEST_CODE_GET && resultCode == RESULT_OK) {
            if (intent == null) {
                logWarning("Return.....");
                return;
            }

            intent.setAction(Intent.ACTION_GET_CONTENT);
            FilePrepareTask filePrepareTask = new FilePrepareTask(this);
            filePrepareTask.execute(intent);
            ProgressDialog temp = null;
            try{
                temp = new ProgressDialog(this);
                temp.setMessage(getString(R.string.upload_prepare));
                temp.show();
            }
            catch(Exception e){
                return;
            }
            statusDialog = temp;
        }
        else if (requestCode == REQUEST_CODE_SELECT_CHAT) {
            isForwardingMessage = false;
            if (resultCode != RESULT_OK) return;
            if (!isOnline(this)) {
                removeProgressDialog();

                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                return;
            }

            showProgressForwarding();

            long[] idMessages = intent.getLongArrayExtra("ID_MESSAGES");
            logDebug("Send " + idMessages.length + " messages");

            long[] chatHandles = intent.getLongArrayExtra("SELECTED_CHATS");
            logDebug("Send to " + chatHandles.length + " chats");

            long[] contactHandles = intent.getLongArrayExtra("SELECTED_USERS");

            if (chatHandles != null && chatHandles.length > 0 && idMessages != null) {
                if (contactHandles != null && contactHandles.length > 0) {
                    ArrayList<MegaUser> users = new ArrayList<>();
                    ArrayList<MegaChatRoom> chats = new ArrayList<>();

                    for (int i = 0; i < contactHandles.length; i++) {
                        MegaUser user = megaApi.getContact(MegaApiAndroid.userHandleToBase64(contactHandles[i]));
                        if (user != null) {
                            users.add(user);
                        }
                    }

                    for (int i = 0; i < chatHandles.length; i++) {
                        MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatHandles[i]);
                        if (chatRoom != null) {
                            chats.add(chatRoom);
                        }
                    }

                    CreateChatToPerformActionListener listener = new CreateChatToPerformActionListener(chats, users, idMessages, this, CreateChatToPerformActionListener.SEND_MESSAGES, idChat);

                    for (MegaUser user : users) {
                        MegaChatPeerList peers = MegaChatPeerList.createInstance();
                        peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
                        megaChatApi.createChat(false, peers, listener);
                    }
                } else {
                    int countChat = chatHandles.length;
                    logDebug("Selected: " + countChat + " chats to send");

                    MultipleForwardChatProcessor forwardChatProcessor = new MultipleForwardChatProcessor(this, chatHandles, idMessages, idChat);
                    forwardChatProcessor.forward(chatRoom);
                }
            } else {
                logError("Error on sending to chat");
            }
        }
        else if (requestCode == TAKE_PHOTO_CODE && resultCode == RESULT_OK) {
            if (resultCode == Activity.RESULT_OK) {
                logDebug("TAKE_PHOTO_CODE ");
                onCaptureImageResult();

            } else {
                logError("TAKE_PHOTO_CODE--->ERROR!");
            }

        } else if (requestCode == REQUEST_CODE_TREE) {
            onRequestSDCardWritePermission(intent, resultCode, true, null);
        }
        else if (requestCode == REQUEST_CODE_SEND_LOCATION && resultCode == RESULT_OK) {
            if (intent == null) {
                return;
            }
            byte[] byteArray = intent.getByteArrayExtra(SNAPSHOT);
            //
            if (byteArray == null) return;
            Bitmap snapshot = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            String encodedSnapshot = Base64.encodeToString(byteArray, Base64.DEFAULT);
            logDebug("Info bitmap: " + snapshot.getByteCount() + " " + snapshot.getWidth() + " " + snapshot.getHeight());

            float latitude = (float) intent.getDoubleExtra(LATITUDE, 0);
            float longitude = (float) intent.getDoubleExtra(LONGITUDE, 0);
            editingMessage = intent.getBooleanExtra(EDITING_MESSAGE, false);
            if (editingMessage) {
                long msgId = intent.getLongExtra(MSG_ID, -1);
                if (msgId != -1) {
                    messageToEdit = megaChatApi.getMessage(idChat, msgId);
                }
            }

            if (editingMessage && messageToEdit != null) {
                logDebug("Edit Geolocation - tempId: " + messageToEdit.getTempId() +" id: " + messageToEdit.getMsgId());
                if (messageToEdit.getTempId() != -1) {
                    MegaChatMessage editedMsg = megaChatApi.editGeolocation(idChat, messageToEdit.getTempId(), longitude, latitude, encodedSnapshot);
                    modifyLocationReceived(new AndroidMegaChatMessage(editedMsg), true);
                }
                else if (messageToEdit.getMsgId() != -1) {
                    MegaChatMessage editedMsg = megaChatApi.editGeolocation(idChat, messageToEdit.getMsgId(), longitude, latitude, encodedSnapshot);
                    modifyLocationReceived(new AndroidMegaChatMessage(editedMsg), false);
                }
                editingMessage = false;
                messageToEdit = null;
            }
            else {
                logDebug("Send location [longLatitude]: " + latitude + " [longLongitude]: " + longitude);
                sendLocationMessage(longitude, latitude, encodedSnapshot);
            }
        } else if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
            logDebug("Local folder selected");
            String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            chatC.prepareForDownload(intent, parentPath);
        }
        else{
            logError("Error onActivityResult");
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

    public void importNodes(final long toHandle, final long[] importMessagesHandles){
        logDebug("importNode: " + toHandle +  " -> " + importMessagesHandles.length);
        statusDialog = new ProgressDialog(this);
        statusDialog.setMessage(getString(R.string.general_importing));
        statusDialog.show();

        MegaNode target = null;
        target = megaApi.getNodeByHandle(toHandle);
        if(target == null){
            target = megaApi.getRootNode();
        }
        logDebug("TARGET handle: " + target.getHandle());

        if(importMessagesHandles.length==1){
            for (int k = 0; k < importMessagesHandles.length; k++){
                MegaChatMessage message = megaChatApi.getMessage(idChat, importMessagesHandles[k]);
                if(message!=null){

                    MegaNodeList nodeList = message.getMegaNodeList();

                    for(int i=0;i<nodeList.size();i++){
                        MegaNode document = nodeList.get(i);
                        if (document != null) {
                            logDebug("DOCUMENT: " + document.getHandle());
                            document = chatC.authorizeNodeIfPreview(document, chatRoom);
                            if (target != null) {
//                            MegaNode autNode = megaApi.authorizeNode(document);

                                megaApi.copyNode(document, target, this);
                            } else {
                                logError("TARGET: null");
                               showSnackbar(SNACKBAR_TYPE, getString(R.string.import_success_error), -1);
                            }
                        }
                        else{
                            logError("DOCUMENT: null");
                            showSnackbar(SNACKBAR_TYPE, getString(R.string.import_success_error), -1);
                        }
                    }

                }
                else{
                    logError("MESSAGE is null");
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.import_success_error), -1);
                }
            }
        }
        else {
            MultipleRequestListener listener = new MultipleRequestListener(MULTIPLE_CHAT_IMPORT, this);

            for (int k = 0; k < importMessagesHandles.length; k++){
                MegaChatMessage message = megaChatApi.getMessage(idChat, importMessagesHandles[k]);
                if(message!=null){

                    MegaNodeList nodeList = message.getMegaNodeList();

                    for(int i=0;i<nodeList.size();i++){
                        MegaNode document = nodeList.get(i);
                        if (document != null) {
                            logDebug("DOCUMENT: " + document.getHandle());
                            document = chatC.authorizeNodeIfPreview(document, chatRoom);
                            if (target != null) {
//                            MegaNode autNode = megaApi.authorizeNode(document);
                                megaApi.copyNode(document, target, listener);
                            } else {
                                logError("TARGET: null");
                            }
                        }
                        else{
                            logError("DOCUMENT: null");
                        }
                    }
                }
                else{
                    logError("MESSAGE is null");
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.import_success_error), -1);
                }
            }
        }
    }

    public void retryNodeAttachment(long nodeHandle){
        megaChatApi.attachNode(idChat, nodeHandle, this);
    }

    public void retryContactAttachment(MegaHandleList handleList){
        logDebug("retryContactAttachment");
        MegaChatMessage contactMessage = megaChatApi.attachContacts(idChat, handleList);
        if(contactMessage!=null){
            AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(contactMessage);
            sendMessageToUI(androidMsgSent);
        }
    }

    public void retryPendingMessage(long idMessage){
        logDebug("retryPendingMessage: " + idMessage);

        PendingMessageSingle pendMsg = dbH.findPendingMessageById(idMessage);

        if(pendMsg!=null){

            if(pendMsg.getNodeHandle()!=-1){
                removePendingMsg(idMessage);
                retryNodeAttachment(pendMsg.getNodeHandle());
            }
            else{
                logDebug("The file was not uploaded yet");

                ////Retry to send

                String filePath = pendMsg.getFilePath();

                File f = new File(filePath);
                if (!f.exists()) {
                    showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.messages_forwarded_error_not_available, 1, 1), -1);
                    return;
                }

                //Remove the old message from the UI and DB
                removePendingMsg(idMessage);

                Intent intent = new Intent(this, ChatUploadService.class);

                PendingMessageSingle pMsgSingle = new PendingMessageSingle();
                pMsgSingle.setChatId(idChat);
                long timestamp = System.currentTimeMillis()/1000;
                pMsgSingle.setUploadTimestamp(timestamp);

                String fingerprint = megaApi.getFingerprint(f.getAbsolutePath());

                pMsgSingle.setFilePath(f.getAbsolutePath());
                pMsgSingle.setName(f.getName());
                pMsgSingle.setFingerprint(fingerprint);

                long idMessageDb = dbH.addPendingMessage(pMsgSingle);
                pMsgSingle.setId(idMessageDb);
                if(idMessageDb!=-1){
                    intent.putExtra(ChatUploadService.EXTRA_ID_PEND_MSG, idMessageDb);

                    if(!isLoadingHistory){
                        AndroidMegaChatMessage newNodeAttachmentMsg = new AndroidMegaChatMessage(pMsgSingle, true);
                        sendMessageToUI(newNodeAttachmentMsg);
                    }

//                ArrayList<String> filePaths = newPendingMsg.getFilePaths();
//                filePaths.add("/home/jfjf.jpg");

                    intent.putExtra(ChatUploadService.EXTRA_CHAT_ID, idChat);

                    startService(intent);
                }
                else{
                    logError("Error when adding pending msg to the database");
                }
            }
        }
        else{
            logError("Pending message does not exist");
            showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.messages_forwarded_error_not_available, 1, 1), -1);
        }
    }

    private void endCall(long chatHang){
        logDebug("chatHang: " + chatHang);
        if(megaChatApi!=null){
            megaChatApi.hangChatCall(chatHang, this);
        }
    }

    private void showConfirmationToJoinCall(final MegaChatRoom c){
        logDebug("showConfirmationToJoinCall");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        logDebug("END & JOIN");
                        //Find the call in progress:
                        if(megaChatApi!=null){
                            endCall(getChatCallInProgress(megaChatApi));
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        String message= getResources().getString(R.string.text_join_call);
        builder.setTitle(R.string.title_join_call);
        builder.setMessage(message).setPositiveButton(context.getString(R.string.answer_call_incoming).toUpperCase(), dialogClickListener).setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void showConfirmationOpenCamera(final MegaChatRoom c){
        logDebug("showConfirmationOpenCamera");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE: {
                        logDebug("Open camera and lost the camera in the call");
                        stopReproductions();

                        //Find the call in progress:
                        if(megaChatApi!=null){
                            long chatIdCallInProgress = getChatCallInProgress(megaChatApi);

                            MegaChatCall callInProgress = megaChatApi.getChatCall(chatIdCallInProgress);
                            if(callInProgress!=null){
                                if(callInProgress.hasLocalVideo()){
                                    megaChatApi.disableVideo(chatIdCallInProgress, null);
                                }
                                openCameraApp();
                            }
                        }
                        break;
                    }
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        String message= getResources().getString(R.string.confirmation_open_camera_on_chat);
        builder.setTitle(R.string.title_confirmation_open_camera_on_chat);
        builder.setMessage(message).setPositiveButton(R.string.context_open_link, dialogClickListener).setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void showConfirmationClearChat(final MegaChatRoom c){
        logDebug("showConfirmationClearChat");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        logDebug("Clear chat!");
                        stopReproductions();
                        chatC.clearHistory(c);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        }
        else{
            builder = new AlertDialog.Builder(this);
        }
        String message= getResources().getString(R.string.confirmation_clear_group_chat);
        builder.setTitle(R.string.title_confirmation_clear_group_chat);
        builder.setMessage(message).setPositiveButton(R.string.general_clear, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void showConfirmationLeaveChat (final MegaChatRoom c){
        logDebug("showConfirmationLeaveChat");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE: {
                        stopReproductions();

                        ChatController chatC = new ChatController(chatActivity);
                        chatC.leaveChat(c);
                        break;
                    }
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        }
        else{
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(getResources().getString(R.string.title_confirmation_leave_group_chat));
        String message= getResources().getString(R.string.confirmation_leave_group_chat);
        builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void showConfirmationRejoinChat(final long publicHandle){
        logDebug("showConfirmationRejoinChat");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE: {
                        logDebug("Rejoin chat!: " + publicHandle);
                        megaChatApi.autorejoinPublicChat(idChat, publicHandle, chatActivity);
                        break;
                    }
                    case DialogInterface.BUTTON_NEGATIVE: {
                        //No button clicked
                        break;
                    }
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        String message= getResources().getString(R.string.confirmation_rejoin_chat_link);
        builder.setMessage(message).setPositiveButton(R.string.action_join, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    @Override
    public void onBackPressed() {
        retryConnectionsAndSignalPresence();

        if (emojiKeyboard != null && emojiKeyboard.getEmojiKeyboardShown()) {
            emojiKeyboard.hideBothKeyboard(this);
        } else {
            if (fileStorageLayout.isShown()) {
                hideFileStorage();
            } else {
                if (handlerEmojiKeyboard != null) {
                    handlerEmojiKeyboard.removeCallbacksAndMessages(null);
                }
                if (handlerKeyboard != null) {
                    handlerKeyboard.removeCallbacksAndMessages(null);
                }
                closeChat(true);
                ifAnonymousModeLogin(false);
            }
        }
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");

        switch (v.getId()) {
            case R.id.home:{
                onBackPressed();
                break;
            }
            case R.id.call_in_progress_layout:{
                logDebug("call_in_progress_layout");
                startVideo = false;
                if(checkPermissionsCall()){
                    startCall();
                }
                break;
            }
            case R.id.send_message_icon_chat:{
                logDebug("send_message_icon_chat");
                writingLayout.setClickable(false);
                String text = textChat.getText().toString();
                if(text.isEmpty()) break;

                if (editingMessage) {
                    logDebug("send_message_icon_chat:editingMessage");
                    editMessage(text);
                    clearSelections();
                    hideMultipleSelect();
                    actionMode.invalidate();
                } else {
                    logDebug("send_message_icon_chat:sendindMessage");
                    sendMessage(text);
                }
                textChat.setText("", TextView.BufferType.EDITABLE);
                break;
            }
            case R.id.keyboard_twemoji_chat:
            case R.id.rl_keyboard_twemoji_chat:{
                logDebug("keyboard_icon_chat");
                hideFileStorage();
                if(emojiKeyboard==null) break;
                changeKeyboard(keyboardTwemojiButton);
                break;
            }

            case R.id.media_icon_chat:
            case R.id.rl_media_icon_chat: {
                logDebug("media_icon_chat");
                if (recordView.isRecordingNow()) break;

                hideKeyboard();
                if (participatingInACall(megaChatApi)) {
                    showConfirmationOpenCamera(chatRoom);
                } else {
                    openCameraApp();
                }
                break;
            }
            case R.id.pick_file_storage_icon_chat:
            case R.id.rl_pick_file_storage_icon_chat:{
                logDebug("file storage icon ");
                if (fileStorageLayout.isShown()) {
                    hideFileStorage();
                    if(emojiKeyboard != null) emojiKeyboard.changeKeyboardIcon(false);
                } else {
                    if ((emojiKeyboard != null) && (emojiKeyboard.getLetterKeyboardShown())) {
                        emojiKeyboard.hideBothKeyboard(this);
                        handlerEmojiKeyboard.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    boolean hasStoragePermission = (ContextCompat.checkSelfPermission(chatActivity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                                    if (!hasStoragePermission) {
                                        ActivityCompat.requestPermissions(chatActivity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);
                                    } else {
                                        chatActivity.attachFromFileStorage();
                                    }
                                } else {
                                    chatActivity.attachFromFileStorage();
                                }
                            }
                        }, 250);
                    } else {

                        if (emojiKeyboard != null) {
                            emojiKeyboard.hideBothKeyboard(this);
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                            if (!hasStoragePermission) {
                                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_STORAGE);

                            } else {
                                this.attachFromFileStorage();
                            }
                        } else {
                            this.attachFromFileStorage();
                        }
                    }
                }
                break;
            }
            case R.id.toolbar_chat:{
                logDebug("toolbar_chat");
                if(recordView.isRecordingNow()) break;

                showGroupInfoActivity();
                break;
            }
            case R.id.message_jump_layout:{
                goToEnd();
                break;
            }
            case R.id.pick_attach_chat:
            case R.id.rl_attach_icon_chat: {
                logDebug("Show attach bottom sheet");
                hideKeyboard();
                showSendAttachmentBottomSheet();
                break;
            }
            case R.id.join_button:{
                if (chatC.isInAnonymousMode()) {
                    ifAnonymousModeLogin(true);
                }
                else {
                    megaChatApi.autojoinPublicChat(idChat, this);
                }
                break;
            }
		}
    }

    public void sendLocation(){
        logDebug("sendLocation");
        if(MegaApplication.isEnabledGeoLocation()){
            getLocationPermission();
        }
        else{
            showSendLocationDialog();
        }
    }

    private void changeKeyboard(ImageButton btn){
        Drawable currentDrawable = btn.getDrawable();
        Drawable emojiDrawable = getResources().getDrawable(R.drawable.ic_emojicon);
        Drawable keyboardDrawable = getResources().getDrawable(R.drawable.ic_keyboard_white);
        if(areDrawablesIdentical(currentDrawable, emojiDrawable) && !emojiKeyboard.getEmojiKeyboardShown()){
            if(emojiKeyboard.getLetterKeyboardShown()){
                emojiKeyboard.hideLetterKeyboard();
                handlerKeyboard.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        emojiKeyboard.showEmojiKeyboard();
                    }
                },250);
            }else{
                emojiKeyboard.showEmojiKeyboard();
            }
        }else if(areDrawablesIdentical(currentDrawable, keyboardDrawable) && !emojiKeyboard.getLetterKeyboardShown()){
            emojiKeyboard.showLetterKeyboard();
        }
    }

    public void sendFromCloud(){
        attachFromCloud();
    }

    public void sendFromFileSystem(){
        attachPhotoVideo();
    }

    void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
        else {
            Intent intent =  new Intent(getApplicationContext(), MapsActivity.class);
            intent.putExtra(EDITING_MESSAGE, editingMessage);
            if (messageToEdit != null) {
                intent.putExtra(MSG_ID, messageToEdit.getMsgId());
            }
            startActivityForResult(intent, REQUEST_CODE_SEND_LOCATION);
        }
    }

    void showSendLocationDialog () {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_activity_maps)
                .setMessage(R.string.explanation_send_location)
                .setPositiveButton(getString(R.string.button_continue),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //getLocationPermission();
                        megaApi.enableGeolocation(chatActivity);
                    }
                })
                .setNegativeButton(R.string.general_cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            locationDialog.dismiss();
                            isLocationDialogShown = false;
                        } catch (Exception e){}
                    }
                });

        locationDialog = builder.create();
        locationDialog.setCancelable(false);
        locationDialog.setCanceledOnTouchOutside(false);
        locationDialog.show();
        isLocationDialogShown = true;
    }

    public void attachFromFileStorage(){
        logDebug("attachFromFileStorage");
        fileStorageF = ChatFileStorageFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container_file_storage, fileStorageF,"fileStorageF").commitNowAllowingStateLoss();
        hideInputText();
        fileStorageLayout.setVisibility(View.VISIBLE);
        pickFileStorageButton.setImageResource(R.drawable.ic_g_select_image);
        placeRecordButton(RECORD_BUTTON_DEACTIVATED);
    }

    public void attachFromCloud(){
        logDebug("attachFromCloud");
        if(megaApi!=null && megaApi.getRootNode()!=null){
            ChatController chatC = new ChatController(this);
            chatC.pickFileToSend();
        }
        else{
            logWarning("Online but not megaApi");
            showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
        }
    }

    public void attachPhotoVideo(){
        logDebug("attachPhotoVideo");

        disablePinScreen();

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("*/*");

        startActivityForResult(Intent.createChooser(intent, null), REQUEST_CODE_GET);
    }

    public void sendMessage(String text){
        logDebug("sendMessage: ");
        MegaChatMessage msgSent = megaChatApi.sendMessage(idChat, text);
        AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(msgSent);
        sendMessageToUI(androidMsgSent);
    }

    public void sendLocationMessage(float longLongitude, float longLatitude, String encodedSnapshot){
        logDebug("sendLocationMessage");
        MegaChatMessage locationMessage = megaChatApi.sendGeolocation(idChat, longLongitude, longLatitude, encodedSnapshot);
        if(locationMessage == null) return;
        AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(locationMessage);
        sendMessageToUI(androidMsgSent);

    }

    public void hideNewMessagesLayout(){
        logDebug("hideNewMessagesLayout");

        int position = positionNewMessagesLayout;

        positionNewMessagesLayout = -1;
        lastIdMsgSeen = -1;
        generalUnreadCount = -1;
        lastSeenReceived = true;
        newVisibility = false;

        if(adapter!=null){
            adapter.notifyItemChanged(position);
        }
    }

    public void openCameraApp(){
        logDebug("openCameraApp()");
        if(checkPermissionsTakePicture()){
            takePicture();
        }
    }

    public void sendMessageToUI(AndroidMegaChatMessage androidMsgSent){
        logDebug("sendMessageToUI");

        if(positionNewMessagesLayout!=-1){
            hideNewMessagesLayout();
        }

        int infoToShow = -1;

        int index = messages.size()-1;
        if(androidMsgSent!=null){
            if(androidMsgSent.isUploading()){
                logDebug("Is uploading: ");
            }
            else{
                logDebug("Sent message with id temp: " + androidMsgSent.getMessage().getTempId());
                logDebug("State of the message: " + androidMsgSent.getMessage().getStatus());
            }

            if(index==-1){
                //First element
                logDebug("First element!");
                messages.add(androidMsgSent);
                messages.get(0).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
            }
            else{
                //Not first element - Find where to add in the queue
                logDebug("NOT First element!");

                AndroidMegaChatMessage msg = messages.get(index);
                if(!androidMsgSent.isUploading()){
                    while(msg.isUploading()){
                        index--;
                        if (index == -1) {
                            break;
                        }
                        msg = messages.get(index);
                    }
                }

                while (!msg.isUploading() && msg.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL) {
                    index--;
                    if (index == -1) {
                        break;
                    }
                    msg = messages.get(index);
                }

                index++;
                logDebug("Add in position: " + index);
                messages.add(index, androidMsgSent);
                infoToShow = adjustInfoToShow(index);
            }

            if (adapter == null){
                logWarning("Adapter NULL");
                adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, messagesPlaying, listView);
                adapter.setHasStableIds(true);
                listView.setLayoutManager(mLayoutManager);
                listView.setAdapter(adapter);
                adapter.setMessages(messages);
            }else{
                logDebug("Adapter is NOT null - addMEssage()");
                adapter.addMessage(messages, index);
                if(infoToShow== AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL){
                    mLayoutManager.scrollToPositionWithOffset(index, scaleHeightPx(50, outMetrics));
                }else{
                    mLayoutManager.scrollToPositionWithOffset(index, scaleHeightPx(20, outMetrics));
                }
            }
        }
        else{
            logError("Error sending message!");
        }
    }

    public void sendMessagesToUI(ArrayList<AndroidMegaChatMessage> messages) {
        for (AndroidMegaChatMessage message : messages) {
            sendMessageToUI(message);
        }
    }

    public void editMessage(String text){
        logDebug("editMessage: ");
        MegaChatMessage msgEdited = null;

        if(messageToEdit.getMsgId()!=-1){
            msgEdited = megaChatApi.editMessage(idChat, messageToEdit.getMsgId(), text);
        }
        else{
            msgEdited = megaChatApi.editMessage(idChat, messageToEdit.getTempId(), text);
        }

        if(msgEdited!=null){
            logDebug("Edited message: status: " + msgEdited.getStatus());
            AndroidMegaChatMessage androidMsgEdited = new AndroidMegaChatMessage(msgEdited);
            modifyMessageReceived(androidMsgEdited, false);
        }
        else{
            logWarning("Message cannot be edited!");
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_editing_message), -1);
        }
    }

    public void editMessageMS(String text, MegaChatMessage messageToEdit){
        logDebug("editMessageMS: ");
        MegaChatMessage msgEdited = null;

        if(messageToEdit.getMsgId()!=-1){
            msgEdited = megaChatApi.editMessage(idChat, messageToEdit.getMsgId(), text);
        }
        else{
            msgEdited = megaChatApi.editMessage(idChat, messageToEdit.getTempId(), text);
        }

        if(msgEdited!=null){
            logDebug("Edited message: status: " + msgEdited.getStatus());
            AndroidMegaChatMessage androidMsgEdited = new AndroidMegaChatMessage(msgEdited);
            modifyMessageReceived(androidMsgEdited, false);
        }
        else{
            logWarning("Message cannot be edited!");
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_editing_message), -1);
        }
    }

    public void showUploadPanel(){
        if (isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        bottomSheetDialogFragment = new AttachmentUploadBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void activateActionMode(){
        if (!adapter.isMultipleSelect()){
            adapter.setMultipleSelect(true);
            actionMode = startSupportActionMode(new ActionBarCallBack());
        }
    }


    //Multiselect
    private class  ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            ArrayList<AndroidMegaChatMessage> messagesSelected = adapter.getSelectedMessages();

            switch(item.getItemId()){
                case R.id.chat_cab_menu_edit:{
                    logDebug("Edit text");
                    if (!messagesSelected.isEmpty() && messagesSelected.get(0) != null) {
                        editingMessage = true;
                        MegaChatMessage msg = messagesSelected.get(0).getMessage();
                        MegaChatContainsMeta meta = msg.getContainsMeta();
                        messageToEdit = msg;
                        textChat.setText(messageToEdit.getContent());
                        textChat.setSelection(textChat.getText().length());
                        if (msg.getType() == MegaChatMessage.TYPE_CONTAINS_META && meta != null && meta.getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION) {
                            sendLocation();
                            clearSelections();
                            hideMultipleSelect();
                            actionMode.invalidate();
                        }
                        else {
                            textChat.setText(messageToEdit.getContent());
                            textChat.setSelection(textChat.getText().length());
                        }
                    }
                    break;
                }
                case R.id.chat_cab_menu_forward:{
                    logDebug("Forward message");
                    forwardMessages(messagesSelected);
                    break;
                }
                case R.id.chat_cab_menu_copy:{
                    clearSelections();
                    hideMultipleSelect();
                    String text = "";

                    if(messagesSelected.size()==1){
                        AndroidMegaChatMessage message = messagesSelected.get(0);
                        text = chatC.createSingleManagementString(message, chatRoom);
                    }else{
                        text = copyMessages(messagesSelected);
                    }

                    if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setText(text);
                    } else {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
                        clipboard.setPrimaryClip(clip);
                    }
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.messages_copied_clipboard), -1);

                    break;
                }
                case R.id.chat_cab_menu_delete:{
                    clearSelections();
                    hideMultipleSelect();
                    //Delete
                    showConfirmationDeleteMessages(messagesSelected, chatRoom);
                    break;
                }
                case R.id.chat_cab_menu_download: {
                    logDebug("chat_cab_menu_download ");
                    clearSelections();
                    hideMultipleSelect();
                    if (!checkPermissionWriteStorage(REQUEST_WRITE_STORAGE)) {
                        preservedMessagesSelected = messagesSelected;
                        return false;
                    }
                    ArrayList<MegaNodeList> list = new ArrayList<>();
                    for (int i = 0; i < messagesSelected.size(); i++) {
                        MegaNodeList megaNodeList = messagesSelected.get(i).getMessage().getMegaNodeList();
                        list.add(megaNodeList);
                    }
                    chatC.prepareForChatDownload(list);
                    break;
                }
                case R.id.chat_cab_menu_import:{
                    clearSelections();
                    hideMultipleSelect();
                    chatC.importNodesFromAndroidMessages(messagesSelected);
                    break;
                }
                case R.id.chat_cab_menu_offline:{
                    clearSelections();
                    hideMultipleSelect();
                    if (!checkPermissionWriteStorage(REQUEST_WRITE_STORAGE_OFFLINE)) {
                        preservedMessagesSelected = messagesSelected;
                        return false;
                    }
                    chatC.saveForOfflineWithAndroidMessages(messagesSelected, chatRoom);
                    break;
                }
            }
            return false;
        }

        public String copyMessages(ArrayList<AndroidMegaChatMessage> messagesSelected){
            logDebug("copyMessages");
            ChatController chatC = new ChatController(chatActivity);
            StringBuilder builder = new StringBuilder();

            for(int i=0;i<messagesSelected.size();i++){
                AndroidMegaChatMessage messageSelected = messagesSelected.get(i);
                builder.append("[");
                String timestamp = formatShortDateTime(messageSelected.getMessage().getTimestamp());
                builder.append(timestamp);
                builder.append("] ");
                String messageString = chatC.createManagementString(messageSelected, chatRoom);
                builder.append(messageString);
                builder.append("\n");
            }
            return builder.toString();
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.messages_chat_action, menu);

            importIcon = menu.findItem(R.id.chat_cab_menu_import);
            menu.findItem(R.id.chat_cab_menu_offline).setIcon(mutateIconSecondary(chatActivity, R.drawable.ic_b_save_offline, R.color.white));

            changeStatusBarColorActionMode(chatActivity, getWindow(), handler, 1);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            logDebug("onDestroyActionMode");
            adapter.setMultipleSelect(false);
//            textChat.getText().clear();
            editingMessage = false;
            clearSelections();
            changeStatusBarColorActionMode(chatActivity, getWindow(), handler, 0);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            logDebug("onPrepareActionMode");
            List<AndroidMegaChatMessage> selected = adapter.getSelectedMessages();
            if (selected.size() !=0) {
//                MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
                if((chatRoom.getOwnPrivilege()==MegaChatRoom.PRIV_RM||chatRoom.getOwnPrivilege()==MegaChatRoom.PRIV_RO) && !chatRoom.isPreview()){

                    logDebug("Chat without permissions || without preview");

                    boolean showCopy = true;
                    for(int i=0; i<selected.size();i++) {
                        MegaChatMessage msg = selected.get(i).getMessage();
                        if ((showCopy) && (msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT || msg.getType()  == MegaChatMessage.TYPE_CONTACT_ATTACHMENT || msg.getType()  == MegaChatMessage.TYPE_VOICE_CLIP || ((msg.getType() == MegaChatMessage.TYPE_CONTAINS_META) && (msg.getContainsMeta() != null) && (msg.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION))) ) {
                            showCopy = false;
                        }
                    }
                    menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                    menu.findItem(R.id.chat_cab_menu_copy).setVisible(showCopy);
                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                    menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                    menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                    menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                    importIcon.setVisible(false);
                }
                else{
                    logDebug("Chat with permissions or preview");
                    if(isOnline(chatActivity) && !chatC.isInAnonymousMode()){
                        menu.findItem(R.id.chat_cab_menu_forward).setVisible(true);
                    }else{
                        menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                    }

                    if (selected.size() == 1) {
                        if(selected.get(0).isUploading()){
                            menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                            importIcon.setVisible(false);

                        }else if(selected.get(0).getMessage().getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){
                            logDebug("TYPE_NODE_ATTACHMENT selected");
                            menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);

                            if(selected.get(0).getMessage().getUserHandle()==myUserHandle && selected.get(0).getMessage().isDeletable()){
                                logDebug("one message Message DELETABLE");
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                            }else{
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                            }

                            if(isOnline(chatActivity)){
                                menu.findItem(R.id.chat_cab_menu_download).setVisible(true);
                                if (chatC.isInAnonymousMode()) {
                                    menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                                    importIcon.setVisible(false);
                                }
                                else {
                                    menu.findItem(R.id.chat_cab_menu_offline).setVisible(true);
                                    importIcon.setVisible(true);
                                }
                            }
                            else{
                                menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                                menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                                importIcon.setVisible(false);
                            }
                        }
                        else if(selected.get(0).getMessage().getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){
                            logDebug("TYPE_CONTACT_ATTACHMENT selected");

                            menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);

                            if(selected.get(0).getMessage().getUserHandle()==myUserHandle && selected.get(0).getMessage().isDeletable()){
                                logDebug("one message Message DELETABLE");
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                            }
                            else{
                                logDebug("one message Message NOT DELETABLE");
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                            }

                            menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                            importIcon.setVisible(false);
                        }
                        else if(selected.get(0).getMessage().getType()==MegaChatMessage.TYPE_VOICE_CLIP){
                            logDebug("TYPE_VOICE_CLIP selected");

                            menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);

                            if((selected.get(0).getMessage().getUserHandle()==myUserHandle) && (selected.get(0).getMessage().isDeletable())){
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                            }else{
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                            }
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                            importIcon.setVisible(false);

                        }
                        else{
                            logDebug("Other type: " + selected.get(0).getMessage().getType());

                            MegaChatMessage messageSelected= megaChatApi.getMessage(idChat, selected.get(0).getMessage().getMsgId());
                            if(messageSelected == null){
                                messageSelected = megaChatApi.getMessage(idChat, selected.get(0).getMessage().getTempId());
                                if(messageSelected == null){
                                    menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                                    menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                                    menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                                    menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                                    menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                                    importIcon.setVisible(false);
                                    return false;
                                }
                            }

                            if((messageSelected.getType() == MegaChatMessage.TYPE_CONTAINS_META) && (messageSelected.getContainsMeta()!=null && messageSelected.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION)){
                                logDebug("TYPE_CONTAINS_META && CONTAINS_META_GEOLOCATION");
                                menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                            }else{
                                menu.findItem(R.id.chat_cab_menu_copy).setVisible(true);
                            }

                            int type = selected.get(0).getMessage().getType();

                            if(messageSelected.getUserHandle()==myUserHandle){

                                if(messageSelected.isEditable()){
                                    logDebug("Message EDITABLE");
                                    menu.findItem(R.id.chat_cab_menu_edit).setVisible(true);
                                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                                }
                                else{
                                    logDebug("Message NOT EDITABLE");
                                    menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                                }

                                if (!isOnline(chatActivity) || type == MegaChatMessage.TYPE_TRUNCATE||type == MegaChatMessage.TYPE_ALTER_PARTICIPANTS||type == MegaChatMessage.TYPE_CHAT_TITLE||type == MegaChatMessage.TYPE_PRIV_CHANGE||type == MegaChatMessage.TYPE_CALL_ENDED||type == MegaChatMessage.TYPE_CALL_STARTED) {
                                    menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                                }
                                else{
                                    menu.findItem(R.id.chat_cab_menu_forward).setVisible(true);
                                }
                            }
                            else{
                                menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                                importIcon.setVisible(false);

                                if (chatC.isInAnonymousMode() || !isOnline(chatActivity) || type == MegaChatMessage.TYPE_TRUNCATE||type == MegaChatMessage.TYPE_ALTER_PARTICIPANTS||type == MegaChatMessage.TYPE_CHAT_TITLE||type == MegaChatMessage.TYPE_PRIV_CHANGE||type == MegaChatMessage.TYPE_CALL_ENDED||type == MegaChatMessage.TYPE_CALL_STARTED) {
                                    menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                                }
                                else{
                                    menu.findItem(R.id.chat_cab_menu_forward).setVisible(true);
                                }
                            }
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                            importIcon.setVisible(false);
                        }
                    }
                    else{
                        logDebug("Many items selected");
                        boolean isUploading = false;
                        boolean showDelete = true;
                        boolean showCopy = true;
                        boolean showForward = true;
                        boolean allNodeAttachments = true;

                        for(int i=0; i<selected.size();i++) {

                            if (!isUploading) {
                                if (selected.get(i).isUploading()) {
                                    isUploading = true;
                                }
                            }

                            MegaChatMessage msg = selected.get(i).getMessage();

                            if ((showCopy) && (msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT || msg.getType()  == MegaChatMessage.TYPE_CONTACT_ATTACHMENT || msg.getType()  == MegaChatMessage.TYPE_VOICE_CLIP || ((msg.getType() == MegaChatMessage.TYPE_CONTAINS_META) && (msg.getContainsMeta() != null) && (msg.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION))) ) {
                                showCopy = false;
                            }

                            if((showDelete) && ((msg.getUserHandle() != myUserHandle) || ((msg.getType() == MegaChatMessage.TYPE_NORMAL || msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT || msg.getType() == MegaChatMessage.TYPE_CONTACT_ATTACHMENT || msg.getType() == MegaChatMessage.TYPE_CONTAINS_META || msg.getType() == MegaChatMessage.TYPE_VOICE_CLIP) && (!(msg.isDeletable()))))){
                                showDelete = false;
                            }

                            if((showForward) &&(msg.getType() == MegaChatMessage.TYPE_TRUNCATE||msg.getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS||msg.getType() == MegaChatMessage.TYPE_CHAT_TITLE||msg.getType() == MegaChatMessage.TYPE_PRIV_CHANGE||msg.getType() == MegaChatMessage.TYPE_CALL_ENDED||msg.getType() == MegaChatMessage.TYPE_CALL_STARTED)) {
                                showForward = false;
                            }

                            if ((allNodeAttachments) && (selected.get(i).getMessage().getType() != MegaChatMessage.TYPE_NODE_ATTACHMENT)){
                                allNodeAttachments = false;
                            }
                        }

                        if (isUploading) {
                            menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                            importIcon.setVisible(false);
                        }
                        else {
                            if(allNodeAttachments && isOnline(chatActivity)){
                                menu.findItem(R.id.chat_cab_menu_download).setVisible(true);
                                if (chatC.isInAnonymousMode()){
                                    menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                                    importIcon.setVisible(false);
                                }
                                else {
                                    menu.findItem(R.id.chat_cab_menu_offline).setVisible(true);
                                    importIcon.setVisible(true);
                                }
                            }
                            else{
                                menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                                menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                                importIcon.setVisible(false);
                            }

                            menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                            if (chatC.isInAnonymousMode()){
                                menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                            }
                            else {
                                menu.findItem(R.id.chat_cab_menu_copy).setVisible(showCopy);
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(showDelete);
                            }
                            if(isOnline(chatActivity) && !chatC.isInAnonymousMode()){
                                menu.findItem(R.id.chat_cab_menu_forward).setVisible(showForward);
                            }
                            else{
                                menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                            }
                        }
                    }
                }
            }
            return false;
        }
    }

    public boolean showSelectMenuItem(){
        if (adapter != null){
            return adapter.isMultipleSelect();
        }
        return false;
    }

    public void showConfirmationDeleteMessages(final ArrayList<AndroidMegaChatMessage> messages, final MegaChatRoom chat){
        logDebug("showConfirmationDeleteMessages");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        stopReproductions();

                        ChatController cC = new ChatController(chatActivity);
                        cC.deleteAndroidMessages(messages, chat);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        }
        else{
            builder = new AlertDialog.Builder(this);
        }

        if(messages.size()==1){
            builder.setMessage(R.string.confirmation_delete_one_message);
        }
        else{
            builder.setMessage(R.string.confirmation_delete_several_messages);
        }
        builder.setPositiveButton(R.string.context_remove, dialogClickListener).setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void showConfirmationDeleteMessage(final long messageId, final long chatId){
        logDebug("showConfirmationDeleteMessage");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        ChatController cC = new ChatController(chatActivity);
                        cC.deleteMessageById(messageId, chatId);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        }
        else{
            builder = new AlertDialog.Builder(this);
        }

        builder.setMessage(R.string.confirmation_delete_one_message);
        builder.setPositiveButton(R.string.context_remove, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    /*
     * Clear all selected items
     */
    private void clearSelections() {
        if(adapter.isMultipleSelect()){
            adapter.clearSelections();
        }
        updateActionModeTitle();
    }

    private void updateActionModeTitle() {
        try {
            actionMode.setTitle(adapter.getSelectedItemCount()+"");
            actionMode.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
            logError("Invalidate error", e);
        }
    }

    /*
     * Disable selection
     */
    public void hideMultipleSelect() {
        adapter.setMultipleSelect(false);
        if (actionMode != null) {
            actionMode.finish();
        }
    }



    public void selectAll() {
        if (adapter != null) {
            if (adapter.isMultipleSelect()) {
                adapter.selectAll();
            } else {
                adapter.setMultipleSelect(true);
                adapter.selectAll();

                actionMode = startSupportActionMode(new ActionBarCallBack());
            }

            updateActionModeTitle();
        }
    }

    public void itemClick(int positionInAdapter, int [] screenPosition) {
        logDebug("Pposition: " + positionInAdapter);
        int positionInMessages = positionInAdapter-1;

        if(positionInMessages < messages.size()){
            AndroidMegaChatMessage m = messages.get(positionInMessages);

            if (adapter.isMultipleSelect()) {
                logDebug("isMultipleSelect");
                if (!m.isUploading()) {
                    logDebug("isMultipleSelect - iNOTsUploading");
                    if (m.getMessage() != null) {
                        MegaChatContainsMeta meta = m.getMessage().getContainsMeta();
                        if (meta != null && meta.getType() == MegaChatContainsMeta.CONTAINS_META_INVALID) {
                        }else{
                            logDebug("Message id: " + m.getMessage().getMsgId());
                            logDebug("Timestamp: " + m.getMessage().getTimestamp());

                            adapter.toggleSelection(positionInAdapter);
                            List<AndroidMegaChatMessage> messages = adapter.getSelectedMessages();
                            if (!messages.isEmpty()) {
                                updateActionModeTitle();
                            }
                        }
                    }

//                    adapter.toggleSelection(positionInAdapter);

//                    List<AndroidMegaChatMessage> messages = adapter.getSelectedMessages();
//                    if (messages.size() > 0) {
//                        updateActionModeTitle();
////                adapter.notifyDataSetChanged();
//                    }
////                    else {
////                        hideMultipleSelect();
////                    }
                }
            }else{
                if(m!=null){
                    if(m.isUploading()){
                        showUploadingAttachmentBottomSheet(m, positionInMessages);
                    }else{
                        if((m.getMessage().getStatus()==MegaChatMessage.STATUS_SERVER_REJECTED)||(m.getMessage().getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL)){
                            if(m.getMessage().getUserHandle()==megaChatApi.getMyUserHandle()) {
                                if (!(m.getMessage().isManagementMessage())) {
                                    logDebug("selected message handle: " + m.getMessage().getTempId());
                                    logDebug("selected message rowId: " + m.getMessage().getRowId());
                                    if ((m.getMessage().getStatus() == MegaChatMessage.STATUS_SERVER_REJECTED) || (m.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                        logDebug("show not sent message panel");
                                        showMsgNotSentPanel(m, positionInMessages);
                                    }
                                }
                            }
                        }
                        else{
                            if(m.getMessage().getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){
                                logDebug("itemCLick: TYPE_NODE_ATTACHMENT");
                                MegaNodeList nodeList = m.getMessage().getMegaNodeList();
                                if(nodeList.size()==1){
                                    MegaNode node = chatC.authorizeNodeIfPreview(nodeList.get(0), chatRoom);
                                    if (MimeTypeList.typeForName(node.getName()).isImage()){

                                        if(node.hasPreview()){
                                            logDebug("Show full screen viewer");
                                            showFullScreenViewer(m.getMessage().getMsgId(), screenPosition);
                                        }
                                        else{
                                            logDebug("Image without preview - show node attachment panel for one node");
                                            showNodeAttachmentBottomSheet(m, positionInMessages);
                                        }
                                    }
                                    else if (MimeTypeList.typeForName(node.getName()).isVideoReproducible()||MimeTypeList.typeForName(node.getName()).isAudio()){
                                        logDebug("isFile:isVideoReproducibleOrIsAudio");
                                        String mimeType = MimeTypeList.typeForName(node.getName()).getType();
                                        logDebug("FILE HANDLE: " + node.getHandle() + " TYPE: " + mimeType);

                                        Intent mediaIntent;
                                        boolean internalIntent;
                                        boolean opusFile = false;
                                        if (MimeTypeList.typeForName(node.getName()).isVideoNotSupported() || MimeTypeList.typeForName(node.getName()).isAudioNotSupported()){
                                            mediaIntent = new Intent(Intent.ACTION_VIEW);
                                            internalIntent=false;
                                            String[] s = node.getName().split("\\.");
                                            if (s != null && s.length > 1 && s[s.length-1].equals("opus")) {
                                                opusFile = true;
                                            }
                                        }
                                        else {
                                            logDebug("setIntentToAudioVideoPlayer");
                                            mediaIntent = new Intent(this, AudioVideoPlayerLollipop.class);
                                            internalIntent=true;
                                        }
                                        logDebug("putExtra: screenPosition("+screenPosition+"), msgId("+m.getMessage().getMsgId()+"), chatId("+idChat+"), filename("+node.getName()+")");

                                        mediaIntent.putExtra("screenPosition", screenPosition);
                                        mediaIntent.putExtra("adapterType", FROM_CHAT);
                                        mediaIntent.putExtra(IS_PLAYLIST, false);
                                        mediaIntent.putExtra("msgId", m.getMessage().getMsgId());
                                        mediaIntent.putExtra("chatId", idChat);
                                        mediaIntent.putExtra("FILENAME", node.getName());

                                        String downloadLocationDefaultPath = getDownloadLocation(this);
                                        String localPath = getLocalFile(this, node.getName(), node.getSize(), downloadLocationDefaultPath);

                                        File f = new File(downloadLocationDefaultPath, node.getName());
                                        boolean isOnMegaDownloads = false;
                                        if(f.exists() && (f.length() == node.getSize())){
                                            isOnMegaDownloads = true;
                                        }
                                        logDebug("isOnMegaDownloads: " + isOnMegaDownloads);
                                        if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(node) != null && megaApi.getFingerprint(node).equals(megaApi.getFingerprint(localPath))))){
                                            logDebug("localPath != null");

                                            File mediaFile = new File(localPath);
                                            //mediaIntent.setDataAndType(Uri.parse(localPath), mimeType);
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                                                logDebug("FileProviderOption");
                                                Uri mediaFileUri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", mediaFile);
                                                if(mediaFileUri==null){
                                                    logDebug("ERROR:NULLmediaFileUri");
                                                    showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                                                }
                                                else{
                                                    mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
                                                }
                                            }else{
                                                Uri mediaFileUri = Uri.fromFile(mediaFile);
                                                if(mediaFileUri==null){
                                                    logError("ERROR:NULLmediaFileUri");
                                                    showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                                                }
                                                else{
                                                    mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
                                                }
                                            }
                                            mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        }else {
                                            logDebug("localPathNULL");
                                            if (isOnline(this)){
                                                if (megaApi.httpServerIsRunning() == 0) {
                                                    logDebug("megaApi.httpServerIsRunning() == 0");
                                                    megaApi.httpServerStart();
                                                }
                                                else{
                                                    logWarning("ERROR:httpServerAlreadyRunning");
                                                }

                                                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                                                ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
                                                activityManager.getMemoryInfo(mi);

                                                if(mi.totalMem>BUFFER_COMP){
                                                    logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                                                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                                                }else{
                                                    logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                                                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                                                }

                                                String url = megaApi.httpServerGetLocalLink(node);
                                                if(url!=null){
                                                    logDebug("URL generated: " + url);
                                                    Uri parsedUri = Uri.parse(url);
                                                    if(parsedUri!=null){
                                                        logDebug("parsedUri!=null ---> " + parsedUri);
                                                        mediaIntent.setDataAndType(parsedUri, mimeType);
                                                    }else{
                                                        logError("ERROR:httpServerGetLocalLink");
                                                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                                                    }
                                                }else{
                                                    logError("ERROR:httpServerGetLocalLink");
                                                    showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                                                }
                                            }
                                            else {
                                                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem)+". "+ getString(R.string.no_network_connection_on_play_file), -1);
                                            }
                                        }
                                        mediaIntent.putExtra("HANDLE", node.getHandle());
                                        if (opusFile){
                                            logDebug("opusFile ");
                                            mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
                                        }
                                        if(internalIntent){
                                            startActivity(mediaIntent);
                                        }else{
                                            logDebug("externalIntent");
                                            if (isIntentAvailable(this, mediaIntent)){
                                                startActivity(mediaIntent);
                                            }else{
                                                logDebug("noAvailableIntent");
                                                showNodeAttachmentBottomSheet(m, positionInMessages);
                                            }
                                        }
                                        overridePendingTransition(0,0);
                                        if (adapter != null) {
                                            adapter.setNodeAttachmentVisibility(false, holder_imageDrag, positionInMessages);
                                        }

                                    }else if (MimeTypeList.typeForName(node.getName()).isPdf()){

                                        logDebug("isFile:isPdf");
                                        String mimeType = MimeTypeList.typeForName(node.getName()).getType();
                                        logDebug("FILE HANDLE: " + node.getHandle() + " TYPE: " + mimeType);
                                        Intent pdfIntent = new Intent(this, PdfViewerActivityLollipop.class);
                                        pdfIntent.putExtra("inside", true);
                                        pdfIntent.putExtra("adapterType", FROM_CHAT);
                                        pdfIntent.putExtra("msgId", m.getMessage().getMsgId());
                                        pdfIntent.putExtra("chatId", idChat);

                                        String downloadLocationDefaultPath = getDownloadLocation(this);
                                        String localPath = getLocalFile(this, node.getName(), node.getSize(), downloadLocationDefaultPath);
                                        File f = new File(downloadLocationDefaultPath, node.getName());
                                        boolean isOnMegaDownloads = false;
                                        if(f.exists() && (f.length() == node.getSize())){
                                            isOnMegaDownloads = true;
                                        }
                                        logDebug("isOnMegaDownloads: " + isOnMegaDownloads);
                                        if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(node) != null && megaApi.getFingerprint(node).equals(megaApi.getFingerprint(localPath))))){
                                            File mediaFile = new File(localPath);
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                                                logDebug("FileProviderOption");
                                                Uri mediaFileUri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", mediaFile);
                                                if(mediaFileUri==null){
                                                    logError("ERROR:NULLmediaFileUri");
                                                    showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                                                }
                                                else{
                                                    pdfIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
                                                }
                                            }
                                            else{
                                                Uri mediaFileUri = Uri.fromFile(mediaFile);
                                                if(mediaFileUri==null){
                                                    logError("ERROR:NULLmediaFileUri");
                                                    showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                                                }
                                                else{
                                                    pdfIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
                                                }
                                            }
                                            pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        }
                                        else {
                                            logWarning("localPathNULL");
                                            if (isOnline(this)){
                                                if (megaApi.httpServerIsRunning() == 0) {
                                                    megaApi.httpServerStart();
                                                }
                                                else{
                                                    logError("ERROR:httpServerAlreadyRunning");
                                                }
                                                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                                                ActivityManager activityManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
                                                activityManager.getMemoryInfo(mi);
                                                if(mi.totalMem>BUFFER_COMP){
                                                    logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                                                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                                                }
                                                else{
                                                    logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                                                    megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                                                }
                                                String url = megaApi.httpServerGetLocalLink(node);
                                                if(url!=null){
                                                    logDebug("URL generated: " + url);
                                                    Uri parsedUri = Uri.parse(url);
                                                    if(parsedUri!=null){
                                                        pdfIntent.setDataAndType(parsedUri, mimeType);
                                                    }
                                                    else{
                                                        logError("ERROR:httpServerGetLocalLink");
                                                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                                                    }
                                                }
                                                else{
                                                    logError("ERROR:httpServerGetLocalLink");
                                                    showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), -1);
                                                }
                                            }
                                            else {
                                                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem)+". "+ getString(R.string.no_network_connection_on_play_file), -1);
                                            }
                                        }
                                        pdfIntent.putExtra("HANDLE", node.getHandle());

                                        if (isIntentAvailable(this, pdfIntent)){
                                            startActivity(pdfIntent);
                                        }
                                        else{
                                            logWarning("noAvailableIntent");
                                            showNodeAttachmentBottomSheet(m, positionInMessages);
                                        }
                                        overridePendingTransition(0,0);
                                    }
                                    else{
                                        logDebug("NOT Image, pdf, audio or video - show node attachment panel for one node");
                                        showNodeAttachmentBottomSheet(m, positionInMessages);
                                    }
                                }
                                else{
                                    logDebug("show node attachment panel");
                                    showNodeAttachmentBottomSheet(m, positionInMessages);
                                }
                            }
                            else if(m.getMessage().getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){
                                logDebug("TYPE_CONTACT_ATTACHMENT");
                                logDebug("show contact attachment panel");
                                if (isOnline(this)) {
                                    if (!chatC.isInAnonymousMode() && m != null) {
                                        if (m.getMessage().getUsersCount() == 1) {
                                            long userHandle = m.getMessage().getUserHandle(0);
                                            if(userHandle != megaChatApi.getMyUserHandle()){
                                                showContactAttachmentBottomSheet(m, positionInMessages);
                                            }
                                        }else{
                                            showContactAttachmentBottomSheet(m, positionInMessages);
                                        }
                                    }
                                }
                                else{
                                    //No shown - is not possible to know is it already contact or not - megaApi not working
                                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                                }
                            } else if (m.getMessage().getType() == MegaChatMessage.TYPE_CONTAINS_META) {
                                logDebug("TYPE_CONTAINS_META");
                                MegaChatContainsMeta meta = m.getMessage().getContainsMeta();
                                if (meta == null || meta.getType() == MegaChatContainsMeta.CONTAINS_META_INVALID)
                                    return;
                                String url = null;
                                if (meta.getType() == MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW) {
                                    url = meta.getRichPreview().getUrl();
                                } else if (meta.getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION) {
                                    url = m.getMessage().getContent();
                                    MegaChatGeolocation location = meta.getGeolocation();
                                    if (location != null) {
                                        float latitude = location.getLatitude();
                                        float longitude = location.getLongitude();
                                        List<Address> addresses = getAddresses(this, latitude, longitude);
                                        if (addresses != null && !addresses.isEmpty()) {
                                            String address = addresses.get(0).getAddressLine(0);
                                            if (address != null) {
                                                url = "geo:" + latitude + "," + longitude + "?q=" + Uri.encode(address);
                                            }
                                        }
                                    }
                                }
                                if (url == null) return;
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                startActivity(browserIntent);

                            } else if(m.getMessage().getType() == MegaChatMessage.TYPE_NORMAL && m.getRichLinkMessage()!=null){
                                logDebug("TYPE_NORMAL");
                                AndroidMegaRichLinkMessage richLinkMessage = m.getRichLinkMessage();
                                String url = richLinkMessage.getUrl();

                                if(richLinkMessage.isChat()){
                                    loadChatLink(url);
                                }
                                else{
                                    if(richLinkMessage.getNode()!=null){
                                        if(richLinkMessage.getNode().isFile()){
                                            openMegaLink(url, true);
                                        }
                                        else{
                                            openMegaLink(url, false);
                                        }
                                    }
                                    else{
                                        if(richLinkMessage.isFile()){
                                            openMegaLink(url, true);
                                        }
                                        else{
                                            openMegaLink(url, false);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }else{
            logDebug("DO NOTHING: Position (" + positionInMessages + ") is more than size in messages (size: " + messages.size() + ")");
        }
    }

    public void loadChatLink(String link){
        logDebug("loadChatLink: ");
        Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
        intentOpenChat.setAction(ACTION_OPEN_CHAT_LINK);
        intentOpenChat.setData(Uri.parse(link));
        this.startActivity(intentOpenChat);
    }

    public void showFullScreenViewer(long msgId, int[] screenPosition){
        logDebug("showFullScreenViewer");
        int position = 0;
        boolean positionFound = false;
        List<Long> ids = new ArrayList<>();
        for(int i=0; i<messages.size();i++){
            AndroidMegaChatMessage androidMessage = messages.get(i);
            if(!androidMessage.isUploading()){
                MegaChatMessage msg = androidMessage.getMessage();

                if(msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){
                    ids.add(msg.getMsgId());

                    if(msg.getMsgId()==msgId){
                        positionFound=true;
                    }
                    if(!positionFound){
                        MegaNodeList nodeList = msg.getMegaNodeList();
                        if(nodeList.size()==1){
                            MegaNode node = nodeList.get(0);
                            if(MimeTypeList.typeForName(node.getName()).isImage()){
                                position++;
                            }
                        }
                    }
                }
            }
        }

        Intent intent = new Intent(this, ChatFullScreenImageViewer.class);
        intent.putExtra("position", position);
        intent.putExtra("chatId", idChat);
        intent.putExtra("screenPosition", screenPosition);
        long[] array = new long[ids.size()];
        for(int i = 0; i < ids.size(); i++) {
            array[i] = ids.get(i);
        }
        intent.putExtra("messageIds", array);
        startActivity(intent);
        overridePendingTransition(0,0);
        if (adapter !=  null) {
            adapter.setNodeAttachmentVisibility(false, holder_imageDrag, position);
        }
    }

    @Override
    public void onChatRoomUpdate(MegaChatApiJava api, MegaChatRoom chat) {
        logDebug("onChatRoomUpdate!");
        this.chatRoom = chat;
        if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_CLOSED)){
            logDebug("CHANGE_TYPE_CLOSED for the chat: " + chat.getChatId());
            int permission = chat.getOwnPrivilege();
            logDebug("Permissions for the chat: " + permission);

            if(chat.isPreview()){
                if(permission==MegaChatRoom.PRIV_RM){
                    //Show alert to user
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.alert_invalid_preview), -1);
                }
            }
            else{
                //Hide field to write
                setChatSubtitle();
                supportInvalidateOptionsMenu();
            }
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_STATUS)){
            logDebug("CHANGE_TYPE_STATUS for the chat: " + chat.getChatId());
            if(!(chatRoom.isGroup())){
                long userHandle = chatRoom.getPeerHandle(0);
                setStatus(userHandle);
            }
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_PARTICIPANTS)){
            logDebug("CHANGE_TYPE_PARTICIPANTS for the chat: " + chat.getChatId());
            setChatSubtitle();
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_OWN_PRIV)){
            logDebug("CHANGE_TYPE_OWN_PRIV for the chat: " + chat.getChatId());
            setChatSubtitle();
            supportInvalidateOptionsMenu();
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_TITLE)){
            logDebug("CHANGE_TYPE_TITLE for the chat: " + chat.getChatId());
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_USER_STOP_TYPING)){
            logDebug("CHANGE_TYPE_USER_STOP_TYPING for the chat: " + chat.getChatId());

            long userHandleTyping = chat.getUserTyping();

            if(userHandleTyping==megaChatApi.getMyUserHandle()){
                return;
            }

            if(usersTypingSync==null){
                return;
            }

            //Find the item
            boolean found = false;
            for(UserTyping user : usersTypingSync) {
                if(user.getParticipantTyping().getHandle() == userHandleTyping) {
                    logDebug("Found user typing!");
                    usersTypingSync.remove(user);
                    found=true;
                    break;
                }
            }

            if(!found){
                logDebug("CHANGE_TYPE_USER_STOP_TYPING: Not found user typing");
            }
            else{
                updateUserTypingFromNotification();
            }

        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_USER_TYPING)){
            logDebug("CHANGE_TYPE_USER_TYPING for the chat: " + chat.getChatId());
            if(chat!=null){

                long userHandleTyping = chat.getUserTyping();

                if(userHandleTyping==megaChatApi.getMyUserHandle()){
                    return;
                }

                if(usersTyping==null){
                    usersTyping = new ArrayList<UserTyping>();
                    usersTypingSync = Collections.synchronizedList(usersTyping);
                }

                //Find if any notification arrives previously
                if(usersTypingSync.size()<=0){
                    logDebug("No more users writing");
                    MegaChatParticipant participantTyping = new MegaChatParticipant(userHandleTyping);
                    UserTyping currentUserTyping = new UserTyping(participantTyping);

                    String nameTyping = chatC.getFirstName(userHandleTyping, chatRoom);

                    logDebug("userHandleTyping: " + userHandleTyping);


                    if(nameTyping==null){
                        logWarning("NULL name");
                        nameTyping = getString(R.string.transfer_unknown);
                    }
                    else{
                        if(nameTyping.trim().isEmpty()){
                            logWarning("EMPTY name");
                            nameTyping = getString(R.string.transfer_unknown);
                        }
                    }
                    participantTyping.setFirstName(nameTyping);

                    userTypingTimeStamp = System.currentTimeMillis()/1000;
                    currentUserTyping.setTimeStampTyping(userTypingTimeStamp);

                    usersTypingSync.add(currentUserTyping);

                    String userTyping =  getResources().getQuantityString(R.plurals.user_typing, 1, toCDATA(usersTypingSync.get(0).getParticipantTyping().getFirstName()));

                    userTyping = userTyping.replace("[A]", "<font color=\'#8d8d94\'>");
                    userTyping = userTyping.replace("[/A]", "</font>");

                    Spanned result = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        result = Html.fromHtml(userTyping,Html.FROM_HTML_MODE_LEGACY);
                    } else {
                        result = Html.fromHtml(userTyping);
                    }

                    userTypingText.setText(result);

                    userTypingLayout.setVisibility(View.VISIBLE);
                }
                else{
                    logDebug("More users writing or the same in different timestamp");

                    //Find the item
                    boolean found = false;
                    for(UserTyping user : usersTypingSync) {
                        if(user.getParticipantTyping().getHandle() == userHandleTyping) {
                            logDebug("Found user typing!");
                            userTypingTimeStamp = System.currentTimeMillis()/1000;
                            user.setTimeStampTyping(userTypingTimeStamp);
                            found=true;
                            break;
                        }
                    }

                    if(!found){
                        logDebug("It's a new user typing");
                        MegaChatParticipant participantTyping = new MegaChatParticipant(userHandleTyping);
                        UserTyping currentUserTyping = new UserTyping(participantTyping);

                        String nameTyping = chatC.getFirstName(userHandleTyping, chatRoom);
                        if(nameTyping==null){
                            logWarning("NULL name");
                            nameTyping = getString(R.string.transfer_unknown);
                        }
                        else{
                            if(nameTyping.trim().isEmpty()){
                                logWarning("EMPTY name");
                                nameTyping = getString(R.string.transfer_unknown);
                            }
                        }
                        participantTyping.setFirstName(nameTyping);

                        userTypingTimeStamp = System.currentTimeMillis()/1000;
                        currentUserTyping.setTimeStampTyping(userTypingTimeStamp);

                        usersTypingSync.add(currentUserTyping);

                        //Show the notification
                        int size = usersTypingSync.size();
                        switch (size){
                            case 1:{
                                String userTyping = getResources().getQuantityString(R.plurals.user_typing, 1, usersTypingSync.get(0).getParticipantTyping().getFirstName());
                                userTyping = toCDATA(userTyping);
                                userTyping = userTyping.replace("[A]", "<font color=\'#8d8d94\'>");
                                userTyping = userTyping.replace("[/A]", "</font>");

                                Spanned result = null;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    result = Html.fromHtml(userTyping,Html.FROM_HTML_MODE_LEGACY);
                                } else {
                                    result = Html.fromHtml(userTyping);
                                }

                                userTypingText.setText(result);
                                break;
                            }
                            case 2:{
                                String userTyping = getResources().getQuantityString(R.plurals.user_typing, 2, usersTypingSync.get(0).getParticipantTyping().getFirstName()+", "+usersTypingSync.get(1).getParticipantTyping().getFirstName());
                                userTyping = toCDATA(userTyping);
                                userTyping = userTyping.replace("[A]", "<font color=\'#8d8d94\'>");
                                userTyping = userTyping.replace("[/A]", "</font>");

                                Spanned result = null;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    result = Html.fromHtml(userTyping,Html.FROM_HTML_MODE_LEGACY);
                                } else {
                                    result = Html.fromHtml(userTyping);
                                }

                                userTypingText.setText(result);
                                break;
                            }
                            default:{
                                String names = usersTypingSync.get(0).getParticipantTyping().getFirstName()+", "+usersTypingSync.get(1).getParticipantTyping().getFirstName();
                                String userTyping = String.format(getString(R.string.more_users_typing),toCDATA(names));

                                userTyping = userTyping.replace("[A]", "<font color=\'#8d8d94\'>");
                                userTyping = userTyping.replace("[/A]", "</font>");

                                Spanned result = null;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    result = Html.fromHtml(userTyping,Html.FROM_HTML_MODE_LEGACY);
                                } else {
                                    result = Html.fromHtml(userTyping);
                                }

                                userTypingText.setText(result);
                                break;
                            }
                        }
                        userTypingLayout.setVisibility(View.VISIBLE);
                    }
                }

                int interval = 5000;
                IsTypingRunnable runnable = new IsTypingRunnable(userTypingTimeStamp, userHandleTyping);
                handlerReceive = new Handler();
                handlerReceive.postDelayed(runnable, interval);
            }
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_ARCHIVE)){
            logDebug("CHANGE_TYPE_ARCHIVE for the chat: " + chat.getChatId());
            setChatSubtitle();
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_CHAT_MODE)){
            logDebug("CHANGE_TYPE_CHAT_MODE for the chat: " + chat.getChatId());
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_UPDATE_PREVIEWERS)){
            logDebug("CHANGE_TYPE_UPDATE_PREVIEWERS for the chat: " + chat.getChatId());
            setPreviewersView();
        }
    }

    void setPreviewersView () {
        if(chatRoom.getNumPreviewers()>0){
            observersNumberText.setText(chatRoom.getNumPreviewers()+"");
            observersLayout.setVisibility(View.VISIBLE);
        }
        else{
            observersLayout.setVisibility(View.GONE);
        }
    }

    private class IsTypingRunnable implements Runnable{

        long timeStamp;
        long userHandleTyping;

        public IsTypingRunnable(long timeStamp, long userHandleTyping) {
            this.timeStamp = timeStamp;
            this.userHandleTyping = userHandleTyping;
        }

        @Override
        public void run() {
            logDebug("Run off notification typing");
            long timeNow = System.currentTimeMillis()/1000;
            if ((timeNow - timeStamp) > 4){
                logDebug("Remove user from the list");

                boolean found = false;
                for(UserTyping user : usersTypingSync) {
                    if(user.getTimeStampTyping() == timeStamp) {
                        if(user.getParticipantTyping().getHandle() == userHandleTyping) {
                            logDebug("Found user typing in runnable!");
                            usersTypingSync.remove(user);
                            found=true;
                            break;
                        }
                    }
                }

                if(!found){
                    logDebug("Not found user typing in runnable!");
                }

                updateUserTypingFromNotification();
            }
        }
    }

    public void updateUserTypingFromNotification(){
        logDebug("updateUserTypingFromNotification");

        int size = usersTypingSync.size();
        logDebug("Size of typing: " + size);
        switch (size){
            case 0:{
                userTypingLayout.setVisibility(View.GONE);
                break;
            }
            case 1:{
                String userTyping = getResources().getQuantityString(R.plurals.user_typing, 1, usersTypingSync.get(0).getParticipantTyping().getFirstName());
                userTyping = toCDATA(userTyping);
                userTyping = userTyping.replace("[A]", "<font color=\'#8d8d94\'>");
                userTyping = userTyping.replace("[/A]", "</font>");

                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(userTyping,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(userTyping);
                }

                userTypingText.setText(result);
                break;
            }
            case 2:{
                String userTyping = getResources().getQuantityString(R.plurals.user_typing, 2, usersTypingSync.get(0).getParticipantTyping().getFirstName()+", "+usersTypingSync.get(1).getParticipantTyping().getFirstName());
                userTyping = toCDATA(userTyping);
                userTyping = userTyping.replace("[A]", "<font color=\'#8d8d94\'>");
                userTyping = userTyping.replace("[/A]", "</font>");

                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(userTyping,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(userTyping);
                }

                userTypingText.setText(result);
                break;
            }
            default:{
                String names = usersTypingSync.get(0).getParticipantTyping().getFirstName()+", "+usersTypingSync.get(1).getParticipantTyping().getFirstName();
                String userTyping = String.format(getString(R.string.more_users_typing), toCDATA(names));

                userTyping = userTyping.replace("[A]", "<font color=\'#8d8d94\'>");
                userTyping = userTyping.replace("[/A]", "</font>");

                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(userTyping,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(userTyping);
                }

                userTypingText.setText(result);
                break;
            }
        }
    }

    public void setRichLinkInfo(long msgId, AndroidMegaRichLinkMessage richLinkMessage){
        logDebug("setRichLinkInfo");

        int indexToChange = -1;
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        // Iterate in reverse.
        while(itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();

            if(!messageToCheck.isUploading()){
                if(messageToCheck.getMessage().getMsgId()==msgId){
                    indexToChange = itr.nextIndex();
                    logDebug("Found index to change: " + indexToChange);
                    break;
                }
            }
        }

        if(indexToChange!=-1){

            AndroidMegaChatMessage androidMsg = messages.get(indexToChange);

            androidMsg.setRichLinkMessage(richLinkMessage);

            try{
                if(adapter!=null){
                    adapter.notifyItemChanged(indexToChange+1);
                }
            }
            catch(IllegalStateException e){
                logError("IllegalStateException: do not update adapter", e);
            }

        }
        else{
            logError("Error, rich link message not found!!");
        }
    }

    public void setRichLinkImage(long msgId){
        logDebug("setRichLinkImage");

        int indexToChange = -1;
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        // Iterate in reverse.
        while(itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();

            if(!messageToCheck.isUploading()){
                if(messageToCheck.getMessage().getMsgId()==msgId){
                    indexToChange = itr.nextIndex();
                    logDebug("Found index to change: " + indexToChange);
                    break;
                }
            }
        }

        if(indexToChange!=-1){

            if(adapter!=null){
                adapter.notifyItemChanged(indexToChange+1);
            }
        }
        else{
            logError("Error, rich link message not found!!");
        }
    }

    public int checkMegaLink(MegaChatMessage msg){
        logDebug("checkMegaLink");

        //Check if it is a MEGA link
        if (msg.getType() != MegaChatMessage.TYPE_NORMAL || msg.getContent() == null) return -1;

        String link = extractMegaLink(msg.getContent());

        if (isChatLink(link)) {
            logDebug("isChatLink");
            ChatLinkInfoListener listener = new ChatLinkInfoListener(this, msg.getMsgId(), megaApi);
            megaChatApi.checkChatLink(link, listener);

            return MEGA_CHAT_LINK;
        }

        if (link == null || megaApi == null || megaApi.getRootNode() == null) return -1;

        logDebug("The link was found");

        ChatLinkInfoListener listener = null;
        if (isFileLink(link)) {
            logDebug("isFileLink");
            listener = new ChatLinkInfoListener(this, msg.getMsgId(), megaApi);
            megaApi.getPublicNode(link, listener);
            return MEGA_FILE_LINK;
        } else {
            logDebug("isFolderLink");

            MegaApiAndroid megaApiFolder = getLocalMegaApiFolder();
            listener = new ChatLinkInfoListener(this, msg.getMsgId(), megaApi, megaApiFolder);
            megaApiFolder.loginToFolder(link, listener);
            return MEGA_FOLDER_LINK;
        }
    }

    @Override
    public void onMessageLoaded(MegaChatApiJava api, MegaChatMessage msg) {
        logDebug("onMessageLoaded");

        if(msg!=null){
            logDebug("STATUS: " + msg.getStatus());
            logDebug("TEMP ID: " + msg.getTempId());
            logDebug("FINAL ID: " + msg.getMsgId());
            logDebug("TIMESTAMP: " + msg.getTimestamp());
            logDebug("TYPE: " + msg.getType());

            if(messages!=null){
                logDebug("Messages size: "+messages.size());
            }

            if(msg.isDeleted()){
                logDebug("DELETED MESSAGE!!!!");
                return;
            }

            if(msg.isEdited()){
                logDebug("EDITED MESSAGE!!!!");
            }

            if(msg.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT) {
                logDebug("TYPE_REVOKE_NODE_ATTACHMENT MESSAGE!!!!");
                return;
            }

            checkMegaLink(msg);

            if(msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){
                logDebug("TYPE_NODE_ATTACHMENT MESSAGE!!!!");
                MegaNodeList nodeList = msg.getMegaNodeList();
                int revokedCount = 0;

                for(int i=0; i<nodeList.size(); i++){
                    MegaNode node = nodeList.get(i);
                    boolean revoked = megaChatApi.isRevoked(idChat, node.getHandle());
                    if(revoked){
                        logDebug("The node is revoked: " + node.getHandle());
                        revokedCount++;
                    }
                    else{
                        logDebug("Node NOT revoked: " + node.getHandle());
                    }
                }

                if(revokedCount==nodeList.size()){
                    logDebug("RETURN");
                    return;
                }
            }

            if(msg.getStatus()==MegaChatMessage.STATUS_SERVER_REJECTED){
                logDebug("STATUS_SERVER_REJECTED " + msg.getStatus());
            }

            if(msg.getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL){

                logDebug("STATUS_SENDING_MANUAL: Getting messages not sent!!!: " + msg.getStatus());
                AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);

                if(msg.isEdited()){
                    logDebug("MESSAGE EDITED");

                    if(!noMoreNoSentMessages){
                        logDebug("NOT noMoreNoSentMessages");
                        addInBufferSending(androidMsg);
                    }else{
                        logDebug("Try to recover the initial msg");
                        if(msg.getMsgId()!=-1){
                            MegaChatMessage notEdited = megaChatApi.getMessage(idChat, msg.getMsgId());
                            logDebug("Content not edited");
                            AndroidMegaChatMessage androidMsgNotEdited = new AndroidMegaChatMessage(notEdited);
                            int returnValue = modifyMessageReceived(androidMsgNotEdited, false);
                            if(returnValue!=-1){
                                logDebug("Message " + returnValue + " modified!");
                            }
                        }

                        appendMessageAnotherMS(androidMsg);
                    }
                }
                else{
                    logDebug("NOT MESSAGE EDITED");
                    int resultModify = -1;
                    if(msg.getUserHandle()==megaChatApi.getMyUserHandle()){
                        if(msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){
                            logDebug("Modify my message and node attachment");

                            long idMsg =  dbH.findPendingMessageByIdTempKarere(msg.getTempId());
                            logDebug("The id of my pending message is: " + idMsg);
                            if(idMsg!=-1){
                                resultModify = modifyAttachmentReceived(androidMsg, idMsg);
                                dbH.removePendingMessageById(idMsg);
                                if(resultModify==-1){
                                    logDebug("Node attachment message not in list -> resultModify -1");
//                            AndroidMegaChatMessage msgToAppend = new AndroidMegaChatMessage(msg);
//                            appendMessagePosition(msgToAppend);
                                }
                                else{
                                    logDebug("Modify attachment");
                                    return;
                                }
                            }
                        }
                    }

                    int returnValue = modifyMessageReceived(androidMsg, true);
                    if(returnValue!=-1){
                        logDebug("Message " + returnValue + " modified!");
                        return;
                    }
                    addInBufferSending(androidMsg);
                    if(!noMoreNoSentMessages){
                        logDebug("NOT noMoreNoSentMessages");
                    }
                }
            }
            else if(msg.getStatus()==MegaChatMessage.STATUS_SENDING){
                logDebug("SENDING: Getting messages not sent !!!-------------------------------------------------: "+msg.getStatus());
                AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);
                int returnValue = modifyMessageReceived(androidMsg, true);
                if(returnValue!=-1){
                    logDebug("Message " + returnValue + " modified!");
                    return;
                }
                addInBufferSending(androidMsg);
                if(!noMoreNoSentMessages){
                    logDebug("NOT noMoreNoSentMessages");
                }
            }
            else{
                if(!noMoreNoSentMessages){
                    logDebug("First message with NORMAL status");
                    noMoreNoSentMessages=true;
                    if(!bufferSending.isEmpty()){
                        bufferMessages.addAll(bufferSending);
                        bufferSending.clear();
                    }
                }

                AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);

                if (lastIdMsgSeen != -1) {
                    if(lastIdMsgSeen ==msg.getMsgId()){
                        logDebug("Last message seen received!");
                        lastSeenReceived=true;
                        positionToScroll = 0;
                        logDebug("positionToScroll: " + positionToScroll);
                    }
                }
                else{
                    logDebug("lastMessageSeen is -1");
                    lastSeenReceived=true;
                }

//                megaChatApi.setMessageSeen(idChat, msg.getMsgId());

                if(positionToScroll>=0){
                    positionToScroll++;
                    logDebug("positionToScroll:increase: " + positionToScroll);
                }
                bufferMessages.add(androidMsg);
                logDebug("Size of buffer: " + bufferMessages.size());
                logDebug("Size of messages: " + messages.size());
            }
        }
        else{
            logDebug("NULLmsg:REACH FINAL HISTORY:stateHistory " + stateHistory);
            if(!bufferSending.isEmpty()){
                bufferMessages.addAll(bufferSending);
                bufferSending.clear();
            }

            logDebug("numberToLoad: " + numberToLoad + " bufferSize: " + bufferMessages.size() + " messagesSize: " + messages.size());
            if((bufferMessages.size()+messages.size())>=numberToLoad){
                logDebug("Full history received");
                fullHistoryReceivedOnLoad();
                isLoadingHistory = false;
            }
            else if(((bufferMessages.size()+messages.size())<numberToLoad) && (stateHistory==MegaChatApi.SOURCE_ERROR)){
                logDebug("noMessagesLoaded&SOURCE_ERROR: wait to CHAT ONLINE connection");
                retryHistory = true;
            }
            else{
                logDebug("lessNumberReceived");
                if((stateHistory!=MegaChatApi.SOURCE_NONE)&&(stateHistory!=MegaChatApi.SOURCE_ERROR)){
                    logDebug("But more history exists --> loadMessages");
                    isLoadingHistory = true;
                    stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
                    logDebug("New state of history: " + stateHistory);
                    getMoreHistory = false;
                    if(stateHistory==MegaChatApi.SOURCE_NONE || stateHistory==MegaChatApi.SOURCE_ERROR){
                        fullHistoryReceivedOnLoad();
                        isLoadingHistory = false;
                    }
                }
                else{
                    fullHistoryReceivedOnLoad();
                    isLoadingHistory = false;
                }
            }
        }
        logDebug("END onMessageLoaded - messages.size=" + messages.size());
    }

    public void fullHistoryReceivedOnLoad(){
        logDebug("fullHistoryReceivedOnLoad");

        isOpeningChat = false;

        if(!bufferMessages.isEmpty()){
            logDebug("Buffer size: " + bufferMessages.size());
            loadBufferMessages();

            if(lastSeenReceived==false){
                logDebug("Last message seen NOT received");
                if(stateHistory!=MegaChatApi.SOURCE_NONE){
                    logDebug("F->loadMessages");
                    isLoadingHistory = true;
                    stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
                }
            }
            else{
                logDebug("Last message seen received");
                if(positionToScroll>0){
                    logDebug("Scroll to position: " + positionToScroll);
                    if(positionToScroll<messages.size()){
//                        mLayoutManager.scrollToPositionWithOffset(positionToScroll+1,scaleHeightPx(50, outMetrics));
                        //Find last message
                        int positionLastMessage = -1;
                        for(int i=messages.size()-1; i>=0;i--) {
                            AndroidMegaChatMessage androidMessage = messages.get(i);

                            if (!androidMessage.isUploading()) {

                                MegaChatMessage msg = androidMessage.getMessage();
                                if (msg.getMsgId() == lastIdMsgSeen) {
                                    positionLastMessage = i;
                                    break;
                                }
                            }
                        }

                        //Check if it has no my messages after
                        positionLastMessage = positionLastMessage + 1;
                        AndroidMegaChatMessage message = messages.get(positionLastMessage);

                        while(message.getMessage().getUserHandle()==megaChatApi.getMyUserHandle()){
                            lastIdMsgSeen = message.getMessage().getMsgId();
                            positionLastMessage = positionLastMessage + 1;
                            message = messages.get(positionLastMessage);
                        }

                        if(isTurn){
                            scrollToMessage(-1);

                        }else{
                            scrollToMessage(lastIdMsgSeen);
                        }

                    }
                    else{
                        logError("Error, the position to scroll is more than size of messages");
                    }
                }
            }

            setLastMessageSeen();
        }
        else{
            logWarning("Buffer empty");
        }

        logDebug("getMoreHistoryTRUE");
        getMoreHistory = true;

        //Load pending messages
        if(!pendingMessagesLoaded){
            pendingMessagesLoaded = true;
            loadPendingMessages();
            if(positionToScroll<=0){
                mLayoutManager.scrollToPosition(messages.size());
            }
        }

        chatRelativeLayout.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.GONE);
    }

    @Override
    public void onMessageReceived(MegaChatApiJava api, MegaChatMessage msg) {
        logDebug("CHAT CONNECTION STATE: " + api.getChatConnectionState(idChat));
        logDebug("STATUS: " + msg.getStatus());
        logDebug("TEMP ID: " + msg.getTempId());
        logDebug("FINAL ID: " + msg.getMsgId());
        logDebug("TIMESTAMP: " + msg.getTimestamp());
        logDebug("TYPE: " + msg.getType());

        if(msg.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT) {
            logDebug("TYPE_REVOKE_NODE_ATTACHMENT MESSAGE!!!!");
            return;
        }

        if(msg.getStatus()==MegaChatMessage.STATUS_SERVER_REJECTED){
            logDebug("STATUS_SERVER_REJECTED: " + msg.getStatus());
        }

        if(!msg.isManagementMessage()){
            logDebug("isNOTManagementMessage!");
            if(positionNewMessagesLayout!=-1){
                logDebug("Layout unread messages shown: " + generalUnreadCount);
                if(generalUnreadCount<0){
                    generalUnreadCount--;
                }
                else{
                    generalUnreadCount++;
                }

                if(adapter!=null){
                    adapter.notifyItemChanged(positionNewMessagesLayout);
                }
            }
        }
        else {
            int messageType = msg.getType();
            logDebug("Message type: " + messageType);

            switch (messageType) {
                case MegaChatMessage.TYPE_ALTER_PARTICIPANTS:{
                    if(msg.getUserHandle()==myUserHandle) {
                        logDebug("me alter participant");
                        hideNewMessagesLayout();
                    }
                    break;
                }
                case MegaChatMessage.TYPE_PRIV_CHANGE:{
                    if(msg.getUserHandle()==myUserHandle){
                        logDebug("I change a privilege");
                        hideNewMessagesLayout();
                    }
                    break;
                }
                case MegaChatMessage.TYPE_CHAT_TITLE:{
                    if(msg.getUserHandle()==myUserHandle) {
                        logDebug("I change the title");
                        hideNewMessagesLayout();
                    }
                    break;
                }
            }
        }

        if(setAsRead){
            markAsSeen(msg);
        }

        if(msg.getType()==MegaChatMessage.TYPE_CHAT_TITLE){
            logDebug("Change of chat title");
            String newTitle = msg.getContent();
            if(newTitle!=null){
                titleToolbar.setText(newTitle);
            }
        }
        else if(msg.getType()==MegaChatMessage.TYPE_TRUNCATE){
            invalidateOptionsMenu();
        }

        AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);
        appendMessagePosition(androidMsg);

        if(mLayoutManager.findLastCompletelyVisibleItemPosition()==messages.size()-1){
            logDebug("Do scroll to end");
            mLayoutManager.scrollToPosition(messages.size());
        }
        else{
            if(emojiKeyboard !=null){
                if((emojiKeyboard.getLetterKeyboardShown() || emojiKeyboard.getEmojiKeyboardShown())&&(messages.size()==1)){
                    mLayoutManager.scrollToPosition(messages.size());
                }
            }
            logDebug("DONT scroll to end");
            if(typeMessageJump !=  TYPE_MESSAGE_NEW_MESSAGE){
                messageJumpText.setText(getResources().getString(R.string.message_new_messages));
                typeMessageJump = TYPE_MESSAGE_NEW_MESSAGE;
            }

            if(messageJumpLayout.getVisibility() != View.VISIBLE){
                messageJumpText.setText(getResources().getString(R.string.message_new_messages));
                messageJumpLayout.setVisibility(View.VISIBLE);
            }
        }

        checkMegaLink(msg);

//        mLayoutManager.setStackFromEnd(true);
//        mLayoutManager.scrollToPosition(0);
    }
    public void sendToDownload(MegaNodeList nodelist){
        logDebug("sendToDownload");
        chatC.prepareForChatDownload(nodelist);
    }

    @Override
    public void onMessageUpdate(MegaChatApiJava api, MegaChatMessage msg) {
        logDebug("msgID "+ msg.getMsgId());

        int resultModify = -1;
        if(msg.isDeleted()){
            if(adapter!=null){
                adapter.stopPlaying(msg.getMsgId());
            }
            deleteMessage(msg, false);
            return;
        }

        AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);

        if(msg.hasChanged(MegaChatMessage.CHANGE_TYPE_ACCESS)){
            logDebug("Change access of the message");
            MegaNodeList nodeList = msg.getMegaNodeList();
            int revokedCount = 0;

            for(int i=0; i<nodeList.size(); i++){
                MegaNode node = nodeList.get(i);
                boolean revoked = megaChatApi.isRevoked(idChat, node.getHandle());
                if(revoked){
                    logDebug("The node is revoked: " + node.getHandle());
                    revokedCount++;
                }
                else{
                    logDebug("Node not revoked: " + node.getHandle());
                }
            }

            if(revokedCount==nodeList.size()){
                logDebug("All the attachments have been revoked");
                deleteMessage(msg, false);
            }
            else{
                logDebug("One attachment revoked, modify message");
                resultModify = modifyMessageReceived(androidMsg, false);

                if(resultModify==-1) {
                    logDebug("Modify result is -1");
                    int firstIndexShown = messages.get(0).getMessage().getMsgIndex();
                    logDebug("The first index is: " + firstIndexShown + " the index of the updated message: " + msg.getMsgIndex());
                    if(firstIndexShown<=msg.getMsgIndex()){
                        logDebug("The message should be in the list");
                        if(msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){

                            logDebug("Node attachment message not in list -> append");
                            AndroidMegaChatMessage msgToAppend = new AndroidMegaChatMessage(msg);
                            reinsertNodeAttachmentNoRevoked(msgToAppend);
                        }
                    }
                    else{
                        if(messages.size()<NUMBER_MESSAGES_BEFORE_LOAD){
                            logDebug("Show more message - add to the list");
                            if(msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){

                                logDebug("Node attachment message not in list -> append");
                                AndroidMegaChatMessage msgToAppend = new AndroidMegaChatMessage(msg);
                                reinsertNodeAttachmentNoRevoked(msgToAppend);
                            }
                        }
                    }

                }
            }
        }
        else if(msg.hasChanged(MegaChatMessage.CHANGE_TYPE_CONTENT)){
            logDebug("Change content of the message");

            if(msg.getType()==MegaChatMessage.TYPE_TRUNCATE){
                logDebug("TRUNCATE MESSAGE");
                clearHistory(androidMsg);
            }
            else{

                disableMultiselection();

                if(msg.isDeleted()){
                    logDebug("Message deleted!!");
                }

                checkMegaLink(msg);

                if (msg.getContainsMeta() != null && msg.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION){
                    logDebug("CONTAINS_META_GEOLOCATION");
                }

                resultModify = modifyMessageReceived(androidMsg, false);
                logDebug("resultModify: " + resultModify);
            }
        }
        else if(msg.hasChanged(MegaChatMessage.CHANGE_TYPE_STATUS)){

            int statusMsg = msg.getStatus();
            logDebug("Status change: "+ statusMsg + "T emporal id: "+ msg.getTempId() + " Final id: "+ msg.getMsgId());

            if(msg.getUserHandle()==megaChatApi.getMyUserHandle()){
                if((msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT)||(msg.getType()==MegaChatMessage.TYPE_VOICE_CLIP)){
                    logDebug("Modify my message and node attachment");

                    long idMsg =  dbH.findPendingMessageByIdTempKarere(msg.getTempId());
                    logDebug("The id of my pending message is: " + idMsg);
                    if(idMsg!=-1){
                        resultModify = modifyAttachmentReceived(androidMsg, idMsg);
                        if(resultModify==-1){
                            logWarning("Node attachment message not in list -> resultModify -1");
//                            AndroidMegaChatMessage msgToAppend = new AndroidMegaChatMessage(msg);
//                            appendMessagePosition(msgToAppend);
                        }
                        else{
                            dbH.removePendingMessageById(idMsg);
                        }
                        return;
                    }
                }
            }

            if(msg.getStatus()==MegaChatMessage.STATUS_SEEN){
                logDebug("STATUS_SEEN");
            }
            else if(msg.getStatus()==MegaChatMessage.STATUS_SERVER_RECEIVED){
                logDebug("STATUS_SERVER_RECEIVED");

                if(msg.getType()==MegaChatMessage.TYPE_NORMAL){
                    if(msg.getUserHandle()==megaChatApi.getMyUserHandle()){
                        checkMegaLink(msg);
                    }
                }

                resultModify = modifyMessageReceived(androidMsg, true);
                logDebug("resultModify: " + resultModify);
            }
            else if(msg.getStatus()==MegaChatMessage.STATUS_SERVER_REJECTED){
                logDebug("STATUS_SERVER_REJECTED: " + msg.getStatus());
                deleteMessage(msg, true);
            }
            else{
                logDebug("Status: " + msg.getStatus());
                logDebug("Timestamp: " + msg.getTimestamp());

                resultModify = modifyMessageReceived(androidMsg, false);
                logDebug("resultModify: " + resultModify);
            }
        }
    }

    private void disableMultiselection(){
        if(adapter == null || !adapter.isMultipleSelect()) return;
        clearSelections();
        hideMultipleSelect();
    }

    @Override
    public void onHistoryReloaded(MegaChatApiJava api, MegaChatRoom chat) {
        logDebug("onHistoryReloaded");
        cleanBuffers();
        invalidateOptionsMenu();
        logDebug("Load new history");

        long unread = chatRoom.getUnreadCount();
        //                        stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
        if (unread == 0) {
            lastIdMsgSeen = -1;
            generalUnreadCount = -1;
            lastSeenReceived = true;
            logDebug("loadMessages unread is 0");
        } else {
            lastIdMsgSeen = megaChatApi.getLastMessageSeenId(idChat);
            generalUnreadCount = unread;

            if (lastIdMsgSeen != -1) {
                logDebug("Id of last message seen: " + lastIdMsgSeen);
            } else {
                logError("Error the last message seen shouldn't be NULL");
            }

            lastSeenReceived = false;
        }
    }

    public void deleteMessage(MegaChatMessage msg, boolean rejected){
        logDebug("deleteMessage");
        int indexToChange = -1;

        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        // Iterate in reverse.
        while(itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();
            logDebug("Index: " + itr.nextIndex());

            if(!messageToCheck.isUploading()){
                if(rejected){
                    if (messageToCheck.getMessage().getTempId() == msg.getTempId()) {
                        indexToChange = itr.nextIndex();
                        break;
                    }
                }
                else{
                    if (messageToCheck.getMessage().getMsgId() == msg.getMsgId()) {
                        indexToChange = itr.nextIndex();
                        break;
                    }
                }
            }
        }

        if(indexToChange!=-1) {
            messages.remove(indexToChange);
            logDebug("Removed index: " + indexToChange + " positionNewMessagesLayout: " + positionNewMessagesLayout + " messages size: " + messages.size());
            if (positionNewMessagesLayout <= indexToChange) {
                if (generalUnreadCount == 1 || generalUnreadCount == -1) {
                    logDebug("Reset generalUnread:Position where new messages layout is show: " + positionNewMessagesLayout);
                    generalUnreadCount = 0;
                    lastIdMsgSeen = -1;
                } else {
                    logDebug("Decrease generalUnread:Position where new messages layout is show: " + positionNewMessagesLayout);
                    generalUnreadCount--;
                }
                adapter.notifyItemChanged(positionNewMessagesLayout);
            }

            if(!messages.isEmpty()){
                //Update infoToShow of the next message also
                if (indexToChange == 0) {
                    messages.get(indexToChange).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                    //Check if there is more messages and update the following one
                    if(messages.size()>1){
                        adjustInfoToShow(indexToChange+1);
                        setShowAvatar(indexToChange+1);
                    }
                }
                else{
                    //Not first element
                    if (indexToChange == messages.size()) {
                        logDebug("The last message removed, do not check more messages");
                        setShowAvatar(indexToChange - 1);
                    } else {
                        adjustInfoToShow(indexToChange);
                        setShowAvatar(indexToChange);
                        setShowAvatar(indexToChange - 1);
                    }
                }
            }

            adapter.removeMessage(indexToChange + 1, messages);
            disableMultiselection();
        } else {
            logWarning("index to change not found");
        }
    }

    public int modifyAttachmentReceived(AndroidMegaChatMessage msg, long idPendMsg){
        logDebug("ID: " + msg.getMessage().getMsgId() + ", tempID: " + msg.getMessage().getTempId() + ", Status: " + msg.getMessage().getStatus());
        int indexToChange = -1;
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        // Iterate in reverse.
        while(itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();

            if(messageToCheck.getPendingMessage()!=null){
                logDebug("Pending ID: " + messageToCheck.getPendingMessage().getId() + ", other: "+ idPendMsg);
                logDebug("Pending ID: " + messageToCheck.getPendingMessage().getId() + ", other: "+ idPendMsg);
                if(messageToCheck.getPendingMessage().getId()==idPendMsg){
                    indexToChange = itr.nextIndex();
                    logDebug("Found index to change: " + indexToChange);
                    break;
                }
            }
        }

        if(indexToChange!=-1){

            logDebug("INDEX change, need to reorder");
            messages.remove(indexToChange);
            logDebug("Removed index: " + indexToChange);
            logDebug("Messages size: " + messages.size());
            adapter.removeMessage(indexToChange+1, messages);

            int scrollToP = appendMessagePosition(msg);
            if(scrollToP!=-1){
                if(msg.getMessage().getStatus()==MegaChatMessage.STATUS_SERVER_RECEIVED){
                    logDebug("Need to scroll to position: " + indexToChange);
                    final int indexToScroll = scrollToP+1;
                    mLayoutManager.scrollToPositionWithOffset(indexToScroll,scaleHeightPx(20, outMetrics));

                }
            }
        }
        else{
            logError("Error, id pending message message not found!!");
        }
        logDebug("Index modified: " + indexToChange);
        return indexToChange;
    }


    public int modifyMessageReceived(AndroidMegaChatMessage msg, boolean checkTempId){
        logDebug("Msg ID: " + msg.getMessage().getMsgId());
        logDebug("Msg TEMP ID: " + msg.getMessage().getTempId());
        logDebug("Msg status: " + msg.getMessage().getStatus());
        int indexToChange = -1;
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        // Iterate in reverse.
        while(itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();
            logDebug("Index: " + itr.nextIndex());

            if(!messageToCheck.isUploading()){
                logDebug("Checking with Msg ID: " + messageToCheck.getMessage().getMsgId());
                logDebug("Checking with Msg TEMP ID: " + messageToCheck.getMessage().getTempId());

                if(checkTempId){
                    logDebug("Check temporal IDS");
                    if (messageToCheck.getMessage().getTempId() == msg.getMessage().getTempId()) {
                        logDebug("Modify received messafe with idTemp");
                        indexToChange = itr.nextIndex();
                        break;
                    }
                }
                else{
                    if (messageToCheck.getMessage().getMsgId() == msg.getMessage().getMsgId()) {
                        logDebug("modifyMessageReceived");
                        indexToChange = itr.nextIndex();
                        break;
                    }
                }
            }
            else{
                logDebug("This message is uploading");
            }
        }

        logDebug("Index to change = " + indexToChange);
        if(indexToChange==-1) return indexToChange;

        AndroidMegaChatMessage messageToUpdate = messages.get(indexToChange);
        if(messageToUpdate.getMessage().getMsgIndex()==msg.getMessage().getMsgIndex()){
            logDebug("The internal index not change");

            if(msg.getMessage().getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL){
                logDebug("Modified a MANUAl SENDING msg");
                //Check the message to change is not the last one
                int lastI = messages.size()-1;
                if(indexToChange<lastI){
                    //Check if there is already any MANUAL_SENDING in the queue
                    AndroidMegaChatMessage previousMessage = messages.get(lastI);
                    if(previousMessage.isUploading()){
                        logDebug("Previous message is uploading");
                    }
                    else{
                        if(previousMessage.getMessage().getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL){
                            logDebug("More MANUAL SENDING in queue");
                            logDebug("Removed index: " + indexToChange);
                            messages.remove(indexToChange);
                            appendMessageAnotherMS(msg);
                            adapter.notifyDataSetChanged();
                            return indexToChange;
                        }
                    }
                }
            }

            logDebug("Modified message keep going");
            messages.set(indexToChange, msg);

            //Update infoToShow also
            if (indexToChange == 0) {
                messages.get(indexToChange).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                messages.get(indexToChange).setShowAvatar(true);
            }
            else{
                //Not first element
                adjustInfoToShow(indexToChange);
                setShowAvatar(indexToChange);

                //Create adapter
                if (adapter == null) {
                    adapter = new MegaChatLollipopAdapter(this, chatRoom, messages,messagesPlaying,  listView);
                    adapter.setHasStableIds(true);
                    listView.setAdapter(adapter);
                } else {
                    adapter.modifyMessage(messages, indexToChange+1);
                }
            }
        }
        else{
            logDebug("INDEX change, need to reorder");
            messages.remove(indexToChange);
            logDebug("Removed index: " + indexToChange);
            logDebug("Messages size: " + messages.size());
            adapter.removeMessage(indexToChange+1, messages);
            int scrollToP = appendMessagePosition(msg);
            if(scrollToP!=-1){
                if(msg.getMessage().getStatus()==MegaChatMessage.STATUS_SERVER_RECEIVED){
                    mLayoutManager.scrollToPosition(scrollToP+1);
                    //mLayoutManager.scrollToPositionWithOffset(scrollToP, scaleHeightPx(20, outMetrics));
                }
            }
            logDebug("Messages size: " + messages.size());
        }

        return indexToChange;
    }

    public void modifyLocationReceived(AndroidMegaChatMessage editedMsg, boolean hasTempId){
        logDebug("Edited Msg ID: " + editedMsg.getMessage().getMsgId() + ", Old Msg ID: " + messageToEdit.getMsgId());
        logDebug("Edited Msg TEMP ID: " + editedMsg.getMessage().getTempId() + ", Old Msg TEMP ID: " + messageToEdit.getTempId());
        logDebug("Edited Msg status: " + editedMsg.getMessage().getStatus() + ", Old Msg status: " + messageToEdit.getStatus());
        int indexToChange = -1;
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        boolean editedMsgHasTempId = false;
        if (editedMsg.getMessage().getTempId() != -1) {
            editedMsgHasTempId = true;
        }

        // Iterate in reverse.
        while(itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();
            logDebug("Index: " + itr.nextIndex());

            if(!messageToCheck.isUploading()){
                logDebug("Checking with Msg ID: " + messageToCheck.getMessage().getMsgId() + " and Msg TEMP ID: " + messageToCheck.getMessage().getTempId());
                if (hasTempId) {
                    if (editedMsgHasTempId && messageToCheck.getMessage().getTempId() == editedMsg.getMessage().getTempId()) {
                        indexToChange = itr.nextIndex();
                        break;
                    }
                    else if (!editedMsgHasTempId && messageToCheck.getMessage().getTempId() == editedMsg.getMessage().getMsgId()){
                        indexToChange = itr.nextIndex();
                        break;
                    }
                }
                else {
                    if (editedMsgHasTempId && messageToCheck.getMessage().getMsgId() == editedMsg.getMessage().getTempId()) {
                        indexToChange = itr.nextIndex();
                        break;
                    }
                    else if (!editedMsgHasTempId && messageToCheck.getMessage().getMsgId() == editedMsg.getMessage().getMsgId()){
                        indexToChange = itr.nextIndex();
                        break;
                    }
                }
            }
            else{
                logDebug("This message is uploading");
            }
        }

        logDebug("Index to change = " + indexToChange);
        if(indexToChange!=-1){

            AndroidMegaChatMessage messageToUpdate = messages.get(indexToChange);
            if(messageToUpdate.getMessage().getMsgIndex()==editedMsg.getMessage().getMsgIndex()){
                logDebug("The internal index not change");

                if(editedMsg.getMessage().getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL){
                    logDebug("Modified a MANUAl SENDING msg");
                    //Check the message to change is not the last one
                    int lastI = messages.size()-1;
                    if(indexToChange<lastI){
                        //Check if there is already any MANUAL_SENDING in the queue
                        AndroidMegaChatMessage previousMessage = messages.get(lastI);
                        if(previousMessage.isUploading()){
                            logDebug("Previous message is uploading");
                        }
                        else if(previousMessage.getMessage().getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL){
                            logDebug("More MANUAL SENDING in queue");
                            logDebug("Removed index: " + indexToChange);
                            messages.remove(indexToChange);
                            appendMessageAnotherMS(editedMsg);
                            adapter.notifyDataSetChanged();
                        }
                    }
                }

                logDebug("Modified message keep going");
                messages.set(indexToChange, editedMsg);

                //Update infoToShow also
                if (indexToChange == 0) {
                    messages.get(indexToChange).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                    messages.get(indexToChange).setShowAvatar(true);
                }
                else{
                    //Not first element
                    adjustInfoToShow(indexToChange);
                    setShowAvatar(indexToChange);

                    //Create adapter
                    if (adapter == null) {
                        adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, messagesPlaying, listView);
                        adapter.setHasStableIds(true);
                        listView.setAdapter(adapter);
                    } else {
                        adapter.modifyMessage(messages, indexToChange+1);
                    }
                }
            }
            else{
                logDebug("INDEX change, need to reorder");
                messages.remove(indexToChange);
                logDebug("Removed index: " + indexToChange);
                logDebug("Messages size: " + messages.size());
                adapter.removeMessage(indexToChange+1, messages);
                int scrollToP = appendMessagePosition(editedMsg);
                if(scrollToP!=-1 && editedMsg.getMessage().getStatus()==MegaChatMessage.STATUS_SERVER_RECEIVED){
                    mLayoutManager.scrollToPosition(scrollToP+1);
                }
                logDebug("Messages size: " + messages.size());
            }
        }
        else{
            logError("Error, id temp message not found!! indexToChange == -1");
        }
    }

    public void loadBufferMessages(){
        logDebug("loadBufferMessages");
        ListIterator<AndroidMegaChatMessage> itr = bufferMessages.listIterator();
        while (itr.hasNext()) {
            int currentIndex = itr.nextIndex();
            AndroidMegaChatMessage messageToShow = itr.next();
            loadMessage(messageToShow);
        }

        //Create adapter
        if(adapter==null){
            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, messagesPlaying, listView);
            adapter.setHasStableIds(true);
            listView.setLayoutManager(mLayoutManager);
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
        }
        else{
            adapter.loadPreviousMessages(messages, bufferMessages.size());

            logDebug("addMessage: " + messages.size());
        }

        logDebug("AFTER updateMessagesLoaded: " + messages.size() + " messages in list");

        bufferMessages.clear();
    }

    public void clearHistory(AndroidMegaChatMessage androidMsg){
        logDebug("clearHistory");

        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        int indexToChange=-1;
        // Iterate in reverse.
        while(itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();

            if(!messageToCheck.isUploading()){
                if(messageToCheck.getMessage().getStatus()!=MegaChatMessage.STATUS_SENDING){

                    indexToChange = itr.nextIndex();
                    logDebug("Found index of last sent and confirmed message: " + indexToChange);
                    break;
                }
            }
        }

//        indexToChange = 2;
        if(indexToChange != messages.size()-1){
            logDebug("Clear history of confirmed messages: " + indexToChange);

            List<AndroidMegaChatMessage> messagesCopy = new ArrayList<>(messages);
            messages.clear();
            messages.add(androidMsg);

            for(int i = indexToChange+1; i<messagesCopy.size();i++){
                messages.add(messagesCopy.get(i));
            }
        }
        else{
            logDebug("Clear all messages");
            messages.clear();
            messages.add(androidMsg);
        }

        if(messages.size()==1){
            androidMsg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
        }
        else{
            for(int i=0; i<messages.size();i++){
                adjustInfoToShow(i);
            }
        }

        adapter.setMessages(messages);
    }

    public void loadPendingMessages(){
        logDebug("loadPendingMessages");
        ArrayList<AndroidMegaChatMessage> pendMsgs = dbH.findPendingMessagesNotSent(idChat);
//        dbH.findPendingMessagesBySent(1);
        logDebug("Number of pending: " + pendMsgs.size());

        for(int i=0;i<pendMsgs.size();i++){
            AndroidMegaChatMessage pMsg = pendMsgs.get(i);
            if(pMsg!=null && pMsg.getPendingMessage()!=null){
                if(pMsg.getPendingMessage().getState()==PendingMessageSingle.STATE_PREPARING){
                    if(pMsg.getPendingMessage().getTransferTag()!=-1){
                        logDebug("STATE_PREPARING: Transfer tag: " + pMsg.getPendingMessage().getTransferTag());
                        if(megaApi!=null) {
                            MegaTransfer t = megaApi.getTransferByTag(pMsg.getPendingMessage().getTransferTag());
                            if(t!=null){
                                if(t.getState()==MegaTransfer.STATE_COMPLETED){
                                    dbH.updatePendingMessageOnTransferFinish(pMsg.getPendingMessage().getId(), "-1", PendingMessageSingle.STATE_SENT);
                                }
                                else if(t.getState()==MegaTransfer.STATE_CANCELLED){
                                    dbH.updatePendingMessageOnTransferFinish(pMsg.getPendingMessage().getId(), "-1", PendingMessageSingle.STATE_SENT);
                                }
                                else if(t.getState()==MegaTransfer.STATE_FAILED){
                                    dbH.updatePendingMessageOnTransferFinish(pMsg.getPendingMessage().getId(), "-1", PendingMessageSingle.STATE_ERROR_UPLOADING);
                                    pMsg.getPendingMessage().setState(PendingMessageSingle.STATE_ERROR_UPLOADING);
                                    appendMessagePosition(pMsg);
                                }
                                else{
                                    logDebug("STATE_PREPARING: Found transfer in progress for the message");
                                    appendMessagePosition(pMsg);
                                }
                            }
                            else{
                                logDebug("STATE_PREPARING: Mark message as error uploading - no transfer in progress");
                                dbH.updatePendingMessageOnTransferFinish(pMsg.getPendingMessage().getId(), "-1", PendingMessageSingle.STATE_ERROR_UPLOADING);
                                pMsg.getPendingMessage().setState(PendingMessageSingle.STATE_ERROR_UPLOADING);
                                appendMessagePosition(pMsg);
                            }
                        }
                    }
                }
                else if(pMsg.getPendingMessage().getState()==PendingMessageSingle.STATE_PREPARING_FROM_EXPLORER){
                    logDebug("STATE_PREPARING_FROM_EXPLORER: Convert to STATE_PREPARING");
                    dbH.updatePendingMessageOnTransferFinish(pMsg.getPendingMessage().getId(), "-1", PendingMessageSingle.STATE_PREPARING);
                    pMsg.getPendingMessage().setState(PendingMessageSingle.STATE_PREPARING);
                    appendMessagePosition(pMsg);
                }
                else if(pMsg.getPendingMessage().getState()==PendingMessageSingle.STATE_UPLOADING){
                    if(pMsg.getPendingMessage().getTransferTag()!=-1){
                        logDebug("STATE_UPLOADING: Transfer tag: " + pMsg.getPendingMessage().getTransferTag());
                        if(megaApi!=null){
                            MegaTransfer t = megaApi.getTransferByTag(pMsg.getPendingMessage().getTransferTag());
                            if(t!=null){
                                if(t.getState()==MegaTransfer.STATE_COMPLETED){
                                    dbH.updatePendingMessageOnTransferFinish(pMsg.getPendingMessage().getId(), "-1", PendingMessageSingle.STATE_SENT);
                                }
                                else if(t.getState()==MegaTransfer.STATE_CANCELLED){
                                    dbH.updatePendingMessageOnTransferFinish(pMsg.getPendingMessage().getId(), "-1", PendingMessageSingle.STATE_SENT);
                                }
                                else if(t.getState()==MegaTransfer.STATE_FAILED){
                                    dbH.updatePendingMessageOnTransferFinish(pMsg.getPendingMessage().getId(), "-1", PendingMessageSingle.STATE_ERROR_UPLOADING);
                                    pMsg.getPendingMessage().setState(PendingMessageSingle.STATE_ERROR_UPLOADING);
                                    appendMessagePosition(pMsg);
                                }
                                else{
                                    logDebug("STATE_UPLOADING: Found transfer in progress for the message");
                                    appendMessagePosition(pMsg);
                                }
                            }
                            else{
                                logDebug("STATE_UPLOADING: Mark message as error uploading - no transfer in progress");
                                dbH.updatePendingMessageOnTransferFinish(pMsg.getPendingMessage().getId(), "-1", PendingMessageSingle.STATE_ERROR_UPLOADING);
                                pMsg.getPendingMessage().setState(PendingMessageSingle.STATE_ERROR_UPLOADING);
                                appendMessagePosition(pMsg);
                            }
                        }
                    }
                }
                else{
                    appendMessagePosition(pMsg);
                }
            }
            else{
                logWarning("Null pending messages");
            }
        }
    }

    public void loadMessage(AndroidMegaChatMessage messageToShow){
        logDebug("loadMessage");
        messageToShow.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
        messages.add(0,messageToShow);

        if(messages.size()>1) {
            adjustInfoToShow(1);
        }

        setShowAvatar(0);

        if(adapter.isMultipleSelect()){
            adapter.updateSelectionOnScroll();
        }
    }

    public void appendMessageAnotherMS(AndroidMegaChatMessage msg){
        logDebug("appendMessageAnotherMS");
        messages.add(msg);
        int lastIndex = messages.size()-1;

        if(lastIndex==0){
            messages.get(lastIndex).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
        }
        else{
            adjustInfoToShow(lastIndex);
        }

        //Create adapter
        if(adapter==null){
            logDebug("Create adapter");
            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, messagesPlaying, listView);
            adapter.setHasStableIds(true);
            listView.setLayoutManager(mLayoutManager);
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
        }
        else{
            logDebug("Update adapter with last index: " + lastIndex);
            if(lastIndex==0){
                logDebug("Arrives the first message of the chat");
                adapter.setMessages(messages);
            }
            else{
                adapter.addMessage(messages, lastIndex+1);
            }
        }
    }

    public int reinsertNodeAttachmentNoRevoked(AndroidMegaChatMessage msg){
        logDebug("reinsertNodeAttachmentNoRevoked");
        int lastIndex = messages.size()-1;
        logDebug("Last index: " + lastIndex);
        if(messages.size()==-1){
            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
            messages.add(msg);
        }
        else {
            logDebug("Finding where to append the message");
            while(messages.get(lastIndex).getMessage().getMsgIndex()>msg.getMessage().getMsgIndex()){
                logDebug("Last index: " + lastIndex);
                lastIndex--;
                if (lastIndex == -1) {
                    break;
                }
            }
            logDebug("Last index: " + lastIndex);
            lastIndex++;
            logDebug("Append in position: " + lastIndex);
            messages.add(lastIndex, msg);
            adjustInfoToShow(lastIndex);
            int nextIndex = lastIndex+1;
            if(nextIndex<=messages.size()-1){
                adjustInfoToShow(nextIndex);
            }
            int previousIndex = lastIndex-1;
            if(previousIndex>=0){
                adjustInfoToShow(previousIndex);
            }
        }

        //Create adapter
        if(adapter==null){
            logDebug("Create adapter");
            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages,messagesPlaying,  listView);
            adapter.setHasStableIds(true);
            listView.setLayoutManager(mLayoutManager);
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
        }
        else{
            logDebug("Update adapter with last index: " + lastIndex);
            if(lastIndex<0){
                logDebug("Arrives the first message of the chat");
                adapter.setMessages(messages);
            }
            else{
                adapter.addMessage(messages, lastIndex+1);
            }
        }
        return lastIndex;
    }

    public int appendMessagePosition(AndroidMegaChatMessage msg){
        logDebug("appendMessagePosition: " + messages.size() + " messages");

        int lastIndex = messages.size()-1;
        if(messages.size()==0){
            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
            msg.setShowAvatar(true);
            messages.add(msg);
        }else{
            logDebug("Finding where to append the message");

            if(msg.isUploading()){
                lastIndex++;
                logDebug("The message is uploading add to index: " + lastIndex + "with state: " + msg.getPendingMessage().getState());
            }else{
                logDebug("Status of message: " + msg.getMessage().getStatus());
                if(lastIndex>=0) {
                    while (messages.get(lastIndex).isUploading()) {
                        logDebug("One less index is uploading");
                        lastIndex--;
                        if(lastIndex==-1){
                            break;
                        }
                    }
                }
                if(lastIndex>=0) {
                    while (messages.get(lastIndex).getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL) {
                        logDebug("One less index is MANUAL SENDING");
                        lastIndex--;
                        if(lastIndex==-1){
                            break;
                        }
                    }
                }
                if(lastIndex>=0) {
                    if (msg.getMessage().getStatus() == MegaChatMessage.STATUS_SERVER_RECEIVED || msg.getMessage().getStatus() == MegaChatMessage.STATUS_NOT_SEEN) {
                        while (messages.get(lastIndex).getMessage().getStatus() == MegaChatMessage.STATUS_SENDING) {
                            logDebug("One less index");
                            lastIndex--;
                            if(lastIndex==-1){
                                break;
                            }
                        }
                    }
                }

                lastIndex++;
                logDebug("Append in position: " + lastIndex);
            }
            if(lastIndex>=0){
                messages.add(lastIndex, msg);
                adjustInfoToShow(lastIndex);
                msg.setShowAvatar(true);
                if(!messages.get(lastIndex).isUploading()){
                    int nextIndex = lastIndex+1;
                    if(nextIndex<messages.size()){
                        if(messages.get(nextIndex)!=null) {
                            if(messages.get(nextIndex).isUploading()){
                                adjustInfoToShow(nextIndex);
                            }
                        }
                    }
                }
                if(lastIndex>0){
                    setShowAvatar(lastIndex-1);
                }
            }
        }

        //Create adapter
        if(adapter==null){
            logDebug("Create adapter");
            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, messagesPlaying, listView);
            adapter.setHasStableIds(true);
            listView.setLayoutManager(mLayoutManager);
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
        }else{
            logDebug("Update adapter with last index: " + lastIndex);
            if(lastIndex<0){
                logDebug("Arrives the first message of the chat");
                adapter.setMessages(messages);
            }
            else{
                adapter.addMessage(messages, lastIndex+1);
                adapter.notifyItemChanged(lastIndex);
            }
        }
        return lastIndex;
    }

    public int adjustInfoToShow(int index) {
        logDebug("Index: " + index);

        AndroidMegaChatMessage msg = messages.get(index);

        long userHandleToCompare = -1;
        long previousUserHandleToCompare = -1;

        if(msg.isUploading()){
            userHandleToCompare = myUserHandle;
        }
        else{

            if ((msg.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (msg.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                userHandleToCompare = msg.getMessage().getHandleOfAction();
            } else {
                userHandleToCompare = msg.getMessage().getUserHandle();
            }
        }

        if(index==0){
            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
        }
        else{
            AndroidMegaChatMessage previousMessage = messages.get(index-1);

            if(previousMessage.isUploading()){

                logDebug("The previous message is uploading");
                if(msg.isUploading()){
                    logDebug("The message is also uploading");
                    if (compareDate(msg.getPendingMessage().getUploadTimestamp(), previousMessage.getPendingMessage().getUploadTimestamp()) == 0) {
                        //Same date
                        if (compareTime(msg.getPendingMessage().getUploadTimestamp(), previousMessage.getPendingMessage().getUploadTimestamp()) == 0) {
                            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING);
                        } else {
                            //Different minute
                            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                        }
                    } else {
                        //Different date
                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                    }
                }
                else{
                    if (compareDate(msg.getMessage().getTimestamp(), previousMessage.getPendingMessage().getUploadTimestamp()) == 0) {
                        //Same date
                        if (compareTime(msg.getMessage().getTimestamp(), previousMessage.getPendingMessage().getUploadTimestamp()) == 0) {
                            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING);
                        } else {
                            //Different minute
                            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                        }
                    } else {
                        //Different date
                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                    }
                }
            }
            else{
                logDebug("The previous message is NOT uploading");

                if (userHandleToCompare == myUserHandle) {
                    logDebug("MY message!!");
//                log("MY message!!: "+messageToShow.getContent());
                    if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                        previousUserHandleToCompare = previousMessage.getMessage().getHandleOfAction();
                    } else {
                        previousUserHandleToCompare = previousMessage.getMessage().getUserHandle();
                    }

//                    log("previous message: "+previousMessage.getContent());
                    if (previousUserHandleToCompare == myUserHandle) {
                        logDebug("Last message and previous is mine");
                        //The last two messages are mine
                        if(msg.isUploading()){
                            logDebug("The msg to append is uploading");
                            if (compareDate(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                //Same date
                                if (compareTime(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING);
                                } else {
                                    //Different minute
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }
                        else{
                            if (compareDate(msg, previousMessage) == 0) {
                                //Same date
                                if (compareTime(msg, previousMessage) == 0) {
                                    if ((msg.getMessage().isManagementMessage())) {
                                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                    }
                                    else{
                                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING);
                                    }
                                } else {
                                    //Different minute
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }

                    } else {
                        //The last message is mine, the previous not
                        logDebug("Last message is mine, NOT previous");
                        if(msg.isUploading()) {
                            logDebug("The msg to append is uploading");
                            if (compareDate(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }
                        else{
                            if (compareDate(msg, previousMessage) == 0) {
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }
                    }

                } else {
                    logDebug("NOT MY message!! - CONTACT");
//                    log("previous message: "+previousMessage.getContent());

                    if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                        previousUserHandleToCompare = previousMessage.getMessage().getHandleOfAction();
                    } else {
                        previousUserHandleToCompare = previousMessage.getMessage().getUserHandle();
                    }

                    if (previousUserHandleToCompare == userHandleToCompare) {
                        //The last message is also a contact's message
                        if(msg.isUploading()) {
                            if (compareDate(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                //Same date
                                if (compareTime(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                    logDebug("Add with show nothing - same userHandle");
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING);

                                } else {
                                    //Different minute
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }
                        else{

                            if (compareDate(msg, previousMessage) == 0) {
                                //Same date
                                if (compareTime(msg, previousMessage) == 0) {
                                    if ((msg.getMessage().isManagementMessage())) {
                                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                    }
                                    else{
                                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING);
                                    }
                                } else {
                                    //Different minute
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }

                    } else {
                        //The last message is from contact, the previous not
                        logDebug("Different user handle");
                        if(msg.isUploading()) {
                            if (compareDate(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                //Same date
                                if (compareTime(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                    if(previousUserHandleToCompare==myUserHandle){
                                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                    }
                                    else{
                                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING);
                                    }

                                } else {
                                    //Different minute
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                }

                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }
                        else{
                            if (compareDate(msg, previousMessage) == 0) {
                                if (compareTime(msg, previousMessage) == 0) {
                                    if(previousUserHandleToCompare==myUserHandle){
                                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                    }
                                    else{
                                        if ((msg.getMessage().isManagementMessage())) {
                                            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                        }
                                        else{
                                            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                        }
                                    }

                                } else {
                                    //Different minute
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                }

                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }
                    }
                }

            }
        }
        return msg.getInfoToShow();
    }

    public void setShowAvatar(int index){
        logDebug("Index: " + index);

        AndroidMegaChatMessage msg = messages.get(index);

        long userHandleToCompare = -1;
        long previousUserHandleToCompare = -1;

        if(msg.isUploading()){
            msg.setShowAvatar(false);
            return;
        }

        if (userHandleToCompare == myUserHandle) {
            logDebug("MY message!!");
        }else{
            logDebug("Contact message");
            if ((msg.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (msg.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                userHandleToCompare = msg.getMessage().getHandleOfAction();
            } else {
                userHandleToCompare = msg.getMessage().getUserHandle();
            }
            logDebug("userHandleTocompare: " + userHandleToCompare);
            AndroidMegaChatMessage previousMessage = null;
            if(messages.size()-1 > index){
                previousMessage = messages.get(index + 1);
                if(previousMessage==null){
                    msg.setShowAvatar(true);
                    logWarning("Previous message is null");
                    return;
                }
                if(previousMessage.isUploading()){
                    msg.setShowAvatar(true);
                    logDebug("Previous is uploading");
                    return;
                }

                if((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                    previousUserHandleToCompare = previousMessage.getMessage().getHandleOfAction();
                }else{
                    previousUserHandleToCompare = previousMessage.getMessage().getUserHandle();
                }

                logDebug("previousUserHandleToCompare: " + previousUserHandleToCompare);

                if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_CALL_ENDED) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_CALL_STARTED) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_CHAT_TITLE)) {
                    msg.setShowAvatar(true);
                    logDebug("Set: " + true);
                } else {
                    if (previousUserHandleToCompare == userHandleToCompare) {
                        msg.setShowAvatar(false);
                        logDebug("Set: " + false);
                    }else{
                        msg.setShowAvatar(true);
                        logDebug("Set: " + true);
                    }
                }
            }
            else{
                logWarning("No previous message");
                msg.setShowAvatar(true);
            }
        }
    }

    public boolean isGroup(){
        return chatRoom.isGroup();
    }

    public void showMsgNotSentPanel(AndroidMegaChatMessage message, int position){
        logDebug("Position: " + position);

        selectedPosition = position;

        if (message == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedMessageId = message.getMessage().getRowId();
        logDebug("Temporal id of MS message: "+message.getMessage().getTempId());
        bottomSheetDialogFragment = new MessageNotSentBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showNodeAttachmentBottomSheet(AndroidMegaChatMessage message, int position){
        logDebug("showNodeAttachmentBottomSheet: "+position);
        selectedPosition = position;

        if (message == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedMessageId = message.getMessage().getMsgId();

        bottomSheetDialogFragment = new NodeAttachmentBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showSendAttachmentBottomSheet(){
        logDebug("showSendAttachmentBottomSheet");

        if (isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        bottomSheetDialogFragment = new SendAttachmentChatBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showUploadingAttachmentBottomSheet(AndroidMegaChatMessage message, int position){
        logDebug("showUploadingAttachmentBottomSheet: "+position);
        selectedPosition = position;

        if (message == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedMessageId = message.getPendingMessage().getId();

        bottomSheetDialogFragment = new PendingMessageBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showContactAttachmentBottomSheet(AndroidMegaChatMessage message, int position){
        logDebug("showContactAttachmentBottomSheet: "+position);
        selectedPosition = position;

        if (message == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedMessageId = message.getMessage().getMsgId();
        bottomSheetDialogFragment = new ContactAttachmentBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void removeMsgNotSent(){
        logDebug("Selected position: " + selectedPosition);
        messages.remove(selectedPosition);
        adapter.removeMessage(selectedPosition, messages);
    }

    public void removePendingMsg(long id){
        logDebug("Selected message ID: " + selectedMessageId);

        PendingMessageSingle pMsg = dbH.findPendingMessageById(id);
        if(pMsg!=null && pMsg.getState()==PendingMessageSingle.STATE_UPLOADING) {
            if (pMsg.getTransferTag() != -1) {
                logDebug("Transfer tag: " + pMsg.getTransferTag());
                if (megaApi != null) {
                    megaApi.cancelTransferByTag(pMsg.getTransferTag(), this);
                }
            }
        }

        if(pMsg!=null && pMsg.getState()!=PendingMessageSingle.STATE_SENT){
            try{
                dbH.removePendingMessageById(id);
                messages.remove(selectedPosition);
                adapter.removeMessage(selectedPosition, messages);
            }
            catch (IndexOutOfBoundsException e){
                logError("EXCEPTION", e);
            }
        }
        else{
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_message_already_sent), -1);
        }
    }

    public void showSnackbar(int type, String s, long idChat){
        showSnackbar(type, fragmentContainer, s, idChat);
    }

    public void removeProgressDialog(){
        if (statusDialog != null) {
            try {
                statusDialog.dismiss();
            } catch (Exception ex) {}
        }
    }

    public void startConversation(long handle){
        logDebug("Handle: " + handle);
        MegaChatRoom chat = megaChatApi.getChatRoomByUser(handle);
        MegaChatPeerList peers = MegaChatPeerList.createInstance();
        if(chat==null){
            logDebug("No chat, create it!");
            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD);
            megaChatApi.createChat(false, peers, this);
        }
        else{
            logDebug("There is already a chat, open it!");
            Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
            intentOpenChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intentOpenChat.putExtra("CHAT_ID", chat.getChatId());
            this.startActivity(intentOpenChat);
            finish();
        }
    }

    public void startGroupConversation(ArrayList<Long> userHandles){
        logDebug("startGroupConversation");

        MegaChatPeerList peers = MegaChatPeerList.createInstance();

        for(int i=0;i<userHandles.size();i++){
            long handle = userHandles.get(i);
            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD);
        }
        megaChatApi.createChat(true, peers, this);
    }

    public void setMessages(ArrayList<AndroidMegaChatMessage> messages){
        if(dialog!=null){
            dialog.dismiss();
        }

        this.messages = messages;
        //Create adapter
        if (adapter == null) {
            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, messagesPlaying, listView);
            adapter.setHasStableIds(true);
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
        } else {
            adapter.setMessages(messages);
        }
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        logDebug("onRequestFinish: " + request.getRequestString() + " " + request.getType());

        if(request.getType() == MegaChatRequest.TYPE_TRUNCATE_HISTORY){
            logDebug("Truncate history request finish!!!");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                logDebug("Ok. Clear history done");
                showSnackbar(SNACKBAR_TYPE, getString(R.string.clear_history_success), -1);
                hideMessageJump();
            }else{
                logError("Error clearing history: " + e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getString(R.string.clear_history_error), -1);
            }
        } else if (request.getType() == MegaChatRequest.TYPE_HANG_CHAT_CALL) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("TYPE_HANG_CHAT_CALL finished with success  ---> answerChatCall chatid = " + idChat);
                if (megaChatApi == null) return;
                MegaChatCall call = megaChatApi.getChatCall(idChat);
                if (call == null) return;
                if (call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                    ((MegaApplication) getApplication()).setSpeakerStatus(chatRoom.getChatId(), false);
                    megaChatApi.answerChatCall(idChat, false, this);

                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                    megaChatApi.startChatCall(idChat, false, this);
                }
            } else {
                logError("ERROR WHEN TYPE_HANG_CHAT_CALL e.getErrorCode(): " + e.getErrorString());
            }

        } else if (request.getType() == MegaChatRequest.TYPE_START_CHAT_CALL) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("TYPE_START_CHAT_CALL finished with success");
                //getFlag - Returns true if it is a video-audio call or false for audio call
            } else {
                logError("ERROR WHEN TYPE_START_CHAT_CALL e.getErrorCode(): " + e.getErrorString());
                if (e.getErrorCode() == MegaChatError.ERROR_TOOMANY) {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.call_error_too_many_participants), -1);
                } else {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.call_error), -1);
                }
            }

        } else if (request.getType() == MegaChatRequest.TYPE_ANSWER_CHAT_CALL) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("TYPE_ANSWER_CHAT_CALL finished with success");
                //getFlag - Returns true if it is a video-audio call or false for audio call
            } else {
                logError("ERROR WHEN TYPE_ANSWER_CHAT_CALL e.getErrorCode(): " + e.getErrorString());
                if (e.getErrorCode() == MegaChatError.ERROR_TOOMANY) {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.call_error_too_many_participants), -1);
                } else {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.call_error), -1);
                }
            }

        }else if(request.getType() == MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM){
            logDebug("Remove participant: " + request.getUserHandle() + " my user: " + megaChatApi.getMyUserHandle());

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                logDebug("Participant removed OK");
                invalidateOptionsMenu();

            }
            else{
                logError("ERROR WHEN TYPE_REMOVE_FROM_CHATROOM " + e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getString(R.string.remove_participant_error), -1);
            }

        }else if(request.getType() == MegaChatRequest.TYPE_INVITE_TO_CHATROOM){
            logDebug("Request type: " + MegaChatRequest.TYPE_INVITE_TO_CHATROOM);
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                showSnackbar(SNACKBAR_TYPE, getString(R.string.add_participant_success), -1);
            }
            else{
                if(e.getErrorCode() == MegaChatError.ERROR_EXIST){
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.add_participant_error_already_exists), -1);
                }
                else{
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.add_participant_error), -1);
                }
            }

        }
        else if(request.getType() == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE){
            removeProgressDialog();

            disableMultiselection();

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                logDebug("File sent correctly");
                MegaNodeList nodeList = request.getMegaNodeList();

                for(int i = 0; i<nodeList.size();i++){
                    logDebug("Node handle: " + nodeList.get(i).getHandle());
                }
                AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(request.getMegaChatMessage());
                sendMessageToUI(androidMsgSent);

            }else{
                logError("File NOT sent: " + e.getErrorCode() + "___" + e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_attaching_node_from_cloud), -1);
            }

        }else if(request.getType() == MegaChatRequest.TYPE_REVOKE_NODE_MESSAGE){
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                logDebug("Node revoked correctly, msg id: " + request.getMegaChatMessage().getMsgId());
            }
            else{
                logError("NOT revoked correctly");
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_revoking_node), -1);
            }

        }else if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM){
            logDebug("Create chat request finish!!!");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){

                logDebug("Open new chat");
                Intent intent = new Intent(this, ChatActivityLollipop.class);
                intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
                intent.putExtra("CHAT_ID", request.getChatHandle());
                this.startActivity(intent);
                finish();
            }
            else{
                logError("ERROR WHEN CREATING CHAT " + e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getString(R.string.create_chat_error), -1);
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_LOAD_PREVIEW){
            if(e.getErrorCode()==MegaChatError.ERROR_OK || e.getErrorCode()==MegaChatError.ERROR_EXIST){
                if (idChat != -1 && megaChatApi.getChatRoom(idChat) != null) {
                    logDebug("Close previous chat");
                    megaChatApi.closeChatRoom(idChat, this);
                }
                idChat = request.getChatHandle();
                MegaApplication.setOpenChatId(idChat);
                showChat(null);

                supportInvalidateOptionsMenu();
                if (e.getErrorCode() == MegaChatError.ERROR_EXIST) {
                    if (megaChatApi.getChatRoom(idChat).isActive()) {
                        logWarning("ERROR: You are already a participant of the chat link or are trying to open it again");
                    } else {
                        showConfirmationRejoinChat(request.getUserHandle());
                    }
                }
            }
            else {

                String text;
                if(e.getErrorCode()==MegaChatError.ERROR_NOENT){
                    text = getString(R.string.invalid_chat_link);
                }
                else{
                    showSnackbar(MESSAGE_SNACKBAR_TYPE, getString(R.string.error_general_nodes), -1);
                    text = getString(R.string.error_chat_link);
                }

                emptyScreen(text);
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {

                if (request.getUserHandle() != -1) {
                    //Rejoin option
                    showChat(null);
                } else {
                    //Join
                    setChatSubtitle();
                    setPreviewersView();
                    supportInvalidateOptionsMenu();
                }
            } else {
                logError("ERROR WHEN JOINING CHAT " + e.getErrorCode() + " " + e.getErrorString());
                showSnackbar(MESSAGE_SNACKBAR_TYPE, getString(R.string.error_general_nodes), -1);
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_LAST_GREEN){
            logDebug("TYPE_LAST_GREEN requested");

        }else if(request.getType() == MegaChatRequest.TYPE_ARCHIVE_CHATROOM){
            long chatHandle = request.getChatHandle();
            chatRoom = megaChatApi.getChatRoom(chatHandle);
            String chatTitle = chatRoom.getTitle();

            if(chatTitle==null){
                chatTitle = "";
            }
            else if(!chatTitle.isEmpty() && chatTitle.length()>60){
                chatTitle = chatTitle.substring(0,59)+"...";
            }

            if(!chatTitle.isEmpty() && chatRoom.isGroup() && !chatRoom.hasCustomTitle()){
                chatTitle = "\""+chatTitle+"\"";
            }

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                if(request.getFlag()){
                    logDebug("Chat archived");
                    sendBroadcastChatArchived(chatTitle);
                }
                else{
                    logDebug("Chat unarchived");
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.success_unarchive_chat, chatTitle), -1);
                }

            }else{
                if(request.getFlag()){
                    logError("ERROR WHEN ARCHIVING CHAT " + e.getErrorString());
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_archive_chat, chatTitle), -1);
                }else{
                    logError("ERROR WHEN UNARCHIVING CHAT " + e.getErrorString());
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_unarchive_chat, chatTitle), -1);
                }
            }

            supportInvalidateOptionsMenu();
            setChatSubtitle();

            if(!chatRoom.isArchived()){
                requestLastGreen(INITIAL_PRESENCE_STATUS);
            }
        }
        else if (request.getType() == MegaChatRequest.TYPE_CHAT_LINK_HANDLE) {
            if(request.getFlag() && request.getNumRetry()==0){
                logDebug("Removing chat link");
                if(e.getErrorCode()==MegaChatError.ERROR_OK){
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.chat_link_deleted), -1);
                }
                else{
                    if (e.getErrorCode() == MegaChatError.ERROR_ARGS) {
                        logError("The chatroom isn't grupal or public");
                    }
                    else if (e.getErrorCode()==MegaChatError.ERROR_NOENT){
                        logError("The chatroom doesn't exist or the chatid is invalid");
                    }
                    else if(e.getErrorCode()==MegaChatError.ERROR_ACCESS){
                        logError("The chatroom doesn't have a topic or the caller isn't an operator");
                    }
                    else{
                        logError("Error TYPE_CHAT_LINK_HANDLE " + e.getErrorCode());
                    }
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error) + ": " + e.getErrorString(), -1);
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        logWarning("onRequestTemporaryError");
    }

    @Override
    protected void onStop() {
        logDebug("onStop()");
        try{
            if(textChat!=null){
                String written = textChat.getText().toString();
                if(written!=null){
                    dbH.setWrittenTextItem(Long.toString(idChat), textChat.getText().toString());
                }
            }
            else{
                logWarning("textChat is NULL");
            }
        }catch (Exception e){
            logError("Written message not stored on DB", e);
        }
        super.onStop();
    }

    private void cleanBuffers(){
        if(!bufferMessages.isEmpty()){
            bufferMessages.clear();
        }
        if(!messages.isEmpty()){
            messages.clear();
        }
    }

    @Override
    protected void onDestroy() {
        logDebug("onDestroy()");

        cleanBuffers();
        if (handlerEmojiKeyboard != null){
            handlerEmojiKeyboard.removeCallbacksAndMessages(null);
        }
        if (handlerKeyboard != null) {
            handlerKeyboard.removeCallbacksAndMessages(null);
        }

        if (megaChatApi != null && idChat != -1) {
            megaChatApi.closeChatRoom(idChat, this);
            MegaApplication.setClosedChat(true);
            megaChatApi.removeChatListener(this);
            megaChatApi.removeChatCallListener(this);

            if (chatRoom != null && chatRoom.isPreview()) {
                megaChatApi.closeChatPreview(idChat);
            }
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (handlerReceive != null) {
            handlerReceive.removeCallbacksAndMessages(null);
        }
        if (handlerSend != null) {
            handlerSend.removeCallbacksAndMessages(null);
        }

        hideCallInProgressLayout(null);

        if(myAudioRecorder!=null){
            myAudioRecorder.reset();
            myAudioRecorder.release();
            myAudioRecorder = null;
            outputFileVoiceNotes = null;
            setRecordingNow(false);
        }
        if(adapter!=null) {
            adapter.destroyVoiceElemnts();
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(dialogConnectReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(voiceclipDownloadedReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(chatArchivedReceiver);

        if(megaApi != null) {
            megaApi.removeRequestListener(this);
        }


        super.onDestroy();
    }

    public void closeChat(boolean shouldLogout){
        logDebug("closeChat");
        if(megaChatApi==null || idChat == -1) return;
        if(chatRoom!=null && chatRoom.isPreview()){
            megaChatApi.closeChatPreview(idChat);
            if(chatC.isInAnonymousMode() && shouldLogout){
                megaChatApi.logout();
            }
        }
        megaChatApi.closeChatRoom(idChat, this);
        MegaApplication.setClosedChat(true);
        megaChatApi.removeChatListener(this);
        megaChatApi.removeChatCallListener(this);

    }

    @Override
    protected void onNewIntent(Intent intent){
        logDebug("onNewIntent");
        hideKeyboard();
        if (intent != null){
            if (intent.getAction() != null){
                if (intent.getAction().equals(ACTION_CLEAR_CHAT)){
                    logDebug("Intent to Clear history");
                    showConfirmationClearChat(chatRoom);
                }
                else if(intent.getAction().equals(ACTION_UPDATE_ATTACHMENT)){
                    logDebug("Intent to update an attachment with error");

                    long idPendMsg = intent.getLongExtra("ID_MSG", -1);
                    if(idPendMsg!=-1){
                        int indexToChange = -1;
                        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

                        // Iterate in reverse.
                        while(itr.hasPrevious()) {
                            AndroidMegaChatMessage messageToCheck = itr.previous();

                            if(messageToCheck.isUploading()){
                                if(messageToCheck.getPendingMessage().getId()==idPendMsg){
                                    indexToChange = itr.nextIndex();
                                    logDebug("Found index to change: " + indexToChange);
                                    break;
                                }
                            }
                        }

                        if(indexToChange!=-1){
                            logDebug("Index modified: " + indexToChange);

                            PendingMessageSingle pendingMsg = null;
                            if(idPendMsg!=-1){
                                pendingMsg = dbH.findPendingMessageById(idPendMsg);

                                if(pendingMsg!=null){
                                    messages.get(indexToChange).setPendingMessage(pendingMsg);
                                    adapter.modifyMessage(messages, indexToChange+1);
                                }
                            }
                        }
                        else{
                            logError("Error, id pending message message not found!!");
                        }
                    }
                    else{
                        logError("Error. The idPendMsg is -1");
                    }

                    int isOverquota = intent.getIntExtra("IS_OVERQUOTA", 0);
                    if(isOverquota==1){
                        showOverquotaAlert(false);
                    }
                    else if (isOverquota==2){
                        showOverquotaAlert(true);
                    }

                    return;
                }else{
                    long newidChat = intent.getLongExtra("CHAT_ID", -1);
                    if(intent.getAction().equals(ACTION_CHAT_SHOW_MESSAGES) || intent.getAction().equals(ACTION_OPEN_CHAT_LINK) || idChat != newidChat) {
                        cleanBuffers();
                    }
                    if (messagesPlaying != null && !messagesPlaying.isEmpty()) {
                        for (MessageVoiceClip m : messagesPlaying) {
                            m.getMediaPlayer().release();
                            m.setMediaPlayer(null);
                        }
                        messagesPlaying.clear();
                    }

                    adapter.notifyDataSetChanged();
                    closeChat(false);
                    MegaApplication.setOpenChatId(-1);
                    initAfterIntent(intent, null);
                }

            }
        }
        super.onNewIntent(intent);
        setIntent(intent);
        return;
    }

    public String getPeerFullName(long userHandle){
        return chatRoom.getPeerFullnameByHandle(userHandle);
    }

    public MegaChatRoom getChatRoom() {
        return chatRoom;
    }

    public void setChatRoom(MegaChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public void revoke(){
        logDebug("revoke");
        megaChatApi.revokeAttachmentMessage(idChat, selectedMessageId);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        removeProgressDialog();

        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT){
            logDebug("MegaRequest.TYPE_INVITE_CONTACT finished: " + request.getNumber());

            if(request.getNumber()== MegaContactRequest.INVITE_ACTION_REMIND){
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_invitation_resent), -1);
            }
            else{
                if (e.getErrorCode() == MegaError.API_OK){
                    logDebug("OK INVITE CONTACT: " + request.getEmail());
                    if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD)
                    {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_request_sent, request.getEmail()), -1);
                    }
                }
                else{
                    logError("Code: " + e.getErrorString());
                    if(e.getErrorCode()==MegaError.API_EEXIST)
                    {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_already_invited, request.getEmail()), -1);
                    }
                    else if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD && e.getErrorCode()==MegaError.API_EARGS)
                    {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.error_own_email_as_contact), -1);
                    }
                    else{
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error), -1);
                    }
                    logError("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
                }
            }
        }
        else if(request.getType() == MegaRequest.TYPE_COPY){
            if (e.getErrorCode() != MegaError.API_OK) {

                logDebug("e.getErrorCode() != MegaError.API_OK");

                if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
                    logWarning("OVERQUOTA ERROR: " + e.getErrorCode());
                    Intent intent = new Intent(this, ManagerActivityLollipop.class);
                    intent.setAction(ACTION_OVERQUOTA_STORAGE);
                    startActivity(intent);
                    finish();
                }
                else if(e.getErrorCode()==MegaError.API_EGOINGOVERQUOTA){
                    logWarning("OVERQUOTA ERROR: " + e.getErrorCode());
                    Intent intent = new Intent(this, ManagerActivityLollipop.class);
                    intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
                    startActivity(intent);
                    finish();
                }
                else
                {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.import_success_error), -1);
                }

            }else{
                showSnackbar(SNACKBAR_TYPE, getString(R.string.import_success_message), -1);
            }
        }
        else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFER){
            if (e.getErrorCode() != MegaError.API_OK) {
                logError("Error TYPE_CANCEL_TRANSFER: " + e.getErrorCode());
            }
            else{
                logDebug("Chat upload cancelled");
            }
        }
        else if (request.getType() == MegaRequest.TYPE_SET_ATTR_USER){
            if(request.getParamType()==MegaApiJava.USER_ATTR_GEOLOCATION){
                if(e.getErrorCode() == MegaError.API_OK){
                    logDebug("Attribute USER_ATTR_GEOLOCATION enabled");
                    MegaApplication.setEnabledGeoLocation(true);
                    getLocationPermission();
                }
                else{
                    logDebug("Attribute USER_ATTR_GEOLOCATION disabled");
                    MegaApplication.setEnabledGeoLocation(false);
                }
            }
        }
        else if (request.getType() == MegaRequest.TYPE_CREATE_FOLDER && CHAT_FOLDER.equals(request.getName())) {
            if (e.getErrorCode() == MegaError.API_OK) {
                logDebug("Create My Chat Files, copy reserved nodes");
                handleStoredData();
            } else {
                logError("Not create My Chat Files" + e.getErrorCode() + " " + e.getErrorString());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        logDebug("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putLong("idChat", idChat);
        outState.putLong("selectedMessageId", selectedMessageId);
        outState.putInt("selectedPosition", selectedPosition);
        outState.putInt("typeMessageJump",typeMessageJump);

        if(messageJumpLayout.getVisibility() == View.VISIBLE){
            visibilityMessageJump = true;
        }else{
            visibilityMessageJump = false;
        }
        outState.putBoolean("visibilityMessageJump",visibilityMessageJump);
        outState.putLong("lastMessageSeen", lastIdMsgSeen);
        outState.putLong("generalUnreadCount", generalUnreadCount);
        outState.putBoolean("isHideJump",isHideJump);
        outState.putString("mOutputFilePath",mOutputFilePath);
        outState.putBoolean("isShareLinkDialogDismissed", isShareLinkDialogDismissed);
        if(adapter == null) return;
        MessageVoiceClip messageVoiceClip = adapter.getVoiceClipPlaying();
        if (messageVoiceClip != null) {
            outState.putBoolean(PLAYING, true);
            outState.putLong(ID_VOICE_CLIP_PLAYING, messageVoiceClip.getIdMessage());
            outState.putLong(MESSAGE_HANDLE_PLAYING, messageVoiceClip.getMessageHandle());
            outState.putLong(USER_HANDLE_PLAYING, messageVoiceClip.getUserHandle());
            outState.putInt(PROGRESS_PLAYING, messageVoiceClip.getProgress());
        } else {
            outState.putBoolean(PLAYING, false);

        }
        outState.putBoolean("isLocationDialogShown", isLocationDialogShown);
//        outState.putInt("position_imageDrag", position_imageDrag);
//        outState.putSerializable("holder_imageDrag", holder_imageDrag);
    }

    public void askSizeConfirmationBeforeChatDownload(String parentPath, ArrayList<MegaNode> nodeList, long size){
        logDebug("askSizeConfirmationBeforeChatDownload");

        final String parentPathC = parentPath;
        final ArrayList<MegaNode> nodeListC = nodeList;
        final long sizeC = size;
        final ChatController chatC = new ChatController(this);

        android.support.v7.app.AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        }
        else{
            builder = new AlertDialog.Builder(this);
        }
        LinearLayout confirmationLayout = new LinearLayout(this);
        confirmationLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(10, outMetrics), scaleWidthPx(17, outMetrics), 0);

        final CheckBox dontShowAgain =new CheckBox(this);
        dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
        dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        confirmationLayout.addView(dontShowAgain, params);

        builder.setView(confirmationLayout);

//				builder.setTitle(getString(R.string.confirmation_required));

        builder.setMessage(getString(R.string.alert_larger_file, getSizeString(sizeC)));
        builder.setPositiveButton(getString(R.string.general_save_to_device),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(dontShowAgain.isChecked()){
                            dbH.setAttrAskSizeDownload("false");
                        }
                        chatC.download(parentPathC, nodeListC);
                    }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if(dontShowAgain.isChecked()){
                    dbH.setAttrAskSizeDownload("false");
                }
            }
        });

        downloadConfirmationDialog = builder.create();
        downloadConfirmationDialog.show();
    }

    /*
	 * Handle processed upload intent
	 */
    public void onIntentProcessed(List<ShareInfo> infos) {
        logDebug("onIntentProcessedLollipop");

        if (infos == null) {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.upload_can_not_open), -1);
        }
        else {
            logDebug("Launch chat upload with files " + infos.size());
            for (ShareInfo info : infos) {
                Intent intent = new Intent(this, ChatUploadService.class);

                PendingMessageSingle pMsgSingle = new PendingMessageSingle();
                pMsgSingle.setChatId(idChat);
                long timestamp = System.currentTimeMillis()/1000;
                pMsgSingle.setUploadTimestamp(timestamp);

                String fingerprint = megaApi.getFingerprint(info.getFileAbsolutePath());

                pMsgSingle.setFilePath(info.getFileAbsolutePath());
                pMsgSingle.setName(info.getTitle());
                pMsgSingle.setFingerprint(fingerprint);

                long idMessage = dbH.addPendingMessage(pMsgSingle);
                pMsgSingle.setId(idMessage);

                if(idMessage!=-1){
                    intent.putExtra(ChatUploadService.EXTRA_ID_PEND_MSG, idMessage);

                    logDebug("Size of the file: " + info.getSize());

                    AndroidMegaChatMessage newNodeAttachmentMsg = new AndroidMegaChatMessage(pMsgSingle, true);
                    sendMessageToUI(newNodeAttachmentMsg);

                    intent.putExtra(ChatUploadService.EXTRA_CHAT_ID, idChat);

                    startService(intent);
                }
                else{
                    logError("Error when adding pending msg to the database");
                }

                removeProgressDialog();
            }
        }
    }

    public void openChatAfterForward(long chatHandle, String text){
        logDebug("openChatAfterForward");

        removeProgressDialog();

        if(chatHandle==idChat){
            logDebug("Chat already opened");

            disableMultiselection();

            if(text!=null){
                showSnackbar(SNACKBAR_TYPE, text, -1);
            }
        }
        else{
            if(chatHandle!=-1){
                logDebug("Open chat to forward messages");

                Intent intentOpenChat = new Intent(this, ManagerActivityLollipop.class);
                intentOpenChat.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intentOpenChat.setAction(ACTION_CHAT_NOTIFICATION_MESSAGE);
                intentOpenChat.putExtra("CHAT_ID", chatHandle);
                if(text!=null){
                    intentOpenChat.putExtra("showSnackbar", text);
                }
                startActivity(intentOpenChat);
                closeChat(true);
                finish();
            }
            else{
                disableMultiselection();
                if(text!=null){
                    showSnackbar(SNACKBAR_TYPE, text, -1);
                }
            }
        }
    }

    public void markAsSeen(MegaChatMessage msg){
        logDebug("markAsSeen");
        if(activityVisible){
            if(msg.getStatus()!=MegaChatMessage.STATUS_SEEN) {
                megaChatApi.setMessageSeen(chatRoom.getChatId(), msg.getMsgId());
            }
        }
    }


   @Override
    protected void onResume(){
       logDebug("onResume");
        super.onResume();

        if(idChat!=-1 && chatRoom!=null) {
            setNodeAttachmentVisible();

            MegaApplication.setShowPinScreen(true);
            MegaApplication.setOpenChatId(idChat);

            supportInvalidateOptionsMenu();

            int chatConnection = megaChatApi.getChatConnectionState(idChat);
            logDebug("Chat connection (" + idChat+ ") is: " + chatConnection);
            if(chatConnection==MegaChatApi.CHAT_CONNECTION_ONLINE) {
                setAsRead = true;
                if(!chatRoom.isGroup()) {
                    requestLastGreen(INITIAL_PRESENCE_STATUS);
                }
            }
            else{
                setAsRead=false;
            }
            setChatSubtitle();
            if(emojiKeyboard!=null){
                emojiKeyboard.hideBothKeyboard(this);
            }
            //Update last seen position if different and there is unread messages
            //If the chat is being opened do not update, onLoad will do that

            //!isLoadingMessages
            if(!isOpeningChat) {
                logDebug("Chat is NOT loading history");
                if(lastSeenReceived == true && messages != null){

                    long unreadCount = chatRoom.getUnreadCount();
                    if (unreadCount != 0) {
                        lastIdMsgSeen = megaChatApi.getLastMessageSeenId(idChat);

                        //Find last message
                        int positionLastMessage = -1;
                        for(int i=messages.size()-1; i>=0;i--) {
                            AndroidMegaChatMessage androidMessage = messages.get(i);

                            if (!androidMessage.isUploading()) {
                                MegaChatMessage msg = androidMessage.getMessage();
                                if (msg.getMsgId() == lastIdMsgSeen) {
                                    positionLastMessage = i;
                                    break;
                                }
                            }
                        }

                        if(positionLastMessage==-1){
                            scrollToMessage(-1);

                        }
                        else{
                            //Check if it has no my messages after

                            if(positionLastMessage >= messages.size()-1){
                                logDebug("Nothing after, do not increment position");
                            }
                            else{
                                positionLastMessage = positionLastMessage + 1;
                            }

                            AndroidMegaChatMessage message = messages.get(positionLastMessage);
                            logDebug("Position lastMessage found: " + positionLastMessage + " messages.size: " + messages.size());

                            while(message.getMessage().getUserHandle()==megaChatApi.getMyUserHandle()){
                                lastIdMsgSeen = message.getMessage().getMsgId();
                                positionLastMessage = positionLastMessage + 1;
                                message = messages.get(positionLastMessage);
                            }

                            generalUnreadCount = unreadCount;

                            scrollToMessage(lastIdMsgSeen);
                        }
                    }
                    else{
                        if(generalUnreadCount!=0){
                            scrollToMessage(-1);
                        }
                    }
                }
                setLastMessageSeen();
            }
            else{
                logDebug("openingChat:doNotUpdateLastMessageSeen");
            }

            activityVisible = true;
            showCallLayout(megaChatApi.getChatCall(idChat));
            if(aB != null && aB.getTitle() != null){
                titleToolbar.setText(adjustForLargeFont(titleToolbar.getText().toString()));
            }
        }
    }

    public void scrollToMessage(long lastId){
        for(int i=messages.size()-1; i>=0;i--) {
            AndroidMegaChatMessage androidMessage = messages.get(i);

            if (!androidMessage.isUploading()) {
                MegaChatMessage msg = androidMessage.getMessage();
                if (msg.getMsgId() == lastId) {
                    logDebug("Scroll to position: " + i);
                    mLayoutManager.scrollToPositionWithOffset(i+1,scaleHeightPx(30, outMetrics));
                    break;
                }
            }
        }

    }

    public void setLastMessageSeen(){
        logDebug("setLastMessageSeen");

        if(messages!=null){
            if(!messages.isEmpty()){
                AndroidMegaChatMessage lastMessage = messages.get(messages.size()-1);
                int index = messages.size()-1;
                if((lastMessage!=null)&&(lastMessage.getMessage()!=null)){
                    if (!lastMessage.isUploading()) {
                        while (lastMessage.getMessage().getUserHandle() == megaChatApi.getMyUserHandle()) {
                            index--;
                            if (index == -1) {
                                break;
                            }
                            lastMessage = messages.get(index);
                        }

                        if (lastMessage.getMessage() != null) {
                            boolean resultMarkAsSeen = megaChatApi.setMessageSeen(idChat, lastMessage.getMessage().getMsgId());
                            logDebug("Result setMessageSeen: " + resultMarkAsSeen);
                        }

                    } else {
                        while (lastMessage.isUploading() == true) {
                            index--;
                            if (index == -1) {
                                break;
                            }
                            lastMessage = messages.get(index);
                        }
                        if((lastMessage!=null)&&(lastMessage.getMessage()!=null)){

                            while (lastMessage.getMessage().getUserHandle() == megaChatApi.getMyUserHandle()) {
                                index--;
                                if (index == -1) {
                                    break;
                                }
                                lastMessage = messages.get(index);
                            }

                            if (lastMessage.getMessage() != null) {
                                boolean resultMarkAsSeen = megaChatApi.setMessageSeen(idChat, lastMessage.getMessage().getMsgId());
                                logDebug("Result setMessageSeen: " + resultMarkAsSeen);
                            }
                        }
                    }
                }
                else{
                    logError("lastMessageNUll");
                }
            }
        }
    }

    @Override
    protected void onPause(){
        logDebug("onPause");
        super.onPause();
        hideKeyboard();

        activityVisible = false;
        MegaApplication.setOpenChatId(-1);
    }

    @Override
    public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {
        if(item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT)) {
            updateNavigationToolbarIcon();
        }
    }

    public void updateNavigationToolbarIcon(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            if(!chatC.isInAnonymousMode()){
                int numberUnread = megaChatApi.getUnreadChats();

                if(numberUnread==0){
                    aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
                }
                else{

                    badgeDrawable.setProgress(1.0f);

                    if(numberUnread>9){
                        badgeDrawable.setText("9+");
                    }
                    else{
                        badgeDrawable.setText(numberUnread+"");
                    }

                    aB.setHomeAsUpIndicator(badgeDrawable);
                }
            }
            else{
                aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
            }
        }
        else{
            aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        }
    }

    @Override
    public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {

    }

    @Override
    public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {

    }

    @Override
    public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userHandle, int status, boolean inProgress) {
        logDebug("status: " + status + ", inProgress: " + inProgress);
        setChatSubtitle();
        requestLastGreen(status);
    }

    @Override
    public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {
        logDebug("Chat ID: "+ chatid + ". New State: " + newState);
        supportInvalidateOptionsMenu();

        if (idChat == chatid) {
            if (newState == MegaChatApi.CHAT_CONNECTION_ONLINE) {
                logDebug("Chat is now ONLINE");
                setAsRead = true;
                setLastMessageSeen();

                if (stateHistory == MegaChatApi.SOURCE_ERROR && retryHistory) {
                    logWarning("SOURCE_ERROR:call to load history again");
                    retryHistory = false;
                    loadHistory();
                }

            } else {
                setAsRead = false;
            }

            setChatSubtitle();
        }
    }

    @Override
    public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {
        logDebug("userhandle: " + userhandle + ", lastGreen: " + lastGreen);
        if(!chatRoom.isGroup() && userhandle == chatRoom.getPeerHandle(0)){
            logDebug("Update last green");
            minutesLastGreen = lastGreen;

            int state = megaChatApi.getUserOnlineStatus(chatRoom.getPeerHandle(0));

            if(state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID){
                String formattedDate = lastGreenDate(this, lastGreen);

                setLastGreen(formattedDate);

                logDebug("Date last green: " + formattedDate);
            }
        }
    }

    public void takePicture(){
        logDebug("takePicture");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            Uri photoURI;
            if(photoFile != null){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    photoURI = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", photoFile);
                }
                else{
                    photoURI = Uri.fromFile(photoFile);
                }
                mOutputFilePath = photoFile.getAbsolutePath();
                if(mOutputFilePath!=null){
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(intent, TAKE_PHOTO_CODE);
                }
            }
        }
    }


    public void uploadPictureOrVoiceClip(String path){
        if(path == null) return;

        File selfie;
        if(isVoiceClip(path)) {
            selfie = buildVoiceClipFile(this, outputFileName);
        }else{
            selfie = new File(path);
        }

        if(!isVoiceClip(selfie.getAbsolutePath()) && !MimeTypeList.typeForName(selfie.getAbsolutePath()).isImage()) return;
        if(isVoiceClip(selfie.getAbsolutePath()) && !isFileAvailable(selfie)) return;

        Intent intent = new Intent(this, ChatUploadService.class);
        PendingMessageSingle pMsgSingle = new PendingMessageSingle();
        pMsgSingle.setChatId(idChat);
        if(isVoiceClip(selfie.getAbsolutePath())) pMsgSingle.setType(TYPE_VOICE_CLIP);

        long timestamp = System.currentTimeMillis()/1000;
        pMsgSingle.setUploadTimestamp(timestamp);

        String fingerprint = megaApi.getFingerprint(selfie.getAbsolutePath());
        pMsgSingle.setFilePath(selfie.getAbsolutePath());
        pMsgSingle.setName(selfie.getName());
        pMsgSingle.setFingerprint(fingerprint);
        long idMessage = dbH.addPendingMessage(pMsgSingle);
        pMsgSingle.setId(idMessage);

        if(idMessage == -1) return;

        logDebug("idMessage = " + idMessage);
        intent.putExtra(ChatUploadService.EXTRA_ID_PEND_MSG, idMessage);
        if(!isLoadingHistory){
            logDebug("sendMessageToUI");
            AndroidMegaChatMessage newNodeAttachmentMsg = new AndroidMegaChatMessage(pMsgSingle, true);
            sendMessageToUI(newNodeAttachmentMsg);
        }
        intent.putExtra(ChatUploadService.EXTRA_CHAT_ID, idChat);
        if(isVoiceClip(selfie.getAbsolutePath())) {
            intent.putExtra(EXTRA_TRANSFER_TYPE, EXTRA_VOICE_CLIP);
        }

        startService(intent);
    }


    private void showOverquotaAlert(boolean prewarning){
        logDebug("prewarning: " + prewarning);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.overquota_alert_title));

        if(prewarning){
            builder.setMessage(getString(R.string.pre_overquota_alert_text));
        }
        else{
            builder.setMessage(getString(R.string.overquota_alert_text));
        }

        if(chatAlertDialog ==null){

            builder.setPositiveButton(getString(R.string.my_account_upgrade_pro), new android.content.DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showUpgradeAccount();
                }
            });
            builder.setNegativeButton(getString(R.string.general_cancel), new android.content.DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    chatAlertDialog =null;
                }
            });

            chatAlertDialog = builder.create();
            chatAlertDialog.setCanceledOnTouchOutside(false);
        }

        chatAlertDialog.show();
    }

    public void showUpgradeAccount(){
        logDebug("showUpgradeAccount");
        Intent upgradeIntent = new Intent(this, ManagerActivityLollipop.class);
        upgradeIntent.setAction(ACTION_SHOW_UPGRADE_ACCOUNT);
        startActivity(upgradeIntent);
    }

    public void showJumpMessage(){
        if((!isHideJump)&&(typeMessageJump!=TYPE_MESSAGE_NEW_MESSAGE)){
            typeMessageJump = TYPE_MESSAGE_JUMP_TO_LEAST;
            messageJumpText.setText(getResources().getString(R.string.message_jump_latest));
            messageJumpLayout.setVisibility(View.VISIBLE);
        }
    }

    private void showCallInProgressLayout(String text, boolean chrono, MegaChatCall call) {
        if (callInProgressText != null) {
            callInProgressText.setText(text);
        }
        activateChrono(chrono, callInProgressChrono, call);

        if (callInProgressLayout != null && callInProgressLayout.getVisibility() != View.VISIBLE) {
            callInProgressLayout.setAlpha(1);
            callInProgressLayout.setVisibility(View.VISIBLE);
            callInProgressLayout.setOnClickListener(this);
        }
    }

    private void hideCallInProgressLayout(MegaChatCall call) {
        invalidateOptionsMenu();
        activateChrono(false, callInProgressChrono, call);
        activateChrono(false, subtitleChronoCall, call);

        if (callInProgressLayout != null && callInProgressLayout.getVisibility() != View.GONE) {
            callInProgressLayout.setVisibility(View.GONE);
            callInProgressLayout.setOnClickListener(null);
            setSubtitleVisibility();
        }
    }

    @Override
    public void onChatCallUpdate(MegaChatApiJava api, MegaChatCall call) {

        if((call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) && (call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS)){
            cancelRecording();
        }

        if (call.getChatid() == idChat) {
            if (call.hasChanged(MegaChatCall.CHANGE_TYPE_STATUS)) {

                int callStatus = call.getStatus();
                logDebug("Call status: " + callStatusToString(callStatus));
                switch (callStatus) {
                    case MegaChatCall.CALL_STATUS_RING_IN:
                    case MegaChatCall.CALL_STATUS_RECONNECTING: {
                        MegaApplication.setCallLayoutStatus(idChat, false);
                        showCallLayout(call);
                        break;
                    }
                    case MegaChatCall.CALL_STATUS_USER_NO_PRESENT:
                    case MegaChatCall.CALL_STATUS_REQUEST_SENT:
                    case MegaChatCall.CALL_STATUS_IN_PROGRESS: {
                        if(callStatus == MegaChatCall.CALL_STATUS_USER_NO_PRESENT && isAfterReconnecting(this, callInProgressLayout, callInProgressText)){
                            break;
                        }
                        showCallLayout(call);
                        break;
                    }
                    case MegaChatCall.CALL_STATUS_DESTROYED: {
                        setSubtitleVisibility();
                        hideCallInProgressLayout(call);
                        break;
                    }
                    default:
                        break;
                }

            } else if ((call.hasChanged(MegaChatCall.CHANGE_TYPE_REMOTE_AVFLAGS)) || (call.hasChanged(MegaChatCall.CHANGE_TYPE_LOCAL_AVFLAGS)) || (call.hasChanged(MegaChatCall.CHANGE_TYPE_CALL_COMPOSITION))) {
                logDebug("Changes in remote/local av flags or in the call composition");
                usersWithVideo();

            }
        } else {
            logDebug("Different chat");

        }
    }

    private void showCallLayout(MegaChatCall call) {
        if (megaChatApi == null) return;
        if (call == null) call = megaChatApi.getChatCall(idChat);
        if (call == null) return;
        logDebug("Call status "+callStatusToString(call.getStatus())+". Group chat: "+isGroup());
        switch (call.getStatus()){
            case MegaChatCall.CALL_STATUS_USER_NO_PRESENT:
            case MegaChatCall.CALL_STATUS_RING_IN:{
                if (isGroup()) {
                    usersWithVideo();

                    long callerHandle = call.getCaller();
                    String textLayout;
                    if (callerHandle != -1 && getPeerFullName(callerHandle) != null) {
                        textLayout = getString(R.string.join_call_layout_in_group_call, getPeerFullName(callerHandle));
                    } else {
                        textLayout = getString(R.string.join_call_layout);
                    }
                    tapToReturnLayout(call, textLayout);
                    return;
                }

                if (call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN && MegaApplication.getCallLayoutStatus(idChat)) {
                    tapToReturnLayout(call, getString(R.string.call_in_progress_layout));
                    return;
                }

                if(isAfterReconnecting(this, callInProgressLayout, callInProgressText)) return;

                hideCallInProgressLayout(call);
                return;

            }
            case MegaChatCall.CALL_STATUS_REQUEST_SENT:{
                if (MegaApplication.getCallLayoutStatus(idChat)) {
                    tapToReturnLayout(call, getString(R.string.call_in_progress_layout));
                    return;
                }

                hideCallInProgressLayout(call);
                return;
            }
            case MegaChatCall.CALL_STATUS_RECONNECTING:{
                activateChrono(false, subtitleChronoCall, call);
                callInProgressLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.reconnecting_bar));
                showCallInProgressLayout(getString(R.string.reconnecting_message), false, call);
                return;
            }
            case MegaChatCall.CALL_STATUS_IN_PROGRESS:{
                callInProgressLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.accentColor));
                if(!isAfterReconnecting(this, callInProgressLayout, callInProgressText)){
                    updateCallInProgressLayout(call);
                    return;
                }
                showCallInProgressLayout(getString(R.string.connected_message), false, call);
                callInProgressLayout.setAlpha(1);
                callInProgressLayout.setVisibility(View.VISIBLE);
                callInProgressLayout.animate()
                        .alpha(0f)
                        .setDuration(QUICK_INFO_ANIMATION)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                callInProgressLayout.setVisibility(View.GONE);
                                updateCallInProgressLayout(null);
                            }
                        });
                return;
            }
        }
    }

    private void tapToReturnLayout(MegaChatCall call, String text){
        activateChrono(false, subtitleChronoCall, call);
        callInProgressLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.accentColor));
        showCallInProgressLayout(text, false, call);
    }

    private void updateCallInProgressLayout(MegaChatCall call){
        if (call == null) call = megaChatApi.getChatCall(idChat);
        if (call == null) return;
        showCallInProgressLayout(getString(R.string.call_in_progress_layout), true, call);
        if (isGroup()) {
            subtitleCall.setVisibility(View.VISIBLE);
            individualSubtitleToobar.setVisibility(View.GONE);
            groupalSubtitleToolbar.setVisibility(View.GONE);
        }
        usersWithVideo();
        activateChrono(true, subtitleChronoCall, call);
        invalidateOptionsMenu();
    }

    private void usersWithVideo() {
        if (megaChatApi == null || !isGroup() || megaChatApi.getChatCall(idChat) == null || subtitleCall.getVisibility() != View.VISIBLE)
            return;

        int usersWithVideo = megaChatApi.getChatCall(idChat).getNumParticipants(MegaChatCall.VIDEO);
        int totalVideosAllowed = megaChatApi.getMaxVideoCallParticipants();
        if (usersWithVideo <= 0 || totalVideosAllowed == 0) {
            participantsLayout.setVisibility(View.GONE);
            return;
        }
        participantsText.setText(usersWithVideo + "/" + totalVideosAllowed);
        participantsLayout.setVisibility(View.VISIBLE);
    }

    public void goToEnd(){
        logDebug("goToEnd()");
        int infoToShow = -1;
        if(!messages.isEmpty()){
            int index = messages.size()-1;

            AndroidMegaChatMessage msg = messages.get(index);

            while (!msg.isUploading() && msg.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL) {
                index--;
                if (index == -1) {
                    break;
                }
                msg = messages.get(index);
            }

            if(index == (messages.size()-1)){
                //Scroll to end
                mLayoutManager.scrollToPositionWithOffset(index+1,scaleHeightPx(20, outMetrics));
            }else{
                index++;
                infoToShow = adjustInfoToShow(index);
                if(infoToShow== AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL){
                    mLayoutManager.scrollToPositionWithOffset(index, scaleHeightPx(50, outMetrics));
                }else{
                    mLayoutManager.scrollToPositionWithOffset(index, scaleHeightPx(20, outMetrics));
                }
            }
        }
        hideMessageJump();
    }

    public void setNewVisibility(boolean vis){
        newVisibility = vis;
    }

    public void hideMessageJump(){
        isHideJump = true;
        visibilityMessageJump=false;
        if(messageJumpLayout.getVisibility() == View.VISIBLE){
            messageJumpLayout.animate()
                        .alpha(0.0f)
                        .setDuration(1000)
                        .withEndAction(new Runnable() {
                            @Override public void run() {
                                messageJumpLayout.setVisibility(View.GONE);
                                messageJumpLayout.setAlpha(1.0f);
                            }
                        })
                        .start();
        }
    }

    public MegaApiAndroid getLocalMegaApiFolder() {

        PackageManager m = getPackageManager();
        String s = getPackageName();
        PackageInfo p;
        String path = null;
        try {
            p = m.getPackageInfo(s, 0);
            path = p.applicationInfo.dataDir + "/";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        MegaApiAndroid megaApiFolder = new MegaApiAndroid(MegaApplication.APP_KEY,
                MegaApplication.USER_AGENT, path);

        megaApiFolder.setDownloadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
        megaApiFolder.setUploadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);

        return megaApiFolder;
    }

    public File createImageFile() {
        logDebug("createImageFile");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "picture" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
        return new File(storageDir, imageFileName + ".jpg");
    }

    private void onCaptureImageResult() {
        logDebug("onCaptureImageResult");
        if (mOutputFilePath != null) {
            File f = new File(mOutputFilePath);
            if(f!=null){
                try {
                    File publicFile = copyImageFile(f);
                    //Remove mOutputFilePath
                    if (f.exists()) {
                        if (f.isDirectory()) {
                            if(f.list().length <= 0){
                                f.delete();
                            }
                        }else{
                            f.delete();
                        }
                    }
                    if(publicFile!=null){
                        Uri finalUri = Uri.fromFile(publicFile);
                        galleryAddPic(finalUri);
                        uploadPictureOrVoiceClip(publicFile.getPath());
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public File copyImageFile(File fileToCopy) throws IOException {
        logDebug("copyImageFile");
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
        File copyFile = new File(storageDir, fileToCopy.getName());
        copyFile.createNewFile();
        copy(fileToCopy, copyFile);
        return copyFile;
    }

    public static void copy(File src, File dst) throws IOException {
        logDebug("copy");
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    private void galleryAddPic(Uri contentUri) {
        logDebug("galleryAddPic");
        if(contentUri!=null){
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
            sendBroadcast(mediaScanIntent);
        }
    }

    public void hideKeyboard() {
        logDebug("hideKeyboard");
        hideFileStorage();
        if (emojiKeyboard == null) return;
        emojiKeyboard.hideBothKeyboard(this);
    }

    public void showConfirmationConnect(){
        logDebug("showConfirmationConnect");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        startConnection();
                        finish();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        logDebug("BUTTON_NEGATIVE");
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        try {
            builder.setMessage(R.string.confirmation_to_reconnect).setPositiveButton(R.string.cam_sync_ok, dialogClickListener)
                    .setNegativeButton(R.string.general_cancel, dialogClickListener).show().setCanceledOnTouchOutside(false);
        }
        catch (Exception e){}
    }

    public void startConnection() {
        logDebug("Broadcast to ManagerActivity");
        Intent intent = new Intent(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE);
        intent.putExtra("actionType", START_RECONNECTION);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    public int getDeviceDensity(){
        int screen = 0;
        switch (getResources().getDisplayMetrics().densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                screen = 1;
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                screen = 1;
                break;
            case DisplayMetrics.DENSITY_HIGH:
                screen = 1;
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                screen = 0;
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                screen = 0;
                break;
            case DisplayMetrics.DENSITY_XXXHIGH:
                screen = 0;
                break;
            default:
                screen = 0;
        }
        return screen;
    }

    public void setNodeAttachmentVisible() {
        logDebug("setNodeAttachmentVisible");
        if (adapter != null && holder_imageDrag != null && position_imageDrag != -1) {
            adapter.setNodeAttachmentVisibility(true, holder_imageDrag, position_imageDrag);
            holder_imageDrag = null;
            position_imageDrag = -1;
        }
    }

    private void addInBufferSending(AndroidMegaChatMessage androidMsg){
        if(bufferSending.isEmpty()){
            bufferSending.add(0,androidMsg);
        }else{
            boolean isContained = false;
            for(int i=0; i<bufferSending.size(); i++){
                if((bufferSending.get(i).getMessage().getMsgId() == androidMsg.getMessage().getMsgId())&&(bufferSending.get(i).getMessage().getTempId() == androidMsg.getMessage().getTempId())){
                    isContained = true;
                    break;
                }
            }
            if(!isContained){
                bufferSending.add(0,androidMsg);
            }
        }
    }

    public void setShareLinkDialogDismissed (boolean dismissed) {
        isShareLinkDialogDismissed = dismissed;
    }

    private void sendBroadcastChatArchived(String chatTitle) {
        Intent intent = new Intent(BROADCAST_ACTION_INTENT_CHAT_ARCHIVED);
        intent.putExtra(CHAT_TITLE, chatTitle);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        closeChat(true);
        finish();
    }
}