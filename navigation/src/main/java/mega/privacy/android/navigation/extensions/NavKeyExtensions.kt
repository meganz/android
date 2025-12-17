package mega.privacy.android.navigation.extensions

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.RubbishBinNavKey

/**
 * Check if two NavKeys match based on their type and properties
 * @param other The other NavKey to compare with
 * @return true if both NavKeys are of the same type and have matching properties
 */
fun NavKey.matches(other: NavKey?): Boolean {
    return when {
        this is RubbishBinNavKey && other is RubbishBinNavKey -> {
            handle == other.handle
        }

        this is CloudDriveNavKey && other is CloudDriveNavKey -> {
            nodeHandle == other.nodeHandle &&
                    nodeSourceType == other.nodeSourceType
        }

        else -> false
    }
}
