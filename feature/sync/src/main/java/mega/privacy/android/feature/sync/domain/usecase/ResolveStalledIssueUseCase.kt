package mega.privacy.android.feature.sync.domain.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.usecase.file.DeleteFileUseCase
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.domain.mapper.StalledIssueToSolvedIssueMapper
import mega.privacy.android.feature.sync.domain.usecase.solvedissues.SetSyncSolvedIssueUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.RenameFilesWithTheSameNameUseCase
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.RenameNodeWithTheSameNameUseCase
import javax.inject.Inject

internal class ResolveStalledIssueUseCase @Inject constructor(
    private val deleteFileUseCase: DeleteFileUseCase,
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val getFileByPathUseCase: GetFileByPathUseCase,
    private val setSyncSolvedIssueUseCase: SetSyncSolvedIssueUseCase,
    private val renameNodeUseCase: RenameNodeUseCase,
    private val moveNodeUseCase: MoveNodeUseCase,
    private val renameNodeWithTheSameNameUseCase: RenameNodeWithTheSameNameUseCase,
    private val renameFilesWithTheSameNameUseCase: RenameFilesWithTheSameNameUseCase,
    private val stalledIssueToSolvedIssueMapper: StalledIssueToSolvedIssueMapper,
) {

    suspend operator fun invoke(
        stalledIssueResolutionAction: StalledIssueResolutionAction,
        stalledIssue: StalledIssue,
    ) {
        runCatching {
            resolveIssue(stalledIssueResolutionAction, stalledIssue)
        }.onSuccess {
            saveSolvedIssue(stalledIssue, stalledIssueResolutionAction)
        }
    }

    private suspend fun resolveIssue(
        stalledIssueResolutionAction: StalledIssueResolutionAction,
        stalledIssue: StalledIssue,
    ) {
        when (stalledIssueResolutionAction.resolutionActionType) {
            StalledIssueResolutionActionType.RENAME_ALL_ITEMS -> {
                if (stalledIssue.nodeIds.size > 1) {
                    renameNodeWithTheSameNameUseCase(
                        stalledIssue.nodeIds.zip(stalledIssue.nodeNames)
                    )
                } else if (stalledIssue.localPaths.size > 1) {
                    renameFilesWithTheSameNameUseCase(stalledIssue.localPaths)
                }
            }

            StalledIssueResolutionActionType.REMOVE_DUPLICATES -> {
                // will be implemented in a separate MR
            }

            StalledIssueResolutionActionType.MERGE_FOLDERS -> {
                val allFolderNodes = stalledIssue.nodeIds.map {
                    getNodeByHandleUseCase(it.longValue)
                }.map {
                    it as FolderNode
                }

                val mainFolder = allFolderNodes.maxBy {
                    it.childFileCount
                }

                val secondaryFolders = allFolderNodes.filter { it.id != mainFolder.id }

                mergeFolders(mainFolder, secondaryFolders)
            }

            StalledIssueResolutionActionType.REMOVE_DUPLICATES_AND_REMOVE_THE_REST -> {
                // will be implemented in a separate MR
            }

            StalledIssueResolutionActionType.CHOOSE_LOCAL_FILE -> {
                moveNodesToRubbishUseCase(listOf(stalledIssue.nodeIds.first().longValue))
            }

            StalledIssueResolutionActionType.CHOOSE_REMOTE_FILE -> {
                deleteFileUseCase(stalledIssue.localPaths.first())
            }

            StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME -> {
                val localFile = getFileByPathUseCase(stalledIssue.localPaths.first())
                val remoteNode = getNodeByHandleUseCase(stalledIssue.nodeIds.first().longValue)
                if (remoteNode is FileNode && localFile != null) {
                    val remoteModifiedDateInSeconds = remoteNode.modificationTime
                    val localModifiedDateInMilliseconds = localFile.lastModified()
                    if (localModifiedDateInMilliseconds / 1000 > remoteModifiedDateInSeconds) {
                        moveNodesToRubbishUseCase(listOf(stalledIssue.nodeIds.first().longValue))
                    } else {
                        deleteFileUseCase(stalledIssue.localPaths.first())
                    }
                }
            }

            else -> {}
        }
    }

    private suspend fun mergeFolders(mainFolder: FolderNode, secondaryFolders: List<FolderNode>) {
        for (secondaryFolderRoot in secondaryFolders) {
            mergeFoldersIterative(mainFolder, secondaryFolderRoot)
        }
        moveNodesToRubbishUseCase(secondaryFolders.map { it.id.longValue })
    }

    private suspend fun mergeFoldersIterative(mainFolder: FolderNode, secondaryFolder: FolderNode) =
        coroutineScope {
            val stack = ArrayDeque<Pair<FolderNode, FolderNode>>()
            stack.add(Pair(mainFolder, secondaryFolder))

            while (stack.isNotEmpty()) {
                val (currentMainFolder, currentSecondaryFolder) = stack.removeLast()

                val (mainFolderChildren, secondaryFolderChildren) = awaitAll(
                    async { currentMainFolder.fetchChildren(SortOrder.ORDER_DEFAULT_ASC) },
                    async { currentSecondaryFolder.fetchChildren(SortOrder.ORDER_DEFAULT_ASC) },
                )

                for (secondaryNode in secondaryFolderChildren) {
                    when (secondaryNode) {
                        is FileNode -> {
                            mergeFiles(currentMainFolder, secondaryNode, mainFolderChildren)
                        }

                        is FolderNode -> {
                            val sameNameFolderInMainFolder = mainFolderChildren.find {
                                it is FolderNode && it.name == secondaryNode.name
                            }

                            if (sameNameFolderInMainFolder == null) {
                                moveNodeUseCase(secondaryNode.id, currentMainFolder.id)
                            } else {
                                stack.add(
                                    Pair(
                                        sameNameFolderInMainFolder as FolderNode,
                                        secondaryNode
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }


    private suspend fun mergeFiles(
        mainFolder: FolderNode,
        secondaryFile: FileNode,
        mainFolderChildren: List<UnTypedNode>,
    ) {
        val sameNameFileInMainFolder = mainFolderChildren.find {
            it is FileNode && it.name == secondaryFile.name
        }

        if (sameNameFileInMainFolder != null) {
            if (sameNameFileInMainFolder is FileNode && sameNameFileInMainFolder.fingerprint != secondaryFile.fingerprint) {
                addCounterToNodeName(secondaryFile.name, secondaryFile.id, 1)
                moveNodeUseCase(secondaryFile.id, mainFolder.id)
            }
        } else {
            moveNodeUseCase(secondaryFile.id, mainFolder.id)
        }
    }

    private suspend fun addCounterToNodeName(
        nodeName: String,
        nodeId: NodeId,
        counter: Int,
    ) {
        val nodeNameWithoutExtension = nodeName.substringBeforeLast(".")
        val nodeExtension =
            nodeName.substringAfterLast(".", missingDelimiterValue = "")
        val fullNodeExtension = if (nodeExtension.isNotEmpty()) {
            ".$nodeExtension"
        } else {
            ""
        }
        renameNodeUseCase(
            nodeId.longValue,
            "$nodeNameWithoutExtension ($counter)$fullNodeExtension"
        )
    }

    private suspend fun saveSolvedIssue(
        stalledIssue: StalledIssue,
        stalledIssueResolutionAction: StalledIssueResolutionAction,
    ) {
        val solvedIssue = stalledIssueToSolvedIssueMapper(
            stalledIssue,
            stalledIssueResolutionAction.resolutionActionType
        )
        setSyncSolvedIssueUseCase(solvedIssue = solvedIssue)
    }
}