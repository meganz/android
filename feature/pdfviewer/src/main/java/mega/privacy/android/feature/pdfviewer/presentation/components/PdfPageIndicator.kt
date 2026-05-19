package mega.privacy.android.feature.pdfviewer.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.CardSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import kotlin.math.roundToInt

/**
 * Fast-scroll page indicator for the PDF viewer.
 *
 * Right edge of the viewer shows a round draggable thumb with an up/down triangle icon.
 * To its left, a compact pill displays "currentPage/totalPages". Both slide vertically
 * together to reflect the document's scroll position and fade as a pair.
 *
 * Renders nothing when [totalPages] is less than 2.
 *
 * @param currentPage 1-based current page
 * @param totalPages total number of pages in the document
 * @param isVisible whether the indicator should be visible (e.g. while the viewer is scrolling
 *                  or within an auto-hide window). The indicator also stays visible while pressed.
 * @param onScrub Called with a 0f..1f scroll proportion during an active scrub drag, or `null`
 *                when the drag ends or is cancelled.
 * @param onScrubPressed Invoked with `true` while the thumb is pressed or dragged, and `false`
 *                       on release. Lets callers stop an in-flight fling on press without
 *                       committing a scroll position.
 * @param modifier Modifier for the composable
 */
@Composable
fun PdfPageIndicator(
    currentPage: Int,
    totalPages: Int,
    isVisible: Boolean,
    onScrub: (Float?) -> Unit,
    onScrubPressed: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (totalPages > 1) {
        PdfPageIndicatorContent(
            currentPage = currentPage,
            totalPages = totalPages,
            isVisible = isVisible,
            onScrub = onScrub,
            onScrubPressed = onScrubPressed,
            modifier = modifier,
        )
    }
}

@Composable
private fun PdfPageIndicatorContent(
    currentPage: Int,
    totalPages: Int,
    isVisible: Boolean,
    onScrub: (Float?) -> Unit,
    onScrubPressed: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val thumbHeightPx = remember(density) { with(density) { thumbSize.toPx() } }

    val pageProportion =
        ((currentPage - 1).toFloat() / (totalPages - 1).toFloat()).coerceIn(0f, 1f)
    val travelScale = travelScaleForTotalPages(totalPages)
    val state = rememberPdfIndicatorScrubState(pageProportion, travelScale, onScrub, onScrubPressed)

    // Snap Y until the track is measured once; enabling tween in the same pass as first layout
    // would animate from target 0 (trackHeightPx was 0) to the real offset.
    var hasCompletedInitialTrackPlacement by remember { mutableStateOf(false) }
    LaunchedEffect(state.trackHeightPx) {
        if (state.trackHeightPx > 0f) {
            hasCompletedInitialTrackPlacement = true
        }
    }

    val animatedYPx by animateIntAsState(
        targetValue = state.targetYPx,
        animationSpec = when {
            state.isPressed -> snap()
            !hasCompletedInitialTrackPlacement -> snap()
            else -> tween(durationMillis = SLIDE_DURATION_MS)
        },
        label = "pdfPageIndicatorY",
    )

    val visible = isVisible || state.isPressed

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(
                top = thumbTrackVerticalInset,
                bottom = thumbTrackVerticalInset,
                end = thumbEndPadding,
            )
            .onGloballyPositioned { coords ->
                state.trackHeightPx = (coords.size.height - thumbHeightPx).coerceAtLeast(0f)
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(thumbSize)
                // Lambda form: state reads deferred to layout phase, no recomposition on scroll.
                .offset { IntOffset(x = 0, y = animatedYPx) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            PageNumberPill(
                label = "$currentPage/$totalPages",
                visible = visible,
            )
            ScrubThumb(
                visible = visible,
                state = state,
                modifier = Modifier.padding(start = pillThumbGap),
            )
        }
    }
}

@Composable
private fun PageNumberPill(
    label: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val (enter, exit) = indicatorTransition(initialScale = 0.8f)
    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit,
        modifier = modifier,
    ) {
        CardSurface(
            modifier = Modifier
                .testTag(PDF_PAGE_INDICATOR_LABEL_TAG)
                .height(pagePillHeight),
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            surfaceColor = SurfaceColor.PageBackground,
        ) {
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                MegaText(
                    text = label,
                    textColor = TextColor.Primary,
                    modifier = Modifier.padding(horizontal = pagePillHorizontalPadding),
                )
            }
        }
    }
}

