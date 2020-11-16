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
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ActionMode;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import mega.privacy.android.app.activities.GiphyPickerActivity;
import mega.privacy.android.app.components.BubbleDrawable;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.NpaLinearLayoutManager;
import mega.privacy.android.app.components.twemoji.EmojiEditText;
import mega.privacy.android.app.components.twemoji.EmojiKeyboard;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.components.voiceClip.OnBasketAnimationEnd;
import mega.privacy.android.app.components.voiceClip.OnRecordListener;
import mega.privacy.android.app.components.voiceClip.RecordButton;
import mega.privacy.android.app.components.voiceClip.RecordView;
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder;
import mega.privacy.android.app.fcm.KeepAliveService;
import mega.privacy.android.app.interfaces.OnProximitySensorListener;
import mega.privacy.android.app.interfaces.StoreDataBeforeForward;
import mega.privacy.android.app.listeners.CreateChatListener;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.listeners.GetPeerAttributesListener;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileLinkActivityLollipop;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.FolderLinkActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.AudioFocusListener;
import mega.privacy.android.app.lollipop.listeners.ChatLinkInfoListener;
import mega.privacy.android.app.lollipop.listeners.MultipleForwardChatProcessor;
import mega.privacy.android.app.lollipop.listeners.MultipleGroupChatRequestListener;
import mega.privacy.android.app.lollipop.listeners.MultipleRequestListener;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatLollipopAdapter;
import mega.privacy.android.app.lollipop.tasks.FilePrepareTask;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ReactionsBottomSheet;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.AttachmentUploadBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.InfoReactionsBottomSheet;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.GeneralChatMessageBottomSheet;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.MessageNotSentBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.PendingMessageBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.SendAttachmentChatBottomSheetDialogFragment;
import mega.privacy.android.app.objects.GifData;
import mega.privacy.android.app.utils.TimeUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatGeolocation;
import nz.mega.sdk.MegaChatGiphy;
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

