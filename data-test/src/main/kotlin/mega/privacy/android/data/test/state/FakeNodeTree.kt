package mega.privacy.android.data.test.state

import mega.privacy.android.data.test.stub.StubMegaNode
import nz.mega.sdk.MegaNode

/**
 * In-memory node tree backing a fake [mega.privacy.android.data.gateway.api.MegaApiGateway].
 *
 * Seeded with the three account root nodes (Cloud Drive, Rubbish Bin and Vault) so node
 * lookups are coherent out of the box. Tests add [StubMegaNode]s under any parent handle
 * to build the tree they need.
 *
 * The mutating helpers ([rename], [move], [copy], [setFavourite], [setLabel], [moveToRubbish],
 * [remove]) both change the
 * backing state and, through [nodeUpdateSink], broadcast the matching SDK node update in a single
 * call — so tests no longer have to hand-simulate the node-tree change and its `OnNodesUpdate`
 * separately after copy/move/rename/delete. Each emitted node carries the real SDK
 * `MegaNode.CHANGE_TYPE_*` flag the operation would set, mirroring what the SDK broadcasts.
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

    /**
     * Sink for the SDK-style node update produced by each mutating helper.
     *
     * [mega.privacy.android.data.test.gateway.FakeMegaApiGateway] wires this to its global-updates
     * flow so a mutating call both changes the tree and emits the matching
     * `GlobalUpdate.OnNodesUpdate`, just as the real SDK does. Defaults to a no-op so a
     * [FakeNodeTree] used on its own still mutates coherently without an attached gateway.
     */
    var nodeUpdateSink: suspend (List<MegaNode>) -> Unit = {}

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

    /**
     * Rename the node with [handle] to [newName], then broadcast it with `CHANGE_TYPE_NAME`.
     *
     * No-op returning null when [handle] is unknown.
     *
     * @return the renamed node, or null if [handle] is not in the tree.
     */
    suspend fun rename(handle: Long, newName: String): MegaNode? {
        val existing = nodesByHandle[handle] ?: return null
        val parentHandle = parentHandleByHandle.getValue(handle)
        val renamed = existing.copyWith(name = newName, changes = MegaNode.CHANGE_TYPE_NAME.toLong())
        addNode(renamed, parentHandle)
        nodeUpdateSink(listOf(renamed))
        return renamed
    }

    /**
     * Reparent the node with [handle] under [newParentHandle], then broadcast it with
     * `CHANGE_TYPE_PARENT`. The old parent folder is broadcast alongside it (with
     * `CHANGE_TYPE_TIMESTAMP`): consumers that filter updates by folder id — e.g.
     * `MonitorNodeUpdatesByIdUseCase`, which drives the Cloud Drive list — would otherwise never
     * see a node leave the folder they are showing, because the moved node carries only its new
     * parent.
     *
     * No-op returning null when [handle] is unknown or when [newParentHandle] is the node itself
     * or one of its descendants (which would make the node its own ancestor).
     *
     * @return the moved node, or null if the move is not applicable.
     */
    suspend fun move(handle: Long, newParentHandle: Long): MegaNode? {
        val existing = nodesByHandle[handle] ?: return null
        if (isInSubtreeOf(newParentHandle, handle)) return null
        val oldParent = parentHandleByHandle[handle]?.let { nodesByHandle[it] }
        val moved = existing.copyWith(
            parentHandle = newParentHandle,
            changes = MegaNode.CHANGE_TYPE_PARENT.toLong(),
        )
        addNode(moved, newParentHandle)
        val oldParentUpdate = oldParent
            ?.takeIf { it.handle != newParentHandle }
            ?.copyWith(changes = MegaNode.CHANGE_TYPE_TIMESTAMP.toLong())
        nodeUpdateSink(listOfNotNull(moved, oldParentUpdate))
        return moved
    }

    /**
     * Create a shallow copy of the node with [handle] under [newParentHandle] (optionally renamed
     * to [newName]) with a freshly allocated handle, then broadcast it with `CHANGE_TYPE_NEW`.
     *
     * No-op returning null when [handle] is unknown.
     *
     * @return the newly created node, or null if [handle] is not in the tree.
     */
    suspend fun copy(handle: Long, newParentHandle: Long, newName: String? = null): MegaNode? {
        val source = nodesByHandle[handle] ?: return null
        val created = source.copyWith(
            handle = nextHandle(),
            name = newName ?: source.name,
            parentHandle = newParentHandle,
            changes = MegaNode.CHANGE_TYPE_NEW.toLong(),
        )
        addNode(created, newParentHandle)
        nodeUpdateSink(listOf(created))
        return created
    }

    /**
     * Set the favourite flag of the node with [handle] to [favourite], then broadcast it with
     * `CHANGE_TYPE_FAVOURITE`.
     *
     * No-op returning null when [handle] is unknown.
     *
     * @return the updated node, or null if [handle] is not in the tree.
     */
    suspend fun setFavourite(handle: Long, favourite: Boolean): MegaNode? {
        val existing = nodesByHandle[handle] ?: return null
        val updated = existing.copyWith(
            isFavourite = favourite,
            changes = MegaNode.CHANGE_TYPE_FAVOURITE.toLong(),
        )
        addNode(updated, parentHandleByHandle.getValue(handle))
        nodeUpdateSink(listOf(updated))
        return updated
    }

    /**
     * Set the label of the node with [handle] to [label] (a `MegaNode.NODE_LBL_*` constant, 0 to
     * clear it), then broadcast it with `CHANGE_TYPE_ATTRIBUTES`.
     *
     * No-op returning null when [handle] is unknown.
     *
     * @return the updated node, or null if [handle] is not in the tree.
     */
    suspend fun setLabel(handle: Long, label: Int): MegaNode? {
        val existing = nodesByHandle[handle] ?: return null
        val updated = existing.copyWith(
            label = label,
            changes = MegaNode.CHANGE_TYPE_ATTRIBUTES.toLong(),
        )
        addNode(updated, parentHandleByHandle.getValue(handle))
        nodeUpdateSink(listOf(updated))
        return updated
    }

    /**
     * Reparent the node with [handle] to the Rubbish Bin root, then broadcast it with
     * `CHANGE_TYPE_PARENT`.
     *
     * @return the moved node, or null if [handle] is not in the tree.
     */
    suspend fun moveToRubbish(handle: Long): MegaNode? = move(handle, rubbishBinNode.handle)

    /**
     * Remove the node with [handle] (and its descendants) from the tree, then broadcast the node
     * with `CHANGE_TYPE_REMOVED`.
     *
     * No-op returning null when [handle] is unknown.
     *
     * @return the removed node carrying the removed flag, or null if [handle] is not in the tree.
     */
    suspend fun remove(handle: Long): MegaNode? {
        val existing = nodesByHandle[handle] ?: return null
        val removed = existing.copyWith(changes = MegaNode.CHANGE_TYPE_REMOVED.toLong())
        removeNode(handle)
        nodeUpdateSink(listOf(removed))
        return removed
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

    private fun nextHandle(): Long = (nodesByHandle.keys.maxOrNull() ?: 0L) + 1L

    private fun isInSubtreeOf(handle: Long, ancestorHandle: Long): Boolean {
        val visited = mutableSetOf<Long>()
        var current: Long? = handle
        while (current != null && visited.add(current)) {
            if (current == ancestorHandle) return true
            current = parentHandleByHandle[current]
        }
        return false
    }

    private fun MegaNode.copyWith(
        handle: Long = this.handle,
        name: String = this.name,
        parentHandle: Long = this.parentHandle,
        isFavourite: Boolean = this.isFavourite,
        label: Int = this.label,
        changes: Long,
    ): StubMegaNode = StubMegaNode(
        handle = handle,
        name = name,
        parentHandle = parentHandle,
        isFolder = isFolder,
        size = size,
        creationTime = creationTime,
        modificationTime = modificationTime,
        fingerprint = fingerprint,
        originalFingerprint = originalFingerprint,
        label = label,
        duration = duration,
        isFavourite = isFavourite,
        isMarkedSensitive = isMarkedSensitive,
        isExported = isExported,
        isTakenDown = isTakenDown,
        isInShare = isInShare,
        isOutShare = isOutShare,
        publicLink = publicLink,
        description = description,
        owner = owner,
        restoreHandle = restoreHandle,
        publicHandle = publicHandle,
        changes = changes,
    )
}
