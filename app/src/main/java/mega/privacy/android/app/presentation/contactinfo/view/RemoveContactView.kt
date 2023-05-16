package mega.privacy.android.app.presentation.contactinfo.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300

@Composable
internal fun RemoveContact() {
    Row(
        modifier = Modifier
            .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_r_remove),
                contentDescription = null,
                tint = MaterialTheme.colors.red_600_red_300,
            )
        }
        Spacer(modifier = Modifier.padding(start = 20.dp))
        Text(
            text = stringResource(id = R.string.title_properties_remove_contact),
            color = MaterialTheme.colors.red_600_red_300,
            lineHeight = 24.sp,
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewRemoveContactLight() {
    AndroidTheme(isDark = false) {
        Surface {
            RemoveContact()
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewRemoveContactDark() {
    AndroidTheme(isDark = true) {
        Surface {
            RemoveContact()
        }
    }
}