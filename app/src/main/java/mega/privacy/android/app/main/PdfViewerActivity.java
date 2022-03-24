package mega.privacy.android.app.main;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.util.SizeF;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.activities.contract.NameCollisionActivityContract;
import mega.privacy.android.app.namecollision.NameCollisionActivity;
import mega.privacy.android.app.namecollision.data.NameCollisionType;
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase;
import mega.privacy.android.app.usecase.CopyNodeUseCase;
import mega.privacy.android.app.usecase.MoveNodeUseCase;
import mega.privacy.android.app.usecase.exception.ForeignNodeException;
import mega.privacy.android.app.usecase.exception.MegaNodeException;
import mega.privacy.android.app.usecase.exception.OverQuotaException;
import mega.privacy.android.app.usecase.exception.PreOverQuotaException;
import mega.privacy.android.app.utils.MegaProgressDialogUtil;
import mega.privacy.android.app.components.attacher.MegaAttacher;
import mega.privacy.android.app.components.dragger.DragToExitSupport;
import mega.privacy.android.app.components.saver.NodeSaver;
import mega.privacy.android.app.interfaces.ActionNodeCallback;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.controllers.NodeController;
import mega.privacy.android.app.utils.AlertsAndWarnings;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;
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

import static mega.privacy.android.app.main.FileInfoActivity.TYPE_EXPORT_REMOVE;
import static mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists;
import static mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showForeignStorageOverQuotaWarningDialog;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showTakenDownAlert;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_FOLDER;
import static mega.privacy.android.app.utils.Constants.ACTION_OVERQUOTA_STORAGE;
import static mega.privacy.android.app.utils.Constants.ACTION_PRE_OVERQUOTA_STORAGE;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN;
import static mega.privacy.android.app.utils.Constants.BUFFER_COMP;
import static mega.privacy.android.app.utils.Constants.CONTACT_FILE_ADAPTER;
import static mega.privacy.android.app.utils.Constants.EXTRA_SERIALIZE_STRING;
import static mega.privacy.android.app.utils.Constants.FILE_LINK_ADAPTER;
import static mega.privacy.android.app.utils.Constants.FROM_CHAT;
import static mega.privacy.android.app.utils.Constants.FROM_INBOX;
import static mega.privacy.android.app.utils.Constants.FROM_INCOMING_SHARES;
import static mega.privacy.android.app.utils.Constants.HIGH_PRIORITY_TRANSFER;
import static mega.privacy.android.app.utils.Constants.INBOX_ADAPTER;
import static mega.privacy.android.app.utils.Constants.INCOMING_SHARES_ADAPTER;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FIRST_LEVEL;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER;
import static mega.privacy.android.app.utils.Constants.MAX_BUFFER_16MB;
import static mega.privacy.android.app.utils.Constants.MAX_BUFFER_32MB;
import static mega.privacy.android.app.utils.Constants.NAME;
import static mega.privacy.android.app.utils.Constants.OFFLINE_ADAPTER;
import static mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FOLDER_TO_COPY;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER;
import static mega.privacy.android.app.utils.Constants.RUBBISH_BIN_ADAPTER;
import static mega.privacy.android.app.utils.Constants.SEARCH_ADAPTER;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Constants.URL_FILE_LINK;
import static mega.privacy.android.app.utils.Constants.WRITE_SD_CARD_REQUEST_CODE;
import static mega.privacy.android.app.utils.Constants.ZIP_ADAPTER;
import static mega.privacy.android.app.utils.FileUtil.addPdfFileExtension;
import static mega.privacy.android.app.utils.FileUtil.getLocalFile;
import static mega.privacy.android.app.utils.FileUtil.getUriForFile;
import static mega.privacy.android.app.utils.FileUtil.shareFile;
import static mega.privacy.android.app.utils.FileUtil.shareWithUri;
import static mega.privacy.android.app.utils.LinksUtil.showGetLinkActivity;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.moveToRubbishOrRemove;
import static mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog;
import static mega.privacy.android.app.utils.MegaNodeUtil.shareLink;
import static mega.privacy.android.app.utils.MegaNodeUtil.shareNode;
import static mega.privacy.android.app.utils.MegaNodeUtil.showShareOption;
import static mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownNodeActionNotAvailableDialog;
import static mega.privacy.android.app.utils.Util.getExternalCardPath;
import static mega.privacy.android.app.utils.Util.getScaleW;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.scaleHeightPx;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import javax.inject.Inject;

