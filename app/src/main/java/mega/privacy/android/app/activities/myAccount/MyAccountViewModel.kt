package mega.privacy.android.app.activities.myAccount

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jeremyliao.liveeventbus.LiveEventBus
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.activities.exportMK.ExportRecoveryKeyActivity
import mega.privacy.android.app.activities.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.constants.EventConstants.EVENT_REFRESH
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.listeners.ShouldShowPasswordReminderDialogListener
import mega.privacy.android.app.lollipop.ChangePasswordActivityLollipop
import mega.privacy.android.app.lollipop.LoginActivityLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import nz.mega.sdk.*
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

class MyAccountViewModel @ViewModelInject constructor(
    private val myAccountInfo: MyAccountInfo,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val dbH: DatabaseHandler
) : BaseRxViewModel() {

    companion object {
        private const val CLICKS_TO_STAGING = 5
        private const val STAGING_URL = "https://staging.api.mega.co.nz/"
        private const val PRODUCTION_URL = "https://g.api.mega.co.nz/"
        private const val TIME_TO_SHOW_PAYMENT_INFO = 604800 //1 week in seconds
    }

    private val versionsInfo: MutableLiveData<MegaError> = MutableLiveData()
    private val avatar: MutableLiveData<MegaError> = MutableLiveData()
    private val killSessions: MutableLiveData<MegaError> = MutableLiveData()
    private val cancelSubscriptions: MutableLiveData<MegaError> = MutableLiveData()

    private var fragment = MY_ACCOUNT_FRAGMENT
    private var numOfClicksLastSession = 0

    fun onUpdateVersionsInfoFinished(): LiveData<MegaError> = versionsInfo
    fun onGetAvatarFinished(): LiveData<MegaError> = avatar
    fun onKillSessionsFinished(): LiveData<MegaError> = killSessions
    fun onCancelSubscriptions(): LiveData<MegaError> = cancelSubscriptions

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

    fun manageActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data == null) {
            return
        }

        if (requestCode == REQUEST_CODE_REFRESH && resultCode == RESULT_OK) {
            val app = MegaApplication.getInstance()

            app.askForAccountDetails()
            app.askForExtendedAccountDetails()
            LiveEventBus.get(EVENT_REFRESH).post(true)
        }
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
}