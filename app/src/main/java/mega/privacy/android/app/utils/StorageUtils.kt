package mega.privacy.android.app.utils

import android.os.StatFs
import timber.log.Timber
import java.io.File

object StorageUtils {

    /**
     * Checks if there is enough free space to store a local file.
     *
     * @param path File path to check.
     * @return True if there is not enough free space, false otherwise.
     */
    @JvmStatic
    fun thereIsNotEnoughFreeSpace(path: String): Boolean {
        var availableFreeSpace = Double.MAX_VALUE

        try {
            val stat = StatFs(path)
            availableFreeSpace = stat.availableBlocksLong.toDouble() * stat.blockSizeLong.toDouble()
        } catch (ex: Exception) {
            Timber.e("Cannot get available space.")
        }

        return availableFreeSpace < File(path).length()
    }
}