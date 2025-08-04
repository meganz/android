package mega.privacy.android.feature.sync.data.mapper.stalledissue

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import nz.mega.sdk.MegaSyncStall
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StalledIssuesMapperTest {

    private lateinit var stalledIssueTypeMapper: StalledIssueTypeMapper
    private lateinit var underTest: StalledIssuesMapper

    @BeforeEach
    fun setUp() {
        stalledIssueTypeMapper = mock()
        underTest = StalledIssuesMapper(stalledIssueTypeMapper)
    }

    @Test
    fun `test that stalled issues sdk object is mapped correctly`() {
        val syncId = 323L
        val stalledIssueNodeName = "somefile.txt"
        val syncLocalFolderPath = "/storage/emulated/0/some_sync"
        val stalledIssueDebugString = "Local and remote changed since last synced"
        val syncs = createTestSyncs(syncId, syncLocalFolderPath, stalledIssueNodeName)
        val stalledIssueNodeId = 20L
        val stalledIssueSDKObject = createMockStalledIssue(
            nodeId = stalledIssueNodeId,
            nodeName = stalledIssueNodeName,
            localPath = "$syncLocalFolderPath/somefile.txt",
            debugString = stalledIssueDebugString,
            reason = MegaSyncStall.SyncStallReason.LocalAndRemoteChangedSinceLastSyncedState_userMustChoose
        )
        val stalledIssuesSDKList = listOf(stalledIssueSDKObject)

        whenever(stalledIssueTypeMapper(MegaSyncStall.SyncStallReason.LocalAndRemoteChangedSinceLastSyncedState_userMustChoose))
            .thenReturn(StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose)

        val actual = underTest(syncs, stalledIssuesSDKList)

        Truth.assertThat(actual).hasSize(1)
        val result = actual.first()
        Truth.assertThat(result.syncId).isEqualTo(syncId)
        Truth.assertThat(result.nodeIds).containsExactly(NodeId(stalledIssueNodeId))
        Truth.assertThat(result.localPaths).containsExactly("$syncLocalFolderPath/somefile.txt")
        Truth.assertThat(result.issueType)
            .isEqualTo(StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose)
        Truth.assertThat(result.conflictName).isEqualTo(stalledIssueDebugString)
        Truth.assertThat(result.nodeNames).containsExactly(stalledIssueNodeName)

        verify(stalledIssueTypeMapper).invoke(MegaSyncStall.SyncStallReason.LocalAndRemoteChangedSinceLastSyncedState_userMustChoose)
    }

    @Test
    fun `test that empty syncs list returns empty result`() {
        val stalledIssueSDKObject = createMockStalledIssue()
        val stalledIssuesSDKList = listOf(stalledIssueSDKObject)

        val actual = underTest(emptyList(), stalledIssuesSDKList)

        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `test that empty stalled issues list returns empty result`() {
        val syncs = createTestSyncs(1L, "/test/path", "test_folder")

        val actual = underTest(syncs, emptyList())

        Truth.assertThat(actual).isEmpty()
    }

    @Test
    fun `test that stalled issue with no matching sync returns invalid sync id`() {
        val syncs = createTestSyncs(1L, "/different/path", "different_folder")
        val stalledIssueSDKObject = createMockStalledIssue(
            localPath = "/unrelated/path/file.txt"
        )
        val stalledIssuesSDKList = listOf(stalledIssueSDKObject)

        whenever(stalledIssueTypeMapper(MegaSyncStall.SyncStallReason.LocalAndRemoteChangedSinceLastSyncedState_userMustChoose))
            .thenReturn(StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose)

        val actual = underTest(syncs, stalledIssuesSDKList)

        Truth.assertThat(actual).hasSize(1)
        Truth.assertThat(actual.first().syncId).isEqualTo(-1L)
    }

    @Test
    fun `test that multiple stalled issues are mapped correctly`() {
        val syncs = createTestSyncs(1L, "/test/path", "test_folder")
        val stalledIssue1 = createMockStalledIssue(
            nodeId = 10L,
            nodeName = "file1.txt",
            localPath = "/test/path/file1.txt"
        )
        val stalledIssue2 = createMockStalledIssue(
            nodeId = 20L,
            nodeName = "file2.txt",
            localPath = "/test/path/file2.txt"
        )
        val stalledIssuesSDKList = listOf(stalledIssue1, stalledIssue2)

        whenever(stalledIssueTypeMapper(MegaSyncStall.SyncStallReason.LocalAndRemoteChangedSinceLastSyncedState_userMustChoose))
            .thenReturn(StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose)

        val actual = underTest(syncs, stalledIssuesSDKList)

        Truth.assertThat(actual).hasSize(2)
        Truth.assertThat(actual[0].nodeIds).containsExactly(NodeId(10L))
        Truth.assertThat(actual[1].nodeIds).containsExactly(NodeId(20L))
    }

    @Test
    fun `test that stalled issue matches by remote folder name when local path does not match`() {
        val syncs = createTestSyncs(1L, "/different/local/path", "test_folder")
        val stalledIssueSDKObject = createMockStalledIssue(
            nodeName = "test_folder/some_file.txt",
            localPath = "/unrelated/path/file.txt"
        )
        val stalledIssuesSDKList = listOf(stalledIssueSDKObject)

        whenever(stalledIssueTypeMapper(MegaSyncStall.SyncStallReason.LocalAndRemoteChangedSinceLastSyncedState_userMustChoose))
            .thenReturn(StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose)

        val actual = underTest(syncs, stalledIssuesSDKList)

        Truth.assertThat(actual).hasSize(1)
        Truth.assertThat(actual.first().syncId).isEqualTo(1L)
    }

    @Test
    fun `test that stalled issue with multiple nodes and paths is handled correctly`() {
        val syncs = createTestSyncs(1L, "/test/path", "test_folder")
        val stalledIssueSDKObject = mock<MegaSyncStall>()

        whenever(stalledIssueSDKObject.pathCount(true)).thenReturn(2)
        whenever(stalledIssueSDKObject.cloudNodeHandle(0)).thenReturn(10L)
        whenever(stalledIssueSDKObject.path(true, 0)).thenReturn("file1.txt")
        whenever(stalledIssueSDKObject.cloudNodeHandle(1)).thenReturn(20L)
        whenever(stalledIssueSDKObject.path(true, 1)).thenReturn("file2.txt")

        whenever(stalledIssueSDKObject.pathCount(false)).thenReturn(2)
        whenever(stalledIssueSDKObject.path(false, 0)).thenReturn("/test/path/file1.txt")
        whenever(stalledIssueSDKObject.path(false, 1)).thenReturn("/test/path/file2.txt")

        whenever(stalledIssueSDKObject.reasonDebugString()).thenReturn("Multiple files conflict")
        whenever(stalledIssueSDKObject.reason()).thenReturn(MegaSyncStall.SyncStallReason.LocalAndRemoteChangedSinceLastSyncedState_userMustChoose)

        val stalledIssuesSDKList = listOf(stalledIssueSDKObject)

        whenever(stalledIssueTypeMapper(MegaSyncStall.SyncStallReason.LocalAndRemoteChangedSinceLastSyncedState_userMustChoose))
            .thenReturn(StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose)

        val actual = underTest(syncs, stalledIssuesSDKList)

        Truth.assertThat(actual).hasSize(1)
        val result = actual.first()
        Truth.assertThat(result.nodeIds).containsExactly(NodeId(10L), NodeId(20L))
        Truth.assertThat(result.localPaths)
            .containsExactly("/test/path/file1.txt", "/test/path/file2.txt")
        Truth.assertThat(result.nodeNames).containsExactly("file1.txt", "file2.txt")
    }

    private fun createTestSyncs(
        syncId: Long,
        localFolderPath: String,
        remoteFolderName: String,
    ): List<FolderPair> = listOf(
        FolderPair(
            id = syncId,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "",
            localFolderPath = localFolderPath,
            remoteFolder = RemoteFolder(id = NodeId(1L), name = remoteFolderName),
            syncStatus = SyncStatus.SYNCED
        )
    )

    private fun createMockStalledIssue(
        nodeId: Long = 1L,
        nodeName: String = "test_file.txt",
        localPath: String = "/test/path/test_file.txt",
        debugString: String = "Test debug string",
        reason: MegaSyncStall.SyncStallReason = MegaSyncStall.SyncStallReason.LocalAndRemoteChangedSinceLastSyncedState_userMustChoose,
    ): MegaSyncStall {
        val mock = mock<MegaSyncStall>()
        whenever(mock.pathCount(true)).thenReturn(1)
        whenever(mock.cloudNodeHandle(0)).thenReturn(nodeId)
        whenever(mock.path(true, 0)).thenReturn(nodeName)
        whenever(mock.pathCount(false)).thenReturn(1)
        whenever(mock.path(false, 0)).thenReturn(localPath)
        whenever(mock.reasonDebugString()).thenReturn(debugString)
        whenever(mock.reason()).thenReturn(reason)
        return mock
    }
}
