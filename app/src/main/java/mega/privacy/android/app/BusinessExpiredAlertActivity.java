package mega.privacy.android.app;

import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.globalmanagement.MyAccountInfo;
import mega.privacy.android.app.utils.ColorUtils;
import nz.mega.sdk.MegaApiAndroid;
import timber.log.Timber;

@AndroidEntryPoint
public class BusinessExpiredAlertActivity extends PasscodeActivity implements View.OnClickListener {

    private static final int IMAGE_HEIGHT_PORTRAIT = 284;
    private static final int IMAGE_HEIGHT_LANDSCAPE = 136;

    @Inject
    MyAccountInfo myAccountInfo;

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
            expiredLayoutParams.height = dp2px(IMAGE_HEIGHT_PORTRAIT, getOutMetrics());
        } else {
            expiredLayoutParams.height = dp2px(IMAGE_HEIGHT_LANDSCAPE, getOutMetrics());
        }

        expiredImageLayout.setLayoutParams(expiredLayoutParams);

        expiredImage = findViewById(R.id.expired_image);
        expiredText = findViewById(R.id.expired_text);
        expiredSubtext = findViewById(R.id.expired_subtext);

        RelativeLayout.LayoutParams expiredImageParams = (RelativeLayout.LayoutParams) expiredImage.getLayoutParams();

        if (megaApi.isMasterBusinessAccount()) {
            expiredImageLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.gradient_business_admin_expired_bg));
            expiredImageParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            if (isScreenInPortrait(this)) {
                expiredImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_expired_admin_portrait));
            } else {
                expiredImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_expired_admin_landscape));
            }
            expiredText.setText(R.string.expired_admin_business_text);
            expiredSubtext.setVisibility(View.GONE);
        } else {
            expiredImageLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.gradient_business_user_expired_bg));
            expiredImageParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            expiredImageParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
            if (isScreenInPortrait(this)) {
                expiredImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_expired_user_portrait));
            } else {
                expiredImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_account_expired_user_landscape));
            }
            String expiredString = getString(R.string.expired_user_business_text);
            try {
                expiredString = expiredString.replace("[B]", "<b><font color=\'" + ColorUtils.getColorHexString(this, R.color.grey_900_grey_100) + "\'>");
                expiredString = expiredString.replace("[/B]", "</font></b>");
            } catch (Exception e) {
                Timber.w(e, "Exception formatting string");
            }
            expiredText.setText(HtmlCompat.fromHtml(expiredString, HtmlCompat.FROM_HTML_MODE_LEGACY));
            expiredSubtext.setVisibility(View.VISIBLE);
        }

        expiredImage.setLayoutParams(expiredImageParams);

        expiredDismissButton = findViewById(R.id.expired_dismiss_button);
        expiredDismissButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.expired_dismiss_button: {
                finish();
                break;
            }
        }
    }

    @Override
    public void finish() {
        myAccountInfo.setBusinessAlertShown(false);
        super.finish();
    }
}
