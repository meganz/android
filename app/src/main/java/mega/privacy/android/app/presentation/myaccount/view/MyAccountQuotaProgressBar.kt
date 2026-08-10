package mega.privacy.android.app.presentation.myaccount.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidTheme
import mega.android.core.ui.theme.values.BackgroundColor
import mega.android.core.ui.theme.values.SupportColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.myaccount.presentation.model.QuotaLevel
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.extensions.body2medium

@Composable
internal fun MyAccountQuotaProgressBar(
    level: QuotaLevel,
    progress: Int,
    progressIndicatorTestTag: String,
    modifier: Modifier = Modifier,
    progressTextTestTag: String? = null,
) {
    val defaultTextStyle = MaterialTheme.typography.body2medium
    var finalTextStyle by remember { mutableStateOf(defaultTextStyle) }
    var shouldDrawText by remember { mutableStateOf(false) }
    val color = when (level) {
        QuotaLevel.Error -> SupportColor.Error to TextColor.Error
        QuotaLevel.Warning -> SupportColor.Warning to TextColor.Warning
        else -> SupportColor.Success to TextColor.Success
    }

    Box(modifier = modifier.size(50.dp)) {
        MegaCircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .testTag(progressIndicatorTestTag),
            supportColor = color.first,
            strokeWidth = 5.dp,
            progress = (progress.toFloat() / 100).coerceAtMost(1f),
            backgroundColor = BackgroundColor.Surface3,
        )

        MegaText(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(5.dp)
                .drawWithContent {
                    if (shouldDrawText) drawContent()
                }
                .then(
                    if (progressTextTestTag != null) Modifier.testTag(progressTextTestTag)
                    else Modifier
                ),
            text = "$progress%",
            textColor = color.second,
            style = finalTextStyle,
            softWrap = false,
            onTextLayout = { result ->
                if (result.didOverflowWidth && defaultTextStyle.fontSize.isSpecified) {
                    finalTextStyle = finalTextStyle.copy(
                        fontSize = finalTextStyle.fontSize * 0.8
                    )
                } else {
                    shouldDrawText = true
                }
            }
        )
    }
}


@CombinedThemePreviews
@Composable
private fun MyAccountQuotaProgressBarPreview(
    @PreviewParameter(QuotaProgressPreviewParameterProvider::class) quota: Pair<QuotaLevel, Int>,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MyAccountQuotaProgressBar(
            level = quota.first,
            progress = quota.second,
            progressIndicatorTestTag = "test_tag"
        )
    }
}

private class QuotaProgressPreviewParameterProvider :
    PreviewParameterProvider<Pair<QuotaLevel, Int>> {
    override val values = sequenceOf(
        Pair(QuotaLevel.Success, 50),
        Pair(QuotaLevel.Warning, 90),
        Pair(QuotaLevel.Error, 124),
        Pair(QuotaLevel.Success, 0)
    )
}
