package mega.privacy.android.app.presentation.videoplayer.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.appstate.content.navigation.PendingBackStack
import mega.privacy.android.navigation.contract.NavOptions
import mega.privacy.android.navigation.contract.NavigationHandler
import timber.log.Timber

/**
 * Simplified [NavigationHandler] for the video player Activity.
 *
 * Unlike [mega.privacy.android.app.appstate.content.navigation.PendingBackStackNavigationHandler],
 * this handler does not deal with auth/passcode/root-node logic because the video player runs
 * in its own Activity that already went through those checks.
 */
class VideoPlayerNavigationHandler(
    private val backStack: PendingBackStack<NavKey>,
    private val navigationResultManager: NavigationResultManager,
) : NavigationHandler {

    override fun back() {
        Timber.d("VideoPlayerNavigationHandler::back")
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    override fun remove(navKey: NavKey) {
        Timber.d("VideoPlayerNavigationHandler::remove $navKey")
        if (backStack.size > 1) {
            backStack.remove(navKey)
        }
    }

    override fun navigate(destination: NavKey, navOptions: NavOptions?) {
        Timber.d("VideoPlayerNavigationHandler::navigate $destination")
        backStack.add(destination)
    }

    override fun navigate(destinations: List<NavKey>, navOptions: NavOptions?) {
        Timber.d("VideoPlayerNavigationHandler::navigate $destinations")
        backStack.addAll(destinations)
    }

    override fun backTo(destination: NavKey, inclusive: Boolean) {
        val index = backStack.indexOfLast { it == destination }
        if (index == -1) return
        val removeCount = backStack.size - index - if (inclusive) 0 else 1
        val maxRemovable = backStack.size - 1
        repeat(minOf(removeCount, maxRemovable)) { backStack.removeLastOrNull() }
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
