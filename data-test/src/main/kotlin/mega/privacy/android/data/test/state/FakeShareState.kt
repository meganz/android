package mega.privacy.android.data.test.state

import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.data.test.stub.StubMegaShare
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare

/**
 * In-memory share topology backing a fake [mega.privacy.android.data.gateway.api.MegaApiGateway]:
 * incoming shares (a simulated second-account owner sharing a folder with the logged-in user),
 * outgoing shares (a folder of the logged-in user shared to another user) and public links.
 *
 * Seeding helpers ([addIncomingShare], [addOutgoingShare], [addPublicLink]) register the share
 * record and place its node in the [FakeNodeTree] in one call, so the gateway's share reads
 * (`get*IncomingShares`, `getOutgoingSharesNode`, `getOutShares`, `getPublicLinks`,
 * `getUserFromInShare`, `getAccess`, `getContact`) resolve coherently out of the box.
 *
 * The mutating helpers ([removeIncomingShare], [removeOutgoingShare], [setOutgoingShareAccess],
 * [removePublicLink]) both change the share records and broadcast the matching SDK node update
 * (with the real `MegaNode.CHANGE_TYPE_*` flag) through the tree's
 * [FakeNodeTree.nodeUpdateSink] — mirroring what the SDK broadcasts after the corresponding
 * server-side change, so the app's node monitoring refreshes the Shared Items surfaces.
 */
class FakeShareState(private val nodeTree: FakeNodeTree) {

    /**
     * An incoming share: [nodeHandle] is shared to the logged-in user by the owner
     * ([ownerEmail]/[ownerHandle]) with SDK [access] level (`MegaShare.ACCESS_*`).
     */
    data class IncomingShare(
        val ownerEmail: String,
        val ownerHandle: Long,
        val nodeHandle: Long,
        val access: Int = MegaShare.ACCESS_READ,
        val isVerified: Boolean = true,
        val timestamp: Long = 0L,
    )

    /**
     * An outgoing share: the logged-in user's [nodeHandle] is shared to [userEmail]/[userHandle]
     * with SDK [access] level (`MegaShare.ACCESS_*`).
     */
    data class OutgoingShare(
        val nodeHandle: Long,
        val userEmail: String,
        val userHandle: Long,
        val access: Int = MegaShare.ACCESS_READ,
        val isVerified: Boolean = true,
        val isPending: Boolean = false,
        val timestamp: Long = 0L,
    )

    /** Incoming share records; the fake gateway's incoming-share reads resolve through this. */
    val incomingShares = mutableListOf<IncomingShare>()

    /** Outgoing share records; the fake gateway's out-share reads resolve through this. */
    val outgoingShares = mutableListOf<OutgoingShare>()

    /** Handles of publicly linked nodes; `getPublicLinks` resolves them through the node tree. */
    val publicLinkHandles = linkedSetOf<Long>()

    /**
     * Seeds an incoming share: registers the record and places [node] in the tree under the SDK's
     * invalid handle (in-share roots have no parent in the account's own tree).
     *
     * [node] must report `isInShare = true` (e.g. `StubMegaNode(isFolder = true, isInShare = true)`)
     * so the mapped domain node renders as an incoming share.
     */
    fun addIncomingShare(
        node: MegaNode,
        ownerEmail: String,
        ownerHandle: Long,
        access: Int = MegaShare.ACCESS_READ,
        isVerified: Boolean = true,
        timestamp: Long = 0L,
    ) {
        require(node.isInShare) { "Incoming share nodes must be built with isInShare = true" }
        nodeTree.addNode(node, parentHandle = MegaApiJava.INVALID_HANDLE)
        incomingShares += IncomingShare(
            ownerEmail = ownerEmail,
            ownerHandle = ownerHandle,
            nodeHandle = node.handle,
            access = access,
            isVerified = isVerified,
            timestamp = timestamp,
        )
    }

