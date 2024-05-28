package mega.privacy.android.app.presentation.meeting.managechathistory.component

import android.os.Build
import android.view.ContextThemeWrapper
import android.widget.NumberPicker
import android.widget.NumberPicker.OnScrollListener.SCROLL_STATE_IDLE
import android.widget.NumberPicker.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@Composable
internal fun NumberPicker(
    modifier: Modifier = Modifier,
    minimumValue: Int = 0,
    maximumValue: Int = 0,
    currentValue: Int = 0,
    displayValues: List<String>? = null,
    isWrapSelectorWheel: Boolean = true,
    selectionDividerHeight: Dp = 1.dp,
    onScrollChange: ((scrollState: NumberPickerScrollState) -> Unit)? = null,
    onValueChange: ((oldValue: Int, newValue: Int) -> Unit)? = null,
) {
    val selectionDividerHeightInPx = with(LocalDensity.current) { selectionDividerHeight.toPx() }
    AndroidView(
        modifier = modifier.testTag(NUMBER_PICKER_TAG),
        factory = { context ->
            NumberPicker(ContextThemeWrapper(context, R.style.Widget_Mega_NumberPicker)).apply {
                setOnScrollListener { _, state ->
                    val numberPickerScrollState = NumberPickerScrollState.getScrollState(state)
                    onScrollChange?.invoke(numberPickerScrollState)
                }

                setOnValueChangedListener { _, oldValue, newValue ->
                    onValueChange?.invoke(oldValue, newValue)
                }
            }
        },
        update = { numberPicker ->
            numberPicker.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.selectionDividerHeight = selectionDividerHeightInPx.toInt()
                }

                wrapSelectorWheel = isWrapSelectorWheel
                minValue = minimumValue
                maxValue = maximumValue
                value = currentValue
                displayedValues = displayValues?.toTypedArray()
            }
        }
    )
}

/**
 * A class to encapsulate the scroll state for [NumberPicker]
 */
internal sealed interface NumberPickerScrollState {

    /**
     * Represents the [android.widget.NumberPicker.OnScrollListener.SCROLL_STATE_IDLE]
     */
    data object Idle : NumberPickerScrollState

    /**
     * Represents the [android.widget.NumberPicker.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL]
     */
    data object TouchScroll : NumberPickerScrollState

    /**
     * Represents the [android.widget.NumberPicker.OnScrollListener.SCROLL_STATE_FLING]
     */
    data object Fling : NumberPickerScrollState

    companion object {
        /**
         * Represents the scroll state from [android.widget.NumberPicker.OnScrollListener] as [NumberPickerScrollState]
         */
        fun getScrollState(from: Int): NumberPickerScrollState =
            when (from) {
                SCROLL_STATE_IDLE -> Idle
                SCROLL_STATE_TOUCH_SCROLL -> TouchScroll
                else -> Fling
            }
    }
}

@CombinedThemePreviews
@Composable
private fun NumberPickerPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        NumberPicker(
            modifier = Modifier.size(500.dp)
        )
    }
}

internal const val NUMBER_PICKER_TAG = "number_picker:root_composable"
