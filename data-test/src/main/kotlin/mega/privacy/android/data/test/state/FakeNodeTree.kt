package mega.privacy.android.data.test.state

import mega.privacy.android.data.test.stub.StubMegaNode
import nz.mega.sdk.MegaNode

/**
 * In-memory node tree backing a fake [mega.privacy.android.data.gateway.api.MegaApiGateway].
 *
 * Seeded with the three account root nodes (Cloud Drive, Rubbish Bin and Vault) so node
 * lookups are coherent out of the box. Tests add [StubMegaNode]s under any parent handle
 * to build the tree they need.
 */
class FakeNodeTree {

    /** Cloud Drive root, handle 1. */
    val rootNode: MegaNode = StubMegaNode(handle = 1L, name = "Cloud Drive", isFolder = true)

    /** Rubbish Bin root, handle 2. */
    val rubbishBinNode: MegaNode = StubMegaNode(handle = 2L, name = "Rubbish Bin", isFolder = true)

    /** Vault (Backups) root, handle 3. */
    val vaultNode: MegaNode = StubMegaNode(handle = 3L, name = "Vault", isFolder = true)

    private val nodesByHandle = linkedMapOf<Long, MegaNode>()
    private val parentHandleByHandle = linkedMapOf<Long, Long>()

    init {
        seedRoots()
    }

    /** Add (or replace) [node] as a child of [parentHandle]. */
    fun addNode(node: MegaNode, parentHandle: Long) {
        nodesByHandle[node.handle] = node
        parentHandleByHandle[node.handle] = parentHandle
    }

    /** The node with [handle], or null if it is not in the tree. */
    fun nodeByHandle(handle: Long): MegaNode? = nodesByHandle[handle]

    /** Direct children of [parentHandle], in insertion order. */
    fun childrenOf(parentHandle: Long): List<MegaNode> =
        parentHandleByHandle
            .filterValues { it == parentHandle }
            .keys
            .mapNotNull { nodesByHandle[it] }

    /** Remove the node with [handle] and all of its descendants. */
    fun removeNode(handle: Long) {
        childrenOf(handle).forEach { removeNode(it.handle) }
        nodesByHandle.remove(handle)
        parentHandleByHandle.remove(handle)
    }

    /** Restore the tree to just the three seeded root nodes. */
    fun clear() {
        nodesByHandle.clear()
        parentHandleByHandle.clear()
        seedRoots()
    }

    private fun seedRoots() {
        nodesByHandle[rootNode.handle] = rootNode
        nodesByHandle[rubbishBinNode.handle] = rubbishBinNode
        nodesByHandle[vaultNode.handle] = vaultNode
    }
}
