package mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.UnTypedNode
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.file.DeleteDocumentFileBySyncContentUriUseCase
import mega.privacy.android.domain.usecase.file.GetLastModifiedTimeForSyncContentUriUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.node.RenameNodeUseCase
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionAction
import mega.privacy.android.feature.sync.domain.entity.StalledIssueResolutionActionType
import mega.privacy.android.feature.sync.domain.mapper.StalledIssueToSolvedIssueMapper
import mega.privacy.android.feature.sync.domain.usecase.solvedissue.SetSyncSolvedIssueUseCase
import javax.inject.Inject
import kotlin.time.ExperimentalTime

internal class ResolveStalledIssueUseCase @Inject constructor(
    private val deleteDocumentFileByContentUriUseCase: DeleteDocumentFileBySyncContentUriUseCase,
    private val getLastModifiedTimeForSyncContentUriUseCase: GetLastModifiedTimeForSyncContentUriUseCase,
    private val moveNodesToRubbishUseCase: MoveNodesToRubbishUseCase,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
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
        }
        runCatching {
            saveSolvedIssue(stalledIssue, stalledIssueResolutionAction)
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun resolveIssue(
        stalledIssueResolutionAction: StalledIssueResolutionAction,
        stalledIssue: StalledIssue,
    ) {
        when (stalledIssueResolutionAction.resolutionActionType) {
            StalledIssueResolutionActionType.RENAME_ALL_ITEMS -> {
                if (stalledIssue.nodeIds.size > 1) {
                    renameNodeWithTheSameNameUseCase(
                        stalledIssue.nodeIds
                    )
                } else if (stalledIssue.localPaths.size > 1) {
                    renameFilesWithTheSameNameUseCase(stalledIssue.localPaths.map { UriPath(it) })
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
                deleteDocumentFileByContentUriUseCase(UriPath(stalledIssue.localPaths.first()))
            }

            StalledIssueResolutionActionType.CHOOSE_LATEST_MODIFIED_TIME -> {
                val lastModifiedInstant =
                    getLastModifiedTimeForSyncContentUriUseCase(UriPath(stalledIssue.localPaths.first()))
                val remoteNode = getNodeByHandleUseCase(stalledIssue.nodeIds.first().longValue)
                if (remoteNode is FileNode && lastModifiedInstant != null) {
                    val remoteModifiedDateInSeconds = remoteNode.modificationTime
                    val localModifiedDateInMilliseconds = lastModifiedInstant.toEpochMilliseconds()
                    if (localModifiedDateInMilliseconds / 1000 > remoteModifiedDateInSeconds) {
                        moveNodesToRubbishUseCase(listOf(stalledIssue.nodeIds.first().longValue))
                    } else {
                        deleteDocumentFileByContentUriUseCase(UriPath(stalledIssue.localPaths.first()))
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
            it is FileNode && it.name.equals(secondaryFile.name, ignoreCase = true)
        }

        if (sameNameFileInMainFolder != null) {
            if (sameNameFileInMainFolder is FileNode) {
                // Rename if different fingerprint OR different case (case-insensitive filesystem conflict)
                val needsRename =
                    sameNameFileInMainFolder.fingerprint != secondaryFile.fingerprint ||
                            sameNameFileInMainFolder.name != secondaryFile.name
                if (needsRename) {
                    // Find unique counter to avoid conflicts with existing files
                    val uniqueCounter = findUniqueCounter(secondaryFile.name, mainFolderChildren)
                    addCounterToNodeName(secondaryFile.name, secondaryFile.id, uniqueCounter)
                }
                moveNodeUseCase(secondaryFile.id, mainFolder.id)
            }
        } else {
            moveNodeUseCase(secondaryFile.id, mainFolder.id)
        }
    }

    /**
     * Finds the first available counter to append to a node name to make it unique.
     * Example: "file.txt" -> "file (1).txt", "file (3).txt", next available is "file (2).txt"
     */

    private fun findUniqueCounter(
        nodeName: String,
        existingNodes: List<UnTypedNode>,
    ): Int {
        val nodeNameWithoutExtension = nodeName.substringBeforeLast(".")
        val nodeExtension = nodeName.substringAfterLast(".", missingDelimiterValue = "")
        val fullNodeExtension = if (nodeExtension.isNotEmpty()) {
            ".$nodeExtension"
        } else {
            ""
        }

        // Get all existing file names for quick lookup (case-insensitive)
        val existingFileNames = existingNodes
            .filterIsInstance<FileNode>()
            .map { it.name.lowercase() }
            .toSet()

        // Find the first available counter starting from 1
        for (counter in 1..Int.MAX_VALUE) {
            val candidateName = "$nodeNameWithoutExtension ($counter)$fullNodeExtension"
            if (!existingFileNames.contains(candidateName.lowercase())) {
                return counter
            }
        }

        // This should never happen in practice, but return 1 as fallback
        return 1
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
