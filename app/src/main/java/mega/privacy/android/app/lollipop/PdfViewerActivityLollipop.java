package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.shockwave.pdfium.PdfDocument;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.megachat.ChatExplorerActivity;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.tasks.FilePrepareTask;
import mega.privacy.android.app.snackbarListeners.SnackbarNavigateOption;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaTransferListenerInterface;
import nz.mega.sdk.MegaUser;

public class PdfViewerActivityLollipop extends PinActivityLollipop implements OnPageChangeListener, OnLoadCompleteListener, OnPageErrorListener, View.OnClickListener, MegaRequestListenerInterface, MegaChatRequestListenerInterface, MegaTransferListenerInterface{

    public static int REQUEST_CODE_SELECT_CHAT = 1005;

    MegaApplication app = null;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    MegaPreferences prefs = null;

    private AlertDialog alertDialogTransferOverquota;
    public static ProgressDialog progressDialog = null;

    private Notification.Builder mBuilder;
    private NotificationManager mNotificationManager;

    public static boolean loading = true;
    boolean transferOverquota = false;

    PDFView pdfView;

    public static int REQUEST_CODE_SELECT_LOCAL_FOLDER = 1004;

    int orderGetChildren = MegaApiJava.ORDER_DEFAULT_ASC;

    AppBarLayout appBarLayout;
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
    String pdfFileName;
    boolean inside = false;
    long handle = -1;
    boolean isFolderLink = false;
    public static boolean isScrolling = false;
    public static boolean scroll = false;
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

    private List<ShareInfo> filePreparedInfos;
    ArrayList<Long> handleListM = new ArrayList<Long>();

    int typeCheckLogin = -1;

    static int TYPE_UPLOAD = 0;
    static int TYPE_DOWNLOAD = 1;

    Drawable share;
    Drawable chat;
    Drawable properties;
    Drawable download;

    private String downloadLocationDefaultPath = "";
    private boolean renamed = false;
    private String path;
    private String pathNavigation;

    NodeController nC;
    private android.support.v7.app.AlertDialog downloadConfirmationDialog;
    private DisplayMetrics outMetrics;

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");

        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null){
            log("intent null");
            finish();
            return;
        }

        if (savedInstanceState != null) {
            log("saveInstanceState");
            currentPage = savedInstanceState.getInt("currentPage");
            handle = savedInstanceState.getLong("HANDLE");
            pdfFileName = savedInstanceState.getString("pdfFileName");
            uri = Uri.parse(savedInstanceState.getString("uri"));
            renamed = savedInstanceState.getBoolean("renamed");
        }
        else {
            currentPage = 0;
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null){
            inside = bundle.getBoolean("APP");
            handle = bundle.getLong("HANDLE");
        }
        isFolderLink = intent.getBooleanExtra("isFolderLink", false);
        type = intent.getIntExtra("adapterType", 0);
        path = intent.getStringExtra("path");

        if (!renamed){
            uri = intent.getData();
            if (uri == null){
                log("uri null");
                finish();
                return;
            }
        }

        if (type == Constants.OFFLINE_ADAPTER){
            isOffLine = true;
            pathNavigation = intent.getStringExtra("pathNavigation");
        }
        else {
            isOffLine = false;
            pathNavigation = null;
        }

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        setContentView(R.layout.activity_pdfviewer);

        app = (MegaApplication)getApplication();
        megaApi = app.getMegaApi();

        if(Util.isChatEnabled()){
            megaChatApi = app.getMegaChatApi();
        }

