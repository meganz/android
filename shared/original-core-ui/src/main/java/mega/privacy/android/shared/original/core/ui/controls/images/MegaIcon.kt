package mega.privacy.android.shared.original.core.ui.controls.images

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.original.core.ui.theme.iconColor

@Composable
fun MegaIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: IconColor = IconColor.Primary,
) = Icon(
    painter = painter,
    contentDescription = contentDescription,
    modifier = modifier,
    tint = DSTokens.iconColor(tint)
)