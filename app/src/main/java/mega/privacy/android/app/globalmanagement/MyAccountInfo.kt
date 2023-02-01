package mega.privacy.android.app.globalmanagement

import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.TimeUtils.getDateString
import mega.privacy.android.app.utils.Util.getSizeString
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.USER_ATTR_MY_BACKUPS_FOLDER
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class to manage account details.
 *
 * @see resetDefaults before adding any new property.
 */
@Singleton
class MyAccountInfo @Inject constructor(
    @MegaApi private val megaApi: MegaApiAndroid,
) {

    companion object {
        const val HAS_STORAGE_DETAILS = 0x01
        const val HAS_TRANSFER_DETAILS = 0x02
        const val HAS_PRO_DETAILS = 0x04
        const val HAS_SESSIONS_DETAILS = 0x020
    }

    enum class UpgradeFrom {
        MANAGER, ACCOUNT, SETTINGS
    }

    var usedPercentage = INVALID_VALUE
    var usedTransferPercentage = INVALID_VALUE
    var usedStorage = INVALID_VALUE.toLong()
    var accountType = INVALID_VALUE
    var subscriptionStatus = INVALID_VALUE
    var subscriptionRenewTime = INVALID_VALUE.toLong()
    var proExpirationTime = INVALID_VALUE.toLong()
    var usedFormatted = ""
    var totalFormatted = ""
    var formattedUsedCloud = ""
    var formattedUsedBackups = ""
    var formattedUsedIncoming = ""
    var formattedUsedRubbish = ""
    private var formattedAvailableSpace = ""
    var usedTransferFormatted = ""
    var totalTransferFormatted = ""
    var levelAccountDetails = INVALID_VALUE

    var isAccountDetailsFinished = false
    var isBusinessAlertShown = false
    private var wasBusinessAlertAlreadyShown = false

    var lastSessionFormattedDate: String? = null
    var createSessionTimeStamp = INVALID_VALUE.toLong()

    var numVersions = INVALID_VALUE
    var previousVersionsSize = INVALID_VALUE.toLong()

    var bonusStorageSMS = "GB"

    var upgradeOpenedFrom = UpgradeFrom.MANAGER

    // Added the subscriptionMethodId parameter for subscription dialog
    var subscriptionMethodId = -1

    /**
     * Resets all values by default.
     * It's mandatory to add here any new attribute included
     * and call it each time the account logs out.
     */
    fun resetDefaults() {
        usedPercentage = INVALID_VALUE
        usedTransferPercentage = INVALID_VALUE
        usedStorage = INVALID_VALUE.toLong()
        accountType = INVALID_VALUE
        subscriptionStatus = INVALID_VALUE
        subscriptionRenewTime = INVALID_VALUE.toLong()
        proExpirationTime = INVALID_VALUE.toLong()
        usedFormatted = ""
        totalFormatted = ""
        formattedUsedCloud = ""
        formattedUsedBackups = ""
        formattedUsedIncoming = ""
        formattedUsedRubbish = ""
        formattedAvailableSpace = ""
        usedTransferFormatted = ""
        totalTransferFormatted = ""
        levelAccountDetails = INVALID_VALUE

        isAccountDetailsFinished = false
        isBusinessAlertShown = false
        wasBusinessAlertAlreadyShown = false

        lastSessionFormattedDate = null
        createSessionTimeStamp = INVALID_VALUE.toLong()

        numVersions = INVALID_VALUE
        previousVersionsSize = INVALID_VALUE.toLong()

        bonusStorageSMS = "GB"

        upgradeOpenedFrom = UpgradeFrom.MANAGER
    }

    fun setAccountDetails(accountInfo: MegaAccountDetails, numDetails: Int) {
        Timber.d("numDetails: $numDetails")
        Timber.d("Renews ts: ${accountInfo.subscriptionRenewTime}")
        Timber.d("Renews on: ${getDateString(accountInfo.subscriptionRenewTime)}")
        Timber.d("Expires ts: ${accountInfo.proExpiration}")
        Timber.d("Expires on: ${getDateString(accountInfo.proExpiration)}")

        val storage = numDetails and HAS_STORAGE_DETAILS != 0
        val transfer = numDetails and HAS_TRANSFER_DETAILS != 0
        val pro = numDetails and HAS_PRO_DETAILS != 0

        if (storage) {
            val totalStorage = accountInfo.storageMax
            val usedCloudDrive: Long
            val usedRubbish: Long
            var usedIncoming: Long = 0

            //Check size of the different nodes
            if (megaApi.rootNode != null) {
                usedCloudDrive = accountInfo.getStorageUsed(megaApi.rootNode.handle)
                formattedUsedCloud = getSizeString(usedCloudDrive)
            }

            // Check the My Backups root folder
            megaApi.getUserAttribute(USER_ATTR_MY_BACKUPS_FOLDER,
                OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            // The Backups Root Folder may be null if
                            // the user has not setup his/her Backups
                            megaApi.getNodeByHandle(request.nodeHandle)
                                ?.let { setUsedBackupsStorage(it) }
                        }
                    }
                )
            )

            if (megaApi.rubbishNode != null) {
                usedRubbish = accountInfo.getStorageUsed(megaApi.rubbishNode.handle)
                formattedUsedRubbish = getSizeString(usedRubbish)
            }

            val nodes = megaApi.inShares

            if (nodes != null) {
                for (i in nodes.indices) {
                    usedIncoming += accountInfo.getStorageUsed(nodes[i].handle)
                }
            }

            formattedUsedIncoming = getSizeString(usedIncoming)
            totalFormatted = getSizeString(totalStorage)
            usedStorage = accountInfo.storageUsed
            usedFormatted = getSizeString(usedStorage)
            usedPercentage = 0
            subscriptionMethodId = accountInfo.subscriptionMethodId

            if (totalStorage != 0L) {
                usedPercentage = (100 * usedStorage / totalStorage).toInt()
            }

            val availableSpace = totalStorage.minus(usedStorage)

            formattedAvailableSpace = getSizeString(if (availableSpace < 0) 0 else availableSpace)
        }

        if (transfer) {
            totalTransferFormatted = getSizeString(accountInfo.transferMax)
            usedTransferFormatted = getSizeString(accountInfo.transferUsed)
            usedTransferPercentage = 0

            if (accountInfo.transferMax != 0L) {
                usedTransferPercentage =
                    (100 * accountInfo.transferUsed / accountInfo.transferMax).toInt()
            }
        }

        if (pro) {
            accountType = accountInfo.proLevel
            subscriptionStatus = accountInfo.subscriptionStatus
            subscriptionRenewTime = accountInfo.subscriptionRenewTime
            proExpirationTime = accountInfo.proExpiration

            when (accountType) {
                0 -> levelAccountDetails = INVALID_VALUE
                1 -> levelAccountDetails = 1
                2 -> levelAccountDetails = 2
                3 -> levelAccountDetails = 3
                4 -> levelAccountDetails = 0
            }
        }

        isAccountDetailsFinished = true
        Timber.d("LEVELACCOUNTDETAILS: $levelAccountDetails")
    }

    /**
     * Displays the total storage used by the My Backups root folder
     * @param rootFolder The My Backups root folder which may be nullable
     */
    private fun setUsedBackupsStorage(rootFolder: MegaNode) {
        megaApi.getFolderInfo(rootFolder, OptionalMegaRequestListenerInterface(
            onRequestFinish = { request, error ->
                if (error.errorCode == MegaError.API_OK) {
                    val totalStorage = request.megaFolderInfo.currentSize
                    formattedUsedBackups =
                        if (totalStorage < 1) "" else getSizeString(totalStorage)
                }
            }
        ))
    }

    fun getFormattedPreviousVersionsSize(): String? {
        return getSizeString(previousVersionsSize)
    }

    fun wasNotBusinessAlertShownYet(): Boolean = !wasBusinessAlertAlreadyShown

    fun isUpgradeFromAccount(): Boolean = upgradeOpenedFrom == UpgradeFrom.ACCOUNT
    fun isUpgradeFromManager(): Boolean = upgradeOpenedFrom == UpgradeFrom.MANAGER
    fun isUpgradeFromSettings(): Boolean = upgradeOpenedFrom == UpgradeFrom.SETTINGS
}