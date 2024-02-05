package mega.privacy.android.core.ui.controls.chat.attachpanel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Partial permission view
 *
 * @param modifier Modifier
 * @param onRequestPermission Callback when request permission
 */
@Composable
fun PartialPermissionView(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit = {},
) {
    ChatGalleryItem(modifier = modifier) {
        Icon(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center)
                .clickable(onClick = onRequestPermission),
            painter = painterResource(id = R.drawable.ic_image_plus),
            contentDescription = "Image plus",
            tint = MegaTheme.colors.icon.accent
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PartialPermissionViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        PartialPermissionView(
            modifier = Modifier.size(88.dp)
        )
    }
}