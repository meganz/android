package mega.privacy.android.app.appstate.content.navigation

import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.node.root.RefreshEvent

/**
 * Provides the "fetch root node" destination and identifies whether a [NavKey] is that destination.
 * Used to decouple navigation logic from concrete destination types.
 */
interface FetchNodeProvider {

    /**
     * Sets whether the login is done by account credentials.
     */
    fun setLoginByAccount()

    /**
     * Clears the login by account flag.
     */
    fun clearLoginByAccount()

    /**
     * Returns the NavKey for the fetch-root-node screen for the given session and optional refresh event.
     */
    fun getDestination(session: String, refreshEvent: RefreshEvent?): NavKey

    /**
     * Returns true if [navKey] represents the fetch-root-node destination.
     */
    fun isFetchNodeDestination(navKey: NavKey): Boolean
}
