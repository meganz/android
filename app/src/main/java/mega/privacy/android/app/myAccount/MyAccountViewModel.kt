package mega.privacy.android.app.myAccount

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.generalusecase.FilePrepareUseCase
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.main.dialog.storagestatus.SubscriptionCheckResult
import mega.privacy.android.app.main.dialog.storagestatus.TYPE_ANDROID_PLATFORM
import mega.privacy.android.app.main.dialog.storagestatus.TYPE_ANDROID_PLATFORM_NO_NAVIGATION
import mega.privacy.android.app.main.dialog.storagestatus.TYPE_ITUNES
import mega.privacy.android.app.middlelayer.iab.BillingConstant
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.app.presentation.testpassword.TestPasswordActivity
import mega.privacy.android.app.presentation.verifytwofactor.VerifyTwoFactorActivity
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ACTION_REFRESH
import mega.privacy.android.app.utils.Constants.CANCEL_ACCOUNT_LINK_REGEXS
import mega.privacy.android.app.utils.Constants.CHANGE_MAIL_2FA
import mega.privacy.android.app.utils.Constants.EMAIL_ADDRESS
import mega.privacy.android.app.utils.Constants.FREE
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT
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
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.verification.VerifiedPhoneNumber
import mega.privacy.android.domain.exception.account.ConfirmCancelAccountException
import mega.privacy.android.domain.exception.account.ConfirmChangeEmailException
import mega.privacy.android.domain.exception.account.QueryCancelLinkException
import mega.privacy.android.domain.exception.account.QueryChangeEmailLinkException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetCurrentUserFullName
import mega.privacy.android.domain.usecase.GetExportMasterKeyUseCase
import mega.privacy.android.domain.usecase.GetExtendedAccountDetail
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.IsUrlMatchesRegexUseCase
import mega.privacy.android.domain.usecase.MonitorBackupFolder
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.BroadcastRefreshSessionUseCase
import mega.privacy.android.domain.usecase.account.CancelSubscriptionsUseCase
import mega.privacy.android.domain.usecase.account.ChangeEmail
import mega.privacy.android.domain.usecase.account.CheckVersionsUseCase
import mega.privacy.android.domain.usecase.account.ConfirmCancelAccountUseCase
import mega.privacy.android.domain.usecase.account.ConfirmChangeEmailUseCase
import mega.privacy.android.domain.usecase.account.GetUserDataUseCase
import mega.privacy.android.domain.usecase.account.IsMultiFactorAuthEnabledUseCase
import mega.privacy.android.domain.usecase.account.KillOtherSessionsUseCase
import mega.privacy.android.domain.usecase.account.QueryCancelLinkUseCase
import mega.privacy.android.domain.usecase.account.QueryChangeEmailLinkUseCase
import mega.privacy.android.domain.usecase.account.UpdateCurrentUserName
import mega.privacy.android.domain.usecase.avatar.GetMyAvatarFileUseCase
import mega.privacy.android.domain.usecase.avatar.SetAvatarUseCase
import mega.privacy.android.domain.usecase.billing.GetPaymentMethodUseCase
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import mega.privacy.android.domain.usecase.login.CheckPasswordReminderUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.verification.MonitorVerificationStatus
import mega.privacy.android.domain.usecase.verification.ResetSMSVerifiedPhoneNumberUseCase
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError.API_EARGS
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaUtilsAndroid
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * My account view model
 *
 * @property context
 * @property myAccountInfo
 * @property megaApi
 * @property setAvatarUseCase
 * @property isMultiFactorAuthEnabledUseCase [IsMultiFactorAuthEnabledUseCase]
 * @property checkVersionsUseCase
 * @property killOtherSessionsUseCase [KillOtherSessionsUseCase]
 * @property cancelSubscriptionsUseCase
 * @property getMyAvatarFileUseCase
 * @property checkPasswordReminderUseCase
 * @property resetSMSVerifiedPhoneNumberUseCase
 * @property getUserDataUseCase
 * @property getFileVersionsOption
 * @property queryCancelLinkUseCase Queries information on an Account Cancellation Link
 * @property queryChangeEmailLinkUseCase Queries information on a Change Email Link
 * @property isUrlMatchesRegexUseCase Checks if the URL Matches any of the Regex Patterns provided
 * @property [confirmCancelAccountUseCase] [ConfirmCancelAccountUseCase]
 * @property confirmChangeEmailUseCase
 * @property filePrepareUseCase
 * @property getAccountDetailsUseCase
 * @property getExtendedAccountDetail
 * @property getNumberOfSubscription
 * @property getPaymentMethodUseCase
 * @property getCurrentUserFullName
 * @property monitorUserUpdates
 * @property changeEmail
 * @property updateCurrentUserName
 * @property getCurrentUserEmail
 * @property monitorVerificationStatus
 * @property snackBarHandler Handler used to display a Snackbar
 */
