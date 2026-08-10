package mega.privacy.android.feature.documentscanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Back / close control for the scanning screen, on a translucent scrim so it
 * stays legible over the camera feed.
 */
@Composable
internal fun ScannerCloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClose,
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f)),
    ) {
        Icon(
            painter = painterResource(IconPackR.drawable.ic_chevron_left_medium_thin_outline),
            contentDescription = stringResource(sharedR.string.general_dialog_cancel_button),
            tint = DSTokens.colors.icon.onColor,
        )
    }
}
