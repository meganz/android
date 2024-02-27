package mega.privacy.android.core.ui.controls.chip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme

/**
 * Chip to filter lists based on user interaction
 *
 * @param selected if chip is selected or not
 * @param contentDescription accessibility text
 * @param modifier optional modifier
 * @param style style of chip
 * @param onClick callback this chip is clicked
 * @param enabled if chip is enabled or grayed out
 * @param onClickLabel optional label after chip is clicked
 * @param content content of chip
 */
@Composable
fun Chip(
    selected: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    style: ChipStyle = DefaultChipStyle,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    onClickLabel: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val backgroundColor = style.colors().backgroundColor(
        selected = selected,
        enabled = enabled
    ).value

    val borderStroke = BorderStroke(
        width = 1.dp,
        color = style
            .colors()
            .borderColor(
                selected = selected,
                enabled = enabled
            ).value
    )

    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onClick,
            )
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                this.onClick(label = onClickLabel) { onClick(); true }
            },
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = borderStroke,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterHorizontally
            )
        ) {
            ProvideTextStyle(
                value = style.typography(),
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides style.colors()
                        .contentColor(
                            selected = selected,
                            enabled = enabled
                        ).value,
                ) {
                    content()
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ChipPreview(
    @PreviewParameter(BooleanProvider::class) selected: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        Chip(selected = selected, contentDescription = "") {
            Text(
                text = "Type"
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ChipWithIconPreview(
    @PreviewParameter(BooleanProvider::class) selected: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        Chip(selected = selected, contentDescription = "") {
            Text(
                text = "Type"
            )
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_chevron_down),
                contentDescription = "See more",
            )
        }
    }
}
