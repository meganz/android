package mega.privacy.android.app.interfaces

import nz.mega.sdk.MegaNode

interface GetLinkInterface {
    fun showFragment(visibleFragment: Int)
    fun getNode(): MegaNode
    fun copyLinkOrKey(linkOrKey: String, isLink: Boolean)
    fun startSetPassword()
    fun showUpgradeToProWarning()
}