package mega.privacy.android.feature.sync.ui.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.CardSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.R as iconPackR

/**
 * One exclusion rule as a row: the pattern it matches on, what it does beneath that, and a chevron
 * onto the Edit screen.
 *
 * The design draws no affordance for a rule that arrives disabled, and the parser keeps those, so
 * a disabled rule renders as any other here rather than being hidden — a rule the user cannot see
 * is one they cannot delete.
 */
@Composable
internal fun ExclusionRuleRow(
    pattern: String,
    supportingText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ExclusionPatternText(pattern = pattern)
            // a rule the design wrote no copy for has no sentence to show, and an empty one still
            // takes a line's height
            if (supportingText.isNotEmpty()) {
                MegaText(
                    text = supportingText,
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodyMedium,
                )
            }
        }
        MegaIcon(
            modifier = Modifier.size(CHEVRON_SIZE),
            painter = painterResource(iconPackR.drawable.ic_chevron_right_medium_thin_outline),
            tint = IconColor.Secondary,
            contentDescription = null,
        )
    }
}

/**
 * A rule's pattern, rendered as code so that the wildcards and punctuation in it read as literal
 * characters rather than as prose. The tint marks where the pattern starts and ends, without which
 * a one-character pattern such as `.` or `~` is easy to miss.
 */
@Composable
internal fun ExclusionPatternText(
    pattern: String,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier,
        surfaceColor = SurfaceColor.Surface1,
        shape = RoundedCornerShape(4.dp),
    ) {
        MegaText(
            modifier = Modifier.padding(horizontal = 2.dp),
            text = pattern,
            textColor = TextColor.Primary,
            style = AppTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = PATTERN_TRACKING,
            ),
        )
    }
}

/**
 * A row for a size bound, which the design draws on a single line with no supporting text because
 * the sentence is the whole rule.
 */
@Composable
internal fun ExclusionSizeRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MegaText(
            modifier = Modifier.weight(1f),
            text = text,
            textColor = TextColor.Primary,
            style = AppTheme.typography.bodyLarge,
        )
        MegaIcon(
            modifier = Modifier.size(CHEVRON_SIZE),
            painter = painterResource(iconPackR.drawable.ic_chevron_right_medium_thin_outline),
            tint = IconColor.Secondary,
            contentDescription = null,
        )
    }
}

private val CHEVRON_SIZE = 24.dp
private val PATTERN_TRACKING = 0.5.sp
