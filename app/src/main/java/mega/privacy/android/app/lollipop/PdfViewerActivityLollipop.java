package mega.privacy.android.app.lollipop;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.shockwave.pdfium.PdfDocument;

import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.tasks.FilePrepareTask;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

public class PdfViewerActivityLollipop extends PinActivityLollipop implements OnPageChangeListener, OnLoadCompleteListener, OnPageErrorListener, View.OnClickListener, MegaRequestListenerInterface, MegaChatRequestListenerInterface {

    MegaApplication app = null;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    PDFView pdfView;

    AppBarLayout appBarLayout;
    Toolbar tB;
    public ActionBar aB;

    private String gSession;
    UserCredentials credentials;
    private String lastEmail;
    DatabaseHandler dbH = null;
    ChatSettings chatSettings;

    Uri uri;
    String pdfFileName;
    int pageNumber = 0;
    boolean inside = false;

    public RelativeLayout uploadContainer;
    RelativeLayout pdfviewerContainer;

    ProgressDialog statusDialog;

    private MenuItem shareMenuItem;

    private List<ShareInfo> filePreparedInfos;

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

        Bundle bundle = getIntent().getExtras();
        if (bundle != null){
            inside = bundle.getBoolean("APP");
        }

        uri = intent.getData();
        if (uri == null){
            log("uri null");
            finish();
            return;
        }

        app = (MegaApplication)getApplication();
        megaApi = app.getMegaApi();

        setContentView(R.layout.activity_pdfviewer);

        appBarLayout = (AppBarLayout) findViewById(R.id.app_bar);

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

        pdfView = (PDFView) findViewById(R.id.pdfView);

        pdfView.setBackgroundColor(Color.LTGRAY);
        pdfFileName = getFileName(uri);

        try {
            pdfView.fromUri(uri)
                    .defaultPage(pageNumber)
                    .onPageChange(this)
                    .enableAnnotationRendering(true)
                    .onLoad(this)
                    .scrollHandle(new DefaultScrollHandle(this))
                    .spacing(10) // in dp
                    .onPageError(this)
                    .load();
        } catch (Exception e) {

        }

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
                checkLogin();
            }
        });
    }

    public void checkLogin(){
        log("uploadToCloud");

        uploadContainer.setVisibility(View.GONE);

        dbH = DatabaseHandler.getDbHandler(this);
        credentials = dbH.getCredentials();
        //Start login process
        if (credentials == null){
            log("No credential to login");
            Util.showAlert(this, getString(R.string.alert_not_logged_in), null);
            uploadContainer.setVisibility(View.VISIBLE);
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
                        int ret = megaChatApi.init(gSession);
                        log("onCreate: result of init ---> " + ret);
                        chatSettings = dbH.getChatSettings();
                        if (ret == MegaChatApi.INIT_NO_CACHE) {
                            log("onCreate: condition ret == MegaChatApi.INIT_NO_CACHE");
                            megaApi.invalidateCache();

                        } else if (ret == MegaChatApi.INIT_ERROR) {

                            log("onCreate: condition ret == MegaChatApi.INIT_ERROR");
                            if (chatSettings == null) {

                                log("1 - onCreate: ERROR----> Switch OFF chat");
                                chatSettings = new ChatSettings(false + "", true + "", "", true + "");
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

                    log("SESSION: " + gSession);
                    megaApi.fastLogin(gSession, this);
                }
            }
            else{
                ((MegaApplication) getApplication()).sendSignalPresenceActivity();
                uploadToCloud();
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
            startIntent.setAction(Constants.ACTION_OPEN_FOLDER);
            startIntent.putExtra("PARENT_HANDLE", handle);
        }
        startActivity(startIntent);
    }

    public  void setToolbarVisibilityShow () {
        aB.show();
        tB.animate().translationY(0).setDuration(200L).start();
        uploadContainer.animate().translationY(0).setDuration(200L).start();
    }

    public void setToolbarVisibilityHide () {
        log("setToolbarVisibilityHide");

        tB.animate().translationY(-tB.getBottom()).setDuration(200L).withEndAction(new Runnable() {
            @Override
            public void run() {
                aB.hide();
            }
        }).start();
        uploadContainer.animate().translationY(220).setDuration(200L).start();
    }

    public void setToolbarVisibility (){
        log("setToolbarVisibility");
        if (aB.isShowing()) {
            //aB.hide();
            tB.animate().translationY(-tB.getBottom()).setDuration(200L).withEndAction(new Runnable() {
                @Override
                public void run() {
                    aB.hide();
                }
            }).start();
            uploadContainer.animate().translationY(220).setDuration(200L).start();
        } else {
            aB.show();
            tB.animate().translationY(0).setDuration(200L).start();
            uploadContainer.animate().translationY(0).setDuration(200L).start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_pdfviewer, menu);

        shareMenuItem = menu.findItem(R.id.pdfviewer_share);

        Drawable share = getResources().getDrawable(R.drawable.ic_social_share_white);
        share.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);

        shareMenuItem.setIcon(share);

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
                break;
            }
            case R.id.pdfviewer_share: {
                intentToSendFile(uri);
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void intentToSendFile(Uri uri){
        log("intentToSendFile");

        if(uri!=null){
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
            startActivity(Intent.createChooser(share, getString(R.string.context_share_image)));
        }
        else{
            pdfviewerContainer = (RelativeLayout) findViewById(R.id.pdf_viewer_container);
            Snackbar.make(pdfviewerContainer, getString(R.string.pdf_viewer_not_download), Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
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
                        megaChatApi.connect(this);
                        MegaApplication.setLoggingIn(false);
                        uploadToCloud();
                    }
                    else{

                        log("Chat NOT enabled - readyToManager");
                        MegaApplication.setLoggingIn(false);
                        uploadToCloud();
                    }
                }
                else{
                    log("chatSettings NULL - readyToManager");
                    MegaApplication.setLoggingIn(false);
                    uploadToCloud();
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
                MegaApplication.setChatConnection(true);
            }
            else{
                log("ERROR WHEN CONNECTING " + e.getErrorString());
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }
}
