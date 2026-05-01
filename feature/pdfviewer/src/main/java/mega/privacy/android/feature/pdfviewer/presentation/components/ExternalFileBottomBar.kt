package mega.privacy.android.feature.pdfviewer.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.surface.CardSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Bottom bar shown in the scaffold's bottomBar slot when the PDF was opened from an
 * external intent (file explorer, browser, share menu). Exposes a single primary action
 * for uploading the file to the user's Cloud drive.
 *
 * Because the host scaffold reserves space for this composable, no IME / overlay padding
 * tricks are needed here — the PDF content sits above the bar instead of underneath it.
 *
 * @param onUploadToCloudDrive Callback invoked when the upload button is pressed
 * @param modifier Modifier for the composable
 */
@Composable
internal fun ExternalFileBottomBar(
    onUploadToCloudDrive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CardSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        surfaceColor = SurfaceColor.PageBackground,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            PrimaryFilledButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(sharedR.string.photos_save_to_cloud_drive_button_text),
                onClick = onUploadToCloudDrive,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewExternalFileBottomBar() {
    AndroidThemeForPreviews {
        ExternalFileBottomBar(onUploadToCloudDrive = {})
    }
}
