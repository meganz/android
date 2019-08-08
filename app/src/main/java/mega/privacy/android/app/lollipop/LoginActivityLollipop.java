package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.BaseActivity;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.EphemeralCredentials;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.utils.JobUtil.stopRunningCameraUploadService;
import static mega.privacy.android.app.utils.JobUtil.scheduleCameraUploadJob;

public class LoginActivityLollipop extends BaseActivity implements MegaGlobalListenerInterface, MegaRequestListenerInterface {

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;

    RelativeLayout relativeContainer;

    boolean cancelledConfirmationProcess = false;

    //Fragments
    TourFragmentLollipop tourFragment;
    LoginFragmentLollipop loginFragment;
    ChooseAccountFragmentLollipop chooseAccountFragment;
    CreateAccountFragmentLollipop createAccountFragment;
    ConfirmEmailFragmentLollipop confirmEmailFragment;

    ActionBar aB;
    int visibleFragment;

    static LoginActivityLollipop loginActivity;

    Intent intentReceived = null;

    public String accountBlocked = null;

    DatabaseHandler dbH;

    Handler handler = new Handler();
    private MegaApiAndroid megaApi;
    private MegaApiAndroid megaApiFolder;

    private android.support.v7.app.AlertDialog alertDialogTransferOverquota;

    boolean waitingForConfirmAccount = false;
    String emailTemp = null;
    String passwdTemp = null;
    String sessionTemp = null;
    String firstNameTemp = null;
    String lastNameTemp = null;

    static boolean isBackFromLoginPage;
    static boolean isFetchingNodes;

    private BroadcastReceiver updateMyAccountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int actionType;

            if (intent != null){
                actionType = intent.getIntExtra("actionType", -1);

                if(actionType == Constants.UPDATE_GET_PRICING){
                    log("BROADCAST TO UPDATE AFTER GET PRICING");
                    //UPGRADE_ACCOUNT_FRAGMENT

                    if(chooseAccountFragment!=null && chooseAccountFragment.isAdded()){
                        chooseAccountFragment.setPricingInfo();
                    }
                }
                else if(actionType == Constants.UPDATE_PAYMENT_METHODS){
                    log("BROADCAST TO UPDATE AFTER UPDATE_PAYMENT_METHODS");
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        log("onDestroy");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateMyAccountReceiver);
        if (megaApi != null) {
            megaApi.removeGlobalListener(this);
            megaApi.removeRequestListener(this);
        }
        super.onDestroy();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");
        super.onCreate(savedInstanceState);
        
        loginActivity = this;

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        aB = getSupportActionBar();
        hideAB();

        scaleW = Util.getScaleW(outMetrics, density);
        scaleH = Util.getScaleH(outMetrics, density);

        dbH = DatabaseHandler.getDbHandler(getApplicationContext());
        if (megaApi == null) {
            megaApi = ((MegaApplication) getApplication()).getMegaApi();
        }

        if (megaApiFolder == null) {
            megaApiFolder = ((MegaApplication) getApplication()).getMegaApiFolder();
        }

        megaApi.addGlobalListener(this);
        megaApi.addRequestListener(this);

        setContentView(R.layout.activity_login);
        relativeContainer = (RelativeLayout) findViewById(R.id.relative_container_login);

        intentReceived = getIntent();
        if(savedInstanceState!=null) {
            log("Bundle is NOT NULL");
            visibleFragment = savedInstanceState.getInt("visibleFragment", Constants.LOGIN_FRAGMENT);
        }
        else{
            if (intentReceived != null) {
                visibleFragment = intentReceived.getIntExtra("visibleFragment", Constants.LOGIN_FRAGMENT);
                log("There is an intent! VisibleFragment: " + visibleFragment);
            } else {
                visibleFragment = Constants.LOGIN_FRAGMENT;
            }
        }

        if (dbH.getEphemeral() != null) {
            visibleFragment = Constants.CONFIRM_EMAIL_FRAGMENT;

            EphemeralCredentials ephemeralCredentials = dbH.getEphemeral();

            emailTemp = ephemeralCredentials.getEmail();
            passwdTemp = ephemeralCredentials.getPassword();
            sessionTemp = ephemeralCredentials.getSession();
            firstNameTemp = ephemeralCredentials.getFirstName();
            lastNameTemp = ephemeralCredentials.getLastName();

            megaApi.resumeCreateAccount(sessionTemp, this);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(updateMyAccountReceiver, new IntentFilter(Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS));
        isBackFromLoginPage = false;
        showFragment(visibleFragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home: {
                switch (visibleFragment) {
                    case Constants.LOGIN_FRAGMENT: {
                        if (loginFragment != null && loginFragment.isAdded()) {
//                            loginFragment.returnToLogin();
                            onBackPressed();
                        }
                        break;
                    }
                    case Constants.CHOOSE_ACCOUNT_FRAGMENT: {
                        if (chooseAccountFragment != null && chooseAccountFragment.isAdded()) {
                            chooseAccountFragment.onFreeClick(null);
                        }
                        break;
                    }
                }
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void showSnackbar(String message) {
        showSnackbar(relativeContainer, message);
    }

    public void showFragment(int visibleFragment) {
        log("showFragment: " + visibleFragment);
        this.visibleFragment = visibleFragment;
        switch (visibleFragment) {
            case Constants.LOGIN_FRAGMENT: {
                log("showLoginFragment");
                if (loginFragment == null) {
                    loginFragment = new LoginFragmentLollipop();
                }
                if ((passwdTemp != null) && (emailTemp != null)) {
                    loginFragment.setEmailTemp(emailTemp);
                    loginFragment.setPasswdTemp(passwdTemp);
                }

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container_login, loginFragment);
                ft.commitNowAllowingStateLoss();

                getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));

//				getFragmentManager()
//						.beginTransaction()
//						.attach(loginFragment)
//						.commit();
                break;
            }
            case Constants.CHOOSE_ACCOUNT_FRAGMENT: {
                log("Show CHOOSE_ACCOUNT_FRAGMENT");

                if (chooseAccountFragment == null) {
                    chooseAccountFragment = new ChooseAccountFragmentLollipop();
                }

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container_login, chooseAccountFragment);
                ft.commitNowAllowingStateLoss();

                Window window = this.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));
                break;
            }
            case Constants.CREATE_ACCOUNT_FRAGMENT: {
                log("Show CREATE_ACCOUNT_FRAGMENT");
                if (createAccountFragment == null || cancelledConfirmationProcess) {
                    createAccountFragment = new CreateAccountFragmentLollipop();
                    cancelledConfirmationProcess = false;
                }

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container_login, createAccountFragment);
                ft.commitNowAllowingStateLoss();

