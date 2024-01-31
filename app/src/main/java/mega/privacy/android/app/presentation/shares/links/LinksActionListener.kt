package mega.privacy.android.app.presentation.shares.links

/**
 * Interface that defines what methods the Activity should implement for [LinksComposeFragment]
 */
interface LinksActionListener {

    /**
     * Exits the [LinksComposeFragment]
     */
    fun exitLinksFragment()

    /**
     * Refreshes the Toolbar Title for Cloud Drive
     *
     * @param invalidateOptionsMenu If true, this invalidates the Options Menu
     */
    fun updateLinksToolbarTitleAndFAB(invalidateOptionsMenu: Boolean)
}