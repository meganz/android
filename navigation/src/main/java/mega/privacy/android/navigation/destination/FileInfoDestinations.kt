package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav key for file/folder info screen
 *
 * @param nodeHandle
 */
@Serializable
data class FileInfoNavKey(
    val nodeHandle: Long
) : NavKey
