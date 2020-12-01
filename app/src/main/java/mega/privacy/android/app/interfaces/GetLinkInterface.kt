package mega.privacy.android.app.interfaces

import nz.mega.sdk.MegaNode

interface GetLinkInterface {
    fun showFragment(visibleFragment: Int)
    fun getNode(): MegaNode
    fun copyLink(link: String)
    fun copyLinkKey(key: String)
    fun copyLinkPassword()
    fun showUpgradeToProWarning()
    fun getLinkWithPassword(): String?
    fun setPasswordLink(passwordLink: String)
    fun getPasswordLink(): String?
    fun removeLinkWithPassword()
}