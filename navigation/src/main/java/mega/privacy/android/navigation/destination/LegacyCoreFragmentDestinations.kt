package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class RubbishBin(
    val handle: Long? = null,
) : NavKey

@Serializable
data object Notifications : NavKey

@Serializable
data class MediaDiscovery(
    val nodeHandle: Long,
    val nodeName: String? = null,
) : NavKey
