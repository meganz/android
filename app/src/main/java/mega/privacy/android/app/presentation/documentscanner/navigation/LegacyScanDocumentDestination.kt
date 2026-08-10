package mega.privacy.android.app.presentation.documentscanner.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.documentscanner.legacy.LegacyScanDocumentLauncher
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.navkey.LegacyScanDocumentNavKey

/**
 * Feature destination that launches the legacy ML Kit document scanner.
 *
 * Reached when the continuous-scanner router resolves to a legacy fallback and
 * navigates to [LegacyScanDocumentNavKey]. On a successful scan it forwards to the
 * Save-Scanned-Documents screen; otherwise it pops itself.
 */
class LegacyScanDocumentDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            entry<LegacyScanDocumentNavKey> {
                LegacyScanDocumentLauncher(
                    onScanned = { saveKey ->
                        navigationHandler.navigate(saveKey)
                        navigationHandler.remove(LegacyScanDocumentNavKey)
                    },
                    onFinished = { navigationHandler.back() },
                )
            }
        }
}
