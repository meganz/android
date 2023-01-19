package mega.privacy.android.app.myAccount

import android.Manifest
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
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.constants.EventConstants.EVENT_REFRESH
import mega.privacy.android.app.generalusecase.FilePrepareUseCase
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.main.TestPasswordActivity
import mega.privacy.android.app.main.VerifyTwoFactorActivity
import mega.privacy.android.app.main.controllers.AccountController
import mega.privacy.android.app.main.qrcode.QRCodeActivity
import mega.privacy.android.app.middlelayer.iab.BillingConstant
import mega.privacy.android.app.myAccount.usecase.CancelSubscriptionsUseCase
import mega.privacy.android.app.myAccount.usecase.Check2FAUseCase
import mega.privacy.android.app.myAccount.usecase.CheckPasswordReminderUseCase
import mega.privacy.android.app.myAccount.usecase.CheckVersionsUseCase
import mega.privacy.android.app.myAccount.usecase.ConfirmCancelAccountUseCase
import mega.privacy.android.app.myAccount.usecase.ConfirmChangeEmailUseCase
import mega.privacy.android.app.myAccount.usecase.GetMyAvatarUseCase
import mega.privacy.android.app.myAccount.usecase.GetUserDataUseCase
import mega.privacy.android.app.myAccount.usecase.KillSessionUseCase
import mega.privacy.android.app.myAccount.usecase.QueryRecoveryLinkUseCase
import mega.privacy.android.app.myAccount.usecase.SetAvatarUseCase
import mega.privacy.android.app.myAccount.usecase.UpdateMyUserAttributesUseCase
import mega.privacy.android.app.smsVerification.usecase.ResetPhoneNumberUseCase
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
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions
import mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.usecase.GetAccountDetails
import mega.privacy.android.domain.usecase.GetExtendedAccountDetail
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.GetPaymentMethod
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaError.API_EARGS
import nz.mega.sdk.MegaError.API_OK
import nz.mega.sdk.MegaUtilsAndroid
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class MyAccountViewModel @Inject constructor(
    private val myAccountInfo: MyAccountInfo,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val setAvatarUseCase: SetAvatarUseCase,
    private val updateMyUserAttributesUseCase: UpdateMyUserAttributesUseCase,
    private val check2FAUseCase: Check2FAUseCase,
    private val checkVersionsUseCase: CheckVersionsUseCase,
    private val killSessionUseCase: KillSessionUseCase,
    private val cancelSubscriptionsUseCase: CancelSubscriptionsUseCase,
    private val getMyAvatarUseCase: GetMyAvatarUseCase,
    private val checkPasswordReminderUseCase: CheckPasswordReminderUseCase,
    private val resetPhoneNumberUseCase: ResetPhoneNumberUseCase,
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
) : BaseRxViewModel() {

    companion object {
        private const val CLICKS_TO_CHANGE_API_SERVER = 5
        private const val TIME_TO_SHOW_PAYMENT_INFO = 604800 //1 week in seconds
        const val PROCESSING_FILE = "PROCESSING_FILE"
        const val CHECKING_2FA = "CHECKING_2FA"
    }

    private val withElevation: MutableLiveData<Boolean> = MutableLiveData()
    private val updateAccountDetails: MutableLiveData<Boolean> = MutableLiveData()
    private val processingFile: MutableLiveData<Boolean> = MutableLiveData()

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

    /**
     * Number of subscription
     */
    val numberOfSubscription = _numberOfSubscription.asStateFlow()

    private val _state = MutableStateFlow(MyAccountUiState())
    val state = _state.asStateFlow()

    /**
     * On my avatar file changed flow
     */
    val onMyAvatarFileChanged: Flow<File?>
        get() = monitorMyAvatarFile()

    init {
        refreshNumberOfSubscription(false)
    }

    /**
     * Refresh number of subscription
     *
     */
    fun refreshNumberOfSubscription(clearCache: Boolean) {
        viewModelScope.launch {
            _numberOfSubscription.value =
                runCatching { getNumberOfSubscription(clearCache) }
                    .getOrDefault(INVALID_VALUE.toLong())
        }
    }

    /**
     * Check the subscription for current account
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
     * Restore the SubscriptionDialogState value
     */
    fun restoreSubscriptionDialogState() {
        _subscriptionDialogState.value = defaultSubscriptionDialogState
    }

    /**
     * Restore the CancelAccountDialogState value
     */
    fun restoreCancelAccountDialogState() {
        _cancelAccountDialogState.value = defaultCancelAccountDialogState
    }

    /**
     * Set cancel account dialog state
     * @param isVisible the dialog if is visible
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

    fun checkElevation(): LiveData<Boolean> = withElevation
    fun onUpdateAccountDetails(): LiveData<Boolean> = updateAccountDetails
    fun isProcessingFile(): LiveData<Boolean> = processingFile

    fun setElevation(withElevation: Boolean) {
        this.withElevation.value = withElevation
    }

    private fun setVersionsInfo() {
        _state.update {
            it.copy(versionsInfo = myAccountInfo.getFormattedPreviousVersionsSize())
        }
    }

    fun updateAccountDetails() {
        updateAccountDetails.value = true
    }

    private var is2FaEnabled = false

    private var numOfClicksLastSession = 0

    private lateinit var snackbarShower: SnackbarShower

    private var confirmationLink: String? = null

    fun getFirstName(): String = myAccountInfo.getFirstNameText()

    fun getLastName(): String = myAccountInfo.getLastNameText()

    fun getName(): String = myAccountInfo.fullName

    fun getEmail(): String? = megaApi.myEmail

    fun getAccountType(): Int = myAccountInfo.accountType

    fun isFreeAccount(): Boolean = getAccountType() == FREE

    fun isProFlexiAccount(): Boolean = getAccountType() == PRO_FLEXI

    fun getUsedStorage(): String = myAccountInfo.usedFormatted

    fun getUsedStoragePercentage(): Int = myAccountInfo.usedPercentage

    fun getTotalStorage(): String = myAccountInfo.totalFormatted

    fun getUsedTransfer(): String = myAccountInfo.usedTransferFormatted

    fun getUsedTransferPercentage(): Int = myAccountInfo.usedTransferPercentage

    fun getTotalTransfer(): String = myAccountInfo.totalTransferFormatted

    fun getRenewTime(): Long = myAccountInfo.subscriptionRenewTime

    fun getBonusStorageSMS(): String = myAccountInfo.bonusStorageSMS

    fun hasRenewableSubscription(): Boolean {
        return myAccountInfo.subscriptionStatus == MegaAccountDetails.SUBSCRIPTION_STATUS_VALID
                && myAccountInfo.subscriptionRenewTime > 0
    }

    fun getExpirationTime(): Long = myAccountInfo.proExpirationTime

    fun hasExpirableSubscription(): Boolean = myAccountInfo.proExpirationTime > 0

    fun getLastSession(): String = myAccountInfo.lastSessionFormattedDate ?: ""

    fun thereIsNoSubscription(): Boolean = _numberOfSubscription.value <= 0L

    fun getRegisteredPhoneNumber(): String? = megaApi.smsVerifiedPhoneNumber()

    fun isAlreadyRegisteredPhoneNumber(): Boolean = !getRegisteredPhoneNumber().isNullOrEmpty()

    fun getCloudStorage(): String = myAccountInfo.formattedUsedCloud

    fun getBackupsStorage(): String = myAccountInfo.formattedUsedBackups

    fun getIncomingStorage(): String = myAccountInfo.formattedUsedIncoming

    fun getRubbishStorage(): String = myAccountInfo.formattedUsedRubbish

    fun isBusinessAccount(): Boolean = megaApi.isBusinessAccount && getAccountType() == BUSINESS

    fun getMasterKey(): String = megaApi.exportMasterKey()

    /**
     * Checks versions info.
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
     * Gets the avatar of the current account.
     *
     * @param context Current context.
     * @param action  Action to perform after the avatar has been get.
     */
    fun getAvatar(context: Context, action: (Boolean) -> Unit) {
        CacheFolderManager.buildAvatarFile(context, megaApi.myEmail)?.absolutePath?.let {
            getMyAvatarUseCase.get(it)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { action.invoke(true) },
                    onError = { error ->
                        Timber.w(error)
                        action.invoke(false)
                    }
                )
                .addTo(composite)
        }
    }

    /**
     * Kills other sessions.
     *
     * @param action Action to perform after kill sessions.
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
     * Launches [LoginActivity] to perform an account refresh.
     *
     * @param activity Current activity.
     */
    fun refresh(activity: Activity) {
        val intent = Intent(activity, LoginActivity::class.java)
        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
        intent.action = ACTION_REFRESH

        activity.startActivityForResult(intent, REQUEST_CODE_REFRESH)
    }

    /**
     * Manages onActivityResult.
     *
     * @param activity       Current activity.
     * @param requestCode    The integer request code originally supplied to
     *                       startActivityForResult(), allowing you to identify who this
     *                       result came from.
     * @param resultCode     The integer result code returned by the child activity
     *                       through its setResult().
     * @param data           An Intent, which can return result data to the caller
     *                       (various data can be attached to Intent "extras").
     * @param snackbarShower Callback to show the action result if needed.
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
                    LiveEventBus.get(EVENT_REFRESH).post(true)
                }
            }
            TAKE_PICTURE_PROFILE_CODE -> addProfileAvatar(null)
            CHOOSE_PICTURE_PROFILE_CODE -> {
                if (data == null) {
                    showResult(getString(R.string.error_changing_user_avatar_image_not_available))
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
                    showResult(getString(R.string.error_changing_user_avatar_image_not_available))
                    return
                }

                data.action = Intent.ACTION_GET_CONTENT
                processingFile.value = true
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
     * Increments the number of clicks before show the change API server dialog.
     *
     * @return True if already clicked CLICKS_TO_CHANGE_API_SERVER times, false otherwise.
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
     * Checks if should show payment info.
     *
     * @return True if should show payment info, false otherwise.
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
     * Cancel current subscriptions.
     *
     * @param feedback Message to send as feedback to cancel subscriptions.
     * @param action   Action to perform after cancel subscriptions.
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
     * Checks if should show Password reminder before logout and logs out if not.
     *
     * @param context Current context.
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
     * Checks if all permissions are granted before capture a photo.
     *
     * @param activity Current activity.
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
     * Launches an intent to choose a photo.
     *
     * @param activity Current activity.
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
     * Checks if can open QRCodeActivity and if so, launches an intent to do it.
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
        val myEmail = megaApi.myUser.email
        val imgFile = if (!path.isNullOrEmpty()) File(path)
        else CacheFolderManager.getCacheFile(
            app,
            CacheFolderManager.TEMPORARY_FOLDER,
            "picture.jpg"
        )

        if (!FileUtil.isFileAvailable(imgFile)) {
            showResult(getString(R.string.general_error))
            return
        }

        val newFile = CacheFolderManager.buildAvatarFile(app, myEmail + "Temp.jpg")

        if (newFile != null) {
            MegaUtilsAndroid.createAvatar(imgFile, newFile)
            setAvatarUseCase.set(newFile.absolutePath)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = {
                        processingFile.value = false
                        showResult(getString(R.string.success_changing_user_avatar))
                    },
                    onError = { showResult(getString(R.string.error_changing_user_avatar)) }
                )
                .addTo(composite)
        } else {
            Timber.e("Destination PATH is NULL")
        }
    }

    /**
     * Deletes the current avatar.
     *
     * @param context        Current context.
     * @param snackbarShower Callback to show the request result.
     */
    fun deleteProfileAvatar(context: Context, snackbarShower: SnackbarShower) {
        CacheFolderManager.buildAvatarFile(context, megaApi.myEmail + JPG_EXTENSION)?.let {
            if (FileUtil.isFileAvailable(it)) {
                Timber.d("Avatar to delete: ${it.absolutePath}")
                it.delete()
            }
        }

        setAvatarUseCase.remove()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { snackbarShower.showSnackbar(getString(R.string.success_deleting_user_avatar)) },
                onError = { snackbarShower.showSnackbar(getString(R.string.error_deleting_user_avatar)) }
            )
            .addTo(composite)
    }

    /**
     * Changes the name of the account.
     *
     * @param newFirstName New first name if changed, same as current one if not.
     * @param newLastName  New last name if changed, same as current one if not.
     * @param action       Action to perform after change name.
     * @return True if something changed, false otherwise.
     */
    fun changeName(newFirstName: String, newLastName: String, action: (Boolean) -> Unit): Boolean {
        val shouldUpdateLastName = newLastName != myAccountInfo.getLastNameText()

        return when {
            newFirstName != myAccountInfo.getFirstNameText() -> {
                if (shouldUpdateLastName) {
                    updateMyUserAttributesUseCase.updateFirstAndLastName(newFirstName, newLastName)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy { result -> action.invoke(result) }
                        .addTo(composite)
                } else {
                    updateMyUserAttributesUseCase.updateFirstName(newFirstName)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy { result -> action.invoke(result) }
                        .addTo(composite)
                }

                true
            }
            shouldUpdateLastName -> {
                updateMyUserAttributesUseCase.updateLastName(newLastName)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy { result -> action.invoke(result) }
                    .addTo(composite)

                true
            }
            else -> false
        }
    }

    /**
     * Checks if 2FA is enabled or not.
     */
    fun check2FA() {
        check2FAUseCase.check()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { result -> is2FaEnabled = result }
            .addTo(composite)
    }

    /**
     * Changes the email of the account.
     *
     * @param context  Current context.
     * @param newEmail New email if changed, same as the current one if not.
     * @param action   Action to perform after change the email.
     * @return An error string if something wrong happened, CHECKING_2FA if checking 2FA or null otherwise
     */
    fun changeEmail(context: Context, newEmail: String, action: (MegaError) -> Unit): String? {
        return when {
            newEmail == getEmail() -> getString(R.string.mail_same_as_old)
            !EMAIL_ADDRESS.matcher(newEmail).matches() -> getString(R.string.error_invalid_email)
            is2FaEnabled -> {
                context.startActivity(
                    Intent(context, VerifyTwoFactorActivity::class.java)
                        .putExtra(VerifyTwoFactorActivity.KEY_VERIFY_TYPE, CHANGE_MAIL_2FA)
                        .putExtra(VerifyTwoFactorActivity.KEY_NEW_EMAIL, newEmail)
                )

                CHECKING_2FA
            }
            else -> {
                updateMyUserAttributesUseCase.updateEmail(newEmail)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy { result -> action.invoke(result) }
                    .addTo(composite)

                null
            }
        }
    }

    /**
     * Resets the verified phone number of the current account.
     *
     * @param isModify       True if the action is modify, false if is remove.
     * @param snackbarShower Callback to show the request result if needed.
     * @param action         Action to perform after reset the phone number if modifying.
     */
    fun resetPhoneNumber(isModify: Boolean, snackbarShower: SnackbarShower, action: () -> Unit) {
        resetPhoneNumberUseCase.reset()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { getUserData(isModify, snackbarShower, action) },
                onError = { error ->
                    Timber.w("Reset phone number failed: ${error.message}")
                    snackbarShower.showSnackbar(getString(R.string.remove_phone_number_fail))
                })
            .addTo(composite)
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
                    else snackbarShower.showSnackbar(getString(R.string.remove_phone_number_success))
                },
                onError = { error ->
                    Timber.w("Reset phone number failed: ${error.message}")
                    snackbarShower.showSnackbar(getString(R.string.remove_phone_number_fail))
                })
            .addTo(composite)
    }

    /**
     * Gets file versions option.
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
     * Queries if the provided account cancellation link is the right one before cancel it.
     *
     * @param link   Confirmation link for the account cancellation.
     * @param action Action to perform after query confirmation link.
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
     * Confirms the account cancellation.
     *
     * @param password Password typed to cancel the account.
     * @param action   Action to perform after confirm the account cancellation if it failed.
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
     * Queries if the provided email change link is the right one before change it.
     *
     * @param link   Confirmation link for the email change.
     * @param action Action to perform after query confirmation link.
     */
    fun confirmChangeEmail(link: String, action: (String) -> Unit) {
        queryRecoveryLinkUseCase.queryChangeEmail(link)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { result ->
                confirmationLink = result
                action.invoke(result)
            }
            .addTo(composite)
    }

    /**
     * Confirms the email change.
     *
     * @param password      Password typed to change the email.
     * @param actionSuccess Action to perform after confirm the email change finished with success.
     * @param actionError   Action to perform after confirm the email change if it failed.
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
     * Checks the result of a password change.
     *
     * @param result        Result of the password change.
     * @param actionSuccess Action to perform after confirm the password change finished with success.
     * @param actionError   Action to perform after confirm the password change if it failed.
     */
    fun finishPasswordChange(
        result: Int,
        actionSuccess: (String) -> Unit,
        actionError: (String) -> Unit,
    ) {
        when (result) {
            API_OK -> actionSuccess.invoke(getString(R.string.pass_changed_alert))
            API_EARGS -> actionError.invoke(getString(R.string.old_password_provided_incorrect))
            else -> actionError.invoke(getString(R.string.general_text_error))
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
     * Sets the Upgrade screen has been opened from My account section.
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
            getExtendedAccountDetail(forceRefresh = false,
                sessions = true,
                purchases = false,
                transactions = false)
            getPaymentMethod(false)
        }
    }
}