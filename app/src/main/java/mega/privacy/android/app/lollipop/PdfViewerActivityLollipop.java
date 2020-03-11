package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.util.SizeF;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeThumbnail;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.components.EditTextCursorWatcher;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.listeners.CreateChatListener;
import mega.privacy.android.app.lollipop.managerSections.FileBrowserFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.InboxFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.IncomingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.OfflineFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.OutgoingSharesFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.RubbishBinFragmentLollipop;
import mega.privacy.android.app.lollipop.managerSections.SearchFragmentLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.lollipop.FileInfoActivityLollipop.TYPE_EXPORT_REMOVE;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.NodeTakenDownAlertHandler.showTakenDownAlert;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaNodeUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class PdfViewerActivityLollipop extends DownloadableActivity implements MegaGlobalListenerInterface, OnPageChangeListener, OnLoadCompleteListener, OnPageErrorListener, MegaRequestListenerInterface, MegaChatRequestListenerInterface, MegaTransferListenerInterface{

    int[] screenPosition;
    int mLeftDelta;
    int mTopDelta;
    float mWidthScale;
    float mHeightScale;
    int screenWidth;
    int screenHeight;

    static PdfViewerActivityLollipop pdfViewerActivityLollipop;

    MegaApplication app = null;
    MegaApiAndroid megaApi;
    MegaApiAndroid megaApiFolder;
    MegaChatApiAndroid megaChatApi;
    MegaPreferences prefs = null;

    private AlertDialog alertDialogTransferOverquota;
    public ProgressBar progressBar;

    public static boolean loading = true;
    boolean transferOverquota = false;

    PDFView pdfView;

    Toolbar tB;
    public ActionBar aB;
    private String gSession;
    UserCredentials credentials;
    private String lastEmail;
    DatabaseHandler dbH = null;
    ChatSettings chatSettings;
    boolean isUrl;
    DefaultScrollHandle defaultScrollHandle;

    Uri uri;
    String password;
    int maxIntents = 3;
    String pdfFileName;
    boolean inside = false;
    long handle = -1;
    boolean isFolderLink = false;
    private int currentPage;
    private int type;
    private boolean isOffLine = false;
    int countChat = 0;
    int errorSent = 0;
    int successSent = 0;

    public RelativeLayout uploadContainer;
    RelativeLayout pdfviewerContainer;

    ProgressDialog statusDialog;

    private MenuItem shareMenuItem;
    private MenuItem downloadMenuItem;
    private MenuItem propertiesMenuItem;
    private MenuItem chatMenuItem;
    private MenuItem getlinkMenuItem;
    private MenuItem renameMenuItem;
    private MenuItem moveMenuItem;
    private MenuItem copyMenuItem;
    private MenuItem moveToTrashMenuItem;
    private MenuItem removeMenuItem;
    private MenuItem removelinkMenuItem;
    private MenuItem importMenuItem;
    private MenuItem saveForOfflineMenuItem;
    private MenuItem chatRemoveMenuItem;

    private List<ShareInfo> filePreparedInfos;
    ArrayList<Long> handleListM = new ArrayList<Long>();

    private String downloadLocationDefaultPath = "";
    private boolean renamed = false;
    private String path;
    private String pathNavigation;

    NodeController nC;
    private android.support.v7.app.AlertDialog downloadConfirmationDialog;
    private DisplayMetrics outMetrics;

    private RelativeLayout bottomLayout;
    private TextView fileNameTextView;
    int typeExport = -1;
    private Handler handler;
    private AlertDialog renameDialog;
    String regex = "[*|\\?:\"<>\\\\\\\\/]";
    boolean moveToRubbish = false;
    ProgressDialog moveToTrashStatusDialog;
    private boolean fromShared = false;

    private TextView actualPage;
    private TextView totalPages;
    private RelativeLayout pageNumber;

    boolean toolbarVisible = true;
    boolean fromChat = false;
    boolean isDeleteDialogShow = false;
    boolean fromDownload = false;

    ChatController chatC;
    private long msgId = -1;
    private long chatId = -1;
    MegaNode nodeChat;
    MegaChatMessage msgChat;

    boolean notChangePage = false;
    MegaNode currentDocument;

    private BroadcastReceiver receiverToFinish = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                finish();
            }
        }
    };

    @Override
    public void onCreate (Bundle savedInstanceState){
        logDebug("onCreate");

        super.onCreate(savedInstanceState);

        pdfViewerActivityLollipop = this;

        LocalBroadcastManager.getInstance(this).registerReceiver(receiverToFinish, new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN));

        final Intent intent = getIntent();
        if (intent == null){
            logWarning("Intent null");
            finish();
            return;
        }
        handler = new Handler();
        if (savedInstanceState != null) {
            logDebug("saveInstanceState");
            currentPage = savedInstanceState.getInt("currentPage");
            handle = savedInstanceState.getLong("HANDLE");
            pdfFileName = savedInstanceState.getString("pdfFileName");
            uri = Uri.parse(savedInstanceState.getString("uri"));
            renamed = savedInstanceState.getBoolean("renamed");
            isDeleteDialogShow = savedInstanceState.getBoolean("isDeleteDialogShow", false);
            toolbarVisible = savedInstanceState.getBoolean("toolbarVisible", toolbarVisible);
            password = savedInstanceState.getString("password");
            maxIntents = savedInstanceState.getInt("maxIntents", 3);
        }
        else {
            currentPage = 1;
            isDeleteDialogShow = false;
            handle = intent.getLongExtra("HANDLE", -1);
            uri = intent.getData();
            logDebug("URI pdf: " + uri);
            if (uri == null){
                logError("Uri null");
                finish();
                return;
            }
        }
        fromDownload = intent.getBooleanExtra("fromDownloadService", false);
        fromShared = intent.getBooleanExtra("fromShared", false);
        inside = intent.getBooleanExtra("inside", false);
