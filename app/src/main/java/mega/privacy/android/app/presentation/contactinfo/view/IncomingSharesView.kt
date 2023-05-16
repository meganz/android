package mega.privacy.android.app.presentation.contactinfo.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary

@Composable
internal fun IncomingSharesView(count: Int) = Column {
    Row(
        modifier = Modifier
            .padding(start = 72.dp, end = 20.dp, top = 4.dp, bottom = 4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.title_incoming_shares_explorer),
            style = MaterialTheme.typography.subtitle1.copy(
                color = MaterialTheme.colors.textColorPrimary,
                lineHeight = 24.sp
            ),
        )
        TextMegaButton(
            modifier = Modifier,
            contentPadding = PaddingValues(0.dp),
            text = pluralStringResource(
                id = R.plurals.num_folders_with_parameter,
                count = count, count,
            ),
            onClick = { },
        )
    }
    Divider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colors.grey_alpha_012_white_alpha_012
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewIncomingSharesLight() {
    AndroidTheme(isDark = false) {
        Surface {
            IncomingSharesView(count = 0)
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewIncomingSharesDark() {
    AndroidTheme(isDark = true) {
        Surface {
            IncomingSharesView(count = 10)
        }
    }
}