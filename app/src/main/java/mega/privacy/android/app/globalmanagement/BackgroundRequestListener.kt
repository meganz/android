package mega.privacy.android.app.globalmanagement

import android.app.Application
import android.content.Intent
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.listeners.GetAttrUserListener
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.IsUseHttpsEnabledUseCase
import mega.privacy.android.domain.usecase.SetUseHttpsUseCase
import mega.privacy.android.domain.usecase.account.GetFullAccountInfoUseCase
import mega.privacy.android.domain.usecase.account.ResetAccountDetailsTimeStampUseCase
import mega.privacy.android.domain.usecase.backup.SetupDeviceNameUseCase
import mega.privacy.android.domain.usecase.business.BroadcastBusinessAccountExpiredUseCase
import mega.privacy.android.domain.usecase.chat.UpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.chat.link.IsRichPreviewsEnabledUseCase
import mega.privacy.android.domain.usecase.chat.link.ShouldShowRichLinkWarningUseCase
import mega.privacy.android.domain.usecase.login.BroadcastFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutAppUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject

/**
 * Background request listener
 *
 * @property application
 * @property myAccountInfo
 * @property megaChatApi
 * @property dbH
 * @property megaApi
 * @property applicationScope
 * @property getFullAccountInfoUseCase
 * @property broadcastFetchNodesFinishUseCase
 * @property localLogoutAppUseCase
 * @property setupDeviceNameUseCase
 * @property broadcastBusinessAccountExpiredUseCase
 * @property loginMutex
 * @property updatePushNotificationSettingsUseCase
 * @property shouldShowRichLinkWarningUseCase
 * @property isRichPreviewsEnabledUseCase
 * @property isUseHttpsEnabledUseCase
 * @property setUseHttpsUseCase
 * @property resetAccountDetailsTimeStampUseCase
 */
