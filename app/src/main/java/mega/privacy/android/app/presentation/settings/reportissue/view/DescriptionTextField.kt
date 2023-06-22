package mega.privacy.android.app.presentation.settings.reportissue.view

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.textfields.MegaTextField
import mega.privacy.android.core.ui.theme.AndroidTheme

@Composable
internal fun DescriptionTextField(
    description: String,
    onDescriptionChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaTextField(
        placeholder = { Text(text = stringResource(R.string.settings_help_report_issue_description_label)) },
        value = description,
        onValueChange = onDescriptionChanged,
        modifier = modifier
    )
}

@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewTextField() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        DescriptionTextField(description = "This is my text",
            onDescriptionChanged = {})
    }
}