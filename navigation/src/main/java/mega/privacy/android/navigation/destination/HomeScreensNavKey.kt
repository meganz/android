package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import mega.privacy.android.navigation.contract.navkey.NoNodeNavKey

@Serializable
data object RecentsScreenNavKey : NavKey

@Serializable
data class RecentsBucketScreenNavKey(
    val identifier: String,
    val isMediaBucket: Boolean,
    val folderName: String,
    val folderHandle: Long = -1L,
    val nodeSourceType: NodeSourceType,
    val timestamp: Long,
    val fileCount: Int,
) : NavKey

/**
 * A NavKey to represent the home screens navigation state, including the root main nav item and
 * the stack of destinations within that main nav item.
 *
 * @param timestamp force recomposition when the same root and destinations are provided again
 */
@Serializable
data class HomeScreensNavKey(
    private val serialisedRoot: String?,
    private val serialisedDestinations: String?,
    private val timestamp: Long = 0,
) : NoNodeNavKey {

    constructor() : this(
        serialisedRoot = null,
        serialisedDestinations = null,
    )

    constructor(root: MainNavItemNavKey, destinations: List<NavKey>?, timestamp: Long = 0) : this(
        serialisedRoot = serialiseMainNavItemNavKey(root),
        serialisedDestinations = destinations?.let { serialiseNavKeyList(it) },
        timestamp = timestamp
    )

    constructor(root: MainNavItemNavKey) : this(
        serialisedRoot = serialiseMainNavItemNavKey(root),
        serialisedDestinations = null,
    )


    val root: MainNavItemNavKey?
        get() = serialisedRoot.deserialiseMainNavItemNavKey()

    val destinations: List<NavKey>?
        get() = root?.let {
            serialisedDestinations.deserialiseNavKeyList()
        }

    companion object {
        private fun <T : MainNavItemNavKey> serialiseMainNavItemNavKey(navKey: T): String =
            Json.encodeToString(NavKeySerializer(), navKey)

        private fun <T : NavKey> serialiseNavKeyList(navKeys: List<T>): String =
            Json.encodeToString(ListSerializer(NavKeySerializer()), navKeys)

        private fun String?.deserialiseMainNavItemNavKey(): MainNavItemNavKey? =
            this?.let { Json.decodeFromString(NavKeySerializer(), it) }

        private fun String?.deserialiseNavKeyList(): List<NavKey>? =
            this?.let { Json.decodeFromString(ListSerializer(NavKeySerializer()), it) }
    }
}