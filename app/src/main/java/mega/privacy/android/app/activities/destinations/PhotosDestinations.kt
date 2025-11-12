package mega.privacy.android.app.activities.destinations

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.photos.SelectAlbumCoverContract
import mega.privacy.android.navigation.destination.LegacyAlbumCoverSelectionNavKey

fun EntryProviderScope<NavKey>.legacyAlbumCoverSelection(
    returnResult: (String, String?) -> Unit,
) {
    entry<LegacyAlbumCoverSelectionNavKey> { contract ->
        val launcher = rememberLauncherForActivityResult(
            SelectAlbumCoverContract()
        ) { result ->
            returnResult(LegacyAlbumCoverSelectionNavKey.MESSAGE, result)
        }

        LaunchedEffect(Unit) {
            launcher.launch(contract)
        }
    }
}