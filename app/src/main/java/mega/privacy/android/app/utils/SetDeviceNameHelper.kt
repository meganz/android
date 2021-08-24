package mega.privacy.android.app.utils

import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.USER_ATTR_DEVICE_NAMES
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_ENOENT
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequest.*

/**
 * Helper class for setting device name.
 */
object SetDeviceNameHelper {

    /**
     * Set default device name for the account, only set it when no device name hasn't been set before.
     * Otherwise will overwrite the device name set by the user.
     *
     * Will try to set when login successfully for backward compatibility.
     *
     * @param api MegaApi object to start the request.
     */
    fun setDefaultDeviceName(api: MegaApiJava) {
        api.getDeviceName(object : BaseListener(null) {

            override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
                if(request.type != TYPE_GET_ATTR_USER || request.paramType != USER_ATTR_DEVICE_NAMES) return
                logDebug("${request.requestString} finished with ${e.errorCode}: ${e.errorString}")

                // Haven't set device name yet, should set a default name. Otherwise do nothing.
                if(request.name == null || e.errorCode == API_ENOENT) {
                    api.setDeviceName(Util.getDeviceName(), object : BaseListener(null) {

                        override fun onRequestFinish(
                            api: MegaApiJava,
                            request: MegaRequest,
                            e: MegaError
                        ) {
                            logDebug("${request.requestString} finished with ${e.errorCode}: ${e.errorString}")
                        }
                    })
                } else {
                    logDebug("Already set device name.")
                }
            }
        })
    }
}