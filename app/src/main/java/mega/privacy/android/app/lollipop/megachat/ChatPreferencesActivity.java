package mega.privacy.android.app.lollipop.megachat;


import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.PinActivityLollipop;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ChatPreferencesActivity extends PinActivityLollipop {

    FrameLayout fragmentContainer;
    SettingsChatFragment sttChat;
    Toolbar tB;
    ActionBar aB;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        logDebug("onCreate");

        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.lollipop_dark_primary_color));
        }

        setContentView(R.layout.activity_chat_settings);

        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container);

        tB = (Toolbar) findViewById(R.id.toolbar_chat_settings);
        if(tB==null){
            logError("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        logDebug("aB.setHomeAsUpIndicator");
        aB.setTitle(getString(R.string.section_chat));
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        sttChat = new SettingsChatFragment();
        ft.replace(R.id.fragment_container, sttChat);
        ft.commit();
    }

    public void changeSound(String soundString){
        logDebug("Sound string: " + soundString);
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.notification_sound_title));

        if (soundString == null){
            logWarning("NULL sound");
            Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, defaultSoundUri);
        }
        else if(soundString.equals("-1")){
            logWarning("Notification sound -1");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
        }
        else if(soundString.isEmpty()){
            logWarning("Empty sound");
            Uri defaultSoundUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, defaultSoundUri);
        }
        else{
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(soundString));
        }


        this.startActivityForResult(intent, SELECT_NOTIFICATION_SOUND);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        logDebug("Result code: " + resultCode);

        if (resultCode == RESULT_OK && requestCode == SELECT_NOTIFICATION_SOUND)
        {
            logDebug("Selected notification sound OK");

            Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

            if(sttChat!=null){
                if(sttChat.isAdded()){
                    sttChat.setNotificationSound(uri);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        logDebug("onOptionsItemSelectedLollipop");
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                finish();
            }
        }
        return true;
    }
}
