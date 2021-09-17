package mega.privacy.android.app.search.callback

import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import java.util.ArrayList

/**
 * Callback which contains all necessary actions to be implemented in a search action.
 */
interface SearchActionsCallback {

    /**
     * Initializes a new search.
     *
     * @return MegaCancelToken to identify and cancel a the search if needed.
     */
    fun initNewSearch(): MegaCancelToken

    /**
     * Updates the search progress view.
     *
     * @param inProgress True if should show it, false if should hide it.
     */
    fun updateSearchProgressView(inProgress: Boolean)

    /**
     * Cancels a previous search.
     */
    fun cancelPreviousSearch()

    /**
     * Finish a search action when searched nodes have been received.
     *
     * @param searchedNodes Searched nodes.
     */
    fun finishSearch(searchedNodes: ArrayList<MegaNode>)
}