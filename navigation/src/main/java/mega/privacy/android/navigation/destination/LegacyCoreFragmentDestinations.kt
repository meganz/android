package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class RubbishBinNavKey(
    val handle: Long? = null,
    val highlightedNodeHandle: Long? = null,
) : NavKey {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RubbishBinNavKey) return false
        return handle == other.handle
    }

    override fun hashCode(): Int {
        return handle?.hashCode() ?: 0
    }
}

@Serializable
data object NotificationsNavKey : NavKey

@Serializable
data class MediaDiscoveryNavKey(
    val nodeHandle: Long,
    val nodeName: String? = null,
) : NavKey
