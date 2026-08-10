package mega.privacy.android.app.activities.destinations

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.presentation.openlink.OpenLinkActivity
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.WebSiteNavKey

fun EntryProviderScope<NavKey>.webDestinations(removeDestination: () -> Unit) {
    entry<WebSiteNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            if (key.isBrowserLink) {
                openWebLinkInBrowser(key.url, context)
            } else {
                context.launchUrl(key.url)
            }

            removeDestination()
        }
    }
}

private fun openWebLinkInBrowser(url: String, context: Context) {
    with(context) {
        val intent = Intent(ACTION_VIEW).apply { data = url.toUri() }

        // On Android 12+ devices, Intent.createChooser cannot properly show browser list.
        // So workaround here: get list of browsers and insert into initial list of
        // chooser.
        val initialBrowserList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val browserActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }
            browserActivities
                .filterNot { it.activityInfo.packageName.contains(packageName) }
                .map {
                    Intent(ACTION_VIEW, url.toUri()).apply {
                        `package` = it.activityInfo.packageName
                    }
                }.takeIf { it.isNotEmpty() }
        } else {
            null
        }

        val chooserIntent = Intent.createChooser(intent, null).apply {
            putExtra(
                Intent.EXTRA_EXCLUDE_COMPONENTS,
                arrayOf(
                    ComponentName(context, OpenLinkActivity::class.java)
                )
            )

            initialBrowserList?.let {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, it.toTypedArray())
            }
        }
        startActivity(chooserIntent)
    }
}