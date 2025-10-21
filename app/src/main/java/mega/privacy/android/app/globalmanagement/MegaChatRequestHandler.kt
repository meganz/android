package mega.privacy.android.app.globalmanagement

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mega chat request handler
 *
 * @property application
 * @property activityLifecycleHandler
 * @property megaApi
 * @property sharingScope
 * @property autoJoinPublicChatHandler
 * @property chatLogoutHandler
 * @property myAccountInfo
 */
@Singleton
class MegaChatRequestHandler @Inject constructor(
    private val application: Application,
    private val activityLifecycleHandler: ActivityLifecycleHandler,
    @MegaApi
    private val megaApi: MegaApiAndroid,
    @ApplicationScope
    private val sharingScope: CoroutineScope,
    private val autoJoinPublicChatHandler: AutoJoinPublicChatHandler,
    private val chatLogoutHandler: ChatLogoutHandler,
    private val myAccountInfo: MyAccountInfo,
) : MegaChatRequestListenerInterface {
    private var isLoginRunning = false

    /**
     * On request start
     *
     * @param api
     * @param request
     */
    override fun onRequestStart(api: MegaChatApiJava?, request: MegaChatRequest) {
        Timber.d("onRequestStart (CHAT): %s", request.requestString)
    }

    /**
     * On request update
     *
     * @param api
     * @param request
     */
    override fun onRequestUpdate(api: MegaChatApiJava?, request: MegaChatRequest?) {}

    /**
     * On request finish
     *
     * @param api
     * @param request
     * @param e
     */
    override fun onRequestFinish(
        api: MegaChatApiJava?,
        request: MegaChatRequest,
        e: MegaChatError,
    ) {
        Timber.d("onRequestFinish (CHAT): %s_%d", request.requestString, e.errorCode)
        when (request.type) {
            MegaChatRequest.TYPE_SET_BACKGROUND_STATUS -> logSetBackgroundStatus(request)

            MegaChatRequest.TYPE_LOGOUT -> onChatLogout(e)

            MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT -> onAutoJoinPublicChat(request)

            MegaChatRequest.TYPE_DISCONNECT -> logDisconnectResponse(e)

            MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE -> logLastGreenResponse(e, request)
        }
    }

    private fun logSetBackgroundStatus(request: MegaChatRequest) {
        Timber.d("SET_BACKGROUND_STATUS: %s", request.flag)
    }

    private fun onChatLogout(e: MegaChatError) {
        Timber.d("CHAT_TYPE_LOGOUT: %d__%s", e.errorCode, e.errorString)
        resetDefaults()
        MegaApplication.getInstance().disableMegaChatApi()
        val loggedState: Int = megaApi.isLoggedIn
        Timber.d("Login status on %s", loggedState)
        if (loggedState == 0) {
            chatLogoutHandler.handleChatLogout(isLoginRunning)
        } else {
            Timber.d("Disable chat finish logout")
        }
    }

    private fun onAutoJoinPublicChat(request: MegaChatRequest) {
        autoJoinPublicChatHandler.handleResponse(request.chatHandle, request.userHandle)
    }

    private fun logDisconnectResponse(e: MegaChatError) {
        if (e.errorCode == MegaChatError.ERROR_OK) {
            Timber.d("DISConnected from chat!")
        } else {
            Timber.e("ERROR WHEN DISCONNECTING %s", e.errorString)
        }
    }

    private fun logLastGreenResponse(e: MegaChatError, request: MegaChatRequest) {
        if (e.errorCode == MegaChatError.ERROR_OK) {
            Timber.d("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE: %s", request.flag)
        } else {
            Timber.e("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE:error: %d", e.errorType)
        }
    }

    /**
     * On request temporary error
     *
     * @param api
     * @param request
     * @param e
     */
    override fun onRequestTemporaryError(
        api: MegaChatApiJava?,
        request: MegaChatRequest?,
        e: MegaChatError,
    ) {
        Timber.w("onRequestTemporaryError (CHAT): %s", e.errorString)
    }

    /**
     * Set is login running
     *
     * @param isLoginRunning
     */
    fun setIsLoginRunning(isLoginRunning: Boolean) {
        this.isLoginRunning = isLoginRunning
    }

    /**
     * Resets all SingleObjects to their default values.
     */
    private fun resetDefaults() {
        myAccountInfo.resetDefaults()
    }
}