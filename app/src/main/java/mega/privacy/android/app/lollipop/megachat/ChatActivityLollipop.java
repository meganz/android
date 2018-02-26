package mega.privacy.android.app.lollipop.megachat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.components.NpaLinearLayoutManager;
import mega.privacy.android.app.components.emojicon.EmojiconEditText;
import mega.privacy.android.app.components.emojicon.EmojiconGridFragment;
import mega.privacy.android.app.components.emojicon.EmojiconsFragment;
import mega.privacy.android.app.components.emojicon.emoji.Emojicon;
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.MultipleGroupChatRequestListener;
import mega.privacy.android.app.lollipop.managerSections.FileBrowserFragmentLollipop;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatLollipopAdapter;
import mega.privacy.android.app.lollipop.tasks.FilePrepareTask;
import mega.privacy.android.app.modalbottomsheet.NodeOptionsBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.AttachmentUploadBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ContactAttachmentBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.MessageNotSentBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.NodeAttachmentBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.PendingMessageBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.TimeChatUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
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
import nz.mega.sdk.MegaUser;




public class ChatActivityLollipop extends PinActivityLollipop implements MegaChatRequestListenerInterface, MegaRequestListenerInterface, MegaChatListenerInterface, MegaChatRoomListenerInterface, RecyclerView.OnItemTouchListener, GestureDetector.OnGestureListener, View.OnClickListener, EmojiconGridFragment.OnEmojiconClickedListener,EmojiconsFragment.OnEmojiconBackspaceClickedListener {

    public static int NUMBER_MESSAGES_TO_LOAD = 20;
    public static int NUMBER_MESSAGES_TO_UPDATE_UI = 7;
    public static int NUMBER_MESSAGES_BEFORE_LOAD = 8;
    public static int REQUEST_CODE_SELECT_CHAT = 1005;

    boolean firstMessageReceived = true;
    boolean getMoreHistory=true;

    private AlertDialog errorOpenChatDialog;

    private android.support.v7.app.AlertDialog downloadConfirmationDialog;

    boolean sendOriginalAttachments = false;

    ProgressDialog dialog;
    ProgressDialog statusDialog;

    MegaChatMessage lastMessageSeen = null;
    boolean lastSeenReceived = false;
    int positionToScroll = -1;

    boolean isTakePicture = false;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    Handler handlerReceive;
    Handler handlerSend;

    boolean pendingMessagesLoaded = false;

    boolean isFirstTimeStorage = true;

    boolean startVideo = false;
    boolean activityVisible = false;

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
    boolean sendIsTyping=true;
    long userTypingTimeStamp = -1;
//    TextView inviteText;
    ImageButton keyboardButton;
    ImageButton mediaButton;
    ImageButton sendContactButton ;
    ImageButton pickFileSystemButton;
    ImageButton pickCloudDriveButton;
    ImageButton pickFileStorageButton;

    RelativeLayout rLKeyboardButton;
    RelativeLayout rLMediaButton;
    RelativeLayout rLSendContactButton ;
    RelativeLayout rLPickFileSystemButton;
    RelativeLayout rLPickCloudDriveButton;
    RelativeLayout rLPickFileStorageButton;

    EmojiconEditText textChat;
    ImageButton sendIcon;
    RelativeLayout messagesContainerLayout;

    //** FloatingActionButton fab;
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

    boolean focusChanged=false;

    String intentAction;
    MegaChatLollipopAdapter adapter;

    int stateHistory;

    DatabaseHandler dbH = null;

    int keyboardSize = -1;
    int firstSize = -1;

    boolean emojiKeyboardShown = false;
    boolean softKeyboardShown = false;

    FrameLayout fragmentContainerFileStorage;
    RelativeLayout fileStorageLayout;
//    private ArrayList<String> images;
    private ChatFileStorageFragment fileStorageF;

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

    boolean setAsRead = false;

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

                if(position<1){
                    log("Position not valid: "+position);
                }
                else{
                    if(!messages.get(position-1).isUploading()){

                        if(MegaApplication.isShowInfoChatMessages()){
                            showMessageInfo(position);
                        }
                        else{
                            adapter.setMultipleSelect(true);

                            actionMode = startSupportActionMode(new ActionBarCallBack());

                            if(position<1){
                                log("Position not valid");
                            }
                            else{
                                itemClick(position);
                            }
                        }
                    }
                }
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
            if(position<1){
                log("Position not valid");
            }
            else{
                itemClick(position);
            }
            return true;
        }
    }

    public void showMessageInfo(int positionInAdapter){

        int position = positionInAdapter-1;

        if(position<messages.size()) {
            AndroidMegaChatMessage androidM = messages.get(position);
            StringBuilder messageToShow = new StringBuilder("");
            String token = FirebaseInstanceId.getInstance().getToken();
            if(token!=null){
                messageToShow.append("FCM TOKEN: " +token);
            }
            messageToShow.append("\nCHAT ID: " + MegaApiJava.handleToBase64(idChat));
            messageToShow.append("\nMY USER HANDLE: " +MegaApiJava.userHandleToBase64(megaChatApi.getMyUserHandle()));
            if(androidM!=null){
                MegaChatMessage m = androidM.getMessage();
                if(m!=null){
                    messageToShow.append("\nMESSAGE TYPE: " +m.getType());
                    messageToShow.append("\nMESSAGE TIMESTAMP: " +m.getTimestamp());
                    messageToShow.append("\nMESSAGE USERHANDLE: " +MegaApiJava.userHandleToBase64(m.getUserHandle()));
                    messageToShow.append("\nMESSAGE ID: " +MegaApiJava.handleToBase64(m.getMsgId()));
                    messageToShow.append("\nMESSAGE TEMP ID: " +MegaApiJava.handleToBase64(m.getTempId()));
                }
            }

            Toast.makeText(this, messageToShow, Toast.LENGTH_SHORT).show();
            log("showMessageInfo: "+messageToShow);
        }
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

        if(megaChatApi==null||megaChatApi.getInitState()==MegaChatApi.INIT_ERROR||megaChatApi.getInitState()==0){
            log("Refresh session - karere");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }

        log("addChatListener");
        megaChatApi.addChatListener(this);

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

        rLKeyboardButton = (RelativeLayout) findViewById(R.id.rl_keyboard_icon_chat);
        rLMediaButton = (RelativeLayout) findViewById(R.id.rl_media_icon_chat);
        rLSendContactButton = (RelativeLayout) findViewById(R.id.rl_send_contact_icon_chat);
        rLPickFileSystemButton = (RelativeLayout) findViewById(R.id.rl_pick_file_system_icon_chat);
        rLPickFileStorageButton = (RelativeLayout) findViewById(R.id.rl_pick_file_storage_icon_chat);
        rLPickCloudDriveButton = (RelativeLayout) findViewById(R.id.rl_pick_cloud_drive_icon_chat);

        keyboardButton = (ImageButton) findViewById(R.id.keyboard_icon_chat);
        mediaButton = (ImageButton) findViewById(R.id.media_icon_chat);
        sendContactButton = (ImageButton) findViewById(R.id.send_contact_icon_chat);
        pickFileSystemButton = (ImageButton) findViewById(R.id.pick_file_system_icon_chat);
        pickFileStorageButton = (ImageButton) findViewById(R.id.pick_file_storage_icon_chat);
        pickCloudDriveButton = (ImageButton) findViewById(R.id.pick_cloud_drive_icon_chat);

        textChat = (EmojiconEditText) findViewById(R.id.edit_text_chat);

        rLKeyboardButton.setOnClickListener(this);
        rLMediaButton.setOnClickListener(this);
        rLSendContactButton.setOnClickListener(this);
        rLPickFileSystemButton.setOnClickListener(this);
        rLPickFileStorageButton.setOnClickListener(this);
        rLPickCloudDriveButton.setOnClickListener(this);

        keyboardButton.setOnClickListener(this);
        mediaButton.setOnClickListener(this);
        sendContactButton.setOnClickListener(this);
        pickFileSystemButton.setOnClickListener(this);
        pickFileStorageButton.setOnClickListener(this);
        pickCloudDriveButton.setOnClickListener(this);

        fragmentContainerFileStorage = (FrameLayout) findViewById(R.id.fragment_container_file_storage);
        fileStorageLayout = (RelativeLayout) findViewById(R.id.relative_layout_file_storage);
        fileStorageLayout.setVisibility(View.GONE);

//        imageView = (ImageView) findViewById(R.id.imageView);


       // GridView gallery = (GridView) findViewById(R.id.galleryGridView);

        //gallery.setAdapter(new ImageAdapter(this));

//        gallery.setOnItemClickListener(new OnItemClickListener() {
//
//            @Override
//            public void onItemClick(AdapterView<?> arg0, View arg1,
//                                    int position, long arg3) {
//                if (null != images && !images.isEmpty())
//                    Toast.makeText(
//                            getApplicationContext(),
//                            "position " + position + " " + images.get(position),
//                            300).show();
//                ;
//
//            }
//        });

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
                            sendIcon.setEnabled(true);

                            sendIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_send_black));

                        }
                        else{
                            sendIcon.setEnabled(false);

                            sendIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_send_trans));
                        }
                    }
                    else {
                        sendIcon.setEnabled(false);

                        sendIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_send_trans));
                    }
                }
                else{
                    sendIcon.setEnabled(false);

                    sendIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_send_trans));
                }

                if(getCurrentFocus() == textChat)
                {
                    // is only executed if the EditText was directly changed by the user

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
            }
        });

        textChat.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (emojiKeyboardShown){
                    keyboardButton.setImageResource(R.drawable.ic_emoticon_white);
                    removeEmojiconFragment();
                }
                if(fileStorageLayout.isShown()){
                    if(fileStorageF != null){
                        fileStorageF.clearSelections();
                        fileStorageF.hideMultipleSelect();
                    }
                    fileStorageLayout.setVisibility(View.GONE);
                }
                return false;
            }
        });

        textChat.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {

                if (emojiKeyboardShown){
                    keyboardButton.setImageResource(R.drawable.ic_emoticon_white);
                    removeEmojiconFragment();
                }
                if(fileStorageLayout.isShown()){
                    if(fileStorageF != null){
                        fileStorageF.clearSelections();
                        fileStorageF.hideMultipleSelect();
                    }
                    fileStorageLayout.setVisibility(View.GONE);
                }
                textChat.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(textChat, InputMethodManager.SHOW_IMPLICIT);

                return false;
            }
        });

        textChat.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                }
                return false;
            }
        });

        chatRelativeLayout  = (RelativeLayout) findViewById(R.id.relative_chat_layout);

        sendIcon = (ImageButton) findViewById(R.id.send_message_icon_chat);
        sendIcon.setOnClickListener(this);
        sendIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_send_trans));
        sendIcon.setEnabled(false);

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

