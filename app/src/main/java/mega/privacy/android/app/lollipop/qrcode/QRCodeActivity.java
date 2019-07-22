package mega.privacy.android.app.lollipop.qrcode;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.QRCodeSaveBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.CacheFolderManager.buildQrFile;

public class QRCodeActivity extends PinActivityLollipop implements MegaRequestListenerInterface{

    private static int REQUEST_DOWNLOAD_FOLDER = 1000;
    private final int MY_PERMISSIONS_REQUEST_CAMERA = 1010;

    private Toolbar tB;
    private ActionBar aB;

    private MenuItem shareMenuItem;
    private MenuItem saveMenuItem;
    private MenuItem settingsMenuItem;
    private MenuItem resetQRMenuItem;
    private MenuItem deleteQRMenuItem;

    private TabLayout tabLayoutQRCode;
    private ViewPager viewPagerQRCode;

    private ScanCodeFragment scanCodeFragment;
    private MyCodeFragment myCodeFragment;

    private QRCodePageAdapter qrCodePageAdapter;

    private DrawerLayout drawerLayout;

    private int qrCodeFragment;

    MegaApiAndroid megaApi;

    private boolean contacts = false;
    private boolean inviteContacts = false;

    DisplayMetrics outMetrics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

        if (savedInstanceState != null) {
            contacts = savedInstanceState.getBoolean("contacts", false);
            inviteContacts = savedInstanceState.getBoolean("inviteContacts", false);
        }
        else {
            contacts = getIntent().getBooleanExtra("contacts", false);
            inviteContacts = getIntent().getBooleanExtra("inviteContacts", false);
        }

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        setContentView(R.layout.activity_qr_code);

        tB = (Toolbar) findViewById(R.id.toolbar);
        if(tB==null){
            log("Tb is Null");
            return;
        }

