package mega.privacy.android.shared.nodes.namecollision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.namecollision.NameCollisionChoice
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.shares.hasFullAccessPermission
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import mega.privacy.android.domain.usecase.node.CopyCollidedNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveCollidedNodesUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionResultUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionsResultUseCase
import mega.privacy.android.domain.usecase.node.namecollision.IsCopyToOriginUseCase
import mega.privacy.android.domain.usecase.node.namecollision.ReorderNodeNameCollisionsUseCase
import mega.privacy.android.domain.usecase.node.namecollision.UpdateNodeNameCollisionsResultUseCase
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.transfers.DeleteCacheFilesUseCase
import mega.privacy.android.shared.nodes.mapper.message.CopyRequestMessageMapper
import mega.privacy.android.shared.nodes.mapper.message.CopyRequestResult
import mega.privacy.android.shared.nodes.mapper.message.NodeMoveRequestMessageMapper
import mega.privacy.android.shared.nodes.mapper.message.toCopyRequestResult
import timber.log.Timber

/**
 * Compose-ready ViewModel that resolves name collisions on a [StateFlow] surface.
 *
 * It orchestrates the same copy, movement and upload use cases as the legacy name-collision screen
 * but exposes a single [NameCollisionUiState] via [uiState], modelling one-shot outcomes as
 * `StateEventWithContent`. The collision(s) to resolve and the launch context arrive through
 * [NameCollisionArgs] via the assisted [Factory].
 *
 * [uiState] is lazy, so nothing is loaded until the screen starts collecting it.
 */
