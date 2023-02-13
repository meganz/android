package mega.privacy.android.app.myAccount

/**
 * My account ui state
 *
 * @property isFileVersioningEnabled
 * @property versionsInfo
 * @property name
 * @property changeEmailResult
 * @property isLoading
 * @property changeUserNameResult
 */
data class MyAccountUiState(
    val isFileVersioningEnabled: Boolean = true,
    val versionsInfo: String? = null,
    val name: String = "",
    val email: String = "",
    val changeEmailResult: Result<String>? = null,
    val isLoading: Boolean = false,
    val changeUserNameResult: Result<Unit>? = null,
)