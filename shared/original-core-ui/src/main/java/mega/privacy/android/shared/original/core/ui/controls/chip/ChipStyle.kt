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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.original.core.ui.theme.extensions.body4
import mega.privacy.android.shared.original.core.ui.theme.extensions.body4Bold
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle2medium


/**
 * Definition of chips
 */
sealed interface ChipStyle {

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

    /**
     * Height of chip
     */
    fun height(): Dp? = null
}

/**
 * Default style for chips
 */
data object DefaultChipStyle : ChipStyle {

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun selectableChipColors(): SelectableChipColors = ChipDefaults.filterChipColors(
        selectedBackgroundColor = DSTokens.colors.components.selectionControl,
        selectedContentColor = DSTokens.colors.text.inverse,
        selectedLeadingIconColor = DSTokens.colors.icon.inverse,
        backgroundColor = DSTokens.colors.button.secondary,
        contentColor = DSTokens.colors.text.secondary,
        leadingIconColor = DSTokens.colors.icon.inverse,
        disabledBackgroundColor = DSTokens.colors.button.secondary,
        disabledContentColor = DSTokens.colors.text.secondary,
        disabledLeadingIconColor = DSTokens.colors.icon.secondary,
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
data object TransparentChipStyle : ChipStyle {

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun selectableChipColors(): SelectableChipColors = ChipDefaults.filterChipColors(
        selectedBackgroundColor = Color.Transparent,
        selectedContentColor = DSTokens.colors.text.primary,
        selectedLeadingIconColor = DSTokens.colors.icon.primary,
        backgroundColor = Color.Transparent,
        contentColor = DSTokens.colors.text.primary,
        leadingIconColor = DSTokens.colors.icon.primary,
        disabledBackgroundColor = Color.Transparent,
        disabledContentColor = DSTokens.colors.text.primary,
        disabledLeadingIconColor = DSTokens.colors.icon.primary,
    )

    @Composable
    override fun typography(): TextStyle = MaterialTheme.typography.subtitle2medium

    @Composable
    override fun borderStyle() = BorderStroke(1.dp, DSTokens.colors.border.strong)
}

/**
 * Rounded style for chips
 */
data object RoundedChipStyle : ChipStyle {

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun selectableChipColors(): SelectableChipColors = ChipDefaults.filterChipColors(
        selectedBackgroundColor = DSTokens.colors.components.selectionControl,
        selectedContentColor = DSTokens.colors.text.inverse,
        selectedLeadingIconColor = DSTokens.colors.icon.inverse,
        backgroundColor = DSTokens.colors.button.secondary,
        contentColor = DSTokens.colors.text.primary,
        leadingIconColor = DSTokens.colors.icon.primary,
        disabledBackgroundColor = DSTokens.colors.button.secondary,
        disabledContentColor = DSTokens.colors.text.secondary,
        disabledLeadingIconColor = DSTokens.colors.icon.secondary,
    )

    @Composable
    override fun borderStyle() = BorderStroke(1.dp, DSTokens.colors.border.strong)

    @Composable
    override fun typography(): TextStyle = MaterialTheme.typography.subtitle2medium

    override fun shape(): Shape = RoundedCornerShape(18.dp)
    override fun height() = 36.dp
}

/**
 * Tag chip style
 */
data object TagChipStyle : ChipStyle {
    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun selectableChipColors(): SelectableChipColors = ChipDefaults.filterChipColors(
        selectedBackgroundColor = DSTokens.colors.button.brand,
        selectedContentColor = DSTokens.colors.text.inverse,
        selectedLeadingIconColor = DSTokens.colors.icon.inverse,
        backgroundColor = DSTokens.colors.button.secondary,
        contentColor = DSTokens.colors.text.secondary,
        leadingIconColor = DSTokens.colors.icon.inverse,
        disabledBackgroundColor = DSTokens.colors.button.secondary,
        disabledContentColor = DSTokens.colors.text.secondary,
        disabledLeadingIconColor = DSTokens.colors.icon.secondary,
    )

    @Composable
    override fun typography(): TextStyle = MaterialTheme.typography.button

    override fun shape(): Shape = RoundedCornerShape(4.dp)

    override fun height() = 24.dp
}

/**
 * Notification chip style
 */
sealed class NotificationChipStyle : ChipStyle {

    override fun shape(): Shape = RoundedCornerShape(12.dp)

    override fun height() = 24.dp

    @Composable
    internal abstract fun backgroundColor(): Color

    @Composable
    internal abstract fun foregroundColor(): Color

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun selectableChipColors(): SelectableChipColors {
        val backgroundColor = backgroundColor()
        val foregroundColor = foregroundColor()
        return ChipDefaults.filterChipColors(
            selectedBackgroundColor = backgroundColor,
            backgroundColor = backgroundColor,
            disabledBackgroundColor = backgroundColor,
            selectedContentColor = foregroundColor,
            selectedLeadingIconColor = foregroundColor,
            contentColor = foregroundColor,
            leadingIconColor = foregroundColor,
            disabledContentColor = foregroundColor,
            disabledLeadingIconColor = foregroundColor,
        )
    }

    /**
     * Success notification chip style
     */
    data object Success : NotificationChipStyle() {
        @Composable
        override fun backgroundColor() = DSTokens.colors.notifications.notificationSuccess

        @Composable
        override fun foregroundColor() = DSTokens.colors.text.success

        @Composable
        override fun typography(): TextStyle = MaterialTheme.typography.body4
    }

    /**
     * Info notification chip style
     */
    data object Info : NotificationChipStyle() {
        @Composable
        override fun backgroundColor() = DSTokens.colors.notifications.notificationInfo

        @Composable
        override fun foregroundColor() = DSTokens.colors.text.info

        @Composable
        override fun typography(): TextStyle = MaterialTheme.typography.body4Bold
    }

    /**
     * Info notification chip style
     */
    data object Warning : NotificationChipStyle() {
        @Composable
        override fun backgroundColor() = DSTokens.colors.notifications.notificationWarning

        @Composable
        override fun foregroundColor() = DSTokens.colors.text.warning

        @Composable
        override fun typography(): TextStyle = MaterialTheme.typography.body4Bold
    }
}
