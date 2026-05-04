package mega.privacy.android.app.presentation.login.createaccount

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.createaccount.view.NewCreateAccountRoute
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.contract.suppression.withOverlaySuppression
import mega.privacy.android.navigation.destination.CreateAccountNavKey

internal fun EntryProviderScope<NavKey>.createAccountScreen(
    sharedViewModel: LoginViewModel,
) {
    entry<CreateAccountNavKey>(
        metadata = buildMetadata { withOverlaySuppression() }
    ) { key ->
        NewCreateAccountRoute(
            activityViewModel = sharedViewModel,
            initialEmail = key.initialEmail,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
