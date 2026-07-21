package mega.privacy.android.feature.photos.presentation.albums.create

import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import mega.privacy.android.navigation.destination.CreateAlbumDialogNavKey
import mega.privacy.android.navigation.destination.CreateAlbumDialogResult

fun EntryProviderScope<NavKey>.createAlbumDialog(
    onDismiss: () -> Unit,
    returnResult: (String, CreateAlbumDialogResult?) -> Unit,
) {
    entry<CreateAlbumDialogNavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                windowTitle = "Create Album Dialog"
            )
        )
    ) {
        CreateAlbumDialogM3(
            onDismiss = onDismiss,
            returnResult = returnResult,
        )
    }
}
