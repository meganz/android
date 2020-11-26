package mega.privacy.android.app.interfaces

import nz.mega.sdk.MegaNode

interface GetLinkInterface {
    fun showFragment(visibleFragment: Int)
    fun getNode(): MegaNode
    fun shareLink(link: String)
    fun copyLinkOrKey(linkOrKey: String, isLink: Boolean)
    fun startSetPassword()
    fun setLink()
    fun exportNode()
}