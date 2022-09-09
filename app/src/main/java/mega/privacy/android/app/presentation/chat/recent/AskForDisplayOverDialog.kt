package mega.privacy.android.app.presentation.chat.recent

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.app.R
import mega.privacy.android.presentation.theme.AndroidTheme
import mega.privacy.android.presentation.theme.Typography

@Composable
fun AskForDisplayOverDialog(
    onNotNow: () -> Unit,
    onAllow: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onNotNow,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        title = {
            val scrollState = rememberScrollState()
            Column(Modifier
                .verticalScroll(scrollState)
                .padding(start = 25.dp, top = 25.dp, bottom = 25.dp, end = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier
                        .wrapContentHeight()
                        .wrapContentWidth()
                        .padding(start = 0.dp, top = 20.dp, bottom = 20.dp, end = 0.dp),
                    text = stringResource(id = R.string.ask_for_display_over_title),
                    style = Typography.subtitle1,
                    color = colorResource(id = R.color.grey_087_white_087),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Image(
                    painterResource(R.drawable.il_call_interface),
                    contentDescription = "",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = stringResource(id = R.string.ask_for_display_over_msg),
                    style = Typography.subtitle2,
                    color = colorResource(id = R.color.grey_054_white_054)
                )
            }

        },
        confirmButton = {
            TextButton(
                onClick = onAllow,
                modifier = Modifier
            ) {
                Text(
                    text = stringResource(id = R.string.general_allow),
                    color = if (!MaterialTheme.colors.isLight) colorResource(id = R.color.teal_200) else colorResource(
                        id = R.color.teal_300
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onNotNow,
                modifier = Modifier
            ) {
                Text(
                    text = stringResource(id = R.string.verify_account_not_now_button),
                    color = if (!MaterialTheme.colors.isLight) colorResource(id = R.color.teal_200) else colorResource(
                        id = R.color.teal_300
                    )
                )
            }
        },
        backgroundColor = if (!MaterialTheme.colors.isLight) colorResource(id = R.color.dark_grey) else MaterialTheme.colors.surface
    )
}

@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PreviewAskForDisplayOverDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        AskForDisplayOverDialog(onNotNow = {},
            onAllow = {})
    }
}