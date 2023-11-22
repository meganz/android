package mega.privacy.android.feature.sync.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.file.DeleteFileUseCase
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.mapper.StalledIssueToSolvedIssueMapper
import mega.privacy.android.feature.sync.domain.usecase.ResolveStalledIssueUseCase
import mega.privacy.android.feature.sync.domain.usecase.solvedissues.SetSyncSolvedIssueUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)

class ResolveStalledIssueUseCaseTest {

    private val deleteFileUseCase: DeleteFileUseCase = mock()
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val getFileByPathUseCase: GetFileByPathUseCase = mock()
    private val setSyncSolvedIssueUseCase: SetSyncSolvedIssueUseCase = mock()
    private val stalledIssueToSolvedIssueMapper: StalledIssueToSolvedIssueMapper = mock()
    private val renameNodeUseCase: RenameNodeUseCase = mock()
    private val getFingerprintUseCase: GetFingerprintUseCase = mock()

    private val underTest = ResolveStalledIssueUseCase(
        deleteFileUseCase,
        moveNodesToRubbishUseCase,
        getNodeByHandleUseCase,
        getFileByPathUseCase,
        setSyncSolvedIssueUseCase,
        renameNodeUseCase,
        getFingerprintUseCase,
        stalledIssueToSolvedIssueMapper
    )

    @AfterEach
    fun resetAndTearDown() {
        Mockito.reset(
            deleteFileUseCase,
            moveNodesToRubbishUseCase,
            getNodeByHandleUseCase,
            getFileByPathUseCase
        )
    }

