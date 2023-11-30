package mega.privacy.android.app.presentation.permissions.view

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.shared.theme.MegaAppTheme

@Composable
fun NotificationsPermissionView(
    onNotNowClicked: () -> Unit,
    onGrantAccessClicked: () -> Unit,
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally) {
        val isPortrait =
            LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

        Spacer(modifier = Modifier.size(if (isPortrait) 120.dp else 20.dp))

        Image(modifier = Modifier.requiredSize(120.dp),
            painter = painterResource(id = R.drawable.ic_notifications_permission),
            contentDescription = "Notifications permissions image")

        Spacer(modifier = Modifier.size(if (isPortrait) 48.dp else 24.dp))

        Text(modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Medium,
            color = colorResource(id = R.color.grey_087_white_087),
            text = stringResource(id = R.string.permissions_notifications_title))

        Spacer(modifier = Modifier.size(if (isPortrait) 16.dp else 8.dp))

        Text(modifier = Modifier.padding(horizontal = 24.dp),
            fontFamily = FontFamily.SansSerif,
            fontSize = 16.sp,
            color = colorResource(id = R.color.grey_054_white_054),
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.permissions_notifications_description))

        Spacer(modifier = Modifier.size(if (isPortrait) 124.dp else 24.dp))

        Row(modifier = Modifier.padding(horizontal = 24.dp)) {
            TextButton(onClick = onNotNowClicked,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = colorResource(id = R.color.teal_300_teal_200))) {
                Text(style = MaterialTheme.typography.button,
                    text = stringResource(id = R.string.permissions_not_now_button))
            }

            Spacer(modifier = Modifier.size(16.dp))

            TextButton(onClick = onGrantAccessClicked,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = colorResource(id = R.color.teal_300_teal_200),
                    contentColor = colorResource(id = R.color.white_087_dark_grey))) {
                Text(style = MaterialTheme.typography.button,
                    text = stringResource(id = R.string.button_continue))
            }
        }

        Spacer(modifier = Modifier.size(24.dp))
    }
}

@Composable
@Preview
fun PreviewNotificationsPermissionView() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        NotificationsPermissionView(
            onNotNowClicked = {},
            onGrantAccessClicked = {}
        )
    }
}