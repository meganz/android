package mega.privacy.android.app.presentation.login.onboarding

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.onboarding.view.NewTourRoute
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey
import mega.privacy.android.navigation.contract.suppression.withOverlaySuppression

@Serializable
data object TourNavKey : NoSessionNavKey.Mandatory

internal fun EntryProviderScope<NavKey>.tourScreen(
    sharedViewModel: LoginViewModel,
    onBackPressed: () -> Unit,
) {
    entry<TourNavKey>(
        metadata = buildMetadata { withOverlaySuppression() }
    ) { key ->
        NewTourRoute(
            activityViewModel = sharedViewModel,
            onBackPressed = onBackPressed,
        )
    }
}
