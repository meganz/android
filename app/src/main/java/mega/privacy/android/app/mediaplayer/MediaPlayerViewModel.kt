package mega.privacy.android.app.mediaplayer

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.domain.usecase.CheckNameCollision
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.mediaplayer.model.MediaPlayerMenuClickedEvent
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.usecase.LegacyCopyNodeUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.node.CopyNodeUseCase
import mega.privacy.android.domain.usecase.node.MoveNodeUseCase
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for business logic regarding the toolbar.
 *
 * @property checkNameCollision Required for checking name collisions.
 * @property copyNodeUseCase Required for copying nodes.
 * @property moveNodeUseCase Required for moving nodes.
 */
@HiltViewModel
class MediaPlayerViewModel @Inject constructor(
    private val checkNameCollision: CheckNameCollision,
    private val copyNodeUseCase: CopyNodeUseCase,
    private val moveNodeUseCase: MoveNodeUseCase,
    private val getNodeByHandle: GetNodeByHandle,
    private val legacyCopyNodeUseCase: LegacyCopyNodeUseCase,
    private val checkNameCollisionUseCase: CheckNameCollisionUseCase,
) : BaseRxViewModel() {

    private val collision = SingleLiveEvent<NameCollision>()
    private val throwable = SingleLiveEvent<Throwable>()
    private val snackbarMessage = SingleLiveEvent<Int>()

    /**
     * The flow for clicked event
     */
    val menuClickEventFlow = MutableSharedFlow<MediaPlayerMenuClickedEvent>()

    /**
     * Update clicked event flow
     *
     * @param menuId menu view id
     * @param adapterType the type of adapter
     * @param playingHandle the current playing item handle
     * @param launchIntent the launched Intent
     */
    fun updateMenuClickEventFlow(
        menuId: Int,
        adapterType: Int,
        playingHandle: Long,
        launchIntent: Intent,
    ) {
        viewModelScope.launch {
            menuClickEventFlow.emit(
                MediaPlayerMenuClickedEvent(
                    menuId = menuId,
                    adapterType = adapterType,
                    playingHandle = playingHandle,
                    launchIntent = launchIntent
                )
            )
        }
    }

    fun getCollision(): LiveData<NameCollision> = collision
    fun onSnackbarMessage(): LiveData<Int> = snackbarMessage
    fun onExceptionThrown(): LiveData<Throwable> = throwable

    private val _itemToRemove = MutableLiveData<Long>()

    /**
     * Removed item update
     */
    val itemToRemove: LiveData<Long> = _itemToRemove

    private val _renameUpdate = MutableLiveData<MegaNode?>()

    /**
     * Rename update
     */
    val renameUpdate: LiveData<MegaNode?> = _renameUpdate

    /**
     * Rename update
     *
     * @param node the renamed node
     */
    fun renameUpdate(node: MegaNode?) {
        _renameUpdate.value = node
    }

    /**
     * Copies a node if there is no name collision.
     *
     * @param node              Node to copy.
     * @param nodeHandle        Node handle to copy.
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun copyNode(
        node: MegaNode? = null,
        nodeHandle: Long? = null,
        newParentHandle: Long,
    ) {
        viewModelScope.launch {
            val nodeId = node?.handle ?: nodeHandle ?: return@launch
            checkForNameCollision(
                nodeHandle = nodeId,
                newParentHandle = newParentHandle,
                type = NameCollisionType.COPY
            ) {
                runCatching {
                    copyNodeUseCase(
                        nodeToCopy = NodeId(nodeId),
                        newNodeParent = NodeId(newParentHandle),
                        newNodeName = null,
                    )
                }.onSuccess {
                    snackbarMessage.value = R.string.context_correctly_copied
                }.onFailure {
                    throwable.value = it
                    Timber.e("Error not copied $it")
                }
            }
        }
    }


    /**
     * Imports a node if there is no name collision.
     *
     * @param node              Node handle to copy.
     * @param newParentHandle   Parent handle in which the node will be copied.
     */
    fun importNode(node: MegaNode, newParentHandle: Long) =
        viewModelScope.launch {
            val parentNode = getNodeByHandle(newParentHandle)
            checkNameCollisionUseCase.check(
                node = node,
                parentNode = parentNode,
                type = NameCollisionType.COPY,
            ).observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { collisionResult -> collision.value = collisionResult },
                    onError = { error ->
                        when (error) {
                            is MegaNodeException.ChildDoesNotExistsException -> {
                                legacyCopyNodeUseCase.copy(
                                    node = node,
                                    parentHandle = newParentHandle
                                ).subscribeAndComplete(
                                    completeAction = {
                                        snackbarMessage.value =
                                            R.string.context_correctly_copied
                                    },
                                    errorAction = { copyError ->
                                        throwable.value = copyError
                                    })
                            }

                            else -> Timber.e(error)
                        }
                    }
                )
                .addTo(composite)
        }


    private fun Completable.subscribeAndComplete(
        addToComposite: Boolean = false,
        completeAction: (() -> Unit)? = null,
        errorAction: ((Throwable) -> Unit)? = null,
    ) {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    completeAction?.invoke()
                },
                onError = { error ->
                    errorAction?.invoke(error)
                    Timber.e(error)
                }
            ).also {
                if (addToComposite) it.addTo(composite)
            }
    }

    /**
     * Moves a node if there is no name collision.
     *
     * @param nodeHandle        Node handle to move.
     * @param newParentHandle   Parent handle in which the node will be moved.
     */
    fun moveNode(nodeHandle: Long, newParentHandle: Long) {
        viewModelScope.launch {
            checkForNameCollision(
                nodeHandle = nodeHandle,
                newParentHandle = newParentHandle,
                type = NameCollisionType.MOVE,
            ) {
                runCatching {
                    moveNodeUseCase(
                        nodeToMove = NodeId(nodeHandle),
                        newNodeParent = NodeId(newParentHandle)
                    )
                }.onSuccess {
                    _itemToRemove.value = nodeHandle
                    snackbarMessage.value = R.string.context_correctly_moved
                }.onFailure {
                    throwable.value = it
                    Timber.e("Error not moved $it")
                }
            }
        }
    }

    /**
     * Checks if there is a name collision before proceeding with the action.
     *
     * @param nodeHandle        Handle of the node to check the name collision.
     * @param newParentHandle   Handle of the parent folder in which the action will be performed.
     * @param completeAction    Action to complete after checking the name collision.
     */
    private suspend fun checkForNameCollision(
        nodeHandle: Long,
        newParentHandle: Long,
        type: NameCollisionType,
        completeAction: suspend (() -> Unit),
    ) {
        runCatching {
            checkNameCollision(
                nodeHandle = NodeId(nodeHandle),
                parentHandle = NodeId(newParentHandle),
                type = type,
            )
        }.onSuccess {
            collision.value = it
        }.onFailure {
            when (it) {
                is MegaNodeException.ChildDoesNotExistsException -> completeAction.invoke()
                is MegaNodeException.ParentDoesNotExistException -> {
                    snackbarMessage.value = R.string.general_error
                }

                else -> Timber.e(it)
            }
        }
    }
}
