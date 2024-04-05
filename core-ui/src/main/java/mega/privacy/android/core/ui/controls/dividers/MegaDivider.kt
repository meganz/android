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
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * MegaDivider
 *
 * @param dividerType [DividerType]
 * @param modifier [Modifier]
 */
@Composable
fun MegaDivider(
    dividerType: DividerType,
    modifier: Modifier = Modifier,
) {
    val finalModifier = when (dividerType) {
        DividerType.FullSize -> modifier.fillMaxWidth()
        DividerType.SmallStartPadding -> modifier.padding(start = 16.dp)
        DividerType.BigStartPadding -> modifier.padding(start = 72.dp)
        DividerType.Centered -> modifier.padding(horizontal = 16.dp)
    }
    Divider(
        modifier = finalModifier,
        thickness = 1.dp,
        color = MegaTheme.colors.border.subtle
    )
}

/**
 * Type of divider
 */
enum class DividerType {
    /**
     * A full width divider
     */
    FullSize,

    /**
     * A divider with a small start padding. Default is 16.dp
     */
    SmallStartPadding,

    /**
     * A divider with a big start padding. Default is 72.dp
     */
    BigStartPadding,

    /**
     * A divider with horizontal padding. Default is 16.dp
     */
    Centered
}


@CombinedThemePreviews
@Composable
private fun PreviewMegaDivider(
    @PreviewParameter(DividerTypeProvider::class) spacing: DividerType,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaDivider(
            dividerType = spacing,
        )
    }
}

private class DividerTypeProvider : PreviewParameterProvider<DividerType> {
    override val values = listOf(
        DividerType.FullSize,
        DividerType.SmallStartPadding,
        DividerType.BigStartPadding,
        DividerType.Centered,
    ).asSequence()
}
