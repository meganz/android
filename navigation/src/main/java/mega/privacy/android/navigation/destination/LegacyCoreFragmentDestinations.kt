package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class RubbishBinNavKey(
    val handle: Long? = null,
) : NavKey

@Serializable
data object NotificationsNavKey : NavKey

@Serializable
data class MediaDiscoveryNavKey(
    val nodeHandle: Long,
    val nodeName: String? = null,
) : NavKey