    /**
     * Seeds an outgoing share: registers the record and places [node] in the tree under its own
     * `parentHandle`. Call once per recipient to share the same node with several users.
     *
     * [node] must report `isOutShare = true` (e.g. `StubMegaNode(isFolder = true, isOutShare = true)`)
     * so the mapped domain node renders as shared.
     */
    fun addOutgoingShare(
        node: MegaNode,
        userEmail: String,
        userHandle: Long = userEmail.hashCode().toLong(),
        access: Int = MegaShare.ACCESS_READ,
        isVerified: Boolean = true,
        isPending: Boolean = false,
        timestamp: Long = 0L,
    ) {
        require(node.isOutShare) { "Outgoing share nodes must be built with isOutShare = true" }
        nodeTree.addNode(node, parentHandle = node.parentHandle)
        outgoingShares += OutgoingShare(
            nodeHandle = node.handle,
            userEmail = userEmail,
            userHandle = userHandle,
            access = access,
            isVerified = isVerified,
            isPending = isPending,
            timestamp = timestamp,
        )
    }

    /**
     * Seeds a public link: registers the handle and places [node] in the tree under its own
     * `parentHandle`.
     *
     * [node] must report `isExported = true` and carry a `publicLink` (e.g.
     * `StubMegaNode(isExported = true, publicLink = "https://mega.nz/folder/x#key")`).
     */
    fun addPublicLink(node: MegaNode) {
        require(node.isExported) { "Publicly linked nodes must be built with isExported = true" }
        nodeTree.addNode(node, parentHandle = node.parentHandle)
        publicLinkHandles += node.handle
    }

    /**
     * Removes the incoming share on [nodeHandle] (as when the owner stops sharing or the user
     * leaves the share on the server), removing its node from the tree and broadcasting it with
     * `CHANGE_TYPE_REMOVED`.
     *
     * @return the removed node, or null if no incoming share exists on [nodeHandle].
     */
    suspend fun removeIncomingShare(nodeHandle: Long): MegaNode? {
        if (!incomingShares.removeAll { it.nodeHandle == nodeHandle }) return null
        return nodeTree.remove(nodeHandle)
    }

    /**
     * Removes the outgoing share on [nodeHandle] — for every recipient, or only for [userEmail]
     * when given — and broadcasts the node with `CHANGE_TYPE_OUTSHARE`. The node stays in the
     * tree; when no recipients remain it is replaced with an `isOutShare = false` copy.
     *
     * @return the updated node, or null if no matching outgoing share exists.
     */
    suspend fun removeOutgoingShare(nodeHandle: Long, userEmail: String? = null): MegaNode? {
        val removed = outgoingShares.removeAll {
            it.nodeHandle == nodeHandle && (userEmail == null || it.userEmail == userEmail)
        }
        if (!removed) return null
        val node = nodeTree.nodeByHandle(nodeHandle) ?: return null
        val stillShared = outgoingShares.any { it.nodeHandle == nodeHandle }
        val updated = node.copyWith(
            isOutShare = stillShared,
            changes = MegaNode.CHANGE_TYPE_OUTSHARE.toLong(),
        )
        nodeTree.addNode(updated, updated.parentHandle)
        nodeTree.nodeUpdateSink(listOf(updated))
        return updated
    }

    /**
     * Changes the access level of the outgoing share on [nodeHandle] to [userEmail] (as the SDK
     * does after a successful `setShareAccess`), broadcasting the node with `CHANGE_TYPE_OUTSHARE`
     * so the app re-reads the node's out-shares.
     *
     * @return the shared node, or null if no matching outgoing share exists.
     */
    suspend fun setOutgoingShareAccess(nodeHandle: Long, userEmail: String, access: Int): MegaNode? {
        val index = outgoingShares.indexOfFirst {
            it.nodeHandle == nodeHandle && it.userEmail == userEmail
        }
        if (index < 0) return null
        outgoingShares[index] = outgoingShares[index].copy(access = access)
        val node = nodeTree.nodeByHandle(nodeHandle) ?: return null
        val updated = node.copyWith(changes = MegaNode.CHANGE_TYPE_OUTSHARE.toLong())
        nodeTree.addNode(updated, updated.parentHandle)
        nodeTree.nodeUpdateSink(listOf(updated))
        return updated
    }

