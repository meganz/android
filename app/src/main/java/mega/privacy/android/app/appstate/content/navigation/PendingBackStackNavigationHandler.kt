package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.appstate.global.model.RootNodeState
import mega.privacy.android.domain.entity.node.root.RefreshEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.NavigationResultsHandler
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
    private var isConnected: Boolean,
    private val defaultLandingScreen: NavKey,
    private var isPasscodeLocked: Boolean,
    private val passcodeDestination: NavKey,
    private val defaultLoginDestination: NoSessionNavKey,
    initialLoginDestination: NoSessionNavKey,
    private val fetchRootNodeDestination: (session: String, fromLogin: Boolean, RefreshEvent?) -> NavKey,
    private val navigationResultManager: NavigationResultManager,
) : NavigationHandler, NavigationResultsHandler by navigationResultManager {
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
        val topDestination = destinations.last()
        when {
            topDestination is NoSessionNavKey.Mandatory && isLoggedOut().not() -> {
                backstack.pending += destinations
                if (backstack.isEmpty()) navigate(defaultLandingScreen)
            }

            isLoggedOut() && topDestination !is NoSessionNavKey -> {
                backstack.pending += destinations
                if (backstack.isEmpty()) backstack.add(defaultLoginDestination)
            }

            isLoggedInNoRoot() && topDestination !is NoNodeNavKey -> {
                backstack.pending += destinations
                val fetchNodesDestinationOrLogin =
                    currentFetchNodesDestinationOrNull(currentAuthStatus) ?: defaultLoginDestination
                if (backstack.lastOrNull() != fetchNodesDestinationOrLogin) backstack.add(
                    fetchNodesDestinationOrLogin
                )
            }

            isLoggedInWithRoot()
                    && topDestination == currentFetchNodesDestinationOrNull(
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
        navigationResultManager.returnResult(key, value)

        // Navigate back after setting the result
        backstack.removeLastOrNull()
    }

    fun onNetworkChange(isConnected: Boolean) {
        this.isConnected = isConnected
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
            val currentTopDestination = backstack.lastOrNull()
            if (currentTopDestination == null || currentTopDestination::class != fetchNodeOrLoginDestination::class) {
                navigate(listOf(fetchNodeOrLoginDestination))
            }
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
            if (isLoggedInWithRoot()) {
                showPasscodeScreen()
            }
        } else {
            if (backstack.contains(passcodeDestination)) {
                backstack.removeAll { it == passcodeDestination }
                navigateToPendingScreens()
            }
        }
    }

    fun displayDialog(dialogDestination: NavKey) {
        if (backstack.base.isEmpty()) {
            backstack.pending += dialogDestination
        } else {
            navigate(dialogDestination)
        }
    }

    fun peekBackStack(): List<NavKey> = backstack.toList()

    fun dropLast(count: Int) {
        repeat(count) {
            backstack.removeLastOrNull()
        }
    }

    private fun replaceAuthRequiredDestinations(newDestination: NoSessionNavKey): List<NavKey> {
        Timber.d("PendingBackStackNavigationHandler::removeAuthRequiredDestinations")
        val authRequiredDestinations = backstack.takeLastWhile { it !is NoSessionNavKey }

        repeat(authRequiredDestinations.size) {
            backstack.removeLastOrNull()
        }
        if (backstack.isEmpty()) backstack.add(newDestination)
        // show non-required destinations immediately
        backstack.addAll(backstack.pending.filter { it is NoSessionNavKey })
        backstack.pending = backstack.pending.filterNot { it is NoSessionNavKey }
        logBackStack("removeAuthRequiredDestinations")
        return authRequiredDestinations
    }

    private fun removeRootNodeRequiredDestinations(event: RefreshEvent? = null): List<NavKey> {
        Timber.d("PendingBackStackNavigationHandler::removeRootNodeRequiredDestinations")
        val rootNodeRequiredDestinations = backstack.takeLastWhile { it !is NoNodeNavKey }

        repeat(rootNodeRequiredDestinations.size) {
            backstack.removeLastOrNull()
        }

        currentFetchNodesDestinationOrNull(
            currentAuthStatus = currentAuthStatus,
            refreshEvent = event
        )?.let { fetchDestination ->
            if (backstack.lastOrNull() != fetchDestination) backstack.add(fetchDestination)
        }
        logBackStack("removeRootNodeRequiredDestinations")
        return rootNodeRequiredDestinations
    }

    private fun isLoggedOut() = currentAuthStatus.isLoggedIn.not()
    private fun isLoggedInWithRoot() = currentAuthStatus.isLoggedIn && hasRootNode
    private fun isLoggedInNoRoot() = currentAuthStatus.isLoggedIn && hasRootNode.not()

    private fun currentFetchNodesDestinationOrNull(
        currentAuthStatus: AuthStatus,
        refreshEvent: RefreshEvent? = null,
    ) = currentAuthStatus.sessionOrNull()?.let {
        fetchRootNodeDestination(
            it,
            fromLogin,
            refreshEvent
        )
    }?.takeIf { isConnected }

    private fun AuthStatus?.sessionOrNull(): String? =
        (this as? AuthStatus.LoggedIn)?.session

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

    private fun showPasscodeScreen() {
        if (backstack.isNotEmpty() && backstack.last() == passcodeDestination) {
            return
        }
        backstack.pending = backstack + backstack.pending
        backstack.clear()
        navigate(passcodeDestination)
    }

    private fun navigateToPendingScreens() {
        val pending = listOf(defaultLandingScreen).union(backstack.pending)
        backstack.pending = emptyList()
        navigate(pending.toList())
    }

    private fun logBackStack(callingFunction: String) {
        val line = "\n*******************\n"
        Timber.d(
            "Pending backstack :: $callingFunction${line}BackStack:\n\t${printBackStack()}${line}Pending items:\n\t${printPendingItems()}${line}"
        )
    }

    private fun printBackStack(): String = backstack.joinToString("\n\t") { it.toString() }

    private fun printPendingItems(): String =
        backstack.pending.joinToString("\n\t") { it.toString() }

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