package mega.privacy.android.feature.payment.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.indicators.ProgressBarIndicator
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.SupportColor

/**
 * Determinate usage bar used on the quota-warning upsell cards. Colours the bar by [level]
 * (green when healthy, amber when approaching the limit, red when over).
 *
 * @param percentage usage as a 0..100 value; coerced into range
 * @param level determines the bar colour
 */
@Composable
fun QuotaUsageProgressBar(
    percentage: Float,
    level: QuotaUsageLevel,
    modifier: Modifier = Modifier,
) {
    val supportColor = when (level) {
        QuotaUsageLevel.Normal -> SupportColor.Success
        QuotaUsageLevel.Warning -> SupportColor.Warning
        QuotaUsageLevel.Error -> SupportColor.Error
    }
    ProgressBarIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp),
        progressPercentage = percentage.coerceIn(0f, 100f),
        supportColor = supportColor,
        surfaceColor = SurfaceColor.Surface3,
    )
}

/**
 * Severity level of a quota usage bar, driving its colour.
 */
enum class QuotaUsageLevel {
    /**
     * Usage is comfortably within the limit.
     */
    Normal,

    /**
     * Usage is approaching the limit.
     */
    Warning,

    /**
     * Usage has reached or exceeded the limit.
     */
    Error,
}

@CombinedThemePreviews
@Composable
private fun QuotaUsageProgressBarPreview(
    @PreviewParameter(QuotaUsagePreviewParameterProvider::class) usage: Pair<QuotaUsageLevel, Float>,
) {
    AndroidTheme(isSystemInDarkTheme()) {
        QuotaUsageProgressBar(percentage = usage.second, level = usage.first)
    }
}

private class QuotaUsagePreviewParameterProvider :
    PreviewParameterProvider<Pair<QuotaUsageLevel, Float>> {
    override val values = sequenceOf(
        QuotaUsageLevel.Normal to 5f,
        QuotaUsageLevel.Warning to 80f,
        QuotaUsageLevel.Error to 100f,
    )
}
