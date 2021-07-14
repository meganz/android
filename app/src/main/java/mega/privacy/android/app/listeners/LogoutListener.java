package mega.privacy.android.app.listeners;

import android.content.Context;
import android.content.Intent;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.WeakAccountProtectionAlertActivity;
import mega.privacy.android.app.activities.settingsActivities.PasscodeLockActivity;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity;
import mega.privacy.android.app.meeting.activity.MeetingActivity;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.Util.*;

public class LogoutListener extends BaseListener {

    MegaApplication app;

    public LogoutListener(Context context) {
        super(context);
        app = MegaApplication.getInstance();
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != MegaRequest.TYPE_LOGOUT) return;

        if (e.getErrorCode() == MegaError.API_OK) {
            AccountController.logoutConfirmed(context);
            if (context instanceof WeakAccountProtectionAlertActivity) {
                ((WeakAccountProtectionAlertActivity) context).finish();
            } else if (context instanceof PasscodeLockActivity) {
                ((PasscodeLockActivity) context).finish();
            } else if ((context instanceof MeetingActivity)) {
                ((MeetingActivity) context).finish();
                Intent leftMeetingIntent = new Intent(context, LeftMeetingActivity.class);
                leftMeetingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(leftMeetingIntent);
                return;
            }

            Intent tourIntent = new Intent(context, LoginActivityLollipop.class);
            tourIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(tourIntent);
        } else {
            showSnackbar(context, SNACKBAR_TYPE, context.getString(R.string.general_error), -1);
        }
    }
}
