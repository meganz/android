package mega.privacy.android.feature.photos.presentation.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.chip.SelectionChipStyle
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoModificationTimePeriod

@Composable
internal fun TimelineTabRoute(modifier: Modifier = Modifier) {
    TimelineTabScreen(
        modifier = modifier,
        onPhotoTimePeriodSelected = {}
    )
}

@Composable
internal fun TimelineTabScreen(
    onPhotoTimePeriodSelected: (PhotoModificationTimePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        PhotoModificationTimePeriodSelector(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            isVisible = true,
            selectedTimePeriod = PhotoModificationTimePeriod.All,
            onPhotoTimePeriodSelected = onPhotoTimePeriodSelected
        )
    }
}

@Composable
private fun PhotoModificationTimePeriodSelector(
    isVisible: Boolean,
    selectedTimePeriod: PhotoModificationTimePeriod,
    onPhotoTimePeriodSelected: (PhotoModificationTimePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        exit = slideOutVertically(),
        enter = slideInVertically(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .requiredWidthIn(max = 360.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            PhotoModificationTimePeriod.entries.forEachIndexed { index, timePeriod ->
                MegaChip(
                    onClick = { onPhotoTimePeriodSelected(timePeriod) },
                    selected = selectedTimePeriod == timePeriod,
                    text = stringResource(id = timePeriod.stringResId),
                    style = SelectionChipStyle,
                )

                if (index != PhotoModificationTimePeriod.entries.lastIndex) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun TimelineTabScreenPreview() {
    AndroidThemeForPreviews {
        TimelineTabScreen(
            modifier = Modifier.fillMaxSize(),
            onPhotoTimePeriodSelected = {}
        )
    }
}
