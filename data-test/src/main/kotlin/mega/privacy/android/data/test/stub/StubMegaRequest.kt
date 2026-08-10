package mega.privacy.android.data.test.stub

import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaAchievementsDetails
import nz.mega.sdk.MegaBackgroundMediaUpload
import nz.mega.sdk.MegaBackupInfoList
import nz.mega.sdk.MegaBannerList
import nz.mega.sdk.MegaCancelSubscriptionReasonList
import nz.mega.sdk.MegaCurrency
import nz.mega.sdk.MegaDiscountCodeInfo
import nz.mega.sdk.MegaDiscountCodeList
import nz.mega.sdk.MegaFileServiceReclaimOptions
import nz.mega.sdk.MegaFolderInfo
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaIntegerList
import nz.mega.sdk.MegaNetworkConnectivityTestResults
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeTree
import nz.mega.sdk.MegaNotificationList
import nz.mega.sdk.MegaPricing
import nz.mega.sdk.MegaPushNotificationSettings
import nz.mega.sdk.MegaRecentActionBucketList
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaScheduledMeetingList
import nz.mega.sdk.MegaSet
import nz.mega.sdk.MegaSetElementList
import nz.mega.sdk.MegaStringIntegerMap
import nz.mega.sdk.MegaStringList
import nz.mega.sdk.MegaStringListMap
import nz.mega.sdk.MegaStringMap
import nz.mega.sdk.MegaStringTable
import nz.mega.sdk.MegaSyncStallList
import nz.mega.sdk.MegaSyncStallMap
import nz.mega.sdk.MegaTextChatList
import nz.mega.sdk.MegaTextChatPeerList
import nz.mega.sdk.MegaTimeZoneDetails
import nz.mega.sdk.MegaVpnCredentials
import nz.mega.sdk.MegaVpnRegionList

/**
 * In-memory stub of [MegaRequest] that is safe to use in unit tests.
 *
 * Every public instance method is overridden, so no call ever reaches the native SDK
 * (which would crash the JVM given the null native pointer). Data getters are backed by
 * constructor parameters with sensible defaults; all other methods return benign constants.
 */
class StubMegaRequest(
    private val type: Int = 0,
    private val nodeHandle: Long = -1L,
    private val parentHandle: Long = -1L,
    private val email: String? = null,
    private val name: String? = null,
    private val link: String? = null,
    private val file: String? = null,
    private val text: String? = null,
    private val paramType: Int = 0,
    private val number: Long = 0L,
    private val flag: Boolean = false,
    private val access: Int = 0,
    private val transferTag: Int = 0,
    private val numDetails: Int = 0,
    private val publicMegaNode: MegaNode? = null,
    private val megaStringMap: MegaStringMap? = null,
    private val megaStringList: MegaStringList? = null,
    private val megaHandleList: MegaHandleList? = null,
    private val megaSet: MegaSet? = null,
    private val megaSetElementList: MegaSetElementList? = null,
    private val megaAccountDetails: MegaAccountDetails? = null,
) : MegaRequest(0, false) {

    override fun delete() = Unit

    override fun getType(): Int = type
    override fun getRequestString(): String = ""
    override fun toString(): String = ""
    override fun getNodeHandle(): Long = nodeHandle
    override fun getLink(): String? = link
    override fun getParentHandle(): Long = parentHandle
    override fun getSessionKey(): String? = null
    override fun getName(): String? = name
    override fun getEmail(): String? = email
    override fun getPassword(): String? = null
    override fun getNewPassword(): String? = null
    override fun getPrivateKey(): String? = null
    override fun getAccess(): Int = access
    override fun getFile(): String? = file
    override fun getNumRetry(): Int = 0
    override fun getPublicMegaNode(): MegaNode? = publicMegaNode
    override fun getParamType(): Int = paramType
    override fun getText(): String? = text
    override fun getNumber(): Long = number
    override fun getFlag(): Boolean = flag
    override fun getTransferredBytes(): Long = 0L
    override fun getTotalBytes(): Long = 0L
    override fun getMegaAccountDetails(): MegaAccountDetails? = megaAccountDetails
    override fun getPricing(): MegaPricing? = null
    override fun getCurrency(): MegaCurrency? = null
    override fun getMegaAchievementsDetails(): MegaAchievementsDetails? = null
    override fun getMegaTimeZoneDetails(): MegaTimeZoneDetails? = null
    override fun getTransferTag(): Int = transferTag
    override fun getNumDetails(): Int = numDetails
    override fun getTag(): Int = 0
    override fun getMegaTextChatPeerList(): MegaTextChatPeerList? = null
    override fun getMegaTextChatList(): MegaTextChatList? = null
    override fun getMegaStringMap(): MegaStringMap? = megaStringMap
    override fun getMegaStringListMap(): MegaStringListMap? = null
    override fun getMegaStringTable(): MegaStringTable? = null
    override fun getMegaFolderInfo(): MegaFolderInfo? = null
    override fun getMegaPushNotificationSettings(): MegaPushNotificationSettings? = null
    override fun getMegaBackgroundMediaUploadPtr(): MegaBackgroundMediaUpload? = null
    override fun getMegaBannerList(): MegaBannerList? = null
    override fun getMegaStringList(): MegaStringList? = megaStringList
    override fun getMegaStringIntegerMap(): MegaStringIntegerMap? = null
    override fun getMegaScheduledMeetingList(): MegaScheduledMeetingList? = null
    override fun getMegaHandleList(): MegaHandleList? = megaHandleList
    override fun getRecentActions(): MegaRecentActionBucketList? = null
    override fun getMegaIntegerList(): MegaIntegerList? = null
    override fun getMegaSet(): MegaSet? = megaSet
    override fun getMegaSetElementList(): MegaSetElementList? = megaSetElementList
    override fun getMegaBackupInfoList(): MegaBackupInfoList? = null
    override fun getMegaSyncStallList(): MegaSyncStallList? = null
    override fun getMegaSyncStallMap(): MegaSyncStallMap? = null
    override fun getMegaVpnRegionsDetailed(): MegaVpnRegionList? = null
    override fun getMegaVpnCredentials(): MegaVpnCredentials? = null
    override fun getMegaNetworkConnectivityTestResults(): MegaNetworkConnectivityTestResults? = null
    override fun getMegaNotifications(): MegaNotificationList? = null
    override fun getMegaNodeTree(): MegaNodeTree? = null
    override fun getMegaCancelSubscriptionReasons(): MegaCancelSubscriptionReasonList? = null
    override fun getMegaDiscountCodeList(): MegaDiscountCodeList? = null
    override fun getMegaDiscountCodeInfo(): MegaDiscountCodeInfo? = null
    override fun getMegaFileServiceReclaimOptions(): MegaFileServiceReclaimOptions? = null
}
