package mega.privacy.android.app.presentation.transfers.view.dialog

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.nav.megaNavigator
import mega.privacy.android.shared.original.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedClickableText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyleWithAnnotation
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R

/**
 * All contacts added dialog
 *
 */
@Composable
internal fun NotEnoughSpaceForUploadDialog(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    NotEnoughSpaceForUploadDialog(
        onUpgrade = {
            context.megaNavigator.openUpgradeAccount(context = context)
        },
        onCancel = onCancel,
        modifier = modifier,
        onLearnMore = {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(LINK_TO_STORAGE_EXCEEDED_HELP_PAGE)
                )
            )

        }
    )
}

@Composable
internal fun NotEnoughSpaceForUploadDialog(
    onUpgrade: () -> Unit,
    onCancel: () -> Unit,
    onLearnMore: () -> Unit,
    modifier: Modifier = Modifier,
) = ConfirmationDialog(
    modifier = modifier.testTag(TEST_TAG_NOT_ENOUGH_SPACE_FOR_UPLOAD_DIALOG),
    title = stringResource(id = R.string.dialog_not_enough_space_to_upload_title),
    text = {
        MegaSpannedClickableText(
            value = stringResource(id = R.string.dialog_not_enough_space_to_upload_message),
            styles = hashMapOf(
                SpanIndicator('A') to MegaSpanStyleWithAnnotation(
                    MegaSpanStyle(
                        SpanStyle(textDecoration = TextDecoration.Underline),
                        color = TextColor.Secondary,
                    ), "https://mega.io/chatandmeetings"
                ),
            ),
            color = TextColor.Secondary,
            onAnnotationClick = {
                onLearnMore()
                onCancel()
            },
            modifier = Modifier.testTag(TEST_TAG_NOT_ENOUGH_SPACE_FOR_UPLOAD_DIALOG_CONTENT)
        )
    },
    confirmButtonText = stringResource(id = R.string.general_upgrade_button),
    onDismiss = onCancel,
    cancelButtonText = stringResource(id = R.string.general_dialog_cancel_button),
    onConfirm = {
        onUpgrade()
        onCancel()
    },
)

@CombinedThemePreviews
@Composable
private fun NotEnoughSpaceForUploadDialogPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        NotEnoughSpaceForUploadDialog(onCancel = {})
    }
}

internal const val TEST_TAG_NOT_ENOUGH_SPACE_FOR_UPLOAD_DIALOG =
    "not_enough_space_for_upload_dialog"
internal const val TEST_TAG_NOT_ENOUGH_SPACE_FOR_UPLOAD_DIALOG_CONTENT =
    "${TEST_TAG_NOT_ENOUGH_SPACE_FOR_UPLOAD_DIALOG}:message"
internal const val LINK_TO_STORAGE_EXCEEDED_HELP_PAGE =
    "https://help.mega.io/plans-storage/space-storage/storage-exceeded"