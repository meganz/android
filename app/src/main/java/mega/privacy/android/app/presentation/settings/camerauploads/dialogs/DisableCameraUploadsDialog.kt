package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.text.SpannableText
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Test tag for the Disable CU Dialog
 */
internal const val TEST_TAG_DISABLE_CU_DIALOG =
    "settings:camera_uploads:disable_camera_uploads:dialog"

/**
 * A Composable that displays a dialog to confirm disabling Camera Uploads
 */
@Composable
internal fun DisableCameraUploadsDialog(
    onDisable: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) = BasicDialog(
    modifier = modifier.testTag(TEST_TAG_DISABLE_CU_DIALOG),
    title = SpannableText(stringResource(id = sharedR.string.settings_camera_uploads_disable_warning)),
    buttons = listOf(
        BasicDialogButton(
            stringResource(id = sharedR.string.general_dialog_cancel_button),
            onClick = onDismiss
        ),
        BasicDialogButton(
            stringResource(id = R.string.verify_2fa_subtitle_diable_2fa),
            onClick = {
                onDisable()
                onDismiss()
            }),
    ).toImmutableList(),
    onDismissRequest = onDismiss,
)

@CombinedThemePreviews
@Composable
private fun DisableCameraUploadsDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        DisableCameraUploadsDialog(
            onDisable = {},
            onDismiss = {},
        )
    }
}