package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.FolderTypeData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

class GetFolderTypeDataUseCaseTest {
    private lateinit var underTest: GetFolderTypeDataUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()
    private val chatRepository = mock<ChatRepository>()
    private val monitorBackupFolder = mock<MonitorBackupFolder>()
    private val nodeRepository = mock<NodeRepository>()

    private val primarySyncHandle = 1234L
    private val secondarySyncHandle = 5678L
    private val chatFilesFolderId = NodeId(9012L)
    private val backupFolderId = NodeId(3456L)
    private val syncedNodeIds = setOf(NodeId(1111L), NodeId(2222L), NodeId(3333L))

    @Before
    fun setUp() {
        underTest = GetFolderTypeDataUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
            chatRepository = chatRepository,
            monitorBackupFolder = monitorBackupFolder,
            nodeRepository = nodeRepository
        )
    }

    @Test
    fun `test that invoke returns correct FolderTypeData when all data is available`() = runTest {
        cameraUploadsRepository.stub {
            onBlocking { getPrimarySyncHandle() }.thenReturn(primarySyncHandle)
            onBlocking { getSecondarySyncHandle() }.thenReturn(secondarySyncHandle)
        }
        chatRepository.stub {
            onBlocking { getChatFilesFolderId() }.thenReturn(chatFilesFolderId)
        }
        monitorBackupFolder.stub {
            onBlocking { invoke() }.thenReturn(flowOf(Result.success(backupFolderId)))
        }
        nodeRepository.stub {
            onBlocking { getAllSyncedNodeIds() }.thenReturn(syncedNodeIds)
        }

        val result = underTest()

        val expected = FolderTypeData(
            primarySyncHandle = primarySyncHandle,
            secondarySyncHandle = secondarySyncHandle,
            chatFilesFolderId = chatFilesFolderId,
            backupFolderId = backupFolderId,
            syncedNodeIds = syncedNodeIds
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that invoke returns correct FolderTypeData when all values are null`() = runTest {
        cameraUploadsRepository.stub {
            onBlocking { getPrimarySyncHandle() }.thenReturn(null)
            onBlocking { getSecondarySyncHandle() }.thenReturn(null)
        }
        chatRepository.stub {
            onBlocking { getChatFilesFolderId() }.thenReturn(null)
        }
        monitorBackupFolder.stub {
            onBlocking { invoke() }.thenReturn(flowOf(Result.failure(Exception("No backup folder"))))
        }
        nodeRepository.stub {
            onBlocking { getAllSyncedNodeIds() }.thenReturn(emptySet())
        }

        val result = underTest()

        val expected = FolderTypeData(
            primarySyncHandle = null,
            secondarySyncHandle = null,
            chatFilesFolderId = null,
            backupFolderId = null,
            syncedNodeIds = emptySet()
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that invoke handles partial data correctly`() = runTest {
        cameraUploadsRepository.stub {
            onBlocking { getPrimarySyncHandle() }.thenReturn(primarySyncHandle)
            onBlocking { getSecondarySyncHandle() }.thenReturn(null)
        }
        chatRepository.stub {
            onBlocking { getChatFilesFolderId() }.thenReturn(chatFilesFolderId)
        }
        monitorBackupFolder.stub {
            onBlocking { invoke() }.thenReturn(flowOf(Result.failure(Exception("No backup folder"))))
        }
        nodeRepository.stub {
            onBlocking { getAllSyncedNodeIds() }.thenReturn(setOf(NodeId(1111L)))
        }

        val result = underTest()

        val expected = FolderTypeData(
            primarySyncHandle = primarySyncHandle,
            secondarySyncHandle = null,
            chatFilesFolderId = chatFilesFolderId,
            backupFolderId = null,
            syncedNodeIds = setOf(NodeId(1111L))
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that invoke handles backup folder flow with multiple emissions`() = runTest {
        // Given
        val firstBackupId = NodeId(1111L)
        val secondBackupId = NodeId(2222L)

        cameraUploadsRepository.stub {
            onBlocking { getPrimarySyncHandle() }.thenReturn(primarySyncHandle)
            onBlocking { getSecondarySyncHandle() }.thenReturn(secondarySyncHandle)
        }
        chatRepository.stub {
            onBlocking { getChatFilesFolderId() }.thenReturn(chatFilesFolderId)
        }
        monitorBackupFolder.stub {
            onBlocking { invoke() }.thenReturn(
                flowOf(
                    Result.success(firstBackupId),
                    Result.success(secondBackupId)
                )
            )
        }
        nodeRepository.stub {
            onBlocking { getAllSyncedNodeIds() }.thenReturn(syncedNodeIds)
        }

        val result = underTest()

        // Should get the first emission (firstOrNull)
        val expected = FolderTypeData(
            primarySyncHandle = primarySyncHandle,
            secondarySyncHandle = secondarySyncHandle,
            chatFilesFolderId = chatFilesFolderId,
            backupFolderId = firstBackupId,
            syncedNodeIds = syncedNodeIds
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that invoke handles backup folder flow with failure then success`() = runTest {
        cameraUploadsRepository.stub {
            onBlocking { getPrimarySyncHandle() }.thenReturn(primarySyncHandle)
            onBlocking { getSecondarySyncHandle() }.thenReturn(secondarySyncHandle)
        }
        chatRepository.stub {
            onBlocking { getChatFilesFolderId() }.thenReturn(chatFilesFolderId)
        }
        monitorBackupFolder.stub {
            onBlocking { invoke() }.thenReturn(
                flowOf(
                    Result.failure(Exception("Backup folder not found")),
                    Result.success(backupFolderId)
                )
            )
        }
        nodeRepository.stub {
            onBlocking { getAllSyncedNodeIds() }.thenReturn(syncedNodeIds)
        }

        val result = underTest()

        // Should get the first emission which is a failure, so backupFolderId should be null
        val expected = FolderTypeData(
            primarySyncHandle = primarySyncHandle,
            secondarySyncHandle = secondarySyncHandle,
            chatFilesFolderId = chatFilesFolderId,
            backupFolderId = null,
            syncedNodeIds = syncedNodeIds
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that invoke handles empty backup folder flow`() = runTest {
        cameraUploadsRepository.stub {
            onBlocking { getPrimarySyncHandle() }.thenReturn(primarySyncHandle)
            onBlocking { getSecondarySyncHandle() }.thenReturn(secondarySyncHandle)
        }
        chatRepository.stub {
            onBlocking { getChatFilesFolderId() }.thenReturn(chatFilesFolderId)
        }
        monitorBackupFolder.stub {
            onBlocking { invoke() }.thenReturn(flowOf())
        }
        nodeRepository.stub {
            onBlocking { getAllSyncedNodeIds() }.thenReturn(syncedNodeIds)
        }

        val result = underTest()

        val expected = FolderTypeData(
            primarySyncHandle = primarySyncHandle,
            secondarySyncHandle = secondarySyncHandle,
            chatFilesFolderId = chatFilesFolderId,
            backupFolderId = null,
            syncedNodeIds = syncedNodeIds
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that invoke handles large synced node set`() = runTest {
        val largeSyncedNodeSet = (1..1000).map { NodeId(it.toLong()) }.toSet()

        cameraUploadsRepository.stub {
            onBlocking { getPrimarySyncHandle() }.thenReturn(primarySyncHandle)
            onBlocking { getSecondarySyncHandle() }.thenReturn(secondarySyncHandle)
        }
        chatRepository.stub {
            onBlocking { getChatFilesFolderId() }.thenReturn(chatFilesFolderId)
        }
        monitorBackupFolder.stub {
            onBlocking { invoke() }.thenReturn(flowOf(Result.success(backupFolderId)))
        }
        nodeRepository.stub {
            onBlocking { getAllSyncedNodeIds() }.thenReturn(largeSyncedNodeSet)
        }

        val result = underTest()

        val expected = FolderTypeData(
            primarySyncHandle = primarySyncHandle,
            secondarySyncHandle = secondarySyncHandle,
            chatFilesFolderId = chatFilesFolderId,
            backupFolderId = backupFolderId,
            syncedNodeIds = largeSyncedNodeSet
        )
        assertThat(result).isEqualTo(expected)
        assertThat(result.syncedNodeIds).hasSize(1000)
    }

    @Test
    fun `test that invoke calls all repositories exactly once`() = runTest {
        cameraUploadsRepository.stub {
            onBlocking { getPrimarySyncHandle() }.thenReturn(primarySyncHandle)
            onBlocking { getSecondarySyncHandle() }.thenReturn(secondarySyncHandle)
        }
        chatRepository.stub {
            onBlocking { getChatFilesFolderId() }.thenReturn(chatFilesFolderId)
        }
        monitorBackupFolder.stub {
            onBlocking { invoke() }.thenReturn(flowOf(Result.success(backupFolderId)))
        }
        nodeRepository.stub {
            onBlocking { getAllSyncedNodeIds() }.thenReturn(syncedNodeIds)
        }

        underTest()

        verify(cameraUploadsRepository).getPrimarySyncHandle()
        verify(cameraUploadsRepository).getSecondarySyncHandle()
        verify(chatRepository).getChatFilesFolderId()
        verify(monitorBackupFolder).invoke()
        verify(nodeRepository).getAllSyncedNodeIds()
    }
}