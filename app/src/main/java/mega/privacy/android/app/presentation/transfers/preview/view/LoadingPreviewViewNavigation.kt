package mega.privacy.android.app.presentation.transfers.preview.view

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.transfers.preview.model.LoadingPreviewViewModel
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Back stack root of the loading preview screen. The screen is opened with an intent, so the key
 * takes no arguments: the hosting activity reads them from the intent and builds the ViewModel.
 */
@Serializable
internal data object LoadingPreviewNavKey : NavKey

/**
 * Registers the loading preview screen.
 *
 * @param viewModel owned by the hosting activity, so a new intent can reach the same instance.
 * @param themeMode resolves the legacy theme wrapping the screen.
 * @param onBackPress closes the screen.
 */
internal fun EntryProviderScope<NavKey>.loadingPreviewEntry(
    viewModel: LoadingPreviewViewModel,
    themeMode: ThemeMode,
    onBackPress: () -> Unit,
) {
    entry<LoadingPreviewNavKey> {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // LoadingPreviewView is still Material 2, so it needs the legacy theme on top of the container.
        OriginalTheme(isDark = themeMode.isDarkMode()) {
            LoadingPreviewView(
                onBackPress = onBackPress,
                uiState = uiState,
                consumeTransferEvent = viewModel::consumeTransferEvent,
            )
        }
    }
}
