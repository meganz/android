package mega.privacy.android.core.nodecomponents.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens

/**
 * Banner to show warning for unverified contact share
 *
 * @param text Text to show in the banner
 * @param modifier [Modifier]
 */
@Composable
fun UnverifiedContactShareBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    MegaText(
        modifier = modifier
            .fillMaxWidth()
            .background(DSTokens.colors.notifications.notificationWarning)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        text = text,
        textColor = TextColor.Primary,
        style = AppTheme.typography.bodySmall,
    )
}

@CombinedThemePreviews
@Composable
fun UnverifiedContactShareBannerPreview() {
    AndroidThemeForPreviews {
        UnverifiedContactShareBanner(
            text = "Favorite Folder is shared by a contact you haven’t verified. To ensure extra security, we recommend that you verify their credentials in Contacts. Tap on the three dots next to the contact you want to verify and tap Info."
        )
    }
}