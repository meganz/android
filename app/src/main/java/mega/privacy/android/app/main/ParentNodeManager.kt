package mega.privacy.android.app.main

import nz.mega.sdk.MegaNode

interface ParentNodeManager {
    val currentParentHandle: Long
    suspend fun getCurrentParentNode(parentHandle: Long, error: Int): MegaNode?
}