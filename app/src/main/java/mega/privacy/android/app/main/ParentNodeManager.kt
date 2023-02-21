package mega.privacy.android.app.main

import nz.mega.sdk.MegaNode

interface ParentNodeManager {
    val currentParentHandle: Long
    fun getCurrentParentNode(parentHandle: Long, error: Int): MegaNode?
}