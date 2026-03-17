package mega.privacy.android.feature.clouddrive.presentation.mediadiscovery.view

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
import mega.privacy.android.feature.clouddrive.presentation.mediadiscovery.model.MediaDiscoveryPeriod

@Composable
internal fun MediaDiscoveryPeriodChip(
    selectedMediaDiscoveryPeriod: MediaDiscoveryPeriod,
    onTimeBarTabSelected: (MediaDiscoveryPeriod) -> Unit,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
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
            MediaDiscoveryPeriod.entries.forEachIndexed { index, timeBarTab ->
                MegaChip(
                    onClick = { onTimeBarTabSelected(timeBarTab) },
                    selected = selectedMediaDiscoveryPeriod == timeBarTab,
                    text = stringResource(id = timeBarTab.stringResId),
                    style = SelectionChipStyle,
                )

                if (index != MediaDiscoveryPeriod.entries.lastIndex) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}
