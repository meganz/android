package mega.privacy.android.core.ui.controls.chip

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Chip colors
 */
@Stable
interface ChipColors {

    /**
     * Background (filling) color of chip
     * @param selected if chip is selected
     * @param enabled if chip is enabled
     *
     * @return background color as [State]
     */
    @Composable
    fun backgroundColor(selected: Boolean, enabled: Boolean): State<Color>

    /**
     * Content (inside chip) color of chip
     * @param selected if chip is selected
     * @param enabled if chip is enabled
     *
     * @return content color as [State]
     */
    @Composable
    fun contentColor(selected: Boolean, enabled: Boolean): State<Color>

    /**
     * Border color of chip
     * @param selected if chip is selected
     * @param enabled if chip is enabled
     *
     * @return border color as [State]
     */
    @Composable
    fun borderColor(selected: Boolean, enabled: Boolean): State<Color>
}

/**
 * Default chip colors
 * @property selectedBackgroundColor color of background when selected
 * @property unselectedBackgroundColor color of background when unselected
 * @property disabledBackgroundColor color of background when disabled
 * @property selectedContentColor color of content when selected
 * @property unselectedContentColor color of content when unselected
 * @property disabledContentColor color of content when disabled
 * @property selectedBorderColor color of border when selected
 * @property unselectedBorderColor color of border when unselected
 * @property disabledBorderColor color of border when disabled
 */
@Immutable
class DefaultChipColors(
    val selectedBackgroundColor: Color,
    val unselectedBackgroundColor: Color,
    val disabledBackgroundColor: Color,
    val selectedContentColor: Color,
    val unselectedContentColor: Color,
    val disabledContentColor: Color,
    val selectedBorderColor: Color,
    val unselectedBorderColor: Color,
    val disabledBorderColor: Color,
) : ChipColors {

    @Composable
    override fun backgroundColor(selected: Boolean, enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (!enabled) {
                disabledBackgroundColor
            } else {
                if (selected) {
                    selectedBackgroundColor
                } else {
                    unselectedBackgroundColor
                }
            }
        )
    }

    @Composable
    override fun contentColor(selected: Boolean, enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (!enabled) {
                disabledContentColor
            } else {
                if (selected) {
                    selectedContentColor
                } else {
                    unselectedContentColor
                }
            }
        )
    }

    @Composable
    override fun borderColor(selected: Boolean, enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (!enabled) {
                disabledBorderColor
            } else {
                if (selected) {
                    selectedBorderColor
                } else {
                    unselectedBorderColor
                }
            }
        )
    }
}

/**
 * Definition of chips
 */
interface ChipStyle {

    /**
     * Colors of chip
     *
     * @return [ChipColors]
     */
    @Composable
    fun colors(): ChipColors

    /**
     * Typography of chip
     *
     * @return typography [TextStyle]
     */
    @Composable
    fun typography(): TextStyle
}

/**
 * Default style for chips
 */
object DefaultChipStyle : ChipStyle {

    /**
     * Colors of chip
     */
    @Composable
    override fun colors(): ChipColors = DefaultChipColors(
        selectedBackgroundColor = MegaTheme.colors.components.selectionControl,
        unselectedBackgroundColor = MegaTheme.colors.button.secondary,
        disabledBackgroundColor = MegaTheme.colors.button.secondary,
        selectedContentColor = MegaTheme.colors.text.inverse,
        unselectedContentColor = MegaTheme.colors.text.secondary,
        disabledContentColor = MegaTheme.colors.text.secondary,
        selectedBorderColor = Color.Transparent,
        unselectedBorderColor = Color.Transparent,
        disabledBorderColor = Color.Transparent,
    )

    /**
     * Typography style of chip
     */
    @Composable
    override fun typography(): TextStyle = MaterialTheme.typography.subtitle2
}
