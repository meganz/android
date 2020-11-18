package mega.privacy.android.app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;

import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.listeners.ChatLogoutListener;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import mega.privacy.android.app.snackbarListeners.SnackbarNavigateOption;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaAccountDetails;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.lollipop.LoginFragmentLollipop.NAME_USER_LOCKED;
import static mega.privacy.android.app.constants.BroadcastConstants.*;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showResumeTransfersWarning;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.PermissionUtils.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.Util.*;
import static mega.privacy.android.app.utils.DBUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static nz.mega.sdk.MegaApiJava.*;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class BaseActivity extends AppCompatActivity {

    private static final String EXPIRED_BUSINESS_ALERT_SHOWN = "EXPIRED_BUSINESS_ALERT_SHOWN";
    private static final String TRANSFER_OVER_QUOTA_WARNING_SHOWN = "TRANSFER_OVER_QUOTA_WARNING_SHOWN";
    private static final String RESUME_TRANSFERS_WARNING_SHOWN = "RESUME_TRANSFERS_WARNING_SHOWN";

    private BaseActivity baseActivity;

    protected  MegaApplication app;

    protected MegaApiAndroid megaApi;
    protected MegaApiAndroid megaApiFolder;
    protected MegaChatApiAndroid megaChatApi;

    protected DatabaseHandler dbH;

    private AlertDialog sslErrorDialog;

    private boolean delaySignalPresence = false;

    //Indicates if app is requesting the required permissions to enable the SDK logger
    private boolean permissionLoggerSDK = false;
    //Indicates if app is requesting the required permissions to enable the Karere logger
    private boolean permissionLoggerKarere = false;

    private boolean isGeneralTransferOverQuotaWarningShown;
    private AlertDialog transferGeneralOverQuotaWarning;

    public BaseActivity() {
        app = MegaApplication.getInstance();

        //Will be checked again and initialized at `onCreate()`
        if (app != null) {
            megaApi = app.getMegaApi();
            megaApiFolder = app.getMegaApiFolder();
            megaChatApi = app.getMegaChatApi();
            dbH = app.getDbH();
        }
    }

    private AlertDialog expiredBusinessAlert;
    private boolean isExpiredBusinessAlertShown = false;

    private boolean isPaused = false;

    private DisplayMetrics outMetrics;

    //Indicates when the activity should finish due to some error
    private static boolean finishActivityAtError;

    private boolean isResumeTransfersWarningShown;
    private AlertDialog resumeTransfersWarning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        baseActivity = this;

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        super.onCreate(savedInstanceState);
        checkMegaObjects();

        registerReceiver(sslErrorReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED));

        registerReceiver(signalPresenceReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE));

        registerReceiver(accountBlockedReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_EVENT_ACCOUNT_BLOCKED));

        registerReceiver(businessExpiredReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED));

        registerReceiver(takenDownFilesReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_TAKEN_DOWN_FILES));

        registerReceiver(transferFinishedReceiver,
                new IntentFilter(BROADCAST_ACTION_INTENT_SHOWSNACKBAR_TRANSFERS_FINISHED));

        IntentFilter filterTransfers = new IntentFilter(BROADCAST_ACTION_INTENT_TRANSFER_UPDATE);
        filterTransfers.addAction(ACTION_TRANSFER_OVER_QUOTA);
        registerReceiver(transferOverQuotaReceiver, filterTransfers);

        registerReceiver(showSnackbarReceiver,
                new IntentFilter(BROADCAST_ACTION_SHOW_SNACKBAR));

        registerReceiver(resumeTransfersReceiver,
                new IntentFilter(BROADCAST_ACTION_RESUME_TRANSFERS));

        if (savedInstanceState != null) {
            isExpiredBusinessAlertShown = savedInstanceState.getBoolean(EXPIRED_BUSINESS_ALERT_SHOWN, false);
            if (isExpiredBusinessAlertShown) {
                showExpiredBusinessAlert();
            }

            isGeneralTransferOverQuotaWarningShown = savedInstanceState.getBoolean(TRANSFER_OVER_QUOTA_WARNING_SHOWN, false);
            if (isGeneralTransferOverQuotaWarningShown) {
                showGeneralTransferOverQuotaWarning();
            }

            isResumeTransfersWarningShown = savedInstanceState.getBoolean(RESUME_TRANSFERS_WARNING_SHOWN, false);
            if (isResumeTransfersWarningShown) {
                showResumeTransfersWarning(this);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(EXPIRED_BUSINESS_ALERT_SHOWN, isExpiredBusinessAlertShown);
        outState.putBoolean(TRANSFER_OVER_QUOTA_WARNING_SHOWN, isGeneralTransferOverQuotaWarningShown);
        outState.putBoolean(RESUME_TRANSFERS_WARNING_SHOWN, isResumeTransfersWarningShown);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        checkMegaObjects();
        isPaused = true;
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setAppFontSize(this);

        checkMegaObjects();
        isPaused = false;

        retryConnectionsAndSignalPresence();
    }

    /**
     * Checks if the current activity is in foreground.
     *
     * @return True if the current activity is in foreground, false otherwise.
     */
    protected boolean isActivityInForeground() {
        return !isPaused;
    }

    @Override
    protected void onDestroy() {

        unregisterReceiver(sslErrorReceiver);
        unregisterReceiver(signalPresenceReceiver);
        unregisterReceiver(accountBlockedReceiver);
        unregisterReceiver(businessExpiredReceiver);
        unregisterReceiver(takenDownFilesReceiver);
        unregisterReceiver(transferFinishedReceiver);
        unregisterReceiver(showSnackbarReceiver);
        unregisterReceiver(transferOverQuotaReceiver);
        unregisterReceiver(resumeTransfersReceiver);

        if (transferGeneralOverQuotaWarning != null) {
            transferGeneralOverQuotaWarning.dismiss();
        }

        if (resumeTransfersWarning != null) {
            resumeTransfersWarning.dismiss();
        }

        super.onDestroy();
    }

    /**
     * Method to check if exist all required objects (MegaApplication, MegaApiAndroid and MegaChatApiAndroid )
     * or create them if necessary.
     */
    private void checkMegaObjects() {

        if (app == null) {
            app = MegaApplication.getInstance();
        }

        if (app != null) {
            if (megaApi == null){
                megaApi = app.getMegaApi();
            }

            if (megaApiFolder == null) {
                megaApiFolder = app.getMegaApiFolder();
            }

            if (megaChatApi == null){
                megaChatApi = app.getMegaChatApi();
            }

            if (dbH == null) {
                dbH = app.getDbH();
            }
        }
    }

    /**
     * Broadcast receiver to manage the errors shown and actions when an account is blocked.
     */
    private BroadcastReceiver accountBlockedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null
                    || !intent.getAction().equals(BROADCAST_ACTION_INTENT_EVENT_ACCOUNT_BLOCKED))
                return;

            checkWhyAmIBlocked(intent.getLongExtra(EVENT_NUMBER, -1), intent.getStringExtra(EVENT_TEXT));
        }
    };

    /**
     * Broadcast receiver to manage a possible SSL verification error.
     */
    private BroadcastReceiver sslErrorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                logDebug("BROADCAST TO MANAGE A SSL VERIFICATION ERROR");
                if (sslErrorDialog != null && sslErrorDialog.isShowing()) return;
                showSSLErrorDialog();
            }
        }
    };

    /**
     * Broadcast to send presence after first launch of app
     */
    private BroadcastReceiver signalPresenceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                logDebug("BROADCAST TO SEND SIGNAL PRESENCE");
                if(delaySignalPresence && megaChatApi != null && megaChatApi.getPresenceConfig() != null && !megaChatApi.getPresenceConfig().isPending()){
                    delaySignalPresence = false;
                    retryConnectionsAndSignalPresence();
                }
            }
        }
    };

    private BroadcastReceiver businessExpiredReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                showExpiredBusinessAlert();
            }
        }
    };

    /**
     * Broadcast to show taken down files info
     */
    private BroadcastReceiver takenDownFilesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            logDebug("BROADCAST INFORM THERE ARE TAKEN DOWN FILES IMPLIED IN ACTION");
            int numberFiles = intent.getIntExtra(NUMBER_FILES, 1);
            Util.showSnackbar(baseActivity, getResources().getQuantityString(R.plurals.alert_taken_down_files, numberFiles, numberFiles));
        }
    };

    /**
     * Broadcast to show a snackbar when all the transfers finish
     */
    private BroadcastReceiver transferFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || isPaused) {
                return;
            }

            if (intent.getBooleanExtra(FILE_EXPLORER_CHAT_UPLOAD, false)) {
                Util.showSnackbar(baseActivity, MESSAGE_SNACKBAR_TYPE, null, intent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE));
                return;
            }

            String message = null;
            int numTransfers = intent.getIntExtra(NUMBER_FILES, 1);

            switch (intent.getStringExtra(TRANSFER_TYPE)) {
                case DOWNLOAD_TRANSFER:
                    message = getResources().getQuantityString(R.plurals.download_finish, numTransfers, numTransfers);
                    break;

                case UPLOAD_TRANSFER:
                    message = getResources().getQuantityString(R.plurals.upload_finish, numTransfers, numTransfers);
                    break;
            }

            Util.showSnackbar(baseActivity, message);
        }
    };

    private BroadcastReceiver showSnackbarReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isPaused || intent == null || intent.getAction() == null
                    || !intent.getAction().equals(BROADCAST_ACTION_SHOW_SNACKBAR))
                return;

            String message = intent.getStringExtra(SNACKBAR_TEXT);
            if (!isTextEmpty(message)) {
                Util.showSnackbar(baseActivity, message);
            }
        }
    };

    /**
     * Broadcast to show a warning when transfer over quota occurs.
     */
    private BroadcastReceiver transferOverQuotaReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null
                    || !intent.getAction().equals(ACTION_TRANSFER_OVER_QUOTA)
                    || !isActivityInForeground()) {
                return;
            }

            showGeneralTransferOverQuotaWarning();
        }
    };

    /**
     * Broadcast to show a warning when it tries to upload files to a chat conversation
     * and the transfers are paused.
     */
    private BroadcastReceiver resumeTransfersReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null
                    || !intent.getAction().equals(BROADCAST_ACTION_RESUME_TRANSFERS)
                    || isResumeTransfersWarningShown()
                    || !isActivityInForeground()) {
                return;
            }

            MegaApplication.getTransfersManagement().setResumeTransfersWarningHasAlreadyBeenShown(true);
            showResumeTransfersWarning(baseActivity);
        }
    };

    /**
     * Method to display an alert dialog indicating that the MEGA SSL key
     * can't be verified (API_ESSL Error) and giving the user several options.
     */
    private void showSSLErrorDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_three_vertical_buttons, null);
        builder.setView(v);

        TextView title = v.findViewById(R.id.dialog_title);
        TextView text = v.findViewById(R.id.dialog_text);

        Button retryButton = v.findViewById(R.id.dialog_first_button);
        Button openBrowserButton = v.findViewById(R.id.dialog_second_button);
        Button dismissButton = v.findViewById(R.id.dialog_third_button);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.RIGHT;

        title.setText(R.string.ssl_error_dialog_title);
        text.setText(R.string.ssl_error_dialog_text);
        retryButton.setText(R.string.general_retry);
        openBrowserButton.setText(R.string.general_open_browser);
        dismissButton.setText(R.string.general_dismiss);

        sslErrorDialog = builder.create();
        sslErrorDialog.setCancelable(false);
        sslErrorDialog.setCanceledOnTouchOutside(false);

        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sslErrorDialog.dismiss();
                megaApi.reconnect();
                megaApiFolder.reconnect();
            }
        });

        openBrowserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sslErrorDialog.dismiss();
                Uri uriUrl = Uri.parse("https://mega.nz/");
                Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
                startActivity(launchBrowser);
            }
        });

        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sslErrorDialog.dismiss();
                megaApi.setPublicKeyPinning(false);
                megaApi.reconnect();
                megaApiFolder.setPublicKeyPinning(false);
                megaApiFolder.reconnect();
            }
        });

        sslErrorDialog.show();
    }

    protected void retryConnectionsAndSignalPresence(){
        logDebug("retryConnectionsAndSignalPresence");
        try{
            if (megaApi != null){
                megaApi.retryPendingConnections();
            }

            if (megaChatApi != null) {
                megaChatApi.retryPendingConnections(false, null);

                if (megaChatApi.getPresenceConfig() != null && !megaChatApi.getPresenceConfig().isPending()) {
                    delaySignalPresence = false;
                    if (!(this instanceof ChatCallActivity) && megaChatApi.isSignalActivityRequired()) {
                        logDebug("Send signal presence");
                        megaChatApi.signalPresenceActivity();
                    }
                } else {
                    delaySignalPresence = true;
                }
            }
        }
        catch (Exception e){
            logWarning("Exception", e);
        }
    }

    @Override
    public void onBackPressed() {
        retryConnectionsAndSignalPresence();
        super.onBackPressed();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN ){
            retryConnectionsAndSignalPresence();
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * Method to display a simple Snackbar.
     *
     * @param view Layout where the snackbar is going to show.
     * @param s Text to shown in the snackbar
     */
    public void showSnackbar (View view, String s) {
        showSnackbar(SNACKBAR_TYPE, view, s, -1);
    }

    /**
     * Method to display a simple or action Snackbar.
     *
     * @param type There are three possible values to this param:
     *            - SNACKBAR_TYPE: creates a simple snackbar
     *            - MESSAGE_SNACKBAR_TYPE: creates an action snackbar which function is to go to Chat section
     *            - NOT_SPACE_SNACKBAR_TYPE: creates an action snackbar which function is to go to Storage-Settings section
     * @param view Layout where the snackbar is going to show.
     * @param s Text to shown in the snackbar
     */
    public void showSnackbar (int type, View view, String s) {
        showSnackbar(type, view, s, -1);
    }

    /**
     * Method to display a simple or action Snackbar.
     *
     * @param type There are three possible values to this param:
     *            - SNACKBAR_TYPE: creates a simple snackbar
     *            - MESSAGE_SNACKBAR_TYPE: creates an action snackbar which function is to go to Chat section
     *            - NOT_SPACE_SNACKBAR_TYPE: creates an action snackbar which function is to go to Storage-Settings section
     * @param view Layout where the snackbar is going to show.
     * @param s Text to shown in the snackbar
     * @param idChat Chat ID. If this param has a valid value the function of MESSAGE_SNACKBAR_TYPE ends in the specified chat.
     *               If the value is -1 (INVALID_HANLDE) the function ends in chats list view.
     */
    public void showSnackbar (int type, View view, String s, long idChat) {
        logDebug("Show snackbar: " + s);
        Display  display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        Snackbar snackbar;
        try {
            switch (type) {
                case MESSAGE_SNACKBAR_TYPE:
                    snackbar = Snackbar.make(view, !isTextEmpty(s) ? s : getString(R.string.sent_as_message), Snackbar.LENGTH_LONG);
                    break;
                case NOT_SPACE_SNACKBAR_TYPE:
                    snackbar = Snackbar.make(view, R.string.error_not_enough_free_space, Snackbar.LENGTH_LONG);
                    break;
                case MUTE_NOTIFICATIONS_SNACKBAR_TYPE:
                    snackbar = Snackbar.make(view, R.string.notifications_are_already_muted, Snackbar.LENGTH_LONG);
                    break;
                default:
                    snackbar = Snackbar.make(view, s, Snackbar.LENGTH_LONG);
                    break;
            }
        } catch (Exception e) {
            logError("Error showing snackbar", e);
            return;
        }

        Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
        snackbarLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.background_snackbar));

        if (snackbarLayout.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
            final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) snackbarLayout.getLayoutParams();
            params.setMargins(dp2px(8, outMetrics),0, dp2px(8, outMetrics), dp2px(8, outMetrics));
            snackbarLayout.setLayoutParams(params);
        }
        else if (snackbarLayout.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarLayout.getLayoutParams();
            params.setMargins(dp2px(8, outMetrics),0, dp2px(8, outMetrics), dp2px(8, outMetrics));
            snackbarLayout.setLayoutParams(params);
        }

        switch (type) {
            case SNACKBAR_TYPE: {
                TextView snackbarTextView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                snackbarTextView.setMaxLines(5);
                snackbar.show();
                break;
            }
            case MESSAGE_SNACKBAR_TYPE: {
                snackbar.setAction(R.string.action_see, new SnackbarNavigateOption(view.getContext(), idChat));
                snackbar.show();
                break;
            }
            case NOT_SPACE_SNACKBAR_TYPE: {
                snackbar.setAction(R.string.action_settings, new SnackbarNavigateOption(view.getContext()));
                snackbar.show();
                break;
            }
            case MUTE_NOTIFICATIONS_SNACKBAR_TYPE:
                snackbar.setAction(R.string.general_unmute, new SnackbarNavigateOption(view.getContext(), MUTE_NOTIFICATIONS_SNACKBAR_TYPE));
                snackbar.show();
                break;
        }
    }

    /**
     * Method to display a simple Snackbar.
     *
     * @param context Context of the Activity where the snackbar has to be displayed
     * @param outMetrics DisplayMetrics of the current device
     * @param view Layout where the snackbar is going to show.
     * @param s Text to shown in the snackbar
     */
    public static void showSimpleSnackbar(Context context, DisplayMetrics outMetrics, View view, String s) {
        Snackbar snackbar = Snackbar.make(view, s, Snackbar.LENGTH_LONG);
        Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
        snackbarLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.background_snackbar));
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarLayout.getLayoutParams();
        params.setMargins(dp2px(8, outMetrics),0, dp2px(8, outMetrics), dp2px(8, outMetrics));
        snackbarLayout.setLayoutParams(params);
        TextView snackbarTextView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    /**
     * Method to refresh the account details info if necessary.
     */
    protected void refreshAccountInfo(){
        logDebug("refreshAccountInfo");

        //Check if the call is recently
        logDebug("Check the last call to getAccountDetails");
        if(callToAccountDetails()){
            logDebug("megaApi.getAccountDetails SEND");
            app.askForAccountDetails();
        }
    }

    /**
     * This method is shown in a business account when the account is expired.
     * It informs that all the actions are only read.
     * The message is different depending if the account belongs to an admin or an user.
     *
     */
    private void showExpiredBusinessAlert(){
        if (isPaused || (expiredBusinessAlert != null && expiredBusinessAlert.isShowing())) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyleNormal);
        builder.setTitle(R.string.expired_business_title);

        if (megaApi.isMasterBusinessAccount()) {
            builder.setMessage(R.string.expired_admin_business_text);
        } else {
            String expiredString = getString(R.string.expired_user_business_text);
            try {
                expiredString = expiredString.replace("[B]", "<b><font color=\'#000000\'>");
                expiredString = expiredString.replace("[/B]", "</font></b>");
            } catch (Exception e) {
                logWarning("Exception formatting string", e);
            }
            builder.setMessage(TextUtils.concat(HtmlCompat.fromHtml(expiredString, HtmlCompat.FROM_HTML_MODE_LEGACY), "\n\n" + getString(R.string.expired_user_business_text_2)));
        }

        builder.setNegativeButton(R.string.general_dismiss, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isExpiredBusinessAlertShown = false;
                if (finishActivityAtError) {
                    finishActivityAtError = false;
                    finish();
                }
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        expiredBusinessAlert = builder.create();
        expiredBusinessAlert.show();
        isExpiredBusinessAlertShown = true;
    }

    /**
     * Method to show an alert or error when the account has been suspended
     * for any reason
     *
     * @param eventNumber long that determines the event for which the account has been suspended
     * @param stringError string shown as an alert in case there is not any specific action for the event
     */
    public void checkWhyAmIBlocked(long eventNumber, String stringError) {
        Intent intent;

        switch (Long.toString(eventNumber)) {
            case ACCOUNT_NOT_BLOCKED:
//                I am not blocked
                break;
            case COPYRIGHT_ACCOUNT_BLOCK:
                megaChatApi.logout(new ChatLogoutListener(this, getString(R.string.account_suspended_breache_ToS)));
                break;
            case MULTIPLE_COPYRIGHT_ACCOUNT_BLOCK:
                megaChatApi.logout(new ChatLogoutListener(this, getString(R.string.account_suspended_multiple_breaches_ToS)));
                break;

            case DISABLED_BUSINESS_ACCOUNT_BLOCK:
                megaChatApi.logout(new ChatLogoutListener(this, getString(R.string.error_business_disabled)));
                break;

            case REMOVED_BUSINESS_ACCOUNT_BLOCK:
                megaChatApi.logout(new ChatLogoutListener(this, getString(R.string.error_business_removed)));
                break;

            case SMS_VERIFICATION_ACCOUNT_BLOCK:
                if (megaApi.smsAllowedState() == 0 || MegaApplication.isVerifySMSShowed()) return;

                MegaApplication.smsVerifyShowed(true);
                String gSession = megaApi.dumpSession();
                //For first login, keep the valid session,
                //after added phone number, the account can use this session to fastLogin
                if (gSession != null) {
                    MegaUser myUser = megaApi.getMyUser();
                    String myUserHandle = null;
                    String lastEmail = null;
                    if (myUser != null) {
                        lastEmail = myUser.getEmail();
                        myUserHandle = myUser.getHandle() + "";
                    }
                    UserCredentials credentials = new UserCredentials(lastEmail, gSession, "", "", myUserHandle);
                    dbH.saveCredentials(credentials);
                }

                logDebug("Show SMS verification activity.");
                intent = new Intent(getApplicationContext(), SMSVerificationActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(NAME_USER_LOCKED, true);
                startActivity(intent);
                break;

            case WEAK_PROTECTION_ACCOUNT_BLOCK:
                if (MegaApplication.isBlockedDueToWeakAccount() || MegaApplication.isWebOpenDueToEmailVerification()) {
                    break;
                }
                intent = new Intent(this, WeakAccountProtectionAlertActivity.class);
                startActivity(intent);
                break;

            default:
                showErrorAlertDialog(stringError, false, this);
        }
    }

    public DisplayMetrics getOutMetrics() {
        return outMetrics;
    }

    protected void setFinishActivityAtError(boolean finishActivityAtError) {
        BaseActivity.finishActivityAtError = finishActivityAtError;
    }

    protected boolean isBusinessExpired() {
        return megaApi.isBusinessAccount() && megaApi.getBusinessStatus() == BUSINESS_STATUS_EXPIRED;
    }

    /**
     * Shows a dialog to confirm enable the SDK logs.
     */
    protected void showConfirmationEnableLogsSDK() {
        showConfirmationEnableLogs(true, false);
    }

    /**
     * Shows a dialog to confirm enable the Karere logs.
     */
    protected void showConfirmationEnableLogsKarere() {
        showConfirmationEnableLogs(false, true);
    }

    /**
     * Shows a dialog to confirm enable the SDK and/or Karere logs.
     * @param sdk True to confirm enable the SDK logs or false otherwise.
     * @param karere True to confirm enable the Karere logs or false otherwise.
     */
    private void showConfirmationEnableLogs(boolean sdk, boolean karere) {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    if (!hasPermissions(baseActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        permissionLoggerSDK = sdk;
                        permissionLoggerKarere = karere;
                        requestPermission(baseActivity, REQUEST_WRITE_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        break;
                    }
                    if (sdk) {
                        setStatusLoggerSDK(baseActivity, true);
                    }
                    if (karere) {
                        setStatusLoggerKarere(baseActivity, true);
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        };

        androidx.appcompat.app.AlertDialog.Builder builder;
        builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        builder.setMessage(R.string.enable_log_text_dialog).setPositiveButton(R.string.general_enable, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show().setCanceledOnTouchOutside(false);
    }

    /**
     * Shows a warning indicating transfer over quota occurred.
     */
    public void showGeneralTransferOverQuotaWarning() {
        if (MegaApplication.getTransfersManagement().isOnTransfersSection() || transferGeneralOverQuotaWarning != null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        View dialogView = this.getLayoutInflater().inflate(R.layout.transfer_overquota_layout, null);
        builder.setView(dialogView)
                .setOnDismissListener(dialog -> {
                    isGeneralTransferOverQuotaWarningShown = false;
                    transferGeneralOverQuotaWarning = null;
                    MegaApplication.getTransfersManagement().resetTransferOverQuotaTimestamp();
                })
                .setCancelable(false);

        transferGeneralOverQuotaWarning = builder.create();
        transferGeneralOverQuotaWarning.setCanceledOnTouchOutside(false);

        TextView text = dialogView.findViewById(R.id.text_transfer_overquota);
        final int stringResource = MegaApplication.getTransfersManagement().isCurrentTransferOverQuota() ? R.string.current_text_depleted_transfer_overquota : R.string.text_depleted_transfer_overquota;
        text.setText(getString(stringResource, getHumanizedTime(megaApi.getBandwidthOverquotaDelay())));

        Button dismissButton = dialogView.findViewById(R.id.transfer_overquota_button_dissmiss);
        dismissButton.setOnClickListener(v -> transferGeneralOverQuotaWarning.dismiss());

        Button paymentButton = dialogView.findViewById(R.id.transfer_overquota_button_payment);

        final boolean isLoggedIn = megaApi.isLoggedIn() != 0 && dbH.getCredentials() != null;
        if (isLoggedIn) {
            boolean isFreeAccount = MegaApplication.getInstance().getMyAccountInfo().getAccountType() == MegaAccountDetails.ACCOUNT_TYPE_FREE;
            paymentButton.setText(getString(isFreeAccount ? R.string.my_account_upgrade_pro : R.string.plans_depleted_transfer_overquota));
        } else {
            paymentButton.setText(getString(R.string.login_text));
        }

        paymentButton.setOnClickListener(v -> {
            transferGeneralOverQuotaWarning.dismiss();

            if (isLoggedIn) {
                navigateToUpgradeAccount();
            } else {
                navigateToLogin();
            }
        });

        createAndShowCountDownTimer(stringResource, transferGeneralOverQuotaWarning, text);
        transferGeneralOverQuotaWarning.show();
        isGeneralTransferOverQuotaWarningShown = true;
    }

    /**
     * Launches an intent to navigate to Login screen.
     */
    protected void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivityLollipop.class);
        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /**
     * Launches an intent to navigate to Upgrade Account screen.
     */
    protected void navigateToUpgradeAccount() {
        Intent intent = new Intent(this, ManagerActivityLollipop.class);
        intent.setAction(ACTION_SHOW_UPGRADE_ACCOUNT);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        logDebug("Request Code: " + requestCode);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (permissionLoggerKarere) {
                permissionLoggerKarere = false;
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setStatusLoggerKarere(baseActivity, true);
                } else {
                    Util.showSnackbar(baseActivity, getString(R.string.logs_not_enabled_permissions));
                }
            } else if (permissionLoggerSDK) {
                permissionLoggerSDK = false;
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setStatusLoggerSDK(baseActivity, true);
                } else {
                    Util.showSnackbar(baseActivity, getString(R.string.logs_not_enabled_permissions));
                }
            }
        }
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        try {
            //If the receiver is not registered, it throws an IllegalArgumentException
            super.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            logWarning("IllegalArgumentException unregistering transfersUpdateReceiver", e);
        }
    }

    public boolean isResumeTransfersWarningShown() {
        return isResumeTransfersWarningShown;
    }

    public void setIsResumeTransfersWarningShown(boolean isResumeTransfersWarningShown) {
        this.isResumeTransfersWarningShown = isResumeTransfersWarningShown;
    }

    public void setResumeTransfersWarning(AlertDialog resumeTransfersWarning) {
        this.resumeTransfersWarning = resumeTransfersWarning;
    }

    public AlertDialog getResumeTransfersWarning() {
        return resumeTransfersWarning;
    }
}
