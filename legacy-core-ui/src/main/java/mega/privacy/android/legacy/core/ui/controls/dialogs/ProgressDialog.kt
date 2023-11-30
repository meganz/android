package mega.privacy.android.legacy.core.ui.controls.dialogs

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalAbsoluteElevation
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.progressindicator.MegaLinearProgressIndicator
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.body3
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_054_white_alpha_054
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_300

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
    cancelButtonText: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    subTitle: String? = null,
) = CompositionLocalProvider(LocalAbsoluteElevation provides 24.dp) {
    Dialog(
        onDismissRequest = {},
        DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 24.dp,
                    top = 24.dp,
                    end = 16.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.grey_alpha_054_white_alpha_054,
                    modifier = Modifier.testTag(PROGRESS_TITLE_TAG),
                )
                MegaLinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .padding(top = 28.dp, bottom = 16.dp),
                )
                subTitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.body3,
                        color = MaterialTheme.colors.grey_alpha_054_white_alpha_054,
                        modifier = Modifier.testTag(PROGRESS_SUBTITLE_TAG),
                    )
                }
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 32.dp)
                        .testTag(PROGRESS_CANCEL_TAG)
                ) {
                    Text(
                        text = cancelButtonText,
                        style = MaterialTheme.typography.button,
                        color = if (!MaterialTheme.colors.isLight) teal_200 else teal_300
                    )
                }
            }
        }
    }
}

internal const val PROGRESS_TITLE_TAG = "progress_dialog:text_title"
internal const val PROGRESS_SUBTITLE_TAG = "progress_dialog:text_subtitle"
internal const val PROGRESS_CANCEL_TAG = "progress_dialog:button_cancel"

@CombinedThemePreviews
@Composable
private fun PreviewProgressDialog(
    @PreviewParameter(BooleanProvider::class) hasSubtitle: Boolean,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ProgressDialog(
            title = "Title goes here",
            subTitle = "Subtitle goes here".takeIf { hasSubtitle },
            progress = 0.3f,
            cancelButtonText = "Cancel",
            onCancel = {},
        )
    }
}