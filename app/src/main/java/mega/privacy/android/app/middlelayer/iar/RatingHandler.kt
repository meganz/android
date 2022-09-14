package mega.privacy.android.app.middlelayer.iar

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.preference.PreferenceManager
import mega.privacy.android.app.MegaApplication
import nz.mega.sdk.MegaApiJava

/**
 * Abstract In-App review handler for determine the base conditions and some special moment
 *
 * @property context Context for rating handler
 */
abstract class RatingHandler(val context: Context) {

    /**
     * Show review dialog by Android system show implement this on GMS and HMS
     * Since the API is different
     *
     * @param context Context for getting rating dialog
     */
    abstract fun showReviewDialog(context: Context, listener: OnCompleteListener)

    // Will set the flag to true when already showed the rating dialog
    private val completeListener = object : OnCompleteListener {
        override fun onComplete() {
            showedRating()
        }
    }

    /**
     * Determine if match the moment 1 that When a user that uploaded or downloaded a file is great than size limitation
     * with an average speed is great than speed limitation
     *
     * @param size the size of downloading or uploading files
     * @param speed current downloading or uploading speed
     * @param listener the listener after showing rating dialog
     */
    fun showRatingBaseOnSpeedAndSize(
        size: Long,
        speed: Long,
        listener: OnCompleteListener
    ) {
        if (!meetBaseCondition() || size <= 0 || speed <= 0) return

        val condition = byteToMb(size) >= SIZE_LIMIT && byteToMb(speed) >= SPEED_LIMIT

        if (condition) {
            showReviewDialog(context, completeListener)
            listener.onComplete()
        }
    }

    /**
     * Determine if match the moment 1 of phase 2 that when a user accepts the incoming request or others accept
     * the outgoing request, then the total contacts number is greater than 5
     */
    fun showRatingBaseOnContacts() {
        if (!meetBaseCondition()) return

        val app = MegaApplication.getInstance()
        val condition = if (app != null && app.megaApi != null) {
            val contact = app.megaApi.contacts
            if (contact.isNullOrEmpty()) {
                false
            } else {
                contact.size > CONTACTS_NUMBER_LIMIT
            }
        } else {
            false
        }

        if (condition) {
            showReviewDialog(context, completeListener)
        }
    }

    fun showRatingBaseOnTransaction() {
        if (!meetBaseCondition()) return

        if (isPurchasedTransaction()) {
            showReviewDialog(context, completeListener)
            updateTransactionFlag(false)
        }
    }


    /**
     * Determine if match the moment 2 that when a user that activates a share and this user already has >=4 file/folder shares
     * Shares includes out shares and public links
     *
     */
    fun showRatingBaseOnSharing() {
        if (!meetBaseCondition()) return

        val app = MegaApplication.getInstance()
        val condition = if (app != null && app.megaApi != null) {
            val totalNum = app.megaApi.publicLinks.size + app.megaApi.outShares.size
            totalNum >= SHARED_NUM_LIMIT
        } else {
            false
        }

        if (condition) {
            showReviewDialog(context, completeListener)
        }
    }

    /**
     * Determine if match the base condition that a user has >= 20 files
     *
     * @return if ture, it matches the base condition
     */
    private fun meetBaseCondition(): Boolean {
        if (isShowedRating()) {
            return false
        }

        val app = MegaApplication.getInstance()
        val megaApi = app.megaApi

        // Exclude ODQ & OBQ accounts
        if (megaApi.bandwidthOverquotaDelay > 0
            && app.storageState == MegaApiJava.STORAGE_STATE_PAYWALL) {
            return false
        }

        return megaApi.numNodes >= FILES_NUM_LIMIT
    }

    /**
     * Determine if it already showed the rating dialog before
     *
     * @return if it already showed the dialog, return ture, if not, return false
     */
    private fun isShowedRating(): Boolean {
        val key = getPreferenceKeyForRating()
        if (key.isNullOrEmpty()) return false

        return getSpValueByKey(key)
    }

    /**
     * Update the flag of showing rating on shared preference
     *
     */
    private fun showedRating() {
        val key = getPreferenceKeyForRating()
        if (key.isNullOrEmpty()) return

        updateSpValueByKey(key, true)
    }

    /**
     * Get the key save the flag for showing rating on shared preference
     *
     * @return the key of the flag for showing rating
     */
    private fun getPreferenceKeyForRating(): String? {
        val pm = context.packageManager
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            return PREFERENCE_SHOW_RATING + packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    fun updateTransactionFlag(flag: Boolean) = updateSpValueByKey(PREFERENCE_PURCHASE_TRANSACTION, flag)

    private fun isPurchasedTransaction() = getSpValueByKey(PREFERENCE_PURCHASE_TRANSACTION)

    private fun getSpValueByKey(key: String): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getBoolean(key, false)
    }

    private fun updateSpValueByKey(key: String, value: Boolean) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().putBoolean(key, value).apply()
    }


    companion object {
        // The size limitation, 10 Mb
        const val SIZE_LIMIT = 10
        // The speed limitation, 2 Mb
        const val SPEED_LIMIT = 2

        const val FILES_NUM_LIMIT = 20
        const val SHARED_NUM_LIMIT = 4

        const val CONTACTS_NUMBER_LIMIT = 5
        const val REFERRAL_BONUS_NUM_LIMIT = 3

        const val PREFERENCE_SHOW_RATING = "show_rating_"
        const val PREFERENCE_PURCHASE_TRANSACTION = "purchase_transaction_"
    }

    /**
     * Convert the size from byte to mb
     *
     * @param bytes the size in bytes
     * @return mbs the size in mbs
     */
    private fun byteToMb(bytes: Long) = bytes / 1024 / 1024
}

/**
 * Listener for completing call review action
 *
 */
interface OnCompleteListener {
    fun onComplete()
}