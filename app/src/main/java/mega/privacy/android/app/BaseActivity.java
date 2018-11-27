package mega.privacy.android.app;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

public class BaseActivity extends AppCompatActivity implements View.OnClickListener{

    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private MegaApiAndroid megaApiFolder;

    private AlertDialog sslErrorDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");

        super.onCreate(savedInstanceState);
        checkMegaApiObjects();

        LocalBroadcastManager.getInstance(this).registerReceiver(sslErrorReceiver,
                new IntentFilter(Constants.BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED));
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

        retryConnectionsAndSignalPresence();
    }

    @Override
    protected void onDestroy() {
        log("onDestroy");

        LocalBroadcastManager.getInstance(this).unregisterReceiver(sslErrorReceiver);
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

    @Override
    public void onClick(View v) {
        retryConnectionsAndSignalPresence();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        retryConnectionsAndSignalPresence();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        retryConnectionsAndSignalPresence();
        return super.onOptionsItemSelected(item);
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

                if(megaChatApi.isSignalActivityRequired()){
                    megaChatApi.signalPresenceActivity();
                }
            }
        }
        catch (Exception e){
            log("retryPendingConnections:Exception: "+e.getMessage());
        }
    }

    /**
     * Local method to write a log message.
     * @param message Text to write in the log message.
     */
    private void log(String message) {
        Util.log("BaseActivityLollipop", message);
    }
}
