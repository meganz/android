package mega.privacy.android.app.presentation.photos.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.controls.chip.RoundedChipStyle

/**
 * A row of buttons to switch time view
 *
 * Year/Month/Days/All (YMDA)
 */
@Composable
fun TimeSwitchBar(
    timeBarTabs: List<TimeBarTab> = TimeBarTab.values().asList(),
    selectedTimeBarTab: TimeBarTab = TimeBarTab.All,
    onTimeBarTabSelected: (TimeBarTab) -> Unit = {},
    isVisible: () -> Boolean = { true },
) {
    AnimatedVisibility(
        visible = isVisible(),
        exit = slideOutVertically {
            it
        },
        enter = slideInVertically {
            it
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .requiredWidthIn(max = 360.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val selectedIndex = selectedTimeBarTab.ordinal
            timeBarTabs.mapIndexed { index, timeBarTab ->
                val timeBarTabTextResId = when (timeBarTab) {
                    TimeBarTab.Years -> R.string.years_view_button
                    TimeBarTab.Months -> R.string.months_view_button
                    TimeBarTab.Days -> R.string.days_view_button
                    TimeBarTab.All -> R.string.all_view_button
                }
                MegaChip(
                    onClick = {
                        onTimeBarTabSelected(timeBarTab)
                    },
                    selected = selectedIndex == index,
                    text = stringResource(id = timeBarTabTextResId),
                    style = RoundedChipStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}