package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.SMSVerificationActivity;
import mega.privacy.android.app.utils.Constants;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.getSizeString;


public class SMSVerificationFragment extends Fragment implements View.OnClickListener, MegaRequestListenerInterface {

    private Context context;

    private ManagerActivityLollipop managerActivity;

    private MegaApiJava megaApi;

    private String bonusStorageSMS = "20 GB";

    private TextView msg;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        megaApi = MegaApplication.getInstance().getMegaApi();

        View v = inflater.inflate(R.layout.fragment_sms_verification, container, false);
        msg = v.findViewById(R.id.sv_dialog_msg);
        v.findViewById(R.id.enable_button).setOnClickListener(this);
        v.findViewById(R.id.not_now_button_2).setOnClickListener(this);

        boolean isAchievementUser = megaApi.isAchievementsEnabled();
        logDebug("is achievement user: " + isAchievementUser);
        if (isAchievementUser) {
            megaApi.getAccountAchievements(this);
            String message = String.format(getString(R.string.sms_add_phone_number_dialog_msg_achievement_user), bonusStorageSMS);
            msg.setText(message);
        } else {
            msg.setText(R.string.sms_add_phone_number_dialog_msg_non_achievement_user);
        }
        ((ManagerActivityLollipop) context).changeStatusBarColor(Constants.COLOR_STATUS_BAR_SMS_VERIFICATION);
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof ManagerActivityLollipop) {
            this.managerActivity = (ManagerActivityLollipop) context;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.enable_button:
                logDebug("To sms verification");
                startActivity(new Intent(context, SMSVerificationActivity.class));
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fadeOut();
                    }
                }, 1000);
                break;
            case R.id.not_now_button_2:
                logDebug("Don't verify now");
                fadeOut();
                break;
        }
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if(request.getType() == MegaRequest.TYPE_GET_ACHIEVEMENTS) {
            if (e.getErrorCode() == MegaError.API_OK) {
                bonusStorageSMS = getSizeString(request.getMegaAchievementsDetails().getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE));
            }
            String message = String.format(getString(R.string.sms_add_phone_number_dialog_msg_achievement_user), bonusStorageSMS);
            msg.setText(message);
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    private void fadeOut() {
        if (managerActivity.firstTimeAfterInstallation) {
            managerActivity.askForAccess();
        }
        managerActivity.destroySMSVerificationFragment();
    }
}
