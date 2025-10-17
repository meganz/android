package mega.privacy.android.feature.sync.domain.stalledissue.resolution

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.file.DeleteDocumentFileBySyncContentUriUseCase
import mega.privacy.android.domain.usecase.file.GetLastModifiedTimeForSyncContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByFingerprintAndParentNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import mega.privacy.android.feature.sync.domain.entity.SolvedIssue
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.domain.mapper.StalledIssueToSolvedIssueMapper
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.SetSyncSolvedIssueUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.RenameFilesWithTheSameNameUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.RenameNodeWithTheSameNameUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.ResolveStalledIssueUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalTime::class)
internal class ResolveStalledIssueUseCaseTest {

    private val deleteDocumentFileByContentUriUseCase: DeleteDocumentFileBySyncContentUriUseCase =
        mock()
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val getLastModifiedTimeForSyncContentUriUseCase: GetLastModifiedTimeForSyncContentUriUseCase =
        mock()
    private val setSyncSolvedIssueUseCase: SetSyncSolvedIssueUseCase = mock()
    private val stalledIssueToSolvedIssueMapper: StalledIssueToSolvedIssueMapper = mock()
    private val renameNodeUseCase: RenameNodeUseCase = mock()
    private val getNodeByFingerprintAndParentNodeUseCase: GetNodeByFingerprintAndParentNodeUseCase =
        mock()
    private val moveNodeUseCase: MoveNodeUseCase = mock()
    private val renameNodeWithTheSameNameUseCase: RenameNodeWithTheSameNameUseCase = mock()
    private val renameFilesWithTheSameNameUseCase: RenameFilesWithTheSameNameUseCase = mock()
    private val syncId = 12323332L

    private val underTest = ResolveStalledIssueUseCase(
        deleteDocumentFileByContentUriUseCase,
        getLastModifiedTimeForSyncContentUriUseCase,
        moveNodesToRubbishUseCase,
        getNodeByHandleUseCase,
        setSyncSolvedIssueUseCase,
        renameNodeUseCase,
        moveNodeUseCase,
        renameNodeWithTheSameNameUseCase,
        renameFilesWithTheSameNameUseCase,
        stalledIssueToSolvedIssueMapper,
    )

    @AfterEach
    fun resetAndTearDown() {
        Mockito.reset(
            deleteDocumentFileByContentUriUseCase,
            moveNodesToRubbishUseCase,
            getNodeByHandleUseCase,
            getLastModifiedTimeForSyncContentUriUseCase,
            setSyncSolvedIssueUseCase,
            renameNodeUseCase,
            getNodeByFingerprintAndParentNodeUseCase,
            moveNodeUseCase,
            stalledIssueToSolvedIssueMapper,
            renameFilesWithTheSameNameUseCase,
            renameNodeWithTheSameNameUseCase
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
                syncId = syncId,
                nodeIds = listOf(NodeId(1L)),
                nodeNames = listOf("nodeName"),
                localPaths = listOf("path/to/file"),
                issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
                conflictName = "conflictName",
                id = "1_1_0"
            )
            val megaNodeId = 1L

            underTest(stalledIssueResolutionAction, stalledIssue)

            verify(moveNodesToRubbishUseCase).invoke(listOf(megaNodeId))
            verifyNoInteractions(deleteDocumentFileByContentUriUseCase)
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
                syncId = syncId,
                nodeIds = listOf(NodeId(1L)),
                nodeNames = listOf("nodeName"),
                localPaths = listOf(localPath),
                issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
                conflictName = "conflictName",
                id = "1_1_0"
            )

            underTest(stalledIssueResolutionAction, stalledIssue)

