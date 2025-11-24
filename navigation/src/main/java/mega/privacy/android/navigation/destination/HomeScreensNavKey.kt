package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class HomeScreensNavKey(
    private val serialisedRoot: String?,
    private val serialisedDestinations: String?,
) : NavKey {

    constructor() : this(
        serialisedRoot = null,
        serialisedDestinations = null,
    )

    constructor(root: NavKey, destinations: List<NavKey>?) : this(
        serialisedRoot = serialiseNavKey(root),
        serialisedDestinations = destinations?.let { serialiseNavKeyList(it) },
    )

    constructor(root: NavKey) : this(
        serialisedRoot = serialiseNavKey(root),
        serialisedDestinations = null,
    )


    val root: NavKey?
        get() = serialisedRoot?.let { Json.decodeFromString(NavKeySerializer(), it) }

    val destinations: List<NavKey>?
        get() = root?.let {
            serialisedDestinations?.let {
                Json.decodeFromString(
                    ListSerializer(NavKeySerializer()),
                    it
                )
            }
        }

    companion object {
        private fun <T : NavKey> serialiseNavKey(navKey: T): String =
            Json.encodeToString(NavKeySerializer(), navKey)

        private fun <T : NavKey> serialiseNavKeyList(navKeys: List<T>): String =
            Json.encodeToString(ListSerializer(NavKeySerializer()), navKeys)
    }
}