package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.OpenPasswordLinkActivity
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest

class PasswordLinkListener(context: Context) : BaseListener(context) {
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type != MegaRequest.TYPE_PASSWORD_LINK) {
            return
        }

        if (context is OpenPasswordLinkActivity) {
            (context as OpenPasswordLinkActivity).managePasswordLinkRequest(e, request.text)
        }
    }
}
