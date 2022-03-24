package mega.privacy.android.app.mediaplayer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.usecase.CopyNodeUseCase
import mega.privacy.android.app.usecase.MoveNodeUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * ViewModel for main audio player UI logic.
 *
 * @property checkNameCollisionUseCase  Required for checking name collisions.
 * @property copyNodeUseCase            Required for copying nodes.
 * @property moveNodeUseCase            Required for moving nodes.
 */
@HiltViewModel
class MediaPlayerViewModel @Inject constructor(
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase,
    private val copyNodeUseCase: CopyNodeUseCase,
    private val moveNodeUseCase: MoveNodeUseCase
) : BaseRxViewModel() {

    private val collision = SingleLiveEvent<NameCollision>()
    private val throwable = SingleLiveEvent<Throwable>()
    private val snackbarMessage = SingleLiveEvent<String>()

    fun getCollision(): LiveData<NameCollision> = collision
    fun onSnackbarMessage(): LiveData<String> = snackbarMessage
    fun onExceptionThrown(): LiveData<Throwable> = throwable

    private val _itemToRemove = MutableLiveData<Long>()
    val itemToRemove: LiveData<Long> = _itemToRemove

    /**
     * Copies a node if there is no name collision.
     *
     * @param node              Node to copy.
     * @param nodeHandle        Node handle to copy.
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun copyNode(node: MegaNode? = null, nodeHandle: Long? = null, newParentHandle: Long) {
        checkNameCollision(
            node = node,
            nodeHandle = nodeHandle,
            newParentHandle = newParentHandle,
            type = NameCollisionType.COPY
        ) {
            if (node != null) {
                copyNodeUseCase.copy(node = node, parentHandle = newParentHandle)
                    .subscribeAndCompleteCopy()
            } else {
                copyNodeUseCase.copy(handle = nodeHandle!!, parentHandle = newParentHandle)
                    .subscribeAndCompleteCopy()
            }
        }
    }

    private fun Completable.subscribeAndCompleteCopy() {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    snackbarMessage.value =
                        StringResourcesUtils.getString(R.string.context_correctly_copied)
                },
                onError = { error ->
                    throwable.value = error
                    LogUtil.logError("Error not copied.", error)
                }
            )
            .addTo(composite)
    }

    /**
     * Moves a node if there is no name collision.
     *
     * @param nodeHandle        Node handle to move.
     * @param newParentHandle   Parent handle in which the node will be moved.
     */
    fun moveNode(nodeHandle: Long, newParentHandle: Long) {
        checkNameCollision(
            nodeHandle = nodeHandle,
            newParentHandle = newParentHandle,
            type = NameCollisionType.MOVEMENT
        ) {
            moveNodeUseCase.move(handle = nodeHandle, parentHandle = newParentHandle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onComplete = {
                        _itemToRemove.value = nodeHandle
                        snackbarMessage.value =
                            StringResourcesUtils.getString(R.string.context_correctly_moved)
                    },
                    onError = { error ->
                        throwable.value = error
                        LogUtil.logError("Error not moved.", error)
                    }
                )
                .addTo(composite)
        }
    }

    /**
     * Checks if there is a name collision before proceeding with the action.
     *
     * @param node              Node to check the name collision.
     * @param nodeHandle        Handle of the node to check the name collision.
     * @param newParentHandle   Handle of the parent folder in which the action will be performed.
     * @param type              [NameCollisionType]
     * @param completeAction    Action to complete after checking the name collision.
     */
    private fun checkNameCollision(
        node: MegaNode? = null,
        nodeHandle: Long? = null,
        newParentHandle: Long,
        type: NameCollisionType,
        completeAction: (() -> Unit)
    ) {
        if (node != null) {
            checkNameCollisionUseCase.check(
                node = node,
                parentHandle = newParentHandle,
                type = type
            ).subscribeAndShowCollisionResult(completeAction)
        } else {
            checkNameCollisionUseCase.check(
                handle = nodeHandle!!,
                parentHandle = newParentHandle,
                type = type
            ).subscribeAndShowCollisionResult(completeAction)
        }
    }

    private fun Single<NameCollision>.subscribeAndShowCollisionResult(completeAction: (() -> Unit)) {
        observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { collisionResult -> collision.value = collisionResult },
                onError = { error ->
                    when (error) {
                        is MegaNodeException.ChildDoesNotExistsException -> completeAction.invoke()
                        else -> LogUtil.logError(error.stackTraceToString())
                    }
                }
            )
            .addTo(composite)
    }
}
