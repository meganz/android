package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants.TAKEDOWN_URL
import mega.privacy.android.core.ui.controls.MegaSpannedClickableText
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.model.SpanStyleWithAnnotation
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_087_yellow_700

/**
 * View to alert when a file or folder is in take down
 */
@Composable
internal fun TakeDownWarningView(
    isFile: Boolean,
    onLinkClick: (link: String) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) =
    Row(modifier = modifier.background(colorResource(id = R.color.yellow_100_yellow_700_alpha_015))) {
        MegaSpannedClickableText(
            modifier = modifier
                .padding(16.dp)
                .weight(1f),
            value = stringResource(
                id = if (isFile) {
                    R.string.cloud_drive_info_taken_down_file_warning
                } else {
                    R.string.cloud_drive_info_taken_down_folder_warning
                }
            ),
            styles = mapOf(
                SpanIndicator('A') to SpanStyleWithAnnotation(
                    SpanStyle(textDecoration = TextDecoration.Underline),
                    TAKEDOWN_URL
                )
            ),
            onAnnotationClick = onLinkClick,
            baseStyle = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.grey_alpha_087_yellow_700),
        )
        IconButton(onClick = onCloseClick) {
            Image(
                painter = painterResource(id = R.drawable.ic_remove_warning),
                contentDescription = "Close"
            )
        }
    }

/**
 * Preview, close button and link will alternate fil/folder text in Interactive Mode
 */
@CombinedTextAndThemePreviews
@Composable
private fun TakeDownWarningPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        var file by remember { mutableStateOf(true) }
        TakeDownWarningView(isFile = file, onLinkClick = {
            file = !file
        }, onCloseClick = {
            file = !file
        })
    }
}