@AndroidEntryPoint
public class PdfViewerActivity extends PasscodeActivity
        implements MegaGlobalListenerInterface, OnPageChangeListener, OnLoadCompleteListener,
        OnPageErrorListener, MegaRequestListenerInterface,
        MegaTransferListenerInterface, ActionNodeCallback, SnackbarShower {

    @Inject
    CheckNameCollisionUseCase checkNameCollisionUseCase;
    @Inject
    MoveNodeUseCase moveNodeUseCase;
    @Inject
    CopyNodeUseCase copyNodeUseCase;

    MegaApplication app = null;
    MegaApiAndroid megaApi;
    MegaApiAndroid megaApiFolder;
    MegaChatApiAndroid megaChatApi;

    public ProgressBar progressBar;

    public static boolean loading = true;
    boolean transferOverquota = false;

    PDFView pdfView;

    Toolbar tB;
    public ActionBar aB;
    UserCredentials credentials;
    private String lastEmail;
    DatabaseHandler dbH = null;
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

    public RelativeLayout uploadContainer;
    RelativeLayout pdfviewerContainer;

    AlertDialog statusDialog;

    private boolean renamed = false;
    private String path;
    private String pathNavigation;

    private final MegaAttacher nodeAttacher = new MegaAttacher(this);
    private final NodeSaver nodeSaver = new NodeSaver(this, this, this,
            AlertsAndWarnings.showSaveToDeviceConfirmDialog(this));

    // it's only used for enter animation
    private final DragToExitSupport dragToExit = new DragToExitSupport(this, null, null);

    NodeController nC;
    private DisplayMetrics outMetrics;

    private RelativeLayout bottomLayout;
    private TextView fileNameTextView;
    int typeExport = -1;
    private Handler handler;

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

    private AlertDialog takenDownDialog;

    private ActivityResultLauncher<Object> nameCollisionActivityContract;

    private final BroadcastReceiver receiverToFinish = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                finish();
            }
        }
    };

    @Override
    protected boolean shouldSetStatusBarTextColor() {
        return false;
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        logDebug("onCreate");

        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.black));
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        nameCollisionActivityContract = registerForActivityResult(
                new NameCollisionActivityContract(),
                result -> {
                    if (result != null) {
                        if (result.equals(StringResourcesUtils.getString(R.string.context_correctly_moved))) {
                            finish();
                            return;
                        }

                        showSnackbar(SNACKBAR_TYPE, result, MEGACHAT_INVALID_HANDLE);
                    }
                });

        registerReceiver(receiverToFinish, new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_UPDATE_FULL_SCREEN));

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

            nodeAttacher.restoreState(savedInstanceState);
            nodeSaver.restoreState(savedInstanceState);
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
        inside = intent.getBooleanExtra("inside", false);

        if (!inside) {
            disablePasscode();
        }

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

        setContentView(R.layout.activity_pdfviewer);

        pdfviewerContainer = (RelativeLayout) findViewById(R.id.pdf_viewer_container);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(getApplicationContext());
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
                    MegaNode node;

                    if (fromChat) {
                        node = nodeChat;
                    } else if (type == FILE_LINK_ADAPTER) {
                        node = currentDocument;
                    } else {
                        node = megaApi.getNodeByHandle(handle);
                    }

                    String url = null;

                    if (node != null) {
                        url = megaApi.httpServerGetLocalLink(node);

                        if (url != null) {
                            uri = Uri.parse(url);
                        }
                    }

                    if (node == null || url == null || uri == null) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.error_streaming), MEGACHAT_INVALID_HANDLE);
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

            if (transfersManagement.isOnTransferOverQuota()) {
                showGeneralTransferOverQuotaWarning();
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
        if (aB != null) {
            aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
        }
        bottomLayout = (RelativeLayout) findViewById(R.id.pdf_viewer_layout_bottom);
        fileNameTextView = (TextView) findViewById(R.id.pdf_viewer_file_name);
        progressBar = (ProgressBar) findViewById(R.id.pdf_viewer_progress_bar);

        pdfView = (PDFView) findViewById(R.id.pdfView);

        pdfView.setBackgroundColor(Color.LTGRAY);
        pdfFileName = getFileName(uri);
        defaultScrollHandle = new DefaultScrollHandle(PdfViewerActivity.this);

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
        }
        else {
            aB.setTitle(" ");
            uploadContainer.setVisibility(View.GONE);
            bottomLayout.setVisibility(View.VISIBLE);
        }

        uploadContainer.setOnClickListener(v -> {
            logDebug("onClick uploadContainer");
            Intent intent1 = new Intent(PdfViewerActivity.this, FileExplorerActivity.class);
            intent1.setAction(Intent.ACTION_SEND);
            intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent1.setDataAndType(uri, "application/pdf");
            startActivity(intent1);
            finish();
        });

        if (!toolbarVisible) {
            setToolbarVisibilityHide(0L);
        }

        if (savedInstanceState == null) {
            runEnterAnimation(intent);
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
            disablePasscode();
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

                if (transfersManagement.isOnTransferOverQuota()) {
                    showGeneralTransferOverQuotaWarning();
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
            if (aB != null) {
                aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
                aB.setHomeButtonEnabled(true);
                aB.setDisplayHomeAsUpEnabled(true);
            }

            bottomLayout = (RelativeLayout) findViewById(R.id.pdf_viewer_layout_bottom);
            fileNameTextView = (TextView) findViewById(R.id.pdf_viewer_file_name);
            progressBar = (ProgressBar) findViewById(R.id.pdf_viewer_progress_bar);

            pdfView = (PDFView) findViewById(R.id.pdfView);

            pdfView.setBackgroundColor(Color.LTGRAY);
            defaultScrollHandle = new DefaultScrollHandle(PdfViewerActivity.this);

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
            uploadContainer.setOnClickListener(v -> {
                logDebug("onClick uploadContainer");
                Intent intent1 = new Intent(PdfViewerActivity.this, FileExplorerActivity.class);
                intent1.setAction(Intent.ACTION_SEND);
                intent1.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent1.setDataAndType(uri, "application/pdf");
                startActivity(intent1);
                finish();
            });

            pdfviewerContainer = (RelativeLayout) findViewById(R.id.pdf_viewer_container);

            runEnterAnimation(intent);
        }
    }

    private void runEnterAnimation(Intent intent) {
        dragToExit.runEnterAnimation(intent, pdfView, animationStart -> {
            if (animationStart) {
                if (aB != null && aB.isShowing()) {
                    if(tB != null) {
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

                        tB.animate().translationY(-220).setDuration(0).withEndAction(aB::hide).start();
                        bottomLayout.animate().translationY(220).setDuration(0).start();
                        uploadContainer.animate().translationY(220).setDuration(0).start();
                    } else {
                        aB.hide();
                    }
                }
            } else if (!isFinishing()) {
                setToolbarVisibilityShow();
            }

            return null;
        });
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
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

        nodeAttacher.saveState(outState);
        nodeSaver.saveState(outState);
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

    @Override
    public void showSnackbar(int type, String content, long chatId) {
        showSnackbar(type, pdfviewerContainer, content, chatId);
    }

    @Override
    public void finishRenameActionWithSuccess(@NonNull String newName) {
        updateFile();
    }

    @Override
    public void actionConfirmed() {
        //No update needed
    }

    @Override
    public void createFolder(@NotNull String folderName) {
        //No action needed
    }

    private class LoadPDFStream extends AsyncTask<String, Void, InputStream> {

        @Override
        protected InputStream doInBackground(String... strings) {
            InputStream inputStream = null;

            try {
                URL url = new URL(strings[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                if (httpURLConnection.getResponseCode() == 200) {
                    inputStream = new BufferedInputStream( (httpURLConnection.getInputStream()));
                }
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
                pdfView.fromStream(inputStream, String.valueOf(handle))
                        .defaultPage(currentPage-1)
                        .onPageChange(PdfViewerActivity.this)
                        .enableAnnotationRendering(true)
                        .onLoad(PdfViewerActivity.this)
                        .scrollHandle(defaultScrollHandle)
                        .spacing(10) // in dp
                        .onPageError(PdfViewerActivity.this)
                        .password(password)
                        .load();
            } catch (Exception e) {
                e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    public void download() {
        if (type == OFFLINE_ADAPTER) {
            MegaOffline node = dbH.findByHandle(handle);
            if (node != null) {
                nodeSaver.saveOfflineNode(node, true);
            }
        } else if (type == FILE_LINK_ADAPTER) {
            nodeSaver.saveNode(currentDocument, false, false, true, true);
        } else if (fromChat) {
            nodeSaver.saveNode(nodeChat, true, false, true, true);
        } else {
            nodeSaver.saveHandle(handle, false, isFolderLink, true, false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        nodeSaver.handleRequestPermissionsResult(requestCode);
    }

    public void setToolbarVisibilityShow () {
        logDebug("setToolbarVisibilityShow");
        toolbarVisible = true;

        aB.show();
        adjustPositionOfScroller();

        if(tB != null) {
            tB.animate().translationY(0).setDuration(200L).start();
            bottomLayout.animate().translationY(0).setDuration(200L).start();
            uploadContainer.animate().translationY(0).setDuration(200L).start();
        }
    }

    public void setToolbarVisibilityHide (long duration) {
        logDebug("Duration: " + duration);
        toolbarVisible = false;
        if(tB != null) {
            tB.animate().translationY(-220).setDuration(duration).withEndAction(() -> aB.hide()).start();
            bottomLayout.animate().translationY(220).setDuration(duration).start();
            uploadContainer.animate().translationY(220).setDuration(duration).start();
        }
        else {
            aB.hide();
        }
    }

    /*
     * Adjust the position of scroller below the ActionBar
     */
    private void adjustPositionOfScroller() {
        defaultScrollHandle.post(() -> {
            int[] location = new int[2];
            defaultScrollHandle.getLocationInWindow(location);

            Rect frame = new Rect();
            getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);

            int height = aB.getHeight();

            // When there is an intersection between the scroller and the ActionBar, move the scroller.
            if (location[1] < height) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(defaultScrollHandle, "translationY", height+16);
                animator.setDuration(200L).start();
            }
        });
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
        return sizeF.getWidth() > sizeF.getHeight();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        logDebug("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_pdfviewer, menu);

        MenuItem shareMenuItem = menu.findItem(R.id.pdf_viewer_share);
        MenuItem downloadMenuItem = menu.findItem(R.id.pdf_viewer_download);
        MenuItem chatMenuItem = menu.findItem(R.id.pdf_viewer_chat);
        MenuItem propertiesMenuItem = menu.findItem(R.id.pdf_viewer_properties);
        MenuItem getlinkMenuItem = menu.findItem(R.id.pdf_viewer_get_link);
        getlinkMenuItem.setTitle(StringResourcesUtils.getQuantityString(R.plurals.get_links, 1));
        MenuItem renameMenuItem = menu.findItem(R.id.pdf_viewer_rename);
        MenuItem moveMenuItem = menu.findItem(R.id.pdf_viewer_move);
        MenuItem copyMenuItem = menu.findItem(R.id.pdf_viewer_copy);
        MenuItem moveToTrashMenuItem = menu.findItem(R.id.pdf_viewer_move_to_trash);
        MenuItem removeMenuItem = menu.findItem(R.id.pdf_viewer_remove);
        MenuItem removelinkMenuItem = menu.findItem(R.id.pdf_viewer_remove_link);
        MenuItem importMenuItem = menu.findItem(R.id.chat_pdf_viewer_import);
        MenuItem saveForOfflineMenuItem = menu.findItem(R.id.chat_pdf_viewer_save_for_offline);
        MenuItem chatRemoveMenuItem = menu.findItem(R.id.chat_pdf_viewer_remove);

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
            shareMenuItem.setVisible(true);
        } else {
            if (nC == null) {
                nC = new NodeController(this);
            }
            boolean fromIncoming = false;
            if (type == SEARCH_ADAPTER) {
                fromIncoming = nC.nodeComesFromIncoming(megaApi.getNodeByHandle(handle));
            }

            shareMenuItem.setVisible(showShareOption(type, isFolderLink, handle));

            if (type == OFFLINE_ADAPTER) {
                getlinkMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                propertiesMenuItem.setVisible(true);
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
            } else if (type == RUBBISH_BIN_ADAPTER
                    || (megaApi != null && megaApi.isInRubbish(megaApi.getNodeByHandle(handle)))) {
                shareMenuItem.setVisible(false);
                getlinkMenuItem.setVisible(false);
                removelinkMenuItem.setVisible(false);
                propertiesMenuItem.setVisible(true);
                downloadMenuItem.setVisible(false);
                renameMenuItem.setVisible(false);
                moveMenuItem.setVisible(false);
                copyMenuItem.setVisible(false);
                moveToTrashMenuItem.setVisible(false);
                removeMenuItem.setVisible(true);
                chatMenuItem.setVisible(false);
                importMenuItem.setVisible(false);
                saveForOfflineMenuItem.setVisible(false);
                chatRemoveMenuItem.setVisible(false);
            } else if (type == SEARCH_ADAPTER && !fromIncoming) {
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

                    chatRemoveMenuItem.setVisible(msgChat.getUserHandle() == megaChatApi.getMyUserHandle()
                            && msgChat.isDeletable());
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

                    chatRemoveMenuItem.setVisible(msgChat.getUserHandle() == megaChatApi.getMyUserHandle() && msgChat.isDeletable());
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

                            moveToTrashMenuItem.setVisible(true);
                            removeMenuItem.setVisible(false);
                        }
                    }

                    downloadMenuItem.setVisible(true);
                    importMenuItem.setVisible(false);
                    saveForOfflineMenuItem.setVisible(false);
                    chatRemoveMenuItem.setVisible(false);
                }
            }
        }

        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
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
                if (type == ZIP_ADAPTER) {
                    shareFile(this, new File(uri.toString()));
                } else if (type == OFFLINE_ADAPTER || !inside) {
                    shareWithUri(this, "pdf", uri);
                } else if (type == FILE_LINK_ADAPTER) {
                    shareLink(this, getIntent().getStringExtra(URL_FILE_LINK));
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
                nodeAttacher.attachNode(handle);
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

                showGetLinkActivity(this, handle);
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
                showRenameNodeDialog(this, megaApi.getNodeByHandle(handle), this, this);
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
            case R.id.pdf_viewer_move_to_trash:
            case R.id.pdf_viewer_remove: {
                moveToRubbishOrRemove(handle, this, this);
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
                    chatC.saveForOffline(msgChat.getMegaNodeList(), megaChatApi.getChatRoom(chatId),
                            true, this);
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

        Intent intent = new Intent(this, FileExplorerActivity.class);
        intent.setAction(FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER);
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER);
    }

    public void showConfirmationDeleteNode(final long chatId, final MegaChatMessage message){
        logDebug("showConfirmationDeleteNode");
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    if (chatC == null){
                        chatC = new ChatController(PdfViewerActivity.this);
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
        };

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
        builder.setMessage(R.string.confirmation_delete_one_attachment);
        builder.setPositiveButton(R.string.context_remove, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();

        isDeleteDialogShow = true;

        builder.setOnDismissListener(dialog -> isDeleteDialogShow = false);
    }

    public void showCopy(){
        logDebug("showCopy");

        ArrayList<Long> handleList = new ArrayList<>();
        handleList.add(handle);

        Intent intent = new Intent(this, FileExplorerActivity.class);
        intent.setAction(FileExplorerActivity.ACTION_PICK_COPY_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("COPY_FROM", longArray);
        startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER_TO_COPY);
    }

    public void showMove(){
        logDebug("showMove");

        ArrayList<Long> handleList = new ArrayList<>();
        handleList.add(handle);

        Intent intent = new Intent(this, FileExplorerActivity.class);
        intent.setAction(FileExplorerActivity.ACTION_PICK_MOVE_FOLDER);
        long[] longArray = new long[handleList.size()];
        for (int i=0; i<handleList.size(); i++){
            longArray[i] = handleList.get(i);
        }
        intent.putExtra("MOVE_FROM", longArray);
        startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER_TO_MOVE);
    }

    public void showPropertiesActivity(){
        Intent i = new Intent(this, FileInfoActivity.class);
        if (isOffLine){
            i.putExtra(NAME, pdfFileName);
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
            i.putExtra(NAME, node.getName());
            if (nC == null) {
                nC = new NodeController(this);
            }
            boolean fromIncoming = false;

            if (type == SEARCH_ADAPTER || type == RECENTS_ADAPTER) {
                fromIncoming = nC.nodeComesFromIncoming(node);
            }
            if (type == INCOMING_SHARES_ADAPTER || fromIncoming) {
                i.putExtra("from", FROM_INCOMING_SHARES);
                i.putExtra(INTENT_EXTRA_KEY_FIRST_LEVEL, false);
            }
            else if(type == INBOX_ADAPTER){
                i.putExtra("from", FROM_INBOX);
            }
        }
        startActivity(i);
        renamed = false;
    }

    public void showRemoveLink(){
        AlertDialog removeLinkDialog;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);

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
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (10*scaleW));
        }else{
            removeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, (15*scaleW));

        }

        builder.setView(dialoglayout);

        builder.setPositiveButton(getString(R.string.context_remove), (dialog, which) -> {
            typeExport=TYPE_EXPORT_REMOVE;
            megaApi.disableExport(megaApi.getNodeByHandle(handle), PdfViewerActivity.this);
        });

        builder.setNegativeButton(getString(R.string.general_cancel), (dialog, which) -> {

        });

        removeLinkDialog = builder.create();
        removeLinkDialog.show();
    }

    public void updateFile (){
        MegaNode file;
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
                    if (aB != null) {
                        aB.setTitle(" ");
                    }
                    setTitle(pdfFileName);
                    fileNameTextView.setText(pdfFileName);
                    supportInvalidateOptionsMenu();

                    String localPath = getLocalFile(file);

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

    /**
     * Checks if there is a name collision before moving or copying the node.
     *
     * @param parentHandle Parent handle of the node in which the node will be moved or copied.
     * @param type         Type of name collision to check.
     */
    private void checkCollision(long parentHandle, NameCollisionType type) {
        checkNameCollisionUseCase.check(handle, parentHandle, type)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(collision -> {
                            dismissAlertDialogIfExists(statusDialog);
                            nameCollisionActivityContract.launch(collision);
                        },
                        throwable -> {
                            if (throwable instanceof MegaNodeException.ParentDoesNotExistException) {
                                showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.general_error), MEGACHAT_INVALID_HANDLE);
                            } else if (throwable instanceof MegaNodeException.ChildDoesNotExistsException) {
                                if (type == NameCollisionType.MOVEMENT) {
                                    move(parentHandle);
                                } else {
                                    copy(parentHandle);
                                }
                            }
                        });
    }

    /**
     * Moves the node.
     *
     * @param parentHandle Parent handle in which the node will be moved.
     */
    private void move(long parentHandle) {
        moveNodeUseCase.move(handle, parentHandle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                            dismissAlertDialogIfExists(statusDialog);
                            showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.context_correctly_moved), MEGACHAT_INVALID_HANDLE);
                            finish();
                        }, throwable -> {
                            dismissAlertDialogIfExists(statusDialog);
                            if (throwable instanceof ForeignNodeException) {
                                showForeignStorageOverQuotaWarningDialog(this);
                            } else {
                                showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.context_no_moved), MEGACHAT_INVALID_HANDLE);
                            }
                        }
                );
    }

    /**
     * Copies the node.
     *
     * @param parentHandle Parent handle in which the node will be copied.
     */
    private void copy(long parentHandle) {
        copyNodeUseCase.copy(handle, parentHandle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                            dismissAlertDialogIfExists(statusDialog);
                            showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.context_correctly_copied), MEGACHAT_INVALID_HANDLE);
                        }, throwable -> {
                            dismissAlertDialogIfExists(statusDialog);
                            if (throwable instanceof ForeignNodeException) {
                                showForeignStorageOverQuotaWarningDialog(this);
                            } else if (throwable instanceof OverQuotaException) {
                                logWarning("OVERQUOTA ERROR: ", throwable);
                                Intent intent = new Intent(this, ManagerActivity.class);
                                intent.setAction(ACTION_OVERQUOTA_STORAGE);
                                startActivity(intent);
                                finish();
                            } else if (throwable instanceof PreOverQuotaException) {
                                logWarning("PRE OVERQUOTA ERROR: ", throwable);
                                Intent intent = new Intent(this, ManagerActivity.class);
                                intent.setAction(ACTION_PRE_OVERQUOTA_STORAGE);
                                startActivity(intent);
                                finish();
                            } else {
                                showSnackbar(SNACKBAR_TYPE, StringResourcesUtils.getString(R.string.context_no_copied), MEGACHAT_INVALID_HANDLE);
                            }
                        }
                );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        logDebug("onActivityResult: " + requestCode + "____" + resultCode);
        if (intent == null) {
            return;
        }

        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
            return;
        }

        if (nodeAttacher.handleActivityResult(requestCode, resultCode, intent, this)) {
            return;
        }

        if (requestCode == REQUEST_CODE_SELECT_FOLDER_TO_MOVE && resultCode == RESULT_OK) {
            if (!isOnline(this)) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                return;
            }

            final long toHandle = intent.getLongExtra("MOVE_TO", 0);

            AlertDialog temp;
            try {
                temp = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.context_moving));
                temp.show();
            } catch (Exception e) {
                return;
            }
            statusDialog = temp;

            checkCollision(toHandle, NameCollisionType.MOVEMENT);
        } else if (requestCode == REQUEST_CODE_SELECT_FOLDER_TO_COPY && resultCode == RESULT_OK) {
            if (!isOnline(this)) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                return;
            }

            final long toHandle = intent.getLongExtra("COPY_TO", 0);

            AlertDialog temp;
            try {
                temp = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.context_copying));
                temp.show();
            } catch (Exception e) {
                return;
            }
            statusDialog = temp;

            checkCollision(toHandle, NameCollisionType.COPY);
        } else if (requestCode == REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
            logDebug("REQUEST_CODE_SELECT_IMPORT_FOLDER OK");

            if(!isOnline(this)||megaApi==null) {
                try{
                    statusDialog.dismiss();
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                return;
            }

            final long toHandle = intent.getLongExtra("IMPORT_TO", 0);
            MegaNode target;
            target = megaApi.getNodeByHandle(toHandle);
            if(target == null){
                target = megaApi.getRootNode();
            }
            logDebug("TARGET: " + target.getName() + "and handle: " + target.getHandle());
            if (nodeChat != null) {
                logDebug("DOCUMENT: " + nodeChat.getName() + "_" + nodeChat.getHandle());
                megaApi.copyNode(nodeChat, target, this);
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
        defaultScrollHandle.setTotalPages(nbPages);
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

        handler.postDelayed(() -> {
            if (toolbarVisible)
                setToolbarVisibilityHide(200L);
        }, 2000);
    }

    @SuppressLint("DefaultLocale")
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

        String gSession;
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
                logDebug("Setting account auth token for folder links.");
                megaApiFolder.setAccountAuth(megaApi.getAccountAuth());
                megaApi.fetchNodes(this);

                // Get cookies settings after login.
                MegaApplication.getInstance().checkEnabledCookies();
            }
        }
        else if (request.getType() == MegaRequest.TYPE_FETCH_NODES){

            if (e.getErrorCode() == MegaError.API_OK){
                gSession = megaApi.dumpSession();
                MegaUser myUser = megaApi.getMyUser();
                String myUserHandle = "";
                if(myUser!=null){
                    lastEmail = megaApi.getMyUser().getEmail();
                    myUserHandle = megaApi.getMyUser().getHandle()+"";
                }

                credentials = new UserCredentials(lastEmail, gSession, "", "", myUserHandle);

                dbH.saveCredentials(credentials);

                MegaApplication.setLoggingIn(false);
                download();
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        logWarning("onRequestTemporaryError");
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
    protected void onDestroy() {
        logDebug("onDestroy()");

        boolean needStopHttpServer = getIntent().getBooleanExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, false);

        if (megaApi != null) {
            megaApi.removeTransferListener(this);
            megaApi.removeGlobalListener(this);

            if (needStopHttpServer) {
                megaApi.httpServerStop();
            }
        }

        if (megaApiFolder != null && needStopHttpServer) {
            megaApiFolder.httpServerStop();
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        unregisterReceiver(receiverToFinish);

        nodeSaver.destroy();

        dismissAlertDialogIfExists(takenDownDialog);

        super.onDestroy();
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
            if (transfer.isForeignOverquota()) {
                return;
            }

            if (e.getValue() != 0) {
                logWarning("TRANSFER OVERQUOTA ERROR: " + e.getErrorCode());
                showGeneralTransferOverQuotaWarning();
            }
        } else if (e.getErrorCode() == MegaError.API_EBLOCKED && !isAlertDialogShown(takenDownDialog)) {
            takenDownDialog = showTakenDownAlert(this);
        }
    }

    @Override
    public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer) {
        return false;
    }

    public void openAdvancedDevices (long handleToDownload, boolean highPriority){
        logDebug("openAdvancedDevices");
        String externalPath = getExternalCardPath();

        if(externalPath!=null){
            logDebug("ExternalPath for advancedDevices: " + externalPath);
            MegaNode node = megaApi.getNodeByHandle(handleToDownload);
            if(node!=null){

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

    public Uri getUri() {
        return uri;
    }

    public String getPassword () {
        return password;
    }

    public int getMaxIntents() {
        return maxIntents;
    }

    public AlertDialog getTakenDownDialog() {
        return takenDownDialog;
    }
}
