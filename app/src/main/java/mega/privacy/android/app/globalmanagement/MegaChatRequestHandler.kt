package mega.privacy.android.app.globalmanagement

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.leolin.shortcutbadger.ShortcutBadger
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder
import mega.privacy.android.app.main.megachat.BadgeIntentService
import mega.privacy.android.app.middlelayer.BuildFlavorHelper.isHMS
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.psa.PsaManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.chat.ChatRequestMapper
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.ClearPsa
import mega.privacy.android.domain.usecase.login.BroadcastFinishActivityUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutAppUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
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
    private val chatManagement: ChatManagement,
    private val myAccountInfo: MyAccountInfo,
    private val passcodeManagement: PasscodeManagement,
    private val transfersManagement: TransfersManagement,
    private val chatRequestMapper: ChatRequestMapper,
    private val broadcastFinishActivityUseCase: BroadcastFinishActivityUseCase,
    private val localLogoutAppUseCase: LocalLogoutAppUseCase,
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

        if (request.type == MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM) {
            chatManagement.addLeavingChatId(request.chatHandle)
        }
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
            try {
                ShortcutBadger.applyCount(application, 0)
                application.startService(
                    Intent(
                        application,
                        BadgeIntentService::class.java
                    ).putExtra("badgeCount", 0)
                )
            } catch (exc: Exception) {
                Timber.e(exc, "EXCEPTION removing badge indicator")
            }
            val loggedState: Int = megaApi.isLoggedIn
            Timber.d("Login status on %s", loggedState)
            if (loggedState == 0) {
                sharingScope.launch {
                    localLogoutAppUseCase(ClearPsa { PsaManager::clearPsa })
                    //Need to finish ManagerActivity to avoid unexpected behaviours after forced logouts.
                    broadcastFinishActivityUseCase()
                }
                if (isLoggingRunning) {
                    Timber.d("Already in Login Activity, not necessary to launch it again")
                    return
                }
                val loginIntent = Intent(application, LoginActivity::class.java).apply {
                    if (MegaApplication.urlConfirmationLink != null) {
                        putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                            .putExtra(
                                Constants.EXTRA_CONFIRMATION,
                                MegaApplication.urlConfirmationLink
                            )
                        if (activityLifecycleHandler.isActivityVisible) {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        } else {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        action = Constants.ACTION_CONFIRM
                        MegaApplication.urlConfirmationLink = null
                    } else {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                }
                application.startActivity(loginIntent)
            } else {
                Timber.d("Disable chat finish logout")
            }
        } else if (request.type == MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT) {
            chatManagement.removeJoiningChatId(request.chatHandle)
            chatManagement.removeJoiningChatId(request.userHandle)
        } else if (request.type == MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM
            && request.userHandle == MegaApiJava.INVALID_HANDLE
        ) {
            chatManagement.removeLeavingChatId(request.chatHandle)
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