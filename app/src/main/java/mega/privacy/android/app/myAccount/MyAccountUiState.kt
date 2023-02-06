package mega.privacy.android.app.myAccount

/**
 * My account ui state
 *
 * @property isFileVersioningEnabled
 * @property versionsInfo
 * @property name
 * @property changeEmailResult
 */
data class MyAccountUiState(
    val isFileVersioningEnabled: Boolean = true,
    val versionsInfo: String? = null,
    val name: String = "",
    val changeEmailResult: Result<String>? = null,
)