                Window window = this.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));

                break;

            }
            case Constants.TOUR_FRAGMENT: {
                log("Show TOUR_FRAGMENT");

                if (tourFragment == null) {
                    tourFragment = new TourFragmentLollipop();
                }

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container_login, tourFragment);
                ft.commitNowAllowingStateLoss();
                break;
            }
            case Constants.CONFIRM_EMAIL_FRAGMENT: {

                if (confirmEmailFragment == null) {
                    confirmEmailFragment = new ConfirmEmailFragmentLollipop();
                    if ((passwdTemp != null) && (emailTemp != null)) {
                        confirmEmailFragment.setEmailTemp(emailTemp);
                        confirmEmailFragment.setPasswdTemp(passwdTemp);
                        confirmEmailFragment.setFirstNameTemp(firstNameTemp);
//						emailTemp = null;
//						passwdTemp = null;
//						nameTemp = null;
                    }
                } else {
                    if ((passwdTemp != null) && (emailTemp != null)) {
                        confirmEmailFragment.setEmailTemp(emailTemp);
                        confirmEmailFragment.setPasswdTemp(passwdTemp);
                        confirmEmailFragment.setFirstNameTemp(firstNameTemp);
//						emailTemp = null;
//						passwdTemp = null;
//						nameTemp = null;
                    }
                }

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container_login, confirmEmailFragment);
                ft.commitNowAllowingStateLoss();
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.executePendingTransactions();

                Window window = this.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));

                break;
            }
        }

        if( ((MegaApplication) getApplication()).isEsid()){
            showAlertLoggedOut();
        }
    }

    public void showAlertIncorrectRK() {
        log("showAlertIncorrectRK");
        final android.support.v7.app.AlertDialog.Builder dialogBuilder = new android.support.v7.app.AlertDialog.Builder(this);

        dialogBuilder.setTitle(getString(R.string.incorrect_MK_title));
        dialogBuilder.setMessage(getString(R.string.incorrect_MK));
        dialogBuilder.setCancelable(false);

        dialogBuilder.setPositiveButton(getString(R.string.cam_sync_ok), new android.content.DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        android.support.v7.app.AlertDialog alert = dialogBuilder.create();
        alert.show();
    }

    public void showAlertLoggedOut() {
        log("showAlertLoggedOut");
        ((MegaApplication) getApplication()).setEsid(false);
        if(!isFinishing()){
            final android.support.v7.app.AlertDialog.Builder dialogBuilder = new android.support.v7.app.AlertDialog.Builder(this);

            dialogBuilder.setTitle(getString(R.string.title_alert_logged_out));
            dialogBuilder.setMessage(getString(R.string.error_server_expired_session));

            dialogBuilder.setPositiveButton(getString(R.string.cam_sync_ok), new android.content.DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            android.support.v7.app.AlertDialog alert = dialogBuilder.create();
            alert.show();
        }
    }

    public void showTransferOverquotaDialog() {
        log("showTransferOverquotaDialog");

        boolean show = true;

        if(alertDialogTransferOverquota!=null){
            if(alertDialogTransferOverquota.isShowing()){
                log("change show to false");
                show = false;
            }
        }

        if(show){
            android.support.v7.app.AlertDialog.Builder dialogBuilder = new android.support.v7.app.AlertDialog.Builder(this);

            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.transfer_overquota_layout_not_logged, null);
            dialogBuilder.setView(dialogView);

            TextView title = (TextView) dialogView.findViewById(R.id.not_logged_transfer_overquota_title);
            title.setText(getString(R.string.title_depleted_transfer_overquota));

            ImageView icon = (ImageView) dialogView.findViewById(R.id.not_logged_image_transfer_overquota);
            icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.transfer_quota_empty));

            TextView text = (TextView) dialogView.findViewById(R.id.not_logged_text_transfer_overquota);
            text.setText(getString(R.string.text_depleted_transfer_overquota));

            Button continueButton = (Button) dialogView.findViewById(R.id.not_logged_transfer_overquota_button_dissmiss);
            continueButton.setText(getString(R.string.login_text));

            Button paymentButton = (Button) dialogView.findViewById(R.id.not_logged_transfer_overquota_button_payment);
            paymentButton.setText(getString(R.string.continue_without_account_transfer_overquota));

            Button cancelButton = (Button) dialogView.findViewById(R.id.not_logged_transfer_overquota_button_cancel);
            cancelButton.setText(getString(R.string.menu_cancel_all_transfers));

            alertDialogTransferOverquota = dialogBuilder.create();

            continueButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    alertDialogTransferOverquota.dismiss();
                }

            });

            paymentButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    alertDialogTransferOverquota.dismiss();
                }

            });

            cancelButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    alertDialogTransferOverquota.dismiss();
                    showConfirmationCancelAllTransfers();

                }

            });

            alertDialogTransferOverquota.setCancelable(false);
            alertDialogTransferOverquota.setCanceledOnTouchOutside(false);
            alertDialogTransferOverquota.show();
        }
    }

    public void startCameraUploadService(boolean firstTimeCam, int time) {
        log("startCameraUploadService");
        if (firstTimeCam) {
            Intent intent = new Intent(this, ManagerActivityLollipop.class);
            intent.putExtra("firstLogin", true);
            startActivity(intent);
            finish();
        } else {
            log("Enciendo el servicio de la camara");
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    log("Now I start the service");
                    scheduleCameraUploadJob(LoginActivityLollipop.this);
                }
            }, time);
        }
    }

    public void showConfirmationCancelAllTransfers() {
        log("showConfirmationCancelAllTransfers");

        setIntent(null);
        //Show confirmation message
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        log("Pressed button positive to cancel transfer");
                        if (megaApi != null) {
                            megaApi.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD);
                            if (megaApiFolder != null) {
                                megaApiFolder.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD);
                            }
                        } else {
                            log("megaAPI is null");
                            if (megaApiFolder != null) {
                                megaApiFolder.cancelTransfers(MegaTransfer.TYPE_DOWNLOAD);
                            }
                        }

                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