@HiltViewModel(assistedFactory = NameCollisionViewModel.Factory::class)
class NameCollisionViewModel @AssistedInject constructor(
    private val getFileVersionsOption: GetFileVersionsOption,
    private val getNodeNameCollisionResultUseCase: GetNodeNameCollisionResultUseCase,
    private val getNodeNameCollisionsResultUseCase: GetNodeNameCollisionsResultUseCase,
    private val reorderNodeNameCollisionsUseCase: ReorderNodeNameCollisionsUseCase,
    private val updateNodeNameCollisionsResultUseCase: UpdateNodeNameCollisionsResultUseCase,
    private val copyCollidedNodesUseCase: CopyCollidedNodesUseCase,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val setCopyLatestTargetPathUseCase: SetCopyLatestTargetPathUseCase,
    private val setMoveLatestTargetPathUseCase: SetMoveLatestTargetPathUseCase,
    private val copyRequestMessageMapper: CopyRequestMessageMapper,
    private val nodeMoveRequestMessageMapper: NodeMoveRequestMessageMapper,
    private val isCopyToOriginUseCase: IsCopyToOriginUseCase,
    private val moveCollidedNodesUseCase: MoveCollidedNodesUseCase,
    private val deleteCacheFilesUseCase: DeleteCacheFilesUseCase,
    private val getNodeAccessPermission: GetNodeAccessPermission,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @Assisted private val args: NameCollisionArgs,
) : ViewModel() {

    private val collisions: List<NameCollision> = when (args) {
        is NameCollisionArgs.Single -> listOf(args.collision)
        is NameCollisionArgs.Multiple -> args.collisions
        NameCollisionArgs.MissingLaunch -> emptyList()
    }
    private val isFolderUploadContext = args.isFolderUploadContext
    private val pitagTrigger =
        collisions.firstOrNull()?.pitagTrigger ?: PitagTrigger.NotApplicable

    /**
     * Where the user is in resolving the collisions: the one being presented, what is left after it
     * and the one-shot outcomes. What can be derived from it, such as the file versioning info, is
     * combined into [uiState] instead of being kept here.
     */
    private val progress = MutableStateFlow(
        CollisionProgress(
            currentCollision = null,
            isCopyToOrigin = false,
            pendingFileCollisions = 0,
            pendingFolderCollisions = 0,
            pendingCollisions = emptyList(),
            resolvedCollisions = emptyList(),
            renameNames = emptyList(),
            uploadedFilesCount = 0,
            uploadEvent = consumed(),
            outcome = consumed(),
            dismissEvent = consumed,
        )
    )

    private val isCopyToOrigin: Boolean get() = progress.value.isCopyToOrigin
    private val pendingFileCollisions: Int get() = progress.value.pendingFileCollisions
    private val pendingFolderCollisions: Int get() = progress.value.pendingFolderCollisions
    private val pendingCollisions: List<NodeNameCollisionResult>
        get() = progress.value.pendingCollisions
    private val resolvedCollisions: List<NodeNameCollisionResult>
        get() = progress.value.resolvedCollisions
    private val renameNames: List<String> get() = progress.value.renameNames

    /**
     * True once every collision has been taken off the queue: nothing is presented and nothing waits
     * behind it. The batch paths clear the current collision as they take it, so a collision that is
     * still current is always one awaiting the user's choice.
     */
    private val isQueueDrained: Boolean
        get() = progress.value.currentCollision == null && pendingCollisions.isEmpty()

    /**
     * Immutable UI state.
     *
     * Lazy so the collisions are only loaded, and the file versions option only watched, once the
     * screen starts collecting. The load is started here rather than inside the flow because it must
     * run once per ViewModel: re-running it when the screen resubscribes after the sharing timeout
     * would reset the user's progress through the collisions.
     */
    val uiState: StateFlow<NameCollisionUiState> by lazy {
        loadCollisions()
        combine(progress, fileVersioningInfoFlow()) { resolution, fileVersioningInfo ->
            NameCollisionUiState(
                currentCollision = resolution.currentCollision,
                fileVersioningInfo = fileVersioningInfo,
                isCopyToOrigin = resolution.isCopyToOrigin,
                pendingFileCollisions = resolution.pendingFileCollisions,
                pendingFolderCollisions = resolution.pendingFolderCollisions,
                hasPendingCollisions = resolution.pendingCollisions.isNotEmpty(),
                uploadedFilesCount = resolution.uploadedFilesCount,
                uploadEvent = resolution.uploadEvent,
                outcome = resolution.outcome,
                dismissEvent = resolution.dismissEvent,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(
                scope = viewModelScope,
                initialValue = NameCollisionUiState(
                    currentCollision = null,
                    fileVersioningInfo = null,
                    isCopyToOrigin = false,
                    pendingFileCollisions = 0,
                    pendingFolderCollisions = 0,
                    hasPendingCollisions = false,
                    uploadedFilesCount = 0,
                    uploadEvent = consumed(),
                    outcome = consumed(),
                    dismissEvent = consumed,
                ),
            )
    }

    /**
     * File versioning info of the current collision, recomputed whenever that collision or the file
     * versions option changes. Null while there is no current collision.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun fileVersioningInfoFlow(): Flow<FileVersioningInfo?> = combine(
        progress.map { it.currentCollision?.nameCollision }.distinctUntilChanged(),
        fileVersionsOptionRefreshes(),
    ) { collision, _ -> collision }
        .mapLatest { collision -> collision?.let { fileVersioningInfo(it) } }

    /**
     * Emits once up front and again each time the user disables file versioning, after refreshing
     * the cached file versions option from the server so [fileVersioningInfo] reads the current one.
     */
    private fun fileVersionsOptionRefreshes(): Flow<Unit> = monitorUserUpdates()
        .catch { Timber.w("Exception monitoring user updates: $it") }
        .filter { it == UserChanges.DisableVersions }
        .map { }
        .onStart { emit(Unit) }
        .onEach {
            runCatching { getFileVersionsOption(true) }
                .onFailure { Timber.e(it, "Error refreshing the file versions option") }
        }

    private fun currentCollision(): NodeNameCollisionResult? = progress.value.currentCollision

    private fun updateCurrentCollisionState(collision: NodeNameCollisionResult?) {
        progress.update { it.copy(currentCollision = collision) }
    }

    private fun updateIsCopyToOrigin(isCopyToOrigin: Boolean) {
        progress.update { it.copy(isCopyToOrigin = isCopyToOrigin) }
    }

    /**
     * A node copied into the folder it already lives in is duplicated under a new name without
     * asking, so the check has to complete before the first collision is presented.
     */
    private suspend fun checkCopyToOrigin(collision: NodeNameCollision) {
        val isCopyToOrigin = runCatching { isCopyToOriginUseCase(collision) }
            .onFailure { Timber.w(it, "Error checking whether the copy targets the origin") }
            .getOrDefault(false)
        updateIsCopyToOrigin(isCopyToOrigin)
    }

    /**
     * Update current name collision result
     *
     * @param collision [NameCollision] to resolve.
     * @param rename    Whether to call rename() or not
     */
    private suspend fun updateCurrentCollision(
        collision: NameCollision,
        rename: Boolean,
    ) {
        runCatching {
            getNodeNameCollisionResultUseCase(collision)
        }.onSuccess {
            updateCurrentCollisionState(it)
            currentCollision()?.let {
                if (isCopyToOrigin && rename)
                    rename(true)
            }
        }.onFailure {
            Timber.e(it, "Error getting collisionResult")
            updateCurrentCollisionState(null)
        }
    }

    /**
     * Loads the collisions to resolve described by the [args].
     *
     * The list is reordered to present files first, then folders. The first one becomes the current
     * collision and the rest are loaded as pending ones. When there is nothing pending, the current
     * collision is renamed straight away if it is being copied to its origin.
     */
    private fun loadCollisions() {
        if (collisions.isEmpty()) {
            setDismissEvent()
            return
        }
        viewModelScope.launch {
            runCatching {
                (collisions.first() as? NodeNameCollision)?.let { checkCopyToOrigin(it) }
                reorderNodeNameCollisionsUseCase(collisions)
            }.onSuccess { (reorderedCollisions, pendingFiles, pendingFolders) ->
                progress.update {
                    it.copy(
                        pendingFileCollisions = pendingFiles,
                        pendingFolderCollisions = pendingFolders,
                    )
                }
                val pending = reorderedCollisions.drop(1)
                updateCurrentCollision(reorderedCollisions.first(), pending.isEmpty())
                if (pending.isNotEmpty()) {
                    getPendingCollisions(pending)
                }
            }.onFailure {
                Timber.e(it, "Exception setting data")
                updateCurrentCollisionState(null)
            }
        }
    }

    /**
     * Gets the list with complete data of pending collisions.
     *
     * @param collisions    List of [NameCollision] to resolve.
     */
    private fun getPendingCollisions(
        collisions: List<NameCollision>,
    ) {
        viewModelScope.launch {
            runCatching {
                getNodeNameCollisionsResultUseCase(collisions)
            }.onSuccess { results ->
                progress.update { it.copy(pendingCollisions = results) }
                if (isCopyToOrigin)
                    rename(true)
            }.onFailure {
                Timber.e(it, "No pending collisions")
            }
        }
    }

    /**
     * Builds the file versioning info of [collision] from the cached file versions option, treating
     * a failure to read it as versioning being disabled.
     */
    private suspend fun fileVersioningInfo(collision: NameCollision): FileVersioningInfo {
        val collisionType = getCollisionType(collision)
        val isFileVersioningDisabled = runCatching { getFileVersionsOption(false) }
            .onFailure { Timber.e(it, "Error getting the file versions option") }
            .getOrNull() ?: true
        return FileVersioningInfo(
            isFileVersioningEnabled = isFileVersioningDisabled.not(),
            nameCollisionType = collisionType,
            isFile = collision.isFile,
            canReplaceOrUpdate = canReplaceOrUpdate(collision, collisionType),
        )
    }

    /**
     * Checks whether the existing node can be replaced or updated. Uploading a file with an existing
     * name only replaces or versions the existing node if the user has full access to it, otherwise
     * the SDK silently creates a duplicated node with the same name.
     *
     * @param collision     [NameCollision] to resolve.
     * @param collisionType [NameCollisionType] of the collision.
     * @return True if the existing node can be replaced or updated, false otherwise.
     */
    private suspend fun canReplaceOrUpdate(
        collision: NameCollision,
        collisionType: NameCollisionType,
    ): Boolean {
        if (collisionType != NameCollisionType.UPLOAD || !collision.isFile) return true

        return runCatching { getNodeAccessPermission(NodeId(collision.collisionHandle)) }
            .onFailure { Timber.e(it, "Error getting the access permission") }
            .getOrNull()
            ?.hasFullAccessPermission() ?: true
    }

    /**
     * Gets the [NameCollisionType] of a collision.
     *
     * @param collision [NameCollision] to check, the current one by default.
     * @return The type of the collision.
     */
    private fun getCollisionType(
        collision: NameCollision? = currentCollision()?.nameCollision,
    ): NameCollisionType =
        when (collision) {
            is NodeNameCollision.Chat, is NodeNameCollision.FileLink -> {
                NameCollisionType.COPY
            }

            is NodeNameCollision.Default -> {
                when (collision.type) {
                    NodeNameCollisionType.MOVE -> NameCollisionType.MOVE
                    else -> NameCollisionType.COPY
                }
            }

            is FileNameCollision -> NameCollisionType.UPLOAD
            else -> NameCollisionType.COPY
        }

    /**
     * After having the option "Apply on the next conflicts" checked with both files and folders
     * pending, takes the current collision and every file behind it off the queue so the caller can
     * apply the same choice to all of them. The queue is left on the first folder, which is then
     * presented for a choice of its own.
     *
     * @return The file collisions taken off the queue, the current one first.
     */
    private fun proceedWithAllFiles(): List<NodeNameCollisionResult> = buildList {
        var current = currentCollision()
        while (current != null && current.nameCollision.isFile) {
            add(current)
            presentNextCollision()
            current = currentCollision()
        }
    }

    /**
     * After having the option "Apply on the next conflicts" checked, manages data to apply
     * the cancel action for all the pending collisions.
     */
    private fun cancelAll() {
        when {
            isFolderUploadContext -> {
                val resolved = resolvedCollisions + getAllPendingCollisions().map {
                    it.copy(choice = NameCollisionChoice.CANCEL)
                }
                progress.update { it.copy(resolvedCollisions = resolved) }
                setOutcome(NameCollisionOutcome.Resolved(resolved))
            }

            else -> {
                progress.update {
                    it.copy(pendingCollisions = emptyList(), currentCollision = null)
                }
                setDismissEvent()
            }
        }
    }

    /**
     * Records the user's [choice] for the current collision and moves the queue on to the next one.
     * Callers decide what a drained queue means for them, since a skip or an upload leaves the screen
     * with a dismiss while a copy or a movement raises a terminal outcome.
     *
     *  @param choice    Resolution type as [NameCollisionChoice].
     */
    private fun continueWithNext(choice: NameCollisionChoice) {
        if (isFolderUploadContext) {
            val resolved = (currentCollision() ?: return).copy(choice = choice)
            progress.update { it.copy(resolvedCollisions = it.resolvedCollisions + resolved) }

            if (pendingCollisions.isEmpty()) {
                setOutcome(NameCollisionOutcome.Resolved(resolvedCollisions))
                return
            }
        }
        presentNextCollision()
    }

    /**
     * Makes the first pending collision the current one, or clears the current one when nothing is
     * pending, leaving the queue drained for the caller to act on. The pending counts exclude the
     * first collision of each type, so the first folder to follow the files takes nothing off the
     * folder count.
     */
    private fun presentNextCollision() {
        val nextCollision = pendingCollisions.firstOrNull() ?: run {
            updateCurrentCollisionState(null)
            return
        }
        val isFile = nextCollision.nameCollision.isFile
        val isFirstFolder = !isFile && currentCollision()?.nameCollision?.isFile == true
        progress.update {
            it.copy(
                currentCollision = nextCollision,
                pendingCollisions = it.pendingCollisions.drop(1),
                pendingFileCollisions = if (isFile) it.pendingFileCollisions - 1 else it.pendingFileCollisions,
                pendingFolderCollisions = if (isFile || isFirstFolder) it.pendingFolderCollisions else it.pendingFolderCollisions - 1,
            )
        }
    }

    /**
     * Replaces, updates or merges an existing node depending on if the collision
     * is related to a file (replaces if file versioning disabled, updates if enabled)
     * or to a folder (merges) with which the current item has the collision. Applies the same
     * for the next ones if [applyOnNext].
     *
     * @param applyOnNext   True if should apply for the next file or folder collisions if any,
     *                      false otherwise.
     */
    fun replaceUpdateOrMerge(applyOnNext: Boolean) {
        proceedWithAction(applyOnNext = applyOnNext, rename = false)
    }

    /**
     * Dismisses the current collision and the next ones if [applyOnNext].
     *
     * @param applyOnNext   True if should dismiss the next file or folder collisions.
     */
    fun cancel(applyOnNext: Boolean) {
        deleteCacheFilesForPendingFileCollisions(applyOnNext)
        when {
            applyOnNext && pendingFileCollisions > 0 && pendingFolderCollisions > 0 ->
                skipAllFiles()

            applyOnNext -> cancelAll()
            else -> {
                continueWithNext(NameCollisionChoice.CANCEL)
                if (isQueueDrained) setDismissEvent()
            }
        }
    }

    /**
     * Skips the current file and the pending ones, leaving the folders to be presented. In the folder
     * upload context the skipped files still have to be reported back to the caller with their choice.
     */
    private fun skipAllFiles() {
        val skipped = proceedWithAllFiles().map { it.copy(choice = NameCollisionChoice.CANCEL) }
        if (isFolderUploadContext) {
            progress.update { it.copy(resolvedCollisions = it.resolvedCollisions + skipped) }
        }
    }

    private fun deleteCacheFilesForPendingFileCollisions(applyOnNext: Boolean) {
        val pathsToDelete = buildList {
            add(currentCollision())
            if (applyOnNext) {
                addAll(pendingCollisions)
            }
        }.mapNotNull { (it?.nameCollision as? FileNameCollision)?.path }
        // Cache files must go even if the screen is left before this completes.
        applicationScope.launch {
            runCatching {
                deleteCacheFilesUseCase(pathsToDelete)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Renames the current item and the next ones if [applyOnNext].
     *
     * @param applyOnNext   True if should rename the next collisions.
     */
    fun rename(applyOnNext: Boolean) {
        val renameName = currentCollision()?.nameCollision?.renameName ?: return
        progress.update { it.copy(renameNames = it.renameNames + renameName) }
        viewModelScope.launch {
            runCatching {
                updateNodeNameCollisionsResultUseCase(
                    pendingCollisions,
                    renameNames,
                    applyOnNext
                )
            }.onSuccess { (updatedCollisions, updatedRenameNames) ->
                progress.update {
                    it.copy(
                        pendingCollisions = updatedCollisions,
                        renameNames = updatedRenameNames,
                    )
                }
                proceedWithAction(applyOnNext = applyOnNext, rename = true)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Proceeds with the user's choice for the current collision and, if [applyOnNext], for the
     * pending ones too. With both files and folders pending, "apply on next" only covers the files:
     * the folders are left for a choice of their own.
     */
    private fun proceedWithAction(applyOnNext: Boolean, rename: Boolean) {
        val collisionType = getCollisionType()
        when {
            applyOnNext && pendingFileCollisions > 0 && pendingFolderCollisions > 0 ->
                proceedWithAll(collisionType, proceedWithAllFiles(), rename)

            applyOnNext -> proceedWithAll(collisionType, getAllPendingCollisions(), rename)
            else -> proceedWithCurrent(collisionType, rename)
        }
    }

    private fun proceedWithCurrent(collisionType: NameCollisionType, rename: Boolean) {
        val current = listOfNotNull(currentCollision())
        when (collisionType) {
            NameCollisionType.UPLOAD -> singleUpload(rename)
            NameCollisionType.MOVE -> move(current, rename, thenContinue = true)
            NameCollisionType.COPY -> copy(current, rename, thenContinue = true)
        }
    }

    private fun proceedWithAll(
        collisionType: NameCollisionType,
        collisions: List<NodeNameCollisionResult>,
        rename: Boolean,
    ) {
        when (collisionType) {
            NameCollisionType.UPLOAD -> upload(collisions, rename)
            NameCollisionType.MOVE -> move(collisions, rename, thenContinue = false)
            NameCollisionType.COPY -> copy(collisions, rename, thenContinue = false)
        }
    }

    private fun choiceFor(rename: Boolean) =
        if (rename) NameCollisionChoice.RENAME else NameCollisionChoice.REPLACE_UPDATE_MERGE

    /**
     * Takes every collision off the queue, the current one included, and returns them. Nothing is
     * left to present, so the queue is drained once the caller's action completes.
     */
    private fun getAllPendingCollisions(): List<NodeNameCollisionResult> {
        val allPendingCollisions = listOfNotNull(currentCollision()) + pendingCollisions
        progress.update { it.copy(currentCollision = null, pendingCollisions = emptyList()) }
        return allPendingCollisions
    }

    /**
     * Proceeds with the upload of the current collision.
     *
     * @param rename    True if should rename the file, false otherwise.
     */
    private fun singleUpload(rename: Boolean) {
        val choice = choiceFor(rename)

        if (currentCollision()?.nameCollision?.isFile == true) {
            progress.update { it.copy(uploadedFilesCount = it.uploadedFilesCount + 1) }
        }

        if (isFolderUploadContext) {
            continueWithNext(choice)
            return
        }

        val currentCollision = currentCollision() ?: return

        viewModelScope.launch {
            val parentId = currentCollision.nameCollision.parentHandle
            val path = (currentCollision.nameCollision as FileNameCollision).path.value
            val name = if (rename)
                currentCollision.nameCollision.renameName
            else
                currentCollision.nameCollision.name
            uploadFiles(
                mapOf(path to name),
                NodeId(parentId),
                choice
            )
        }
    }

    /**
     * Proceeds with the upload of the collisions list.
     *
     * @param list      List of collisions to upload.
     * @param rename    True if should rename the file, false otherwise.
     */
    private fun upload(
        list: List<NodeNameCollisionResult>,
        rename: Boolean,
    ) {
        val fileCount = list.count { it.nameCollision.isFile }
        progress.update { it.copy(uploadedFilesCount = it.uploadedFilesCount + fileCount) }

        if (isFolderUploadContext) {
            val choice = choiceFor(rename)
            val resolved = resolvedCollisions + list.map { it.copy(choice = choice) }
            progress.update { it.copy(resolvedCollisions = resolved) }

            if (isQueueDrained) {
                setOutcome(NameCollisionOutcome.Resolved(resolved))
            }
            return
        }

        viewModelScope.launch {
            val parentId = list.first().nameCollision.parentHandle
            val pathsAndNames = list
                .filter { it.nameCollision is FileNameCollision }
                .associate {
                    (it.nameCollision as FileNameCollision).path.value to
                            if (rename) it.nameCollision.renameName else it.nameCollision.name
                }
            uploadFiles(
                pathsAndNames = pathsAndNames,
                destinationId = NodeId(parentId),
                choice = null,
            )
        }
    }

    /**
     * Moves the nodes of [collisions]. After the current collision alone the next one is presented;
     * after a batch the queue is already positioned, so [thenContinue] is false.
     */
    private fun move(
        collisions: List<NodeNameCollisionResult>,
        rename: Boolean,
        thenContinue: Boolean,
    ) {
        val nodeCollisions = collisions.nodeCollisions()
        if (nodeCollisions.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                moveCollidedNodesUseCase(nodeCollisions, rename)
            }.onSuccess {
                if (thenContinue) continueWithNext(choiceFor(rename))
                setMovementResult(it, nodeCollisions.first().parentHandle)
            }.onFailure {
                setOutcome(NameCollisionOutcome.Failed(it))
                Timber.w(it)
            }
        }
    }

    /**
     * Sets the movement result.
     *
     * @param movementResult    [MoveRequestResult.GeneralMovement] containing all the required info
     *                          about the movement.
     */
    private fun setMovementResult(
        movementResult: MoveRequestResult.GeneralMovement,
        moveToHandle: Long,
    ) {
        if (moveToHandle != -1L)
            setMoveLatestPath(moveToHandle)
        // The outcome is only raised once the whole queue is resolved; the intermediate movements of a
        // sequential resolution simply advance to the next collision.
        if (!isQueueDrained) return
        setOutcome(
            NameCollisionOutcome.ActionCompleted(
                NameCollisionActionResult(
                    message = nodeMoveRequestMessageMapper(movementResult),
                    shouldFinish = true,
                )
            )
        )
    }

    /**
     * Copies the nodes of [collisions]. After the current collision alone the next one is presented;
     * after a batch the queue is already positioned, so [thenContinue] is false.
     */
    private fun copy(
        collisions: List<NodeNameCollisionResult>,
        rename: Boolean,
        thenContinue: Boolean,
    ) {
        val nodeCollisions = collisions.nodeCollisions()
        if (nodeCollisions.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                copyCollidedNodesUseCase(nameCollisions = nodeCollisions, rename = rename)
            }.onSuccess {
                if (thenContinue) continueWithNext(choiceFor(rename))
                setCopyResult(
                    copyResult = it.toCopyRequestResult(),
                    copyToHandle = nodeCollisions.first().parentHandle,
                )
            }.onFailure {
                setOutcome(NameCollisionOutcome.Failed(it))
                Timber.w(it)
            }
        }
    }

    private fun List<NodeNameCollisionResult>.nodeCollisions() =
        map { it.nameCollision }.filterIsInstance<NodeNameCollision>()

    /**
     * Sets the copy result.
     *
     * @param copyResult    [CopyRequestResult] containing all the required info about the copy.
     */
    private fun setCopyResult(copyResult: CopyRequestResult, copyToHandle: Long) {
        if (copyToHandle != -1L)
            setCopyLatestPath(copyToHandle)
        // The outcome is only raised once the whole queue is resolved; the intermediate copies of a
        // sequential resolution simply advance to the next collision.
        if (!isQueueDrained) return
        setOutcome(
            NameCollisionOutcome.ActionCompleted(
                NameCollisionActionResult(
                    message = copyRequestMessageMapper(copyResult),
                    shouldFinish = true,
                )
            )
        )
    }

    /**
     * Set last used path of copy as target path for next copy
     */
    private fun setCopyLatestPath(path: Long) {
        viewModelScope.launch {
            runCatching { setCopyLatestTargetPathUseCase(path) }
                .onFailure { Timber.e(it) }
        }
    }

    /**
     * Set last used path of move as target path for next move
     */
    private fun setMoveLatestPath(path: Long) {
        viewModelScope.launch {
            runCatching { setMoveLatestTargetPathUseCase(path) }
                .onFailure { Timber.e(it) }
        }
    }

    private fun uploadFiles(
        pathsAndNames: Map<String, String?>,
        destinationId: NodeId,
        choice: NameCollisionChoice?,
    ) {
        progress.update { state ->
            state.copy(
                uploadEvent = triggered(
                    TransferTriggerEvent.StartUpload.CollidedFiles(
                        pathsAndNames = pathsAndNames,
                        destinationId = destinationId,
                        collisionChoice = choice,
                        pitagTrigger = pitagTrigger,
                    )
                )
            )
        }
    }

    /**
     * Consumes the upload event.
     */
    fun consumeUploadEvent() {
        with(progress.value.uploadEvent) {
            if (this is StateEventWithContentTriggered) {
                // A batch upload carries no choice to advance by: it already took its files off the
                // queue, leaving any folders behind them to be presented for a choice of their own.
                content.collisionChoice?.let { continueWithNext(it) }
                if (isQueueDrained) setDismissEvent()
                progress.update { state -> state.copy(uploadEvent = consumed()) }
            }
        }
    }

    private fun setOutcome(outcome: NameCollisionOutcome) {
        progress.update { it.copy(outcome = triggered(outcome)) }
    }

    /**
     * Consumes the outcome event.
     */
    fun consumeOutcome() {
        progress.update { it.copy(outcome = consumed()) }
    }

    private fun setDismissEvent() {
        progress.update { it.copy(dismissEvent = triggered) }
    }

    /**
     * Consumes the dismiss event.
     */
    fun consumeDismissEvent() {
        progress.update { it.copy(dismissEvent = consumed) }
    }

    private data class CollisionProgress(
        val currentCollision: NodeNameCollisionResult?,
        val isCopyToOrigin: Boolean,
        val pendingFileCollisions: Int,
        val pendingFolderCollisions: Int,
        val pendingCollisions: List<NodeNameCollisionResult>,
        val resolvedCollisions: List<NodeNameCollisionResult>,
        val renameNames: List<String>,
        val uploadedFilesCount: Int,
        val uploadEvent: StateEventWithContent<TransferTriggerEvent.StartUpload.CollidedFiles>,
        val outcome: StateEventWithContent<NameCollisionOutcome>,
        val dismissEvent: StateEvent,
    )

    /**
     * Factory to create a [NameCollisionViewModel] with the collision(s) to resolve.
     */
    @AssistedFactory
    interface Factory {
        /**
         * @param args The [NameCollisionArgs] describing the collision(s) and launch context.
         */
        fun create(args: NameCollisionArgs): NameCollisionViewModel
    }
}
