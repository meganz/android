package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaAccountBalance
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaAccountFeature
import nz.mega.sdk.MegaAccountPlan
import nz.mega.sdk.MegaAccountPurchase
import nz.mega.sdk.MegaAccountSession
import nz.mega.sdk.MegaAccountSubscription
import nz.mega.sdk.MegaAccountTransaction
import nz.mega.sdk.MegaStringIntegerMap

/**
 * In-memory stub of [MegaAccountDetails] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Defaults describe a free
 * account with nothing used; quotas are non-zero so percentage calculations stay sane.
 */
class StubMegaAccountDetails(
    private val proLevel: Int = ACCOUNT_TYPE_FREE,
    private val storageUsed: Long = 0L,
    private val storageMax: Long = 21_474_836_480L,
    private val transferUsed: Long = 0L,
    private val transferMax: Long = 1_099_511_627_776L,
    private val subscriptionStatus: Int = 0,
    private val subscriptionRenewTime: Long = 0L,
    private val proExpiration: Long = 0L,
    private val subscriptionMethodId: Int = -1,
    private val subscriptionCycle: String = "",
    private val subscriptionMethod: String = "",
) : MegaAccountDetails(0, false) {

    override fun delete() = Unit

    override fun getProLevel(): Int = proLevel
    override fun getProExpiration(): Long = proExpiration
    override fun getSubscriptionStatus(): Int = subscriptionStatus
    override fun getSubscriptionRenewTime(): Long = subscriptionRenewTime
    override fun getSubscriptionMethod(): String = subscriptionMethod
    override fun getSubscriptionMethodId(): Int = subscriptionMethodId
    override fun getSubscriptionCycle(): String = subscriptionCycle
    override fun getStorageMax(): Long = storageMax
    override fun getStorageUsed(): Long = storageUsed
    override fun getVersionStorageUsed(): Long = 0L
    override fun getTransferMax(): Long = transferMax
    override fun getTransferOwnUsed(): Long = transferUsed
    override fun getTransferSrvUsed(): Long = 0L
    override fun getTransferUsed(): Long = transferUsed
    override fun getNumUsageItems(): Int = 0
    override fun getStorageUsed(handle: Long): Long = 0L
    override fun getNumFiles(handle: Long): Long = 0L
    override fun getNumFolders(handle: Long): Long = 0L
    override fun getVersionStorageUsed(handle: Long): Long = 0L
    override fun getNumVersionFiles(handle: Long): Long = 0L
    override fun getNumBalances(): Int = 0
    override fun getBalance(i: Int): MegaAccountBalance? = null
    override fun getNumSessions(): Int = 0
    override fun getSession(i: Int): MegaAccountSession? = null
    override fun getNumPurchases(): Int = 0
    override fun getPurchase(i: Int): MegaAccountPurchase? = null
    override fun getNumTransactions(): Int = 0
    override fun getTransaction(i: Int): MegaAccountTransaction? = null
    override fun getTemporalBandwidthInterval(): Int = 0
    override fun getTemporalBandwidth(): Long = 0L
    override fun isTemporalBandwidthValid(): Boolean = false
    override fun getNumActiveFeatures(): Int = 0
    override fun getActiveFeature(featureIndex: Int): MegaAccountFeature? = null
    override fun getSubscriptionLevel(): Long = 0L
    override fun getSubscriptionFeatures(): MegaStringIntegerMap? = null
    override fun getNumSubscriptions(): Int = 0
    override fun getSubscription(subscriptionsIndex: Int): MegaAccountSubscription? = null
    override fun getNumPlans(): Int = 0
    override fun getPlan(plansIndex: Int): MegaAccountPlan? = null
}