//		builder.setTitle(getResources().getString(R.string.cancel_transfer_title));

        builder.setMessage(getResources().getString(R.string.cancel_all_transfer_confirmation));
        builder.setPositiveButton(R.string.context_delete, dialogClickListener);
        builder.setNegativeButton(R.string.general_cancel, dialogClickListener);

        builder.show();
    }

    @Override
    public void onBackPressed() {
        log("onBackPressed");
        retryConnectionsAndSignalPresence();

        int valueReturn = -1;

        switch (visibleFragment) {
            case Constants.LOGIN_FRAGMENT: {
                if (loginFragment != null) {
                    valueReturn = loginFragment.onBackPressed();
                }
                break;
            }
            case Constants.CREATE_ACCOUNT_FRAGMENT: {
                showFragment(Constants.TOUR_FRAGMENT);
                break;
            }
            case Constants.TOUR_FRAGMENT: {
                valueReturn = 0;
                break;
            }
            case Constants.CONFIRM_EMAIL_FRAGMENT: {
                valueReturn = 0;
                break;
            }
            case Constants.CHOOSE_ACCOUNT_FRAGMENT: {
                if (chooseAccountFragment != null && chooseAccountFragment.isAdded()) {
                    chooseAccountFragment.onFreeClick(null);
                }
                break;
            }
        }

        if (valueReturn == 0) {
            super.onBackPressed();
        }
    }

    @Override
    public void onResume() {
        log("onResume");
        super.onResume();
        Util.setAppFontSize(this);
        Intent intent = getIntent();

        if (intent != null) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION_CANCEL_CAM_SYNC)) {
                    log("ACTION_CANCEL_CAM_SYNC");
                    String title = getString(R.string.cam_sync_syncing);
                    String text = getString(R.string.cam_sync_cancel_sync);
                    AlertDialog.Builder builder = Util.getCustomAlertBuilder(this, title, text, null);
                    builder.setPositiveButton(getString(R.string.cam_sync_stop),
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    stopRunningCameraUploadService(LoginActivityLollipop.this);
                                    dbH.setCamSyncEnabled(false);
                                }
                            });
                    builder.setNegativeButton(getString(R.string.general_cancel), null);
                    final AlertDialog dialog = builder.create();
                    try {
                        dialog.show();
                    } catch (Exception ex) {
                        log(ex.toString());
                    }
                }
                else if (intent.getAction().equals(Constants.ACTION_CANCEL_DOWNLOAD)) {
                    showConfirmationCancelAllTransfers();
                }
                else if (intent.getAction().equals(Constants.ACTION_OVERQUOTA_TRANSFER)) {
                    showTransferOverquotaDialog();

                }
                intent.setAction(null);
            }
        }

        setIntent(null);
    }

    boolean loggerPermissionKarere = false;
    boolean loggerPermissionSDK = false;

    public void showConfirmationEnableLogsKarere() {
        log("showConfirmationEnableLogsKarere");

        if (loginFragment != null) {
            loginFragment.numberOfClicksKarere = 0;
        }

        loginActivity = this;

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:{
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(loginActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                            if (!hasStoragePermission) {
                                loggerPermissionKarere = true;
                                ActivityCompat.requestPermissions(loginActivity,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        Constants.REQUEST_WRITE_STORAGE);
                            } else {
                                enableLogsKarere();
                            }
                        } else {
                            enableLogsKarere();
                        }
                        break;
                    }

                    case DialogInterface.BUTTON_NEGATIVE: {
                        break;
                    }
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder;
        builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setMessage(R.string.enable_log_text_dialog).setPositiveButton(R.string.general_enable, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show().setCanceledOnTouchOutside(false);
    }

    public void showConfirmationEnableLogsSDK() {
        log("showConfirmationEnableLogsSDK");

        if (loginFragment != null) {
            loginFragment.numberOfClicksSDK = 0;
        }

        loginActivity = this;

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:{
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            boolean hasStoragePermission = (ContextCompat.checkSelfPermission(loginActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                            if (!hasStoragePermission) {
                                loggerPermissionSDK = true;
                                ActivityCompat.requestPermissions(loginActivity,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        Constants.REQUEST_WRITE_STORAGE);
                            } else {
                                enableLogsSDK();
                            }
                        } else {
                            enableLogsSDK();
                        }
                        break;
                    }

                    case DialogInterface.BUTTON_NEGATIVE: {
                        break;
                    }
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder;
        builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setMessage(R.string.enable_log_text_dialog).setPositiveButton(R.string.general_enable, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show().setCanceledOnTouchOutside(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        log("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case Constants.REQUEST_WRITE_STORAGE:{
                if (loggerPermissionKarere){
                    loggerPermissionKarere = false;
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                        enableLogsKarere();
                    }
                }
                else if (loggerPermissionSDK){
                    loggerPermissionSDK = false;
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                        enableLogsSDK();
                    }
                }
            }
        }
    }

    public void enableLogsSDK() {
        log("enableLogsSDK");

        dbH.setFileLoggerSDK(true);
        Util.setFileLoggerSDK(true);
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);
        showSnackbar(getString(R.string.settings_enable_logs));
        log("App Version: " + Util.getVersion(this));
    }

    public void enableLogsKarere() {
        log("enableLogsKarere");

        dbH.setFileLoggerKarere(true);
        Util.setFileLoggerKarere(true);
        MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX);
        showSnackbar(getString(R.string.settings_enable_logs));
        log("App Version: " + Util.getVersion(this));
    }

    public void setWaitingForConfirmAccount(boolean waitingForConfirmAccount) {
        this.waitingForConfirmAccount = waitingForConfirmAccount;
    }

    public boolean getWaitingForConfirmAccount() {
        return this.waitingForConfirmAccount;
    }

    public void setFirstNameTemp(String firstNameTemp) {
        this.firstNameTemp = firstNameTemp;
    }

    public void setLastNameTemp(String lastNameTemp) {
        this.lastNameTemp = lastNameTemp;
    }

    public String getFirstNameTemp() {
        return this.firstNameTemp;
    }

    public void setPasswdTemp(String passwdTemp) {
        this.passwdTemp = passwdTemp;
    }

    public String getPasswdTemp() {
        return this.passwdTemp;
    }

    public void setEmailTemp(String emailTemp) {
        this.emailTemp = emailTemp;
        if (dbH != null) {
            if (dbH.getEphemeral() != null) {
                EphemeralCredentials ephemeralCredentials = dbH.getEphemeral();
                ephemeralCredentials.setEmail(emailTemp);
                dbH.clearEphemeral();
                dbH.saveEphemeral(ephemeralCredentials);
            }
        }
    }

    public String getEmailTemp() {
        return this.emailTemp;
    }


