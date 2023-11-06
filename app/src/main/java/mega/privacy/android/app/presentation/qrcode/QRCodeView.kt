package mega.privacy.android.app.presentation.qrcode

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import mega.privacy.android.app.R
import mega.privacy.android.app.namecollision.data.NameCollision
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
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.core.ui.controls.snackbars.MegaSnackbar
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_038_white_alpha_038
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_087_white_alpha_087
import mega.privacy.android.core.ui.theme.extensions.teal_300_teal_200
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.legacy.core.ui.controls.dialogs.LoadingDialog
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * View to render the QR code Screen, including toolbar, content, etc.
 */
@OptIn(ExperimentalMaterialApi::class)
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
internal fun QRCodeView(
    viewState: QRCodeUIState,
    onBackPressed: () -> Unit,
    onCreateQRCode: () -> Unit,
    onDeleteQRCode: () -> Unit,
    onResetQRCode: () -> Unit,
    onScanQrCodeClicked: () -> Unit,
    onCopyLinkClicked: () -> Unit,
    onViewContactClicked: (String) -> Unit,
    onInviteContactClicked: (Long, String) -> Unit,
    onResultMessageConsumed: () -> Unit,
    onScannedContactLinkResultConsumed: () -> Unit,
    onInviteContactResultConsumed: () -> Unit,
    onInviteResultDialogDismiss: () -> Unit,
    onInviteContactDialogDismiss: () -> Unit,
    onCloudDriveClicked: () -> Unit,
    onFileSystemClicked: () -> Unit,
    onShowCollision: (NameCollision) -> Unit,
    onShowCollisionConsumed: () -> Unit,
    onUploadFile: (Pair<File, Long>) -> Unit,
    onUploadFileConsumed: () -> Unit,
    onScanCancelConsumed: () -> Unit,
    qrCodeMapper: QRCodeMapper,
) {
    val view: View = LocalView.current
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    var showMoreMenu by remember { mutableStateOf(false) }
    var showScannedContactLinkResult by remember { mutableStateOf<ScannedContactLinkResult?>(null) }
    var showInviteContactResult by remember { mutableStateOf<InviteContactRequest?>(null) }
    var qrCodeComposableBounds by remember { mutableStateOf<Rect?>(null) }

    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded },
        skipHalfExpanded = true,
    )

    BackHandler(enabled = modalSheetState.isVisible) {
        coroutineScope.launch { modalSheetState.hide() }
    }

    EventEffect(
        event = viewState.resultMessage,
        onConsumed = onResultMessageConsumed
    ) {
        snackBarHostState.showSnackbar(context.resources.getString(it.first, *it.second))
    }

    EventEffect(
        event = viewState.scannedContactLinkResult,
        onConsumed = onScannedContactLinkResultConsumed
    ) {
        if (viewState.finishActivityOnScanComplete) {
            finishActivityAndSetResult(context.findActivity(), it)
        } else {
            showScannedContactLinkResult = it
        }
    }

    EventEffect(
        event = viewState.inviteContactResult,
        onConsumed = onInviteContactResultConsumed
    ) {
        showInviteContactResult = it
    }

    EventEffect(
        event = viewState.showCollision,
        onConsumed = onShowCollisionConsumed,
        action = onShowCollision
    )

    EventEffect(
        event = viewState.uploadFile,
        onConsumed = onUploadFileConsumed,
        action = onUploadFile
    )

    EventEffect(
        event = viewState.scanCancel,
        onConsumed = onScanCancelConsumed
    ) {
        if (viewState.finishActivityOnScanComplete)
            finishActivity(context.findActivity())
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
                context = context,
                isQRCodeAvailable = viewState.myQRCodeState is MyCodeUIState.QRCodeAvailable,
                showMoreMenu = showMoreMenu,
                onShowMoreClicked = { showMoreMenu = !showMoreMenu },
                onMenuDismissed = { showMoreMenu = false },
                onSave = {
                    context.findActivity()?.let { activity ->
                        qrCodeComposableBounds?.let { viewBounds ->
                            handleSave(
                                activity,
                                view,
                                viewBounds,
                                viewState.myQRCodeState,
                                coroutineScope,
                                snackBarHostState,
                            ) {
                                coroutineScope.launch { modalSheetState.show() }
                            }
                        }
                    }
                },
                onResetQRCode = onResetQRCode,
                onDeleteQRCode = onDeleteQRCode,
                onBackPressed = onBackPressed,
                onShare = {
                    context.findActivity()?.let { activity ->
                        qrCodeComposableBounds?.let { viewBounds ->
                            handleShare(
                                activity,
                                view,
                                viewBounds,
                                viewState.myQRCodeState,
                                coroutineScope,
                                snackBarHostState
                            )
                        }
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            ShowSnackBar(
                qrCodeUIState = viewState.myQRCodeState,
                coroutineScope = coroutineScope,
                snackbarHostState = snackBarHostState
            )

            when (viewState.myQRCodeState) {
                is MyCodeUIState.CreatingQRCode -> {
                    if (viewState.myQRCodeState.showLoader)
                        LoadingDialog(text = stringResource(id = R.string.generatin_qr))
                }

                is MyCodeUIState.QRCodeAvailable -> {
                    val contactLink = viewState.myQRCodeState.contactLink
                    val avatarContent = viewState.myQRCodeState.avatarContent

                    Box(
                        modifier = Modifier
                            .padding(top = 60.dp)
                            .size(280.dp)
                            .testTag(QRCODE_TAG)
                            .onGloballyPositioned {
                                qrCodeComposableBounds =
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        it.boundsInWindow()
                                    } else {
                                        it.boundsInRoot()
                                    }
                            },
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

                is MyCodeUIState.QRCodeDeleted, is MyCodeUIState.Idle -> {
                    Box(
                        modifier = Modifier
                            .padding(top = 60.dp)
                            .size(280.dp)
                            .testTag(CREATE_TAG),
                        contentAlignment = Alignment.Center,
                    ) {
                        OutlinedMegaButton(
                            textId = R.string.button_create_qr,
                            onClick = onCreateQRCode,
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
                    onInviteResultDialogDismiss = {
                        onInviteResultDialogDismiss()
                        showInviteContactResult = null
                    }
                )
            }

            showScannedContactLinkResult?.let {
                when (it.qrCodeQueryResult) {
                    QRCodeQueryResults.CONTACT_QUERY_OK -> {
                        InviteContactDialog(
                            scannedContactLinkResult = it,
                            onViewContactClicked = onViewContactClicked,
                            onInviteContactClicked = onInviteContactClicked,
                            onInviteContactDialogDismiss = {
                                showScannedContactLinkResult = null
                                onInviteContactDialogDismiss()
                            },
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
                            onInviteResultDialogDismiss = {
                                onInviteResultDialogDismiss()
                                showScannedContactLinkResult = null
                            }
                        )
                    }

                    else -> {
                        InviteResultDialog(
                            title = stringResource(id = R.string.invite_not_sent),
                            text = stringResource(id = R.string.invite_not_sent_text),
                            onInviteResultDialogDismiss = {
                                onInviteResultDialogDismiss()
                                showScannedContactLinkResult = null
                            }
                        )
                    }
                }
            }
        }

        QRCodeSaveBottomSheetView(
            modalSheetState = modalSheetState,
            coroutineScope = coroutineScope,
            onCloudDriveClicked = onCloudDriveClicked,
            onFileSystemClicked = onFileSystemClicked
        )
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
    onInviteResultDialogDismiss: () -> Unit,
) {
    ConfirmationDialog(
        title = title,
        text = text,
        confirmButtonText = stringResource(id = R.string.general_ok),
        cancelButtonText = null,
        onConfirm = onInviteResultDialogDismiss,
        onDismiss = onInviteResultDialogDismiss
    )
}

@Composable
private fun InviteContactDialog(
    scannedContactLinkResult: ScannedContactLinkResult,
    onViewContactClicked: (String) -> Unit,
    onInviteContactClicked: (Long, String) -> Unit,
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
                            onViewContactClicked(scannedContactLinkResult.email)
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
                            onInviteContactClicked(
                                scannedContactLinkResult.handle,
                                scannedContactLinkResult.email
                            )
                            onInviteContactDialogDismiss()
                        }
                    )
                }
            }
        }
    }
}

