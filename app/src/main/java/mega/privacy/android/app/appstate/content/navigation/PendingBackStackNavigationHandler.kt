package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.appstate.global.model.RootNodeState
import mega.privacy.android.domain.entity.node.root.RefreshEvent
import mega.privacy.android.navigation.contract.NavOptions
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.NavigationResultsHandler
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.contract.navOptions
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
    private val initialLoginDestination: NoSessionNavKey,
    private val fetchNodeProvider: FetchNodeProvider,
    private val navigationResultManager: NavigationResultManager,
) : NavigationHandler, NavigationResultsHandler by navigationResultManager {

    /**
     * Returns true if [navKey] is the fetch-root-node destination.
     * Exposed so callers (e.g. UI) can branch without depending on concrete destination types.
     */
    fun isFetchNodeDestination(navKey: NavKey): Boolean =
        fetchNodeProvider.isFetchNodeDestination(navKey)

    init {
        Timber.d("PendingBackStackNavigationHandler::init")
        if (hasRootNode.not()) {
            val rootNodeDestinations = removeRootNodeRequiredDestinations()
            backstack.pendingNavigations =
                rootNodeDestinations.asPendingNavigations() + backstack.pendingNavigations
        }
        if (currentAuthStatus.isLoggedIn.not()) {
            val authRequiredDestinations = replaceAuthRequiredDestinations(initialLoginDestination)
            backstack.pendingNavigations =
                authRequiredDestinations.asPendingNavigations() + backstack.pendingNavigations
        }
        ensureNotEmpty()
        onPasscodeStateChanged(isPasscodeLocked)

        logBackStack("init isLoggedIn: ${currentAuthStatus.isLoggedIn} hasRootNode: $hasRootNode")
    }

    override fun back() {
        Timber.d("PendingBackStackNavigationHandler::back")
        backstack.removeLastOrNull()
        if (backstack.isEmpty()) {
            navigate(defaultLandingScreen)
        }
        ensureNotEmpty()
        logBackStack("back")
    }

    override fun remove(navKey: NavKey) {
        Timber.d("PendingBackStackNavigationHandler::remove $navKey")
        backstack.pendingNavigations = backstack.pendingNavigations.mapNotNull { navigation ->
            navigation.removeDestinations { it == navKey }
        }
        backstack.remove(navKey)
        ensureNotEmpty()
    }

    override fun navigate(destination: NavKey, navOptions: NavOptions?) {
        Timber.d("PendingBackStackNavigationHandler::navigate $destination")
        navigate(listOf(destination), navOptions)
    }

    override fun navigate(destinations: List<NavKey>, navOptions: NavOptions?) {
        Timber.d("PendingBackStackNavigationHandler::navigate $destinations")
        performNavigate(destinations, navOptions?.toPendingNavOptions())
    }

    private fun performNavigate(destinations: List<NavKey>, navOptions: PendingNavOptions?) {
        if (navOptions?.dropIfAlreadyShown == true && isAlreadyShown(destinations)) {
            Timber.d("Destinations are already on the backstack, dropping navigation")
            return
        }
        if (deferBehindPasscode(PendingNavigation.Navigate(destinations, navOptions))) return
        applyNavOptions(navOptions, destinations)
        if (backstack.takeLast(destinations.size).containsAll(destinations)) {
            Timber.d("Destinations are already on the backstack")
            return
        }

        val topDestination = destinations.last()
        when {
            topDestination is NoSessionNavKey.Mandatory && isLoggedOut().not() -> {
                queueDestinations(destinations)
                if (backstack.isEmpty()) navigate(defaultLandingScreen)
            }

            isLoggedOut() && topDestination !is NoSessionNavKey -> {
                queueDestinations(destinations)
                if (backstack.isEmpty()) backstack.add(defaultLoginDestination)
            }

            isLoggedInNoRoot() && topDestination !is NoNodeNavKey -> {
                queueDestinations(destinations)
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
                if (destinations.any { it.isHomeScreenKey() }) {
                    backstack.removeAll { it.isHomeScreenKey() }
                }
                val incomingDialogs = destinations.filterIsInstance<DialogNavKey>().toSet()
                if (incomingDialogs.isNotEmpty()) {
                    backstack.removeAll { it in incomingDialogs }
                }
                backstack.addAll(destinations)
            }
        }
        ensureNotEmpty()
        logBackStack("navigate : $destinations")
    }

    override fun backTo(destination: NavKey, inclusive: Boolean) {
        removeFromBackStackTo(destination, inclusive)
        ensureNotEmpty()
    }

    override fun navigateAndClearBackStack(destination: NavKey, navOptions: NavOptions?) {
        performNavigateAndClearBackStack(
            listOf(destination),
            navOptions?.toPendingNavOptions()
        )
    }

    private fun performNavigateAndClearBackStack(
        destinations: List<NavKey>,
        navOptions: PendingNavOptions?,
    ) {
        val navigation = PendingNavigation.ClearBackStack(destinations, navOptions)
        if (deferBehindPasscode(navigation)) return
        backstack.clear()
        performNavigate(destinations, navOptions)
    }

    override fun navigateAndClearTo(
        destination: List<NavKey>,
        newParent: NavKey,
        inclusive: Boolean,
        navOptions: NavOptions?,
    ) {
        performNavigateAndClearTo(
            destination,
            newParent,
            inclusive,
            navOptions?.toPendingNavOptions()
        )
    }

    private fun performNavigateAndClearTo(
        destinations: List<NavKey>,
        newParent: NavKey,
        inclusive: Boolean,
        navOptions: PendingNavOptions?,
    ) {
        val navigation =
            PendingNavigation.ClearTo(destinations, newParent, inclusive, navOptions)
        if (deferBehindPasscode(navigation)) return
        removeFromBackStackTo(newParent, inclusive)
        performNavigate(destinations, navOptions)
    }

    override fun <T> returnResult(key: String, value: T) {
        // Store the result in the NavigationResultManager
        navigationResultManager.returnResult(key, value)

        // Navigate back after setting the result
        backstack.removeLastOrNull()
        ensureNotEmpty()
    }

    fun onNetworkChange(isConnected: Boolean) {
        this.isConnected = isConnected
    }

    fun onLoginChange(authStatus: AuthStatus) {
        Timber.d("PendingBackStackNavigationHandler::onLoginChange")
        if (currentAuthStatus == authStatus) return

        if (!authStatus.isLoggedIn) {
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
        ensureNotEmpty()
    }

    fun onRootNodeChange(rootNodeState: RootNodeState) {
        Timber.d("PendingBackStackNavigationHandler::onRootNodeChange")
        if (this.hasRootNode == rootNodeState.exists) return
        this.hasRootNode = rootNodeState.exists

        if (this.hasRootNode) {
            backstack.removeAll { fetchNodeProvider.isFetchNodeDestination(it) }
            if (isPasscodeLocked) {
                showPasscodeScreen()
            } else {
                navigateToPendingScreens()
            }
        } else {
            removeRootNodeRequiredDestinations(rootNodeState.refreshEvent)
            ensureNotEmpty()
        }
    }

    fun onPasscodeStateChanged(isLocked: Boolean) {
        Timber.d("PendingBackStackNavigationHandler::onPasscodeStateChanged")
        isPasscodeLocked = isLocked

        if (isPasscodeLocked) {
            if (currentAuthStatus.isLoggedIn) {
                showPasscodeScreen()
            }
        } else {
            if (backstack.contains(passcodeDestination)) {
                backstack.removeAll { it == passcodeDestination }
                ensureNotEmpty()
                navigateToPendingScreens()
            }
        }
    }

    fun displayDialog(dialogDestination: NavKey) {
        if (backstack.base.isEmpty()) {
            if (!backstack.pending.contains(dialogDestination)) {
                queueDestinations(listOf(dialogDestination))
            }
        } else {
            // remove duplicated content and add new one at top
            backstack.removeAll { it == dialogDestination }
            navigate(
                dialogDestination,
                navOptions {
                    popUpTo(dialogDestination::class) { inclusive = true }
                }
            )
        }
    }

    private fun replaceAuthRequiredDestinations(newDestination: NoSessionNavKey): List<NavKey> {
        Timber.d("PendingBackStackNavigationHandler::removeAuthRequiredDestinations")
        val authRequiredDestinations = backstack.takeLastWhile { it !is NoSessionNavKey }

        repeat(authRequiredDestinations.size) {
            backstack.removeLastOrNull()
        }
        if (backstack.isEmpty()) backstack.addAll(setOf(initialLoginDestination, newDestination))
        // show non-required destinations immediately
        backstack.addAll(backstack.pending.filter { it is NoSessionNavKey && it !in backstack })
        backstack.pendingNavigations = backstack.pendingNavigations.mapNotNull { navigation ->
            navigation.removeDestinations { it is NoSessionNavKey }
        }
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

    private fun ensureNotEmpty() {
        // Topmost dialog has no non-dialog entry behind it. Covers both the empty
        // backstack (vacuous true) and the all-DialogNavKey case in one check.
        if (backstack.all { it is DialogNavKey }) {
            backstack.add(0, defaultLandingScreen)
        }
    }

    private fun isLoggedOut() = currentAuthStatus.isLoggedIn.not()
    private fun isLoggedInWithRoot() = currentAuthStatus.isLoggedIn && hasRootNode
    private fun isLoggedInNoRoot() = currentAuthStatus.isLoggedIn && hasRootNode.not()

    private fun currentFetchNodesDestinationOrNull(
        currentAuthStatus: AuthStatus,
        refreshEvent: RefreshEvent? = null,
    ) = currentAuthStatus.sessionOrNull()?.let {
        fetchNodeProvider.getDestination(it, refreshEvent)
    }?.takeIf { isConnected }

    private fun AuthStatus?.sessionOrNull(): String? =
        (this as? AuthStatus.LoggedIn)?.session

    private fun isAlreadyShown(destinations: List<NavKey>): Boolean {
        val backStackClasses = (backstack + backstack.pending).map { it::class }
        return destinations.all { it::class in backStackClasses }
    }

    private fun applyNavOptions(navOptions: PendingNavOptions?, destinations: List<NavKey>) {
        if (navOptions == null) return
        if (navOptions.launchSingleTop) {
            val backStackKeys = backstack.takeLast(destinations.size)
            if (backStackKeys.size == destinations.size &&
                backStackKeys.zip(destinations).all { (a, b) -> a::class == b::class }
            ) {
                repeat(destinations.size) {
                    backstack.removeLastOrNull()
                }
            }
        }
        val popUpTo = navOptions.popUpTo ?: return
        val popUpToKey = findPopUpToKey(popUpTo) ?: return
        removeFromBackStackTo(popUpToKey, popUpTo.inclusive)
    }

    private fun findPopUpToKey(popUpTo: PendingNavOptions.PopUpTo): NavKey? {
        val routeClassName = popUpTo.routeClassName ?: return backstack.firstOrNull()
        return if (popUpTo.firstOccurrence) {
            backstack.firstOrNull { it.isOfRoute(routeClassName) }
        } else {
            backstack.lastOrNull { it.isOfRoute(routeClassName) }
        }
    }

    private fun NavKey.isOfRoute(routeClassName: String) = this::class.java.name == routeClassName

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

    /**
     * Queues [destinations] instead of applying them, for destinations that cannot be shown yet
     * because the session or the root node they need is missing. The queue is replayed once the
     * missing state arrives.
     */
    private fun queueDestinations(destinations: List<NavKey>) {
        backstack.pendingNavigations += PendingNavigation.Navigate(destinations, navOptions = null)
    }

    /**
     * Queues [navigation] instead of applying it while the passcode screen is gating the app, and
     * returns whether it was queued.
     *
     * Nothing renders above the top of the back stack, so a destination added now would be shown
     * over a passcode screen that is still locked. The queue is replayed by
     * [onPasscodeStateChanged] once the passcode is unlocked.
     *
     * Gating is keyed on the passcode destination actually being on the back stack rather than on
     * [isPasscodeLocked] alone: the lock survives a logout, and the destinations that replace the
     * passcode screen then - the login flow and the fetch nodes screen - must still be able to
     * navigate.
     */
    private fun deferBehindPasscode(navigation: PendingNavigation<NavKey>): Boolean {
        if (isPasscodeLocked.not() || backstack.contains(passcodeDestination).not()) return false
        backstack.pendingNavigations += navigation
        logBackStack("deferred behind passcode : ${navigation.destinations}")
        return true
    }

    private fun showPasscodeScreen() {
        if (backstack.isNotEmpty() && backstack.last() == passcodeDestination) {
            return
        }
        backstack.add(passcodeDestination)
    }

    /**
     * Replays the queue in the order it was built.
     *
     * The leading run of requests carrying no options is replayed as a single navigation because
     * the branch a navigation takes is decided by its topmost destination: replaying a hierarchy
     * one destination at a time would evaluate intermediate destinations on their own. Everything
     * after that run is replayed as the operation it was requested as, so the back stack
     * manipulation the caller asked for is applied against the back stack as it stands now.
     */
    private fun navigateToPendingScreens() {
        val queued = backstack.pendingNavigations
        backstack.pendingNavigations = emptyList()

        val plain = queued.takeWhile { it.isPlainNavigation }
        val pending = mutableListOf<NavKey>().apply {
            if (backstack.isEmpty()) {
                add(defaultLandingScreen)
            }
        }.union(plain.flatMap { it.destinations })
            .filterNot { it in backstack }
        if (pending.isNotEmpty()) {
            performNavigate(pending.toList(), navOptions = null)
        }
        queued.drop(plain.size).forEach { replay(it) }
    }

    private fun replay(navigation: PendingNavigation<NavKey>) {
        when (navigation) {
            is PendingNavigation.Navigate ->
                performNavigate(navigation.destinations, navigation.navOptions)

            is PendingNavigation.ClearBackStack ->
                performNavigateAndClearBackStack(navigation.destinations, navigation.navOptions)

            is PendingNavigation.ClearTo -> performNavigateAndClearTo(
                navigation.destinations,
                navigation.newParent,
                navigation.inclusive,
                navigation.navOptions
            )
        }
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