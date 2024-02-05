package mega.privacy.android.core.ui.controls.chat.attachpanel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.body4
import mega.privacy.android.core.ui.theme.tokens.TextColor

/**
 * Ask gallery permission view
 *
 * @param modifier
 */
@Composable
fun AskGalleryPermissionView(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit = {},
) {
    ChatGalleryItem(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(24.dp),
                painter = painterResource(id = R.drawable.ic_image_no_permission),
                contentDescription = "Icon Photo",
                tint = MegaTheme.colors.icon.disabled
            )

            Column(
                modifier = Modifier.padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MegaText(
                    style = MaterialTheme.typography.body4,
                    text = stringResource(id = R.string.chat_toolbar_bottom_sheet_allow_gallery_access_title),
                    textColor = TextColor.Primary,
                )
                MegaText(
                    modifier = Modifier.clickable(onClick = onRequestPermission),
                    text = stringResource(id = R.string.chat_toolbar_bottom_sheet_grant_gallery_access_button),
                    style = MaterialTheme.typography.body4,
                    textColor = TextColor.Accent,
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun AskGalleryPermissionViewPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        AskGalleryPermissionView(
            modifier = Modifier
                .height(88.dp)
                .fillMaxWidth()
        )
    }
}