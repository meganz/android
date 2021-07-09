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
import mega.privacy.android.app.exportMK.ExportRecoveryKeyActivity
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.listeners.ShouldShowPasswordReminderDialogListener
import mega.privacy.android.app.lollipop.ChangePasswordActivityLollipop
import mega.privacy.android.app.lollipop.LoginActivityLollipop
import mega.privacy.android.app.lollipop.qrcode.QRCodeActivity
import mega.privacy.android.app.myAccount.usecase.SetAvatarUseCase
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.app.utils.LogUtil.*
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class MyAccountViewModel @ViewModelInject constructor(
    private val myAccountInfo: MyAccountInfo,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val setAvatarUseCase: SetAvatarUseCase
) : BaseRxViewModel() {

    companion object {
        private const val CLICKS_TO_STAGING = 5
        private const val TIME_TO_SHOW_PAYMENT_INFO = 604800 //1 week in seconds
    }

    private val versionsInfo: MutableLiveData<MegaError> = MutableLiveData()
    private val avatar: MutableLiveData<MegaError> = MutableLiveData()
    private val killSessions: MutableLiveData<MegaError> = MutableLiveData()
    private val cancelSubscriptions: MutableLiveData<MegaError> = MutableLiveData()
    private val setProfileAvatar: MutableLiveData<Pair<MegaRequest, MegaError>> = MutableLiveData()

    private var fragment = MY_ACCOUNT_FRAGMENT
    private var numOfClicksLastSession = 0

    fun onUpdateVersionsInfoFinished(): LiveData<MegaError> = versionsInfo
    fun onGetAvatarFinished(): LiveData<MegaError> = avatar
    fun onKillSessionsFinished(): LiveData<MegaError> = killSessions
    fun onCancelSubscriptions(): LiveData<MegaError> = cancelSubscriptions
    fun onSetProfileAvatar(): LiveData<Pair<MegaRequest, MegaError>> = setProfileAvatar

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

    fun setFragment(fragment: Int) {
        this.fragment = fragment
    }

    fun isMyAccountFragment(): Boolean = fragment == MY_ACCOUNT_FRAGMENT

    fun thereIsNoSubscription(): Boolean = myAccountInfo.numberOfSubscriptions <= 0

    fun getRegisteredPhoneNumber(): String? = megaApi.smsVerifiedPhoneNumber()

    fun isAlreadyRegisteredPhoneNumber(): Boolean = !getRegisteredPhoneNumber().isNullOrEmpty()

    fun checkVersions() {
        if (myAccountInfo.numVersions == -1) {
            megaApi.getFolderInfo(megaApi.rootNode, OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        val info: MegaFolderInfo = request.megaFolderInfo

                        myAccountInfo.numVersions = info.numVersions
                        myAccountInfo.previousVersionsSize = info.versionsSize
                    } else {
                        logError("Error refreshing info: " + error.errorString)
                    }

                    versionsInfo.value = error
                }
            ))
        }
    }

    fun getAvatar(context: Context) {
        megaApi.getUserAvatar(megaApi.myUser,
            buildAvatarFile(context, megaApi.myEmail).absolutePath,
            OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (error.errorCode == MegaError.API_EARGS) {
                        logError("Error getting avatar: " + error.errorString)
                    } else {
                        avatar.value = error
                    }
                }
            ))
    }

    fun killSessions() {
        megaApi.killSession(INVALID_HANDLE, OptionalMegaRequestListenerInterface(
            onRequestFinish = { _, error ->
                if (error.errorCode == MegaError.API_OK) {
                    logDebug("Success kill sessions")
                } else {
                    logError("Error when killing sessions: " + error.errorString)
                }

                killSessions.value = error
            }
        ))
    }

    fun changePassword(context: Context) {
        context.startActivity(Intent(context, ChangePasswordActivityLollipop::class.java))
    }

    fun exportMK(context: Context) {
        context.startActivity(Intent(context, ExportRecoveryKeyActivity::class.java))
    }

    fun refresh(activity: Activity) {
        val intent = Intent(activity, LoginActivityLollipop::class.java)
        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
        intent.action = ACTION_REFRESH

        activity.startActivityForResult(intent, REQUEST_CODE_REFRESH)
    }

    fun manageActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): String? {
        if (resultCode != RESULT_OK) {
            logWarning("Result code not OK. Request code $requestCode")
            return null
        }

        when (requestCode) {
            REQUEST_CODE_REFRESH -> {
                val app = MegaApplication.getInstance()

                app.askForAccountDetails()
                app.askForExtendedAccountDetails()
                LiveEventBus.get(EVENT_REFRESH).post(true)
            }
            TAKE_PICTURE_PROFILE_CODE -> {
                return addProfileAvatar(activity, null)
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

                val path = ShareInfo.getRealPathFromURI(activity, data.data)
                return addProfileAvatar(activity, path)
            }
        }

        return null
    }

    fun incrementLastSessionClick(): Boolean {
        numOfClicksLastSession++

        if (numOfClicksLastSession < CLICKS_TO_STAGING)
            return false

        numOfClicksLastSession = 0
        return true
    }

    private fun isBusinessPaymentAttentionNeeded(): Boolean {
        val status = megaApi.businessStatus

        return megaApi.isBusinessAccount && megaApi.isMasterBusinessAccount
                && (status == MegaApiJava.BUSINESS_STATUS_EXPIRED
                || status == MegaApiJava.BUSINESS_STATUS_GRACE_PERIOD)
    }

    fun shouldShowPaymentInfo(): Boolean {
        val timeToCheck =
            if (hasRenewableSubscription()) myAccountInfo.subscriptionRenewTime
            else myAccountInfo.proExpirationTime

        val currentTime = System.currentTimeMillis() / 1000

        return isBusinessPaymentAttentionNeeded()
                || timeToCheck.minus(currentTime) <= TIME_TO_SHOW_PAYMENT_INFO
    }

    fun cancelSubscriptions(feedback: String) {
        megaApi.creditCardCancelSubscriptions(feedback,
            OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    cancelSubscriptions.value = error
                }
            ))
    }

    fun upgradeAccount(context: Context) {
        context.startActivity(Intent(context, UpgradeAccountActivity::class.java))
    }

    fun logout(context: Context) {
        megaApi.shouldShowPasswordReminderDialog(
            true,
            ShouldShowPasswordReminderDialogListener(context, true)
        )
    }

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

    private fun addProfileAvatar(activity: Activity, uri: String?): String? {
        val myEmail = megaApi.myUser.email
        val imgFile = if (!uri.isNullOrEmpty()) File(uri)
        else CacheFolderManager.getCacheFile(
            activity,
            CacheFolderManager.TEMPORAL_FOLDER,
            "picture.jpg"
        )

        if (!FileUtil.isFileAvailable(imgFile)) {
            return getString(R.string.general_error)
        }

        val newFile = buildAvatarFile(activity, myEmail + "Temp.jpg")

        if (newFile != null) {
            MegaUtilsAndroid.createAvatar(imgFile, newFile)
            setAvatarUseCase.set(newFile.absolutePath)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy { result -> setProfileAvatar.value = result }
                .addTo(composite)
        } else {
            logError("ERROR! Destination PATH is NULL")
        }

        return null
    }

    fun deleteProfileAvatar(context: Context) {
        val avatar = buildAvatarFile(context, megaApi.myEmail + JPG_EXTENSION)

        if (FileUtil.isFileAvailable(avatar)) {
            logDebug("Avatar to delete: " + avatar.absolutePath)
            avatar.delete()
        }

        setAvatarUseCase.remove()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy { result -> setProfileAvatar.value = result }
            .addTo(composite)
    }
}