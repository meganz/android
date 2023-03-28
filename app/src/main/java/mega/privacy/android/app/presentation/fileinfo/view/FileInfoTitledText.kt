package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

/**
 * Title and text
 */
@Composable
internal fun FileInfoTitledText(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier
        .fillMaxWidth()
) {
    Text(
        text = title,
        style = MaterialTheme.typography.subtitle2.copy(color = MaterialTheme.colors.textColorPrimary),
    )
    Text(
        text = text,
        style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.textColorSecondary),
    )
    Spacer(modifier = Modifier.height(verticalSpace.dp))
}

/**
 * Preview for [FileInfoTitledText]
 */
@CombinedTextAndThemePreviews
@Composable
private fun FileInfoTitledTextPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        FileInfoTitledText(title = "Title", text = "Something interesting")
    }
}