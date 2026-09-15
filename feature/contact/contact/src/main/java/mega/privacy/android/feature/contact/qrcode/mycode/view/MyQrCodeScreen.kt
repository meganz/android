package mega.privacy.android.feature.contact.qrcode.mycode.view

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.consumed
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.SecondaryFilledButton
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.model.menu.MenuActionWithoutIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.qr.rememberQrCodeBitmap
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeError
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeMessage
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeUiState
import mega.privacy.android.feature.contact.qrcode.mycode.model.QrCode
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.account.presentation.model.AvatarContent
import mega.privacy.android.shared.account.presentation.model.TextAvatarContent
import mega.privacy.android.shared.account.presentation.widget.view.Avatar
import mega.privacy.android.shared.resources.R as sharedR

private val QrCodeSize = 280.dp
private val QrCodeTopPadding = 40.dp
private val ContactLinkHeight = 56.dp
private val ContactLinkTextHeight = 16.dp
private val QrContentSpacing = 24.dp
private val ContactLinkCorner = RoundedCornerShape(8.dp)

/**
 * Height every content state occupies, so the "Scan QR code" button below keeps its position as
 * the screen moves between generating, available and empty instead of jumping up and down.
 */
private val ContentAreaHeight =
    QrCodeTopPadding + QrCodeSize + QrContentSpacing + ContactLinkHeight

/**
 * My qr code screen
 * Composable function — emits UI and is re-invoked (recomposed) when its inputs change.
 *
 * @param uiState the ui state
 * @param onBackClick the on back click
 * @param onCreateQRCode the on create qr code
 * @param onCopyContactLink the on copy contact link
 * @param onSaveToFile the on save to file
 * @param onResetQRCode the on reset qr code
 * @param onDeleteQRCode the on delete qr code
 * @param onScanClick the on scan click
 * @param onMessageConsumed the on message consumed
 * @param modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyQrCodeScreen(
    uiState: MyQrCodeUiState,
    onBackClick: () -> Unit,
    onCreateQRCode: () -> Unit,
    onCopyContactLink: (String) -> Unit,
    onSaveToFile: (UriPath) -> Unit,
    onResetQRCode: () -> Unit,
    onDeleteQRCode: (String) -> Unit,
    onScanClick: () -> Unit,
    onMessageConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let { onSaveToFile(UriPath(it.toString())) }
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .testTag(MY_QR_CODE_SCREEN_TAG),
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.contacts_qr_screen_title),
                navigationType = AppBarNavigationType.Back(onBackClick),
                actions = if (uiState is MyQrCodeUiState.QRCodeAvailable) {
                    listOf(
                        MenuActionWithClick(SaveQrCodeMenuAction) { folderPicker.launch(null) },
                        MenuActionWithClick(ResetQrCodeMenuAction, onResetQRCode),
                        MenuActionWithClick(
                            DeleteQrCodeMenuAction,
                            { onDeleteQRCode(uiState.qrCode.contactLink) }),
                    )
                } else {
                    emptyList()
                },
            )
        },
    ) { padding ->
        val snackbarHostState = LocalSnackBarHostState.current
        EventEffect(
            event = uiState.message,
            onConsumed = onMessageConsumed,
        ) { message ->
            snackbarHostState?.showAutoDurationSnackbar(message.toText(context))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ContentAreaHeight),
            ) {
                when (uiState) {
                    MyQrCodeUiState.CreatingQRCode -> {
                        QrCodePlaceholder(modifier = Modifier.align(Alignment.TopCenter))
                    }

                    is MyQrCodeUiState.QRCodeAvailable -> {
                        QrCodeContent(
                            modifier = Modifier.align(Alignment.TopCenter),
                            contactLink = uiState.qrCode.contactLink,
                            avatarContent = uiState.qrCode.avatarContent,
                            onCopyContactLink = { onCopyContactLink(uiState.qrCode.contactLink) },
                        )
                    }

                    is MyQrCodeUiState.QRCodeUnavailable -> {
                        PrimaryFilledButton(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .testTag(MY_QR_CODE_CREATE_TAG),
                            text = stringResource(sharedR.string.contacts_qr_create_action),
                            onClick = onCreateQRCode,
                        )
                    }
                }
            }

            SecondaryFilledButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .testTag(MY_QR_CODE_SCAN_TAG),
                text = stringResource(sharedR.string.contacts_qr_scan_action),
                onClick = onScanClick,
            )
        }
    }
}

@Composable
private fun QrCodeContent(
    contactLink: String,
    avatarContent: AvatarContent,
    onCopyContactLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(QrContentSpacing),
    ) {
        Box(
            modifier = Modifier
                .padding(top = QrCodeTopPadding)
                .size(QrCodeSize)
                .testTag(MY_QR_CODE_IMAGE_TAG),
            contentAlignment = Alignment.Center,
        ) {
            rememberQrCodeBitmap(text = contactLink, size = QrCodeSize)?.let { qrCode ->
                MegaIcon(
                    modifier = Modifier.fillMaxSize(),
                    painter = BitmapPainter(qrCode),
                    tint = IconColor.Accent,
                )
            }
            Avatar(
                modifier = Modifier.size(64.dp),
                content = avatarContent,
            )
        }

        ContactLinkRow(contactLink = contactLink, onCopyContactLink = onCopyContactLink)
    }
}

/**
 * The contact link with its copy affordance. The link is kept to a single ellipsised line so the
 * copy icon always has room at the end — a wrapping link pushes a button's trailing icon out
 * entirely. The whole row is the tap target, with the icon as the affordance.
 */
