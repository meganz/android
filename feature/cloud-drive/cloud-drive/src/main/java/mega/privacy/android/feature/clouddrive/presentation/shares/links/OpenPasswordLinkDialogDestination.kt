package mega.privacy.android.feature.clouddrive.presentation.shares.links

import android.net.Uri
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.serialization.Serializable
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey


/**
 * Open folder or file link with password dialog
 * @property uriString
 */
@Serializable
data class OpenPasswordLinkDialogNavKey(val uriString: String) : NoSessionNavKey.Optional

fun EntryProviderScope<NavKey>.openPasswordLinkDialog(
    onBack: () -> Unit,
    onNavigateToFileLink: (String) -> Unit,
    onNavigateToFolderLink: (String) -> Unit,
) {
    entry<OpenPasswordLinkDialogNavKey>(
        metadata = DialogSceneStrategy.dialog(
            DialogProperties(
                windowTitle = "Enter password"
            )
        )
    ) { key ->
        OpenPasswordLinkDialog(
            passwordProtectedLink = key.uriString,
            onDismiss = onBack,
            onNavigateToFileLink = onNavigateToFileLink,
            onNavigateToFolderLink = onNavigateToFolderLink,
        )
    }
}