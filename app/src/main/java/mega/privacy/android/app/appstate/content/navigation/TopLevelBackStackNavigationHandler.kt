package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavOptions
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.NavigationResultsHandler
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
        backStack.topLevelBackStacks.values.forEach {
            it.remove(navKey)
        }
    }

    override fun navigate(destination: NavKey, navOptions: NavOptions?) {
        applyNavOptions(navOptions)
        backStack.add(destination)
    }

    override fun navigate(destinations: List<NavKey>, navOptions: NavOptions?) {
        applyNavOptions(navOptions)
        backStack.addAll(destinations)
    }

    override fun backTo(destination: NavKey, inclusive: Boolean) {
        removeFromBackStackTo(destination, inclusive)
    }

    override fun navigateAndClearBackStack(destination: NavKey) {
        backStack.replaceStack(destination)
    }

    override fun navigateAndClearTo(destination: List<NavKey>, newParent: NavKey, inclusive: Boolean) {
        removeFromBackStackTo(newParent, inclusive)
        backStack.addAll(destination)
    }

    override fun <T> returnResult(key: String, value: T) {
        // Store the result in the NavigationResultManager
        navigationResultManager.returnResult(key, value)

        // Navigate back after setting the result
        backStack.removeLast()
    }

    private fun applyNavOptions(navOptions: NavOptions?) {
        if (navOptions == null) return
        val popUpTo = navOptions.popUpTo ?: return
        val popUpToKey = findPopUpToKey(popUpTo) ?: return
        removeFromBackStackTo(popUpToKey, popUpTo.inclusive)
    }

    private fun findPopUpToKey(popUpTo: NavOptions.PopUpTo): NavKey? =
        backStack.backStack.lastOrNull { it::class == popUpTo.routeClass }

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