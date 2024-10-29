package mega.privacy.android.app.presentation.contact.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.extensions.iconRes
import mega.privacy.android.domain.entity.contacts.UserChatStatus

/**
 * Contact status view
 */
@Deprecated(
    message = "Use ContactStatusView with iconPainter instead",
    replaceWith = ReplaceWith("ContactStatusView(iconPainter, modifier)")
)
@Composable
fun ContactStatusView(
    status: UserChatStatus,
    modifier: Modifier = Modifier,
) {
    val statusIcon = status.iconRes(MaterialTheme.colors.isLight)
    val painter = painterResource(id = statusIcon)
    ContactStatusView(iconPainter = painter, modifier = modifier)
}

/**
 * Contact status view
 */
@Composable
fun ContactStatusView(
    iconPainter: Painter,
    modifier: Modifier = Modifier,
) {
    Image(
        modifier = modifier.padding(start = 5.dp, top = 2.dp),
        painter = iconPainter,
        contentDescription = "Contact status"
    )
}