private fun handleSave(
    activity: Activity,
    view: View,
    viewBounds: Rect,
    myQRCodeState: MyCodeUIState,
    coroutineScope: CoroutineScope,
    snackBarHostState: SnackbarHostState,
    onSaveQRCode: () -> Unit,
) {
    coroutineScope.launch {
        (myQRCodeState as? MyCodeUIState.QRCodeAvailable)?.qrCodeFilePath?.let { qrFilePath ->
            runCatching {
                val bitmap = captureViewToBitmap(view, activity.window, viewBounds)
                bitmap?.let {
                    saveBitmap(bitmap, qrFilePath)
                    onSaveQRCode()
                } ?: snackBarHostState.showSnackbar(activity.getString(R.string.general_text_error))
            }.onFailure { snackBarHostState.showSnackbar(activity.getString(R.string.general_text_error)) }
        }
    }
}

private fun handleShare(
    activity: Activity,
    view: View,
    viewBounds: Rect,
    myQRCodeState: MyCodeUIState,
    coroutineScope: CoroutineScope,
    snackBarHostState: SnackbarHostState
) {
    coroutineScope.launch {
        (myQRCodeState as? MyCodeUIState.QRCodeAvailable)?.qrCodeFilePath?.let { qrFilePath ->
            runCatching {
                val bitmap = captureViewToBitmap(view, activity.window, viewBounds)
                bitmap?.let {
                    val file = saveBitmap(bitmap, qrFilePath)
                    val uri = getFileUri(activity, file)
                    shareImage(activity, uri)
                } ?: snackBarHostState.showSnackbar(activity.getString(R.string.error_share_qr))
            }.onFailure { snackBarHostState.showSnackbar(activity.getString(R.string.error_share_qr)) }
        }
    }
}

