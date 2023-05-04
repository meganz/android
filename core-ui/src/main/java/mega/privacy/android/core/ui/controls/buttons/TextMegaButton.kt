package mega.privacy.android.core.ui.controls.buttons

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_200_alpha_038
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.teal_300_alpha_038

@Composable
fun TextMegaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = TextButton(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    colors = if (MaterialTheme.colors.isLight) lightColors() else darkColors(),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
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
) = TextMegaButton(
    text = stringResource(id = textId),
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
)

@Composable
private fun lightColors() = ButtonDefaults.buttonColors(
    backgroundColor = Color.Transparent,
    contentColor = teal_300,
    disabledContentColor = teal_300_alpha_038,
    disabledBackgroundColor = Color.Transparent,
)

@Composable
private fun darkColors() = ButtonDefaults.buttonColors(
    backgroundColor = Color.Transparent,
    contentColor = teal_200,
    disabledContentColor = teal_200_alpha_038,
    disabledBackgroundColor = Color.Transparent,
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