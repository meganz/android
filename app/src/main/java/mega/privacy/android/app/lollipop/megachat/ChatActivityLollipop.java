package mega.privacy.android.app.lollipop.megachat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import io.github.rockerhieu.emojicon.EmojiconEditText;
import io.github.rockerhieu.emojicon.EmojiconGridFragment;
import io.github.rockerhieu.emojicon.EmojiconsFragment;
import io.github.rockerhieu.emojicon.emoji.Emojicon;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.components.NpaLinearLayoutManager;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ChatFullScreenImageViewer;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.MultipleGroupChatRequestListener;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatLollipopAdapter;
import mega.privacy.android.app.lollipop.tasks.FilePrepareTask;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.AttachmentUploadBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ContactAttachmentBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.MessageNotSentBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.NodeAttachmentBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.TimeChatUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatRoomListenerInterface;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

public class ChatActivityLollipop extends PinActivityLollipop implements MegaChatRequestListenerInterface, MegaRequestListenerInterface, MegaChatRoomListenerInterface, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener, View.OnClickListener, EmojiconGridFragment.OnEmojiconClickedListener, EmojiconsFragment.OnEmojiconBackspaceClickedListener {

    public static int NUMBER_MESSAGES_TO_LOAD = 20;
    public static int NUMBER_MESSAGES_TO_UPDATE_UI = 7;
    public static int NUMBER_MESSAGES_BEFORE_LOAD = 8;

    boolean firstMessageReceived = true;
    boolean getMoreHistory=true;

    private AlertDialog errorOpenChatDialog;

    private android.support.v7.app.AlertDialog downloadConfirmationDialog;

    ProgressDialog dialog;
    ProgressDialog statusDialog;

    MegaChatMessage lastMessageSeen = null;
    boolean lastSeenReceived = false;
    int positionToScroll = -1;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    Handler handlerReceive;
    Handler handlerSend;

    boolean pendingMessagesLoaded = false;

//    AndroidMegaChatMessage selectedMessage;
    int selectedPosition;
    public long selectedMessageId = -1;

    MegaChatRoom chatRoom;

    public long idChat;

    boolean noMoreNoSentMessages = false;

    ChatController chatC;
    boolean scrollingUp = false;

    long myUserHandle;

    ActionBar aB;
    Toolbar tB;

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;

    boolean editingMessage = false;
    MegaChatMessage messageToEdit = null;

    GestureDetectorCompat detector;

    CoordinatorLayout fragmentContainer;
    RelativeLayout writingContainerLayout;
    RelativeLayout writingLayout;
    RelativeLayout disabledWritingLayout;
    RelativeLayout chatRelativeLayout;
    RelativeLayout userTypingLayout;
    TextView userTypingText;
    TextView userTypingName;
    boolean sendIsTyping=true;
    long userTypingTimeStamp = -1;
//    TextView inviteText;
    ImageButton keyboardButton;
    EmojiconEditText textChat;
    ImageButton sendIcon;
    RelativeLayout messagesContainerLayout;
    ScrollView emptyScrollView;

    FloatingActionButton fab;
    FrameLayout emojiKeyboardLayout;

    RecyclerView listView;
    NpaLinearLayoutManager mLayoutManager;

    ChatActivityLollipop chatActivity;
    String myMail;

    MenuItem callMenuItem;
    MenuItem videoMenuItem;
    MenuItem inviteMenuItem;
    MenuItem clearHistoryMenuItem;
    MenuItem contactInfoMenuItem;
    MenuItem leaveMenuItem;

    LinearLayout megaInfoEmptyLayout;
    LinearLayout confidentialityEmptyLayout;
    LinearLayout authenticityEmptyLayout;
    TextView megaInfoTextView;
    TextView confidentialityTextView;
    TextView authenticityTextView;
    TextView megaInfoTitle;
    TextView authenticityTitle;
    TextView confidentialityTitle;

    boolean focusChanged=false;

    String intentAction;
    MegaChatLollipopAdapter adapter;

    int stateHistory;

    DatabaseHandler dbH = null;

    int keyboardSize = -1;
    int firstSize = -1;

    boolean emojiKeyboardShown = false;
    boolean softKeyboardShown = false;

    ArrayList<AndroidMegaChatMessage> messages;
    ArrayList<AndroidMegaChatMessage> bufferMessages;
    ArrayList<AndroidMegaChatMessage> bufferManualSending;
    ArrayList<AndroidMegaChatMessage> bufferSending;

    View.OnFocusChangeListener focus = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            log("onFocusChange");
            if(!focusChanged){
                focusChanged = true;
            }
        }
    };

    private ActionMode actionMode;

    @Override
    public void onEmojiconClicked(Emojicon emojicon) {
        EmojiconsFragment.input(textChat, emojicon);
    }

    @Override
    public void onEmojiconBackspaceClicked(View v) {
        EmojiconsFragment.backspace(textChat);
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

    ArrayList<UserTyping> usersTyping;
    List<UserTyping> usersTypingSync;

    private class RecyclerViewOnGestureListener extends GestureDetector.SimpleOnGestureListener {

        public void onLongPress(MotionEvent e) {
            log("onLongPress");
            if(megaChatApi.isSignalActivityRequired()){
                megaChatApi.signalPresenceActivity();
            }

            View view = listView.findChildViewUnder(e.getX(), e.getY());
            int position = listView.getChildLayoutPosition(view);

            if (!adapter.isMultipleSelect()){
                adapter.setMultipleSelect(true);

                actionMode = startSupportActionMode(new ActionBarCallBack());

                itemClick(position);
            }

            super.onLongPress(e);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            log("onSingleTapUp");

            if(megaChatApi.isSignalActivityRequired()){
                megaChatApi.signalPresenceActivity();
            }

            View view = listView.findChildViewUnder(e.getX(), e.getY());
            int position = listView.getChildLayoutPosition(view);
            itemClick(position);
            return true;
        }
    }

    public void showLoadingDialog(){
        dialog = new ProgressDialog(this);
        dialog.setTitle("Calculating...");
        dialog.setMessage("Please wait...");
        dialog.setIndeterminate(true);
        dialog.show();
    }

    public void showGroupInfoActivity(){
        log("showGroupInfoActivity");
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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");
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

        dbH = DatabaseHandler.getDbHandler(this);

        detector = new GestureDetectorCompat(this, new RecyclerViewOnGestureListener());

        chatActivity = this;
        chatC = new ChatController(chatActivity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
        }

        setContentView(R.layout.activity_chat);

        //Set toolbar
        tB = (Toolbar) findViewById(R.id.toolbar_chat);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
//		aB.setHomeAsUpIndicator(R.drawable.ic_menu_white);
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);

        tB.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                showGroupInfoActivity();
            }
        });

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density  = getResources().getDisplayMetrics().density;

        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);

        fragmentContainer = (CoordinatorLayout) findViewById(R.id.fragment_container_chat);

        writingContainerLayout = (RelativeLayout) findViewById(R.id.writing_container_layout_chat_layout);

        writingLayout = (RelativeLayout) findViewById(R.id.writing_linear_layout_chat);
        disabledWritingLayout = (RelativeLayout) findViewById(R.id.writing_disabled_linear_layout_chat);

        ///Empty screen
        emptyScrollView = (ScrollView) findViewById(R.id.layout_empty_scroll_view);
//        emptyScrollView.setVisibility(View.VISIBLE);

        megaInfoEmptyLayout = (LinearLayout) findViewById(R.id.mega_info_layout);
        RelativeLayout.LayoutParams megaInfoParams = (RelativeLayout.LayoutParams)megaInfoEmptyLayout.getLayoutParams();
        megaInfoParams.setMargins(Util.scaleWidthPx(36, outMetrics), Util.scaleHeightPx(40, outMetrics), Util.scaleWidthPx(36, outMetrics), Util.scaleHeightPx(24, outMetrics));
        megaInfoEmptyLayout.setLayoutParams(megaInfoParams);

        confidentialityEmptyLayout = (LinearLayout) findViewById(R.id.confidentiality_info);
        RelativeLayout.LayoutParams megaConfidentialityParams = (RelativeLayout.LayoutParams)confidentialityEmptyLayout.getLayoutParams();
        megaConfidentialityParams.setMargins(Util.scaleWidthPx(36, outMetrics), 0, Util.scaleWidthPx(36, outMetrics), Util.scaleHeightPx(24, outMetrics));
        confidentialityEmptyLayout.setLayoutParams(megaConfidentialityParams);

        authenticityEmptyLayout = (LinearLayout) findViewById(R.id.authenticity_info);
        RelativeLayout.LayoutParams megaAuthenticityParams = (RelativeLayout.LayoutParams)authenticityEmptyLayout.getLayoutParams();
        megaAuthenticityParams.setMargins(Util.scaleWidthPx(36, outMetrics), 0, Util.scaleWidthPx(36, outMetrics), Util.scaleHeightPx(24, outMetrics));
        authenticityEmptyLayout.setLayoutParams(megaAuthenticityParams);

        megaInfoTitle = (TextView) findViewById(R.id.mega_title);
        RelativeLayout.LayoutParams megaTitleParams = (RelativeLayout.LayoutParams)megaInfoTitle.getLayoutParams();
        megaTitleParams.setMargins(Util.scaleWidthPx(24, outMetrics), 0, 0, 0);
        megaInfoTitle.setLayoutParams(megaTitleParams);

        confidentialityTitle = (TextView) findViewById(R.id.confidentiality_title);
        RelativeLayout.LayoutParams confidentialityTitleParams = (RelativeLayout.LayoutParams)confidentialityTitle.getLayoutParams();
        confidentialityTitleParams.setMargins(Util.scaleWidthPx(24, outMetrics), 0, 0, 0);
        confidentialityTitle.setLayoutParams(confidentialityTitleParams);

        authenticityTitle = (TextView) findViewById(R.id.authenticity_title);
        RelativeLayout.LayoutParams authenticityTitleParams = (RelativeLayout.LayoutParams)authenticityTitle.getLayoutParams();
        authenticityTitleParams.setMargins(Util.scaleWidthPx(24, outMetrics), 0, 0, 0);
        authenticityTitle.setLayoutParams(authenticityTitleParams);

        megaInfoTextView = (TextView) findViewById(R.id.mega_info);
        RelativeLayout.LayoutParams megaTextParams = (RelativeLayout.LayoutParams)megaInfoTextView.getLayoutParams();
        megaTextParams.setMargins(Util.scaleWidthPx(24, outMetrics), 0, 0, 0);
        megaInfoTextView.setLayoutParams(megaTextParams);

        confidentialityTextView = (TextView) findViewById(R.id.confidentiality_text);
        RelativeLayout.LayoutParams confidentialityTextParams = (RelativeLayout.LayoutParams)confidentialityTextView.getLayoutParams();
        confidentialityTextParams.setMargins(Util.scaleWidthPx(24, outMetrics), 0, 0, 0);
        confidentialityTextView.setLayoutParams(confidentialityTextParams);

        authenticityTextView = (TextView) findViewById(R.id.authenticity_text);
        RelativeLayout.LayoutParams authenticityTextParams = (RelativeLayout.LayoutParams)authenticityTextView.getLayoutParams();
        authenticityTextParams.setMargins(Util.scaleWidthPx(24, outMetrics), 0, 0, 0);
        authenticityTextView.setLayoutParams(authenticityTextParams);

        keyboardButton = (ImageButton) findViewById(R.id.keyboard_icon_chat);
        textChat = (EmojiconEditText) findViewById(R.id.edit_text_chat);
        keyboardButton.setOnClickListener(this);

        textChat.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {}

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                log("onTextChanged: " + s + ", " + start + ", " + before + ", " + count);

                if (s != null) {
                    if (s.length() > 0) {
                        String temp = s.toString();
                        if(temp.trim().length()>0){
                            sendIcon.setVisibility(View.VISIBLE);
                        }
                        else{
                            sendIcon.setVisibility(View.GONE);
                        }
                    }
                    else {
                        sendIcon.setVisibility(View.GONE);
                    }
                }
                else{
                    sendIcon.setVisibility(View.GONE);
                }

                if(sendIsTyping){
                    log("Send is typing notification");
                    sendIsTyping=false;
                    megaChatApi.sendTypingNotification(chatRoom.getChatId());

                    int interval = 4000;
                    Runnable runnable = new Runnable(){
                        public void run() {
                            sendIsTyping=true;
                        }
                    };
                    handlerSend = new Handler();
                    handlerSend.postDelayed(runnable, interval);
                }

                if(megaChatApi.isSignalActivityRequired()){
                    megaChatApi.signalPresenceActivity();
                }
            }
        });

        textChat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (emojiKeyboardShown){
//                    int inputType = textChat.getInputType();
//                    textChat.setInputType(InputType.TYPE_NULL);
//                    textChat.onTouchEvent(event);
//                    textChat.setInputType(inputType);
//
//                    float x = event.getX();
//                    float y = event.getY();
//
//                    int touchPosition = textChat.getOffsetForPosition(x, y);
//                    Toast.makeText(ChatActivityLollipop.this, "X: " + x + "__ Y " + y + "__TOUCHPOSITION: " + touchPosition, Toast.LENGTH_SHORT).show();
//                    if (touchPosition  > 0){
//                        textChat.setSelection(touchPosition);
//                    }
////                    InputMethodManager imm = (InputMethodManager) getSystemService(ChatActivityLollipop.this.INPUT_METHOD_SERVICE);
////                    imm.hideSoftInputFromWindow(textChat.getWindowToken(), 0);
//
//                    return true;

                    removeEmojiconFragment();
                }
                return false;
            }
        });

        chatRelativeLayout  = (RelativeLayout) findViewById(R.id.relative_chat_layout);

        sendIcon = (ImageButton) findViewById(R.id.send_message_icon_chat);
        sendIcon.setOnClickListener(this);
        sendIcon.setVisibility(View.GONE);

        listView = (RecyclerView) findViewById(R.id.messages_chat_list_view);
        listView.setClipToPadding(false);;
        listView.setNestedScrollingEnabled(false);
        ((SimpleItemAnimator) listView.getItemAnimator()).setSupportsChangeAnimations(false);

        mLayoutManager = new NpaLinearLayoutManager(this);
        mLayoutManager.setStackFromEnd(true);
        listView.setLayoutManager(mLayoutManager);
        listView.addOnItemTouchListener(this);

        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // Get the first visible item
