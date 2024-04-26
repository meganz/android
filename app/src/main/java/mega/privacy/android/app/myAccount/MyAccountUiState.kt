package mega.privacy.android.app.myAccount

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.Feature

/**
 * My account ui state
 *
 * @property isFileVersioningEnabled
 * @property versionsInfo
 * @property name
 * @property email
 * @property changeEmailResult
 * @property isLoading
 * @property changeUserNameResult
 * @property verifiedPhoneNumber
 * @property canVerifyPhoneNumber
 * @property isBusinessAccount
 * @property backupStorageSize
 * @property errorMessage The Error Message to be displayed
 * @property errorMessageRes A [StringRes] version of the Error Message to be displayed
 * @property shouldNavigateToSmsVerification
 * @property showInvalidChangeEmailLinkPrompt true if a prompt should be shown explaining that the
 * link to change the User's email is invalid
 * @property showChangeEmailConfirmation true if a confirmation should be shown explaining that the
 * @property showNewCancelSubscriptionFeature true if the new Cancel Subscription feature should be shown
 * User's Email Address will be changed
 */
data class MyAccountUiState(
    val isFileVersioningEnabled: Boolean = true,
    val name: String = "",
    val email: String = "",
    val versionsInfo: String? = null,
    val changeEmailResult: Result<String>? = null,
    val isLoading: Boolean = false,
    val changeUserNameResult: Result<Unit>? = null,
    val verifiedPhoneNumber: String? = null,
    val canVerifyPhoneNumber: Boolean = false,
    val isBusinessAccount: Boolean = false,
    val backupStorageSize: Long = 0L,
    val errorMessage: String = "",
    @StringRes val errorMessageRes: Int? = null,
    val shouldNavigateToSmsVerification: Boolean = false,
    val showInvalidChangeEmailLinkPrompt: Boolean = false,
    val showChangeEmailConfirmation: Boolean = false,
    val showNewCancelSubscriptionFeature: Boolean? = null,
)
