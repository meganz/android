package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.NavOptions

/**
 * A navigation request that has been queued instead of being applied to the back stack.
 *
 * A queued request outlives the process it was made in, so the operation that was requested and the
 * options it was made with are stored next to the destinations. Replaying a request that only knew
 * its destinations would silently drop the back stack manipulation the caller asked for.
 */
@Serializable
sealed interface PendingNavigation<T : NavKey> {

    /**
     * Destinations the request navigates to.
     */
    val destinations: List<T>

    /**
     * Options the request was made with, or `null` when it carries none.
     */
    val navOptions: PendingNavOptions?

    /**
     * A plain `navigate` request.
     */
    @Serializable
    data class Navigate<T : NavKey>(
        override val destinations: List<T>,
        override val navOptions: PendingNavOptions?,
    ) : PendingNavigation<T>

    /**
     * A `navigateAndClearBackStack` request. [destinations] holds the single destination the
     * cleared back stack is replaced with.
     */
    @Serializable
    data class ClearBackStack<T : NavKey>(
        override val destinations: List<T>,
        override val navOptions: PendingNavOptions?,
    ) : PendingNavigation<T>

    /**
     * A `navigateAndClearTo` request, popping the back stack to [newParent] before navigating.
     */
    @Serializable
    data class ClearTo<T : NavKey>(
        override val destinations: List<T>,
        val newParent: T,
        val inclusive: Boolean,
        override val navOptions: PendingNavOptions?,
    ) : PendingNavigation<T>
}

/**
 * Serializable form of [NavOptions].
 *
 * [NavOptions.PopUpTo.routeClass] is kept as a class name rather than a `KClass` so that it can be
 * persisted, matching how `NavKeySerializer` persists the keys themselves. Back stack entries are
 * matched against the stored name, which stays consistent under minification because both sides are
 * produced by the same build.
 */
@Serializable
data class PendingNavOptions(
    val launchSingleTop: Boolean,
    val dropIfAlreadyShown: Boolean,
    val popUpTo: PopUpTo?,
) {

    /**
     * Serializable form of [NavOptions.PopUpTo]. A `null` [routeClassName] pops to the root of the
     * back stack.
     */
    @Serializable
    data class PopUpTo(
        val routeClassName: String?,
        val inclusive: Boolean,
        val firstOccurrence: Boolean,
    )
}

/**
 * Whether the request carries no semantics beyond the destinations it navigates to.
 */
val PendingNavigation<*>.isPlainNavigation: Boolean
    get() = this is PendingNavigation.Navigate && navOptions == null

/**
 * Wraps each destination in a plain [PendingNavigation.Navigate] request.
 */
fun <T : NavKey> List<T>.asPendingNavigations(): List<PendingNavigation<T>> =
    map { PendingNavigation.Navigate(listOf(it), navOptions = null) }

/**
 * Returns a copy of the request without the destinations matching [predicate], or `null` when no
 * destination remains.
 */
fun <T : NavKey> PendingNavigation<T>.removeDestinations(
    predicate: (T) -> Boolean,
): PendingNavigation<T>? {
    val remaining = destinations.filterNot(predicate)
    if (remaining.isEmpty()) return null
    if (remaining.size == destinations.size) return this
    return when (this) {
        is PendingNavigation.Navigate -> copy(destinations = remaining)
        is PendingNavigation.ClearBackStack -> copy(destinations = remaining)
        is PendingNavigation.ClearTo -> copy(destinations = remaining)
    }
}

/**
 * Converts [NavOptions] to the form that can be queued and persisted.
 */
fun NavOptions.toPendingNavOptions(): PendingNavOptions = PendingNavOptions(
    launchSingleTop = launchSingleTop,
    dropIfAlreadyShown = dropIfAlreadyShown,
    popUpTo = popUpTo?.let {
        PendingNavOptions.PopUpTo(
            routeClassName = it.routeClass?.java?.name,
            inclusive = it.inclusive,
            firstOccurrence = it.firstOccurrence,
        )
    },
)
