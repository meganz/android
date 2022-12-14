package mega.privacy.android.app.globalmanagement

import android.util.Base64
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.middlelayer.iab.MegaPurchase
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.ContactUtil.notifyFirstNameUpdate
import mega.privacy.android.app.utils.ContactUtil.notifyLastNameUpdate
import mega.privacy.android.app.utils.StringResourcesUtils.getString
import mega.privacy.android.app.utils.TimeUtils.getDateString
import mega.privacy.android.app.utils.Util.getSizeString
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.account.MegaSku
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaApiJava.USER_ATTR_MY_BACKUPS_FOLDER
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale
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
    private val dbH: DatabaseHandler,
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
    private var accountInfo: MegaAccountDetails? = null
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
    var levelInventory = INVALID_VALUE
    var levelAccountDetails = INVALID_VALUE

    var isInventoryFinished = false
    var isAccountDetailsFinished = false
    var isBusinessAlertShown = false
    private var wasBusinessAlertAlreadyShown = false

    private var firstNameText = ""
    private var lastNameText = ""
    private var firstLetter: String? = null
    var fullName = ""

    var lastSessionFormattedDate: String? = null
    var createSessionTimeStamp = INVALID_VALUE.toLong()

    var availableSkus: List<MegaSku> = ArrayList()
    var activeSubscription: MegaPurchase? = null

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
        accountInfo = null
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
        levelInventory = INVALID_VALUE
        levelAccountDetails = INVALID_VALUE

        isInventoryFinished = false
        isAccountDetailsFinished = false
        isBusinessAlertShown = false
        wasBusinessAlertAlreadyShown = false

        firstNameText = ""
        lastNameText = ""
        firstLetter = null
        fullName = ""

        lastSessionFormattedDate = null
        createSessionTimeStamp = INVALID_VALUE.toLong()

        availableSkus = ArrayList()
        activeSubscription = null

        numVersions = INVALID_VALUE
        previousVersionsSize = INVALID_VALUE.toLong()

        bonusStorageSMS = "GB"

        upgradeOpenedFrom = UpgradeFrom.MANAGER
    }

    fun setAccountDetails(numDetails: Int) {
        Timber.d("numDetails: $numDetails")

        if (accountInfo == null) {
            Timber.e("Error because account info is NUll in setAccountDetails")
        }

        val accountInfo = accountInfo ?: return

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
        Timber.d("LEVELACCOUNTDETAILS: $levelAccountDetails; LEVELINVENTORY: $levelInventory; INVENTORYFINISHED: $isInventoryFinished")
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

    /**
     * Updates own firstName/lastName and fullName data.
     *
     * @param firstName True if the update makes reference to the firstName, false it to the lastName.
     * @param newName   New firstName/lastName text.
     * @param e         MegaError of the request.
     */
    fun updateMyData(firstName: Boolean, newName: String?, e: MegaError) {
        if (e.errorCode != MegaError.API_OK || newName == null) {
            Timber.e("ERROR - request.getText(): $newName")

            if (firstName) {
                setFirstNameText("")
            } else {
                setLastNameText("")
            }

            return
        }

        Timber.d("request.getText(): $newName")

        val handle = megaApi.myUser?.handle ?: MegaApiJava.INVALID_HANDLE

        if (firstName) {
            setFirstNameText(newName)
            dbH.saveMyFirstName(newName)

            if (handle != MegaApiJava.INVALID_HANDLE) {
                notifyFirstNameUpdate(MegaApplication.getInstance(), handle)
            }
        } else {
            setLastNameText(newName)
            dbH.saveMyLastName(newName)

            if (handle != MegaApiJava.INVALID_HANDLE) {
                notifyLastNameUpdate(MegaApplication.getInstance(), handle)
            }
        }
    }

    fun setAccountInfo(accountInfo: MegaAccountDetails) {
        this.accountInfo = accountInfo
        Timber.d("Renews ts: ${accountInfo.subscriptionRenewTime}")
        Timber.d("Renews on: ${getDateString(accountInfo.subscriptionRenewTime)}")
        Timber.d("Expires ts: ${accountInfo.proExpiration}")
        Timber.d("Expires on: ${getDateString(accountInfo.proExpiration)}")
    }

    fun getFirstNameText(): String = firstNameText

    fun setFirstNameText(firstName: String) {
        firstNameText = firstName
        setFullName()
    }

    fun getLastNameText(): String = lastNameText

    fun setLastNameText(firstName: String) {
        lastNameText = firstName
        setFullName()
    }

    private fun setFullName() {
        Timber.d("setFullName")

        fullName = if (firstNameText.trim().isEmpty()) {
            lastNameText
        } else {
            "$firstNameText $lastNameText"
        }

        if (fullName.trim().isEmpty()) {
            Timber.d("Put email as fullname")

            var email = ""
            val user = megaApi.myUser

            if (user != null) {
                email = user.email
            }

            val splitEmail = email.split("[@._]".toRegex()).toTypedArray()
            fullName = splitEmail[0]
        }

        if (fullName.trim().isEmpty()) {
            fullName = getString(R.string.name_text) + " " + getString(R.string.lastname_text)
            Timber.d("Full name set by default: $fullName")
        }

        firstLetter = fullName[0].toString() + ""
        firstLetter = firstLetter?.uppercase(Locale.getDefault())
    }

    fun getFormattedPreviousVersionsSize(): String? {
        return getSizeString(previousVersionsSize)
    }

    fun isPurchasedAlready(sku: String): Boolean {
        if (activeSubscription == null) {
            return false
        }

        val result = activeSubscription!!.sku == sku

        if (result) {
            Timber.d("$sku already subscribed.")
        }

        return result
    }

    fun wasNotBusinessAlertShownYet(): Boolean = !wasBusinessAlertAlreadyShown

    /**
     * Generate an obfuscated account Id.
     * The obfuscated account id can be passed to 'BillingFlowParams' for fraud prevention.
     *
     * @return A one-way hash based on the unique userHandleBinary.
     */
    fun generateObfuscatedAccountId(): String? {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val encodeHash = digest.digest(
                megaApi.myUserHandleBinary.toString().toByteArray(StandardCharsets.UTF_8)
            )

            return Base64.encodeToString(encodeHash, Base64.DEFAULT)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            Timber.e(e, "Generate obfuscated account Id failed.")
        }

        return null
    }

    fun isUpgradeFromAccount(): Boolean = upgradeOpenedFrom == UpgradeFrom.ACCOUNT
    fun isUpgradeFromManager(): Boolean = upgradeOpenedFrom == UpgradeFrom.MANAGER
    fun isUpgradeFromSettings(): Boolean = upgradeOpenedFrom == UpgradeFrom.SETTINGS
}