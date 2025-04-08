package mega.privacy.android.app.extensions

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import androidx.annotation.NonNull
import androidx.browser.customtabs.CustomTabsIntent
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.Util
import timber.log.Timber

/**
 * Navigate to the App's Settings page to manually grant the requested permissions
 */
@NonNull
fun Context.navigateToAppSettings() {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    try {
        startActivity(intent)
    } catch (e: Exception) {
        if (this is ManagerActivity) {
            // in case few devices cannot handle 'ACTION_APPLICATION_DETAILS_SETTINGS' action.
            Util.showSnackbar(
                this,
                getString(R.string.on_permanently_denied)
            )
        } else {
            Timber.e(e, "Exception opening device settings")
        }
    }
}

/**
 * Check if device is in Portrait mode
 */
fun Context.isPortrait() =
    resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

/**
 * Check if device is tablet or not
 * @return true if device is tablet false otherwise
 */
fun Context.isTablet() =
    ((resources.configuration.screenLayout
            and Configuration.SCREENLAYOUT_SIZE_MASK)
            >= Configuration.SCREENLAYOUT_SIZE_LARGE)

/**
 * Extension to launch a URL with multiple fallback strategies from context
 *
 * The method first attempts to launch the URL using Chrome Custom Tabs. If that fails,
 * it falls back to launching in WebViewActivity, and finally the default browser.
 */
fun Context?.launchUrl(url: String?) {
    if (url.isNullOrEmpty()) {
        Timber.w("URL is null or empty")
        return
    }
    if (this == null) {
        Timber.w("Context is null")
        return
    }

    val uri = runCatching { Uri.parse(url) }.getOrElse {
        Timber.e(it, "Failed to parse URL")
        return
    }

    fun Intent.applyNewTaskFlag() = apply {
        if (this@launchUrl is Application) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching {
        // Don't open URL in Custom Tabs if it's a deeplink
        val deeplinkHosts = setOf(
            "mega.co.nz",
            "www.mega.co.nz",
            "mega.nz",
            "www.mega.nz"
        )
        if (deeplinkHosts.contains(uri.host) || uri.scheme == "mega") {
            throw IllegalArgumentException("URL is a deeplink")
        }
        CustomTabsIntent.Builder()
            .apply {
                setTheme(R.style.Theme_Mega)
                setShowTitle(true)
                setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                setDownloadButtonEnabled(false)
                setBookmarksButtonEnabled(false)
                setSendToExternalDefaultHandlerEnabled(false)
            }
            .build()
            .also {
                it.intent.putExtra(
                    Intent.EXTRA_REFERRER,
                    Uri.parse("android-app://$packageName")
                )
                it.intent.applyNewTaskFlag()
            }.launchUrl(this, uri)
    }.recoverCatching { e ->
        Timber.e(e, "Falling back to WebViewActivity")
        startActivity(
            Intent(this, WebViewActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                applyNewTaskFlag()
                data = uri
            }
        )
    }.recoverCatching { e ->
        Timber.e(e, "Falling back to default browser")
        startActivity(Intent(Intent.ACTION_VIEW, uri).applyNewTaskFlag())
    }.onFailure {
        Timber.e(it, "Failed to launch URL")
    }
}