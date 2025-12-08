package mega.privacy.android.navigation.destination

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey

/**
 * Cloud drive route args
 * @property nodeHandle the handle of the node to display
 * @property nodeName optional name to show as screen title
 * @property nodeSourceType the source type of the node
 * @property isNewFolder whether the screen is opened after creating a new folder
 * @property highlightedNodeHandle the handle of the node to highlight
 * @property highlightedNodeNames the names of the nodes to highlight
 */
@Serializable
@Parcelize
data class CloudDriveNavKey(
    val nodeHandle: Long = -1L,
    val nodeName: String? = null,
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    val isNewFolder: Boolean = false,
    val highlightedNodeHandle: Long? = null,
    val highlightedNodeNames: List<String>? = null,
) : NavKey, Parcelable

/**
 * Shares route args
 */
@Serializable
data object SharesNavKey : NavKey

/**
 * Offline route args
 */
@Serializable
data class OfflineNavKey(
    val title: String? = null,
    val nodeId: Int = -1,
    val path: String? = null,
    val highlightedFiles: String? = null,
) : NavKey

@Serializable
data class DriveSyncNavKey(
    val initialTabIndex: Int = 0,
    val highlightedNodeHandle: Long? = null,
) : MainNavItemNavKey

/**
 * Favourites route args
 */
@Serializable
data object FavouritesNavKey : NavKey

@Serializable
data class OverQuotaDialogNavKey(
    val isOverQuota: Boolean,
) : NavKey
