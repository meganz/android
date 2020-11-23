package mega.privacy.android.app.interfaces

import nz.mega.sdk.MegaNode

interface GetLinkInterface {
    fun showFragment(visibleFragment: Int)
    fun getNode(): MegaNode
    fun shareLink(link: String)
    fun copyLink(link: String)
    fun startSetPassword()
    fun setLink(link: String)
}