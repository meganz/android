package mega.privacy.android.app;

import android.os.Bundle;

import mega.privacy.android.app.lollipop.PinActivityLollipop;

public class SMSVerificationActivity extends PinActivityLollipop {
    
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_verification);
    }
    
    @Override
    public void onBackPressed(){
        //todo finish activity as per scenario
        if(true){
            finish();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
