package mega.privacy.android.feature.photos.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.chip.SelectionChipStyle
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod

@Composable
internal fun MediaTimePeriodSelector(
    isVisible: Boolean,
    selectedTimePeriod: MediaTimePeriod,
    onMediaTimePeriodSelected: (MediaTimePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = isVisible,
        exit = slideOutVertically { it },
        enter = slideInVertically { it },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            MediaTimePeriod.entries.forEachIndexed { index, timePeriod ->
                MegaChip(
                    onClick = { onMediaTimePeriodSelected(timePeriod) },
                    selected = selectedTimePeriod == timePeriod,
                    text = stringResource(id = timePeriod.stringResId),
                    style = SelectionChipStyle,
                )

                if (index != MediaTimePeriod.entries.lastIndex) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}
