package mega.privacy.android.app.presentation.filelink.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.filelink.model.FileLinkState
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.core.ui.theme.extensions.subtitle2medium
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

@Composable
internal fun FileLinkContent(
    viewState: FileLinkState,
    onPreviewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.info_ic),
                contentDescription = "info icon",
                colorFilter = ColorFilter.tint(MaterialTheme.colors.textColorSecondary)
            )
            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = stringResource(id = R.string.file_properties_info_info_file),
                style = MaterialTheme.typography.subtitle1medium.copy(color = MaterialTheme.colors.textColorSecondary),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 72.dp)
        ) {
            Text(
                text = stringResource(id = R.string.file_properties_info_size_file),
                style = MaterialTheme.typography.subtitle2medium.copy(color = MaterialTheme.colors.textColorSecondary),
            )
            Text(
                modifier = Modifier.padding(start = 16.dp),
                text = formatFileSize(viewState.sizeInBytes, LocalContext.current),
                style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.textColorSecondary),
            )
        }

        RaisedDefaultMegaButton(
            modifier = Modifier.padding(start = 72.dp, top = 48.dp),
            textId = R.string.preview_content,
            onClick = onPreviewClick,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun FileLinkContentPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val viewState = FileLinkState(title = "File", sizeInBytes = 10000)
        FileLinkContent(viewState = viewState, onPreviewClick = { })
    }
}