import static mega.privacy.android.app.activities.GiphyPickerActivity.GIF_DATA;
import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop.*;
import static mega.privacy.android.app.lollipop.megachat.AndroidMegaRichLinkMessage.*;
import static mega.privacy.android.app.lollipop.megachat.MapsActivity.*;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.*;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.GiphyUtil.getGiphySrc;
import static mega.privacy.android.app.utils.LinksUtil.isMEGALinkAndRequiresTransferSession;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.TextUtil.*;
import static mega.privacy.android.app.utils.StringResourcesUtils.getTranslatedErrorString;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaApiJava.STORAGE_STATE_PAYWALL;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class ChatActivityLollipop extends PinActivityLollipop implements MegaChatRequestListenerInterface, MegaRequestListenerInterface, MegaChatListenerInterface, MegaChatRoomListenerInterface, View.OnClickListener, StoreDataBeforeForward<ArrayList<AndroidMegaChatMessage>> {

    private static final int MAX_NAMES_PARTICIPANTS = 3;

    public MegaChatLollipopAdapter.ViewHolderMessageChat holder_imageDrag;
    public int position_imageDrag = -1;
    private static final String PLAYING = "isAnyPlaying";
    private static final String ID_VOICE_CLIP_PLAYING = "idMessageVoicePlaying";
    private static final String PROGRESS_PLAYING = "progressVoicePlaying";
    private static final String MESSAGE_HANDLE_PLAYING = "messageHandleVoicePlaying";
    private static final String USER_HANDLE_PLAYING = "userHandleVoicePlaying";
    private final static String JOIN_CALL_DIALOG = "isJoinCallDialogShown";
    private static final String LAST_MESSAGE_SEEN = "LAST_MESSAGE_SEEN";
    private static final String GENERAL_UNREAD_COUNT = "GENERAL_UNREAD_COUNT";
    private static final String SELECTED_ITEMS = "selectedItems";
    private static final String JOINING_OR_LEAVING = "JOINING_OR_LEAVING";
    private static final String JOINING_OR_LEAVING_ACTION = "JOINING_OR_LEAVING_ACTION";
    private static final String OPENING_AND_JOINING_ACTION = "OPENING_AND_JOINING_ACTION";
    private static final String ERROR_REACTION_DIALOG = "ERROR_REACTION_DIALOG";
    private static final String TYPE_ERROR_REACTION = "TYPE_ERROR_REACTION";

    private final static int NUMBER_MESSAGES_TO_LOAD = 32;
    private final static int MAX_NUMBER_MESSAGES_TO_LOAD_NOT_SEEN = 256;
    private final static int NUMBER_MESSAGES_BEFORE_LOAD = 8;
    public static final int REPEAT_INTERVAL = 40;

    private final static int ROTATION_PORTRAIT = 0;
    private final static int ROTATION_LANDSCAPE = 1;
    private final static int ROTATION_REVERSE_PORTRAIT = 2;
    private final static int ROTATION_REVERSE_LANDSCAPE = 3;
    private final static int MAX_LINES_INPUT_TEXT = 5;
    private final static int TITLE_TOOLBAR_PORT = 140;
    private final static int TITLE_TOOLBAR_LAND = 250;
    private final static int TITLE_TOOLBAR_IND_PORT = 100;
    private final static int HINT_LAND = 550;
    private final static int HINT_PORT = 250;
    private final static boolean IS_LOW = true;
    private final static boolean IS_HIGH = false;

    public static int MEGA_FILE_LINK = 1;
    public static int MEGA_FOLDER_LINK = 2;
    public static int MEGA_CHAT_LINK = 3;

    private final static int SHOW_WRITING_LAYOUT = 1;
    private final static int SHOW_JOIN_LAYOUT = 2;
    private final static int SHOW_NOTHING_LAYOUT = 3;
    private final static  int SHOW_JOINING_OR_LEFTING_LAYOUT = 4;
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
    private int MIN_FIRST_AMPLITUDE = 2;
    private int MIN_SECOND_AMPLITUDE;
    private int MIN_THIRD_AMPLITUDE;
    private int MIN_FOURTH_AMPLITUDE;
    private int MIN_FIFTH_AMPLITUDE;
    private int MIN_SIXTH_AMPLITUDE;
    private final static int NOT_SOUND = 0;
    private final static int FIRST_RANGE = 1;
    private final static int SECOND_RANGE = 2;
    private final static int THIRD_RANGE = 3;
    private final static int FOURTH_RANGE = 4;
    private final static int FIFTH_RANGE = 5;
    private final static int SIXTH_RANGE = 6;
    private final static int WIDTH_BAR = 8;

    private final static int TYPE_MESSAGE_JUMP_TO_LEAST = 0;
    private final static int TYPE_MESSAGE_NEW_MESSAGE = 1;

    private int currentRecordButtonState;
    private String mOutputFilePath;
    private int keyboardHeight;
    private int marginBottomDeactivated;
    private int marginBottomActivated;
    private boolean newVisibility;
    private boolean getMoreHistory;
    private int minutesLastGreen = INVALID_VALUE;
    private boolean isLoadingHistory;
    private AlertDialog errorOpenChatDialog;
    private long numberToLoad;
    private ArrayList<Integer> recoveredSelectedPositions = null;

    private androidx.appcompat.app.AlertDialog downloadConfirmationDialog;
    private AlertDialog chatAlertDialog;
    private AlertDialog errorReactionsDialog;
    private boolean errorReactionsDialogIsShown;
    private long typeErrorReaction = REACTION_ERROR_DEFAULT_VALUE;
    private android.app.AlertDialog dialogCall;

    ProgressDialog dialog;
    ProgressDialog statusDialog;

    boolean retryHistory = false;

    private long lastIdMsgSeen = MEGACHAT_INVALID_HANDLE;
    private long generalUnreadCount;
    private boolean lastSeenReceived;
    private int positionToScroll = INVALID_VALUE;
    private int positionNewMessagesLayout = INVALID_VALUE;

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
    public int selectedPosition = INVALID_POSITION;
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
    private ImageView iconStateToolbar;
    private ImageView muteIconToolbar;
    private LinearLayout subtitleCall;
    private TextView subtitleChronoCall;
    private LinearLayout participantsLayout;
    private TextView participantsText;
    private ImageView privateIconToolbar;

    private boolean editingMessage = false;
    private MegaChatMessage messageToEdit = null;

    private CoordinatorLayout fragmentContainer;
    private RelativeLayout writingContainerLayout;
    private RelativeLayout writingLayout;

    private RelativeLayout joinChatLinkLayout;
    private Button joinButton;

    private RelativeLayout chatRelativeLayout;
    private RelativeLayout userTypingLayout;
    private TextView userTypingText;
    private RelativeLayout joiningLeavingLayout;
    private TextView joiningLeavingText;

    boolean sendIsTyping=true;
    long userTypingTimeStamp = -1;
    private ImageButton keyboardTwemojiButton;

    private ImageButton mediaButton;
    private ImageButton pickFileStorageButton;
    private ImageButton pickAttachButton;
    private ImageButton gifButton;

    private EmojiKeyboard emojiKeyboard;
    private RelativeLayout rLKeyboardTwemojiButton;

    private RelativeLayout rLMediaButton;
    private RelativeLayout rLPickFileStorageButton;
    private RelativeLayout rLPickAttachButton;
    private RelativeLayout rlGifButton;

    private RelativeLayout returnCallOnHoldButton;
    private ImageView returnCallOnHoldButtonIcon;
    private TextView returnCallOnHoldButtonText;

    private RelativeLayout callInProgressLayout;
    private long chatIdBanner;
    private TextView callInProgressText;
    private Chronometer callInProgressChrono;
    private boolean startVideo = false;

    private EmojiEditText textChat;
    ImageButton sendIcon;
    RelativeLayout messagesContainerLayout;

    RelativeLayout observersLayout;
    TextView observersNumberText;

    RecyclerView listView;
    NpaLinearLayoutManager mLayoutManager;

    ChatActivityLollipop chatActivity;

    private MenuItem importIcon;
    private MenuItem callMenuItem;
    private MenuItem videoMenuItem;
    private MenuItem selectMenuItem;
    private MenuItem inviteMenuItem;
    private MenuItem clearHistoryMenuItem;
    private MenuItem contactInfoMenuItem;
    private MenuItem leaveMenuItem;
    private MenuItem archiveMenuItem;
    private MenuItem muteMenuItem;
    private MenuItem unMuteMenuItem;

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
    private ArrayList<MessageVoiceClip> messagesPlaying = new ArrayList<>();
    private ArrayList<RemovedMessage> removedMessages = new ArrayList<>();

    RelativeLayout messageJumpLayout;
    TextView messageJumpText;
    boolean isHideJump = false;
    int typeMessageJump = 0;
    boolean visibilityMessageJump=false;
    boolean isTurn = false;
    Handler handler;

    private AlertDialog locationDialog;
    private boolean isLocationDialogShown = false;
    private boolean isJoinCallDialogShown = false;
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
    private RelativeLayout recordingLayout;
    private TextView recordingChrono;
    private RelativeLayout firstBar, secondBar, thirdBar, fourthBar, fifthBar, sixthBar;
    private int currentAmplitude = -1;
    private Handler handlerVisualizer = new Handler();

    private ActionMode actionMode;

    private AppRTCAudioManager rtcAudioManager = null;
    private boolean speakerWasActivated = true;

    // Data being stored when My Chat Files folder does not exist
    private ArrayList<AndroidMegaChatMessage> preservedMessagesSelected;
    private ArrayList<MegaChatMessage> preservedMsgSelected;
    private ArrayList<MegaChatMessage> preservedMsgToImport;
    private boolean isForwardingFromNC;

    private ArrayList<Intent> preservedIntents = new ArrayList<>();
    private boolean isWaitingForMoreFiles;
    private boolean isAskingForMyChatFiles;
    // The flag to indicate whether forwarding message is on going
    private boolean isForwardingMessage = false;

    private BottomSheetDialogFragment bottomSheetDialogFragment;

    private MegaNode myChatFilesFolder;
    private TextUtils.TruncateAt typeEllipsize = TextUtils.TruncateAt.END;

    private boolean joiningOrLeaving;
    private String joiningOrLeavingAction;
    private boolean openingAndJoining;

    private AudioFocusRequest request;
    private AudioManager mAudioManager;
    private AudioFocusListener audioFocusListener;

    @Override
    public void storedUnhandledData(ArrayList<AndroidMegaChatMessage> preservedData) {
        this.preservedMessagesSelected = preservedData;
    }

    @Override
    public void handleStoredData() {
        if (preservedMessagesSelected != null && !preservedMessagesSelected.isEmpty()) {
            forwardMessages(preservedMessagesSelected);
            preservedMessagesSelected = null;
        } else if (preservedMsgSelected != null && !preservedMsgSelected.isEmpty()) {
            chatC.proceedWithForward(myChatFilesFolder, preservedMsgSelected, preservedMsgToImport, idChat);
            isForwardingFromNC = false;
            preservedMsgSelected = null;
            preservedMsgToImport = null;
        }
    }

    @Override
    public void storedUnhandledData(ArrayList<MegaChatMessage> messagesSelected, ArrayList<MegaChatMessage> messagesToImport) {
        isForwardingFromNC = true;
        preservedMsgSelected = messagesSelected;
        preservedMsgToImport = messagesToImport;
        preservedMessagesSelected = null;
    }

    /**
     * Method for finding out if the selected message is deleted.
     *
     * @param messageSelected The message selected.
     * @return True if it's removed. False, otherwise.
     */
    public boolean hasMessagesRemoved(MegaChatMessage messageSelected) {
        if (removedMessages != null && !removedMessages.isEmpty()) {
            for (int i = 0; i < removedMessages.size(); i++) {
                if (messageSelected.getMsgId() == removedMessages.get(i).msgId) {
                    return true;
                }
            }
        }

        return false;
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

    private BroadcastReceiver userNameReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null
                || intent.getLongExtra(EXTRA_USER_HANDLE, INVALID_HANDLE) == INVALID_HANDLE) {
                return;
            }

            if (intent.getAction().equals(ACTION_UPDATE_NICKNAME)
                || intent.getAction().equals(ACTION_UPDATE_FIRST_NAME)
                || intent.getAction().equals(ACTION_UPDATE_LAST_NAME)) {
                updateUserNameInChat();
            }
        }
    };

    private BroadcastReceiver chatCallUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;

            long chatId = intent.getLongExtra(UPDATE_CHAT_CALL_ID, MEGACHAT_INVALID_HANDLE);
            if (chatId != getCurrentChatid()) {
                logDebug("Different chat");
                updateCallBar();
                return;
            }

            if (intent.getAction().equals(ACTION_CHANGE_CALL_ON_HOLD)) {
                updateCallBar();
            }

            if (intent.getAction().equals(ACTION_CALL_STATUS_UPDATE)) {

                int callStatus = intent.getIntExtra(UPDATE_CALL_STATUS, INVALID_CALL_STATUS);
                if (intent.getAction().equals(ACTION_CALL_STATUS_UPDATE) && callStatus >= MegaChatCall.CALL_STATUS_REQUEST_SENT) {
                    updateCallBar();
                }
                switch (callStatus) {
                    case MegaChatCall.CALL_STATUS_IN_PROGRESS:
                        cancelRecording();
                        break;

                    case MegaChatCall.CALL_STATUS_DESTROYED:
                        if (dialogCall != null) {
                            dialogCall.dismiss();
                        }
                        usersWithVideo();
                        break;
                }
            }

            if (intent.getAction().equals(ACTION_CHANGE_LOCAL_AVFLAGS) || intent.getAction().equals(ACTION_CHANGE_COMPOSITION)) {
                usersWithVideo();
            }
        }
    };

    private BroadcastReceiver chatSessionUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;

            long chatId = intent.getLongExtra(UPDATE_CHAT_CALL_ID, MEGACHAT_INVALID_HANDLE);
            if (chatId != getCurrentChatid()) {
                logWarning("Call different chat");
                return;
            }

            if (intent.getAction().equals(ACTION_CHANGE_REMOTE_AVFLAGS)) {
                usersWithVideo();
            }

            if (intent.getAction().equals(ACTION_CHANGE_SESSION_ON_HOLD)) {
                updateCallBar();
            }
        }
    };

    private BroadcastReceiver chatRoomMuteUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;

            if(intent.getAction().equals(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING)){
                if (chatRoom == null) {
                    chatRoom = megaChatApi.getChatRoom(idChat);
                }
                if (chatRoom != null) {
                    muteIconToolbar.setVisibility(isEnableChatNotifications(chatRoom.getChatId()) ? View.GONE : View.VISIBLE);
                }
            }
        }
    };

    private BroadcastReceiver leftChatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;

            if (intent.getAction().equals(ACTION_LEFT_CHAT)) {
                long extraIdChat = intent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE);
                if (extraIdChat != MEGACHAT_INVALID_HANDLE) {
                    chatC.leaveChat(extraIdChat);
                    if (idChat == extraIdChat) {
                        setJoiningOrLeaving(getString(R.string.leaving_label));
                    }
                }
            }
        }
    };

    private BroadcastReceiver closeChatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;

            if (intent.getAction().equals(ACTION_CLOSE_CHAT_AFTER_IMPORT)
                    || intent.getAction().equals(ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS)) {
                finish();
            }
        }
    };

    ArrayList<UserTyping> usersTyping;
    List<UserTyping> usersTypingSync;

    public void openMegaLink(String url, boolean isFile) {
        logDebug("url: " + url + ", isFile: " + isFile);
        Intent openLink;

        if (isFile) {
            openLink = new Intent(this, FileLinkActivityLollipop.class);
            openLink.setAction(ACTION_OPEN_MEGA_LINK);
        } else {
            openLink = new Intent(this, FolderLinkActivityLollipop.class);
            openLink.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);
        }

        openLink.putExtra(OPENED_FROM_CHAT, true);
        openLink.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openLink.setData(Uri.parse(url));
        startActivity(openLink);
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
    public void onCreate(Bundle savedInstanceState) {
        logDebug("onCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }

        if (megaChatApi == null || megaChatApi.getInitState() == MegaChatApi.INIT_ERROR || megaChatApi.getInitState() == MegaChatApi.INIT_NOT_DONE) {
            logDebug("Refresh session - karere");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        dbH = DatabaseHandler.getDbHandler(this);

        handler = new Handler();

        chatActivity = this;
        chatC = new ChatController(chatActivity);

        registerReceiver(dialogConnectReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE_DIALOG));
        registerReceiver(voiceclipDownloadedReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_VOICE_CLIP_DOWNLOADED));

        IntentFilter contactUpdateFilter = new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE);
        contactUpdateFilter.addAction(ACTION_UPDATE_NICKNAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_FIRST_NAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_LAST_NAME);
        registerReceiver(userNameReceiver, contactUpdateFilter);

        registerReceiver(chatArchivedReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_CHAT_ARCHIVED_GROUP));

        IntentFilter filterCall = new IntentFilter(ACTION_CALL_STATUS_UPDATE);
        filterCall.addAction(ACTION_CHANGE_LOCAL_AVFLAGS);
        filterCall.addAction(ACTION_CHANGE_COMPOSITION);
        filterCall.addAction(ACTION_CHANGE_CALL_ON_HOLD);
        registerReceiver(chatCallUpdateReceiver, filterCall);

        IntentFilter filterSession = new IntentFilter(ACTION_CHANGE_SESSION_ON_HOLD);
        filterSession.addAction(ACTION_CHANGE_REMOTE_AVFLAGS);
        registerReceiver(chatSessionUpdateReceiver, filterSession);

        registerReceiver(chatRoomMuteUpdateReceiver, new IntentFilter(ACTION_UPDATE_PUSH_NOTIFICATION_SETTING));

        IntentFilter leftChatFilter = new IntentFilter(BROADCAST_ACTION_INTENT_LEFT_CHAT);
        leftChatFilter.addAction(ACTION_LEFT_CHAT);
        registerReceiver(leftChatReceiver, leftChatFilter);

        IntentFilter closeChatFilter = new IntentFilter(ACTION_CLOSE_CHAT_AFTER_IMPORT);
        closeChatFilter.addAction(ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS);
        registerReceiver(closeChatReceiver, closeChatFilter);

        changeStatusBarColor(this, getWindow(), R.color.lollipop_dark_primary_color);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        setContentView(R.layout.activity_chat);

        //Set toolbar
        tB = findViewById(R.id.toolbar_chat);

        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);
        aB.setTitle(null);
        aB.setSubtitle(null);
        tB.setOnClickListener(this);

        int range = 32000/6;
        MIN_SECOND_AMPLITUDE = range;
        MIN_THIRD_AMPLITUDE = range * SECOND_RANGE;
        MIN_FOURTH_AMPLITUDE = range * THIRD_RANGE;
        MIN_FIFTH_AMPLITUDE = range * FOURTH_RANGE;
        MIN_SIXTH_AMPLITUDE = range * FIFTH_RANGE;

        toolbarElementsInside = tB.findViewById(R.id.toolbar_elements_inside);
        titleToolbar = tB.findViewById(R.id.title_toolbar);
        iconStateToolbar = tB.findViewById(R.id.state_icon_toolbar);
        privateIconToolbar = tB.findViewById(R.id.private_icon_toolbar);
        muteIconToolbar = tB.findViewById(R.id.mute_icon_toolbar);

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
        setGroupalSubtitleToolbarVisibility(false);
        subtitleCall.setVisibility(View.GONE);
        subtitleChronoCall.setVisibility(View.GONE);
        participantsLayout.setVisibility(View.GONE);
        iconStateToolbar.setVisibility(View.GONE);
        privateIconToolbar.setVisibility(View.GONE);
        muteIconToolbar.setVisibility(View.GONE);
        badgeDrawable = new BadgeDrawerArrowDrawable(getSupportActionBar().getThemedContext());
        updateNavigationToolbarIcon();


        joinChatLinkLayout = findViewById(R.id.join_chat_layout_chat_layout);
        joinButton = findViewById(R.id.join_button);
        joinButton.setOnClickListener(this);

        joiningLeavingLayout = findViewById(R.id.joining_leaving_layout_chat_layout);
        joiningLeavingText = findViewById(R.id.joining_leaving_text_chat_layout);

        messageJumpLayout = findViewById(R.id.message_jump_layout);
        messageJumpText = findViewById(R.id.message_jump_text);
        messageJumpLayout.setVisibility(View.GONE);
        writingLayout = findViewById(R.id.writing_linear_layout_chat);

        rLKeyboardTwemojiButton = findViewById(R.id.rl_keyboard_twemoji_chat);
        rLMediaButton = findViewById(R.id.rl_media_icon_chat);
        rLPickFileStorageButton = findViewById(R.id.rl_pick_file_storage_icon_chat);
        rLPickAttachButton = findViewById(R.id.rl_attach_icon_chat);
        rlGifButton = findViewById(R.id.rl_gif_chat);

        keyboardTwemojiButton = findViewById(R.id.keyboard_twemoji_chat);
        mediaButton = findViewById(R.id.media_icon_chat);
        pickFileStorageButton = findViewById(R.id.pick_file_storage_icon_chat);
        pickAttachButton = findViewById(R.id.pick_attach_chat);
        gifButton = findViewById(R.id.gif_chat);

        keyboardHeight = getOutMetrics().heightPixels / 2 - getActionBarHeight(this, getResources());
        marginBottomDeactivated = px2dp(MARGIN_BUTTON_DEACTIVATED, getOutMetrics());
        marginBottomActivated = px2dp(MARGIN_BUTTON_ACTIVATED, getOutMetrics());

        callInProgressLayout = findViewById(R.id.call_in_progress_layout);
        callInProgressLayout.setVisibility(View.GONE);
        callInProgressText = findViewById(R.id.call_in_progress_text);
        callInProgressChrono = findViewById(R.id.call_in_progress_chrono);
        callInProgressChrono.setVisibility(View.GONE);

        returnCallOnHoldButton = findViewById(R.id.call_on_hold_layout);
        returnCallOnHoldButtonIcon = findViewById(R.id.call_on_hold_icon);
        returnCallOnHoldButtonText = findViewById(R.id.call_on_hold_text);

        returnCallOnHoldButton.setOnClickListener(this);
        returnCallOnHoldButton.setVisibility(View.GONE);

        /*Recording views*/
        recordingLayout = findViewById(R.id.recording_layout);
        recordingChrono = findViewById(R.id.recording_time);
        recordingChrono.setText(new SimpleDateFormat("mm:ss").format(0));
        firstBar = findViewById(R.id.first_bar);
        secondBar = findViewById(R.id.second_bar);
        thirdBar = findViewById(R.id.third_bar);
        fourthBar = findViewById(R.id.fourth_bar);
        fifthBar = findViewById(R.id.fifth_bar);
        sixthBar = findViewById(R.id.sixth_bar);

        initRecordingItems(IS_LOW);
        recordingLayout.setVisibility(View.GONE);

        enableButton(rLKeyboardTwemojiButton, keyboardTwemojiButton);
        enableButton(rLMediaButton, mediaButton);
        enableButton(rLPickAttachButton, pickAttachButton);
        enableButton(rLPickFileStorageButton, pickFileStorageButton);
        enableButton(rlGifButton, gifButton);

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
        BubbleDrawable myBubble = new BubbleDrawable(BubbleDrawable.CENTER, ContextCompat.getColor(this,R.color.voice_clip_bubble));
        myBubble.setCornerRadius(CORNER_RADIUS_BUBBLE);
        myBubble.setPointerAlignment(BubbleDrawable.RIGHT);
        myBubble.setPadding(PADDING_BUBBLE, PADDING_BUBBLE, PADDING_BUBBLE, PADDING_BUBBLE);
        bubbleLayout.setBackground(myBubble);
        bubbleLayout.setVisibility(View.GONE);
        bubbleText = findViewById(R.id.bubble_text);
        bubbleText.setMaxWidth(px2dp(MAX_WIDTH_BUBBLE, getOutMetrics()));
        recordButton.setRecordView(recordView);
        myAudioRecorder = new MediaRecorder();
        showInputText();

        //Input text:
        handlerKeyboard = new Handler();
        handlerEmojiKeyboard = new Handler();

        emojiKeyboard = findViewById(R.id.emojiView);
        emojiKeyboard.initEmoji(this, textChat, keyboardTwemojiButton);
        emojiKeyboard.setListenerActivated(true);

        observersLayout = findViewById(R.id.observers_layout);
        observersNumberText = findViewById(R.id.observers_text);

        textChat.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) { }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s != null && !s.toString().trim().isEmpty()) {
                    showSendIcon();
                } else {
                    refreshTextInput();
                }

                if (getCurrentFocus() == textChat) {
                    // is only executed if the EditText was directly changed by the user
                    if (sendIsTyping) {
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
                    if (chatRoom != null) {
                        megaChatApi.sendStopTypingNotification(chatRoom.getChatId());
                    }
                }
            }
        });

        textChat.setOnTouchListener((v, event) -> {
            showLetterKB();
            return false;
        });

        textChat.setOnLongClickListener(v -> {
            showLetterKB();
            return false;
        });

        textChat.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                showLetterKB();
            }
            return false;
        });

        textChat.setMediaListener(path -> uploadPictureOrVoiceClip(path));

        /*
        *If the recording button (an arrow) is clicked, the recording will be sent to the chat
        */
        recordButton.setOnRecordClickListener(v -> {
            recordButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
            sendRecording();
        });

        /*
         *Events of the recording
         */
        recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                if (participatingInACall()) {
                    showSnackbar(SNACKBAR_TYPE, getApplicationContext().getString(R.string.not_allowed_recording_voice_clip), -1);
                    return;
                }
                if (!isAllowedToRecord()) return;
                audioFocusListener = new AudioFocusListener(ChatActivityLollipop.this);
                request = getRequest(audioFocusListener, AUDIOFOCUS_DEFAULT);
                if (getAudioFocus(mAudioManager, audioFocusListener, request, AUDIOFOCUS_DEFAULT, STREAM_MUSIC_DEFAULT)) {
                    prepareRecording();
                }
            }

            @Override
            public void onLessThanSecond() {
                if (!isAllowedToRecord()) return;
                showBubble();
            }

            @Override
            public void onCancel() {
                recordButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                cancelRecording();
            }

            @Override
            public void onLock() {
                recordButtonStates(RECORD_BUTTON_SEND);
            }

            @Override
            public void onFinish(long recordTime) {
                recordButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                sendRecording();
            }

            @Override
            public void finishedSound() {
                if (!isAllowedToRecord()) return;
                startRecording();
            }

            @Override
            public void changeTimer(CharSequence time) {
               if(recordingLayout != null && recordingChrono != null && recordingLayout.getVisibility() == View.VISIBLE){
                   recordingChrono.setText(time);
               }
            }
        });

        recordView.setOnBasketAnimationEndListener(new OnBasketAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                recordButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                cancelRecording();
            }

            @Override
            public void deactivateRecordButton() {
                hideChatOptions();
                recordView.setVisibility(View.VISIBLE);
                recordLayout.setVisibility(View.VISIBLE);
                recordButtonLayout.setVisibility(View.VISIBLE);
                recordButton.activateOnTouchListener(false);
                recordButtonDeactivated(true);
                placeRecordButton(RECORD_BUTTON_DEACTIVATED);
            }
        });


        emojiKeyboard.setOnPlaceButtonListener(() -> {
            if(sendIcon.getVisibility() != View.VISIBLE){
                recordLayout.setVisibility(View.VISIBLE);
                recordButtonLayout.setVisibility(View.VISIBLE);
            }
            recordView.setVisibility(View.INVISIBLE);
            recordButton.activateOnTouchListener(true);
            placeRecordButton(RECORD_BUTTON_DEACTIVATED);
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
                    scrollingUp = dy > 0 ? true : false;

                    if (!scrollingUp && mLayoutManager.findFirstVisibleItemPosition() <= NUMBER_MESSAGES_BEFORE_LOAD && getMoreHistory) {
                        askForMoreMessages();
                        positionToScroll = INVALID_VALUE;
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
        if (participatingInACall()) return false;
        if (!checkPermissionsVoiceClip()) return false;
        return true;
    }

    private void showLetterKB() {
        hideFileStorage();
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

        if (newIntent != null){
            logDebug("Intent is not null");
            intentAction = newIntent.getAction();
            if (intentAction != null){

                if (intentAction.equals(ACTION_OPEN_CHAT_LINK) || intentAction.equals(ACTION_JOIN_OPEN_CHAT_LINK)){
                    String link = newIntent.getDataString();
                    megaChatApi.openChatPreview(link, this);

                    if (intentAction.equals(ACTION_JOIN_OPEN_CHAT_LINK)) {
                        openingAndJoining = true;
                        setJoiningOrLeaving(getString(R.string.joining_label));
                    }
                } else {
                    long newIdChat = newIntent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE);

                    if(idChat != newIdChat){
                        megaChatApi.closeChatRoom(idChat, this);
                        idChat = newIdChat;
                    }
                    megaChatApi.addChatListener(this);
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
                        isJoinCallDialogShown = savedInstanceState.getBoolean(JOIN_CALL_DIALOG, false);
                        recoveredSelectedPositions = savedInstanceState.getIntegerArrayList(SELECTED_ITEMS);

                        if(visibilityMessageJump){
                            if(typeMessageJump == TYPE_MESSAGE_NEW_MESSAGE){
                                messageJumpText.setText(getResources().getString(R.string.message_new_messages));
                                messageJumpLayout.setVisibility(View.VISIBLE);
                            }else if(typeMessageJump == TYPE_MESSAGE_JUMP_TO_LEAST){
                                messageJumpText.setText(getResources().getString(R.string.message_jump_latest));
                                messageJumpLayout.setVisibility(View.VISIBLE);
                            }
                        }

                        lastIdMsgSeen = savedInstanceState.getLong(LAST_MESSAGE_SEEN, MEGACHAT_INVALID_HANDLE);
                        isTurn = lastIdMsgSeen != MEGACHAT_INVALID_HANDLE;

                        generalUnreadCount = savedInstanceState.getLong(GENERAL_UNREAD_COUNT, 0);

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

                        joiningOrLeaving = savedInstanceState.getBoolean(JOINING_OR_LEAVING, false);
                        joiningOrLeavingAction = savedInstanceState.getString(JOINING_OR_LEAVING_ACTION);
                        openingAndJoining = savedInstanceState.getBoolean(OPENING_AND_JOINING_ACTION, false);
                        errorReactionsDialogIsShown = savedInstanceState.getBoolean(ERROR_REACTION_DIALOG, false);
                        typeErrorReaction = savedInstanceState.getLong(TYPE_ERROR_REACTION, REACTION_ERROR_DEFAULT_VALUE);
                        if(errorReactionsDialogIsShown && typeErrorReaction != REACTION_ERROR_DEFAULT_VALUE){
                            createLimitReactionsAlertDialog(typeErrorReaction);
                        }
                    }

                    String text = null;
                    if (intentAction.equals(ACTION_CHAT_SHOW_MESSAGES)) {
                        logDebug("ACTION_CHAT_SHOW_MESSAGES");
                        isOpeningChat = true;

                        int errorCode = newIntent.getIntExtra("PUBLIC_LINK", 1);
                        if (savedInstanceState == null) {
                            text = newIntent.getStringExtra(SHOW_SNACKBAR);
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
                    initAndShowChat(text);
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
        ChatItemPreferences chatPrefs = dbH.findChatPreferencesByHandle(Long.toString(idChat));

        if (chatPrefs != null) {
            String written = chatPrefs.getWrittenText();
            if (!TextUtils.isEmpty(written)) {
                String editedMsgId = chatPrefs.getEditedMsgId();
                editingMessage = !isTextEmpty(editedMsgId);
                messageToEdit = editingMessage ? megaChatApi.getMessage(idChat, Long.parseLong(editedMsgId)) : null;
                textChat.setText(written);
                showSendIcon();
                return;
            }
        } else {
            chatPrefs = new ChatItemPreferences(Long.toString(idChat), "");
            dbH.setChatItemPreferences(chatPrefs);
        }
        refreshTextInput();
    }

    private CharSequence transformEmojis(String textToTransform, float sizeText){
        CharSequence text = textToTransform == null ? "" : textToTransform;
        String resultText = converterShortCodes(text.toString());
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(resultText);
        EmojiManager.getInstance().replaceWithImages(this, spannableStringBuilder, sizeText, sizeText);
        int maxWidth;
        if(isScreenInPortrait(this)){
            maxWidth = HINT_PORT;
        }else{
            maxWidth = HINT_LAND;
        }
        CharSequence textF = TextUtils.ellipsize(spannableStringBuilder, textChat.getPaint(), px2dp(maxWidth, getOutMetrics()), typeEllipsize);
        return textF;
    }

    private void refreshTextInput() {
        recordButtonStates(RECORD_BUTTON_DEACTIVATED);
        sendIcon.setVisibility(View.GONE);
        sendIcon.setEnabled(false);
        sendIcon.setImageDrawable(ContextCompat.getDrawable(chatActivity, R.drawable.ic_send_trans));
        if (chatRoom != null) {
            megaChatApi.sendStopTypingNotification(chatRoom.getChatId());
            String title;
            setSizeInputText(true);
            if (chatRoom.hasCustomTitle()) {
                title = getString(R.string.type_message_hint_with_customized_title, getTitleChat(chatRoom));
            } else {
                title = getString(R.string.type_message_hint_with_default_title, getTitleChat(chatRoom));
            }
            textChat.setHint(transformEmojis(title, textChat.getTextSize()));
        }
    }

    public void updateUserNameInChat() {
        if (chatRoom.isGroup()) {
            setChatSubtitle();
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void updateTitle() {
        initializeInputText();
        titleToolbar.setText(getTitleChat(chatRoom));
    }

    /**
     * Opens a new chat conversation, checking if the id is valid and if the ChatRoom exists.
     * If an error ocurred opening the chat, an error dialog is shown.
     *
     * @return True if the chat was successfully opened, false otherwise
     */
    private boolean initChat() {
        if (idChat == MEGACHAT_INVALID_HANDLE) {
            logError("Chat ID -1 error");
            return false;
        }

        //Recover chat
        logDebug("Recover chat with id: " + idChat);
        chatRoom = megaChatApi.getChatRoom(idChat);
        if (chatRoom == null) {
            logError("Chatroom is NULL - finish activity!!");
            finish();
        }

        if (adapter != null) {
            adapter.updateChatRoom(chatRoom);
        }

        megaChatApi.closeChatRoom(idChat, this);
        if (megaChatApi.openChatRoom(idChat, this)) {
            MegaApplication.setClosedChat(false);
            return true;
        }

        logError("Error openChatRoom");
        if (errorOpenChatDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.chat_error_open_title))
                    .setMessage(getString(R.string.chat_error_open_message))
                    .setPositiveButton(getString(R.string.general_ok), (dialog, whichButton) -> finish());
            errorOpenChatDialog = builder.create();
            errorOpenChatDialog.show();
        }

        return false;
    }

    /**
     * Opens a new chat conversation.
     * If it went well, shows the chat with the empty state and requests messages.
     *
     * @param textSnackbar  if there is a chat link involved in the action, it it indicates the "Copy chat link" dialog has to be shown.
     *                      If not, a simple Snackbar has to be shown with this text.
     */
    private void initAndShowChat(String textSnackbar) {
        if (!initChat()) {
            return;
        }

        initializeInputText();

        int chatConnection = megaChatApi.getChatConnectionState(idChat);
        logDebug("Chat connection (" + idChat + ") is: " + chatConnection);

        if (adapter == null) {
            createAdapter();
        } else {
            adapter.updateChatRoom(chatRoom);
            adapter.notifyDataSetChanged();
        }

        setPreviewersView();
        titleToolbar.setText(chatRoom.getTitle());
        setChatSubtitle();
        privateIconToolbar.setVisibility(chatRoom.isPublic() ? View.GONE : View.VISIBLE);
        muteIconToolbar.setVisibility(isEnableChatNotifications(chatRoom.getChatId()) ? View.GONE : View.VISIBLE);
        isOpeningChat = true;

        String textToShowB = getString(R.string.chat_loading_messages);

        try {
            textToShowB = textToShowB.replace("[A]", "<font color=\'#7a7a7a\'>");
            textToShowB = textToShowB.replace("[/A]", "</font>");
            textToShowB = textToShowB.replace("[B]", "<font color=\'#000000\'>");
            textToShowB = textToShowB.replace("[/B]", "</font>");
        } catch (Exception e) {
            logWarning("Exception formatting string", e);
        }

        emptyScreen(HtmlCompat.fromHtml(textToShowB, HtmlCompat.FROM_HTML_MODE_LEGACY).toString());

        if (!isTextEmpty(textSnackbar)) {
            String chatLink = getIntent().getStringExtra(CHAT_LINK_EXTRA);

            if (!isTextEmpty(chatLink) && !isShareLinkDialogDismissed) {
                showShareChatLinkDialog(this, chatRoom, chatLink);
            } else {
                showSnackbar(SNACKBAR_TYPE, textSnackbar, MEGACHAT_INVALID_HANDLE);
            }
        }

        loadHistory();
        logDebug("On create: stateHistory: " + stateHistory);
        if (isLocationDialogShown) {
            showSendLocationDialog();
        }

        if (isJoinCallDialogShown) {
            MegaChatCall callInThisChat = megaChatApi.getChatCall(chatRoom.getChatId());
            if (callInThisChat != null && !callInThisChat.isOnHold() && chatRoom.isGroup()) {
                MegaChatCall anotherCallActive = getAnotherActiveCall(chatRoom.getChatId());
                if (anotherCallActive != null ) {
                    showJoinCallDialog(callInThisChat.getChatid(), anotherCallActive);
                }else{
                    MegaChatCall anotherCallOnHold = getAnotherCallOnHold(chatRoom.getChatId());
                    if(anotherCallOnHold != null){
                        showJoinCallDialog(callInThisChat.getChatid(), anotherCallOnHold);
                    }
                }
            }
        }
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

    /**
     * Requests to load for first time chat messages.
     * It controls if it is the real first time, or the device was rotated with the "isTurn" flag.
     */
    public void loadHistory() {
        long unreadCount = chatRoom.getUnreadCount();
        lastSeenReceived = unreadCount == 0;

        if (lastSeenReceived) {
            logDebug("loadMessages:unread is 0");

            if (!isTurn) {
                lastIdMsgSeen = MEGACHAT_INVALID_HANDLE;
                generalUnreadCount = 0;
            }
        } else {
            if (!isTurn) {
                lastIdMsgSeen = megaChatApi.getLastMessageSeenId(idChat);
                generalUnreadCount = unreadCount;
            } else {
                logDebug("Do not change lastSeenId --> rotating screen");
            }

            if (lastIdMsgSeen != -1) {
                logDebug("lastSeenId: " + lastIdMsgSeen);
            } else {
                logError("Error:InvalidLastMessage");
            }
        }

        askForMoreMessages();
    }

    /**
     * Sets the visibility of the groupalSubtitleToolbar view.
     * If it is visible some attributes of the layout should be updated due to the marquee behaviour.
     *
     * This method should be used always the visibility of groupalSubtitleToolbar
     * changes instead of change the visibility directly.
     *
     * @param visible   true if visible, false otherwise
     */
    private void setGroupalSubtitleToolbarVisibility(boolean visible) {
        groupalSubtitleToolbar.setVisibility(visible ? View.VISIBLE : View.GONE);

        if (visible) {
            groupalSubtitleToolbar.setSelected(true);
            groupalSubtitleToolbar.setHorizontallyScrolling(true);
            groupalSubtitleToolbar.setFocusable(true);
            groupalSubtitleToolbar.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            groupalSubtitleToolbar.setMarqueeRepeatLimit(-1);
            groupalSubtitleToolbar.setSingleLine(true);
            groupalSubtitleToolbar.setHorizontallyScrolling(true);
        }
    }

    private void setSubtitleVisibility() {
        if(chatRoom == null){
            chatRoom = megaChatApi.getChatRoom(idChat);
        }

        if(chatRoom == null)
            return;

        boolean isGroup = chatRoom.isGroup();

        individualSubtitleToobar.setVisibility(isGroup ? View.GONE : View.VISIBLE);
        setGroupalSubtitleToolbarVisibility(isGroup);

        if (chatRoom.isGroup()) {
            iconStateToolbar.setVisibility(View.GONE);
        }

        subtitleCall.setVisibility(View.GONE);
    }

    private void setPreviewGroupalSubtitle() {
        long participants = chatRoom.getPeerCount();

        setGroupalSubtitleToolbarVisibility(participants > 0);
        if (participants > 0) {
            groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.number_of_participants, participants)));
        }
    }

    public void setChatSubtitle(){
        logDebug("setChatSubtitle");
        if(chatRoom==null){
            return;
        }
        int width;
        if(isScreenInPortrait(this)){
            if(chatRoom.isGroup()) {
                width = scaleWidthPx(TITLE_TOOLBAR_PORT, getOutMetrics());
            }else {
                width = scaleWidthPx(TITLE_TOOLBAR_IND_PORT, getOutMetrics());
            }
        }else{
            width = scaleWidthPx(TITLE_TOOLBAR_LAND, getOutMetrics());
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
                   setBottomLayout(openingAndJoining ? SHOW_JOINING_OR_LEFTING_LAYOUT : SHOW_JOIN_LAYOUT);
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
                            setGroupalSubtitleToolbarVisibility(false);
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

    /**
     * Updates the views that have to be shown at the bottom of the UI.
     *
     * @param show  indicates which layout has to be shown at the bottom of the UI
     */
    public void setBottomLayout(int show) {
        if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
            show = SHOW_NOTHING_LAYOUT;
        } else if (joiningOrLeaving) {
            show = SHOW_JOINING_OR_LEFTING_LAYOUT;
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messagesContainerLayout.getLayoutParams();

        switch (show) {
            case SHOW_JOINING_OR_LEFTING_LAYOUT:
                writingContainerLayout.setVisibility(View.GONE);
                joinChatLinkLayout.setVisibility(View.GONE);
                params.addRule(RelativeLayout.ABOVE, R.id.joining_leaving_layout_chat_layout);
                messagesContainerLayout.setLayoutParams(params);
                fragmentVoiceClip.setVisibility(View.GONE);
                joiningLeavingLayout.setVisibility(View.VISIBLE);
                joiningLeavingText.setText(joiningOrLeavingAction);
                break;

            case SHOW_JOIN_LAYOUT:
                writingContainerLayout.setVisibility(View.GONE);
                joinChatLinkLayout.setVisibility(View.VISIBLE);
                params.addRule(RelativeLayout.ABOVE, R.id.join_chat_layout_chat_layout);
                messagesContainerLayout.setLayoutParams(params);
                fragmentVoiceClip.setVisibility(View.GONE);
                joiningLeavingLayout.setVisibility(View.GONE);
                break;

            case SHOW_NOTHING_LAYOUT:
                writingContainerLayout.setVisibility(View.GONE);
                joinChatLinkLayout.setVisibility(View.GONE);
                fragmentVoiceClip.setVisibility(View.GONE);
                joiningLeavingLayout.setVisibility(View.GONE);
                break;

            default:
                writingContainerLayout.setVisibility(View.VISIBLE);
                joinChatLinkLayout.setVisibility(View.GONE);
                params.addRule(RelativeLayout.ABOVE, R.id.writing_container_layout_chat_layout);
                messagesContainerLayout.setLayoutParams(params);
                fragmentVoiceClip.setVisibility(View.VISIBLE);
                joiningLeavingLayout.setVisibility(View.GONE);
        }
    }

    /**
     * When the group chat has a custom title, the subtitle has to contain the participants' names.
     * It sets the custom subtitle. The subtitle would contain the participant's names following these rules:
     * - If the group has four or less participants: all their names.
     * - If the group has more than four participants: the names of the three first participants and X more,
     *      which "X" is the number of the rest of participants
     */
    private void setCustomSubtitle() {
        logDebug("setCustomSubtitle");
        long participantsCount = chatRoom.getPeerCount();

        if (participantsCount == 0 && !chatRoom.isPreview()) {
            groupalSubtitleToolbar.setText(R.string.bucket_word_me);
            return;
        }

        StringBuilder customSubtitle = new StringBuilder();

        for (int i = 0; i < participantsCount; i++) {
            if ((i == 1 || i == 2) && areMoreParticipants(i)) {
                customSubtitle.append(", ");
            }
            String participantName = chatC.getParticipantFullName(chatRoom.getPeerHandle(i));
            if (isTextEmpty(participantName)) {
                sendGetPeerAttributesRequest(participantsCount);
                return;
            } else if (i == 0 && !areMoreParticipants(i)) {
                if (!chatRoom.isPreview()) {
                    customSubtitle.append(participantName)
                            .append(", ").append(getString(R.string.bucket_word_me));
                    groupalSubtitleToolbar.setText(adjustForLargeFont(customSubtitle.toString()));
                } else {
                    groupalSubtitleToolbar.setText(adjustForLargeFont(participantName));
                }
            } else if (areMoreParticipantsThanMaxAllowed(i)) {
                String firstNames = customSubtitle.append(participantName).toString();
                groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.custom_subtitle_of_group_chat, firstNames, participantsCount - 2)));
                break;
            } else {
                customSubtitle.append(participantName);

                if (i == participantsCount - 1) {
                    if (!chatRoom.isPreview()) {
                        customSubtitle.append(", ").append(getString(R.string.bucket_word_me));
                    }

                    groupalSubtitleToolbar.setText(adjustForLargeFont(customSubtitle.toString()));
                }
            }
        }

        if (isTextEmpty(groupalSubtitleToolbar.getText().toString())) {
            groupalSubtitleToolbar.setText(null);
            setGroupalSubtitleToolbarVisibility(false);
        }
    }

    /**
     * Checks if there are more participants in the group chat after the current position.
     *
     * @param position  position to check
     * @return  True if there are more participants after the current position, false otherwise.
     */
    private boolean areMoreParticipants(long position) {
        return chatRoom.getPeerCount() > position;
    }

    /**
     * Checks if there only three participants in the group chat.
     *
     * @param position  position to check
     * @return  True if there are three participants, false otherwise.
     */
    private boolean areSameParticipantsAsMaxAllowed(long position) {
        return chatRoom.getPeerCount() == MAX_NAMES_PARTICIPANTS && position == 2;
    }

    /**
     * Checks if there are more than three participants in the group chat.
     *
     * @param position  position to check
     * @return True if there are more than three participants, false otherwise.
     */
    private boolean areMoreParticipantsThanMaxAllowed(long position) {
        return chatRoom.getPeerCount() > MAX_NAMES_PARTICIPANTS && position == 2;
    }

    /**
     * Requests the attributes of the participants when they unavailable.
     *
     * @param participantsCount number of participants in the group chat.
     */
    private void sendGetPeerAttributesRequest(long participantsCount) {
        MegaHandleList handleList = MegaHandleList.createInstance();

        for (int i = 0; i < participantsCount; i++) {
            handleList.addMegaHandle(chatRoom.getPeerHandle(i));

            if (areMoreParticipantsThanMaxAllowed(i) || areSameParticipantsAsMaxAllowed(i))
                break;
        }

        if (handleList.size() > 0) {
            megaChatApi.loadUserAttributes(chatRoom.getChatId(), handleList, new GetPeerAttributesListener(this));
        }
    }

    /**
     * Updates the custom subtitle when the request for load the participants' attributes finishes.
     *
     * @param chatId        identifier of the chat received in the request
     * @param handleList    list of the participants' handles
     */
    public void updateCustomSubtitle(long chatId, MegaHandleList handleList) {
        if (handleList == null || handleList.size() == 0
                || chatId != chatRoom.getChatId() || megaChatApi.getChatRoom(chatId) == null)
            return;

        chatRoom = megaChatApi.getChatRoom(chatId);

        for (int i = 0; i < handleList.size(); i++) {
            chatC.setNonContactAttributesInDB(handleList.get(i));
        }

        setCustomSubtitle();
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
        selectMenuItem = menu.findItem(R.id.cab_menu_select_messages);
        inviteMenuItem = menu.findItem(R.id.cab_menu_invite_chat);
        clearHistoryMenuItem = menu.findItem(R.id.cab_menu_clear_history_chat);
        contactInfoMenuItem = menu.findItem(R.id.cab_menu_contact_info_chat);
        leaveMenuItem = menu.findItem(R.id.cab_menu_leave_chat);
        archiveMenuItem = menu.findItem(R.id.cab_menu_archive_chat);
        muteMenuItem = menu.findItem(R.id.cab_menu_mute_chat);
        unMuteMenuItem = menu.findItem(R.id.cab_menu_unmute_chat);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        logDebug("onPrepareOptionsMenu");

        if (chatRoom != null && !joiningOrLeaving) {
            if (isEnableChatNotifications(chatRoom.getChatId())) {
                unMuteMenuItem.setVisible(false);
                muteMenuItem.setVisible(true);
            } else {
                muteMenuItem.setVisible(false);
                unMuteMenuItem.setVisible(true);
            }

            checkSelectOption();
            callMenuItem.setEnabled(false);
            callMenuItem.setIcon(mutateIcon(this, R.drawable.ic_phone_white, R.color.white_50_opacity));
            if (chatRoom.isGroup()) {
                videoMenuItem.setVisible(false);
            }else{
                videoMenuItem.setEnabled(false);
                videoMenuItem.setIcon(mutateIcon(this, R.drawable.ic_videocam_white, R.color.white_50_opacity));
            }

            if(chatRoom.isPreview() || !isStatusConnected(this, idChat)) {
                muteMenuItem.setVisible(false);
                unMuteMenuItem.setVisible(false);
                leaveMenuItem.setVisible(false);
                clearHistoryMenuItem.setVisible(false);
                inviteMenuItem.setVisible(false);
                contactInfoMenuItem.setVisible(false);
                archiveMenuItem.setVisible(false);
            }else {

                if (megaChatApi != null && (megaChatApi.getNumCalls() <= 0 || (!participatingInACall() && !megaChatApi.hasCallInChatRoom(chatRoom.getChatId())))) {
                    if (!chatRoom.isGroup() || chatRoom.getPeerCount() > 0) {
                        callMenuItem.setEnabled(true);
                        callMenuItem.setIcon(mutateIcon(this, R.drawable.ic_phone_white, R.color.background_chat));
                    }

                    if (chatRoom.isGroup()) {
                        videoMenuItem.setVisible(false);
                    } else {
                        videoMenuItem.setEnabled(true);
                        videoMenuItem.setIcon(mutateIcon(this, R.drawable.ic_videocam_white, R.color.background_chat));
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
            muteMenuItem.setVisible(false);
            unMuteMenuItem.setVisible(false);
            leaveMenuItem.setVisible(false);
            callMenuItem.setVisible(false);
            videoMenuItem.setVisible(false);
            selectMenuItem.setVisible(false);
            clearHistoryMenuItem.setVisible(false);
            inviteMenuItem.setVisible(false);
            contactInfoMenuItem.setVisible(false);
            archiveMenuItem.setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    void ifAnonymousModeLogin(boolean pendingJoin) {
        if (chatC.isInAnonymousMode()) {
            Intent loginIntent = new Intent(this, LoginActivityLollipop.class);
            if (pendingJoin && getIntent() != null && getIntent().getDataString() != null) {
                loginIntent.putExtra(VISIBLE_FRAGMENT,  LOGIN_FRAGMENT);
                loginIntent.setAction(ACTION_JOIN_OPEN_CHAT_LINK);
                loginIntent.setData(Uri.parse(getIntent().getDataString()));
            } else {
                loginIntent.putExtra(VISIBLE_FRAGMENT,  TOUR_FRAGMENT);
            }

            loginIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(loginIntent);
            app.setIsLoggingRunning(true);
        }

        closeChat(true);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        logDebug("onOptionsItemSelected");

        if (app.getStorageState() == STORAGE_STATE_PAYWALL &&
                (item.getItemId() == R.id.cab_menu_call_chat || item.getItemId() == R.id.cab_menu_video_chat)) {
            showOverDiskQuotaPaywallWarning();
            return false;
        }

        if (joiningOrLeaving && item.getItemId() != android.R.id.home) {
            return false;
        }

        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home: {
                if (emojiKeyboard != null) {
                    emojiKeyboard.hideBothKeyboard(this);
                }
                if (fileStorageLayout.isShown()) {
                    hideFileStorage();
                }
                if (handlerEmojiKeyboard != null) {
                    handlerEmojiKeyboard.removeCallbacksAndMessages(null);
                }
                if (handlerKeyboard != null) {
                    handlerKeyboard.removeCallbacksAndMessages(null);
                }
                ifAnonymousModeLogin(false);
                break;
            }
            case R.id.cab_menu_call_chat:{
                if(recordView.isRecordingNow() || canNotStartCall(this, chatRoom)) break;

                startVideo = false;
                if(checkPermissionsCall()){
                    startCall();
                }
                break;
            }
            case R.id.cab_menu_video_chat:{
                logDebug("cab_menu_video_chat");
                if(recordView.isRecordingNow() || canNotStartCall(this, chatRoom)) break;

                startVideo = true;
                if(checkPermissionsCall()){
                    startCall();
                }
                break;
            }
            case R.id.cab_menu_select_messages:
                activateActionMode();
                break;

            case R.id.cab_menu_invite_chat:
                if(recordView.isRecordingNow())
                    break;

                chooseAddParticipantDialog();
                break;

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

            case R.id.cab_menu_mute_chat:
                createMuteNotificationsAlertDialogOfAChat(this, chatRoom.getChatId());
                break;

            case R.id.cab_menu_unmute_chat:
                MegaApplication.getPushNotificationSettingManagement().controlMuteNotificationsOfAChat(this, NOTIFICATIONS_ENABLED, chatRoom.getChatId());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     *Prepare recording
     */
    private void prepareRecording() {
        recordView.playSound(TYPE_START_RECORD);
        stopReproductions();
    }


    /*
     * Start recording
     */
    public void startRecording(){
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
        handlerVisualizer.post(updateVisualizer);
        initRecordingItems(IS_LOW);
        recordingLayout.setVisibility(View.VISIBLE);
    }

    private void initRecordingItems(boolean isLow){
        changeColor(firstBar, isLow);
        changeColor(secondBar, isLow);
        changeColor(thirdBar, isLow);
        changeColor(fourthBar, isLow);
        changeColor(fifthBar, isLow);
        changeColor(sixthBar, isLow);

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
        destroyAudioRecorderElements();
        textChat.requestFocus();
    }

    private void hideRecordingLayout(){
        if(recordingLayout == null || recordingLayout.getVisibility() == View.GONE) return;
        recordingChrono.setText("00:00");
        recordingLayout.setVisibility(View.GONE);
    }

    private void destroyAudioRecorderElements(){
        abandonAudioFocus(audioFocusListener, mAudioManager, request);
        handlerVisualizer.removeCallbacks(updateVisualizer);

        hideRecordingLayout();
        outputFileVoiceNotes = null;
        outputFileName = null;
        setRecordingNow(false);

        if (myAudioRecorder == null) return;
        myAudioRecorder.reset();
        myAudioRecorder.release();
        myAudioRecorder = null;
    }

    /*
     * Cancel recording and reset the audio recorder
     */
    public void cancelRecording() {
        if (!isRecordingNow() || myAudioRecorder == null)
            return;

        hideRecordingLayout();
        handlerVisualizer.removeCallbacks(updateVisualizer);

        try {
            myAudioRecorder.stop();
            myAudioRecorder.reset();
            myAudioRecorder = null;
            abandonAudioFocus(audioFocusListener, mAudioManager, request);
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
        hideRecordingLayout();
        handlerVisualizer.removeCallbacks(updateVisualizer);

        try {
            myAudioRecorder.stop();
            recordView.playSound(TYPE_END_RECORD);
            abandonAudioFocus(audioFocusListener, mAudioManager, request);
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
        disableButton(rlGifButton, gifButton);
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
        enableButton(rlGifButton, gifButton);
    }

    private void enableButton(RelativeLayout layout, ImageButton button){
        logDebug("enableButton");
        layout.setOnClickListener(this);
        button.setOnClickListener(this);
        button.setVisibility(View.VISIBLE);
    }

    /**
     * Method that displays the send icon.
     */
    private void showSendIcon() {
        if(recordView.isRecordingNow())
            return;

        sendIcon.setEnabled(true);
        sendIcon.setImageDrawable(ContextCompat.getDrawable(chatActivity, R.drawable.ic_send_black));
        textChat.setHint(" ");
        setSizeInputText(false);
        sendIcon.setVisibility(View.VISIBLE);
        currentRecordButtonState = 0;
        recordLayout.setVisibility(View.GONE);
        recordButtonLayout.setVisibility(View.GONE);
    }

    /*
     *Record button deactivated or ready to send
     */
    private void recordButtonDeactivated(boolean isDeactivated) {
        if (textChat != null && textChat.getText() != null && !isTextEmpty(textChat.getText().toString()) && isDeactivated) {
            showSendIcon();
        } else {
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
            recordButton.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.accentColor));
        }
    }

    /*
     *Update the record button view depending on the state the recording is in
     */
    private void recordButtonStates(int recordButtonState){
        if (currentRecordButtonState == recordButtonState)
            return;

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
                marginRight = px2dp(14, getOutMetrics());
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
        params.height = px2dp(value, getOutMetrics());
        params.width = px2dp(value, getOutMetrics());
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
        if (recordView == null) return;

        recordView.setRecordingNow(recordingNow);
        if (recordView.isRecordingNow()) {
            recordButtonStates(RECORD_BUTTON_ACTIVATED);
            int screenRotation = getWindowManager().getDefaultDisplay().getRotation();
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
        stopReproductions();
        hideKeyboard();

        if (megaChatApi == null)
            return;

        MegaChatCall callInThisChat = megaChatApi.getChatCall(chatRoom.getChatId());
        if(callInThisChat != null){
            logDebug("There is a call in this chat");
            if (participatingInACall()) {
                long chatIdCallInProgress = getChatCallInProgress();
                if (callInThisChat.isOnHold() || chatIdCallInProgress == chatRoom.getChatId()) {
                    logDebug("I'm participating in the call of this chat");
                    returnCall(this, chatRoom.getChatId());
                    return;
                }

                logDebug("I'm participating in another call from another chat");
                MegaChatCall anotherOnHoldCall = getAnotherOnHoldCall(chatRoom.getChatId());
                if(anotherOnHoldCall != null) {
                    showJoinCallDialog(chatRoom.getChatId(), anotherOnHoldCall);
                }
                return;
            }

            if (canNotJoinCall(this, callInThisChat, chatRoom)) return;

            if (callInThisChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                logDebug("The call in this chat is Ring in");
                addChecksForACall(chatRoom.getChatId(), false);
                MegaApplication.setShowPinScreen(false);
                Intent intent = new Intent(this, ChatCallActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(CHAT_ID, idChat);
                startActivity(intent);
                return;
            }

            if (callInThisChat.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                logDebug("The call in this chat is In progress, but I do not participate");
                addChecksForACall(chatRoom.getChatId(), startVideo);
                megaChatApi.startChatCall(idChat, startVideo, this);
            }

            return;
        }

        if (!participatingInACall()) {
            logDebug("There is not a call in this chat and I am NOT in another call");
            addChecksForACall(chatRoom.getChatId(), startVideo);
            megaChatApi.startChatCall(idChat, startVideo, this);
        }else{
            logDebug("There is not a call in this chat and I am in another call");
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
                    if(preservedMessagesSelected != null && !preservedMessagesSelected.isEmpty()) {
                        for (int i = 0; i < preservedMessagesSelected.size(); i++) {
                            MegaNodeList megaNodeList = preservedMessagesSelected.get(i).getMessage().getMegaNodeList();
                            list.add(megaNodeList);
                        }

                        chatC.prepareForChatDownload(list);
                        preservedMessagesSelected = null;
                    }
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
                    in.putExtra("aBtitle", getString(R.string.send_contacts));
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

        if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
            showOverDiskQuotaPaywallWarning();
            return;
        }

        //Prevent trigger multiple forwarding messages screens in multiple clicks
        if (isForwardingMessage) {
            logDebug("Forwarding message is on going");
            return;
        }

        isForwardingMessage = true;
        storedUnhandledData(messagesSelected);
        checkIfIsNeededToAskForMyChatFilesFolder();
    }

    public void proceedWithAction() {
        if (isForwardingMessage) {
            stopReproductions();
            chatC.prepareAndroidMessagesToForward(preservedMessagesSelected, idChat);
        } else {
            startUploadService();
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

            long[] idMessages = intent.getLongArrayExtra(ID_MESSAGES);
            if (idMessages != null) logDebug("Send " + idMessages.length + " messages");

            long[] chatHandles = intent.getLongArrayExtra(SELECTED_CHATS);
            if (chatHandles != null) logDebug("Send to " + chatHandles.length + " chats");

            long[] contactHandles = intent.getLongArrayExtra(SELECTED_USERS);
            if (contactHandles != null) logDebug("Send to " + contactHandles.length + " contacts");

            if(idMessages != null) {
                ArrayList<MegaChatRoom> chats = new ArrayList<>();
                ArrayList<MegaUser> users = new ArrayList<>();

                if (contactHandles != null && contactHandles.length > 0) {
                    for (int i = 0; i < contactHandles.length; i++) {
                        MegaUser user = megaApi.getContact(MegaApiAndroid.userHandleToBase64(contactHandles[i]));
                        if (user != null) {
                            users.add(user);
                        }
                    }
                    if (chatHandles != null && chatHandles.length > 0 ){
                        for (int i = 0; i < chatHandles.length; i++) {
                            MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatHandles[i]);
                            if (chatRoom != null) {
                                chats.add(chatRoom);
                            }
                        }
                    }
                    CreateChatListener listener = new CreateChatListener(chats, users, idMessages, this, CreateChatListener.SEND_MESSAGES, idChat);

                    if(users != null && !users.isEmpty()) {
                        for (MegaUser user : users) {
                            MegaChatPeerList peers = MegaChatPeerList.createInstance();
                            peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
                            megaChatApi.createChat(false, peers, listener);
                        }
                    }

                }else if (chatHandles != null && chatHandles.length > 0 ){
                    int countChat = chatHandles.length;
                    logDebug("Selected: " + countChat + " chats to send");

                    MultipleForwardChatProcessor forwardChatProcessor = new MultipleForwardChatProcessor(this, chatHandles, idMessages, idChat);
                    forwardChatProcessor.forward(chatRoom);
                }else{
                    logError("Error on sending to chat");
                }
            }
        }
        else if (requestCode == TAKE_PHOTO_CODE && resultCode == RESULT_OK) {
            if (resultCode == Activity.RESULT_OK) {
                logDebug("TAKE_PHOTO_CODE ");
                onCaptureImageResult();

            } else {
                logError("TAKE_PHOTO_CODE--->ERROR!");
            }

        } else if (requestCode == REQUEST_CODE_SEND_LOCATION && resultCode == RESULT_OK) {
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
        } else if (requestCode == REQUEST_CODE_PICK_GIF && resultCode == RESULT_OK && intent != null) {
            sendGiphyMessageFromGifData(intent.getParcelableExtra(GIF_DATA));
        } else{
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

                    checkIfServiceCanStart(intent);
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

    private void setSizeInputText(boolean isEmpty){
        textChat.setMinLines(1);
        if(isEmpty){
            textChat.setMaxLines(1);
        }else {
            int maxLines;
            if (textChat.getMaxLines() < MAX_LINES_INPUT_TEXT && textChat.getLineCount() == textChat.getMaxLines()) {
                maxLines = textChat.getLineCount() + 1;
            } else {
                maxLines = MAX_LINES_INPUT_TEXT;
            }
            textChat.setEllipsize(null);
            textChat.setMaxLines(maxLines);
        }
    }
    private void endCall(long chatHang){
        logDebug("chatHang: " + chatHang);
        if(megaChatApi!=null){
            megaChatApi.hangChatCall(chatHang, this);
        }
    }

    /**
     * Dialog to allow joining a group call when another one is active.
     *
     * @param callInThisChat  The chat ID of the group call.
     * @param anotherCall The in progress call.
     */
    public void showJoinCallDialog(long callInThisChat, MegaChatCall anotherCall) {
        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.join_call_dialog, null);
        final Button holdJoinButton = dialoglayout.findViewById(R.id.hold_join_button);
        final Button endJoinButton = dialoglayout.findViewById(R.id.end_join_button);
        final Button cancelButton = dialoglayout.findViewById(R.id.cancel_button);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setView(dialoglayout);
        dialogCall = builder.create();
        isJoinCallDialogShown = true;
        dialogCall.show();

        View.OnClickListener clickListener = v -> {
            switch (v.getId()) {
                case R.id.hold_join_button:
                    if(anotherCall.isOnHold()){
                        MegaChatCall callInChat = megaChatApi.getChatCall(callInThisChat);
                        if(callInChat != null){
                            if (callInChat.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                                addChecksForACall(callInThisChat, false);
                                megaChatApi.answerChatCall(callInThisChat, false, ChatActivityLollipop.this);
                            } else if (callInChat.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                                megaChatApi.startChatCall(idChat, false, ChatActivityLollipop.this);
                            }
                        }
                    }else{
                        megaChatApi.setCallOnHold(anotherCall.getChatid(), true, ChatActivityLollipop.this);
                    }
                    break;

                case R.id.end_join_button:
                    endCall(anotherCall.getChatid());
                    break;

                case R.id.cancel_button:
                    break;

            }
            if (dialogCall != null) {
                dialogCall.dismiss();
            }
        };

        dialogCall.setOnDismissListener(dialog -> isJoinCallDialogShown = false);
        holdJoinButton.setOnClickListener(clickListener);
        endJoinButton.setOnClickListener(clickListener);
        cancelButton.setOnClickListener(clickListener);
    }

    public void controlCamera(){
        stopReproductions();
        openCameraApp();
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

        androidx.appcompat.app.AlertDialog.Builder builder;
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

                        setJoiningOrLeaving(getString(R.string.leaving_label));
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

        androidx.appcompat.app.AlertDialog.Builder builder;
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

    @Override
    public void onBackPressed() {
        logDebug("onBackPressed");
        retryConnectionsAndSignalPresence();
        if (emojiKeyboard != null && emojiKeyboard.getEmojiKeyboardShown()) {
            emojiKeyboard.hideBothKeyboard(this);
        } else if (fileStorageLayout.isShown()) {
            hideFileStorage();
        } else {
            if (handlerEmojiKeyboard != null) {
                handlerEmojiKeyboard.removeCallbacksAndMessages(null);
            }
            if (handlerKeyboard != null) {
                handlerKeyboard.removeCallbacksAndMessages(null);
            }
            ifAnonymousModeLogin(false);
        }
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");

        if (joiningOrLeaving && v.getId() != R.id.home)
            return;
        MegaChatCall callInThisChat;
        switch (v.getId()) {
            case R.id.home:
                onBackPressed();
                break;

            case R.id.call_on_hold_layout:
                callInThisChat = megaChatApi.getChatCall(chatRoom.getChatId());
                if (callInThisChat == null)
                    break;

                if (callInThisChat.getStatus() != MegaChatCall.CALL_STATUS_RING_IN &&
                        ((callInThisChat.getStatus() >= MegaChatCall.CALL_STATUS_REQUEST_SENT &&
                                callInThisChat.getStatus() <= MegaChatCall.CALL_STATUS_IN_PROGRESS) ||
                                callInThisChat.getStatus() == MegaChatCall.CALL_STATUS_RECONNECTING)) {
                    if (callInThisChat.isOnHold()) {
                        returnCall(this, chatRoom.getChatId());
                    }

                } else if (chatRoom.isGroup()) {
                    MegaChatCall anotherCall = getAnotherActiveCall(chatRoom.getChatId());
                    if (anotherCall == null) {
                        anotherCall = getAnotherOnHoldCall(chatRoom.getChatId());
                    }
                    if (anotherCall != null) {
                        showJoinCallDialog(callInThisChat.getChatid(), anotherCall);
                    }
                }
                break;

            case R.id.call_in_progress_layout:
                if (chatIdBanner == MEGACHAT_INVALID_HANDLE)
                    break;

                MegaChatCall callBanner = megaChatApi.getChatCall(chatIdBanner);
                if (callBanner == null || callBanner.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT ||
                        callBanner.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                    startVideo = false;
                    if (checkPermissionsCall()) {
                        startCall();
                    }
                } else {
                    returnCall(this, chatIdBanner);
                }
                break;

            case R.id.send_message_icon_chat:
                writingLayout.setClickable(false);
                String text = textChat.getText().toString();
                if(text.trim().isEmpty()) break;
                if (editingMessage) {
                    editMessage(text);
                    finishMultiselectionMode();
                    checkActionMode();
                } else {
                    sendMessage(text);
                }
                textChat.setText("", TextView.BufferType.EDITABLE);
                break;

            case R.id.keyboard_twemoji_chat:
            case R.id.rl_keyboard_twemoji_chat:
                logDebug("keyboard_icon_chat");
                hideFileStorage();
                if(emojiKeyboard==null) break;
                changeKeyboard(keyboardTwemojiButton);
                break;


            case R.id.media_icon_chat:
            case R.id.rl_media_icon_chat:
                logDebug("media_icon_chat");
                if (recordView.isRecordingNow()) break;
                hideKeyboard();
                if(isNecessaryDisableLocalCamera() != -1){
                    showConfirmationOpenCamera(this, ACTION_TAKE_PICTURE, false);
                    break;
                }
                controlCamera();
                break;

            case R.id.pick_file_storage_icon_chat:
            case R.id.rl_pick_file_storage_icon_chat:
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

            case R.id.rl_gif_chat:
            case R.id.gif_chat:
                startActivityForResult(
                        new Intent(this, GiphyPickerActivity.class), REQUEST_CODE_PICK_GIF);
                break;

            case R.id.toolbar_chat:
                logDebug("toolbar_chat");
                if(recordView.isRecordingNow()) break;

                showGroupInfoActivity();
                break;

            case R.id.message_jump_layout:
                goToEnd();
                break;

            case R.id.pick_attach_chat:
            case R.id.rl_attach_icon_chat:
                logDebug("Show attach bottom sheet");
                hideKeyboard();
                showSendAttachmentBottomSheet();
                break;

            case R.id.join_button:
                if (chatC.isInAnonymousMode()) {
                    ifAnonymousModeLogin(true);
                } else {
                    setJoiningOrLeaving(getString(R.string.joining_label));
                    megaChatApi.autojoinPublicChat(idChat, this);
                }
                break;

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

    /**
     * Sends a Giphy message from MegaChatGiphy object.
     *
     * @param giphy Giphy to send.
     */
    public void sendGiphyMessageFromMegaChatGiphy(MegaChatGiphy giphy) {
        if (giphy == null) {
            logWarning("MegaChatGiphy is null");
            return;
        }
        sendGiphyMessage(giphy.getMp4Src(), giphy.getWebpSrc(), giphy.getMp4Size(), giphy.getWebpSize(),
                giphy.getWidth(), giphy.getHeight(), giphy.getTitle());

    }

    /**
     * Sends a Giphy message from GifData object.
     *
     * @param gifData   Giphy to send.
     */
    public void sendGiphyMessageFromGifData(GifData gifData) {
        if (gifData == null) {
            logWarning("GifData is null");
            return;
        }

        sendGiphyMessage(gifData.getMp4Url(), gifData.getWebpUrl(), gifData.getMp4Size(), gifData.getWebpSize(),
                gifData.getWidth(), gifData.getHeight(), gifData.getTitle());
    }

    /**
     * Sends a Giphy message.
     *
     * @param srcMp4    Source location of the mp4
     * @param srcWebp   Source location of the webp
     * @param sizeMp4   Size in bytes of the mp4
     * @param sizeWebp  Size in bytes of the webp
     * @param width     Width of the giphy
     * @param height    Height of the giphy
     * @param title     Title of the giphy
     */
    public void sendGiphyMessage(String srcMp4, String srcWebp, long sizeMp4, long sizeWebp, int width, int height, String title) {
        MegaChatMessage giphyMessage = megaChatApi.sendGiphy(idChat, getGiphySrc(srcMp4), getGiphySrc(srcWebp),
                sizeMp4, sizeWebp, width, height, title);
        if (giphyMessage == null) return;

        AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(giphyMessage);
        sendMessageToUI(androidMsgSent);
    }

    public void hideNewMessagesLayout(){
        logDebug("hideNewMessagesLayout");

        int position = positionNewMessagesLayout;

        positionNewMessagesLayout = INVALID_VALUE;
        lastIdMsgSeen = MEGACHAT_INVALID_HANDLE;
        generalUnreadCount = 0;
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

            }else if(androidMsgSent.getMessage() != null) {
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

            if (adapter == null) {
                createAdapter();
            } else {
                //Increment header position
                index++;
                adapter.addMessage(messages, index);
                mLayoutManager.scrollToPositionWithOffset(index, scaleHeightPx(infoToShow == AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL ? 50 : 20, getOutMetrics()));
            }
        }
        else{
            logError("Error sending message!");
        }
    }

    /**
     * Method for copying a message.
     *
     * @param message The message.
     * @return The copied text.
     */
    public String copyMessage(AndroidMegaChatMessage message) {
        return chatC.createSingleManagementString(message, chatRoom);
    }

    /**
     * Method for copying a text to the clipboard.
     *
     * @param text The text.
     */
    public void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
        clipboard.setPrimaryClip(clip);
        showSnackbar(SNACKBAR_TYPE, getString(R.string.messages_copied_clipboard), -1);
    }


    public void editMessage(ArrayList<AndroidMegaChatMessage> messagesSelected) {
        if (messagesSelected.isEmpty() || messagesSelected.get(0) == null)
            return;

        editingMessage = true;
        MegaChatMessage msg = messagesSelected.get(0).getMessage();
        MegaChatContainsMeta meta = msg.getContainsMeta();
        messageToEdit = msg;

        if (msg.getType() == MegaChatMessage.TYPE_CONTAINS_META && meta != null && meta.getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION) {
            sendLocation();
            finishMultiselectionMode();
            checkActionMode();
        } else {
            textChat.setText(messageToEdit.getContent());
            textChat.setSelection(textChat.getText().length());
        }
    }

    public void editMessage(String text) {
        if (messageToEdit.getContent().equals(text)) return;
        MegaChatMessage msgEdited = megaChatApi.editMessage(idChat,
                messageToEdit.getMsgId() != MEGACHAT_INVALID_HANDLE ? messageToEdit.getMsgId() : messageToEdit.getTempId(),
                text);

        if (msgEdited != null) {
            logDebug("Edited message: status: " + msgEdited.getStatus());
            AndroidMegaChatMessage androidMsgEdited = new AndroidMegaChatMessage(msgEdited);
            modifyMessageReceived(androidMsgEdited, false);
        } else {
            logWarning("Message cannot be edited!");
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_editing_message), MEGACHAT_INVALID_HANDLE);
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
        if (!adapter.isMultipleSelect()) {
            adapter.setMultipleSelect(true);
            actionMode = startSupportActionMode(new ActionBarCallBack());
            updateActionModeTitle();
        }
    }

    private void reDoTheSelectionAfterRotation() {
        if (recoveredSelectedPositions == null || adapter == null)
            return;

        if (recoveredSelectedPositions.size() > 0) {
            activateActionMode();

            for (int position : recoveredSelectedPositions) {
                AndroidMegaChatMessage msg = adapter.getMessageAtMessagesPosition(position);
                if(msg != null) {
                    adapter.toggleSelection(msg.getMessage().getMsgId());
                }
            }
        }

        updateActionModeTitle();
    }

    public void activateActionModeWithItem(int positionInAdapter) {
        logDebug("activateActionModeWithItem");

        activateActionMode();
        if (adapter.isMultipleSelect()) {
            itemClick((positionInAdapter + 1), null);
        }
    }

    //Multiselect
    private class  ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            ArrayList<AndroidMegaChatMessage> messagesSelected = adapter.getSelectedMessages();

            if (app.getStorageState() == STORAGE_STATE_PAYWALL) {
                showOverDiskQuotaPaywallWarning();
                return false;
            }

            switch(item.getItemId()){
                case R.id.chat_cab_menu_edit:
                    logDebug("Edit text");
                    editMessage(messagesSelected);
                    break;

                case R.id.chat_cab_menu_forward:
                    logDebug("Forward message");
                    forwardMessages(messagesSelected);
                    break;

                case R.id.chat_cab_menu_copy:
                    finishMultiselectionMode();
                    String text = "";
                    if (messagesSelected.size() == 1) {
                        text = copyMessage(messagesSelected.get(0));
                    } else {
                        text = copyMessages(messagesSelected);
                    }
                    copyToClipboard(text);
                    break;
                case R.id.chat_cab_menu_delete:
                    finishMultiselectionMode();
                    //Delete
                    showConfirmationDeleteMessages(messagesSelected, chatRoom);
                    break;

                case R.id.chat_cab_menu_download:
                    logDebug("chat_cab_menu_download ");
                    finishMultiselectionMode();

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

                case R.id.chat_cab_menu_import:
                    finishMultiselectionMode();
                    chatC.importNodesFromAndroidMessages(messagesSelected);
                    break;

                case R.id.chat_cab_menu_offline:
                    finishMultiselectionMode();
                    if (!checkPermissionWriteStorage(REQUEST_WRITE_STORAGE_OFFLINE)) {
                        preservedMessagesSelected = messagesSelected;
                        return false;
                    }
                    chatC.saveForOfflineWithAndroidMessages(messagesSelected, chatRoom);
                    break;

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
            logDebug("onCreateActionMode");

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
            editingMessage = false;
            recoveredSelectedPositions = null;
            clearSelections();
            changeStatusBarColorActionMode(chatActivity, getWindow(), handler, 0);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            logDebug("onPrepareActionMode");
            List<AndroidMegaChatMessage> selected = adapter.getSelectedMessages();
            if(selected.size() == 0){
                menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                importIcon.setVisible(false);

            }else {

                if((chatRoom.getOwnPrivilege()==MegaChatRoom.PRIV_RM||chatRoom.getOwnPrivilege()==MegaChatRoom.PRIV_RO) && !chatRoom.isPreview()){
                    logDebug("Chat without permissions || without preview");

                    boolean showCopy = true;
                    for (int i = 0; i < selected.size(); i++) {
                        MegaChatMessage msg = selected.get(i).getMessage();
                        if (msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT
                                || msg.getType() == MegaChatMessage.TYPE_CONTACT_ATTACHMENT
                                || msg.getType() == MegaChatMessage.TYPE_VOICE_CLIP
                                || (msg.getType() == MegaChatMessage.TYPE_CONTAINS_META && msg.getContainsMeta() != null
                                && (msg.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION
                                || msg.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GIPHY))) {
                            showCopy = false;
                            break;
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
                        if(hasMessagesRemoved(selected.get(0).getMessage())){
                            menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_download).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_offline).setVisible(false);
                            importIcon.setVisible(false);

                        }else if(selected.get(0).isUploading()){
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

                            if (messageSelected.getType() == MegaChatMessage.TYPE_CONTAINS_META
                                    && messageSelected.getContainsMeta() != null
                                    && (messageSelected.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION
                                        || messageSelected.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GIPHY)) {
                                menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                            } else {
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
                        boolean isRemoved = false;

                        for(int i=0; i<selected.size();i++) {

                            if(hasMessagesRemoved(selected.get(i).getMessage())){
                                isRemoved = true;
                                break;
                            }

                            if (!isUploading) {
                                if (selected.get(i).isUploading()) {
                                    isUploading = true;
                                }
                            }

                            MegaChatMessage msg = selected.get(i).getMessage();

                            if (showCopy
                                    && (msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT
                                    || msg.getType() == MegaChatMessage.TYPE_CONTACT_ATTACHMENT
                                    || msg.getType() == MegaChatMessage.TYPE_VOICE_CLIP
                                    || (msg.getType() == MegaChatMessage.TYPE_CONTAINS_META && msg.getContainsMeta() != null
                                        && (msg.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION
                                            || msg.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GIPHY)))) {
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

                        if (isUploading || isRemoved) {
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

    public void updateActionModeTitle() {
        try {
            if (actionMode != null) {
                if (adapter.getSelectedItemCount() == 0) {
                    actionMode.setTitle(getString(R.string.select_message_title).toUpperCase());
                } else {
                    actionMode.setTitle(adapter.getSelectedItemCount() + "");
                }
                actionMode.invalidate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            logError("Invalidate error", e);
        }
    }

    /*
     * Disable selection
     */
    public void hideMultipleSelect() {
        if (adapter != null) {
            adapter.setMultipleSelect(false);
        }

        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public void finishMultiselectionMode() {
        clearSelections();
        hideMultipleSelect();
    }

    private void checkActionMode(){
        if (adapter.isMultipleSelect() && actionMode != null) {
            actionMode.invalidate();
        }else{
            editingMessage = false;
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

            new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
        }
    }

    /**
     * Method for displaying feedback dialogue in a message.
     *
     * @param chatId      The chat ID.
     * @param megaMessage The message.
     * @param reaction    The reaction.
     */
    public void openInfoReactionBottomSheet(long chatId, AndroidMegaChatMessage megaMessage, String reaction) {
        if (chatRoom.getChatId() != chatId)
            return;

        showReactionBottomSheet(megaMessage, messages.indexOf(megaMessage), reaction);
    }

    public void openReactionBottomSheet(long chatId, AndroidMegaChatMessage megaMessage) {
        if (chatRoom.getChatId() != chatId)
            return;

        int positionInMessages = messages.indexOf(megaMessage);
        showReactionBottomSheet(megaMessage, positionInMessages, null);
    }

    public void itemLongClick(int positionInAdapter) {
        int positionInMessages = positionInAdapter - 1;
        if (positionInMessages >= messages.size())
            return;

        AndroidMegaChatMessage m = messages.get(positionInMessages);
        if (adapter.isMultipleSelect() || m == null || m.isUploading() || m.getMessage().getStatus() == MegaChatMessage.STATUS_SERVER_REJECTED || m.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL)
            return;

        int type = m.getMessage().getType();
        switch (type) {
            case MegaChatMessage.TYPE_NODE_ATTACHMENT:
            case MegaChatMessage.TYPE_CONTACT_ATTACHMENT:
            case MegaChatMessage.TYPE_VOICE_CLIP:
            case MegaChatMessage.TYPE_NORMAL:
                showGeneralChatMessageBottomSheet(m, positionInMessages);
                break;

            case MegaChatMessage.TYPE_CONTAINS_META:
                MegaChatContainsMeta meta = m.getMessage().getContainsMeta();
                if (meta == null || meta.getType() == MegaChatContainsMeta.CONTAINS_META_INVALID)
                    return;

                if (meta.getType() == MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW
                        || meta.getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION
                        || meta.getType() == MegaChatContainsMeta.CONTAINS_META_GIPHY) {
                    showGeneralChatMessageBottomSheet(m, positionInMessages);
                }
                break;
        }
    }

    private boolean isSelectableMessage(AndroidMegaChatMessage message) {
        if (message.getMessage().getStatus() == MegaChatMessage.STATUS_SERVER_REJECTED || message.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL)
            return false;

        int type = message.getMessage().getType();
        switch (type) {
            case MegaChatMessage.TYPE_NODE_ATTACHMENT:
            case MegaChatMessage.TYPE_CONTACT_ATTACHMENT:
            case MegaChatMessage.TYPE_VOICE_CLIP:
            case MegaChatMessage.TYPE_NORMAL:
            case MegaChatMessage.TYPE_CONTAINS_META:
            case MegaChatMessage.TYPE_PUBLIC_HANDLE_CREATE:
            case MegaChatMessage.TYPE_PUBLIC_HANDLE_DELETE:
            case MegaChatMessage.TYPE_SET_PRIVATE_MODE:
                return true;
            default:
                return false;
        }
    }

    public void itemClick(int positionInAdapter, int [] screenPosition) {
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
                            if (isSelectableMessage(m)) {
                                adapter.toggleSelection(m.getMessage().getMsgId());
                                List<AndroidMegaChatMessage> messages = adapter.getSelectedMessages();
                                if (!messages.isEmpty()) {
                                    updateActionModeTitle();
                                }
                            }
                        }
                    }
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
                                            showGeneralChatMessageBottomSheet(m, positionInMessages);
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

                                        String localPath = getLocalFile(this, node.getName(), node.getSize());

                                        if (localPath != null){
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
                                                showGeneralChatMessageBottomSheet(m, positionInMessages);
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

                                        String localPath = getLocalFile(this, node.getName(), node.getSize());

                                        if (localPath != null){
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
                                            showGeneralChatMessageBottomSheet(m, positionInMessages);
                                        }
                                        overridePendingTransition(0,0);
                                    }
                                    else{
                                        logDebug("NOT Image, pdf, audio or video - show node attachment panel for one node");
                                        showGeneralChatMessageBottomSheet(m, positionInMessages);
                                    }
                                }
                                else{
                                    logDebug("show node attachment panel");
                                    showGeneralChatMessageBottomSheet(m, positionInMessages);
                                }
                            }
                            else if(m.getMessage().getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){
                                logDebug("TYPE_CONTACT_ATTACHMENT");
                                logDebug("show contact attachment panel");
                                if (isOnline(this)) {
                                    if (!chatC.isInAnonymousMode() && m != null) {
                                        if (m.getMessage().getUsersCount() == 1) {
                                            showGeneralChatMessageBottomSheet(m, positionInMessages);
                                        }else{
                                            showGeneralChatMessageBottomSheet(m, positionInMessages);
                                        }
                                    }
                                }
                                else{
                                    //No shown - is not possible to know is it already contact or not - megaApi not working
                                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                                }
                            }
                            else if (m.getMessage().getType() == MegaChatMessage.TYPE_CONTAINS_META) {
                                logDebug("TYPE_CONTAINS_META");
                                MegaChatContainsMeta meta = m.getMessage().getContainsMeta();
                                if (meta == null || meta.getType() == MegaChatContainsMeta.CONTAINS_META_INVALID)
                                    return;
                                String url = null;
                                if (meta.getType() == MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW) {
                                    url = meta.getRichPreview().getUrl();

                                    if (isMEGALinkAndRequiresTransferSession(this, url)) {
                                        return;
                                    }
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

                            } else if(m.getMessage().getType() == MegaChatMessage.TYPE_NORMAL ){
                                logDebug("TYPE_NORMAL");
                                AndroidMegaRichLinkMessage richLinkMessage = m.getRichLinkMessage();

                                if(richLinkMessage == null){
                                    showGeneralChatMessageBottomSheet(m, positionInMessages);
                                }else{
                                    String url = richLinkMessage.getUrl();
                                    if (richLinkMessage.isChat()) {
                                        loadChatLink(url);
                                    } else {
                                        openMegaLink(url, richLinkMessage.getNode() != null ? richLinkMessage.getNode().isFile() : richLinkMessage.isFile());
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
        if (adapter != null) {
            adapter.updateChatRoom(chatRoom);
        }

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
                joiningOrLeaving = false;
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
            updateTitle();
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
                    usersTyping = new ArrayList<>();
                    usersTypingSync = Collections.synchronizedList(usersTyping);
                }

                //Find if any notification arrives previously
                if(usersTypingSync.size()<=0){
                    logDebug("No more users writing");
                    MegaChatParticipant participantTyping = new MegaChatParticipant(userHandleTyping);
                    UserTyping currentUserTyping = new UserTyping(participantTyping);

                    String nameTyping = chatC.getParticipantFirstName(userHandleTyping);

                    logDebug("userHandleTyping: " + userHandleTyping);

                    if (isTextEmpty(nameTyping)) {
                        nameTyping = getString(R.string.transfer_unknown);
                    }

                    participantTyping.setFirstName(nameTyping);

                    userTypingTimeStamp = System.currentTimeMillis()/1000;
                    currentUserTyping.setTimeStampTyping(userTypingTimeStamp);

                    usersTypingSync.add(currentUserTyping);

                    String userTyping =  getResources().getQuantityString(R.plurals.user_typing, 1, toCDATA(usersTypingSync.get(0).getParticipantTyping().getFirstName()));
                    userTyping = userTyping.replace("[A]", "<font color=\'#8d8d94\'>");
                    userTyping = userTyping.replace("[/A]", "</font>");
                    userTypingText.setText(HtmlCompat.fromHtml(userTyping, HtmlCompat.FROM_HTML_MODE_LEGACY));

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

                        String nameTyping = chatC.getParticipantFirstName(userHandleTyping);
                        if (isTextEmpty(nameTyping)) {
                            nameTyping = getString(R.string.transfer_unknown);
                        }

                        participantTyping.setFirstName(nameTyping);

                        userTypingTimeStamp = System.currentTimeMillis()/1000;
                        currentUserTyping.setTimeStampTyping(userTypingTimeStamp);

                        usersTypingSync.add(currentUserTyping);

                        //Show the notification
                        String userTyping;
                        int size = usersTypingSync.size();
                        switch (size) {
                            case 1:
                                userTyping = getResources().getQuantityString(R.plurals.user_typing, 1, usersTypingSync.get(0).getParticipantTyping().getFirstName());
                                userTyping = toCDATA(userTyping);
                                break;

                            case 2:
                                userTyping = getResources().getQuantityString(R.plurals.user_typing, 2, usersTypingSync.get(0).getParticipantTyping().getFirstName() + ", " + usersTypingSync.get(1).getParticipantTyping().getFirstName());
                                userTyping = toCDATA(userTyping);
                                break;

                            default:
                                String names = usersTypingSync.get(0).getParticipantTyping().getFirstName() + ", " + usersTypingSync.get(1).getParticipantTyping().getFirstName();
                                userTyping = String.format(getString(R.string.more_users_typing), toCDATA(names));
                                break;
                        }

                        userTyping = userTyping.replace("[A]", "<font color=\'#8d8d94\'>");
                        userTyping = userTyping.replace("[/A]", "</font>");

                        userTypingText.setText(HtmlCompat.fromHtml(userTyping, HtmlCompat.FROM_HTML_MODE_LEGACY));
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

        if (msg != null) {
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
                numberToLoad--;
                return;
            }

            if(msg.isEdited()){
                logDebug("EDITED MESSAGE!!!!");
            }

            if(msg.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT) {
                logDebug("TYPE_REVOKE_NODE_ATTACHMENT MESSAGE!!!!");
                numberToLoad--;
                return;
            }

            checkMegaLink(msg);

            if(msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){
                logDebug("TYPE_NODE_ATTACHMENT MESSAGE!!!!");
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
                                    numberToLoad--;
                                    return;
                                }
                            }
                        }
                    }

                    int returnValue = modifyMessageReceived(androidMsg, true);
                    if(returnValue!=-1){
                        logDebug("Message " + returnValue + " modified!");
                        numberToLoad--;
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
                    numberToLoad--;
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
            if (!bufferSending.isEmpty()) {
                bufferMessages.addAll(bufferSending);
                bufferSending.clear();
            }

            if (stateHistory == MegaChatApi.SOURCE_ERROR) {
                logDebug("SOURCE_ERROR: wait to CHAT ONLINE connection");
                retryHistory = true;
            } else if (thereAreNotMoreMessages()) {
                logDebug("SOURCE_NONE: there are no more messages");
                fullHistoryReceivedOnLoad();
            } else if (bufferMessages.size() == NUMBER_MESSAGES_TO_LOAD) {
                allMessagesRequestedAreLoaded();
            } else {
                long pendingMessagesCount = numberToLoad - bufferMessages.size();
                if (pendingMessagesCount > 0) {
                    logDebug("Fewer messages received (" + bufferMessages.size() + ") than asked (" + NUMBER_MESSAGES_TO_LOAD + "): ask for the rest of messages (" + pendingMessagesCount + ")");
                    askForMoreMessages(pendingMessagesCount);

                    if (thereAreNotMoreMessages()) {
                        logDebug("SOURCE_NONE: there are no more messages");
                        fullHistoryReceivedOnLoad();
                    }
                } else {
                    allMessagesRequestedAreLoaded();
                }
            }
        }
        logDebug("END onMessageLoaded - messages.size=" + messages.size());
    }

    private void allMessagesRequestedAreLoaded() {
        logDebug("All the messages asked are loaded");
        long messagesLoadedCount = bufferMessages.size() + messages.size();
        fullHistoryReceivedOnLoad();

        if (messagesLoadedCount < Math.abs(generalUnreadCount) && messagesLoadedCount < MAX_NUMBER_MESSAGES_TO_LOAD_NOT_SEEN) {
            askForMoreMessages();
        }
    }

    public boolean thereAreNotMoreMessages() {
        return stateHistory == MegaChatApi.SOURCE_NONE;
    }

    /**
     * Initiates fetching 32 messages more of the current ChatRoom.
     */
    private void askForMoreMessages() {
        askForMoreMessages(NUMBER_MESSAGES_TO_LOAD);
    }

    /**
     * Initiates fetching some messages more of the current ChatRoom.
     *
     * @param messagesCount number of messages to be fetched
     */
    private void askForMoreMessages(long messagesCount) {
        isLoadingHistory = true;
        numberToLoad = messagesCount;
        stateHistory = megaChatApi.loadMessages(idChat, (int) numberToLoad);
        getMoreHistory = false;
    }

    /**
     * Updates the loaded messages in the adapter when all the messages have been received.
     */
    public void fullHistoryReceivedOnLoad() {
        logDebug("fullHistoryReceivedOnLoad");

        isLoadingHistory = false;
        isOpeningChat = false;

        if (!bufferMessages.isEmpty()) {
            logDebug("Buffer size: " + bufferMessages.size());
            loadBufferMessages();

            if (lastSeenReceived && positionToScroll > 0 && positionToScroll < messages.size()) {
                logDebug("Last message seen received");
                //Find last message
                int positionLastMessage = -1;
                for (int i = messages.size() - 1; i >= 0; i--) {
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

                while (message.getMessage().getUserHandle() == megaChatApi.getMyUserHandle()) {
                    lastIdMsgSeen = message.getMessage().getMsgId();
                    positionLastMessage = positionLastMessage + 1;
                    message = messages.get(positionLastMessage);
                }

                scrollToMessage(isTurn ? -1 : lastIdMsgSeen);
            }

            setLastMessageSeen();
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

        if (!msg.isManagementMessage() || msg.getType() == MegaChatMessage.TYPE_CALL_ENDED) {
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
    public void onReactionUpdate(MegaChatApiJava api, long msgid, String reaction, int count) {
        MegaChatMessage message = api.getMessage(idChat, msgid);
        if (adapter == null || message == null) {
            logWarning("Message not found");
            return;
        }

        adapter.checkReactionUpdated(idChat, message, reaction, count);

        if (bottomSheetDialogFragment != null && bottomSheetDialogFragment.isAdded() && bottomSheetDialogFragment instanceof InfoReactionsBottomSheet) {
            ((InfoReactionsBottomSheet) bottomSheetDialogFragment).changeInReactionReceived(msgid, idChat, reaction, count);
        }
    }

    @Override
    public void onMessageUpdate(MegaChatApiJava api, MegaChatMessage msg) {
        logDebug("msgID "+ msg.getMsgId());
        logDebug("onMessageUpdate ");
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
            logDebug("One attachment revoked, modify message");
            resultModify = modifyMessageReceived(androidMsg, false);
            if (resultModify == -1) {
                logDebug("Modify result is -1");
                int firstIndexShown = messages.get(0).getMessage().getMsgIndex();
                logDebug("The first index is: " + firstIndexShown + " the index of the updated message: " + msg.getMsgIndex());
                if (firstIndexShown <= msg.getMsgIndex()) {
                    logDebug("The message should be in the list");
                    if (msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT) {

                        logDebug("Node attachment message not in list -> append");
                        AndroidMegaChatMessage msgToAppend = new AndroidMegaChatMessage(msg);
                        reinsertNodeAttachmentNoRevoked(msgToAppend);
                    }
                } else {
                    if (messages.size() < NUMBER_MESSAGES_BEFORE_LOAD) {
                        logDebug("Show more message - add to the list");
                        if (msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT) {

                            logDebug("Node attachment message not in list -> append");
                            AndroidMegaChatMessage msgToAppend = new AndroidMegaChatMessage(msg);
                            reinsertNodeAttachmentNoRevoked(msgToAppend);
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
        if (adapter == null || !adapter.isMultipleSelect())
            return;

        finishMultiselectionMode();
    }

    @Override
    public void onHistoryReloaded(MegaChatApiJava api, MegaChatRoom chat) {
        logDebug("onHistoryReloaded");
        cleanBuffers();
        invalidateOptionsMenu();
        logDebug("Load new history");

        long unread = chatRoom.getUnreadCount();
        generalUnreadCount = unread;
        lastSeenReceived = unread == 0;

        if (unread == 0) {
            lastIdMsgSeen = MEGACHAT_INVALID_HANDLE;
            logDebug("loadMessages unread is 0");
        } else {
            lastIdMsgSeen = megaChatApi.getLastMessageSeenId(idChat);
            if (lastIdMsgSeen != -1) {
                logDebug("Id of last message seen: " + lastIdMsgSeen);
            } else {
                logError("Error the last message seen shouldn't be NULL");
            }
        }
    }

    public void deleteMessage(MegaChatMessage msg, boolean rejected){
        int indexToChange = -1;

        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        // Iterate in reverse.
        while(itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();
            logDebug("Index: " + itr.nextIndex());

            if(!messageToCheck.isUploading()){
                if (rejected) {
                    if (messageToCheck.getMessage().getTempId() == msg.getTempId()) {
                        indexToChange = itr.nextIndex();
                        break;
                    }
                } else {
                    if (messageToCheck.getMessage().getMsgId() == msg.getMsgId()
                        || (msg.getTempId() != -1
                        && messageToCheck.getMessage().getTempId() == msg.getTempId())) {
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
                    mLayoutManager.scrollToPositionWithOffset(indexToScroll,scaleHeightPx(20, getOutMetrics()));

                }
            }
        }
        else{
            logError("Error, id pending message message not found!!");
        }
        logDebug("Index modified: " + indexToChange);
        return indexToChange;
    }

    /**
     * Checks on the provided list if the message to update exists.
     * If so, returns its index on list.
     *
     * @param msg           The updated AndroidMegaChatMessage.
     * @param checkTempId   True if has to check the temp id instead of final id.
     * @param itr           ListIterator containing the list of messages to check.
     * @return The index to change if successful, INVALID_POSITION otherwise.
     */
    private int getIndexToUpdate(AndroidMegaChatMessage msg, boolean checkTempId, ListIterator<AndroidMegaChatMessage> itr) {
        // Iterate in reverse.
        while (itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();

            if (!messageToCheck.isUploading()) {
                logDebug("Checking with Msg ID: " + messageToCheck.getMessage().getMsgId()
                        + " and Msg TEMP ID: " + messageToCheck.getMessage().getTempId());

                if (checkTempId && messageToCheck.getMessage().getTempId() == msg.getMessage().getTempId()) {
                    logDebug("Modify received message with idTemp");
                    return itr.nextIndex();
                } else if (messageToCheck.getMessage().getMsgId() == msg.getMessage().getMsgId()) {
                    logDebug("modifyMessageReceived");
                    return itr.nextIndex();
                }
            } else {
                logDebug("This message is uploading");
            }
        }

        return INVALID_POSITION;
    }

    /**
     * Modifies a message on UI (messages list and adapter), on bufferMessages list
     * or on bufferSending list, if it has been already loaded.
     *
     * @param msg           The updated AndroidMegaChatMessage.
     * @param checkTempId   True if has to check the temp id instead of final id.
     * @return The index to change if successful, INVALID_POSITION otherwise.
     */
    public int modifyMessageReceived(AndroidMegaChatMessage msg, boolean checkTempId){
        logDebug("Msg ID: " + msg.getMessage().getMsgId()
                + "Msg TEMP ID: " + msg.getMessage().getTempId()
                + "Msg status: " + msg.getMessage().getStatus());

        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());
        int indexToChange = getIndexToUpdate(msg, checkTempId, itr);


        if (indexToChange == INVALID_POSITION) {
            itr = bufferMessages.listIterator(bufferMessages.size());
            indexToChange = getIndexToUpdate(msg, checkTempId, itr);

            if (indexToChange != INVALID_POSITION) {
                bufferMessages.set(indexToChange, msg);
                return indexToChange;
            }
        }

        if (indexToChange == INVALID_POSITION) {
            itr = bufferSending.listIterator(bufferSending.size());
            indexToChange = getIndexToUpdate(msg, checkTempId, itr);

            if (indexToChange != INVALID_POSITION) {
                bufferSending.set(indexToChange, msg);
                return indexToChange;
            }
        }

        logDebug("Index to change = " + indexToChange);
        if (indexToChange == INVALID_POSITION) {
            return indexToChange;
        }

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
                    adapter = new MegaChatLollipopAdapter(this, chatRoom, messages,messagesPlaying, removedMessages,  listView);
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
                        createAdapter();
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
            loadMessage(messageToShow, currentIndex);
        }

        //Create adapter
        if(adapter==null){
           createAdapter();
        }
        else{
            adapter.loadPreviousMessages(messages, bufferMessages.size());

            logDebug("addMessage: " + messages.size());
            updateActionModeTitle();
            reDoTheSelectionAfterRotation();
            recoveredSelectedPositions = null;
        }

        logDebug("AFTER updateMessagesLoaded: " + messages.size() + " messages in list");

        bufferMessages.clear();
    }

    public void clearHistory(AndroidMegaChatMessage androidMsg){
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

        removedMessages.clear();

        if(messages.size()==1){
            androidMsg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
        }
        else{
            for(int i=0; i<messages.size();i++){
                adjustInfoToShow(i);
            }
        }

        updateMessages();
    }

    /**
     * Method to control the visibility of the select option.
     */
    private void checkSelectOption() {
        if (selectMenuItem == null)
            return;

        boolean selectableMessages = false;
        if ((messages != null && chatRoom != null && !joiningOrLeaving && messages.size() > 0)) {
            for (AndroidMegaChatMessage msg : messages) {
                if (msg == null || msg.getMessage() == null) {
                    continue;
                }

                switch (msg.getMessage().getType()) {
                    case MegaChatMessage.TYPE_CONTAINS_META:
                    case MegaChatMessage.TYPE_NORMAL:
                    case MegaChatMessage.TYPE_NODE_ATTACHMENT:
                    case MegaChatMessage.TYPE_VOICE_CLIP:
                    case MegaChatMessage.TYPE_CONTACT_ATTACHMENT:
                        selectableMessages = true;
                        break;
                }
            }
        }
        selectMenuItem.setVisible(selectableMessages);
    }


    private void updateMessages(){
        checkSelectOption();
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

    public void loadMessage(AndroidMegaChatMessage messageToShow, int currentIndex){
        messageToShow.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
        messages.add(0,messageToShow);

        if(messages.size()>1) {
            adjustInfoToShow(1);
        }

        setShowAvatar(0);

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
            createAdapter();
        }else{

            if(lastIndex==0){
                logDebug("Arrives the first message of the chat");
                updateMessages();
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
            createAdapter();
        }else{
            if(lastIndex<0){
                logDebug("Arrives the first message of the chat");
                updateMessages();
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
            createAdapter();
        }else{
            logDebug("Update adapter with last index: " + lastIndex);
            if(lastIndex<0){
                logDebug("Arrives the first message of the chat");
                updateMessages();
            }else{
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

    public void showMsgNotSentPanel(AndroidMegaChatMessage message, int position){
        logDebug("Position: " + position);

        selectedPosition = position;

        if (message == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedMessageId = message.getMessage().getRowId();
        logDebug("Temporal id of MS message: "+message.getMessage().getTempId());
        bottomSheetDialogFragment = new MessageNotSentBottomSheetDialogFragment();
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

    public void showReactionBottomSheet(AndroidMegaChatMessage message, int position, String reaction) {
        if (message == null || message.getMessage() == null)
            return;

        selectedPosition = position;
        hideBottomSheet();
        selectedMessageId = message.getMessage().getMsgId();

        if (reaction == null) {
            bottomSheetDialogFragment = new ReactionsBottomSheet();
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        } else if (chatRoom != null && !chatRoom.isPreview() && chatRoom.getOwnPrivilege() != MegaChatRoom.PRIV_RM) {
            bottomSheetDialogFragment = new InfoReactionsBottomSheet(this, reaction);
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        }
    }

    public void hideBottomSheet() {
        if (isBottomSheetDialogShown(bottomSheetDialogFragment)) {
            bottomSheetDialogFragment.dismissAllowingStateLoss();
        }
    }

    private void showGeneralChatMessageBottomSheet(AndroidMegaChatMessage message, int position) {
        selectedPosition = position;

        if (message == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedMessageId = message.getMessage().getMsgId();
        bottomSheetDialogFragment = new GeneralChatMessageBottomSheet();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void removeMsgNotSent(){
        logDebug("Selected position: " + selectedPosition);
        messages.remove(selectedPosition);
        adapter.removeMessage(selectedPosition, messages);
    }

    public void updatingRemovedMessage(MegaChatMessage message) {
        for (int i = 0; i < messages.size(); i++) {
            MegaChatMessage messageToCompare = messages.get(i).getMessage();
            if (messageToCompare != null && messageToCompare.getTempId() == message.getTempId() && messageToCompare.getMsgId() == message.getMsgId()) {
                RemovedMessage msg = new RemovedMessage(messageToCompare.getTempId(), messageToCompare.getMsgId());
                removedMessages.add(msg);
                adapter.notifyItemChanged(i + 1);
            }
        }
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
            intentOpenChat.putExtra(CHAT_ID, chat.getChatId());
            this.startActivity(intentOpenChat);
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
            createAdapter();
        } else {
            updateMessages();
        }
    }

    private void createAdapter() {
        //Create adapter
        adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, messagesPlaying, removedMessages, listView);
        adapter.setHasStableIds(true);
        listView.setLayoutManager(mLayoutManager);
        listView.setAdapter(adapter);
        updateMessages();
    }

    public void updateReactionAdapter(MegaChatMessage msg, String reaction, int count) {
        adapter.checkReactionUpdated(idChat, msg, reaction, count);
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
                logDebug("The call has been successfully hung up");
                MegaChatCall call = api.getChatCall(idChat);
                if (call == null) return;
                if (call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                    addChecksForACall(chatRoom.getChatId(), false);
                    api.answerChatCall(idChat, false, this);

                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                    api.startChatCall(idChat, false, this);
                }
            } else {
                logError("ERROR WHEN TYPE_HANG_CHAT_CALL e.getErrorCode(): " + e.getErrorString());
            }

        } else if (request.getType() == MegaChatRequest.TYPE_START_CHAT_CALL) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug(" The call has been started success");
                openCall();
            } else {
                logError("ERROR WHEN TYPE_START_CHAT_CALL e.getErrorCode(): " + e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getString(R.string.call_error), MEGACHAT_INVALID_HANDLE);
            }
        } else if (request.getType() == MegaChatRequest.TYPE_ANSWER_CHAT_CALL) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                logDebug("The call has been answered success");
                openCall();
            } else {
                logError("ERROR WHEN TYPE_ANSWER_CHAT_CALL e.getErrorCode(): " + e.getErrorString());
                if (e.getErrorCode() == MegaChatError.ERROR_TOOMANY) {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.call_error_too_many_participants), -1);
                } else {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.call_error), -1);
                }
            }

        }else if (request.getType() == MegaChatRequest.TYPE_SET_CALL_ON_HOLD) {
                if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                    MegaChatCall call = megaChatApi.getChatCall(idChat);
                    if (call == null)
                        return;

                    logDebug("Active call on hold. Joinning in the group chat.");
                    if (call.getStatus() == MegaChatCall.CALL_STATUS_RING_IN) {
                        addChecksForACall(chatRoom.getChatId(), false);
                        megaChatApi.answerChatCall(idChat, false, this);
                    } else if (call.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                        megaChatApi.startChatCall(idChat, false, this);
                    }
                } else {
                    logError("Error putting the call on hold" + e.getErrorCode());
                }

        }else if(request.getType() == MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM){
            logDebug("Remove participant: " + request.getUserHandle() + " my user: " + megaChatApi.getMyUserHandle());

            if (request.getUserHandle() == INVALID_HANDLE) {
                joiningOrLeaving = false;
            }

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                logDebug("Participant removed OK");
                invalidateOptionsMenu();

            }
            else{
                logError("ERROR WHEN TYPE_REMOVE_FROM_CHATROOM " + e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getTranslatedErrorString(e), MEGACHAT_INVALID_HANDLE);
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
                intent.putExtra(CHAT_ID, request.getChatHandle());
                this.startActivity(intent);
                finish();
            }
            else{
                logError("ERROR WHEN CREATING CHAT " + e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getString(R.string.create_chat_error), -1);
            }
        } else if (request.getType() == MegaChatRequest.TYPE_LOAD_PREVIEW) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK || e.getErrorCode() == MegaChatError.ERROR_EXIST) {
                if (idChat != MEGACHAT_INVALID_HANDLE && megaChatApi.getChatRoom(idChat) != null) {
                    logDebug("Close previous chat");
                    megaChatApi.closeChatRoom(idChat, this);
                }

                idChat = request.getChatHandle();
                megaChatApi.addChatListener(this);
                
                if (idChat != MEGACHAT_INVALID_HANDLE) {
                    dbH.setLastPublicHandle(idChat);
                    dbH.setLastPublicHandleTimeStamp();
                    dbH.setLastPublicHandleType(MegaApiJava.AFFILIATE_TYPE_CHAT);
                }

                MegaApplication.setOpenChatId(idChat);

                if (e.getErrorCode() == MegaChatError.ERROR_OK && openingAndJoining) {
                    megaChatApi.autojoinPublicChat(idChat, this);
                    openingAndJoining = false;
                } else if (e.getErrorCode() == MegaChatError.ERROR_EXIST) {
                    if (megaChatApi.getChatRoom(idChat).isActive()) {
                        //I'm already participant
                        joiningOrLeaving = false;
                        openingAndJoining = false;
                    } else {
                        if (initChat()) {
                            //Chat successfully initialized, now can rejoin
                            setJoiningOrLeaving(getString(R.string.joining_label));
                            titleToolbar.setText(chatRoom.getTitle());
                            groupalSubtitleToolbar.setText(null);
                            setGroupalSubtitleToolbarVisibility(false);
                            if (adapter == null) {
                                createAdapter();
                            } else {
                                adapter.updateChatRoom(chatRoom);
                                adapter.notifyDataSetChanged();
                            }
                            megaChatApi.autorejoinPublicChat(idChat, request.getUserHandle(), this);
                        } else {
                            logWarning("Error opening chat before rejoin");
                        }
                        return;
                    }
                }

                initAndShowChat(null);
                supportInvalidateOptionsMenu();
            } else {
                String text;
                if (e.getErrorCode() == MegaChatError.ERROR_NOENT) {
                    text = getString(R.string.invalid_chat_link);
                } else {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_general_nodes), MEGACHAT_INVALID_HANDLE);
                    text = getString(R.string.error_chat_link);
                }

                emptyScreen(text);
            }
        } else if (request.getType() == MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT) {
            joiningOrLeaving = false;

            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                if (request.getUserHandle() != MEGACHAT_INVALID_HANDLE) {
                    //Rejoin option
                    initializeInputText();
                    isOpeningChat = true;
                    loadHistory();
                } else {
                    //Join
                    setChatSubtitle();
                    setPreviewersView();
                }

                supportInvalidateOptionsMenu();
            } else {
                MegaChatRoom chatRoom = megaChatApi.getChatRoom(idChat);
                if (chatRoom != null && chatRoom.getOwnPrivilege() >= MegaChatRoom.PRIV_RO) {
                    logWarning("I'm already a participant");
                    return;
                }

                logError("ERROR WHEN JOINING CHAT " + e.getErrorCode() + " " + e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_general_nodes), MEGACHAT_INVALID_HANDLE);
            }
        } else if(request.getType() == MegaChatRequest.TYPE_LAST_GREEN){
            logDebug("TYPE_LAST_GREEN requested");

        }else if(request.getType() == MegaChatRequest.TYPE_ARCHIVE_CHATROOM){

            long chatHandle = request.getChatHandle();
            chatRoom = megaChatApi.getChatRoom(chatHandle);
            String chatTitle = getTitleChat(chatRoom);

            if(chatTitle==null){
                chatTitle = "";
            }
            else if(!chatTitle.isEmpty() && chatTitle.length()>60){
                chatTitle = chatTitle.substring(0,59)+"...";
            }

            if(!chatTitle.isEmpty() && chatRoom.isGroup() && !chatRoom.hasCustomTitle()){
                chatTitle = "\""+chatTitle+"\"";
            }

            supportInvalidateOptionsMenu();
            setChatSubtitle();

            if (!chatRoom.isArchived()) {
                requestLastGreen(INITIAL_PRESENCE_STATUS);
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
        super.onStop();
    }

    private void cleanBuffers(){
        if(!bufferMessages.isEmpty()){
            bufferMessages.clear();
        }
        if(!messages.isEmpty()){
            messages.clear();
        }
        if(!removedMessages.isEmpty()){
            removedMessages.clear();
        }
    }

    @Override
    protected void onDestroy() {
        logDebug("onDestroy()");
        destroySpeakerAudioManger();
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

        hideCallBar(null);

        destroyAudioRecorderElements();
        if(adapter!=null) {
            adapter.stopAllReproductionsInProgress();
            adapter.destroyVoiceElemnts();
        }

        unregisterReceiver(chatRoomMuteUpdateReceiver);
        unregisterReceiver(dialogConnectReceiver);
        unregisterReceiver(voiceclipDownloadedReceiver);
        unregisterReceiver(userNameReceiver);
        unregisterReceiver(chatArchivedReceiver);
        unregisterReceiver(chatCallUpdateReceiver);
        unregisterReceiver(chatSessionUpdateReceiver);
        unregisterReceiver(leftChatReceiver);
        unregisterReceiver(closeChatReceiver);

        if(megaApi != null) {
            megaApi.removeRequestListener(this);
        }

        super.onDestroy();
    }

    /**
     * Method for saving in the database if there was something written in the input text or if a message was being edited
     */
    private void saveInputText() {
        try {
            if (textChat != null) {
                String written = textChat.getText().toString();
                if (written != null) {
                    if (dbH == null) {
                        dbH = MegaApplication.getInstance().getDbH();
                    }
                    ChatItemPreferences prefs = dbH.findChatPreferencesByHandle(Long.toString(idChat));
                    String editedMessageId = editingMessage && messageToEdit != null ? Long.toString(messageToEdit.getMsgId()) : "";
                    if (prefs != null) {
                        prefs.setEditedMsgId(editedMessageId);
                        prefs.setWrittenText(written);
                        dbH.setWrittenTextItem(Long.toString(idChat), written, editedMessageId);
                    } else {
                        prefs = new ChatItemPreferences(Long.toString(idChat), written, editedMessageId);
                        dbH.setChatItemPreferences(prefs);
                    }
                }
            }
        } catch (Exception e) {
            logError("Written message not stored on DB", e);
        }
    }

    public void closeChat(boolean shouldLogout) {
        logDebug("closeChat");
        if (megaChatApi == null || chatRoom == null || idChat == MEGACHAT_INVALID_HANDLE) {
            return;
        }
        saveInputText();
        shouldLogout = chatC.isInAnonymousMode() && shouldLogout;

        megaChatApi.closeChatRoom(idChat, this);

        if (chatRoom.isPreview()) {
            megaChatApi.closeChatPreview(idChat);
        }

        MegaApplication.setClosedChat(true);
        megaChatApi.removeChatListener(this);

        if (shouldLogout) {
            megaChatApi.logout();
        }

        chatRoom = null;
        idChat = MEGACHAT_INVALID_HANDLE;
    }

    @Override
    protected void onNewIntent(Intent intent){
        logDebug("onNewIntent");
        hideKeyboard();
        if (intent != null){
            if (intent.getAction() != null){
                if(intent.getAction().equals(ACTION_UPDATE_ATTACHMENT)){
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
                    long newidChat = intent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE);
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

                    closeChat(false);
                    MegaApplication.setOpenChatId(-1);
                    initAfterIntent(intent, null);
                }

            }
        }
        super.onNewIntent(intent);
        setIntent(intent);
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
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    protected MegaChatLollipopAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        logDebug("onSaveInstance");
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
        outState.putLong(LAST_MESSAGE_SEEN, lastIdMsgSeen);
        outState.putLong(GENERAL_UNREAD_COUNT, generalUnreadCount);
        outState.putBoolean("isHideJump",isHideJump);
        outState.putString("mOutputFilePath",mOutputFilePath);
        outState.putBoolean("isShareLinkDialogDismissed", isShareLinkDialogDismissed);

        if(adapter == null)
            return;


        RotatableAdapter currentAdapter = getAdapter();
        if(currentAdapter != null & adapter.isMultipleSelect()){
            ArrayList<Integer> selectedPositions= (ArrayList<Integer>) (currentAdapter.getSelectedItems());
            outState.putIntegerArrayList(SELECTED_ITEMS, selectedPositions);
        }

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
        outState.putBoolean(JOINING_OR_LEAVING, joiningOrLeaving);
        outState.putString(JOINING_OR_LEAVING_ACTION, joiningOrLeavingAction);
        outState.putBoolean(OPENING_AND_JOINING_ACTION, openingAndJoining);
        outState.putBoolean(ERROR_REACTION_DIALOG, errorReactionsDialogIsShown);
        outState.putLong(TYPE_ERROR_REACTION, typeErrorReaction);
    }

    public void askSizeConfirmationBeforeChatDownload(String parentPath, ArrayList<MegaNode> nodeList, long size){
        logDebug("askSizeConfirmationBeforeChatDownload");

        final String parentPathC = parentPath;
        final ArrayList<MegaNode> nodeListC = nodeList;
        final long sizeC = size;
        final ChatController chatC = new ChatController(this);

        androidx.appcompat.app.AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        }
        else{
            builder = new AlertDialog.Builder(this);
        }
        LinearLayout confirmationLayout = new LinearLayout(this);
        confirmationLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(scaleWidthPx(20, getOutMetrics()), scaleHeightPx(10, getOutMetrics()), scaleWidthPx(17, getOutMetrics()), 0);

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

                    checkIfServiceCanStart(intent);
                }
                else{
                    logError("Error when adding pending msg to the database");
                }

                removeProgressDialog();
            }
        }
    }

    /**
     * If the chat id received is valid, opens a chat after forward messages.
     * If no, only disables select mode and shows a Snackbar if the text received is not null neither empty.
     *
     * @param chatHandle Chat id.
     * @param text       Text to show as Snackbar if needed, null or empty otherwise.
     */
    public void openChatAfterForward(long chatHandle, String text) {
        removeProgressDialog();

        if (chatHandle == idChat || chatHandle == MEGACHAT_INVALID_HANDLE) {
            disableMultiselection();

            if (text != null) {
                showSnackbar(SNACKBAR_TYPE, text, MEGACHAT_INVALID_HANDLE);
            }

            return;
        }

        Intent intentOpenChat = new Intent(this, ManagerActivityLollipop.class);
        intentOpenChat.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentOpenChat.setAction(ACTION_CHAT_NOTIFICATION_MESSAGE);
        intentOpenChat.putExtra(CHAT_ID, chatHandle);
        intentOpenChat.putExtra(SHOW_SNACKBAR, text);
        closeChat(true);
        startActivity(intentOpenChat);
        finish();
    }

    public void markAsSeen(MegaChatMessage msg) {
        logDebug("markAsSeen");
        if (activityVisible) {
            if (msg.getStatus() != MegaChatMessage.STATUS_SEEN) {
                logDebug("Mark message: " + msg.getMsgId() + " as seen");
                megaChatApi.setMessageSeen(chatRoom.getChatId(), msg.getMsgId());
            }
        }
    }


   @Override
    public void onResume(){
        logDebug("onResume");
        super.onResume();
       stopService(new Intent(this, KeepAliveService.class));
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

            try {
                ChatAdvancedNotificationBuilder notificationBuilder;
                notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this, megaApi, megaChatApi);
                notificationBuilder.removeAllChatNotifications();
            } catch (Exception e) {
                logError("Exception NotificationManager - remove all notifications", e);
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
            updateCallBar();
            if(aB != null && aB.getTitle() != null){
                titleToolbar.setText(adjustForLargeFont(titleToolbar.getText().toString()));
            }
            updateActionModeTitle();
        }
    }

    public void scrollToMessage(long lastId){
        for(int i=messages.size()-1; i>=0;i--) {
            AndroidMegaChatMessage androidMessage = messages.get(i);

            if (!androidMessage.isUploading()) {
                MegaChatMessage msg = androidMessage.getMessage();
                if (msg.getMsgId() == lastId) {
                    logDebug("Scroll to position: " + i);
                    mLayoutManager.scrollToPositionWithOffset(i+1,scaleHeightPx(30, getOutMetrics()));
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

                        if (lastMessage.getMessage() != null && app.isActivityVisible()) {
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

                            if (lastMessage.getMessage() != null && app.isActivityVisible()) {
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
        super.onPause();
        if (rtcAudioManager != null)
            rtcAudioManager.unregisterProximitySensor();

        destroyAudioRecorderElements();
        if(adapter!=null) {
            adapter.pausePlaybackInProgress();
        }
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

            updateCallBar();
            setChatSubtitle();
            supportInvalidateOptionsMenu();
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

        File file;
        if (path.startsWith("content:")) {
            file = getFileFromContentUri(this, Uri.parse(path));
        } else if (isVoiceClip(path)) {
            file = buildVoiceClipFile(this, outputFileName);
            if (!isFileAvailable(file)) return;
        } else {
            file = new File(path);
            if (!MimeTypeList.typeForName(file.getAbsolutePath()).isImage()) return;
        }

        Intent intent = new Intent(this, ChatUploadService.class);
        PendingMessageSingle pMsgSingle = new PendingMessageSingle();
        pMsgSingle.setChatId(idChat);
        if(isVoiceClip(file.getAbsolutePath())){
            pMsgSingle.setType(TYPE_VOICE_CLIP);
            intent.putExtra(EXTRA_TRANSFER_TYPE, EXTRA_VOICE_CLIP);
        }

        long timestamp = System.currentTimeMillis()/1000;
        pMsgSingle.setUploadTimestamp(timestamp);

        String fingerprint = megaApi.getFingerprint(file.getAbsolutePath());
        pMsgSingle.setFilePath(file.getAbsolutePath());
        pMsgSingle.setName(file.getName());
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

        checkIfServiceCanStart(intent);
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
                    navigateToUpgradeAccount();
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

    public void showJumpMessage(){
        if((!isHideJump)&&(typeMessageJump!=TYPE_MESSAGE_NEW_MESSAGE)){
            typeMessageJump = TYPE_MESSAGE_JUMP_TO_LEAST;
            messageJumpText.setText(getResources().getString(R.string.message_jump_latest));
            messageJumpLayout.setVisibility(View.VISIBLE);
        }
    }

    private void showCallInProgressLayout(String text, boolean shouldChronoShown, MegaChatCall call) {
        if (callInProgressText != null) {
            callInProgressText.setText(text);
        }

        if (shouldChronoShown) {
            startChronometers(call);
        } else {
            stopChronometers(call);
        }

        chatIdBanner = call.getChatid();

        if (callInProgressLayout != null && callInProgressLayout.getVisibility() != View.VISIBLE) {
            callInProgressLayout.setAlpha(1);
            callInProgressLayout.setVisibility(View.VISIBLE);
            callInProgressLayout.setOnClickListener(this);
        }
    }

    /**
     * Method to start the chronometer related to the current call
     *
     * @param call The current call in progress.
     */
    private void startChronometers(MegaChatCall call) {
        if (callInProgressChrono == null) {
            return;
        }

        activateChrono(true, callInProgressChrono, call);
        callInProgressChrono.setOnChronometerTickListener(chronometer -> {
            if (subtitleChronoCall == null) {
                return;
            }

            subtitleChronoCall.setVisibility(View.VISIBLE);
            subtitleChronoCall.setText(chronometer.getText());
        });
    }

    /**
     * Method to stop the chronometer related to the current call
     *
     * @param call The current call in progress.
     */
    private void stopChronometers(MegaChatCall call) {
        if (callInProgressChrono != null) {
            activateChrono(false, callInProgressChrono, call);
            callInProgressChrono.setOnChronometerTickListener(null);
        }

        if (subtitleChronoCall != null) {
            subtitleChronoCall.setVisibility(View.GONE);
        }
    }

    /**
     * Method for hiding the current call bar.
     *
     * @param call The call.
     */
    private void hideCallBar(MegaChatCall call) {
        invalidateOptionsMenu();
        stopChronometers(call);

        if (callInProgressLayout != null) {
            callInProgressLayout.setVisibility(View.GONE);
            callInProgressLayout.setOnClickListener(null);
            setSubtitleVisibility();
        }
        if(returnCallOnHoldButton != null) {
            returnCallOnHoldButton.setVisibility(View.GONE);
        }
    }

    /**
     * Method to get another call on hold.
     *
     * @param currentChatId Call id.
     * @return The another call.
     */
    private MegaChatCall getAnotherOnHoldCall(long currentChatId) {
        return getAnotherOnHoldOrActiveCall(currentChatId, false);
    }

    /**
     * Method to get another call in progress.
     *
     * @param currentChatId Call id.
     * @return The another call.
     */
    private MegaChatCall getAnotherActiveCall(long currentChatId) {
        return getAnotherOnHoldOrActiveCall(currentChatId, true);
    }

    /**
     * Method to get another call in progress or on hold.
     *
     * @param currentChatId Call id.
     * @param isActiveCall  True if wants to get a call in progress,
     *                      false if wants to get a call on hold.
     * @return The another call.
     */
    private MegaChatCall getAnotherOnHoldOrActiveCall(long currentChatId, boolean isActiveCall) {
        ArrayList<Long> chatsIDsWithCallActive = getCallsParticipating();
        if (chatsIDsWithCallActive != null && !chatsIDsWithCallActive.isEmpty()) {
            for (Long anotherChatId : chatsIDsWithCallActive) {
                if (anotherChatId != currentChatId && megaChatApi.getChatCall(anotherChatId) != null &&
                        ((isActiveCall && !megaChatApi.getChatCall(anotherChatId).isOnHold()) ||
                                (!isActiveCall && megaChatApi.getChatCall(anotherChatId).isOnHold()))) {
                    return megaChatApi.getChatCall(anotherChatId);
                }
            }
        }
        return null;
    }

    /**
     * Method for updating the bar that indicates the current call in this chat.
     */
    private void updateCallBar() {
        if (chatRoom == null || chatRoom.isPreview() || !chatRoom.isActive() ||
                megaChatApi.getNumCalls() <= 0 || !isStatusConnected(this, idChat)) {
            /*No calls*/
            setSubtitleVisibility();
            MegaChatCall call = megaChatApi.getChatCall(idChat);
            hideCallBar(call);
            return;
        }

        MegaChatCall anotherActiveCall = getAnotherActiveCall(idChat);
        MegaChatCall anotherOnHoldCall = getAnotherOnHoldCall(idChat);
        MegaChatCall callInThisChat = megaChatApi.getChatCall(chatRoom.getChatId());

        if (callInThisChat == null || (callInThisChat.getStatus() != MegaChatCall.CALL_STATUS_RECONNECTING && !isStatusConnected(this, idChat))) {
            /*No call in this chatRoom*/
            if ((anotherActiveCall != null || anotherOnHoldCall != null) &&
                    MegaApplication.getCallLayoutStatus(anotherActiveCall != null ? anotherActiveCall.getChatid() : anotherOnHoldCall.getChatid())) {
                updateCallInProgressLayout(anotherActiveCall != null ? anotherActiveCall : anotherOnHoldCall,
                        getString(R.string.call_in_progress_layout));
                returnCallOnHoldButton.setVisibility(View.GONE);
            } else {
                hideCallBar(null);
            }
            return;
        }

        /*Call in this chatRoom*/
        int callStatus = callInThisChat.getStatus();
        logDebug("The call status in this chatRoom is "+callStatusToString(callStatus));
        if (callStatus == MegaChatCall.CALL_STATUS_DESTROYED) {
            setSubtitleVisibility();
            if ((anotherActiveCall != null || anotherOnHoldCall != null) &&
                    MegaApplication.getCallLayoutStatus(anotherActiveCall != null ? anotherActiveCall.getChatid() : anotherOnHoldCall.getChatid())) {
                updateCallInProgressLayout(anotherActiveCall != null ? anotherActiveCall : anotherOnHoldCall,
                        getString(anotherActiveCall != null ? R.string.call_in_progress_layout : R.string.call_on_hold));
                returnCallOnHoldButton.setVisibility(View.GONE);
            } else {
                hideCallBar(megaChatApi.getChatCall(idChat));
            }
            return;
        }

        if (callStatus == MegaChatCall.CALL_STATUS_IN_PROGRESS && (callInThisChat.isOnHold() || isSessionOnHold(callInThisChat.getChatid()))) {
            if (anotherActiveCall != null || anotherOnHoldCall != null) {
                updateCallInProgressLayout(anotherActiveCall != null ? anotherActiveCall : anotherOnHoldCall,
                        getString(R.string.call_in_progress_layout));
                returnCallOnHoldButtonText.setText(getResources().getString(R.string.call_on_hold));
                returnCallOnHoldButtonIcon.setImageResource(R.drawable.ic_transfers_pause);
                returnCallOnHoldButton.setVisibility(View.VISIBLE);
            } else {
                updateCallInProgressLayout(callInThisChat, getString(R.string.call_in_progress_layout));
                returnCallOnHoldButton.setVisibility(View.GONE);
            }
            return;
        }

        returnCallOnHoldButton.setVisibility(View.GONE);

        if ((anotherActiveCall == null && anotherOnHoldCall == null) && callStatus == MegaChatCall.CALL_STATUS_RECONNECTING) {
            MegaApplication.setCallLayoutStatus(idChat, false);
        }

        logDebug("Call Status in this chatRoom: "+callStatusToString(callStatus));
        switch (callStatus){
            case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:
            case MegaChatCall.CALL_STATUS_USER_NO_PRESENT:
            case MegaChatCall.CALL_STATUS_RING_IN:
                if(chatRoom == null)
                    break;

                if(chatRoom.isGroup()){
                    if (anotherActiveCall == null && anotherOnHoldCall == null) {
                        usersWithVideo();
                        long callerHandle = callInThisChat.getCaller();
                        String callerFullName = chatC.getParticipantFullName(callerHandle);
                        String textLayout;
                        if (callerHandle != MEGACHAT_INVALID_HANDLE && !isTextEmpty(callerFullName)) {
                            textLayout = getString(R.string.join_call_layout_in_group_call, callerFullName);
                        } else {
                            textLayout = getString(R.string.join_call_layout);
                        }
                        tapToReturnLayout(callInThisChat, textLayout);
                    }else{
                        updateCallInProgressLayout(anotherActiveCall != null ? anotherActiveCall : anotherOnHoldCall,
                                getString(R.string.call_in_progress_layout));
                        returnCallOnHoldButton.setVisibility(View.VISIBLE);
                        returnCallOnHoldButtonText.setText(getResources().getString(R.string.title_join_call));
                        returnCallOnHoldButtonIcon.setImageResource(R.drawable.ic_call_chat);
                    }
                }else{
                    if(callStatus == MegaChatCall.CALL_STATUS_USER_NO_PRESENT &&
                            isAfterReconnecting(this, callInProgressLayout, callInProgressText))
                        break;

                    if(anotherActiveCall == null && anotherOnHoldCall == null) {
                        if(callStatus == MegaChatCall.CALL_STATUS_RING_IN &&
                                (MegaApplication.getCallLayoutStatus(idChat) || !megaApi.isChatNotifiable(idChat))){
                            tapToReturnLayout(callInThisChat, getString(R.string.call_in_progress_layout));
                            break;
                        }
                        hideCallBar(callInThisChat);
                    }else{
                        updateCallInProgressLayout(anotherActiveCall != null ? anotherActiveCall : anotherOnHoldCall,
                                getString(R.string.call_in_progress_layout));
                    }
                }
                break;

            case MegaChatCall.CALL_STATUS_REQUEST_SENT:
                if (MegaApplication.getCallLayoutStatus(idChat)) {
                    tapToReturnLayout(callInThisChat, getString(R.string.call_in_progress_layout));
                    break;
                }

                hideCallBar(callInThisChat);
                break;

            case MegaChatCall.CALL_STATUS_RECONNECTING:
                subtitleChronoCall.setVisibility(View.GONE);
                callInProgressLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.reconnecting_bar));
                showCallInProgressLayout(getString(R.string.reconnecting_message), false, callInThisChat);
                callInProgressLayout.setOnClickListener(this);
                break;

            case MegaChatCall.CALL_STATUS_IN_PROGRESS:
                if(!MegaApplication.getCallLayoutStatus(idChat)){
                    hideCallBar(callInThisChat);
                    break;
                }

                callInProgressLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.accentColor));
                if (!isAfterReconnecting(this, callInProgressLayout, callInProgressText)) {
                    updateCallInProgressLayout(callInThisChat, getString(R.string.call_in_progress_layout));
                    break;
                }

                callInProgressLayout.setOnClickListener(null);
                showCallInProgressLayout(getString(R.string.connected_message), false, callInThisChat);
                callInProgressLayout.setAlpha(1);
                callInProgressLayout.setVisibility(View.VISIBLE);
                callInProgressLayout.animate()
                        .alpha(0f)
                        .setDuration(QUICK_INFO_ANIMATION)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                callInProgressLayout.setVisibility(View.GONE);
                                updateCallInProgressLayout(callInThisChat, getString(R.string.call_in_progress_layout));
                            }
                        });
                break;

        }
    }

    private void tapToReturnLayout(MegaChatCall call, String text){
        callInProgressLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.accentColor));
        showCallInProgressLayout(text, false, call);
        callInProgressLayout.setOnClickListener(this);
    }

    private void updateCallInProgressLayout(MegaChatCall call, String text){
        if (call == null)
            return;

        showCallInProgressLayout(text, true, call);
        callInProgressLayout.setOnClickListener(this);
        if (chatRoom != null && chatRoom.isGroup()) {
            subtitleCall.setVisibility(View.VISIBLE);
            individualSubtitleToobar.setVisibility(View.GONE);
            setGroupalSubtitleToolbarVisibility(false);
        }

        usersWithVideo();
        invalidateOptionsMenu();
    }

    public void usersWithVideo() {
        if (megaChatApi == null || chatRoom == null || !chatRoom.isGroup() || subtitleCall.getVisibility() != View.VISIBLE)
            return;

        MegaChatCall call = megaChatApi.getChatCall(idChat);
        if(call == null)
            return;

        int usersWithVideo = call.getNumParticipants(MegaChatCall.VIDEO);
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
                mLayoutManager.scrollToPositionWithOffset(index+1,scaleHeightPx(20, getOutMetrics()));
            }else{
                index++;
                infoToShow = adjustInfoToShow(index);
                if(infoToShow== AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL){
                    mLayoutManager.scrollToPositionWithOffset(index, scaleHeightPx(50, getOutMetrics()));
                }else{
                    mLayoutManager.scrollToPositionWithOffset(index, scaleHeightPx(20, getOutMetrics()));
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

        MegaApiAndroid megaApiFolder = new MegaApiAndroid(MegaApplication.APP_KEY, MegaApplication.USER_AGENT, path);

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
            builder.setMessage(R.string.confirmation_to_reconnect).setPositiveButton(R.string.general_ok, dialogClickListener)
                    .setNegativeButton(R.string.general_cancel, dialogClickListener).show().setCanceledOnTouchOutside(false);
        }
        catch (Exception e){}
    }

    public void startConnection() {
        logDebug("Broadcast to ManagerActivity");
        Intent intent = new Intent(BROADCAST_ACTION_INTENT_CONNECTIVITY_CHANGE);
        intent.putExtra("actionType", START_RECONNECTION);
        sendBroadcast(intent);
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

    private void createSpeakerAudioManger(){
        if(rtcAudioManager != null) return;
        speakerWasActivated = true;
        rtcAudioManager = AppRTCAudioManager.create(this, speakerWasActivated, INVALID_CALL_STATUS);
        rtcAudioManager.setOnProximitySensorListener(new OnProximitySensorListener() {
            @Override
            public void needToUpdate(boolean isNear) {
                if(!speakerWasActivated && !isNear){
                    adapter.pausePlaybackInProgress();
                }else if(speakerWasActivated && isNear){
                    speakerWasActivated = false;
                }
            }
        });
    }

    public void startProximitySensor(){
        logDebug("Starting proximity sensor");
        createSpeakerAudioManger();
        rtcAudioManager.startProximitySensor();
    }
    private void activateSpeaker(){
        if(!speakerWasActivated){
            speakerWasActivated = true;
        }
        if(rtcAudioManager != null){
            rtcAudioManager.updateSpeakerStatus(true, INVALID_CALL_STATUS);
        }
    }

    public void stopProximitySensor(){
        if(rtcAudioManager == null) return;
        activateSpeaker();
        rtcAudioManager.unregisterProximitySensor();
        destroySpeakerAudioManger();
    }

    private void destroySpeakerAudioManger(){
        if (rtcAudioManager == null) return;
        try {
            rtcAudioManager.stop();
            rtcAudioManager = null;
        } catch (Exception e) {
            logError("Exception stopping speaker audio manager", e);
        }
    }


    public void setShareLinkDialogDismissed (boolean dismissed) {
        isShareLinkDialogDismissed = dismissed;
    }

    private void checkIfServiceCanStart(Intent intent) {
        preservedIntents.add(intent);
        if (!isAskingForMyChatFiles) {
            checkIfIsNeededToAskForMyChatFilesFolder();
        }
    }

    private void checkIfIsNeededToAskForMyChatFilesFolder() {
        if (existsMyChatFilesFolder()) {
            setMyChatFilesFolder(getMyChatFilesFolder());

            if (isForwardingFromNC()) {
                handleStoredData();
            } else {
                proceedWithAction();
            }
        } else {
            isAskingForMyChatFiles = true;
            megaApi.getMyChatFilesFolder(new GetAttrUserListener(this));
        }
    }

    public void startUploadService() {
        if (!isWaitingForMoreFiles && !preservedIntents.isEmpty()) {
            for (Intent intent : preservedIntents) {
                intent.putExtra(ChatUploadService.EXTRA_PARENT_NODE, myChatFilesFolder.serialize());
                startService(intent);
            }
            preservedIntents.clear();
        }
    }

    public void setMyChatFilesFolder(MegaNode myChatFilesFolder) {
        isAskingForMyChatFiles = false;
        this.myChatFilesFolder = myChatFilesFolder;
    }

    public boolean isForwardingFromNC() {
        return isForwardingFromNC;
    }

    private void sendBroadcastChatArchived(String chatTitle) {
        Intent intent = new Intent(BROADCAST_ACTION_INTENT_CHAT_ARCHIVED);
        intent.putExtra(CHAT_TITLE, chatTitle);
        sendBroadcast(intent);
        closeChat(true);
        finish();
    }

    public void setIsWaitingForMoreFiles (boolean isWaitingForMoreFiles) {
        this.isWaitingForMoreFiles = isWaitingForMoreFiles;
    }

    public long getCurrentChatid() {
        return idChat;
    }

    Runnable updateVisualizer = new Runnable() {
        @Override
        public void run() {
            if (recordView.isRecordingNow() && recordingLayout.getVisibility() == View.VISIBLE) {
                updateAmplitudeVisualizer(myAudioRecorder.getMaxAmplitude());
                handlerVisualizer.postDelayed(this, REPEAT_INTERVAL);
            }
        }
    };

    private void updateAmplitudeVisualizer(int newAmplitude) {
        if (currentAmplitude != -1 && getRangeAmplitude(currentAmplitude) == getRangeAmplitude(newAmplitude))
            return;
        currentAmplitude = newAmplitude;
        needToUpdateVisualizer(currentAmplitude);
    }

    private int getRangeAmplitude(int value) {
        if(value < MIN_FIRST_AMPLITUDE) return NOT_SOUND;
        if(value >= MIN_FIRST_AMPLITUDE && value < MIN_SECOND_AMPLITUDE) return FIRST_RANGE;
        if(value >= MIN_SECOND_AMPLITUDE && value < MIN_THIRD_AMPLITUDE) return SECOND_RANGE;
        if(value >= MIN_THIRD_AMPLITUDE && value < MIN_FOURTH_AMPLITUDE) return THIRD_RANGE;
        if(value >= MIN_FOURTH_AMPLITUDE && value < MIN_FIFTH_AMPLITUDE) return FOURTH_RANGE;
        if(value >= MIN_FIFTH_AMPLITUDE && value < MIN_SIXTH_AMPLITUDE) return FIFTH_RANGE;
        return SIXTH_RANGE;
    }

    private void changeColor(RelativeLayout bar, boolean isLow) {
        Drawable background;
        if(isLow){
            background = ContextCompat.getDrawable(this, R.drawable.recording_low);
        }else{
            background = ContextCompat.getDrawable(this, R.drawable.recording_high);
        }
        if (bar.getBackground() == background) return;
        bar.setBackground(background);
    }
    private void needToUpdateVisualizer(int currentAmplitude) {
        int resultRange = getRangeAmplitude(currentAmplitude);

        if (resultRange == NOT_SOUND) {
            initRecordingItems(IS_LOW);
            return;
        }
        if (resultRange == SIXTH_RANGE) {
            initRecordingItems(IS_HIGH);
            return;
        }
        changeColor(firstBar, IS_HIGH);
        changeColor(sixthBar, IS_LOW);

        if (resultRange > FIRST_RANGE) {
            changeColor(secondBar, IS_HIGH);
            if (resultRange > SECOND_RANGE) {
                changeColor(thirdBar, IS_HIGH);
                if (resultRange > THIRD_RANGE) {
                    changeColor(fourthBar, IS_HIGH);
                    if (resultRange > FOURTH_RANGE) {
                        changeColor(fifthBar, IS_HIGH);
                    } else {
                        changeColor(fifthBar, IS_LOW);
                    }
                } else {
                    changeColor(fourthBar, IS_LOW);
                    changeColor(fifthBar, IS_LOW);
                }
            } else {
                changeColor(thirdBar, IS_LOW);
                changeColor(fourthBar, IS_LOW);
                changeColor(fifthBar, IS_LOW);
            }
        } else {
            changeColor(secondBar, IS_LOW);
            changeColor(thirdBar, IS_LOW);
            changeColor(fourthBar, IS_LOW);
            changeColor(fifthBar, IS_LOW);
        }
    }

    public long getLastIdMsgSeen() {
        return lastIdMsgSeen;
    }

    public long getGeneralUnreadCount() {
        return generalUnreadCount;
    }

    public void setPositionNewMessagesLayout(int positionNewMessagesLayout) {
        this.positionNewMessagesLayout = positionNewMessagesLayout;
    }

    /**
     * Initializes the joining or leaving UI depending on the action received.
     *
     * @param action    String which indicates if the UI to set is the joining or leaving state.
     */
    private void setJoiningOrLeaving(String action) {
        joiningOrLeaving = true;
        joiningOrLeavingAction = action;
        joiningLeavingText.setText(action);
        setBottomLayout(SHOW_JOINING_OR_LEFTING_LAYOUT);
        invalidateOptionsMenu();
    }

    public void setLastIdMsgSeen(long lastIdMsgSeen) {
        this.lastIdMsgSeen = lastIdMsgSeen;
    }

    /**
     * Gets the visible positions on adapter and updates the uploading messages between them, if any.
     */
    public void updatePausedUploadingMessages() {
        if (mLayoutManager == null || adapter == null) {
            return;
        }

        adapter.updatePausedUploadingMessages(mLayoutManager.findFirstVisibleItemPosition(),
                mLayoutManager.findLastVisibleItemPosition());
    }

    /**
     * Method for opening the Call Activity.
     */
    private void openCall(){
        MegaApplication.setShowPinScreen(false);
        Intent i = new Intent(this, ChatCallActivity.class);
        i.putExtra(CHAT_ID, idChat);
        i.setAction(SECOND_CALL);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(i);
    }


    /*
     * Gets the position of an attachment message if it is visible and exists.
     *
     * @param handle The handle of the attachment.
     * @return The position of the message if it is visible and exists, INVALID_POSITION otherwise.
     */
    public int getPositionOfAttachmentMessageIfVisible(long handle) {
        if (mLayoutManager == null || adapter == null) {
            return INVALID_POSITION;
        }

        int firstVisiblePosition = mLayoutManager.findFirstVisibleItemPosition();
        if (firstVisiblePosition == INVALID_POSITION || firstVisiblePosition == 0) {
            firstVisiblePosition = 1;
        }

        int lastVisiblePosition = mLayoutManager.findLastVisibleItemPosition();
        if (lastVisiblePosition == INVALID_POSITION) {
            lastVisiblePosition = adapter.getItemCount() - 1;
        }

        for (int i = lastVisiblePosition; i >= firstVisiblePosition; i--) {
            AndroidMegaChatMessage msg = adapter.getMessageAtAdapterPosition(i);
            MegaChatMessage chatMessage = msg.getMessage();
            if (chatMessage != null
                    && chatMessage.getMegaNodeList() != null
                    && chatMessage.getMegaNodeList().get(0) != null
                    && chatMessage.getMegaNodeList().get(0).getHandle() == handle) {
                return i;
            }
        }

        return INVALID_POSITION;
    }

    /**
     * Method to display a dialog to show the error related with the chat reactions.
     *
     * @param typeError Type of Error.
     */
    public void createLimitReactionsAlertDialog(long typeError) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        dialogBuilder.setMessage(typeError == REACTION_ERROR_TYPE_USER
                ? getString(R.string.limit_reaction_per_user, MAX_REACTIONS_PER_USER)
                : getString(R.string.limit_reaction_per_message, MAX_REACTIONS_PER_MESSAGE))
                .setOnDismissListener(dialog -> {
                    errorReactionsDialogIsShown = false;
                    typeErrorReaction = REACTION_ERROR_DEFAULT_VALUE;
                })
                .setPositiveButton(getString(R.string.general_ok),
                        (dialog, which) -> {
                            dialog.dismiss();
                        });

        errorReactionsDialog = dialogBuilder.create();
        errorReactionsDialog.show();
        errorReactionsDialogIsShown = true;
        typeErrorReaction = typeError;
    }
}
