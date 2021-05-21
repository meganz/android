package mega.privacy.android.app.utils

import android.content.Context
import androidx.annotation.ColorRes
import mega.privacy.android.app.R
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaUser
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object MegaUserUtils {

    private const val RECENTLY_ADDED_MIN_HOURS = 24

    @ColorRes
    @JvmStatic
    fun getUserStatusColor(userStatus: Int): Int =
        when (userStatus) {
            MegaChatApi.STATUS_AWAY -> R.color.orange_400
            MegaChatApi.STATUS_ONLINE -> R.color.lime_green_500
            MegaChatApi.STATUS_BUSY -> R.color.salmon_700
            else -> R.color.grey_700
        }

    @JvmStatic
    fun getUserAvatarFile(context: Context, userEmail: String): File? =
        CacheFolderManager.buildAvatarFile(context, "$userEmail.jpg")

    @JvmStatic
    fun MegaUser.wasRecentlyAdded(): Boolean {
        val now = LocalDateTime.now()
        val addedTime = Instant.ofEpochSecond(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        return Duration.between(addedTime, now).toHours() < RECENTLY_ADDED_MIN_HOURS
    }
}
