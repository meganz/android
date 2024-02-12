package mega.privacy.android.app.presentation.shares.outgoing

/**
 * Interface that defines what methods the Activity should implement for [OutgoingSharesComposeFragment]
 */
interface OutgoingSharesActionListener {

    /**
     * Exits the [OutgoingSharesComposeFragment]
     */
    fun exitOutgoingSharesComposeFragment()

    /**
     * Refreshes the Toolbar Title for Cloud Drive
     *
     * @param invalidateOptionsMenu If true, this invalidates the Options Menu
     */
    fun updateOutgoingSharesToolbarTitleAndFAB(invalidateOptionsMenu: Boolean)
}