//        handle = intent.getLongExtra("HANDLE", -1);
        isFolderLink = intent.getBooleanExtra("isFolderLink", false);
        type = intent.getIntExtra("adapterType", 0);
        path = intent.getStringExtra("path");

        if (type == OFFLINE_ADAPTER){
            isOffLine = true;
            pathNavigation = intent.getStringExtra("pathNavigation");
        }
        else if (type == FILE_LINK_ADAPTER) {
            String serialize = intent.getStringExtra(EXTRA_SERIALIZE_STRING);
            if(serialize!=null) {
                currentDocument = MegaNode.unserialize(serialize);
                if (currentDocument != null) {
                    logDebug("currentDocument NOT NULL");
                }
                else {
                    logWarning("currentDocument is NULL");
                }
            }
            isOffLine = false;
            fromChat = false;
        }
        else {
            isOffLine = false;
            pathNavigation = null;
            if (type == FROM_CHAT){
                fromChat = true;
                chatC = new ChatController(this);
                msgId = intent.getLongExtra("msgId", -1);
                chatId = intent.getLongExtra("chatId", -1);
            }
            else {
                fromChat = false;
            }
        }

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        screenHeight = outMetrics.heightPixels;
        screenWidth = outMetrics.widthPixels;

        setContentView(R.layout.activity_pdfviewer);

        pdfviewerContainer = (RelativeLayout) findViewById(R.id.pdf_viewer_container);

        if (Build.VERSION.SDK_INT >= 26) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }
        if (!isOffLine && type != ZIP_ADAPTER) {
            app = (MegaApplication) getApplication();
            megaApi = app.getMegaApi();
            if (isFolderLink) {
                megaApiFolder = app.getMegaApiFolder();
            }

            megaChatApi = app.getMegaChatApi();
            if (megaChatApi != null) {
                if (msgId != -1 && chatId != -1) {
                    msgChat = megaChatApi.getMessage(chatId, msgId);
                    if (msgChat == null) {
                        msgChat = megaChatApi.getMessageFromNodeHistory(chatId, msgId);
                    }
                    if (msgChat != null) {
                        nodeChat = chatC.authorizeNodeIfPreview(msgChat.getMegaNodeList().get(0), megaChatApi.getChatRoom(chatId));
                        if (isDeleteDialogShow) {
                            showConfirmationDeleteNode(chatId, msgChat);
                        }
                    }
                } else {
                    logWarning("msgId or chatId null");
                }
            }

            logDebug("Add transfer listener");
            megaApi.addTransferListener(this);
            megaApi.addGlobalListener(this);

            if (dbH == null){
                dbH = DatabaseHandler.getDbHandler(getApplicationContext());
            }

            if (uri.toString().contains("http://")) {
                if (dbH != null && dbH.getCredentials() != null) {
                    if (megaApi.httpServerIsRunning() == 0) {
                        megaApi.httpServerStart();
                    }

                    ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                    ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                    activityManager.getMemoryInfo(mi);

                    if (mi.totalMem > BUFFER_COMP) {
                        logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                        megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                    }
                    else {
                        logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                        megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                    }
                }
                else if (isFolderLink) {
                    if (megaApiFolder.httpServerIsRunning() == 0) {
                        megaApiFolder.httpServerStart();
                    }

                    ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                    ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                    activityManager.getMemoryInfo(mi);

                    if (mi.totalMem > BUFFER_COMP) {
                        logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                        megaApiFolder.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                    }
                    else {
                        logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                        megaApiFolder.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                    }
                }
                if (savedInstanceState != null && ! isFolderLink) {
                    MegaNode node = null;
                    if (fromChat) {
                        node = nodeChat;
                    }
                    else if (type == FILE_LINK_ADAPTER) {
                        node = currentDocument;
                    }
                    else {
                        node = megaApi.getNodeByHandle(handle);
                    }
                    if (node != null){
                        uri = Uri.parse(megaApi.httpServerGetLocalLink(node));
                    }
                    else {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.error_streaming), -1);
                    }
                }
            }

            if (isFolderLink){
                logDebug("Folder link node");
                MegaNode currentDocumentAuth = megaApiFolder.authorizeNode(megaApiFolder.getNodeByHandle(handle));
                if (currentDocumentAuth == null){
                    logWarning("CurrentDocumentAuth is null");
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_streaming)+ ": node not authorized", -1);
                }
                else{
                    logDebug("CurrentDocumentAuth is not null");
                    String url;
                    if (dbH != null && dbH.getCredentials() != null) {
                        url = megaApi.httpServerGetLocalLink(currentDocumentAuth);
                    }
                    else {
                        url = megaApiFolder.httpServerGetLocalLink(currentDocumentAuth);
                    }
                    if (url != null) {
                        uri = Uri.parse(url);
                    }
                }
            }

            logDebug("Overquota delay: " + megaApi.getBandwidthOverquotaDelay());
            if(megaApi.getBandwidthOverquotaDelay()>0){
                if(alertDialogTransferOverquota==null){
                    showTransferOverquotaDialog();
                }
                else {
                    if (!(alertDialogTransferOverquota.isShowing())) {
                        showTransferOverquotaDialog();
                    }
                }
            }
        }

        tB = (Toolbar) findViewById(R.id.toolbar_pdf_viewer);
        if(tB==null){
            logWarning("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));

        bottomLayout = (RelativeLayout) findViewById(R.id.pdf_viewer_layout_bottom);
        fileNameTextView = (TextView) findViewById(R.id.pdf_viewer_file_name);
        actualPage = (TextView) findViewById(R.id.pdf_viewer_actual_page_number);
        actualPage.setText(""+currentPage);
        totalPages = (TextView) findViewById(R.id.pdf_viewer_total_page_number);
        pageNumber = (RelativeLayout) findViewById(R.id.pdf_viewer_page_number);
        progressBar = (ProgressBar) findViewById(R.id.pdf_viewer_progress_bar);

        pdfView = (PDFView) findViewById(R.id.pdfView);

        pdfView.setBackgroundColor(Color.LTGRAY);
        pdfFileName = getFileName(uri);
        defaultScrollHandle = new DefaultScrollHandle(PdfViewerActivityLollipop.this);

        loading = true;
        if (uri.toString().contains("http://")){
            isUrl = true;
            loadStreamPDF();
        }
        else {
            isUrl = false;
            loadLocalPDF();
        }

        setTitle(pdfFileName);

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            fileNameTextView.setMaxWidth(scaleWidthPx(300, outMetrics));
        }
        else{
            fileNameTextView.setMaxWidth(scaleWidthPx(300, outMetrics));
        }

        fileNameTextView.setText(pdfFileName);

        uploadContainer = (RelativeLayout) findViewById(R.id.upload_container_layout_bottom);
        if (!inside) {
            aB.setTitle(pdfFileName);
            uploadContainer.setVisibility(View.VISIBLE);
            bottomLayout.setVisibility(View.GONE);
            pageNumber.animate().translationY(-150).start();
        }
        else {
            aB.setTitle(" ");
            uploadContainer.setVisibility(View.GONE);
            bottomLayout.setVisibility(View.VISIBLE);
            pageNumber.animate().translationY(-100).start();
        }

        uploadContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logDebug("onClick uploadContainer");
                Intent intent1 = new Intent(PdfViewerActivityLollipop.this, FileExplorerActivityLollipop.class);
                intent1.setAction(Intent.ACTION_SEND);
                intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent1.setDataAndType(uri, "application/pdf");
                startActivity(intent1);
                finish();
            }
        });

        if (!toolbarVisible) {
            setToolbarVisibilityHide(0L);
        }

        if (savedInstanceState == null){
            ViewTreeObserver observer = pdfView.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {

                    pdfView.getViewTreeObserver().removeOnPreDrawListener(this);
                    int[] location = new int[2];
                    pdfView.getLocationOnScreen(location);
                    int[] getlocation = new int[2];
                    getLocationOnScreen(getlocation);
                    screenPosition = getIntent().getIntArrayExtra("screenPosition");
                    if (screenPosition != null){
                        mLeftDelta = getlocation[0] - location[0];
                        mTopDelta = getlocation[1] - location[1];

                        mWidthScale = (float) screenPosition[2] / pdfView.getWidth();
                        mHeightScale = (float) screenPosition[3] / pdfView.getHeight();
                    }
                    else {
                        mLeftDelta = (screenWidth/2) - location[0];
                        mTopDelta = (screenHeight/2) - location[1];

                        mWidthScale = (float) (screenWidth/4) / pdfView.getWidth();
                        mHeightScale = (float) (screenHeight/4) / pdfView.getHeight();
                    }
                    logDebug("mLeftDelta: " + mLeftDelta + " mTopDelta: " + mTopDelta +
                            " mWidthScale: " + mWidthScale + " mHeightScale: " + mHeightScale);
                    runEnterAnimation();

                    return true;
                }
            });
        }
    }

    public void runEnterAnimation() {
        final long duration = 400;
        if (aB != null && aB.isShowing()) {
            if(tB != null) {
                tB.animate().translationY(-220).setDuration(0)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                aB.hide();
                            }
                        }).start();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                bottomLayout.animate().translationY(220).setDuration(0).start();
                uploadContainer.animate().translationY(220).setDuration(0).start();
                pageNumber.animate().translationY(0).setDuration(0).start();
            } else {
                aB.hide();
            }
        }
        pageNumber.animate().translationY(0).start();

        pdfView.setPivotX(0);
        pdfView.setPivotY(0);
        pdfView.setScaleX(mWidthScale);
        pdfView.setScaleY(mHeightScale);
        pdfView.setTranslationX(mLeftDelta);
        pdfView.setTranslationY(mTopDelta);

        pdfView.animate().setDuration(duration).scaleX(1).scaleY(1).translationX(0).translationY(0).setInterpolator(new DecelerateInterpolator()).withEndAction(new Runnable() {
            @Override
            public void run() {
                setToolbarVisibilityShow();
            }
        });
    }

    void getLocationOnScreen(int[] location){
        if (type == RUBBISH_BIN_ADAPTER){
            RubbishBinFragmentLollipop.imageDrag.getLocationOnScreen(location);
        }
        else if (type == INBOX_ADAPTER){
            InboxFragmentLollipop.imageDrag.getLocationOnScreen(location);
        }
        else if (type == INCOMING_SHARES_ADAPTER){
            IncomingSharesFragmentLollipop.imageDrag.getLocationOnScreen(location);
        }
        else if (type == OUTGOING_SHARES_ADAPTER){
            OutgoingSharesFragmentLollipop.imageDrag.getLocationOnScreen(location);
        }
        else if (type == CONTACT_FILE_ADAPTER){
            ContactFileListFragmentLollipop.imageDrag.getLocationOnScreen(location);
        }
        else if (type == FOLDER_LINK_ADAPTER){
            FolderLinkActivityLollipop.imageDrag.getLocationOnScreen(location);
        }
        else if (type == SEARCH_ADAPTER){
            SearchFragmentLollipop.imageDrag.getLocationOnScreen(location);
        }
        else if (type == FILE_BROWSER_ADAPTER){
            FileBrowserFragmentLollipop.imageDrag.getLocationOnScreen(location);
        }
        else if (type == OFFLINE_ADAPTER){
            OfflineFragmentLollipop.imageDrag.getLocationOnScreen(location);
        }
        else if (type == ZIP_ADAPTER) {
            ZipBrowserActivityLollipop.imageDrag.getLocationOnScreen(location);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        logDebug("onNewIntent");

        if (intent == null){
            logWarning("intent null");
            finish();
            return;
        }
        pdfViewerActivityLollipop = this;

        handler = new Handler();
        if (intent.getBooleanExtra("inside", false)){
            setIntent(intent);
            if (!intent.getBooleanExtra("isUrl", true)){
                isUrl = false;
                uri = intent.getData();
                supportInvalidateOptionsMenu();
            }
        }
        else {
            type = intent.getIntExtra("adapterType", 0);
            path = intent.getStringExtra("path");
            currentPage = 1;
            inside = false;
            if (type == OFFLINE_ADAPTER){
                isOffLine = true;
                pathNavigation = intent.getStringExtra("pathNavigation");
            }
            else if (type == FILE_LINK_ADAPTER) {
                String serialize = intent.getStringExtra(EXTRA_SERIALIZE_STRING);
                if(serialize!=null) {
                    currentDocument = MegaNode.unserialize(serialize);
                    if (currentDocument != null) {
                        logDebug("currentDocument NOT NULL");
                    }
                    else {
                        logWarning("currentDocument is NULL");
                    }
                }
                isOffLine = false;
                fromChat = false;
            }
            else {
                isOffLine = false;
                pathNavigation = null;
                if (type == FROM_CHAT){
                    fromChat = true;
                    chatC = new ChatController(this);
                    msgId = intent.getLongExtra("msgId", -1);
                    chatId = intent.getLongExtra("chatId", -1);
                }
                else {
                    fromChat = false;
                }
            }
            handle = getIntent().getLongExtra("HANDLE", -1);

            uri = intent.getData();
            if (uri == null){
                logError("Uri null");
                finish();
                return;
            }
            Intent newIntent = new Intent();
            newIntent.setDataAndType(uri, "application/pdf");
            newIntent.setAction(ACTION_OPEN_FOLDER);
            setIntent(newIntent);
            Display display = getWindowManager().getDefaultDisplay();
            outMetrics = new DisplayMetrics ();
            display.getMetrics(outMetrics);
            screenHeight = outMetrics.heightPixels;
            screenWidth = outMetrics.widthPixels;

            setContentView(R.layout.activity_pdfviewer);

            if (!isOffLine && type != ZIP_ADAPTER){
                app = (MegaApplication)getApplication();
                megaApi = app.getMegaApi();

                megaChatApi = app.getMegaChatApi();
                if (megaChatApi != null) {
                    if (msgId != -1 && chatId != -1) {
                        msgChat = megaChatApi.getMessage(chatId, msgId);
                        if (msgChat == null) {
                            msgChat = megaChatApi.getMessageFromNodeHistory(chatId, msgId);
                        }
                        if (msgChat != null) {
                            nodeChat = msgChat.getMegaNodeList().get(0);
                        }
                    } else {
                        logWarning("msgId or chatId null");
                    }
                }

                logDebug("Add transfer listener");
                megaApi.addTransferListener(this);
                megaApi.addGlobalListener(this);

                logDebug("Overquota delay: " + megaApi.getBandwidthOverquotaDelay());
                if(megaApi.getBandwidthOverquotaDelay()>0){
                    if(alertDialogTransferOverquota==null){
                        showTransferOverquotaDialog();
                    }
                    else {
                        if (!(alertDialogTransferOverquota.isShowing())) {
                            showTransferOverquotaDialog();
                        }
                    }
                }
            }

            tB = (Toolbar) findViewById(R.id.toolbar_pdf_viewer);
            if(tB==null){
                logWarning("Tb is Null");
                return;
            }

            tB.setVisibility(View.VISIBLE);
            setSupportActionBar(tB);
            aB = getSupportActionBar();
            aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = this.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD){
                requestWindowFeature(Window.FEATURE_NO_TITLE);
                this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            bottomLayout = (RelativeLayout) findViewById(R.id.pdf_viewer_layout_bottom);
            fileNameTextView = (TextView) findViewById(R.id.pdf_viewer_file_name);
            actualPage = (TextView) findViewById(R.id.pdf_viewer_actual_page_number);
            actualPage.setText(""+currentPage);
            totalPages = (TextView) findViewById(R.id.pdf_viewer_total_page_number);
            pageNumber = (RelativeLayout) findViewById(R.id.pdf_viewer_page_number);
            pageNumber.animate().translationY(-150).start();
            progressBar = (ProgressBar) findViewById(R.id.pdf_viewer_progress_bar);

            pdfView = (PDFView) findViewById(R.id.pdfView);

            pdfView.setBackgroundColor(Color.LTGRAY);
            defaultScrollHandle = new DefaultScrollHandle(PdfViewerActivityLollipop.this);

            isUrl = false;
            loadLocalPDF();
            pdfFileName = getFileName(uri);

            path = uri.getPath();
            setTitle(pdfFileName);
            aB.setTitle(pdfFileName);

            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                fileNameTextView.setMaxWidth(scaleWidthPx(300, outMetrics));
            }
            else{
                fileNameTextView.setMaxWidth(scaleWidthPx(300, outMetrics));
            }
            fileNameTextView.setText(pdfFileName);

            uploadContainer = (RelativeLayout) findViewById(R.id.upload_container_layout_bottom);
            uploadContainer.setVisibility(View.VISIBLE);
            bottomLayout.setVisibility(View.GONE);
            uploadContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logDebug("onClick uploadContainer");
                    Intent intent1 = new Intent(PdfViewerActivityLollipop.this, FileExplorerActivityLollipop.class);
                    intent1.setAction(Intent.ACTION_SEND);
                    intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent1.setDataAndType(uri, "application/pdf");
                    startActivity(intent1);
                    finish();
                }
            });

            pdfviewerContainer = (RelativeLayout) findViewById(R.id.pdf_viewer_container);

            ViewTreeObserver observer = pdfView.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {

                    pdfView.getViewTreeObserver().removeOnPreDrawListener(this);
                    int[] location = new int[2];
                    pdfView.getLocationOnScreen(location);
                    int[] getlocation = new int[2];
                    getLocationOnScreen(getlocation);
                    screenPosition = getIntent().getIntArrayExtra("screenPosition");
                    if (screenPosition != null){
                        mLeftDelta = getlocation[0] - location[0];
                        mTopDelta = getlocation[1] - location[1];

                        mWidthScale = (float) screenPosition[2] / pdfView.getWidth();
                        mHeightScale = (float) screenPosition[3] / pdfView.getHeight();
                    }
                    else {
                        mLeftDelta = (screenWidth/2) - location[0];
                        mTopDelta = (screenHeight/2) - location[1];

                        mWidthScale = (float) (screenWidth/4) / pdfView.getWidth();
                        mHeightScale = (float) (screenHeight/4) / pdfView.getHeight();
                    }
                    logDebug("mLeftDelta: " + mLeftDelta + " mTopDelta: " + mTopDelta + " mWidthScale: " + mWidthScale + " mHeightScale: " + mHeightScale);
                    runEnterAnimation();

                    return true;
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        logDebug("onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putInt("currentPage", currentPage);
        outState.putLong("HANDLE", handle);
        outState.putString("pdfFileName", pdfFileName);
        outState.putString("uri", uri.toString());
        outState.putBoolean("renamed", renamed);
        outState.putBoolean("isDeleteDialogShow", isDeleteDialogShow);
        outState.putBoolean("toolbarVisible", toolbarVisible);
        outState.putString("password", password);
        outState.putInt("maxIntents", maxIntents);
    }

    @Override
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {

    }

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
        logDebug("onUserAlertsUpdate");
    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList) {
        logDebug("onNodesUpdate");
        if (megaApi.getNodeByHandle(handle) == null){
            return;
        }
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onReloadNeeded(MegaApiJava api) {

    }

    @Override
    public void onAccountUpdate(MegaApiJava api) {

    }

    @Override
    public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {

    }

    @Override
    public void onEvent(MegaApiJava api, MegaEvent event) {

    }

    class LoadPDFStream extends AsyncTask<String, Void, InputStream> {

        @Override
        protected InputStream doInBackground(String... strings) {
            InputStream inputStream = null;

            try {
                URL url = new URL(strings[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                if (httpURLConnection.getResponseCode() == 200) {
                    inputStream = new BufferedInputStream( (httpURLConnection.getInputStream()));
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            return inputStream;
        }

        @Override
        protected void onPostExecute(InputStream inputStream) {
            logDebug("onPostExecute");
            try {
                pdfView.fromStream(inputStream)
                        .defaultPage(currentPage-1)
                        .onPageChange(PdfViewerActivityLollipop.this)
                        .enableAnnotationRendering(true)
                        .onLoad(PdfViewerActivityLollipop.this)
                        .scrollHandle(defaultScrollHandle)
                        .spacing(10) // in dp
                        .onPageError(PdfViewerActivityLollipop.this)
                        .password(password)
                        .load();
            } catch (Exception e) {

            }

            if (loading && !transferOverquota){
                progressBar.setVisibility(View.VISIBLE);
            }
        }
    }

    public void reloadPDFwithPassword (String password) {
        this.password = password;
        maxIntents--;
        if (isUrl) {
            loadStreamPDF();
        }
        else {
            loadLocalPDF();
        }
    }

    public void loadStreamPDF() {
        logDebug("loading: " + loading);
        new LoadPDFStream().execute(uri.toString());
    }

    private void loadLocalPDF() {
        logDebug("loading: " + loading);

        progressBar.setVisibility(View.VISIBLE);
        try {
            pdfView.fromUri(uri)
                    .defaultPage(currentPage-1)
                    .onPageChange(this)
                    .enableAnnotationRendering(true)
                    .onLoad(this)
                    .scrollHandle(defaultScrollHandle)
                    .spacing(10) // in dp
                    .onPageError(this)
                    .password(password)
                    .load();
        } catch (Exception e) {

        }
    }

    public void download(){

        if (type == FILE_LINK_ADAPTER){
            if (nC == null) {
                nC = new NodeController(this);
            }
            nC.downloadFileLink(currentDocument, uri.toString());
        }
        else if (fromChat){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasStoragePermission) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                    handleListM.add(nodeChat.getHandle());
                    return;
                }
            }
            if (chatC == null){
                chatC = new ChatController(this);
            }
            if (nodeChat != null){
                chatC.prepareForChatDownload(nodeChat);
            }
        }
        else {
            MegaNode node = megaApi.getNodeByHandle(handle);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasStoragePermission) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);

                    handleListM.add(node.getHandle());
                    return;
                }
            }

            ArrayList<Long> handleList = new ArrayList<Long>();
            handleList.add(node.getHandle());

            if(nC==null){
                nC = new NodeController(this, isFolderLink);
            }
            nC.prepareForDownload(handleList, false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case REQUEST_WRITE_STORAGE:{
                boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (hasStoragePermission) {
                    if (type == FILE_LINK_ADAPTER) {
                        if(nC==null){
                            nC = new NodeController(this, isFolderLink);
                        }
                        nC.downloadFileLink(currentDocument, uri.toString());
                    }
                    else if (fromChat) {
                        if (chatC == null){
                            chatC = new ChatController(this);
                        }
                        if (nodeChat != null){
                            chatC.prepareForChatDownload(nodeChat);
                        }
                    }
                    else{
                        if(nC==null){
                            nC = new NodeController(this, isFolderLink);
                        }
                        nC.prepareForDownload(handleListM, false);
                    }
                }
                break;
            }
        }
    }

    public void onIntentProcessed(List<ShareInfo> infos) {

        if (statusDialog != null) {
            try {
                statusDialog.dismiss();
            }
            catch(Exception ex){}
        }

        logDebug("Intent processed!");

        if (infos == null) {
            logError("Error: infos is NULL");
            return;
        }
        else {

            MegaNode parentNode = megaApi.getRootNode();
            showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.upload_began, infos.size(), infos.size()), -1);
            for (ShareInfo info : infos) {

                Intent intent = new Intent(this, UploadService.class);
                intent.putExtra(UploadService.EXTRA_FILEPATH, info.getFileAbsolutePath());
                intent.putExtra(UploadService.EXTRA_NAME, info.getTitle());
                intent.putExtra(UploadService.EXTRA_PARENT_HASH, parentNode.getHandle());
                intent.putExtra(UploadService.EXTRA_SIZE, info.getSize());
                startService(intent);
            }
            filePreparedInfos = null;
        }
    }

    public void backToCloud(long handle){
        logDebug("Handle: "+handle);
        Intent startIntent = new Intent(this, ManagerActivityLollipop.class);
        if(handle!=-1){
            startIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startIntent.setAction(ACTION_OPEN_FOLDER);
            startIntent.putExtra("PARENT_HANDLE", handle);
        }
        startActivity(startIntent);
    }

    public  void setToolbarVisibilityShow () {
        logDebug("setToolbarVisibilityShow");
        toolbarVisible = true;
        aB.show();
        if(tB != null) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            tB.animate().translationY(0).setDuration(200L).start();
            bottomLayout.animate().translationY(0).setDuration(200L).start();
            uploadContainer.animate().translationY(0).setDuration(200L).start();
            if (inside){
                pageNumber.animate().translationY(-100).setDuration(200L).start();
            }
            else {
                pageNumber.animate().translationY(-150).setDuration(200L).start();
            }
        }
    }

    public void setToolbarVisibilityHide (long duration) {
        logDebug("Duration: " + duration);
        toolbarVisible = false;
        if(tB != null) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            tB.animate().translationY(-220).setDuration(duration).withEndAction(new Runnable() {
                @Override
                public void run() {
                    aB.hide();
                }
            }).start();
            bottomLayout.animate().translationY(220).setDuration(duration).start();
            uploadContainer.animate().translationY(220).setDuration(duration).start();
            pageNumber.animate().translationY(0).setDuration(duration).start();
        }
        else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            aB.hide();
        }
    }

    public boolean isToolbarVisible(){
        return toolbarVisible;
    }

    public void setToolbarVisibility (){

        int page = pdfView.getCurrentPage();

        if (queryIfPdfIsHorizontal(page) &&  getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && !pdfView.isZooming()) {
            notChangePage = true;
            pdfView.jumpTo(page - 1);
        }

        if (aB != null && aB.isShowing()) {
            setToolbarVisibilityHide(200L);
        } else if (aB != null && !aB.isShowing()){
            setToolbarVisibilityShow();
        }
    }

    boolean queryIfPdfIsHorizontal(int page){
        SizeF sizeF = pdfView.getPageSize(page);
        if (sizeF.getWidth() > sizeF.getHeight()){
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        logDebug("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_pdfviewer, menu);

        shareMenuItem = menu.findItem(R.id.pdf_viewer_share);
        downloadMenuItem = menu.findItem(R.id.pdf_viewer_download);
        chatMenuItem = menu.findItem(R.id.pdf_viewer_chat);
        chatMenuItem.setIcon(mutateIconSecondary(this, R.drawable.ic_send_to_contact, R.color.white));
        propertiesMenuItem = menu.findItem(R.id.pdf_viewer_properties);
        getlinkMenuItem = menu.findItem(R.id.pdf_viewer_get_link);
        renameMenuItem = menu.findItem(R.id.pdf_viewer_rename);
        moveMenuItem = menu.findItem(R.id.pdf_viewer_move);
        copyMenuItem = menu.findItem(R.id.pdf_viewer_copy);
        moveToTrashMenuItem = menu.findItem(R.id.pdf_viewer_move_to_trash);
        removeMenuItem = menu.findItem(R.id.pdf_viewer_remove);
        removelinkMenuItem = menu.findItem(R.id.pdf_viewer_remove_link);
        importMenuItem = menu.findItem(R.id.chat_pdf_viewer_import);
        saveForOfflineMenuItem = menu.findItem(R.id.chat_pdf_viewer_save_for_offline);
        saveForOfflineMenuItem.setIcon(mutateIconSecondary(this, R.drawable.ic_b_save_offline, R.color.white));
        chatRemoveMenuItem = menu.findItem(R.id.chat_pdf_viewer_remove);

        shareMenuItem.setVisible(true);

        if (!inside){
            propertiesMenuItem.setVisible(false);
            chatMenuItem.setVisible(false);
            downloadMenuItem.setVisible(false);
            getlinkMenuItem.setVisible(false);
            renameMenuItem.setVisible(false);
            moveMenuItem.setVisible(false);
            copyMenuItem.setVisible(false);
            moveToTrashMenuItem.setVisible(false);
            removeMenuItem.setVisible(false);
            removelinkMenuItem.setVisible(false);
            importMenuItem.setVisible(false);
            saveForOfflineMenuItem.setVisible(false);
            chatRemoveMenuItem.setVisible(false);
        }
        else {
            if (nC == null) {
                nC = new NodeController(this);
            }
            boolean fromIncoming = false;
            if (type == SEARCH_ADAPTER) {
                fromIncoming = nC.nodeComesFromIncoming(megaApi.getNodeByHandle(handle));
            }

            if (type == OFFLINE_ADAPTER){
                getlinkMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                propertiesMenuItem.setVisible(true);
                downloadMenuItem.setVisible(false);
                renameMenuItem.setVisible(false);
                moveMenuItem.setVisible(false);
                copyMenuItem.setVisible(false);
                moveToTrashMenuItem.setVisible(false);
                removeMenuItem.setVisible(false);
                chatMenuItem.setVisible(false);
                importMenuItem.setVisible(false);
                saveForOfflineMenuItem.setVisible(false);
                chatRemoveMenuItem.setVisible(false);
            }
            else if(type == SEARCH_ADAPTER && !fromIncoming){
                MegaNode node = megaApi.getNodeByHandle(handle);

                if(node.isExported()){
                    removelinkMenuItem.setVisible(true);
                    getlinkMenuItem.setVisible(false);
                }else{
                    removelinkMenuItem.setVisible(false);
                    getlinkMenuItem.setVisible(true);
                }

                downloadMenuItem.setVisible(true);
                propertiesMenuItem.setVisible(true);
                renameMenuItem.setVisible(true);
                moveMenuItem.setVisible(true);
                copyMenuItem.setVisible(true);
                chatMenuItem.setVisible(true);

                MegaNode parent = megaApi.getNodeByHandle(handle);
                while (megaApi.getParentNode(parent) != null){
                    parent = megaApi.getParentNode(parent);
                }

                if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
                    moveToTrashMenuItem.setVisible(true);
                    removeMenuItem.setVisible(false);
                }
                else{
                    moveToTrashMenuItem.setVisible(false);
                    removeMenuItem.setVisible(true);
                }
                importMenuItem.setVisible(false);
                saveForOfflineMenuItem.setVisible(false);
                chatRemoveMenuItem.setVisible(false);
            }
            else if (type == FROM_CHAT){
                getlinkMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                propertiesMenuItem.setVisible(false);
                renameMenuItem.setVisible(false);
                moveMenuItem.setVisible(false);
                copyMenuItem.setVisible(false);
                moveToTrashMenuItem.setVisible(false);
                removeMenuItem.setVisible(false);
                chatMenuItem.setVisible(false);

                if(megaApi==null || !isOnline(this)) {
                    downloadMenuItem.setVisible(false);
                    importMenuItem.setVisible(false);
                    saveForOfflineMenuItem.setVisible(false);

                    if (MegaApiJava.userHandleToBase64(msgChat.getUserHandle()).equals(megaChatApi.getMyUserHandle()) && msgChat.isDeletable()) {
                        chatRemoveMenuItem.setVisible(true);
                    }
                    else {
                        chatRemoveMenuItem.setVisible(false);
                    }
                }
                else if (nodeChat != null){
                    downloadMenuItem.setVisible(true);
                    if (chatC.isInAnonymousMode()) {
                        importMenuItem.setVisible(false);
                        saveForOfflineMenuItem.setVisible(false);
                    }
                    else {
                        importMenuItem.setVisible(true);
                        saveForOfflineMenuItem.setVisible(true);
                    }

                    if (msgChat.getUserHandle() == megaChatApi.getMyUserHandle() && msgChat.isDeletable()) {
                        chatRemoveMenuItem.setVisible(true);
                    }
                    else {
                        chatRemoveMenuItem.setVisible(false);
                    }
                }
                else {
                    downloadMenuItem.setVisible(false);
                    importMenuItem.setVisible(false);
                    saveForOfflineMenuItem.setVisible(false);
                    chatRemoveMenuItem.setVisible(false);
                }
            }
            else if (type == FILE_LINK_ADAPTER) {
                logDebug("FILE_LINK_ADAPTER");
                getlinkMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                propertiesMenuItem.setVisible(false);
                downloadMenuItem.setVisible(true);
                renameMenuItem.setVisible(false);
                moveMenuItem.setVisible(false);
                copyMenuItem.setVisible(false);
                moveToTrashMenuItem.setVisible(false);
                removeMenuItem.setVisible(false);
                chatMenuItem.setVisible(false);
                importMenuItem.setVisible(false);
                saveForOfflineMenuItem.setVisible(false);
                chatRemoveMenuItem.setVisible(false);
            }
            else if (type == ZIP_ADAPTER) {
                propertiesMenuItem.setVisible(false);
                chatMenuItem.setVisible(false);
                downloadMenuItem.setVisible(false);
                getlinkMenuItem.setVisible(false);
                renameMenuItem.setVisible(false);
                moveMenuItem.setVisible(false);
                copyMenuItem.setVisible(false);
                moveToTrashMenuItem.setVisible(false);
                removeMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                importMenuItem.setVisible(false);
                saveForOfflineMenuItem.setVisible(false);
                chatRemoveMenuItem.setVisible(false);
            }
            else if (type == INCOMING_SHARES_ADAPTER ||  fromIncoming) {
                propertiesMenuItem.setVisible(true);
                chatMenuItem.setVisible(true);
                copyMenuItem.setVisible(true);
                removeMenuItem.setVisible(false);
                importMenuItem.setVisible(false);
                saveForOfflineMenuItem.setVisible(false);
                chatRemoveMenuItem.setVisible(false);
                getlinkMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                downloadMenuItem.setVisible(true);

                MegaNode node = megaApi.getNodeByHandle(handle);
                int accessLevel = megaApi.getAccess(node);

                switch (accessLevel) {
                    case MegaShare.ACCESS_FULL: {
                        logDebug("Access FULL");
                        renameMenuItem.setVisible(true);
                        moveMenuItem.setVisible(true);
                        moveToTrashMenuItem.setVisible(true);

                        break;
                    }
                    case MegaShare.ACCESS_READ:
                        logDebug("Access read");
                    case MegaShare.ACCESS_READWRITE: {
                        logDebug("Access read & write");
                        renameMenuItem.setVisible(false);
                        moveMenuItem.setVisible(false);
                        moveToTrashMenuItem.setVisible(false);
                        break;
                    }
                }
            }
            else if (type == RECENTS_ADAPTER) {
                MegaNode node = megaApi.getNodeByHandle(handle);
                chatRemoveMenuItem.setVisible(false);
                removeMenuItem.setVisible(false);
                getlinkMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                importMenuItem.setVisible(false);
                saveForOfflineMenuItem.setVisible(false);

                int accessLevel = megaApi.getAccess(node);
                switch (accessLevel) {
                    case MegaShare.ACCESS_READWRITE:
                    case MegaShare.ACCESS_READ:
                    case MegaShare.ACCESS_UNKNOWN: {
                        renameMenuItem.setVisible(false);
                        moveMenuItem.setVisible(false);
                        moveToTrashMenuItem.setVisible(false);
                        break;
                    }
                    case MegaShare.ACCESS_FULL:
                    case MegaShare.ACCESS_OWNER: {
                        renameMenuItem.setVisible(true);
                        moveMenuItem.setVisible(true);
                        moveToTrashMenuItem.setVisible(true);
                        break;
                    }
                }
            }
            else {
                MegaNode node = megaApi.getNodeByHandle(handle);

                if (node == null) {
                    getlinkMenuItem.setVisible(false);
                    removelinkMenuItem.setVisible(false);
                    propertiesMenuItem.setVisible(false);
                    downloadMenuItem.setVisible(false);
                    renameMenuItem.setVisible(false);
                    moveMenuItem.setVisible(false);
                    copyMenuItem.setVisible(false);
                    moveToTrashMenuItem.setVisible(false);
                    removeMenuItem.setVisible(false);
                    chatMenuItem.setVisible(false);
                    importMenuItem.setVisible(false);
                    saveForOfflineMenuItem.setVisible(false);
                    chatRemoveMenuItem.setVisible(false);
                }
                else {
                    copyMenuItem.setVisible(true);

                    if(node.isExported()){
                        getlinkMenuItem.setVisible(false);
                        removelinkMenuItem.setVisible(true);
                    }
                    else{
                        if(type==CONTACT_FILE_ADAPTER){
                            getlinkMenuItem.setVisible(false);
                            removelinkMenuItem.setVisible(false);
                        }
                        else{
                            if(isFolderLink){
                                getlinkMenuItem.setVisible(false);
                                removelinkMenuItem.setVisible(false);

                            }
                            else{
                                getlinkMenuItem.setVisible(true);
                                removelinkMenuItem.setVisible(false);
                            }
                        }
                    }

                    if(isFolderLink){
                        propertiesMenuItem.setVisible(false);
                        moveToTrashMenuItem.setVisible(false);
                        removeMenuItem.setVisible(false);
                        renameMenuItem.setVisible(false);
                        moveMenuItem.setVisible(false);
                        copyMenuItem.setVisible(false);
                        chatMenuItem.setVisible(false);
                    }
                    else{
                        propertiesMenuItem.setVisible(true);

                        if(type==CONTACT_FILE_ADAPTER){
                            removeMenuItem.setVisible(false);
                            node = megaApi.getNodeByHandle(handle);
                            int accessLevel = megaApi.getAccess(node);
                            switch(accessLevel){
                                case MegaShare.ACCESS_OWNER:
                                case MegaShare.ACCESS_FULL:{
                                    renameMenuItem.setVisible(true);
                                    moveMenuItem.setVisible(true);
                                    moveToTrashMenuItem.setVisible(true);
                                    chatMenuItem.setVisible(true);
                                    break;
                                }
                                case MegaShare.ACCESS_READWRITE:
                                case MegaShare.ACCESS_READ:{
                                    renameMenuItem.setVisible(false);
                                    moveMenuItem.setVisible(false);
                                    moveToTrashMenuItem.setVisible(false);
                                    chatMenuItem.setVisible(false);
                                    break;
                                }
                            }
                        }
                        else{
                            chatMenuItem.setVisible(true);
                            renameMenuItem.setVisible(true);
                            moveMenuItem.setVisible(true);

                            node = megaApi.getNodeByHandle(handle);

                            final long handle = node.getHandle();
                            MegaNode parent = megaApi.getNodeByHandle(handle);

                            while (megaApi.getParentNode(parent) != null){
                                parent = megaApi.getParentNode(parent);
                            }

                            if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
                                moveToTrashMenuItem.setVisible(true);
                                removeMenuItem.setVisible(false);

                            }
                            else{
                                moveToTrashMenuItem.setVisible(false);
                                removeMenuItem.setVisible(true);
                                getlinkMenuItem.setVisible(false);
                                removelinkMenuItem.setVisible(false);
                            }
                        }
                    }

                    downloadMenuItem.setVisible(true);
                    importMenuItem.setVisible(false);
                    saveForOfflineMenuItem.setVisible(false);
                    chatRemoveMenuItem.setVisible(false);
                }
            }

            if (type != OFFLINE_ADAPTER || type != ZIP_ADAPTER) {
                MegaNode node = megaApi.getNodeByHandle(handle);
                if (node != null && megaApi.getAccess(node) != MegaShare.ACCESS_OWNER) {
                    shareMenuItem.setVisible(false);
                }
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        logDebug("onOptionsItemSelected");

        int id = item.getItemId();
        switch(id) {
            case android.R.id.home: {
                super.onBackPressed();
                break;
            }
            case R.id.pdf_viewer_share: {
                if (type == OFFLINE_ADAPTER || type == ZIP_ADAPTER) {
                    shareFile(this, new File(uri.toString()));
                } else {
                    shareNode(this, megaApi.getNodeByHandle(handle));
                }
                break;
            }
            case R.id.pdf_viewer_download: {
                download();
                break;
            }
            case R.id.pdf_viewer_chat: {

                if(nC ==null){
                    nC = new NodeController(this, isFolderLink);
                }

                MegaNode attachNode = megaApi.getNodeByHandle(handle);
                if (attachNode != null) {
                    nC.checkIfNodeIsMineAndSelectChatsToSendNode(attachNode);
                }
                break;
            }
            case R.id.pdf_viewer_properties: {
                showPropertiesActivity();
                break;
            }
            case R.id.pdf_viewer_get_link: {
                if (showTakenDownNodeActionNotAvailableDialog(megaApi.getNodeByHandle(handle), this)) {
                    break;
                }

                showGetLinkActivity();
                break;
            }
            case R.id.pdf_viewer_remove_link: {
                if (showTakenDownNodeActionNotAvailableDialog(megaApi.getNodeByHandle(handle), this)) {
                    break;
                }

                showRemoveLink();
                break;
            }
            case R.id.pdf_viewer_rename: {
                showRenameDialog();
                break;
            }
            case R.id.pdf_viewer_move: {
                showMove();
                break;
            }
            case R.id.pdf_viewer_copy: {
                showCopy();
                break;
            }
            case R.id.pdf_viewer_move_to_trash: {
                moveToTrash();
                break;
            }
            case R.id.pdf_viewer_remove: {
                moveToTrash();
                break;
            }
            case R.id.chat_pdf_viewer_import:{
                if (nodeChat != null){
                    importNode();
                }
                break;
            }
            case R.id.chat_pdf_viewer_save_for_offline:{
                if (chatC == null){
                    chatC = new ChatController(this);
                }
                if (msgChat != null){
                    chatC.saveForOffline(msgChat.getMegaNodeList(), megaChatApi.getChatRoom(chatId));
                }
                break;
            }
            case R.id.chat_pdf_viewer_remove:{
                if (msgChat != null && chatId != -1){
                    showConfirmationDeleteNode(chatId, msgChat);
                }
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void importNode(){
        logDebug("importNode");

        Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_IMPORT_FOLDER);
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
    }

    public void showConfirmationDeleteNode(final long chatId, final MegaChatMessage message){
        logDebug("showConfirmationDeleteNode");
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        if (chatC == null){
                            chatC = new ChatController(pdfViewerActivityLollipop);
                        }
                        chatC.deleteMessage(message, chatId);
                        isDeleteDialogShow = false;
                        finish();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        isDeleteDialogShow = false;
                        break;
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        }
        else{
            builder = new android.support.v7.app.AlertDialog.Builder(this);
        }
        builder.setMessage(R.string.confirmation_delete_one_attachment);
        builder.setPositiveButton(R.string.context_remove, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();

        isDeleteDialogShow = true;

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isDeleteDialogShow = false;
            }
        });
    }

    public void moveToTrash(){
        logDebug("moveToTrash");

        moveToRubbish = false;
        if (!isOnline(this)){
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
            return;
        }

        if(isFinishing()){
            return;
        }

        final MegaNode rubbishNode = megaApi.getRubbishNode();

        MegaNode parent = megaApi.getNodeByHandle(handle);
        while (megaApi.getParentNode(parent) != null){
            parent = megaApi.getParentNode(parent);
        }

        if (parent.getHandle() != megaApi.getRubbishNode().getHandle()){
            moveToRubbish = true;
        }
        else{
            moveToRubbish = false;
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Check if the node is not yet in the rubbish bin (if so, remove it)

                        if (moveToRubbish){
                            megaApi.moveNode(megaApi.getNodeByHandle(handle), rubbishNode, PdfViewerActivityLollipop.this);
                            ProgressDialog temp = null;
                            try{
                                temp = new ProgressDialog(PdfViewerActivityLollipop.this);
                                temp.setMessage(getString(R.string.context_move_to_trash));
                                temp.show();
                            }
                            catch(Exception e){
                                return;
                            }
                            moveToTrashStatusDialog = temp;
                        }
                        else{
                            megaApi.remove(megaApi.getNodeByHandle(handle), PdfViewerActivityLollipop.this);
                            ProgressDialog temp = null;
                            try{
                                temp = new ProgressDialog(PdfViewerActivityLollipop.this);
                                temp.setMessage(getString(R.string.context_delete_from_mega));
                                temp.show();
                            }
                            catch(Exception e){
                                return;
                            }
                            moveToTrashStatusDialog = temp;
                        }


                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        if (moveToRubbish){
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
            String message= getResources().getString(R.string.confirmation_move_to_rubbish);
            builder.setMessage(message).setPositiveButton(R.string.general_move, dialogClickListener)
                    .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
        }
        else{
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
            String message= getResources().getString(R.string.confirmation_delete_from_mega);
            builder.setMessage(message).setPositiveButton(R.string.general_remove, dialogClickListener)
                    .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
        }
    }


    public void showCopy(){
        logDebug("showCopy");

        ArrayList<Long> handleList = new ArrayList<Long>();
        handleList.add(handle);

        Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_COPY_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("COPY_FROM", longArray);
        startActivityForResult(intent, REQUEST_CODE_SELECT_COPY_FOLDER);
    }

    public void showMove(){
        logDebug("showMove");

        ArrayList<Long> handleList = new ArrayList<Long>();
        handleList.add(handle);

        Intent intent = new Intent(this, FileExplorerActivityLollipop.class);
        intent.setAction(FileExplorerActivityLollipop.ACTION_PICK_MOVE_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("MOVE_FROM", longArray);
        startActivityForResult(intent, REQUEST_CODE_SELECT_MOVE_FOLDER);
    }

    private void showKeyboardDelayed(final View view) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 50);
    }

    public void showRenameDialog() {
        logDebug("showRenameDialog");
        final MegaNode node = megaApi.getNodeByHandle(handle);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(17, outMetrics), 0);
        //	    layout.setLayoutParams(params);

        final EditTextCursorWatcher input = new EditTextCursorWatcher(this, node.isFolder());
        input.setSingleLine();
        input.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);

        input.setImeActionLabel(getString(R.string.context_rename), EditorInfo.IME_ACTION_DONE);
        input.setText(node.getName());


        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, boolean hasFocus) {
                if (hasFocus) {
                    if (node.isFolder()) {
                        input.setSelection(0, input.getText().length());
                    } else {
                        String[] s = node.getName().split("\\.");
                        if (s != null) {
                            int numParts = s.length;
                            int lastSelectedPos = 0;
                            if (numParts == 1) {
                                input.setSelection(0, input.getText().length());
                            } else if (numParts > 1) {
                                for (int i = 0; i < (numParts - 1); i++) {
                                    lastSelectedPos += s[i].length();
                                    lastSelectedPos++;
                                }
                                lastSelectedPos--; //The last point should not be selected)
                                input.setSelection(0, lastSelectedPos);
                            }
                        }
                        showKeyboardDelayed(v);
                    }
                }
            }
        });


        layout.addView(input, params);

        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params1.setMargins(scaleWidthPx(20, outMetrics), 0, scaleWidthPx(17, outMetrics), 0);

        final RelativeLayout error_layout = new RelativeLayout(PdfViewerActivityLollipop.this);
        layout.addView(error_layout, params1);

        final ImageView error_icon = new ImageView(PdfViewerActivityLollipop.this);
        error_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_input_warning));
        error_layout.addView(error_icon);
        RelativeLayout.LayoutParams params_icon = (RelativeLayout.LayoutParams) error_icon.getLayoutParams();

        params_icon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        error_icon.setLayoutParams(params_icon);

        error_icon.setColorFilter(ContextCompat.getColor(PdfViewerActivityLollipop.this, R.color.login_warning));

        final TextView textError = new TextView(PdfViewerActivityLollipop.this);
        error_layout.addView(textError);
        RelativeLayout.LayoutParams params_text_error = (RelativeLayout.LayoutParams) textError.getLayoutParams();
        params_text_error.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        params_text_error.addRule(RelativeLayout.CENTER_VERTICAL);
        params_text_error.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        params_text_error.setMargins(scaleWidthPx(3, outMetrics), 0, 0, 0);
        textError.setLayoutParams(params_text_error);

        textError.setTextColor(ContextCompat.getColor(PdfViewerActivityLollipop.this, R.color.login_warning));

        error_layout.setVisibility(View.GONE);

        input.getBackground().mutate().clearColorFilter();
        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(this, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (error_layout.getVisibility() == View.VISIBLE) {
                    error_layout.setVisibility(View.GONE);
                    input.getBackground().mutate().clearColorFilter();
                    input.getBackground().mutate().setColorFilter(ContextCompat.getColor(pdfViewerActivityLollipop, R.color.accentColor), PorterDuff.Mode.SRC_ATOP);
                }
            }
        });

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId,
                                          KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    String value = v.getText().toString().trim();
                    if (value.length() == 0) {
                        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(pdfViewerActivityLollipop, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                        textError.setText(getString(R.string.invalid_string));
                        error_layout.setVisibility(View.VISIBLE);
                        input.requestFocus();

                    } else {
                        boolean result = matches(regex, value);
                        if (result) {
                            input.getBackground().mutate().setColorFilter(ContextCompat.getColor(pdfViewerActivityLollipop, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                            textError.setText(getString(R.string.invalid_characters));
                            error_layout.setVisibility(View.VISIBLE);
                            input.requestFocus();

                        } else {
                            //						nC.renameNode(node, value);
                            renameDialog.dismiss();
                            rename(value, node);
                        }
                    }
                    return true;
                }
                return false;
            }
        });
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.context_rename) + " "	+ new String(node.getName()));
        builder.setPositiveButton(getString(R.string.context_rename),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString().trim();
                        if (value.length() == 0) {
                            return;
                        }
                        rename(value, node);
                    }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                input.getBackground().clearColorFilter();
            }
        });
        builder.setView(layout);
        renameDialog = builder.create();
        renameDialog.show();
        renameDialog.getButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new   View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String value = input.getText().toString().trim();

                if (value.length() == 0) {
                    input.getBackground().mutate().setColorFilter(ContextCompat.getColor(pdfViewerActivityLollipop, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                    textError.setText(getString(R.string.invalid_string));
                    error_layout.setVisibility(View.VISIBLE);
                    input.requestFocus();
                }
                else{
                    boolean result=matches(regex, value);
                    if(result){
                        input.getBackground().mutate().setColorFilter(ContextCompat.getColor(pdfViewerActivityLollipop, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
                        textError.setText(getString(R.string.invalid_characters));
                        error_layout.setVisibility(View.VISIBLE);
                        input.requestFocus();

                    }else{
                        //nC.renameNode(node, value);
                        renameDialog.dismiss();
                        rename(value, node);
                    }
                }
            }
        });
    }

    private void rename(String newName, MegaNode node){
        if (newName.equals(node.getName())) {
            return;
        }

        if(!isOnline(this)){
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
            return;
        }

        if (isFinishing()){
            return;
        }

        ProgressDialog temp = null;
        try{
            temp = new ProgressDialog(this);
            temp.setMessage(getString(R.string.context_renaming));
            temp.show();
        }
        catch(Exception e){
            return;
        }
        statusDialog = temp;

        logDebug("Renaming " + node.getName() + " to " + newName);

        megaApi.renameNode(node, newName, this);
    }

    public static boolean matches(String regex, CharSequence input) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(input);
        return m.find();
    }

    public void showPropertiesActivity(){
        Intent i = new Intent(this, FileInfoActivityLollipop.class);
        if (isOffLine){
            i.putExtra("name", pdfFileName);
            i.putExtra("adapterType", OFFLINE_ADAPTER);
            i.putExtra("path", path);
            if (pathNavigation != null){
                i.putExtra("pathNavigation", pathNavigation);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                i.setDataAndType(uri, MimeTypeList.typeForName(pdfFileName).getType());
            }
            else{
                i.setDataAndType(uri, MimeTypeList.typeForName(pdfFileName).getType());
            }
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        else {
            MegaNode node = megaApi.getNodeByHandle(handle);
            i.putExtra("handle", node.getHandle());
            i.putExtra("name", node.getName());
            if (nC == null) {
                nC = new NodeController(this);
            }
            boolean fromIncoming = false;

            if (type == SEARCH_ADAPTER || type == RECENTS_ADAPTER) {
                fromIncoming = nC.nodeComesFromIncoming(node);
            }
            if (type == INCOMING_SHARES_ADAPTER || fromIncoming) {
                i.putExtra("from", FROM_INCOMING_SHARES);
                i.putExtra("firstLevel", false);
            }
            else if(type == INBOX_ADAPTER){
                i.putExtra("from", FROM_INBOX);
            }
        }
        startActivity(i);
        renamed = false;
    }

    public void showRemoveLink(){
        android.support.v7.app.AlertDialog removeLinkDialog;
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);

        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.dialog_link, null);
        TextView url = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_url);
        TextView key = (TextView) dialoglayout.findViewById(R.id.dialog_link_link_key);
        TextView symbol = (TextView) dialoglayout.findViewById(R.id.dialog_link_symbol);
        TextView removeText = (TextView) dialoglayout.findViewById(R.id.dialog_link_text_remove);

        ((RelativeLayout.LayoutParams) removeText.getLayoutParams()).setMargins(scaleWidthPx(25, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(10, outMetrics), 0);

        url.setVisibility(View.GONE);
        key.setVisibility(View.GONE);
        symbol.setVisibility(View.GONE);
        removeText.setVisibility(View.VISIBLE);

        removeText.setText(getString(R.string.context_remove_link_warning_text));

        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = getResources().getDisplayMetrics().density;

        float scaleW = getScaleW(outMetrics, density);
        float scaleH = getScaleH(outMetrics, density);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (10*scaleW));
        }else{
            removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (15*scaleW));

        }

        builder.setView(dialoglayout);

        builder.setPositiveButton(getString(R.string.context_remove), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                typeExport=TYPE_EXPORT_REMOVE;
                megaApi.disableExport(megaApi.getNodeByHandle(handle), pdfViewerActivityLollipop);
            }
        });

        builder.setNegativeButton(getString(R.string.general_cancel), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        removeLinkDialog = builder.create();
        removeLinkDialog.show();
    }

    public void showGetLinkActivity(){
        logDebug("showGetLinkActivity");
        Intent linkIntent = new Intent(this, GetLinkActivityLollipop.class);
        linkIntent.putExtra("handle", handle);
        startActivity(linkIntent);
    }

    public void updateFile (){
        MegaNode file = null;
        if (pdfFileName != null && handle != -1 ) {
            file = megaApi.getNodeByHandle(handle);
            if (file != null){
                logDebug("Pdf File: " + pdfFileName + " node file: " + file.getName());
                if (!pdfFileName.equals(file.getName())) {
                    logDebug("Update File");

                    pdfFileName = file.getName();
                    if (aB != null){
                        tB = (Toolbar) findViewById(R.id.toolbar_pdf_viewer);
                        if(tB==null){
                            logError("Tb is Null");
                            return;
                        }
                        tB.setVisibility(View.VISIBLE);
                        setSupportActionBar(tB);
                        aB = getSupportActionBar();
                    }
                    aB.setTitle(" ");
                    setTitle(pdfFileName);
                    fileNameTextView.setText(pdfFileName);
                    supportInvalidateOptionsMenu();

                    String localPath = getLocalFile(this, file.getName(), file.getSize());

                    if (localPath != null){
                        File mediaFile = new File(localPath);
                        uri = getUriForFile(this, mediaFile);
                    }
                    else {
                        if (megaApi == null){
                            MegaApplication app = (MegaApplication)getApplication();
                            megaApi = app.getMegaApi();
                            megaApi.addTransferListener(this);
                            megaApi.addGlobalListener(this);
                        }
                        if (megaApi.httpServerIsRunning() == 0) {
                            megaApi.httpServerStart();
                        }

                        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                        activityManager.getMemoryInfo(mi);

                        if(mi.totalMem>BUFFER_COMP) {
                            logDebug("Total mem: " + mi.totalMem + " allocate 32 MB");
                            megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
                        } else {
                            logDebug("Total mem: " + mi.totalMem + " allocate 16 MB");
                            megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
                        }

                        String url = megaApi.httpServerGetLocalLink(file);
                        if (url != null){
                            uri = Uri.parse(url);
                        }
                    }
                    renamed = true;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        logDebug("onActivityResult: " + requestCode + "____" + resultCode);
        if (intent == null) {
            return;
        }

        if (requestCode == REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK){
            long[] chatHandles = intent.getLongArrayExtra(SELECTED_CHATS);
            long[] contactHandles = intent.getLongArrayExtra(SELECTED_USERS);
            long[] nodeHandles = intent.getLongArrayExtra(NODE_HANDLES);

            if ((chatHandles != null && chatHandles.length > 0) || (contactHandles != null && contactHandles.length > 0)) {
                if (contactHandles != null && contactHandles.length > 0) {
                    ArrayList<MegaChatRoom> chats = new ArrayList<>();
                    ArrayList<MegaUser> users = new ArrayList<>();

                    for (int i=0; i<contactHandles.length; i++) {
                        MegaUser user = megaApi.getContact(MegaApiAndroid.userHandleToBase64(contactHandles[i]));
                        if (user != null) {
                            users.add(user);
                        }
                    }

                    if (chatHandles != null) {
                        for (int i = 0; i < chatHandles.length; i++) {
                            MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatHandles[i]);
                            if (chatRoom != null) {
                                chats.add(chatRoom);
                            }
                        }
                    }

                    if(nodeHandles!=null){
                        CreateChatListener listener = new CreateChatListener(chats, users, nodeHandles[0], this, CreateChatListener.SEND_FILE);
                        for (MegaUser user : users) {
                            MegaChatPeerList peers = MegaChatPeerList.createInstance();
                            peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
                            megaChatApi.createChat(false, peers, listener);
                        }
                    }
                    else{
                        logError("Error on sending to chat");
                    }
                }
                else {
                    countChat = chatHandles.length;
                    for (int i = 0; i < chatHandles.length; i++) {
                        megaChatApi.attachNode(chatHandles[i], nodeHandles[0], this);
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_TREE) {
            onRequestSDCardWritePermission(intent, resultCode, (type == FROM_CHAT), nC);
        }
        else if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
            logDebug("Local folder selected");
            String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            if (type == FILE_LINK_ADAPTER){
                if (nC == null) {
                    nC = new NodeController(this);
                }
                nC.downloadTo(currentDocument, parentPath, uri.toString());
            }
            else if (type == FROM_CHAT) {
                chatC.prepareForDownload(intent, parentPath);
            }
            else {
                String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
                long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
                long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
                logDebug("URL: " + url + "___SIZE: " + size);

                if(nC==null){
                    nC = new NodeController(this, isFolderLink);
                }
                nC.checkSizeBeforeDownload(parentPath,url, size, hashes, false);
            }
        }
        else if (requestCode == REQUEST_CODE_SELECT_MOVE_FOLDER && resultCode == RESULT_OK) {

            if(!isOnline(this)){
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                return;
            }

            final long[] moveHandles = intent.getLongArrayExtra("MOVE_HANDLES");
            final long toHandle = intent.getLongExtra("MOVE_TO", 0);
            final int totalMoves = moveHandles.length;

            MegaNode parent = megaApi.getNodeByHandle(toHandle);
            moveToRubbish = false;

            ProgressDialog temp = null;
            try{
                temp = new ProgressDialog(this);
                temp.setMessage(getString(R.string.context_moving));
                temp.show();
            }
            catch(Exception e){
                return;
            }
            statusDialog = temp;

            for(int i=0; i<moveHandles.length;i++){
                megaApi.moveNode(megaApi.getNodeByHandle(moveHandles[i]), parent, this);
            }
        }
        else if (requestCode == REQUEST_CODE_SELECT_COPY_FOLDER && resultCode == RESULT_OK){
            if(!isOnline(this)){
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                return;
            }

            final long[] copyHandles = intent.getLongArrayExtra("COPY_HANDLES");
            final long toHandle = intent.getLongExtra("COPY_TO", 0);
            final int totalCopy = copyHandles.length;

            ProgressDialog temp = null;
            try{
                temp = new ProgressDialog(this);
                temp.setMessage(getString(R.string.context_copying));
                temp.show();
            }
            catch(Exception e){
                return;
            }
            statusDialog = temp;

            MegaNode parent = megaApi.getNodeByHandle(toHandle);
            for(int i=0; i<copyHandles.length;i++){
                MegaNode cN = megaApi.getNodeByHandle(copyHandles[i]);
                if (cN != null){
                    logDebug("cN != null, i = " + i + " of " + copyHandles.length);
                    megaApi.copyNode(cN, parent, this);
                }
                else{
                    logWarning("cN == null, i = " + i + " of " + copyHandles.length);
                    try {
                        statusDialog.dismiss();
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied), -1);
                    }
                    catch (Exception ex) {}
                }
            }
        }
        else if (requestCode == REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK){
            logDebug("REQUEST_CODE_SELECT_IMPORT_FOLDER OK");

            if(!isOnline(this)||megaApi==null) {
                try{
                    statusDialog.dismiss();
                } catch(Exception ex) {};
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                return;
            }

            final long toHandle = intent.getLongExtra("IMPORT_TO", 0);
            MegaNode target = null;
            target = megaApi.getNodeByHandle(toHandle);
            if(target == null){
                target = megaApi.getRootNode();
            }
            logDebug("TARGET: " + target.getName() + "and handle: " + target.getHandle());
            if (nodeChat != null) {
                logDebug("DOCUMENT: " + nodeChat.getName() + "_" + nodeChat.getHandle());
                if (target != null) {
                    megaApi.copyNode(nodeChat, target, this);
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

    @Override
    public void onPageChanged(int page, int pageCount) {
        logDebug("page: " + page + ", pageCount: " + pageCount);
        if (!notChangePage) {
            currentPage = page+1;
            actualPage.setText(String.valueOf(currentPage));
            setTitle(String.format("%s %s / %s", pdfFileName, currentPage, pageCount));
        }
        else {
            notChangePage = false;
        }
    }

    @Override
    public void onPageError(int page, Throwable t) {
        logError("Cannot load page " + page);
    }

    @Override
    public void loadComplete(int nbPages) {
        totalPages.setText(""+nbPages);
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        logDebug("Title = " + meta.getTitle());
        logDebug("Author = " + meta.getAuthor());
        logDebug("Subject = " + meta.getSubject());
        logDebug("Keywords = " + meta.getKeywords());
        logDebug("Creator = " + meta.getCreator());
        logDebug("Producer = " + meta.getProducer());
        logDebug("Creation Date = " + meta.getCreationDate());
        logDebug("Mod. Date = " + meta.getModDate());
        printBookmarksTree(pdfView.getTableOfContents(), "-");

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (toolbarVisible)
                    setToolbarVisibilityHide(200L);
            }
        }, 2000);
    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            logDebug(String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    public String getPdfFileName () {
        return pdfFileName;
    }

    public String getFileName(Uri uri) {
        if (uri == null || uri.getScheme() == null) {
            logWarning("URI is null");
            return null;
        }

        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                logWarning("Exception getting PDF file name.", e);
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? addPdfFileExtension(result) : null;
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        logDebug("onRequestFinish");

        MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());

        if (request.getType() == MegaRequest.TYPE_LOGIN){

            if (e.getErrorCode() != MegaError.API_OK) {
                logWarning("Login failed with error code: " + e.getErrorCode());
                MegaApplication.setLoggingIn(false);
            } else {
                //LOGIN OK
                gSession = megaApi.dumpSession();
                credentials = new UserCredentials(lastEmail, gSession, "", "", "");
                dbH.saveCredentials(credentials);
                logDebug("Logged in with session");
                megaApi.fetchNodes(this);
            }
        }
        else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){

            if (e.getErrorCode() == MegaError.API_OK){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Window window = this.getWindow();
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                    window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));

                }
                DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());

                gSession = megaApi.dumpSession();
                MegaUser myUser = megaApi.getMyUser();
                String myUserHandle = "";
                if(myUser!=null){
                    lastEmail = megaApi.getMyUser().getEmail();
                    myUserHandle = megaApi.getMyUser().getHandle()+"";
                }

                credentials = new UserCredentials(lastEmail, gSession, "", "", myUserHandle);

                dbH.saveCredentials(credentials);

                logDebug("Chat enabled-->connect");
                if ((megaChatApi.getInitState() != MegaChatApi.INIT_ERROR)) {
                    logDebug("Connection goes!!!");
                    megaChatApi.connect(this);
                } else {
                    logDebug("Not launch connect: " + megaChatApi.getInitState());
                }
                MegaApplication.setLoggingIn(false);
                download();
            }
        }
        else if (request.getType() == MegaRequest.TYPE_RENAME){

            try {
                statusDialog.dismiss();
            }
            catch (Exception ex) {}

            if (e.getErrorCode() == MegaError.API_OK){
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_renamed), -1);
                updateFile();
            }
            else{
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_renamed), -1);
            }
        }
        else if (request.getType() == MegaRequest.TYPE_MOVE){
            try {
                statusDialog.dismiss();
            }
            catch (Exception ex) {}

            if (moveToRubbish){
                if (e.getErrorCode() == MegaError.API_OK){
                    this.finish();
                }
                else{
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_moved), -1);
                }
                moveToRubbish = false;
                logDebug("Move to rubbish request finished");
            }
            else{
                if (e.getErrorCode() == MegaError.API_OK){
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_moved), -1);
                    finish();
                }
                else{
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_moved), -1);
                }
                logDebug("Move nodes request finished");
            }
        }
        else if (request.getType() == MegaRequest.TYPE_REMOVE){


            if (e.getErrorCode() == MegaError.API_OK){
                if (moveToTrashStatusDialog.isShowing()){
                    try {
                        moveToTrashStatusDialog.dismiss();
                    }
                    catch (Exception ex) {}
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_removed), -1);
                }
                finish();
            }
            else{
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_removed), -1);
            }
            logDebug("Remove request finished");
        }
        else if (request.getType() == MegaRequest.TYPE_COPY){
            try {
                statusDialog.dismiss();
            }
            catch (Exception ex) {}

            if (e.getErrorCode() == MegaError.API_OK){
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_correctly_copied), -1);
            }
            else if(e.getErrorCode()==MegaError.API_EOVERQUOTA){
                logWarning("OVERQUOTA ERROR: " + e.getErrorCode());
                Intent intent = new Intent(this, ManagerActivityLollipop.class);
                intent.setAction(ACTION_OVERQUOTA_STORAGE);
                startActivity(intent);
                finish();
            }
            else if(e.getErrorCode()==MegaError.API_EGOINGOVERQUOTA){
                logWarning("PRE OVERQUOTA ERROR: " + e.getErrorCode());
                Intent intent = new Intent(this, ManagerActivityLollipop.class);
                intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
                startActivity(intent);
                finish();
            }
            else{
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_no_copied), -1);
            }
            logDebug("Copy nodes request finished");
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        logWarning("onRequestTemporaryError");
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        logDebug("onRequestFinish - MegaChatApi");

        if (request.getType() == MegaChatRequest.TYPE_CONNECT){
            MegaApplication.setLoggingIn(false);
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                logDebug("Connected to chat!");
            }
            else{
                logError("ERROR WHEN CONNECTING " + e.getErrorString());
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE){

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                logDebug("File sent correctly");
                successSent++;
            }
            else{
                logError("File NOT sent: "+e.getErrorCode()+"___"+e.getErrorString());
                errorSent++;
            }

            if(countChat==errorSent+successSent){
                if(successSent==countChat){
                    if(countChat==1){
                        showSnackbar(MESSAGE_SNACKBAR_TYPE, null, request.getChatHandle());
                    }
                    else{
                        showSnackbar(MESSAGE_SNACKBAR_TYPE, getString(R.string.sent_as_message), -1);
                    }
                }
                else if(errorSent==countChat){
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_attaching_node_from_cloud), -1);
                }
                else{
                    showSnackbar(MESSAGE_SNACKBAR_TYPE, getString(R.string.error_sent_as_message), -1);
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    @Override
    protected void onStop() {
        super.onStop();
        logDebug("onStop");
    }

    @Override
    protected void onStart() {
        super.onStart();
        logDebug("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        logDebug("onResume");
        if (!isOffLine && !fromChat && !isFolderLink
                && type != FILE_LINK_ADAPTER
                && type != ZIP_ADAPTER){
            if (megaApi.getNodeByHandle(handle) == null && inside && !fromDownload){
                finish();
            }
            updateFile();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        logDebug("onPause");
    }

    @Override
    protected void onDestroy() {
        logDebug("onDestroy()");

        if (megaApi != null) {
            megaApi.removeTransferListener(this);
            megaApi.removeGlobalListener(this);
            megaApi.httpServerStop();
        }

        if (megaApiFolder != null) {
            megaApiFolder.httpServerStop();
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiverToFinish);

        super.onDestroy();
    }

    public void showTransferOverquotaDialog(){
        logDebug("showTransferOverquotaDialog");

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PdfViewerActivityLollipop.this);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.transfer_overquota_layout, null);
        dialogBuilder.setView(dialogView);

        TextView title = (TextView) dialogView.findViewById(R.id.transfer_overquota_title);
        title.setText(getString(R.string.title_depleted_transfer_overquota));

        ImageView icon = (ImageView) dialogView.findViewById(R.id.image_transfer_overquota);
        icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.transfer_quota_empty));

        TextView text = (TextView) dialogView.findViewById(R.id.text_transfer_overquota);
        text.setText(getString(R.string.text_depleted_transfer_overquota));

        Button continueButton = (Button) dialogView.findViewById(R.id.transfer_overquota_button_dissmiss);

        Button paymentButton = (Button) dialogView.findViewById(R.id.transfer_overquota_button_payment);
        paymentButton.setText(getString(R.string.action_upgrade_account));

        alertDialogTransferOverquota = dialogBuilder.create();

        alertDialogTransferOverquota.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                transferOverquota = true;
                progressBar.setVisibility(View.GONE);
            }
        });

        continueButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                alertDialogTransferOverquota.dismiss();
                transferOverquota = false;
                if (loading && !transferOverquota){
                    progressBar.setVisibility(View.VISIBLE);
                }
            }

        });

        paymentButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                alertDialogTransferOverquota.dismiss();
                transferOverquota = false;
                showUpgradeAccount();
            }
        });

        alertDialogTransferOverquota.setCancelable(false);
        alertDialogTransferOverquota.setCanceledOnTouchOutside(false);
        alertDialogTransferOverquota.show();
    }

    public void showUpgradeAccount(){
        logDebug("showUpgradeAccount");
        Intent upgradeIntent = new Intent(this, ManagerActivityLollipop.class);
        upgradeIntent.setAction(ACTION_SHOW_UPGRADE_ACCOUNT);
        startActivity(upgradeIntent);
    }

    @Override
    public void onTransferStart(MegaApiJava api, MegaTransfer transfer) {

    }

    @Override
    public void onTransferFinish(MegaApiJava api, MegaTransfer transfer, MegaError e) {

    }

    @Override
    public void onTransferUpdate(MegaApiJava api, MegaTransfer transfer) {

    }

    @Override
    public void onTransferTemporaryError(MegaApiJava api, MegaTransfer transfer, MegaError e) {

        if(e.getErrorCode() == MegaError.API_EOVERQUOTA){
            if (e.getValue() != 0) {
                logWarning("TRANSFER OVERQUOTA ERROR: " + e.getErrorCode());

                if(alertDialogTransferOverquota==null){
                    showTransferOverquotaDialog();
                }
                else {
                    if (!(alertDialogTransferOverquota.isShowing())) {
                        showTransferOverquotaDialog();
                    }
                }
            }
        } else if (e.getErrorCode() == MegaError.API_EBLOCKED) {
            showTakenDownAlert(this);
        }
    }

    @Override
    public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer) {
        return false;
    }

    public void showSnackbar(int type, String s, long idChat){
        showSnackbar(type, pdfviewerContainer, s, idChat);
    }

    public void openAdvancedDevices (long handleToDownload, boolean highPriority){
        logDebug("openAdvancedDevices");
//		handleToDownload = handle;
        String externalPath = getExternalCardPath();

        if(externalPath!=null){
            logDebug("ExternalPath for advancedDevices: " + externalPath);
            MegaNode node = megaApi.getNodeByHandle(handleToDownload);
            if(node!=null){

//				File newFile =  new File(externalPath+"/"+node.getName());
                File newFile =  new File(node.getName());
                logDebug("File: " + newFile.getPath());
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

                // Filter to only show results that can be "opened", such as
                // a file (as opposed to a list of contacts or timezones).
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // Create a file with the requested MIME type.
                String mimeType = MimeTypeList.getMimeType(newFile);
                logDebug("Mimetype: " + mimeType);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_TITLE, node.getName());
                intent.putExtra("handleToDownload", handleToDownload);
                intent.putExtra(HIGH_PRIORITY_TRANSFER, highPriority);
                try{
                    startActivityForResult(intent, WRITE_SD_CARD_REQUEST_CODE);
                }
                catch(Exception e) {
                    logError("Exception in External SDCARD", e);
                    Environment.getExternalStorageDirectory();
                    Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        }
        else{
            logWarning("No external SD card");
            Environment.getExternalStorageDirectory();
            Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void askSizeConfirmationBeforeChatDownload(String parentPath, ArrayList<MegaNode> nodeList, long size){
        logDebug("askSizeConfirmationBeforeChatDownload");

        final String parentPathC = parentPath;
        final ArrayList<MegaNode> nodeListC = nodeList;
        final long sizeC = size;
        final ChatController chatC = new ChatController(this);

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        LinearLayout confirmationLayout = new LinearLayout(this);
        confirmationLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(10, outMetrics), scaleWidthPx(17, outMetrics), 0);

        final CheckBox dontShowAgain =new CheckBox(this);
        dontShowAgain.setText(getString(R.string.checkbox_not_show_again));
        dontShowAgain.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        confirmationLayout.addView(dontShowAgain, params);

        builder.setView(confirmationLayout);

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

    public void askSizeConfirmationBeforeDownload(String parentPath, String url, long size, long [] hashes, final boolean highPriority){
        logDebug("askSizeConfirmationBeforeDownload");

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        final long sizeC=size;

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
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
                        if(nC==null){
                            nC = new NodeController(pdfViewerActivityLollipop, isFolderLink);
                        }
                        nC.checkInstalledAppBeforeDownload(parentPathC,urlC, sizeC, hashesC, highPriority);
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

    public void askConfirmationNoAppInstaledBeforeDownload (String parentPath, String url, long size, long [] hashes, String nodeToDownload, final boolean highPriority){
        logDebug("askConfirmationNoAppInstaledBeforeDownload");

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        final long sizeC=size;

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
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
        builder.setMessage(getString(R.string.alert_no_app, nodeToDownload));
        builder.setPositiveButton(getString(R.string.general_save_to_device),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(dontShowAgain.isChecked()){
                            dbH.setAttrAskNoAppDownload("false");
                        }
                        if(nC==null){
                            nC = new NodeController(pdfViewerActivityLollipop, isFolderLink);
                        }
                        nC.download(parentPathC, urlC, sizeC, hashesC, highPriority);
                    }
                });
        builder.setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if(dontShowAgain.isChecked()){
                    dbH.setAttrAskNoAppDownload("false");
                }
            }
        });
        downloadConfirmationDialog = builder.create();
        downloadConfirmationDialog.show();
    }

    public Uri getUri() {
        return uri;
    }

    public String getPassword () {
        return password;
    }

    public int getMaxIntents() {
        return maxIntents;
    }
}
