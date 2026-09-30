package mega.privacy.android.app.presentation.offline.offlinefileinfocompose

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.presentation.offline.offlinefileinfocompose.view.OfflineFileInfoScreen
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.navigation.destination.OfflineInfoNavKey
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import timber.log.Timber

/**
 * Offline file info destination. Renders [OfflineFileInfoScreen] for the offline node in
 * [OfflineInfoNavKey] and pops itself once the node is removed or cannot be loaded.
 */
internal fun EntryProviderScope<NavKey>.offlineFileInfoScreen(
    onBack: () -> Unit,
) {
    entry<OfflineInfoNavKey> { key ->
        val nodeHandle = key.handle.toLongOrNull()
        if (nodeHandle == null) {
            LaunchedEffect(Unit) {
                Timber.e("Offline info opened with an invalid handle: ${key.handle}")
                onBack()
            }
            return@entry
        }
        val viewModel =
            hiltViewModel<OfflineFileInfoComposeViewModel, OfflineFileInfoComposeViewModel.Factory> { factory ->
                factory.create(nodeHandle)
            }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        EventEffect(
            event = uiState.errorEvent,
            onConsumed = viewModel::onErrorEventConsumed,
        ) { onBack() }

        // The screen is built from original-core-ui components, which resolve their colors from
        // the legacy MaterialTheme palette that only OriginalTheme provides.
        OriginalTheme(isDark = uiState.themeMode.isDarkMode()) {
            OfflineFileInfoScreen(
                uiState = uiState,
                onBackPressed = onBack,
                onRemoveFromOffline = {
                    viewModel.removeFromOffline()
                    onBack()
                },
            )
        }
    }
}
