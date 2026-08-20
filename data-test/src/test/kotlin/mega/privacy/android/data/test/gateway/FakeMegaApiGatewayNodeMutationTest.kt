package mega.privacy.android.data.test.gateway

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.test.stub.StubMegaNode
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Documents the [mega.privacy.android.data.test.state.FakeNodeTree] mutating helpers surfaced
 * through [FakeMegaApiGateway]: each helper both changes the tree and broadcasts the matching
 * [GlobalUpdate.OnNodesUpdate], with the emitted node carrying the real SDK `CHANGE_TYPE_*` flag.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeMegaApiGatewayNodeMutationTest {

    private lateinit var underTest: FakeMegaApiGateway

    @BeforeEach
    fun setUp() {
        underTest = FakeMegaApiGateway()
        underTest.nodeTree.addNode(
            StubMegaNode(handle = FILE_HANDLE, name = ORIGINAL_NAME, parentHandle = ROOT_HANDLE),
            parentHandle = ROOT_HANDLE,
        )
        underTest.nodeTree.addNode(
            StubMegaNode(handle = FOLDER_HANDLE, name = "folder", parentHandle = ROOT_HANDLE, isFolder = true),
            parentHandle = ROOT_HANDLE,
        )
    }

    @Test
    fun `test that rename updates the node name and emits an OnNodesUpdate with the name change flag`() =
        runTest {
            underTest.globalUpdates.test {
                val renamed = underTest.nodeTree.rename(FILE_HANDLE, NEW_NAME)

                assertThat(renamed?.name).isEqualTo(NEW_NAME)
                assertThat(underTest.nodeTree.nodeByHandle(FILE_HANDLE)?.name).isEqualTo(NEW_NAME)

                val node = awaitItem().singleUpdatedNode()
                assertThat(node.handle).isEqualTo(FILE_HANDLE)
                assertThat(node.name).isEqualTo(NEW_NAME)
                assertThat(node.hasChanged(MegaNode.CHANGE_TYPE_NAME.toLong())).isTrue()
            }
        }

    @Test
    fun `test that move reparents the node and emits an OnNodesUpdate with the parent change flag`() =
        runTest {
            underTest.globalUpdates.test {
                val moved = underTest.nodeTree.move(FILE_HANDLE, FOLDER_HANDLE)

                assertThat(moved?.parentHandle).isEqualTo(FOLDER_HANDLE)
                assertThat(underTest.nodeTree.childrenOf(FOLDER_HANDLE).map { it.handle })
                    .containsExactly(FILE_HANDLE)
                assertThat(underTest.nodeTree.childrenOf(ROOT_HANDLE).map { it.handle })
                    .containsExactly(FOLDER_HANDLE)

                val nodes = awaitItem().updatedNodes()
                assertThat(nodes.map { it.handle }).containsExactly(FILE_HANDLE, ROOT_HANDLE)
                val node = nodes.first { it.handle == FILE_HANDLE }
                assertThat(node.parentHandle).isEqualTo(FOLDER_HANDLE)
                assertThat(node.hasChanged(MegaNode.CHANGE_TYPE_PARENT.toLong())).isTrue()
                val oldParent = nodes.first { it.handle == ROOT_HANDLE }
                assertThat(oldParent.hasChanged(MegaNode.CHANGE_TYPE_TIMESTAMP.toLong())).isTrue()
            }
        }

    @Test
    fun `test that move is a no-op when the new parent is the node's own descendant`() = runTest {
        underTest.nodeTree.addNode(
            StubMegaNode(handle = CHILD_HANDLE, name = "child", parentHandle = FOLDER_HANDLE, isFolder = true),
            parentHandle = FOLDER_HANDLE,
        )

        underTest.globalUpdates.test {
            val moved = underTest.nodeTree.move(FOLDER_HANDLE, CHILD_HANDLE)

            assertThat(moved).isNull()
            assertThat(underTest.nodeTree.nodeByHandle(FOLDER_HANDLE)?.parentHandle)
                .isEqualTo(ROOT_HANDLE)
            expectNoEvents()
        }
    }

    @Test
    fun `test that copy creates a new node under the target and emits an OnNodesUpdate with the new flag`() =
        runTest {
            underTest.globalUpdates.test {
                val copy = underTest.nodeTree.copy(FILE_HANDLE, FOLDER_HANDLE)

                assertThat(copy).isNotNull()
                assertThat(copy?.handle).isNotEqualTo(FILE_HANDLE)
                assertThat(copy?.name).isEqualTo(ORIGINAL_NAME)
                assertThat(copy?.parentHandle).isEqualTo(FOLDER_HANDLE)
                assertThat(underTest.nodeTree.nodeByHandle(FILE_HANDLE)?.parentHandle)
                    .isEqualTo(ROOT_HANDLE)
                assertThat(underTest.nodeTree.childrenOf(FOLDER_HANDLE).map { it.handle })
                    .containsExactly(copy?.handle)

                val node = awaitItem().singleUpdatedNode()
                assertThat(node.handle).isEqualTo(copy?.handle)
                assertThat(node.hasChanged(MegaNode.CHANGE_TYPE_NEW.toLong())).isTrue()
            }
        }

    @Test
    fun `test that copy applies the given name when one is provided`() = runTest {
        underTest.globalUpdates.test {
            val copy = underTest.nodeTree.copy(FILE_HANDLE, FOLDER_HANDLE, newName = NEW_NAME)

            assertThat(copy?.name).isEqualTo(NEW_NAME)
            awaitItem()
        }
    }

    @Test
    fun `test that moveToRubbish reparents the node to the rubbish root and emits the parent change flag`() =
        runTest {
            underTest.globalUpdates.test {
                val moved = underTest.nodeTree.moveToRubbish(FILE_HANDLE)

                assertThat(moved?.parentHandle).isEqualTo(underTest.nodeTree.rubbishBinNode.handle)
                assertThat(underTest.isInRubbish(underTest.nodeTree.nodeByHandle(FILE_HANDLE)!!))
                    .isTrue()

                val nodes = awaitItem().updatedNodes()
                val node = nodes.first { it.handle == FILE_HANDLE }
                assertThat(node.parentHandle).isEqualTo(underTest.nodeTree.rubbishBinNode.handle)
                assertThat(node.hasChanged(MegaNode.CHANGE_TYPE_PARENT.toLong())).isTrue()
                assertThat(nodes.map { it.handle }).contains(ROOT_HANDLE)
            }
        }

    @Test
    fun `test that remove deletes the node from the tree and emits an OnNodesUpdate with the removed flag`() =
        runTest {
            underTest.globalUpdates.test {
                val removed = underTest.nodeTree.remove(FILE_HANDLE)

                assertThat(removed?.handle).isEqualTo(FILE_HANDLE)
                assertThat(underTest.nodeTree.nodeByHandle(FILE_HANDLE)).isNull()

                val node = awaitItem().singleUpdatedNode()
                assertThat(node.handle).isEqualTo(FILE_HANDLE)
                assertThat(node.hasChanged(MegaNode.CHANGE_TYPE_REMOVED.toLong())).isTrue()
            }
        }

    @Test
    fun `test that remove deletes descendants when the node has children`() = runTest {
        underTest.nodeTree.addNode(
            StubMegaNode(handle = CHILD_HANDLE, name = "child", parentHandle = FOLDER_HANDLE),
            parentHandle = FOLDER_HANDLE,
        )

        underTest.globalUpdates.test {
            underTest.nodeTree.remove(FOLDER_HANDLE)

            assertThat(underTest.nodeTree.nodeByHandle(FOLDER_HANDLE)).isNull()
            assertThat(underTest.nodeTree.nodeByHandle(CHILD_HANDLE)).isNull()
            awaitItem()
        }
    }

    @Test
    fun `test that setFavourite updates the flag and emits an OnNodesUpdate with the favourite change flag`() =
        runTest {
            underTest.globalUpdates.test {
                val updated = underTest.nodeTree.setFavourite(FILE_HANDLE, true)

                assertThat(updated?.isFavourite).isTrue()
                assertThat(underTest.nodeTree.nodeByHandle(FILE_HANDLE)?.isFavourite).isTrue()

                val node = awaitItem().singleUpdatedNode()
                assertThat(node.handle).isEqualTo(FILE_HANDLE)
                assertThat(node.isFavourite).isTrue()
                assertThat(node.hasChanged(MegaNode.CHANGE_TYPE_FAVOURITE.toLong())).isTrue()
            }
        }

    @Test
    fun `test that setLabel updates the label and emits an OnNodesUpdate with the attributes change flag`() =
        runTest {
            underTest.globalUpdates.test {
                val updated = underTest.nodeTree.setLabel(FILE_HANDLE, MegaNode.NODE_LBL_RED)

                assertThat(updated?.label).isEqualTo(MegaNode.NODE_LBL_RED)
                assertThat(underTest.nodeTree.nodeByHandle(FILE_HANDLE)?.label)
                    .isEqualTo(MegaNode.NODE_LBL_RED)

                val node = awaitItem().singleUpdatedNode()
                assertThat(node.handle).isEqualTo(FILE_HANDLE)
                assertThat(node.label).isEqualTo(MegaNode.NODE_LBL_RED)
                assertThat(node.hasChanged(MegaNode.CHANGE_TYPE_ATTRIBUTES.toLong())).isTrue()
            }
        }

    @Test
    fun `test that a mutating helper is a no-op when the handle is unknown`() = runTest {
        underTest.globalUpdates.test {
            assertThat(underTest.nodeTree.rename(UNKNOWN_HANDLE, NEW_NAME)).isNull()
            assertThat(underTest.nodeTree.move(UNKNOWN_HANDLE, FOLDER_HANDLE)).isNull()
            assertThat(underTest.nodeTree.copy(UNKNOWN_HANDLE, FOLDER_HANDLE)).isNull()
            assertThat(underTest.nodeTree.setFavourite(UNKNOWN_HANDLE, true)).isNull()
            assertThat(underTest.nodeTree.setLabel(UNKNOWN_HANDLE, MegaNode.NODE_LBL_RED)).isNull()
            assertThat(underTest.nodeTree.moveToRubbish(UNKNOWN_HANDLE)).isNull()
            assertThat(underTest.nodeTree.remove(UNKNOWN_HANDLE)).isNull()

            expectNoEvents()
        }
    }

    private fun GlobalUpdate.singleUpdatedNode(): MegaNode {
        assertThat(this).isInstanceOf(GlobalUpdate.OnNodesUpdate::class.java)
        val nodes = (this as GlobalUpdate.OnNodesUpdate).nodeList
        assertThat(nodes).hasSize(1)
        return nodes!!.single()
    }

    private fun GlobalUpdate.updatedNodes(): List<MegaNode> {
        assertThat(this).isInstanceOf(GlobalUpdate.OnNodesUpdate::class.java)
        return (this as GlobalUpdate.OnNodesUpdate).nodeList!!
    }

    private companion object {
        const val ROOT_HANDLE = 1L
        const val FILE_HANDLE = 10L
        const val FOLDER_HANDLE = 20L
        const val CHILD_HANDLE = 30L
        const val UNKNOWN_HANDLE = 999L
        const val ORIGINAL_NAME = "a.jpg"
        const val NEW_NAME = "b.jpg"
    }
}
