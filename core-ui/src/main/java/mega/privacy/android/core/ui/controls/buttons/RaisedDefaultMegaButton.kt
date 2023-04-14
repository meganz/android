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
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.dark_grey
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_200_alpha_038
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.teal_300_alpha_038
import mega.privacy.android.core.ui.theme.white

@Composable
fun TextMegaButton(
    modifier: Modifier = Modifier,
    textId: Int,
    onClick: () -> Unit,
    enabled: Boolean = true,
) = TextButton(
    modifier = modifier,
    onClick = onClick,
    enabled = enabled,
    colors = if (MaterialTheme.colors.isLight) lightColors() else darkColors(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    elevation = ButtonDefaults.elevation(8.dp),
) {
    Text(
        text = stringResource(id = textId),
        style = MaterialTheme.typography.button
    )
}

@Composable
private fun lightColors() = ButtonDefaults.buttonColors(
    backgroundColor = teal_300,
    contentColor = white,
    disabledContentColor = white,
    disabledBackgroundColor = teal_300_alpha_038,
)

@Composable
private fun darkColors() = ButtonDefaults.buttonColors(
    backgroundColor = teal_200,
    contentColor = dark_grey,
    disabledContentColor = dark_grey,
    disabledBackgroundColor = teal_200_alpha_038,
)

@CombinedThemePreviews
@Composable
private fun PreviewTextMegaButton(
    @PreviewParameter(BooleanProvider::class) enabled: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        TextMegaButton(
            textId = R.string.search_label,
            onClick = {},
            enabled = enabled
        )
    }
}