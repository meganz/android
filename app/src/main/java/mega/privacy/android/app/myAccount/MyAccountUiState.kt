package mega.privacy.android.app.myAccount

import java.io.File

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
 * @property avatar
 */
data class MyAccountUiState(
    val isFileVersioningEnabled: Boolean = true,
    val versionsInfo: String? = null,
    val name: String = "",
    val email: String = "",
    val changeEmailResult: Result<String>? = null,
    val isLoading: Boolean = false,
    val changeUserNameResult: Result<Unit>? = null,
    val verifiedPhoneNumber: String? = null,
    val canVerifyPhoneNumber: Boolean = false,
    val avatar: File? = null,
    val avatarColor: Int? = null,
    val isBusinessAccount: Boolean = false,
    val isMasterBusinessAccount: Boolean = false,
    val isAchievementsEnabled: Boolean = false,
    val isBusinessStatusActive: Boolean = false,
    val visibleContacts: Int? = null,
)