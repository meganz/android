package mega.privacy.android.app.presentation.contact.invite.navigation

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.contact.invite.InviteContactActivity
import mega.privacy.android.app.presentation.contact.invite.InviteContactViewModel
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.destination.InviteContactNavKey

/**
 * Registers the [InviteContactNavKey] destination. Behind [ApiFeatures.ContactComposeFeature] either
 * renders the Compose [InviteContactAppHost] invite screen (flag on) or launches the legacy
 * [InviteContactActivity] and pops itself (flag off).
 */
fun EntryProviderScope<NavKey>.inviteContactLegacyDestination(navigationHandler: NavigationHandler) {
    entry<InviteContactNavKey> { navKey ->
        FeatureFlagGate(
            feature = ApiFeatures.ContactComposeFeature,
            disabled = {
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    val intent = Intent(context, InviteContactActivity::class.java).apply {
                        putExtra(InviteContactViewModel.KEY_FROM, navKey.isFromAchievement)
                    }
                    context.startActivity(intent)
                    navigationHandler.back()
                }
            },
            enabled = {
                InviteContactAppHost(
                    navigationHandler = navigationHandler,
                    isFromAchievement = navKey.isFromAchievement,
                )
            },
        )
    }
}
