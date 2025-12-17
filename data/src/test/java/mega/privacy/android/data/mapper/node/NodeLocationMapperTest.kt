package mega.privacy.android.data.mapper.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.NodeLocation
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeLocationMapperTest {

    private lateinit var underTest: NodeLocationMapper

    private val rootNodeHandle = 1L
    private val rubbishBinNodeHandle = 2L
    private val otherNodeHandle = 3L
    private val nodeHandle = 100L
    private val nodeParentHandle = 50L

    @BeforeAll
    fun setup() {
        underTest = NodeLocationMapper()
    }

    @Test
    fun `test that returns IncomingSharesRoot when node is in share`() = runTest {
        val node = mock<MegaNode> {
            on { isInShare } doReturn true
            on { handle } doReturn nodeHandle
            on { parentHandle } doReturn nodeParentHandle
        }
        val rootParent = mock<MegaNode> {
            on { isInShare } doReturn false
            on { handle } doReturn otherNodeHandle
        }

        val result = underTest(
            node = node,
            rootParent = rootParent,
            getRootNode = { null },
            getRubbishBinNode = { null },
        )

        assertThat(result).isEqualTo(NodeLocation.IncomingSharesRoot)
    }

    @Test
    fun `test that returns IncomingShares when root parent is in share and node is not`() =
        runTest {
            val node = mock<MegaNode> {
                on { isInShare } doReturn false
                on { handle } doReturn nodeHandle
                on { parentHandle } doReturn nodeParentHandle
            }
            val rootParent = mock<MegaNode> {
                on { isInShare } doReturn true
                on { handle } doReturn otherNodeHandle
            }

            val result = underTest(
                node = node,
                rootParent = rootParent,
                getRootNode = { null },
                getRubbishBinNode = { null },
            )

            assertThat(result).isEqualTo(NodeLocation.IncomingShares)
        }

    @Test
    fun `test that returns CloudDriveRoot when root parent is root node and node parent is root parent`() =
        runTest {
            val rootNode = mock<MegaNode> {
                on { handle } doReturn rootNodeHandle
            }
            val node = mock<MegaNode> {
                on { isInShare } doReturn false
                on { handle } doReturn nodeHandle
                on { parentHandle } doReturn rootNodeHandle
            }
            val rootParent = mock<MegaNode> {
                on { isInShare } doReturn false
                on { handle } doReturn rootNodeHandle
            }

            val result = underTest(
                node = node,
                rootParent = rootParent,
                getRootNode = { rootNode },
                getRubbishBinNode = { null },
            )

            assertThat(result).isEqualTo(NodeLocation.CloudDriveRoot)
        }

    @Test
    fun `test that returns CloudDrive when root parent is root node but node parent is not root parent`() =
        runTest {
            val rootNode = mock<MegaNode> {
                on { handle } doReturn rootNodeHandle
            }
            val node = mock<MegaNode> {
                on { isInShare } doReturn false
                on { handle } doReturn nodeHandle
                on { parentHandle } doReturn nodeParentHandle
            }
            val rootParent = mock<MegaNode> {
                on { isInShare } doReturn false
                on { handle } doReturn rootNodeHandle
            }

            val result = underTest(
                node = node,
                rootParent = rootParent,
                getRootNode = { rootNode },
                getRubbishBinNode = { null },
            )

            assertThat(result).isEqualTo(NodeLocation.CloudDrive)
        }

    @Test
    fun `test that returns RubbishBinRoot when root parent is rubbish bin node and node parent is root parent`() =
        runTest {
            val rubbishBinNode = mock<MegaNode> {
                on { handle } doReturn rubbishBinNodeHandle
            }
            val node = mock<MegaNode> {
                on { isInShare } doReturn false
                on { handle } doReturn nodeHandle
                on { parentHandle } doReturn rubbishBinNodeHandle
            }
            val rootParent = mock<MegaNode> {
                on { isInShare } doReturn false
                on { handle } doReturn rubbishBinNodeHandle
            }

            val result = underTest(
                node = node,
                rootParent = rootParent,
                getRootNode = { null },
                getRubbishBinNode = { rubbishBinNode },
            )

            assertThat(result).isEqualTo(NodeLocation.RubbishBinRoot)
        }

    @Test
    fun `test that returns RubbishBin when root parent is rubbish bin node but node parent is not root parent`() =
        runTest {
            val rubbishBinNode = mock<MegaNode> {
                on { handle } doReturn rubbishBinNodeHandle
            }
            val node = mock<MegaNode> {
                on { isInShare } doReturn false
                on { handle } doReturn nodeHandle
                on { parentHandle } doReturn nodeParentHandle
            }
            val rootParent = mock<MegaNode> {
                on { isInShare } doReturn false
                on { handle } doReturn rubbishBinNodeHandle
            }

            val result = underTest(
                node = node,
                rootParent = rootParent,
                getRootNode = { null },
                getRubbishBinNode = { rubbishBinNode },
            )

            assertThat(result).isEqualTo(NodeLocation.RubbishBin)
        }

    @Test
    fun `test that returns CloudDrive as default when no other conditions match`() = runTest {
        val node = mock<MegaNode> {
            on { isInShare } doReturn false
            on { handle } doReturn nodeHandle
            on { parentHandle } doReturn nodeParentHandle
        }
        val rootParent = mock<MegaNode> {
            on { isInShare } doReturn false
            on { handle } doReturn otherNodeHandle
        }

        val result = underTest(
            node = node,
            rootParent = rootParent,
            getRootNode = { null },
            getRubbishBinNode = { null },
        )

        assertThat(result).isEqualTo(NodeLocation.CloudDrive)
    }
}