    /**
     * Removes the public link on [nodeHandle] (as the SDK does after a successful
     * `disableExport`), replacing the node with a non-exported copy and broadcasting it with
     * `CHANGE_TYPE_PUBLIC_LINK`.
     *
     * @return the updated node, or null if [nodeHandle] is not publicly linked.
     */
    suspend fun removePublicLink(nodeHandle: Long): MegaNode? {
        if (!publicLinkHandles.remove(nodeHandle)) return null
        val node = nodeTree.nodeByHandle(nodeHandle) ?: return null
        val updated = node.copyWith(
            isExported = false,
            publicLink = null,
            changes = MegaNode.CHANGE_TYPE_PUBLIC_LINK.toLong(),
        )
        nodeTree.addNode(updated, updated.parentHandle)
        nodeTree.nodeUpdateSink(listOf(updated))
        return updated
    }

    /** Incoming shares as SDK [MegaShare]s, filtered by verification state. */
    fun incomingMegaShares(verified: Boolean): List<MegaShare> = incomingShares
        .filter { it.isVerified == verified }
        .map {
            StubMegaShare(
                user = it.ownerEmail,
                nodeHandle = it.nodeHandle,
                access = it.access,
                timestamp = it.timestamp,
                isVerified = it.isVerified,
            )
        }

    /** Outgoing shares as SDK [MegaShare]s, optionally restricted to one node. */
    fun outgoingMegaShares(nodeHandle: Long? = null): List<MegaShare> = outgoingShares
        .filter { nodeHandle == null || it.nodeHandle == nodeHandle }
        .map {
            StubMegaShare(
                user = it.userEmail,
                nodeHandle = it.nodeHandle,
                access = it.access,
                timestamp = it.timestamp,
                isPending = it.isPending,
                isVerified = it.isVerified,
            )
        }

    /** The nodes of all incoming shares, resolved through the tree. */
    fun incomingShareNodes(): List<MegaNode> =
        incomingShares.mapNotNull { nodeTree.nodeByHandle(it.nodeHandle) }

    /** The nodes of all public links, resolved through the tree. */
    fun publicLinkNodes(): List<MegaNode> =
        publicLinkHandles.mapNotNull { nodeTree.nodeByHandle(it) }

    /**
     * The incoming share covering [node]: the share on the node itself or, when [recursive], on
     * its nearest in-share ancestor.
     */
    fun incomingShareFor(node: MegaNode, recursive: Boolean = true): IncomingShare? {
        val visited = mutableSetOf<Long>()
        var current: MegaNode? = node
        while (current != null && visited.add(current.handle)) {
            incomingShares.firstOrNull { it.nodeHandle == current?.handle }?.let { return it }
            if (!recursive) return null
            current = nodeTree.nodeByHandle(current.parentHandle)
        }
        return null
    }

    /**
     * Every simulated share peer — incoming-share owners and outgoing-share recipients — as
     * email-to-handle pairs, for contact lookups.
     */
    fun shareUsers(): List<Pair<String, Long>> =
        incomingShares.map { it.ownerEmail to it.ownerHandle } +
                outgoingShares.map { it.userEmail to it.userHandle }

    /** Removes every share record and public link. */
    fun clear() {
        incomingShares.clear()
        outgoingShares.clear()
        publicLinkHandles.clear()
    }

    private fun MegaNode.copyWith(
        isOutShare: Boolean = this.isOutShare,
        isExported: Boolean = this.isExported,
        publicLink: String? = this.publicLink,
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
