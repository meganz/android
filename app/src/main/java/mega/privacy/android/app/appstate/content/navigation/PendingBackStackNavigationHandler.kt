package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.appstate.global.model.RefreshEvent
import mega.privacy.android.app.appstate.global.model.RootNodeState
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.navkey.NoNodeNavKey
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey
import timber.log.Timber

/**
 * NavigationHandler implementation that wraps pending back stack functionality.
 */
class PendingBackStackNavigationHandler(
    private val backstack: PendingBackStack<NavKey>,
    private var currentAuthStatus: AuthStatus,
    private var hasRootNode: Boolean,
    private val defaultLandingScreen: NavKey,
    private var isPasscodeLocked: Boolean,
    private val passcodeDestination: NavKey,
    private val defaultLoginDestination: NoSessionNavKey,
    initialLoginDestination: NoSessionNavKey,
    private val fetchRootNodeDestination: (session: String, fromLogin: Boolean, RefreshEvent?) -> NavKey,
    private val navigationResultManager: NavigationResultManager,
) : NavigationHandler {
    private var fromLogin = false

    init {
        Timber.d("PendingBackStackNavigationHandler::init")
        if (hasRootNode.not()) {
            val rootNodeDestinations = removeRootNodeRequiredDestinations()
            backstack.pending = rootNodeDestinations + backstack.pending
        }
        if (currentAuthStatus.isLoggedIn.not()) {
            val authRequiredDestinations = replaceAuthRequiredDestinations(initialLoginDestination)
            backstack.pending = authRequiredDestinations + backstack.pending
        }
        if (backstack.isEmpty()) backstack.add(defaultLandingScreen)
        onPasscodeStateChanged(isPasscodeLocked)

        logBackStack("init isLoggedIn: ${currentAuthStatus.isLoggedIn} hasRootNode: $hasRootNode")
    }

    private fun replaceAuthRequiredDestinations(newDestination: NoSessionNavKey): List<NavKey> {
        Timber.d("PendingBackStackNavigationHandler::removeAuthRequiredDestinations")
        val authRequiredDestinations = backstack.takeLastWhile { it !is NoSessionNavKey }

        repeat(authRequiredDestinations.size) {
            backstack.removeLastOrNull()
        }
        if (backstack.isEmpty()) backstack.add(newDestination)
        logBackStack("removeAuthRequiredDestinations")
        return authRequiredDestinations
    }

    private fun removeRootNodeRequiredDestinations(event: RefreshEvent? = null): List<NavKey> {
        Timber.d("PendingBackStackNavigationHandler::removeRootNodeRequiredDestinations")
        val rootNodeRequiredDestinations = backstack.takeLastWhile { it !is NoNodeNavKey }

        repeat(rootNodeRequiredDestinations.size) {
            backstack.removeLastOrNull()
        }

        (currentAuthStatus as? AuthStatus.LoggedIn)?.session?.let {
            val fetchDestination = fetchRootNodeDestination(it, fromLogin, event)
            if (backstack.lastOrNull() != fetchDestination) backstack.add(fetchDestination)
        }
        logBackStack("removeRootNodeRequiredDestinations")
        return rootNodeRequiredDestinations
    }

    override fun back() {
        Timber.d("PendingBackStackNavigationHandler::back")
        if (backstack.size == 1 && backstack.last() != defaultLandingScreen) {
            backstack.removeLastOrNull()
            navigate(defaultLandingScreen)
        } else {
            backstack.removeLastOrNull()
        }
        logBackStack("back")
    }

    override fun remove(navKey: NavKey) {
        Timber.d("PendingBackStackNavigationHandler::remove $navKey")
        backstack.pending = backstack.pending.filterNot { it == navKey }
        backstack.remove(navKey)
    }

    override fun navigate(destination: NavKey) {
        Timber.d("PendingBackStackNavigationHandler::navigate $destination")
        if (destination === backstack.lastOrNull()) {
            Timber.d("Destination is already on the backstack")
            return
        }
        navigate(listOf(destination))
    }

    override fun navigate(destinations: List<NavKey>) {
        Timber.d("PendingBackStackNavigationHandler::navigate $destinations")
        when {
            destinations.last() is NoSessionNavKey.Mandatory && currentAuthStatus.isLoggedIn -> {
                if (backstack.isEmpty()) navigate(defaultLandingScreen)
            }

            currentAuthStatus.isLoggedIn.not() && destinations.last() !is NoSessionNavKey -> {
                backstack.pending += destinations
                if (backstack.isEmpty()) backstack.add(defaultLoginDestination)
            }

            currentAuthStatus is AuthStatus.LoggedIn && destinations.last() !is NoNodeNavKey && hasRootNode.not() -> {
                backstack.pending += destinations
                val fetchNodesDestinationOrLogin =
                    currentFetchNodesDestinationOrNull(currentAuthStatus) ?: defaultLoginDestination
                if (backstack.lastOrNull() != fetchNodesDestinationOrLogin) backstack.add(
                    fetchNodesDestinationOrLogin
                )
            }

            currentAuthStatus is AuthStatus.LoggedIn && destinations.last() is NoSessionNavKey.Mandatory -> {
                navigate(defaultLandingScreen)
            }

            currentAuthStatus is AuthStatus.LoggedIn
                    && hasRootNode
                    && destinations.last() == currentFetchNodesDestinationOrNull(
                currentAuthStatus
            ) -> {
                navigate(defaultLandingScreen)
            }

            else -> {
                backstack.addAll(destinations)
            }
        }
        logBackStack("navigate : $destinations")
    }

    private fun currentFetchNodesDestinationOrNull(currentAuthStatus: AuthStatus) =
        (currentAuthStatus as? AuthStatus.LoggedIn)?.session?.let {
            fetchRootNodeDestination(
                it,
                fromLogin,
                null
            )
        }

    override fun backTo(destination: NavKey, inclusive: Boolean) {
        removeFromBackStackTo(destination, inclusive)
    }

    override fun navigateAndClearBackStack(destination: NavKey) {
        backstack.clear()
        navigate(destination)
    }

    override fun navigateAndClearTo(destination: NavKey, newParent: NavKey, inclusive: Boolean) {
        removeFromBackStackTo(newParent, inclusive)
        navigate(destination)
    }

    override fun <T> returnResult(key: String, value: T) {
        // Store the result in the NavigationResultManager
        navigationResultManager.setResult(key, value)

        // Navigate back after setting the result
        backstack.removeLastOrNull()
    }

    override fun <T> monitorResult(key: String): Flow<T?> {
        return navigationResultManager.monitorResult(key)
    }

    /**
     * Clears the result for a specific key after it has been consumed.
     * This helps prevent memory leaks and ensures results are only consumed once.
     *
     * @param key The key to clear the result for
     */
    override fun clearResult(key: String) {
        navigationResultManager.clearResult(key)
    }

    /**
     * Clears all stored results. Useful for cleanup or when starting fresh.
     */
    fun clearAllResults() {
        navigationResultManager.clearAllResults()
    }

    /**
     * Removes elements from the back stack up to the specified destination.
     *
     * @param destination The destination to navigate back to
     * @param inclusive Whether to include the destination in the removal operation
     */
    private fun removeFromBackStackTo(destination: NavKey, inclusive: Boolean) {
        val index = backstack.indexOfLast { it == destination }
        if (index == -1) return
        val removeCount = backstack.size - index - if (inclusive) 0 else 1
        if (removeCount <= 0) return

        repeat(removeCount) {
            backstack.removeLastOrNull()
        }
    }

    fun onLoginChange(authStatus: AuthStatus) {
        Timber.d("PendingBackStackNavigationHandler::onLoginChange")
        if (currentAuthStatus == authStatus) return

        if (authStatus.isLoggedIn) {
            fromLogin = true
        } else {
            hasRootNode = false
        }

        this.currentAuthStatus = authStatus

        if (currentAuthStatus.isLoggedIn) {
            backstack.removeAll { it is NoSessionNavKey }
            val fetchNodeOrLoginDestination =
                currentFetchNodesDestinationOrNull(currentAuthStatus) ?: defaultLoginDestination
            navigate(listOf(fetchNodeOrLoginDestination))
        } else {
            replaceAuthRequiredDestinations(defaultLoginDestination)
        }
    }

    fun onRootNodeChange(rootNodeState: RootNodeState) {
        Timber.d("PendingBackStackNavigationHandler::onRootNodeChange")
        if (this.hasRootNode == rootNodeState.exists) return
        this.hasRootNode = rootNodeState.exists

        if (this.hasRootNode) {
            backstack.removeAll { it is NoNodeNavKey }
            if (isPasscodeLocked) {
                showPasscodeScreen()
            } else {
                navigateToPendingScreens()
            }
        } else {
            removeRootNodeRequiredDestinations(rootNodeState.refreshEvent)
        }
    }

    fun onPasscodeStateChanged(isLocked: Boolean) {
        Timber.d("PendingBackStackNavigationHandler::onPasscodeStateChanged")
        isPasscodeLocked = isLocked

        if (isPasscodeLocked) {
            if (currentAuthStatus.isLoggedIn && hasRootNode) {
                showPasscodeScreen()
            }
        } else {
            if (backstack.contains(passcodeDestination)) {
                backstack.removeAll { it == passcodeDestination }
                navigateToPendingScreens()
            }
        }
    }

    private fun showPasscodeScreen() {
        backstack.pending = backstack + backstack.pending
        backstack.clear()
        navigate(passcodeDestination)
    }

    private fun navigateToPendingScreens() {
        val pending = listOf(defaultLandingScreen).union(backstack.pending)
        backstack.pending = emptyList()
        navigate(pending.toList())
    }

    fun displayDialog(dialogDestination: NavKey) {
        if (backstack.pending.isNotEmpty() || backstack.base.isEmpty()) {
            backstack.pending = backstack.pending + dialogDestination
        } else {
            navigate(dialogDestination)
        }
    }

    private fun logBackStack(callingFunction: String) {
        val line = "\n*******************\n"
        Timber.d(
            "Pending backstack :: $callingFunction${line}BackStack:\n\t${printBackStack()}${line}Pending items:\n\t${printPendingItems()}${line}"
        )
    }

    private fun printBackStack(): String = backstack.joinToString("\n\t") { it.toString() }

    private fun printPendingItems(): String = backstack.pending.joinToString("\n\t") { it.toString() }

    sealed interface AuthStatus {
        val isLoggedIn: Boolean

        data class LoggedIn(val session: String) :
            AuthStatus {
            override val isLoggedIn: Boolean = true
        }

        data object NotLoggedIn :
            AuthStatus {
            override val isLoggedIn: Boolean = false
        }
    }
}