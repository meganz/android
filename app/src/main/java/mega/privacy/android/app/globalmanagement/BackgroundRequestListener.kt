package mega.privacy.android.app.globalmanagement

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.PushNotificationSettingManagement
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_TYPE
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.app.listeners.GetAttrUserListener
import mega.privacy.android.app.listeners.GetCameraUploadAttributeListener
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning
import mega.privacy.android.app.utils.CUBackupInitializeChecker
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS
import mega.privacy.android.app.utils.Constants.UPDATE_CREDIT_CARD_SUBSCRIPTION
import mega.privacy.android.app.utils.Constants.UPDATE_GET_PRICING
import mega.privacy.android.app.utils.Constants.UPDATE_PAYMENT_METHODS
import mega.privacy.android.app.utils.JobUtil.scheduleCameraUploadJob
import mega.privacy.android.app.utils.TimeUtils.DATE_LONG_FORMAT
import mega.privacy.android.app.utils.TimeUtils.formatDateAndTime
import mega.privacy.android.app.utils.Util.convertToBitSet
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import nz.mega.sdk.MegaAccountSession
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaPricing
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
 * @property sharingScope
 * @property dbH
 * @property megaApi
 * @property transfersManagement
 * @property pushNotificationSettingManagement
 */
class BackgroundRequestListener @Inject constructor(
    private val application: Application,
    private val myAccountInfo: MyAccountInfo,
    private val megaChatApi: MegaChatApiAndroid,
    @ApplicationScope private val sharingScope: CoroutineScope,
    private val dbH: DatabaseHandler,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val transfersManagement: TransfersManagement,
    private val pushNotificationSettingManagement: PushNotificationSettingManagement,
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
) : MegaRequestListenerInterface {
    /**
     * On request start
     */
    override fun onRequestStart(api: MegaApiJava, request: MegaRequest) {
        Timber.d("BackgroundRequestListener:onRequestStart: %s", request.requestString)
    }

    /**
     * On request update
     */
    override fun onRequestUpdate(api: MegaApiJava, request: MegaRequest) {
        Timber.d("BackgroundRequestListener:onRequestUpdate: %s", request.requestString)
    }

    /**
     * On request finish
     */
    override fun onRequestFinish(
        api: MegaApiJava,
        request: MegaRequest,
        e: MegaError,
    ) {
        Timber.d(
            "BackgroundRequestListener:onRequestFinish: %s____%d___%d",
            request.requestString,
            e.errorCode,
            request.paramType
        )
        if (e.errorCode == MegaError.API_EPAYWALL) {
            showOverDiskQuotaPaywallWarning()
            return
        }
        if (e.errorCode == MegaError.API_EBUSINESSPASTDUE) {
            application.sendBroadcast(Intent(BROADCAST_ACTION_INTENT_BUSINESS_EXPIRED))
            return
        }
        when (request.type) {
            MegaRequest.TYPE_LOGOUT -> handleLogoutRequest(e, request, api)
            MegaRequest.TYPE_FETCH_NODES -> handleFetchNodeRequest(e)
            MegaRequest.TYPE_GET_ATTR_USER -> handleGetAttrUserRequest(request, e)
            MegaRequest.TYPE_SET_ATTR_USER -> handleSetAttrUserRequest(request, e)
            MegaRequest.TYPE_GET_PRICING -> handleGetPricingRequest(e, request)
            MegaRequest.TYPE_GET_PAYMENT_METHODS -> handleGetPaymentMethodsRequest(e, request)
            MegaRequest.TYPE_CREDIT_CARD_QUERY_SUBSCRIPTIONS -> handleCreditCardQuerySubscriptionsRequest(
                e,
                request
            )
            MegaRequest.TYPE_ACCOUNT_DETAILS -> handleAccountDetailRequest(e, request)
            MegaRequest.TYPE_PAUSE_TRANSFERS -> dbH.transferQueueStatus = request.flag
        }
    }

    private fun handleAccountDetailRequest(e: MegaError, request: MegaRequest) {
        Timber.d("Account details request")
        if (e.errorCode == MegaError.API_OK) {
            val storage = request.numDetails and MyAccountInfo.HAS_STORAGE_DETAILS != 0
            if (storage && megaApi.rootNode != null) {
                dbH.setAccountDetailsTimeStamp()
            }
            if (request.megaAccountDetails != null) {
                myAccountInfo.setAccountInfo(request.megaAccountDetails)
                myAccountInfo.setAccountDetails(request.numDetails)
                val sessions =
                    request.numDetails and MyAccountInfo.HAS_SESSIONS_DETAILS != 0
                if (sessions) {
                    val megaAccountSession: MegaAccountSession? =
                        request.megaAccountDetails.getSession(0)
                    if (megaAccountSession != null) {
                        Timber.d("getMegaAccountSESSION not Null")
                        dbH.setExtendedAccountDetailsTimestamp()
                        val mostRecentSession: Long = megaAccountSession.mostRecentUsage
                        val date: String = formatDateAndTime(application,
                            mostRecentSession,
                            DATE_LONG_FORMAT)
                        myAccountInfo.lastSessionFormattedDate = date
                        myAccountInfo.createSessionTimeStamp =
                            megaAccountSession.creationTimestamp
                    }
                }
                Timber.d("onRequest TYPE_ACCOUNT_DETAILS: %s", myAccountInfo.usedPercentage)
            }
            sendBroadcastUpdateAccountDetails()
        }
    }

    private fun handleCreditCardQuerySubscriptionsRequest(
        e: MegaError,
        request: MegaRequest,
    ) {
        if (e.errorCode == MegaError.API_OK) {
            myAccountInfo.numberOfSubscriptions = request.number
            Timber.d("NUMBER OF SUBS: %s", myAccountInfo.numberOfSubscriptions)
            val intent = Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
            intent.putExtra(ACTION_TYPE, UPDATE_CREDIT_CARD_SUBSCRIPTION)
            application.sendBroadcast(intent)
        }
    }

    private fun handleGetPaymentMethodsRequest(
        e: MegaError,
        request: MegaRequest,
    ) {
        Timber.d("Payment methods request")
        myAccountInfo.getPaymentMethodsBoolean = true
        if (e.errorCode == MegaError.API_OK) {
            dbH.setPaymentMethodsTimeStamp()
            myAccountInfo.paymentBitSet = convertToBitSet(request.number)
            val intent = Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
            intent.putExtra(ACTION_TYPE, UPDATE_PAYMENT_METHODS)
            application.sendBroadcast(intent)
        }
    }

    private fun handleGetPricingRequest(e: MegaError, request: MegaRequest) {
        if (e.errorCode == MegaError.API_OK) {
            val p: MegaPricing = request.pricing
            dbH.setPricingTimestamp()
            myAccountInfo.setProductAccounts(p, request.currency)
            myAccountInfo.pricing = p
            val intent = Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
            intent.putExtra(ACTION_TYPE, UPDATE_GET_PRICING)
            application.sendBroadcast(intent)
        } else {
            Timber.e("Error TYPE_GET_PRICING: %s", e.errorCode)
        }
    }

    private fun handleSetAttrUserRequest(request: MegaRequest, e: MegaError) {
        if (request.paramType == MegaApiJava.USER_ATTR_PUSH_SETTINGS) {
            if (e.errorCode == MegaError.API_OK) {
                pushNotificationSettingManagement.sendPushNotificationSettings(request.megaPushNotificationSettings)
            } else {
                Timber.e("Chat notification settings cannot be updated")
            }
        }
    }

    private fun handleGetAttrUserRequest(request: MegaRequest, e: MegaError) {
        if (request.paramType == MegaApiJava.USER_ATTR_PUSH_SETTINGS) {
            if (e.errorCode == MegaError.API_OK || e.errorCode == MegaError.API_ENOENT) {
                pushNotificationSettingManagement.sendPushNotificationSettings(request.megaPushNotificationSettings)
            }
        } else if (e.errorCode == MegaError.API_OK) {
            if (request.paramType == MegaApiJava.USER_ATTR_FIRSTNAME || request.paramType == MegaApiJava.USER_ATTR_LASTNAME) {
                request.email?.let { email ->
                    megaApi.getContact(email)?.let { user ->
                        Timber.d("User handle: %s", user.handle)
                        Timber.d("Visibility: %s",
                            user.visibility) //If user visibility == MegaUser.VISIBILITY_UNKNOWN then, non contact
                        if (user.visibility != MegaUser.VISIBILITY_VISIBLE) {
                            Timber.d("Non-contact")
                            when (request.paramType) {
                                MegaApiJava.USER_ATTR_FIRSTNAME -> {
                                    dbH.setNonContactEmail(request.email,
                                        user.handle.toString() + "")
                                    dbH.setNonContactFirstName(request.text,
                                        user.handle.toString() + "")
                                }
                                MegaApiJava.USER_ATTR_LASTNAME -> {
                                    dbH.setNonContactLastName(request.text,
                                        user.handle.toString() + "")
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
        if (e.errorCode == MegaError.API_OK) {
            askForFullAccountInfo()
            var listener: GetAttrUserListener? = GetAttrUserListener(application)
            megaApi.shouldShowRichLinkWarning(listener)
            megaApi.isRichPreviewsEnabled(listener)
            listener = GetAttrUserListener(application, true)
            if (dbH.myChatFilesFolderHandle == INVALID_HANDLE) {
                megaApi.getMyChatFilesFolder(listener)
            }

            //Ask for MU and CU folder when App in init state
            Timber.d("Get CameraUpload attribute on fetch nodes.")
            megaApi.getUserAttribute(USER_ATTR_CAMERA_UPLOADS_FOLDER,
                GetCameraUploadAttributeListener(application))

            // Init CU sync data after login successfully
            CUBackupInitializeChecker(megaApi).initCuSync()

            //Login check resumed pending transfers
            transfersManagement.checkResumedPendingTransfers()
            scheduleCameraUploadJob(application)
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
                application.sendBroadcast(Intent(
                    BROADCAST_ACTION_INTENT_SSL_VERIFICATION_FAILED))
            }
        } else if (e.errorCode == MegaError.API_ESID) {
            Timber.w("TYPE_LOGOUT:API_ESID")
            myAccountInfo.resetDefaults()
            (application as MegaApplication).isEsid = true
            AccountController.localLogoutApp(application, sharingScope)
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
        Timber.d("BackgroundRequestListener: onRequestTemporaryError: %s",
            request.requestString)
    }

    private fun sendBroadcastUpdateAccountDetails() {
        application.sendBroadcast(Intent(BROADCAST_ACTION_INTENT_UPDATE_ACCOUNT_DETAILS)
            .putExtra(ACTION_TYPE, Constants.UPDATE_ACCOUNT_DETAILS))
    }

    private fun askForFullAccountInfo() {
        Timber.d("askForFullAccountInfo")
        megaApi.run {
            getPaymentMethods(null)
            if (monitorStorageStateEvent.getState() == StorageState.Unknown) {
                getAccountDetails()
            } else {
                getSpecificAccountDetails(false, true, true)
            }
            getPricing(null)
            creditCardQuerySubscriptions(null)
        }
    }
}