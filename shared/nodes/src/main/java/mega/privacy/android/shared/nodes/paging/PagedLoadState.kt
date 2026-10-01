package mega.privacy.android.shared.nodes.paging

/**
 * Load state of a [NodeListPager]
 */
sealed interface PagedLoadState {
    /**
     * The first page has not landed yet, nothing can be shown
     */
    data object FirstPage : PagedLoadState

    /**
     * No request in flight
     */
    data object Idle : PagedLoadState

    /**
     * A page after the first is being loaded, the items so far can be shown
     */
    data object LoadingMore : PagedLoadState

    /**
     * The first page could not be loaded, there are no items to show
     */
    data object FirstPageFailed : PagedLoadState

    /**
     * A page after the first could not be loaded, the items so far stay on screen
     */
    data object PageFailed : PagedLoadState
}
