package mega.privacy.android.app.mediaplayer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.mediaplayer.model.SubtitleDisplayState
import mega.privacy.android.app.namecollision.data.NameCollision
import mega.privacy.android.app.namecollision.data.NameCollisionType
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase
import mega.privacy.android.app.presentation.extensions.getStateFlow
import mega.privacy.android.app.usecase.CopyNodeUseCase
import mega.privacy.android.app.usecase.MoveNodeUseCase
import mega.privacy.android.app.usecase.exception.MegaNodeException
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaNode
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
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
    private val moveNodeUseCase: MoveNodeUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    savedStateHandle: SavedStateHandle,
) : BaseRxViewModel() {

    private val collision = SingleLiveEvent<NameCollision>()
    private val throwable = SingleLiveEvent<Throwable>()
    private val snackbarMessage = SingleLiveEvent<String>()

    /**
     * The subtitle file info by add subtitles
     */
    var subtitleInfoByAddSubtitles: SubtitleFileInfo? = null
        private set

    private var currentMediaPlayerMediaId: String? = null

    private val _isAddSubtitle = MutableStateFlow(false)

    private val _state = MutableStateFlow(SubtitleDisplayState())

    /**
     * Subtitle display state
     */
    val state: StateFlow<SubtitleDisplayState> = _state

    fun getCollision(): LiveData<NameCollision> = collision
    fun onSnackbarMessage(): LiveData<String> = snackbarMessage
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

    private val _isLockUpdate = MutableStateFlow(false)

    /**
     * Lock status update
     */
    val isLockUpdate: StateFlow<Boolean> = _isLockUpdate

    /**
     * Update the lock status
     *
     * @param isLock true is screen is locked, otherwise is screen is not locked
     */
    fun updateLockStatus(isLock: Boolean) = _isLockUpdate.update { isLock }

    /**
     * Rename update
     *
     * @param node the renamed node
     */
    fun renameUpdate(node: MegaNode?) {
        _renameUpdate.value = node
    }

    private val subtitleDialogShowKey = "SUBTITLE_DIALOG_SHOW"
    private val subtitleShowKey = "SUBTITLE_SHOW"
    private val videoPlayerPausedForPlaylistKey = "VIDEO_PLAYER_PAUSED_FOR_PLAYLIST"
    private val currentSubtitleFileInfoKey = "CURRENT_SUBTITLE_FILE_INFO"

    private val _isSubtitleDialogShown = savedStateHandle.getStateFlow(
        viewModelScope,
        subtitleDialogShowKey,
        false
    )

    private val _isSubtitleShown = savedStateHandle.getStateFlow(
        viewModelScope,
        subtitleShowKey,
        false
    )

    private val _videoPlayerPausedForPlaylist = savedStateHandle.getStateFlow(
        viewModelScope,
        videoPlayerPausedForPlaylistKey,
        false
    )

    private val _currentSubtitleFileInfo: MutableStateFlow<SubtitleFileInfo?> =
        savedStateHandle.getStateFlow(
            viewModelScope,
            currentSubtitleFileInfoKey,
            null
        )

    init {
        viewModelScope.launch {
            combine(
                _isSubtitleShown,
                _isSubtitleDialogShown,
                _isAddSubtitle,
                _currentSubtitleFileInfo,
                _videoPlayerPausedForPlaylist,
                ::mapToSubtitleDisplayState
            ).collectLatest { newState ->
                _state.update {
                    newState
                }
            }
        }
    }

    private fun mapToSubtitleDisplayState(
        subtitleShown: Boolean,
        subtitleDialogShown: Boolean,
        isAddSubtitle: Boolean,
        subtitleFileInfo: SubtitleFileInfo?,
        videoPlayerPausedForPlaylistState: Boolean,
    ) = SubtitleDisplayState(
        isSubtitleShown = subtitleShown,
        isSubtitleDialogShown = subtitleDialogShown,
        isAddSubtitle = isAddSubtitle,
        subtitleFileInfo = subtitleFileInfo,
        videoPlayerPausedForPlaylistState = videoPlayerPausedForPlaylistState
    )


    /**
     * Update the subtitle file info by added subtitles
     *
     * @param subtitleFileInfo [SubtitleFileInfo]
     */
    fun updateSubtitleInfoByAddSubtitles(subtitleFileInfo: SubtitleFileInfo?) {
        subtitleInfoByAddSubtitles = subtitleFileInfo
        subtitleFileInfo?.let { info ->
            if (_currentSubtitleFileInfo.value?.id != info.id) {
                _currentSubtitleFileInfo.update { info }
                _isAddSubtitle.update { true }
            } else {
                _isAddSubtitle.update { false }
            }
        }
    }

    /**
     * Update the current media player media id
     *
     * @param mediaId media item id
     */
    fun updateCurrentMediaId(mediaId: String?) {
        if (currentMediaPlayerMediaId != mediaId) {
            if (currentMediaPlayerMediaId != null) {
                _isSubtitleShown.update { false }
                subtitleInfoByAddSubtitles = null
            }
            currentMediaPlayerMediaId = mediaId
        }
    }

    /**
     * Update whether video player paused for playlist
     *
     * @param isPaused true is paused, otherwise is false
     */
    fun updateVideoPlayerPausedForPlaylist(isPaused: Boolean) {
        _videoPlayerPausedForPlaylist.update { isPaused }
    }

    /**
     * Update current subtitle file info
     *
     * @param subtitleFileInfo [SubtitleFileInfo]
     */
    fun updateCurrentSubtitleFileInfo(subtitleFileInfo: SubtitleFileInfo) {
        if (subtitleFileInfo.id != _currentSubtitleFileInfo.value?.id) {
            _currentSubtitleFileInfo.update { subtitleFileInfo }
            currentMediaPlayerMediaId = subtitleFileInfo.id.toString()
            _isAddSubtitle.update { true }
        } else {
            _isAddSubtitle.update { false }
        }
    }

    /**
     * Update the subtitle shown state
     *
     * @param isShow true is that subtitle is shown, otherwise is false
     */
    fun showSubtitle(isShow: Boolean) {
        _isSubtitleShown.update { isShow }
        if (!isShow) {
            _isAddSubtitle.update { false }
        }
    }

    /**
     * Update state for subtitle dialog shown state
     *
     * @param isShow true is show dialog, otherwise is false
     */
    fun showSubtitleDialog(isShow: Boolean) {
        _isSubtitleDialogShown.update { isShow }
    }

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
                nodeHandle?.let {
                    copyNodeUseCase.copy(handle = it, parentHandle = newParentHandle)
                        .subscribeAndCompleteCopy()
                }
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
                    Timber.e(error, "Error not copied.")
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
            type = NameCollisionType.MOVE
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
                        Timber.e(error, "Error not moved.")
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
        completeAction: (() -> Unit),
    ) {
        if (node != null) {
            checkNameCollisionUseCase.check(
                node = node,
                parentHandle = newParentHandle,
                type = type
            ).subscribeAndShowCollisionResult(completeAction)
        } else {
            nodeHandle?.let {
                checkNameCollisionUseCase.check(
                    handle = it,
                    parentHandle = newParentHandle,
                    type = type
                ).subscribeAndShowCollisionResult(completeAction)
            }
        }
    }

    private fun Single<NameCollision>.subscribeAndShowCollisionResult(completeAction: (() -> Unit)) {
        observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { collisionResult -> collision.value = collisionResult },
                onError = { error ->
                    when (error) {
                        is MegaNodeException.ChildDoesNotExistsException -> completeAction.invoke()
                        else -> Timber.e(error)
                    }
                }
            )
            .addTo(composite)
    }

    /**
     * Capture the screenshot when video playing
     *
     * @param captureAreaView the view that capture area
     * @param rootFolderPath the root folder path of screenshots
     * @param captureView the view that will be captured
     * @param successCallback the callback after the screenshot is saved successfully
     *
     */
    @SuppressLint("SimpleDateFormat")
    fun screenshotWhenVideoPlaying(
        captureAreaView: View,
        rootFolderPath: String,
        captureView: View,
        successCallback: (bitmap: Bitmap) -> Unit,
    ) {
        File(rootFolderPath).apply {
            if (exists().not()) {
                mkdirs()
            }
        }
        val screenshotFileName =
            SimpleDateFormat(DATE_FORMAT_PATTERN).format(Date(System.currentTimeMillis()))
        val filePath =
            "$rootFolderPath$SCREENSHOT_NAME_PREFIX$screenshotFileName$SCREENSHOT_NAME_SUFFIX"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val screenshotBitmap = Bitmap.createBitmap(
                    captureView.width,
                    captureView.height,
                    Bitmap.Config.ARGB_8888
                )
                PixelCopy.request(
                    captureView as SurfaceView,
                    Rect(0, 0, captureAreaView.width, captureAreaView.height),
                    screenshotBitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            viewModelScope.launch {
                                saveBitmap(filePath, screenshotBitmap, successCallback)
                            }
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            }
        } catch (e: Exception) {
            Timber.e("Capture screenshot error: ${e.message}")
        }
    }

    private suspend fun saveBitmap(
        filePath: String,
        bitmap: Bitmap,
        successCallback: (bitmap: Bitmap) -> Unit,
    ) =
        withContext(ioDispatcher) {
            val screenshotFile = File(filePath)
            try {
                val outputStream = FileOutputStream(screenshotFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY_SCREENSHOT, outputStream)
                outputStream.flush()
                outputStream.close()
                successCallback(bitmap)
            } catch (e: Exception) {
                Timber.e("Bitmap is saved error: ${e.message}")
            }
        }

    /**
     * Format milliseconds to time string
     * @param milliseconds time value that unit is milliseconds
     * @return strings of time
     */
    fun formatMillisecondsToString(milliseconds: Long): String {
        val totalSeconds = (milliseconds / 1000).toInt()
        return formatSecondsToString(seconds = totalSeconds)
    }

    /**
     * Format seconds to time string
     * @param seconds time value that unit is seconds
     * @return strings of time
     */
    private fun formatSecondsToString(seconds: Int): String {
        val hour = TimeUnit.SECONDS.toHours(seconds.toLong())
        val minutes =
            TimeUnit.SECONDS.toMinutes(seconds.toLong()) - TimeUnit.HOURS.toMinutes(hour)
        val resultSeconds =
            seconds.toLong() - TimeUnit.MINUTES.toSeconds(
                TimeUnit.SECONDS.toMinutes(
                    seconds.toLong()
                )
            )

        return if (hour >= 1) {
            String.format("%2d:%02d:%02d", hour, minutes, resultSeconds)
        } else {
            String.format("%02d:%02d", minutes, resultSeconds)
        }
    }

    companion object {
        private const val QUALITY_SCREENSHOT = 100
        private const val DATE_FORMAT_PATTERN = "yyyyMMdd-HHmmss"
        private const val SCREENSHOT_NAME_PREFIX = "Screenshot_"
        private const val SCREENSHOT_NAME_SUFFIX = ".jpg"
    }
}
