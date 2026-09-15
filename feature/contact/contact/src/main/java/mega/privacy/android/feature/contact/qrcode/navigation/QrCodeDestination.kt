package mega.privacy.android.feature.contact.qrcode.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.feature.contact.qrcode.mycode.MyQrCodeViewModel
import mega.privacy.android.feature.contact.qrcode.mycode.view.MyQrCodeScreen
import mega.privacy.android.feature.contact.qrcode.scan.ScanQrCodeViewModel
import mega.privacy.android.feature.contact.qrcode.scan.view.ScanQrCodeScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.ContactInfoNavKey

/**
 * Navigation key for the QR code screen. It hosts both the personal "My QR code" UI and the QR
 * scan / invite flow, mirroring the single legacy QR screen where scanning is an action on the
 * personal screen rather than a separate destination.
 */
@Serializable
data object QrCodeNavKey : NavKey

/**
 * Wires the [MyQrCodeScreen] and [ScanQrCodeScreen] to their independent view models. Both are
 * created in this entry: the personal screen owns the scaffold and the "Scan QR code" button
 * triggers [ScanQrCodeViewModel.scanCode], whose results are surfaced by the scan screen.
 */
internal fun EntryProviderScope<NavKey>.qrCode(navigationHandler: NavigationHandler) {
    entry<QrCodeNavKey> {
        val myQrCodeViewModel: MyQrCodeViewModel = hiltViewModel()
        val scanQrCodeViewModel: ScanQrCodeViewModel = hiltViewModel()
        val myQrCodeState by myQrCodeViewModel.uiState.collectAsStateWithLifecycle()
        val scanState by scanQrCodeViewModel.uiState.collectAsStateWithLifecycle()



        MyQrCodeScreen(
            uiState = myQrCodeState,
            onBackClick = navigationHandler::back,
            onCreateQRCode = myQrCodeViewModel::createQRCode,
            onCopyContactLink = myQrCodeViewModel::copyContactLink,
            onSaveToFile = myQrCodeViewModel::saveToFileSystem,
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

