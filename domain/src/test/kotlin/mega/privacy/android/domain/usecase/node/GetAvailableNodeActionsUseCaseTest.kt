package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [GetAvailableNodeActionsUseCase]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetAvailableNodeActionsUseCaseTest {

    private val fileNode = mock<TypedFileNode>()
    private val folderNode = mock<TypedFolderNode>()

    private val parentNode = mock<TypedFolderNode> {
        on { id }.thenReturn(parentId)
    }
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val isNodeInRubbish = mock<IsNodeInRubbish>()
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>()
    private val underTest = GetAvailableNodeActionsUseCase(
        getNodeByIdUseCase = getNodeByIdUseCase,
        isNodeInRubbish = isNodeInRubbish,
        getNodeAccessPermission = getNodeAccessPermission,
        isNodeInBackupsUseCase = isNodeInBackupsUseCase,
    )

    @BeforeEach
    fun reset() {
        resetMocks()
    }

    private fun resetMocks() {
        reset(
            fileNode,
            folderNode,
            getNodeByIdUseCase,
            getNodeAccessPermission,
            isNodeInRubbish,
            isNodeInBackupsUseCase,
        )
    }


    @TestFactory
    fun `test that Delete action is returned if the node is in rubbish bin`() =
        listOf(true, false).flatMap { inRubbish ->
            mockedNodes.map { (name, mockNode) ->
                dynamicTest("node: $name, in rubbish bin: $inRubbish") {
                    runTest {
                        val node = mockNode()
                        whenever(isNodeInRubbish(node.id.longValue)).thenReturn(inRubbish)
                        val result = underTest(node)
                        if (inRubbish) {
                            Truth.assertThat(result).containsExactly(NodeAction.Delete)
                        } else {
                            Truth.assertThat(result).doesNotContain(NodeAction.Delete)
                        }
                    }
                }
            }
        }


    @TestFactory
    fun `test that Send to chat action is returned if the node is a file and not taken down`() =
        listOf(true, false).flatMap { takenDown ->
            mockedNodes.map { (name, mockNode) ->
                val isFile = mockNode.isFile()
                dynamicTest("node: $name, takeDown: $takenDown, isFile: $isFile") {
                    runTest {
                        val node = mockNode()
                        whenever(node.isTakenDown).thenReturn(takenDown)
                        val result = underTest(node)

                        if (takenDown || !isFile) {
                            Truth.assertThat(result).doesNotContain(NodeAction.SendToChat)
                        } else {
                            Truth.assertThat(result).contains(NodeAction.SendToChat)
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Download action is returned if the node is not taken down`() =
        listOf(true, false).flatMap { takenDown ->
            mockedNodes.map { (name, mockNode) ->
                dynamicTest("node: $name, takeDown: $takenDown") {
                    runTest {
                        val node = mockNode()
                        whenever(node.isTakenDown).thenReturn(takenDown)
                        val result = underTest(node)

                        if (takenDown) {
                            Truth.assertThat(result).doesNotContain(NodeAction.Download)
                        } else {
                            Truth.assertThat(result).contains(NodeAction.Download)
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Copy action is returned if the node is not taken down`() =
        listOf(true, false).flatMap { takenDown ->
            mockedNodes.map { (name, mockNode) ->
                dynamicTest("node: $name, takeDown: $takenDown") {
                    runTest {
                        val node = mockNode()
                        whenever(node.isTakenDown).thenReturn(takenDown)
                        val result = underTest(node)

                        if (takenDown) {
                            Truth.assertThat(result).doesNotContain(NodeAction.Copy)
                        } else {
                            Truth.assertThat(result).contains(NodeAction.Copy)
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Leave action is returned if the node is not taken down and is a shared root node`() =
        listOf(true, false).flatMap { takenDown ->
            mockedNodes.map { (name, mockNode) ->
                val root = mockNode.isRootInShared()
                dynamicTest("node: $name, root: $root, takenDown: $takenDown") {
                    runTest {
                        val node = mockNode()
                        whenever(node.isTakenDown).thenReturn(takenDown)
                        val result = underTest(node)

                        if (root && !takenDown) {
                            Truth.assertThat(result).contains(NodeAction.Leave)
                        } else {
                            Truth.assertThat(result).doesNotContain(NodeAction.Leave)
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Rename action is returned if the non backup node is not a shared node, or is a shared node and has owner permission`() =
        listOf(true, false).flatMap { owner ->
            mockedNodes.map { (name, mockNode) ->
                val shared = mockNode.isInShared()
                dynamicTest("node $name, owner: $owner, in shared: $shared") {
                    runTest {
                        val node = mockNode()
                        whenever(getNodeAccessPermission(node.id)).thenReturn(if (owner) AccessPermission.OWNER else AccessPermission.READ)
                        whenever(isNodeInBackupsUseCase(node.id.longValue)).thenReturn(false)
                        val result = underTest(node)

                        if (owner || !shared) {
                            Truth.assertThat(result).contains(NodeAction.Rename)
                        } else {
                            Truth.assertThat(result).doesNotContain(NodeAction.Rename)
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Move to rubbish action is returned if the non backup node is not a shared node, or is a shared root node and has owner permission`() =
        listOf(true, false).flatMap { owner ->
            mockedNodes.map { (name, mockNode) ->
                val shared = mockNode.isInShared()
                val root = mockNode.isRootInShared()
                dynamicTest("node: $name, shared: $shared, owner: $owner, in shared root:$root") {
                    runTest {
                        val node = mockNode()
                        whenever(getNodeAccessPermission(node.id)).thenReturn(if (owner) AccessPermission.OWNER else AccessPermission.READ)
                        whenever(isNodeInBackupsUseCase(node.id.longValue)).thenReturn(false)

                        val result = underTest(node)

                        if (!shared || (owner && root)) {
                            Truth.assertThat(result).contains(NodeAction.MoveToRubbishBin)
                        } else {
                            Truth.assertThat(result).doesNotContain(NodeAction.MoveToRubbishBin)
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Move action is returned if the non backup node is not a shared node`() =
        listOf(true, false).flatMap { takenDown ->
            mockedNodes.map { (name, mockNode) ->
                val shared = mockNode.isInShared()
                dynamicTest("node: $name, shared: $shared, takenDown: $takenDown") {
                    runTest {
                        val node = mockNode()
                        whenever(node.isTakenDown).thenReturn(takenDown)
                        whenever(isNodeInBackupsUseCase(node.id.longValue)).thenReturn(false)

                        val result = underTest(node)

                        if (!shared) {
                            Truth.assertThat(result).contains(NodeAction.Move)
                        } else {
                            Truth.assertThat(result).doesNotContain(NodeAction.Move)
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that share folder action is returned if the node is a folder, not taken down, and not a shared folder`() =
        listOf(true, false).flatMap { takenDown ->
            mockedNodes.map { (name, mockNode) ->
                val inShared = mockNode.isInShared()
                val folder = mockNode.isFolder()
                dynamicTest("node: $name, takenDown: $takenDown, inShared: $inShared, folder: $folder") {
                    runTest {
                        val node = mockNode()
                        whenever(node.isTakenDown).thenReturn(takenDown)
                        val result = underTest(node)

                        if (takenDown || inShared || !folder) {
                            Truth.assertThat(result).doesNotContain(NodeAction.ShareFolder)
                        } else {
                            Truth.assertThat(result).contains(NodeAction.ShareFolder)
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Manage link action is returned if the node is exported, not taken down, and not a shared node`() =
        listOf(true, false).flatMap { takenDown ->
            listOf(true, false).flatMap { exported ->
                mockedNodes.map { (name, mockNode) ->
                    val inShared = mockNode.isInShared()
                    dynamicTest("node: $name, takenDown: $takenDown, inShared: $inShared, exported: $exported") {
                        runTest {
                            val node = mockNode()
                            whenever(node.isTakenDown).thenReturn(takenDown)
                            whenever(node.exportedData).thenReturn(if (exported) mock() else null)
                            val result = underTest(node)

                            if (exported && !takenDown && !inShared) {
                                Truth.assertThat(result).contains(NodeAction.ManageLink)
                            } else {
                                Truth.assertThat(result).doesNotContain(NodeAction.ManageLink)
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Remove link action is returned if the node is exported, not taken down, and not a shared node`() =
        listOf(true, false).flatMap { takenDown ->
            listOf(true, false).flatMap { exported ->
                mockedNodes.map { (name, mockNode) ->
                    val inShared = mockNode.isInShared()
                    dynamicTest("node: $name, takenDown: $takenDown, inShared: $inShared, exported: $exported") {
                        runTest {
                            val node = mockNode()
                            whenever(node.isTakenDown).thenReturn(takenDown)
                            whenever(node.exportedData).thenReturn(if (exported) mock() else null)
                            val result = underTest(node)

                            if (exported && !takenDown && !inShared) {
                                Truth.assertThat(result).contains(NodeAction.RemoveLink)
                            } else {
                                Truth.assertThat(result).doesNotContain(NodeAction.RemoveLink)
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Get link action is returned if the node is not exported, not taken down, and not a shared node`() =
        listOf(true, false).flatMap { takenDown ->
            listOf(true, false).flatMap { exported ->
                mockedNodes.map { (name, mockNode) ->
                    val inShared = mockNode.isInShared()
                    dynamicTest("node: $name, takenDown: $takenDown, inShared: $inShared, exported: $exported") {
                        runTest {
                            val node = mockNode()
                            whenever(node.isTakenDown).thenReturn(takenDown)
                            whenever(node.exportedData).thenReturn(if (exported) mock() else null)
                            val result = underTest(node)

                            if (!exported && !takenDown && !inShared) {
                                Truth.assertThat(result).contains(NodeAction.GetLink)
                            } else {
                                Truth.assertThat(result).doesNotContain(NodeAction.GetLink)
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Dispute taken down action is returned if the node is taken down and not a shared node`() =
        listOf(true, false).flatMap { takenDown ->
            mockedNodes.map { (name, mockNode) ->
                val inShared = mockNode.isInShared()
                dynamicTest("node: $name, takenDown: $takenDown, inShared: $inShared") {
                    runTest {
                        val node = mockNode()
                        whenever(node.isTakenDown).thenReturn(takenDown)
                        val result = underTest(node)

                        if (takenDown && !inShared) {
                            Truth.assertThat(result).contains(NodeAction.DisputeTakedown)
                        } else {
                            Truth.assertThat(result).doesNotContain(NodeAction.DisputeTakedown)
                        }
                    }
                }
            }
        }

    private val mockedNodes = mapOf(
        "File" to this::mockFile,
        "InSharedFile" to this::mockInShareFile,
        "InSharedRootFile" to this::mockInShareRootFile,
        "Folder" to this::mockFolder,
        "InSharedFolder" to this::mockInShareFolder,
        "InSharedRootFolder" to this::mockInShareRootFolder,
    )

    private suspend fun mockFolder(): TypedNode {
        resetMocks()
        whenever(getNodeByIdUseCase.invoke(nodeId)).thenReturn(folderNode)
        whenever(folderNode.id).thenReturn(nodeId)
        whenever(isNodeInRubbish(nodeId.longValue)).thenReturn(false)
        whenever(folderNode.isIncomingShare).thenReturn(false)
        whenever(folderNode.parentId).thenReturn(invalidNode)
        whenever(isNodeInBackupsUseCase(nodeId.longValue)).thenReturn(false)
        return folderNode
    }

    private suspend fun mockFile(): TypedNode {
        resetMocks()
        whenever(getNodeByIdUseCase.invoke(nodeId)).thenReturn(fileNode)
        whenever(fileNode.id).thenReturn(nodeId)
        whenever(isNodeInRubbish(nodeId.longValue)).thenReturn(false)
        whenever(fileNode.isIncomingShare).thenReturn(false)
        whenever(fileNode.parentId).thenReturn(invalidNode)
        whenever(isNodeInBackupsUseCase(nodeId.longValue)).thenReturn(false)
        return fileNode
    }

    private suspend fun mockInShareFile(): TypedNode {
        mockFile()
        whenever(fileNode.isIncomingShare).thenReturn(true)
        whenever(fileNode.parentId).thenReturn(parentId)
        whenever(getNodeByIdUseCase(parentId)).thenReturn(parentNode)
        whenever(isNodeInBackupsUseCase(nodeId.longValue)).thenReturn(false)
        return fileNode
    }

    private suspend fun mockInShareFolder(): TypedNode {
        mockFolder()
        whenever(folderNode.isIncomingShare).thenReturn(true)
        whenever(folderNode.parentId).thenReturn(parentId)
        whenever(getNodeByIdUseCase(parentId)).thenReturn(parentNode)
        whenever(isNodeInBackupsUseCase(nodeId.longValue)).thenReturn(false)
        return folderNode
    }

    private suspend fun mockInShareRootFile(): TypedNode {
        mockFile()
        whenever(fileNode.isIncomingShare).thenReturn(true)
        whenever(isNodeInBackupsUseCase(nodeId.longValue)).thenReturn(false)
        return fileNode
    }

    private suspend fun mockInShareRootFolder(): TypedNode {
        mockFolder()
        whenever(folderNode.isIncomingShare).thenReturn(true)
        whenever(isNodeInBackupsUseCase(nodeId.longValue)).thenReturn(false)
        return folderNode
    }

    private fun <R> (suspend () -> R).isRootInShared() =
        this == this@GetAvailableNodeActionsUseCaseTest::mockInShareRootFolder
                || this == this@GetAvailableNodeActionsUseCaseTest::mockInShareRootFile

    private fun <R> (suspend () -> R).isInShared() =
        this.isRootInShared()
                || this == this@GetAvailableNodeActionsUseCaseTest::mockInShareFolder
                || this == this@GetAvailableNodeActionsUseCaseTest::mockInShareFile

    private fun <R> (suspend () -> R).isFolder() =
        this == this@GetAvailableNodeActionsUseCaseTest::mockInShareRootFolder
                || this == this@GetAvailableNodeActionsUseCaseTest::mockInShareFolder
                || this == this@GetAvailableNodeActionsUseCaseTest::mockFolder

    private fun <R> (suspend () -> R).isFile() = !this.isFolder()

    private companion object {
        val nodeId = NodeId(1L)
        val parentId = NodeId(2L)
        val invalidNode = NodeId(-1L)
    }

}