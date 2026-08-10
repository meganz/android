package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeAction
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [GetAvailableNodeActionsUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetAvailableNodeActionsUseCaseTest {

    private val fileNode = mock<TypedFileNode>()
    private val folderNode = mock<TypedFolderNode>()

    private val parentNode = mock<TypedFolderNode> {
        on { id }.thenReturn(parentId)
    }
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()
    private val isNodeInBackupsUseCase = mock<IsNodeInBackupsUseCase>()
    private val underTest = GetAvailableNodeActionsUseCase(
        getNodeByIdUseCase = getNodeByIdUseCase,
        isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
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
            isNodeInRubbishBinUseCase,
            isNodeInBackupsUseCase,
        )
    }


    @TestFactory
    fun `test that Delete action is returned if the node is in rubbish bin`() =
        listOf(true, false).flatMap { inRubbish ->
            listOf(true, false).flatMap { isNodeKeyDecrypted ->
                mockedNodes.map { (name, mockNode) ->
                    dynamicTest("node: $name, in rubbish bin: $inRubbish, isNodeKeyDecrypted: $isNodeKeyDecrypted") {
                        runTest {
                            val node = mockNode()
                            whenever(isNodeInRubbishBinUseCase(node.id)).thenReturn(inRubbish)
                            whenever(node.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
                            val result = underTest(node)
                            if (inRubbish) {
                                Truth.assertThat(result).containsExactly(NodeAction.Delete)
                            } else if (!isNodeKeyDecrypted) {
                                Truth.assertThat(result).isEmpty()
                            } else {
                                Truth.assertThat(result).doesNotContain(NodeAction.Delete)
                            }
                        }
                    }
                }
            }
        }


    @TestFactory
    fun `test that Send to chat action is returned if the node is a file and not taken down and key is decrypted`() =
        listOf(true, false).flatMap { takenDown ->
            listOf(true, false).flatMap { isNodeKeyDecrypted ->
                mockedNodes.map { (name, mockNode) ->
                    val isFile = mockNode.isFile()
                    dynamicTest("node: $name, takeDown: $takenDown, isFile: $isFile, isNodeKeyDecrypted: $isNodeKeyDecrypted") {
                        runTest {
                            val node = mockNode()
                            whenever(node.isTakenDown).thenReturn(takenDown)
                            whenever(node.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
                            whenever(getNodeAccessPermission(node.id)).thenReturn(AccessPermission.OWNER)
                            val result = underTest(node)

                            if (!isNodeKeyDecrypted) {
                                Truth.assertThat(result).isEmpty()
                            } else if (takenDown || !isFile) {
                                Truth.assertThat(result).doesNotContain(NodeAction.SendToChat)
                            } else {
                                Truth.assertThat(result).contains(NodeAction.SendToChat)
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Download action is returned if the node is not taken down and key is decrypted`() =
        listOf(true, false).flatMap { takenDown ->
            listOf(true, false).flatMap { isNodeKeyDecrypted ->
                mockedNodes.map { (name, mockNode) ->
                    dynamicTest("node: $name, takeDown: $takenDown, isNodeKeyDecrypted: $isNodeKeyDecrypted") {
                        runTest {
                            val node = mockNode()
                            whenever(node.isTakenDown).thenReturn(takenDown)
                            whenever(node.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
                            whenever(getNodeAccessPermission(node.id)).thenReturn(AccessPermission.OWNER)
                            val result = underTest(node)

                            if (!isNodeKeyDecrypted) {
                                Truth.assertThat(result).isEmpty()
                            } else if (takenDown) {
                                Truth.assertThat(result).doesNotContain(NodeAction.Download)
                            } else {
                                Truth.assertThat(result).contains(NodeAction.Download)
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Copy action is returned if the node is not taken down and key is decrypted`() =
        listOf(true, false).flatMap { takenDown ->
            listOf(true, false).flatMap { isNodeKeyDecrypted ->
                mockedNodes.map { (name, mockNode) ->
                    dynamicTest("node: $name, takeDown: $takenDown, isNodeKeyDecrypted: $isNodeKeyDecrypted") {
                        runTest {
                            val node = mockNode()
                            whenever(node.isTakenDown).thenReturn(takenDown)
                            whenever(node.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
                            whenever(getNodeAccessPermission(node.id)).thenReturn(AccessPermission.OWNER)
                            val result = underTest(node)

                            if (!isNodeKeyDecrypted) {
                                Truth.assertThat(result).isEmpty()
                            } else if (takenDown) {
                                Truth.assertThat(result).doesNotContain(NodeAction.Copy)
                            } else {
                                Truth.assertThat(result).contains(NodeAction.Copy)
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Leave action is returned if the node is not taken down and is a shared root node and key is decrypted`() =
        listOf(true, false).flatMap { takenDown ->
            listOf(true, false).flatMap { isNodeKeyDecrypted ->
                mockedNodes.map { (name, mockNode) ->
                    val root = mockNode.isRootInShared()
                    dynamicTest("node: $name, root: $root, takenDown: $takenDown, isNodeKeyDecrypted: $isNodeKeyDecrypted") {
                        runTest {
                            val node = mockNode()
                            whenever(node.isTakenDown).thenReturn(takenDown)
                            whenever(node.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
                            whenever(getNodeAccessPermission(node.id)).thenReturn(if (root) AccessPermission.READ else AccessPermission.OWNER)
                            // For non-root shared nodes, we need to set up parent node
                            if (!root && mockNode.isInShared()) {
                                whenever(node.parentId).thenReturn(parentId)
                                whenever(getNodeByIdUseCase(parentId)).thenReturn(parentNode)
                                whenever(parentNode.parentId).thenReturn(invalidNode)
                            }
                            val result = underTest(node)

                            if (!isNodeKeyDecrypted) {
                                Truth.assertThat(result).isEmpty()
                            } else if (root && !takenDown) {
                                Truth.assertThat(result).contains(NodeAction.Leave)
                            } else {
                                Truth.assertThat(result).doesNotContain(NodeAction.Leave)
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Rename action is returned if the non backup node is not a shared node, or is a shared node and has owner permission and key is decrypted`() =
        listOf(
            AccessPermission.OWNER,
            AccessPermission.READ,
            AccessPermission.FULL,
            AccessPermission.READWRITE
        ).flatMap { permission ->
            listOf(true, false).flatMap { isNodeKeyDecrypted ->
                mockedNodes.map { (name, mockNode) ->
                    dynamicTest("node $name, permission: $permission, isNodeKeyDecrypted: $isNodeKeyDecrypted") {
                        runTest {
                            val node = mockNode()
                            whenever(getNodeAccessPermission(node.id)).thenReturn(permission)
                            whenever(isNodeInBackupsUseCase(node.id.longValue)).thenReturn(false)
                            whenever(node.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
                            // Set up parent node for non-OWNER permissions to check incomingShareFirstLevel
                            if (permission != AccessPermission.OWNER) {
                                val isRoot = mockNode.isRootInShared()
                                if (isRoot) {
                                    whenever(node.parentId).thenReturn(invalidNode)
                                } else if (mockNode.isInShared()) {
                                    whenever(node.parentId).thenReturn(parentId)
                                    whenever(getNodeByIdUseCase(parentId)).thenReturn(parentNode)
                                    whenever(parentNode.parentId).thenReturn(invalidNode)
                                }
                            }
                            val result = underTest(node)

                            if (!isNodeKeyDecrypted) {
                                Truth.assertThat(result).isEmpty()
                            } else if (permission == AccessPermission.OWNER || permission == AccessPermission.FULL) {
                                Truth.assertThat(result).contains(NodeAction.Rename)
                            } else {
                                Truth.assertThat(result).doesNotContain(NodeAction.Rename)
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Move to rubbish action is returned if the non backup node is not a shared node, or is a shared root node and has owner permission and key is decrypted`() =
        listOf(
            AccessPermission.FULL,
            AccessPermission.READ,
            AccessPermission.OWNER,
            AccessPermission.READWRITE,
            AccessPermission.UNKNOWN
        ).flatMap { permission ->
            listOf(true, false).flatMap { isNodeKeyDecrypted ->
                mockedNodes.map { (name, mockNode) ->
                    val shared = mockNode.isInShared()
                    val root = mockNode.isRootInShared()
                    dynamicTest("node: $name, shared: $shared, permission: $permission, in shared root:$root, isNodeKeyDecrypted: $isNodeKeyDecrypted") {
                        runTest {
                            val node = mockNode()
                            whenever(isNodeInBackupsUseCase(node.id.longValue)).thenReturn(false)
                            whenever(getNodeAccessPermission(node.id)).thenReturn(permission)
                            whenever(node.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
                            // Set up parent node for incomingShareFirstLevel check
                            if (root) {
                                whenever(node.parentId).thenReturn(invalidNode)
                            } else if (shared) {
                                whenever(node.parentId).thenReturn(parentId)
                                whenever(getNodeByIdUseCase(parentId)).thenReturn(parentNode)
                                whenever(parentNode.parentId).thenReturn(invalidNode)
                            }
                            val result = underTest(node)

                            if (!isNodeKeyDecrypted) {
                                Truth.assertThat(result).isEmpty()
                            } else {
                                val incomingShareFirstLevel =
                                    node.parentId.longValue == -1L || getNodeByIdUseCase(node.parentId)?.parentId?.longValue == -1L
                                if ((permission == AccessPermission.OWNER) || (permission == AccessPermission.FULL && incomingShareFirstLevel)) {
                                    Truth.assertThat(result).contains(NodeAction.MoveToRubbishBin)
                                } else {
                                    Truth.assertThat(result).doesNotContain(NodeAction.MoveToRubbishBin)
                                }
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Move action is returned if the non backup node is not a shared node and key is decrypted`() =
        listOf(true, false).flatMap { takenDown ->
            listOf(true, false).flatMap { isNodeKeyDecrypted ->
                mockedNodes.map { (name, mockNode) ->
                    val shared = mockNode.isInShared()
                    dynamicTest("node: $name, shared: $shared, takenDown: $takenDown, isNodeKeyDecrypted: $isNodeKeyDecrypted") {
                        runTest {
                            val node = mockNode()
                            whenever(node.isTakenDown).thenReturn(takenDown)
                            whenever(node.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
                            whenever(isNodeInBackupsUseCase(node.id.longValue)).thenReturn(false)
                            whenever(getNodeAccessPermission(node.id)).thenReturn(if (shared) AccessPermission.READ else AccessPermission.OWNER)
                            val result = underTest(node)

                            if (!isNodeKeyDecrypted) {
                                Truth.assertThat(result).isEmpty()
                            } else if (!shared) {
                                Truth.assertThat(result).contains(NodeAction.Move)
                            } else {
                                Truth.assertThat(result).doesNotContain(NodeAction.Move)
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that share folder action is returned if the node is a folder, not taken down, and not a shared folder and key is decrypted`() =
        listOf(true, false).flatMap { takenDown ->
            listOf(true, false).flatMap { isNodeKeyDecrypted ->
                mockedNodes.map { (name, mockNode) ->
                    val inShared = mockNode.isInShared()
                    val folder = mockNode.isFolder()
                    dynamicTest("node: $name, takenDown: $takenDown, inShared: $inShared, folder: $folder, isNodeKeyDecrypted: $isNodeKeyDecrypted") {
                        runTest {
                            val node = mockNode()
                            whenever(getNodeAccessPermission(node.id)).thenReturn(if (inShared) AccessPermission.READ else AccessPermission.OWNER)
                            whenever(node.isTakenDown).thenReturn(takenDown)
                            whenever(node.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
                            val result = underTest(node)

                            if (!isNodeKeyDecrypted) {
                                Truth.assertThat(result).isEmpty()
                            } else if (takenDown || inShared || !folder) {
                                Truth.assertThat(result).doesNotContain(NodeAction.ShareFolder)
                            } else {
                                Truth.assertThat(result).contains(NodeAction.ShareFolder)
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Manage link action is returned if the node is exported, not taken down, and not a shared node and key is decrypted`() =
        listOf(true, false).flatMap { takenDown ->
            listOf(true, false).flatMap { exported ->
                listOf(true, false).flatMap { isNodeKeyDecrypted ->
                    mockedNodes.map { (name, mockNode) ->
                        val inShared = mockNode.isInShared()
                        dynamicTest("node: $name, takenDown: $takenDown, inShared: $inShared, exported: $exported, isNodeKeyDecrypted: $isNodeKeyDecrypted") {
                            runTest {
                                val node = mockNode()
                                whenever(node.isTakenDown).thenReturn(takenDown)
                                whenever(node.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
                                whenever(getNodeAccessPermission(node.id)).thenReturn(if (inShared) AccessPermission.READ else AccessPermission.OWNER)
                                whenever(node.exportedData).thenReturn(if (exported) mock() else null)
                                val result = underTest(node)

                                if (!isNodeKeyDecrypted) {
                                    Truth.assertThat(result).isEmpty()
                                } else if (exported && !takenDown && !inShared) {
                                    Truth.assertThat(result).contains(NodeAction.ManageLink)
                                } else {
                                    Truth.assertThat(result).doesNotContain(NodeAction.ManageLink)
                                }
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Remove link action is returned if the node is exported, not taken down, and not a shared node and key is decrypted`() =
        listOf(true, false).flatMap { takenDown ->
            listOf(true, false).flatMap { exported ->
                listOf(true, false).flatMap { isNodeKeyDecrypted ->
                    mockedNodes.map { (name, mockNode) ->
                        val inShared = mockNode.isInShared()
                        dynamicTest("node: $name, takenDown: $takenDown, inShared: $inShared, exported: $exported, isNodeKeyDecrypted: $isNodeKeyDecrypted") {
                            runTest {
                                val node = mockNode()
                                whenever(node.isTakenDown).thenReturn(takenDown)
                                whenever(node.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
                                whenever(node.exportedData).thenReturn(if (exported) mock() else null)
                                whenever(getNodeAccessPermission(node.id)).thenReturn(if (inShared) AccessPermission.READ else AccessPermission.OWNER)
                                val result = underTest(node)

                                if (!isNodeKeyDecrypted) {
                                    Truth.assertThat(result).isEmpty()
                                } else if (exported && !takenDown && !inShared) {
                                    Truth.assertThat(result).contains(NodeAction.RemoveLink)
                                } else {
                                    Truth.assertThat(result).doesNotContain(NodeAction.RemoveLink)
                                }
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Get link action is returned if the node is not exported, not taken down, and not a shared node and key is decrypted`() =
        listOf(true, false).flatMap { takenDown ->
            listOf(true, false).flatMap { exported ->
                listOf(true, false).flatMap { isNodeKeyDecrypted ->
                    mockedNodes.map { (name, mockNode) ->
                        val inShared = mockNode.isInShared()
                        dynamicTest("node: $name, takenDown: $takenDown, inShared: $inShared, exported: $exported, isNodeKeyDecrypted: $isNodeKeyDecrypted") {
                            runTest {
                                val node = mockNode()
                                whenever(node.isTakenDown).thenReturn(takenDown)
                                whenever(node.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
                                whenever(node.exportedData).thenReturn(if (exported) mock() else null)
                                whenever(getNodeAccessPermission(node.id)).thenReturn(if (inShared) AccessPermission.READ else AccessPermission.OWNER)
                                val result = underTest(node)

                                if (!isNodeKeyDecrypted) {
                                    Truth.assertThat(result).isEmpty()
                                } else if (!exported && !takenDown && !inShared) {
                                    Truth.assertThat(result).contains(NodeAction.GetLink)
                                } else {
                                    Truth.assertThat(result).doesNotContain(NodeAction.GetLink)
                                }
                            }
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `test that Dispute taken down action is returned if the node is taken down and not a shared node and key is decrypted`() =
        listOf(true, false).flatMap { takenDown ->
            listOf(true, false).flatMap { isNodeKeyDecrypted ->
                mockedNodes.map { (name, mockNode) ->
                    val inShared = mockNode.isInShared()
                    dynamicTest("node: $name, takenDown: $takenDown, inShared: $inShared, isNodeKeyDecrypted: $isNodeKeyDecrypted") {
                        runTest {
                            val node = mockNode()
                            whenever(node.isTakenDown).thenReturn(takenDown)
                            whenever(node.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
                            whenever(getNodeAccessPermission(node.id)).thenReturn(if (inShared) AccessPermission.READ else AccessPermission.OWNER)
                            val result = underTest(node)

                            if (!isNodeKeyDecrypted) {
                                Truth.assertThat(result).isEmpty()
                            } else if (takenDown && !inShared) {
                                Truth.assertThat(result).contains(NodeAction.DisputeTakedown)
                            } else {
                                Truth.assertThat(result).doesNotContain(NodeAction.DisputeTakedown)
                            }
                        }
                    }
                }
            }
        }

    @ParameterizedTest(name = "folder with access permission : {0}")
    @EnumSource(AccessPermission::class)
    fun `test that share folder action is not returned when node is an incoming share`(permission: AccessPermission) {
        listOf(true, false).forEach { isNodeKeyDecrypted ->
            mockedNodes.forEach { (name, mockNode) ->
                runTest {
                    val node = mockNode()
                    whenever(getNodeAccessPermission(node.id)).thenReturn(permission)
                    whenever(node.isTakenDown).thenReturn(false)
                    whenever(node.isIncomingShare).thenReturn(false)
                    whenever(node.isNodeKeyDecrypted).thenReturn(isNodeKeyDecrypted)
                    val result = underTest(node)
                    if (!isNodeKeyDecrypted) {
                        Truth.assertThat(result).isEmpty()
                    } else if (permission == AccessPermission.OWNER && mockNode.isFolder()) {
                        Truth.assertThat(result).contains(NodeAction.ShareFolder)
                    } else {
                        Truth.assertThat(result).doesNotContain(NodeAction.ShareFolder)
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
        whenever(isNodeInRubbishBinUseCase(nodeId)).thenReturn(false)
        whenever(folderNode.isIncomingShare).thenReturn(false)
        whenever(folderNode.parentId).thenReturn(invalidNode)
        whenever(isNodeInBackupsUseCase(nodeId.longValue)).thenReturn(false)
        return folderNode
    }

    private suspend fun mockFile(): TypedNode {
        resetMocks()
        whenever(getNodeByIdUseCase.invoke(nodeId)).thenReturn(fileNode)
        whenever(fileNode.id).thenReturn(nodeId)
        whenever(isNodeInRubbishBinUseCase(nodeId)).thenReturn(false)
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
