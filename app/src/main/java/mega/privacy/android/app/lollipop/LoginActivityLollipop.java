package mega.privacy.android.app.lollipop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.EphemeralCredentials;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaTransfer;
import nz.mega.sdk.MegaUser;


public class LoginActivityLollipop extends AppCompatActivity implements MegaGlobalListenerInterface, MegaRequestListenerInterface {

    float scaleH, scaleW;
    float density;
    DisplayMetrics outMetrics;
    Display display;

    RelativeLayout relativeContainer;

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

    @Override
    protected void onDestroy() {
        log("onDestroy");
        megaApi.removeGlobalListener(this);
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
        if (aB != null) {
            aB.hide();
        }

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

        setContentView(R.layout.activity_login);
        relativeContainer = (RelativeLayout) findViewById(R.id.relative_container_login);

        intentReceived = getIntent();
        if (intentReceived != null) {
            visibleFragment = intentReceived.getIntExtra("visibleFragment", Constants.LOGIN_FRAGMENT);
            log("There is an intent! VisibleFragment: " + visibleFragment);
        } else {
            visibleFragment = Constants.LOGIN_FRAGMENT;
        }

        if (dbH.getEphemeral() != null) {
            EphemeralCredentials ephemeralCredentials = dbH.getEphemeral();

            emailTemp = ephemeralCredentials.getEmail();
            passwdTemp = ephemeralCredentials.getPassword();
            sessionTemp = ephemeralCredentials.getSession();
            firstNameTemp = ephemeralCredentials.getFirstName();
            lastNameTemp = ephemeralCredentials.getLastName();

            megaApi.resumeCreateAccount(sessionTemp, this);
            return;
        }

//		visibleFragment = Constants.CHOOSE_ACCOUNT_FRAGMENT;
//		visibleFragment = Constants.CONFIRM_EMAIL_FRAGMENT;
        showFragment(visibleFragment);
    }

    public void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(relativeContainer, message, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    public void showFragment(int visibleFragment) {
        log("showFragment: " + visibleFragment);
        this.visibleFragment = visibleFragment;
        switch (visibleFragment) {
            case Constants.LOGIN_FRAGMENT: {
                log("showLoginFragment");
                if (loginFragment == null) {
                    loginFragment = new LoginFragmentLollipop();
                    if ((passwdTemp != null) && (emailTemp != null)) {
                        loginFragment.setEmailTemp(emailTemp);
                        loginFragment.setPasswdTemp(passwdTemp);
//						emailTemp = null;
//						passwdTemp = null;
//						nameTemp = null;
                    }
                } else {
                    if ((passwdTemp != null) && (emailTemp != null)) {
                        loginFragment.setEmailTemp(emailTemp);
                        loginFragment.setPasswdTemp(passwdTemp);
//						emailTemp = null;
//						passwdTemp = null;
//						nameTemp = null;
                    }
                }

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container_login, loginFragment);
                ft.commitNowAllowingStateLoss();

//
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
                break;
            }
            case Constants.CREATE_ACCOUNT_FRAGMENT: {
                log("Show CREATE_ACCOUNT_FRAGMENT");
                if (createAccountFragment == null) {
                    createAccountFragment = new CreateAccountFragmentLollipop();
                }

                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container_login, createAccountFragment);
                ft.commitNowAllowingStateLoss();
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
                break;
            }
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

    public void stopCameraSyncService() {
        log("stopCameraSyncService");
        dbH.clearPreferences();
        dbH.setFirstTime(false);
//					dbH.setPinLockEnabled(false);
//					dbH.setPinLockCode("");
//					dbH.setCamSyncEnabled(false);
        Intent stopIntent = null;
        stopIntent = new Intent(this, CameraSyncService.class);
        stopIntent.setAction(CameraSyncService.ACTION_LOGOUT);
        startService(stopIntent);
    }

    public void startCameraSyncService(boolean firstTimeCam, int time) {
        log("startCameraSyncService");
        Intent intent = null;
        if (firstTimeCam) {
            intent = new Intent(this, ManagerActivityLollipop.class);
            intent.putExtra("firstTimeCam", true);
            startActivity(intent);
            finish();
        } else {
            log("Enciendo el servicio de la camara");
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    log("Now I start the service");
                    startService(new Intent(getApplicationContext(), CameraSyncService.class));
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
        builder.setPositiveButton(R.string.general_cancel, dialogClickListener);
        builder.setNegativeButton(R.string.general_dismiss, dialogClickListener);

        builder.show();
    }

    @Override
    public void onBackPressed() {
        log("onBackPressed");

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
                //nothing to do
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

        Intent intent = getIntent();

        if (intent != null) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(Constants.ACTION_CANCEL_CAM_SYNC)) {
                    log("ACTION_CANCEL_CAM_SYNC");
                    Intent tempIntent = null;
                    String title = null;
                    String text = null;
                    if (intent.getAction().equals(Constants.ACTION_CANCEL_CAM_SYNC)) {
                        tempIntent = new Intent(this, CameraSyncService.class);
                        tempIntent.setAction(CameraSyncService.ACTION_CANCEL);
                        title = getString(R.string.cam_sync_syncing);
                        text = getString(R.string.cam_sync_cancel_sync);
                    }

                    final Intent cancelIntent = tempIntent;
                    AlertDialog.Builder builder = Util.getCustomAlertBuilder(this,
                            title, text, null);
                    builder.setPositiveButton(getString(R.string.general_yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    startService(cancelIntent);
                                }
                            });
                    builder.setNegativeButton(getString(R.string.general_no), null);
                    final AlertDialog dialog = builder.create();
                    try {
                        dialog.show();
                    } catch (Exception ex) {
                        startService(cancelIntent);
                    }
                } else if (intent.getAction().equals(Constants.ACTION_CANCEL_DOWNLOAD)) {
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

    public void showConfirmationEnableLogs() {
        log("showConfirmationEnableLogs");

        if (loginFragment != null) {
            loginFragment.numberOfClicks = 0;
        }
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        enableLogs();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:

                        break;
                }
            }
        };

        android.support.v7.app.AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            builder = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        } else {
            builder = new android.support.v7.app.AlertDialog.Builder(this);
        }

        builder.setMessage(R.string.enable_log_text_dialog).setPositiveButton(R.string.general_enable, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show().setCanceledOnTouchOutside(false);
    }

    public void enableLogs() {
        log("enableLogs");

        dbH.setFileLogger(true);
        Util.setFileLogger(true);
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX);
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

        if (request.getType() == MegaRequest.TYPE_CREATE_ACCOUNT){
            if (request.getParamType() == 1){
                if (e.getErrorCode() == MegaError.API_OK){
                    waitingForConfirmAccount = true;
                    visibleFragment = Constants.CONFIRM_EMAIL_FRAGMENT;
                    showFragment(visibleFragment);
                }
                else{
                    dbH.clearEphemeral();
                    waitingForConfirmAccount = false;
                    visibleFragment = Constants.LOGIN_FRAGMENT;
                    showFragment(visibleFragment);
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {
        log("onRequestTemporaryError - " + request.getRequestString());
    }

    @Override
    protected void onPostResume() {
        log("onPostResume");
        super.onPostResume();

    }
}