    @Test
    fun `test that moveNodesToRubbishUseCase is invoked when resolutionActionType is CHOOSE_LOCAL_FILE`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Choose local file",
                resolutionActionType = StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE
            )
            val stalledIssue = StalledIssue(
                nodeIds = listOf(NodeId(1L)),
                nodeNames = listOf("nodeName"),
                localPaths = listOf("path/to/file"),
                issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
                conflictName = "conflictName"
            )
            val megaNodeId = 1L

            underTest(stalledIssueResolutionAction, stalledIssue)

            verify(moveNodesToRubbishUseCase).invoke(listOf(megaNodeId))
            verifyNoInteractions(deleteFileUseCase)
        }

    @Test
    fun `test that deleteFileUseCase is invoked when resolutionActionType is CHOOSE_REMOTE_FILE`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Choose remote file",
                resolutionActionType = StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE
            )
            val localPath = "path/to/file"
            val stalledIssue = StalledIssue(
                nodeIds = listOf(NodeId(1L)),
                nodeNames = listOf("nodeName"),
                localPaths = listOf(localPath),
                issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
                conflictName = "conflictName"
            )

            underTest(stalledIssueResolutionAction, stalledIssue)

            verify(deleteFileUseCase).invoke(localPath)
            verifyNoInteractions(moveNodesToRubbishUseCase)
        }

    @Test
    fun `test that moveNodesToRubbishUseCase is invoked when resolutionActionType is CHOOSE_LATEST_MODIFIED_TIME and local file is the latest`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Choose file with the latest modified date",
                resolutionActionType = StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME
            )
            val localPath = "path/to/file"
            val nodeId = NodeId(1L)
            val stalledIssue = StalledIssue(
                nodeIds = listOf(nodeId),
                nodeNames = listOf("nodeName"),
                localPaths = listOf(localPath),
                issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
                conflictName = "conflictName"
            )
            val remoteNodeModificationTimeInSeconds = 100L
            val fileModificationTimeInMilliseconds = 105000L
            val remoteNode = mock<FileNode> {
                on { id } doReturn nodeId
                on { modificationTime } doReturn remoteNodeModificationTimeInSeconds
            }
            val localFile = mock<File> {
                on { lastModified() } doReturn fileModificationTimeInMilliseconds
            }
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(remoteNode)
            whenever(getFileByPathUseCase(localPath)).thenReturn(localFile)

            underTest(stalledIssueResolutionAction, stalledIssue)

            verify(moveNodesToRubbishUseCase).invoke(listOf(nodeId.longValue))
            verifyNoInteractions(deleteFileUseCase)
        }

    @Test
    fun `test that deleteFileUseCase is invoked when resolutionActionType is CHOOSE_LATEST_MODIFIED_TIME and remote file is the latest`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Choose file with the latest modified date",
                resolutionActionType = StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME
            )
            val localPath = "path/to/file"
            val nodeId = NodeId(1L)
            val stalledIssue = StalledIssue(
                nodeIds = listOf(nodeId),
                nodeNames = listOf("nodeName"),
                localPaths = listOf(localPath),
                issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
                conflictName = "conflictName"
            )
            val remoteNodeModificationTimeInSeconds = 105L
            val fileModificationTimeInMilliseconds = 100000L
            val remoteNode = mock<FileNode> {
                on { id } doReturn nodeId
                on { modificationTime } doReturn remoteNodeModificationTimeInSeconds
            }
            val localFile = mock<File> {
                on { lastModified() } doReturn fileModificationTimeInMilliseconds
            }
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(remoteNode)
            whenever(getFileByPathUseCase(localPath)).thenReturn(localFile)

            underTest(stalledIssueResolutionAction, stalledIssue)

            verify(deleteFileUseCase).invoke(localPath)
            verifyNoInteractions(moveNodesToRubbishUseCase)
        }

    @Test
    fun `test that rename all action invokes renameNodeUseCase on every file node after first node`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Rename all items",
                resolutionActionType = StalledIssueResolutionActionType.RENAME_ALL_ITEMS
            )
            val stalledIssue = StalledIssue(
                nodeIds = listOf(NodeId(1L), NodeId(2L)),
                nodeNames = listOf("/folder12/aa.txt", "/folder12/AA.txt"),
                localPaths = emptyList(),
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                conflictName = "Names would clash when synced"
            )

            underTest(stalledIssueResolutionAction, stalledIssue)

            verify(renameNodeUseCase).invoke(2L, "AA (1).txt")
        }

    @Test
    fun `test that rename all action invokes renameNodeUseCase on every folder node after first node`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Rename all items",
                resolutionActionType = StalledIssueResolutionActionType.RENAME_ALL_ITEMS
            )
            val stalledIssue = StalledIssue(
                nodeIds = listOf(NodeId(1L), NodeId(2L)),
                nodeNames = listOf("/folder12/aa", "/folder12/AA"),
                localPaths = emptyList(),
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                conflictName = "Names would clash when synced"
            )

            underTest(stalledIssueResolutionAction, stalledIssue)

            verify(renameNodeUseCase).invoke(2L, "AA (1)")
        }

    @Test
    fun `test that after resolving stalled issue it is saved as solved issue`() = runTest {
        val stalledIssueResolutionAction = StalledIssueResolutionAction(
            actionName = "Choose file with the latest modified date",
            resolutionActionType = StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME
        )
        val localPath = "path/to/file"
        val nodeId = NodeId(1L)
        val stalledIssue = StalledIssue(
            nodeIds = listOf(nodeId),
            nodeNames = listOf("nodeName"),
            localPaths = listOf(localPath),
            issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
            conflictName = "conflictName"
        )
        val remoteNodeModificationTimeInSeconds = 105L
        val fileModificationTimeInMilliseconds = 100000L
        val remoteNode = mock<FileNode> {
            on { id } doReturn nodeId
            on { modificationTime } doReturn remoteNodeModificationTimeInSeconds
        }
        val localFile = mock<File> {
            on { lastModified() } doReturn fileModificationTimeInMilliseconds
        }
        val solvedIssue = SolvedIssue(
            nodeIds = listOf(nodeId),
            localPaths = listOf(localPath),
            resolutionExplanation = stalledIssueResolutionAction.actionName
        )
        whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(remoteNode)
        whenever(getFileByPathUseCase(localPath)).thenReturn(localFile)
        whenever(
            stalledIssueToSolvedIssueMapper(
                stalledIssue,
                stalledIssueResolutionAction.actionName
            )
        ).thenReturn(solvedIssue)

        underTest(stalledIssueResolutionAction, stalledIssue)

        verify(setSyncSolvedIssueUseCase).invoke(solvedIssue)
    }
}
