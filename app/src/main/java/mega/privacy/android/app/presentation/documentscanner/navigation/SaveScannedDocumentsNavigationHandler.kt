package mega.privacy.android.app.presentation.documentscanner.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.appstate.content.navigation.PendingBackStack
import mega.privacy.android.navigation.contract.NavOptions
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Simplified [NavigationHandler] for [SaveScannedDocumentsActivity].
 *
 * The activity hosts its own [PendingBackStack] (so the cloud explorer destinations can be
 * reached without depending on the main app shell). Auth/passcode/root-node concerns are not
 * handled here — the activity is launched from screens that already passed those gates.
 */
class SaveScannedDocumentsNavigationHandler(
    private val backStack: PendingBackStack<NavKey>,
    private val navigationResultManager: NavigationResultManager,
    private val onEmptyBackStack: () -> Unit,
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
        backStack.add(destination)
    }

    override fun navigate(destinations: List<NavKey>, navOptions: NavOptions?) {
        backStack.addAll(destinations)
    }

    override fun backTo(destination: NavKey, inclusive: Boolean) {
        val index = backStack.indexOfLast { it == destination }
        if (index == -1) return
        val removeCount = backStack.size - index - if (inclusive) 0 else 1
        if (removeCount <= 0) return
        if (removeCount >= backStack.size) {
            // Don't empty the stack — NavDisplay would crash mid-composition.
            // Just finish the activity; teardown will dispose the existing entries.
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
