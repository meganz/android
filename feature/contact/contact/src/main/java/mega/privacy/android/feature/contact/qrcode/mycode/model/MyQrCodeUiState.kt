package mega.privacy.android.feature.contact.qrcode.mycode.model

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEventWithContent

/**
 * UI state for the personal "My QR code" screen.
 *
 * @property contentState the current state of the QR code content
 * @property message a one-shot message for the UI to surface, if any
 */
@Stable
data class MyQrCodeUiState(
    val contentState: MyQrCodeContentState,
    val message: StateEventWithContent<MyQrCodeMessage>,
)
