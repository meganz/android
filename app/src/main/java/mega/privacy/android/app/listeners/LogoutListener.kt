package mega.privacy.android.app.listeners

import android.app.Activity
import android.content.Context
import android.content.Intent
import mega.privacy.android.app.R
import mega.privacy.android.app.lollipop.LoginActivity
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.meeting.activity.LeftMeetingActivity
import mega.privacy.android.app.meeting.activity.MeetingActivity
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util.showSnackbar
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class LogoutListener(context: Context) : BaseListener(context) {

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type != MegaRequest.TYPE_LOGOUT) return

        if (e.errorCode != MegaError.API_OK) {
            showSnackbar(
                context,
                SNACKBAR_TYPE,
                getString(R.string.general_error),
                MEGACHAT_INVALID_HANDLE
            )
            return
        }

        AccountController.logoutConfirmed(context)

        context.startActivity(
            Intent(
                context,
                if (context is MeetingActivity) LeftMeetingActivity::class.java
                else LoginActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )

        (context as Activity).finish()
    }
}