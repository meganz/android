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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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
import mega.privacy.android.app.components.MegaLinearLayoutManager;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaChatLollipopAdapter;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.MultipleGroupChatRequestListener;
import mega.privacy.android.app.modalbottomsheet.MessageNotSentBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.TimeChatUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatRoomListenerInterface;
import nz.mega.sdk.MegaUser;

public class ChatActivityLollipop extends PinActivityLollipop implements MegaChatRequestListenerInterface, MegaChatRoomListenerInterface, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener, View.OnClickListener, EmojiconGridFragment.OnEmojiconClickedListener, EmojiconsFragment.OnEmojiconBackspaceClickedListener {

    public static int NUMBER_MESSAGES_TO_LOAD = 20;
    public static int NUMBER_MESSAGES_TO_UPDATE_UI = 7;
    public static int NUMBER_MESSAGES_BEFORE_LOAD = 8;

    boolean firstMessageReceived = true;
    boolean getMoreHistory=true;

    private AlertDialog errorOpenChatDialog;

    ProgressDialog dialog;

    MegaChatMessage lastMessageSeen = null;
    boolean lastSeenReceived = false;
    int positionToScroll = -1;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    Handler handlerReceive;
    Handler handlerSend;

    AndroidMegaChatMessage selectedMessage;
    int selectedPosition;

    MegaChatRoom chatRoom;
    long idChat;

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
    TextView userTypingtext;
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
    MegaLinearLayoutManager mLayoutManager;

    ChatActivityLollipop chatActivity;
    String myMail;

    RelativeLayout uploadPanel;
    RelativeLayout uploadFromGalleryOption;
    RelativeLayout uploadFromCloudOption;
    RelativeLayout uploadAudioOption;
    RelativeLayout uploadContactOption;
    RelativeLayout uploadFromFilesystemOption;

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
            }
        });

        textChat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(uploadPanel.getVisibility()==View.VISIBLE){
                    hideUploadPanel();
                }
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

        mLayoutManager = new MegaLinearLayoutManager(this);
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
        userTypingtext = (TextView) findViewById(R.id.user_typing_text);

        fab = (FloatingActionButton) findViewById(R.id.fab_chat);
        fab.setOnClickListener(this);

        uploadPanel = (RelativeLayout) findViewById(R.id.upload_panel_chat);
        uploadFromGalleryOption = (RelativeLayout) findViewById(R.id.upload_from_gallery_chat);
        uploadFromGalleryOption.setOnClickListener(this);

        uploadFromCloudOption = (RelativeLayout) findViewById(R.id.upload_from_cloud_chat);
        uploadFromCloudOption.setOnClickListener(this);

        uploadFromFilesystemOption = (RelativeLayout) findViewById(R.id.upload_from_filesystem_chat);
        uploadFromFilesystemOption.setOnClickListener(this);

        uploadAudioOption = (RelativeLayout) findViewById(R.id.upload_audio_chat);
        uploadAudioOption.setOnClickListener(this);

        uploadContactOption = (RelativeLayout) findViewById(R.id.upload_contact_chat);
        uploadContactOption.setOnClickListener(this);

        emojiKeyboardLayout = (FrameLayout) findViewById(R.id.chat_emoji_keyboard);

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

                fab.setVisibility(View.VISIBLE);

                idChat = newIntent.getLongExtra("CHAT_ID", -1);
//                    idChat=8179160514871859886L;
                myMail = megaApi.getMyEmail();
                myUserHandle = megaChatApi.getMyUserHandle();

                log("Show empty screen");
                chatRelativeLayout.setVisibility(View.GONE);
                emptyScrollView.setVisibility(View.VISIBLE);

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
                        messages = new ArrayList<AndroidMegaChatMessage>();
                        bufferMessages = new ArrayList<AndroidMegaChatMessage>();

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
                                int state = chatRoom.getOnlineStatus();
                                if(state == MegaChatApi.STATUS_ONLINE){
                                    log("This user is connected: "+chatRoom.getTitle());
                                    aB.setSubtitle(getString(R.string.online_status));
                                }
                                else{
                                    log("This user status is: "+state+  " " + chatRoom.getTitle());
                                    aB.setSubtitle(getString(R.string.offline_status));
                                }
                            }
                        }

                        if (intentAction.equals(Constants.ACTION_CHAT_NEW)) {
                            log("ACTION_CHAT_NEW");

                            fab.setVisibility(View.VISIBLE);
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
                        //                else if (intentAction.equals(Constants.ACTION_CHAT_INVITE)){
//                    fab.setVisibility(View.GONE);
//                    listView.setVisibility(View.GONE);
//                    chatRelativeLayout.setVisibility(View.GONE);
//                    emptyScrollView.setVisibility(View.VISIBLE);
////                    inviteText.setVisibility(View.VISIBLE);
//                    textChat.setOnFocusChangeListener(focus);
//                    textChat.setText("Hi there!\nLet's chat!");
//                }
                    }

                }
                else{
                    log("Chat ID -1 error");
                }
            }

        }
    }

