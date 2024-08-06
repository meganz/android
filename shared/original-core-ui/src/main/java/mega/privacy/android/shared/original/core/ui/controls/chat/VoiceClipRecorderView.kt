package mega.privacy.android.shared.original.core.ui.controls.chat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.controls.layouts.LocalSnackBarHostState
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.controls.tooltips.Tooltip
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.original.core.ui.utils.showAutoDurationSnackbar
import kotlin.math.abs
import kotlin.math.roundToInt


/**
 * Event state of the voice recorder. This data class is used to pass data between
 * ChatInputTextToolBar and VoiceClipRecorderView.
 *
 * @property show whether to show the voice clip recorder view
 * @property offsetX X offset of pointer movement
 * @property offsetY Y offset of point movement
 * @property type [PointerEventType]
 * @property event [VoiceClipRecordEvent]
 */
data class VoiceClipRecorderState(
    val show: Boolean = false,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val type: PointerEventType = PointerEventType.Unknown,
    val event: VoiceClipRecordEvent = VoiceClipRecordEvent.None,
)

/**
 * Event of voice clip recording
 *
 */
enum class VoiceClipRecordEvent {
    /**
     * Record started
     *
     */
    Start,

    /**
     * User has locked the recording.
     *
     */
    Lock,

    /**
     * Recording cancelled
     *
     */
    Cancel,

    /**
     * Recording finished
     *
     */
    Finish,

    /**
     * Initial state before anything happens.
     *
     */
    None,
}