        if (megaApi == null) {
            megaApi = ((MegaApplication) getApplication()).getMegaApi();
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        tB.setTitle(getString(R.string.section_qr_code));

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        qrCodePageAdapter =new QRCodePageAdapter(getSupportFragmentManager(),this);

        tabLayoutQRCode =  (TabLayout) findViewById(R.id.sliding_tabs_qr_code);
        viewPagerQRCode = (ViewPager) findViewById(R.id.qr_code_tabs_pager);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }else {
            initActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initActivity();
                }else{
                    this.finish();
                }
                return;
            }
        }
    }

    void initActivity(){
        viewPagerQRCode.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                supportInvalidateOptionsMenu();

                if (position == 0) {
                    qrCodeFragment = 0;
                    myCodeFragment = (MyCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 0);
                }
                else {
                    qrCodeFragment = 1;
                    scanCodeFragment = (ScanCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 1);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        viewPagerQRCode.setAdapter(qrCodePageAdapter);
        tabLayoutQRCode.setupWithViewPager(viewPagerQRCode);
        if (contacts || inviteContacts){
            viewPagerQRCode.setCurrentItem(1);
        }
        else {
            viewPagerQRCode.setCurrentItem(0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("contacts", contacts);
        outState.putBoolean("inviteContacts", inviteContacts);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_qr_code, menu);

        shareMenuItem = menu.findItem(R.id.qr_code_share);
        saveMenuItem = menu.findItem(R.id.qr_code_save);
        settingsMenuItem = menu.findItem(R.id.qr_code_settings);
        resetQRMenuItem = menu.findItem(R.id.qr_code_reset);
        deleteQRMenuItem = menu.findItem(R.id.qr_code_delete);

        switch (qrCodeFragment) {
            case 0: {
                shareMenuItem.setVisible(true);
                saveMenuItem.setVisible(true);
                settingsMenuItem.setVisible(true);
                resetQRMenuItem.setVisible(true);
                deleteQRMenuItem.setVisible(true);
                break;
            }
            case 1: {
                shareMenuItem.setVisible(false);
                saveMenuItem.setVisible(false);
                settingsMenuItem.setVisible(false);
                resetQRMenuItem.setVisible(false);
                deleteQRMenuItem.setVisible(false);
                break;
            }
        }

//        if (contacts) {
////            ScanCodeFragment.scannerView.setAutoFocus(true);
////            ScanCodeFragment.scannerView.startCamera();
//        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");

        int id = item.getItemId();
        switch(id) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            case R.id.qr_code_share: {
                shareQR ();
                break;
            }
            case R.id.qr_code_save: {
                QRCodeSaveBottomSheetDialogFragment qrCodeSaveBottomSheetDialogFragment = new QRCodeSaveBottomSheetDialogFragment();
                qrCodeSaveBottomSheetDialogFragment.show(getSupportFragmentManager(), qrCodeSaveBottomSheetDialogFragment.getTag());

                break;
            }
            case R.id.qr_code_settings: {
                Intent intent = new Intent(this, ManagerActivityLollipop.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("drawerItemQR", ManagerActivityLollipop.DrawerItem.SETTINGS);
                intent.putExtras(bundle);
                intent.putExtra("fromQR", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                this.finish();
                break;
            }
            case R.id.qr_code_reset: {
                resetQR();
                break;
            }
            case R.id.qr_code_delete: {
                deleteQR();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_DOWNLOAD_FOLDER && resultCode == Activity.RESULT_OK && intent != null) {
            String parentPath = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            if (parentPath != null) {
                File qrFile = null;
                if (megaApi == null) {
                    megaApi = ((MegaApplication) getApplication()).getMegaApi();
                }
                String myEmail = megaApi.getMyEmail();
                qrFile = buildQrFile(this,myEmail + "QRcode.jpg");
                if (qrFile == null) {
                    showSnackbar(drawerLayout, getString(R.string.general_error));
                }
                else {
                    if (qrFile.exists()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                            if (!hasStoragePermission) {
                                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.REQUEST_WRITE_STORAGE);
                            }
                        }

                        double availableFreeSpace = Double.MAX_VALUE;
                        try {
                            StatFs stat = new StatFs(parentPath);
                            availableFreeSpace = (double) stat.getAvailableBlocks() * (double) stat.getBlockSize();
                        } catch (Exception ex) {
                        }

                        if (availableFreeSpace < qrFile.length()) {
                            showSnackbar(drawerLayout, getString(R.string.error_not_enough_free_space));
                            return;
                        }
                        File newQrFile = new File(parentPath, myEmail + "QRcode.jpg");
                        if (newQrFile == null) {
                            showSnackbar(drawerLayout, getString(R.string.general_error));
                        }
                        else {
                            try {
                                newQrFile.createNewFile();
                                FileChannel src = new FileInputStream(qrFile).getChannel();
                                FileChannel dst = new FileOutputStream(newQrFile, false).getChannel();
                                dst.transferFrom(src, 0, src.size());       // copy the first file to second.....
                                src.close();
                                dst.close();
                                showSnackbar(drawerLayout, getString(R.string.success_download_qr, parentPath));
                            } catch (IOException e) {
                                showSnackbar(drawerLayout, getString(R.string.general_error));
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        showSnackbar(drawerLayout, getString(R.string.error_download_qr));
                    }
                }
            }
        }
    }

    public void shareQR () {
        log("shareQR");

        if (myCodeFragment == null) {
            log("MyCodeFragment is NULL");
            myCodeFragment = (MyCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 0);
        }
        if (myCodeFragment!= null && myCodeFragment.isAdded()){
            File qrCodeFile = myCodeFragment.queryIfQRExists();
            if (qrCodeFile != null && qrCodeFile.exists()){
                Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setType("image/*");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    log("Use provider to share");
                    Uri uri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider",qrCodeFile);
                    share.putExtra(Intent.EXTRA_STREAM, Uri.parse(uri.toString()));
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                else{
                    share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + qrCodeFile));
                }
                startActivity(Intent.createChooser(share, getString(R.string.context_share)));
            }
            else {
                showSnackbar(drawerLayout, getString(R.string.error_share_qr));
            }
        }
    }

    public void resetQR () {
        log("resetQR");

        if (myCodeFragment == null) {
            log("MyCodeFragment is NULL");
            myCodeFragment = (MyCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 0);
        }
        if (myCodeFragment != null && myCodeFragment.isAdded()) {
            myCodeFragment.resetQRCode();
        }
    }

    public void deleteQR () {
        log("deleteQR");

        if (myCodeFragment == null) {
            log("MyCodeFragment is NULL");
            myCodeFragment = (MyCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 0);
        }
        if (myCodeFragment != null && myCodeFragment.isAdded()){
            myCodeFragment.deleteQRCode();
        }
    }

    public void resetSuccessfully (boolean success) {
        log("resetSuccessfully");
        if (success){
            showSnackbar(drawerLayout, getString(R.string.qrcode_reset_successfully));
        }
        else {
            showSnackbar(drawerLayout, getString(R.string.qrcode_reset_not_successfully));
        }
    }

    public void showSnackbar(View view, String s){
        if (view == null) {
            showSnackbar(Constants.SNACKBAR_TYPE, drawerLayout, s);
        }
        else {
            showSnackbar(Constants.SNACKBAR_TYPE, view, s);
        }
    }

    public static void log(String message) {
        Util.log("QRCodeActivity", message);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish ");
        if(request.getType() == MegaRequest.TYPE_INVITE_CONTACT  && request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD){
            if (scanCodeFragment == null) {
                log("ScanCodeFragment is NULL");
                scanCodeFragment = (ScanCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 1);
            }
            if (scanCodeFragment != null && scanCodeFragment.isAdded()){
                scanCodeFragment.myEmail = request.getEmail();
                if (e.getErrorCode() == MegaError.API_OK){
                    log("OK INVITE CONTACT: "+request.getEmail());
                    scanCodeFragment.dialogTitleContent = R.string.invite_sent;
                    scanCodeFragment.dialogTextContent = R.string.invite_sent_text;
                    scanCodeFragment.showAlertDialog(scanCodeFragment.dialogTitleContent, scanCodeFragment.dialogTextContent, true);
                }
                else if (e.getErrorCode() == MegaError.API_EACCESS){
                    scanCodeFragment.dialogTitleContent = R.string.invite_not_sent;
                    scanCodeFragment.dialogTextContent = R.string.invite_not_sent_text_error;
                    scanCodeFragment.showAlertDialog(scanCodeFragment.dialogTitleContent, scanCodeFragment.dialogTextContent, false);
                }
                else if (e.getErrorCode() == MegaError.API_EEXIST){
                    scanCodeFragment.dialogTitleContent = R.string.invite_not_sent;
                    scanCodeFragment.dialogTextContent = R.string.invite_not_sent_text_already_contact;
                    scanCodeFragment.showAlertDialog(scanCodeFragment.dialogTitleContent, scanCodeFragment.dialogTextContent, true);
                }
                else if (e.getErrorCode() == MegaError.API_EARGS){
                    scanCodeFragment.dialogTitleContent = R.string.invite_not_sent;
                    scanCodeFragment.dialogTextContent = R.string.error_own_email_as_contact;
                    scanCodeFragment.showAlertDialog(scanCodeFragment.dialogTitleContent, scanCodeFragment.dialogTextContent, true);
                }
            }
        }
        //        megaApi.contactLinkQuery(request.getNodeHandle(), this);
        else if (request.getType() == MegaRequest.TYPE_CONTACT_LINK_QUERY){
            if (inviteContacts) {
                Intent intent = new Intent();
                intent.putExtra("mail", request.getEmail());
                setResult(RESULT_OK, intent);
                finish();
            }
            else {
                if (scanCodeFragment == null) {
                    log("ScanCodeFragment is NULL");
                    scanCodeFragment = (ScanCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 1);
                }
                if (scanCodeFragment != null && scanCodeFragment.isAdded()) {
                    scanCodeFragment.myEmail = request.getEmail();
                    scanCodeFragment.initDialogInvite(request, e);
                }
            }
        }
        else if (request.getType() == MegaRequest.TYPE_GET_ATTR_USER) {
            if (scanCodeFragment == null) {
                log("ScanCodeFragment is NULL");
                scanCodeFragment = (ScanCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 1);
            }
            if (scanCodeFragment != null && scanCodeFragment.isAdded()) {
                if (e.getErrorCode() == MegaError.API_OK) {
                    log("Get user avatar OK");
                    scanCodeFragment.setAvatar();
                } else {
                    log("Get user avatar FAIL");
                    scanCodeFragment.setDefaultAvatar();
                }
            }
        }
//        megaApi.contactLinkCreate(true/false, this);
        else if (request.getType() == MegaRequest.TYPE_CONTACT_LINK_CREATE) {
            if (myCodeFragment == null){
                log("MyCodeFragment is NULL");
                myCodeFragment = (MyCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 0);
            }
            if (myCodeFragment != null && myCodeFragment.isAdded()){
                myCodeFragment.initCreateQR(request, e);
            }
        }
//        megaApi.contactLinkDelete(request.getNodeHandle(), this);
        else if (request.getType() == MegaRequest.TYPE_CONTACT_LINK_DELETE){
            if (myCodeFragment == null){
                log("MyCodeFragment is NULL");
                myCodeFragment = (MyCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 0);
            }
            if (myCodeFragment != null && myCodeFragment.isAdded()){
                myCodeFragment.initDeleteQR(request, e);
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    public void deleteSuccessfully() {
        log("deleteSuccessfully");
        shareMenuItem.setVisible(false);
        saveMenuItem.setVisible(false);
        settingsMenuItem.setVisible(true);
        resetQRMenuItem.setVisible(true);
        deleteQRMenuItem.setVisible(false);
    }

    public void createSuccessfully() {
        log("createSuccesfully");
        shareMenuItem.setVisible(true);
        saveMenuItem.setVisible(true);
        settingsMenuItem.setVisible(true);
        resetQRMenuItem.setVisible(true);
        deleteQRMenuItem.setVisible(true);
    }
}
