package mega.privacy.android.app.presentation.login.confirmemail.updateEmail

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey
import mega.privacy.android.navigation.contract.suppression.withOverlaySuppression

@Serializable
data class UpdateEmailForAccountCreationScreen(
    val email: String?,
    val fullName: String?,
) : NoSessionNavKey.Mandatory


internal fun EntryProviderScope<NavKey>.updateEmailForAccountCreation(
    onChangeEmailSuccess: (String) -> Unit,
) {
    entry<UpdateEmailForAccountCreationScreen>(
        metadata = buildMetadata { withOverlaySuppression() }
    ) { key ->
        val viewModel =
            hiltViewModel<UpdateEmailForAccountCreationViewModel, UpdateEmailForAccountCreationViewModel.Factory>(
                creationCallback = { it.create(key.email, key.fullName) }
            )
        UpdateEmailForAccountCreationRoute(
            viewModel = viewModel,
            onChangeEmailSuccess = onChangeEmailSuccess
        )
    }
}
