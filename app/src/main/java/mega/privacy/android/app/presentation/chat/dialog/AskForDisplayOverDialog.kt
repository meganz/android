package mega.privacy.android.app.presentation.chat.dialog

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.app.R
import mega.privacy.android.presentation.theme.AndroidTheme
import mega.privacy.android.presentation.theme.Typography
import timber.log.Timber

@Composable
fun AskForDisplayOverDialog(
    show: Boolean,
    onNotNow: () -> Unit,
    onAllow: () -> Unit,
) {
    if (show) {
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
                    .padding(start = 10.dp, top = 0.dp, bottom = 10.dp, end = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier
                            .wrapContentHeight()
                            .wrapContentWidth()
                            .padding(start = 0.dp, top = 20.dp, bottom = 20.dp, end = 0.dp),
                        textAlign = TextAlign.Center,
                        text = stringResource(id = R.string.ask_for_display_over_title),
                        style = Typography.h6,
                        color = colorResource(id = R.color.grey_087_white),
                        fontWeight = FontWeight.Bold
                    )
                    Image(
                        painterResource(R.drawable.il_call_interface),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .height(70.dp)
                            .width(70.dp)
                    )
                    Text(
                        modifier = Modifier
                            .wrapContentHeight()
                            .wrapContentWidth()
                            .padding(start = 0.dp, top = 20.dp, bottom = 20.dp, end = 0.dp),
                        text = stringResource(id = R.string.ask_for_display_over_msg),
                        style = Typography.subtitle1,
                        color = colorResource(id = R.color.grey_060_white_060)
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
                        color = colorResource(id = R.color.teal_300_teal_200)
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
                        color = colorResource(id = R.color.teal_300_teal_200)
                    )
                }
            },
            backgroundColor = colorResource(id = R.color.white_dark_grey)
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkPreviewAskForDisplayOverDialog")
@Preview
@Composable
fun PreviewAskForDisplayOverDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        AskForDisplayOverDialog(
            show = true,
            onNotNow = {},
            onAllow = {}
        )
    }
}