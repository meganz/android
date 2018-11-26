package mega.privacy.android.app.fcm;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;


import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;

public class TestActivity extends AppCompatActivity {
    
    Ringtone ringtone;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("FCM fake call UI on create");
        super.onCreate(savedInstanceState);
    
        setContentView(R.layout.test_activity_layout);
    
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        
        Button button = findViewById(R.id.dismiss_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(this,uri);
    
        ring();
        vibrate();
    }
    
    private void ring(){
        log("ring");
        ringtone.play();
    }
    
    private void stopRing(){
        log("stopRing");
        ringtone.stop();
    }
    
    private void vibrate(){
        log("vibrate");
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(2000);
        }
    }
    
    private void dismiss(){
        log("finish");
        stopRing();
        finish();
    }
    
    private void log(String message) {
        Util.log("FAKE CALL UI ACTIVITY", "FCM " + message);
    }
}
