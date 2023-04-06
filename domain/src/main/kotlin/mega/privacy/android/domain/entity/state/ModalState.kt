package mega.privacy.android.domain.entity.state

/**
 * Modal state
 *
 * @constructor Create empty Modal state
 */
sealed interface ModalState {
    /**
     * Upgrade required
     *
     * @property accountType
     * @constructor Create empty Upgrade required
     */
    data class UpgradeRequired(val accountType: Int?) : ModalState

    /**
     * Verify phone number
     */
    object VerifyPhoneNumber : ModalState

    /**
     * Request initial permissions
     */
    object RequestInitialPermissions : ModalState

    /**
     * Request two factor authentication
     */
    object RequestTwoFactorAuthentication : ModalState

    /**
     * FirstLogin
     */
    object FirstLogin : ModalState

    /**
     * ExpiredBusinessAccountGracePeriod
     */
    object ExpiredBusinessAccountGracePeriod : ModalState

    /**
     * ExpiredBusinessAccount
     */
    object ExpiredBusinessAccount : ModalState
}