//    public void setStatus(){
//
//        int state = chatRoom.getOnlineStatus();
//        if(state == MegaChatApi.STATUS_ONLINE){
//            log("This user is connected: "+chatRoom.getTitle());
//            aB.setSubtitle(getString(R.string.online_status));
//        }
//        else{
//            log("This user status is: "+state+  " " + chatRoom.getTitle());
//            aB.setSubtitle(getString(R.string.offline_status));
//        }
//    }

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

        if(chatRoom!=null){
            int permission = chatRoom.getOwnPrivilege();
            if(chatRoom.isGroup()){

                if(permission==MegaChatRoom.PRIV_MODERATOR) {
                    inviteMenuItem.setVisible(true);
                    clearHistoryMenuItem.setVisible(true);
                }
                else {
                    inviteMenuItem.setVisible(false);
                    clearHistoryMenuItem.setVisible(false);
                }

                contactInfoMenuItem.setTitle(getString(R.string.group_chat_info_label));
                contactInfoMenuItem.setVisible(true);

                if(permission==MegaChatRoom.PRIV_RM) {
                    log("Group chat PRIV_RM");
                    leaveMenuItem.setVisible(false);
                }
                else{
                    log("Permission: "+permission);
                    leaveMenuItem.setVisible(true);
                }
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

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home: {
                if(uploadPanel.getVisibility()==View.VISIBLE){
                    hideUploadPanel();
                    break;
                }

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

        Intent in = new Intent(this, AddContactActivityLollipop.class);
        in.putExtra("contactType", Constants.CONTACT_TYPE_MEGA);
        startActivityForResult(in, Constants.REQUEST_ADD_PARTICIPANTS);

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
        else{
            log("Error onActivityResult: REQUEST_ADD_PARTICIPANTS");
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
                        ChatController chatC = new ChatController(chatActivity);
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

        if(uploadPanel.getVisibility()==View.VISIBLE){
            hideUploadPanel();
            return;
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
                    sendMessage(text);
                    log("Send message: "+text);
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


//                editText.requestFocus();
//                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);

//                InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
//                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

//                Intent intent = new Intent(this, KeyboardActivityLollipop.class);
//                this.startActivity(intent);

                break;
            }
            case R.id.fab_chat:{
                showUploadPanel();
                break;
            }
            case R.id.upload_from_gallery_chat:{
                hideUploadPanel();
                showSnackbar(getString(R.string.general_not_yet_implemented));
                break;
            }
            case R.id.upload_from_cloud_chat:{
                hideUploadPanel();
                showSnackbar(getString(R.string.general_not_yet_implemented));
                break;
            }
            case R.id.upload_audio_chat:{
                hideUploadPanel();
                showSnackbar(getString(R.string.general_not_yet_implemented));
                break;
            }
            case R.id.upload_contact_chat:{
                hideUploadPanel();
                showSnackbar(getString(R.string.general_not_yet_implemented));
                break;
            }
            case R.id.upload_from_filesystem_chat:{
                hideUploadPanel();
                showSnackbar(getString(R.string.general_not_yet_implemented));
                break;
            }
		}
    }

    public void sendMessage(String text){
        log("sendMessage: "+text);

        MegaChatMessage msgSent = megaChatApi.sendMessage(idChat, text);
        AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(msgSent);

        if(msgSent!=null){
            log("Mensaje enviado con id temp: "+msgSent.getTempId());
            log("State of the message: "+msgSent.getStatus());

            messages.add(androidMsgSent);
            int index = messages.size()-1;

            if(index==0){
                //First element
                log("First element!");
                messages.get(0).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
            }
            else{
                //Not first element
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
                    if(compareDate(androidMsgSent, androidPreviousMessage)==0){
                        messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                    }
                    else{
                        //Different date
                        messages.get(index).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                    }
                }
            }
            if (adapter == null){
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
                adapter.appendMessage(messages);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        log("Now I update the recyclerview (send)");
                        mLayoutManager.scrollToPosition(adapter.getItemCount()-1);

                    }
                }, 100);

//                mLayoutManager.setStackFromEnd(true);
//                mLayoutManager.scrollToPosition(messages.size());
            }
        }
        else{
            log("Error al enviar mensaje!");
        }
    }

    public void editMessage(String text){
        log("editMessage: "+text);

        MegaChatMessage msgEdited = megaChatApi.editMessage(idChat, messageToEdit.getMsgId(), text);
        AndroidMegaChatMessage androidMsgEdited = new AndroidMegaChatMessage(msgEdited);
        if(msgEdited!=null){
            log("Edited message");
            modifyMessageReceived(androidMsgEdited, false);
        }
        else{
            log("Message cannot be edited!");
        }
    }

    public void showUploadPanel(){
        fab.setVisibility(View.GONE);
        uploadPanel.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messagesContainerLayout.getLayoutParams();
        params.addRule(RelativeLayout.ABOVE, R.id.upload_panel_chat);
        messagesContainerLayout.setLayoutParams(params);
    }

    public void hideUploadPanel(){
        fab.setVisibility(View.VISIBLE);
        uploadPanel.setVisibility(View.GONE);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messagesContainerLayout.getLayoutParams();
        params.addRule(RelativeLayout.ABOVE, R.id.writing_container_layout_chat_layout);
        messagesContainerLayout.setLayoutParams(params);
    }

    /////Multiselect/////
    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            ArrayList<AndroidMegaChatMessage> messagesSelected = adapter.getSelectedMessages();

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

                    String text = copyMessages(messagesSelected);
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
                    Toast.makeText(chatActivity, "Delete: "+messagesSelected.size()+" chats",Toast.LENGTH_SHORT).show();
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
                    AndroidMegaChatMessage message = selected.get(0);
                    if(message.getMessage().getUserHandle()==myUserHandle){
                        if(message.getMessage().isEditable()){
                            menu.findItem(R.id.chat_cab_menu_edit).setVisible(true);
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                        }
                    }
                    else{
                        menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                        menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                    }

                    menu.findItem(R.id.chat_cab_menu_copy).setVisible(true);

