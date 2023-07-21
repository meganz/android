package mega.privacy.android.app.utils


/**
 * Landing Page / Download URL for MEGA app in Google Play Store
 */
const val PLAY_STORE_URL =
    "https://play.google.com/store/apps/details?id=mega.privacy.android.app&referrer=meganzmobileapps"

/**
 * Landing Page / Download URL for MEGA app in Apple App Store
 */
const val APP_STORE_URL = "https://apps.apple.com/app/mega/id706857885"

private val whitelistedURL = listOf(
    PLAY_STORE_URL,
    APP_STORE_URL
)

/**
 * Extension to Check whether URL is allowed to be loaded in the WebView
 * Pass a URL here as a [String]
 * This checks whether a URL is null or blank, will return false if null or blank
 * Then checks if a URL matches Mega regex pattern, will return true if matches
 * Or, it will check if a URL is whitelisted, will return true if whitelisted
 */
fun String?.isURLSanitizedForWebView(): Boolean =
    !this.isNullOrBlank() &&
            (Util.matchRegexs(this, Constants.MEGA_REGEXS) || whitelistedURL.contains(this))