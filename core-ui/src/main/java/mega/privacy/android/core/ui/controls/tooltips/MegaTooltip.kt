package mega.privacy.android.core.ui.controls.tooltips

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.OnBalloonDismissListener
import com.skydoves.balloon.compose.Balloon
import com.skydoves.balloon.compose.BalloonWindow
import com.skydoves.balloon.compose.rememberBalloonBuilder
import com.skydoves.balloon.compose.setBackgroundColor
import kotlinx.coroutines.delay
import mega.privacy.android.core.ui.theme.extensions.body2medium
import mega.privacy.android.core.ui.theme.extensions.dark_blue_tooltip_white
import mega.privacy.android.core.ui.theme.extensions.white_black
import java.util.concurrent.TimeUnit

/**
 * Mega tooltip Balloon
 *
 * @param modifier
 * @param titleText
 * @param descriptionText
 * @param actionText
 * @param showOnTop
 * @param arrowPosition
 * @param onDismissed
 * @param content
 */
@Composable
fun MegaTooltip(
    modifier: Modifier = Modifier,
    titleText: String,
    descriptionText: String,
    actionText: String,
    showOnTop: Boolean,
    arrowPosition: Float,
    onDismissed: () -> Unit,
    content: @Composable () -> Unit,
) {
    var window: BalloonWindow? by remember { mutableStateOf(null) }
    val bgColor = MaterialTheme.colors.dark_blue_tooltip_white
    val builder = rememberBalloonBuilder {
        setIsVisibleOverlay(true)
        setDismissWhenTouchOutside(false)
        setDismissWhenOverlayClicked(false)
        setWidth(BalloonSizeSpec.WRAP)
        setHeight(BalloonSizeSpec.WRAP)
        setArrowSize(10)
        setArrowPosition(arrowPosition)
        setMarginHorizontal(16)
        setMarginVertical(8)
        setPadding(16)
        setCornerRadius(8f)
        setBalloonAnimation(BalloonAnimation.FADE)
        setOnBalloonDismissListener(OnBalloonDismissListener(onDismissed))
        setBackgroundColor(bgColor)
    }

    Balloon(
        modifier = modifier,
        builder = builder,
        balloonContent = {
            Column(modifier = Modifier.width(216.dp)) {
                Text(
                    text = titleText,
                    color = MaterialTheme.colors.white_black,
                    style = MaterialTheme.typography.body2medium,
                )
                Text(
                    text = descriptionText,
                    color = MaterialTheme.colors.white_black,
                    style = MaterialTheme.typography.caption,
                )
                Text(
                    text = actionText,
                    color = MaterialTheme.colors.white_black,
                    textDecoration = TextDecoration.Underline,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { window?.dismiss() },
                )
            }
        }
    ) { balloonWindow ->
        window = balloonWindow
        content()

        LaunchedEffect(Unit) {
            delay(TimeUnit.SECONDS.toMillis(2))
            if (showOnTop) {
                balloonWindow.showAlignTop()
            } else {
                balloonWindow.showAtCenter()
            }
        }
    }
}
