package mega.privacy.android.app.fragments.photos

/**
 * TODO: This is a quick solution but not good enough
 * Homepage UI should be more data-driven, that is the
 * data layer is able to get notified for any Node's change, without
 * going through the UI component. The UI just reflects the data change
 * dumbly. In other words, HomepageRefreshable is an UI component (e.g. Fragment)
 * which ManagerActivity can retrieve and notify easily. Yet, a publisher/subscriber
 * solution should be adopted (Livedata, Rx, Event bus or regular callbacks, etc.)
 */
interface HomepageRefreshable {
    /**
     * Refresh list items. Recalculate the visibility of UI elements of each item
     * based on the binding data
     * Call refreshUi if some modes' meta data have been changed (e.g. offline, get link)
     */
    fun refreshUi()

    /**
     * Ask the repo to reload Nodes data by calling MegaApi.
     * Call forceUpdate() if some nodes may have been removed or added
     */
    fun forceUpdate()
}