//                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
//                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
//                    unselect.setTitle(getString(R.string.action_unselect_one));
//                    unselect.setVisible(true);
                }
                else if (selected.size()==adapter.getItemCount()){
                    menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                    menu.findItem(R.id.chat_cab_menu_copy).setVisible(true);
                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                    for(int i=0; i<selected.size();i++){
                        if(messages.get(i).getMessage().getUserHandle()==myUserHandle){
                            if(!(messages.get(i).getMessage().isEditable())){
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                                break;
                            }
                        }
                        else{
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                        }
                    }
//                    menu.findItem(R.id.cab_menu_select_all).setVisible(false);
//                    unselect.setTitle(getString(R.string.action_unselect_all));
//                    unselect.setVisible(true);
                }
                else{
                    menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                    menu.findItem(R.id.chat_cab_menu_copy).setVisible(true);
                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                    for(int i=0; i<selected.size();i++){
                        if(messages.get(i).getMessage().getUserHandle()==myUserHandle){
                            if(!(messages.get(i).getMessage().isEditable())){
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                                break;
                            }
                        }
                        else{
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                        }
                    }
//                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
//                    unselect.setTitle(getString(R.string.action_unselect_all));
//                    unselect.setVisible(true);
                }
            }
//            else{
//                menu.findItem(R.id.cab_menu_select_all).setVisible(true);
//                menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
//            }

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
            log("show not sent message panel");
            AndroidMegaChatMessage m = messages.get(position);