private suspend fun captureViewToBitmap(view: View, window: Window, bounds: Rect): Bitmap? {
    return suspendCancellableCoroutine { continuation ->
        with(view) {
            val bitmap = Bitmap.createBitmap(
                bounds.width.toInt(),
                bounds.height.toInt(),
                Bitmap.Config.ARGB_8888,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PixelCopy.request(
                    window,
                    android.graphics.Rect(
                        bounds.left.toInt(),
                        bounds.top.toInt(),
                        bounds.right.toInt(),
                        bounds.bottom.toInt()
                    ),
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            continuation.resume(bitmap)
                        } else {
                            continuation.resume(null)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } else {
                val canvas = Canvas(bitmap)
                    .apply {
                        translate(-bounds.left, -bounds.top)
                    }
                this.draw(canvas)
                canvas.setBitmap(null)
                continuation.resume(bitmap)
            }
        }
    }
}

private suspend fun getFileUri(context: Context, file: File): Uri? {
    return suspendCancellableCoroutine { continuation ->
        runCatching {
            FileProvider.getUriForFile(context, Constants.AUTHORITY_STRING_FILE_PROVIDER, file)
        }.onSuccess {
            continuation.resume(it)
        }.onFailure {
            continuation.resumeWithException(it)
        }
    }
}

private suspend fun saveBitmap(bitmap: Bitmap, destPath: String): File {
    return suspendCancellableCoroutine { continuation ->
        runCatching {
            val out: OutputStream
            val file = File(destPath)
            out = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
            continuation.resume(file)
        }.onFailure {
            Timber.e(it)
            continuation.resumeWithException(it)
        }
    }
}

private fun shareImage(activity: Activity, uri: Uri?) {
    uri?.let {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(uri.toString()))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        activity.startActivity(
            Intent.createChooser(
                shareIntent,
                activity.getString(R.string.context_share)
            )
        )
    }
}

private fun finishActivityAndSetResult(
    activity: Activity?,
    scannedContactLinkResult: ScannedContactLinkResult
) {
    val intent = Intent()
    intent.putExtra(Constants.INTENT_EXTRA_KEY_MAIL, scannedContactLinkResult.email)
    activity?.setResult(Activity.RESULT_OK, intent)
    activity?.finish()
}

private fun finishActivity(activity: Activity?) {
    activity?.finish()
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
            onCreateQRCode = { },
            onBackPressed = { },
            onDeleteQRCode = { },
            onResetQRCode = { },
            onScanQrCodeClicked = { },
            onCopyLinkClicked = { },
            onViewContactClicked = { },
            onInviteContactClicked = { _, _ -> },
            onResultMessageConsumed = { },
            onScannedContactLinkResultConsumed = { },
            onInviteContactResultConsumed = { },
            onInviteResultDialogDismiss = { },
            onInviteContactDialogDismiss = { },
            onCloudDriveClicked = { },
            onFileSystemClicked = { },
            onShowCollision = { },
            onShowCollisionConsumed = { },
            onUploadFile = { },
            onUploadFileConsumed = { },
            onScanCancelConsumed = { },
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
            onInviteContactClicked = { _, _ -> },
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
            onInviteResultDialogDismiss = {}
        )
    }
}

internal const val QRCODE_TAG = "qr_code_view:view_qrcode"
internal const val LINK_TAG = "qr_code_view:button_link"
internal const val SCAN_TAG = "qr_code_view:button_scan"
internal const val CREATE_TAG = "qr_code_view:button_create"
internal const val SNACKBAR_TAG = "qr_code_view:snackbar_message"