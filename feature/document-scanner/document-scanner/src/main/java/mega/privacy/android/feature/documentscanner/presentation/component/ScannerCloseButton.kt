package mega.privacy.android.feature.documentscanner.presentation.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Close button for the scanning screen, positioned at the top.
 *
 * @param onClose Callback to close the scanner
 * @param modifier Modifier for the button
 */
@Composable
fun ScannerCloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClose,
        modifier = modifier.padding(8.dp),
    ) {
        Icon(
            painter = painterResource(IconPackR.drawable.ic_x_medium_regular_outline),
            contentDescription = stringResource(sharedR.string.general_dialog_cancel_button),
            tint = Color.White,
        )
    }
}
