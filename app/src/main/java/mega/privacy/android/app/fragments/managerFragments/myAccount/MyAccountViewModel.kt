package mega.privacy.android.app.fragments.managerFragments.myAccount

import android.content.Context
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.lollipop.LoginActivityLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.Constants.*
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava

class MyAccountViewModel @ViewModelInject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
    private val dbH: DatabaseHandler,
    private val accountInfo: MyAccountInfo
) : BaseRxViewModel() {

    companion object {
        private const val CLICKS_TO_STAGING = 5
        private const val STAGING_URL = "https://staging.api.mega.co.nz/"
        private const val PRODUCTION_URL = "https://g.api.mega.co.nz/"
        private const val TIME_TO_SHOW_PAYMENT_INFO = 604800 //1 week in seconds
    }

    private var numOfClicksLastSession = 0
    private var staging = false

    fun getName(): String = accountInfo.fullName

    fun getEmail(): String = megaApi.myEmail

    fun getAccountType(): Int = accountInfo.accountType

    fun isFreeAccount(): Boolean = getAccountType() == FREE

    fun getUsedStorage(): String = accountInfo.usedFormatted

    fun getUsedStoragePercentage(): Int = accountInfo.usedPercentage

    fun getTotalStorage(): String = accountInfo.totalFormatted

    fun getUsedTransfer(): String = accountInfo.usedTransferFormatted

    fun getUsedTransferPercentage(): Int = accountInfo.usedTransferPercentage

    fun getTotalTransfer(): String = accountInfo.totalTransferFormatted

    fun getRenewTime(): Long = accountInfo.subscriptionRenewTime

    fun hasRenewableSubscription(): Boolean {
        return accountInfo.subscriptionStatus == MegaAccountDetails.SUBSCRIPTION_STATUS_VALID
                && accountInfo.subscriptionRenewTime > 0
    }

    fun getExpirationTime(): Long = accountInfo.proExpirationTime

    fun hasExpirableSubscription(): Boolean = accountInfo.proExpirationTime > 0

    fun getLastSession(): String = accountInfo.lastSessionFormattedDate ?: ""

    fun incrementLastSessionClick(context: Context): Boolean {
        numOfClicksLastSession++

        if (numOfClicksLastSession < CLICKS_TO_STAGING)
            return false

        numOfClicksLastSession = 0
        staging = false

        val attrs = dbH.attributes

        if (attrs != null && attrs.staging != null) {
            staging = try {
                java.lang.Boolean.parseBoolean(attrs.staging)
            } catch (e: Exception) {
                false
            }
        }

        if (staging) {
            setStaging(context, false)
            return false
        }

        return true
    }

    fun setStaging(context: Context, set: Boolean) {
        staging = set
        megaApi.changeApiUrl(if (set) STAGING_URL else PRODUCTION_URL)
        dbH.setStaging(set)

        val intent = Intent(context, LoginActivityLollipop::class.java)
        intent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT)
        intent.action = ACTION_REFRESH_STAGING
        (context as ManagerActivityLollipop)
            .startActivityForResult(intent, REQUEST_CODE_REFRESH_STAGING)
    }

    private fun isBusinessPaymentAttentionNeeded(): Boolean {
        val status = megaApi.businessStatus

        return megaApi.isBusinessAccount && megaApi.isMasterBusinessAccount
                && (status == MegaApiJava.BUSINESS_STATUS_EXPIRED
                || status == MegaApiJava.BUSINESS_STATUS_GRACE_PERIOD)
    }

    fun shouldShowPaymentInfo(): Boolean {
        val timeToCheck =
            if (hasRenewableSubscription()) accountInfo.subscriptionRenewTime
            else accountInfo.proExpirationTime

        val currentTime = System.currentTimeMillis() / 1000

        return isBusinessPaymentAttentionNeeded()
                || timeToCheck.minus(currentTime) <= TIME_TO_SHOW_PAYMENT_INFO
    }
}