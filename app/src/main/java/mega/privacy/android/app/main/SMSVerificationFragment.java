package mega.privacy.android.app.main;

import static mega.privacy.android.app.utils.Util.getSizeString;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.presentation.verification.SMSVerificationActivity;
import nz.mega.sdk.MegaAchievementsDetails;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import timber.log.Timber;


public class SMSVerificationFragment extends Fragment implements View.OnClickListener, MegaRequestListenerInterface {

    private Context context;

    private ManagerActivity managerActivity;

    private MegaApiJava megaApi;

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
        Timber.d("is achievement user: %s", isAchievementUser);
        if (isAchievementUser) {
            megaApi.getAccountAchievements(this);
            String message = String.format(getString(R.string.sms_add_phone_number_dialog_msg_achievement_user),
                    managerActivity.myAccountInfo.getBonusStorageSMS());
            msg.setText(message);
        } else {
            msg.setText(R.string.sms_add_phone_number_dialog_msg_non_achievement_user);
        }
        return v;
    }

    @Override
    public void onDestroyView() {
        megaApi.removeRequestListener(this);
        super.onDestroyView();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof ManagerActivity) {
            this.managerActivity = (ManagerActivity) context;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.enable_button) {
            Timber.d("To sms verification");
            startActivity(new Intent(context, SMSVerificationActivity.class));
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    fadeOut();
                }
            }, 1000);
        } else if (id == R.id.not_now_button_2) {
            Timber.d("Don't verify now");
            fadeOut();
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
        if (request.getType() == MegaRequest.TYPE_GET_ACHIEVEMENTS) {
            if (e.getErrorCode() == MegaError.API_OK) {
                long bonusStorage = request.getMegaAchievementsDetails().getClassStorage(MegaAchievementsDetails.MEGA_ACHIEVEMENT_ADD_PHONE);
                managerActivity.myAccountInfo
                        .setBonusStorageSMS(getSizeString(bonusStorage, context));
            }
            if (isAdded()) {
                String message = String.format(getString(R.string.sms_add_phone_number_dialog_msg_achievement_user),
                        managerActivity.myAccountInfo.getBonusStorageSMS());
                msg.setText(message);
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    private void fadeOut() {
        managerActivity.destroySMSVerificationFragment();
        if (managerActivity.firstTimeAfterInstallation || managerActivity.getAskPermissions()) {
            managerActivity.askForAccess();
        }
    }
}
