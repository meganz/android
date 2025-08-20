package mega.privacy.android.app.presentation.qrcode.mycode.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.shared.original.core.ui.theme.extensions.black_white

/**
 * QR code for a [text]
 * @param modifier
 * @param text text value in the QR code
 * @param qrCodeMapper the mapper that maps the [text] to a bitmap
 */
@Composable
fun QRCode(
    text: String?,
    qrCodeMapper: QRCodeMapper,
    modifier: Modifier = Modifier,
    bgColor: Color = Color.Transparent,
    penColor: Color = MaterialTheme.colors.black_white,
) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val scope = rememberCoroutineScope()

    text?.let {
        LaunchedEffect(text) {
            scope.launch {
                bitmap = qrCodeMapper(
                    text = text,
                    width = 300,
                    height = 300,
                    penColor = penColor.toArgb(),
                    bgColor = bgColor.toArgb(),
                )?.asImageBitmap()
            }
        }
    }

    Box(
        modifier = modifier
            .size(300.dp)
    ) {
        bitmap?.let {
            Image(
                modifier = Modifier
                    .fillMaxSize(),
                painter = BitmapPainter(it),
                contentDescription = "QR Code",
            )
        }
    }
}
