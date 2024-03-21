package mega.privacy.android.core.ui.controls.status

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Item status indicator
 *
 * @param statusText    Status text
 * @param modifier      Modifier
 * @param statusIcon    Status icon
 * @param statusColor   Color to be applied for [statusText] and [statusIcon]
 */
@Composable
fun MegaStatusIndicator(
    statusText: String,
    modifier: Modifier = Modifier,
    @DrawableRes statusIcon: Int? = null,
    statusColor: StatusColor? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (statusIcon != null) {
            Icon(
                painter = painterResource(id = statusIcon),
                contentDescription = "Status Icon",
                tint = statusColor.getStatusIconColor(),
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = statusText,
            color = statusColor.getStatusTextColor(),
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.subtitle2,
        )
    }
}

/**
 * Gets the final [Color] from a [StatusColor] for a Text
 *
 * @return The corresponding color
 */
@Composable
fun StatusColor?.getStatusTextColor(): Color = this?.let {
    getStatusColor(statusColor = this)
} ?: MegaTheme.colors.text.secondary

/**
 * Gets the final [Color] from a [StatusColor] for an Icon
 *
 * @return The corresponding color
 */
@Composable
fun StatusColor?.getStatusIconColor(): Color = this?.let {
    getStatusColor(statusColor = this)
} ?: MegaTheme.colors.icon.secondary

/**
 * Gets the final [Color] from a [StatusColor]
 *
 * @param statusColor The status color
 * @return The corresponding color
 */
@Composable
private fun getStatusColor(statusColor: StatusColor): Color =
    when (statusColor) {
        StatusColor.Success -> MegaTheme.colors.support.success
        StatusColor.Info -> MegaTheme.colors.support.info
        StatusColor.Warning -> MegaTheme.colors.support.warning
        StatusColor.Error -> MegaTheme.colors.support.error
    }

/**
 * Status color
 */
enum class StatusColor {
    /**
     * Success: everything OK or up to date
     */
    Success,

    /**
     * Info: there is some info or something in progress
     */
    Info,

    /**
     * Warning: there is some alert
     */
    Warning,

    /**
     * Error: there is some final error
     */
    Error
}