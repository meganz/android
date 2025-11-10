package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mega.privacy.android.app.appstate.content.destinations.FetchingContentNavKey
import mega.privacy.android.app.presentation.login.LoginNavKey
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
    private val defaultLoginDestination: NavKey = LoginNavKey,
    private val fetchRootNodeDestination: (session: String, fromLogin: Boolean) -> NavKey = ::FetchingContentNavKey,
) : NavigationHandler {
    private val resultFlows = mutableMapOf<String, MutableStateFlow<Any?>>()
    private var fromLogin = false

    init {
        Timber.d("PendingBackStackNavigationHandler::init")
        if (hasRootNode.not()) {
            val rootNodeDestinations = removeRootNodeRequiredDestinations()
            backstack.pending = rootNodeDestinations + backstack.pending
        }
        if (currentAuthStatus.isLoggedIn.not()) {
            val authRequiredDestinations = removeAuthRequiredDestinations()
            backstack.pending = authRequiredDestinations + backstack.pending
            if (backstack.isEmpty()) backstack.add(defaultLoginDestination)
        }
        if (backstack.isEmpty()) backstack.add(defaultLandingScreen)
        onPasscodeStateChanged(isPasscodeLocked)

        logBackStack()
    }

    private fun removeAuthRequiredDestinations(): List<NavKey> {
        Timber.d("PendingBackStackNavigationHandler::removeAuthRequiredDestinations")
        val authRequiredDestinations = backstack.takeLastWhile { it !is NoSessionNavKey }

        repeat(authRequiredDestinations.size) {
            backstack.removeLastOrNull()
        }
        logBackStack()
        return authRequiredDestinations
    }

    private fun removeRootNodeRequiredDestinations(): List<NavKey> {
        Timber.d("PendingBackStackNavigationHandler::removeRootNodeRequiredDestinations")
        val rootNodeRequiredDestinations = backstack.takeLastWhile { it !is NoNodeNavKey }

        repeat(rootNodeRequiredDestinations.size) {
            backstack.removeLastOrNull()
        }

        (currentAuthStatus as? AuthStatus.LoggedIn)?.session?.let {
            val fetchDestination = fetchRootNodeDestination(it, fromLogin)
            if (backstack.lastOrNull() != fetchDestination) backstack.add(fetchDestination)
        }
        logBackStack()
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
        logBackStack()
    }

    override fun navigate(destination: NavKey) {
        navigate(listOf(destination))
    }

    override fun navigate(destinations: List<NavKey>) {
        Timber.d("PendingBackStackNavigationHandler::navigate")
        when {
            destinations.last() is NoSessionNavKey.Mandatory && currentAuthStatus.isLoggedIn -> {
                if (backstack.isEmpty()) navigate(defaultLandingScreen)
            }

            currentAuthStatus.isLoggedIn.not() && destinations.last() !is NoSessionNavKey -> {
                backstack.pending = destinations
                if (backstack.isEmpty()) backstack.add(defaultLoginDestination)
            }

            currentAuthStatus is AuthStatus.LoggedIn && destinations.last() !is NoNodeNavKey && hasRootNode.not() -> {
                backstack.pending = destinations
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
            )
                -> {
                navigate(defaultLandingScreen)
            }

            else -> {
                backstack.addAll(destinations)
            }
        }
        logBackStack()
    }

    private fun currentFetchNodesDestinationOrNull(currentAuthStatus: AuthStatus) =
        (currentAuthStatus as? AuthStatus.LoggedIn)?.session?.let {
            fetchRootNodeDestination(
                it,
                fromLogin
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
        // Store the result in the appropriate StateFlow
        val resultFlow = resultFlows.getOrPut(key) { MutableStateFlow(null) }
        resultFlow.value = value

        // Navigate back after setting the result
        backstack.removeLastOrNull()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> monitorResult(key: String): Flow<T?> {
        // Get or create the StateFlow for this key
        val resultFlow = resultFlows.getOrPut(key) { MutableStateFlow(null) }
        return resultFlow.asStateFlow() as StateFlow<T?>
    }

    /**
     * Clears the result for a specific key after it has been consumed.
     * This helps prevent memory leaks and ensures results are only consumed once.
     *
     * @param key The key to clear the result for
     */
    override fun clearResult(key: String) {
        resultFlows[key]?.value = null
    }

    /**
     * Clears all stored results. Useful for cleanup or when starting fresh.
     */
    fun clearAllResults() {
        resultFlows.values.forEach { it.value = null }
        resultFlows.clear()
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
        if (authStatus.isLoggedIn) {
            fromLogin = true
        } else {
            hasRootNode = false
        }

        if (currentAuthStatus == authStatus) return
        this.currentAuthStatus = authStatus

        if (currentAuthStatus.isLoggedIn) {
            backstack.removeAll { it is NoSessionNavKey }
            val fetchNodeOrLoginDestination =
                currentFetchNodesDestinationOrNull(currentAuthStatus) ?: defaultLoginDestination
            navigate(listOf(fetchNodeOrLoginDestination))
        } else {
            removeAuthRequiredDestinations()
            if (backstack.isEmpty()) backstack.add(defaultLoginDestination)
        }
    }

    fun onRootNodeChange(hasRootNode: Boolean) {
        Timber.d("PendingBackStackNavigationHandler::onRootNodeChange")
        if (this.hasRootNode == hasRootNode) return
        this.hasRootNode = hasRootNode

        if (this.hasRootNode) {
            backstack.removeAll { it is NoNodeNavKey }
            val pending =
                backstack.pending.takeUnless { it.isEmpty() } ?: listOf(defaultLandingScreen)
            navigate(pending)
            backstack.pending = emptyList()
        } else {
            removeRootNodeRequiredDestinations()
        }
    }

    fun onPasscodeStateChanged(isLocked: Boolean) {
        Timber.d("PendingBackStackNavigationHandler::onPasscodeStateChanged")
        isPasscodeLocked = isLocked

        if (isPasscodeLocked) {
            backstack.pending = backstack + backstack.pending
            backstack.clear()
            navigate(passcodeDestination)
        } else {
            if (backstack.contains(passcodeDestination)) {
                backstack.removeAll { it == passcodeDestination }
                val pendingDestinations = backstack.pending
                backstack.pending = emptyList()
                navigate(pendingDestinations)
            }
        }
    }

    fun displayDialog(dialogDestination: NavKey) {
        if (backstack.pending.isNotEmpty()) {
            backstack.pending = backstack.pending + dialogDestination
        } else {
            navigate(dialogDestination)
        }
    }

    private fun logBackStack() {
        Timber.d(
            "Pending backstack: \n BackStack: \n ${backstack.joinToString("\n") { it.toString() }} \n Pending: \n ${
                backstack.pending.joinToString(
                    "\n"
                ) { it.toString() }
            }"
        )
    }

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