//            showMsgNotSentPanel(m);
            if(m!=null){
                if(m.getMessage().getUserHandle()==megaChatApi.getMyUserHandle()) {
                    if(!(m.getMessage().isManagementMessage())){
                        log("selected message: "+m.getMessage().getContent());
                        if((m.getMessage().getStatus()==MegaChatMessage.STATUS_SERVER_REJECTED)||(m.getMessage().getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL)){
                            showMsgNotSentPanel(m, selectedPosition);
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

        if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_CLOSED)){
            log("CHANGE_TYPE_CLOSED for the chat: "+chat.getChatId());
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_STATUS)){
            log("CHANGE_TYPE_STATUS for the chat: "+chat.getChatId());
            log("chat online status: "+chat.getOnlineStatus());
            if(!(chatRoom.isGroup())){
                int state = chat.getOnlineStatus();
                if(state == MegaChatApi.STATUS_ONLINE){
                    log("This user is connected: "+chatRoom.getTitle());
                    aB.setSubtitle(getString(R.string.online_status));
                }
                else{
                    log("This user status is: "+state+  " " + chatRoom.getTitle());
                    aB.setSubtitle(getString(R.string.offline_status));
                }
            }
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_UNREAD_COUNT)){
            log("CHANGE_TYPE_UNREAD_COUNT for the chat: "+chat.getChatId());
            chat = megaChatApi.getChatRoom(chat.getChatId());
            int unreadCount = chat.getUnreadCount();
            log("get unread count: "+unreadCount);
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

                    String nameTyping = chat.getPeerFirstnameByHandle(userHandleTyping);
                    log("userHandleTyping: "+userHandleTyping);
                    if(nameTyping==null){
                        log("NULL name");
                        nameTyping = "";
                    }
                    if(nameTyping.trim().isEmpty()){
                        log("EMPTY name");
                        nameTyping = chat.getPeerFullnameByHandle(userHandleTyping);
                        participantTyping.setFirstName(nameTyping);
                        userTypingtext.setText(getString(R.string.one_user_typing, nameTyping));
                    }
                    else{
                        participantTyping.setFirstName(nameTyping);
                        userTypingtext.setText(getString(R.string.one_user_typing, nameTyping));
                    }

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

                        String nameTyping = chat.getPeerFirstnameByHandle(userHandleTyping);
                        if(nameTyping==null){
                            nameTyping = "";
                        }
                        if(nameTyping.trim().isEmpty()){
                            nameTyping = chat.getPeerFullnameByHandle(userHandleTyping);
                            participantTyping.setFirstName(nameTyping);
                        }
                        else{
                            participantTyping.setFirstName(nameTyping);
                        }
                        userTypingTimeStamp = System.currentTimeMillis()/1000;
                        currentUserTyping.setTimeStampTyping(userTypingTimeStamp);

                        usersTypingSync.add(currentUserTyping);

                        //Show the notification
                        int size = usersTypingSync.size();
                        switch (size){
                            case 1:{
                                userTypingtext.setText(getString(R.string.one_user_typing, usersTypingSync.get(0).getParticipantTyping().getFirstName()));
                                break;
                            }
                            case 2:{
                                userTypingtext.setText(getString(R.string.two_users_typing, usersTypingSync.get(0).getParticipantTyping().getFirstName(), usersTypingSync.get(1).getParticipantTyping().getFirstName()));
                                break;
                            }
                            default:{
                                userTypingtext.setText(getString(R.string.three_users_typing, usersTypingSync.get(0).getParticipantTyping().getFirstName(), usersTypingSync.get(1).getParticipantTyping().getFirstName()));
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
                        userTypingtext.setText(getString(R.string.one_user_typing, usersTypingSync.get(0).getParticipantTyping().getFirstName()));
                        userTypingLayout.setVisibility(View.VISIBLE);
                        break;
                    }
                    case 2:{
                        userTypingtext.setText(getString(R.string.two_users_typing, usersTypingSync.get(0).getParticipantTyping().getFirstName(), usersTypingSync.get(1).getParticipantTyping().getFirstName()));
                        userTypingLayout.setVisibility(View.VISIBLE);
                        break;
                    }
                    default:{
                        userTypingtext.setText(getString(R.string.three_users_typing, usersTypingSync.get(0).getParticipantTyping().getFirstName(), usersTypingSync.get(1).getParticipantTyping().getFirstName()));
                        userTypingLayout.setVisibility(View.VISIBLE);
                        break;
                    }
                }
            }
        }
    }

    public void onLoadMessageNotSent(){
        log("onLoadMessageNotSent");

    }

    @Override
    public void onMessageLoaded(MegaChatApiJava api, MegaChatMessage msg) {
        log("onMessageLoaded!------------------------");

        if(msg!=null){

            log("Temporal id: "+msg.getTempId());
            log("Final id: "+msg.getMsgId());

            if((msg.getStatus()==MegaChatMessage.STATUS_SENDING)||(msg.getStatus()==MegaChatMessage.STATUS_SERVER_REJECTED)||(msg.getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL)){
                log("onMessageLoaded: Getting messages not sent yet!!!-------------------------------------------------: "+msg.getStatus());
                AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);
                int returnValue = modifyMessageReceived(androidMsg, false);
                if(returnValue!=-1){
                    log("onMessageLoaded: Message " + returnValue + " modified!");
                    return;
                }
            }

            AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);
            if (lastMessageSeen != null) {
                if(lastMessageSeen.getMsgId()==msg.getMsgId()){
                    log("onMessageLoaded: Last message seen received!");
                    lastSeenReceived=true;
                    positionToScroll = 0;
                }
            }
