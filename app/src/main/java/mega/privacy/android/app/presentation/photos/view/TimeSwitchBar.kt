package mega.privacy.android.app.presentation.photos.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.model.TimeBarTab

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
                Button(
                    onClick = {
                        onTimeBarTabSelected(timeBarTab)
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (selectedIndex == index)
                            colorResource(id = R.color.black_white)
                        else
                            colorResource(id = R.color.white_black),
                        contentColor = if (selectedIndex == index)
                            colorResource(id = R.color.white_black)
                        else
                            colorResource(id = R.color.black_white),
                    ),
                    border = BorderStroke(width = 1.dp,
                        color = if (selectedIndex == index)
                            colorResource(id = R.color.black_grey_alpha_012)
                        else
                            colorResource(id = R.color.grey_alpha_012_black)),
                    modifier = Modifier
                        .height(36.dp)
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = stringResource(id = timeBarTabTextResId),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.15.sp
                    )
                }
            }
        }
    }
}