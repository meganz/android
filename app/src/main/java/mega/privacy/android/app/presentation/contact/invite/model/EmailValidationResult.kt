package mega.privacy.android.app.presentation.contact.invite.model

import mega.privacy.android.app.main.model.InviteContactUiState.MessageTypeUiState

/**
 * Represents the email validation result.
 */
sealed interface EmailValidationResult {

    /**
     * The email is valid.
     */
    data object ValidResult : EmailValidationResult

    /**
     * The email is invalid.
     *
     * @param message The invalid message.
     */
    data class InvalidResult(val message: MessageTypeUiState) : EmailValidationResult
}
