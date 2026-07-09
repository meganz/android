package mega.privacy.android.feature.sharelink.presentation

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.changepassword.PasswordStrength

/**
 * UI state for the Link settings editor screen.
 */
@Stable
data class LinkSettingsUiState(
    val isLoading: Boolean = true,
    val isSeparateKeyEnabled: Boolean = false,
    val isExpiryEnabled: Boolean = false,
    val expiryDate: Long? = null,
    val isPasswordEnabled: Boolean = false,
    val password: String? = null,
    val passwordStrength: PasswordStrength? = null,
    val accountType: AccountType? = null,
    val hasUnsavedChanges: Boolean = false,
    val isSaveEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val savedEvent: StateEvent = consumed,
    val errorEvent: StateEvent = consumed,
)
