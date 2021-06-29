package mega.privacy.android.app.middlelayer.iar

import android.content.Context
import android.content.pm.PackageManager
import androidx.preference.PreferenceManager
import mega.privacy.android.app.MegaApplication

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
     * Determine if match the moment 1 that When a user that uploaded or downloaded a file of >= 10 MB with an average speed of >= 2 MB/s
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
        if (!baseCondition() || size <= 0 || speed <= 0) return

        val condition = byteToMb(size) >= SIZE_LIMIT && byteToMb(speed) >= SPEED_LIMIT

        if (condition) {
            showReviewDialog(context, completeListener)
            listener.onComplete()
        }
    }


    /**
     * Determine if match the moment 2 that when a user that activates a share and this user already has >=4 file/folder shares
     * Shares includes out shares and public links
     *
     */
    fun showRatingBaseOnSharing() {
        if (!baseCondition()) return

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
    private fun baseCondition(): Boolean {
        if (!isShowedRating()) {
            return false
        }

        val app = MegaApplication.getInstance();
        if (app != null && app.megaApi != null) {
            val totalNum = app.megaApi.numNodes
            return totalNum >= 20
        }

        return false
    }

    /**
     * Determine if it already showed the rating dialog before
     *
     * @return if it already showed the dialog, return ture, if not, return false
     */
    private fun isShowedRating(): Boolean {
        val key = getPreferenceKeyForRating()
        if (key.isNullOrEmpty()) return false

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return !sharedPreferences.getBoolean(key, false)
    }

    /**
     * Update the flag of showing rating on shared preference
     *
     */
    private fun showedRating() {
        val key = getPreferenceKeyForRating()
        if (key.isNullOrEmpty()) return

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPreferences.edit().putBoolean(key, true).apply()
    }

    /**
     * Get the key save the flag for showing rating on shared preference
     *
     * @return the key of the flag for showing rating
     */
    fun getPreferenceKeyForRating(): String? {
        val pm = context.packageManager
        try {
            val packageInfo = pm.getPackageInfo(context.packageName, 0)
            return PREFERENCE_SHOW_RATING + packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    companion object {
        const val SIZE_LIMIT = 10
        const val SPEED_LIMIT = 2

        const val FILES_NUM_LIMIT = 20
        const val SHARED_NUM_LIMIT = 4

        const val PREFERENCE_SHOW_RATING = "show_rating_"
    }

    /**
     * Convert the size from byte to mb
     *
     * @param bytes the size in bytes
     * @return mbs the size in mbs
     */
    private fun byteToMb(bytes: Long): Long = bytes / 1024 / 1024
}

/**
 * Listener for completing call review action
 *
 */
interface OnCompleteListener {
    fun onComplete()
}