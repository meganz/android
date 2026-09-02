package mega.privacy.android.feature.myaccount.presentation.widget.view

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
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.values.SupportColor
import mega.privacy.android.feature.myaccount.presentation.model.QuotaLevel

/**
 * Horizontal progress bar for MyAccount widget showing storage quota usage
 *
 * @param level The quota level (Success, Warning, Error) determining the color
 * @param progress The progress percentage (0-100)
 * @param modifier Modifier for the progress bar
 */
@Composable
fun MyAccountHorizontalProgressBar(
    level: QuotaLevel,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val supportColor = when (level) {
        QuotaLevel.Error -> SupportColor.Error
        QuotaLevel.Warning -> SupportColor.Warning
        QuotaLevel.Success -> SupportColor.Success
    }

    ProgressBarIndicator(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp),
        progressPercentage = progress.coerceIn(0f, 100f),
        supportColor = supportColor,
        surfaceColor = SurfaceColor.Surface3
    )
}

@CombinedThemePreviews
@Composable
private fun MyAccountHorizontalProgressBarPreview(
    @PreviewParameter(QuotaProgressPreviewParameterProvider::class) quota: Pair<QuotaLevel, Float>,
) {
    AndroidThemeForPreviews {
        MyAccountHorizontalProgressBar(
            level = quota.first,
            progress = quota.second
        )
    }
}

private class QuotaProgressPreviewParameterProvider :
    PreviewParameterProvider<Pair<QuotaLevel, Float>> {
    override val values = sequenceOf(
        Pair(QuotaLevel.Success, 50f),
        Pair(QuotaLevel.Success, 10f),
        Pair(QuotaLevel.Warning, 85f),
        Pair(QuotaLevel.Warning, 90f),
        Pair(QuotaLevel.Error, 95f),
        Pair(QuotaLevel.Error, 100f),
        Pair(QuotaLevel.Success, 0f)
    )
}
