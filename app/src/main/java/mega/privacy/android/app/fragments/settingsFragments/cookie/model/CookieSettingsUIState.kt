package mega.privacy.android.app.fragments.settingsFragments.cookie.model

/**
 * UI state for Cookie Settings
 *
 * @property showAdsCookiePreference Show Ads cookie preference
 * @property cookiePolicyWithAdsLink Cookie policy link
 */
data class CookieSettingsUIState(
    val showAdsCookiePreference: Boolean = false,
    val cookiePolicyWithAdsLink: String? = null,
)
