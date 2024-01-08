package mega.privacy.android.core.ui.controls.dividers

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * MegaDivider
 *
 * @param dividerSpacing [DividerSpacing]
 * @param modifier [Modifier]
 */
@Composable
fun MegaDivider(
    dividerSpacing: DividerSpacing,
    modifier: Modifier = Modifier,
) {
    val finalModifier = when (dividerSpacing) {
        DividerSpacing.Full -> modifier.fillMaxWidth()
        DividerSpacing.StartSmall -> modifier.padding(start = 16.dp)
        DividerSpacing.StartBig -> modifier.padding(start = 72.dp)
        DividerSpacing.Center -> modifier.padding(horizontal = 16.dp)
    }
    Divider(
        modifier = finalModifier,
        thickness = 1.dp,
        color = MegaTheme.colors.border.disabled
    )
}

/**
 * Spacing for divider
 */
enum class DividerSpacing {
    /**
     * Length of divider in full width
     */
    Full,

    /**
     * Length of divider beginning from small padding
     */
    StartSmall,

    /**
     * Length of divider beginning from big padding
     */
    StartBig,

    /**
     * Length of divider beginning and ending from small padding
     */
    Center
}


@CombinedThemePreviews
@Composable
private fun PreviewMegaDivider(
    @PreviewParameter(DividerSpacingProvider::class) spacing: DividerSpacing,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaDivider(
            dividerSpacing = spacing,
        )
    }
}

private class DividerSpacingProvider : PreviewParameterProvider<DividerSpacing> {
    override val values = listOf(
        DividerSpacing.Full,
        DividerSpacing.StartSmall,
        DividerSpacing.StartBig,
        DividerSpacing.Center,
    ).asSequence()
}
