package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorSecondary
import java.time.Instant.now

/**
 * Shows shared link with a "Copy" button
 */
@Composable
internal fun ShareLinkView(
    link: String,
    date: Long,
    onCopyLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(modifier = modifier.fillMaxWidth()) {
    Text(
        text = stringResource(id = R.string.file_properties_shared_folder_public_link_name),
        style = MaterialTheme.typography.subtitle2medium
            .copy(color = MaterialTheme.colors.textColorPrimary),
    )
    Text(
        text = link,
        style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.textColorSecondary),
    )
    Spacer(modifier = Modifier.height(14.dp))
    Text(
        text = stringResource(
            id = R.string.general_date_label, TimeUtils.formatLongDateTime(date)
        ),
        style = MaterialTheme.typography.body2.copy(
            letterSpacing = 0.sp,
            color = MaterialTheme.colors.textColorPrimary
        ),
    )
    Spacer(modifier = Modifier.height(14.dp))
    OutlinedMegaButton(
        modifier = Modifier
            .widthIn(min = 100.dp)
            .testTag(TEST_SHARE_LINK_COPY),
        textId = R.string.context_copy,
        onClick = onCopyLinkClick,
        rounded = false,
    )
    Spacer(modifier = Modifier.height(verticalSpace.dp))
}

/**
 * Preview for [ShareLinkView]
 */
@CombinedTextAndThemePreviews
@Composable
private fun ShareLinkPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ShareLinkView(
            link = "http://mega.app/folder/longhpajsdfg",
            date = now().epochSecond,
            onCopyLinkClick = {}
        )
    }
}