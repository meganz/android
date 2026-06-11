package mega.privacy.android.app.presentation.audiosection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.AudioNavKey
import mega.privacy.android.navigation.destination.AudioSectionNavKey

/**
 * Transparent gate for the Audio section: navigates to [AudioNavKey] (Compose, cloud-drive).
 */
fun EntryProviderScope<NavKey>.audioSectionDestination(
    removeDestination: () -> Unit,
    navigationHandler: NavigationHandler,
) {
    entry<AudioSectionNavKey>(
        metadata = transparentMetadata(),
    ) {
        AudioSectionEntry(
            removeDestination = removeDestination,
            navigationHandler = navigationHandler,
        )
    }
}

@Composable
private fun AudioSectionEntry(
    removeDestination: () -> Unit,
    navigationHandler: NavigationHandler,
) {
    LaunchedEffect(Unit) {
        removeDestination()
        navigationHandler.navigate(AudioNavKey)
    }
}