//        if(megaApi==null||megaApi.getRootNode()==null){
//            log("Refresh session - sdk");
//            Intent intentLogin = new Intent(this, LoginActivityLollipop.class);
//            intentLogin.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
//            intentLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            startActivity(intentLogin);
//            finish();
//            return;
//        }
//
//        if(Util.isChatEnabled()){
//            if (megaChatApi == null){
//                megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
//            }
//
//            if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
//                log("Refresh session - karere");
//                Intent intentLogin = new Intent(this, LoginActivityLollipop.class);
//                intentLogin.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
//                intentLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(intentLogin);
//                finish();
//                return;
//            }
//        }

        tB = (Toolbar) findViewById(R.id.toolbar_pdf_viewer);
        if(tB==null){
            log("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_black);
        upArrow.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);
        aB.setHomeAsUpIndicator(upArrow);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        log("Overquota delay: "+megaApi.getBandwidthOverquotaDelay());
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

        log("Add transfer listener");
        megaApi.addTransferListener(this);

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
        progressDialog = new ProgressDialog(PdfViewerActivityLollipop.this);
        progressDialog.setMessage(getString(R.string.general_loading));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (loading) {
                    finish();
                }
            }
        });

        pdfView.setOnClickListener(this);

        setTitle(pdfFileName);
        aB.setTitle(pdfFileName);

        uploadContainer = (RelativeLayout) findViewById(R.id.upload_container_layout_bottom);
        if (!inside) {
            uploadContainer.setVisibility(View.VISIBLE);
        }
        else {
            uploadContainer.setVisibility(View.GONE);
        }

        uploadContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log("onClick uploadContainer");
                typeCheckLogin = TYPE_UPLOAD;
                checkLogin();
            }
        });

        pdfviewerContainer = (RelativeLayout) findViewById(R.id.pdf_viewer_container);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        log("onNewIntent");

        if (intent == null){
            log("intent null");
            finish();
            return;
        }

        if (intent.getBooleanExtra("inside", false)){
            setIntent(intent);
            if (!intent.getBooleanExtra("isUrl", true)){
                isUrl = false;
                uri = intent.getData();
                invalidateOptionsMenu();
            }
        }
        else {
            currentPage = 0;
            isFolderLink = false;
            type = 0;
            inside = false;
            isOffLine = false;
            pathNavigation = null;
            handle = -1;

            uri = intent.getData();
            if (uri == null){
                log("uri null");
                finish();
                return;
            }
            Intent newIntent = new Intent();
            newIntent.setDataAndType(uri, "application/pdf");
            newIntent.setAction(Constants.ACTION_OPEN_FOLDER);
            setIntent(newIntent);
            Display display = getWindowManager().getDefaultDisplay();
            outMetrics = new DisplayMetrics ();
            display.getMetrics(outMetrics);

            setContentView(R.layout.activity_pdfviewer);

            app = (MegaApplication)getApplication();
            megaApi = app.getMegaApi();

            if(Util.isChatEnabled()){
                megaChatApi = app.getMegaChatApi();
            }

            tB = (Toolbar) findViewById(R.id.toolbar_pdf_viewer);
            if(tB==null){
                log("Tb is Null");
                return;
            }

            tB.setVisibility(View.VISIBLE);
            setSupportActionBar(tB);
            aB = getSupportActionBar();
            Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_black);
            upArrow.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);
            aB.setHomeAsUpIndicator(upArrow);
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);

            log("Overquota delay: "+megaApi.getBandwidthOverquotaDelay());
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

            log("Add transfer listener");
            megaApi.addTransferListener(this);

            pdfView = (PDFView) findViewById(R.id.pdfView);

            pdfView.setBackgroundColor(Color.LTGRAY);
            defaultScrollHandle = new DefaultScrollHandle(PdfViewerActivityLollipop.this);

            isUrl = false;
            loadLocalPDF();
            pdfFileName = getFileName(uri);
            pdfView.setOnClickListener(this);

            path = uri.getPath();
            setTitle(pdfFileName);
            aB.setTitle(pdfFileName);

            uploadContainer = (RelativeLayout) findViewById(R.id.upload_container_layout_bottom);
            uploadContainer.setVisibility(View.VISIBLE);
            uploadContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    log("onClick uploadContainer");
                    typeCheckLogin = TYPE_UPLOAD;
                    checkLogin();
                }
            });

            pdfviewerContainer = (RelativeLayout) findViewById(R.id.pdf_viewer_container);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        log("onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putInt("currentPage", currentPage);
        outState.putLong("HANDLE", handle);
        outState.putString("pdfFileName", pdfFileName);
        outState.putString("uri", uri.toString());
        outState.putBoolean("renamed", renamed);
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
            log("onPostExecute");
            try {
                pdfView.fromStream(inputStream)
                        .defaultPage(currentPage)
                        .onPageChange(PdfViewerActivityLollipop.this)
                        .enableAnnotationRendering(true)
                        .onLoad(PdfViewerActivityLollipop.this)
                        .scrollHandle(defaultScrollHandle)
                        .spacing(10) // in dp
                        .onPageError(PdfViewerActivityLollipop.this)
                        .load();
            } catch (Exception e) {

            }
            if (loading && progressDialog!=null && !transferOverquota) {
                progressDialog.show();
            }
        }
    }

    private void loadStreamPDF() {
        log("loadStreamPDF loading: "+loading);
        new LoadPDFStream().execute(uri.toString());
    }

    private void loadLocalPDF() {
        log("loadLocalPDF loading: "+loading);

//        if (loading && progressDialog!=null && !transferOverquota) {
//            progressDialog.show();
//        }
        try {
            pdfView.fromUri(uri)
                    .defaultPage(currentPage)
                    .onPageChange(this)
                    .enableAnnotationRendering(true)
                    .onLoad(this)
                    .scrollHandle(defaultScrollHandle)
                    .spacing(10) // in dp
                    .onPageError(this)
                    .load();
        } catch (Exception e) {

        }
    }

    public void checkLogin(){
        log("checkLogin");

        if (typeCheckLogin == TYPE_UPLOAD) {
            uploadContainer.setVisibility(View.GONE);
        }

        dbH = DatabaseHandler.getDbHandler(this);
        credentials = dbH.getCredentials();
        //Start login process
        if (credentials == null){
            log("No credential to login");
            if (typeCheckLogin == TYPE_UPLOAD) {
                Util.showAlert(this, getString(R.string.alert_not_logged_in), null);
                uploadContainer.setVisibility(View.VISIBLE);
                typeCheckLogin = -1;
            }
        }
        else{
            if (megaApi.getRootNode() == null) {
                if (!MegaApplication.isLoggingIn()) {

                    MegaApplication.setLoggingIn(true);
                    gSession = credentials.getSession();

                    if (Util.isChatEnabled()) {

                        log("onCreate: Chat is ENABLED");
                        if (megaChatApi == null) {
                            megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
                        }

                        int ret = megaChatApi.getInitState();

                        if(ret==0||ret==MegaChatApi.INIT_ERROR){
                            ret = megaChatApi.init(gSession);
                            log("onCreate: result of init ---> " + ret);
                            chatSettings = dbH.getChatSettings();
                            if (ret == MegaChatApi.INIT_NO_CACHE) {
                                log("onCreate: condition ret == MegaChatApi.INIT_NO_CACHE");
                            } else if (ret == MegaChatApi.INIT_ERROR) {

                                log("onCreate: condition ret == MegaChatApi.INIT_ERROR");
                                if (chatSettings == null) {

                                    log("1 - onCreate: ERROR----> Switch OFF chat");
                                    chatSettings = new ChatSettings();
                                    chatSettings.setEnabled(false+"");
                                    dbH.setChatSettings(chatSettings);
                                } else {

                                    log("2 - onCreate: ERROR----> Switch OFF chat");
                                    dbH.setEnabledChat(false + "");
                                }
                                megaChatApi.logout(this);
                            } else {

                                log("onCreate: Chat correctly initialized");
                            }
                        }
                    }

                    log("SESSION: " + gSession);
                    megaApi.fastLogin(gSession, this);
                }
                else{
                    log("Another login is processing");
                }
            }
            else{
                ((MegaApplication) getApplication()).sendSignalPresenceActivity();
                if (typeCheckLogin == TYPE_UPLOAD) {
                    typeCheckLogin = -1;

                    uploadToCloud();
                }
                else if (typeCheckLogin == TYPE_DOWNLOAD){
                    typeCheckLogin = -1;

                    downloadFromCloud();
                }
            }
        }
    }

    public void uploadToCloud(){
        log("uploadToCloud");

        if (filePreparedInfos == null){

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

            FilePrepareTask filePrepareTask = new FilePrepareTask(this);
            filePrepareTask.execute(getIntent());

        }
        else{
            onIntentProcessed(filePreparedInfos);
        }

        if(megaApi.getRootNode()!=null){
            this.backToCloud(megaApi.getRootNode().getHandle());
        }
        else{
            log("Error on logging");
            Util.showAlert(this, getString(R.string.alert_not_logged_in), null);
            uploadContainer.setVisibility(View.VISIBLE);
        }
    }

    public void downloadFromCloud(){
        MegaNode node = megaApi.getNodeByHandle(handle);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Constants.REQUEST_WRITE_STORAGE);

                handleListM.add(node.getHandle());
            }
        }
        ArrayList<Long> handleList = new ArrayList<Long>();
        handleList.add(node.getHandle());

        if(nC==null){
            nC = new NodeController(this);
        }
        nC.prepareForDownload(handleList);
    }

    public void onIntentProcessed(List<ShareInfo> infos) {

        if (statusDialog != null) {
            try {
                statusDialog.dismiss();
            }
            catch(Exception ex){}
        }

        log("intent processed!");

        if (infos == null) {
            log("Error infos is NULL");
            return;
        }
        else {

            MegaNode parentNode = megaApi.getRootNode();
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
        log("backToCloud: "+handle);
        Intent startIntent = new Intent(this, ManagerActivityLollipop.class);
        if(handle!=-1){
            startIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startIntent.setAction(Constants.ACTION_OPEN_FOLDER);
            startIntent.putExtra("PARENT_HANDLE", handle);
        }
        startActivity(startIntent);
    }

    public  void setToolbarVisibilityShow () {
        log("setToolbarVisibilityShow");
        aB.show();
        if(tB != null) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            tB.animate().translationY(0).setDuration(200L).start();
            uploadContainer.animate().translationY(0).setDuration(200L).start();
        }
    }

    public void setToolbarVisibilityHide () {
        log("setToolbarVisibilityHide");
        if(tB != null) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            tB.animate().translationY(-220).setDuration(200L).withEndAction(new Runnable() {
                @Override
                public void run() {
                    aB.hide();
                }
            }).start();
            uploadContainer.animate().translationY(220).setDuration(200L).start();
        }
        else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            aB.hide();
        }
    }

    public void setToolbarVisibility (){
        if (aB != null && aB.isShowing()) {
            setToolbarVisibilityHide();
        } else if (aB != null && !aB.isShowing()){
            setToolbarVisibilityShow();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_pdfviewer, menu);

        shareMenuItem = menu.findItem(R.id.pdfviewer_share);
        share = getResources().getDrawable(R.drawable.ic_social_share_white);
        share.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);
        shareMenuItem.setIcon(share);

        downloadMenuItem = menu.findItem(R.id.pdfviewer_download);
        if (isUrl){
            log("isURL");
            shareMenuItem.setVisible(false);
            downloadMenuItem.setVisible(true);
            download = getResources().getDrawable(R.drawable.ic_download_white);
            download.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);

            downloadMenuItem.setIcon(download);
        }
        else {
            log("NOT isURL");
            downloadMenuItem.setVisible(false);
        }

        chatMenuItem = menu.findItem(R.id.pdfviewer_chat);
        chat = getResources().getDrawable(R.drawable.ic_chat_white);
        chat.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);
        chatMenuItem.setIcon(chat);

        propertiesMenuItem = menu.findItem(R.id.pdfviewer_properties);
        properties = getResources().getDrawable(R.drawable.info_ic_white);
        properties.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);
        propertiesMenuItem.setIcon(properties);

        if (inside){
            propertiesMenuItem.setVisible(true);
            if(Util.isChatEnabled()){
                if (isOffLine){
                    chatMenuItem.setVisible(false);
                }
                else{
                    chatMenuItem.setVisible(true);
                }
            }
            else{
                chatMenuItem.setVisible(false);
            }
        }
        else {
            propertiesMenuItem.setVisible(false);
            chatMenuItem.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        int id = item.getItemId();
        switch(id) {
            case android.R.id.home: {
                onBackPressed();
                finish();
                break;
            }
            case R.id.pdfviewer_share: {
                intentToSendFile();
                break;
            }
            case R.id.pdfviewer_download: {
                downloadFile();
                break;
            }
            case R.id.pdfviewer_chat: {
                long[] longArray = new long[1];

                longArray[0] = handle;

                Intent i = new Intent(PdfViewerActivityLollipop.this, ChatExplorerActivity.class);
                i.putExtra("NODE_HANDLES", longArray);
                startActivityForResult(i, REQUEST_CODE_SELECT_CHAT);
                break;
            }
            case R.id.pdfviewer_properties: {
                Intent i = new Intent(this, FileInfoActivityLollipop.class);
                if (isOffLine){
                    i.putExtra("name", pdfFileName);
                    i.putExtra("imageId", MimeTypeMime.typeForName(pdfFileName).getIconResourceId());
                    i.putExtra("adapterType", Constants.OFFLINE_ADAPTER);
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
                    i.putExtra("imageId", MimeTypeMime.typeForName(node.getName()).getIconResourceId());
                    i.putExtra("name", node.getName());
                }
                startActivity(i);
                renamed = false;
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void intentToSendFile(){
        log("intentToSendFile");

        if(uri!=null){
            if (!isUrl){
                Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setType("application/pdf");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    log("Use provider to share");
                    share.putExtra(Intent.EXTRA_STREAM, Uri.parse(uri.toString()));
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                else{
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                }
                startActivity(Intent.createChooser(share, getString(R.string.context_share)));
            }
            else {
                Snackbar.make(pdfviewerContainer, getString(R.string.not_download), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    public void downloadFile() {

        typeCheckLogin = TYPE_DOWNLOAD;
        checkLogin();
    }

    public void updateFile (){
        MegaNode file = null;
        if (pdfFileName != null && handle != -1 ) {
            file = megaApi.getNodeByHandle(handle);
            if (file != null){
                log("Pdf File: "+pdfFileName+" node file: "+file.getName());
                if (!pdfFileName.equals(file.getName())) {
                    log("updateFile");

                    pdfFileName = file.getName();
                    if (aB != null){
                        tB = (Toolbar) findViewById(R.id.toolbar_pdf_viewer);
                        if(tB==null){
                            log("Tb is Null");
                            return;
                        }
                        tB.setVisibility(View.VISIBLE);
                        setSupportActionBar(tB);
                        aB = getSupportActionBar();
                    }
                    aB.setTitle(pdfFileName);
                    setTitle(pdfFileName);
                    invalidateOptionsMenu();

                    if (megaApi == null){
                        MegaApplication app = (MegaApplication)getApplication();
                        megaApi = app.getMegaApi();
                        megaApi.addTransferListener(this);
                    }
                    if (megaApi.httpServerIsRunning() == 0) {
                        megaApi.httpServerStart();
                    }

                    ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                    ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                    activityManager.getMemoryInfo(mi);

                    if(mi.totalMem>Constants.BUFFER_COMP){
                        log("Total mem: "+mi.totalMem+" allocate 32 MB");
                        megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB);
                    }
                    else{
                        log("Total mem: "+mi.totalMem+" allocate 16 MB");
                        megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB);
                    }

                    String url = megaApi.httpServerGetLocalLink(file);
                    getDownloadLocation();
                    String localPath = Util.getLocalFile(this, file.getName(), file.getSize(), downloadLocationDefaultPath);

                    if (localPath != null){
                        File mediaFile = new File(localPath);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && prefs.getStorageDownloadLocation().contains(Environment.getExternalStorageDirectory().getPath())) {
                            uri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", mediaFile);
                        }
                        else{
                            uri = Uri.fromFile(mediaFile);
                        }
                    }
                    else {
                        uri = Uri.parse(url);
                    }
                    renamed = true;
                }
            }
        }
    }

    public void getDownloadLocation(){
        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(getApplicationContext());
        }

        prefs = dbH.getPreferences();
        if (prefs != null){
            if (prefs.getStorageAskAlways() != null){
                if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
                    if (prefs.getStorageDownloadLocation() != null){
                        if (prefs.getStorageDownloadLocation().compareTo("") != 0){
                            downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        log("-------------------onActivityResult " + requestCode + "____" + resultCode);
        if (intent == null) {
            return;
        }

        if (requestCode == REQUEST_CODE_SELECT_CHAT && resultCode == RESULT_OK){
            long[] chatHandles = intent.getLongArrayExtra("SELECTED_CHATS");
            log("Send to "+chatHandles.length+" chats");

            long[] nodeHandles = intent.getLongArrayExtra("NODE_HANDLES");
            log("Send "+nodeHandles.length+" nodes");

            countChat = chatHandles.length;
            if (megaChatApi != null) {
                if(countChat==1){
                    megaChatApi.attachNode(chatHandles[0], nodeHandles[0], this);
                }
                else if(countChat>1){

                    for(int i=0; i<chatHandles.length; i++){
                        megaChatApi.attachNode(chatHandles[i], nodeHandles[0], this);
                    }
                }
            }
            else{
                log("megaChatApi is Null - cannot attach nodes");
            }
        }
        else if (requestCode == REQUEST_CODE_SELECT_LOCAL_FOLDER && resultCode == RESULT_OK) {
            log("local folder selected");
            String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            String url = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_URL);
            long size = intent.getLongExtra(FileStorageActivityLollipop.EXTRA_SIZE, 0);
            long[] hashes = intent.getLongArrayExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES);
            log("URL: " + url + "___SIZE: " + size);

            if(nC==null){
                nC = new NodeController(this);
            }
            nC.checkSizeBeforeDownload(parentPath, url, size, hashes);
        }
    }

    public void establishScroll() {
        if (isScrolling && !scroll) {
            scroll = true;
            setToolbarVisibilityHide();
        }
        else if (!isScrolling){
            scroll = false;
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        currentPage = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));

        establishScroll();
    }

    @Override
    public void onPageError(int page, Throwable t) {
        log("Cannot load page " + page);
    }

    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        log("title = " + meta.getTitle());
        log("author = " + meta.getAuthor());
        log("subject = " + meta.getSubject());
        log("keywords = " + meta.getKeywords());
        log("creator = " + meta.getCreator());
        log("producer = " + meta.getProducer());
        log("creationDate = " + meta.getCreationDate());
        log("modDate = " + meta.getModDate());
        printBookmarksTree(pdfView.getTableOfContents(), "-");
    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            log(String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    public static void log(String log) {
        Util.log("PdfViewerActivityLollipop", log);
    }

    @Override
    public void onClick(View v) {
        log("onClick");
        setToolbarVisibility();
        defaultScrollHandle.hideDelayed();
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish");
        if (request.getType() == MegaRequest.TYPE_LOGIN){

            if (e.getErrorCode() != MegaError.API_OK) {

                MegaApplication.setLoggingIn(false);

                DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
                dbH.clearCredentials();
                if (dbH.getPreferences() != null){
                    dbH.clearPreferences();
                    dbH.setFirstTime(false);
                }
                typeCheckLogin = -1;
            }
            else{
                //LOGIN OK

                gSession = megaApi.dumpSession();
                credentials = new UserCredentials(lastEmail, gSession, "", "", "");

                DatabaseHandler dbH = DatabaseHandler.getDbHandler(getApplicationContext());
                dbH.clearCredentials();

                log("Logged in: " + gSession);

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

                chatSettings = dbH.getChatSettings();
                if(chatSettings!=null) {

                    boolean chatEnabled = Boolean.parseBoolean(chatSettings.getEnabled());
                    if(chatEnabled){

                        log("Chat enabled-->connect");
                        if((megaChatApi.getInitState()!=MegaChatApi.INIT_ERROR)){
                            log("Connection goes!!!");
                            megaChatApi.connect(this);
                        }
                        else{
                            log("Not launch connect: "+megaChatApi.getInitState());
                        }
                        MegaApplication.setLoggingIn(false);
                        if (typeCheckLogin == TYPE_UPLOAD){
                            typeCheckLogin = -1;
                            uploadToCloud();
                        }
                        else if (typeCheckLogin == TYPE_DOWNLOAD){
                            typeCheckLogin = -1;
                            downloadFromCloud();
                        }
                    }
                    else{

                        log("Chat NOT enabled - readyToManager");
                        MegaApplication.setLoggingIn(false);
                        if (typeCheckLogin == TYPE_UPLOAD){
                            typeCheckLogin = -1;
                            uploadToCloud();
                        }
                        else if (typeCheckLogin == TYPE_DOWNLOAD){
                            typeCheckLogin = -1;
                            downloadFromCloud();
                        }
                    }
                }
                else{
                    log("chatSettings NULL - readyToManager");
                    MegaApplication.setLoggingIn(false);
                    if (typeCheckLogin == TYPE_UPLOAD){
                        typeCheckLogin = -1;
                        uploadToCloud();
                    }
                    else if (typeCheckLogin == TYPE_DOWNLOAD){
                        typeCheckLogin = -1;
                        downloadFromCloud();
                    }
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        log("onRequestFinish - MegaChatApi");

        if (request.getType() == MegaChatRequest.TYPE_CONNECT){
            MegaApplication.setLoggingIn(false);
            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                log("Connected to chat!");
            }
            else{
                log("ERROR WHEN CONNECTING " + e.getErrorString());
            }
        }
        else if(request.getType() == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE){

            if(e.getErrorCode()==MegaChatError.ERROR_OK){
                log("File sent correctly");
                successSent++;
            }
            else{
                log("File NOT sent: "+e.getErrorCode()+"___"+e.getErrorString());
                errorSent++;
            }

            if(countChat==errorSent+successSent){
                if(successSent==countChat){
                    freeColorFilter();
                    if(countChat==1){
                        long handle = request.getChatHandle();
                        MegaChatListItem chatItem = megaChatApi.getChatListItem(handle);
                        if(chatItem!=null){
                            Intent intent = new Intent(this, ManagerActivityLollipop.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.setAction(Constants.ACTION_CHAT_NOTIFICATION_MESSAGE);
                            intent.putExtra("CHAT_ID", handle);
                            startActivity(intent);
                            finish();
                        }
                    }
                    else{
                        showSnackbar(getString(R.string.success_attaching_node_from_cloud_chats, countChat));
                    }
                }
                else if(errorSent==countChat){
                    showSnackbar(getString(R.string.error_attaching_node_from_cloud));
                }
                else{
                    showSnackbar(getString(R.string.error_attaching_node_from_cloud_chats));
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
        log("onStop");

        freeColorFilter();
    }

    void freeColorFilter(){
        if (chat != null) {
            chat.setColorFilter(null);
        }
        if (download != null){
            download.setColorFilter(null);
        }
        if (properties != null){
            properties.setColorFilter(null);
        }
        if (share != null) {
            share.setColorFilter(null);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        log("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume");

        updateFile();

        if (chat != null) {
            chat.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);
        }
        if (download != null){
            download.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);
        }
        if (properties != null){
            properties.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);
        }
        if (share != null) {
            share.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause");
    }

    @Override
    protected void onDestroy() {
        log("onDestroy()");

        if (megaApi != null) {
            megaApi.removeTransferListener(this);
        }
        freeColorFilter();

        super.onDestroy();
    }

    public void showTransferOverquotaDialog(){
        log("showTransferOverquotaDialog");

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
                if (progressDialog != null) {
                    progressDialog.hide();
                }
            }
        });

        continueButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                alertDialogTransferOverquota.dismiss();
                transferOverquota = false;
                if (loading && progressDialog != null && !transferOverquota) {
                    progressDialog.show();
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
        log("showUpgradeAccount");
        Intent upgradeIntent = new Intent(this, ManagerActivityLollipop.class);
        upgradeIntent.setAction(Constants.ACTION_SHOW_UPGRADE_ACCOUNT);
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
            log("API_EOVERQUOTA error!!");

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

    @Override
    public boolean onTransferData(MegaApiJava api, MegaTransfer transfer, byte[] buffer) {
        return false;
    }

    public void showSnackbar(String s){
        log("showSnackbar");
        Snackbar snackbar = Snackbar.make(pdfviewerContainer, s, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    public void openAdvancedDevices (long handleToDownload){
        log("openAdvancedDevices");
//		handleToDownload = handle;
        String externalPath = Util.getExternalCardPath();

        if(externalPath!=null){
            log("ExternalPath for advancedDevices: "+externalPath);
            MegaNode node = megaApi.getNodeByHandle(handleToDownload);
            if(node!=null){

//				File newFile =  new File(externalPath+"/"+node.getName());
                File newFile =  new File(node.getName());
                log("File: "+newFile.getPath());
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

                // Filter to only show results that can be "opened", such as
                // a file (as opposed to a list of contacts or timezones).
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // Create a file with the requested MIME type.
                String mimeType = MimeTypeList.getMimeType(newFile);
                log("Mimetype: "+mimeType);
                intent.setType(mimeType);
                intent.putExtra(Intent.EXTRA_TITLE, node.getName());
                intent.putExtra("handleToDownload", handleToDownload);
                try{
                    startActivityForResult(intent, Constants.WRITE_SD_CARD_REQUEST_CODE);
                }
                catch(Exception e){
                    log("Exception in External SDCARD");
                    Environment.getExternalStorageDirectory();
                    Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        }
        else{
            log("No external SD card");
            Environment.getExternalStorageDirectory();
            Toast toast = Toast.makeText(this, getString(R.string.no_external_SD_card_detected), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void showSnackbarNotSpace(){
        log("showSnackbarNotSpace");
        Snackbar mySnackbar = Snackbar.make(pdfviewerContainer, R.string.error_not_enough_free_space, Snackbar.LENGTH_LONG);
        mySnackbar.setAction("Settings", new SnackbarNavigateOption(this));
        mySnackbar.show();
    }

    public void askSizeConfirmationBeforeDownload(String parentPath, String url, long size, long [] hashes){
        log("askSizeConfirmationBeforeDownload");

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        final long sizeC=size;

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
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
                        if(nC==null){
                            nC = new NodeController(PdfViewerActivityLollipop.this);
                        }
                        nC.checkInstalledAppBeforeDownload(parentPathC, urlC, sizeC, hashesC);
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

    public void askConfirmationNoAppInstaledBeforeDownload (String parentPath, String url, long size, long [] hashes, String nodeToDownload){
        log("askConfirmationNoAppInstaledBeforeDownload");

        final String parentPathC = parentPath;
        final String urlC = url;
        final long [] hashesC = hashes;
        final long sizeC=size;

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
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
        builder.setMessage(getString(R.string.alert_no_app, nodeToDownload));
        builder.setPositiveButton(getString(R.string.general_download),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(dontShowAgain.isChecked()){
                            dbH.setAttrAskNoAppDownload("false");
                        }
                        if(nC==null){
                            nC = new NodeController(PdfViewerActivityLollipop.this);
                        }
                        nC.download(parentPathC, urlC, sizeC, hashesC);
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
}
