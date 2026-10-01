package mega.privacy.android.domain.entity.settings.cookie

/**
 * Data class to represent cookie dialog
 *
 * @param dialogType Type of cookie dialog.
 * @param url        Url to be opened when the Cookie Policy link is clicked
 */
data class CookieDialog(
    val dialogType: CookieDialogType = CookieDialogType.None,
    val url: String? = null,
)
