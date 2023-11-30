package mega.privacy.android.core.ui.controls.chat.messages

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

/**
 * Test Tag Management Message Icon
 */
const val TEST_TAG_MANAGEMENT_MESSAGE_ICON = "chat_management_message:icon"

/**
 * Chat Management Message
 *
 * @param iconResId Icon resource id
 * @param text Text
 * @param modifier Modifier
 */
@Composable
fun ChatManagementMessage(
    @DrawableRes iconResId: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .size(16.dp)
                .testTag(TEST_TAG_MANAGEMENT_MESSAGE_ICON),
            painter = painterResource(id = iconResId),
            contentDescription = "Call Icon Status",
            tint = MegaTheme.colors.icon.primary
        )

        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = text,
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Medium,
            color = MegaTheme.colors.text.primary
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatManagementMessagePreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        ChatManagementMessage(
            iconResId = R.drawable.ic_favorite,
            text = "This is a management message"
        )
    }
}