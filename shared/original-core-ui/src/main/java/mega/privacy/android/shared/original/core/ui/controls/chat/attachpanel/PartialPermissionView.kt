package mega.privacy.android.shared.original.core.ui.controls.chat.attachpanel

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
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

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
            tint = MegaOriginalTheme.colors.icon.accent
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PartialPermissionViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        PartialPermissionView(
            modifier = Modifier.size(88.dp)
        )
    }
}