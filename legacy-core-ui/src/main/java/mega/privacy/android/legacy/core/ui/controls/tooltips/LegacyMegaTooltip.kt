package mega.privacy.android.legacy.core.ui.controls.tooltips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.skydoves.balloon.ArrowOrientationRules
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.compose.Balloon
import com.skydoves.balloon.compose.BalloonWindow
import com.skydoves.balloon.compose.rememberBalloonBuilder
import com.skydoves.balloon.compose.setBackgroundColor
import mega.privacy.android.legacy.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.theme.extensions.body2medium
import mega.privacy.android.core.ui.theme.extensions.dark_blue_tooltip_white
import mega.privacy.android.core.ui.theme.extensions.white_black

/**
 * Mega tooltip Balloon
 *
 * @param modifier
 * @param titleText
 * @param descriptionText
 * @param actionText
 * @param showOnTop
 * @param onDismissed
 * @param content
 */
@Composable
fun LegacyMegaTooltip(
    modifier: Modifier = Modifier,
    titleText: String,
    descriptionText: String,
    actionText: String,
    showOnTop: Boolean,
    onDismissed: () -> Unit,
    content: @Composable () -> Unit,
) {
    var window: BalloonWindow? by remember { mutableStateOf(null) }
    val bgColor = MaterialTheme.colors.dark_blue_tooltip_white
    val builder = rememberBalloonBuilder {
        setIsVisibleOverlay(false)
        setDismissWhenTouchOutside(false)
        setDismissWhenOverlayClicked(false)
        setDismissWhenLifecycleOnPause(false)
        setWidth(BalloonSizeSpec.WRAP)
        setHeight(BalloonSizeSpec.WRAP)
        setArrowSize(10)
        setMarginHorizontal(16)
        setMarginVertical(8)
        setPadding(16)
        setCornerRadius(8f)
        setArrowOrientationRules(ArrowOrientationRules.ALIGN_ANCHOR)
        setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
        setBalloonAnimation(BalloonAnimation.FADE)
        passTouchEventToAnchor = true
        setBackgroundColor(bgColor)
    }

    Balloon(
        modifier = modifier,
        builder = builder,
        balloonContent = {
            Column(
                modifier = if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.width(216.dp)
                }
            ) {
                Text(
                    text = titleText,
                    color = MaterialTheme.colors.white_black,
                    style = MaterialTheme.typography.body2medium,
                )
                MegaSpannedText(
                    value = descriptionText,
                    baseStyle = MaterialTheme.typography.caption.copy(
                        color = MaterialTheme.colors.white_black,
                    ),
                    styles = mapOf(
                        SpanIndicator('A') to SpanStyle(fontWeight = FontWeight.Bold)
                    )
                )
                Text(
                    text = actionText,
                    color = MaterialTheme.colors.white_black,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable {
                            onDismissed()
                            window?.dismiss()
                        },
                )
            }
        }
    ) { balloonWindow ->
        window = balloonWindow
        content()
        if (showOnTop) {
            balloonWindow.showAlignTop()
        } else {
            balloonWindow.showAtCenter()
        }
    }
}
