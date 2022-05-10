package mega.privacy.android.app.utils

import androidx.annotation.ColorRes
import mega.privacy.android.app.R
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaUser
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object MegaUserUtils {

    private const val RECENTLY_ADDED_MIN_DAYS = 3

    /**
     * Get user status color based on user status code
     *
     * @param userStatus    User status code
     * @return              User status color
     */
    @ColorRes
    @JvmStatic
    fun getUserStatusColor(userStatus: Int): Int =
        when (userStatus) {
            MegaChatApi.STATUS_AWAY -> R.color.orange_400
            MegaChatApi.STATUS_ONLINE -> R.color.lime_green_500
            MegaChatApi.STATUS_BUSY -> R.color.salmon_700
            else -> R.color.grey_700
        }

    /**
     * Check if the user has been added within the last 24h.
     *
     * @return  True if it has been added recently, false otherwise
     */
    @JvmStatic
    fun MegaUser.wasRecentlyAdded(): Boolean {
        val now = LocalDateTime.now()
        val addedTime = Instant.ofEpochSecond(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return Duration.between(addedTime, now).toDays() < RECENTLY_ADDED_MIN_DAYS
    }

    /**
     * Indicates if the user is changed by another client.
     */
    fun MegaUser.isExternalChange(): Boolean =
        isOwnChange == 0

    /**
     * Indicates if the user is changed as a result of an explicit request.
     */
    fun MegaUser.isRequestedChange(): Boolean =
        isOwnChange > 0

    /**
     * Indicates if the user is changed as a result of an implicit request made by
     * the SDK internally.
     */
    fun MegaUser.isRequestedSDKChange(): Boolean =
        isOwnChange == -1
}
