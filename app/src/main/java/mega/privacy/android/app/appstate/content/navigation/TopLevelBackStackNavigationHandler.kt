package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavOptions
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.NavigationResultsHandler
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey

/**
 * Default implementation of NavigationHandler that wraps back stack functionality.
 */
class TopLevelBackStackNavigationHandler(
    private val backStack: TopLevelBackStack<NavKey, MainNavItemNavKey>,
    private val navigationResultManager: NavigationResultManager,
) : NavigationHandler, NavigationResultsHandler by navigationResultManager {

    override fun back() {
        backStack.removeLast()
    }

    override fun remove(navKey: NavKey) {
        backStack.removeAll { it == navKey }
    }

    override fun navigate(destination: NavKey, navOptions: NavOptions?) {
        navigate(listOf(destination), navOptions)
    }

    override fun navigate(destinations: List<NavKey>, navOptions: NavOptions?) {
        if (navOptions?.dropIfAlreadyShown == true && isAlreadyShown(destinations)) {
            return
        }
        applyNavOptions(navOptions, destinations)
        if (backStack.backStack.takeLast(destinations.size).containsAll(destinations)) {
            return
        }
        val incomingDialogs = destinations.filterIsInstance<DialogNavKey>().toSet()
        if (incomingDialogs.isNotEmpty()) {
            backStack.removeAll { it in incomingDialogs }
        }
        backStack.addAll(destinations)
    }

    override fun backTo(destination: NavKey, inclusive: Boolean) {
        removeFromBackStackTo(destination, inclusive)
    }

    override fun navigateAndClearBackStack(destination: NavKey, navOptions: NavOptions?) {
        applyNavOptions(navOptions, listOf(destination))
        backStack.replaceStack(destination)
    }

    override fun navigateAndClearTo(destination: List<NavKey>, newParent: NavKey, inclusive: Boolean, navOptions: NavOptions?) {
        removeFromBackStackTo(newParent, inclusive)
        applyNavOptions(navOptions, destination)
        backStack.addAll(destination)
    }

    override fun <T> returnResult(key: String, value: T) {
        // Store the result in the NavigationResultManager
        navigationResultManager.returnResult(key, value)

        // Navigate back after setting the result
        backStack.removeLast()
    }

    private fun isAlreadyShown(destinations: List<NavKey>): Boolean {
        val backStackClasses = backStack.backStack.map { it::class }
        return destinations.all { it::class in backStackClasses }
    }

    private fun applyNavOptions(navOptions: NavOptions?, destinations: List<NavKey>) {
        if (navOptions == null) return
        if (navOptions.launchSingleTop) {
            val backStackKeys = backStack.backStack.takeLast(destinations.size)
            if (backStackKeys.size == destinations.size &&
                backStackKeys.zip(destinations).all { (a, b) -> a::class == b::class }
            ) {
                repeat(destinations.size) {
                    backStack.removeLast()
                }
            }
        }
        val popUpTo = navOptions.popUpTo ?: return
        val popUpToKey = findPopUpToKey(popUpTo) ?: return
        removeFromBackStackTo(popUpToKey, popUpTo.inclusive)
    }

    private fun findPopUpToKey(popUpTo: NavOptions.PopUpTo): NavKey? =
        if (popUpTo.routeClass == null) {
            backStack.backStack.firstOrNull()
        } else {
            backStack.backStack.lastOrNull { it::class == popUpTo.routeClass }
        }

    /**
     * Removes elements from the back stack up to the specified destination.
     *
     * @param destination The destination to navigate back to
     * @param inclusive Whether to include the destination in the removal operation
     */
    private fun removeFromBackStackTo(destination: NavKey, inclusive: Boolean) {
        val index = backStack.backStack.indexOfLast { it == destination }
        if (index == -1) return
        val removeCount = backStack.backStack.size - index - if (inclusive) 0 else 1
        if (removeCount <= 0) return

        repeat(removeCount) {
            backStack.removeLast()
        }
    }
}