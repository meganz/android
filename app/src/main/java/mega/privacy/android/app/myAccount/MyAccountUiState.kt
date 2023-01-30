package mega.privacy.android.app.myAccount

/**
 * My account ui state
 *
 * @property isFileVersioningEnabled
 * @property versionsInfo
 * @property name
 */
data class MyAccountUiState(
    val isFileVersioningEnabled: Boolean = true,
    val versionsInfo: String? = null,
    val name: String = ""
)