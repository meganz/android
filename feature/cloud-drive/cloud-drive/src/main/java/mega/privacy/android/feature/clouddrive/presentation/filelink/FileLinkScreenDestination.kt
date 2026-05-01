package mega.privacy.android.feature.clouddrive.presentation.filelink

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.contract.suppression.withOverlaySuppression
import mega.privacy.android.navigation.destination.AdConsentDialogNavKey
import mega.privacy.android.navigation.destination.CookieDialogNavKey
import mega.privacy.android.navigation.destination.FileLinkNavKey
import mega.privacy.android.navigation.destination.LegacyFileLinkNavKey

@OptIn(ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.fileLinkScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<FileLinkNavKey>(
        metadata = buildMetadata {
            withOverlaySuppression(AdConsentDialogNavKey, CookieDialogNavKey)
        }
    ) { key ->
        FeatureFlagGate(
            feature = ApiFeatures.FileLinkRevamp,
            disabled = {
                LaunchedEffect(Unit) {
                    navigationHandler.remove(key)
                    navigationHandler.navigate(LegacyFileLinkNavKey(key.uriString))
                }
            }
        ) {
            FileLinkScreen(
                onBack = navigationHandler::back,
                onNavigate = navigationHandler::navigate,
            )
        }
    }
}
