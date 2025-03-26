package mega.privacy.android.shared.original.core.ui.controls.chip

import android.content.Context
import android.util.AttributeSet
import android.view.View
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.content.withStyledAttributes
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeComponentPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * A custom wrapper view for the `MegaChip` composable, extending `AbstractComposeView`.
 *
 * This class allows the use of the Jetpack Compose-based `MegaChip` component in traditional
 * Android XML layouts. It provides various properties to configure the chip's appearance and behavior,
 * such as text, selection state, icons, and custom style.
 *
 */
class MegaChip : AbstractComposeView {
    /**
     * whether the chip is selected or not
     */
    @get:JvmName("isSelectedKt")
    @set:JvmName("setSelectedKt")
    var selected by mutableStateOf(false)

    /**
     * whether the chip is enabled or not
     */
    @get:JvmName("isEnabledKt")
    @set:JvmName("setEnabledKt")
    var enabled by mutableStateOf(true)

    /**
     * callback when the chip is clicked
     */
    var onClick by mutableStateOf({})

    /**
     * text of the chip
     */
    var text by mutableStateOf("")

    /**
     * leading icon of the chip
     */
    var leadingIcon by mutableStateOf<Int?>(null)

    /**
     * trailing icon of the chip
     */
    var trailingIcon by mutableStateOf<Int?>(null)

    /**
     * style of the chip
     */
    var chipStyle: ChipStyle = DefaultChipStyle


    /**
     * overridden getter to be sure it's not used by mistake or from java code
     */
    override fun isEnabled() = enabled

    /**
     * overridden setter to be sure it's not used by mistake or from java code
     */
    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    /**
     * overridden getter to be sure it's not used by mistake or from java code
     */
    override fun isSelected() = selected

    /**
     * overridden setter to be sure it's not used by mistake or from java code
     */
    override fun setSelected(enabled: Boolean) {
        this.selected = selected
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        context.withStyledAttributes(attrs, R.styleable.MegaChip) {
            selected = getBoolean(R.styleable.MegaChip_mega_chip_selected, selected)
            enabled = getBoolean(R.styleable.MegaChip_mega_chip_enabled, enabled)
            text = getString(R.styleable.MegaChip_mega_chip_text) ?: text
            chipStyle = when (getInt(R.styleable.MegaChip_mega_chip_style, 0)) {
                0 -> DefaultChipStyle
                1 -> TransparentChipStyle
                2 -> RoundedChipStyle
                3 -> TagChipStyle
                else -> DefaultChipStyle
            }
            leadingIcon = getResourceId(R.styleable.MegaChip_mega_chip_leading_icon, 0)
                .takeIf { it != 0 }
            trailingIcon =
                getResourceId(R.styleable.MegaChip_mega_chip_trailing_icon, 0).takeIf { it != 0 }
        }
    }

    /**
     * Content
     */
    @Composable
    override fun Content() {
        OriginalTheme(isDark = isSystemInDarkTheme()) {
            MegaChip(
                selected = selected,
                text = text,
                style = chipStyle,
                enabled = enabled,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                onClick = onClick
            )
        }
    }


    /**
     * Set the click listener for the chip
     */
    fun setOnClickListener(listener: (View) -> Unit) {
        super.setOnClickListener(listener)
        this.onClick = { listener(this) }
    }
}

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
                    .contentColor(enabled = enabled, selected = selected).value,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = text,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@CombinedThemeComponentPreviews
@Composable
private fun ChipPreview(
    @PreviewParameter(BooleanProvider::class) selected: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaChip(
            selected = selected,
            text = "Type",
        )
    }
}

@CombinedThemeComponentPreviews
@Composable
private fun ChipPreviewWithLeadAndTrail(
    @PreviewParameter(BooleanProvider::class) selected: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
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

@CombinedThemeComponentPreviews
@Composable
private fun ChipPreviewWithStyles(
    @PreviewParameter(ChipStyleProvider::class) chipStyleAndName: Pair<ChipStyle, String>,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        var selected by remember { mutableStateOf(true) }
        MegaChip(
            selected = selected,
            text = chipStyleAndName.second,
            style = chipStyleAndName.first,
        ) {
            selected = selected.not()
        }
    }
}