@Composable
private fun ContactLinkRow(
    contactLink: String,
    onCopyContactLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val copyDescription = stringResource(sharedR.string.general_copy)
    BoxSurface(
        surfaceColor = SurfaceColor.Surface1,
        modifier = modifier
            .fillMaxWidth()
            .height(ContactLinkHeight)
            .clip(ContactLinkCorner)
            .testTag(MY_QR_CODE_LINK_TAG),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClickLabel = copyDescription,
                    role = Role.Button,
                    onClick = onCopyContactLink,
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MegaText(
                modifier = Modifier.weight(1f),
                text = contactLink,
                textColor = TextColor.Primary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = AppTheme.typography.bodyLarge,
            )
            MegaIcon(
                modifier = Modifier
                    .size(24.dp)
                    .testTag(MY_QR_CODE_COPY_ICON_TAG),
                painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Copy01),
                tint = IconColor.Primary,
                contentDescription = copyDescription,
            )
        }
    }
}

/**
 * Loading placeholder shaped like the content it stands in for — the QR code square and the
 * contact link row — so the screen keeps its layout while the code is generated. The generating
 * message is carried as the placeholder's accessibility description, since a shimmer announces
 * nothing on its own.
 */
@Composable
private fun QrCodePlaceholder(modifier: Modifier = Modifier) {
    val generatingDescription = stringResource(sharedR.string.contacts_qr_generating)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = generatingDescription }
            .testTag(MY_QR_CODE_LOADING_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(QrContentSpacing),
    ) {
        Spacer(
            modifier = Modifier
                .padding(top = QrCodeTopPadding)
                .size(QrCodeSize)
                .shimmerEffect(shape = ContactLinkCorner),
        )
        BoxSurface(
            surfaceColor = SurfaceColor.Surface1,
            modifier = Modifier
                .fillMaxWidth()
                .height(ContactLinkHeight)
                .clip(ContactLinkCorner),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(ContactLinkTextHeight)
                        .shimmerEffect(),
                )
                Spacer(
                    modifier = Modifier
                        .size(24.dp)
                        .shimmerEffect(),
                )
            }
        }
    }
}

