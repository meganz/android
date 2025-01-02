package mega.privacy.android.shared.original.core.ui.controls.images

import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.values.IconColor

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
    tint = MegaOriginalTheme.iconColor(tint)
)