package mega.privacy.android.app.activities.destinations

import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.photos.SelectAlbumCoverContract
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.navigation.destination.AlbumGetLinkNavKey
import mega.privacy.android.navigation.destination.LegacyAlbumCoverSelectionNavKey
import mega.privacy.android.navigation.destination.LegacyAlbumImportNavKey

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

fun EntryProviderScope<NavKey>.legacyAlbumGetLink(
    removeDestination: () -> Unit,
) {
    entry<AlbumGetLinkNavKey> { args ->
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            val intent = AlbumScreenWrapperActivity.createAlbumGetLinkScreen(
                context = context,
                albumId = AlbumId(args.albumId),
                hasSensitiveElement = args.hasSensitiveContent,
            )

            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

fun EntryProviderScope<NavKey>.legacyAlbumImport(
    removeDestination: () -> Unit,
) {
    entry<LegacyAlbumImportNavKey> { key ->
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            val intent = AlbumScreenWrapperActivity.createAlbumImportScreen(
                context = context,
                albumLink = AlbumLink(key.link.orEmpty()),
            ).apply {
                flags = FLAG_ACTIVITY_CLEAR_TOP
            }

            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}