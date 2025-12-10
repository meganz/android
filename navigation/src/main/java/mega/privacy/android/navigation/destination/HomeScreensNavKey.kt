package mega.privacy.android.navigation.destination

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey

@Serializable
data object RecentsScreenNavKey : NavKey

@Serializable
data class HomeScreensNavKey(
    private val serialisedRoot: String?,
    private val serialisedDestinations: String?,
) : NavKey {

    constructor() : this(
        serialisedRoot = null,
        serialisedDestinations = null,
    )

    constructor(root: MainNavItemNavKey, destinations: List<NavKey>?) : this(
        serialisedRoot = serialiseMainNavItemNavKey(root),
        serialisedDestinations = destinations?.let { serialiseNavKeyList(it) },
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