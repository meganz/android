package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants.TAKEDOWN_URL
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.banners.WarningBanner
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.legacy.core.ui.controls.text.MegaSpannedClickableText
import mega.privacy.android.legacy.core.ui.model.SpanStyleWithAnnotation

/**
 * View to alert when a file or folder is in take down
 */
@Composable
internal fun TakeDownWarningView(
    isFile: Boolean,
    onLinkClick: (link: String) -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) = WarningBanner(
    textComponent = {
        MegaSpannedClickableText(
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
            modifier = Modifier.testTag(TEST_TAKE_TEXT),
        )
    },
    onCloseClick = onCloseClick,
    modifier = modifier,
)

/**
 * Preview, close button and link will alternate file/folder text in Interactive Mode
 */
@CombinedTextAndThemePreviews
@Composable
private fun TakeDownWarningPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        var file by remember { mutableStateOf(true) }
        TakeDownWarningView(isFile = file, onLinkClick = {
            file = !file
        }, onCloseClick = {
            file = !file
        })
    }
}