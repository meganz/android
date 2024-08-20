package mega.privacy.android.app.globalmanagement

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.MainDispatcher
import mega.privacy.android.domain.usecase.ResetSdkLoggerUseCase
import mega.privacy.android.domain.usecase.login.BroadcastFinishActivityUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutAppUseCase
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
 * @property chatManagement
 * @property myAccountInfo
 * @property passcodeManagement
 * @property transfersManagement
 */
@Singleton
class MegaChatRequestHandler @Inject constructor(
    private val application: Application,
    private val activityLifecycleHandler: ActivityLifecycleHandler,
    @MegaApi
    private val megaApi: MegaApiAndroid,
    @ApplicationScope
    private val sharingScope: CoroutineScope,
    @MainDispatcher
    private val mainDispatcher: CoroutineDispatcher,
    private val chatManagement: ChatManagement,
    private val myAccountInfo: MyAccountInfo,
    private val passcodeManagement: PasscodeManagement,
    private val transfersManagement: TransfersManagement,
    private val broadcastFinishActivityUseCase: BroadcastFinishActivityUseCase,
    private val localLogoutAppUseCase: LocalLogoutAppUseCase,
    private val resetSdkLoggerUseCase: ResetSdkLoggerUseCase,
) : MegaChatRequestListenerInterface {
    private var isLoggingRunning = false

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
        if (request.type == MegaChatRequest.TYPE_SET_BACKGROUND_STATUS) {
            Timber.d("SET_BACKGROUND_STATUS: %s", request.flag)
        } else if (request.type == MegaChatRequest.TYPE_LOGOUT) {
            Timber.d("CHAT_TYPE_LOGOUT: %d__%s", e.errorCode, e.errorString)
            resetDefaults()
            MegaApplication.getInstance().disableMegaChatApi()
            resetSdkLoggerUseCase()
            val loggedState: Int = megaApi.isLoggedIn
            Timber.d("Login status on %s", loggedState)
            if (loggedState == 0) {
                sharingScope.launch {
                    runCatching { localLogoutAppUseCase() }
                        .onFailure { Timber.d(it) }
                    //Need to finish ManagerActivity to avoid unexpected behaviours after forced logouts.
                    broadcastFinishActivityUseCase()
                    withContext(mainDispatcher) {
                        if (isLoggingRunning) {
                            Timber.d("Already in Login Activity, not necessary to launch it again")
                            return@withContext
                        }
                        val loginIntent = Intent(application, LoginActivity::class.java).apply {
                            putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                            if (MegaApplication.urlConfirmationLink != null) {
                                putExtra(
                                    Constants.EXTRA_CONFIRMATION,
                                    MegaApplication.urlConfirmationLink
                                )
                                if (activityLifecycleHandler.isActivityVisible) {
                                    flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                } else {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                }
                                action = Constants.ACTION_CONFIRM
                                MegaApplication.urlConfirmationLink = null
                            } else {
                                flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        }
                        application.startActivity(loginIntent)
                    }
                }
            } else {
                Timber.d("Disable chat finish logout")
            }
        } else if (request.type == MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT) {
            chatManagement.removeJoiningChatId(request.chatHandle)
            chatManagement.removeJoiningChatId(request.userHandle)
            chatManagement.broadcastJoinedSuccessfully()
        } else if (request.type == MegaChatRequest.TYPE_DISCONNECT) {
            if (e.errorCode == MegaChatError.ERROR_OK) {
                Timber.d("DISConnected from chat!")
            } else {
                Timber.e("ERROR WHEN DISCONNECTING %s", e.errorString)
            }
        } else if (request.type == MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE) {
            if (e.errorCode == MegaChatError.ERROR_OK) {
                Timber.d("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE: %s", request.flag)
            } else {
                Timber.e("MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE:error: %d", e.errorType)
            }
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
     * Set is logging running
     *
     * @param isLoggingRunning
     */
    fun setIsLoggingRunning(isLoggingRunning: Boolean) {
        this.isLoggingRunning = isLoggingRunning
    }

    /**
     * Resets all SingleObjects to their default values.
     */
    private fun resetDefaults() {
        passcodeManagement.resetDefaults()
        myAccountInfo.resetDefaults()
        transfersManagement.resetDefaults()
    }
}