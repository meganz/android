package mega.privacy.android.app.namecollision

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContentTriggered
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.namecollision.data.NameCollisionActionResult
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.data.NameCollisionUiEntity
import mega.privacy.android.app.namecollision.model.NameCollisionUiState
import mega.privacy.android.app.presentation.copynode.CopyRequestResult
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.copynode.toCopyRequestResult
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.node.FileNameCollision
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NameCollision
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.namecollision.NameCollisionChoice
import mega.privacy.android.domain.entity.node.namecollision.NodeNameCollisionResult
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import mega.privacy.android.domain.usecase.node.CopyCollidedNodeUseCase
import mega.privacy.android.domain.usecase.node.CopyCollidedNodesUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByFingerprintAndParentNodeUseCase
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import mega.privacy.android.domain.usecase.node.MoveCollidedNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveCollidedNodesUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionResultUseCase
import mega.privacy.android.domain.usecase.node.namecollision.GetNodeNameCollisionsResultUseCase
import mega.privacy.android.domain.usecase.node.namecollision.ReorderNodeNameCollisionsUseCase
import mega.privacy.android.domain.usecase.node.namecollision.UpdateNodeNameCollisionsResultUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel which manages data of [NameCollisionActivity]
 */
@HiltViewModel
class NameCollisionViewModel @Inject constructor(
    private val getFileVersionsOption: GetFileVersionsOption,
    private val getNodeNameCollisionResultUseCase: GetNodeNameCollisionResultUseCase,
    private val getNodeNameCollisionsResultUseCase: GetNodeNameCollisionsResultUseCase,
    private val reorderNodeNameCollisionsUseCase: ReorderNodeNameCollisionsUseCase,
    private val updateNodeNameCollisionsResultUseCase: UpdateNodeNameCollisionsResultUseCase,
    private val copyCollidedNodeUseCase: CopyCollidedNodeUseCase,
    private val copyCollidedNodesUseCase: CopyCollidedNodesUseCase,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase,
    private val setCopyLatestTargetPathUseCase: SetCopyLatestTargetPathUseCase,
    private val setMoveLatestTargetPathUseCase: SetMoveLatestTargetPathUseCase,
    private val copyRequestMessageMapper: CopyRequestMessageMapper,
    private val moveRequestMessageMapper: MoveRequestMessageMapper,
    private val getNodeByFingerprintAndParentNodeUseCase: GetNodeByFingerprintAndParentNodeUseCase,
    private val moveCollidedNodeUseCase: MoveCollidedNodeUseCase,
    private val moveCollidedNodesUseCase: MoveCollidedNodesUseCase,
) : ViewModel() {
    private val composite = CompositeDisposable()

    private val _uiState = MutableStateFlow(NameCollisionUiState())

    /**
     * Immutable UI state
     */
    val uiState: StateFlow<NameCollisionUiState> = _uiState

    private val currentCollision: MutableLiveData<NodeNameCollisionResult?> = MutableLiveData()
    private val fileVersioningInfo: MutableLiveData<Triple<Boolean, NameCollisionType, Boolean>> =
        MutableLiveData()
    private val actionResult: MutableLiveData<NameCollisionActionResult> = MutableLiveData()
    private val collisionsResolution: MutableLiveData<ArrayList<NodeNameCollisionResult>> =
        MutableLiveData()
    private val throwable = SingleLiveEvent<Throwable>()
    private val renameNames = mutableListOf<String>()
    private val resolvedCollisions = mutableListOf<NodeNameCollisionResult>()
    var isFolderUploadContext = false
    private val pendingCollisions: MutableList<NodeNameCollisionResult> = mutableListOf()
    var pendingFileCollisions = 0
    var pendingFolderCollisions = 0
    private var allCollisionsProcessed = false
    var isCopyToOrigin = false

    init {
        viewModelScope.launch {
            monitorUserUpdates()
                .catch { Timber.w("Exception monitoring user updates: $it") }
                .filter { it == UserChanges.DisableVersions }
                .map {
                    getFileVersionsOption(true)
                }
                .onStart {
                    getFileVersionsOption(true)
                }.catch { Timber.e(it) }
                .collect {
                    updateFileVersioningInfo()
                }
        }
    }

    /**
     * Checks if a node is attempted to be copied to its parent folder again
     * isCopyToOrigin flag is used to create a duplicate node
     *
     * @param collision [NodeNameCollision] to resolve.
     */
    private suspend fun checkCopyToOrigin(collision: NodeNameCollision.Default) {
        if (collision.type != NodeNameCollisionType.COPY) return
        runCatching {
            getNodeByHandleUseCase(collision.nodeHandle, true)
        }.getOrNull()?.let {
            isCopyToOrigin = it.parentId.longValue == collision.parentHandle
        } ?: runCatching {
            val node = MegaNode.unserialize(collision.serializedData)
            requireNotNull(node)
            if (!node.isForeign) {
                node.fingerprint?.let { fingerprint ->
                    getNodeByFingerprintAndParentNodeUseCase(
                        fingerprint = fingerprint,
                        parentNode = NodeId(collision.parentHandle)
                    )?.let {
                        isCopyToOrigin = it.parentId.longValue == collision.parentHandle
                    }
                }
            }
        }.onFailure {
            Timber.w("Fingerprint is null", it)
        }
    }

    /**
     * Sets the initial data for resolving a single name collision.
     *
     * @param collision [NameCollision] to resolve.
     */
    fun setSingleData(collision: NameCollision, context: Context) {
        viewModelScope.launch {
            runCatching {
                if (collision is NodeNameCollision.Default) {
                    checkCopyToOrigin(collision)
                }
                updateCurrentCollision(collision, context, true)
            }.onFailure { Timber.e("Exception setting single data $it") }
        }
    }

    /**
     * Update current name collision result
     *
     * @param collision [NameCollision] to resolve.
     * @param context   Required Context for uploads.
     * @param rename    Whether to call rename() or not
     */
    private suspend fun updateCurrentCollision(
        collision: NameCollision,
        context: Context,
        rename: Boolean,
    ) {
        runCatching {
            getNodeNameCollisionResultUseCase(collision)
        }.onSuccess {
            currentCollision.value = it
            updateFileVersioningInfo()
            currentCollision.value?.let {
                if (isCopyToOrigin && rename)
                    rename(context, true)
            }
        }.onFailure {
            Timber.e(it, "Error getting collisionResult")
            currentCollision.value = null
        }
    }

    /**
     * Sets the initial data for resolving a list of name collisions.
     * Reorders the list to show files first, then folders. Then gets the current collision.
     *
     * @param collisions    ArrayList of [NameCollision] to resolve.
     * @param context       Required Context for uploads.
     */
    fun setData(collisions: List<NameCollision>, context: Context) {
        viewModelScope.launch {
            runCatching {
                require(collisions.isNotEmpty()) { "Collisions list is empty" }
                collisions.first().let {
                    if (it is NodeNameCollision.Default) {
                        checkCopyToOrigin(it)
                    }
                }
                reorderNodeNameCollisionsUseCase(collisions)
            }.onSuccess { (collisions, pendingFiles, pendingFolders) ->
                val reorderedCollisions = collisions.toMutableList()
                pendingFileCollisions = pendingFiles
                pendingFolderCollisions = pendingFolders
                updateCurrentCollision(reorderedCollisions.first(), context, false)
                reorderedCollisions.removeAt(0)
                if (reorderedCollisions.isNotEmpty()) {
                    getPendingCollisions(reorderedCollisions, context)
                }
            }.onFailure {
                Timber.e("Exception setting data", it)
                currentCollision.value = null
            }
        }
    }

    /**
     * Gets the list with complete data of pending collisions.
     *
     * @param collisions    MutableList of [NameCollisionUiEntity] to resolve.
     * @param context       Required Context for uploads.
     */
    private fun getPendingCollisions(
        collisions: MutableList<NameCollision>,
        context: Context,
    ) {
        viewModelScope.launch {
            runCatching {
                getNodeNameCollisionsResultUseCase(collisions)
            }.onSuccess {
                pendingCollisions.clear()
                pendingCollisions.addAll(it)
                if (isCopyToOrigin)
                    rename(context, true)
            }.onFailure {
                Timber.e(it, "No pending collisions")
            }
        }
    }

    /**
     * Updates file versioning info.
     */
    private fun updateFileVersioningInfo() {
        val currentCollision = currentCollision.value ?: return
        viewModelScope.launch {
            runCatching { getFileVersionsOption(false) }
                .onSuccess { isFileDisable ->
                    fileVersioningInfo.value =
                        Triple(
                            isFileDisable.not(),
                            getCollisionType(),
                            currentCollision.nameCollision.isFile
                        )
                }
        }
    }

    /**
     * Gets the [NameCollisionType] of the current collision.
     *
     * @return The type of the current collision.
     */
    private fun getCollisionType(): NameCollisionType =
        when (val collision = currentCollision.value?.nameCollision) {
            is NodeNameCollision.Chat -> {
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
     * After having the option "Apply on the next conflicts" checked, manages data to apply
     * the same user's choice for all the pending file collisions.
     *
     * @param choice    Resolution type as [NameCollisionChoice].
     * @return A MutableList with all the pending file collisions.
     */
    private fun proceedWithAllFiles(choice: NameCollisionChoice): MutableList<NodeNameCollisionResult> {
        val fileCollisions = mutableListOf<NodeNameCollisionResult>().apply {
            add(currentCollision.value ?: return@apply)
        }

        while (pendingFileCollisions > 0) {
            fileCollisions.add(pendingCollisions[0])
            continueWithNext(choice)
        }

        return fileCollisions
    }

    /**
     * After having the option "Apply on the next conflicts" checked, manages data to apply
     * the cancel action for all the pending collisions.
     */
    private fun cancelAll() {
        when {
            isFolderUploadContext -> {
                getAllPendingCollisions().forEach { pendingCollision ->
                    resolvedCollisions.add(pendingCollision.apply {
                        this.choice = NameCollisionChoice.CANCEL
                    })
                }

                collisionsResolution.value = arrayListOf<NodeNameCollisionResult>().apply {
                    addAll(resolvedCollisions)
                }
            }

            else -> currentCollision.value = null
        }
    }

    /**
     * Manages data to pass to the next collision.
     *
     *  @param choice    Resolution type as [NameCollisionChoice].
     */
    private fun continueWithNext(choice: NameCollisionChoice) {
        if (isFolderUploadContext) {
            resolvedCollisions.add((currentCollision.value ?: return).apply {
                this.choice = choice
            })

            if (pendingCollisions.isEmpty()) {
                collisionsResolution.value = arrayListOf<NodeNameCollisionResult>().apply {
                    addAll(resolvedCollisions)
                }
                return
            }
        }

        if (pendingCollisions.isEmpty()) {
            currentCollision.value = null
            return
        }

        val nextCollision = pendingCollisions[0]
        pendingCollisions.removeAt(0)
        if (nextCollision.nameCollision.isFile) {
            pendingFileCollisions--
        } else {
            pendingFolderCollisions--
        }
        currentCollision.value = nextCollision
        updateFileVersioningInfo()
    }

    /**
     * Replaces, updates or merges an existing node depending on if the collision
     * is related to a file (replaces if file versioning disabled, updates if enabled)
     * or to a folder (merges) with which the current item has the collision. Applies the same
     * for the next ones if [applyOnNext].
     *
     * @param context       Context required for start uploads.
     * @param applyOnNext   True if should apply for the next file or folder collisions if any,
     *                      false otherwise.
     */
    fun replaceUpdateOrMerge(context: Context, applyOnNext: Boolean) {
        proceedWithAction(context, applyOnNext)
    }

    /**
     * Dismisses the current collision and the next ones if [applyOnNext].
     *
     * @param applyOnNext   True if should dismiss the next file or folder collisions.
     */
    fun cancel(applyOnNext: Boolean) {
        when {
            applyOnNext && pendingFileCollisions > 0 && pendingFolderCollisions > 0 ->
                proceedWithAllFiles(NameCollisionChoice.CANCEL)

            applyOnNext -> cancelAll()
            else -> continueWithNext(NameCollisionChoice.CANCEL)
        }
    }

    /**
     * Renames the current item and the next ones if [applyOnNext]. Only available for files.
     *
     * @param context       Required Context for uploads.
     * @param applyOnNext   True if should rename the next file collisions.
     */
    fun rename(context: Context, applyOnNext: Boolean) {
        renameNames.add(currentCollision.value?.nameCollision?.renameName ?: return)
        viewModelScope.launch {
            runCatching {
                updateNodeNameCollisionsResultUseCase(
                    pendingCollisions,
                    renameNames,
                    applyOnNext
                )
            }.onSuccess { (updatedCollisions, updatedRenameNames) ->
                pendingCollisions.clear()
                pendingCollisions.addAll(updatedCollisions)
                renameNames.clear()
                renameNames.addAll(updatedRenameNames)
                proceedWithAction(context = context, applyOnNext = applyOnNext, rename = true)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Proceeds with the user's choice for resolving the current collision and the next ones if [applyOnNext].
     *
     * @param context       Required Context for uploads, null otherwise.
     * @param applyOnNext   True if should apply the same action for the next file or folder collisions,
     *                      false otherwise.
     * @param rename        True if the user's choice is rename, false otherwise.
     */
    fun proceedWithAction(
        context: Context,
        applyOnNext: Boolean,
        rename: Boolean = false,
    ) {
        val choice =
            if (rename) NameCollisionChoice.RENAME
            else NameCollisionChoice.REPLACE_UPDATE_MERGE

        when (getCollisionType()) {
            NameCollisionType.UPLOAD -> {
                when {
                    applyOnNext && pendingFileCollisions > 0 && pendingFolderCollisions > 0 -> {
                        upload(proceedWithAllFiles(choice), rename)
                    }

                    applyOnNext -> upload(getAllPendingCollisions(), rename)
                    else -> singleUpload(rename)
                }
            }

            NameCollisionType.MOVE -> {
                when {
                    applyOnNext && pendingFileCollisions > 0 && pendingFolderCollisions > 0 -> {
                        move(proceedWithAllFiles(choice), rename)
                    }

                    applyOnNext -> move(getAllPendingCollisions(), rename)
                    else -> singleMovement(rename)
                }
            }

            NameCollisionType.COPY -> {
                when {
                    applyOnNext && pendingFileCollisions > 0 && pendingFolderCollisions > 0 -> {
                        copy(proceedWithAllFiles(choice).map {
                            it.nameCollision
                        }.filterIsInstance<NodeNameCollision>(), rename)
                    }

                    applyOnNext -> copy(getAllPendingCollisions().map {
                        it.nameCollision
                    }.filterIsInstance<NodeNameCollision>(), rename)

                    else -> {
                        val nameCollision =
                            currentCollision.value?.nameCollision as? NodeNameCollision ?: return
                        singleCopy(nameCollision = nameCollision, rename = rename)
                    }
                }
            }
        }
    }

    /**
     * Processes all the pending collisions, including the current one,
     * and returns a list with all of them.
     */
    private fun getAllPendingCollisions(): MutableList<NodeNameCollisionResult> {
        val allPendingCollisions = mutableListOf<NodeNameCollisionResult>().apply {
            add(currentCollision.value!!)
            addAll(pendingCollisions)
        }

        allCollisionsProcessed = true
        pendingCollisions.clear()

        return allPendingCollisions
    }

    /**
     * Proceeds with the upload of the current collision.
     *
     * @param context   Required context for start the service.
     * @param rename    True if should rename the file, false otherwise.
     */
    private fun singleUpload(rename: Boolean) {
        val choice =
            if (rename) NameCollisionChoice.RENAME
            else NameCollisionChoice.REPLACE_UPDATE_MERGE

        if (isFolderUploadContext) {
            continueWithNext(choice)
            return
        }

        val currentCollision = currentCollision.value ?: return

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
        list: MutableList<NodeNameCollisionResult>,
        rename: Boolean,
    ) {
        if (isFolderUploadContext) {
            list.forEach { item ->
                resolvedCollisions.add(item.apply {
                    choice = if (rename)
                        NameCollisionChoice.RENAME
                    else
                        NameCollisionChoice.REPLACE_UPDATE_MERGE
                })
            }

            if (pendingCollisions.isEmpty()) {
                collisionsResolution.value = arrayListOf<NodeNameCollisionResult>().apply {
                    addAll(resolvedCollisions)
                }
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
                pathsAndNames,
                NodeId(parentId)
            )
        }
    }

    /**
     * Proceeds with the movement of the current collision.
     *
     * @param rename    True if should rename the node, false otherwise.
     */
    private fun singleMovement(rename: Boolean) {
        val choice =
            if (rename) NameCollisionChoice.RENAME
            else NameCollisionChoice.REPLACE_UPDATE_MERGE

        viewModelScope.launch {
            runCatching {
                moveCollidedNodeUseCase(
                    currentCollision.value?.nameCollision as? NodeNameCollision
                        ?: throw IllegalArgumentException("Invalid collision"),
                    rename
                )
            }.onSuccess {
                setMovementResult(it, currentCollision.value?.nameCollision?.parentHandle ?: -1)
                continueWithNext(choice)
            }.onFailure {
                throwable.value = it
                Timber.w(it)
            }
        }
    }

    /**
     * Proceeds with the movement of the collisions list.
     *
     * @param list      List of collisions to move.
     * @param rename    True if should rename the nodes, false otherwise.
     */
    private fun move(list: List<NodeNameCollisionResult>, rename: Boolean) {
        if (list.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                moveCollidedNodesUseCase(
                    list.map { it.nameCollision }.filterIsInstance<NodeNameCollision>(),
                    rename
                )
            }.onSuccess {
                setMovementResult(it, list.firstOrNull()?.nameCollision?.parentHandle ?: -1)
            }.onFailure {
                throwable.value = it
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
        actionResult.value = NameCollisionActionResult(
            message = moveRequestMessageMapper(movementResult),
            shouldFinish = pendingCollisions.isEmpty()
        )
    }

    /**
     * Proceeds with the copy of the current collision.
     *
     * @param rename    True if should rename the node, false otherwise.
     */
    fun singleCopy(nameCollision: NodeNameCollision, rename: Boolean) {
        val choice =
            if (rename) NameCollisionChoice.RENAME
            else NameCollisionChoice.REPLACE_UPDATE_MERGE

        viewModelScope.launch {
            runCatching {
                copyCollidedNodeUseCase(
                    nameCollision = nameCollision,
                    rename = rename
                )
            }.onSuccess {
                setCopyResult(
                    copyResult = it.toCopyRequestResult(),
                    copyToHandle = nameCollision.parentHandle
                )
                continueWithNext(choice)
            }.onFailure {
                throwable.value = it
                Timber.w(it)
            }
        }
    }

    /**
     * Proceeds with the movement of the collisions list.
     *
     * @param list      List of collisions to copy.
     * @param rename    True if should rename the nodes, false otherwise.
     */
    fun copy(list: List<NodeNameCollision>, rename: Boolean) {
        viewModelScope.launch {
            runCatching {
                copyCollidedNodesUseCase(
                    nameCollisions = list,
                    rename = rename
                )
            }.onSuccess {
                setCopyResult(
                    copyResult = it.toCopyRequestResult(),
                    copyToHandle = list.firstOrNull()?.parentHandle ?: -1
                )
            }.onFailure {
                throwable.value = it
                Timber.w(it)
            }
        }
    }

    /**
     * Sets the copy result.
     *
     * @param copyResult    [CopyRequestResult] containing all the required info about the copy.
     */
    private fun setCopyResult(copyResult: CopyRequestResult, copyToHandle: Long) {
        if (copyToHandle != -1L)
            setCopyLatestPath(copyToHandle)
        actionResult.value = NameCollisionActionResult(
            message = copyRequestMessageMapper(copyResult),
            shouldFinish = pendingCollisions.isEmpty()
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

    internal fun uploadFiles(
        pathsAndNames: Map<String, String?>,
        destinationId: NodeId,
        choice: NameCollisionChoice? = null,
    ) {
        _uiState.update { state ->
            state.copy(
                uploadEvent = triggered(
                    TransferTriggerEvent.StartUpload.CollidedFiles(
                        pathsAndNames = pathsAndNames,
                        destinationId = destinationId,
                        collisionChoice = choice,
                    )
                )
            )
        }
    }

    /**
     * Consumes the upload event.
     */
    fun consumeUploadEvent() {
        (uiState.value.uploadEvent as StateEventWithContentTriggered).content.collisionChoice?.let { choice ->
            continueWithNext(choice)
        }
        _uiState.update { state -> state.copy(uploadEvent = consumed()) }
    }

    /**
     * Checks if all the pending collisions have been processed.
     */
    fun shouldFinish() = pendingCollisions.isEmpty()

    /**
     * Gets the current collision.
     */
    fun updateCurrentCollision(): LiveData<NodeNameCollisionResult?> = currentCollision

    /**
     * Gets the file versioning info.
     */
    fun getFileVersioningInfo(): LiveData<Triple<Boolean, NameCollisionType, Boolean>> =
        fileVersioningInfo

    /**
     * Gets the action result.
     */
    fun onActionResult(): LiveData<NameCollisionActionResult> = actionResult

    /**
     * Gets the collisions resolution.
     */
    fun getCollisionsResolution(): LiveData<ArrayList<NodeNameCollisionResult>> =
        collisionsResolution

    /**
     * Gets the throwable.
     */
    fun onExceptionThrown(): LiveData<Throwable> = throwable


    override fun onCleared() {
        super.onCleared()
        composite.clear()
    }
}