//        listView.setAdapter(null);
//        adapter = null;

        messagesContainerLayout = (RelativeLayout) findViewById(R.id.message_container_chat_layout);

        userTypingLayout = (RelativeLayout) findViewById(R.id.user_typing_layout);
        userTypingLayout.setVisibility(View.GONE);
        userTypingText = (TextView) findViewById(R.id.user_typing_text);

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
                    }else{
                        softKeyboardShown = false;
                    }
                    if (shouldShowEmojiKeyboard){
                        setEmojiconFragment(false);
                        shouldShowEmojiKeyboard = false;
                    }

                }
            });
        }

        if(savedInstanceState!=null) {
            log("Bundle is NOT NULL");

        }
        Intent newIntent = getIntent();

        if (newIntent != null){
            log("Intent is not null");
            intentAction = newIntent.getAction();
            if (intentAction != null){

                idChat = newIntent.getLongExtra("CHAT_ID", -1);
//                    idChat=8179160514871859886L;
                myMail = megaApi.getMyEmail();
                myUserHandle = megaChatApi.getMyUserHandle();

                if(savedInstanceState!=null) {
                    log("Bundle is NOT NULL");
                    selectedMessageId = savedInstanceState.getLong("selectedMessageId", -1);
                    log("Handle of the message: "+selectedMessageId);
                    selectedPosition = savedInstanceState.getInt("selectedPosition", -1);
                }

                if(idChat!=-1) {

                    if(megaApi.getNumPendingUploads()<=0){
                        dbH.setFinishedPendingMessages();
                    }

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
                        int chatConnection = megaChatApi.getChatConnectionState(idChat);
                        log("Chat connection (" + idChat+ ") is: "+chatConnection);
                        if(chatConnection==MegaChatApi.CHAT_CONNECTION_ONLINE){
                            setAsRead=true;
                        }
                        else{
                            setAsRead=false;
                        }

                        MegaApplication.setOpenChatId(idChat);
                        messages = new ArrayList<AndroidMegaChatMessage>();
                        bufferMessages = new ArrayList<AndroidMegaChatMessage>();
                        bufferManualSending = new ArrayList<AndroidMegaChatMessage>();
                        bufferSending = new ArrayList<AndroidMegaChatMessage>();

                        if (adapter == null) {
                            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
                            adapter.setHasStableIds(true);
                            listView.setAdapter(adapter);
                        }

                        log("Result of open chat: " + result);

                        aB.setTitle(chatRoom.getTitle());
                        setChatPermissions();

                        if (intentAction.equals(Constants.ACTION_NEW_CHAT)) {
                            log("ACTION_CHAT_NEW");
                            textChat.setOnFocusChangeListener(focus);
                        } else if (intentAction.equals(Constants.ACTION_CHAT_SHOW_MESSAGES)) {
                            log("ACTION_CHAT_SHOW_MESSAGES");

                            loadHistory();
                            log("On create: stateHistory: "+stateHistory);
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

    public void loadHistory(){
        log("loadHistory");

        long unread = chatRoom.getUnreadCount();
        //                        stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
        if (unread == 0) {
            lastMessageSeen = null;
            lastSeenReceived = true;
            log("loadMessages unread is 0");
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
                log("A->loadMessages " + chatRoom.getUnreadCount());
                long unreadAbs = Math.abs(unread);
                stateHistory = megaChatApi.loadMessages(idChat, (int) unreadAbs);
            } else if (unread >= 0 && unread <= NUMBER_MESSAGES_TO_LOAD) {
                log("B->loadMessages " + chatRoom.getUnreadCount());
                stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
            } else if (unread < NUMBER_MESSAGES_TO_LOAD) {
                log("C->loadMessages " + chatRoom.getUnreadCount());
                stateHistory = megaChatApi.loadMessages(idChat, chatRoom.getUnreadCount());
            } else {
                log("D->loadMessages " + chatRoom.getUnreadCount());
                stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
            }
        }
    }

    public void setChatPermissions(){
        log("setChatPermissions");
        if(megaChatApi.getConnectionState()!=MegaChatApi.CONNECTED){
            log("Chat not connected");
            aB.setSubtitle(getString(R.string.invalid_connection_state));
        }
        else{
            int permission = chatRoom.getOwnPrivilege();
            if (chatRoom.isGroup()) {
                log("Check permissions group chat");
                if(permission==MegaChatRoom.PRIV_RO) {
                    log("Permission RO");
                    writingContainerLayout.setVisibility(View.GONE);

                    mediaButton.setVisibility(View.GONE);
                    sendContactButton.setVisibility(View.GONE);
                    pickFileSystemButton.setVisibility(View.GONE);
                    pickCloudDriveButton.setVisibility(View.GONE);
                    pickFileStorageButton.setVisibility(View.GONE);

                    aB.setSubtitle(getString(R.string.observer_permission_label_participants_panel));
                }
                else if(permission==MegaChatRoom.PRIV_RM) {
                    log("Permission RM");
                    writingContainerLayout.setVisibility(View.GONE);

                    mediaButton.setVisibility(View.GONE);
                    sendContactButton.setVisibility(View.GONE);
                    pickFileSystemButton.setVisibility(View.GONE);
                    pickCloudDriveButton.setVisibility(View.GONE);
                    pickFileStorageButton.setVisibility(View.GONE);

                    aB.setSubtitle(null);
                }
                else{
                    log("permission: "+permission);
                    writingContainerLayout.setVisibility(View.VISIBLE);

                    mediaButton.setVisibility(View.VISIBLE);
                    sendContactButton.setVisibility(View.VISIBLE);
                    pickFileSystemButton.setVisibility(View.VISIBLE);
                    pickCloudDriveButton.setVisibility(View.VISIBLE);
                    pickFileStorageButton.setVisibility(View.VISIBLE);

                    aB.setSubtitle(null);
                }
            }
            else{
                log("Check permissions one to one chat");
                if(permission==MegaChatRoom.PRIV_RO) {
                    log("Permission RO");
                    writingContainerLayout.setVisibility(View.GONE);

                    mediaButton.setVisibility(View.GONE);
                    sendContactButton.setVisibility(View.GONE);
                    pickFileSystemButton.setVisibility(View.GONE);
                    pickCloudDriveButton.setVisibility(View.GONE);
                    pickFileStorageButton.setVisibility(View.GONE);

                    aB.setSubtitle(getString(R.string.observer_permission_label_participants_panel));
                }
                else if(permission==MegaChatRoom.PRIV_RM) {
                    log("Permission RM");
                    writingContainerLayout.setVisibility(View.GONE);

                    mediaButton.setVisibility(View.GONE);
                    sendContactButton.setVisibility(View.GONE);
                    pickFileSystemButton.setVisibility(View.GONE);
                    pickCloudDriveButton.setVisibility(View.GONE);
                    pickFileStorageButton.setVisibility(View.GONE);

                    aB.setSubtitle(null);
                }
                else{
                    long userHandle = chatRoom.getPeerHandle(0);
                    setStatus(userHandle);
                    writingContainerLayout.setVisibility(View.VISIBLE);

                    mediaButton.setVisibility(View.VISIBLE);
                    sendContactButton.setVisibility(View.VISIBLE);
                    pickFileSystemButton.setVisibility(View.VISIBLE);
                    pickCloudDriveButton.setVisibility(View.VISIBLE);
                    pickFileStorageButton.setVisibility(View.VISIBLE);

                }
            }
        }
    }

    public void setStatus(long userHandle){

        if(megaChatApi.getConnectionState()!=MegaChatApi.CONNECTED){
            log("Chat not connected");
            aB.setSubtitle(getString(R.string.invalid_connection_state));
        }
        else{
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
            else if(state == MegaChatApi.STATUS_OFFLINE){
                log("This user is offline");
                aB.setSubtitle(getString(R.string.offline_status));
            }
            else if(state == MegaChatApi.STATUS_INVALID){
                log("INVALID status: "+state);
                aB.setSubtitle(null);
            }
            else{
                log("This user status is: "+state);
                aB.setSubtitle(null);
            }
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
                if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) emojiKeyboardLayout.getLayoutParams();
                    params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics());
                    emojiKeyboardLayout.setLayoutParams(params);
                }else{
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) emojiKeyboardLayout.getLayoutParams();
                    params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300, getResources().getDisplayMetrics());
                    emojiKeyboardLayout.setLayoutParams(params);
                }
            }else {
                if (emojiKeyboardLayout != null) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) emojiKeyboardLayout.getLayoutParams();
                    params.height = keyboardSize;
                    emojiKeyboardLayout.setLayoutParams(params);
                }
            }
        }else{
            if (emojiKeyboardLayout != null) {
                if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) emojiKeyboardLayout.getLayoutParams();
                    params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics());
                    emojiKeyboardLayout.setLayoutParams(params);
                }else{
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) emojiKeyboardLayout.getLayoutParams();
                    params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300, getResources().getDisplayMetrics());
                    emojiKeyboardLayout.setLayoutParams(params);
                }
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

            if(megaChatApi.getConnectionState()!=MegaChatApi.CONNECTED){
                leaveMenuItem.setVisible(false);
                callMenuItem.setVisible(false);
                videoMenuItem.setVisible(false);
                clearHistoryMenuItem.setVisible(false);
                inviteMenuItem.setVisible(false);
            }
            else{
                int permission = chatRoom.getOwnPrivilege();
                log("Permission in the chat: "+permission);
                if(chatRoom.isGroup()){

                    if(permission==MegaChatRoom.PRIV_MODERATOR) {
                        inviteMenuItem.setVisible(true);

                        int lastMessageIndex = messages.size()-1;
                        if(lastMessageIndex>=0){
                            AndroidMegaChatMessage lastMessage = messages.get(lastMessageIndex);
                            if(!lastMessage.isUploading()){
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
                                log("Last message is UPLOADING");
                                clearHistoryMenuItem.setVisible(true);
                            }
                        }
                        else{
                            clearHistoryMenuItem.setVisible(false);
                        }

                        leaveMenuItem.setVisible(true);
                    }
                    else if(permission==MegaChatRoom.PRIV_RM) {
                        log("Group chat PRIV_RM");
                        leaveMenuItem.setVisible(false);
                        clearHistoryMenuItem.setVisible(false);
                        inviteMenuItem.setVisible(false);
                    }
                    else if(permission==MegaChatRoom.PRIV_RO) {
                        log("Group chat PRIV_RM");
                        leaveMenuItem.setVisible(true);
                        clearHistoryMenuItem.setVisible(false);
                        inviteMenuItem.setVisible(false);
                    }
                    else{
                        log("Permission: "+permission);
                        leaveMenuItem.setVisible(true);
                        clearHistoryMenuItem.setVisible(false);
                        inviteMenuItem.setVisible(false);
                    }

                    callMenuItem.setVisible(false);
                    videoMenuItem.setVisible(false);

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
                        callMenuItem.setVisible(true);
                        videoMenuItem.setVisible(true);
                    }
                    leaveMenuItem.setVisible(false);
                }
            }
        }
        else{
            log("Chatroom NULL on create menu");
            leaveMenuItem.setVisible(false);
            callMenuItem.setVisible(false);
            videoMenuItem.setVisible(false);
            clearHistoryMenuItem.setVisible(false);
            inviteMenuItem.setVisible(false);
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


                if (chatRoom.isGroup())
                {
                    showSnackbar("Coming soon...!");
                }
                else
                {
                    startVideo = false;
                    if(checkPermissionsCall()){
                        startCall();
                    }
                }
                break;
            }
            case R.id.cab_menu_video_chat:{

                if (chatRoom.isGroup())
                {
                    showSnackbar("Coming soon...!");
                }
                else
                {
                    startVideo = true;
                    if(checkPermissionsCall()){
                        startCall();
                    }
                }
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

    public void startCall(){
        if(startVideo){
            log("Start video call");
            megaChatApi.startChatCall(chatRoom.getChatId(), startVideo, this);
        }
        else{
            log("Start audio call");
            megaChatApi.startChatCall(chatRoom.getChatId(), startVideo, this);
        }
    }

    public boolean checkPermissionsCall(){
        log("checkPermissionsCall");

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

    public boolean checkPermissionsTakePicture(){
        log("checkPermissionsCall");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            boolean hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
            if (!hasCameraPermission) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, Constants.REQUEST_CAMERA);
                return false;
            }

            boolean hasRecordAudioPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasRecordAudioPermission) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.REQUEST_WRITE_STORAGE);
                return false;
            }

            return true;
        }
        return true;
    }

    public boolean checkPermissionsReadStorage(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            boolean hasReadStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasReadStoragePermission) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Constants.REQUEST_READ_STORAGE);
                return false;
            }

            return true;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        log("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constants.REQUEST_CAMERA: {
                log("REQUEST_CAMERA");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(!isTakePicture){
                        if(checkPermissionsCall()){
                            startCall();
                        }
                    }
                    else{
                        if(checkPermissionsTakePicture()){
                            takePicture();
                        }
                    }
                }
                break;
            }
            case Constants.RECORD_AUDIO: {
                log("RECORD_AUDIO");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(checkPermissionsCall()){
                        startCall();
                    }
                }
                break;
            }
            case Constants.REQUEST_WRITE_STORAGE:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(checkPermissionsTakePicture()){
                        takePicture();
                    }
                }
                break;
            }
            case Constants.REQUEST_READ_STORAGE:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(checkPermissionsReadStorage()){
                        this.attachFromFileStorage();
                    }
                }
                break;
            }
        }
    }

    public void chooseAddParticipantDialog(){
        log("chooseAddContactDialog");

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }
        if(megaApi!=null && megaApi.getRootNode()!=null){
            ArrayList<MegaUser> contacts = megaApi.getContacts();
            if(contacts==null){
                showSnackbar("You have no MEGA contacts. Please, invite friends from the Contacts section");
            }
            else {
                if(contacts.isEmpty()){
                    showSnackbar("You have no MEGA contacts. Please, invite friends from the Contacts section");
                }
                else{
                    Intent in = new Intent(this, AddContactActivityLollipop.class);
                    in.putExtra("contactType", Constants.CONTACT_TYPE_MEGA);
                    in.putExtra("chat", true);
                    startActivityForResult(in, Constants.REQUEST_ADD_PARTICIPANTS);
                }
            }
        }
        else{
            log("Online but not megaApi");
            Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
        }
    }

    public void chooseContactsDialog(){
        log("chooseContactsDialog");

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        if(megaApi!=null && megaApi.getRootNode()!=null){
            ArrayList<MegaUser> contacts = megaApi.getContacts();
            if(contacts==null){
                showSnackbar("You have no MEGA contacts. Please, invite friends from the Contacts section");
            }
            else {
                if(contacts.isEmpty()){
                    showSnackbar("You have no MEGA contacts. Please, invite friends from the Contacts section");
                }
                else{
                    Intent in = new Intent(this, AddContactActivityLollipop.class);
                    in.putExtra("contactType", Constants.CONTACT_TYPE_MEGA);
                    in.putExtra("chat", true);
                    startActivityForResult(in, Constants.REQUEST_SEND_CONTACTS);
                }
            }
        }
        else{
            log("Online but not megaApi");
            Util.showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
        }
    }

    public void disablePinScreen(){
        log("disablePinScreen");
        MegaApplication.setShowPinScreen(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        log("onActivityResult, resultCode: " + resultCode);
        if (requestCode == Constants.REQUEST_ADD_PARTICIPANTS && resultCode == RESULT_OK) {
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
                    sendMessageToUI(contactMessage);
                }
            }
        }
        else if (requestCode == Constants.REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {

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

            for(int i=0; i<handles.length; i++){
                megaChatApi.attachNode(idChat, handles[i], this);
            }
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
        else if (requestCode == REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK) {
            if(!Util.isOnline(this)) {
                try{
                    statusDialog.dismiss();
                } catch(Exception ex) {};

                Snackbar.make(fragmentContainer, getString(R.string.error_server_connection_problem), Snackbar.LENGTH_LONG).show();
                return;
            }

            long[] chatHandles = intent.getLongArrayExtra("SELECTED_CHATS");
            log("Send to "+chatHandles.length+" chats");

            long[] idMessages = intent.getLongArrayExtra("ID_MESSAGES");
            log("Send "+idMessages.length+" messages");

            for(int k=0;k<chatHandles.length;k++){
                for(int i=0;i<idMessages.length;i++){
                    MegaChatMessage messageToForward = megaChatApi.getMessage(idChat, idMessages[i]);
                    if(messageToForward!=null){
                        int type = messageToForward.getType();
                        log("Type of message to forward: "+type);
                        switch(type){
                            case MegaChatMessage.TYPE_NORMAL:{
                                String text = messageToForward.getContent();
                                megaChatApi.sendMessage(chatHandles[k], text);
                                break;
                            }
                            case MegaChatMessage.TYPE_CONTACT_ATTACHMENT:{

                                MegaHandleList handleList = MegaHandleList.createInstance();
                                long userCount  = messageToForward.getUsersCount();
                                for(int j=0; j<userCount;j++){
                                    MegaUser user = megaApi.getContact(messageToForward.getUserEmail(j));
                                    if (user != null) {
                                        handleList.addMegaHandle(user.getHandle());
                                    }
                                    else{
                                        log("The user in not contact - cannot be forwarded");
                                    }
                                }

                                megaChatApi.attachContacts(chatHandles[k], handleList);
                                break;
                            }
                            case MegaChatMessage.TYPE_NODE_ATTACHMENT:{

                                MegaNodeList nodeList = messageToForward.getMegaNodeList();
                                if(nodeList != null) {
                                    for (int j = 0; j < nodeList.size(); j++) {
                                        MegaNode temp = nodeList.get(j);
                                        megaChatApi.attachNode(chatHandles[k], temp.getHandle(), null);
                                    }
                                }
                                break;
                            }
                        }
                    }
                    else{
                        log("ERROR -> message is null on forwarding");
                    }
                }
            }

            if(chatHandles.length==1){
                log("Open chat to forward messages");

                Intent intentOpenChat = new Intent(this, ManagerActivityLollipop.class);
                intentOpenChat.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intentOpenChat.setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE);
                intentOpenChat.putExtra("CHAT_ID", chatHandles[0]);
                startActivity(intentOpenChat);
                finish();
            }
            else{
                log("Send messages to no of chats: "+chatHandles.length);
                clearSelections();
                hideMultipleSelect();
            }
        }
        else if (requestCode == Constants.TAKE_PHOTO_CODE && resultCode == RESULT_OK) {
            if (resultCode == Activity.RESULT_OK) {
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.temporalPicDIR + "/picture.jpg";
                File imgFile = new File(filePath);

                String name = Util.getPhotoSyncName(imgFile.lastModified(), imgFile.getAbsolutePath());
                log("Taken picture Name: " + name);
                String newPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.temporalPicDIR + "/" + name;
                log("----NEW Name: " + newPath);
                File newFile = new File(newPath);
                imgFile.renameTo(newFile);

                uploadPicture(newPath);
            } else {
                log("TAKE_PHOTO_CODE--->ERROR!");
            }

        }else{
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
            keyboardButton.setImageResource(R.drawable.ic_emoticon_white);
            removeEmojiconFragment();
        }else if(fileStorageLayout.isShown()){
            if(fileStorageF != null){
                fileStorageF.clearSelections();
                fileStorageF.hideMultipleSelect();
            }
            fileStorageLayout.setVisibility(View.GONE);
        }else{
            finish();
        }
    }

    public static void log(String message) {
        Util.log("ChatActivityLollipop", message);
    }

    @Override
    public void onClick(View v) {
        log("onClick");
        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        switch (v.getId()) {
            case R.id.home:{
                break;
            }
//			case R.id.attach_icon_chat:{
//                showUploadPanel();
//                break;
//			}
            case R.id.send_message_icon_chat:{
                log("click on Send message");

//                fileStorageLayout.setVisibility(View.GONE);

                writingLayout.setClickable(false);
                String text = textChat.getText().toString();

                if((fileStorageF != null)&&(fileStorageF.isMultipleselect())){
                    fileStorageF.sendImages();
                }else{

                    if(!text.isEmpty()) {

                        if (editingMessage) {
                            editMessage(text);
                            log("Edited message: " + text);
                            clearSelections();
                            hideMultipleSelect();
                            actionMode.invalidate();
                        } else {
                            log("Call to send message: " + text);
                            sendMessage(text);
                        }

                        textChat.getText().clear();
                        textChat.setText("", TextView.BufferType.EDITABLE);
                    }
                }



                break;
            }
            case R.id.keyboard_icon_chat:
            case R.id.rl_keyboard_icon_chat:{
                log("open emoji keyboard:  " + emojiKeyboardShown);
                if(fileStorageLayout.isShown()){
                    if(fileStorageF != null){
                        fileStorageF.clearSelections();
                        fileStorageF.hideMultipleSelect();
                    }
                    fileStorageLayout.setVisibility(View.GONE);
                }
                if (emojiKeyboardShown){
                    removeEmojiconFragment();
                    textChat.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

                    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }else{
                        imm.showSoftInput(textChat, InputMethodManager.SHOW_IMPLICIT);
                    }
                    keyboardButton.setImageResource(R.drawable.ic_emoticon_white);

                } else{
                    InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);

                    if (softKeyboardShown){
                        log("imm.isAcceptingText()");

                        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                        }else{
                            imm.hideSoftInputFromWindow(textChat.getWindowToken(), 0);
                        }

                        shouldShowEmojiKeyboard = true;
                    }
                    else{
                        setEmojiconFragment(false);
                    }
                    keyboardButton.setImageResource(R.drawable.ic_keyboard_white);

                }
                break;
            }

            case R.id.media_icon_chat:
            case R.id.rl_media_icon_chat:{

                if(fileStorageLayout.isShown()){
                    if(fileStorageF != null){
                        fileStorageF.clearSelections();
                        fileStorageF.hideMultipleSelect();
                    }
                    fileStorageLayout.setVisibility(View.GONE);
                }
                if (emojiKeyboardShown) {
                    keyboardButton.setImageResource(R.drawable.ic_emoticon_white);
                    removeEmojiconFragment();
                }
                else if(softKeyboardShown){
                    InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
                    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }else{
                        imm.hideSoftInputFromWindow(textChat.getWindowToken(), 0);
                    }
                }

                isTakePicture = true;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                    if (!hasStoragePermission) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                Constants.REQUEST_WRITE_STORAGE);
                    }

                    boolean hasCameraPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
                    if (!hasCameraPermission) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.CAMERA},
                                Constants.REQUEST_CAMERA);
                    }

                    if (hasStoragePermission && hasCameraPermission){
                        this.takePicture();
                    }
                }
                else{
                    this.takePicture();
                }

                break;
            }

            case R.id.send_contact_icon_chat:
            case R.id.rl_send_contact_icon_chat:{
                if(fileStorageLayout.isShown()){
                    if(fileStorageF != null){
                        fileStorageF.clearSelections();
                        fileStorageF.hideMultipleSelect();
                    }
                    fileStorageLayout.setVisibility(View.GONE);
                }
                if (emojiKeyboardShown) {
                    keyboardButton.setImageResource(R.drawable.ic_emoticon_white);
                    removeEmojiconFragment();
                }
                else if(softKeyboardShown){
                    InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
                    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }else{
                        imm.hideSoftInputFromWindow(textChat.getWindowToken(), 0);
                    }
                }
                attachContact();

                break;
            }

            case R.id.pick_file_system_icon_chat:
            case R.id.rl_pick_file_system_icon_chat:{
                if(fileStorageLayout.isShown()){
                    if(fileStorageF != null){
                        fileStorageF.clearSelections();
                        fileStorageF.hideMultipleSelect();
                    }
                    fileStorageLayout.setVisibility(View.GONE);
                }
                if (emojiKeyboardShown) {
                    keyboardButton.setImageResource(R.drawable.ic_emoticon_white);
                    removeEmojiconFragment();
                }
                else if(softKeyboardShown){
                    InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
                    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }else{
                        imm.hideSoftInputFromWindow(textChat.getWindowToken(), 0);
                    }
                }

                attachPhotoVideo();
                break;
            }

            case R.id.pick_cloud_drive_icon_chat:
            case R.id.rl_pick_cloud_drive_icon_chat:{
                if(fileStorageLayout.isShown()){
                    if(fileStorageF != null){
                        fileStorageF.clearSelections();
                        fileStorageF.hideMultipleSelect();
                    }
                    fileStorageLayout.setVisibility(View.GONE);
                }

                if (emojiKeyboardShown) {
                    keyboardButton.setImageResource(R.drawable.ic_emoticon_white);
                    removeEmojiconFragment();
                }
                else if(softKeyboardShown){
                    InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
                    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }else{
                        imm.hideSoftInputFromWindow(textChat.getWindowToken(), 0);
                    }
                }

                attachFromCloud();
                break;
            }

            case R.id.pick_file_storage_icon_chat:
            case R.id.rl_pick_file_storage_icon_chat:{
                if (emojiKeyboardShown) {
                    keyboardButton.setImageResource(R.drawable.ic_emoticon_white);
                    removeEmojiconFragment();
                }
                else if(softKeyboardShown){
                    InputMethodManager imm = (InputMethodManager) getSystemService(this.INPUT_METHOD_SERVICE);
                    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }else{
                        imm.hideSoftInputFromWindow(textChat.getWindowToken(), 0);
                    }
                }

                if(fileStorageLayout.isShown()){
                    if(fileStorageF != null){
                        fileStorageF.clearSelections();
                        fileStorageF.hideMultipleSelect();
                    }
                    fileStorageLayout.setVisibility(View.GONE);

                }else{

                    fileStorageLayout.setVisibility(View.VISIBLE);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                        if (!hasStoragePermission) {
                            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},Constants.REQUEST_READ_STORAGE);

                        }else{
                            this.attachFromFileStorage();
                        }
                    }
                    else{
                        this.attachFromFileStorage();
                    }
                }

                break;
            }
		}
    }

    public void attachFromFileStorage(){

        if (isFirstTimeStorage) {
            fileStorageF = ChatFileStorageFragment.newInstance();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container_file_storage, fileStorageF,"fileStorageF")
                    .commitNowAllowingStateLoss();
            isFirstTimeStorage = false;
        }
