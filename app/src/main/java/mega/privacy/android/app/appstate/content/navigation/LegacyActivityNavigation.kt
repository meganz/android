package mega.privacy.android.app.appstate.content.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.navigation.contract.NavOptions
import mega.privacy.android.navigation.contract.NavigationHandler

/** Back stack + handler returned by [rememberLegacyActivityNavigation]. */
data class LegacyActivityNavigation(
    val backStack: PendingBackStack<NavKey>,
    val handler: NavigationHandler,
)

/**
 * [NavigationHandler] for activities that host their own [PendingBackStack] outside the
 * [mega.privacy.android.app.appstate.MegaActivity] single-activity shell. Back-navigation ops
 * that would empty the stack leave it untouched and invoke [onEmptyBackStack] instead, so
 * [androidx.navigation3.ui.NavDisplay] never sees an empty stack mid-composition.
 *
 * @param onEmptyBackStack fires when a back-navigation op would leave the stack empty (back from
 * the root entry, or a `backTo` / `remove` that targets the last remaining destination). The
 * back stack is *not* mutated in that case.
 */
class LegacyActivityNavigationHandler(
    private val backStack: PendingBackStack<NavKey>,
    private val navigationResultManager: NavigationResultManager,
    private val onEmptyBackStack: () -> Unit = {},
) : NavigationHandler {

    override fun back() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        } else {
            onEmptyBackStack()
        }
    }

    override fun remove(navKey: NavKey) {
        if (backStack.size > 1) {
            backStack.remove(navKey)
        } else if (backStack.lastOrNull() == navKey) {
            onEmptyBackStack()
        }
    }

    override fun navigate(destination: NavKey, navOptions: NavOptions?) {
        navigate(listOf(destination), navOptions)
    }

    override fun navigate(destinations: List<NavKey>, navOptions: NavOptions?) {
        if (navOptions?.dropIfAlreadyShown == true && isAlreadyShown(destinations)) {
            return
        }
        applyNavOptions(navOptions, destinations)
        backStack.addAll(destinations)
    }

    private fun isAlreadyShown(destinations: List<NavKey>): Boolean {
        val backStackClasses = backStack.map { it::class }
        return destinations.all { it::class in backStackClasses }
    }

    private fun applyNavOptions(navOptions: NavOptions?, destinations: List<NavKey>) {
        navOptions ?: return
        if (navOptions.launchSingleTop) {
            val top = backStack.takeLast(destinations.size)
            if (top.size == destinations.size &&
                top.zip(destinations).all { (a, b) -> a::class == b::class }
            ) {
                repeat(destinations.size) { backStack.removeLastOrNull() }
            }
        }
        val popUpTo = navOptions.popUpTo ?: return
        val popUpToKey = if (popUpTo.routeClass == null) {
            backStack.firstOrNull()
        } else {
            backStack.lastOrNull { it::class == popUpTo.routeClass }
        } ?: return
        // Pop without triggering onEmptyBackStack: the destinations are added right after.
        val index = backStack.indexOfLast { it == popUpToKey }
        if (index == -1) return
        val removeCount = backStack.size - index - if (popUpTo.inclusive) 0 else 1
        repeat(removeCount.coerceAtLeast(0)) { backStack.removeLastOrNull() }
    }

    override fun backTo(destination: NavKey, inclusive: Boolean) {
        val index = backStack.indexOfLast { it == destination }
        if (index == -1) return
        val removeCount = backStack.size - index - if (inclusive) 0 else 1
        if (removeCount <= 0) return
        if (removeCount >= backStack.size) {
            onEmptyBackStack()
        } else {
            repeat(removeCount) { backStack.removeLastOrNull() }
        }
    }

    override fun navigateAndClearBackStack(destination: NavKey) {
        backStack.clear()
        backStack.add(destination)
    }

    override fun navigateAndClearTo(
        destination: List<NavKey>,
        newParent: NavKey,
        inclusive: Boolean,
    ) {
        backTo(newParent, inclusive)
        backStack.addAll(destination)
    }

    override fun <T> returnResult(key: String, value: T) {
        navigationResultManager.returnResult(key, value)
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    override fun clearResult(key: String) {
        navigationResultManager.clearResult(key)
    }

    override fun <T> monitorResult(key: String): Flow<T?> {
        return navigationResultManager.monitorResult(key)
    }

    override fun clearAllResults() {
        navigationResultManager.clearAllResults()
    }
}

/**
 * Builds a [LegacyActivityNavigation] for legacy-shell activities. The back stack lives in memory
 * for the lifetime of the hosting activity; process-death survival is left to the platform's
 * `savedInstanceState` (via [rememberPendingBackStack]) rather than being mirrored to disk.
 */
@Composable
fun rememberLegacyActivityNavigation(
    initialKey: NavKey,
    navigationResultManager: NavigationResultManager,
    onEmptyBackStack: () -> Unit = {},
): LegacyActivityNavigation {
    val backStack = rememberPendingBackStack(initialKey)
    val handler = remember {
        LegacyActivityNavigationHandler(
            backStack = backStack,
            navigationResultManager = navigationResultManager,
            onEmptyBackStack = onEmptyBackStack,
        )
    }

    return remember(backStack, handler) {
        LegacyActivityNavigation(backStack, handler)
    }
}