class BackgroundRequestListener @Inject constructor(
    private val application: Application,
    private val myAccountInfo: MyAccountInfo,
    private val megaChatApi: MegaChatApiAndroid,
    private val dbH: Lazy<DatabaseHandler>,
    @MegaApi private val megaApi: MegaApiAndroid,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val getFullAccountInfoUseCase: GetFullAccountInfoUseCase,
    private val broadcastFetchNodesFinishUseCase: BroadcastFetchNodesFinishUseCase,
    private val localLogoutAppUseCase: LocalLogoutAppUseCase,
    private val setupDeviceNameUseCase: SetupDeviceNameUseCase,
    private val broadcastBusinessAccountExpiredUseCase: BroadcastBusinessAccountExpiredUseCase,
    @LoginMutex private val loginMutex: Mutex,
    private val updatePushNotificationSettingsUseCase: UpdatePushNotificationSettingsUseCase,
    private val shouldShowRichLinkWarningUseCase: ShouldShowRichLinkWarningUseCase,
    private val isRichPreviewsEnabledUseCase: IsRichPreviewsEnabledUseCase,
    private val isUseHttpsEnabledUseCase: IsUseHttpsEnabledUseCase,
    private val setUseHttpsUseCase: SetUseHttpsUseCase,
    private val resetAccountDetailsTimeStampUseCase: ResetAccountDetailsTimeStampUseCase,
) : MegaRequestListenerInterface {
    /**
     * On request start
     */
    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("BackgroundRequestListener:onRequestStart: ${request.requestString}")
    }

    /**
     * On request update
     */
    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        Timber.d("BackgroundRequestListener:onRequestUpdate: ${request.requestString}")
    }

    /**
     * On request finish
     */
    override fun onRequestFinish(
        api: MegaApiJava,
        request: MegaRequest,
        e: MegaError,
    ) {
        Timber.d("BackgroundRequestListener:onRequestFinish: ${request.requestString}____${e.errorCode}___${request.paramType}")
        if (e.errorCode == MegaError.API_EPAYWALL) {
            showOverDiskQuotaPaywallWarning()
            return
        }
        if (e.errorCode == MegaError.API_EBUSINESSPASTDUE) {
            applicationScope.launch {
                broadcastBusinessAccountExpiredUseCase()
            }
            return
        }
        when (request.type) {
            MegaRequest.TYPE_LOGOUT -> handleLogoutRequest(e, request, api)
            MegaRequest.TYPE_FETCH_NODES -> handleFetchNodeRequest(e)
            MegaRequest.TYPE_GET_ATTR_USER -> handleGetAttrUserRequest(request, e)
        }
    }

    private fun handleGetAttrUserRequest(request: MegaRequest, e: MegaError) {
        if (e.errorCode == MegaError.API_OK) {
            if (request.paramType == MegaApiJava.USER_ATTR_FIRSTNAME || request.paramType == MegaApiJava.USER_ATTR_LASTNAME) {
                request.email?.let { email ->
                    megaApi.getContact(email)?.let { user ->
                        Timber.d("User handle: ${user.handle}")
                        Timber.d("Visibility: ${user.visibility}") //If user visibility == MegaUser.VISIBILITY_UNKNOWN then, non contact
                        if (user.visibility != MegaUser.VISIBILITY_VISIBLE) {
                            Timber.d("Non-contact")
                            when (request.paramType) {
                                MegaApiJava.USER_ATTR_FIRSTNAME -> {
                                    dbH.get().setNonContactEmail(
                                        request.email,
                                        user.handle.toString() + ""
                                    )
                                    dbH.get().setNonContactFirstName(
                                        request.text,
                                        user.handle.toString() + ""
                                    )
                                }

                                MegaApiJava.USER_ATTR_LASTNAME -> {
                                    dbH.get().setNonContactLastName(
                                        request.text,
                                        user.handle.toString() + ""
                                    )
                                }

                                else -> {}
                            }
                        } else {
                            Timber.d("The user is or was CONTACT:")
                        }
                    } ?: run {
                        Timber.w("User is NULL")
                    }
                }
            }
        }
    }

    private fun handleFetchNodeRequest(e: MegaError) {
        Timber.d("TYPE_FETCH_NODES")
        applicationScope.launch {
            runCatching { loginMutex.unlock() }
                .onFailure { Timber.w("Exception unlocking login mutex", it) }

            broadcastFetchNodesFinishUseCase()
            runCatching {
                setUseHttpsUseCase(isUseHttpsEnabledUseCase())
                resetAccountDetailsTimeStampUseCase()
            }.onFailure {
                Timber.e(it)
            }
        }

        if (e.errorCode == MegaError.API_OK) {
            askForFullAccountInfo()
            applicationScope.launch {
                runCatching {
                    shouldShowRichLinkWarningUseCase()
                    isRichPreviewsEnabledUseCase()
                }.onFailure {
                    Timber.e(it, "Error checking rich link settings")
                }
            }
            val listener = GetAttrUserListener(application, true)
            if (dbH.get().myChatFilesFolderHandle == INVALID_HANDLE) {
                megaApi.getMyChatFilesFolder(listener)
            }
            MegaApplication.getInstance().setupMegaChatApi()
            applicationScope.launch {
                // Init CU sync data after login successfully
                runCatching {
                    setupDeviceNameUseCase()
                }.onFailure {
                    Timber.e(it)
                }

                runCatching {
                    updatePushNotificationSettingsUseCase()
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }

    private fun handleLogoutRequest(
        e: MegaError,
        request: MegaRequest,
        api: MegaApiJava,
    ) {
        Timber.d("Logout finished: %s(%d)", e.errorString, e.errorCode)
        if (e.errorCode == MegaError.API_OK) {
            Timber.d("END logout sdk request - wait chat logout")
            MegaApplication.isLoggingOut = false
        } else if (e.errorCode == MegaError.API_EINCOMPLETE) {
            if (request.paramType == MegaError.API_ESSL) {
                Timber.w("SSL verification failed")
                application.sendBroadcast(
                    Intent(BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED).setPackage(application.applicationContext.packageName)
                )
            }
        } else if (e.errorCode == MegaError.API_ESID) {
            Timber.w("TYPE_LOGOUT:API_ESID")
            myAccountInfo.resetDefaults()
            (application as MegaApplication).isEsid = true
            applicationScope.launch {
                runCatching { localLogoutAppUseCase() }
                    .onFailure { Timber.d(it) }
            }
        } else if (e.errorCode == MegaError.API_EBLOCKED) {
            api.localLogout()
            megaChatApi.logout()
        }
    }

    /**
     * On request temporary error
     */
    override fun onRequestTemporaryError(
        api: MegaApiJava,
        request: MegaRequest, e: MegaError,
    ) {
        Timber.d("BackgroundRequestListener: onRequestTemporaryError: ${request.requestString}")
    }

    private fun askForFullAccountInfo() {
        Timber.d("askForFullAccountInfo")
        applicationScope.launch {
            runCatching { getFullAccountInfoUseCase() }.onFailure {
                Timber.w("Exception getting full account info.", it)
            }
        }
    }
}
