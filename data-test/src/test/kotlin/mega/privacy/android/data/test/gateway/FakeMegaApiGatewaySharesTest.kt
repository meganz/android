package mega.privacy.android.data.test.gateway

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.data.test.stub.StubMegaShare
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Documents how [FakeMegaApiGateway]'s share and public-link reads resolve through the seeded
 * [FakeMegaApiGateway.shares] state, and how its mutating helpers broadcast the matching
 * `OnNodesUpdate` — the surface the Shared Items screens (Incoming/Outgoing/Links) drive.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaApiGatewaySharesTest {

    private lateinit var underTest: FakeMegaApiGateway

    @BeforeEach
    fun setUp() {
        underTest = FakeMegaApiGateway()
    }

    @Test
    fun `test that share reads return empty success when nothing is seeded`() = runTest {
        assertThat(underTest.getUnverifiedIncomingShares(ORDER)).isEmpty()
        assertThat(underTest.getVerifiedIncomingShares(ORDER)).isEmpty()
        assertThat(underTest.getIncomingSharesNode(ORDER)).isEmpty()
        assertThat(underTest.getInShares(ORDER)).isEmpty()
        assertThat(underTest.getOutgoingSharesNode(ORDER)).isEmpty()
        assertThat(underTest.getOutShares(ORDER)).isEmpty()
        assertThat(underTest.getOutShares(StubMegaNode(handle = 5L))).isEmpty()
        assertThat(underTest.getPublicLinks()).isEmpty()
        assertThat(underTest.getPublicLinks(ORDER)).isEmpty()
        assertThat(underTest.getUserFromInShare(StubMegaNode(handle = 5L), true)).isNull()
        assertThat(underTest.getContact("someone@mega.nz")).isNull()
        assertThat(underTest.getAccess(StubMegaNode(handle = 5L)))
            .isEqualTo(MegaShare.ACCESS_OWNER)
    }

    @Test
    fun `test that incoming share reads resolve the seeded share by verification state`() =
        runTest {
            underTest.shares.addIncomingShare(
                node = inShareFolder(IN_SHARE_HANDLE, "shared-in"),
                ownerEmail = OWNER_EMAIL,
                ownerHandle = OWNER_HANDLE,
                access = MegaShare.ACCESS_READWRITE,
            )

            val verified = underTest.getVerifiedIncomingShares(ORDER).single()
            assertThat(verified.user).isEqualTo(OWNER_EMAIL)
            assertThat(verified.nodeHandle).isEqualTo(IN_SHARE_HANDLE)
            assertThat(verified.access).isEqualTo(MegaShare.ACCESS_READWRITE)
            assertThat(verified.isVerified).isTrue()
            assertThat(underTest.getUnverifiedIncomingShares(ORDER)).isEmpty()
            assertThat(underTest.getIncomingSharesNode(ORDER).single().handle)
                .isEqualTo(IN_SHARE_HANDLE)
            assertThat(underTest.getInShares(ORDER).single().handle).isEqualTo(IN_SHARE_HANDLE)
        }

    @Test
    fun `test that unverified incoming shares resolve separately when seeded unverified`() =
        runTest {
            underTest.shares.addIncomingShare(
                node = inShareFolder(IN_SHARE_HANDLE, "shared-in"),
                ownerEmail = OWNER_EMAIL,
                ownerHandle = OWNER_HANDLE,
                isVerified = false,
            )

            assertThat(underTest.getUnverifiedIncomingShares(ORDER)).hasSize(1)
            assertThat(underTest.getVerifiedIncomingShares(ORDER)).isEmpty()
        }

    @Test
    fun `test that getInShares by user returns only the folders shared by that owner`() = runTest {
        underTest.shares.addIncomingShare(
            node = inShareFolder(IN_SHARE_HANDLE, "shared-in"),
            ownerEmail = OWNER_EMAIL,
            ownerHandle = OWNER_HANDLE,
        )

        val byOwner = underTest.getInShares(stubUser(OWNER_EMAIL, OWNER_HANDLE))
        val byOther = underTest.getInShares(stubUser("other@mega.nz", 999L))

        assertThat(byOwner.single().handle).isEqualTo(IN_SHARE_HANDLE)
        assertThat(byOther).isEmpty()
    }

    @Test
    fun `test that getUserFromInShare resolves the owner of the share root and its descendants`() =
        runTest {
            val shareRoot = inShareFolder(IN_SHARE_HANDLE, "shared-in")
            underTest.shares.addIncomingShare(shareRoot, OWNER_EMAIL, OWNER_HANDLE)
            val child = StubMegaNode(handle = 11L, name = "child.txt", parentHandle = IN_SHARE_HANDLE)
            underTest.nodeTree.addNode(child, parentHandle = IN_SHARE_HANDLE)

            val owner = underTest.getUserFromInShare(shareRoot, recursive = false)
            val ownerOfChild = underTest.getUserFromInShare(child, recursive = true)

            assertThat(owner?.email).isEqualTo(OWNER_EMAIL)
            assertThat(owner?.handle).isEqualTo(OWNER_HANDLE)
            assertThat(ownerOfChild?.email).isEqualTo(OWNER_EMAIL)
            assertThat(underTest.getUserFromInShare(child, recursive = false)).isNull()
        }

    @Test
    fun `test that getAccess returns the incoming share access for covered nodes and owner otherwise`() =
        runTest {
            val shareRoot = inShareFolder(IN_SHARE_HANDLE, "shared-in")
            underTest.shares.addIncomingShare(
                node = shareRoot,
                ownerEmail = OWNER_EMAIL,
                ownerHandle = OWNER_HANDLE,
                access = MegaShare.ACCESS_FULL,
            )
            val child = StubMegaNode(handle = 11L, name = "child.txt", parentHandle = IN_SHARE_HANDLE)
            underTest.nodeTree.addNode(child, parentHandle = IN_SHARE_HANDLE)

            assertThat(underTest.getAccess(shareRoot)).isEqualTo(MegaShare.ACCESS_FULL)
            assertThat(underTest.getAccess(child)).isEqualTo(MegaShare.ACCESS_FULL)
            assertThat(underTest.getAccess(underTest.nodeTree.rootNode))
                .isEqualTo(MegaShare.ACCESS_OWNER)
        }

    @Test
    fun `test that getContact resolves seeded incoming share owners by email and base64 handle`() =
        runTest {
            underTest.shares.addIncomingShare(
                node = inShareFolder(IN_SHARE_HANDLE, "shared-in"),
                ownerEmail = OWNER_EMAIL,
                ownerHandle = OWNER_HANDLE,
            )

            val byEmail = underTest.getContact(OWNER_EMAIL)
            val byHandle = underTest.getContact(underTest.userHandleToBase64(OWNER_HANDLE))

            assertThat(byEmail?.handle).isEqualTo(OWNER_HANDLE)
            assertThat(byHandle?.email).isEqualTo(OWNER_EMAIL)
            assertThat(underTest.getContact("unknown@mega.nz")).isNull()
        }

    @Test
    fun `test that getContact resolves seeded outgoing share recipients by email`() = runTest {
        underTest.shares.addOutgoingShare(
            node = outShareFolder(OUT_SHARE_HANDLE, "shared-out"),
            userEmail = PEER_EMAIL,
            userHandle = 333L,
        )

        val byEmail = underTest.getContact(PEER_EMAIL)

        assertThat(byEmail?.email).isEqualTo(PEER_EMAIL)
        assertThat(byEmail?.handle).isEqualTo(333L)
    }

    @Test
    fun `test that outgoing share reads resolve the seeded share for all reads and per node`() =
        runTest {
            val node = outShareFolder(OUT_SHARE_HANDLE, "shared-out")
            underTest.shares.addOutgoingShare(
                node = node,
                userEmail = PEER_EMAIL,
                access = MegaShare.ACCESS_READ,
            )

            val all = underTest.getOutgoingSharesNode(ORDER).single()
            assertThat(all.user).isEqualTo(PEER_EMAIL)
            assertThat(all.nodeHandle).isEqualTo(OUT_SHARE_HANDLE)
            assertThat(all.access).isEqualTo(MegaShare.ACCESS_READ)
            assertThat(all.isVerified).isTrue()
            assertThat(underTest.getOutShares(ORDER)).hasSize(1)
            assertThat(underTest.getOutShares(node)).hasSize(1)
            assertThat(underTest.getOutShares(StubMegaNode(handle = 99L))).isEmpty()
        }

    @Test
    fun `test that public link reads resolve the seeded exported nodes`() = runTest {
        val node = linkedFile(LINK_HANDLE, "linked.txt")
        underTest.shares.addPublicLink(node)

        assertThat(underTest.getPublicLinks().single().handle).isEqualTo(LINK_HANDLE)
        assertThat(underTest.getPublicLinks(ORDER).single().handle).isEqualTo(LINK_HANDLE)
        assertThat(underTest.getPublicLinks(null).single().handle).isEqualTo(LINK_HANDLE)
    }

    @Test
    fun `test that share mutating helpers broadcast OnNodesUpdate through the gateway flow`() =
        runTest {
            val inShare = inShareFolder(IN_SHARE_HANDLE, "shared-in")
            underTest.shares.addIncomingShare(inShare, OWNER_EMAIL, OWNER_HANDLE)
            val outShare = outShareFolder(OUT_SHARE_HANDLE, "shared-out")
            underTest.shares.addOutgoingShare(outShare, PEER_EMAIL)
            val linked = linkedFile(LINK_HANDLE, "linked.txt")
            underTest.shares.addPublicLink(linked)

            underTest.globalUpdates.test {
                underTest.shares.removeIncomingShare(IN_SHARE_HANDLE)
                val removed = awaitItem() as GlobalUpdate.OnNodesUpdate
                assertThat(removed.nodeList?.single()?.hasChanged(MegaNode.CHANGE_TYPE_REMOVED.toLong()))
                    .isTrue()

                underTest.shares.setOutgoingShareAccess(
                    OUT_SHARE_HANDLE,
                    PEER_EMAIL,
                    MegaShare.ACCESS_FULL,
                )
                val accessChanged = awaitItem() as GlobalUpdate.OnNodesUpdate
                assertThat(accessChanged.nodeList?.single()?.hasChanged(MegaNode.CHANGE_TYPE_OUTSHARE.toLong()))
                    .isTrue()
                assertThat(underTest.getOutShares(outShare).single().access)
                    .isEqualTo(MegaShare.ACCESS_FULL)

                underTest.shares.removeOutgoingShare(OUT_SHARE_HANDLE)
                awaitItem()
                assertThat(underTest.getOutgoingSharesNode(ORDER)).isEmpty()

                underTest.shares.removePublicLink(LINK_HANDLE)
                val linkRemoved = awaitItem() as GlobalUpdate.OnNodesUpdate
                assertThat(linkRemoved.nodeList?.single()?.hasChanged(MegaNode.CHANGE_TYPE_PUBLIC_LINK.toLong()))
                    .isTrue()
                assertThat(underTest.getPublicLinks()).isEmpty()
            }
        }

    @Test
    fun `test that stubs win over the seeded share state`() = runTest {
        underTest.shares.addOutgoingShare(outShareFolder(OUT_SHARE_HANDLE, "out"), PEER_EMAIL)
        val stubbed = listOf(StubMegaShare(user = "stub@mega.nz", nodeHandle = 1L))
        underTest.stub(MegaApiGateway::getOutgoingSharesNode) { stubbed }

        assertThat(underTest.getOutgoingSharesNode(ORDER)).isEqualTo(stubbed)
    }

    @Test
    fun `test that resetToDefaults clears the seeded share state`() = runTest {
        underTest.shares.addIncomingShare(
            inShareFolder(IN_SHARE_HANDLE, "in"),
            OWNER_EMAIL,
            OWNER_HANDLE,
        )
        underTest.shares.addOutgoingShare(outShareFolder(OUT_SHARE_HANDLE, "out"), PEER_EMAIL)
        underTest.shares.addPublicLink(linkedFile(LINK_HANDLE, "linked.txt"))

        underTest.resetToDefaults()

        assertThat(underTest.getVerifiedIncomingShares(ORDER)).isEmpty()
        assertThat(underTest.getOutgoingSharesNode(ORDER)).isEmpty()
        assertThat(underTest.getPublicLinks()).isEmpty()
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
        parentHandle = underTest.nodeTree.rootNode.handle,
        isFolder = true,
        isOutShare = true,
    )

    private fun linkedFile(handle: Long, name: String) = StubMegaNode(
        handle = handle,
        name = name,
        parentHandle = underTest.nodeTree.rootNode.handle,
        isExported = true,
        publicLink = "https://mega.nz/file/link#key",
    )

    private fun stubUser(email: String, handle: Long): MegaUser =
        mega.privacy.android.data.test.stub.StubMegaUser(email = email, handle = handle)

    private companion object {
        const val ORDER = MegaApiJava.ORDER_DEFAULT_ASC
        const val IN_SHARE_HANDLE = 10L
        const val OUT_SHARE_HANDLE = 20L
        const val LINK_HANDLE = 30L
        const val OWNER_EMAIL = "owner@mega.nz"
        const val OWNER_HANDLE = 222L
        const val PEER_EMAIL = "peer@mega.nz"
    }
}
