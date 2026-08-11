package mega.privacy.android.core.test.weblate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.tooltip.direction.TooltipDirection
import mega.android.core.ui.components.tooltip.model.TooltipPointerSizeInPx
import mega.android.core.ui.components.tooltip.popup.simple.SimpleTooltipContentBodyOnly
import mega.android.core.ui.components.tooltip.shape.MegaTooltipShape
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.tokens.theme.DSTokens

const val WEBLATE_SNACKBAR_TAG = "weblate_snackbar:snackbar"
const val WEBLATE_CONTENT_DESCRIPTION_TOOLTIP_TAG = "weblate_content_description:tooltip"

/**
 * Renders [screenContent] with a MEGA-styled snackbar pinned to the bottom, for Weblate
 * screenshot tests of snackbar/toast strings.
 *
 * `MegaSnackbar` cannot be captured in a static preview: it renders through a
 * [androidx.compose.material3.SnackbarHost] that shows nothing until a `showSnackbar`
 * call animates it in. This helper reproduces its styling on a plain slot-based
 * [Snackbar] so the message is visible in a recorded golden.
 *
 * Must be wrapped in `AndroidThemeForPreviews` — `DSTokens` colors only resolve inside
 * the MEGA theme.
 *
 * @param snackbarText the snackbar message (the string under translation)
 * @param actionLabel optional action label shown next to the message
 * @param screenContent the screen on which the snackbar appears, for translator context
 */
@Composable
fun WeblateSnackbarScreenshot(
    snackbarText: String,
    actionLabel: String? = null,
    screenContent: @Composable () -> Unit = {},
) {
    Box {
        screenContent()
        Snackbar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = LocalSpacing.current.x8,
                    end = LocalSpacing.current.x8,
                    bottom = LocalSpacing.current.x12,
                )
                .testTag(WEBLATE_SNACKBAR_TAG),
            action = actionLabel?.let {
                {
                    Text(
                        text = it,
                        modifier = Modifier.padding(horizontal = LocalSpacing.current.x8),
                    )
                }
            },
            containerColor = DSTokens.colors.components.toastBackground,
            contentColor = DSTokens.colors.text.inverse,
            actionContentColor = DSTokens.colors.link.inverse,
        ) {
            Text(text = snackbarText)
        }
    }
}

/**
 * Renders [screenContent] with a MEGA tooltip bubble overlaid on top showing [description],
 * for Weblate screenshot tests of content-description strings, which have no visual of
 * their own. The whole screen stays visible so translators see which control the text
 * describes and where it lives.
 *
 * The core-ui tooltip composables render inside a [androidx.compose.ui.window.Popup], a
 * separate window the screenshot engine cannot capture. This helper assembles the public
 * visual pieces ([MegaTooltipShape] + [SimpleTooltipContentBodyOnly]) directly in the
 * hierarchy instead.
 *
 * Position the bubble under the described control with [tooltipAlignment] plus
 * [tooltipOffset]; the caret points upward from the bubble's start/centre/end edge
 * following the alignment's horizontal bias. Record once, view the golden, and nudge the
 * offset until the caret sits under the control.
 *
 * Must be wrapped in `AndroidThemeForPreviews` — `DSTokens` colors only resolve inside
 * the MEGA theme.
 *
 * @param description the content description (the string under translation)
 * @param tooltipAlignment where in the screen the bubble is placed (e.g.
 * [Alignment.TopEnd] for a top-app-bar action icon)
 * @param tooltipOffset nudge applied after alignment so the caret lines up with the control
 * @param screenContent the screen containing the described control
 */
@Composable
fun WeblateContentDescriptionScreenshot(
    description: String,
    tooltipAlignment: Alignment = Alignment.TopCenter,
    tooltipOffset: DpOffset = DpOffset.Zero,
    screenContent: @Composable () -> Unit,
) {
    // Values mirror core-ui's TooltipSizeDefaults, which is internal to the library
    val pointerHeight = 11.dp
    val direction = when (tooltipAlignment) {
        Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart -> TooltipDirection.Top.Left
        Alignment.TopEnd, Alignment.CenterEnd, Alignment.BottomEnd -> TooltipDirection.Top.Right
        else -> TooltipDirection.Top.Centre
    }
    val shape = with(LocalDensity.current) {
        MegaTooltipShape(
            radius = 8.dp.toPx(),
            pointerSize = TooltipPointerSizeInPx(
                width = 18.dp.toPx(),
                height = pointerHeight.toPx(),
            ),
            direction = direction,
        )
    }
    Box {
        screenContent()
        Box(
            modifier = Modifier
                .align(tooltipAlignment)
                .offset(x = tooltipOffset.x, y = tooltipOffset.y)
                .background(color = DSTokens.colors.background.inverse, shape = shape)
                .padding(top = pointerHeight)
                .testTag(WEBLATE_CONTENT_DESCRIPTION_TOOLTIP_TAG),
        ) {
            SimpleTooltipContentBodyOnly(description).Content()
        }
    }
}
