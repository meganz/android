package mega.privacy.android.app.presentation.browser

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService
import androidx.core.net.toUri
import timber.log.Timber

/**
 * Opens a URL in a Custom Tab, or in a browser if no browser supports Custom Tabs.
 *
 * The intent is bound to a browser package, because the App Links of the MEGA domains otherwise
 * send the URL back to this app.
 *
 * @param context Context that starts the browser
 * @param url URL to open. It can contain a link key or a session, so it must never be logged.
 * @return true if a browser was opened
 */
internal fun launchInBrowser(context: Context, url: String): Boolean {
    val uri = url.toUri()
    return runCatching {
        when (val browser = selectBrowser(
            customTabsPackage = CustomTabsClient.getPackageName(
                context,
                context.findCustomTabsProviders(),
            ),
            defaultBrowserPackage = context.findDefaultBrowserPackage(),
            browserPackages = context.findBrowserPackages(),
            appPackage = context.packageName,
        )) {
            is SelectedBrowser.CustomTab -> CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setDownloadButtonEnabled(false)
                .setBookmarksButtonEnabled(false)
                .setSendToExternalDefaultHandlerEnabled(false)
                .build()
                .apply {
                    intent.setPackage(browser.packageName)
                    intent.putExtra(
                        Intent.EXTRA_REFERRER,
                        "android-app://${context.packageName}".toUri(),
                    )
                }
                .launchUrl(context, uri)

            is SelectedBrowser.Browser -> context.startActivity(
                Intent(Intent.ACTION_VIEW, uri)
                    .setPackage(browser.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )

            null -> throw IllegalStateException("No browser is installed")
        }
    }.onFailure {
        // Log only the exception class, because the exception message can contain the intent
        // data, which includes the URL.
        Timber.e("Failed to open the URL in a browser: ${it.javaClass.simpleName}")
    }.isSuccess
}

/**
 * Browser that opens the URL
 */
internal sealed interface SelectedBrowser {
    /**
     * Package name of the browser
     */
    val packageName: String

    /**
     * Browser that supports Custom Tabs
     */
    data class CustomTab(override val packageName: String) : SelectedBrowser

    /**
     * Browser that does not support Custom Tabs
     */
    data class Browser(override val packageName: String) : SelectedBrowser
}

/**
 * Selects the browser that opens the URL. A Custom Tabs provider comes first. This app and the
 * system resolver are never selected, because the URL must not come back to this app.
 *
 * @param customTabsPackage Custom Tabs provider, as returned by [CustomTabsClient.getPackageName]
 * @param defaultBrowserPackage Default browser, or null if the user has not selected one
 * @param browserPackages Installed browsers
 * @param appPackage Package name of this app
 * @return the browser, or null if no browser can open the URL
 */
internal fun selectBrowser(
    customTabsPackage: String?,
    defaultBrowserPackage: String?,
    browserPackages: List<String>,
    appPackage: String,
): SelectedBrowser? {
    val excludedPackages = setOf(appPackage, SYSTEM_RESOLVER_PACKAGE)
    customTabsPackage?.takeUnless { it in excludedPackages }?.let {
        return SelectedBrowser.CustomTab(it)
    }
    val browserPackage = defaultBrowserPackage?.takeUnless { it in excludedPackages }
        ?: browserPackages.firstOrNull { it !in excludedPackages }
    return browserPackage?.let { SelectedBrowser.Browser(it) }
}

private fun Context.findCustomTabsProviders(): List<String> = packageManager
    .queryIntentServices(Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION), 0)
    .map { it.serviceInfo.packageName }

private fun Context.findDefaultBrowserPackage(): String? = packageManager
    .resolveActivity(browserIntent(), PackageManager.MATCH_DEFAULT_ONLY)
    ?.activityInfo
    ?.packageName

private fun Context.findBrowserPackages(): List<String> = packageManager
    .queryIntentActivities(browserIntent(), 0)
    .map { it.activityInfo.packageName }

/**
 * Intent for a URL that no app claims as an App Link, so that it resolves to browsers only.
 */
private fun browserIntent() = Intent(Intent.ACTION_VIEW, "https://www.example.com".toUri())
    .addCategory(Intent.CATEGORY_BROWSABLE)

/**
 * Package that [PackageManager.resolveActivity] returns when no default app is selected
 */
private const val SYSTEM_RESOLVER_PACKAGE = "android"
