package mega.privacy.android.app.presentation.requeststatus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaAnimatedLinearProgressIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Composable for request status progress bar container
 * @param viewModel ViewModel for request status progress bar container
 */
@Composable
fun RequestStatusProgressContainer(
    viewModel: RequestStatusProgressViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    RequestStatusProgressBarContent(
        showProgressBar = uiState.showProgressBar,
        progress = uiState.progress,
        modifier = modifier
    )
}

/**
 * Composable for request status progress bar content
 */
@Composable
fun RequestStatusProgressBarContent(
    showProgressBar: Boolean,
    progress: Long,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = showProgressBar,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(durationMillis = 400)
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 100)
        ),
        modifier = modifier
    ) {
        MegaAnimatedLinearProgressIndicator(
            indicatorProgress = progress / 1000f,
            height = 4.dp,
            progressAnimDuration = 500,
            modifier = Modifier.testTag(PROGRESS_BAR_TEST_TAG),
            clip = RoundedCornerShape(0.dp),
            strokeCap = StrokeCap.Square
        )
    }
}

internal const val PROGRESS_BAR_TEST_TAG = "request_status_progress_bar_content:progress_bar"

@CombinedThemePreviews
@Composable
private fun RequestStatusProgressBarContentPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        RequestStatusProgressBarContent(true, progress = 500)
    }
}