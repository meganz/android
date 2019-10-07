package mega.privacy.android.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import mega.privacy.android.app.lollipop.PinActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class BusinessExpiredAlertActivity extends PinActivityLollipop implements View.OnClickListener {

    private MegaApiAndroid megaApi;

    private ImageView expiredImage;
    private TextView expiredText;
    private TextView expiredSubtext;
    private Button expiredDismissButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        megaApi = MegaApplication.getInstance().getMegaApi();

        setContentView(R.layout.activity_business_expired_alert);

        expiredImage = findViewById(R.id.expired_image);
        expiredText = findViewById(R.id.expired_text);
        expiredSubtext = findViewById(R.id.expired_subtext);

        if (megaApi.isMasterBusinessAccount()) {
            expiredImage.setImageDrawable(getDrawable(R.drawable.account_expired_admin));
            expiredText.setText(R.string.expired_admin_business_text);
            expiredSubtext.setVisibility(View.GONE);
            getWindow().setStatusBarColor(getResources().getColor(R.color.expired_business_admin));
        } else {
            expiredImage.setImageDrawable(getDrawable(R.drawable.account_expired_user));
            String expiredString = getString(R.string.expired_user_business_text);
            try {
                expiredString = expiredString.replace("[B]", "<b><font color=\'#000000\'>");
                expiredString = expiredString.replace("[/B]", "</font></b>");
            } catch (Exception e) {
                logWarning("Exception formatting string", e);
            }
            expiredText.setText(getSpannedHtmlText(expiredString));
            expiredSubtext.setVisibility(View.VISIBLE);
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
