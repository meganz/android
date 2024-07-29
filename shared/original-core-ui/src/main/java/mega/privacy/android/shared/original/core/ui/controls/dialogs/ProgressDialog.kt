package mega.privacy.android.shared.original.core.ui.controls.dialogs

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.dialogs.internal.BaseMegaAlertDialog
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaLinearProgressIndicator
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.body3

/**
 * Dialog to show cancellable progress
 * @param title
 * @param progress set the current progress [0..1] or null for an indeterminate progress indicator
 * @param cancelButtonText
 * @param onCancel lambda to be triggered when cancel button is pressed
 * @param modifier
 * @param subTitle optional sub title
 */
@Composable
fun ProgressDialog(
    title: String,
    progress: Float?,
    cancelButtonText: String?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    subTitle: String? = null,
) = BaseMegaAlertDialog(
    content = {
        Column {
            Text(
                modifier = Modifier
                    .testTag(PROGRESS_TITLE_TAG)
                    .fillMaxWidth(),
                text = title,
                style = MaterialTheme.typography.subtitle1,
                color = MegaOriginalTheme.colors.text.primary,
            )
            MegaLinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 20.dp),
            )
            subTitle?.let {
                Text(
                    modifier = Modifier.testTag(PROGRESS_SUBTITLE_TAG),
                    text = it,
                    style = MaterialTheme.typography.body3,
                    color = MegaOriginalTheme.colors.text.primary,
                )
            }
        }
    },
    confirmButtonText = cancelButtonText,
    cancelButtonText = null,
    onConfirm = onCancel,
    onDismiss = {},
    modifier = modifier,
    dismissOnClickOutside = false,
    dismissOnBackPress = false,
)

/**
 * Dialog to show not cancellable progress
 * @param title
 * @param progress set the current progress [0..1] or null for an indeterminate progress indicator
 * @param modifier
 * @param subTitle optional sub title
 */
@Composable
fun ProgressDialog(
    title: String,
    progress: Float?,
    modifier: Modifier = Modifier,
    subTitle: String? = null,
) = ProgressDialog(
    title = title,
    progress = progress,
    cancelButtonText = null,
    onCancel = {},
    modifier = modifier,
    subTitle = subTitle,
)

internal const val PROGRESS_TITLE_TAG = "progress_dialog:text_title"
internal const val PROGRESS_SUBTITLE_TAG = "progress_dialog:text_subtitle"

@CombinedThemePreviews
@Composable
private fun ProgressDialogPreview(
    @PreviewParameter(BooleanProvider::class) hasSubtitleAndButton: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        ProgressDialog(
            title = "Title goes here",
            subTitle = "Subtitle goes here".takeIf { hasSubtitleAndButton },
            progress = 0.3f,
            cancelButtonText = "Cancel".takeIf { hasSubtitleAndButton },
            onCancel = {},
        )
    }
}