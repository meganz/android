package mega.privacy.android.data.test.gateway

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.test.stub.StubMegaNode
import nz.mega.sdk.MegaApiJava
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Documents how [FakeMegaApiGateway] defaults are backed by [FakeMegaApiGateway.account] and
 * [FakeMegaApiGateway.nodeTree]: mutating the state objects changes what the fake answers.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaApiGatewayStateTest {

    private lateinit var underTest: FakeMegaApiGateway

    @BeforeEach
    fun setUp() {
        underTest = FakeMegaApiGateway()
    }

    @Test
    fun `test that account reads return the seeded defaults when nothing is configured`() {
        assertThat(underTest.myUserHandle).isEqualTo(111L)
        assertThat(underTest.getMyUserHandleBinary()).isEqualTo(111L)
        assertThat(underTest.accountEmail).isEqualTo("test@mega.nz")
        assertThat(underTest.dumpSession).isEqualTo("fake-session")
        assertThat(underTest.myCredentials).isEqualTo("fake-credentials")
        assertThat(underTest.isBusinessAccount).isFalse()
        assertThat(underTest.isAchievementsEnabled).isTrue()
        assertThat(underTest.isEphemeralPlusPlus).isFalse()
    }

    @Test
    fun `test that myUser carries the account handle and email when logged in`() {
        val user = underTest.myUser

        assertThat(user).isNotNull()
        assertThat(user?.handle).isEqualTo(111L)
        assertThat(user?.email).isEqualTo("test@mega.nz")
    }

    @Test
    fun `test that account reads change when account state is mutated`() = runTest {
        underTest.account.myUserHandle = 42L
        underTest.account.email = "other@mega.nz"
        underTest.account.session = "other-session"

        assertThat(underTest.myUserHandle).isEqualTo(42L)
        assertThat(underTest.accountEmail).isEqualTo("other@mega.nz")
        assertThat(underTest.dumpSession).isEqualTo("other-session")
        assertThat(underTest.getAccountAuth()).isEqualTo("other-session")
        assertThat(underTest.myUser?.email).isEqualTo("other@mega.nz")
    }

    @Test
    fun `test that session gated reads return null when the account is logged out`() {
        underTest.account.isLoggedIn = false

        assertThat(underTest.accountEmail).isNull()
        assertThat(underTest.dumpSession).isNull()
        assertThat(underTest.myCredentials).isNull()
        assertThat(underTest.myUser).isNull()
    }

    @Test
    fun `test that getLoggedInUser returns null when the account is logged out`() = runTest {
        underTest.account.isLoggedIn = false

        assertThat(underTest.getLoggedInUser()).isNull()
    }

    @Test
    fun `test that isMegaApiLoggedIn reflects the account login state`() {
        assertThat(underTest.isMegaApiLoggedIn()).isEqualTo(3)

        underTest.account.isLoggedIn = false

        assertThat(underTest.isMegaApiLoggedIn()).isEqualTo(0)
    }

    @Test
    fun `test that business status switches when the account becomes a business account`() =
        runTest {
            assertThat(underTest.businessStatus)
                .isEqualTo(MegaApiJava.BUSINESS_STATUS_INACTIVE)
            assertThat(underTest.getBusinessStatus())
                .isEqualTo(MegaApiJava.BUSINESS_STATUS_INACTIVE)

            underTest.account.isBusinessAccount = true

            assertThat(underTest.isBusinessAccount).isTrue()
            assertThat(underTest.businessStatus).isEqualTo(MegaApiJava.BUSINESS_STATUS_ACTIVE)
            assertThat(underTest.getBusinessStatus()).isEqualTo(MegaApiJava.BUSINESS_STATUS_ACTIVE)
        }

    @Test
    fun `test that achievements reads reflect the account flag when it is disabled`() = runTest {
        underTest.account.isAchievementsEnabled = false

        assertThat(underTest.isAchievementsEnabled).isFalse()
        assertThat(underTest.areAccountAchievementsEnabled()).isFalse()
    }

    @Test
    fun `test that the seeded root nodes resolve when nothing is configured`() = runTest {
        assertThat(underTest.getRootNode()?.name).isEqualTo("Cloud Drive")
        assertThat(underTest.getRubbishBinNode()?.name).isEqualTo("Rubbish Bin")
        assertThat(underTest.getBackupsNode()?.name).isEqualTo("Vault")
    }

    @Test
    fun `test that getMegaNodeByHandle resolves nodes added to the tree`() = runTest {
        val node = StubMegaNode(handle = 10L, name = "photo.jpg", parentHandle = 1L)
        underTest.nodeTree.addNode(node, parentHandle = 1L)

        assertThat(underTest.getMegaNodeByHandle(10L)).isSameInstanceAs(node)
        assertThat(underTest.getMegaNodeByHandle(999L)).isNull()
    }

    @Test
    fun `test that getParentNode resolves through the tree when the node has a parent`() = runTest {
        val node = StubMegaNode(handle = 10L, name = "photo.jpg", parentHandle = 1L)
        underTest.nodeTree.addNode(node, parentHandle = 1L)

        assertThat(underTest.getParentNode(node)).isSameInstanceAs(underTest.nodeTree.rootNode)
    }

    @Test
    fun `test that getChildNode finds a child by name when it exists`() = runTest {
        val folder = StubMegaNode(handle = 20L, name = "Photos", parentHandle = 1L, isFolder = true)
        val file = StubMegaNode(handle = 21L, name = "a.jpg", parentHandle = 20L)
        underTest.nodeTree.addNode(folder, parentHandle = 1L)
        underTest.nodeTree.addNode(file, parentHandle = 20L)

        assertThat(underTest.getChildNode(folder, "a.jpg")).isSameInstanceAs(file)
        assertThat(underTest.getChildNode(folder, "missing.jpg")).isNull()
    }

    @Test
    fun `test that child counts and hasChildren reflect the tree when children are added`() =
        runTest {
            val root = underTest.nodeTree.rootNode
            underTest.nodeTree.addNode(
                StubMegaNode(handle = 30L, name = "folder", parentHandle = 1L, isFolder = true),
                parentHandle = 1L,
            )
            underTest.nodeTree.addNode(
                StubMegaNode(handle = 31L, name = "file", parentHandle = 1L),
                parentHandle = 1L,
            )

            assertThat(underTest.hasChildren(root)).isTrue()
            assertThat(underTest.getNumChildFolders(root)).isEqualTo(1)
            assertThat(underTest.getNumChildFiles(root)).isEqualTo(1)
            assertThat(underTest.hasChildren(underTest.nodeTree.rubbishBinNode)).isFalse()
        }

    @Test
    fun `test that isInRubbish walks the parent chain when the node is under the rubbish root`() =
        runTest {
            val node = StubMegaNode(handle = 40L, name = "deleted.txt", parentHandle = 2L)
            underTest.nodeTree.addNode(node, parentHandle = 2L)

            assertThat(underTest.isInRubbish(node)).isTrue()
            assertThat(underTest.isInCloudDrive(node)).isFalse()
            assertThat(underTest.isInBackups(node)).isFalse()
        }

    @Test
    fun `test that isInCloudDrive resolves nested nodes when they are under the root`() = runTest {
        val folder = StubMegaNode(handle = 50L, name = "docs", parentHandle = 1L, isFolder = true)
        val nested = StubMegaNode(handle = 51L, name = "cv.pdf", parentHandle = 50L)
        underTest.nodeTree.addNode(folder, parentHandle = 1L)
        underTest.nodeTree.addNode(nested, parentHandle = 50L)

        assertThat(underTest.isInCloudDrive(nested)).isTrue()
        assertThat(underTest.isInBackups(nested)).isFalse()
    }

    @Test
    fun `test that getNodePathByHandle returns a slash prefixed name when the node exists`() =
        runTest {
            underTest.nodeTree.addNode(
                StubMegaNode(handle = 60L, name = "movie.mp4", parentHandle = 1L),
                parentHandle = 1L,
            )

            assertThat(underTest.getNodePathByHandle(60L)).isEqualTo("/movie.mp4")
            assertThat(underTest.getNodePathByHandle(999L)).isNull()
        }

    @Test
    fun `test that handle base64 encoding round trips when decoded back`() {
        val encoded = underTest.handleToBase64(12345L)

        assertThat(underTest.base64ToHandle(encoded)).isEqualTo(12345L)
    }

    @Test
    fun `test that base64ToHandle returns the invalid handle when input is not decodable`() {
        assertThat(underTest.base64ToHandle("!!not-base64!!"))
            .isEqualTo(MegaApiJava.INVALID_HANDLE)
    }

    @Test
    fun `test that resetToDefaults restores account and tree state when both were mutated`() =
        runTest {
            underTest.account.isLoggedIn = false
            underTest.account.email = "other@mega.nz"
            underTest.account.isBusinessAccount = true
            underTest.nodeTree.addNode(
                StubMegaNode(handle = 70L, name = "temp", parentHandle = 1L),
                parentHandle = 1L,
            )

            underTest.resetToDefaults()

            assertThat(underTest.accountEmail).isEqualTo("test@mega.nz")
            assertThat(underTest.isBusinessAccount).isFalse()
            assertThat(underTest.getMegaNodeByHandle(70L)).isNull()
            assertThat(underTest.getRootNode()?.name).isEqualTo("Cloud Drive")
        }
}