//
            if(firstMessageReceived){
                //TODO Only do this if the message is not ours and it's not already seen
                megaChatApi.setMessageSeen(idChat, msg.getMsgId());
                log("onMessageLoaded: set Message Seen: "+msg.getMsgIndex());
                firstMessageReceived = false;
            }
//
            if((msg.getStatus()==MegaChatMessage.STATUS_SENDING)||(msg.getStatus()==MegaChatMessage.STATUS_SERVER_REJECTED)||(msg.getStatus()==MegaChatMessage.STATUS_SENDING_MANUAL)){
                log("onMessageLoaded: No rise position to scroll: "+msg.getStatus());
            }
            else{
                if(positionToScroll>=0){
                    log("onMessageLoaded: Position to scroll up!");
                    positionToScroll++;
                }
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
            if(bufferMessages.size()==NUMBER_MESSAGES_TO_UPDATE_UI){
                log("onMessageLoaded: Show messages screen");
                chatRelativeLayout.setVisibility(View.VISIBLE);
                emptyScrollView.setVisibility(View.GONE);
                loadMessages();
            }
        }
        else{
            log("onMessageLoaded: The message is null");
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
                        mLayoutManager.scrollToPositionWithOffset(positionToScroll,10);

                    }
                }
            }
            getMoreHistory = true;
        }
    }

    @Override
    public void onMessageReceived(MegaChatApiJava api, MegaChatMessage msg) {
        log("onMessageReceived!");

        megaChatApi.setMessageSeen(idChat, msg.getMsgId());

        if(msg.getType()==MegaChatMessage.TYPE_CHAT_TITLE){
            log("Change of chat title");
            String newTitle = msg.getContent();
            if(newTitle!=null){
                aB.setTitle(newTitle);
            }
        }

        AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);
        appendMessage(androidMsg);

        int firstVisiblePosition = mLayoutManager.findLastVisibleItemPosition();
        log("The last item visible: "+firstVisiblePosition+" the last message: "+messages.size());
        mLayoutManager.scrollToPosition(messages.size()-1);
