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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.mediaplayer.model.SubtitleDisplayState
import mega.privacy.android.app.presentation.extensions.getStateFlow
import mega.privacy.android.domain.entity.mediaplayer.RepeatToggleMode
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import mega.privacy.android.domain.entity.statistics.MediaPlayerStatisticsEvents
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.mediaplayer.MonitorVideoRepeatModeUseCase
import mega.privacy.android.domain.usecase.mediaplayer.SendStatisticsMediaPlayerUseCase
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * ViewModel for video player.
 *
 * @property ioDispatcher CoroutineDispatcher
 * @property sendStatisticsMediaPlayerUseCase SendStatisticsMediaPlayerUseCase
 */
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val sendStatisticsMediaPlayerUseCase: SendStatisticsMediaPlayerUseCase,
    monitorVideoRepeatModeUseCase: MonitorVideoRepeatModeUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
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

    /**
     * SelectState for updating the background color of add subtitle dialog options
     */
    var selectOptionState by mutableStateOf(SUBTITLE_SELECTED_STATE_OFF)
        private set

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

    internal val subtitleDialogShowKey = "SUBTITLE_DIALOG_SHOW"
    internal val subtitleShowKey = "SUBTITLE_SHOW"
    internal val videoPlayerPausedForPlaylistKey = "VIDEO_PLAYER_PAUSED_FOR_PLAYLIST"
    internal val currentSubtitleFileInfoKey = "CURRENT_SUBTITLE_FILE_INFO"

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

    private val _currentSubtitleFileInfo: MutableStateFlow<SubtitleFileInfo?> =
        savedStateHandle.getStateFlow(
            viewModelScope,
            currentSubtitleFileInfoKey,
            null
        )

    private var videoRepeatToggleMode = monitorVideoRepeatModeUseCase().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        RepeatToggleMode.REPEAT_NONE
    )


    init {
        viewModelScope.launch {
            combine(
                _isSubtitleShown,
                _isSubtitleDialogShown,
                _isAddSubtitle,
                _currentSubtitleFileInfo,
                ::mapToSubtitleDisplayState
            ).collectLatest { newState ->
                _state.update {
                    newState
                }
            }
        }
    }

    /**
     * Get video repeat mode
     *
     * @return repeat mode
     */
    fun getVideoRepeatMode() = videoRepeatToggleMode.value

    private fun mapToSubtitleDisplayState(
        subtitleShown: Boolean,
        subtitleDialogShown: Boolean,
        isAddSubtitle: Boolean,
        subtitleFileInfo: SubtitleFileInfo?,
    ) = SubtitleDisplayState(
        isSubtitleShown = subtitleShown,
        isSubtitleDialogShown = subtitleDialogShown,
        isAddSubtitle = isAddSubtitle,
        subtitleFileInfo = subtitleFileInfo,
    )

    /**
     * Update the subtitle file info by added subtitles
     *
     * @param subtitleFileInfo [SubtitleFileInfo]
     */
    private fun updateSubtitleInfoByAddSubtitles(subtitleFileInfo: SubtitleFileInfo?) {
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
     * Update isAddSubtitle state
     */
    fun updateAddSubtitleState() {
        _isAddSubtitle.update { state.value.isAddSubtitle && state.value.subtitleFileInfo == null }
    }

    /**
     * Update current subtitle file info
     *
     * @param subtitleFileInfo [SubtitleFileInfo]
     */
    private fun updateCurrentSubtitleFileInfo(subtitleFileInfo: SubtitleFileInfo) {
        if (subtitleFileInfo.id != _currentSubtitleFileInfo.value?.id) {
            _currentSubtitleFileInfo.update { subtitleFileInfo }
            currentMediaPlayerMediaId = subtitleFileInfo.id.toString()
            _isAddSubtitle.update { true }
        } else {
            _isAddSubtitle.update { false }
        }
    }

    /**
     * The function is for showing the add subtitle dialog
     */
    fun showAddSubtitleDialog() {
        _isSubtitleShown.update { true }
        _isAddSubtitle.update { false }
        _isSubtitleDialogShown.update { true }
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.SubtitleDialogShownEvent())
    }

    /**
     * The function is for the added subtitle option is clicked
     */
    fun onAddedSubtitleOptionClicked() {
        _isSubtitleShown.update { true }
        _isSubtitleDialogShown.update { false }
        selectOptionState = SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM
    }

    /**
     * The function is for the adding subtitle file
     *
     * @param info the added subtitle file info
     */
    fun onAddSubtitleFile(info: SubtitleFileInfo?) {
        info?.let {
            it.url?.let {
                updateSubtitleInfoByAddSubtitles(info)
                selectOptionState = SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM
            } ?: Timber.d("The subtitle file url is null")
            _isSubtitleShown.update { true }
        } ?: let {
            _isAddSubtitle.update { false }
            _isSubtitleShown.update {
                selectOptionState != SUBTITLE_SELECTED_STATE_OFF
            }
        }
        _isSubtitleDialogShown.update { false }
    }

    /**
     * The function is for the off item is clicked
     */
    fun onOffItemClicked() {
        // Only when the subtitle file has been loaded and shown, send hide subtitle event if off item is clicked
        if (_currentSubtitleFileInfo.value != null && selectOptionState != SUBTITLE_SELECTED_STATE_OFF) {
            sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.HideSubtitleEvent())
        }
        _isAddSubtitle.update { false }
        _isSubtitleShown.update { false }
        _isSubtitleDialogShown.update { false }
        selectOptionState = SUBTITLE_SELECTED_STATE_OFF
    }

    /**
     * The function is for the dialog dismiss request
     */
    fun onDismissRequest() {
        _isAddSubtitle.update { false }
        _isSubtitleShown.update {
            selectOptionState != SUBTITLE_SELECTED_STATE_OFF
        }
        _isSubtitleDialogShown.update { false }
    }

    /**
     * The function is for the auto matched item is clicked
     *
     * @param info matched subtitle file info
     */
    fun onAutoMatchItemClicked(info: SubtitleFileInfo) {
        info.url?.let {
            updateCurrentSubtitleFileInfo(info)
            _isSubtitleShown.update { true }
            updateSubtitleInfoByAddSubtitles(null)
            sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.AutoMatchSubtitleClickedEvent())
            selectOptionState = SUBTITLE_SELECTED_STATE_MATCHED_ITEM
        } ?: Timber.d("The subtitle file url is null")
        _isSubtitleDialogShown.update { false }
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
            "$rootFolderPath${SCREENSHOT_NAME_PREFIX}$screenshotFileName${SCREENSHOT_NAME_SUFFIX}"
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

    /**
     * Send OpenSelectSubtitlePageEvent
     */
    fun sendOpenSelectSubtitlePageEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.OpenSelectSubtitlePageEvent())

    /**
     * Send LoopButtonEnabledEvent
     */
    fun sendLoopButtonEnabledEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.LoopButtonEnabledEvent())

    /**
     * Send ScreenLockedEvent
     */
    fun sendScreenLockedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.ScreenLockedEvent())

    /**
     * Send ScreenUnlockedEvent
     */
    fun sendScreenUnlockedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.ScreenUnlockedEvent())

    /**
     * Send SnapshotButtonClickedEvent
     */
    fun sendSnapshotButtonClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.SnapshotButtonClickedEvent())

    /**
     * Send InfoButtonClickedEvent
     */
    fun sendInfoButtonClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.InfoButtonClickedEvent())

    /**
     * Send SaveToDeviceButtonClickedEvent
     */
    fun sendSaveToDeviceButtonClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.SaveToDeviceButtonClickedEvent())

    /**
     * Send SendToChatButtonClickedEvent
     */
    fun sendSendToChatButtonClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.SendToChatButtonClickedEvent())

    /**
     * Send ShareButtonClickedEvent
     */
    fun sendShareButtonClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.ShareButtonClickedEvent())

    /**
     * Send GetLinkButtonClickedEvent
     */
    fun sendGetLinkButtonClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.GetLinkButtonClickedEvent())

    /**
     * Send RemoveLinkButtonClickedEvent
     */
    fun sendRemoveLinkButtonClickedEvent() =
        sendMediaPlayerStatisticsEvent(MediaPlayerStatisticsEvents.RemoveLinkButtonClickedEvent())

    /**
     * Send MediaPlayerStatisticsEvent
     *
     * @param event MediaPlayerStatisticsEvents
     */
    private fun sendMediaPlayerStatisticsEvent(event: MediaPlayerStatisticsEvents) {
        viewModelScope.launch {
            sendStatisticsMediaPlayerUseCase(event)
        }
    }

    companion object {
        private const val QUALITY_SCREENSHOT = 100
        private const val DATE_FORMAT_PATTERN = "yyyyMMdd-HHmmss"
        private const val SCREENSHOT_NAME_PREFIX = "Screenshot_"
        private const val SCREENSHOT_NAME_SUFFIX = ".jpg"

        /**
         * The state for the off is selected
         */
        const val SUBTITLE_SELECTED_STATE_OFF = 900

        /**
         * The state for the matched item is selected
         */
        const val SUBTITLE_SELECTED_STATE_MATCHED_ITEM = 901

        /**
         * The state for the add subtitle item is selected
         */
        const val SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM = 902
    }
}