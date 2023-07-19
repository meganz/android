package mega.privacy.android.app.presentation.contactinfo.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_012_white_alpha_012
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary

@Composable
internal fun VerifyCredentialsView(isVerified: Boolean) = Column {
    Row(
        modifier = Modifier
            .padding(start = 4.dp, end = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_verify_credential),
                contentDescription = null,
                tint = MaterialTheme.colors.textColorPrimary,
            )
        }
        Spacer(modifier = Modifier.padding(start = 20.dp))
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = stringResource(id = R.string.label_verify_credentials),
                style = MaterialTheme.typography.subtitle1.copy(
                    color = MaterialTheme.colors.textColorPrimary,
                    lineHeight = 24.sp
                ),
            )
            if (isVerified) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag(R.drawable.ic_verified.toString()),
                        painter = painterResource(id = R.drawable.ic_verified),
                        contentDescription = "Verified user",
                    )
                    Text(
                        text = stringResource(id = R.string.label_verified),
                        style = MaterialTheme.typography.body2.copy(
                            color = MaterialTheme.colors.textColorSecondary,
                        ),
                    )
                }

            } else {
                Text(
                    text = stringResource(id = R.string.label_not_verified),
                    style = MaterialTheme.typography.body2.copy(
                        color = MaterialTheme.colors.textColorSecondary,
                    ),
                )
            }
        }
    }
    Divider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colors.grey_alpha_012_white_alpha_012
    )
}

@CombinedThemePreviews
@Composable
private fun PreviewVerifyCredentialsLight() {
    AndroidTheme(isDark = false) {
        Surface {
            VerifyCredentialsView(isVerified = false)
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewVerifyCredentialsDark() {
    AndroidTheme(isDark = true) {
        Surface {
            VerifyCredentialsView(isVerified = true)
        }
    }
}