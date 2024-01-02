package mega.privacy.android.domain.entity.settings.cookie

/**
 * Enum class to represent cookie dialog types.
 */
enum class CookieDialogType {

    /**
     * Show generic cookie dialog.
     */
    GenericCookieDialog,

    /**
     * Show cookie dialog with ads.
     */
    CookieDialogWithAds,

    /**
     * Do not show any cookie dialog.
     */
    None
}