/**
 * Voice clip recorder view
 *
 * @param modifier
 * @param voiceClipRecorderState
 * @param onVoiceClipEvent callback to notify that a [VoiceClipRecordEvent] just happens
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceClipRecorderView(
    modifier: Modifier = Modifier,
    voiceClipRecorderState: MutableState<VoiceClipRecorderState> = mutableStateOf(
        VoiceClipRecorderState()
    ),
    onVoiceClipEvent: (VoiceClipRecordEvent) -> Unit = {},
    onNavigateToAppSettings: () -> Unit = {},
) {
    val show = voiceClipRecorderState.value.show
    val density = LocalDensity.current
    val viewHeight =
        remember { with(density) { VOICE_RECORDER_VIEW_HEIGHT_IN_DP.dp.toPx() }.roundToInt() }
    val recordScrollerSize = remember {
        with(density) { REC_SCROLLER_SIZE_IN_DP.dp.toPx() }.roundToInt()
    }
    val textInputBarHeight = remember { with(density) { 40.dp.toPx() } }.roundToInt()
    val textInputBarBottomPadding =
        remember { with(density) { 16.dp.toPx() } }.roundToInt()
    val verticalBarWidth = remember { with(density) { 40.dp.toPx() } }.roundToInt()
    var isLocked by remember(show) { mutableStateOf(false) }
    var seconds by remember(show) { mutableIntStateOf(0) }
    var timerRunning by remember(show) { mutableStateOf(false) }
    val screenWidth = LocalConfiguration.current.screenWidthDp
    var actualViewWidth by remember(show) { mutableIntStateOf(screenWidth) }
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val showTapAndHoldTooltip = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = LocalSnackBarHostState.current
    val recordAudioPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            coroutineScope.launch {
                val result = snackbarHostState?.showAutoDurationSnackbar(
                    context.getString(R.string.chat_microphone_permissions_denied_for_voice_clip),
                    context.getString(R.string.general_allow),
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onNavigateToAppSettings()
                }
            }
        }
    }

    Tooltip(
        expanded = showTapAndHoldTooltip,
        text = stringResource(id = R.string.recording_less_than_second),
        alignment = Alignment.TopEnd,
        offset = IntOffset(0, -textInputBarHeight)
    )

    if (show) {
        voiceClipRecorderState.value
            .takeIf { it.type == PointerEventType.Press && it.event != VoiceClipRecordEvent.Start }
            ?.run {
                if (audioPermissionState.status.isGranted) {
                    seconds = 0
                    timerRunning = true
                    sendEvent(VoiceClipRecordEvent.Start, onVoiceClipEvent, voiceClipRecorderState)
                } else {
                    recordAudioPermissionsLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    voiceClipRecorderState.value = VoiceClipRecorderState(show = false)
                    return
                }
            }

        voiceClipRecorderState.value.takeIf { it.type == PointerEventType.Release && !isLocked && audioPermissionState.status.isGranted }
            ?.run {
                val event = if (seconds < 1) {
                    showTapAndHoldTooltip.value = true
                    VoiceClipRecordEvent.Cancel
                } else {
                    VoiceClipRecordEvent.Finish
                }

                timerRunning = false
                seconds = 0
                sendEvent(event, onVoiceClipEvent, voiceClipRecorderState)
                voiceClipRecorderState.value = VoiceClipRecorderState(show = false)
                return
            }
        LaunchedEffect(key1 = timerRunning) {
            while (timerRunning) {
                delay(1000L)
                seconds++
            }
        }
        Popup(
            alignment = Alignment.TopStart,
            offset = IntOffset(
                x = if (voiceClipRecorderState.value.event == VoiceClipRecordEvent.Lock) -textInputBarBottomPadding else 0,
                y = 0 - (viewHeight - textInputBarHeight)
            )
        ) {
            Box(
                modifier = modifier
                    .width(actualViewWidth.dp)
                    .padding(
                        bottom = 10.dp,
                        start = 16.dp,
                        end = if (voiceClipRecorderState.value.event == VoiceClipRecordEvent.Lock) 0.dp else 16.dp
                    )
                    .height(VOICE_RECORDER_VIEW_HEIGHT_IN_DP.dp)
            ) {
                if (!isLocked) {
                    // vertical bar to lock the recording
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(40.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(color = MegaOriginalTheme.colors.background.surface1),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Icon(
                            modifier = Modifier
                                .padding(top = 12.dp)
                                .size(18.dp),
                            imageVector = ImageVector.vectorResource(R.drawable.ic_icon_lock_medium_regular_solid),
                            tint = MegaOriginalTheme.colors.icon.secondary,
                            contentDescription = null
                        )
                        Icon(
                            modifier = Modifier
                                .padding(top = 34.dp)
                                .size(18.dp),
                            imageVector = ImageVector.vectorResource(R.drawable.ic_icon_chevron_up_medium_regular_outline),
                            tint = MegaOriginalTheme.colors.icon.disabled,
                            contentDescription = null
                        )
                    }
                }

                // Horizontal bar to cancel the recording and showing recording progress
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            end = if (voiceClipRecorderState.value.event == VoiceClipRecordEvent.Lock) 0.dp else 32.dp
                        )
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(color = MegaOriginalTheme.colors.background.surface1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TimerWithIndicator(
                        seconds = seconds,
                        modifier = Modifier
                            .wrapContentWidth(Alignment.Start)
                            .padding(start = 12.dp)
                    )

                    if (isLocked) {
                        Spacer(modifier = Modifier.weight(1f))
                        RecordingWave()
                        Spacer(modifier = Modifier.weight(1f))
                        CancelButton(
                            modifier = Modifier
                                .wrapContentWidth(Alignment.End)
                                .padding(end = 12.dp),
                            onCancel = {
                                voiceClipRecorderState.value = VoiceClipRecorderState(show = false)
                                timerRunning = false
                                sendEvent(
                                    VoiceClipRecordEvent.Cancel,
                                    onVoiceClipEvent,
                                    voiceClipRecorderState
                                )
                            }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                        SlideToCancel(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .offset {
                                    val x = voiceClipRecorderState.value.offsetX
                                        .roundToInt()
                                        .takeIf { it < 0 } ?: 0
                                    IntOffset(x, 0)
                                }
                        )
                    }
                }

                if (!isLocked) {
                    RecordScroller(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset {
                                var x =
                                    if (voiceClipRecorderState.value.offsetX > 0f) 0 else voiceClipRecorderState.value.offsetX.roundToInt()
                                var y =
                                    if (voiceClipRecorderState.value.offsetY > 0f) 0 else voiceClipRecorderState.value.offsetY.roundToInt()

                                if (x == 0 && y == 0) { // no movement yet
                                    x = abs(verticalBarWidth - recordScrollerSize) / 2
                                    y =
                                        abs(textInputBarHeight - recordScrollerSize) / 2
                                } else if (abs(x) < abs(y)) { // move vertically
                                    x = abs(verticalBarWidth - recordScrollerSize) / 2
                                } else {  // move horizontally
                                    y =
                                        abs(textInputBarHeight - recordScrollerSize) / 2
                                }

                                if (abs(x) > SLIDE_TO_CANCEL_THRESHOLD_IN_PX) {
                                    voiceClipRecorderState.value =
                                        VoiceClipRecorderState(show = false)
                                    timerRunning = false
                                    seconds = 0
                                    sendEvent(
                                        VoiceClipRecordEvent.Cancel,
                                        onVoiceClipEvent,
                                        voiceClipRecorderState
                                    )
                                } else if (abs(y) > (viewHeight - recordScrollerSize)) {
                                    // in lock mode, we reduce the width of parent Popup, so the send
                                    // button is visible and clickable.
                                    actualViewWidth = screenWidth - 48
                                    isLocked = true
                                    sendEvent(
                                        VoiceClipRecordEvent.Lock,
                                        onVoiceClipEvent,
                                        voiceClipRecorderState
                                    )
                                }
                                IntOffset(x, y)
                            }
                    )
                }
            }
        }
    }
}

private fun sendEvent(
    event: VoiceClipRecordEvent,
    callback: (VoiceClipRecordEvent) -> Unit,
    state: MutableState<VoiceClipRecorderState>,
) {
    callback(event)
    state.value = state.value.copy(event = event)
}

@Composable
private fun TimerWithIndicator(seconds: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RecordIndicator()
        MegaText(
            modifier = Modifier.padding(start = 8.dp),
            text = formatSeconds(seconds),
            textColor = TextColor.Disabled,
            style = MaterialTheme.typography.subtitle2
        )
    }
}

@CombinedThemePreviews
@Composable
private fun TimerWithIndicatorPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        TimerWithIndicator(seconds = 100)
    }
}

@Composable
private fun CancelButton(
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = {},
) {
    Text(
        modifier = modifier
            .padding(start = 8.dp)
            .clickable { onCancel() },
        text = stringResource(id = R.string.general_cancel),
        color = MegaOriginalTheme.colors.components.interactive,
        style = MaterialTheme.typography.subtitle2,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun RecordingWave(modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier,
        imageVector = ImageVector.vectorResource(R.drawable.ic_icon_audio_wave),
        contentDescription = null,
        tint = MegaOriginalTheme.colors.icon.primary
    )
}

@Composable
private fun RecordIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .size(12.dp)
            .background(color = MegaOriginalTheme.colors.components.interactive),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White)
        )
    }
}

@Composable
private fun RecordScroller(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(REC_SCROLLER_SIZE_IN_DP.dp)
            .clip(CircleShape)
            .background(color = MegaOriginalTheme.colors.components.interactive),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_icon_mic_medium_regular_solid),
            contentDescription = null,
            tint = MegaOriginalTheme.colors.icon.onColor,
        )
    }
}

@Composable
private fun SlideToCancel(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_icon_chevron_left_medium_regular_outline),
            contentDescription = null,
            tint = MegaOriginalTheme.colors.icon.onColorDisabled
        )
        MegaText(
            text = stringResource(id = R.string.slide_to_cancel),
            textColor = TextColor.Placeholder,
            style = MaterialTheme.typography.subtitle2
        )
    }
}

private fun formatSeconds(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

private const val VOICE_RECORDER_VIEW_HEIGHT_IN_DP = 118
private const val REC_SCROLLER_SIZE_IN_DP = 80
private const val SLIDE_TO_CANCEL_THRESHOLD_IN_PX = 400

@CombinedThemePreviews
@Composable
private fun SlideToCancelPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SlideToCancel()
    }
}

@CombinedThemePreviews
@Composable
private fun RecordIndicatorPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        RecordIndicator()
    }
}

@CombinedThemePreviews
@Composable
private fun VoiceClipRecorderViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        VoiceClipRecorderView()
    }
}

@CombinedThemePreviews
@Composable
private fun RecordScrollerPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        RecordScroller()
    }
}
