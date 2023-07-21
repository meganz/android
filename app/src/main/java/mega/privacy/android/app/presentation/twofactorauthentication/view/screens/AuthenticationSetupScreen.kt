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
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.qrcode.mycode.view.QRCode
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.drawableId
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.toSeedArray
import mega.privacy.android.app.presentation.twofactorauthentication.model.TwoFactorAuthenticationUIState
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.body1Medium
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_800
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.core.ui.theme.extensions.white_grey_700


@Composable
internal fun AuthenticationSetupScreen(
    uiState: TwoFactorAuthenticationUIState,
    isDarkMode: Boolean,
    qrCodeMapper: QRCodeMapper,
    openPlayStore: () -> Unit,
    onNextClicked: () -> Unit,
    isIntentAvailable: (String) -> Boolean,
    onOpenInClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {

    if (uiState.is2FAFetchCompleted) {
        val qrText = uiState.twoFactorAuthUrl
        var isNoAppAvailableDialogShown by remember { mutableStateOf(false) }
        if (isNoAppAvailableDialogShown) {
            AlertNoAppAvailableDialog(
                isDarkMode,
                onConfirm = {
                    isNoAppAvailableDialogShown = false
                    openPlayStore()
                },
                onDismissRequest = {
                    isNoAppAvailableDialogShown = false
                }
            )
        }
        Column(
            modifier = modifier
                .background(MaterialTheme.colors.white_grey_700)
                .fillMaxSize()
                .testTag(CONTENT_TEST_TAG)
        ) {
            InstructionBox(
                modifier = Modifier.testTag(INSTRUCTIONS_TEST_TAG),
                isDarkMode = isDarkMode,
                openPlayStore = openPlayStore,
            )
            Spacer(modifier = Modifier.height(20.dp))
            QRCode(
                modifier = Modifier
                    .size(120.dp)
                    .align(CenterHorizontally)
                    .testTag(QR_CODE_TEST_TAG),
                text = qrText,
                qrCodeMapper = qrCodeMapper
            )
            Spacer(modifier = Modifier.height(20.dp))
            SeedsBox(
                modifier = Modifier.testTag(
                    SEED_BOX_TEST_TAG
                ),
                seedsList = uiState.seed?.toSeedArray().orEmpty()
            )
            Spacer(modifier = Modifier.padding(top = 24.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {

                RaisedDefaultMegaButton(modifier = Modifier
                    .wrapContentSize(),
                    textId = R.string.open_app_button,
                    onClick = {
                        if (isIntentAvailable(qrText))
                            onOpenInClicked(qrText)
                        else
                            isNoAppAvailableDialogShown = true
                    })
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
            contentAlignment = Center
        ) {
            MegaCircularProgressIndicator(
                modifier = Modifier
                    .size(50.dp)
                    .testTag(SETUP_PROGRESSBAR_TEST_TAG),
            )
        }
    }
}

@Composable
internal fun InstructionBox(
    isDarkMode: Boolean,
    openPlayStore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isAlertHelpDialogShown by remember { mutableStateOf(false) }

    if (isAlertHelpDialogShown) {
        AlertHelpDialog(
            isDarkMode,
            onConfirm = {
                isAlertHelpDialogShown = false
                openPlayStore()
            },
            onDismissRequest = {
                isAlertHelpDialogShown = false
            }
        )
    }
    Box(
        modifier = modifier.background(MaterialTheme.colors.primary)
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

            val inlineIcon: Map<String, InlineTextContent> = mapOf(
                "inlineContent" to InlineTextContent(
                    placeholder = Placeholder(
                        width = 20.sp,
                        height = 20.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_question_mark),
                        contentDescription = "question mark icon",
                        modifier = Modifier
                            .testTag(QUESTION_MARK_ICON_TEST_TAG)
                            .semantics { drawableId = R.drawable.ic_question_mark }
                            .clickable {
                                isAlertHelpDialogShown = true
                            }
                    )
                })

            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag(INSTRUCTION_MESSAGE_TEST_TAG),
                inlineContent = inlineIcon,
                text = explanationTextWithIcon,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.textColorPrimary,
            )
        }
    }
}


@Composable
private fun SeedsBox(seedsList: List<String>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
private fun AlertNoAppAvailableDialog(
    isDarkMode: Boolean,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AndroidTheme(isDark = isDarkMode) {
        val firstText = stringResource(id = R.string.intent_not_available_2fa)
        val secondText = stringResource(id = R.string.open_play_store_2fa)
        val text = "$firstText\n\n$secondText"
        MegaAlertDialog(
            title = stringResource(id = R.string.no_authentication_apps_title),
            text = text,
            confirmButtonText = stringResource(id = R.string.context_open_link),
            cancelButtonText = stringResource(id = R.string.general_cancel),
            onConfirm = onConfirm,
            onDismiss = onDismissRequest,
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        )
    }
}

@Composable
private fun AlertHelpDialog(
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
    val uiState = TwoFactorAuthenticationUIState(
        seed = "eqfhcqxhiq4he6ahjqameqaheqwhrqaheqaheqaheqaheqaheqah",
        is2FAFetchCompleted = true,
        twoFactorAuthUrl = "123dsfsdfaf2e32"
    )
    AuthenticationSetupScreen(
        uiState = uiState,
        isDarkMode = false,
        qrCodeMapper = { _, _, _, _, _ ->
            Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        },
        isIntentAvailable = { true },
        onNextClicked = {},
        onOpenInClicked = {},
        openPlayStore = {})
}

internal const val CONTENT_TEST_TAG = "authentication_setup_screen:column_content_view"
internal const val INSTRUCTIONS_TEST_TAG =
    "authentication_setup_screen:mega_instruction_box_instructions"
internal const val SEED_BOX_TEST_TAG = "authentication_setup_screen:mega_seed_box_codes"
internal const val QR_CODE_TEST_TAG = "authentication_setup_screen:mega_qr_code_authentication_code"
internal const val SETUP_PROGRESSBAR_TEST_TAG =
    "authentication_setup_screen:mega_circular_progress_indicator_loading"
internal const val QUESTION_MARK_ICON_TEST_TAG = "instruction_box:icon_question_mark"
internal const val INSTRUCTION_MESSAGE_TEST_TAG = "instruction_box:text_message"
