package mega.privacy.android.app.presentation.qrcode

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.avatar.model.AvatarContent
import mega.privacy.android.app.presentation.avatar.model.TextAvatarContent
import mega.privacy.android.app.presentation.avatar.view.Avatar
import mega.privacy.android.app.presentation.extensions.dialogContent
import mega.privacy.android.app.presentation.extensions.dialogTitle
import mega.privacy.android.app.presentation.extensions.printEmail
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.qrcode.model.QRCodeUIState
import mega.privacy.android.app.presentation.qrcode.mycode.model.MyCodeUIState
import mega.privacy.android.app.presentation.qrcode.mycode.view.QRCode
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.dialogs.LoadingDialog
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_038_white_alpha_038
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_087_white_alpha_087
import mega.privacy.android.core.ui.theme.extensions.teal_300_teal_200
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult

/**
 * View to render the QR code Screen, including toolbar, content, etc.
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
internal fun QRCodeView(
    viewState: QRCodeUIState,
    onBackPressed: () -> Unit,
    onDeleteQRCode: () -> Unit,
    onResetQRCode: () -> Unit,
    onGotoSettings: () -> Unit,
    onSaveQRCode: () -> Unit,
    onShareClicked: () -> Unit,
    onScanQrCodeClicked: () -> Unit,
    onCopyLinkClicked: () -> Unit,
    onViewContactClicked: () -> Unit,
    onInviteContactClicked: () -> Unit,
    onResultMessageConsumed: () -> Unit,
    onScannedContactLinkResultConsumed: () -> Unit,
    onInviteContactResultConsumed: () -> Unit,
    qrCodeMapper: QRCodeMapper,
) {
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    var showMoreMenu by remember { mutableStateOf(false) }
    var showScannedContactLinkResult by remember { mutableStateOf<ScannedContactLinkResult?>(null) }
    var showInviteContactResult by remember { mutableStateOf<InviteContactRequest?>(null) }

    EventEffect(
        event = viewState.resultMessage,
        onConsumed = onResultMessageConsumed
    ) {
        snackBarHostState.showSnackbar(context.resources.getString(it))
    }

    EventEffect(
        event = viewState.scannedContactLinkResult,
        onConsumed = onScannedContactLinkResultConsumed
    ) {
        showScannedContactLinkResult = it
    }

    EventEffect(
        event = viewState.inviteContactResult,
        onConsumed = onInviteContactResultConsumed
    ) {
        showInviteContactResult = it
    }

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                MegaSnackbar(modifier = Modifier.testTag(SNACKBAR_TAG), snackbarData = data)
            }
        },
        topBar = {
            QRCodeTopBar(
                isQRCodeAvailable = viewState.myQRCodeState is MyCodeUIState.QRCodeAvailable,
                showMoreMenu = showMoreMenu,
                onShowMoreClicked = { showMoreMenu = !showMoreMenu },
                onMenuDismissed = { showMoreMenu = false },
                onSave = onSaveQRCode,
                onGotoSettings = onGotoSettings,
                onResetQRCode = onResetQRCode,
                onDeleteQRCode = onDeleteQRCode,
                onBackPressed = onBackPressed,
                onShare = onShareClicked
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            ShowSnackBar(
                qrCodeUIState = viewState.myQRCodeState,
                coroutineScope = coroutineScope,
                snackbarHostState = snackBarHostState
            )

            when (viewState.myQRCodeState) {
                is MyCodeUIState.CreatingQRCode -> {
                    LoadingDialog(text = stringResource(id = R.string.generatin_qr))
                }

                is MyCodeUIState.QRCodeAvailable -> {
                    val contactLink = viewState.myQRCodeState.contactLink
                    val avatarContent = viewState.myQRCodeState.avatarContent

                    Box(
                        modifier = Modifier
                            .padding(top = 60.dp)
                            .size(280.dp)
                            .testTag(QRCODE_TAG),
                        contentAlignment = Alignment.Center,
                    ) {
                        QRCode(
                            modifier = Modifier.fillMaxSize(),
                            text = contactLink,
                            qrCodeMapper = qrCodeMapper,
                        )

                        Avatar(
                            modifier = Modifier.size(64.dp),
                            content = avatarContent,
                        )
                    }

                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 24.dp)
                            .testTag(LINK_TAG),
                        onClick = onCopyLinkClicked,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colors.grey_alpha_038_white_alpha_038
                        )
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = contactLink,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.grey_alpha_087_white_alpha_087,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSize))
                        Icon(
                            painter = painterResource(id = R.drawable.copy),
                            contentDescription = stringResource(id = R.string.context_copy),
                            tint = MaterialTheme.colors.teal_300_teal_200
                        )
                    }
                }

                else -> {}
            }

            Spacer(modifier = Modifier.weight(1f))

            RaisedDefaultMegaButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 72.dp)
                    .testTag(SCAN_TAG),
                textId = R.string.menu_item_scan_code,
                onClick = onScanQrCodeClicked
            )

            showInviteContactResult?.let {
                val contentText = if (it.printEmail) {
                    stringResource(
                        id = it.dialogContent,
                        viewState.scannedContactEmail ?: ""
                    )
                } else {
                    stringResource(id = it.dialogContent)
                }

                InviteResultDialog(
                    title = stringResource(id = it.dialogTitle),
                    text = contentText,
                    onConfirmButtonClick = { showInviteContactResult = null }
                )
            }

            showScannedContactLinkResult?.let {
                when (it.qrCodeQueryResult) {
                    QRCodeQueryResults.CONTACT_QUERY_OK -> {
                        InviteContactDialog(
                            scannedContactLinkResult = it,
                            onViewContactClicked = onViewContactClicked,
                            onInviteContactClicked = onInviteContactClicked,
                            onInviteContactDialogDismiss = { showScannedContactLinkResult = null },
                            avatarContent = viewState.scannedContactAvatarContent,
                        )
                    }

                    QRCodeQueryResults.CONTACT_QUERY_EEXIST -> {
                        InviteResultDialog(
                            title = stringResource(id = R.string.invite_not_sent),
                            text = stringResource(
                                id = R.string.invite_not_sent_text_already_contact,
                                it.email
                            ),
                            onConfirmButtonClick = { showScannedContactLinkResult = null }
                        )
                    }

                    else -> {
                        InviteResultDialog(
                            title = stringResource(id = R.string.invite_not_sent),
                            text = stringResource(id = R.string.invite_not_sent_text),
                            onConfirmButtonClick = { showScannedContactLinkResult = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowSnackBar(
    qrCodeUIState: MyCodeUIState,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
) {
    val snackBarText = when (qrCodeUIState) {
        is MyCodeUIState.QRCodeDeleted -> stringResource(R.string.qrcode_delete_successfully)
        is MyCodeUIState.QRCodeResetDone -> stringResource(R.string.qrcode_reset_successfully)
        is MyCodeUIState.Error -> qrCodeUIState.error
        else -> null
    }
    snackBarText?.let {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(snackBarText)
        }
    }
}

@Composable
private fun InviteResultDialog(
    title: String,
    text: String,
    onConfirmButtonClick: () -> Unit,
) {
    MegaAlertDialog(
        title = title,
        text = text,
        confirmButtonText = stringResource(id = R.string.general_ok),
        cancelButtonText = null,
        onConfirm = onConfirmButtonClick,
        onDismiss = {}
    )
}

@Composable
private fun InviteContactDialog(
    scannedContactLinkResult: ScannedContactLinkResult,
    onViewContactClicked: () -> Unit,
    onInviteContactClicked: () -> Unit,
    onInviteContactDialogDismiss: () -> Unit,
    avatarContent: AvatarContent? = null,
) {
    Dialog(onDismissRequest = onInviteContactDialogDismiss) {
        Surface(
            modifier = Modifier.padding(16.dp),
            elevation = 24.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                avatarContent?.let {
                    Avatar(
                        modifier = Modifier.size(64.dp),
                        content = avatarContent
                    )
                }

                Text(
                    modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
                    text = scannedContactLinkResult.contactName
                )

                if (scannedContactLinkResult.isContact) {
                    Text(
                        modifier = Modifier.padding(bottom = 32.dp),
                        text = stringResource(
                            id = R.string.context_contact_already_exists,
                            scannedContactLinkResult.email
                        )
                    )
                    RaisedDefaultMegaButton(
                        textId = R.string.contact_view,
                        onClick = {
                            onViewContactClicked()
                            onInviteContactDialogDismiss()
                        }
                    )
                } else {
                    Text(
                        modifier = Modifier.padding(bottom = 32.dp),
                        text = scannedContactLinkResult.email
                    )
                    RaisedDefaultMegaButton(
                        textId = R.string.contact_invite,
                        onClick = {
                            onInviteContactClicked()
                            onInviteContactDialogDismiss()
                        }
                    )
                }
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewQRCodeView() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val avatarContent = TextAvatarContent(
            avatarText = "A",
            backgroundColor = colorResource(id = R.color.red_300_red_200).toArgb(),
            showBorder = true,
            textSize = 36.sp,
        )
        val viewState = QRCodeUIState(
            myQRCodeState = MyCodeUIState.QRCodeAvailable(
                contactLink = "abc@gmail.com",
                avatarContent = avatarContent,
                avatarBgColor = colorResource(id = R.color.red_300_red_200).toArgb()
            )
        )
        QRCodeView(
            viewState = viewState,
            onBackPressed = { },
            onDeleteQRCode = { },
            onResetQRCode = { },
            onGotoSettings = { },
            onSaveQRCode = { },
            onShareClicked = { },
            onScanQrCodeClicked = { },
            onCopyLinkClicked = { },
            onViewContactClicked = { },
            onInviteContactClicked = { },
            onResultMessageConsumed = { },
            onScannedContactLinkResultConsumed = { },
            onInviteContactResultConsumed = { }
        ) { _, _, _, _, _ ->
            Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewInviteContactDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val result = ScannedContactLinkResult(
            contactName = "Abc",
            email = "abc@gmail.com",
            handle = 12345,
            isContact = false,
            qrCodeQueryResult = QRCodeQueryResults.CONTACT_QUERY_OK,
        )
        val avatarContent = TextAvatarContent(
            avatarText = "A",
            backgroundColor = colorResource(id = R.color.red_300_red_200).toArgb(),
            showBorder = true,
            textSize = 36.sp,
        )
        InviteContactDialog(
            scannedContactLinkResult = result,
            onViewContactClicked = {},
            onInviteContactClicked = {},
            onInviteContactDialogDismiss = {},
            avatarContent = avatarContent
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewInviteResultDialog() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        InviteResultDialog(
            title = "Title",
            text = "Message content text",
            onConfirmButtonClick = {}
        )
    }
}

internal const val QRCODE_TAG = "qr_code_view:view_qrcode"
internal const val LINK_TAG = "qr_code_view:button_link"
internal const val SCAN_TAG = "qr_code_view:button_scan"
internal const val SNACKBAR_TAG = "qr_code_view:snackbar_message"