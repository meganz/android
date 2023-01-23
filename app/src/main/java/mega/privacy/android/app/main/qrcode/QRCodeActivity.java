package mega.privacy.android.app.main.qrcode;

import static mega.privacy.android.app.main.qrcode.MyCodeFragment.QR_IMAGE_FILE_NAME;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown;
import static mega.privacy.android.app.utils.CacheFolderManager.buildQrFile;
import static mega.privacy.android.app.utils.Constants.OPEN_SCAN_QR;
import static mega.privacy.android.app.utils.Constants.REQUEST_DOWNLOAD_FOLDER;
import static mega.privacy.android.app.utils.Constants.REQUEST_WRITE_STORAGE;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions;
import static mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.globalmanagement.MyAccountInfo;
import mega.privacy.android.app.main.FileStorageActivity;
import mega.privacy.android.app.modalbottomsheet.QRCodeSaveBottomSheetDialogFragment;
import mega.privacy.android.app.presentation.qrcode.scan.ScanCodeFragment;
import mega.privacy.android.app.presentation.qrcode.scan.ScanCodeViewModel;
import mega.privacy.android.app.presentation.settings.SettingsActivity;
import mega.privacy.android.app.presentation.settings.model.TargetPreference;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import timber.log.Timber;

@AndroidEntryPoint
public class QRCodeActivity extends PasscodeActivity implements MegaRequestListenerInterface {

    private final int MY_PERMISSIONS_REQUEST_CAMERA = 1010;

    @Inject
    MyAccountInfo myAccountInfo;

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

    private ScanCodeViewModel scanCodeViewModel;

    private QRCodePageAdapter qrCodePageAdapter;

    private LinearLayout rootLevelLayout;

    private int qrCodeFragment;

    MegaApiAndroid megaApi;

    private boolean inviteContacts = false;
    private boolean showScanQrView = false;

    DisplayMetrics outMetrics;

    private QRCodeSaveBottomSheetDialogFragment qrCodeSaveBottomSheetDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate");

        if (savedInstanceState != null) {
            showScanQrView = savedInstanceState.getBoolean(OPEN_SCAN_QR, false);
            inviteContacts = savedInstanceState.getBoolean("inviteContacts", false);
        } else {
            showScanQrView = getIntent().getBooleanExtra(OPEN_SCAN_QR, false);
            inviteContacts = getIntent().getBooleanExtra("inviteContacts", false);
        }

        scanCodeViewModel = new ViewModelProvider(this).get(ScanCodeViewModel.class);
        scanCodeViewModel.updateFinishActivityOnScanComplete(inviteContacts);

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        setContentView(R.layout.activity_qr_code);

