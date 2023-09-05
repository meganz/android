package mega.privacy.android.core.ui.controls.buttons

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
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
fun TextMegaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
) = TextButton(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    colors = MegaTheme.colors.buttonsColors,
    contentPadding = contentPadding
) {
    Text(
        text = text,
        style = MaterialTheme.typography.button
    )
}

@Composable
fun TextMegaButton(
    textId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
) = TextMegaButton(
    text = stringResource(id = textId),
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    contentPadding = contentPadding,
)

@CombinedThemePreviews
@Composable
private fun PreviewTextMegaButton(
    @PreviewParameter(BooleanProvider::class) enabled: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        TextMegaButton(
            textId = androidx.appcompat.R.string.search_menu_title,
            onClick = {},
            enabled = enabled
        )
    }
}