@HiltViewModel
@SuppressLint("StaticFieldLeak")
class MyAccountViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val myAccountInfo: MyAccountInfo,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val setAvatarUseCase: SetAvatarUseCase,
    private val isMultiFactorAuthEnabledUseCase: IsMultiFactorAuthEnabledUseCase,
    private val checkVersionsUseCase: CheckVersionsUseCase,
    private val killOtherSessionsUseCase: KillOtherSessionsUseCase,
    private val cancelSubscriptionsUseCase: CancelSubscriptionsUseCase,
    private val getMyAvatarFileUseCase: GetMyAvatarFileUseCase,
    private val checkPasswordReminderUseCase: CheckPasswordReminderUseCase,
    private val resetSMSVerifiedPhoneNumberUseCase: ResetSMSVerifiedPhoneNumberUseCase,
    private val getUserDataUseCase: GetUserDataUseCase,
    private val getFileVersionsOption: GetFileVersionsOption,
    private val queryCancelLinkUseCase: QueryCancelLinkUseCase,
    private val queryChangeEmailLinkUseCase: QueryChangeEmailLinkUseCase,
    private val isUrlMatchesRegexUseCase: IsUrlMatchesRegexUseCase,
    private val confirmCancelAccountUseCase: ConfirmCancelAccountUseCase,
    private val confirmChangeEmailUseCase: ConfirmChangeEmailUseCase,
    private val filePrepareUseCase: FilePrepareUseCase,
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val getExtendedAccountDetail: GetExtendedAccountDetail,
    private val getNumberOfSubscription: GetNumberOfSubscription,
    private val getPaymentMethodUseCase: GetPaymentMethodUseCase,
    private val getCurrentUserFullName: GetCurrentUserFullName,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val changeEmail: ChangeEmail,
    private val updateCurrentUserName: UpdateCurrentUserName,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val monitorVerificationStatus: MonitorVerificationStatus,
    private val getExportMasterKeyUseCase: GetExportMasterKeyUseCase,
    private val broadcastRefreshSessionUseCase: BroadcastRefreshSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val monitorBackupFolder: MonitorBackupFolder,
    private val getFolderTreeInfo: GetFolderTreeInfo,
    private val getNodeByIdUseCase: GetNodeByIdUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val snackBarHandler: SnackBarHandler,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) : ViewModel() {

    companion object {
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
            flow {
                emitAll(monitorUserUpdates()
                    .catch { Timber.w("Exception monitoring user updates: $it") }
                    .filter { it == UserChanges.Firstname || it == UserChanges.Lastname || it == UserChanges.Email })
            }.collect {
                when (it) {
                    UserChanges.Email -> refreshCurrentUserEmail()
                    UserChanges.Firstname, UserChanges.Lastname -> refreshUserName(true)
                    else -> Unit
                }
            }
        }
        viewModelScope.launch {
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
        viewModelScope.launch {
            monitorBackupFolder()
                .catch { Timber.w("Exception monitoring backups folder: $it") }
                .map { it.getOrNull() }
                .collectLatest { backupsFolderNodeId ->
                    backupsFolderNodeId?.let {
                        runCatching {
                            getNodeByIdUseCase(it)?.let { node ->
                                getFolderTreeInfo(node as TypedFolderNode).let { folderTreeInfo ->
                                    _state.update {
                                        it.copy(
                                            backupStorageSize = folderTreeInfo.totalCurrentSizeInBytes
                                        )
                                    }
                                }
                            }
                        }.onFailure { Timber.w(it) }
                    }
                }
        }
        checkNewCancelSubscriptionFeature()
    }

    override fun onCleared() {
        super.onCleared()
        resetJob?.cancel()
    }


    /**
     * Checks if the new cancel subscription feature is enabled.
     */
    fun isNewCancelSubscriptionFeatureEnabled(): Boolean =
        state.value.showNewCancelSubscriptionFeature

    /**
     * Checks if the cancel subscription feature is enabled
     */
    private fun checkNewCancelSubscriptionFeature() {
        viewModelScope.launch {
            runCatching {
                val isEnabled = getFeatureFlagValueUseCase(AppFeatures.CancelSubscription)
                _state.update {
                    it.copy(showNewCancelSubscriptionFeature = isEnabled)
                }
            }.onFailure {
                Timber.e(
                    it,
                    "Failed to check for new cancel subscription feature"
                )
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

    private fun refreshCurrentUserEmail() {
        viewModelScope.launch {
            _state.update {
                it.copy(email = getCurrentUserEmail().orEmpty())
            }
        }
    }

    /**
     * Refresh number of subscription
     *
     * @param clearCache
     */
    private fun refreshNumberOfSubscription(clearCache: Boolean) {
        viewModelScope.launch {
            _numberOfSubscription.value =
                runCatching { getNumberOfSubscription(clearCache) }
                    .getOrDefault(INVALID_VALUE.toLong())
        }
    }

    /**
     * Checks the User's Subscription type
     */
    private fun checkSubscription() {
        PlatformInfo.entries.firstOrNull {
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
            it.copy(versionsInfo = myAccountInfo.getFormattedPreviousVersionsSize(context))
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

    private lateinit var snackbarShower: SnackbarShower

    private var confirmationLink: String? = null

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
     * Get master key
     *
     * @return
     */
    suspend fun getMasterKey(): String? = getExportMasterKeyUseCase()

    /**
     * Check versions
     *
     */
    fun checkVersions() {
        if (myAccountInfo.numVersions == INVALID_VALUE) {
            viewModelScope.launch {
                runCatching { checkVersionsUseCase() }
                    .fold(
                        onSuccess = { value ->
                            value?.let {
                                myAccountInfo.numVersions = value.numberOfVersions
                                myAccountInfo.previousVersionsSize =
                                    value.sizeOfPreviousVersionsInBytes
                                setVersionsInfo()
                            }
                        },
                        onFailure = Timber::w
                    )
            }
        } else setVersionsInfo()
    }

    /**
     * Kills all other active Sessions except the current Session
     */
    fun killOtherSessions() = viewModelScope.launch {
        runCatching {
            killOtherSessionsUseCase()
        }.onSuccess {
            Timber.d("Successfully killed all other sessions")
            snackBarHandler.postSnackbarMessage(
                resId = R.string.success_kill_all_sessions,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
        }.onFailure {
            Timber.w("Error killing all other sessions: ${it.message}")
            snackBarHandler.postSnackbarMessage(
                resId = R.string.error_kill_all_sessions,
                snackbarDuration = MegaSnackbarDuration.Long,
            )
        }
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
     * @param requestCode
     * @param resultCode
     * @param snackbarShower
     */
    fun manageActivityResult(
        requestCode: Int,
        resultCode: Int,
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
                    runCatching {
                        getAccountDetailsUseCase(true)
                        getExtendedAccountDetail(
                            forceRefresh = true,
                            sessions = true,
                            purchases = false,
                            transactions = false
                        )
                    }.onFailure {
                        Timber.e(it)
                    }
                    broadcastRefreshSessionUseCase()
                }
            }

            TAKE_PICTURE_PROFILE_CODE -> addProfileAvatar(null)
        }
    }

    /**
     * Handle avatar change
     * we use @ioDispatcher to avoid blocking the main thread but it's temporary
     * will replace by the use case in the future
     *
     * @param uri Uri containing the file to be set as avatar.
     */
    fun handleAvatarChange(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(ioDispatcher) {
                    context.contentResolver?.openInputStream(uri)?.use { inputStream ->
                        CacheFolderManager.getCacheFile(
                            CacheFolderManager.TEMPORARY_FOLDER,
                            "picture.jpg"
                        )?.let {
                            it.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                            addProfileAvatar(it.absolutePath)
                        }
                    }
                }
            }.onFailure {
                showResult(context.getString(R.string.error_changing_user_avatar_image_not_available))
            }
        }
    }

    /**
     * Cancel subscriptions
     *
     * @param feedback Feedback message to cancel subscriptions.
     */
    fun cancelSubscriptions(feedback: String?) {
        viewModelScope.launch {
            runCatching {
                cancelSubscriptionsUseCase(feedback).let { isSuccessful ->
                    snackBarHandler.postSnackbarMessage(
                        if (isSuccessful) R.string.cancel_subscription_ok
                        else R.string.cancel_subscription_error,
                        snackbarDuration = MegaSnackbarDuration.Long,
                    )
                }
            }.onFailure {
                Timber.e(it, "Error cancelling subscriptions")
                snackBarHandler.postSnackbarMessage(
                    R.string.cancel_subscription_error,
                    snackbarDuration = MegaSnackbarDuration.Long,
                )
            }
            refreshNumberOfSubscription(true)
        }
    }

    /**
     * Logout
     *
     * @param context
     */
    fun logout(context: Context) {
        viewModelScope.launch {
            runCatching { checkPasswordReminderUseCase(true) }
                .onSuccess { show ->
                    if (show) {
                        context.startActivity(
                            Intent(context, TestPasswordActivity::class.java)
                                .putExtra("logout", true)
                        )
                    } else logout()
                }.onFailure { error ->
                    Timber.e(error, "Error when killing sessions")
                }
        }
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
     * Adds a photo as avatar.
     *
     * @param path           Path of the chosen photo or null if is a new taken photo.
     */
    private fun addProfileAvatar(path: String?) {
        val myEmail = megaApi.myUser?.email
        val imgFile = if (!path.isNullOrEmpty()) File(path)
        else CacheFolderManager.getCacheFile(
            CacheFolderManager.TEMPORARY_FOLDER,
            "picture.jpg"
        )
        if (!FileUtil.isFileAvailable(imgFile) || myEmail.isNullOrEmpty()) {
            showResult(context.getString(R.string.general_error))
            return
        }

        val newFile = CacheFolderManager.buildAvatarFile(myEmail + "Temp.jpg")

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
        CacheFolderManager.buildAvatarFile(megaApi.myEmail + JPG_EXTENSION)?.let {
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
     * Checks if Multi-Factor Authentication has been enabled or not
     */
    fun checkMultiFactorAuthenticationState() {
        viewModelScope.launch {
            runCatching {
                isMultiFactorAuthEnabledUseCase()
            }.onSuccess {
                Timber.d("Multi-Factor Authentication check successful")
                is2FaEnabled = it
            }.onFailure {
                Timber.w("Error checking the Multi-Factor Authentication state: ${it.message}")
            }
        }
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
     * @receiver
     */
    fun resetPhoneNumber(isModify: Boolean, snackbarShower: SnackbarShower) {
        resetJob = viewModelScope.launch {
            runCatching { resetSMSVerifiedPhoneNumberUseCase() }
                .onSuccess {
                    getUserData(isModify, snackbarShower)
                }
                .onFailure {
                    Timber.e(it, "Reset phone number failed")
                    snackbarShower.showSnackbar(context.getString(R.string.remove_phone_number_fail))
                }
        }
    }

    /**
     * Gets the current account user data.
     *
     * @param isModify       True if the action is modify phone number, false if is remove.
     * @param snackbarShower Callback to show the request result if needed.
     */
    private fun getUserData(isModify: Boolean, snackbarShower: SnackbarShower) {
        viewModelScope.launch {
            runCatching {
                getUserDataUseCase()
            }.onSuccess {
                if (isModify) {
                    _state.update {
                        it.copy(shouldNavigateToSmsVerification = true)
                    }
                } else {
                    snackbarShower.showSnackbar(context.getString(R.string.remove_phone_number_success))
                }
            }.onFailure {
                Timber.w("Reset phone number failed: ${it.message}")
                snackbarShower.showSnackbar(context.getString(R.string.remove_phone_number_fail))
            }
        }
    }

    /**
     * Get file versions option
     *
     */
    fun getFileVersionsOption() {
        viewModelScope.launch {
            runCatching {
                getFileVersionsOption(forceRefresh = true)
            }.onSuccess { isDisableFileVersions ->
                _state.update {
                    it.copy(
                        versionsInfo = myAccountInfo.getFormattedPreviousVersionsSize(context),
                        isFileVersioningEnabled = isDisableFileVersions.not()
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Begins the process of cancelling the User's Account
     *
     * @param accountCancellationLink the Account Cancellation Link
     */
    fun cancelAccount(accountCancellationLink: String) {
        viewModelScope.launch {
            runCatching {
                queryCancelLinkUseCase(accountCancellationLink)
            }.onSuccess { newAccountCancellationLink ->
                Timber.d("Successfully queried the Account Cancellation Link")
                confirmationLink = newAccountCancellationLink
                checkAccountCancellationLinkValidity()
            }.onFailure { exception ->
                Timber.e("An issue occurred when querying the Account Cancellation Link", exception)
                _state.update {
                    it.copy(
                        errorMessageRes = when (exception) {
                            is QueryCancelLinkException.UnrelatedAccountCancellationLink -> R.string.error_not_logged_with_correct_account
                            is QueryCancelLinkException.ExpiredAccountCancellationLink -> R.string.cancel_link_expired
                            else -> R.string.invalid_link
                        }
                    )
                }
            }
        }
    }

    /**
     * Once the Account Cancellation Link has been successfully queried and no issues were found,
     * check if the Account Cancellation Link matches the specified Regex
     */
    private fun checkAccountCancellationLinkValidity() {
        viewModelScope.launch {
            runCatching {
                isUrlMatchesRegexUseCase(
                    url = confirmationLink,
                    patterns = CANCEL_ACCOUNT_LINK_REGEXS,
                )
            }.onSuccess { isMatching ->
                Timber.d(
                    "Successfully checked if the URL matches the Cancel Account Link Regex\n" +
                            "Is Account Cancellation Link Valid: $isMatching"
                )
                if (isMatching) {
                    checkSubscription()
                } else {
                    _state.update { it.copy(errorMessageRes = R.string.general_error_word) }
                }
            }.onFailure { exception ->
                Timber.e(
                    "An issue occurred when checking if the URL matches the Cancel Account Link Regex",
                    exception
                )
                _state.update { it.copy(errorMessageRes = R.string.general_error_word) }
            }
        }
    }

    /**
     * Finishes the Account cancellation process
     *
     * @param accountPassword The password of the Account to be cancelled
     */
    fun finishAccountCancellation(accountPassword: String) {
        viewModelScope.launch {
            confirmationLink?.let { link ->
                runCatching {
                    confirmCancelAccountUseCase(
                        cancellationLink = link,
                        accountPassword = accountPassword,
                    )
                }.onSuccess {
                    Timber.d("Successfully cancelled the Account")
                }.onFailure { exception ->
                    Timber.e("An issue occurred when cancelling the Account:\n${exception}")
                    _state.update {
                        it.copy(
                            errorMessageRes = if (exception is ConfirmCancelAccountException.IncorrectPassword) {
                                R.string.old_password_provided_incorrect
                            } else {
                                R.string.general_text_error
                            },
                        )
                    }
                }
            }
        }
    }

    /**
     * Resets the value of [MyAccountUiState.errorMessage]
     */
    fun resetErrorMessage() {
        _state.update { it.copy(errorMessage = "") }
    }

    /**
     * Resets the value of [MyAccountUiState.errorMessageRes]
     */
    fun resetErrorMessageRes() {
        _state.update { it.copy(errorMessageRes = null) }
    }

    /**
     * Begins the process of changing the User's Email Address
     *
     * @param changeEmailLink the Change Email Link
     */
    fun beginChangeEmailProcess(changeEmailLink: String) {
        viewModelScope.launch {
            runCatching {
                queryChangeEmailLinkUseCase(changeEmailLink)
            }.onSuccess { newChangeEmailLink ->
                Timber.d("Successfully queried the Change Email Link")
                confirmationLink = newChangeEmailLink
                checkChangeEmailLinkValidity()
            }.onFailure { exception ->
                Timber.e("An issue occurred when querying the Change Email Link", exception)
                if (exception is QueryChangeEmailLinkException.LinkNotGenerated) {
                    _state.update { it.copy(showInvalidChangeEmailLinkPrompt = true) }
                } else {
                    _state.update { it.copy(errorMessageRes = R.string.general_error_word) }
                }
            }
        }
    }

    /**
     * Once the Change Email Link has been successfully queried and no issues were found,
     * check if the Change Email Link matches the specified Regex
     */
    private fun checkChangeEmailLinkValidity() {
        runCatching {
            isUrlMatchesRegexUseCase(
                url = confirmationLink,
                patterns = Constants.VERIFY_CHANGE_MAIL_LINK_REGEXS,
            )
        }.onSuccess { isMatching ->
            Timber.d(
                "Successfully checked if the URL matches the Change Email Link Regex\n" +
                        "Is Change Email Link Valid: $isMatching"
            )
            if (isMatching) {
                _state.update { it.copy(showChangeEmailConfirmation = true) }
            } else {
                _state.update { it.copy(errorMessageRes = R.string.general_error_word) }
            }
        }.onFailure { exception ->
            Timber.e(
                "An issue occurred when checking if the URL matches the Change Email Link Regex",
                exception
            )
            _state.update { it.copy(errorMessageRes = R.string.general_error_word) }
        }
    }

    /**
     * Change the specific State Parameter to hide the Change Email Confirmation
     */
    fun resetChangeEmailConfirmation() {
        _state.update { it.copy(showChangeEmailConfirmation = false) }
    }

    /**
     * Change the specific State Parameter to hide the Invalid Change Email Link Prompt
     */
    fun resetInvalidChangeEmailLinkPrompt() {
        _state.update { it.copy(showInvalidChangeEmailLinkPrompt = false) }
    }

    /**
     * Finishes the process of changing the User's Email Address
     *
     * @param accountPassword The password of the Account whose email to be changed
     */
    fun finishChangeEmailConfirmation(accountPassword: String) {
        viewModelScope.launch {
            confirmationLink?.let { link ->
                runCatching {
                    confirmChangeEmailUseCase(
                        changeEmailLink = link,
                        accountPassword = accountPassword,
                    )
                }.onSuccess { newEmail ->
                    if (Patterns.EMAIL_ADDRESS.matcher(newEmail).find()) {
                        Timber.d("Successfully changed the email address associated to the Account")
                        snackBarHandler.postSnackbarMessage(
                            resId = R.string.email_changed, newEmail,
                            snackbarDuration = MegaSnackbarDuration.Long,
                        )
                    } else {
                        Timber.e("The new email address does not match the email address pattern")
                        _state.update { it.copy(errorMessage = newEmail) }
                    }
                }.onFailure { exception ->
                    Timber.e("An issue occurred when changing the email address:\n${exception}")
                    _state.update {
                        it.copy(
                            errorMessageRes = when (exception) {
                                is ConfirmChangeEmailException.EmailAlreadyInUse -> R.string.mail_already_used
                                is ConfirmChangeEmailException.IncorrectPassword -> R.string.old_password_provided_incorrect
                                else -> R.string.general_text_error
                            }
                        )
                    }
                }
            }
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
            runCatching {
                val accountDetails = getAccountDetailsUseCase(
                    forceRefresh = myAccountInfo.usedFormatted.trim().isEmpty()
                )
                getExtendedAccountDetail(
                    forceRefresh = false,
                    sessions = true,
                    purchases = false,
                    transactions = false
                )
                getPaymentMethodUseCase(false)

                val businessProFlexiStatus = getBusinessStatusUseCase()

                _state.update { state ->
                    state.copy(
                        isBusinessAccount = accountDetails.isBusinessAccount &&
                                accountDetails.accountTypeIdentifier == AccountType.BUSINESS,
                        isProFlexiAccount = accountDetails.isBusinessAccount && accountDetails.accountTypeIdentifier == AccountType.PRO_FLEXI,
                        businessProFlexiStatus = businessProFlexiStatus,
                        isStandardProAccount = accountDetails.accountTypeIdentifier?.let {
                            isStandardProAccountCheck(it)
                        } ?: false
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
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

    /**
     * Logout
     *
     * logs out the user from mega application and navigates to login activity
     * logic is handled at [MegaChatRequestHandler] onRequestFinished callback
     */
    private fun logout() = viewModelScope.launch {
        runCatching {
            logoutUseCase()
        }.onFailure {
            Timber.d("Error on logout $it")
        }
    }

    /**
     * Reset shouldNavigateToSmsVerification state
     */
    fun onNavigatedToSmsVerification() {
        _state.update {
            it.copy(shouldNavigateToSmsVerification = false)
        }
    }

    /**
     * Get account status for Business or Pro Flexi accounts
     */
    fun getBusinessProFlexiStatus(): BusinessAccountStatus? =
        state.value.businessProFlexiStatus

    private fun isStandardProAccountCheck(accountType: AccountType): Boolean = when (accountType) {
        AccountType.PRO_I -> true
        AccountType.PRO_II -> true
        AccountType.PRO_III -> true
        AccountType.PRO_LITE -> true
        AccountType.PRO_FLEXI -> true
        else -> false
    }

    /**
     * Check if account has standard Pro subscription (Pro Lite, Pro I, Pro II, Pro III or Pro Flexi)
     */
    fun isStandardProAccount(): Boolean =
        state.value.isStandardProAccount
}
