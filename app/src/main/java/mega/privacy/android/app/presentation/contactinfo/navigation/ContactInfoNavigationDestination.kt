package mega.privacy.android.app.presentation.contactinfo.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.feature.contact.info.navigation.ContactInfoEntry
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.ContactInfoNavKey

/**
 * Registers the [ContactInfoNavKey] destination. Behind [AppFeatures.ContactInfoComposeUI] either
 * renders the Compose [ContactInfoEntry] contact info screen (flag on) or launches the legacy
 * [ContactInfoActivity] and pops itself (flag off).
 */
fun EntryProviderScope<NavKey>.contactInfoDestination(navigationHandler: NavigationHandler) {
    entry<ContactInfoNavKey> { navKey ->
        FeatureFlagGate(
            feature = AppFeatures.ContactInfoComposeUI,
            disabled = {
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    val intent = Intent(context, ContactInfoActivity::class.java)
                    navKey.email?.let { intent.putExtra(Constants.NAME, it) }
                    navKey.chatId?.let { intent.putExtra(Constants.HANDLE, it) }
                    context.startActivity(intent)
                    navigationHandler.back()
                }
            },
            enabled = {
                ContactInfoEntry(
                    navigationHandler = navigationHandler,
                    email = navKey.email,
                    chatId = navKey.chatId,
                )
            },
        )
    }
}
