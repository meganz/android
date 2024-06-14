package mega.privacy.android.shared.original.core.ui.controls.chip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle2medium


/**
 * Definition of chips
 */
interface ChipStyle {

    /**
     * Colors of selectable chip
     */
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun selectableChipColors(): SelectableChipColors

    /**
     * Typography of chip
     *
     * @return typography [TextStyle]
     */
    @Composable
    fun typography(): TextStyle

    /**
     * Border style of chip
     *
     * @return [BorderStroke] or null if no border
     */
    @Composable
    fun borderStyle(): BorderStroke = BorderStroke(1.dp, Color.Transparent)

    /**
     * Shape of chip
     *
     * @return [Shape]
     */
    fun shape(): Shape = RoundedCornerShape(8.dp)
}

/**
 * Default style for chips
 */
object DefaultChipStyle : ChipStyle {

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun selectableChipColors(): SelectableChipColors = ChipDefaults.filterChipColors(
        selectedBackgroundColor = MegaOriginalTheme.colors.components.selectionControl,
        selectedContentColor = MegaOriginalTheme.colors.text.inverse,
        selectedLeadingIconColor = MegaOriginalTheme.colors.icon.inverse,
        backgroundColor = MegaOriginalTheme.colors.button.secondary,
        contentColor = MegaOriginalTheme.colors.text.secondary,
        leadingIconColor = MegaOriginalTheme.colors.icon.inverse,
        disabledBackgroundColor = MegaOriginalTheme.colors.button.secondary,
        disabledContentColor = MegaOriginalTheme.colors.text.secondary,
        disabledLeadingIconColor = MegaOriginalTheme.colors.icon.secondary,
    )

    /**
     * Typography style of chip
     */
    @Composable
    override fun typography(): TextStyle = MaterialTheme.typography.subtitle2medium
}

/**
 * Transparent style for chips
 */
object TransparentChipStyle : ChipStyle {

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun selectableChipColors(): SelectableChipColors = ChipDefaults.filterChipColors(
        selectedBackgroundColor = Color.Transparent,
        selectedContentColor = MegaOriginalTheme.colors.text.primary,
        selectedLeadingIconColor = MegaOriginalTheme.colors.icon.primary,
        backgroundColor = Color.Transparent,
        contentColor = MegaOriginalTheme.colors.text.primary,
        leadingIconColor = MegaOriginalTheme.colors.icon.primary,
        disabledBackgroundColor = Color.Transparent,
        disabledContentColor = MegaOriginalTheme.colors.text.primary,
        disabledLeadingIconColor = MegaOriginalTheme.colors.icon.primary,
    )

    @Composable
    override fun typography(): TextStyle = MaterialTheme.typography.subtitle2medium

    @Composable
    override fun borderStyle() = BorderStroke(1.dp, MegaOriginalTheme.colors.border.strong)
}

/**
 * Rounded style for chips
 */
object RoundedChipStyle : ChipStyle {

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun selectableChipColors(): SelectableChipColors = ChipDefaults.filterChipColors(
        selectedBackgroundColor = MegaOriginalTheme.colors.components.selectionControl,
        selectedContentColor = MegaOriginalTheme.colors.text.inverse,
        selectedLeadingIconColor = MegaOriginalTheme.colors.icon.inverse,
        backgroundColor = MegaOriginalTheme.colors.button.secondary,
        contentColor = MegaOriginalTheme.colors.text.secondary,
        leadingIconColor = MegaOriginalTheme.colors.icon.inverse,
        disabledBackgroundColor = MegaOriginalTheme.colors.button.secondary,
        disabledContentColor = MegaOriginalTheme.colors.text.secondary,
        disabledLeadingIconColor = MegaOriginalTheme.colors.icon.secondary,
    )

    @Composable
    override fun typography(): TextStyle = MaterialTheme.typography.subtitle2medium

    override fun shape(): Shape = RoundedCornerShape(18.dp)
}
