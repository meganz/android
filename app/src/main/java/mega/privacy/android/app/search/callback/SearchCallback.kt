package mega.privacy.android.app.search.callback

import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import java.util.ArrayList

/**
 * Class containing callbacks related to search actions.
 */
class SearchCallback {

    /**
     * Callback which contains all data necessary actions to be implemented in a search action.
     */
    interface Data {

        /**
         * Initializes a new search.
         *
         * @return MegaCancelToken to identify and cancel a the search if needed.
         */
        fun initNewSearch(): MegaCancelToken

        /**
         * Cancels a previous search if any.
         */
        fun cancelSearch()
    }

    /**
     * Callback which contains all view necessary actions to be implemented in a search action.
     */
    interface View {

        /**
         * Updates the search progress view.
         *
         * @param inProgress True if should show it, false if should hide it.
         */
        fun updateSearchProgressView(inProgress: Boolean)

        /**
         * Finish a search action when searched nodes have been received.
         *
         * @param searchedNodes Searched nodes.
         */
        fun finishSearch(searchedNodes: ArrayList<MegaNode>)
    }
}