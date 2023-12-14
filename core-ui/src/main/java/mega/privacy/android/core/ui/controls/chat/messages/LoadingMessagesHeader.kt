package mega.privacy.android.core.ui.controls.chat.messages

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Loading messages header
 */
@Composable
fun LoadingMessagesHeader(
    modifier: Modifier = Modifier,
) {
    var componentHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(modifier = modifier
        .background(MegaTheme.colors.background.pageBackground)
        .onGloballyPositioned {
            componentHeight = with(density) { it.size.height.toDp() }
        }) {
        Image(
            painter = painterResource(id = R.drawable.ic_loading_messages),
            contentDescription = "loading messages view",
            modifier = modifier
                .fillMaxWidth()
                .testTag(TEST_TAG_LOADING_MESSAGES),
            colorFilter = ColorFilter.tint(MegaTheme.colors.background.surface2)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(componentHeight)
                .background(brush = shimmerBrush())
        )
    }
}


@Composable
private fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        MegaTheme.colors.background.pageBackground.copy(alpha = 0f),
        MegaTheme.colors.background.pageBackground.copy(alpha = 0.2f),
        MegaTheme.colors.background.pageBackground.copy(alpha = 0.6f),
        MegaTheme.colors.background.pageBackground.copy(alpha = 0.2f),
        MegaTheme.colors.background.pageBackground.copy(alpha = 0f),
    )

    val transition = rememberInfiniteTransition(label = "")
    val translateAnimation = transition.animateFloat(
        initialValue = 2000f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "transition"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = 0f, y = translateAnimation.value),
        end = Offset.Zero
    )
}

@CombinedThemePreviews
@Composable
private fun LoadingMessagesHeaderPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        LoadingMessagesHeader()
    }
}

/**
 * Test tag loading messages.
 */
const val TEST_TAG_LOADING_MESSAGES = "chat_view:loading_messages_view"