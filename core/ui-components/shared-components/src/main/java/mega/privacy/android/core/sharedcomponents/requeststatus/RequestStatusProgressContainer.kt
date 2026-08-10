package mega.privacy.android.core.sharedcomponents.requeststatus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.indicators.MegaAnimatedLinearProgressIndicator
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.domain.entity.Progress

/**
 * Composable for request status progress bar container
 * @param viewModel ViewModel for request status progress bar container
 */
@Composable
fun RequestStatusProgressContainer(
    viewModel: RequestStatusProgressViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RequestStatusProgressBarContent(
        progress = uiState.progress,
        modifier = modifier
    )
}

/**
 * Composable for request status progress bar content
 */
@Composable
fun RequestStatusProgressBarContent(
    progress: Progress?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = progress != null,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = FADE_ENTER_ANIM_MS)
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = FADE_EXIT_ANIM_MS)
        ),
        modifier = Modifier
            .background(DSTokens.colors.background.pageBackground)
            .then(modifier)
    ) {
        val progressValue = progress?.floatValue ?: 0f
        MegaAnimatedLinearProgressIndicator(
            indicatorProgress = progressValue,
            height = 3.dp,
            progressAnimDuration = if (progressValue > PROGRESS_FAST_THRESHOLD) {
                PROGRESS_FAST_ANIM_MS
            } else {
                PROGRESS_NORMAL_ANIM_MS
            },
            modifier = Modifier.testTag(PROGRESS_BAR_TEST_TAG),
            clip = RoundedCornerShape(0.dp),
            strokeCap = StrokeCap.Square
        )
    }
}

private const val PROGRESS_FAST_ANIM_MS = 200
private const val PROGRESS_NORMAL_ANIM_MS = 500
private const val PROGRESS_FAST_THRESHOLD = 0.9f
private const val FADE_ENTER_ANIM_MS = 400
private const val FADE_EXIT_ANIM_MS = 100
internal const val PROGRESS_BAR_TEST_TAG = "request_status_progress_bar_content:progress_bar"

@CombinedThemePreviews
@Composable
private fun RequestStatusProgressBarContentPreview() {
    AndroidThemeForPreviews {
        RequestStatusProgressBarContent(progress = Progress(0.5f))
    }
}