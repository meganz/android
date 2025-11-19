package mega.privacy.android.app.appstate.content.destinations

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import mega.privacy.android.app.appstate.content.navigation.view.HomeScreens
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

@Serializable
data class HomeScreensNavKey(
    private val serialisedRoot: String?,
    private val serialisedDestinations: String?,
) : NavKey {

    constructor() : this(
        serialisedRoot = null,
        serialisedDestinations = null,
    )

    constructor(root: NavKey, destinations: List<NavKey>) : this(
        serialisedRoot = serialiseNavKey(root),
        serialisedDestinations = serialiseNavKeyList(destinations),
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

fun EntryProviderScope<NavKey>.homeScreens(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<HomeScreensNavKey> { key ->
        HomeScreens(
            transferHandler = transferHandler,
            outerNavigationHandler = navigationHandler,
            initialDestination = key.root?.let { it to key.destinations },
        )
    }
}
