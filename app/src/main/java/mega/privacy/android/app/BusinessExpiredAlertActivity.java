package mega.privacy.android.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import mega.privacy.android.app.lollipop.PinActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;

public class BusinessExpiredAlertActivity extends PinActivityLollipop implements View.OnClickListener {

    private MegaApiAndroid megaApi;

    private ImageView expiredImage;
    private TextView expiredText;
    private Button expiredDismissButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        megaApi = MegaApplication.getInstance().getMegaApi();

        setContentView(R.layout.activity_business_expired_alert);

        expiredImage = findViewById(R.id.expired_image);
        expiredText = findViewById(R.id.expired_text);

        if (megaApi.isMasterBusinessAccount()) {
            expiredImage.setImageDrawable(getDrawable(R.drawable.account_expired_admin));
            expiredText.setText(R.string.expired_admin_business_text);
            getWindow().setStatusBarColor(getResources().getColor(R.color.expired_business_admin));
        } else {
            expiredImage.setImageDrawable(getDrawable(R.drawable.account_expired_user));


            expiredText.setText(R.string.expired_user_business_text);
            getWindow().setStatusBarColor(getResources().getColor(R.color.expired_business_user));
        }

        expiredDismissButton = findViewById(R.id.expired_dismiss_button);
        expiredDismissButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.expired_dismiss_button:{
                finish();
                break;
            }
        }
    }
}
