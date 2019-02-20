package mega.privacy.android.app;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
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

import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import mega.privacy.android.app.snackbarListeners.SnackbarNavigateOption;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

public class BaseActivity extends AppCompatActivity {

    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private MegaApiAndroid megaApiFolder;

    private AlertDialog sslErrorDialog;

    protected boolean callToSuperBack = false;
    boolean delaySignalPresence = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");

        super.onCreate(savedInstanceState);
        checkMegaApiObjects();

        LocalBroadcastManager.getInstance(this).registerReceiver(sslErrorReceiver,
                new IntentFilter(Constants.BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED));

        LocalBroadcastManager.getInstance(this).registerReceiver(signalPresenceReceiver,
                new IntentFilter(Constants.BROADCAST_ACTION_INTENT_SIGNAL_PRESENCE));
    }

    @Override
    protected void onPause() {
        log("onPause");

        checkMegaApiObjects();
        super.onPause();
    }

    @Override
    protected void onResume() {
        log("onResume");

        super.onResume();
        Util.setAppFontSize(this);

        checkMegaApiObjects();

        if(megaChatApi != null){
            if(megaChatApi.getPresenceConfig()==null){
                delaySignalPresence = true;
            }
            else{
                if(megaChatApi.getPresenceConfig().isPending()==true){
                    delaySignalPresence = true;
                }
                else{
                    delaySignalPresence = false;
                    retryConnectionsAndSignalPresence();
                }
            }
        }
        else{
            delaySignalPresence = false;
            retryConnectionsAndSignalPresence();
        }
    }

    @Override
    protected void onDestroy() {
        log("****onDestroy");

        LocalBroadcastManager.getInstance(this).unregisterReceiver(sslErrorReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(signalPresenceReceiver);

        super.onDestroy();
    }

    /**
     * Method to check if exist all required MegaApiAndroid and MegaChatApiAndroid objects
     * or create them if necessary.
     */
    private void checkMegaApiObjects() {
        log("checkMegaApiObjects");

        if (megaApi == null){
            megaApi = ((MegaApplication)getApplication()).getMegaApi();
        }

        if (megaApiFolder == null) {
            megaApiFolder = ((MegaApplication) getApplication()).getMegaApiFolder();
        }

        if(Util.isChatEnabled()){
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication)getApplication()).getMegaChatApi();
            }
        }
    }

    /**
     * Broadcast receiver to manage a possible SSL verification error.
     */
    private BroadcastReceiver sslErrorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                log("BROADCAST TO MANAGE A SSL VERIFICATION ERROR");
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
                log("****BROADCAST TO SEND SIGNAL PRESENCE");
                if(delaySignalPresence && megaChatApi.getPresenceConfig().isPending()==false){
                    delaySignalPresence = false;
                    retryConnectionsAndSignalPresence();
                }
            }
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

    public void retryConnectionsAndSignalPresence(){
        log("retryConnectionsAndSignalPresence");
        try{
            if (megaApi != null){
                megaApi.retryPendingConnections();
            }

            if(Util.isChatEnabled()){
                if (megaChatApi != null){
                    megaChatApi.retryPendingConnections(false, null);
                }

                if(!(this instanceof ChatCallActivity)){
                    log("Send signal presence if needed");
                    if(megaChatApi.isSignalActivityRequired()){
                        megaChatApi.signalPresenceActivity();
                    }
                }
            }
        }
        catch (Exception e){
            log("retryPendingConnections:Exception: "+e.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
        retryConnectionsAndSignalPresence();
        if(callToSuperBack){
            super.onBackPressed();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN ){
            retryConnectionsAndSignalPresence();
        }
        return super.dispatchTouchEvent(event);
    }

    public void showSnackbar (int type, View view, String s, long idChat) {
        log("showSnackbar: "+s);
        Display  display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        Snackbar snackbar = null;
        if (type == Constants.MESSAGE_SNACKBAR_TYPE && (s==null || s.isEmpty())) {
            snackbar = Snackbar.make(view, R.string.sent_as_message, Snackbar.LENGTH_LONG);
        }
        else if (type == Constants.NOT_SPACE_SNACKBAR_TYPE) {
            snackbar = Snackbar.make(view, R.string.error_not_enough_free_space, Snackbar.LENGTH_LONG);
        }
        else {
            snackbar = Snackbar.make(view, s, Snackbar.LENGTH_LONG);
        }

        Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
        snackbarLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.background_snackbar));

        if (snackbarLayout.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
            final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) snackbarLayout.getLayoutParams();
            params.setMargins(Util.px2dp(8, outMetrics),0,Util.px2dp(8, outMetrics), Util.px2dp(8, outMetrics));
            snackbarLayout.setLayoutParams(params);
        }
        else if (snackbarLayout.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarLayout.getLayoutParams();
            params.setMargins(mega.privacy.android.app.utils.Util.px2dp(8, outMetrics),0, mega.privacy.android.app.utils.Util.px2dp(8, outMetrics), mega.privacy.android.app.utils.Util.px2dp(8, outMetrics));
            snackbarLayout.setLayoutParams(params);
        }

        switch (type) {
            case Constants.SNACKBAR_TYPE: {
                TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                snackbarTextView.setMaxLines(5);
                snackbar.show();
                break;
            }
            case Constants.MESSAGE_SNACKBAR_TYPE: {
                snackbar.setAction("SEE", new SnackbarNavigateOption(this, idChat));
                snackbar.show();
                break;
            }
            case Constants.NOT_SPACE_SNACKBAR_TYPE: {
                snackbar.setAction("Settings", new SnackbarNavigateOption(this));
                snackbar.show();
                break;
            }
        }
    }

    public static void showSimpleSnackbar(Context context, DisplayMetrics outMetrics, View view, String s) {
        Snackbar snackbar = Snackbar.make(view, s, Snackbar.LENGTH_LONG);
        Snackbar.SnackbarLayout snackbarLayout = (Snackbar.SnackbarLayout) snackbar.getView();
        snackbarLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.background_snackbar));
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) snackbarLayout.getLayoutParams();
        params.setMargins(Util.px2dp(8, outMetrics),0,Util.px2dp(8, outMetrics), Util.px2dp(8, outMetrics));
        snackbarLayout.setLayoutParams(params);
        TextView snackbarTextView = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }

    /**
     * Local method to write a log message.
     * @param message Text to write in the log message.
     */
    private void log(String message) {
        Util.log("BaseActivityLollipop", message);
    }
}
