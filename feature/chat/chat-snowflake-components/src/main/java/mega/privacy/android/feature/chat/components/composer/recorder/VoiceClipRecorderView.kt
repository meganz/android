package mega.privacy.android.feature.chat.components.composer.recorder

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.tooltip.direction.TooltipDirection
import mega.android.core.ui.components.tooltip.popup.simple.SimpleBottomDirectionTooltipPopup
import mega.android.core.ui.components.tooltip.popup.simple.SimpleTooltipContentBodyOnly
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.privacy.android.icon.pack.R as IconPackR
import kotlin.time.Duration.Companion.seconds

/**
 * Record button of the chat composer, with the overlay that shows while the user records a
 * voice clip.
 *
 * The user holds the button to record, slides towards the start to cancel and slides up to lock
 * the recording. When the recording is locked, the overlay shows cancel and send controls. If
 * the user releases the button in less than one second, the recording is cancelled and a hint
 * tells the user to tap and hold.
 *
 * @param onVoiceClipEvent Called with each [VoiceClipRecordEvent]
 * @param onRecordAudioPermissionDenied Called when the user denies the record audio permission,
 * so that the caller can tell the user how to give the permission
 * @param slideToCancelText Hint that tells the user to slide to cancel
 * @param tapAndHoldHintText Hint that shows when the user releases the button too early
 * @param modifier [Modifier]
 * @param state [VoiceClipRecorderState]
 * @param contentDescription Accessibility label of the button
 */
@Composable
fun VoiceClipRecorderView(
    onVoiceClipEvent: (VoiceClipRecordEvent) -> Unit,
    onRecordAudioPermissionDenied: () -> Unit,
    slideToCancelText: String,
    tapAndHoldHintText: String,
    modifier: Modifier = Modifier,
    state: VoiceClipRecorderState = rememberVoiceClipRecorderState(),
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val currentOnVoiceClipEvent by rememberUpdatedState(onVoiceClipEvent)
    val currentOnPermissionDenied by rememberUpdatedState(onRecordAudioPermissionDenied)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) currentOnPermissionDenied()
    }
    var buttonCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var buttonCenterX by remember { mutableFloatStateOf(0f) }
    var rootWidth by remember { mutableIntStateOf(0) }

    DisposableEffect(state) {
        onDispose { state.cancel()?.let(currentOnVoiceClipEvent) }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                buttonCoordinates = it
                buttonCenterX = it.boundsInWindow().center.x
                rootWidth = it.findRootCoordinates().size.width
            }
            .semantics { role = Role.Button }
            .testTag(VOICE_CLIP_RECORDER_BUTTON_TAG)
            .pointerInput(state, layoutDirection) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val isGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!isGranted) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        return@awaitEachGesture
                    }
                    state.start()?.let(currentOnVoiceClipEvent)
                    down.consume()
                    while (state.isRecording && !state.isLocked) {
                        val change = awaitPointerEvent().changes
                            .firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            state.release()?.let(currentOnVoiceClipEvent)
                            break
                        }
                        val offset = change.position - down.position
                        val offsetTowardsEnd = if (layoutDirection == LayoutDirection.Rtl) {
                            offset.copy(x = -offset.x)
                        } else {
                            offset
                        }
                        state.drag(offsetTowardsEnd)?.let(currentOnVoiceClipEvent)
                        change.consume()
                    }
                }
            }
            .padding(8.dp),
    ) {
        MegaIcon(
            painter = painterResource(IconPackR.drawable.ic_mic_medium_thin_outline),
            tint = IconColor.Secondary,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
        )

        if (state.isRecording) {
            RecorderPopup(
                state = state,
                buttonCenterX = buttonCenterX,
                rootWidth = rootWidth,
                slideToCancelText = slideToCancelText,
                onVoiceClipEvent = currentOnVoiceClipEvent,
            )
        }
    }

    val coordinates = buttonCoordinates
    if (state.showTapAndHoldHint && coordinates != null) {
        LaunchedEffect(Unit) {
            delay(TapAndHoldHintDuration)
            state.showTapAndHoldHint = false
        }
        SimpleBottomDirectionTooltipPopup(
            direction = if (layoutDirection == LayoutDirection.Rtl) {
                TooltipDirection.Bottom.Left
            } else {
                TooltipDirection.Bottom.Right
            },
            anchorViewCoordinates = coordinates,
            content = SimpleTooltipContentBodyOnly(tapAndHoldHintText),
            onDismissRequest = { state.showTapAndHoldHint = false },
        )
    }
}

@Composable
private fun RecorderPopup(
    state: VoiceClipRecorderState,
    buttonCenterX: Float,
    rootWidth: Int,
    slideToCancelText: String,
    onVoiceClipEvent: (VoiceClipRecordEvent) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val scrollerRadiusPx = with(density) {
        (VoiceClipRecorderDefaults.ScrollerSize / 2).roundToPx()
    }
    val windowMarginPx = with(density) { VoiceClipRecorderDefaults.WindowMargin.roundToPx() }
    val overlayWidthPx = if (layoutDirection == LayoutDirection.Rtl) {
        rootWidth - (buttonCenterX.toInt() - scrollerRadiusPx) - windowMarginPx
    } else {
        buttonCenterX.toInt() + scrollerRadiusPx - windowMarginPx
    }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state) {
        while (true) {
            elapsedSeconds = state.elapsed.inWholeSeconds
            delay(TimerUpdateInterval)
        }
    }

    Popup(
        popupPositionProvider = remember(scrollerRadiusPx) {
            RecorderPositionProvider(scrollerRadiusPx)
        },
        properties = PopupProperties(clippingEnabled = false),
    ) {
        VoiceClipRecorderOverlay(
            isLocked = state.isLocked,
            dragOffset = state.dragOffset,
            elapsedSeconds = elapsedSeconds,
            slideToCancelText = slideToCancelText,
            onCancel = { state.cancel()?.let(onVoiceClipEvent) },
            onSend = { state.finish()?.let(onVoiceClipEvent) },
            modifier = Modifier.width(with(density) { overlayWidthPx.coerceAtLeast(0).toDp() }),
        )
    }
}

/**
 * Places the bottom-end corner of the overlay [scrollerRadiusPx] from the centre of the record
 * button on both axes, so that the scroller covers the record button.
 */
private class RecorderPositionProvider(
    private val scrollerRadiusPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val center = anchorBounds.center
        val x = if (layoutDirection == LayoutDirection.Rtl) {
            center.x - scrollerRadiusPx
        } else {
            center.x + scrollerRadiusPx - popupContentSize.width
        }
        return IntOffset(x, center.y + scrollerRadiusPx - popupContentSize.height)
    }
}

/**
 * Test tag for the record button of the voice clip recorder.
 */
const val VOICE_CLIP_RECORDER_BUTTON_TAG = "voice_clip_recorder:button"

private val TapAndHoldHintDuration = 3.seconds
private val TimerUpdateInterval = 0.25.seconds

@CombinedThemePreviews
@Composable
private fun VoiceClipRecorderViewPreview() {
    AndroidThemeForPreviews {
        VoiceClipRecorderView(
            onVoiceClipEvent = {},
            onRecordAudioPermissionDenied = {},
            slideToCancelText = "Slide to cancel",
            tapAndHoldHintText = "Tap and hold to record",
        )
    }
}
