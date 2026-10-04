package mega.privacy.android.feature.contact.qrcode.navigation

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.contact.qrcode.mycode.MyQrCodeViewModel
import mega.privacy.android.feature.contact.qrcode.mycode.model.MyQrCodeUiState
import mega.privacy.android.feature.contact.qrcode.mycode.view.MyQrCodeScreen
import mega.privacy.android.feature.contact.qrcode.scan.ScanQrCodeViewModel
import mega.privacy.android.feature.contact.qrcode.scan.view.ScanQrCodeScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.navigation.destination.QrCodeNavKey
import mega.privacy.android.shared.nodes.namecollision.rememberNameCollisionLauncher
import mega.privacy.android.shared.transfers.components.UploadingFiles
import mega.privacy.android.shared.transfers.components.rememberUploadUrisEventState
import java.io.File

/**
 * Wires the [MyQrCodeScreen] and [ScanQrCodeScreen] to their independent view models. Both are
 * created in this entry: the personal screen owns the scaffold and the "Scan QR code" button
 * triggers [ScanQrCodeViewModel.scanCode], whose results are surfaced by the scan screen.
 *
 * Saving the QR code to the Cloud drive hands the image to [UploadingFiles]; from there on quota,
 * name collisions and progress are the upload flow's concern, not this entry's.
 */
internal fun EntryProviderScope<NavKey>.qrCode(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<QrCodeNavKey> {
        val myQrCodeViewModel: MyQrCodeViewModel = hiltViewModel()
        val scanQrCodeViewModel: ScanQrCodeViewModel = hiltViewModel()
        val myQrCodeState by myQrCodeViewModel.uiState.collectAsStateWithLifecycle()
        val scanState by scanQrCodeViewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val uploadUrisEventState = rememberUploadUrisEventState()
        val nameCollisionLauncher = rememberNameCollisionLauncher(navigationHandler::navigate)

        EventEffect(
            event = (myQrCodeState as? MyQrCodeUiState.QRCodeAvailable)?.uploadQrCodeEvent
                ?: consumed(),
            onConsumed = myQrCodeViewModel::onUploadQrCodeEventConsumed,
        ) { uriPath ->
            uploadUrisEventState.trigger(listOf(context.toFileProviderUri(uriPath)))
        }

        UploadingFiles(
            onNameCollisions = nameCollisionLauncher::launch,
            parentNodeId = NodeId(-1L),
            urisEvent = uploadUrisEventState.event,
            onUrisConsumed = uploadUrisEventState::consume,
            pitagTrigger = PitagTrigger.NotApplicable,
            onStartUpload = transferHandler::setTransferEvent,
        )

        MyQrCodeScreen(
            uiState = myQrCodeState,
            onBackClick = navigationHandler::back,
            onCreateQRCode = myQrCodeViewModel::createQRCode,
            onCopyContactLink = myQrCodeViewModel::copyContactLink,
            onSaveToFile = myQrCodeViewModel::saveToFileSystem,
            onSaveToCloudDrive = myQrCodeViewModel::saveToCloudDrive,
            onResetQRCode = myQrCodeViewModel::resetQRCode,
            onDeleteQRCode = myQrCodeViewModel::deleteQRCode,
            onScanClick = scanQrCodeViewModel::scanCode,
            onMessageConsumed = myQrCodeViewModel::onMessageConsumed,
        )

        ScanQrCodeScreen(
            uiState = scanState,
            onViewContact = { email -> navigationHandler.navigate(ContactInfoNavKey(email = email)) },
            onInviteContact = scanQrCodeViewModel::sendInvite,
            onScannedContactLinkResultConsumed = scanQrCodeViewModel::onScannedContactLinkResultConsumed,
            onInviteContactResultConsumed = scanQrCodeViewModel::onInviteContactResultConsumed,
            onScanCancelConsumed = scanQrCodeViewModel::onScanCancelConsumed,
            onMessageConsumed = scanQrCodeViewModel::onMessageConsumed,
            onScannedContactEmailReset = scanQrCodeViewModel::resetScannedContactEmail,
            onScannedContactAvatarReset = scanQrCodeViewModel::resetScannedContactAvatar,
        )
    }
}

/**
 * The upload flow refuses `file://` uris that resolve into the app's private storage, and the QR
 * image lives in the cache directory, so it is exposed through the app's own [FileProvider].
 */
private fun Context.toFileProviderUri(uriPath: UriPath): Uri = FileProvider.getUriForFile(
    this,
    "$packageName.providers.fileprovider",
    File(uriPath.value),
)
