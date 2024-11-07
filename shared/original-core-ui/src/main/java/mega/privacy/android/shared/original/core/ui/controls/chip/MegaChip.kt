package mega.privacy.android.shared.original.core.ui.controls.chip

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Chip to filter lists based on user interaction
 *
 * @param selected if chip is selected or not
 * @param text text of chip
 * @param modifier optional modifier
 * @param style style of chip
 * @param onClick callback this chip is clicked
 * @param enabled if chip is enabled or grayed out
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MegaChip(
    selected: Boolean,
    text: String,
    modifier: Modifier = Modifier,
    style: ChipStyle = DefaultChipStyle,
    enabled: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
    @DrawableRes trailingIcon: Int? = null,
    onClick: () -> Unit = {},
) {
    FilterChip(
        modifier = modifier
            .clearAndSetSemantics {
                this.contentDescription = text
            }
            .composed {
                style
                    .height()
                    ?.let { height(it) }
                    ?: Modifier
            },
        selected = selected,
        enabled = enabled,
        onClick = { onClick() },
        colors = style.selectableChipColors(),
        border = style.borderStyle(),
        leadingIcon = leadingIcon?.let {
            {
                CompositionLocalProvider(
                    LocalContentColor provides style.selectableChipColors()
                        .leadingIconColor(selected, enabled).value
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .size(18.dp),
                        imageVector = ImageVector.vectorResource(id = it),
                        contentDescription = "Leading icon",
                    )
                }
            }
        },
        trailingIcon = trailingIcon?.let {
            {
                CompositionLocalProvider(
                    LocalContentColor provides style.selectableChipColors()
                        .leadingIconColor(selected, enabled).value
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = ImageVector.vectorResource(id = it),
                        contentDescription = "Trailing icon",
                    )
                }
            }
        },
        shape = style.shape()
    ) {
        ProvideTextStyle(
            value = style.typography(),
        ) {
            CompositionLocalProvider(
                LocalContentColor provides style.selectableChipColors()
                    .contentColor(selected, enabled).value,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = text,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ChipPreview(
    @PreviewParameter(BooleanProvider::class) selected: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MegaChip(
            selected = selected,
            text = "Type",
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChipPreviewWithLeadAndTrail(
    @PreviewParameter(BooleanProvider::class) selected: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MegaChip(
            selected = selected,
            text = "Type",
            leadingIcon = R.drawable.ic_chevron_down,
            trailingIcon = R.drawable.ic_icon_chevron_left_medium_regular_outline,
        )
    }
}

private class ChipStyleProvider : PreviewParameterProvider<Pair<ChipStyle, String>> {
    override val values = listOf(
        DefaultChipStyle to "Default",
        TransparentChipStyle to "Transparent",
        RoundedChipStyle to "Rounded",
        TagChipStyle to "Tag"
    ).asSequence()
}

@CombinedThemePreviews
@Composable
private fun ChipPreviewWithStyles(
    @PreviewParameter(ChipStyleProvider::class) chipStyleAndName: Pair<ChipStyle, String>,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        var selected = remember { mutableStateOf(true) }
        MegaChip(
            selected = selected.value,
            text = chipStyleAndName.second,
            style = chipStyleAndName.first,
        ) {
            selected.value = selected.value.not()
        }
    }
}
