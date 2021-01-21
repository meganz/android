package mega.privacy.android.app.interfaces

import nz.mega.sdk.MegaNode

interface GetLinkInterface {

    /**
     * Changes the current shown fragment by a new one.
     *
     * @param visibleFragment The new fragment to show.
     */
    fun showFragment(visibleFragment: Int)

    /**
     * Gets the node from which it wants to get or manage the link.
     *
     * @return The node.
     */
    fun getNode(): MegaNode

    /**
     * Gets the link without its decryption key.
     *
     * @return The link without its decryption key.
     */
    fun getLinkWithoutKey(): String

    /**
     * Gets the decryption key of the link.
     *
     * @return The decryption key of the link.
     */
    fun getLinkKey(): String

    /**
     * Copies a link into clipboard.
     *
     * @param link The link to copy.
     */
    fun copyLink(link: String)

    /**
     * Copies the decryption key of the link into clipboard.
     */
    fun copyLinkKey()

    /**
     * Copies the password protection of the link into clipboard.
     */
    fun copyLinkPassword()

    /**
     * Shows a warning informing the option is not available for free users and suggesting upgrade to pro.
     */
    fun showUpgradeToProWarning()

    /**
     * Gets the link with password protection enabled.
     *
     * @return The link with password protection if set, null otherwise.
     */
    fun getLinkWithPassword(): String?

    /**
     * Sets the password protection of the link.
     *
     * @param passwordLink The password protection.
     */
    fun setLinkPassword(passwordLink: String)

    /**
     * Gets the password protection of the link.
     *
     * @return The password protection of the link if set, null otherwise.
     */
    fun getLinkPassword(): String?

    /**
     * Disables the password protection of the link.
     */
    fun removeLinkWithPassword()
}