package mega.privacy.android.app.presentation.contactinfo.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.contactinfo.model.ContactInfoState
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary


@Composable
internal fun InfoOptionsView(
    primaryDisplayName: String,
    secondaryDisplayName: String?,
    modifyNickNameTextId: Int,
    email: String?,
) =
    Column(modifier = Modifier.padding(start = 72.dp, top = 16.dp)) {
        Text(
            text = secondaryDisplayName ?: primaryDisplayName,
            style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.textColorPrimary),
        )
        email?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.textColorPrimary),
            )
        }
        TextMegaButton(
            modifier = Modifier.padding(top = 8.dp),
            contentPadding = PaddingValues(0.dp),
            text = stringResource(id = modifyNickNameTextId),
            onClick = { },
        )
    }

@CombinedThemePreviews
@Composable
private fun PreviewInfoOptionsLight() {
    AndroidTheme(isDark = false) {
        Surface {
            InfoOptionsView(
                primaryDisplayName = "Nick Name",
                secondaryDisplayName = "name",
                modifyNickNameTextId = 1,
                email = "test@gmail.com"
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewInfoOptionsDark() {
    AndroidTheme(isDark = true) {
        Surface {
            InfoOptionsView(
                primaryDisplayName = "Nick Name",
                secondaryDisplayName = "name",
                modifyNickNameTextId = 1,
                email = "test@gmail.com"
            )
        }
    }
}