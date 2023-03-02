package mega.privacy.android.app.presentation.qrcode.mycode.view

import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.avatar.model.TextAvatarContent
import mega.privacy.android.app.presentation.avatar.view.Avatar
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.qrcode.mycode.model.MyCodeUIState
import mega.privacy.android.core.ui.controls.LoadingDialog
import mega.privacy.android.core.ui.theme.grey_700
import mega.privacy.android.core.ui.theme.secondary_light
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.white

/**
 * Compose view of My QR Code page
 */
@Composable
fun MyQRCodeView(
    uiState: MyCodeUIState,
    modifier: Modifier = Modifier,
    onButtonClicked: () -> Unit,
    onScroll: (Int) -> Unit,
    qrCodeMapper: QRCodeMapper,
) {
    val scrollState = rememberScrollState()

    if (scrollState.isScrollInProgress) {
        onScroll(scrollState.value)
    }

    val bgColor = white.takeIf { MaterialTheme.colors.isLight } ?: grey_700
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(bgColor)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        when (uiState) {
            is MyCodeUIState.QRCodeAvailable -> {
                val contactLink = uiState.contactLink
                Box(
                    modifier = Modifier
                        .padding(top = 60.dp, bottom = 58.dp)
                        .size(280.dp)
                        .testTag("QR Code Container"),
                    contentAlignment = Alignment.Center,
                ) {
                    QRCode(
                        modifier = Modifier
                            .fillMaxSize(),
                        text = contactLink,
                        qrCodeMapper = qrCodeMapper
                    )

                    Avatar(
                        modifier = Modifier,
                        content = uiState.avatarContent,
                        avatarBgColor = uiState.avatarBgColor ?: secondary_light.toArgb(),
                    )

                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .padding(top = 60.dp, bottom = 58.dp)
                        .height(280.dp)
                        .background(color = Color.Transparent)
                )
            }
        }

        val text = if (uiState is MyCodeUIState.QRCodeAvailable) uiState.contactLink else ""
        Text(
            text = text,
            modifier = Modifier.padding(bottom = 15.dp),
            style = MaterialTheme.typography.body1
        )

        Button(
            onClick = onButtonClicked,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = teal_300.takeIf { MaterialTheme.colors.isLight } ?: teal_200
            ),
        ) {
            val textId =
                if (uiState is MyCodeUIState.QRCodeAvailable) R.string.button_copy_link else R.string.button_create_qr
            Text(
                text = stringResource(id = textId),
                style = MaterialTheme.typography.body1,
            )
        }
    }

    if (uiState is MyCodeUIState.CreatingQRCode) {
        LoadingDialog(text = stringResource(id = R.string.generatin_qr))
    }
}

/**
 * Preview of MyQRCodeView
 */
@Preview
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    name = "DarkMyQRCodeViewPreview"
)
@Composable
fun MyQRCodeViewPreview() {
    val uiState = MyCodeUIState.QRCodeAvailable(
        contactLink = "https://conatctlink",
        avatarBgColor = Color.Red.toArgb(),
        avatarContent = TextAvatarContent(avatarText = "Jackson")
    )
    MyQRCodeView(
        uiState = uiState,
        modifier = Modifier,
        onButtonClicked = {},
        onScroll = {},
        qrCodeMapper = { _, _, _, _, _ ->
            Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        },
    )
}