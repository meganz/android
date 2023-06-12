package mega.privacy.android.app.namecollision

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionActionResult
import mega.privacy.android.app.namecollision.data.NameCollisionChoice
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.GetNameCollisionResultUseCase
import mega.privacy.android.app.presentation.copynode.CopyRequestResult
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.app.usecase.LegacyCopyNodeUseCase
import mega.privacy.android.app.usecase.GetNodeUseCase
import mega.privacy.android.app.usecase.LegacyMoveNodeUseCase
import mega.privacy.android.app.usecase.UploadUseCase
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.file.GetFileVersionsOption
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel which manages data of [NameCollisionActivity]
 *
 * @property getNameCollisionResultUseCase  Required for getting all the needed info for present a collision.
 * @property uploadUseCase                  Required for uploading files.
 * @property legacyMoveNodeUseCase          Required for moving nodes.
 * @property legacyCopyNodeUseCase          Required for copying nodes.
 * @property getNodeUseCase                 Required for getting node from handle
 */
@HiltViewModel
class NameCollisionViewModel @Inject constructor(
    private val getFileVersionsOption: GetFileVersionsOption,
    private val getNameCollisionResultUseCase: GetNameCollisionResultUseCase,
    private val uploadUseCase: UploadUseCase,
    private val legacyMoveNodeUseCase: LegacyMoveNodeUseCase,
    private val legacyCopyNodeUseCase: LegacyCopyNodeUseCase,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val getNodeUseCase: GetNodeUseCase,
    private val setCopyLatestTargetPathUseCase: SetCopyLatestTargetPathUseCase,
    private val setMoveLatestTargetPathUseCase: SetMoveLatestTargetPathUseCase,
    private val copyRequestMessageMapper: CopyRequestMessageMapper,
    private val moveRequestMessageMapper: MoveRequestMessageMapper,
) : BaseRxViewModel() {

    private val currentCollision: MutableLiveData<NameCollisionResult?> = MutableLiveData()
    private val fileVersioningInfo: MutableLiveData<Triple<Boolean, NameCollisionType, Boolean>> =
        MutableLiveData()
    private val actionResult: MutableLiveData<NameCollisionActionResult> = MutableLiveData()
    private val collisionsResolution: MutableLiveData<ArrayList<NameCollisionResult>> =
        MutableLiveData()
    private val throwable = SingleLiveEvent<Throwable>()

    fun getCurrentCollision(): LiveData<NameCollisionResult?> = currentCollision
    fun getFileVersioningInfo(): LiveData<Triple<Boolean, NameCollisionType, Boolean>> =
        fileVersioningInfo

    fun onActionResult(): LiveData<NameCollisionActionResult> = actionResult
    fun getCollisionsResolution(): LiveData<ArrayList<NameCollisionResult>> = collisionsResolution
    fun onExceptionThrown(): LiveData<Throwable> = throwable

    private val renameNames = mutableListOf<String>()
    private val resolvedCollisions = mutableListOf<NameCollisionResult>()
    var isFolderUploadContext = false
    private val pendingCollisions: MutableList<NameCollisionResult> = mutableListOf()
    var pendingFileCollisions = 0
    var pendingFolderCollisions = 0
    private var allCollisionsProcessed = false
    var isCopyToOrigin = false

    init {
        viewModelScope.launch {
            monitorUserUpdates()
                .filter { it == UserChanges.DisableVersions }
                .map {
                    getFileVersionsOption(true)
                }
                .onStart {
                    getFileVersionsOption(true)
                }
                .catch { Timber.e(it) }
                .collect {
                    updateFileVersioningInfo()
                }
        }
    }

    private fun setCopyToOrigin(collision: NameCollision.Copy) {
        val node = getNodeUseCase.get(collision.nodeHandle).blockingGetOrNull()
        if (node?.parentHandle == collision.parentHandle) {
            isCopyToOrigin = true
        }
    }

    /**
     * Sets the initial data for resolving a single name collision.
     *
     * @param collision [NameCollision] to resolve.
     */
    fun setSingleData(collision: NameCollision, context: Context) {
        if (collision is NameCollision.Copy)
            setCopyToOrigin(collision)
        getCurrentCollision(collision, context, true)
    }

    /**
     * Gets the complete collision data.
     *
     * @param collision [NameCollision] to resolve.
     * @param context   Required Context for uploads.
     * @param rename    Whether to call rename() or not
     */
    private fun getCurrentCollision(collision: NameCollision, context: Context, rename: Boolean) {
        var firstUpdate = true

        getNameCollisionResultUseCase.get(collision)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { collisionResult ->
                    currentCollision.value = collisionResult

                    if (firstUpdate) {
                        firstUpdate = false
                        updateFileVersioningInfo()
                    }
                },
                onError = { error ->
                    Timber.e(error, "Error getting collisionResult")
                    currentCollision.value = null
                },
                onComplete = {
                    Timber.d("Get current name collision finished")
                    currentCollision.value?.let {
                        if (isCopyToOrigin && rename)
                            rename(context, true)
                    }
                }
            )
            .addTo(composite)
    }

    /**
     * Sets the initial data for resolving a list of name collisions.
     * Reorders the list to show files first, then folders. Then gets the current collision.
     *
     * @param collisions    ArrayList of [NameCollision] to resolve.
     * @param context       Required Context for uploads.
     */
    fun setData(collisions: ArrayList<NameCollision>, context: Context) {
        val firstCollision = collisions[0]
        if (firstCollision is NameCollision.Copy)
            setCopyToOrigin(firstCollision)
        getNameCollisionResultUseCase.reorder(collisions)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { result ->
                    val reorderedCollisions = result.first
                    pendingFileCollisions = result.second
                    pendingFolderCollisions = result.third
                    getCurrentCollision(reorderedCollisions[0], context, false)
                    reorderedCollisions.removeAt(0)

                    if (reorderedCollisions.isNotEmpty()) {
                        getPendingCollisions(reorderedCollisions, context)
                    }
                },
                onError = { error ->
                    Timber.e(error, "No pending collisions")
                    currentCollision.value = null
                }
            )
            .addTo(composite)
    }

    /**
     * Gets the list with complete data of pending collisions.
     *
     * @param collisions    MutableList of [NameCollision] to resolve.
     * @param context       Required Context for uploads.
     */
    private fun getPendingCollisions(collisions: MutableList<NameCollision>, context: Context) {
        getNameCollisionResultUseCase.get(collisions)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error -> Timber.e(error, "No pending collisions") },
                onNext = { collisionsResult ->
                    pendingCollisions.clear()
                    pendingCollisions.addAll(collisionsResult)
                },
                onComplete = {
                    Timber.d("Get complete name collisions finished")
                    if (isCopyToOrigin)
                        rename(context, true)
                }
            )
            .addTo(composite)
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
        when (currentCollision.value?.nameCollision) {
            is NameCollision.Copy, is NameCollision.Import -> NameCollisionType.COPY
            is NameCollision.Movement -> NameCollisionType.MOVE
            else -> NameCollisionType.UPLOAD
        }

    /**
     * After having the option "Apply on the next conflicts" checked, manages data to apply
     * the same user's choice for all the pending file collisions.
     *
     * @param choice    Resolution type as [NameCollisionChoice].
     * @return A MutableList with all the pending file collisions.
     */
    private fun proceedWithAllFiles(choice: NameCollisionChoice): MutableList<NameCollisionResult> {
        val fileCollisions = mutableListOf<NameCollisionResult>().apply {
            add(currentCollision.value!!)
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

                collisionsResolution.value =
                    arrayListOf<NameCollisionResult>().apply { addAll(resolvedCollisions) }

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
                collisionsResolution.value =
                    arrayListOf<NameCollisionResult>().apply { addAll(resolvedCollisions) }

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
        renameNames.add(currentCollision.value?.renameName!!)
        getNameCollisionResultUseCase.updateRenameNames(pendingCollisions, renameNames, applyOnNext)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    proceedWithAction(context = context, applyOnNext = applyOnNext, rename = true)
                },
                onError = Timber::w
            ).addTo(composite)
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
        context: Context? = null,
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
                        upload(context!!, proceedWithAllFiles(choice), rename)
                    }

                    applyOnNext -> upload(context!!, getAllPendingCollisions(), rename)
                    else -> singleUpload(context!!, rename)
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
                        copy(proceedWithAllFiles(choice), rename)
                    }

                    applyOnNext -> copy(getAllPendingCollisions(), rename)
                    else -> singleCopy(rename)
                }
            }
        }
    }

    /**
     * Processes all the pending collisions, including the current one,
     * and returns a list with all of them.
     */
    private fun getAllPendingCollisions(): MutableList<NameCollisionResult> {
        val allPendingCollisions = mutableListOf<NameCollisionResult>().apply {
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
    private fun singleUpload(context: Context, rename: Boolean) {
        val choice =
            if (rename) NameCollisionChoice.RENAME
            else NameCollisionChoice.REPLACE_UPDATE_MERGE

        if (isFolderUploadContext) {
            continueWithNext(choice)
            return
        }

        val currentCollision = currentCollision.value ?: return
        uploadUseCase.upload(context, currentCollision, rename)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    setUploadResult(1, context)
                    continueWithNext(choice)
                },
                onError = Timber::e
            ).addTo(composite)
    }

    /**
     * Proceeds with the upload of the collisions list.
     *
     * @param context   Required context for start the service.
     * @param list      List of collisions to upload.
     * @param rename    True if should rename the file, false otherwise.
     */
    private fun upload(
        context: Context,
        list: MutableList<NameCollisionResult>,
        rename: Boolean,
    ) {
        if (isFolderUploadContext) {
            list.forEach { item ->
                resolvedCollisions.add(item.apply {
                    choice =
                        if (rename) NameCollisionChoice.RENAME
                        else NameCollisionChoice.REPLACE_UPDATE_MERGE
                })

            }

            if (pendingCollisions.isEmpty()) {
                collisionsResolution.value =
                    arrayListOf<NameCollisionResult>().apply { addAll(resolvedCollisions) }
            }
            return
        }

        uploadUseCase.upload(context, list, rename)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { setUploadResult(list.size, context) },
                onError = Timber::e
            ).addTo(composite)
    }

    /**
     * Sets the upload result.
     *
     * @param quantity  Number of processed uploads.
     */
    private fun setUploadResult(quantity: Int, context: Context) {
        actionResult.value = NameCollisionActionResult(
            message = context.resources.getQuantityString(
                R.plurals.upload_began,
                quantity,
                quantity
            ),
            isForeignNode = false,
            shouldFinish = pendingCollisions.isEmpty()
        )
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

        legacyMoveNodeUseCase.move(currentCollision.value ?: return, rename)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { result ->
                    setMovementResult(
                        result,
                        currentCollision.value?.nameCollision?.parentHandle ?: -1
                    )
                    continueWithNext(choice)
                },
                onError = { error ->
                    throwable.value = error
                    Timber.w(error)
                })
            .addTo(composite)
    }

    /**
     * Proceeds with the movement of the collisions list.
     *
     * @param list      List of collisions to move.
     * @param rename    True if should rename the nodes, false otherwise.
     */
    private fun move(list: MutableList<NameCollisionResult>, rename: Boolean) {
        legacyMoveNodeUseCase.move(list, rename)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { result ->
                    setMovementResult(
                        result,
                        list[0].nameCollision.parentHandle ?: -1
                    )
                },
                onError = { error ->
                    throwable.value = error
                    Timber.w(error)
                })
            .addTo(composite)
    }

    /**
     * Sets the movement result.
     *
     * @param movementResult    [MoveRequestResult.GeneralMovement] containing all the required info
     *                          about the movement.
     */
    private fun setMovementResult(
        movementResult: MoveRequestResult.GeneralMovement,
        moveToHandle: Long
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
    private fun singleCopy(rename: Boolean) {
        val choice =
            if (rename) NameCollisionChoice.RENAME
            else NameCollisionChoice.REPLACE_UPDATE_MERGE

        legacyCopyNodeUseCase.copy(currentCollision.value ?: return, rename)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { result ->
                    setCopyResult(
                        result,
                        currentCollision.value?.nameCollision?.parentHandle ?: -1
                    )
                    continueWithNext(choice)
                },
                onError = { error ->
                    throwable.value = error
                    Timber.w(error)
                })
            .addTo(composite)
    }

    /**
     * Proceeds with the movement of the collisions list.
     *
     * @param list      List of collisions to copy.
     * @param rename    True if should rename the nodes, false otherwise.
     */
    private fun copy(list: MutableList<NameCollisionResult>, rename: Boolean) {
        legacyCopyNodeUseCase.copy(list, rename)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { result ->
                    setCopyResult(
                        result,
                        list[0].nameCollision.parentHandle ?: -1
                    )
                },
                onError = { error ->
                    throwable.value = error
                    Timber.w(error)
                })
            .addTo(composite)
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
}