//        mLayoutManager.setStackFromEnd(true);
//        mLayoutManager.scrollToPosition(0);
    }

    @Override
    public void onMessageUpdate(MegaChatApiJava api, MegaChatMessage msg) {
        log("onMessageUpdate!: "+ msg.getMsgId());

        AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);

        if(msg.hasChanged(MegaChatMessage.CHANGE_TYPE_CONTENT)){
            log("Change content of the message");
            if(msg.isDeleted()){
                log("Message deleted!!");
            }
            modifyMessageReceived(androidMsg, false);
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
                modifyMessageReceived(androidMsg, true);
            }
            else{
                log("-----------Status : "+msg.getStatus());
                log("-----------Timestamp: "+msg.getTimestamp());
                log("-----------Content: "+msg.getContent());

                modifyMessageReceived(androidMsg, false);
            }
        }
    }

    public void addAlertMessageNotSent(MegaChatMessage msg){
        log("addAlertMessageNotSent");

        AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);
        modifyMessageReceived(androidMsg, false);



    }

    public int modifyMessageReceived(AndroidMegaChatMessage msg, boolean markAsSent){
        log("modifyMessageReceived");
        int indexToChange = -1;
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator();

        while (itr.hasNext()) {
            AndroidMegaChatMessage messageToCheck = itr.next();
            if(markAsSent){
                if (messageToCheck.getMessage().getTempId() == msg.getMessage().getTempId()) {
                    log("modifyMessageReceived: Mark as sent: " + messageToCheck.getMessage().getContent());
                    indexToChange = itr.nextIndex()-1;
                    break;
                }
            }
            else{
                if (messageToCheck.getMessage().getMsgId() == msg.getMessage().getMsgId()) {
                    log("modifyMessageReceived: Found message status !!: " + messageToCheck.getMessage().getContent());
                    indexToChange = itr.nextIndex()-1;
                    break;
                }
            }
        }

        if(indexToChange!=-1){
            AndroidMegaChatMessage messageToUpdate = messages.get(indexToChange);
            if(messageToUpdate.getMessage().getMsgIndex()==msg.getMessage().getMsgIndex()){
                log("modifyMessageReceived: The internal index not change");

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
                        if (compareDate(msg, previousMessage) == 0) {
                            messages.get(indexToChange).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                        } else {
                            //Different date
                            messages.get(indexToChange).setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                        }
                    }
                }

                //Create adapter
                if (adapter == null) {
                    adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
                    adapter.setHasStableIds(true);
                    listView.setAdapter(adapter);
                    adapter.setPositionClicked(-1);
                } else {
                    adapter.setPositionClicked(-1);
                    adapter.modifyMessage(messages, indexToChange);
                }
            }
            else{
                log("modifyMessageReceived: INDEX change, need to reorder");
                messages.remove(indexToChange);
                log("modifyMessageReceived: messages size: "+messages.size());
                adapter.notifyItemRemoved(indexToChange);
                appendMessage(msg);
                log("modifyMessageReceived: messages size 2: "+messages.size());
            }

        }
        else{
            log("Error, id temp message not found!!");
        }
        return indexToChange;
    }

    public void loadMessages(){
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
//            adapter.setPositionClicked(-1);
        }
        else{
//            adapter.setPositionClicked(-1);
            adapter.loadPreviousMessages(messages, bufferMessages.size());
//            adapter.setMessages(messages, infoToShow);
//            adapter.notifyDataSetChanged();
            log("addMesagge: "+messages.size());
        }

        log("AFTER updateMessagesLoaded: "+messages.size()+" messages in list");

        bufferMessages.clear();
    }

    public void loadMessage(AndroidMegaChatMessage messageToShow){
        log("createInfoToSHOw");

        log("Message to show: "+messageToShow.getMessage().getContent());
        messageToShow.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
        messages.add(0,messageToShow);

//        for(int i=0; i<messages.size(); i++){
//            log("Print i: "+i+" content: "+messages.get(i).getMessage().getContent());
//        }

        if(messages.size()>1) {
            long userHandleToCompare = -1;
            if ((messageToShow.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (messageToShow.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                userHandleToCompare = messageToShow.getMessage().getUserHandleOfAction();
            } else {
                userHandleToCompare = messageToShow.getMessage().getUserHandle();
            }

//            int lastIndex = messages.size() - 2;
            AndroidMegaChatMessage previousMessage = messages.get(1);
            log("Previous message: "+previousMessage.getMessage().getContent());
            if (userHandleToCompare == myUserHandle) {
//                log("MY message!!: "+messageToShow.getContent());
                long previousUserHandleToCompare = -1;
                if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (messageToShow.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                    previousUserHandleToCompare = previousMessage.getMessage().getUserHandleOfAction();
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
                    previousUserHandleToCompare = previousMessage.getMessage().getUserHandleOfAction();
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

    public void appendMessage(AndroidMegaChatMessage msg){
        log("appendMessage: "+messages.size()+" messages");

        if(messages.size()==0){
            msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
            messages.add(msg);
        }
        else{
            int lastIndex = messages.size()-1;
            AndroidMegaChatMessage previousMessage = messages.get(lastIndex);
//            log("Previous message: "+previousMessage.getMessage().getContent());

            messages.add(msg);
            log("appendMessage size: "+messages.size());

            long userHandleToCompare = -1;
            if ((msg.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (msg.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                userHandleToCompare = msg.getMessage().getUserHandleOfAction();
            } else {
                userHandleToCompare = msg.getMessage().getUserHandle();
            }

            if (userHandleToCompare == myUserHandle) {
//                log("MY message!!: "+messageToShow.getContent());
                long previousUserHandleToCompare = -1;
                if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (msg.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                    previousUserHandleToCompare = previousMessage.getMessage().getUserHandleOfAction();
                } else {
                    previousUserHandleToCompare = previousMessage.getMessage().getUserHandle();
                }

//                    log("previous message: "+previousMessage.getContent());
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

            } else {
                log("NOT MY message!! - CONTACT");
//                    log("previous message: "+previousMessage.getContent());
                long previousUserHandleToCompare = -1;
                if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (msg.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                    previousUserHandleToCompare = previousMessage.getMessage().getUserHandleOfAction();
                } else {
                    previousUserHandleToCompare = previousMessage.getMessage().getUserHandle();
                }

                if (previousUserHandleToCompare == userHandleToCompare) {
                    //The last message is also a contact's message
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
                    //The last message is from contact, the previous not
                    if (compareDate(msg, previousMessage) == 0) {
                        msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_TIME);
                    } else {
                        //Different date
                        msg.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                    }
                }
            }
        }

        //Create adapter
        if(adapter==null){
            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
            adapter.setHasStableIds(true);
            listView.setLayoutManager(mLayoutManager);
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
//            adapter.setPositionClicked(-1);
        }
        else{
//            adapter.setPositionClicked(-1);
            adapter.appendMessage(messages);
//            adapter.setMessages(messages, infoToShow);
//            adapter.notifyDataSetChanged();
            log("addMesagge: "+messages.size());
        }
    }

    public boolean isGroup(){
        return chatRoom.isGroup();
    }

    public void showMsgNotSentPanel(AndroidMegaChatMessage message, int position){
//        showSnackbar("Not yet implemented!");

        log("showMessagePanel");
        this.selectedMessage = message;
        this.selectedPosition = position;

        if(message!=null){
//            this.selectedChatItem = chat;
            MessageNotSentBottomSheetDialogFragment bottomSheetDialogFragment = new MessageNotSentBottomSheetDialogFragment();
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        }
    }

    public void removeMsgNotSent(){
        log("removeMsgNotSent");
        messages.remove(selectedPosition);
        adapter.notifyItemRemoved(selectedPosition);
    }

    public void showSnackbar(String s){
        log("showSnackbar: "+s);
        Snackbar snackbar = Snackbar.make(fragmentContainer, s, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
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
            adapter.setPositionClicked(-1);
            adapter.setMessages(messages);
        } else {
            adapter.setPositionClicked(-1);
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
                int lastIndex = messages.size()-1;
                AndroidMegaChatMessage lastMessage = messages.get(lastIndex);
                lastMessage.setInfoToShow(Constants.CHAT_ADAPTER_SHOW_ALL);
                messages.clear();
                messages.add(lastMessage);
                adapter.setMessages(messages);
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
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestTemporaryError");
    }

    @Override
    protected void onDestroy(){
        log("onDestroy()");

        megaChatApi.closeChatRoom(idChat, this);

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

    public AndroidMegaChatMessage getSelectedMessage() {
        return selectedMessage;
    }

    public void setSelectedMessage(AndroidMegaChatMessage selectedMessage) {
        this.selectedMessage = selectedMessage;
    }

    public MegaChatRoom getChatRoom() {
        return chatRoom;
    }

    public void setChatRoom(MegaChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
    }

    //    @Override
//    protected void onResume() {
//        log("onResume");
//        super.onResume();
//
//        Intent newIntent = getIntent();
//
//        if (newIntent != null){
//            if (newIntent.getAction() != null){
//                log("Intent is here!: "+newIntent.getAction());
//                if (newIntent.getAction().equals(Constants.ACTION_CLEAR_CHAT)){
//                    log("Intent to Clear history");
//                    showConfirmationClearChat(chatRoom);
//                }
//                else{
//                    log("Other intent");
//                }
//            }
//        }
//
//    }
}