//        fileStorageF = ChatFileStorageFragment.newInstance();
//        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//        ft.replace(R.id.fragment_container_file_storage, fileStorageF, "fileStorageF");
//        ft.commitNow();

        //ft.commitAllowingStateLoss();
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

        disablePinScreen();

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
        sendMessageToUI(msgSent);
    }

    public void sendMessageUploading(AndroidMegaChatMessage androidMsgSent){
        log("sendMessageUploading");

        log("Name of the file uploading: "+androidMsgSent.getPendingMessage().getNames().get(0));

        int index = messages.size();
        if(androidMsgSent!=null){

            if(index==0){
                //First element
                log("First element!");
                messages.add(androidMsgSent);
                messages.get(0).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
            }
            else{

                messages.add(index, androidMsgSent);
                log("Add to index: "+index);
                adjustInfoToShow(index);
            }
            if (adapter == null){
                log("adapter NULL");
                adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
                adapter.setHasStableIds(true);
                listView.setLayoutManager(mLayoutManager);
                listView.setAdapter(adapter);
                adapter.setMessages(messages);
            }
            else{
                log("adapter is NOT null");
                adapter.addMessage(messages, index+1);
                final int indexToScroll = index+1;
                mLayoutManager.scrollToPositionWithOffset(indexToScroll,Util.scaleHeightPx(20, outMetrics));

            }
        }
        else{
            log("Error sending message (2)!");
        }
    }

    public void sendMessageToUI(MegaChatMessage msgSent){
        log("sendMessageToUI");
        int infoToShow = -1;
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
                messages.get(0).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
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

                infoToShow = adjustInfoToShow(index);
            }
            if (adapter == null){
                log("adapter NULL");
                adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
                adapter.setHasStableIds(true);
                listView.setLayoutManager(mLayoutManager);
                listView.setAdapter(adapter);
                adapter.setMessages(messages);
            }
            else{
                log("adapter is NOT null");
                adapter.addMessage(messages, index);
                if(infoToShow== AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL){
                    mLayoutManager.scrollToPositionWithOffset(index, Util.scaleHeightPx(50, outMetrics));
                }
                else{
                    mLayoutManager.scrollToPositionWithOffset(index, Util.scaleHeightPx(20, outMetrics));
                }
            }
        }
        else{
            log("Error sending message!");
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
                    textChat.setSelection(textChat.getText().length());
                    //Show keyboard

                    break;
                }
                case R.id.chat_cab_menu_forward:{
                    log("Forward message");

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
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            log("onDEstroyActionMode");
            adapter.setMultipleSelect(false);
            textChat.getText().clear();
            editingMessage = false;
            clearSelections();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            log("onPrepareActionMode");
            List<AndroidMegaChatMessage> selected = adapter.getSelectedMessages();

            if (selected.size() !=0) {
//                MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);

                if(chatRoom.getOwnPrivilege()==MegaChatRoom.PRIV_RM||chatRoom.getOwnPrivilege()==MegaChatRoom.PRIV_RO){

                    boolean showCopy = true;

                    for(int i=0; i<selected.size();i++) {

                        if (showCopy) {
                            if (selected.get(i).getMessage().getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT || selected.get(i).getMessage().getType() == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
                                showCopy = false;
                            }
                        }
                    }

                    menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                    menu.findItem(R.id.chat_cab_menu_copy).setVisible(showCopy);
                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                    menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                }
                else{
                    log("Chat with permissions");
                    if (selected.size() == 1) {

                        if(selected.get(0).isUploading()){
                            menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                        }
                        else if(selected.get(0).getMessage().getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){
                            log("TYPE_NODE_ATTACHMENT selected");
                            menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);

                            if(selected.get(0).getMessage().getUserHandle()==myUserHandle){
                                if(selected.get(0).getMessage().isDeletable()){
                                    log("one message Message DELETABLE");
                                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                                }
                                else{
                                    log("one message Message NOT DELETABLE");
                                    menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                                }
                                menu.findItem(R.id.chat_cab_menu_forward).setVisible(true);

                            }
                            else{
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                                menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                            }
                        }
                        else if(selected.get(0).getMessage().getType()==MegaChatMessage.TYPE_CONTACT_ATTACHMENT){
                            menu.findItem(R.id.chat_cab_menu_copy).setVisible(false);
                            menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);

                            if(selected.get(0).getMessage().isDeletable()){
                                log("one message Message DELETABLE");
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(true);
                            }
                            else{
                                log("one message Message NOT DELETABLE");
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                            }

                            if(selected.get(0).getMessage().getUserHandle()==myUserHandle){
                                menu.findItem(R.id.chat_cab_menu_forward).setVisible(true);
                            }
                            else{
                                menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                            }
                        }
                        else{
                            MegaChatMessage messageSelected= megaChatApi.getMessage(idChat, selected.get(0).getMessage().getMsgId());

                            menu.findItem(R.id.chat_cab_menu_copy).setVisible(true);

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

                                int type = selected.get(0).getMessage().getType();
                                if (type == MegaChatMessage.TYPE_TRUNCATE||type == MegaChatMessage.TYPE_ALTER_PARTICIPANTS||type == MegaChatMessage.TYPE_CHAT_TITLE||type == MegaChatMessage.TYPE_PRIV_CHANGE) {
                                    menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                                }
                                else{
                                    menu.findItem(R.id.chat_cab_menu_forward).setVisible(true);
                                }
                            }
                            else{
                                menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                                menu.findItem(R.id.chat_cab_menu_delete).setVisible(false);
                                menu.findItem(R.id.chat_cab_menu_forward).setVisible(false);
                            }
                        }
                    }
                    else{
                        log("Many items selected");
                        boolean showDelete = true;
                        boolean showCopy = true;
                        boolean showForward = true;

                        for(int i=0; i<selected.size();i++) {

                            if (showCopy) {
                                if (selected.get(i).getMessage().getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT || selected.get(i).getMessage().getType() == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
                                    showCopy = false;
                                }
                            }

                            if (showDelete) {
                                if (selected.get(i).getMessage().getUserHandle() == myUserHandle) {
                                    if (selected.get(i).getMessage().getType() == MegaChatMessage.TYPE_NORMAL || selected.get(i).getMessage().getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT || selected.get(i).getMessage().getType() == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
                                        if (!(selected.get(i).getMessage().isDeletable())) {
                                            showDelete = false;
                                        }
                                    } else {
                                        showDelete = false;
                                    }
                                } else {
                                    showDelete = false;
                                }
                            }

                            if (showForward) {
                                if (selected.get(i).getMessage().getUserHandle() == myUserHandle) {
                                    int type = selected.get(i).getMessage().getType();
                                    if (type == MegaChatMessage.TYPE_TRUNCATE||type == MegaChatMessage.TYPE_ALTER_PARTICIPANTS||type == MegaChatMessage.TYPE_CHAT_TITLE||type == MegaChatMessage.TYPE_PRIV_CHANGE) {
                                        showForward = false;
                                    }
                                } else {
                                    showForward = false;
                                }
                            }
                        }

                        menu.findItem(R.id.chat_cab_menu_edit).setVisible(false);
                        menu.findItem(R.id.chat_cab_menu_copy).setVisible(showCopy);
                        menu.findItem(R.id.chat_cab_menu_delete).setVisible(showDelete);
                        menu.findItem(R.id.chat_cab_menu_forward).setVisible(showForward);
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
        builder.setPositiveButton(R.string.context_remove, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void showConfirmationDeleteMessage(final long messageId, final long chatId){
        log("showConfirmationDeleteMessage");

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
//        if (actionMode == null || getActivity() == null) {
//            return;
//        }
        List<AndroidMegaChatMessage> messages = adapter.getSelectedMessages();

        actionMode.setTitle(messages.size()+"");

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

    public void itemClick(int positionInAdapter) {
        int position = positionInAdapter-1;
        log("itemClick: "+position);
        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        if(position<messages.size()){
            AndroidMegaChatMessage m = messages.get(position);

            if (adapter.isMultipleSelect()) {

                if (!m.isUploading()) {
                    if (m.getMessage() != null) {
                        log("Message id: " + m.getMessage().getMsgId());
                        log("Timestamp: " + m.getMessage().getTimestamp());
                    }

                    adapter.toggleSelection(positionInAdapter);

                    List<AndroidMegaChatMessage> messages = adapter.getSelectedMessages();
                    if (messages.size() > 0) {
                        updateActionModeTitle();
//                adapter.notifyDataSetChanged();
                    } else {
                        hideMultipleSelect();
                    }
                }
            }
            else{

                if(m!=null){
                    if(m.isUploading()){
                        if(m.getPendingMessage().getState()==PendingMessage.STATE_ERROR){
                            showUploadingAttachmentBottomSheet(m, position);
                        }
                    }
                    else{

                        if(m.getMessage().getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){

                            MegaNodeList nodeList = m.getMessage().getMegaNodeList();
                            if(nodeList.size()==1){
                                MegaNode node = nodeList.get(0);

                                if (MimeTypeList.typeForName(node.getName()).isImage()){
                                    if(node.hasPreview()){
                                        log("Show full screen viewer");
                                        showFullScreenViewer(m.getMessage().getMsgId());
                                    }
                                    else{
                                        log("Image without preview - show node attachment panel for one node");
                                        showNodeAttachmentBottomSheet(m, position);
                                    }
                                }
                                else{
                                    log("NOT Image - show node attachment panel for one node");
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
        else{
            log("DO NOTHING: Position ("+position+") is more than size in messages (size: "+messages.size()+")");
        }
    }

    public void showFullScreenViewer(long msgId){
        log("showFullScreenViewer");
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
        long[] array = new long[ids.size()];
        for(int i = 0; i < ids.size(); i++) {
            array[i] = ids.get(i);
        }
        intent.putExtra("messageIds", array);
        startActivity(intent);
    }

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
            setChatPermissions();
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_STATUS)){
            log("CHANGE_TYPE_STATUS for the chat: "+chat.getChatId());
            if(!(chatRoom.isGroup())){
                long userHandle = chatRoom.getPeerHandle(0);
                setStatus(userHandle);
            }
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_PARTICIPANTS)){
            log("CHANGE_TYPE_PARTICIPANTS for the chat: "+chat.getChatId());
        }
        else if(chat.hasChanged(MegaChatRoom.CHANGE_TYPE_OWN_PRIV)){
            log("CHANGE_TYPE_OWN_PRIV for the chat: "+chat.getChatId());
            setChatPermissions();
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


                    if(nameTyping==null){
                        log("NULL name");
                        nameTyping = getString(R.string.transfer_unknown);
                    }
                    else{
                        if(nameTyping.trim().isEmpty()){
                            log("EMPTY name");
                            nameTyping = getString(R.string.transfer_unknown);
                        }
                    }
                    participantTyping.setFirstName(nameTyping);

                    userTypingTimeStamp = System.currentTimeMillis()/1000;
                    currentUserTyping.setTimeStampTyping(userTypingTimeStamp);

                    usersTypingSync.add(currentUserTyping);

                    String userTyping =  getResources().getQuantityString(R.plurals.user_typing, 1, usersTypingSync.get(0).getParticipantTyping().getFirstName());

                    userTyping = userTyping.replace("[A]", "<small><font color=\'#8d8d94\'>");
                    userTyping = userTyping.replace("[/A]", "</font></small>");

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
                            nameTyping = getString(R.string.transfer_unknown);
                        }
                        else{
                            if(nameTyping.trim().isEmpty()){
                                log("EMPTY name");
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

                                userTyping = userTyping.replace("[A]", "<small><font color=\'#8d8d94\'>");
                                userTyping = userTyping.replace("[/A]", "</font></small>");

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

                                userTyping = userTyping.replace("[A]", "<small><font color=\'#8d8d94\'>");
                                userTyping = userTyping.replace("[/A]", "</font></small>");

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
                                String userTyping = String.format(getString(R.string.more_users_typing), names);

                                userTyping = userTyping.replace("[A]", "<small><font color=\'#8d8d94\'>");
                                userTyping = userTyping.replace("[/A]", "</font></small>");

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
                        String userTyping = getResources().getQuantityString(R.plurals.user_typing, 1, usersTypingSync.get(0).getParticipantTyping().getFirstName());
                        userTyping = userTyping.replace("[A]", "<small><font color=\'#8d8d94\'>");
                        userTyping = userTyping.replace("[/A]", "</font></small>");

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
                        userTyping = userTyping.replace("[A]", "<small><font color=\'#8d8d94\'>");
                        userTyping = userTyping.replace("[/A]", "</font></small>");

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
                        String userTyping = String.format(getString(R.string.more_users_typing), names);

                        userTyping = userTyping.replace("[A]", "<small><font color=\'#8d8d94\'>");
                        userTyping = userTyping.replace("[/A]", "</font></small>");

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

            if(msg.isDeleted()){
                log("DELETED MESSAGE!!!!");
                return;
            }

            if(msg.isEdited()){
                log("EDITED MESSAGE!!!!");
            }

            if(activityVisible){
                if(setAsRead){
                    boolean markAsRead = megaChatApi.setMessageSeen(idChat, msg.getMsgId());
                    log("Result of markAsRead: "+markAsRead);
                }
            }

            if(msg.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT) {
                log("TYPE_REVOKE_NODE_ATTACHMENT MESSAGE!!!!");
                return;
            }

            if(msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){
                log("TYPE_NODE_ATTACHMENT MESSAGE!!!!");
                MegaNodeList nodeList = msg.getMegaNodeList();
                int revokedCount = 0;

                for(int i=0; i<nodeList.size(); i++){
                    MegaNode node = nodeList.get(i);
                    boolean revoked = megaChatApi.isRevoked(idChat, node.getHandle());
                    if(revoked){
                        log("The node is revoked: "+node.getName());
                        revokedCount++;
                    }
                    else{
                        log("Node NOT revoked: "+node.getName());
                    }
                }

                if(revokedCount==nodeList.size()){
                    log("RETURN");
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
                            log("Content not edited");
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

//                megaChatApi.setMessageSeen(idChat, msg.getMsgId());

                if(positionToScroll>=0){
                    log("onMessageLoaded: Position to scroll up!");
                    positionToScroll++;
                }

                bufferMessages.add(androidMsg);
                log("onMessageLoaded: Counter: "+bufferMessages.size());
                log("onMessageLoaded: Get type of message: "+msg.getType());

                log("onMessageLoaded: Size of messages: "+messages.size());
                if(bufferMessages.size()>=NUMBER_MESSAGES_TO_UPDATE_UI){
                    log("onMessageLoaded: Show messages screen");
                    loadBufferMessages();
                }
                else{
                    log("Do not update screen yet: "+bufferMessages.size());
                }
            }
        }
        else{
            log("onMessageLoaded: The message is null");
            log("----> REACH FINAL HISTORY: onMessageLoaded");

            log("Status of the history: "+stateHistory);

            if(bufferSending.size()!=0){
                for(int i=0;i<bufferSending.size();i++){
                    bufferMessages.add(bufferSending.get(i));
                }
                bufferSending.clear();
            }

            if(bufferMessages.size()!=0){
                loadBufferMessages();

                if(lastSeenReceived==false){
                    log("onMessageLoaded: last message seen NOT received");
                    if(stateHistory!=MegaChatApi.SOURCE_NONE){
                        log("onMessageLoaded: ask more history: loadMessages: "+messages.size());
                        stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
                    }
                }
                else{
                    log("onMessageLoaded: last message seen received");
                    if(positionToScroll>0){
                        log("onMessageLoaded: Scroll to position: "+positionToScroll);
                        if(positionToScroll<messages.size()){
                            log("onMessageLoaded: message position to scroll: "+positionToScroll);
                            messages.get(positionToScroll).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            adapter.notifyItemChanged(positionToScroll+1);
                            mLayoutManager.scrollToPositionWithOffset(positionToScroll+1,Util.scaleHeightPx(20, outMetrics));
                        }
                        else{
                            log("Error, the position to scroll is more than size of messages");
                        }
                    }
                }
            }
            getMoreHistory = true;

            //Before loading pending message, mark the last one to seen
            log("Mark last message as seen");
            if(!messages.isEmpty()){
                AndroidMegaChatMessage lastSeen = messages.get(messages.size()-1);
                if(lastSeen!=null){
                    if(!lastSeen.isUploading()){
                        if(lastSeen.getMessage()!=null){
                            boolean markAsRead = megaChatApi.setMessageSeen(idChat, lastSeen.getMessage().getMsgId());
                            log("LOAD NULL - Result of markAsRead: "+markAsRead);
                        }
                    }
                }
            }

            //Load pending messages
            if(!pendingMessagesLoaded){
                pendingMessagesLoaded = true;
                loadPendingMessages();
                if(positionToScroll<=0){
                    log("positionToScroll is 0 - no unread messages");
                    mLayoutManager.scrollToPosition(messages.size());
                }
            }

            if(messages.size()<NUMBER_MESSAGES_BEFORE_LOAD){
                log("Less than 8 messages in UI: "+messages.size()+ " stateHistory is: "+stateHistory);
                if((stateHistory!=MegaChatApi.SOURCE_NONE)&&(stateHistory!=MegaChatApi.SOURCE_ERROR)){
                    log("But more history exists --> loadMessages");
                    stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
                    log("New state of history: "+stateHistory);
                    getMoreHistory = false;
                }
            }

        }
        log("END onMessageLoaded------------------------------------------ messages.size="+messages.size());
    }

    @Override
    public void onMessageReceived(MegaChatApiJava api, MegaChatMessage msg) {
        log("onMessageReceived!");
        log("------------------------------------------"+api.getChatConnectionState(idChat));
        log("STATUS: "+msg.getStatus());
        log("TEMP ID: "+msg.getTempId());
        log("FINAL ID: "+msg.getMsgId());
        log("TIMESTAMP: "+msg.getTimestamp());
        log("TYPE: "+msg.getType());

        if(msg.getType()==MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT) {
            log("TYPE_REVOKE_NODE_ATTACHMENT MESSAGE!!!!");
            return;
        }

        if(msg.getStatus()==MegaChatMessage.STATUS_SERVER_REJECTED){
            log("onMessageReceived: STATUS_SERVER_REJECTED----- "+msg.getStatus());
        }

        if(setAsRead){
            if(activityVisible){
                log("Mark message as seen");
                megaChatApi.setMessageSeen(idChat, msg.getMsgId());
            }
        }

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

        if(mLayoutManager.findLastCompletelyVisibleItemPosition()==messages.size()-1){
            log("Do scroll to end");
            mLayoutManager.scrollToPosition(messages.size());
        }
        else{
            if((softKeyboardShown || emojiKeyboardShown)&&(messages.size()==1)){
                mLayoutManager.scrollToPosition(messages.size());
            }
            log("DONT scroll to end");
        }

//        mLayoutManager.setStackFromEnd(true);
//        mLayoutManager.scrollToPosition(0);
        log("------------------------------------------");
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

        if(msg.hasChanged(MegaChatMessage.CHANGE_TYPE_ACCESS)){
            log("Change access of the message");

            MegaNodeList nodeList = msg.getMegaNodeList();
            int revokedCount = 0;

            for(int i=0; i<nodeList.size(); i++){
                MegaNode node = nodeList.get(i);
                boolean revoked = megaChatApi.isRevoked(idChat, node.getHandle());
                if(revoked){
                    log("The node is revoked: "+node.getName());
                    revokedCount++;
                }
                else{
                    log("Node not revoked: "+node.getName());
                }
            }

            if(revokedCount==nodeList.size()){
                log("All the attachments have been revoked");
                deleteMessage(msg);
            }
            else{
                log("One attachment revoked, modify message");
                resultModify = modifyMessageReceived(androidMsg, false);

                if(resultModify==-1) {
                    log("Modify result is -1");
                    int firstIndexShown = messages.get(0).getMessage().getMsgIndex();
                    log("The first index is: "+firstIndexShown+ " the index of the updated message: "+msg.getMsgIndex());
                    if(firstIndexShown<=msg.getMsgIndex()){
                        log("The message should be in the list");
                        if(msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){

                            log("A - Node attachment message not in list -> append");
                            AndroidMegaChatMessage msgToAppend = new AndroidMegaChatMessage(msg);
                            reinsertNodeAttachmentNoRevoked(msgToAppend);
                        }
                    }
                    else{
                        if(messages.size()<NUMBER_MESSAGES_BEFORE_LOAD){
                            log("Show more message - add to the list");
                            if(msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){

                                log("B - Node attachment message not in list -> append");
                                AndroidMegaChatMessage msgToAppend = new AndroidMegaChatMessage(msg);
                                reinsertNodeAttachmentNoRevoked(msgToAppend);
                            }
                        }
                    }

                }
            }
        }
        else if(msg.hasChanged(MegaChatMessage.CHANGE_TYPE_CONTENT)){
            log("Change content of the message");

            if(msg.getType()==MegaChatMessage.TYPE_TRUNCATE){
                log("TRUNCATE MESSAGE");
                clearHistory(androidMsg);
            }
            else{
                if(msg.isDeleted()){
                    log("Message deleted!!");
                }
                resultModify = modifyMessageReceived(androidMsg, false);
                log("onMessageUpdate: resultModify: "+resultModify);
            }
        }
        else{
            log("Status change");
            log("Temporal id: "+msg.getTempId());
            log("Final id: "+msg.getMsgId());

            if(msg.getUserHandle()==megaChatApi.getMyUserHandle()){
                if(msg.getType()==MegaChatMessage.TYPE_NODE_ATTACHMENT){
                    log("Modify my message and node attachment");

                    long idMsg =  dbH.findPendingMessageByIdTempKarere(msg.getTempId());
                    log("----The id of my pending message is: "+idMsg);
                    if(idMsg!=-1){
                        resultModify = modifyAttachmentReceived(androidMsg, idMsg);
                        dbH.removePendingMessageById(idMsg);
                        if(resultModify==-1){
                            log("Node attachment message not in list -> resultModify -1");
//                            AndroidMegaChatMessage msgToAppend = new AndroidMegaChatMessage(msg);
//                            appendMessagePosition(msgToAppend);
                        }
                        return;
                    }
                }
            }

            if(msg.getStatus()==MegaChatMessage.STATUS_SEEN){
                log("STATUS_SEEN");
            }
            if(msg.getStatus()==MegaChatMessage.STATUS_SERVER_RECEIVED){
                log("STATUS_SERVER_RECEIVED");
                resultModify = modifyMessageReceived(androidMsg, true);
                log("onMessageUpdate: resultModify: "+resultModify);
            }
            else{
                log("-----------Status : "+msg.getStatus());
                log("-----------Timestamp: "+msg.getTimestamp());

                if(msg.getStatus()==MegaChatMessage.STATUS_SERVER_REJECTED){
                    log("onMessageLoaded: STATUS_SERVER_REJECTED----- "+msg.getStatus());
                    //Buscar en la de sending por temporal id.

                }

                resultModify = modifyMessageReceived(androidMsg, false);
                log("onMessageUpdate: resultModify: "+resultModify);
            }
        }
    }

    @Override
    public void onHistoryReloaded(MegaChatApiJava api, MegaChatRoom chat) {
        log("onHistoryReloaded");
        bufferMessages.clear();
        messages.clear();

        invalidateOptionsMenu();
        log("Load new history");

        long unread = chatRoom.getUnreadCount();
        //                        stateHistory = megaChatApi.loadMessages(idChat, NUMBER_MESSAGES_TO_LOAD);
        if (unread == 0) {
            lastMessageSeen = null;
            lastSeenReceived = true;
            log("onHistoryReloaded: loadMessages unread is 0");
        } else {
            lastMessageSeen = megaChatApi.getLastMessageSeen(idChat);
            if (lastMessageSeen != null) {
                log("onHistoryReloaded: Id of last message seen: " + lastMessageSeen.getMsgId());
            } else {
                log("onHistoryReloaded: Error the last message seen shouldn't be NULL");
            }

            lastSeenReceived = false;
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

            if(!messageToCheck.isUploading()){
                if (messageToCheck.getMessage().getMsgId() == msg.getMsgId()) {
                    indexToChange = itr.nextIndex();
                    break;
                }
            }
        }

        if(indexToChange!=-1) {
            messages.remove(indexToChange);
            log("Removed index: " + indexToChange);
            log("deleteMessage: messages size: " + messages.size());
//                adapter.notifyDataSetChanged();
            adapter.removeMessage(indexToChange+1, messages);

            if(!messages.isEmpty()){
                //Update infoToShow of the next message also
                if (indexToChange == 0) {
                    messages.get(indexToChange).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                }
                else{
                    //Not first element
                    if(indexToChange==messages.size()){
                        log("The last message removed, do not check more messages");
                        setShowAvatar(indexToChange-1);
                        adapter.modifyMessage(messages, indexToChange);
                        return;
                    }

                    adjustInfoToShow(indexToChange);
                    setShowAvatar(indexToChange);
                    setShowAvatar(indexToChange-1);

                    adapter.modifyMessage(messages, indexToChange+1);
                }
            }
        }
        else{
            log("index to change not found");
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
            adapter.removeMessage(indexToChange+1, messages);
            int scrollToP = appendMessagePosition(msg);
            if(scrollToP!=-1){
                if(msg.getMessage().getStatus()==MegaChatMessage.STATUS_SERVER_RECEIVED){
                    log("modifyAttachmentReceived: need to scroll to position: "+indexToChange);
                    final int indexToScroll = scrollToP+1;
                    mLayoutManager.scrollToPositionWithOffset(indexToScroll,Util.scaleHeightPx(20, outMetrics));

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

                if(checkTempId){
                    log("Check temporal IDS----");
                    if (messageToCheck.getMessage().getTempId() == msg.getMessage().getTempId()) {
                        log("modifyMessageReceived with idTemp");
                        indexToChange = itr.nextIndex();
                        break;
                    }
                }
                else{
                    if (messageToCheck.getMessage().getMsgId() == msg.getMessage().getMsgId()) {
                        log("modifyMessageReceived");
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
                    messages.get(indexToChange).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                    messages.get(indexToChange).setShowAvatar(true);
                }
                else{
                    //Not first element
                    adjustInfoToShow(indexToChange);
                    setShowAvatar(indexToChange);

                    //Create adapter
                    if (adapter == null) {
                        adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
                        adapter.setHasStableIds(true);
                        listView.setAdapter(adapter);
                    } else {
                        adapter.modifyMessage(messages, indexToChange+1);
                    }
                }
            }
            else{
                log("modifyMessageReceived: INDEX change, need to reorder");
                messages.remove(indexToChange);
                log("Removed index: "+indexToChange);
                log("modifyMessageReceived: messages size: "+messages.size());
                adapter.removeMessage(indexToChange+1, messages);
                int scrollToP = appendMessagePosition(msg);
                if(scrollToP!=-1){
                    if(msg.getMessage().getStatus()==MegaChatMessage.STATUS_SERVER_RECEIVED){
                        log("modifyMessageReceived: need to scroll to position: "+indexToChange);
                        mLayoutManager.scrollToPosition(scrollToP+1);
                        //mLayoutManager.scrollToPositionWithOffset(scrollToP, Util.scaleHeightPx(20, outMetrics));
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

    public void loadBufferMessages(){
        log("loadBufferMessages");
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

            log("addMessage: "+messages.size());
        }

        log("AFTER updateMessagesLoaded: "+messages.size()+" messages in list");

        bufferMessages.clear();
    }

    public void clearHistory(AndroidMegaChatMessage androidMsg){
        log("clearHistory");

        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        int indexToChange=-1;
        // Iterate in reverse.
        while(itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();

            if(!messageToCheck.isUploading()){
                if(messageToCheck.getMessage().getStatus()!=MegaChatMessage.STATUS_SENDING){

                    indexToChange = itr.nextIndex();
                    log("Found index of last sent and confirmed message: "+indexToChange);
                    break;
                }
            }
        }

//        indexToChange = 2;
        if(indexToChange != messages.size()-1){
            log("Clear history of confirmed messages: "+indexToChange);

            List<AndroidMegaChatMessage> messagesCopy = new ArrayList<>(messages);
            messages.clear();
            messages.add(androidMsg);
            for(int i = indexToChange+1; i<messagesCopy.size();i++){
                messages.add(messagesCopy.get(i));
            }
        }
        else{
            log("Clear all messages");
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
        adapter.notifyDataSetChanged();
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
        log("appendMessageAnotherMS");
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
            log("Create adapter");
            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
            adapter.setHasStableIds(true);
            listView.setLayoutManager(mLayoutManager);
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
        }
        else{
            log("Update adapter with last index: "+lastIndex);
            if(lastIndex==0){
                log("Arrives the first message of the chat");
                adapter.setMessages(messages);
            }
            else{
                adapter.addMessage(messages, lastIndex+1);
            }
        }
    }

    public int reinsertNodeAttachmentNoRevoked(AndroidMegaChatMessage msg){
        log("reinsertNodeAttachmentNoRevoked");
        int lastIndex = messages.size()-1;
        log("1lastIndex: "+lastIndex);
        if(messages.size()==-1){
            log("2lastIndex: "+lastIndex);
            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
            messages.add(msg);
        }
        else {
            log("Finding where to append the message");
            while(messages.get(lastIndex).getMessage().getMsgIndex()>msg.getMessage().getMsgIndex()){
                log("3lastIndex: "+lastIndex);
                lastIndex--;
                if (lastIndex == -1) {
                    break;
                }
            }
            log("4lastIndex: "+lastIndex);
            lastIndex++;
            log("Append in position: "+lastIndex);
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
            log("Create adapter");
            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
            adapter.setHasStableIds(true);
            listView.setLayoutManager(mLayoutManager);
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
        }
        else{
            log("Update adapter with last index: "+lastIndex);
            if(lastIndex<0){
                log("Arrives the first message of the chat");
                adapter.setMessages(messages);
            }
            else{
                adapter.addMessage(messages, lastIndex+1);
            }
        }
        return lastIndex;
    }

    public int appendMessagePosition(AndroidMegaChatMessage msg){
        log("appendMessagePosition: "+messages.size()+" messages");

        int lastIndex = messages.size()-1;
        if(messages.size()==0){
            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
            msg.setShowAvatar(true);
            messages.add(msg);
        }
        else{
            log("Finding where to append the message");

            if(msg.isUploading()){
                lastIndex++;
                log("The message is uploading add to index: "+lastIndex);
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
            }

            messages.add(lastIndex, msg);
            adjustInfoToShow(lastIndex);
            msg.setShowAvatar(true);
            setShowAvatar(lastIndex-1);
        }

        //Create adapter
        if(adapter==null){
            log("Create adapter");
            adapter = new MegaChatLollipopAdapter(this, chatRoom, messages, listView);
            adapter.setHasStableIds(true);
            listView.setLayoutManager(mLayoutManager);
            listView.setAdapter(adapter);
            adapter.setMessages(messages);
        }
        else{
            log("Update adapter with last index: "+lastIndex);
            if(lastIndex<0){
                log("Arrives the first message of the chat");
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
        log("adjustInfoToShow");

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

                log("The previous message is uploading");
                if(msg.isUploading()){
                    log("The message is also uploading");
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
                log("The previous message is NOT uploading");

                if (userHandleToCompare == myUserHandle) {
                    log("MY message!!");
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

                    } else {
                        //The last message is mine, the previous not
                        log("Last message is mine, NOT previous");
                        if(msg.isUploading()) {
                            log("The msg to append is uploading");
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
                                    log("Add with show nothing - same userHandle");
                                    if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_CHAT_TITLE)) {
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
                        log("Different user handle");
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
                                        if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_CHAT_TITLE)) {
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
        log("setShowAvatar");

        AndroidMegaChatMessage msg = messages.get(index);

        long userHandleToCompare = -1;
        long previousUserHandleToCompare = -1;

        if(msg.isUploading()){
            msg.setShowAvatar(false);
            return;
        }

        if (userHandleToCompare == myUserHandle) {
            log("MY message!!");
        }
        else{
            if ((msg.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (msg.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                userHandleToCompare = msg.getMessage().getHandleOfAction();
            } else {
                userHandleToCompare = msg.getMessage().getUserHandle();
            }

            log("userHandleTocompare: "+userHandleToCompare);
            AndroidMegaChatMessage previousMessage = null;
            if(messages.size()-1>index){
                previousMessage = messages.get(index + 1);

                if(previousMessage==null){
                    msg.setShowAvatar(true);
                    log("2 - Previous message is null");
                    return;
                }

                if(previousMessage.isUploading()){
                    msg.setShowAvatar(true);
                    log("Previous is uploading");
                    return;
                }

                if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                    previousUserHandleToCompare = previousMessage.getMessage().getHandleOfAction();
                } else {
                    previousUserHandleToCompare = previousMessage.getMessage().getUserHandle();
                }

                log("previousUserHandleToCompare: "+previousUserHandleToCompare);

                String content = msg.getMessage().getContent();
                if(content!=null){
                    log("Content is: "+content);
                }

//                if(previousMessage.getInfoToShow()!=AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING){
//                    msg.setShowAvatar(true);
//                }
//                else{
                    if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_CHAT_TITLE)) {
                        msg.setShowAvatar(true);
                        log("Set: "+true);
                    } else {
                        if (previousUserHandleToCompare == userHandleToCompare) {

                            msg.setShowAvatar(false);
                            log("Set: "+false);
                        }
                        else{
                            msg.setShowAvatar(true);
                            log("Set: "+true);
                        }
                    }
//                }
            }
            else{
                log("No previous message");
                msg.setShowAvatar(true);
            }
        }
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
        if(message!=null){
            this.selectedMessageId = message.getPendingMessage().getId();

            PendingMessageBottomSheetDialogFragment pendingMsgSheetDialogFragment = new PendingMessageBottomSheetDialogFragment();
            pendingMsgSheetDialogFragment.show(getSupportFragmentManager(), pendingMsgSheetDialogFragment.getTag());
        }
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

    public void removePendingMsg(long id){
        log("removePendingMsg: "+selectedMessageId);
        dbH.removePendingMessageById(id);
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
        else if(request.getType() == MegaChatRequest.TYPE_START_CHAT_CALL){
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                log("TYPE_START_CHAT_CALL finished with success");
                //getFlag - Returns true if it is a video-audio call or false for audio call
                Intent i = new Intent(this, ChatCallActivity.class);
                i.putExtra("chatHandle", chatRoom.getChatId());
                startActivity(i);
            }
            else{
                log("EEEERRRRROR WHEN TYPE_START_CHAT_CALL " + e.getErrorString());
                showSnackbar(getString(R.string.call_error));
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
                sendMessageToUI(request.getMegaChatMessage());
            }
            else{
                log("File NOT sent: "+e.getErrorCode()+"___"+e.getErrorString());
                showSnackbar(getString(R.string.error_attaching_node_from_cloud));
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_REVOKE_NODE_MESSAGE){
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                log("Node revoked correctly, msg id: "+request.getMegaChatMessage().getMsgId());
            }
            else{
                log("NOT revoked correctly");
                showSnackbar(getString(R.string.error_revoking_node));
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM){
            log("Create chat request finish!!!");
            if(e.getErrorCode()==MegaChatError.ERROR_OK){

                log("open new chat");
                Intent intent = new Intent(this, ChatActivityLollipop.class);
                intent.setAction(Constants.ACTION_NEW_CHAT);
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
        log("removeChatListener");
        megaChatApi.removeChatListener(this);

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
                else if(intent.getAction().equals(Constants.ACTION_UPDATE_ATTACHMENT)){
                    log("Intent to update an attachment with error");

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
                                    log("Found index to change: "+indexToChange);
                                    break;
                                }
                            }
                        }

                        if(indexToChange!=-1){
                            log("Index modified: "+indexToChange);
                            messages.get(indexToChange).getPendingMessage().setState(PendingMessage.STATE_ERROR);
                            adapter.modifyMessage(messages, indexToChange+1);
                        }
                        else{
                            log("Error, id pending message message not found!!");
                        }
                    }
                    else{
                        log("Error. The idPendMsg is -1");
                    }
                    return;
                }
                else if(intent.getAction().equals(Constants.ACTION_NEW_CHAT)){
                    log("Intent to open new chat");
                    finish();
                    long chatIdIntent = intent.getLongExtra("CHAT_ID", -1);
                    if(chatIdIntent!=-1){
                        Intent intentOpenChat = new Intent(this, ChatActivityLollipop.class);
                        intentOpenChat.setAction(Constants.ACTION_CHAT_SHOW_MESSAGES);
                        intentOpenChat.putExtra("CHAT_ID", chatIdIntent);
                        this.startActivity(intentOpenChat);
                    }
                    else{
                        log("Error the chat Id is not valid: "+chatIdIntent);
                    }

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
                    intent.setAction(Constants.ACTION_OVERQUOTA_STORAGE);
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
        outState.putLong("idChat", idChat);
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

        if (infos == null) {
            Snackbar.make(fragmentContainer, getString(R.string.upload_can_not_open), Snackbar.LENGTH_LONG).show();
        }
        else {
            log("Launch chat upload with files "+infos.size());
            for (ShareInfo info : infos) {
                Intent intent = new Intent(this, ChatUploadService.class);

                long timestamp = System.currentTimeMillis()/1000;
                long idPendingMsg = dbH.setPendingMessage(idChat+"", Long.toString(timestamp));
                if(idPendingMsg!=-1){
                    intent.putExtra(ChatUploadService.EXTRA_ID_PEND_MSG, idPendingMsg);

                    log("name of the file: "+info.getTitle());
                    log("size of the file: "+info.getSize());
                    PendingNodeAttachment nodeAttachment = null;

                    if (MimeTypeList.typeForName(info.getFileAbsolutePath()).isImage()) {

                        if(sendOriginalAttachments){
                            String fingerprint = megaApi.getFingerprint(info.getFileAbsolutePath());

                            //Add node to db
                            long idNode = dbH.setNodeAttachment(info.getFileAbsolutePath(), info.getTitle(), fingerprint);

                            dbH.setMsgNode(idPendingMsg, idNode);

                            nodeAttachment = new PendingNodeAttachment(info.getFileAbsolutePath(), fingerprint, info.getTitle());

                        }
                        else{
                            File previewDir = PreviewUtils.getPreviewFolder(this);
                            String nameFilePreview = idChat+"_"+info.getTitle();
                            File preview = new File(previewDir, nameFilePreview);

                            boolean isPreview = megaApi.createPreview(info.getFileAbsolutePath(), preview.getAbsolutePath());

                            if(isPreview){
                                log("Preview: "+preview.getAbsolutePath());
                                String fingerprint = megaApi.getFingerprint(preview.getAbsolutePath());

                                //Add node to db
                                long idNode = dbH.setNodeAttachment(preview.getAbsolutePath(), info.getTitle(), fingerprint);

                                dbH.setMsgNode(idPendingMsg, idNode);

                                nodeAttachment = new PendingNodeAttachment(preview.getAbsolutePath(), fingerprint, info.getTitle());
                            }
                            else{
                                log("No preview");
                                String fingerprint = megaApi.getFingerprint(info.getFileAbsolutePath());

                                //Add node to db
                                long idNode = dbH.setNodeAttachment(info.getFileAbsolutePath(), info.getTitle(), fingerprint);

                                dbH.setMsgNode(idPendingMsg, idNode);

                                nodeAttachment = new PendingNodeAttachment(info.getFileAbsolutePath(), fingerprint, info.getTitle());
                            }
                        }
                    }
                    else{
                        String fingerprint = megaApi.getFingerprint(info.getFileAbsolutePath());

                        //Add node to db
                        long idNode = dbH.setNodeAttachment(info.getFileAbsolutePath(), info.getTitle(), fingerprint);

                        dbH.setMsgNode(idPendingMsg, idNode);

                        nodeAttachment = new PendingNodeAttachment(info.getFileAbsolutePath(), fingerprint, info.getTitle());
                    }

                    ArrayList<PendingNodeAttachment> nodeAttachments = new ArrayList<>();
                    nodeAttachments.add(nodeAttachment);
                    PendingMessage newPendingMsg = new PendingMessage(idPendingMsg, idChat, nodeAttachments, timestamp, PendingMessage.STATE_SENDING);
                    AndroidMegaChatMessage newNodeAttachmentMsg = new AndroidMegaChatMessage(newPendingMsg, true);
                    sendMessageUploading(newNodeAttachmentMsg);

//                ArrayList<String> filePaths = newPendingMsg.getFilePaths();
//                filePaths.add("/home/jfjf.jpg");

                    intent.putStringArrayListExtra(ChatUploadService.EXTRA_FILEPATHS, newPendingMsg.getFilePaths());
                    intent.putExtra(ChatUploadService.EXTRA_CHAT_ID, idChat);

                    startService(intent);
                }
                else{
                    log("Error when adding pending msg to the database");
                }

                if (statusDialog != null) {
                    try {
                        statusDialog.dismiss();
                    }
                    catch(Exception ex){}
                }
            }
        }
    }

    public void forwardMessages(ArrayList<AndroidMegaChatMessage> messagesSelected){

        long[] idMessages = new long[messagesSelected.size()];
        for(int i=0; i<messagesSelected.size();i++){
            idMessages[i] = messagesSelected.get(i).getMessage().getMsgId();
        }

        Intent i = new Intent(this, ChatExplorerActivity.class);
        i.putExtra("ID_MESSAGES", idMessages);
        i.putExtra("ID_CHAT_FROM", idChat);
        i.setAction(Constants.ACTION_FORWARD_MESSAGES);
        startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
    }

   @Override
    protected void onResume(){
        super.onResume();

        MegaApplication.setShowPinScreen(true);

        activityVisible = true;
        setLastMessageSeen();

        if (emojiKeyboardShown){
            keyboardButton.setImageResource(R.drawable.ic_emoticon_white);
            removeEmojiconFragment();
        }
    }

    public void setLastMessageSeen(){
        log("setLastMessageSeen");

        if(messages!=null){
            if(!messages.isEmpty()){
                AndroidMegaChatMessage lastMessage = messages.get(messages.size()-1);
                if(!lastMessage.isUploading()){
                    megaChatApi.setMessageSeen(idChat, lastMessage.getMessage().getMsgId());
                }
                else{
                    int index = messages.size()-1;
                    while(lastMessage.isUploading()==true){
                        index--;
                        if(index==-1){
                            break;
                        }
                        lastMessage = messages.get(index);
                    }
                    if(lastMessage!=null){
                        if(lastMessage.getMessage()!=null){
                            megaChatApi.setMessageSeen(idChat, lastMessage.getMessage().getMsgId());
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onPause(){
        log("onPause");
        super.onPause();

        activityVisible = false;
    }


    @Override
    public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {

    }

    @Override
    public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {

    }

    @Override
    public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {

    }

    @Override
    public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userHandle, int status, boolean inProgress) {
        log("onChatOnlineStatusUpdate: " + status + "___" + inProgress);
        setChatPermissions();
    }

    @Override
    public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {
        log("onChatConnectionStateUpdate: "+newState);

        supportInvalidateOptionsMenu();

        if(idChat==chatid){
            if(newState==MegaChatApi.CHAT_CONNECTION_ONLINE){
                log("Chat is now ONLINE");
                setAsRead=true;
                setLastMessageSeen();
            }
            else{
                setAsRead=false;
            }
        }
    }

    public void takePicture(){
        log("takePicture");
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+ Util.temporalPicDIR;
        File newFolder = new File(path);
        newFolder.mkdirs();

        String file = path + "/picture.jpg";
        File newFile = new File(file);
        try {
            newFile.createNewFile();
        } catch (IOException e) {}

        Uri outputFileUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            outputFileUri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", newFile);
        }
        else{
            outputFileUri = Uri.fromFile(newFile);
        }

        isTakePicture = false;

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        cameraIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(cameraIntent, Constants.TAKE_PHOTO_CODE);
    }

    public void uploadPicture(String imagePath){
        log("uploadPicture");
        Intent intent = new Intent(this, ChatUploadService.class);
        File selfie = new File(imagePath);

        long timestamp = System.currentTimeMillis()/1000;
        long idPendingMsg = dbH.setPendingMessage(idChat+"", Long.toString(timestamp));
        if(idPendingMsg!=-1){
            intent.putExtra(ChatUploadService.EXTRA_ID_PEND_MSG, idPendingMsg);
        }
        PendingNodeAttachment nodeAttachment = null;

        if(sendOriginalAttachments){
            log("sendOriginalAttachments");

            String fingerprint = megaApi.getFingerprint(selfie.getAbsolutePath());

            //Add node to db
            long idNode = dbH.setNodeAttachment(selfie.getAbsolutePath(), selfie.getName(), fingerprint);
            dbH.setMsgNode(idPendingMsg, idNode);
            nodeAttachment = new PendingNodeAttachment(selfie.getAbsolutePath(), fingerprint, selfie.getName());

        }else{
            File previewDir = PreviewUtils.getPreviewFolder(this);
            String nameFilePreview = idChat+"_"+selfie.getName();
            File preview = new File(previewDir, nameFilePreview);

            boolean isPreview = megaApi.createPreview(selfie.getAbsolutePath(), preview.getAbsolutePath());

            if(isPreview){
                log("Preview: "+preview.getAbsolutePath());
                String fingerprint = megaApi.getFingerprint(preview.getAbsolutePath());

                //Add node to db
                long idNode = dbH.setNodeAttachment(preview.getAbsolutePath(), selfie.getName(), fingerprint);

                dbH.setMsgNode(idPendingMsg, idNode);

                nodeAttachment = new PendingNodeAttachment(preview.getAbsolutePath(), fingerprint, selfie.getName());
            }else{
                log("No preview");
                String fingerprint = megaApi.getFingerprint(selfie.getAbsolutePath());

                //Add node to db
                long idNode = dbH.setNodeAttachment(selfie.getAbsolutePath(), selfie.getName(), fingerprint);

                dbH.setMsgNode(idPendingMsg, idNode);

                nodeAttachment = new PendingNodeAttachment(selfie.getAbsolutePath(), fingerprint, selfie.getName());
            }
        }

        ArrayList<PendingNodeAttachment> nodeAttachments = new ArrayList<>();
        nodeAttachments.add(nodeAttachment);
        PendingMessage newPendingMsg = new PendingMessage(idPendingMsg, idChat, nodeAttachments, timestamp, PendingMessage.STATE_SENDING);
        AndroidMegaChatMessage newNodeAttachmentMsg = new AndroidMegaChatMessage(newPendingMsg, true);
        sendMessageUploading(newNodeAttachmentMsg);

        intent.putStringArrayListExtra(ChatUploadService.EXTRA_FILEPATHS, newPendingMsg.getFilePaths());
        intent.putExtra(ChatUploadService.EXTRA_CHAT_ID, idChat);

        startService(intent);
    }

    public void multiselectActivated(boolean flag){
        String text = textChat.getText().toString();

        if(flag){
            //multiselect on
            if(!text.isEmpty()) {
                textChat.setTextColor(getResources().getColor(R.color.transfer_progress));
            }
            if(!sendIcon.isEnabled()){
                sendIcon.setEnabled(true);
                sendIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_send_black));
            }
        }else if(!flag){

            //multiselect off
            if(sendIcon.isEnabled()) {

                textChat.setTextColor(getResources().getColor(R.color.black));
                if(!text.isEmpty()) {
                    sendIcon.setEnabled(true);
                    sendIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_send_black));
                }else{
                    sendIcon.setEnabled(false);
                    sendIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_send_trans));
                }

            }

        }
    }


}