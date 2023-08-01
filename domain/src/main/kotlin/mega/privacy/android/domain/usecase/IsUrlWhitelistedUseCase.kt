package mega.privacy.android.domain.usecase

import javax.inject.Inject

/**
 * Checks whether a certain Url is Whitelisted in MEGA app
 */
class IsUrlWhitelistedUseCase @Inject constructor() {
    private val whitelistedUrlList = listOf(
        PLAY_STORE_URL,
        APP_STORE_URL
    )

    /**
     * When invoked, this method checks whether the passed Url matches the set of whitelisted and allowed
     * Url, because of the limitation we imposed on the WebView. All Urls except MEGA's & this Whitelisted
     * Url list are blocked in MEGA app for security reasons.
     * @param url as the url to check
     */
    operator fun invoke(url: String?) = whitelistedUrlList.contains(url)

    companion object {
        /**
         * Landing Page / Download URL for MEGA app in Google Play Store
         */
        const val PLAY_STORE_URL =
            "https://play.google.com/store/apps/details?id=mega.privacy.android.app&referrer=meganzmobileapps"

        /**
         * Landing Page / Download URL for MEGA app in Apple App Store
         */
        const val APP_STORE_URL = "https://apps.apple.com/app/mega/id706857885"
    }
}