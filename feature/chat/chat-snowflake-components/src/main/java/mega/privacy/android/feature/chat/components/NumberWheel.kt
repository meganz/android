package mega.privacy.android.feature.chat.components

import android.os.Build
import android.widget.NumberPicker
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * A single scrolling wheel of values, backed by the platform [NumberPicker].
 *
 * This lives in the SnowFlake module because [NumberPicker] is a platform View: it cannot read the
 * Compose theme, so its colours have to be resolved from the design tokens and handed in.
 * Without that it renders in the Android theme's colours and is all but invisible on a dark surface.
 *
 * @param minimumValue lowest selectable value.
 * @param maximumValue highest selectable value.
 * @param currentValue value the wheel rests on.
 * @param onValueChange called with the newly selected value.
 * @param onSettled called once the wheel stops scrolling.
 * @param modifier [Modifier]
 * @param displayValues labels to show in place of the raw numbers, in range order.
 */
@Composable
fun NumberWheel(
    minimumValue: Int,
    maximumValue: Int,
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    onSettled: () -> Unit,
    modifier: Modifier = Modifier,
    displayValues: List<String>? = null,
) {
    val wheelTextSizeInPx = with(LocalDensity.current) { 20.sp.toPx() }
    val wheelTextColor = DSTokens.colors.text.primary.toArgb()
    val dividerColor = DSTokens.colors.border.strong
    // The setters below are API 29+; on 28 the platform text and divider stand.
    val styleWheel = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    AndroidView(
        modifier = modifier.drawWithContent {
            drawContent()
            // NumberPicker has no divider colour setter, so its own divider is hidden and these
            // are drawn where it would sit: either side of the centre row, 48dp apart.
            if (styleWheel) {
                val dividerHeight = 1.dp.toPx()
                val dividerSize = Size(size.width, dividerHeight)
                val selectedRowHeight = SELECTED_ROW_HEIGHT.toPx()
                val topDividerTop = (size.height - selectedRowHeight) / 2 - dividerHeight
                drawRect(dividerColor, Offset(0f, topDividerTop), dividerSize)
                drawRect(
                    dividerColor,
                    Offset(0f, topDividerTop + dividerHeight + selectedRowHeight),
                    dividerSize,
                )
            }
        },
        factory = { context ->
            NumberPicker(context).apply {
                background = null
                setOnScrollListener { _, state ->
                    if (state == NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                        onSettled()
                    }
                }
                setOnValueChangedListener { _, _, newValue -> onValueChange(newValue) }
            }
        },
        update = { picker ->
            picker.apply {
                if (styleWheel) {
                    selectionDividerHeight = 0
                    textSize = wheelTextSizeInPx
                    textColor = wheelTextColor
                }
                wrapSelectorWheel = true
                // The displayed values must be cleared before the range shrinks, otherwise the
                // picker indexes past the end of the old array and throws.
                displayedValues = null
                minValue = minimumValue
                maxValue = maximumValue
                value = currentValue
                displayedValues = displayValues?.toTypedArray()
            }
        },
    )
}

/** Matches the platform's selectionDividersDistance in Widget.Material.NumberPicker. */
private val SELECTED_ROW_HEIGHT = 48.dp
