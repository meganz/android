package mega.privacy.android.feature.sharelink.presentation

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.changepassword.PasswordStrength

/**
 * UI state for the Link settings editor screen.
 *
 * @property isExpiryAlreadySet Whether the link already had an expiry date when the screen opened,
 * so the expiry toggle starts on and Save only enables on an actual change or removal.
 * @property initialExpiryDate The existing expiry date, in UTC milliseconds, used as the baseline
 * for detecting an actual change; null when no expiry was set.
 * @property isPasswordAlreadySet Whether the link already had a password when the screen opened,
 * so the password toggle starts on and Save only enables on an actual change or removal.
 * @property initialPassword The existing plaintext password pre-filled for change/remove, used as
 * the baseline for detecting an actual change; null when no password was set.
 */
@Stable
data class LinkSettingsUiState(
    val isLoading: Boolean = true,
    val isSeparateKeyEnabled: Boolean = false,
    val isExpiryEnabled: Boolean = false,
    val expiryDate: Long? = null,
    val isExpiryAlreadySet: Boolean = false,
    val initialExpiryDate: Long? = null,
    val isPasswordEnabled: Boolean = false,
    val isPasswordAlreadySet: Boolean = false,
    val initialPassword: String? = null,
    val password: String? = null,
    val passwordStrength: PasswordStrength? = null,
    val accountType: AccountType? = null,
    val hasUnsavedChanges: Boolean = false,
    val isSaveEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val savedEvent: StateEvent = consumed,
    val errorEvent: StateEvent = consumed,
)
