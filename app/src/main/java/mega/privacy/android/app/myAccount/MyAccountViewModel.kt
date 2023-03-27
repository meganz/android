package mega.privacy.android.app.myAccount

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.constants.EventConstants.EVENT_REFRESH
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.generalusecase.FilePrepareUseCase
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.main.VerifyTwoFactorActivity
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.presentation.qrcode.QRCodeActivity
import mega.privacy.android.app.middlelayer.iab.BillingConstant
import mega.privacy.android.app.myAccount.usecase.CancelSubscriptionsUseCase
import mega.privacy.android.app.myAccount.usecase.Check2FAUseCase
import mega.privacy.android.app.myAccount.usecase.CheckPasswordReminderUseCase
import mega.privacy.android.app.myAccount.usecase.CheckVersionsUseCase
import mega.privacy.android.app.myAccount.usecase.ConfirmCancelAccountUseCase
import mega.privacy.android.app.myAccount.usecase.ConfirmChangeEmailUseCase
import mega.privacy.android.app.myAccount.usecase.GetUserDataUseCase
import mega.privacy.android.app.myAccount.usecase.KillSessionUseCase
import mega.privacy.android.app.myAccount.usecase.QueryRecoveryLinkUseCase
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.testpassword.TestPasswordActivity
import mega.privacy.android.app.presentation.verification.usecase.ResetPhoneNumberUseCase
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants.ACTION_OPEN_QR
import mega.privacy.android.app.utils.Constants.ACTION_REFRESH
import mega.privacy.android.app.utils.Constants.BUSINESS
import mega.privacy.android.app.utils.Constants.CHANGE_MAIL_2FA
import mega.privacy.android.app.utils.Constants.CHOOSE_PICTURE_PROFILE_CODE
import mega.privacy.android.app.utils.Constants.EMAIL_ADDRESS
import mega.privacy.android.app.utils.Constants.FREE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT
import mega.privacy.android.app.utils.Constants.OPEN_SCAN_QR
import mega.privacy.android.app.utils.Constants.PRO_FLEXI
import mega.privacy.android.app.utils.Constants.REQUEST_CAMERA
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_REFRESH
import mega.privacy.android.app.utils.Constants.REQUEST_WRITE_STORAGE
import mega.privacy.android.app.utils.Constants.TAKE_PICTURE_PROFILE_CODE
import mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber
import mega.privacy.android.domain.usecase.GetAccountDetails
import mega.privacy.android.domain.usecase.GetCurrentUserFullName
import mega.privacy.android.domain.usecase.GetExtendedAccountDetail
import mega.privacy.android.domain.usecase.GetMyAvatarFile
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.GetPaymentMethod
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.ChangeEmail
import mega.privacy.android.domain.usecase.account.UpdateCurrentUserName
import mega.privacy.android.domain.usecase.avatar.SetAvatarUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import mega.privacy.android.domain.usecase.verification.MonitorVerificationStatus
import mega.privacy.android.domain.usecase.verification.ResetSMSVerifiedPhoneNumber
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError.API_EARGS
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaUtilsAndroid
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

/**
 * My account view model
 *
 * @property context
 * @property myAccountInfo
 * @property megaApi
 * @property setAvatarUseCase
 * @property check2FAUseCase
 * @property checkVersionsUseCase
 * @property killSessionUseCase
 * @property cancelSubscriptionsUseCase
 * @property getMyAvatarFile
 * @property checkPasswordReminderUseCase
 * @property resetPhoneNumberUseCase
 * @property resetSMSVerifiedPhoneNumber
 * @property getUserDataUseCase
 * @property getFileVersionsOption
 * @property queryRecoveryLinkUseCase
 * @property confirmCancelAccountUseCase
 * @property confirmChangeEmailUseCase
 * @property filePrepareUseCase
 * @property monitorMyAvatarFile
 * @property getAccountDetails
 * @property getExtendedAccountDetail
 * @property getNumberOfSubscription
 * @property getPaymentMethod
 * @property getCurrentUserFullName
 * @property monitorUserUpdates
 * @property changeEmail
 * @property updateCurrentUserName
 * @property getCurrentUserEmail
 * @property monitorVerificationStatus
 * @property getFeatureFlagValueUseCase
 */
