package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation key for the video editor screen.
 *
 * @param nodeHandle The MEGA node handle of the video to edit.
 */
@Serializable
data class VideoEditorScreenNavKey(val nodeHandle: Long) : NavKey