//                            int firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();

                if(stateHistory!=MegaChatApi.SOURCE_NONE){
                    if (dy > 0) {
                        // Scrolling up
                        scrollingUp = true;
                    } else {
                        // Scrolling down
                        scrollingUp = false;
                    }

                    if(!scrollingUp){
                        int pos = mLayoutManager.findFirstVisibleItemPosition();
//                if(mLayoutManager.findViewByPosition(pos).getTop()==0 && pos==0){
//                    showSnackbar("loadMoreMessages!!!");
//                }

                        if(pos<=NUMBER_MESSAGES_BEFORE_LOAD&&getMoreHistory){
                            if(megaChatApi.isSignalActivityRequired()){
                                megaChatApi.signalPresenceActivity();
                            }
                            stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
                            getMoreHistory = false;
                            log("Get more history------------------------");
                        }
                    }
                }
            }
        });
        listView.setAdapter(null);
        adapter = null;

        messagesContainerLayout = (RelativeLayout) findViewById(R.id.message_container_chat_layout);

        userTypingLayout = (RelativeLayout) findViewById(R.id.user_typing_layout);
        userTypingLayout.setVisibility(View.GONE);
        userTypingText = (TextView) findViewById(R.id.user_typing_text);
        userTypingName = (TextView) findViewById(R.id.user_typing_name);

        userTypingName.setMaxWidth(Util.scaleWidthPx(165, outMetrics));

        fab = (FloatingActionButton) findViewById(R.id.fab_chat);
        fab.setOnClickListener(this);

        emojiKeyboardLayout = (FrameLayout) findViewById(R.id.chat_emoji_keyboard);

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        ViewTreeObserver viewTreeObserver = fragmentContainer.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {

                @Override
                public void onGlobalLayout() {
                    log("onGlobalLayout");
                    InputMethodManager imm = (InputMethodManager) ChatActivityLollipop.this.getSystemService(Context.INPUT_METHOD_SERVICE);

                    if (firstSize == -1){
                        if (messagesContainerLayout != null){
                            firstSize = messagesContainerLayout.getHeight();
                        }
                    }
                    else {
                        if (keyboardSize == -1) {
                            if (imm.isAcceptingText()) {
                                if (messagesContainerLayout != null) {
                                    keyboardSize = firstSize - messagesContainerLayout.getHeight();
                                }
                            }
                        }
                    }

                    if ((firstSize - messagesContainerLayout.getHeight()) > 150 ) { //Every keyboard is at least 180 px height
                        if (!emojiKeyboardShown){
                            softKeyboardShown = true;
                        }
                    }
                    else{
                        softKeyboardShown = false;
                    }

                    if (shouldShowEmojiKeyboard){
                        setEmojiconFragment(false);
                        shouldShowEmojiKeyboard = false;
                    }
                }
            });
        }
        Intent newIntent = getIntent();

        if (newIntent != null){
            intentAction = newIntent.getAction();
            if (intentAction != null){

                idChat = newIntent.getLongExtra("CHAT_ID", -1);
//                    idChat=8179160514871859886L;
                myMail = megaApi.getMyEmail();
                myUserHandle = megaChatApi.getMyUserHandle();

                log("Show empty screen");
                chatRelativeLayout.setVisibility(View.GONE);
                emptyScrollView.setVisibility(View.VISIBLE);

                if(savedInstanceState!=null) {
                    log("Bundle is NOT NULL");
                    selectedMessageId = savedInstanceState.getLong("selectedMessageId", -1);
                    log("Handle of the message: "+selectedMessageId);
                    selectedPosition = savedInstanceState.getInt("selectedPosition", -1);
                }

                if(idChat!=-1) {
                    //REcover chat
                    log("Recover chat with id: " + idChat);
                    chatRoom = megaChatApi.getChatRoom(idChat);
                    if(chatRoom==null){
                        log("Chatroom is NULL - finisg activity!!");
                        finish();
                    }

                    log("Call to open chat");
                    boolean result = megaChatApi.openChatRoom(idChat, this);

                    if(!result){
                        log("----Error on openChatRoom");
                        if(errorOpenChatDialog==null){
                            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
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
                    else{
                        MegaApplication.setOpenChatId(idChat);
                        messages = new ArrayList<AndroidMegaChatMessage>();
                        bufferMessages = new ArrayList<AndroidMegaChatMessage>();
                        bufferManualSending = new ArrayList<AndroidMegaChatMessage>();
                        bufferSending = new ArrayList<AndroidMegaChatMessage>();

                        log("Result of open chat: " + result);

                        int permission = chatRoom.getOwnPrivilege();
                        aB.setTitle(chatRoom.getTitle());

                        if (chatRoom.isGroup()) {
                            log("Check permissions group chat");
                            if(permission==MegaChatRoom.PRIV_RO) {
                                log("Permission RO");
                                writingContainerLayout.setVisibility(View.GONE);
                                aB.setSubtitle(getString(R.string.observer_permission_label_participants_panel));
                            }
                            else{
                                log("permission: "+permission);
                            }
                        }
                        else{
                            log("Check permissions one to one chat");
                            if(permission==MegaChatRoom.PRIV_RO) {
                                log("Permission RO");
                                writingContainerLayout.setVisibility(View.GONE);
                                aB.setSubtitle(getString(R.string.observer_permission_label_participants_panel));
                            }
                            else{
                                long userHandle = chatRoom.getPeerHandle(0);
                                setStatus(userHandle);
                            }
                        }

                        if (intentAction.equals(Constants.ACTION_CHAT_NEW)) {
                            log("ACTION_CHAT_NEW");

                            listView.setVisibility(View.GONE);
                            chatRelativeLayout.setVisibility(View.GONE);
                            emptyScrollView.setVisibility(View.VISIBLE);
                            //                    inviteText.setVisibility(View.VISIBLE);
                            textChat.setOnFocusChangeListener(focus);
                        } else if (intentAction.equals(Constants.ACTION_CHAT_SHOW_MESSAGES)) {
                            log("ACTION_CHAT_SHOW_MESSAGES");

                            long unread = chatRoom.getUnreadCount();
                            //                        stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
                            if (unread == 0) {
                                lastMessageSeen = null;
                                lastSeenReceived = true;
                                stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
                            } else {
                                lastMessageSeen = megaChatApi.getLastMessageSeen(idChat);
                                if (lastMessageSeen != null) {
                                    log("Id of last message seen: " + lastMessageSeen.getMsgId());
                                } else {
                                    log("Error the last message seen shouldn't be NULL");
                                }

                                lastSeenReceived = false;
                                if (unread < 0) {
                                    log("A->Load history of " + chatRoom.getUnreadCount());
                                    long unreadAbs = Math.abs(unread);
                                    stateHistory = megaChatApi.loadMessages(idChat, (int) unreadAbs);
                                } else if (unread >= 0 && unread <= NUMBER_MESSAGES_TO_LOAD) {
                                    log("B->Load history of " + chatRoom.getUnreadCount());
                                    stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
                                } else if (unread < NUMBER_MESSAGES_TO_LOAD) {
                                    log("C->Load history of " + chatRoom.getUnreadCount());
                                    stateHistory = megaChatApi.loadMessages(idChat, chatRoom.getUnreadCount());
                                } else {
                                    log("D->Load history of " + chatRoom.getUnreadCount());
                                    stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
                                }
                                listView.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                }
                else{
                    log("Chat ID -1 error");
                }
            }
        }
        else{
            log("INTENT is NULL");
        }
    }

    public void setStatus(long userHandle){

        int state = megaChatApi.getUserOnlineStatus(userHandle);

        if(state == MegaChatApi.STATUS_ONLINE){
            log("This user is connected");
            aB.setSubtitle(getString(R.string.online_status));
        }
        else if(state == MegaChatApi.STATUS_AWAY){
            log("This user is away");
            aB.setSubtitle(getString(R.string.away_status));
        }
        else if(state == MegaChatApi.STATUS_BUSY){
            log("This user is busy");
            aB.setSubtitle(getString(R.string.busy_status));
        }
        else{
            log("This user status is: "+state);
            aB.setSubtitle(getString(R.string.offline_status));
        }
    }

    public int compareTime(AndroidMegaChatMessage message, AndroidMegaChatMessage previous){

        if(previous!=null){

            Calendar cal = Util.calculateDateFromTimestamp(message.getMessage().getTimestamp());
            Calendar previousCal =  Util.calculateDateFromTimestamp(previous.getMessage().getTimestamp());

            TimeChatUtils tc = new TimeChatUtils(TimeChatUtils.TIME);

            int result = tc.compare(cal, previousCal);
            log("RESULTS compareTime: "+result);
            return result;
        }
        else{
            log("return -1");
            return -1;
        }
    }

    public int compareTime(long timeStamp, AndroidMegaChatMessage previous){

        if(previous!=null){

            Calendar cal = Util.calculateDateFromTimestamp(timeStamp);
            Calendar previousCal =  Util.calculateDateFromTimestamp(previous.getMessage().getTimestamp());

            TimeChatUtils tc = new TimeChatUtils(TimeChatUtils.TIME);

            int result = tc.compare(cal, previousCal);
            log("RESULTS compareTime: "+result);
            return result;
        }
        else{
            log("return -1");
            return -1;
        }
    }

    public int compareTime(long timeStamp, long previous){

        if(previous!=-1){

            Calendar cal = Util.calculateDateFromTimestamp(timeStamp);
            Calendar previousCal =  Util.calculateDateFromTimestamp(previous);

            TimeChatUtils tc = new TimeChatUtils(TimeChatUtils.TIME);

            int result = tc.compare(cal, previousCal);
            log("RESULTS compareTime: "+result);
            return result;
        }
        else{
            log("return -1");
            return -1;
        }
    }

    public int compareDate(AndroidMegaChatMessage message, AndroidMegaChatMessage previous){

        if(previous!=null){
            Calendar cal = Util.calculateDateFromTimestamp(message.getMessage().getTimestamp());
            Calendar previousCal =  Util.calculateDateFromTimestamp(previous.getMessage().getTimestamp());

            TimeChatUtils tc = new TimeChatUtils(TimeChatUtils.DATE);

            int result = tc.compare(cal, previousCal);
            log("RESULTS compareDate: "+result);
            return result;
        }
        else{
            log("return -1");
            return -1;
        }
    }

    public int compareDate(long timeStamp, AndroidMegaChatMessage previous){

        if(previous!=null){
            Calendar cal = Util.calculateDateFromTimestamp(timeStamp);
            Calendar previousCal =  Util.calculateDateFromTimestamp(previous.getMessage().getTimestamp());

            TimeChatUtils tc = new TimeChatUtils(TimeChatUtils.DATE);

            int result = tc.compare(cal, previousCal);
            log("RESULTS compareDate: "+result);
            return result;
        }
        else{
            log("return -1");
            return -1;
        }
    }

    public int compareDate(long timeStamp, long previous){

        if(previous!=-1){
            Calendar cal = Util.calculateDateFromTimestamp(timeStamp);
            Calendar previousCal =  Util.calculateDateFromTimestamp(previous);

            TimeChatUtils tc = new TimeChatUtils(TimeChatUtils.DATE);

            int result = tc.compare(cal, previousCal);
            log("RESULTS compareDate: "+result);
            return result;
        }
        else{
            log("return -1");
            return -1;
        }
    }

    EmojiconsFragment emojiconsFragment = null;
    boolean firstTimeEmoji = true;
    boolean shouldShowEmojiKeyboard = false;

    private void setEmojiconFragment(boolean useSystemDefault) {
        log("setEmojiconFragment(" + useSystemDefault + ")");
        if (firstTimeEmoji) {
            emojiconsFragment = EmojiconsFragment.newInstance(useSystemDefault);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.chat_emoji_keyboard, emojiconsFragment)
                    .commitNow();
            firstTimeEmoji = false;
        }

        if (keyboardSize != -1) {
            if (keyboardSize == 0){
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) emojiKeyboardLayout.getLayoutParams();
                params.height = 660;
                emojiKeyboardLayout.setLayoutParams(params);
            }
            else {
                if (emojiKeyboardLayout != null) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) emojiKeyboardLayout.getLayoutParams();
                    params.height = keyboardSize;
                    emojiKeyboardLayout.setLayoutParams(params);
                }
            }
        }
        else{
            if (emojiKeyboardLayout != null) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) emojiKeyboardLayout.getLayoutParams();
                params.height = 660;
                emojiKeyboardLayout.setLayoutParams(params);
            }
        }
        emojiKeyboardShown = true;
    }

    private void removeEmojiconFragment(){
        log("removeEmojiconFragment");
        if (emojiconsFragment != null){
//            getSupportFragmentManager().beginTransaction().remove(emojiconsFragment).commitNow();

            if (emojiKeyboardLayout != null) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) emojiKeyboardLayout.getLayoutParams();
                params.height = 0;
                emojiKeyboardLayout.setLayoutParams(params);
            }
        }
        emojiKeyboardShown = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenuLollipop");

        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_action, menu);

        callMenuItem = menu.findItem(R.id.cab_menu_call_chat);
        videoMenuItem = menu.findItem(R.id.cab_menu_video_chat);
        inviteMenuItem = menu.findItem(R.id.cab_menu_invite_chat);
        clearHistoryMenuItem = menu.findItem(R.id.cab_menu_clear_history_chat);
        contactInfoMenuItem = menu.findItem(R.id.cab_menu_contact_info_chat);
        leaveMenuItem = menu.findItem(R.id.cab_menu_leave_chat);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        log("onPrepareOptionsMenu");
        if(chatRoom!=null){
            int permission = chatRoom.getOwnPrivilege();
            log("Permission in the chat: "+permission);
            if(chatRoom.isGroup()){

                if(permission==MegaChatRoom.PRIV_MODERATOR) {
                    inviteMenuItem.setVisible(true);

                    int lastMessageIndex = messages.size()-1;
                    if(lastMessageIndex>=0){
                        AndroidMegaChatMessage lastMessage = messages.get(lastMessageIndex);
                        if(lastMessage.getMessage().getType()==MegaChatMessage.TYPE_TRUNCATE){
                            log("Last message is TRUNCATE");
                            clearHistoryMenuItem.setVisible(false);
                        }
                        else{
                            log("Last message is NOT TRUNCATE");
                            clearHistoryMenuItem.setVisible(true);
                        }
                    }
                    else{
                        clearHistoryMenuItem.setVisible(false);
                    }

                    leaveMenuItem.setVisible(true);
                    callMenuItem.setVisible(true);
                    videoMenuItem.setVisible(true);

                }
                else if(permission==MegaChatRoom.PRIV_RM) {
                    log("Group chat PRIV_RM");
                    leaveMenuItem.setVisible(false);
                    callMenuItem.setVisible(false);
                    videoMenuItem.setVisible(false);
                    clearHistoryMenuItem.setVisible(false);
                    inviteMenuItem.setVisible(false);
                }
                else if(permission==MegaChatRoom.PRIV_RO) {
                    log("Group chat PRIV_RM");
                    leaveMenuItem.setVisible(true);
                    callMenuItem.setVisible(false);
                    videoMenuItem.setVisible(false);
                    clearHistoryMenuItem.setVisible(false);
                    inviteMenuItem.setVisible(false);
                }
                else{
                    log("Permission: "+permission);
                    leaveMenuItem.setVisible(true);
                    callMenuItem.setVisible(true);
                    videoMenuItem.setVisible(true);
                    clearHistoryMenuItem.setVisible(false);
                    inviteMenuItem.setVisible(false);
                }

                contactInfoMenuItem.setTitle(getString(R.string.group_chat_info_label));
                contactInfoMenuItem.setVisible(true);
            }
            else{
                inviteMenuItem.setVisible(false);
                if(permission==MegaChatRoom.PRIV_RO) {
                    clearHistoryMenuItem.setVisible(false);
                    contactInfoMenuItem.setVisible(false);
                    callMenuItem.setVisible(false);
                    videoMenuItem.setVisible(false);
                }
                else{
                    clearHistoryMenuItem.setVisible(true);
                    contactInfoMenuItem.setTitle(getString(R.string.contact_properties_activity));
                    contactInfoMenuItem.setVisible(true);
                }
                leaveMenuItem.setVisible(false);
            }
        }
        else{
            log("Chatroom NULL on create menu");
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home: {
                finish();
                break;
            }
            case R.id.cab_menu_call_chat:{
                showSnackbar("Coming soon...");
                break;
            }
            case R.id.cab_menu_video_chat:{
                showSnackbar("Coming soon...");
                break;
            }
            case R.id.cab_menu_invite_chat:{
                chooseAddParticipantDialog();
                break;
            }
            case R.id.cab_menu_contact_info_chat:{
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
                log("Clear history selected!");
                showConfirmationClearChat(chatRoom);
                break;
            }
            case R.id.cab_menu_leave_chat:{
                log("Leave selected!");
                showConfirmationLeaveChat(chatRoom);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void chooseAddParticipantDialog(){
        log("chooseAddContactDialog");

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        Intent in = new Intent(this, AddContactActivityLollipop.class);
        in.putExtra("contactType", Constants.CONTACT_TYPE_MEGA);
        startActivityForResult(in, Constants.REQUEST_ADD_PARTICIPANTS);

    }

    public void chooseContactsDialog(){
        log("chooseContactsDialog");

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        Intent in = new Intent(this, AddContactActivityLollipop.class);
        in.putExtra("contactType", Constants.CONTACT_TYPE_MEGA);
        startActivityForResult(in, Constants.REQUEST_SEND_CONTACTS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        log("onActivityResult, resultCode: " + resultCode);

        if (requestCode == Constants.REQUEST_ADD_PARTICIPANTS && resultCode == RESULT_OK) {
            log("onActivityResult REQUEST_ADD_PARTICIPANTS OK");

            if (intent == null) {
                log("Return.....");
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
                    log("Add multiple participants "+contactsData.size());
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
        else if (requestCode == Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
            log("onActivityResult REQUEST_CODE_SELECT_IMPORT_FOLDER OK");

            if(!Util.isOnline(this)) {
                try{
                    statusDialog.dismiss();
                } catch(Exception ex) {};

                Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
                return;
            }

            final long toHandle = intent.getLongExtra("IMPORT_TO", 0);

            MegaNode target = null;
            target = megaApi.getNodeByHandle(toHandle);
            if(target == null){
                target = megaApi.getRootNode();
            }
            log("TARGET: " + target.getName() + "and handle: " + target.getHandle());

            MegaChatMessage message = megaChatApi.getMessage(idChat, selectedMessageId);
            if(message!=null){
                statusDialog = new ProgressDialog(this);
                statusDialog.setMessage(getString(R.string.general_importing));
                statusDialog.show();

                MegaNodeList nodeList = message.getMegaNodeList();
                for(int i=0;i<nodeList.size();i++){
                    MegaNode document = nodeList.get(i);
                    if (document != null) {
                        log("DOCUMENT: " + document.getName() + "_" + document.getHandle());
                        if (target != null) {
//                            MegaNode autNode = megaApi.authorizeNode(document);

                            megaApi.copyNode(document, target, this);
                        } else {
                            log("TARGET: null");
                            Snackbar.make(fragmentContainer, getString(R.string.import_success_error), Snackbar.LENGTH_LONG).show();
                        }
                    }
                    else{
                        log("DOCUMENT: null");
                        Snackbar.make(fragmentContainer, getString(R.string.import_success_error), Snackbar.LENGTH_LONG).show();
                    }
                }
            }
            else{
                log("MESSAGE is null");
                Snackbar.make(fragmentContainer, getString(R.string.import_success_error), Snackbar.LENGTH_LONG).show();
            }
        }
        else if (requestCode == Constants.REQUEST_SEND_CONTACTS && resultCode == RESULT_OK) {
            log("onActivityResult REQUEST_SEND_CONTACTS OK");

            final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);
            if (contactsData != null) {
                MegaHandleList handleList = MegaHandleList.createInstance();
                for(int i=0; i<contactsData.size();i++){
                    MegaUser user = megaApi.getContact(contactsData.get(i));
                    if (user != null) {
                        handleList.addMegaHandle(user.getHandle());

                    }
                }
                MegaChatMessage contactMessage = megaChatApi.attachContacts(idChat, handleList);
                if(contactMessage!=null){
                    sendMessage(contactMessage);
                }
            }
        }
        else if (requestCode == Constants.REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
            log("requestCode == REQUEST_CODE_SELECT_FILE");
            if (intent == null) {
                log("Return.....");
                return;
            }

//            final ArrayList<String> selectedContacts = intent.getStringArrayListExtra("SELECTED_CONTACTS");
//            final long fileHandle = intent.getLongExtra("NODE_HANDLES", 0);
//            MegaNode node = megaApi.getNodeByHandle(fileHandle);
//            if(node!=null){
//                log("Node to send: "+node.getName());
//                MegaNodeList nodeList = MegaNodeList.createInstance();
//                nodeList.addNode(node);
//                megaChatApi.attachNodes(idChat, nodeList, this);
//
//            }


            long handles[] = intent.getLongArrayExtra("NODE_HANDLES");
            log("Number of files to send: "+handles.length);

            MegaNodeList nodeList = MegaNodeList.createInstance();
            for(int i=0; i<handles.length; i++){
                MegaNode node = megaApi.getNodeByHandle(handles[i]);
                if(node!=null){
                    log("Node to send: "+node.getName());
                    nodeList.addNode(node);
                }
            }
            megaChatApi.attachNodes(idChat, nodeList, this);
            log("---- no more files to send");
        }
        else if (requestCode == Constants.REQUEST_CODE_GET && resultCode == RESULT_OK) {
            if (intent == null) {
                log("Return.....");
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
        else{
            log("Error onActivityResult");
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

    public void showConfirmationClearChat(final MegaChatRoom c){
        log("showConfirmationClearChat");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        log("Clear chat!");
//						megaChatApi.truncateChat(chatHandle, MegaChatHandle.MEGACHAT_INVALID_HANDLE);
                        log("Clear history selected!");
                        chatC.clearHistory(c);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        String message= getResources().getString(R.string.confirmation_clear_group_chat);
        builder.setTitle(R.string.title_confirmation_clear_group_chat);
        builder.setMessage(message).setPositiveButton(R.string.general_clear, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void showConfirmationLeaveChat (final MegaChatRoom c){
        log("showConfirmationLeaveChat");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE: {
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

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setTitle(getResources().getString(R.string.title_confirmation_leave_group_chat));
        String message= getResources().getString(R.string.confirmation_leave_group_chat);
        builder.setMessage(message).setPositiveButton(R.string.general_leave, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    @Override
    public void onBackPressed() {
        log("onBackPressedLollipop");

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        if (emojiKeyboardShown) {
            removeEmojiconFragment();
        }
        else{
            finish();
        }
    }

    public static void log(String message) {
        Util.log("ChatActivityLollipop", message);
    }

    @Override
    public void onClick(View v) {

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        switch (v.getId()) {
            case R.id.home:{
                break;
            }
			case R.id.send_message_icon_chat:{
                log("click on Send message");
                writingLayout.setClickable(false);
                String text = textChat.getText().toString();

                if(editingMessage){
                    editMessage(text);
                    log("Edited message: "+text);
                    clearSelections();
                    hideMultipleSelect();
                    actionMode.invalidate();
                }
                else{
                    log("Call to send message: "+text);
                    sendMessage(text);
                }

                textChat.getText().clear();

//                textChat.setFocusable(false);
//                textChat.setEnabled(false);
//                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) disabledWritingLayout.getLayoutParams();
//                params.height = writingLayout.getHeight();
//                disabledWritingLayout.setLayoutParams(params);
//                disabledWritingLayout.setVisibility(View.VISIBLE);

//                inviteText.setVisibility(View.GONE);
                break;
			}
            case R.id.keyboard_icon_chat:{
                log("open emoji keyboard:  " + emojiKeyboardShown);

                if (emojiKeyboardShown){
                    removeEmojiconFragment();
                    textChat.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(textChat, InputMethodManager.SHOW_IMPLICIT);
                }
                else{
                    InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);

                    if (softKeyboardShown){
                        log("imm.isAcceptingText()");
                        imm.hideSoftInputFromWindow(textChat.getWindowToken(), 0);
                        shouldShowEmojiKeyboard = true;
                    }
                    else{
                        setEmojiconFragment(false);
                    }
                }
                break;
            }
            case R.id.fab_chat:{
                showUploadPanel();
                break;
            }

		}
    }

    public void attachFromCloud(){
        log("attachFromCloud");
        ChatController chatC = new ChatController(this);
        chatC.pickFileToSend();
    }

    public void attachContact(){
        log("attachContact");
        chooseContactsDialog();
    }

    public void attachPhotoVideo(){
        log("attachPhotoVideo");
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("*/*");


        startActivityForResult(Intent.createChooser(intent, null), Constants.REQUEST_CODE_GET);
    }

    public void sendMessage(String text){
        log("sendMessage: "+text);

        MegaChatMessage msgSent = megaChatApi.sendMessage(idChat, text);
        AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(msgSent);
        int index = messages.size()-1;
        if(msgSent!=null){
            log("Sent message with id temp: "+msgSent.getTempId());
            log("State of the message: "+msgSent.getStatus());
            log("Index: "+index);
            if(index==-1){
                //First element
                log("First element!");
                messages.add(androidMsgSent);
                messages.get(0).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
            }
            else{
                //Not first element
                //Find where to add in the queue

                AndroidMegaChatMessage msg = messages.get(index);

                while(msg.isUploading()){
                    index--;
                    msg = messages.get(index);
                }

                while(msg.getMessage().getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL){
                    index--;
                    msg = messages.get(index);
                }
                index++;
                log("Add in position: "+index);

                messages.add(index, androidMsgSent);

                AndroidMegaChatMessage androidPreviousMessage = messages.get(index-1);
                log("previous message: "+androidPreviousMessage.getMessage().getContent());
                if(androidPreviousMessage.getMessage().getUserHandle()==myUserHandle) {
                    //The last two messages are mine
                    if(compareDate(androidMsgSent, androidPreviousMessage)==0){
                        //Same date
                        if(compareTime(androidMsgSent, androidPreviousMessage)==0){
                            messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                        }
                        else{
                            //Different minute
                            messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                        }
                    }
                    else{
                        //Different date
                        messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                    }
                }
                else{
                    //The last message is mine, the previous not
                    log("Modify not my message");
                    if (compareDate(androidMsgSent, androidPreviousMessage) == 0) {
                        if (compareTime(androidMsgSent, androidPreviousMessage) == 0) {
                            messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                        } else {
                            //Different minute
                            messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                        }
                    } else {
                        //Different date
                        messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                    }
                }
            }
            if (adapter == null){
                log("adapter NULL");
                adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
                adapter.setHasStableIds(true);
                listView.setLayoutManager(mLayoutManager);
                listView.setAdapter(adapter);
                adapter.setMessages(messages);
                if(adapter.getItemCount()>0){
                    listView.setVisibility(View.VISIBLE);
                    chatRelativeLayout.setVisibility(View.VISIBLE);
                    emptyScrollView.setVisibility(View.GONE);
                }
            }
            else{
                log("adapter is NOT null");
                final int indexToScroll = index;

                if(adapter.getItemCount()>0){
                    listView.setVisibility(View.VISIBLE);
                    chatRelativeLayout.setVisibility(View.VISIBLE);
                    emptyScrollView.setVisibility(View.GONE);
                }

                mLayoutManager.scrollToPositionWithOffset(indexToScroll-1,20);

                adapter.addMessage(messages, index);
            }
        }
        else{
            log("Error sending message!");
            //EL mensaje no se ha enviado, mostrar error al usuario pero no cambiar interfaz

        }
    }

    public void sendMessageUploading(AndroidMegaChatMessage androidMsgSent){
        log("sendMessageUploading");

        log("Name of the file uploading: "+androidMsgSent.getPendingMessage().getNames().get(0));

        int index = messages.size()-1;
        if(androidMsgSent!=null){

            if(index==-1){
                //First element
                log("First element!");
                messages.add(androidMsgSent);
                messages.get(0).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
            }
            else{
                //Not first element
                //Find where to add in the queue
                while(messages.get(index).getMessage().getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL){
                    index--;
                }
                index++;
                log("Add in position: "+index);

                messages.add(index, androidMsgSent);

                AndroidMegaChatMessage androidPreviousMessage = messages.get(index-1);
                log("previous message: "+androidPreviousMessage.getMessage().getContent());
                if(androidPreviousMessage.getMessage().getUserHandle()==myUserHandle) {
                    //The last two messages are mine
                    if(compareDate(androidMsgSent.getPendingMessage().getUploadTimestamp(), androidPreviousMessage)==0){
                        //Same date
                        if(compareTime(androidMsgSent.getPendingMessage().getUploadTimestamp(), androidPreviousMessage)==0){
                            messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                        }
                        else{
                            //Different minute
                            messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                        }
                    }
                    else{
                        //Different date
                        messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                    }
                }
                else{
                    //The last message is mine, the previous not
                    log("Modify not my message");
                    if (compareDate(androidMsgSent.getPendingMessage().getUploadTimestamp(), androidPreviousMessage) == 0) {
                        if (compareTime(androidMsgSent.getPendingMessage().getUploadTimestamp(), androidPreviousMessage) == 0) {
                            messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                        } else {
                            //Different minute
                            messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                        }
                    } else {
                        //Different date
                        messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                    }
                }
            }
            if (adapter == null){
                log("adapter NULL");
                adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
                adapter.setHasStableIds(true);
                listView.setLayoutManager(mLayoutManager);
                listView.setAdapter(adapter);
                adapter.setMessages(messages);
                if(adapter.getItemCount()>0){
                    listView.setVisibility(View.VISIBLE);
                    chatRelativeLayout.setVisibility(View.VISIBLE);
                    emptyScrollView.setVisibility(View.GONE);
                }
            }
            else{
                log("adapter is NOT null");
                adapter.addMessage(messages, index);
                final int indexToScroll = index;

                mLayoutManager.scrollToPositionWithOffset(indexToScroll,20);
//                Handler handler = new Handler();
//                handler.postDelayed(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        log("sendMessage: Now I update the recyclerview (send): "+indexToScroll);
//
//
//                    }
//                }, 100);
            }
        }
        else{
            log("Error sending message (2)!");
            //EL mensaje no se ha enviado, mostrar error al usuario pero no cambiar interfaz

        }
    }

    public void sendMessage(MegaChatMessage msgSent){
        log("sendMessage: msgSent");
        AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(msgSent);
        int index = messages.size()-1;
        if(msgSent!=null){
            log("Sent message with id temp: "+msgSent.getTempId());
            log("State of the message: "+msgSent.getStatus());
            log("Index: "+index);
            if(index==-1){
                //First element
                log("First element!");
                messages.add(androidMsgSent);
                messages.get(0).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
            }
            else{
                //Not first element
                //Find where to add in the queue
                while(messages.get(index).getMessage().getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL){
                    index--;
                }
                index++;
                log("Add in position: "+index);

                messages.add(index, androidMsgSent);

                AndroidMegaChatMessage androidPreviousMessage = messages.get(index-1);
                log("previous message: "+androidPreviousMessage.getMessage().getContent());
                if(androidPreviousMessage.getMessage().getUserHandle()==myUserHandle) {
                    //The last two messages are mine
                    if(compareDate(androidMsgSent, androidPreviousMessage)==0){
                        //Same date
                        if(compareTime(androidMsgSent, androidPreviousMessage)==0){
                            messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                        }
                        else{
                            //Different minute
                            messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                        }
                    }
                    else{
                        //Different date
                        messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                    }
                }
                else{
                    //The last message is mine, the previous not
                    log("Modify not my message");
                    if (compareDate(androidMsgSent, androidPreviousMessage) == 0) {
                        if (compareTime(androidMsgSent, androidPreviousMessage) == 0) {
                            messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                        } else {
                            //Different minute
                            messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                        }
                    } else {
                        //Different date
                        messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                    }
                }
            }
            if (adapter == null){
                log("adapter NULL");
                adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
                adapter.setHasStableIds(true);
                listView.setLayoutManager(mLayoutManager);
                listView.setAdapter(adapter);
                adapter.setMessages(messages);
                if(adapter.getItemCount()>0){
                    listView.setVisibility(View.VISIBLE);
                    chatRelativeLayout.setVisibility(View.VISIBLE);
                    emptyScrollView.setVisibility(View.GONE);
                }
            }
            else{
                log("adapter is NOT null");
                adapter.addMessage(messages, index);
                final int indexToScroll = index;

                mLayoutManager.scrollToPositionWithOffset(indexToScroll,20);
//                Handler handler = new Handler();
//                handler.postDelayed(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        log("sendMessage: Now I update the recyclerview (send): "+indexToScroll);
//
//
//                    }
//                }, 100);
            }
        }
        else{
            log("Error sending message (2)!");
            //EL mensaje no se ha enviado, mostrar error al usuario pero no cambiar interfaz

        }
    }


    public void editMessage(String text){
        log("editMessage: "+text);
        MegaChatMessage msgEdited = null;

        if(messageToEdit.getMsgId()!=-1){
            msgEdited = megaChatApi.editMessage(idChat, messageToEdit.getMsgId(), text);
        }
        else{
            msgEdited = megaChatApi.editMessage(idChat, messageToEdit.getTempId(), text);
        }

        if(msgEdited!=null){
            log("Edited message: status: "+msgEdited.getStatus());
            AndroidMegaChatMessage androidMsgEdited = new AndroidMegaChatMessage(msgEdited);
            modifyMessageReceived(androidMsgEdited, false);
        }
        else{
            log("Message cannot be edited!");
            showSnackbar(getString(R.string.error_editing_message));
        }
    }

    public void editMessageMS(String text, MegaChatMessage messageToEdit){
        log("editMessageMS: "+text);
        MegaChatMessage msgEdited = null;

        if(messageToEdit.getMsgId()!=-1){
            msgEdited = megaChatApi.editMessage(idChat, messageToEdit.getMsgId(), text);
        }
        else{
            msgEdited = megaChatApi.editMessage(idChat, messageToEdit.getTempId(), text);
        }

        if(msgEdited!=null){
            log("Edited message: status: "+msgEdited.getStatus());
            AndroidMegaChatMessage androidMsgEdited = new AndroidMegaChatMessage(msgEdited);
            modifyMessageReceived(androidMsgEdited, false);
        }
        else{
            log("Message cannot be edited!");
            showSnackbar(getString(R.string.error_editing_message));
        }
    }

    public void showUploadPanel(){
        AttachmentUploadBottomSheetDialogFragment bottomSheetDialogFragment = new AttachmentUploadBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

//    public void hideUploadPanel(){
//        fab.setVisibility(View.VISIBLE);
//        uploadPanel.setVisibility(View.GONE);
//        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messagesContainerLayout.getLayoutParams();
//        params.addRule(RelativeLayout.ABOVE, R.id.writing_container_layout_chat_layout);
//        messagesContainerLayout.setLayoutParams(params);
//    }

    /////Multiselect/////
    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            ArrayList<AndroidMegaChatMessage> messagesSelected = adapter.getSelectedMessages();

            if(megaChatApi.isSignalActivityRequired()){
                megaChatApi.signalPresenceActivity();
            }

            switch(item.getItemId()){
//                case R.id.cab_menu_select_all:{
//                    selectAll();
//                    actionMode.invalidate();
//                    break;
//                }
//                case R.id.cab_menu_unselect_all:{
//                    clearSelections();
//                    hideMultipleSelect();
//                    actionMode.invalidate();
//                    break;
//                }
                case R.id.chat_cab_menu_edit:{
                    log("Edit text");
                    editingMessage = true;
                    messageToEdit = messagesSelected.get(0).getMessage();
                    textChat.setText(messageToEdit.getContent());
                    //Show keyboard

                    break;
                }
                case R.id.chat_cab_menu_copy:{
                    clearSelections();
                    hideMultipleSelect();

                    String text = "";

                    if(messagesSelected.size()==1){
                        AndroidMegaChatMessage message = messagesSelected.get(0);
                        text = chatC.createSingleManagementString(message, chatRoom);
                    }
                    else{
                        text = copyMessages(messagesSelected);
                    }

                    log("Copy: "+text);
                    if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setText(text);
                    } else {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
                        clipboard.setPrimaryClip(clip);
                    }

                    Snackbar.make(fragmentContainer, getString(R.string.messages_copied_clipboard), Snackbar.LENGTH_LONG).show();

                    break;
                }
                case R.id.chat_cab_menu_delete:{
                    clearSelections();
                    hideMultipleSelect();
                    //Delete
                    showConfirmationDeleteMessages(messagesSelected, chatRoom);
                    break;
                }
            }
            return false;
        }

        public String copyMessages(ArrayList<AndroidMegaChatMessage> messagesSelected){
            ChatController chatC = new ChatController(chatActivity);
            StringBuilder builder = new StringBuilder();

            for(int i=0;i<messagesSelected.size();i++){
                AndroidMegaChatMessage messageSelected = messagesSelected.get(i);
                builder.append("[");
                String timestamp = TimeChatUtils.formatShortDateTime(messageSelected.getMessage().getTimestamp());
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
            fab.setVisibility(View.GONE);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            log("onDEstroyActionMode");
            adapter.setMultipleSelect(false);
            textChat.getText().clear();
            editingMessage = false;
            clearSelections();
            fab.setVisibility(View.VISIBLE);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            List<AndroidMegaChatMessage> selected = adapter.getSelectedMessages();

            if (selected.size() !=0) {
//                MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
                if (selected.size() == 1) {
                    MegaChatMessage messageSelected= megaChatApi.getMessage(idChat, selected.get(0).getMessage().getMsgId());

                    if(messageSelected.getUserHandle()==myUserHandle){
                        if(messageSelected.isEditable()){
                            log("Message EDITABLE");
                            menu.findItem(R.id.chat_cab_menu_edit).setVisible(true);
                        }
                        else{
                            log("Message NOT EDITABLE");
                            menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                        }
                        if(messageSelected.isDeletable()){
                            log("Message DELETABLE");
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                        }
                        else{
                            log("Message NOT DELETABLE");
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                        }
                    }
                    else{
                        menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                        menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                    }

                    if(messageSelected.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT||messageSelected.getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){
                        menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                    }
                    else{
                        menu.findItem(R.id.chat_cab_menu_copy).setVisible(true);
                    }
                }
                else if (selected.size()==adapter.getItemCount()){
                    menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                    menu.findItem(R.id.chat_cab_menu_copy).setVisible(true);
                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                    for(int i=0; i<selected.size();i++){
                        if(selected.get(i).getMessage().getUserHandle()==myUserHandle){
                            if(!(selected.get(i).getMessage().isDeletable())){
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                                break;
                            }
                        }
                        else{
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                        }
                    }

                    for(int i=0; i<selected.size();i++){
                        if(selected.get(i).getMessage().getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT||selected.get(i).getMessage().getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){
                            menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                        }
                        else{
                            menu.findItem(R.id.chat_cab_menu_copy).setVisible(true);
                        }
                    }

                }
                else{
                    menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                    menu.findItem(R.id.chat_cab_menu_copy).setVisible(true);
                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                    for(int i=0; i<selected.size();i++){
                        if(selected.get(i).getMessage().getUserHandle()==myUserHandle){
                            if(!(selected.get(i).getMessage().isDeletable())){
                                log("onPrepareActionMode: not deletable");
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                                break;
                            }
                        }
                        else{
                            log("onPrepareActionMode: not MY message");
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                        }
                    }

                    for(int i=0; i<selected.size();i++){
                        if(selected.get(i).getMessage().getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT||selected.get(i).getMessage().getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){
                            menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                        }
                        else{
                            menu.findItem(R.id.chat_cab_menu_copy).setVisible(true);
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
        log("showConfirmationDeleteMessages");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        ChatController cC = new ChatController(chatActivity);
                        cC.deleteMessages(messages, chat);
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
        builder.setPositiveButton(R.string.context_delete, dialogClickListener)
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
//        if (actionMode == null || getActivity() == null) {
//            return;
//        }
        List<AndroidMegaChatMessage> messages = adapter.getSelectedMessages();

        actionMode.setTitle(getString(R.string.selected_items, messages.size()));

        try {
            actionMode.invalidate();
        } catch (NullPointerException e) {
            e.printStackTrace();
            log("oninvalidate error");
        }
    }

    /*
     * Disable selection
     */
    void hideMultipleSelect() {
        log("hideMultipleSelect");
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

    public void itemClick(int position) {
        log("itemClick: "+position);
        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        if (adapter.isMultipleSelect()){
            adapter.toggleSelection(position);

            List<AndroidMegaChatMessage> messages = adapter.getSelectedMessages();
            if (messages.size() > 0){
                updateActionModeTitle();
//                adapter.notifyDataSetChanged();
            }
            else{
                hideMultipleSelect();
            }
        }
        else{

            AndroidMegaChatMessage m = messages.get(position);
//            showMsgNotSentPanel(m);
            if(m!=null){
                if(m.isUploading()){
                    showUploadingAttachmentBottomSheet(m, position);
                }
                else{
                    if(m.getMessage().getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){

                        MegaNodeList nodeList = m.getMessage().getMegaNodeList();
                        if(nodeList.size()==1){
                            MegaNode node = nodeList.get(0);
                            if(node.hasPreview()){
                                log("Show full screen viewer");
                                Intent intent = new Intent(this, ChatFullScreenImageViewer.class);
                                intent.putExtra("position", 0);
                                intent.putExtra("chatId", idChat);
                                long [] messagesIds = {m.getMessage().getMsgId()};
                                intent.putExtra("messageIds", messagesIds);
                                startActivity(intent);
                            }
                            else{
                                log("show node attachment panel for one node");
                                showNodeAttachmentBottomSheet(m, position);
                            }
                        }
                        else{
                            log("show node attachment panel");
                            showNodeAttachmentBottomSheet(m, position);
                        }
                    }
                    if(m.getMessage().getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){
                        log("show contact attachment panel");
                        if (m != null) {
                            if (m.getMessage().getUsersCount() == 1) {
                                long userHandle = m.getMessage().getUserHandle(0);
                                if(userHandle != megaApi.getMyUser().getHandle()){
                                    showContactAttachmentBottomSheet(m, position);
                                }
                            }
                            else{
                                showContactAttachmentBottomSheet(m, position);
                            }
                        }
                    }
                    else if(m.getMessage().getUserHandle()==megaChatApi.getMyUserHandle()) {
                        if(!(m.getMessage().isManagementMessage())){
                            log("selected message: "+m.getMessage().getContent());
                            log("selected message handle: "+m.getMessage().getTempId());
                            log("selected message rowId: "+m.getMessage().getRowId());
                            if((m.getMessage().getStatus()==MegaChatMessage.STATUS_SERVER_REJECTED)||(m.getMessage().getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL)){
                                log("show not sent message panel");
                                showMsgNotSentPanel(m, position);
                            }
                        }
                    }
                }
            }

        }
    }
    /////END Multiselect/////

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        detector.onTouchEvent(e);
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    public String getMyMail() {
        return myMail;
    }

    public void setMyMail(String myMail) {
        this.myMail = myMail;
    }

    @Override
    public void onChatRoomUpdate(MegaChatApiJava api, MegaChatRoom chat) {
        log("onChatRoomUpdate!");
        this.chatRoom = chat;
        if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_CLOSED)){
            log("CHANGE_TYPE_CLOSED for the chat: "+chat.getChatId());
            log("Permissions for the chat: "+chat.getOwnPrivilege());
            //Hide field to write
            writingContainerLayout.setVisibility(View.GONE);
            fab.setVisibility(View.GONE);
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_STATUS)){
            log("CHANGE_TYPE_STATUS for the chat: "+chat.getChatId());
            if(!(chatRoom.isGroup())){
                long userHandle = chatRoom.getPeerHandle(0);
                setStatus(userHandle);
            }
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_UNREAD_COUNT)){
            log("CHANGE_TYPE_UNREAD_COUNT for the chat: "+chat.getChatId());
//            chat = megaChatApi.getChatRoom(chat.getChatId());
//            int unreadCount = chat.getUnreadCount();
//            log("get unread count: "+unreadCount);
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_PARTICIPANTS)){
            log("CHANGE_TYPE_PARTICIPANTS for the chat: "+chat.getChatId());
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_TITLE)){
            log("CHANGE_TYPE_TITLE for the chat: "+chat.getChatId());
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_USER_TYPING)){
            log("CHANGE_TYPE_USER_TYPING for the chat: "+chat.getChatId());
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
                    log("No more users writing");
                    MegaChatParticipant participantTyping = new MegaChatParticipant(userHandleTyping);
                    UserTyping currentUserTyping = new UserTyping(participantTyping);

                    String nameTyping = chatC.getFirstName(userHandleTyping, chatRoom);

                    log("userHandleTyping: "+userHandleTyping);
                    String userTyping = getResources().getQuantityString(R.plurals.user_typing, 1);
                    userTypingText.setText(" "+userTyping);

                    if(nameTyping==null){
                        log("NULL name");
                        userTypingName.setText(getString(R.string.transfer_unknown));
                    }
                    else{
                        if(nameTyping.trim().isEmpty()){
                            log("EMPTY name");
                            userTypingName.setText(getString(R.string.transfer_unknown));
                        }
                        else{
                            userTypingName.setText(nameTyping);
                        }
                    }
                    participantTyping.setFirstName(nameTyping);

                    userTypingTimeStamp = System.currentTimeMillis()/1000;
                    currentUserTyping.setTimeStampTyping(userTypingTimeStamp);

                    usersTypingSync.add(currentUserTyping);
                    userTypingLayout.setVisibility(View.VISIBLE);

                }
                else{
                    log("More users writing or the same in different timestamp");

                    //Find the item
                    boolean found = false;
                    for(UserTyping user : usersTypingSync) {
                        if(user.getParticipantTyping().getHandle() == userHandleTyping) {
                            log("Found user typing!");
                            userTypingTimeStamp = System.currentTimeMillis()/1000;
                            user.setTimeStampTyping(userTypingTimeStamp);
                            found=true;
                            break;
                        }
                    }

                    if(!found){
                        log("It's a new user typing");
                        MegaChatParticipant participantTyping = new MegaChatParticipant(userHandleTyping);
                        UserTyping currentUserTyping = new UserTyping(participantTyping);

                        String nameTyping = chatC.getFirstName(userHandleTyping, chatRoom);
                        if(nameTyping==null){
                            log("NULL name");
                            userTypingName.setText(getString(R.string.transfer_unknown));
                        }
                        else{
                            if(nameTyping.trim().isEmpty()){
                                log("EMPTY name");
                                userTypingName.setText(getString(R.string.transfer_unknown));
                            }
                            else{
                                userTypingName.setText(nameTyping);
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
                                String userTyping = getResources().getQuantityString(R.plurals.user_typing, 1);
                                userTypingText.setText(" "+userTyping);
                                userTypingName.setText(usersTypingSync.get(0).getParticipantTyping().getFirstName()+" ");
                                break;
                            }
                            case 2:{
                                String userTyping = getResources().getQuantityString(R.plurals.user_typing, 2);
                                userTypingText.setText(" "+userTyping);
                                String userNames = usersTypingSync.get(0).getParticipantTyping().getFirstName()+", "+usersTypingSync.get(1).getParticipantTyping().getFirstName()+" ";
                                userTypingName.setText(userNames);
                                break;
                            }
                            default:{
                                String userTyping = getString(R.string.more_users_typing);
                                userTypingText.setText(" "+userTyping);
                                String userNames = usersTypingSync.get(0).getParticipantTyping().getFirstName()+", "+usersTypingSync.get(1).getParticipantTyping().getFirstName()+" ";
                                userTypingName.setText(userNames);
                                break;
                            }
                        }
                        userTypingLayout.setVisibility(View.VISIBLE);
                    }
                }

                int interval = 5000;
                IsTypingRunnable runnable = new IsTypingRunnable(userTypingTimeStamp);
                handlerReceive = new Handler();
                handlerReceive.postDelayed(runnable, interval);
            }
        }
    }

    private class IsTypingRunnable implements Runnable{

        long timeStamp;

        public IsTypingRunnable(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        @Override
        public void run() {
            log("Run off notification typing");
            long timeNow = System.currentTimeMillis()/1000;
            if ((timeNow - timeStamp) > 4){
                log("Remove user from the list");

                boolean found = false;
                for(UserTyping user : usersTypingSync) {
                    if(user.getTimeStampTyping() == timeStamp) {
                        log("Found user typing in runnable!");
                        usersTypingSync.remove(user);
                        found=true;
                        break;
                    }
                }

                if(!found){
                    log("Error when removing an user typing");
                }

                //Update notification
                int size = usersTypingSync.size();
                log("Size of typing: "+size);
                switch (size){
                    case 0:{
                        userTypingLayout.setVisibility(View.GONE);
                        break;
                    }
                    case 1:{
                        String userTyping = getResources().getQuantityString(R.plurals.user_typing, 1);
                        userTypingText.setText(" "+userTyping);
                        userTypingName.setText(usersTypingSync.get(0).getParticipantTyping().getFirstName()+" ");
                        break;
                    }
                    case 2:{
                        String userTyping = getResources().getQuantityString(R.plurals.user_typing, 2);
                        userTypingText.setText(" "+userTyping);
                        String userNames = usersTypingSync.get(0).getParticipantTyping().getFirstName()+", "+usersTypingSync.get(1).getParticipantTyping().getFirstName()+" ";
                        userTypingName.setText(userNames);
                        break;
                    }
                    default:{
                        String userTyping = getString(R.string.more_users_typing);
                        userTypingText.setText(" "+userTyping);
                        String userNames = usersTypingSync.get(0).getParticipantTyping().getFirstName()+", "+usersTypingSync.get(1).getParticipantTyping().getFirstName()+" ";
                        userTypingName.setText(userNames);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onMessageLoaded(MegaChatApiJava api, MegaChatMessage msg) {
        log("------------------------------------------");
        log("onMessageLoaded!------------------------");

        if(msg!=null){
            log("------------------------------------------");
            log("STATUS: "+msg.getStatus());
            log("TEMP ID: "+msg.getTempId());
            log("FINAL ID: "+msg.getMsgId());
            log("TIMESTAMP: "+msg.getTimestamp());
            log("TYPE: "+msg.getType());
//            log("ROW id: "+msg.getR)
            if(msg.getContent()!=null){
                log("CONTENT: "+msg.getContent());
            }
            else{
                if(msg.isDeleted()){
                    log("DELETED MESSAGE!!!!");
                    return;
                }
            }

            if(msg.getStatus()==MegaChatMessage.STATUS_SERVER_REJECTED){
                log("onMessageLoaded: STATUS_SERVER_REJECTED----- "+msg.getStatus());
            }

            if(msg.getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL){

                log("MANUAL_S: onMessageLoaded: Getting messages not sent !!!-------------------------------------------------: "+msg.getStatus());
                AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);

                if(msg.isEdited()){
                    log("MESSAGE EDITED");

                    if(!noMoreNoSentMessages){
                        log("onMessageLoaded: NOT noMoreNoSentMessages");
                        bufferSending.add(0,androidMsg);
                    }
                    else{
                        log("Try to recover the initial msg");
                        if(msg.getMsgId()!=-1){
                            MegaChatMessage notEdited = megaChatApi.getMessage(idChat, msg.getMsgId());
                            log("Content not edited: "+notEdited.getContent());
                            AndroidMegaChatMessage androidMsgNotEdited = new AndroidMegaChatMessage(notEdited);
                            int returnValue = modifyMessageReceived(androidMsgNotEdited, false);
                            if(returnValue!=-1){
                                log("onMessageLoaded: Message " + returnValue + " modified!");
                            }
                        }

                        appendMessageAnotherMS(androidMsg);
                    }
                }
                else{
                    log("NOOOT MESSAGE EDITED");
                    int returnValue = modifyMessageReceived(androidMsg, true);
                    if(returnValue!=-1){
                        log("onMessageLoaded: Message " + returnValue + " modified!");
                        return;
                    }

                    bufferSending.add(0,androidMsg);

                    if(!noMoreNoSentMessages){
                        log("onMessageLoaded: NOT noMoreNoSentMessages");
                    }
                }
            }
            else if(msg.getStatus()==MegaChatMessage.STATUS_SENDING){
                log("SENDING: onMessageLoaded: Getting messages not sent !!!-------------------------------------------------: "+msg.getStatus());
                AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);
                int returnValue = modifyMessageReceived(androidMsg, true);
                if(returnValue!=-1){
                    log("onMessageLoaded: Message " + returnValue + " modified!");
                    return;
                }

                bufferSending.add(0,androidMsg);

                if(!noMoreNoSentMessages){
                    log("onMessageLoaded: NOT noMoreNoSentMessages");
                }
            }
            else{
                if(!noMoreNoSentMessages){
                    log("First message with NORMAL status");
                    noMoreNoSentMessages=true;
                    //Copy to bufferMessages
                    for(int i=0;i<bufferSending.size();i++){
                        bufferMessages.add(bufferSending.get(i));
                    }
                    bufferSending.clear();
                }

                AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);
                if (lastMessageSeen != null) {
                    if(lastMessageSeen.getMsgId()==msg.getMsgId()){
                        log("onMessageLoaded: Last message seen received!");
                        lastSeenReceived=true;
                        positionToScroll = 0;
                    }
                }

                megaChatApi.setMessageSeen(idChat, msg.getMsgId());

                if(positionToScroll>=0){
                    log("onMessageLoaded: Position to scroll up!");
                    positionToScroll++;
                }

                bufferMessages.add(androidMsg);
                log("onMessageLoaded: Counter: "+bufferMessages.size());
                if(msg.getContent()!=null){
                    log("onMessageLoaded: Content: "+msg.getContent());
                }
                else{
                    log("onMessageLoaded: content NULL");
                }
                log("onMessageLoaded: Get type of message: "+msg.getType());

                log("onMessageLoaded: Size of messages: "+messages.size());
                if(bufferMessages.size()>=NUMBER_MESSAGES_TO_UPDATE_UI){
                    log("onMessageLoaded: Show messages screen");
                    chatRelativeLayout.setVisibility(View.VISIBLE);
                    emptyScrollView.setVisibility(View.GONE);
                    loadMessages();
                }
                else{
                    log("Do not update screen yet: "+bufferMessages.size());
                }
            }
        }
        else{
            log("onMessageLoaded: The message is null");
            log("----> REACH FINAL HISTORY: onMessageLoaded");

            if(bufferSending.size()!=0){
                for(int i=0;i<bufferSending.size();i++){
                    bufferMessages.add(bufferSending.get(i));
                }
                bufferSending.clear();
            }

            if(bufferMessages.size()!=0){
                chatRelativeLayout.setVisibility(View.VISIBLE);
                emptyScrollView.setVisibility(View.GONE);

                loadMessages();

                if(lastSeenReceived==false){
                    log("onMessageLoaded: last message seen NOT received");
                    if(stateHistory!=MegaChatApi.SOURCE_NONE){
                        log("onMessageLoaded: ask more history!: "+messages.size());
                        stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
                    }
                }
                else{
                    log("onMessageLoaded: last message seen received");
                    if(positionToScroll>0){
                        log("onMessageLoaded: message position to scroll: "+positionToScroll+" content: "+messages.get(positionToScroll).getMessage().getContent());
                        log("onMessageLoaded: Scroll to position: "+positionToScroll);
                        messages.get(positionToScroll).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                        adapter.notifyItemChanged(positionToScroll);
                        mLayoutManager.scrollToPositionWithOffset(positionToScroll,20);

                    }
                }
            }
            getMoreHistory = true;
            if(!pendingMessagesLoaded){
                pendingMessagesLoaded = true;
                loadPendingMessages();
            }
        }
        log("END onMessageLoaded------------------------------------------");
    }

    @Override
    public void onMessageReceived(MegaChatApiJava api, MegaChatMessage msg) {
        log("onMessageReceived!");
        log("------------------------------------------");
        log("STATUS: "+msg.getStatus());
        log("TEMP ID: "+msg.getTempId());
        log("FINAL ID: "+msg.getMsgId());
        log("TIMESTAMP: "+msg.getTimestamp());
        log("TYPE: "+msg.getType());
        if(msg.getContent()!=null){
            log("CONTENT: "+msg.getContent());
        }

        if(msg.getStatus()==MegaChatMessage.STATUS_SERVER_REJECTED){
            log("onMessageReceived: STATUS_SERVER_REJECTED----- "+msg.getStatus());
        }

        megaChatApi.setMessageSeen(idChat, msg.getMsgId());

        if(msg.getType()==MegaChatMessage.TYPE_CHAT_TITLE){
            log("Change of chat title");
            String newTitle = msg.getContent();
            if(newTitle!=null){
                aB.setTitle(newTitle);
            }
        }
        else if(msg.getType()==MegaChatMessage.TYPE_TRUNCATE){
            invalidateOptionsMenu();
        }

        AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);
        appendMessagePosition(androidMsg);

        int firstVisiblePosition = mLayoutManager.findLastVisibleItemPosition();
        log("The last item visible: "+firstVisiblePosition+" the last message: "+messages.size());
        mLayoutManager.scrollToPosition(messages.size()-1);
//        mLayoutManager.setStackFromEnd(true);
//        mLayoutManager.scrollToPosition(0);
        log("------------------------------------------");
    }

    public void sendAttachment(MegaChatMessage msg){
        log("sendAttachment");
        AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(msg);
        messages.add(androidMsgSent);
        int index = messages.size()-1;

        AndroidMegaChatMessage androidPreviousMessage = messages.get(index-1);
        log("previous message: "+androidPreviousMessage.getMessage().getContent());
        if(androidPreviousMessage.getMessage().getUserHandle()==myUserHandle) {
            //The last two messages are mine
            if(compareDate(androidMsgSent, androidPreviousMessage)==0){
                //Same date
                if(compareTime(androidMsgSent, androidPreviousMessage)==0){
                    messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                }
                else{
                    //Different minute
                    messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                }
            }
            else{
                //Different date
                messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
            }
        }
        else{
            //The last message is mine, the previous not
            log("Modify not my message");
            if (compareDate(androidMsgSent, androidPreviousMessage) == 0) {
                if (compareTime(androidMsgSent, androidPreviousMessage) == 0) {
                    messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                } else {
                    //Different minute
                    messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                }
            } else {
                //Different date
                messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
            }
        }

        if (adapter == null){
            log("adapter NULL");
            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
            adapter.setHasStableIds(true);
            listView.setLayoutManager(mLayoutManager);
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
            if(adapter.getItemCount()>0){
                listView.setVisibility(View.VISIBLE);
                chatRelativeLayout.setVisibility(View.VISIBLE);
                emptyScrollView.setVisibility(View.GONE);
            }
        }
        else{
            log("adapter is NOT null");
            final int indexToScroll = index;

            if(adapter.getItemCount()>0){
                listView.setVisibility(View.VISIBLE);
                chatRelativeLayout.setVisibility(View.VISIBLE);
                emptyScrollView.setVisibility(View.GONE);
            }

            mLayoutManager.scrollToPositionWithOffset(indexToScroll-1,20);

            adapter.addMessage(messages, index);
        }
    }

    @Override
    public void onMessageUpdate(MegaChatApiJava api, MegaChatMessage msg) {
        log("onMessageUpdate!: "+ msg.getMsgId());

        int resultModify = -1;
        if(msg.isDeleted()){
            log("The message has been deleted");
            deleteMessage(msg);
            return;
        }

        AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);

        if(msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){

            long idMsg =  dbH.findPendingMessageById(msg.getTempId());
            log("----The id of my pending message is: "+idMsg);
            if(idMsg!=-1){
                resultModify = modifyAttachmentReceived(androidMsg, idMsg);
//                dbH.removePendingMessageById(idMsg);
                if(resultModify==-1){
                    log("Node attachment message not in list - add");
                    AndroidMegaChatMessage msgToAppend = new AndroidMegaChatMessage(msg);
                    appendMessagePosition(msgToAppend);
                }
                return;
            }
        }

        if(msg.getStatus()==MegaChatMessage.STATUS_SERVER_REJECTED){
            log("Processing SERVER_REJECTED message");
            MegaChatMessage oldMessage = megaChatApi.getMessage(idChat, msg.getMsgId());
            if(oldMessage!=null){
                log("content of the rejected message: "+oldMessage.getContent());
            }
        }

        if(msg.hasChanged(MegaChatMessage.CHANGE_TYPE_CONTENT)){
            log("Change content of the message");

            if(msg.getType()==MegaChatMessage.TYPE_TRUNCATE){
                log("TRUNCATE MESSAGE");
                messages.clear();
                androidMsg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                messages.add(androidMsg);
                adapter.setMessages(messages);
                adapter.notifyDataSetChanged();
                invalidateOptionsMenu();
            }
            else{
                if(msg.isDeleted()){
                    log("Message deleted!!");
                }
                resultModify = modifyMessageReceived(androidMsg, false);
            }
        }
        else{
            log("Status change");
            log("Temporal id: "+msg.getTempId());
            log("Final id: "+msg.getMsgId());


            if(msg.getStatus()==MegaChatMessage.STATUS_SEEN){
                log("STATUS_SEEN");
            }
            if(msg.getStatus()==MegaChatMessage.STATUS_SERVER_RECEIVED){
                log("STATUS_SERVER_RECEIVED");
                resultModify = modifyMessageReceived(androidMsg, true);
            }
            else{
                log("-----------Status : "+msg.getStatus());
                log("-----------Timestamp: "+msg.getTimestamp());
                log("-----------Content: "+msg.getContent());

                if(msg.getStatus()==MegaChatMessage.STATUS_SERVER_REJECTED){
                    log("onMessageLoaded: STATUS_SERVER_REJECTED----- "+msg.getStatus());
                    //Buscar en la de sending por temporal id.

                }

                resultModify = modifyMessageReceived(androidMsg, false);
            }
        }

        if(resultModify == -1){
            log("Modify not found match --> add to messages");
            AndroidMegaChatMessage msgToAppend = new AndroidMegaChatMessage(msg);
            appendMessagePosition(msgToAppend);
        }
    }

    public void deleteMessage(MegaChatMessage msg){
        log("deleteMessage");

        int indexToChange = -1;

        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        // Iterate in reverse.
        while(itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();
            log("Index: " + itr.nextIndex());

            if (messageToCheck.getMessage().getMsgId() == msg.getMsgId()) {
                log("content to delete: " + messageToCheck.getMessage().getContent());
                indexToChange = itr.nextIndex();
                break;
            }
        }

        if(indexToChange!=-1) {
            messages.remove(indexToChange);
            log("Removed index: " + indexToChange);
            log("deleteMessage: messages size: " + messages.size());
//                adapter.notifyDataSetChanged();
            adapter.removeMessage(indexToChange, messages);

            if(messages.isEmpty()){
                log("No more messages in list");
                listView.setVisibility(View.GONE);
                chatRelativeLayout.setVisibility(View.GONE);
                emptyScrollView.setVisibility(View.VISIBLE);
            }
            else{
                //Update infoToShow of the next message also
                if (indexToChange == 0) {
                    messages.get(indexToChange).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                }
                else{
                    //Not first element
                    int indexToUpdate = indexToChange;
                    int indexPrevious = indexToChange - 1;
                    if(indexToUpdate==messages.size()){
                        log("The last message removed, do not check more messages");
                        return;
                    }
                    AndroidMegaChatMessage messageToUpdate = messages.get(indexToUpdate);
                    AndroidMegaChatMessage newPreviousMessage = messages.get(indexPrevious);

                    log("deleteMessage: message to update message: " + messageToUpdate.getMessage().getContent());
                    log("deleteMessage: message previous: " + newPreviousMessage.getMessage().getContent());

                    if(messageToUpdate.getMessage().getUserHandle()==myUserHandle){
                        log("Message to update message is mine");
                        if(newPreviousMessage.getMessage().getUserHandle()==myUserHandle){
                            log("New previous is mine");
                            //The last two messages are mine
                            if (compareDate(newPreviousMessage, messageToUpdate) == 0) {
                                //Same date
                                if (compareTime(newPreviousMessage, messageToUpdate) == 0) {
                                    messages.get(indexToUpdate).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                                } else {
                                    //Different minute
                                    messages.get(indexToUpdate).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                messages.get(indexToUpdate).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }
                        else{
                            log("New previous is NOT mine");
                            //The last message is mine, the previous not
                            log("Check NOT my message");
                            if (compareDate(newPreviousMessage, messageToUpdate) == 0) {
                                if (compareTime(newPreviousMessage, messageToUpdate) == 0) {
                                    messages.get(indexToUpdate).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                                } else {
                                    //Different minute
                                    messages.get(indexToUpdate).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                messages.get(indexToUpdate).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                            }

                        }
                    }
                    else{
                        log("Message to update message is NOT mine");
                        if (compareDate(newPreviousMessage, messageToUpdate) == 0) {
                            if (compareTime(newPreviousMessage, messageToUpdate) == 0) {
                                messages.get(indexToUpdate).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                            } else {
                                //Different minute
                                messages.get(indexToUpdate).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                            }
                        } else {
                            //Different date
                            messages.get(indexToUpdate).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                        }

                    }

                    adapter.modifyMessage(messages, indexToChange);
                }
            }
        }

    }

    public int modifyAttachmentReceived(AndroidMegaChatMessage msg, long idPendMsg){
        log("modifyAttachmentReceived");
        log("Msg ID: "+msg.getMessage().getMsgId());
        log("Msg TEMP ID: "+msg.getMessage().getTempId());
        log("Msg status: "+msg.getMessage().getStatus());
        int indexToChange = -1;
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        // Iterate in reverse.
        while(itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();

            if(messageToCheck.isUploading()){
                if(messageToCheck.getPendingMessage().getId()==idPendMsg){
                    indexToChange = itr.nextIndex();
                    log("Found index to change: "+indexToChange);
                    break;
                }
            }
        }

        if(indexToChange!=-1){

            log("modifyAttachmentReceived: INDEX change, need to reorder");
            messages.remove(indexToChange);
            log("Removed index: "+indexToChange);
            log("modifyAttachmentReceived: messages size: "+messages.size());
            adapter.removeMessage(indexToChange, messages);
            int scrollToP = appendMessagePosition(msg);
            if(scrollToP!=-1){
                if(msg.getMessage().getStatus()==MegaChatMessage.STATUS_SERVER_RECEIVED){
                    log("modifyAttachmentReceived: need to scroll to position: "+indexToChange);
                    mLayoutManager.scrollToPositionWithOffset(scrollToP, 20);
                }
            }
        }
        else{
            log("Error, id pending message message not found!!");
        }
        log("Index modified: "+indexToChange);
        return indexToChange;
    }


    public int modifyMessageReceived(AndroidMegaChatMessage msg, boolean checkTempId){
        log("modifyMessageReceived");
        log("Msg ID: "+msg.getMessage().getMsgId());
        log("Msg TEMP ID: "+msg.getMessage().getTempId());
        log("Msg status: "+msg.getMessage().getStatus());
        int indexToChange = -1;
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        // Iterate in reverse.
        while(itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();
            log("Index: "+itr.nextIndex());


            if(!messageToCheck.isUploading()){
                log("Checking with Msg ID: "+messageToCheck.getMessage().getMsgId());
                log("Checking with Msg TEMP ID: "+messageToCheck.getMessage().getTempId());
                log("Content: " + messageToCheck.getMessage().getContent());
                if(checkTempId){
                    log("Check temporal IDS----");
                    if (messageToCheck.getMessage().getTempId() == msg.getMessage().getTempId()) {
                        log("modifyMessageReceived: " + messageToCheck.getMessage().getContent());
                        indexToChange = itr.nextIndex();
                        break;
                    }
                }
                else{
                    if (messageToCheck.getMessage().getMsgId() == msg.getMessage().getMsgId()) {
                        log("modifyMessageReceived: " + messageToCheck.getMessage().getContent());
                        indexToChange = itr.nextIndex();
                        break;
                    }
                }
            }
            else{
                log("This message is uploading");
            }
        }

        log("---------------Index to change = "+indexToChange);
        if(indexToChange!=-1){

//            if(msg.getMessage().isDeleted()){
//                messages.remove(indexToChange);
//                log("Removed index: "+indexToChange);
//                log("modifyMessageReceived: messages size: "+messages.size());
////                adapter.notifyDataSetChanged();
//                adapter.removeMessage(indexToChange, messages);
//                return indexToChange;
//            }

            AndroidMegaChatMessage messageToUpdate = messages.get(indexToChange);
            if(messageToUpdate.getMessage().getMsgIndex()==msg.getMessage().getMsgIndex()){
                log("modifyMessageReceived: The internal index not change");

                if(msg.getMessage().getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL){
                    log("Modified a MANUAl SENDING msg");
                    //Check the message to change is not the last one
                    int lastI = messages.size()-1;
                    if(indexToChange<lastI){
                        //Check if there is already any MANUAL_SENDING in the queue
                        AndroidMegaChatMessage previousMessage = messages.get(lastI);
                        if(previousMessage.getMessage().getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL){
                            log("More MANUAL SENDING in queue");
                            log("Removed index: "+indexToChange);
                            messages.remove(indexToChange);
                            appendMessageAnotherMS(msg);
                            adapter.notifyDataSetChanged();
                            return indexToChange;
                        }
                    }
                }

                log("Modified message keep going");
                messages.set(indexToChange, msg);

                //Update infoToShow also
                if (indexToChange == 0) {
                    messages.get(indexToChange).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                }
                else{
                    //Not first element
                    AndroidMegaChatMessage previousMessage = messages.get(indexToChange - 1);
                    log("modifyMessageReceived: previous message: " + previousMessage.getMessage().getContent());
                    if (previousMessage.getMessage().getUserHandle() == myUserHandle) {
                        //The last two messages are mine
                        if (compareDate(msg, previousMessage) == 0) {
                            //Same date
                            if (compareTime(msg, previousMessage) == 0) {
                                messages.get(indexToChange).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                            } else {
                                //Different minute
                                messages.get(indexToChange).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                            }
                        } else {
                            //Different date
                            messages.get(indexToChange).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                        }
                    } else {
                        //The last message is mine, the previous not
                        log("Modify not my message");
                        if (compareDate(msg, previousMessage) == 0) {
                            if (compareTime(msg, previousMessage) == 0) {
                                messages.get(indexToChange).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                            } else {
                                //Different minute
                                messages.get(indexToChange).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                            }
                        } else {
                            //Different date
                            messages.get(indexToChange).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                        }
                    }

                    //Create adapter
                    if (adapter == null) {
                        adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
                        adapter.setHasStableIds(true);
                        listView.setAdapter(adapter);
                    } else {
                        adapter.modifyMessage(messages, indexToChange);
                    }
                }

            }
            else{
                log("modifyMessageReceived: INDEX change, need to reorder");
                messages.remove(indexToChange);
                log("Removed index: "+indexToChange);
                log("modifyMessageReceived: messages size: "+messages.size());
                adapter.removeMessage(indexToChange, messages);
                int scrollToP = appendMessagePosition(msg);
                if(scrollToP!=-1){
                    if(msg.getMessage().getStatus()==MegaChatMessage.STATUS_SERVER_RECEIVED){
                        log("modifyMessageReceived: need to scroll to position: "+indexToChange);
                        mLayoutManager.scrollToPositionWithOffset(scrollToP, 20);
                    }
                }
                log("modifyMessageReceived: messages size 2: "+messages.size());
            }

        }
        else{
            log("Error, id temp message not found!!");
        }
        return indexToChange;
    }

    public void loadMessages(){
        log("loadMessages");
        ListIterator<AndroidMegaChatMessage> itr = bufferMessages.listIterator();
        while (itr.hasNext()) {
            int currentIndex = itr.nextIndex();
            AndroidMegaChatMessage messageToShow = itr.next();
            loadMessage(messageToShow);
        }

        //Create adapter
        if(adapter==null){
            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
            adapter.setHasStableIds(true);
            listView.setLayoutManager(mLayoutManager);
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
        }
        else{
//            adapter.setPositionClicked(-1);
            adapter.loadPreviousMessages(messages, bufferMessages.size());
//            adapter.setMessages(messages, infoToShow);
//            adapter.notifyDataSetChanged();
            log("addMessage: "+messages.size());
        }

        log("AFTER updateMessagesLoaded: "+messages.size()+" messages in list");

        bufferMessages.clear();
    }

    public void loadPendingMessages(){
        log("loadPendingMessages");
        ArrayList<AndroidMegaChatMessage> pendMsgs = dbH.findAndroidMessagesNotSent(idChat);
        dbH.findPendingMessagesBySent(1);
        log("Number of pending: "+pendMsgs.size());
        for(int i=0;i<pendMsgs.size();i++){
            if(pendMsgs.get(i)!=null){
                appendMessagePosition(pendMsgs.get(i));
            }
            else{
                log("Null pending messages");
            }
        }

    }

    public void loadMessage(AndroidMegaChatMessage messageToShow){
        log("loadMessage");

        log("Message to show: "+messageToShow.getMessage().getContent());
        messageToShow.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
        messages.add(0,messageToShow);

        if(messages.size()>1) {
            long userHandleToCompare = -1;
            if ((messageToShow.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (messageToShow.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                userHandleToCompare = messageToShow.getMessage().getHandleOfAction();
            } else {
                userHandleToCompare = messageToShow.getMessage().getUserHandle();
            }

            AndroidMegaChatMessage previousMessage = messages.get(1);
            log("Previous message: "+previousMessage.getMessage().getContent());
            if (userHandleToCompare == myUserHandle) {
//                log("MY message!!: "+messageToShow.getContent());
                long previousUserHandleToCompare = -1;
                if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (messageToShow.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                    previousUserHandleToCompare = previousMessage.getMessage().getHandleOfAction();
                } else {
                    previousUserHandleToCompare = previousMessage.getMessage().getUserHandle();
                }

//                    log("previous message: "+previousMessage.getContent());
                if (previousUserHandleToCompare == myUserHandle) {
                    log("Last message and previous is mine");
                    //The last two messages are mine
                    if (compareDate(messageToShow, previousMessage) == 0) {
                        //Same date
                        if (compareTime(messageToShow, previousMessage) == 0) {
                            previousMessage.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                        } else {
                            //Different minute
                            previousMessage.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                        }
                    } else {
                        //Different date
                        previousMessage.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                    }
                } else {
                    //The last message is mine, the previous not
                    log("Last message is mine, NOT previous");
                    if (compareDate(messageToShow, previousMessage) == 0) {
                        previousMessage.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                    } else {
                        //Different date
                        previousMessage.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                    }
                }

            } else {
                log("NOT MY message!! - CONTACT");
//                    log("previous message: "+previousMessage.getContent());
                long previousUserHandleToCompare = -1;
                if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (messageToShow.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                    previousUserHandleToCompare = previousMessage.getMessage().getHandleOfAction();
                } else {
                    previousUserHandleToCompare = previousMessage.getMessage().getUserHandle();
                }

                if (previousUserHandleToCompare == userHandleToCompare) {
                    //The last message is also a contact's message
                    if (compareDate(messageToShow, previousMessage) == 0) {
                        //Same date
                        if (compareTime(messageToShow, previousMessage) == 0) {
                            previousMessage.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                        } else {
                            //Different minute
                            previousMessage.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                        }
                    } else {
                        //Different date
                        previousMessage.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                    }
                } else {
                    //The last message is from contact, the previous not
                    if (compareDate(messageToShow, previousMessage) == 0) {
                        previousMessage.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                    } else {
                        //Different date
                        previousMessage.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                    }
                }
            }
        }
    }

    public void appendMessageAnotherMS(AndroidMegaChatMessage msg){
        log("appendMessageAnotherMS: "+msg.getMessage().getContent());
        messages.add(msg);
        int lastIndex = messages.size()-1;
        AndroidMegaChatMessage previousMessage = messages.get(lastIndex-1);

        long previousUserHandleToCompare = previousMessage.getMessage().getUserHandle();

        if (previousUserHandleToCompare == myUserHandle) {
            log("Last message and previous is mine");
            //The last two messages are mine
            if (compareDate(msg, previousMessage) == 0) {
                //Same date
                if (compareTime(msg, previousMessage) == 0) {
                    msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                } else {
                    //Different minute
                    msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                }
            } else {
                //Different date
                msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
            }
        } else {
            //The last message is mine, the previous not
            log("Last message is mine, NOT previous");
            if (compareDate(msg, previousMessage) == 0) {
                msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
            } else {
                //Different date
                msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
            }
        }

        //Create adapter
        if(adapter==null){
            log("Create adapter");
            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
            adapter.setHasStableIds(true);
            listView.setLayoutManager(mLayoutManager);
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
            listView.setVisibility(View.VISIBLE);
            chatRelativeLayout.setVisibility(View.VISIBLE);
            emptyScrollView.setVisibility(View.GONE);
        }
        else{
            log("Update apapter with last index: "+lastIndex);
            if(lastIndex==0){
                log("Arrives the first message of the chat");
                adapter.setMessages(messages);
                listView.setVisibility(View.VISIBLE);
                chatRelativeLayout.setVisibility(View.VISIBLE);
                emptyScrollView.setVisibility(View.GONE);
            }
            else{
                adapter.addMessage(messages, lastIndex);
            }
        }
    }

    public int appendMessagePosition(AndroidMegaChatMessage msg){
        log("appendMessagePosition: "+messages.size()+" messages");

        long userHandleToCompare = -1;
        long previousUserHandleToCompare = -1;

        int lastIndex = messages.size()-1;
        if(messages.size()==0){
            msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
            messages.add(msg);
        }
        else{
            log("Finding where to append the message");

            if(msg.isUploading()){
                lastIndex++;
                log("The message is uploading add to index: "+lastIndex);
                userHandleToCompare = myUserHandle;
            }
            else{
                log("status of message: "+msg.getMessage().getStatus());
                while(messages.get(lastIndex).isUploading()){
                    log("one less index is uploading");
                    lastIndex--;
                }
                while(messages.get(lastIndex).getMessage().getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL){
                    log("one less index is MANUAL SENDING");
                    lastIndex--;
                }
                if(msg.getMessage().getStatus()==MegaChatMessage.STATUS_SERVER_RECEIVED||msg.getMessage().getStatus()==MegaChatMessage.STATUS_NOT_SEEN){
                    while(messages.get(lastIndex).getMessage().getStatus()==MegaChatMessage.STATUS_SENDING){
                        log("one less index");
                        lastIndex--;
                    }
                }
                lastIndex++;
                log("Append in position: "+lastIndex);

                if ((msg.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (msg.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                    userHandleToCompare = msg.getMessage().getHandleOfAction();
                } else {
                    userHandleToCompare = msg.getMessage().getUserHandle();
                }
            }

            messages.add(lastIndex, msg);

            AndroidMegaChatMessage previousMessage = messages.get(lastIndex-1);

            if(previousMessage.isUploading()){

                log("The previous message is uploading");
                if(msg.isUploading()){
                    log("The message is also uploading");
                    if (compareDate(msg.getPendingMessage().getUploadTimestamp(), previousMessage.getPendingMessage().getUploadTimestamp()) == 0) {
                        //Same date
                        if (compareTime(msg.getPendingMessage().getUploadTimestamp(), previousMessage.getPendingMessage().getUploadTimestamp()) == 0) {
                            msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                        } else {
                            //Different minute
                            msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                        }
                    } else {
                        //Different date
                        msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                    }
                }
                else{
                    if (compareDate(msg.getMessage().getTimestamp(), previousMessage.getPendingMessage().getUploadTimestamp()) == 0) {
                        //Same date
                        if (compareTime(msg.getMessage().getTimestamp(), previousMessage.getPendingMessage().getUploadTimestamp()) == 0) {
                            msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                        } else {
                            //Different minute
                            msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                        }
                    } else {
                        //Different date
                        msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                    }
                }
            }
            else{
                log("The previous message is NOT uploading");

                if (userHandleToCompare == myUserHandle) {
//                log("MY message!!: "+messageToShow.getContent());
                    if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                        previousUserHandleToCompare = previousMessage.getMessage().getHandleOfAction();
                    } else {
                        previousUserHandleToCompare = previousMessage.getMessage().getUserHandle();
                    }

//                    log("previous message: "+previousMessage.getContent());
                    if (previousUserHandleToCompare == myUserHandle) {
                        log("Last message and previous is mine");
                        //The last two messages are mine
                        if(msg.isUploading()){
                            log("The msg to append is uploading");
                            if (compareDate(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                //Same date
                                if (compareTime(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                    msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                                } else {
                                    //Different minute
                                    msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }
                        else{
                            if (compareDate(msg, previousMessage) == 0) {
                                //Same date
                                if (compareTime(msg, previousMessage) == 0) {
                                    msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                                } else {
                                    //Different minute
                                    msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }

                    } else {
                        //The last message is mine, the previous not
                        log("Last message is mine, NOT previous");
                        if(msg.isUploading()) {
                            log("The msg to append is uploading");
                            if (compareDate(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                            } else {
                                //Different date
                                msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }
                        else{
                            if (compareDate(msg, previousMessage) == 0) {
                                msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                            } else {
                                //Different date
                                msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }

                    }

                } else {
                    log("NOT MY message!! - CONTACT");
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
                                    log("Add with show nothing - same userHandle");
                                    msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                                } else {
                                    //Different minute
                                    msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }
                        else{
                            if (compareDate(msg, previousMessage) == 0) {
                                //Same date
                                if (compareTime(msg, previousMessage) == 0) {
                                    log("Add with show nothing - same userHandle");
                                    msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                                } else {
                                    //Different minute
                                    msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }

                    } else {
                        //The last message is from contact, the previous not
                        log("Different user handle");
                        if(msg.isUploading()) {
                            if (compareDate(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                if (compareTime(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                    log("Add with show nothing - same userHandle");
                                    msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                                } else {
                                    //Different minute
                                    msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }
                        else{
                            if (compareDate(msg, previousMessage) == 0) {
                                if (compareTime(msg, previousMessage) == 0) {
                                    log("Add with show nothing - same userHandle");
                                    msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_NOTHING);
                                } else {
                                    //Different minute
                                    msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }

                    }
                }

            }

        }


        //Create adapter
        if(adapter==null){
            log("Create adapter");
            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
            adapter.setHasStableIds(true);
            listView.setLayoutManager(mLayoutManager);
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
            listView.setVisibility(View.VISIBLE);
            chatRelativeLayout.setVisibility(View.VISIBLE);
            emptyScrollView.setVisibility(View.GONE);
        }
        else{
            log("Update adapter with last index: "+lastIndex);
            if(lastIndex<0){
                log("Arrives the first message of the chat");
                adapter.setMessages(messages);
                listView.setVisibility(View.VISIBLE);
                chatRelativeLayout.setVisibility(View.VISIBLE);
                emptyScrollView.setVisibility(View.GONE);
            }
            else{
                adapter.addMessage(messages, lastIndex);
            }
        }
        return lastIndex;
    }

    public boolean isGroup(){
        return chatRoom.isGroup();
    }

    public void showMsgNotSentPanel(AndroidMegaChatMessage message, int position){
        log("showMsgNotSentPanel: "+position);

        this.selectedPosition = position;
        this.selectedMessageId = message.getMessage().getRowId();
        log("Temporal id of MS message: "+message.getMessage().getTempId());

        if(message!=null){
            MessageNotSentBottomSheetDialogFragment bottomSheetDialogFragment = new MessageNotSentBottomSheetDialogFragment();
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        }
    }

    public void showNodeAttachmentBottomSheet(AndroidMegaChatMessage message, int position){
        log("showNodeAttachmentBottomSheet: "+position);
        this.selectedPosition = position;

        if(message!=null){
            this.selectedMessageId = message.getMessage().getMsgId();
//            this.selectedChatItem = chat;
            NodeAttachmentBottomSheetDialogFragment bottomSheetDialogFragment = new NodeAttachmentBottomSheetDialogFragment();
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        }
    }

    public void showUploadingAttachmentBottomSheet(AndroidMegaChatMessage message, int position){
        log("showUploadingAttachmentBottomSheet: "+position);
        this.selectedPosition = position;

        log("Cooming soon");
//        if(message!=null){
//            this.selectedMessageId = message.getMessage().getMsgId();
////            this.selectedChatItem = chat;
//            NodeAttachmentBottomSheetDialogFragment bottomSheetDialogFragment = new NodeAttachmentBottomSheetDialogFragment();
//            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
//        }
    }

    public void showContactAttachmentBottomSheet(AndroidMegaChatMessage message, int position){
        log("showContactAttachmentBottomSheet: "+position);
        this.selectedPosition = position;

        if(message!=null){
            this.selectedMessageId = message.getMessage().getMsgId();
//            this.selectedChatItem = chat;
            ContactAttachmentBottomSheetDialogFragment bottomSheetDialogFragment = new ContactAttachmentBottomSheetDialogFragment();
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        }
    }

    public void removeMsgNotSent(){
        log("removeMsgNotSent: "+selectedPosition);
        messages.remove(selectedPosition);
        adapter.removeMessage(selectedPosition, messages);
    }

    public void showSnackbar(String s){
        log("showSnackbar: "+s);
        Snackbar snackbar = Snackbar.make(fragmentContainer, s, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    public void startConversation(long handle){
        log("startConversation");
        MegaChatRoom chat = megaChatApi.getChatRoomByUser(handle);
        MegaChatPeerList peers = MegaChatPeerList.createInstance();
        if(chat==null){
            log("No chat, create it!");
            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD);
            megaChatApi.createChat(false, peers, this);
        }
        else{
            log("There is already a chat, open it!");
            Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
            intentOpenChat.setAction(Constants.ACTION_CHAT_SHOW_MESSAGES);
            intentOpenChat.putExtra("CHAT_ID", chat.getChatId());
            finish();
            this.startActivity(intentOpenChat);
        }
    }

    public void startGroupConversation(ArrayList<Long> userHandles){
        log("startGroupConversation");

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
            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
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
        log("onRequestFinish");

        if(request.getType() == MegaChatRequest.TYPE_TRUNCATE_HISTORY){
            log("Truncate history request finish!!!");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                log("Ok. Clear history done");
                showSnackbar(getString(R.string.clear_history_success));
            }
            else{
                log("Error clearing history: "+e.getErrorString());
                showSnackbar(getString(R.string.clear_history_error));
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM){
            log("Remove participant: "+request.getUserHandle()+" my user: "+megaChatApi.getMyUserHandle());

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                log("Participant removed OK");
                invalidateOptionsMenu();
            }
            else{
                log("EEEERRRRROR WHEN TYPE_REMOVE_FROM_CHATROOM " + e.getErrorString());
                showSnackbar(getString(R.string.remove_participant_error));
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_INVITE_TO_CHATROOM){
            log("Request type: "+MegaChatRequest.TYPE_INVITE_TO_CHATROOM);
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                showSnackbar(getString(R.string.add_participant_success));
            }
            else{
                if(e.getErrorCode() == MegaChatError.ERROR_EXIST){
                    showSnackbar(getString(R.string.add_participant_error_already_exists));
                }
                else{
                    showSnackbar(getString(R.string.add_participant_error));
                }
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE){
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                log("File sent correctly");
                MegaNodeList nodeList = request.getMegaNodeList();
                for(int i = 0; i<nodeList.size();i++){
                    log("Node name: "+nodeList.get(i).getName());
                }
                sendMessage(request.getMegaChatMessage());
            }
            else{
                log("File NOT sent: "+e.getErrorCode());
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_REVOKE_NODE_MESSAGE){
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                log("Node revoked correctly");
            }
            else{
                log("NOT revoked correctly");
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM){
            log("Create chat request finish!!!");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){

                log("open new chat");
                Intent intent = new Intent(this, ChatActivityLollipop.class);
                intent.setAction(Constants.ACTION_CHAT_NEW);
                intent.putExtra("CHAT_ID", request.getChatHandle());
                finish();
                this.startActivity(intent);

            }
            else{
                log("EEEERRRRROR WHEN CREATING CHAT " + e.getErrorString());
                showSnackbar(getString(R.string.create_chat_error));
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestTemporaryError");
    }

    @Override
    protected void onDestroy(){
        log("onDestroy()");

        megaChatApi.closeChatRoom(idChat, this);

        MegaApplication.setOpenChatId(-1);

        super.onDestroy();
    }


    @Override
    protected void onNewIntent(Intent intent){
        log("onNewIntent");

        if (intent != null){
            if (intent.getAction() != null){
                log("Intent is here!: "+intent.getAction());
                if (intent.getAction().equals(Constants.ACTION_CLEAR_CHAT)){
                    log("Intent to Clear history");
                    showConfirmationClearChat(chatRoom);
                }
                else{
                    log("Other intent");
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

    public void importNode(){
        log("importNode");

        Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER);
        startActivityForResult(intent, Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER);

    }

    public void revoke(){
        log("revoke");

        MegaChatMessage message = megaChatApi.getMessage(idChat, selectedMessageId);

        if(message!=null) {
            MegaNodeList nodeList = message.getMegaNodeList();
            for (int i = 0; i < nodeList.size(); i++) {
                MegaNode document = nodeList.get(i);
                megaChatApi.revokeAttachment(idChat,document.getHandle(),this);
            }
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        try{
            statusDialog.dismiss();
        } catch(Exception ex){};

        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT){
            log("MegaRequest.TYPE_INVITE_CONTACT finished: "+request.getNumber());

            if(request.getNumber()== MegaContactRequest.INVITE_ACTION_REMIND){
                showSnackbar(getString(R.string.context_contact_invitation_resent));
            }
            else{
                if (e.getErrorCode() == MegaError.API_OK){
                    log("OK INVITE CONTACT: "+request.getEmail());
                    if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD)
                    {
                        showSnackbar(getString(R.string.context_contact_request_sent, request.getEmail()));
                    }
                }
                else{
                    log("Code: "+e.getErrorString());
                    if(e.getErrorCode()==MegaError.API_EEXIST)
                    {
                        showSnackbar(getString(R.string.context_contact_already_invited, request.getEmail()));
                    }
                    else{
                        showSnackbar(getString(R.string.general_error));
                    }
                    log("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
                }
            }
        }
        else if(request.getType() == MegaRequest.TYPE_COPY){
            if (e.getErrorCode() != MegaError.API_OK) {

                log("e.getErrorCode() != MegaError.API_OK");

                if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
                    log("OVERQUOTA ERROR: "+e.getErrorCode());
                    Intent intent = new Intent(this, ManagerActivityLollipop.class);
                    intent.setAction(Constants.ACTION_OVERQUOTA_ALERT);
                    startActivity(intent);
                    finish();

                }
                else
                {
                    Snackbar.make(fragmentContainer, getString(R.string.import_success_error), Snackbar.LENGTH_LONG).show();
                }

            }else{
                Snackbar.make(fragmentContainer, getString(R.string.import_success_message), Snackbar.LENGTH_LONG).show();
            }
        }

    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        log("onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putLong("selectedMessageId", selectedMessageId);
        outState.putInt("selectedPosition", selectedPosition);
    }

    public void askSizeConfirmationBeforeChatDownload(String parentPath, ArrayList<MegaNode> nodeList, long size){
        log("askSizeConfirmationBeforeChatDownload");

        final String parentPathC = parentPath;
        final ArrayList<MegaNode> nodeListC = nodeList;
        final long sizeC = size;
        final ChatController chatC = new ChatController(this);

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        LinearLayout confirmationLayout = new LinearLayout(this);
        confirmationLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(10, outMetrics), Util.scaleWidthPx(17, outMetrics), 0);

        final CheckBox dontShowAgain =new CheckBox(this);
        dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
        dontShowAgain.setTextColor(getResources().getColor(R.color.text_secondary));

        confirmationLayout.addView(dontShowAgain, params);

        builder.setView(confirmationLayout);

//				builder.setTitle(getString(R.string.confirmation_required));

        builder.setMessage(getString(R.string.alert_larger_file, Util.getSizeString(sizeC)));
        builder.setPositiveButton(getString(R.string.general_download),
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
        log("onIntentProcessedLollipop");
//		List<ShareInfo> infos = filePreparedInfos;
        if (statusDialog != null) {
            try {
                statusDialog.dismiss();
            }
            catch(Exception ex){}
        }

        if (infos == null) {
            Snackbar.make(fragmentContainer, getString(R.string.upload_can_not_open), Snackbar.LENGTH_LONG).show();
        }
        else {

            Intent intent = new Intent(this, ChatUploadService.class);
            ArrayList<PendingNodeAttachment> nodeAttachments = new ArrayList<>();

            long timestamp = System.currentTimeMillis()/1000;
            long idPendingMsg = dbH.setPendingMessage(idChat+"", Long.toString(timestamp));
            if(idPendingMsg!=-1){
                intent.putExtra(ChatUploadService.EXTRA_ID_PEND_MSG, idPendingMsg);

                log("Launch chat upload with files "+infos.size());

                for (ShareInfo info : infos) {
                    log("name of the file: "+info.getTitle());
                    log("size of the file: "+info.getSize());
                    String fingerprint = megaApi.getFingerprint(info.getFileAbsolutePath());

                    //Add node to db
                    long idNode = dbH.setNodeAttachment(info.getFileAbsolutePath(), info.getTitle(), fingerprint);

                    dbH.setMsgNode(idPendingMsg, idNode);

                    PendingNodeAttachment nodeAttachment = new PendingNodeAttachment(info.getFileAbsolutePath(), fingerprint, info.getTitle());
                    nodeAttachments.add(nodeAttachment);
                }

                PendingMessage newPendingMsg = new PendingMessage(idPendingMsg, idChat, nodeAttachments, timestamp, PendingMessage.STATE_SENDING);
                AndroidMegaChatMessage newNodeAttachmentMsg = new AndroidMegaChatMessage(newPendingMsg, true);
                sendMessageUploading(newNodeAttachmentMsg);

                intent.putStringArrayListExtra(ChatUploadService.EXTRA_FILEPATHS, newPendingMsg.getFilePaths());
                intent.putExtra(ChatUploadService.EXTRA_CHAT_ID, idChat);

                startService(intent);
            }
            else{
                log("Error when adding pending msg to the database");
            }

        }
    }
}