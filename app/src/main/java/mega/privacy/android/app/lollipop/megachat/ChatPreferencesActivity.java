package mega.privacy.android.app.lollipop.megachat;


import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import mega.privacy.android.app.R;

import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;

public class ChatPreferencesActivity extends AppCompatActivity {

    FrameLayout fragmentContainer;
    SettingsChatFragment sttChat;
    Toolbar tB;
    ActionBar aB;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        log("onCreate");

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
            log("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        log("aB.setHomeAsUpIndicator_1");
        aB.setTitle(getString(R.string.section_chat));
        aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);


//        getFragmentManager().beginTransaction().replace(android.R.id.fragmentContainer, new MyPreferenceFragment()).commit();

//        FragmentManager fm = getSupportFragmentManager();
//        fm.beginTransaction().replace(R.id.fragment_container, new SettingsChatFragment()).commit();

        android.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
        sttChat = new SettingsChatFragment();
        ft.replace(R.id.fragment_container, sttChat);
        ft.commit();
    }

    public void changeSound(){
        log("Change sound");
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.notification_sound_title));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
        this.startActivityForResult(intent, Constants.SELECT_NOTIFICATION_SOUND);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        log("onActivityResult, resultCode: " + resultCode);

        if (resultCode == RESULT_OK && requestCode == Constants.SELECT_NOTIFICATION_SOUND)
        {
            log("Selected notification sound OK");

            Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                if(sttChat!=null){
                    if(sttChat.isAdded()){
                        sttChat.setNotificationSound(uri);
                    }
                }

            }
            else{
                log("Error uri NULL");
            }

        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelectedLollipop");
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home: {
                finish();
            }
        }
        return true;
    }

    private static void log(String log) {
        Util.log("ChatPreferencesActivity", log);
    }

}
