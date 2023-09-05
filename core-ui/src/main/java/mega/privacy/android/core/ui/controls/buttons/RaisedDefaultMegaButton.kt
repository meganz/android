package mega.privacy.android.core.ui.controls.buttons

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme

@Composable
fun RaisedDefaultMegaButton(
    modifier: Modifier = Modifier,
    textId: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
) = TextButton(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    colors = MegaTheme.colors.raisedButtonColors,
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    elevation = ButtonDefaults.elevation(8.dp),
) {
    Text(
        text = stringResource(id = textId),
        style = MaterialTheme.typography.button
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewTextMegaButton(
    @PreviewParameter(BooleanProvider::class) enabled: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        RaisedDefaultMegaButton(
            textId = androidx.appcompat.R.string.search_menu_title,
            onClick = {},
            enabled = enabled
        )
    }
}