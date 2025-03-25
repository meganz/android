package mega.privacy.android.shared.original.core.ui.controls.chip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.Typography
import mega.privacy.android.shared.original.core.ui.theme.extensions.buttonNormal
import mega.privacy.android.shared.original.core.ui.theme.extensions.buttonThin
import mega.android.core.ui.theme.values.TextColor
import kotlin.math.pow

@Immutable
sealed interface BadgeSize {
    val height: Dp
    val textStyle: TextStyle
    val textPadding: Dp
    val iconPadding: Dp

    fun iconSize() = height - iconPadding * 2

    @Immutable
    data object Normal : BadgeSize {
        override val height = 20.dp
        override val textStyle = Typography.buttonNormal
        override val textPadding = 6.dp
        override val iconPadding = 3.dp
    }

    @Immutable
    data object Small : BadgeSize {
        override val height = 16.dp
        override val textStyle = Typography.buttonThin
        override val textPadding = 5.dp
        override val iconPadding = 3.dp
    }
}


@Composable
fun CounterBadge(
    count: Int,
    size: BadgeSize,
    modifier: Modifier = Modifier,
    maxDigits: Int = 2,
) = TextBadge(
    text = count.formatNumberWithMaxDigits(maxDigits),
    size = size,
    modifier = modifier
)

@Composable
fun TextBadge(
    text: String,
    size: BadgeSize,
    modifier: Modifier = Modifier,
) = Badge(
    size,
    modifier = modifier,
    {
        MegaText(
            text = text,
            textColor = TextColor.OnColor,
            style = size.textStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = size.textPadding),
        )
    }
)

@Composable
fun IconBadge(
    imageVector: ImageVector,
    size: BadgeSize,
    modifier: Modifier = Modifier,
    contentDescription: String = "",
) = Badge(
    size,
    modifier = modifier,
    {
        Icon(
            imageVector = imageVector,
            tint = MegaOriginalTheme.colors.icon.onColor,
            contentDescription = contentDescription,
            modifier = Modifier.size(size.iconSize()),
        )
    }
)


fun Int.formatNumberWithMaxDigits(maxDigits: Int): String {
    val maxValue = (10.0.pow(maxDigits) - 1).toInt()
    return if (this <= maxValue) {
        this.toString()
    } else {
        "${maxValue}+"
    }
}


@Composable
private fun Badge(
    size: BadgeSize,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) = Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
        .height(size.height)
        .widthIn(min = size.height)
        .background(
            MegaOriginalTheme.colors.components.interactive,
            shape = RoundedCornerShape(size.height / 2)
        ),
    content = content,
)