@HiltViewModel
@SuppressLint("StaticFieldLeak")
class MyAccountViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val myAccountInfo: MyAccountInfo,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val setAvatarUseCase: SetAvatarUseCase,
    private val check2FAUseCase: Check2FAUseCase,
    private val checkVersionsUseCase: CheckVersionsUseCase,
    private val killSessionUseCase: KillSessionUseCase,
    private val cancelSubscriptionsUseCase: CancelSubscriptionsUseCase,
    private val getMyAvatarFile: GetMyAvatarFile,
    private val checkPasswordReminderUseCase: CheckPasswordReminderUseCase,
    private val resetPhoneNumberUseCase: ResetPhoneNumberUseCase,
    private val resetSMSVerifiedPhoneNumber: ResetSMSVerifiedPhoneNumber,
    private val getUserDataUseCase: GetUserDataUseCase,
    private val getFileVersionsOption: GetFileVersionsOption,
    private val queryRecoveryLinkUseCase: QueryRecoveryLinkUseCase,
    private val confirmCancelAccountUseCase: ConfirmCancelAccountUseCase,
    private val confirmChangeEmailUseCase: ConfirmChangeEmailUseCase,
    private val filePrepareUseCase: FilePrepareUseCase,
    private val monitorMyAvatarFile: MonitorMyAvatarFile,
    private val getAccountDetails: GetAccountDetails,
    private val getExtendedAccountDetail: GetExtendedAccountDetail,
    private val getNumberOfSubscription: GetNumberOfSubscription,
    private val getPaymentMethod: GetPaymentMethod,
    private val getCurrentUserFullName: GetCurrentUserFullName,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val changeEmail: ChangeEmail,
    private val updateCurrentUserName: UpdateCurrentUserName,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val monitorVerificationStatus: MonitorVerificationStatus,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : BaseRxViewModel() {

    companion object {
        private const val CLICKS_TO_CHANGE_API_SERVER = 5
        private const val TIME_TO_SHOW_PAYMENT_INFO = 604800 //1 week in seconds
        const val CHECKING_2FA = "CHECKING_2FA"
    }

    private val withElevation: MutableLiveData<Boolean> = MutableLiveData()
    private val updateAccountDetails: MutableLiveData<Boolean> = MutableLiveData()

    private val defaultSubscriptionDialogState = SubscriptionDialogState.Invisible
    private val defaultCancelAccountDialogState = CancelAccountDialogState.Invisible

    private val _subscriptionDialogState =
        MutableStateFlow<SubscriptionDialogState>(defaultSubscriptionDialogState)
    val dialogVisibleState: StateFlow<SubscriptionDialogState>
        get() = _subscriptionDialogState

    private val _cancelAccountDialogState =
        MutableStateFlow<CancelAccountDialogState>(defaultCancelAccountDialogState)
    val cancelAccountDialogState: StateFlow<CancelAccountDialogState>
        get() = _cancelAccountDialogState

    private val _numberOfSubscription = MutableStateFlow(INVALID_VALUE.toLong())

    private var resetJob: Job? = null

    /**
     * Number of subscription
     */
    val numberOfSubscription = _numberOfSubscription.asStateFlow()

    private val _state = MutableStateFlow(MyAccountUiState())
    val state = _state.asStateFlow()

    init {
        refreshNumberOfSubscription(false)
        refreshUserName(false)
        refreshCurrentUserEmail()
        viewModelScope.launch {
            monitorUserUpdates()
                .filter { it == UserChanges.Firstname || it == UserChanges.Lastname || it == UserChanges.Email }
                .collect {
                    when (it) {
                        UserChanges.Email -> refreshCurrentUserEmail()
                        UserChanges.Firstname,
                        UserChanges.Lastname,
                        -> refreshUserName(true)
                        else -> Unit
                    }
                }
        }
        viewModelScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.MonitorPhoneNumber)) {
                monitorVerificationStatus().collect { status ->
                    _state.update {
                        it.copy(
                            verifiedPhoneNumber = (status.phoneNumber as? VerifiedPhoneNumber.PhoneNumber)
                                ?.phoneNumberString,
                            canVerifyPhoneNumber = status.canRequestOptInVerification,
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            flow {
                emit(getMyAvatarFile(isForceRefresh = false))
                emitAll(monitorMyAvatarFile())
            }.collect { file ->
                _state.update {
                    it.copy(avatar = file)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        resetJob?.cancel()
    }

    private fun refreshCurrentUserEmail() {
        viewModelScope.launch {
            _state.update {
                it.copy(email = getCurrentUserEmail().orEmpty())
            }
        }
    }

    private fun refreshUserName(forceRefresh: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    name = getCurrentUserFullName(
                        forceRefresh = forceRefresh,
                        defaultFirstName = context.getString(R.string.first_name_text),
                        defaultLastName = context.getString(R.string.lastname_text),
                    )
                )
            }
        }
    }

    /**
     * Refresh number of subscription
     *
     * @param clearCache
     */
    fun refreshNumberOfSubscription(clearCache: Boolean) {
        viewModelScope.launch {
            _numberOfSubscription.value =
                runCatching { getNumberOfSubscription(clearCache) }
                    .getOrDefault(INVALID_VALUE.toLong())
        }
    }

    /**
     * Check subscription
     *
     */
    fun checkSubscription() {
        PlatformInfo.values().firstOrNull {
            it.subscriptionMethodId == myAccountInfo.subscriptionMethodId
        }?.run {
            when {
                isSubscriptionAndGatewaySame(subscriptionMethodId) -> {
                    _subscriptionDialogState.value = SubscriptionDialogState.Visible(
                        SubscriptionCheckResult(typeID = TYPE_ANDROID_PLATFORM, platformInfo = this)
                    )
                }
                subscriptionMethodId == MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET ||
                        subscriptionMethodId == MegaApiJava.PAYMENT_METHOD_HUAWEI_WALLET -> {
                    _subscriptionDialogState.value = SubscriptionDialogState.Visible(
                        SubscriptionCheckResult(
                            typeID = TYPE_ANDROID_PLATFORM_NO_NAVIGATION,
                            platformInfo = this
                        )
                    )
                }
                subscriptionMethodId == MegaApiJava.PAYMENT_METHOD_ITUNES -> {
                    _subscriptionDialogState.value = SubscriptionDialogState.Visible(
                        SubscriptionCheckResult(typeID = TYPE_ITUNES, platformInfo = this)
                    )
                }
                else -> {
                    _cancelAccountDialogState.value =
                        CancelAccountDialogState.VisibleWithSubscription
                }
            }
        } ?: run {
            _cancelAccountDialogState.value =
                CancelAccountDialogState.VisibleDefault
        }
    }

    /**
     * Restore subscription dialog state
     *
     */
    fun restoreSubscriptionDialogState() {
        _subscriptionDialogState.value = defaultSubscriptionDialogState
    }

    /**
     * Restore cancel account dialog state
     *
     */
    fun restoreCancelAccountDialogState() {
        _cancelAccountDialogState.value = defaultCancelAccountDialogState
    }

    /**
     * Set cancel account dialog state
     *
     * @param isVisible
     */
    fun setCancelAccountDialogState(isVisible: Boolean) {
        _cancelAccountDialogState.value = if (isVisible) {
            CancelAccountDialogState.VisibleDefault
        } else {
            CancelAccountDialogState.Invisible
        }
    }

    /**
     * Check the subscription platform and current gateway if is same
     * @param subscriptionMethodId current subscription method id
     * @return ture is same
     */
    private fun isSubscriptionAndGatewaySame(subscriptionMethodId: Int): Boolean {
        return (BillingConstant.PAYMENT_GATEWAY == MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET
                && subscriptionMethodId == MegaApiJava.PAYMENT_METHOD_GOOGLE_WALLET) ||
                (BillingConstant.PAYMENT_GATEWAY == MegaApiJava.PAYMENT_METHOD_HUAWEI_WALLET
                        && subscriptionMethodId == MegaApiJava.PAYMENT_METHOD_HUAWEI_WALLET)
    }

    /**
     * Check elevation
     *
     * @return
     */
    fun checkElevation(): LiveData<Boolean> = withElevation

    /**
     * On update account details
     *
     * @return
     */
    fun onUpdateAccountDetails(): LiveData<Boolean> = updateAccountDetails

    /**
     * Set elevation
     *
     * @param withElevation
     */
    fun setElevation(withElevation: Boolean) {
        this.withElevation.value = withElevation
    }

    private fun setVersionsInfo() {
        _state.update {
            it.copy(versionsInfo = myAccountInfo.getFormattedPreviousVersionsSize())
        }
    }

    /**
     * Update account details
     *
     */
    fun updateAccountDetails() {
        updateAccountDetails.value = true
    }

    private var is2FaEnabled = false

    private var numOfClicksLastSession = 0

    private lateinit var snackbarShower: SnackbarShower

    private var confirmationLink: String? = null

    /**
     * Get name
     *
     * @return
     */
    fun getName(): String = state.value.name

    /**
     * Get email
     *
     * @return
     */
    fun getEmail(): String = state.value.email

    /**
     * Get account type
     *
     * @return
     */
    fun getAccountType(): Int = myAccountInfo.accountType

    /**
     * Is free account
     *
     * @return
     */
    fun isFreeAccount(): Boolean = getAccountType() == FREE

    /**
     * Is pro flexi account
     *
     * @return
     */
    fun isProFlexiAccount(): Boolean = getAccountType() == PRO_FLEXI

    /**
     * Get used storage
     *
     * @return
     */
    fun getUsedStorage(): String = myAccountInfo.usedFormatted

    /**
     * Get used storage percentage
     *
     * @return
     */
    fun getUsedStoragePercentage(): Int = myAccountInfo.usedPercentage

    /**
     * Get total storage
     *
     * @return
     */
    fun getTotalStorage(): String = myAccountInfo.totalFormatted

    /**
     * Get used transfer
     *
     * @return
     */
    fun getUsedTransfer(): String = myAccountInfo.usedTransferFormatted

    /**
     * Get used transfer percentage
     *
     * @return
     */
    fun getUsedTransferPercentage(): Int = myAccountInfo.usedTransferPercentage

    /**
     * Get total transfer
     *
     * @return
     */
    fun getTotalTransfer(): String = myAccountInfo.totalTransferFormatted

    /**
     * Get renew time
     *
     * @return
     */
    fun getRenewTime(): Long = myAccountInfo.subscriptionRenewTime

    /**
     * Get bonus storage s m s
     *
     * @return
     */
    fun getBonusStorageSMS(): String = myAccountInfo.bonusStorageSMS

    /**
     * Has renewable subscription
     *
     * @return
     */
    fun hasRenewableSubscription(): Boolean {
        return myAccountInfo.subscriptionStatus == MegaAccountDetails.SUBSCRIPTION_STATUS_VALID
                && myAccountInfo.subscriptionRenewTime > 0
    }

    /**
     * Get expiration time
     *
     * @return
     */
    fun getExpirationTime(): Long = myAccountInfo.proExpirationTime

    /**
     * Has expirable subscription
     *
     * @return
     */
    fun hasExpirableSubscription(): Boolean = myAccountInfo.proExpirationTime > 0

    /**
     * Get last session
     *
     * @return
     */
    fun getLastSession(): String = myAccountInfo.lastSessionFormattedDate ?: ""

    /**
     * There is no subscription
     *
     * @return
     */
    fun thereIsNoSubscription(): Boolean = _numberOfSubscription.value <= 0L

    /**
     * Get registered phone number
     *
     * @return
     */
    fun getRegisteredPhoneNumber(): String? = megaApi.smsVerifiedPhoneNumber()

    /**
     * Is already registered phone number
     *
     * @return
     */
    fun isAlreadyRegisteredPhoneNumber(): Boolean = !getRegisteredPhoneNumber().isNullOrEmpty()

    /**
     * Get cloud storage
     *
     * @return
     */
    fun getCloudStorage(): String = myAccountInfo.formattedUsedCloud

    /**
     * Get backups storage
     *
     * @return
     */
    fun getBackupsStorage(): String = myAccountInfo.formattedUsedBackups

    /**
     * Get incoming storage
     *
     * @return
     */
    fun getIncomingStorage(): String = myAccountInfo.formattedUsedIncoming

    /**
     * Get rubbish storage
     *
     * @return
     */
    fun getRubbishStorage(): String = myAccountInfo.formattedUsedRubbish

    /**
     * Is business account
     *
     * @return
     */
    fun isBusinessAccount(): Boolean = megaApi.isBusinessAccount && getAccountType() == BUSINESS

    /**
     * Get master key
     *
     * @return
     */
    fun getMasterKey(): String = megaApi.exportMasterKey()

    /**
     * Check versions
     *
     */
    fun checkVersions() {
        if (myAccountInfo.numVersions == INVALID_VALUE) {
            checkVersionsUseCase.check()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { setVersionsInfo() },
                    onError = Timber::w
                )
                .addTo(composite)
        } else setVersionsInfo()
    }

    /**
     * Kill sessions
     *
     * @param action
     * @receiver
     */
    fun killSessions(action: (Boolean) -> Unit) {
        killSessionUseCase.kill()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { action.invoke(true) },
                onError = { error ->
                    Timber.w("Error when killing sessions: ${error.message}")
                    action.invoke(false)
                }
            )
            .addTo(composite)
    }

    /**
     * Refresh
     *
     * @param activity
     */
    fun refresh(activity: Activity) {
        val intent = Intent(activity, LoginActivity::class.java)
        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
        intent.action = ACTION_REFRESH

        activity.startActivityForResult(intent, REQUEST_CODE_REFRESH)
    }

    /**
     * Manage activity result
     *
     * @param activity
     * @param requestCode
     * @param resultCode
     * @param data
     * @param snackbarShower
     */
    fun manageActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        snackbarShower: SnackbarShower,
    ) {
        if (resultCode != RESULT_OK) {
            Timber.w("Result code not OK. Request code $requestCode")
            return
        }

        this.snackbarShower = snackbarShower

        when (requestCode) {
            REQUEST_CODE_REFRESH -> {
                viewModelScope.launch {
                    getAccountDetails(true)
                    getExtendedAccountDetail(
                        forceRefresh = true,
                        sessions = true,
                        purchases = false,
                        transactions = false
                    )
                    LiveEventBus.get<Boolean>(EVENT_REFRESH).post(true)
                }
            }
            TAKE_PICTURE_PROFILE_CODE -> addProfileAvatar(null)
            CHOOSE_PICTURE_PROFILE_CODE -> {
                if (data == null) {
                    showResult(context.getString(R.string.error_changing_user_avatar_image_not_available))
                    return
                }

                /* Need to check image existence before use due to android content provider issue.
                Can not check query count - still get count = 1 even file does not exist
                */
                var fileExists = false
                val inputStream: InputStream?

                try {
                    inputStream = data.data?.let { activity.contentResolver?.openInputStream(it) }
                    if (inputStream != null) {
                        fileExists = true
                    }

                    inputStream?.close()
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                if (!fileExists) {
                    showResult(context.getString(R.string.error_changing_user_avatar_image_not_available))
                    return
                }

                data.action = Intent.ACTION_GET_CONTENT
                _state.update { it.copy(isLoading = true) }
                prepareAvatarFile(data)
            }
        }
    }

    /**
     * Prepares a file to be set as avatar.
     *
     * @param data Intent containing the file to be set as avatar.
     */
    private fun prepareAvatarFile(data: Intent) {
        filePrepareUseCase.prepareFile(data)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { info -> addProfileAvatar(info.fileAbsolutePath) },
                onError = Timber::w
            )
            .addTo(composite)
    }

    /**
     * Increment last session click
     *
     * @return
     */
    fun incrementLastSessionClick(): Boolean {
        numOfClicksLastSession++

        if (numOfClicksLastSession < CLICKS_TO_CHANGE_API_SERVER)
            return false

        numOfClicksLastSession = 0
        return true
    }

    /**
     * Checks if business payment attention is needed.
     *
     * @return True if business payment attention is needed, false otherwise.
     */
    private fun isBusinessPaymentAttentionNeeded(): Boolean {
        val status = megaApi.businessStatus

        return isBusinessAccount() && megaApi.isMasterBusinessAccount
                && (status == MegaApiJava.BUSINESS_STATUS_EXPIRED
                || status == MegaApiJava.BUSINESS_STATUS_GRACE_PERIOD)
    }

    /**
     * Should show payment info
     *
     * @return
     */
    fun shouldShowPaymentInfo(): Boolean {
        val timeToCheck =
            if (hasRenewableSubscription()) myAccountInfo.subscriptionRenewTime
            else myAccountInfo.proExpirationTime

        val currentTime = System.currentTimeMillis() / 1000

        return isBusinessPaymentAttentionNeeded()
                || timeToCheck.minus(currentTime) <= TIME_TO_SHOW_PAYMENT_INFO
    }

    /**
     * Cancel subscriptions
     *
     * @param feedback
     * @param action
     * @receiver
     */
    fun cancelSubscriptions(feedback: String?, action: (Boolean) -> Unit) {
        cancelSubscriptionsUseCase.cancel(feedback)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { action.invoke(true) },
                onError = { error ->
                    Timber.w("Error when killing sessions: ${error.message}")
                    action.invoke(false)
                }
            )
            .addTo(composite)
    }

    /**
     * Logout
     *
     * @param context
     */
    fun logout(context: Context) {
        checkPasswordReminderUseCase.check(true)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { show ->
                    if (show) {
                        context.startActivity(
                            Intent(context, TestPasswordActivity::class.java)
                                .putExtra("logout", true)
                        )
                    } else AccountController.logout(context, megaApi, viewModelScope)
                },
                onError = { error ->
                    Timber.e(error, "Error when killing sessions")
                }
            )
            .addTo(composite)
    }

    /**
     * Capture photo
     *
     * @param activity
     */
    fun capturePhoto(activity: Activity) {
        val hasStoragePermission: Boolean = hasPermissions(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val hasCameraPermission: Boolean = hasPermissions(
            activity,
            Manifest.permission.CAMERA
        )

        if (!hasStoragePermission && !hasCameraPermission) {
            requestPermission(
                activity,
                REQUEST_WRITE_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )

            return
        } else if (!hasStoragePermission) {
            requestPermission(
                activity,
                REQUEST_WRITE_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            return
        } else if (!hasCameraPermission) {
            requestPermission(
                activity,
                REQUEST_CAMERA,
                Manifest.permission.CAMERA
            )

            return
        }

        Util.checkTakePicture(activity, TAKE_PICTURE_PROFILE_CODE)
    }

    /**
     * Launch choose photo intent
     *
     * @param activity
     */
    fun launchChoosePhotoIntent(activity: Activity) {
        val intent = Intent()
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        intent.type = "image/*"
        activity.startActivityForResult(
            Intent.createChooser(intent, null),
            CHOOSE_PICTURE_PROFILE_CODE
        )
    }

    /**
     * Open q r
     *
     * @param activity
     */
    fun openQR(activity: Activity) {
        if (CallUtil.isNecessaryDisableLocalCamera() != INVALID_VALUE.toLong()) {
            CallUtil.showConfirmationOpenCamera(activity, ACTION_OPEN_QR, false)
        } else {
            activity.startActivity(
                Intent(activity, QRCodeActivity::class.java)
                    .putExtra(OPEN_SCAN_QR, false)
            )
        }
    }

    /**
     * Adds a photo as avatar.
     *
     * @param path           Path of the chosen photo or null if is a new taken photo.
     */
    private fun addProfileAvatar(path: String?) {
        val app = MegaApplication.getInstance()
        val myEmail = megaApi.myUser?.email
        val imgFile = if (!path.isNullOrEmpty()) File(path)
        else CacheFolderManager.getCacheFile(
            app,
            CacheFolderManager.TEMPORARY_FOLDER,
            "picture.jpg"
        )
        if (!FileUtil.isFileAvailable(imgFile) || myEmail.isNullOrEmpty()) {
            showResult(context.getString(R.string.general_error))
            return
        }

        val newFile = CacheFolderManager.buildAvatarFile(app, myEmail + "Temp.jpg")

        if (newFile != null) {
            MegaUtilsAndroid.createAvatar(imgFile, newFile)
            viewModelScope.launch {
                runCatching {
                    setAvatarUseCase(newFile.absolutePath)
                }.onSuccess {
                    showResult(context.getString(R.string.success_changing_user_avatar))
                }.onFailure {
                    showResult(context.getString(R.string.error_changing_user_avatar))
                }
                _state.update { it.copy(isLoading = false) }
            }
        } else {
            Timber.e("Destination PATH is NULL")
        }
    }

    /**
     * Delete profile avatar
     *
     * @param context
     * @param snackbarShower
     */
    fun deleteProfileAvatar(context: Context, snackbarShower: SnackbarShower) {
        CacheFolderManager.buildAvatarFile(context, megaApi.myEmail + JPG_EXTENSION)?.let {
            if (FileUtil.isFileAvailable(it)) {
                Timber.d("Avatar to delete: ${it.absolutePath}")
                it.delete()
            }
        }
        viewModelScope.launch {
            runCatching {
                setAvatarUseCase(null)
            }.onSuccess {
                snackbarShower.showSnackbar(context.getString(R.string.success_deleting_user_avatar))
            }.onFailure {
                snackbarShower.showSnackbar(context.getString(R.string.error_deleting_user_avatar))
            }
        }
    }

    /**
     * Change name
     *
     * @param oldFirstName
     * @param oldLastName
     * @param newFirstName
     * @param newLastName
     */
    fun changeName(
        oldFirstName: String,
        oldLastName: String,
        newFirstName: String,
        newLastName: String,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = runCatching {
                updateCurrentUserName(
                    oldFirstName = oldFirstName,
                    oldLastName = oldLastName,
                    newFirstName = newFirstName,
                    newLastName = newLastName,
                )
            }
            _state.update { it.copy(changeUserNameResult = result, isLoading = false) }
        }
    }

    /**
     * Check2f a
     *
     */
    fun check2FA() {
        check2FAUseCase.check()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { result -> is2FaEnabled = result }
            .addTo(composite)
    }

    /**
     * Change email
     *
     * @param context
     * @param newEmail
     * @return
     */
    fun changeEmail(context: Context, newEmail: String): String? {
        return when {
            newEmail == getEmail() -> context.getString(R.string.mail_same_as_old)
            !EMAIL_ADDRESS.matcher(newEmail)
                .matches() -> context.getString(R.string.error_invalid_email)
            is2FaEnabled -> {
                context.startActivity(
                    Intent(context, VerifyTwoFactorActivity::class.java)
                        .putExtra(VerifyTwoFactorActivity.KEY_VERIFY_TYPE, CHANGE_MAIL_2FA)
                        .putExtra(VerifyTwoFactorActivity.KEY_NEW_EMAIL, newEmail)
                )

                CHECKING_2FA
            }
            else -> {
                viewModelScope.launch {
                    val changeEmailResult = runCatching { changeEmail(newEmail) }
                    _state.update { it.copy(changeEmailResult = changeEmailResult) }
                }
                null
            }
        }
    }

    /**
     * Reset phone number
     *
     * @param isModify
     * @param snackbarShower
     * @param action
     * @receiver
     */
    fun resetPhoneNumber(isModify: Boolean, snackbarShower: SnackbarShower, action: () -> Unit) {
        resetJob = viewModelScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.MonitorPhoneNumber)) {
                runCatching { resetSMSVerifiedPhoneNumber() }
                    .onSuccess {
                        getUserData(isModify, snackbarShower, action)
                    }
                    .onFailure {
                        Timber.e(it, "Reset phone number failed")
                        snackbarShower.showSnackbar(context.getString(R.string.remove_phone_number_fail))
                    }
            } else {
                runCatching { resetPhoneNumberUseCase() }
                    .onSuccess {
                        getUserData(isModify, snackbarShower, action)
                    }
                    .onFailure {
                        Timber.e(it, "Reset phone number failed")
                        snackbarShower.showSnackbar(context.getString(R.string.remove_phone_number_fail))
                    }
            }
        }
    }

    /**
     * Gets the current account user data.
     *
     * @param isModify       True if the action is modify phone number, false if is remove.
     * @param snackbarShower Callback to show the request result if needed.
     * @param action         Action to perform after reset the phone number if modifying.
     */
    private fun getUserData(isModify: Boolean, snackbarShower: SnackbarShower, action: () -> Unit) {
        getUserDataUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    if (isModify) action.invoke()
                    else snackbarShower.showSnackbar(context.getString(R.string.remove_phone_number_success))
                },
                onError = { error ->
                    Timber.w("Reset phone number failed: ${error.message}")
                    snackbarShower.showSnackbar(context.getString(R.string.remove_phone_number_fail))
                })
            .addTo(composite)
    }

    /**
     * Get file versions option
     *
     */
    fun getFileVersionsOption() {
        viewModelScope.launch {
            val isDisableFileVersions = getFileVersionsOption(forceRefresh = true)
            _state.update {
                it.copy(
                    versionsInfo = myAccountInfo.getFormattedPreviousVersionsSize(),
                    isFileVersioningEnabled = isDisableFileVersions.not()
                )
            }
        }
    }

    /**
     * Confirm cancel account
     *
     * @param link
     * @param action
     * @receiver
     */
    fun confirmCancelAccount(link: String, action: (String) -> Unit) {
        queryRecoveryLinkUseCase.queryCancelAccount(link)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { result ->
                confirmationLink = result
                action.invoke(result)
            }
            .addTo(composite)
    }

    /**
     * Finish confirm cancel account
     *
     * @param password
     * @param action
     * @receiver
     */
    fun finishConfirmCancelAccount(password: String, action: (String) -> Unit) {
        confirmationLink?.let { link ->
            confirmCancelAccountUseCase.confirm(link, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { Timber.d("ACCOUNT CANCELED") },
                    onError = { error -> error.message?.let { message -> action.invoke(message) } })
                .addTo(composite)
        }
    }

    /**
     * Confirm change email
     *
     * @param link
     * @param action
     * @receiver
     */
    fun confirmChangeEmail(link: String, action: (String) -> Unit) {
        queryRecoveryLinkUseCase.queryChangeEmail(link)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { result ->
                confirmationLink = link
                action.invoke(result)
            }
            .addTo(composite)
    }

    /**
     * Finish confirm change email
     *
     * @param password
     * @param actionSuccess
     * @param actionError
     * @receiver
     * @receiver
     */
    fun finishConfirmChangeEmail(
        password: String,
        actionSuccess: (String) -> Unit,
        actionError: (String) -> Unit,
    ) {
        confirmationLink?.let { link ->
            confirmChangeEmailUseCase.confirm(link, password)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { result ->
                    if (Patterns.EMAIL_ADDRESS.matcher(result).find()) {
                        Timber.d("EMAIL_CHANGED")
                        actionSuccess.invoke(result)
                    } else {
                        actionError.invoke(result)
                    }
                }
                .addTo(composite)
        }
    }

    /**
     * Finish password change
     *
     * @param result
     * @param actionSuccess
     * @param actionError
     * @receiver
     * @receiver
     */
    fun finishPasswordChange(
        result: Int,
        actionSuccess: (String) -> Unit,
        actionError: (String) -> Unit,
    ) {
        when (result) {
            API_OK -> actionSuccess.invoke(context.getString(R.string.pass_changed_alert))
            API_EARGS -> actionError.invoke(context.getString(R.string.old_password_provided_incorrect))
            else -> actionError.invoke(context.getString(R.string.general_text_error))
        }
    }

    /**
     * Uses the SnackbarShower object if is initialized to show an action result.
     *
     * @param result String to show as result.
     */
    private fun showResult(result: String) {
        if (this::snackbarShower.isInitialized) {
            snackbarShower.showSnackbar(result)
        }
    }

    /**
     * Set open upgrade from
     *
     */
    fun setOpenUpgradeFrom() {
        myAccountInfo.upgradeOpenedFrom = MyAccountInfo.UpgradeFrom.ACCOUNT
    }

    /**
     * Refresh account info
     *
     */
    fun refreshAccountInfo() {
        viewModelScope.launch {
            getAccountDetails(forceRefresh = myAccountInfo.usedFormatted.trim().isEmpty())
            getExtendedAccountDetail(
                forceRefresh = false,
                sessions = true,
                purchases = false,
                transactions = false
            )
            getPaymentMethod(false)
        }
    }

    /**
     * Mark handle change email result
     *
     */
    fun markHandleChangeEmailResult() {
        _state.update { it.copy(changeEmailResult = null) }
    }

    /**
     * Mark handle change user name result
     *
     */
    fun markHandleChangeUserNameResult() {
        _state.update { it.copy(changeUserNameResult = null) }
    }
}