package mega.privacy.android.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class BusinessExpiredAlertActivity extends PinActivityLollipop implements View.OnClickListener {

    private static final int IMAGE_HEIGHT_PORTRAIT = 284;
    private static final int IMAGE_HEIGHT_LANDSCAPE = 136;

    private MegaApiAndroid megaApi;

    private RelativeLayout expiredImageLayout;
    private ImageView expiredImage;
    private TextView expiredText;
    private TextView expiredSubtext;
    private Button expiredDismissButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        megaApi = MegaApplication.getInstance().getMegaApi();

        setContentView(R.layout.activity_business_expired_alert);

        expiredImageLayout = findViewById(R.id.expired_image_layout);
        LinearLayout.LayoutParams expiredLayoutParams = (LinearLayout.LayoutParams) expiredImageLayout.getLayoutParams();

        if (isScreenInPortrait(this)) {
            expiredLayoutParams.height = px2dp(IMAGE_HEIGHT_PORTRAIT, getOutMetrics());
        } else {
            expiredLayoutParams.height = px2dp(IMAGE_HEIGHT_LANDSCAPE, getOutMetrics());
        }

        expiredImageLayout.setLayoutParams(expiredLayoutParams);

        expiredImage = findViewById(R.id.expired_image);
        expiredText = findViewById(R.id.expired_text);
        expiredSubtext = findViewById(R.id.expired_subtext);

        RelativeLayout.LayoutParams expiredImageParams = (RelativeLayout.LayoutParams) expiredImage.getLayoutParams();

        if (megaApi.isMasterBusinessAccount()) {
            expiredImageLayout.setBackgroundColor(getResources().getColor(R.color.expired_business_admin));
            expiredImageParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            if (isScreenInPortrait(this)) {
                expiredImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_expired_admin_portrait));
            } else {
                expiredImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_expired_admin_landscape));
            }
            expiredText.setText(R.string.expired_admin_business_text);
            expiredSubtext.setVisibility(View.GONE);
            getWindow().setStatusBarColor(getResources().getColor(R.color.expired_business_admin_statusbar));
        } else {
            expiredImageLayout.setBackgroundColor(getResources().getColor(R.color.expired_business_user));
            expiredImageParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            expiredImageParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            if (isScreenInPortrait(this)) {
                expiredImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_expired_user_portrait));
            } else {
                expiredImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_expired_user_landscape));
            }
            String expiredString = getString(R.string.expired_user_business_text);
            try {
                expiredString = expiredString.replace("[B]", "<b><font color=\'#000000\'>");
                expiredString = expiredString.replace("[/B]", "</font></b>");
            } catch (Exception e) {
                logWarning("Exception formatting string", e);
            }
            expiredText.setText(getSpannedHtmlText(expiredString));
            expiredSubtext.setVisibility(View.VISIBLE);
            getWindow().setStatusBarColor(getResources().getColor(R.color.expired_business_user_statusbar));
        }

        expiredImage.setLayoutParams(expiredImageParams);

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

    @Override
    public void finish() {
        MyAccountInfo myAccountInfo = MegaApplication.getInstance().getMyAccountInfo();
        if (myAccountInfo != null) {
            myAccountInfo.setBusinessAlertShown(false);
        }

        super.finish();
    }
}
