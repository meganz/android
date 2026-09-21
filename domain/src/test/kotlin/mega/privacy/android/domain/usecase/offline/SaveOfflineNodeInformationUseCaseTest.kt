package mega.privacy.android.domain.usecase.offline

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.MonitorBackupFolder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaveOfflineNodeInformationUseCaseTest {

    private val nodeRepository: NodeRepository = mock()
    private val getOfflineNodeInformationUseCase: GetOfflineNodeInformationUseCase = mock()
    private val node: FileNode = mock()
    private val parent: FolderNode = mock()
    private val nodeOfflineInformation: OtherOfflineNodeInformation = mock()
    private val parentOfflineInformation: OtherOfflineNodeInformation = mock()
    private val monitorBackupFolder: MonitorBackupFolder = mock()
    private val parentParentOfflineNodeInformation = mock<OtherOfflineNodeInformation>()

    private lateinit var underTest: SaveOfflineNodeInformationUseCase

    @BeforeAll
    fun setup() {
        underTest = SaveOfflineNodeInformationUseCase(
            nodeRepository,
            monitorBackupFolder,
            getOfflineNodeInformationUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            nodeRepository,
            getOfflineNodeInformationUseCase,
            node,
            parent,
            nodeOfflineInformation,
            parentOfflineInformation,
            monitorBackupFolder,
        )
        nodeRepository.stub {
            on {
                saveOfflineNodeInformation(
                    eq(nodeOfflineInformation),
                    anyOrNull(),
                    anyOrNull()
                )
            }.thenReturn(
                nodeOfflineInformationId
            )

            on {
                saveOfflineNodeInformation(
                    eq(parentOfflineInformation),
                    anyOrNull(),
                    anyOrNull()
                )
            }.thenReturn(
                nodeParentOfflineInformationId
            )
        }
    }

    @Test
    fun `test that information is not saved if it's already saved`() = runTest {
        stubDriveNodeWithoutParent()

        whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(mock<OtherOfflineNodeInformation>())

        underTest(transferFinishEvent)
        verify(nodeRepository, times(0))
            .saveOfflineNodeInformation(anyOrNull(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `test that node is saved without parents when it's a root parent`() = runTest {
        stubDriveNodeWithoutParent()
        stubNodeOfflineInfo()

        whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(null)

        underTest(transferFinishEvent)
        verify(nodeRepository).saveOfflineNodeInformation(nodeOfflineInformation, null, false)
    }

    @Test
    fun `test that node and its parent are saved when the node has a parent`() = runTest {
        stubDriveNodeWithParent()
        stubNodeOfflineInfo()
        stubParentOfflineInfo()

        whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(null)

        underTest(transferFinishEvent)
        verify(nodeRepository).saveOfflineNodeInformation(
            nodeOfflineInformation,
            nodeParentOfflineInformationId,
            false
        )
        verify(nodeRepository).saveOfflineNodeInformation(
            parentOfflineInformation,
            null,
            false
        )
    }

    @Test
    fun `test that node is saved without parent when the parent is drive root node`() = runTest {
        stubDriveNodeWithParent()
        stubNodeOfflineInfo()
        stubParentOfflineInfo()

        whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(null)
        whenever(nodeRepository.getRootNode()).thenReturn(parent)

        underTest(transferFinishEvent)
        verify(nodeRepository).saveOfflineNodeInformation(nodeOfflineInformation, null, false)
        verify(nodeRepository, times(0))
            .saveOfflineNodeInformation(parentOfflineInformation, null, false)
    }

    @Test
    fun `test that node is saved without parent when the node is backup root node`() = runTest {
        stubDriveNodeWithParent()
        stubNodeOfflineInfo()
        stubParentOfflineInfo()

        whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(null)
        whenever(monitorBackupFolder()).thenReturn(flowOf(Result.success(backupId)))

        underTest(transferFinishEvent)
        verify(nodeRepository).saveOfflineNodeInformation(
            nodeOfflineInformation,
            nodeParentOfflineInformationId,
            false
        )
        verify(nodeRepository).saveOfflineNodeInformation(
            parentOfflineInformation,
            null,
            false
        )
    }

    @Test
    fun `test that node is saved as offline root when the transfer is a root transfer`() = runTest {
        stubDriveNodeWithParent()
        stubNodeOfflineInfo()
        stubParentOfflineInfo()

        whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(null)

        underTest(rootTransferFinishEvent)

        verify(nodeRepository).saveOfflineNodeInformation(
            nodeOfflineInformation,
            nodeParentOfflineInformationId,
            true
        )
    }

    @Test
    fun `test that ancestors are not saved as offline roots when the transfer is a root transfer`() =
        runTest {
            stubDriveNodeWithParent()
            stubNodeOfflineInfo()
            stubParentOfflineInfo()

            whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(null)

            underTest(rootTransferFinishEvent)

            verify(nodeRepository).saveOfflineNodeInformation(
                parentOfflineInformation,
                null,
                false
            )
        }

    @Test
    fun `test that node is not saved as offline root when the transfer is a child transfer`() =
        runTest {
            stubDriveNodeWithParent()
            stubNodeOfflineInfo()
            stubParentOfflineInfo()

            whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(null)

            underTest(transferFinishEvent)

            verify(nodeRepository).saveOfflineNodeInformation(
                nodeOfflineInformation,
                nodeParentOfflineInformationId,
                false
            )
        }

    @Test
    fun `test that an already saved node is marked as offline root when the transfer is a root transfer`() =
        runTest {
            stubDriveNodeWithoutParent()
            val alreadySaved = mock<OtherOfflineNodeInformation> {
                on { id } doReturn nodeOfflineInformationId.toInt()
            }
            whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(alreadySaved)

            underTest(rootTransferFinishEvent)

            verify(nodeRepository).setOfflineNodeRoot(nodeOfflineInformationId.toInt(), true)
        }

    @Test
    fun `test that an already saved node is not marked as offline root when the transfer is a child transfer`() =
        runTest {
            stubDriveNodeWithoutParent()
            val alreadySaved = mock<OtherOfflineNodeInformation> {
                on { id } doReturn nodeOfflineInformationId.toInt()
            }
            whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(alreadySaved)

            underTest(transferFinishEvent)

            verify(nodeRepository, times(0)).setOfflineNodeRoot(anyOrNull(), anyOrNull())
        }

    @Test
    fun `test that chat node is obtained from the repository when app data is chat download`() =
        runTest {
            val chatId = 458L
            val msgId = 943L
            val msgIndex = 75
            stubDriveNodeWithoutParent()
            stubNodeOfflineInfo()
            whenever(nodeRepository.getNodeById(nodeId)) doReturn null
            whenever(nodeRepository.getNodeFromChatMessage(chatId, msgId, msgIndex)) doReturn node
            whenever(transfer.appData) doReturn listOf(
                TransferAppData.ChatDownload(chatId, msgId, msgIndex)
            )
            whenever(nodeRepository.getOfflineNodeInformation(nodeId)).thenReturn(null)

            underTest(transferFinishEvent)

            verify(nodeRepository).saveOfflineNodeInformation(nodeOfflineInformation, null, false)
        }

    private fun stubDriveNodeWithoutParent() = runTest {
        whenever(nodeRepository.getNodeById(nodeId)).thenReturn(node)
        whenever(node.id).thenReturn(nodeId)
        whenever(node.parentId).thenReturn(invalidId)
        whenever(nodeRepository.getNodeById(invalidId)).thenReturn(null)
        whenever(monitorBackupFolder()).thenReturn(flowOf(Result.failure(Throwable())))
    }

    private fun stubDriveNodeWithParent() = runTest {
        whenever(nodeRepository.getNodeById(nodeId)).thenReturn(node)
        whenever(node.id).thenReturn(nodeId)
        whenever(node.parentId).thenReturn(parentId)
        whenever(nodeRepository.getNodeById(parentId)).thenReturn(parent)
        whenever(parent.id).thenReturn(parentId)
        whenever(parent.parentId).thenReturn(parentParentId)
        whenever(nodeRepository.getOfflineNodeInformation(parentParentId)).thenReturn(
            parentParentOfflineNodeInformation
        )
        whenever(parentParentOfflineNodeInformation.id).thenReturn(
            nodeParentParentOfflineInformationId.toInt()
        )
        whenever(monitorBackupFolder()).thenReturn(flowOf(Result.failure(Throwable())))
    }

    private fun stubNodeOfflineInfo() = runTest {
        whenever(nodeOfflineInformation.id).thenReturn(nodeOfflineInformationId.toInt())
        whenever(getOfflineNodeInformationUseCase(node, originalName))
            .thenReturn(nodeOfflineInformation)
    }

    private fun stubParentOfflineInfo() = runTest {
        whenever(parentOfflineInformation.id).thenReturn(nodeParentOfflineInformationId.toInt())
        whenever(getOfflineNodeInformationUseCase(parent))
            .thenReturn(parentOfflineInformation)
    }

    companion object {
        private val nodeId = NodeId(1L)
        private val parentId = NodeId(2L)
        private val parentParentId = NodeId(3L)
        private val invalidId = NodeId(-1L)
        private val backupId = NodeId(2L)
        private const val originalName = "originalName"
        private const val nodeOfflineInformationId = 5L
        private const val nodeParentOfflineInformationId = 6L
        private const val nodeParentParentOfflineInformationId = 7L
        private val transfer = mock<Transfer> {
            on { this.nodeHandle } doReturn nodeId.longValue
            on { this.fileName } doReturn originalName
            on { this.isRootTransfer } doReturn false
        }
        private val transferFinishEvent = TransferEvent.TransferFinishEvent(transfer, null)
        private val rootTransfer = mock<Transfer> {
            on { this.nodeHandle } doReturn nodeId.longValue
            on { this.fileName } doReturn originalName
            on { this.isRootTransfer } doReturn true
        }
        private val rootTransferFinishEvent = TransferEvent.TransferFinishEvent(rootTransfer, null)
    }
}
