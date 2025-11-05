package mega.privacy.android.app.appstate.content.model

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableSet
import mega.privacy.android.domain.entity.permission.OnboardingPermissionsCheckResult
import mega.privacy.android.navigation.contract.AppDialogDestinations
import mega.privacy.android.navigation.contract.FeatureDestination

@Stable
sealed interface NavigationGraphState {
    data object Loading : NavigationGraphState

    data class Data(
        val featureDestinations: ImmutableSet<FeatureDestination>,
        val appDialogDestinations: ImmutableSet<AppDialogDestinations>,
        val onboardingPermissionsCheckResult: OnboardingPermissionsCheckResult? = null,
    ) : NavigationGraphState
}