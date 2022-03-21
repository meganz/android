package mega.privacy.android.app.namecollision

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.myAccount.usecase.GetFileVersionsOptionUseCase
import mega.privacy.android.app.namecollision.data.NameCollisionActionResult
import mega.privacy.android.app.namecollision.data.NameCollisionResult
import mega.privacy.android.app.namecollision.usecase.GetNameCollisionResultUseCase
import mega.privacy.android.app.usecase.CopyNodeUseCase
import mega.privacy.android.app.usecase.MoveNodeUseCase
import mega.privacy.android.app.usecase.UploadNodeUseCase
import mega.privacy.android.app.usecase.data.CopyRequestResult
import mega.privacy.android.app.usecase.data.MoveRequestResult
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.StringResourcesUtils
import javax.inject.Inject

/**
 * ViewModel which manages data of [NameCollisionActivity]
 *
 * @property getFileVersionsOptionUseCase   Required for checking file versioning.
 * @property getNameCollisionResultUseCase  Required for getting all the needed info for present a collision.
 * @property uploadNodeUseCase              Required for uploading files.
 * @property moveNodeUseCase                Required for moving nodes.
 * @property copyNodeUseCase                Required for copying nodes.
 */
@HiltViewModel
class NameCollisionViewModel @Inject constructor(
    private val getFileVersionsOptionUseCase: GetFileVersionsOptionUseCase,
    private val getNameCollisionResultUseCase: GetNameCollisionResultUseCase,
    private val uploadNodeUseCase: UploadNodeUseCase,
    private val moveNodeUseCase: MoveNodeUseCase,
    private val copyNodeUseCase: CopyNodeUseCase
) : BaseRxViewModel() {

    /**
     * Enum class for defining the type of a collision.
     */
    enum class NameCollisionType { UPLOAD, COPY, MOVEMENT }

    /**
     * Enum class for defining the type of resolution for a collision.
     */
    enum class NameCollisionChoice { REPLACE_UPDATE_MERGE, CANCEL, RENAME }

    private val currentCollision: MutableLiveData<NameCollisionResult?> = MutableLiveData()
    private val fileVersioningInfo: MutableLiveData<Triple<Boolean, NameCollisionType, Boolean>> =
        MutableLiveData()
    private val actionResult: MutableLiveData<Event<NameCollisionActionResult>> = MutableLiveData()
    private val collisionsResolution: MutableLiveData<ArrayList<NameCollisionResult>> =
        MutableLiveData()

    fun getCurrentCollision(): LiveData<NameCollisionResult?> = currentCollision
    fun getFileVersioningInfo(): LiveData<Triple<Boolean, NameCollisionType, Boolean>> =
        fileVersioningInfo

    fun onActionResult(): LiveData<Event<NameCollisionActionResult>> = actionResult
    fun getCollisionsResolution(): LiveData<ArrayList<NameCollisionResult>> = collisionsResolution

    private val resolvedCollisions = mutableListOf<NameCollisionResult>()
    var isFolderUploadContext = false
    private val pendingCollisions: MutableList<NameCollisionResult> = mutableListOf()
    var pendingFileCollisions = 0
    var pendingFolderCollisions = 0
    private var allCollisionsProcessed = false

    init {
        getFileVersionsOption()
    }

    /**
     * Sets the initial data for resolving a single name collision.
     *
     * @param collision [NameCollision] to resolve.
     */
    fun setSingleData(collision: NameCollision) {
        getCurrentCollision(collision)
    }

    /**
     * Gets the complete collision data.
     *
     * @param collision [NameCollision] to resolve.
     */
    private fun getCurrentCollision(collision: NameCollision) {
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
                    logError("Error getting collisionResult", error)
                    currentCollision.value = null
                },
                onComplete = { logDebug("Get current name collision finished") }
            )
            .addTo(composite)
    }

    /**
     * Sets the initial data for resolving a list of name collisions.
     * Reorders the list to show files first, then folders. Then gets the current collision.
     *
     * @param collisions    ArrayList of [NameCollision] to resolve.
     */
    fun setData(collisions: ArrayList<NameCollision>) {
        getNameCollisionResultUseCase.reorder(collisions)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { result ->
                    val reorderedCollisions = result.first
                    pendingFileCollisions = result.second
                    pendingFolderCollisions = result.third
                    getCurrentCollision(reorderedCollisions[0])
                    reorderedCollisions.removeAt(0)

                    if (reorderedCollisions.isNotEmpty()) {
                        getPendingCollisions(reorderedCollisions)
                    }
                },
                onError = { error ->
                    logError("No pending collisions", error)
                    currentCollision.value = null
                }
            )
            .addTo(composite)
    }

    /**
     * Gets the list with complete data of pending collisions.
     *
     * @param collisions    MutableList of [NameCollision] to resolve.
     */
    private fun getPendingCollisions(collisions: MutableList<NameCollision>) {
        getNameCollisionResultUseCase.get(collisions)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { error -> logError("No pending collisions", error) },
                onNext = { collisionsResult ->
                    pendingCollisions.clear()
                    pendingCollisions.addAll(collisionsResult)
                },
                onComplete = { logDebug("Get complete name collisions finished") }
            )
            .addTo(composite)
    }

    /**
     * Checks if file versioning is enabled or not depending on if the value has been already set or not.
     */
    private fun getFileVersionsOption() {
        if (MegaApplication.isDisableFileVersions() == INVALID_VALUE) {
            getFileVersionsOptionUseCase.get()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = { logDebug("File versioning: ${MegaApplication.isDisableFileVersions()}") },
                    onError = { error -> LogUtil.logWarning(error.message) })
                .addTo(composite)
        }
    }

    /**
     * Updates file versioning info.
     */
    fun updateFileVersioningInfo() {
        if (currentCollision.value != null) {
            fileVersioningInfo.value =
                Triple(
                    isFileVersioningEnabled(),
                    getCollisionType(),
                    currentCollision.value!!.nameCollision.isFile
                )
        }
    }

    /**
     * Gets the [NameCollisionType] of the current collision.
     *
     * @return The type of the current collision.
     */
    private fun getCollisionType(): NameCollisionType =
        when (currentCollision.value?.nameCollision) {
            is NameCollision.Copy -> NameCollisionType.COPY
            is NameCollision.Movement -> NameCollisionType.MOVEMENT
            else -> NameCollisionType.UPLOAD
        }

    /**
     * Checks if is file versioning enabled.
     *
     * @return True if file versioning is enabled, false otherwise.
     */
    private fun isFileVersioningEnabled(): Boolean =
        when (MegaApplication.isDisableFileVersions()) {
            0 -> true
            else -> false
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
                while (pendingCollisions.isNotEmpty()) {
                    continueWithNext(NameCollisionChoice.CANCEL)
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
            resolvedCollisions.add(currentCollision.value!!.apply { this.choice = choice })

            if (pendingCollisions.isEmpty()) {
                collisionsResolution.apply {
                    value = arrayListOf<NameCollisionResult>().apply { addAll(resolvedCollisions) }
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
        proceedWithAction(context = context, applyOnNext = applyOnNext, rename = true)
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
        rename: Boolean = false
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
            NameCollisionType.MOVEMENT -> {
                when {
                    applyOnNext && pendingFileCollisions > 0 && pendingFolderCollisions > 0 -> {
                        move(proceedWithAllFiles(choice), rename)
                    }
                    applyOnNext -> move(getAllPendingCollisions(), rename)
                    else -> singleMove(rename)
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

        uploadNodeUseCase.upload(context, currentCollision.value!!, rename)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    setUploadResult(1)
                    continueWithNext(choice)
                },
                onError = { error -> LogUtil.logWarning(error.message) })
            .addTo(composite)
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
        rename: Boolean
    ) {
        if (isFolderUploadContext) {
            return
        }

        uploadNodeUseCase.upload(context, list, rename)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { setUploadResult(list.size) },
                onError = { error -> LogUtil.logWarning(error.message) })
            .addTo(composite)
    }

    /**
     * Sets the upload result.
     *
     * @param quantity  Number of processed uploads.
     */
    private fun setUploadResult(quantity: Int) {
        actionResult.value = Event(
            NameCollisionActionResult(
                message = StringResourcesUtils.getQuantityString(
                    R.plurals.upload_began,
                    quantity,
                    quantity
                ),
                isForeignNode = false,
                shouldFinish = pendingCollisions.isEmpty()
            )
        )
    }

    /**
     * Proceeds with the movement of the current collision.
     *
     * @param rename    True if should rename the node, false otherwise.
     */
    private fun singleMove(rename: Boolean) {
        val choice =
            if (rename) NameCollisionChoice.RENAME
            else NameCollisionChoice.REPLACE_UPDATE_MERGE

        moveNodeUseCase.move(currentCollision.value!!, rename)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { result ->
                    setMovementResult(result)
                    continueWithNext(choice)
                },
                onError = { error -> LogUtil.logWarning(error.message) })
            .addTo(composite)
    }

    /**
     * Proceeds with the movement of the collisions list.
     *
     * @param list      List of collisions to move.
     * @param rename    True if should rename the nodes, false otherwise.
     */
    private fun move(list: MutableList<NameCollisionResult>, rename: Boolean) {
        moveNodeUseCase.move(list, rename)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { result -> setMovementResult(result) },
                onError = { error -> LogUtil.logWarning(error.message) })
            .addTo(composite)
    }

    /**
     * Sets the movement result.
     *
     * @param movementResult    [MoveRequestResult.GeneralMovement] containing all the required info
     *                          about the movement.
     */
    private fun setMovementResult(movementResult: MoveRequestResult.GeneralMovement) {
        actionResult.value =
            Event(
                NameCollisionActionResult(
                    message = movementResult.getResultText(),
                    isForeignNode = movementResult.isForeignNode,
                    shouldFinish = pendingCollisions.isEmpty()
                )
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

        copyNodeUseCase.copy(currentCollision.value!!, rename)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { result ->
                    setCopyResult(result)
                    continueWithNext(choice)
                },
                onError = { error -> LogUtil.logWarning(error.message) })
            .addTo(composite)
    }

    /**
     * Proceeds with the movement of the collisions list.
     *
     * @param list      List of collisions to copy.
     * @param rename    True if should rename the nodes, false otherwise.
     */
    private fun copy(list: MutableList<NameCollisionResult>, rename: Boolean) {
        copyNodeUseCase.copy(list, rename)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { result -> setCopyResult(result) },
                onError = { error -> LogUtil.logWarning(error.message) })
            .addTo(composite)
    }

    /**
     * Sets the copy result.
     *
     * @param copyResult    [CopyRequestResult] containing all the required info about the copy.
     */
    private fun setCopyResult(copyResult: CopyRequestResult) {
        actionResult.value =
            Event(
                NameCollisionActionResult(
                    message = copyResult.getResultText(),
                    isForeignNode = copyResult.isForeignNode,
                    shouldFinish = pendingCollisions.isEmpty()
                )
            )
    }
}