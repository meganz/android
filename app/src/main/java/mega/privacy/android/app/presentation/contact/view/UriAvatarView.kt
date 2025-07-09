package mega.privacy.android.app.presentation.contact.view

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil.compose.rememberAsyncImagePainter

/**
 * Avatar with Uri
 */
@Composable
fun UriAvatarView(modifier: Modifier, uri: String) {
    Image(
        modifier = modifier,
        painter = rememberAsyncImagePainter(model = uri),
        contentDescription = "User avatar"
    )
}