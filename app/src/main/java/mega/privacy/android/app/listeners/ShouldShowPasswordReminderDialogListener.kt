package mega.privacy.android.app.listeners

import android.content.Context
import android.content.Intent
import mega.privacy.android.app.lollipop.TestPasswordActivity
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.StringResourcesUtils.getTranslatedErrorString
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.USER_ATTR_PWD_REMINDER
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_ENOENT
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequest.TYPE_GET_ATTR_USER

class ShouldShowPasswordReminderDialogListener(
    context: Context,
    private val atLogout: Boolean
) : BaseListener(context) {
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type != TYPE_GET_ATTR_USER || request.paramType != USER_ATTR_PWD_REMINDER) {
            return
        }

        if (e.errorCode != API_OK || e.errorCode != API_ENOENT) {
            logError(getTranslatedErrorString(e))
        }

        if (request.flag) {
            context.startActivity(
                Intent(context, TestPasswordActivity::class.java)
                    .putExtra("logout", atLogout)
            )
        } else if (atLogout) {
            AccountController.logout(context, api as MegaApiAndroid)
        }
    }
}