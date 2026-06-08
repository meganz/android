package mega.privacy.android.app.presentation.verifytwofactor.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * UI state for the verify-2FA screen.
 *
 * @property verifyType Which sensitive action is being gated (one of [CANCEL_ACCOUNT_2FA],
 *                      [CHANGE_MAIL_2FA], [DISABLE_2FA], [CHANGE_PASSWORD_2FA]).
 * @property pin Current PIN typed by the user (length 0..6).
 * @property isPinError True when the SDK rejected the last submitted PIN.
 * @property isLoading True while a verification request is in flight.
 * @property is2FAEnabled Defensive flag mirroring the SDK check; suppresses the error UI when 2FA
 *                       was already disabled on another device.
 * @property isBackEnabled False while a non-cancellable request is in flight.
 * @property recoveryUrl Pre-resolved URL for the "lost authenticator device" link.
 * @property resultEvent Result dialog to render (mapped to a [VerifyTwoFactorResult]).
 * @property passwordChangedEvent Follow-up navigation after a successful password change.
 */
data class VerifyTwoFactorUiState(
    val verifyType: Int = 0,
    val pin: String = "",
    val isPinError: Boolean = false,
    val isLoading: Boolean = false,
    val is2FAEnabled: Boolean = true,
    val isBackEnabled: Boolean = true,
    val recoveryUrl: String = "",
    val resultEvent: StateEventWithContent<VerifyTwoFactorResult> = consumed(),
    val passwordChangedEvent: StateEventWithContent<PasswordChangedAction> = consumed(),
    val logoutEvent: StateEvent = consumed,
    val disableSuccessEvent: StateEvent = consumed,
    val finishEvent: StateEvent = consumed,
)
