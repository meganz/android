package mega.privacy.android.feature.sync.data.mapper.stalledissue

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import nz.mega.sdk.MegaSyncStall
import nz.mega.sdk.MegaSyncStallList
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StalledIssuesMapperTest {

    private val stalledIssueTypeMapper: StalledIssueTypeMapper = mock()
    private val underTest = StalledIssuesMapper(stalledIssueTypeMapper)

    @Test
    fun `test that stalled issues sdk object is mapped correctly`() {
        val syncId = 323L
        val stalledIssueNodeName = "somefile.txt"
        val syncLocalFolderPath = "/storage/emulated/0/some_sync"
        val stalledIssueDebugString = "Local and remote changed since last synced"
        val syncs =
            listOf(
                FolderPair(
                    syncId,
                    "",
                    syncLocalFolderPath,
                    RemoteFolder(1L, stalledIssueNodeName),
                    SyncStatus.SYNCED
                )
            )
        val stalledIssuesSDKList: MegaSyncStallList = mock()
        val issuesCount = 1L
        val stalledIssueNodeId = 20L
        val stalledIssueSDKObject: MegaSyncStall = mock()
        whenever(stalledIssuesSDKList.size()).thenReturn(issuesCount)
        whenever(stalledIssuesSDKList.get(0)).thenReturn(stalledIssueSDKObject)
        whenever(stalledIssueSDKObject.pathCount(true)).thenReturn(1)
        whenever(stalledIssueSDKObject.cloudNodeHandle(0)).thenReturn(stalledIssueNodeId)
        whenever(stalledIssueSDKObject.path(true, 0)).thenReturn(stalledIssueNodeName)
        whenever(stalledIssueSDKObject.pathCount(false)).thenReturn(1)
        whenever(
            stalledIssueSDKObject.path(
                false,
                0
            )
        ).thenReturn("$syncLocalFolderPath/somefile.txt")
        whenever(stalledIssueSDKObject.reasonDebugString()).thenReturn(stalledIssueDebugString)
        whenever(stalledIssueSDKObject.reason()).thenReturn(MegaSyncStall.SyncStallReason.LocalAndRemoteChangedSinceLastSyncedState_userMustChoose)
        whenever(stalledIssueTypeMapper(MegaSyncStall.SyncStallReason.LocalAndRemoteChangedSinceLastSyncedState_userMustChoose)).thenReturn(
            StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose
        )
        val expected = listOf(
            StalledIssue(
                syncId,
                listOf(NodeId(stalledIssueNodeId)),
                listOf("$syncLocalFolderPath/somefile.txt"),
                StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
                stalledIssueDebugString,
                listOf(stalledIssueNodeName)
            )
        )

        val actual = underTest(syncs, stalledIssuesSDKList)

        Truth.assertThat(actual).isEqualTo(expected)
    }
}