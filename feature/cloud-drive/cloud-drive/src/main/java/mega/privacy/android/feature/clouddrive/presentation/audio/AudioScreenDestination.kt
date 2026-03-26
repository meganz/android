package mega.privacy.android.feature.clouddrive.presentation.audio

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.AudioNavKey

/**
 * Registers the Compose Audio destination for [AudioNavKey].
 * Feature-flag routing (Compose vs legacy activity) lives in the app module on `AudioSectionNavKey`.
 */
fun EntryProviderScope<NavKey>.audioScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<AudioNavKey> {
        // Implement the behavior to navigate to AudioScreen in next ticket
    }
}

