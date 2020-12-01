package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.OpenPasswordLinkActivity
import mega.privacy.android.app.activities.GetLinkActivity
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.StringResourcesUtils.getTranslatedErrorString
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class PasswordLinkListener(context: Context) : BaseListener(context) {
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type != MegaRequest.TYPE_PASSWORD_LINK) {
            return
        }

        if (e.errorCode == MegaError.API_OK) {
            if (isTextEmpty(request.text)) {
                logError("Encrypted link is empty")
                return
            }

            if (context is GetLinkActivity) {
                (context as GetLinkActivity).setLinkWithPassword(request.text)
            } else if (context is OpenPasswordLinkActivity) {
                (context as OpenPasswordLinkActivity).managePasswordLinkRequest(e, request.text)
            }
        } else {
            logError("Error encrypting link: " + getTranslatedErrorString(e))
        }
    }
}