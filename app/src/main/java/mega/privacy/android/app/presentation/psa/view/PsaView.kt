package mega.privacy.android.app.presentation.psa.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.R as IconPack
import mega.privacy.android.shared.original.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium

/**
 * Psa view
 *
 * @param modifier
 * @param title
 * @param text
 * @param imageUrl
 * @param positiveText
 * @param onPositiveTapped
 * @param onDismiss
 */
@Composable
fun PsaView(
    title: String,
    text: String,
    imageUrl: String?,
    positiveText: String,
    onPositiveTapped: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PsaViewContent(
        modifier = modifier.testTag(PsaViewTag),
        title = title,
        text = text,
        painter = imageUrl?.let { rememberAsyncImagePainter(it) },
        positiveButton = getPositiveButton(positiveText, onPositiveTapped),
        onDismiss = onDismiss,
    )
}

/**
 * Psa info view
 *
 * @param modifier
 * @param title
 * @param text
 * @param imageUrl
 * @param onDismiss
 */
@Composable
fun InfoPsaView(
    title: String,
    text: String,
    imageUrl: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PsaViewContent(
        modifier = modifier.testTag(PsaInfoViewTag),
        title = title,
        text = text,
        painter = imageUrl?.let { rememberAsyncImagePainter(it) },
        positiveButton = null,
        onDismiss = onDismiss,
    )
}

@Composable
private fun getPositiveButton(
    positiveText: String,
    onPositiveTapped: () -> Unit,
): @Composable () -> Unit {
    val positiveButton = @Composable {
        TextMegaButton(
            text = positiveText,
            onClick = onPositiveTapped,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag(PsaPositiveButtonTag),
        )
    }
    return positiveButton
}

@Composable
private fun PsaViewContent(
    modifier: Modifier,
    title: String,
    text: String,
    painter: Painter?,
    positiveButton: (@Composable () -> Unit)?,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        painter?.let {
            Image(
                painter = it,
                contentDescription = "PSA Image",
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(48.dp)
                    .testTag(PsaImageViewTag),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            MegaText(
                text = title, textColor = TextColor.Primary,
                style = MaterialTheme.typography.subtitle1medium,
                modifier = Modifier.testTag(PsaTitleTag)
            )

            Spacer(modifier = Modifier.size(8.dp))

            MegaText(
                text = text,
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.testTag(PsaBodyTag)
            )

            ButtonRow(
                positiveButton = positiveButton,
                onDismiss = onDismiss,
            )
        }
    }

}

@Composable
private fun ButtonRow(
    positiveButton: @Composable (() -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontalArrangement =
        if (positiveButton == null) Arrangement.Start else Arrangement.SpaceAround
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 16.dp),
        horizontalArrangement = horizontalArrangement,
    ) {
        positiveButton?.invoke()

        TextMegaButton(
            text = stringResource(R.string.general_dismiss),
            onClick = onDismiss,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .testTag(PsaDismissButtonTag),
        )
    }
}


@CombinedThemePreviews
@Composable
private fun PsaViewPreview(
    @PreviewParameter(ImagePainterProvider::class) imagePainter: () -> Painter?,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        PsaViewContent(
            modifier = Modifier,
            title = "Title",
            text = "Text",
            painter = imagePainter(),
            positiveButton = getPositiveButton(
                positiveText = "Positive Button Text",
                onPositiveTapped = {},
            ),
            onDismiss = {},
        )
    }
}


@CombinedThemePreviews
@Composable
private fun InfoPsaViewPreview(
    @PreviewParameter(ImagePainterProvider::class) imagePainter: @Composable () -> Painter?,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        Box(modifier = Modifier.fillMaxSize()) {
            PsaViewContent(
                modifier = Modifier
                    .navigationBarsPadding()
                    .align(Alignment.BottomCenter),
                title = "Title",
                text = "Text",
                painter = imagePainter(),
                positiveButton = null,
                onDismiss = {},
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun ButtonRowPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        Box(Modifier.fillMaxSize()) {
            ButtonRow(
                positiveButton = { TextMegaButton(text = "Positive", onClick = {}) },
                onDismiss = {},
                modifier = Modifier.navigationBarsPadding()
                    .align(Alignment.BottomCenter),
            )
        }
    }
}

internal class ImagePainterProvider : PreviewParameterProvider<@Composable () -> Painter?> {
    override val values = listOf<@Composable () -> Painter?>(
        { null },
        { painterResource(IconPack.drawable.ic_bell_glass) },
    ).asSequence()
}
internal const val PsaViewTag = "psa_view"
internal const val PsaInfoViewTag = "psa_info_view"
internal const val PsaImageViewTag = "psa_view:image_view"
internal const val PsaPositiveButtonTag = "psa_view:button_positive"
internal const val PsaDismissButtonTag = "psa_info:button_dismiss"
internal const val PsaTitleTag = "psa_info:text_title"
internal const val PsaBodyTag = "psa_info:text_body"