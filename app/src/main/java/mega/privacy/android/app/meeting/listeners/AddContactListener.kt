package mega.privacy.android.app.meeting.listeners

import mega.privacy.android.app.R
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.StringResourcesUtils
import nz.mega.sdk.*

/**
 * Listener for adding contacts
 *
 * @property callback the callback when receive the response of adding contacts
 */
class AddContactListener(private val callback: (String) -> Unit) : MegaRequestListenerInterface {
    override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {}

    override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {}

    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_INVITE_CONTACT) {
            LogUtil.logDebug("MegaRequest.TYPE_INVITE_CONTACT finished: " + request.number)
            when{
                e.errorCode == MegaError.API_OK -> {
                    if (request.number == MegaContactRequest.INVITE_ACTION_ADD.toLong()) {
                        callback.invoke(
                            StringResourcesUtils.getString(
                                R.string.context_contact_request_sent,
                                request.email
                            )
                        )
                    }
                    return
                }
                e.errorCode == MegaError.API_EEXIST ->{
                    callback.invoke(
                        StringResourcesUtils.getString(R.string.context_contact_already_invited, request.email)
                    )
                }
                request.number == MegaContactRequest.INVITE_ACTION_ADD.toLong() && e.errorCode == MegaError.API_EARGS ->{
                    callback.invoke(
                        StringResourcesUtils.getString(R.string.error_own_email_as_contact)
                    )
                }
                else ->{
                    callback.invoke(
                        StringResourcesUtils.getString(R.string.general_error).toString()
                    )
                }
            }

            logError("ERROR: " + e.errorCode + "___" + e.errorString)
        }
    }

    override fun onRequestTemporaryError(api: MegaApiJava?, request: MegaRequest?, e: MegaError?) {}
}