private fun MyQrCodeMessage.toText(context: Context): String = when (this) {
    MyQrCodeMessage.ContactLinkCopied ->
        context.getString(sharedR.string.general_link_copied_clipboard)

    MyQrCodeMessage.SaveError,
    MyQrCodeMessage.GeneralError,
        -> context.getString(sharedR.string.general_text_error)

    MyQrCodeMessage.NotEnoughSpace ->
        context.getString(sharedR.string.general_transfer_error_not_enough_free_space)

    is MyQrCodeMessage.SaveSuccess ->
        context.getString(sharedR.string.contacts_qr_save_success, destinationPath)

    MyQrCodeMessage.ResetDone ->
        context.getString(sharedR.string.contacts_qr_reset_success)

    is MyQrCodeMessage.OperationError -> context.getString(
        when (error) {
            MyQrCodeError.ResetFailed -> sharedR.string.contacts_qr_reset_error
            MyQrCodeError.DeleteFailed -> sharedR.string.contacts_qr_delete_error
            MyQrCodeError.General -> sharedR.string.general_text_error
        },
    )
}

/**
 * Overflow action that saves the QR code image to the device.
 */
internal data object SaveQrCodeMenuAction : MenuActionWithoutIcon(
    descriptionRes = sharedR.string.general_save_to_device,
    testTag = MY_QR_CODE_SAVE_ACTION_TAG,
)

/**
 * Overflow action that resets the QR code, generating a new contact link.
 */
internal data object ResetQrCodeMenuAction : MenuActionWithoutIcon(
    descriptionRes = sharedR.string.contacts_qr_reset_action,
    testTag = MY_QR_CODE_RESET_ACTION_TAG,
)

/**
 * Overflow action that deletes the QR code and its contact link.
 */
internal data object DeleteQrCodeMenuAction : MenuActionWithoutIcon(
    descriptionRes = sharedR.string.contacts_qr_delete_action,
    testTag = MY_QR_CODE_DELETE_ACTION_TAG,
)

internal const val MY_QR_CODE_SCREEN_TAG = "my_qr_code_screen"
internal const val MY_QR_CODE_IMAGE_TAG = "my_qr_code_screen:image_qr_code"
internal const val MY_QR_CODE_LINK_TAG = "my_qr_code_screen:button_contact_link"
internal const val MY_QR_CODE_COPY_ICON_TAG = "my_qr_code_screen:icon_copy_contact_link"
internal const val MY_QR_CODE_CREATE_TAG = "my_qr_code_screen:button_create"
internal const val MY_QR_CODE_SCAN_TAG = "my_qr_code_screen:button_scan"
internal const val MY_QR_CODE_LOADING_TAG = "my_qr_code_screen:progress_generating"
internal const val MY_QR_CODE_SAVE_ACTION_TAG = "my_qr_code_screen:action_save"
internal const val MY_QR_CODE_RESET_ACTION_TAG = "my_qr_code_screen:action_reset"
internal const val MY_QR_CODE_DELETE_ACTION_TAG = "my_qr_code_screen:action_delete"

@CombinedThemePreviews
@Composable
private fun MyQrCodeScreenPreview(
    @PreviewParameter(MyQrCodeContentStateProvider::class) contentState: MyQrCodeUiState,
) {
    AndroidThemeForPreviews {
        MyQrCodeScreen(
            uiState = contentState,
            onBackClick = {},
            onCreateQRCode = {},
            onCopyContactLink = {},
            onSaveToFile = {},
            onResetQRCode = {},
            onDeleteQRCode = {},
            onScanClick = {},
            onMessageConsumed = {},
        )
    }
}

private class MyQrCodeContentStateProvider : PreviewParameterProvider<MyQrCodeUiState> {
    override val values = sequenceOf(
        MyQrCodeUiState.QRCodeAvailable(
            qrCode = QrCode(
                contactLink = "https://mega.nz/C!abcdef",
                avatarContent = TextAvatarContent(
                    avatarText = "A",
                    backgroundColor = 0xFFF30C14.toInt(),
                    showBorder = true,
                    textSize = 38.sp,
                ),
                avatarBgColor = 0xFFF30C14.toInt(),
                qrCodeFilePath = null,
            ),
            message = consumed()
        ),
        MyQrCodeUiState.QRCodeUnavailable(
            message = consumed()
        ),
    )
}
