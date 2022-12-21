package mega.privacy.android.app.globalmanagement

import android.app.Application
import android.content.Intent
import com.jeremyliao.liveeventbus.LiveEventBus
import kotlinx.coroutines.CoroutineScope
import me.leolin.shortcutbadger.ShortcutBadger
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.constants.EventConstants.EVENT_FINISH_ACTIVITY
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder
import mega.privacy.android.app.main.LoginActivity
import mega.privacy.android.app.main.controllers.AccountController.Companion.logoutConfirmed
import mega.privacy.android.app.main.megachat.BadgeIntentService
import mega.privacy.android.app.middlelayer.BuildFlavorHelper.isHMS
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.ChatRequestMapper
import mega.privacy.android.domain.qualifier.ApplicationScope
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
 * @property sortOrderManagement
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
            try {
                ShortcutBadger.applyCount(application, 0)
                application.startService(Intent(application,
                    BadgeIntentService::class.java).putExtra("badgeCount", 0))
            } catch (exc: Exception) {
                Timber.e(exc, "EXCEPTION removing badge indicator")
            }
            val loggedState: Int = megaApi.isLoggedIn
            Timber.d("Login status on %s", loggedState)
            if (loggedState == 0) {
                logoutConfirmed(application, sharingScope)
                //Need to finish ManagerActivity to avoid unexpected behaviours after forced logouts.
                LiveEventBus.get(EVENT_FINISH_ACTIVITY, Boolean::class.java).post(true)
                if (isLoggingRunning) {
                    Timber.d("Already in Login Activity, not necessary to launch it again")
                    return
                }
                val loginIntent = Intent(application,
                    LoginActivity::class.java).apply {
                    if (MegaApplication.urlConfirmationLink != null) {
                        putExtra(Constants.VISIBLE_FRAGMENT, Constants.LOGIN_FRAGMENT)
                            .putExtra(Constants.EXTRA_CONFIRMATION,
                                MegaApplication.urlConfirmationLink)
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
        } else if (request.type == MegaChatRequest.TYPE_PUSH_RECEIVED) {
            Timber.d("TYPE_PUSH_RECEIVED: %d__%s", e.errorCode, e.errorString)

            //Temporary HMS code to show pushes until AND-13803 is resolved.
            if (isHMS()) {
                if (e.errorCode == MegaChatError.ERROR_OK) {
                    Timber.d("OK:TYPE_PUSH_RECEIVED")
                    if (!megaApi.isEphemeralPlusPlus) {
                        ChatAdvancedNotificationBuilder.newInstance(application)
                            .generateChatNotification(chatRequestMapper(request))
                    }
                } else {
                    Timber.w("Error TYPE_PUSH_RECEIVED: %s", e.errorString)
                }
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