@Composable
private fun ScrubThumb(
    visible: Boolean,
    state: PdfIndicatorScrubState,
    modifier: Modifier = Modifier,
) {
    val (enter, exit) = indicatorTransition(initialScale = 0.5f)
    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit,
        modifier = modifier,
    ) {
        CardSurface(
            modifier = Modifier
                .testTag(PDF_PAGE_INDICATOR_TAG)
                .scrubGestures(state)
                .size(thumbSize),
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            surfaceColor = SurfaceColor.PageBackground,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                MegaIcon(
                    painter = rememberVectorPainter(
                        IconPack.Medium.Thin.Outline.ChevronUpDown
                    ),
                    tint = IconColor.Secondary,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

/**
 * Shared enter/exit pair for the indicator's two sub-elements. The pill uses a subtle
 * 0.8 initial scale while the thumb uses a more pronounced 0.5 pop.
 */
@Composable
private fun indicatorTransition(initialScale: Float): Pair<EnterTransition, ExitTransition> {
    val enter = remember(initialScale) {
        fadeIn() + scaleIn(
            transformOrigin = TransformOrigin(1f, 0.5f),
            initialScale = initialScale,
        )
    }
    val exit = remember(initialScale) {
        scaleOut(
            animationSpec = tween(delayMillis = HIDE_DELAY_MILLIS),
            targetScale = initialScale,
            transformOrigin = TransformOrigin(1f, 0.5f),
        ) + fadeOut(animationSpec = tween(delayMillis = HIDE_DELAY_MILLIS))
    }
    return enter to exit
}

private fun Modifier.scrubGestures(state: PdfIndicatorScrubState): Modifier =
    pointerInput(Unit) {
        detectTapGestures(
            onPress = {
                state.onPress()
                if (tryAwaitRelease()) state.onRelease()
            },
        )
    }.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragStart = { state.onDragStart() },
            onDragEnd = { state.onDragEnd() },
            onDragCancel = { state.onDragEnd() },
        ) { change, dragAmount ->
            change.consume()
            state.onDragDelta(dragAmount)
        }
    }

/**
 * Holds the scrub state for [PdfPageIndicator] so the composable body can focus on layout.
 *
 * [pageProportion] is the 0f..1f position derived from the currently displayed page and
 * ticks discretely at each page boundary. During an active drag the class tracks a
 * continuous `dragProportion` instead, so the thumb can follow the finger frame-by-frame
 * between page ticks. [targetYPx] resolves to whichever is live at the moment.
 *
 * Gesture callbacks are methods so they can be wired from a plain gesture `Modifier`
 * without capturing stale lambdas.
 */
@Stable
private class PdfIndicatorScrubState(
    private val onScrub: (Float?) -> Unit,
    private val onScrubPressed: (Boolean) -> Unit,
) {
    /** Page-derived proportion, updated every recomposition from the input state. */
    var pageProportion: Float by mutableFloatStateOf(0f)

    /** Container track height in pixels (container height minus the thumb's own height). */
    var trackHeightPx: Float by mutableFloatStateOf(0f)

    /** True while the user is pressing or dragging the thumb. */
    var isPressed: Boolean by mutableStateOf(false)
        private set

    /** Continuous drag proportion in document space 0..1; only read when [isPressed] is true. */
    private var dragProportion: Float by mutableFloatStateOf(0f)

    /**
     * Multiplier on document→visual mapping: 1f = thumb uses full track; smaller = band around center.
     */
    var travelScale: Float by mutableFloatStateOf(1f)

    // Plain vars (not State) — gesture scratch that must not trigger recomposition.
    private var dragStart = 0f
    private var dragAccumulatedPx = 0f

    val targetYPx: Int
        get() {
            val documentProportion = if (isPressed) dragProportion else pageProportion
            val visualProportion = documentToVisualProportion(documentProportion, travelScale)
            return (trackHeightPx * visualProportion).roundToInt()
        }

    // Press only signals "stop any in-flight fling" — it does not commit a scroll position.
    // dragProportion must be re-synced here because targetYPx switches to reading it once
    // isPressed flips true; otherwise the thumb jumps to a stale value.
    fun onPress() {
        isPressed = true
        dragProportion = pageProportion
        onScrubPressed(true)
    }

    fun onRelease() {
        isPressed = false
        onScrubPressed(false)
    }

    fun onDragStart() {
        isPressed = true
        dragStart = pageProportion
        dragAccumulatedPx = 0f
        dragProportion = pageProportion
    }

    fun onDragDelta(dyPx: Float) {
        if (trackHeightPx <= 0f) return
        dragAccumulatedPx += dyPx
        val vStart = documentToVisualProportion(dragStart, travelScale)
        val vMin = documentToVisualProportion(0f, travelScale)
        val vMax = documentToVisualProportion(1f, travelScale)
        val vNew = (vStart + dragAccumulatedPx / trackHeightPx).coerceIn(vMin, vMax)
        val documentProportion = visualToDocumentProportion(vNew, travelScale)
        dragProportion = documentProportion
        onScrub(documentProportion)
    }

    fun onDragEnd() {
        isPressed = false
        onScrub(null)
        onScrubPressed(false)
    }
}

@Composable
private fun rememberPdfIndicatorScrubState(
    pageProportion: Float,
    travelScale: Float,
    onScrub: (Float?) -> Unit,
    onScrubPressed: (Boolean) -> Unit,
): PdfIndicatorScrubState {
    // Indirection via rememberUpdatedState so the state object holds a stable lambda
    // reference while always calling the freshest caller-provided callbacks.
    val latestOnScrub = rememberUpdatedState(onScrub)
    val latestOnScrubPressed = rememberUpdatedState(onScrubPressed)
    val state = remember {
        PdfIndicatorScrubState(
            onScrub = { latestOnScrub.value(it) },
            onScrubPressed = { latestOnScrubPressed.value(it) },
        )
    }
    state.pageProportion = pageProportion
    state.travelScale = travelScale
    return state
}

internal const val PDF_PAGE_INDICATOR_TAG = "pdf_page_indicator:thumb"
internal const val PDF_PAGE_INDICATOR_LABEL_TAG = "pdf_page_indicator:label"

private val thumbSize = 40.dp
private val pagePillHeight = 32.dp
private val pagePillHorizontalPadding = 12.dp
private val pillThumbGap = 8.dp
private val thumbEndPadding = 8.dp
private val thumbTrackVerticalInset = 4.dp
private const val HIDE_DELAY_MILLIS = 900
private const val SLIDE_DURATION_MS = 250

@CombinedThemePreviews
@Composable
private fun PdfPageIndicatorPreview() {
    AndroidThemeForPreviews {
        PdfPageIndicator(
            currentPage = 2,
            totalPages = 14,
            isVisible = true,
            onScrub = {},
            onScrubPressed = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PdfPageIndicatorTwoPagePreview() {
    AndroidThemeForPreviews {
        PdfPageIndicator(
            currentPage = 1,
            totalPages = 2,
            isVisible = true,
            onScrub = {},
            onScrubPressed = {},
        )
    }
}
