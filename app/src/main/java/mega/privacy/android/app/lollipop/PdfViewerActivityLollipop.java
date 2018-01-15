package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.os.StatFs;
import android.provider.OpenableColumns;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.UserCredentials;
import mega.privacy.android.app.lollipop.megachat.ChatSettings;
import mega.privacy.android.app.lollipop.tasks.FilePrepareTask;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
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
    MegaPreferences prefs = null;

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
    int pageNumber = 0;
    boolean inside = false;
    long handle;
    boolean isFolderLink = false;
    public static boolean isScrolling = false;
    public static boolean scroll = false;

    public RelativeLayout uploadContainer;
    RelativeLayout pdfviewerContainer;

    ProgressDialog statusDialog;

    private MenuItem shareMenuItem;
    private MenuItem downloadMenuItem;

    private List<ShareInfo> filePreparedInfos;
    ArrayList<Long> handleListM = new ArrayList<Long>();

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
            handle  = bundle.getLong("HANDLE");
        }
        isFolderLink = intent.getBooleanExtra("isFolderLink", false);

        uri = intent.getData();
        if (uri == null){
            log("uri null");
            finish();
            return;
        }

        app = (MegaApplication)getApplication();
        megaApi = app.getMegaApi();
//        if(megaApi==null||megaApi.getRootNode()==null){
//            log("Refresh session - sdk");
//            Intent intentLogin = new Intent(this, LoginActivityLollipop.class);
//            intentLogin.putExtra("visibleFragment", Constants. LOGIN_FRAGMENT);
//            intentLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            startActivity(intentLogin);
//            finish();
//            return;
//        }
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
        setContentView(R.layout.activity_pdfviewer);

        //appBarLayout = (AppBarLayout) findViewById(R.id.app_bar_pdfviewer);

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
        defaultScrollHandle = new DefaultScrollHandle(PdfViewerActivityLollipop.this);

        if (uri.toString().contains("http://")){
            isUrl = true;
            loadStreamPDF();
        }
        else {
            isUrl = false;
            loadLocalPDF();
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

        pdfviewerContainer = (RelativeLayout) findViewById(R.id.pdf_viewer_container);
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
            try {
                pdfView.fromStream(inputStream)
                        .defaultPage(pageNumber)
                        .onPageChange(PdfViewerActivityLollipop.this)
                        .enableAnnotationRendering(true)
                        .onLoad(PdfViewerActivityLollipop.this)
                        .scrollHandle(defaultScrollHandle)
                        .spacing(10) // in dp
                        .onPageError(PdfViewerActivityLollipop.this)
                        .load();
            } catch (Exception e) {

            }
        }
    }

    private void loadStreamPDF() {
        new LoadPDFStream().execute(uri.toString());
    }

    private void loadLocalPDF() {
        try {
            pdfView.fromUri(uri)
                    .defaultPage(pageNumber)
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
                else{
                    log("Another login is processing");
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

        Drawable share = getResources().getDrawable(R.drawable.ic_social_share_white);
        share.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);

        shareMenuItem.setIcon(share);

        downloadMenuItem = menu.findItem(R.id.pdfviewer_download);
        if (isUrl){
            log("isURL");
            downloadMenuItem.setVisible(true);
            Drawable download = getResources().getDrawable(R.drawable.ic_download_white);
            download.setColorFilter(getResources().getColor(R.color.lollipop_primary_color), PorterDuff.Mode.SRC_ATOP);

            downloadMenuItem.setIcon(download);
        }
        else {

            log("NOT isURL");
            downloadMenuItem.setVisible(false);
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
                break;
            }
            case R.id.pdfviewer_share: {
                intentToSendFile();
                break;
            }
            case R.id.pdfviewer_download: {
                downloadFile();
                finish();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void intentToSendFile(){
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
            Snackbar.make(pdfviewerContainer, getString(R.string.pdf_viewer_not_download), Snackbar.LENGTH_LONG).show();
        }
    }

    public void downloadFile() {

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
        downloadNode(handleList);
    }

    @SuppressLint("NewApi")
    public void downloadNode(ArrayList<Long> handleList){

        long size = 0;
        long[] hashes = new long[handleList.size()];
        for (int i=0;i<handleList.size();i++){
            hashes[i] = handleList.get(i);
            size += megaApi.getNodeByHandle(hashes[i]).getSize();
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(getApplicationContext());
        }

        boolean askMe = true;
        String downloadLocationDefaultPath = "";
        prefs = dbH.getPreferences();
        if (prefs != null){
            if (prefs.getStorageAskAlways() != null){
                if (!Boolean.parseBoolean(prefs.getStorageAskAlways())){
                    if (prefs.getStorageDownloadLocation() != null){
                        if (prefs.getStorageDownloadLocation().compareTo("") != 0){
                            askMe = false;
                            downloadLocationDefaultPath = prefs.getStorageDownloadLocation();
                        }
                    }
                }
            }
        }

        if (askMe){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                File[] fs = getExternalFilesDirs(null);
                if (fs.length > 1){
                    if (fs[1] == null){
                        Intent intent = new Intent(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
                        intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
                        intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
                        intent.setClass(this, FileStorageActivityLollipop.class);
                        intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
                        startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
                    }
                    else{
                        Dialog downloadLocationDialog;
                        String[] sdCardOptions = getResources().getStringArray(R.array.settings_storage_download_location_array);
                        AlertDialog.Builder b=new AlertDialog.Builder(this);

                        b.setTitle(getResources().getString(R.string.settings_storage_download_location));
                        final long sizeFinal = size;
                        final long[] hashesFinal = new long[hashes.length];
                        for (int i=0; i< hashes.length; i++){
                            hashesFinal[i] = hashes[i];
                        }

                        b.setItems(sdCardOptions, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch(which){
                                    case 0:{
                                        Intent intent = new Intent(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
                                        intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
                                        intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, sizeFinal);
                                        intent.setClass(getApplicationContext(), FileStorageActivityLollipop.class);
                                        intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashesFinal);
                                        startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
                                        break;
                                    }
                                    case 1:{
                                        File[] fs = getExternalFilesDirs(null);
                                        if (fs.length > 1){
                                            String path = fs[1].getAbsolutePath();
                                            File defaultPathF = new File(path);
                                            defaultPathF.mkdirs();
                                            Toast.makeText(getApplicationContext(), getString(R.string.general_download) + ": "  + defaultPathF.getAbsolutePath() , Toast.LENGTH_LONG).show();
                                            downloadTo(path, null, sizeFinal, hashesFinal);
                                        }
                                        break;
                                    }
                                }
                            }
                        });
                        b.setNegativeButton(getResources().getString(R.string.general_cancel), new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        downloadLocationDialog = b.create();
                        downloadLocationDialog.show();
                    }
                }
                else{
                    Intent intent = new Intent(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
                    intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
                    intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
                    intent.setClass(this, FileStorageActivityLollipop.class);
                    intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
                    startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
                }
            }
            else{
                Intent intent = new Intent(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
                intent.putExtra(FileStorageActivityLollipop.EXTRA_BUTTON_PREFIX, getString(R.string.context_download_to));
                intent.putExtra(FileStorageActivityLollipop.EXTRA_SIZE, size);
                intent.setClass(this, FileStorageActivityLollipop.class);
                intent.putExtra(FileStorageActivityLollipop.EXTRA_DOCUMENT_HASHES, hashes);
                startActivityForResult(intent, REQUEST_CODE_SELECT_LOCAL_FOLDER);
            }
        }
        else{
            downloadTo(downloadLocationDefaultPath, null, size, hashes);
        }
    }

    public void downloadTo(String parentPath, String url, long size, long [] hashes){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
            if (!hasStoragePermission) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        Constants.REQUEST_WRITE_STORAGE);
            }
        }

        double availableFreeSpace = Double.MAX_VALUE;
        try{
            StatFs stat = new StatFs(parentPath);
            availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
        }
        catch(Exception ex){}


        if (hashes == null){
            if(url != null) {
                if(availableFreeSpace < size) {
                    Snackbar.make(pdfviewerContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
                    return;
                }
                Intent service = new Intent(this, DownloadService.class);
                service.putExtra(DownloadService.EXTRA_URL, url);
                service.putExtra(DownloadService.EXTRA_SIZE, size);
                service.putExtra(DownloadService.EXTRA_PATH, parentPath);
                service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink);
                startService(service);
            }
        }
        else{
            if(hashes.length == 1){
                MegaNode tempNode = megaApi.getNodeByHandle(hashes[0]);
                if((tempNode != null) && tempNode.getType() == MegaNode.TYPE_FILE){
                    log("ISFILE");
                    String localPath = Util.getLocalFile(this, tempNode.getName(), tempNode.getSize(), parentPath);
                    if(localPath != null){
                        try {
                            Util.copyFile(new File(localPath), new File(parentPath, tempNode.getName()));
                        }
                        catch(Exception e) {}

                        try {

                            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                viewIntent.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                            } else {
                                viewIntent.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                            }
                            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            if (MegaApiUtils.isIntentAvailable(this, viewIntent))
                                startActivity(viewIntent);
                            else {
                                Intent intentShare = new Intent(Intent.ACTION_SEND);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    intentShare.setDataAndType(FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                } else {
                                    intentShare.setDataAndType(Uri.fromFile(new File(localPath)), MimeTypeList.typeForName(tempNode.getName()).getType());
                                }
                                intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                if (MegaApiUtils.isIntentAvailable(this, intentShare))
                                    startActivity(intentShare);
                                String message = getString(R.string.general_already_downloaded) + ": " + localPath;
                                Snackbar.make(pdfviewerContainer, message, Snackbar.LENGTH_LONG).show();
                            }
                        }
                        catch (Exception e){
                            String message = getString(R.string.general_already_downloaded) + ": " + localPath;
                            Snackbar.make(pdfviewerContainer, message, Snackbar.LENGTH_LONG).show();
                        }
                        return;
                    }
                }
            }

            for (long hash : hashes) {
                MegaNode node = megaApi.getNodeByHandle(hash);
                if(node != null){
                    Map<MegaNode, String> dlFiles = new HashMap<MegaNode, String>();
                    if (node.getType() == MegaNode.TYPE_FOLDER) {
                        getDlList(dlFiles, node, new File(parentPath, new String(node.getName())));
                    } else {
                        dlFiles.put(node, parentPath);
                    }

                    for (MegaNode document : dlFiles.keySet()) {

                        String path = dlFiles.get(document);

                        if(availableFreeSpace < document.getSize()){
                            Snackbar.make(pdfviewerContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
                            continue;
                        }

                        Intent service = new Intent(this, DownloadService.class);
                        service.putExtra(DownloadService.EXTRA_HASH, document.getHandle());
                        service.putExtra(DownloadService.EXTRA_URL, url);
                        service.putExtra(DownloadService.EXTRA_SIZE, document.getSize());
                        service.putExtra(DownloadService.EXTRA_PATH, path);
                        service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink);
                        startService(service);
                    }
                }
                else if(url != null) {
                    if(availableFreeSpace < size) {
                        Snackbar.make(pdfviewerContainer, getString(R.string.error_not_enough_free_space), Snackbar.LENGTH_LONG).show();
                        continue;
                    }

                    Intent service = new Intent(this, DownloadService.class);
                    service.putExtra(DownloadService.EXTRA_HASH, hash);
                    service.putExtra(DownloadService.EXTRA_URL, url);
                    service.putExtra(DownloadService.EXTRA_SIZE, size);
                    service.putExtra(DownloadService.EXTRA_PATH, parentPath);
                    service.putExtra(DownloadService.EXTRA_FOLDER_LINK, isFolderLink);
                    startService(service);
                }
                else {
                    log("node not found");
                }
            }
        }
    }

    private void getDlList(Map<MegaNode, String> dlFiles, MegaNode parent, File folder) {

        if (megaApi.getRootNode() == null)
            return;

        folder.mkdir();
        ArrayList<MegaNode> nodeList = megaApi.getChildren(parent, orderGetChildren);
        for(int i=0; i<nodeList.size(); i++){
            MegaNode document = nodeList.get(i);
            if (document.getType() == MegaNode.TYPE_FOLDER) {
                File subfolder = new File(folder, new String(document.getName()));
                getDlList(dlFiles, document, subfolder);
            }
            else {
                dlFiles.put(document, folder.getAbsolutePath());
            }
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
        pageNumber = page;
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
