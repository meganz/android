@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package mega.privacy.android.core.ui.controls.tooltips


import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuPositionProvider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.body2medium


/**
 * Tooltip implementation for AndroidX Jetpack Compose.
 * Based on material [DropdownMenu] implementation
 *
 * A [Tooltip] behaves similarly to a [Popup], and will use the position of the parent layout
 * to position itself on screen. Commonly a [Tooltip] will be placed in a [Box] with a sibling
 * that will be used as the 'anchor'. Note that a [Tooltip] by itself will not take up any
 * space in a layout, as the tooltip is displayed in a separate window, on top of other content.
 *
 * [Tooltip] changes its positioning depending on the available space, always trying to be
 * fully visible. It will try to expand horizontally, depending on layout direction, to the end of
 * its parent, then to the start of its parent, and then screen end-aligned. Vertically, it will
 * try to expand to the bottom of its parent, then from the top of its parent, and then screen
 * top-aligned.
 *
 * @param expanded Whether the tooltip is currently visible to the user
 * @param text The text to be shown
 *
 * @see androidx.compose.material.DropdownMenu
 * @see androidx.compose.material.DropdownMenuPositionProvider
 * @see androidx.compose.ui.window.Popup
 *
 * @author based on Artyom Krivolapov solution (https://gist.github.com/amal/aad53791308e6edb055f3cf61f881451)
 */
@Composable
fun Tooltip(
    expanded: MutableState<Boolean>,
    text: String,
) = Tooltip(expanded) {
    TooltipText(
        text = text,
    )
}

/**
 * @param expanded Whether the tooltip is currently visible to the user
 * @param text The text to be shown
 * @param alignment to position the tooltip instead of default [DropdownMenuPositionProvider]
 * @param offset
 */
@Composable
fun Tooltip(
    expanded: MutableState<Boolean>,
    text: String,
    alignment: Alignment,
    offset: IntOffset? = null,
) = Tooltip(expanded, alignment = alignment, intOffset = offset) {
    TooltipText(
        text = text,
    )
}

@Composable
internal fun Tooltip(
    expanded: MutableState<Boolean>,
    timeoutMillis: Long = TOOLTIP_TIMEOUT,
    properties: PopupProperties = PopupProperties(focusable = false),
    alignment: Alignment? = null,
    intOffset: IntOffset? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val expandedStates = remember { MutableTransitionState(false) }
    expandedStates.targetState = expanded.value

    if (expandedStates.currentState || expandedStates.targetState) {
        if (expandedStates.isIdle) {
            LaunchedEffect(timeoutMillis, expanded) {
                delay(timeoutMillis)
                expanded.value = false
            }
        }
        if (alignment == null) {
            val offset = DpOffset(0.dp, 0.dp)
            Popup(
                onDismissRequest = { expanded.value = false },
                popupPositionProvider = DropdownMenuPositionProvider(offset, LocalDensity.current),
                properties = properties,
            ) {
                PopupContent(expandedStates, content)
            }
        } else {
            Popup(
                alignment = alignment,
                offset = intOffset ?: IntOffset(0, 0),
                onDismissRequest = { expanded.value = false },
                properties = properties,
            ) {
                PopupContent(expandedStates, content)
            }
        }
    }
}

@Composable
private fun PopupContent(
    expandedStates: MutableTransitionState<Boolean>,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        // Add space for elevation shadow
        modifier = Modifier.padding(TOOLTIP_ELEVATION.dp),
    ) {
        TooltipContent(expandedStates, MegaTheme.colors.icon.primary, content)
    }
}

@Composable
private fun TooltipText(text: String) =
    Text(
        text = text,
        color = MegaTheme.colors.text.inverse,
        style = MaterialTheme.typography.body2medium,
    )


/** @see androidx.compose.material.DropdownMenuContent */
@Composable
private fun TooltipContent(
    expandedStates: MutableTransitionState<Boolean>,
    backgroundColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Tooltip open/close animation.
    val transition = updateTransition(expandedStates, "Tooltip")

    val alpha by transition.animateFloat(
        label = "alpha",
        transitionSpec = {
            if (false isTransitioningTo true) {
                // Dismissed to expanded
                tween(durationMillis = IN_TRANSITION_DURATION)
            } else {
                // Expanded to dismissed.
                tween(durationMillis = OUT_TRANSITION_DURATION)
            }
        }
    ) { if (it) 1f else 0f }

    Card(
        backgroundColor = backgroundColor,
        contentColor = MegaTheme.colors.text.inverse,
        modifier = Modifier.alpha(alpha),
        elevation = TOOLTIP_ELEVATION.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = TOOLTIP_PADDING_H.dp, vertical = TOOLTIP_PADDING_V.dp)
                .width(IntrinsicSize.Max),
            content = content,
        )
    }
}

private const val TOOLTIP_ELEVATION = 0
private const val TOOLTIP_PADDING_H = 12
private const val TOOLTIP_PADDING_V = 8

// Tooltip open/close animation duration.
private const val IN_TRANSITION_DURATION = 64
private const val OUT_TRANSITION_DURATION = 240

// Default timeout before tooltip close
private const val TOOLTIP_TIMEOUT = 2_000L - OUT_TRANSITION_DURATION