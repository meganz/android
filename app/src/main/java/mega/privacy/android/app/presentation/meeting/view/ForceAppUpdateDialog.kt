package mega.privacy.android.app.presentation.meeting.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.theme.MegaAppTheme
import timber.log.Timber

/**
 * Show a dialog to force the user to update the app.
 */
@Composable
fun ForceAppUpdateDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    val context = LocalContext.current
    ConfirmationDialog(
        title = stringResource(
            R.string.meetings_chat_screen_app_update_dialog_title
        ),
        text = stringResource(
            R.string.meetings_chat_screen_app_update_dialog_message,
        ),
        confirmButtonText = stringResource(
            R.string.meetings_chat_screen_app_update_dialog_update_button
        ),
        cancelButtonText = stringResource(id = R.string.general_skip),
        onConfirm = {
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW, Uri.parse(MARKET_URI)
                    )
                )
            } catch (exception: ActivityNotFoundException) {
                Timber.e(exception, "Exception opening Play Store")
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URI)
                    )
                )
            }
            onDismiss()
        },
        onDismiss = onDismiss,
        modifier = modifier.testTag(FORCE_APP_UPDATE_TAG)
    )
}

@CombinedThemePreviews
@Composable
private fun ForceAppUpdateDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ForceAppUpdateDialog()
    }
}

private const val PACKAGE_NAME = "id=mega.privacy.android.app"
private const val MARKET_URI = "market://details?$PACKAGE_NAME"
private const val PLAY_STORE_URI = "https://play.google.com/store/apps/details?$PACKAGE_NAME"

internal const val FORCE_APP_UPDATE_TAG = "force_app_update:dialog_update"
