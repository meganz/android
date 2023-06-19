package mega.privacy.android.app.presentation.twofactorauthentication.view.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.qrcode.mycode.view.QRCode
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.toSeedArray
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.body1Medium
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_800
import mega.privacy.android.core.ui.theme.extensions.teal_300_teal_200
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.white_grey_700

@Composable
internal fun SetupScreen(
    is2FAFetchCompleted: Boolean,
    isDarkMode: Boolean,
    qrText: String,
    qrCodeMapper: QRCodeMapper,
    seedsList: List<String>?,
    openPlayStore: () -> Unit,
    onNextClicked: () -> Unit,
    onOpenInClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {

    if (is2FAFetchCompleted) {
        Column(
            modifier = modifier
                .background(MaterialTheme.colors.white_grey_700)
                .fillMaxSize()
        ) {
            InstructionBox(isDarkMode, openPlayStore)
            Spacer(modifier = Modifier.height(20.dp))
            QRCode(
                modifier = Modifier
                    .size(120.dp)
                    .align(CenterHorizontally),
                text = qrText,
                qrCodeMapper = qrCodeMapper
            )
            Spacer(modifier = Modifier.height(20.dp))
            SeedsBox(seedsList.orEmpty())
            Spacer(modifier = Modifier.padding(top = 24.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                RaisedDefaultMegaButton(modifier = Modifier
                    .wrapContentSize(),
                    textId = R.string.open_app_button,
                    onClick = { onOpenInClicked(qrText) })
                Spacer(modifier = Modifier.width(16.dp))
                TextMegaButton(
                    modifier = Modifier
                        .wrapContentSize(),
                    textId = R.string.general_next,
                    onClick = onNextClicked
                )
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(50.dp),
                color = MaterialTheme.colors.teal_300_teal_200
            )
        }
    }
}

@Composable
private fun InstructionBox(
    isDarkMode: Boolean,
    openPlayStore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAlertHelpDialogShown = remember { mutableStateOf(false) }

    if (isAlertHelpDialogShown.value) {
        AlertHelpDialog(
            isDarkMode,
            onConfirm = {
                isAlertHelpDialogShown.value = false
                openPlayStore()
            },
            onDismissRequest = {
                isAlertHelpDialogShown.value = false
            }
        )
    }
    Box(
        modifier = modifier
            .background(MaterialTheme.colors.primary)
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.explain_qr_seed_2fa_1),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.textColorPrimary
            )
            Spacer(modifier = Modifier.padding(top = 2.dp))

            val explanationText = stringResource(id = R.string.explain_qr_seed_2fa_2)
            val explanationTextWithIcon =
                remember {
                    buildAnnotatedString {
                        append(explanationText)
                        appendInlineContent("inlineContent", "[?]")
                    }
                }
            Row {
                val inlineIcon = mapOf(
                    Pair("inlineContent",
                        InlineTextContent(
                            placeholder = Placeholder(
                                width = 20.sp,
                                height = 20.sp,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                            )
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_question_mark),
                                "question mark icon",
                                modifier = Modifier.clickable {
                                    isAlertHelpDialogShown.value = true
                                }
                            )
                        }
                    )
                )
                Text(
                    inlineContent = inlineIcon,
                    text = explanationTextWithIcon,
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.textColorPrimary,

                    )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}


@Composable
private fun SeedsBox(seedsList: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(
                color = MaterialTheme.colors.grey_020_grey_800,
                shape = RoundedCornerShape(8.dp)
            )
    ) {

        Column(
            Modifier
                .padding(vertical = 16.dp)
                .align(
                    Center
                ), horizontalAlignment = CenterHorizontally
        ) {
            seedsList.chunked(5).forEach { rowItems ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowItems.forEach { seed ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .width(50.dp)
                                .align(CenterVertically)
                        ) {
                            Text(
                                modifier = Modifier
                                    .align(Center)
                                    .wrapContentSize(),
                                text = seed,
                                style = MaterialTheme.typography.body1Medium,
                                color = MaterialTheme.colors.textColorPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AlertHelpDialog(
    isDarkMode: Boolean,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AndroidTheme(isDark = isDarkMode) {
        MegaAlertDialog(
            title = stringResource(id = R.string.no_authentication_apps_title),
            text = stringResource(id = R.string.text_2fa_help),
            confirmButtonText = stringResource(id = R.string.play_store_label),
            cancelButtonText = stringResource(id = R.string.general_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismissRequest,
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewSetupView() {
    val seedsList = "eqfhcqxhiq4he6ahjqameqaheqwhrqaheqaheqaheqaheqaheqah".toSeedArray()
    SetupScreen(
        is2FAFetchCompleted = true,
        isDarkMode = false,
        qrText = "123dsfsdfaf2e32",
        qrCodeMapper = { _, _, _, _, _ ->
            Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        },
        seedsList = seedsList,
        onNextClicked = {},
        onOpenInClicked = {},
        openPlayStore = {})
}