//	public void onNewIntent(Intent intent){
//		if (intent != null && Constants.ACTION_CONFIRM.equals(intent.getAction())) {
//			loginFragment.handleConfirmationIntent(intent);
//		}
//	}

    public static void log(String message) {
        Util.log("LoginActivityLollipop", message);
    }

    @Override
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {

    }

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {
        log("onUserAlertsUpdate");
    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList) {

    }

    @Override
    public void onReloadNeeded(MegaApiJava api) {
    }

    @Override
    public void onAccountUpdate(MegaApiJava api) {
        log("onAccountUpdate");

        if (waitingForConfirmAccount) {
            waitingForConfirmAccount = false;
            visibleFragment = Constants.LOGIN_FRAGMENT;
            showFragment(visibleFragment);
        }
    }

    @Override
    public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {
    }


    @Override
    public void onEvent(MegaApiJava api, MegaEvent event) {
        log("onEvent");
        if(event.getType()==MegaEvent.EVENT_ACCOUNT_BLOCKED){
            log("Event received: "+event.getText()+"_"+event.getNumber());
            if(event.getNumber()==200){
                accountBlocked = getString(R.string.account_suspended_multiple_breaches_ToS);
            }
            else if(event.getNumber()==300){
                accountBlocked = getString(R.string.account_suspended_breache_ToS);
            }
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        log("onRequestStart - " + request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        log("onRequestUpdate - " + request.getRequestString());
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestFinish - " + request.getRequestString() + "_" + e.getErrorCode());

        if(request.getType() == MegaRequest.TYPE_LOGOUT){

            if(accountBlocked!=null){
                showSnackbar(accountBlocked);
            }
            accountBlocked=null;

        }
        else if (request.getType() == MegaRequest.TYPE_CREATE_ACCOUNT){
            try {
                if (request.getParamType() == 1) {
                    if (e.getErrorCode() == MegaError.API_OK) {
                        waitingForConfirmAccount = true;
                        visibleFragment = Constants.CONFIRM_EMAIL_FRAGMENT;
                        showFragment(visibleFragment);

                    } else {
                        cancelConfirmationAccount();
                    }
                }
            }
            catch (Exception exc){
                log("ExceptionManager");
            }
        }
    }

    public void cancelConfirmationAccount(){
        log("cancelConfirmationAccount");
        dbH.clearEphemeral();
        dbH.clearCredentials();
        cancelledConfirmationProcess = true;
        waitingForConfirmAccount = false;
        passwdTemp = null;
        emailTemp = null;
        visibleFragment = Constants.TOUR_FRAGMENT;
        showFragment(visibleFragment);
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestTemporaryError - " + request.getRequestString());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        log("onSaveInstanceState");

        super.onSaveInstanceState(outState);

        outState.putInt("visibleFragment", visibleFragment);
    }

    @Override
    protected void onPause() {
        log("onPause");
        super.onPause();
    }

    public void showAB(Toolbar tB){
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.show();
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (visibleFragment == Constants.LOGIN_FRAGMENT) {
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color_secondary));
        }
    }

    public void hideAB(){
        if (aB != null){
            aB.hide();
        }

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));
    }
}