            verify(deleteDocumentFileByContentUriUseCase).invoke(UriPath(localPath))
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
                syncId = syncId,
                nodeIds = listOf(nodeId),
                nodeNames = listOf("nodeName"),
                localPaths = listOf(localPath),
                issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
                conflictName = "conflictName",
                id = "1_1_0"
            )
            val remoteNodeModificationTimeInSeconds = 100L
            val fileModificationTimeInMilliseconds = 105000L
            val remoteNode = mock<FileNode> {
                on { id } doReturn nodeId
                on { modificationTime } doReturn remoteNodeModificationTimeInSeconds
            }
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(remoteNode)
            whenever(getLastModifiedTimeForSyncContentUriUseCase(UriPath(localPath))).thenReturn(
                Instant.fromEpochMilliseconds(
                    fileModificationTimeInMilliseconds
                )
            )

            underTest(stalledIssueResolutionAction, stalledIssue)

            verify(moveNodesToRubbishUseCase).invoke(listOf(nodeId.longValue))
            verifyNoInteractions(deleteDocumentFileByContentUriUseCase)
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
                syncId = syncId,
                nodeIds = listOf(nodeId),
                nodeNames = listOf("nodeName"),
                localPaths = listOf(localPath),
                issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
                conflictName = "conflictName",
                id = "1_1_0"
            )
            val remoteNodeModificationTimeInSeconds = 105L
            val fileModificationTimeInMilliseconds = 100000L
            val remoteNode = mock<FileNode> {
                on { id } doReturn nodeId
                on { modificationTime } doReturn remoteNodeModificationTimeInSeconds
            }
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(remoteNode)
            whenever(getLastModifiedTimeForSyncContentUriUseCase(UriPath(localPath))).thenReturn(
                Instant.fromEpochMilliseconds(
                    fileModificationTimeInMilliseconds
                )
            )

            underTest(stalledIssueResolutionAction, stalledIssue)

            verify(deleteDocumentFileByContentUriUseCase).invoke(UriPath(localPath))
            verifyNoInteractions(moveNodesToRubbishUseCase)
        }

    @Test
    fun `test that rename all action invokes renameNodeWithTheSameNameUseCase with all file nodes if nodes list size is more than 1`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Rename all items",
                resolutionActionType = StalledIssueResolutionActionType.RENAME_ALL_ITEMS
            )
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = listOf(NodeId(1L), NodeId(2L)),
                nodeNames = listOf("/folder12/aa.txt", "/folder12/AA.txt"),
                localPaths = emptyList(),
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                conflictName = "Names would clash when synced",
                id = "1_1_0"
            )

            underTest(stalledIssueResolutionAction, stalledIssue)

            verify(renameNodeWithTheSameNameUseCase).invoke(stalledIssue.nodeIds)
        }

    @Test
    fun `test that rename all action invokes renameNodeWithTheSameNameUseCase with all folder nodes if nodes list size is more than 1`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Rename all items",
                resolutionActionType = StalledIssueResolutionActionType.RENAME_ALL_ITEMS
            )
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = listOf(NodeId(1L), NodeId(2L)),
                nodeNames = listOf("/folder12/aa", "/folder12/AA"),
                localPaths = emptyList(),
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                conflictName = "Names would clash when synced",
                id = "1_1_0"
            )
            val solvedIssue = SolvedIssue(
                syncId = syncId,
                nodeIds = listOf(NodeId(1L), NodeId(2L)),
                localPaths = emptyList(),
                resolutionExplanation = stalledIssueResolutionAction.actionName
            )
            whenever(
                stalledIssueToSolvedIssueMapper(
                    stalledIssue,
                    stalledIssueResolutionAction.resolutionActionType
                )
            ).thenReturn(solvedIssue)

            underTest(stalledIssueResolutionAction, stalledIssue)

            verify(renameNodeWithTheSameNameUseCase).invoke(stalledIssue.nodeIds)
        }

    @Test
    fun `test that rename all action invokes renameFilesWithTheSameNameUseCase with all local paths if local paths list size is more than 1`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Rename all items",
                resolutionActionType = StalledIssueResolutionActionType.RENAME_ALL_ITEMS
            )
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = emptyList(),
                nodeNames = emptyList(),
                localPaths = listOf("/folder12/aa", "/folder12/AA"),
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                conflictName = "Names would clash when synced",
                id = "1_1_0"
            )

            underTest(stalledIssueResolutionAction, stalledIssue)

            verify(renameFilesWithTheSameNameUseCase).invoke(stalledIssue.localPaths.map {
                UriPath(it)
            })
        }


    @Test
    fun `test that merge folders action results into folders merged to the biggest folder`() =
        runTest {
            val mainFolderId = NodeId(999L)
            val secondaryFolderId = NodeId(888L)
            val mainFolder: FolderNode = mock {
                on { it.childFileCount } doReturn 5
                on { id } doReturn mainFolderId
            }
            val secondaryFolder: FolderNode = mock {
                on { it.childFileCount } doReturn 3
                on { id } doReturn secondaryFolderId
            }
            val childOne: FolderNode = mock {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "childOne"
            }
            val childTwo: FileNode = mock {
                on { id } doReturn NodeId(2L)
                on { name } doReturn "childTwo.png"
                on { fingerprint } doReturn "123"
            }
            val childThree: FolderNode = mock {
                on { id } doReturn NodeId(3L)
                on { name } doReturn "childThree"
            }
            val childThreeWithDifferentFiles: FolderNode = mock {
                on { id } doReturn NodeId(4L)
                on { name } doReturn "childThree"
            }
            val subChildFour: FileNode = mock {
                on { id } doReturn NodeId(4L)
                on { name } doReturn "subChildFour.txt"
                on { fingerprint } doReturn "456"
            }
            val subChildFive: FileNode = mock {
                on { id } doReturn NodeId(5L)
                on { name } doReturn "subChildFive.jpeg"
                on { fingerprint } doReturn "789"
            }
            val childSix: FileNode = mock {
                on { id } doReturn NodeId(6L)
                on { name } doReturn "childSix.jpeg"
                on { fingerprint } doReturn "101112"
            }
            val childSeven: FileNode = mock {
                on { id } doReturn NodeId(7L)
                on { name } doReturn "childSeven.jpeg"
                on { fingerprint } doReturn "131415"
            }
            val childEight: FileNode = mock {
                on { id } doReturn NodeId(8L)
                on { name } doReturn "subChildFive.jpeg"
                on { fingerprint } doReturn "161718"
            }
            val mainFolderChildren = listOf(
                childOne,
                childTwo,
                childThree,
                childSeven
            )
            val secondaryFolderChildren = listOf(
                childOne,
                childSix,
                childThreeWithDifferentFiles,
            )
            val childOneChildren = listOf<UnTypedNode>()
            val childThreeChildren = listOf<UnTypedNode>(
                subChildFour,
                subChildFive,
            )
            val childThreeDifferentChildren = listOf<UnTypedNode>(
                childEight
            )
            whenever(mainFolder.fetchChildren).thenReturn { mainFolderChildren }
            whenever(secondaryFolder.fetchChildren).thenReturn { secondaryFolderChildren }
            whenever(childOne.fetchChildren).thenReturn { childOneChildren }
            whenever(childThree.fetchChildren).thenReturn { childThreeChildren }
            whenever(childThreeWithDifferentFiles.fetchChildren).thenReturn { childThreeDifferentChildren }
            whenever(getNodeByHandleUseCase(mainFolderId.longValue)).thenReturn(mainFolder)
            whenever(getNodeByHandleUseCase(secondaryFolderId.longValue)).thenReturn(secondaryFolder)
            val resolutionAction = StalledIssueResolutionAction(
                actionName = "Merge folders",
                resolutionActionType = StalledIssueResolutionActionType.MERGE_FOLDERS
            )
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = listOf(mainFolderId, secondaryFolderId),
                nodeNames = listOf("/folder12/aa", "/folder12/AA"),
                localPaths = emptyList(),
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                conflictName = "Names would clash when synced",
                id = "1_1_0"
            )

            underTest(
                resolutionAction, stalledIssue
            )

            verify(moveNodeUseCase).invoke(childSix.id, mainFolderId)
            verify(renameNodeUseCase).invoke(childEight.id.longValue, "subChildFive (1).jpeg")
            verify(moveNodeUseCase).invoke(childEight.id, childThree.id)
            verify(moveNodesToRubbishUseCase).invoke(listOf(secondaryFolderId.longValue))
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
            syncId = syncId,
            nodeIds = listOf(nodeId),
            nodeNames = listOf("nodeName"),
            localPaths = listOf(localPath),
            issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
            conflictName = "conflictName",
            id = "1_1_0"
        )
        val remoteNodeModificationTimeInSeconds = 105L
        val fileModificationTimeInMilliseconds = 100000L
        val remoteNode = mock<FileNode> {
            on { id } doReturn nodeId
            on { modificationTime } doReturn remoteNodeModificationTimeInSeconds
        }
        val solvedIssue = SolvedIssue(
            syncId = syncId,
            nodeIds = listOf(nodeId),
            localPaths = listOf(localPath),
            resolutionExplanation = stalledIssueResolutionAction.actionName
        )
        whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(remoteNode)
        whenever(getLastModifiedTimeForSyncContentUriUseCase(UriPath(localPath))).thenReturn(
            Instant.fromEpochMilliseconds(
                fileModificationTimeInMilliseconds
            )
        )
        whenever(
            stalledIssueToSolvedIssueMapper(
                stalledIssue,
                stalledIssueResolutionAction.resolutionActionType
            )
        ).thenReturn(solvedIssue)

        underTest(stalledIssueResolutionAction, stalledIssue)

        verify(setSyncSolvedIssueUseCase).invoke(solvedIssue)
    }

    @Test
    fun `test that rename all action with single node does not invoke renameNodeWithTheSameNameUseCase`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Rename all items",
                resolutionActionType = StalledIssueResolutionActionType.RENAME_ALL_ITEMS
            )
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = listOf(NodeId(1L)),
                nodeNames = listOf("/folder12/aa.txt"),
                localPaths = emptyList(),
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                conflictName = "Names would clash when synced",
                id = "1_1_0"
            )
            val solvedIssue = SolvedIssue(
                syncId = syncId,
                nodeIds = listOf(NodeId(1L)),
                localPaths = emptyList(),
                resolutionExplanation = stalledIssueResolutionAction.actionName
            )
            whenever(
                stalledIssueToSolvedIssueMapper(
                    stalledIssue,
                    stalledIssueResolutionAction.resolutionActionType
                )
            ).thenReturn(solvedIssue)

            underTest(stalledIssueResolutionAction, stalledIssue)

            verifyNoInteractions(renameNodeWithTheSameNameUseCase)
        }

    @Test
    fun `test that rename all action with single local path does not invoke renameFilesWithTheSameNameUseCase`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Rename all items",
                resolutionActionType = StalledIssueResolutionActionType.RENAME_ALL_ITEMS
            )
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = emptyList(),
                nodeNames = emptyList(),
                localPaths = listOf("/folder12/aa"),
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                conflictName = "Names would clash when synced",
                id = "1_1_0"
            )
            val solvedIssue = SolvedIssue(
                syncId = syncId,
                nodeIds = emptyList(),
                localPaths = listOf("/folder12/aa"),
                resolutionExplanation = stalledIssueResolutionAction.actionName
            )
            whenever(
                stalledIssueToSolvedIssueMapper(
                    stalledIssue,
                    stalledIssueResolutionAction.resolutionActionType
                )
            ).thenReturn(solvedIssue)

            underTest(stalledIssueResolutionAction, stalledIssue)

            verifyNoInteractions(renameFilesWithTheSameNameUseCase)
        }

    @Test
    fun `test that rename all action with empty nodeIds and localPaths does not invoke any rename use cases`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Rename all items",
                resolutionActionType = StalledIssueResolutionActionType.RENAME_ALL_ITEMS
            )
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = emptyList(),
                nodeNames = emptyList(),
                localPaths = emptyList(),
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                conflictName = "Names would clash when synced",
                id = "1_1_0"
            )
            val solvedIssue = SolvedIssue(
                syncId = syncId,
                nodeIds = emptyList(),
                localPaths = emptyList(),
                resolutionExplanation = stalledIssueResolutionAction.actionName
            )
            whenever(
                stalledIssueToSolvedIssueMapper(
                    stalledIssue,
                    stalledIssueResolutionAction.resolutionActionType
                )
            ).thenReturn(solvedIssue)

            underTest(stalledIssueResolutionAction, stalledIssue)

            verifyNoInteractions(renameNodeWithTheSameNameUseCase)
            verifyNoInteractions(renameFilesWithTheSameNameUseCase)
        }

    @Test
    fun `test that REMOVE_DUPLICATES action does not perform any operations`() = runTest {
        val stalledIssueResolutionAction = StalledIssueResolutionAction(
            actionName = "Remove duplicates",
            resolutionActionType = StalledIssueResolutionActionType.REMOVE_DUPLICATES
        )
        val stalledIssue = StalledIssue(
            syncId = syncId,
            nodeIds = listOf(NodeId(1L), NodeId(2L)),
            nodeNames = listOf("file1.txt", "file2.txt"),
            localPaths = emptyList(),
            issueType = StallIssueType.NamesWouldClashWhenSynced,
            conflictName = "Names would clash when synced",
            id = "1_1_0"
        )
        val solvedIssue = SolvedIssue(
            syncId = syncId,
            nodeIds = listOf(NodeId(1L), NodeId(2L)),
            localPaths = emptyList(),
            resolutionExplanation = stalledIssueResolutionAction.actionName
        )
        whenever(
            stalledIssueToSolvedIssueMapper(
                stalledIssue,
                stalledIssueResolutionAction.resolutionActionType
            )
        ).thenReturn(solvedIssue)

        underTest(stalledIssueResolutionAction, stalledIssue)

        verifyNoInteractions(moveNodesToRubbishUseCase)
        verifyNoInteractions(deleteDocumentFileByContentUriUseCase)
        verifyNoInteractions(renameNodeWithTheSameNameUseCase)
        verifyNoInteractions(renameFilesWithTheSameNameUseCase)
    }

    @Test
    fun `test that REMOVE_DUPLICATES_AND_REMOVE_THE_REST action does not perform any operations`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Remove duplicates and remove the rest",
                resolutionActionType = StalledIssueResolutionActionType.REMOVE_DUPLICATES_AND_REMOVE_THE_REST
            )
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = listOf(NodeId(1L), NodeId(2L)),
                nodeNames = listOf("file1.txt", "file2.txt"),
                localPaths = emptyList(),
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                conflictName = "Names would clash when synced",
                id = "1_1_0"
            )
            val solvedIssue = SolvedIssue(
                syncId = syncId,
                nodeIds = listOf(NodeId(1L), NodeId(2L)),
                localPaths = emptyList(),
                resolutionExplanation = stalledIssueResolutionAction.actionName
            )
            whenever(
                stalledIssueToSolvedIssueMapper(
                    stalledIssue,
                    stalledIssueResolutionAction.resolutionActionType
                )
            ).thenReturn(solvedIssue)

            underTest(stalledIssueResolutionAction, stalledIssue)

            verifyNoInteractions(moveNodesToRubbishUseCase)
            verifyNoInteractions(deleteDocumentFileByContentUriUseCase)
            verifyNoInteractions(renameNodeWithTheSameNameUseCase)
            verifyNoInteractions(renameFilesWithTheSameNameUseCase)
        }

    @Test
    fun `test that CHOOSE_LATEST_MODIFIED_TIME with null lastModifiedInstant does not perform any operations`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Choose file with the latest modified date",
                resolutionActionType = StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME
            )
            val localPath = "path/to/file"
            val nodeId = NodeId(1L)
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = listOf(nodeId),
                nodeNames = listOf("nodeName"),
                localPaths = listOf(localPath),
                issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
                conflictName = "conflictName",
                id = "1_1_0"
            )
            val remoteNode = mock<FileNode> {
                on { id } doReturn nodeId
                on { modificationTime } doReturn 100L
            }
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(remoteNode)
            whenever(getLastModifiedTimeForSyncContentUriUseCase(UriPath(localPath))).thenReturn(
                null
            )

            underTest(stalledIssueResolutionAction, stalledIssue)

            verifyNoInteractions(moveNodesToRubbishUseCase)
            verifyNoInteractions(deleteDocumentFileByContentUriUseCase)
        }

    @Test
    fun `test that CHOOSE_LATEST_MODIFIED_TIME with non-FileNode does not perform any operations`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Choose file with the latest modified date",
                resolutionActionType = StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME
            )
            val localPath = "path/to/file"
            val nodeId = NodeId(1L)
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = listOf(nodeId),
                nodeNames = listOf("nodeName"),
                localPaths = listOf(localPath),
                issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
                conflictName = "conflictName",
                id = "1_1_0"
            )
            val remoteNode = mock<FolderNode> {
                on { id } doReturn nodeId
            }
            whenever(getNodeByHandleUseCase(nodeId.longValue)).thenReturn(remoteNode)
            whenever(getLastModifiedTimeForSyncContentUriUseCase(UriPath(localPath))).thenReturn(
                Instant.fromEpochMilliseconds(100000L)
            )

            underTest(stalledIssueResolutionAction, stalledIssue)

            verifyNoInteractions(moveNodesToRubbishUseCase)
            verifyNoInteractions(deleteDocumentFileByContentUriUseCase)
        }

    @Test
    fun `test that merge folders action handles folders with same child count correctly`() =
        runTest {
            val mainFolderId = NodeId(999L)
            val secondaryFolderId = NodeId(888L)
            val mainFolder: FolderNode = mock {
                on { it.childFileCount } doReturn 5
                on { id } doReturn mainFolderId
            }
            val secondaryFolder: FolderNode = mock {
                on { it.childFileCount } doReturn 5 // Same count
                on { id } doReturn secondaryFolderId
            }
            val childOne: FolderNode = mock {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "childOne"
            }
            val mainFolderChildren = listOf(childOne)
            val secondaryFolderChildren = listOf<UnTypedNode>()
            whenever(mainFolder.fetchChildren).thenReturn { mainFolderChildren }
            whenever(secondaryFolder.fetchChildren).thenReturn { secondaryFolderChildren }
            whenever(getNodeByHandleUseCase(mainFolderId.longValue)).thenReturn(mainFolder)
            whenever(getNodeByHandleUseCase(secondaryFolderId.longValue)).thenReturn(secondaryFolder)
            val resolutionAction = StalledIssueResolutionAction(
                actionName = "Merge folders",
                resolutionActionType = StalledIssueResolutionActionType.MERGE_FOLDERS
            )
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = listOf(mainFolderId, secondaryFolderId),
                nodeNames = listOf("/folder12/aa", "/folder12/AA"),
                localPaths = emptyList(),
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                conflictName = "Names would clash when synced",
                id = "1_1_0"
            )

            underTest(resolutionAction, stalledIssue)

            verify(moveNodesToRubbishUseCase).invoke(listOf(secondaryFolderId.longValue))
        }

    @Test
    fun `test that error in resolveIssue does not prevent saving solved issue`() = runTest {
        val stalledIssueResolutionAction = StalledIssueResolutionAction(
            actionName = "Choose local file",
            resolutionActionType = StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE
        )
        val stalledIssue = StalledIssue(
            syncId = syncId,
            nodeIds = listOf(NodeId(1L)),
            nodeNames = listOf("nodeName"),
            localPaths = listOf("path/to/file"),
            issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
            conflictName = "conflictName",
            id = "1_1_0"
        )
        val solvedIssue = SolvedIssue(
            syncId = syncId,
            nodeIds = listOf(NodeId(1L)),
            localPaths = listOf("path/to/file"),
            resolutionExplanation = stalledIssueResolutionAction.actionName
        )

        // Make moveNodesToRubbishUseCase throw an exception
        whenever(moveNodesToRubbishUseCase.invoke(any())).thenThrow(RuntimeException("Test error"))
        whenever(
            stalledIssueToSolvedIssueMapper(
                stalledIssue,
                stalledIssueResolutionAction.resolutionActionType
            )
        ).thenReturn(solvedIssue)

        underTest(stalledIssueResolutionAction, stalledIssue)

        // Even though resolveIssue failed, solved issue should still be saved
        verify(setSyncSolvedIssueUseCase).invoke(solvedIssue)
    }

    @Test
    fun `test that error in saveSolvedIssue does not prevent resolveIssue from executing`() =
        runTest {
            val stalledIssueResolutionAction = StalledIssueResolutionAction(
                actionName = "Choose local file",
                resolutionActionType = StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE
            )
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = listOf(NodeId(1L)),
                nodeNames = listOf("nodeName"),
                localPaths = listOf("path/to/file"),
                issueType = StallIssueType.LocalAndRemoteChangedSinceLastSyncedStateUserMustChoose,
                conflictName = "conflictName",
                id = "1_1_0"
            )

            // Make stalledIssueToSolvedIssueMapper throw an exception
            whenever(
                stalledIssueToSolvedIssueMapper(
                    stalledIssue,
                    stalledIssueResolutionAction.resolutionActionType
                )
            ).thenThrow(RuntimeException("Mapper error"))

            underTest(stalledIssueResolutionAction, stalledIssue)

            // resolveIssue should still execute successfully
            verify(moveNodesToRubbishUseCase).invoke(listOf(NodeId(1L).longValue))
        }

    @Test
    fun `test that merge folders handles null nodes gracefully`() = runTest {
        val mainFolderId = NodeId(999L)
        val secondaryFolderId = NodeId(888L)
        val mainFolder: FolderNode = mock {
            on { it.childFileCount } doReturn 5
            on { id } doReturn mainFolderId
        }
        val resolutionAction = StalledIssueResolutionAction(
            actionName = "Merge folders",
            resolutionActionType = StalledIssueResolutionActionType.MERGE_FOLDERS
        )
        val stalledIssue = StalledIssue(
            syncId = syncId,
            nodeIds = listOf(mainFolderId, secondaryFolderId),
            nodeNames = listOf("/folder12/aa", "/folder12/AA"),
            localPaths = emptyList(),
            issueType = StallIssueType.NamesWouldClashWhenSynced,
            conflictName = "Names would clash when synced",
            id = "1_1_0"
        )

        // Return null for one of the nodes
        whenever(getNodeByHandleUseCase(mainFolderId.longValue)).thenReturn(mainFolder)
        whenever(getNodeByHandleUseCase(secondaryFolderId.longValue)).thenReturn(null)

        underTest(resolutionAction, stalledIssue)

        // Should not crash and should not invoke moveNodesToRubbishUseCase
        verifyNoInteractions(moveNodesToRubbishUseCase)
    }

    @Test
    fun `test that merge folders handles non-FolderNode gracefully`() = runTest {
        val mainFolderId = NodeId(999L)
        val secondaryFolderId = NodeId(888L)
        val mainFolder: FolderNode = mock {
            on { it.childFileCount } doReturn 5
            on { id } doReturn mainFolderId
        }
        val fileNode = mock<FileNode> {
            on { id } doReturn secondaryFolderId
        }
        val resolutionAction = StalledIssueResolutionAction(
            actionName = "Merge folders",
            resolutionActionType = StalledIssueResolutionActionType.MERGE_FOLDERS
        )
        val stalledIssue = StalledIssue(
            syncId = syncId,
            nodeIds = listOf(mainFolderId, secondaryFolderId),
            nodeNames = listOf("/folder12/aa", "/folder12/AA"),
            localPaths = emptyList(),
            issueType = StallIssueType.NamesWouldClashWhenSynced,
            conflictName = "Names would clash when synced",
            id = "1_1_0"
        )

        whenever(getNodeByHandleUseCase(mainFolderId.longValue)).thenReturn(mainFolder)
        whenever(getNodeByHandleUseCase(secondaryFolderId.longValue)).thenReturn(fileNode)

        underTest(resolutionAction, stalledIssue)

        // Should not crash and should not invoke moveNodesToRubbishUseCase
        verifyNoInteractions(moveNodesToRubbishUseCase)
    }

    @Test
    fun `test that merge folders handles case-insensitive file name conflicts correctly`() =
        runTest {
            val mainFolderId = NodeId(999L)
            val secondaryFolderId = NodeId(888L)
            val mainFolder: FolderNode = mock {
                on { it.childFileCount } doReturn 1
                on { id } doReturn mainFolderId
            }
            val secondaryFolder: FolderNode = mock {
                on { it.childFileCount } doReturn 1
                on { id } doReturn secondaryFolderId
            }

            // Create files with same name but different case
            val mainFileNode: FileNode = mock {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "j1.txt"
                on { fingerprint } doReturn "fingerprint1"
            }
            val secondaryFileNode: FileNode = mock {
                on { id } doReturn NodeId(2L)
                on { name } doReturn "J1.txt"
                on { fingerprint } doReturn "fingerprint2"
            }

            val mainFolderChildren = listOf(mainFileNode)
            val secondaryFolderChildren = listOf(secondaryFileNode)

            whenever(mainFolder.fetchChildren).thenReturn { mainFolderChildren }
            whenever(secondaryFolder.fetchChildren).thenReturn { secondaryFolderChildren }
            whenever(getNodeByHandleUseCase(mainFolderId.longValue)).thenReturn(mainFolder)
            whenever(getNodeByHandleUseCase(secondaryFolderId.longValue)).thenReturn(secondaryFolder)

            val resolutionAction = StalledIssueResolutionAction(
                actionName = "Merge folders",
                resolutionActionType = StalledIssueResolutionActionType.MERGE_FOLDERS
            )
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = listOf(mainFolderId, secondaryFolderId),
                nodeNames = listOf("/folder12/i1", "/folder12/I1"),
                localPaths = emptyList(),
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                conflictName = "Names would clash when synced",
                id = "1_1_0"
            )

            underTest(resolutionAction, stalledIssue)

            // Verify that the case-insensitive name conflict was detected and resolved
            verify(renameNodeUseCase).invoke(secondaryFileNode.id.longValue, "J1 (1).txt")
            verify(moveNodeUseCase).invoke(secondaryFileNode.id, mainFolderId)
            verify(moveNodesToRubbishUseCase).invoke(listOf(secondaryFolderId.longValue))
        }

    @Test
    fun `test that merge folders handles same case same fingerprint files without renaming`() =
        runTest {
            val mainFolderId = NodeId(999L)
            val secondaryFolderId = NodeId(888L)
            val mainFolder: FolderNode = mock {
                on { it.childFileCount } doReturn 1
                on { id } doReturn mainFolderId
            }
            val secondaryFolder: FolderNode = mock {
                on { it.childFileCount } doReturn 1
                on { id } doReturn secondaryFolderId
            }

            // Create files with same name and same fingerprint (duplicates)
            val mainFileNode: FileNode = mock {
                on { id } doReturn NodeId(1L)
                on { name } doReturn "duplicate.txt"
                on { fingerprint } doReturn "same_fingerprint"
            }
            val secondaryFileNode: FileNode = mock {
                on { id } doReturn NodeId(2L)
                on { name } doReturn "duplicate.txt"
                on { fingerprint } doReturn "same_fingerprint"
            }

            val mainFolderChildren = listOf(mainFileNode)
            val secondaryFolderChildren = listOf(secondaryFileNode)

            whenever(mainFolder.fetchChildren).thenReturn { mainFolderChildren }
            whenever(secondaryFolder.fetchChildren).thenReturn { secondaryFolderChildren }
            whenever(getNodeByHandleUseCase(mainFolderId.longValue)).thenReturn(mainFolder)
            whenever(getNodeByHandleUseCase(secondaryFolderId.longValue)).thenReturn(secondaryFolder)

            val resolutionAction = StalledIssueResolutionAction(
                actionName = "Merge folders",
                resolutionActionType = StalledIssueResolutionActionType.MERGE_FOLDERS
            )
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = listOf(mainFolderId, secondaryFolderId),
                nodeNames = listOf("/folder12/folder1", "/folder12/folder2"),
                localPaths = emptyList(),
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                conflictName = "Names would clash when synced",
                id = "1_1_0"
            )

            underTest(resolutionAction, stalledIssue)

            // Verify that duplicate files (same name, same fingerprint) are not renamed
            verify(renameNodeUseCase, never()).invoke(any(), any())
            verify(moveNodeUseCase).invoke(secondaryFileNode.id, mainFolderId)
            verify(moveNodesToRubbishUseCase).invoke(listOf(secondaryFolderId.longValue))
        }

    @Test
    fun `test that merge folders allocates unique counters for multiple conflicting files`() =
        runTest {
            val mainFolderId = NodeId(999L)
            val secondaryFolderId = NodeId(888L)
            val mainFolder: FolderNode = mock {
                on { it.childFileCount } doReturn 3
                on { id } doReturn mainFolderId
            }
            val secondaryFolder: FolderNode = mock {
                on { it.childFileCount } doReturn 2
                on { id } doReturn secondaryFolderId
            }

            // Create existing files in main folder with counters already used
            val existingFile1: FileNode = mock {
                on { id } doReturn NodeId(10L)
                on { name } doReturn "file.txt"
                on { fingerprint } doReturn "existing1"
            }
            val existingFile2: FileNode = mock {
                on { id } doReturn NodeId(11L)
                on { name } doReturn "file (1).txt"
                on { fingerprint } doReturn "existing2"
            }
            val existingFile3: FileNode = mock {
                on { id } doReturn NodeId(12L)
                on { name } doReturn "file (3).txt"
                on { fingerprint } doReturn "existing3"
            }

            // Create new conflicting files to be merged
            val newFile1: FileNode = mock {
                on { id } doReturn NodeId(2L)
                on { name } doReturn "FILE.txt"  // Different case
                on { fingerprint } doReturn "new1"
            }
            val newFile2: FileNode = mock {
                on { id } doReturn NodeId(3L)
                on { name } doReturn "file.TXT"  // Different case and extension
                on { fingerprint } doReturn "new2"
            }

            val mainFolderChildren = listOf(existingFile1, existingFile2, existingFile3)
            val secondaryFolderChildren = listOf(newFile1, newFile2)

            whenever(mainFolder.fetchChildren).thenReturn { mainFolderChildren }
            whenever(secondaryFolder.fetchChildren).thenReturn { secondaryFolderChildren }
            whenever(getNodeByHandleUseCase(mainFolderId.longValue)).thenReturn(mainFolder)
            whenever(getNodeByHandleUseCase(secondaryFolderId.longValue)).thenReturn(secondaryFolder)

            val resolutionAction = StalledIssueResolutionAction(
                actionName = "Merge folders",
                resolutionActionType = StalledIssueResolutionActionType.MERGE_FOLDERS
            )
            val stalledIssue = StalledIssue(
                syncId = syncId,
                nodeIds = listOf(mainFolderId, secondaryFolderId),
                nodeNames = listOf("/folder12/main", "/folder12/secondary"),
                localPaths = emptyList(),
                issueType = StallIssueType.NamesWouldClashWhenSynced,
                conflictName = "Names would clash when synced",
                id = "1_1_0"
            )

            underTest(resolutionAction, stalledIssue)

            // Verify that unique counters are allocated (2 and 4 since 1 and 3 are taken)
            verify(renameNodeUseCase).invoke(newFile1.id.longValue, "FILE (2).txt")
            verify(renameNodeUseCase).invoke(newFile2.id.longValue, "file (2).TXT")
            verify(moveNodeUseCase).invoke(newFile1.id, mainFolderId)
            verify(moveNodeUseCase).invoke(newFile2.id, mainFolderId)
            verify(moveNodesToRubbishUseCase).invoke(listOf(secondaryFolderId.longValue))
        }

    @Test
    fun `test that merge folders handles files without extensions correctly`() = runTest {
        val mainFolderId = NodeId(999L)
        val secondaryFolderId = NodeId(888L)
        val mainFolder: FolderNode = mock {
            on { it.childFileCount } doReturn 1
            on { id } doReturn mainFolderId
        }
        val secondaryFolder: FolderNode = mock {
            on { it.childFileCount } doReturn 1
            on { id } doReturn secondaryFolderId
        }

        // Create files without extensions
        val mainFileNode: FileNode = mock {
            on { id } doReturn NodeId(1L)
            on { name } doReturn "readme"
            on { fingerprint } doReturn "fingerprint1"
        }
        val secondaryFileNode: FileNode = mock {
            on { id } doReturn NodeId(2L)
            on { name } doReturn "README"  // Different case, no extension
            on { fingerprint } doReturn "fingerprint2"
        }

        val mainFolderChildren = listOf(mainFileNode)
        val secondaryFolderChildren = listOf(secondaryFileNode)

        whenever(mainFolder.fetchChildren).thenReturn { mainFolderChildren }
        whenever(secondaryFolder.fetchChildren).thenReturn { secondaryFolderChildren }
        whenever(getNodeByHandleUseCase(mainFolderId.longValue)).thenReturn(mainFolder)
        whenever(getNodeByHandleUseCase(secondaryFolderId.longValue)).thenReturn(secondaryFolder)

        val resolutionAction = StalledIssueResolutionAction(
            actionName = "Merge folders",
            resolutionActionType = StalledIssueResolutionActionType.MERGE_FOLDERS
        )
        val stalledIssue = StalledIssue(
            syncId = syncId,
            nodeIds = listOf(mainFolderId, secondaryFolderId),
            nodeNames = listOf("/folder12/i1", "/folder12/I1"),
            localPaths = emptyList(),
            issueType = StallIssueType.NamesWouldClashWhenSynced,
            conflictName = "Names would clash when synced",
            id = "1_1_0"
        )

        underTest(resolutionAction, stalledIssue)

        // Verify that files without extensions get renamed correctly
        verify(renameNodeUseCase).invoke(secondaryFileNode.id.longValue, "README (1)")
        verify(moveNodeUseCase).invoke(secondaryFileNode.id, mainFolderId)
        verify(moveNodesToRubbishUseCase).invoke(listOf(secondaryFolderId.longValue))
    }
}
