package mega.privacy.android.app.globalmanagement

import android.content.Context
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.TimeUtils.getDateString
import mega.privacy.android.app.utils.Util.getSizeString
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaAccountDetails
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
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

    var usedStorage = INVALID_VALUE.toLong()
    var accountType = INVALID_VALUE
    var formattedUsedRubbish = ""

    /**
     * Resets all values by default.
     * It's mandatory to add here any new attribute included
     * and call it each time the account logs out.
     */
    fun resetDefaults() {
        usedStorage = INVALID_VALUE.toLong()
        accountType = INVALID_VALUE
        formattedUsedRubbish = ""
    }

    fun setAccountDetails(accountInfo: MegaAccountDetails, numDetails: Int, context: Context) {
        Timber.d("numDetails: $numDetails")
        Timber.d("Renews ts: ${accountInfo.subscriptionRenewTime}")
        Timber.d("Renews on: ${getDateString(accountInfo.subscriptionRenewTime)}")
        Timber.d("Expires ts: ${accountInfo.proExpiration}")
        Timber.d("Expires on: ${getDateString(accountInfo.proExpiration)}")

        val storage = numDetails and HAS_STORAGE_DETAILS != 0
        val pro = numDetails and HAS_PRO_DETAILS != 0

        if (storage) {
            if (megaApi.rubbishNode != null) {
                val usedRubbish =
                    accountInfo.getStorageUsed(megaApi.rubbishNode?.handle ?: INVALID_HANDLE)
                formattedUsedRubbish = getSizeString(usedRubbish, context)
            }

            usedStorage = accountInfo.storageUsed
        }

        if (pro) {
            accountType = accountInfo.proLevel
        }

        Timber.d("pro level: ${accountInfo.proLevel}")
    }
}
