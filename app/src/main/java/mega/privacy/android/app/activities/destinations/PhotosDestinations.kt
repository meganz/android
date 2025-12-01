package mega.privacy.android.app.activities.destinations

import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.AlbumContentImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.photos.SelectAlbumCoverContract
import mega.privacy.android.app.presentation.photos.albums.AlbumScreenWrapperActivity
import mega.privacy.android.app.presentation.photos.albums.model.AlbumType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.domain.entity.photos.AlbumLink
import mega.privacy.android.navigation.destination.AlbumContentPreviewNavKey
import mega.privacy.android.navigation.destination.AlbumGetLinkNavKey
import mega.privacy.android.navigation.destination.LegacyAlbumImportNavKey
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

fun EntryProviderScope<NavKey>.legacyAlbumContentPreview(
    removeDestination: () -> Unit,
) {
    entry<AlbumContentPreviewNavKey> { args ->
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            val intent = ImagePreviewActivity.createIntent(
                context = context,
                imageSource = ImagePreviewFetcherSource.ALBUM_CONTENT,
                menuOptionsSource = ImagePreviewMenuSource.ALBUM_CONTENT,
                anchorImageNodeId = NodeId(args.photoId),
                params = buildMap {
                    // Legacy convert string value to AlbumType
                    this[AlbumContentImageNodeFetcher.ALBUM_TYPE] =
                        AlbumType.valueOf(
                            args
                                .albumType
                                .lowercase()
                                .replaceFirstChar { it.uppercase() }
                        )
                    args.albumId?.let { id ->
                        this[AlbumContentImageNodeFetcher.CUSTOM_ALBUM_ID] = id
                    }
                    this[AlbumContentImageNodeFetcher.ALBUM_SORT_TYPE] = args.sortType
                    this[AlbumContentImageNodeFetcher.ALBUM_TITLE] = args.title
                },
            )

            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}