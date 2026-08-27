package mega.privacy.android.data.test.state

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.test.stub.StubMegaNode
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeShareStateTest {

    private lateinit var nodeTree: FakeNodeTree
    private lateinit var underTest: FakeShareState
    private lateinit var emittedNodes: MutableList<List<MegaNode>>

    @BeforeEach
    fun setUp() {
        nodeTree = FakeNodeTree()
        emittedNodes = mutableListOf()
        nodeTree.nodeUpdateSink = { emittedNodes.add(it) }
        underTest = FakeShareState(nodeTree)
    }

    @Test
    fun `test that the share state is empty when created`() {
        assertThat(underTest.incomingShares).isEmpty()
        assertThat(underTest.outgoingShares).isEmpty()
        assertThat(underTest.publicLinkHandles).isEmpty()
        assertThat(underTest.incomingMegaShares(verified = true)).isEmpty()
        assertThat(underTest.incomingMegaShares(verified = false)).isEmpty()
        assertThat(underTest.outgoingMegaShares()).isEmpty()
        assertThat(underTest.publicLinkNodes()).isEmpty()
    }

    @Test
    fun `test that addIncomingShare registers the record and places the node under the invalid handle`() {
        val node = inShareFolder(handle = 10L, name = "shared-in")

        underTest.addIncomingShare(
            node = node,
            ownerEmail = OWNER_EMAIL,
            ownerHandle = OWNER_HANDLE,
            access = MegaShare.ACCESS_READWRITE,
        )

        assertThat(nodeTree.nodeByHandle(10L)).isSameInstanceAs(node)
        assertThat(nodeTree.childrenOf(MegaApiJava.INVALID_HANDLE)).containsExactly(node)
        val share = underTest.incomingShares.single()
        assertThat(share.ownerEmail).isEqualTo(OWNER_EMAIL)
        assertThat(share.ownerHandle).isEqualTo(OWNER_HANDLE)
        assertThat(share.access).isEqualTo(MegaShare.ACCESS_READWRITE)
        assertThat(share.isVerified).isTrue()
    }

    @Test
    fun `test that addIncomingShare rejects nodes without the in-share flag`() {
        assertThrows<IllegalArgumentException> {
            underTest.addIncomingShare(
                node = StubMegaNode(handle = 10L, isFolder = true),
                ownerEmail = OWNER_EMAIL,
                ownerHandle = OWNER_HANDLE,
            )
        }
    }

    @Test
    fun `test that addOutgoingShare registers one record per recipient under the node's own parent`() {
        val node = outShareFolder(handle = 20L, name = "shared-out")

        underTest.addOutgoingShare(node, userEmail = PEER_EMAIL)
        underTest.addOutgoingShare(node, userEmail = "second@mega.nz", access = MegaShare.ACCESS_FULL)

        assertThat(nodeTree.childrenOf(nodeTree.rootNode.handle)).containsExactly(node)
        assertThat(underTest.outgoingShares).hasSize(2)
        assertThat(underTest.outgoingMegaShares(20L).map { it.user })
            .containsExactly(PEER_EMAIL, "second@mega.nz")
    }

    @Test
    fun `test that addOutgoingShare rejects nodes without the out-share flag`() {
        assertThrows<IllegalArgumentException> {
            underTest.addOutgoingShare(
                node = StubMegaNode(handle = 20L, isFolder = true),
                userEmail = PEER_EMAIL,
            )
        }
    }

    @Test
    fun `test that addPublicLink registers the handle and rejects non-exported nodes`() {
        val node = linkedFile(handle = 30L, name = "linked.txt")

        underTest.addPublicLink(node)

        assertThat(underTest.publicLinkHandles).containsExactly(30L)
        assertThat(underTest.publicLinkNodes()).containsExactly(node)
        assertThrows<IllegalArgumentException> {
            underTest.addPublicLink(StubMegaNode(handle = 31L))
        }
    }

    @Test
    fun `test that removeIncomingShare removes the record and node and broadcasts CHANGE_TYPE_REMOVED`() =
        runTest {
            underTest.addIncomingShare(
                node = inShareFolder(handle = 10L, name = "shared-in"),
                ownerEmail = OWNER_EMAIL,
                ownerHandle = OWNER_HANDLE,
            )

            val removed = underTest.removeIncomingShare(10L)

            assertThat(underTest.incomingShares).isEmpty()
            assertThat(nodeTree.nodeByHandle(10L)).isNull()
            assertThat(removed?.hasChanged(MegaNode.CHANGE_TYPE_REMOVED.toLong())).isTrue()
            assertThat(emittedNodes.single().single().handle).isEqualTo(10L)
        }

    @Test
    fun `test that removeIncomingShare is a no-op returning null when the handle is not shared`() =
        runTest {
            assertThat(underTest.removeIncomingShare(99L)).isNull()
            assertThat(emittedNodes).isEmpty()
        }

    @Test
    fun `test that removeOutgoingShare clears the out-share flag and broadcasts CHANGE_TYPE_OUTSHARE when the last recipient is removed`() =
        runTest {
            underTest.addOutgoingShare(outShareFolder(handle = 20L, name = "out"), PEER_EMAIL)

            val updated = underTest.removeOutgoingShare(20L)

            assertThat(underTest.outgoingShares).isEmpty()
            assertThat(nodeTree.nodeByHandle(20L)?.isOutShare).isFalse()
            assertThat(updated?.hasChanged(MegaNode.CHANGE_TYPE_OUTSHARE.toLong())).isTrue()
            assertThat(emittedNodes.single().single().handle).isEqualTo(20L)
        }

    @Test
    fun `test that removeOutgoingShare keeps the out-share flag when another recipient remains`() =
        runTest {
            val node = outShareFolder(handle = 20L, name = "out")
            underTest.addOutgoingShare(node, PEER_EMAIL)
            underTest.addOutgoingShare(node, "second@mega.nz")

            underTest.removeOutgoingShare(20L, userEmail = PEER_EMAIL)

            assertThat(underTest.outgoingShares.single().userEmail).isEqualTo("second@mega.nz")
            assertThat(nodeTree.nodeByHandle(20L)?.isOutShare).isTrue()
        }

    @Test
    fun `test that setOutgoingShareAccess updates the record and broadcasts CHANGE_TYPE_OUTSHARE`() =
        runTest {
            underTest.addOutgoingShare(
                node = outShareFolder(handle = 20L, name = "out"),
                userEmail = PEER_EMAIL,
                access = MegaShare.ACCESS_READ,
            )

            val updated =
                underTest.setOutgoingShareAccess(20L, PEER_EMAIL, MegaShare.ACCESS_FULL)

            assertThat(underTest.outgoingShares.single().access).isEqualTo(MegaShare.ACCESS_FULL)
            assertThat(updated?.hasChanged(MegaNode.CHANGE_TYPE_OUTSHARE.toLong())).isTrue()
            assertThat(nodeTree.nodeByHandle(20L)?.isOutShare).isTrue()
            assertThat(emittedNodes.single().single().handle).isEqualTo(20L)
        }

    @Test
    fun `test that setOutgoingShareAccess is a no-op returning null when no matching share exists`() =
        runTest {
            assertThat(underTest.setOutgoingShareAccess(99L, PEER_EMAIL, MegaShare.ACCESS_FULL))
                .isNull()
            assertThat(emittedNodes).isEmpty()
        }

    @Test
    fun `test that removePublicLink replaces the node with a non-exported copy and broadcasts CHANGE_TYPE_PUBLIC_LINK`() =
        runTest {
            underTest.addPublicLink(linkedFile(handle = 30L, name = "linked.txt"))

            val updated = underTest.removePublicLink(30L)

            assertThat(underTest.publicLinkHandles).isEmpty()
            assertThat(nodeTree.nodeByHandle(30L)?.isExported).isFalse()
            assertThat(nodeTree.nodeByHandle(30L)?.publicLink).isNull()
            assertThat(updated?.hasChanged(MegaNode.CHANGE_TYPE_PUBLIC_LINK.toLong())).isTrue()
            assertThat(emittedNodes.single().single().handle).isEqualTo(30L)
        }

    @Test
    fun `test that incomingShareFor resolves the share of a descendant only when recursive`() {
        val shareRoot = inShareFolder(handle = 10L, name = "shared-in")
        underTest.addIncomingShare(shareRoot, OWNER_EMAIL, OWNER_HANDLE)
        val child = StubMegaNode(handle = 11L, name = "child.txt", parentHandle = 10L)
        nodeTree.addNode(child, parentHandle = 10L)

        assertThat(underTest.incomingShareFor(shareRoot, recursive = false)?.ownerEmail)
            .isEqualTo(OWNER_EMAIL)
        assertThat(underTest.incomingShareFor(child, recursive = true)?.ownerEmail)
            .isEqualTo(OWNER_EMAIL)
        assertThat(underTest.incomingShareFor(child, recursive = false)).isNull()
    }

    @Test
    fun `test that clear removes every share record and public link`() {
        underTest.addIncomingShare(inShareFolder(10L, "in"), OWNER_EMAIL, OWNER_HANDLE)
        underTest.addOutgoingShare(outShareFolder(20L, "out"), PEER_EMAIL)
        underTest.addPublicLink(linkedFile(30L, "linked.txt"))

        underTest.clear()

        assertThat(underTest.incomingShares).isEmpty()
        assertThat(underTest.outgoingShares).isEmpty()
        assertThat(underTest.publicLinkHandles).isEmpty()
    }

    private fun inShareFolder(handle: Long, name: String) = StubMegaNode(
        handle = handle,
        name = name,
        parentHandle = MegaApiJava.INVALID_HANDLE,
        isFolder = true,
        isInShare = true,
    )

    private fun outShareFolder(handle: Long, name: String) = StubMegaNode(
        handle = handle,
        name = name,
        parentHandle = nodeTree.rootNode.handle,
        isFolder = true,
        isOutShare = true,
    )

    private fun linkedFile(handle: Long, name: String) = StubMegaNode(
        handle = handle,
        name = name,
        parentHandle = nodeTree.rootNode.handle,
        isExported = true,
        publicLink = "https://mega.nz/file/link#key",
    )

    private companion object {
        const val OWNER_EMAIL = "owner@mega.nz"
        const val OWNER_HANDLE = 222L
        const val PEER_EMAIL = "peer@mega.nz"
    }
}
