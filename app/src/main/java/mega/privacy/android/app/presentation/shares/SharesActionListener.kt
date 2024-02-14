package mega.privacy.android.app.presentation.shares

/**
 * Interface that defines what methods the Activity should implement for Shares pages: Incoming, outgoing, and links
 */
interface SharesActionListener {

    /**
     * Exits the shares page
     */
    fun exitSharesPage()

    /**
     * Refreshes the Toolbar Title for Shares page
     *
     * @param invalidateOptionsMenu If true, this invalidates the Options Menu
     */
    fun updateSharesPageToolbarTitleAndFAB(invalidateOptionsMenu: Boolean)
}