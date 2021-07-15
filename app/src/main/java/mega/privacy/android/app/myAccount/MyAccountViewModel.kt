package mega.privacy.android.app.myAccount

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jeremyliao.liveeventbus.LiveEventBus
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.constants.EventConstants.EVENT_REFRESH
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.exportRK.ExportRecoveryKeyActivity
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.lollipop.ChangePasswordActivityLollipop
import mega.privacy.android.app.lollipop.LoginActivityLollipop
import mega.privacy.android.app.lollipop.TestPasswordActivity
import mega.privacy.android.app.lollipop.VerifyTwoFactorActivity
import mega.privacy.android.app.lollipop.controllers.AccountController
import mega.privacy.android.app.lollipop.qrcode.QRCodeActivity
import mega.privacy.android.app.lollipop.tasks.FilePrepareTask
import mega.privacy.android.app.myAccount.usecase.*
import mega.privacy.android.app.smsVerification.usecase.ResetPhoneNumberUseCase
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.LogUtil.*
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class MyAccountViewModel @ViewModelInject constructor(
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
    private val getFileVersionsOptionUseCase: GetFileVersionsOptionUseCase
) : BaseRxViewModel(), FilePrepareTask.ProcessedFilesCallback {

    companion object {
        private const val CLICKS_TO_CHANGE_API_SERVER = 5
        private const val TIME_TO_SHOW_PAYMENT_INFO = 604800 //1 week in seconds
        const val PROCESSING_FILE = "PROCESSING_FILE"
        const val CHECKING_2FA = "CHECKING_2FA"
    }

    private val withElevation: MutableLiveData<Boolean> = MutableLiveData()
    private val versionsInfo: MutableLiveData<String> = MutableLiveData()
    private val updateAccountDetails: MutableLiveData<Boolean> = MutableLiveData()

    fun checkElevation(): LiveData<Boolean> = withElevation
    fun getVersionsInfo(): LiveData<String> = versionsInfo
    fun onUpdateAccountDetails(): LiveData<Boolean> = updateAccountDetails

    fun setElevation(withElevation: Boolean) {
        this.withElevation.value = withElevation
    }

    private fun setVersionsInfo() {
        versionsInfo.value = myAccountInfo.getFormattedPreviousVersionsSize()
    }

    fun updateAccountDetails() {
        updateAccountDetails.value = true
    }

    private var is2FaEnabled = false

    private var numOfClicksLastSession = 0

    private var setAvatarAction: ((Pair<Boolean, Boolean>) -> Unit)? = null

    fun getFirstName(): String = myAccountInfo.getFirstNameText()

    fun getLastName(): String = myAccountInfo.getLastNameText()

    fun getName(): String = myAccountInfo.fullName

    fun getEmail(): String = megaApi.myEmail

    fun getAccountType(): Int = myAccountInfo.accountType

    fun isFreeAccount(): Boolean = getAccountType() == FREE

    fun getUsedStorage(): String = myAccountInfo.usedFormatted

    fun getUsedStoragePercentage(): Int = myAccountInfo.usedPercentage

    fun getTotalStorage(): String = myAccountInfo.totalFormatted

    fun getUsedTransfer(): String = myAccountInfo.usedTransferFormatted

    fun getUsedTransferPercentage(): Int = myAccountInfo.usedTransferPercentage

    fun getTotalTransfer(): String = myAccountInfo.totalTransferFormatted

    fun getRenewTime(): Long = myAccountInfo.subscriptionRenewTime

    fun hasRenewableSubscription(): Boolean {
        return myAccountInfo.subscriptionStatus == MegaAccountDetails.SUBSCRIPTION_STATUS_VALID
                && myAccountInfo.subscriptionRenewTime > 0
    }

    fun getExpirationTime(): Long = myAccountInfo.proExpirationTime

    fun hasExpirableSubscription(): Boolean = myAccountInfo.proExpirationTime > 0

    fun getLastSession(): String = myAccountInfo.lastSessionFormattedDate ?: ""

    fun thereIsNoSubscription(): Boolean = myAccountInfo.numberOfSubscriptions <= 0

    fun getRegisteredPhoneNumber(): String? = megaApi.smsVerifiedPhoneNumber()

    fun isAlreadyRegisteredPhoneNumber(): Boolean = !getRegisteredPhoneNumber().isNullOrEmpty()

    fun getCloudStorage(): String = myAccountInfo.formattedUsedCloud

    fun getInboxStorage(): String = myAccountInfo.formattedUsedInbox

    fun getIncomingStorage(): String = myAccountInfo.formattedUsedIncoming

    fun getRubbishStorage(): String = myAccountInfo.formattedUsedRubbish

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
                    onError = { error -> logWarning(error.message) }
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
        getMyAvatarUseCase.get(buildAvatarFile(context, megaApi.myEmail).absolutePath)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { action.invoke(true) },
                onError = { error ->
                    logWarning(error.message)
                    action.invoke(false)
                }
            )
            .addTo(composite)
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
                    logWarning("Error when killing sessions: ${error.message}")
                    action.invoke(false)
                }
            )
            .addTo(composite)
    }

    /**
     * Launches the ChangePasswordActivityLollipop activity.
     *
     * @param context Current context.
     */
    fun changePassword(context: Context) {
        context.startActivity(Intent(context, ChangePasswordActivityLollipop::class.java))
    }

    /**
     * Launches the ExportRecoveryKeyActivity activity.
     *
     * @param context Current context.
     */
    fun exportMK(context: Context) {
        context.startActivity(Intent(context, ExportRecoveryKeyActivity::class.java))
    }

    /**
     * Launches the LoginActivityLollipop activity to perform an account refresh.
     *
     * @param activity Current activity.
     */
    fun refresh(activity: Activity) {
        val intent = Intent(activity, LoginActivityLollipop::class.java)
        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
        intent.action = ACTION_REFRESH

        activity.startActivityForResult(intent, REQUEST_CODE_REFRESH)
    }

    /**
     * Manages onActivityResult.
     *
     * @param activity    Current activity.
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     */
    fun manageActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        setAvatarAction: ((Pair<Boolean, Boolean>) -> Unit)? = null
    ): String? {
        if (resultCode != RESULT_OK) {
            logWarning("Result code not OK. Request code $requestCode")
            return null
        }

        this.setAvatarAction = setAvatarAction

        when (requestCode) {
            REQUEST_CODE_REFRESH -> {
                val app = MegaApplication.getInstance()

                app.askForAccountDetails()
                app.askForExtendedAccountDetails()
                LiveEventBus.get(EVENT_REFRESH).post(true)
            }
            TAKE_PICTURE_PROFILE_CODE -> {
                return addProfileAvatar(null)
            }
            CHOOSE_PICTURE_PROFILE_CODE -> {
                if (data == null) {
                    return getString(R.string.error_changing_user_avatar_image_not_available)
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
                    return getString(R.string.error_changing_user_avatar_image_not_available)
                }

                data.action = Intent.ACTION_GET_CONTENT
                FilePrepareTask(this).execute(data)
                return PROCESSING_FILE
            }
        }

        return null
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

        return megaApi.isBusinessAccount && megaApi.isMasterBusinessAccount
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
                    logWarning("Error when killing sessions: ${error.message}")
                    action.invoke(false)
                }
            )
            .addTo(composite)
    }

    /**
     * Launches the UpgradeAccountActivity activity.
     *
     * @param context Current context.
     */
    fun upgradeAccount(context: Context) {
        context.startActivity(Intent(context, UpgradeAccountActivity::class.java))
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
                    } else AccountController.logout(context, megaApi)
                },
                onError = { error ->
                    logError("Error when killing sessions: ${error.message}")
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasStoragePermission: Boolean = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val hasCameraPermission: Boolean = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasStoragePermission && !hasCameraPermission) {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                    ),
                    REQUEST_WRITE_STORAGE
                )

                return
            } else if (!hasStoragePermission) {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_STORAGE
                )

                return
            } else if (!hasCameraPermission) {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(Manifest.permission.CAMERA),
                    REQUEST_CAMERA
                )

                return
            }
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
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "image/*";
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
     * @param path Path of the chosen photo or null if is a new taken photo.
     * @return An error message if something was wrong, null otherwise.
     */
    private fun addProfileAvatar(path: String?): String? {
        val app = MegaApplication.getInstance()
        val myEmail = megaApi.myUser.email
        val imgFile = if (!path.isNullOrEmpty()) File(path)
        else CacheFolderManager.getCacheFile(
            app,
            CacheFolderManager.TEMPORAL_FOLDER,
            "picture.jpg"
        )

        if (!FileUtil.isFileAvailable(imgFile)) {
            return getString(R.string.general_error)
        }

        val newFile = buildAvatarFile(app, myEmail + "Temp.jpg")

        if (newFile != null) {
            MegaUtilsAndroid.createAvatar(imgFile, newFile)
            setAvatarUseCase.set(newFile.absolutePath)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { result -> setAvatarAction?.invoke(result) }
                .addTo(composite)
        } else {
            logError("ERROR! Destination PATH is NULL")
        }

        return null
    }

    /**
     * Deletes the current avatar.
     *
     * @param context Current context.
     * @param action  Action to perform after delete the avatar.
     */
    fun deleteProfileAvatar(context: Context, action: (Pair<Boolean, Boolean>) -> Unit) {
        val avatar = buildAvatarFile(context, megaApi.myEmail + JPG_EXTENSION)

        if (FileUtil.isFileAvailable(avatar)) {
            logDebug("Avatar to delete: " + avatar.absolutePath)
            avatar.delete()
        }

        setAvatarUseCase.remove()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { result -> action.invoke(result) }
            .addTo(composite)
    }

    override fun onIntentProcessed(info: MutableList<ShareInfo>) {
        addProfileAvatar(info[0].fileAbsolutePath)
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
     * @param action Action to perform after reset the phone number.
     */
    fun resetPhoneNumber(action: (Boolean) -> Unit) {
        resetPhoneNumberUseCase.reset()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { getUserData(action) },
                onError = { error ->
                    logWarning("Reset phone number failed: ${error.message}")
                    action.invoke(false)
                })
            .addTo(composite)
    }

    /**
     * Gets the current account user data.
     *
     * @param action Action to perform after reset the phone number.
     */
    private fun getUserData(action: (Boolean) -> Unit) {
        getUserDataUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { success -> action.invoke(success) }
            .addTo(composite)
    }

    /**
     * Gets file versions option.
     */
    fun getFileVersionsOption() {
        getFileVersionsOptionUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { setVersionsInfo() },
                onError = { error -> logWarning(error.message) })
            .addTo(composite)
    }
}