        tB = (Toolbar) findViewById(R.id.toolbar);
        if (tB == null) {
            Timber.w("Tb is Null");
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

        rootLevelLayout = findViewById(R.id.root_level_layout);

        qrCodePageAdapter = new QRCodePageAdapter(getSupportFragmentManager(), this);

        tabLayoutQRCode = (TabLayout) findViewById(R.id.sliding_tabs_qr_code);
        viewPagerQRCode = (ViewPager) findViewById(R.id.qr_code_tabs_pager);

        if (!hasPermissions(this, Manifest.permission.CAMERA)) {
            requestPermission(this, MY_PERMISSIONS_REQUEST_CAMERA, Manifest.permission.CAMERA);
        } else {
            initActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initActivity();
            } else {
                this.finish();
            }
        }
    }

    void initActivity() {
        viewPagerQRCode.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                supportInvalidateOptionsMenu();

                if (position == 0) {
                    qrCodeFragment = 0;
                } else {
                    qrCodeFragment = 1;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        viewPagerQRCode.setAdapter(qrCodePageAdapter);
        tabLayoutQRCode.setupWithViewPager(viewPagerQRCode);
        if (showScanQrView || inviteContacts) {
            viewPagerQRCode.setCurrentItem(1);
        } else {
            viewPagerQRCode.setCurrentItem(0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(OPEN_SCAN_QR, showScanQrView);
        outState.putBoolean("inviteContacts", inviteContacts);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Timber.d("onCreateOptionsMenu");

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

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("onOptionsItemSelected");

        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            case R.id.qr_code_share: {
                shareQR();
                break;
            }
            case R.id.qr_code_save: {
                if (isBottomSheetDialogShown(qrCodeSaveBottomSheetDialogFragment)) break;

                qrCodeSaveBottomSheetDialogFragment = new QRCodeSaveBottomSheetDialogFragment();
                qrCodeSaveBottomSheetDialogFragment.show(getSupportFragmentManager(), qrCodeSaveBottomSheetDialogFragment.getTag());

                break;
            }
            case R.id.qr_code_settings: {
                Intent settingsIntent = SettingsActivity.Companion.getIntent(this, TargetPreference.QR.INSTANCE);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(settingsIntent);
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

    public String getName() {
        return myAccountInfo.getFullName();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_DOWNLOAD_FOLDER && resultCode == Activity.RESULT_OK && intent != null) {
            String parentPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
            if (parentPath != null) {
                File qrFile = null;
                if (megaApi == null) {
                    megaApi = ((MegaApplication) getApplication()).getMegaApi();
                }
                String myEmail = megaApi.getMyEmail();
                qrFile = buildQrFile(this, myEmail + QR_IMAGE_FILE_NAME);
                if (qrFile == null) {
                    showSnackbar(rootLevelLayout, getString(R.string.general_error));
                } else {
                    if (qrFile.exists()) {
                        boolean hasStoragePermission = hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (!hasStoragePermission) {
                            requestPermission(this, REQUEST_WRITE_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        }

                        double availableFreeSpace = Double.MAX_VALUE;
                        try {
                            StatFs stat = new StatFs(parentPath);
                            availableFreeSpace = (double) stat.getAvailableBlocks() * (double) stat.getBlockSize();
                        } catch (Exception ex) {
                        }

                        if (availableFreeSpace < qrFile.length()) {
                            showSnackbar(rootLevelLayout, getString(R.string.error_not_enough_free_space));
                            return;
                        }
                        File newQrFile = new File(parentPath, myEmail + QR_IMAGE_FILE_NAME);
                        if (newQrFile == null) {
                            showSnackbar(rootLevelLayout, getString(R.string.general_error));
                        } else {
                            try {
                                newQrFile.createNewFile();
                                FileChannel src = new FileInputStream(qrFile).getChannel();
                                FileChannel dst = new FileOutputStream(newQrFile, false).getChannel();
                                dst.transferFrom(src, 0, src.size());       // copy the first file to second.....
                                src.close();
                                dst.close();
                                showSnackbar(rootLevelLayout, getString(R.string.success_download_qr, parentPath));
                            } catch (IOException e) {
                                showSnackbar(rootLevelLayout, getString(R.string.general_error));
                                e.printStackTrace();
                            }
                        }
                    } else {
                        showSnackbar(rootLevelLayout, getString(R.string.error_download_qr));
                    }
                }
            }
        }
    }

    public void shareQR() {
        Timber.d("shareQR");

        if (myCodeFragment == null) {
            Timber.w("MyCodeFragment is NULL");
            myCodeFragment = (MyCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 0);
        }
        if (myCodeFragment != null && myCodeFragment.isAdded()) {
            File qrCodeFile = myCodeFragment.queryIfQRExists();
            if (qrCodeFile != null && qrCodeFile.exists()) {
                Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setType("image/*");

                Timber.d("Use provider to share");
                Uri uri = FileProvider.getUriForFile(this, "mega.privacy.android.app.providers.fileprovider", qrCodeFile);
                share.putExtra(Intent.EXTRA_STREAM, Uri.parse(uri.toString()));
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(share, getString(R.string.context_share)));
            } else {
                showSnackbar(rootLevelLayout, getString(R.string.error_share_qr));
            }
        }
    }

    public void resetQR() {
        Timber.d("resetQR");

        if (myCodeFragment == null) {
            Timber.w("MyCodeFragment is NULL");
            myCodeFragment = (MyCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 0);
        }
        if (myCodeFragment != null && myCodeFragment.isAdded()) {
            myCodeFragment.resetQRCode();
        }
    }

    public void deleteQR() {
        Timber.d("deleteQR");

        if (myCodeFragment == null) {
            Timber.w("MyCodeFragment is NULL");
            myCodeFragment = (MyCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 0);
        }
        if (myCodeFragment != null && myCodeFragment.isAdded()) {
            myCodeFragment.deleteQRCode();
        }
    }

    public void resetSuccessfully(boolean success) {
        Timber.d("resetSuccessfully");
        if (success) {
            showSnackbar(rootLevelLayout, getString(R.string.qrcode_reset_successfully));
        } else {
            showSnackbar(rootLevelLayout, getString(R.string.qrcode_reset_not_successfully));
        }
    }

    public void showSnackbar(View view, String s) {
        if (view == null) {
            showSnackbar(SNACKBAR_TYPE, rootLevelLayout, s);
        } else {
            showSnackbar(SNACKBAR_TYPE, view, s);
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
        Timber.d("onRequestFinish ");
        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT && request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD) {
            if (scanCodeFragment == null) {
                Timber.w("ScanCodeFragment is NULL");
                scanCodeFragment = (ScanCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 1);
            }
            if (scanCodeFragment.isAdded()) {
                scanCodeViewModel.updateMyEmail(request.getEmail());
                if (e.getErrorCode() == MegaError.API_OK) {
                    Timber.d("OK INVITE CONTACT: %s", request.getEmail());
                    scanCodeViewModel.showInviteResultDialog(R.string.invite_sent, R.string.invite_sent_text, true, false);
                } else if (e.getErrorCode() == MegaError.API_EACCESS) {
                    scanCodeViewModel.showInviteResultDialog(R.string.invite_not_sent, R.string.invite_not_sent_text_error, false, false);
                } else if (e.getErrorCode() == MegaError.API_EEXIST) {
                    scanCodeViewModel.showInviteResultDialog(R.string.invite_not_sent, R.string.invite_not_sent_text_already_contact, true, true);
                } else if (e.getErrorCode() == MegaError.API_EARGS) {
                    scanCodeViewModel.showInviteResultDialog(R.string.invite_not_sent, R.string.error_own_email_as_contact, true, false);
                }
            }
        } else if (request.getType() == MegaRequest.TYPE_CONTACT_LINK_CREATE) {
            if (myCodeFragment == null) {
                Timber.w("MyCodeFragment is NULL");
                myCodeFragment = (MyCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 0);
            }
            if (myCodeFragment != null && myCodeFragment.isAdded()) {
                myCodeFragment.initCreateQR(request, e);
            }
        } else if (request.getType() == MegaRequest.TYPE_CONTACT_LINK_DELETE) {
            if (myCodeFragment == null) {
                Timber.w("MyCodeFragment is NULL");
                myCodeFragment = (MyCodeFragment) qrCodePageAdapter.instantiateItem(viewPagerQRCode, 0);
            }
            if (myCodeFragment != null && myCodeFragment.isAdded()) {
                myCodeFragment.initDeleteQR(request, e);
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    public void deleteSuccessfully() {
        Timber.d("deleteSuccessfully");
        shareMenuItem.setVisible(false);
        saveMenuItem.setVisible(false);
        settingsMenuItem.setVisible(true);
        resetQRMenuItem.setVisible(true);
        deleteQRMenuItem.setVisible(false);
    }

    public void createSuccessfully() {
        Timber.d("createSuccesfully");
        shareMenuItem.setVisible(true);
        saveMenuItem.setVisible(true);
        settingsMenuItem.setVisible(true);
        resetQRMenuItem.setVisible(true);
        deleteQRMenuItem.setVisible(true);
    }
}
