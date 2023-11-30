package mega.privacy.android.app.presentation.account

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.app.R
import mega.privacy.android.shared.theme.MegaAppTheme

/**
 * An [AlertDialog] that is shown when Business Account users attempt to enable Camera Uploads, and
 * reminding them that the Business Account Administrator can access their uploaded data
 */
@Composable
fun CameraUploadsBusinessAlertDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDeny: () -> Unit,
) {
    if (show) {
        AlertDialog(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(5.dp),
            backgroundColor = if (MaterialTheme.colors.isLight) colorResource(id = R.color.white) else colorResource(
                id = R.color.dark_grey
            ),
            onDismissRequest = onDeny,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
            title = { Text(stringResource(R.string.section_photo_sync)) },
            text = { Text(stringResource(R.string.camera_uploads_business_alert)) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = stringResource(R.string.general_enable),
                        color = if (MaterialTheme.colors.isLight) colorResource(id = R.color.teal_300) else colorResource(
                            id = R.color.teal_200
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDeny) {
                    Text(
                        text = stringResource(R.string.general_cancel),
                        color = if (MaterialTheme.colors.isLight) colorResource(id = R.color.teal_300) else colorResource(
                            id = R.color.teal_200
                        ),
                    )
                }
            },
        )
    }
}

/**
 * Composable Preview
 */
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "DarkPreviewCameraUploadsBusinessAlertDialog"
)
@Preview
@Composable
fun PreviewCameraUploadsBusinessAlertDialog() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        CameraUploadsBusinessAlertDialog(
            show = true,
            onConfirm = {},
            onDeny = {},
        )
    }
}