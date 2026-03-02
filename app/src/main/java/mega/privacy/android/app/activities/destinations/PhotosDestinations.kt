package mega.privacy.android.app.activities.destinations

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.AlbumContentImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.DefaultImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.fetcher.TimelineImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.photos.albums.add.AddToAlbumActivity
import mega.privacy.android.app.presentation.photos.albums.model.AlbumType
import mega.privacy.android.app.presentation.settings.camerauploads.SettingsCameraUploadsActivity
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_SHOW_HOW_TO_UPLOAD_PROMPT
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.AlbumContentPreviewNavKey
import mega.privacy.android.navigation.destination.LegacyAddToAlbumActivityNavKey
import mega.privacy.android.navigation.destination.LegacyImagePreviewNavKey
import mega.privacy.android.navigation.destination.LegacySettingsCameraUploadsActivityNavKey
import mega.privacy.android.navigation.destination.MediaTimelinePhotoPreviewNavKey

fun EntryProviderScope<NavKey>.legacyAlbumContentPreview(
    removeDestination: () -> Unit,
) {
    entry<AlbumContentPreviewNavKey>(
        metadata = transparentMetadata()
    ) { args ->
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

fun EntryProviderScope<NavKey>.legacyAddToAlbumActivityNavKey(returnResult: (String, String?) -> Unit) {
    entry<LegacyAddToAlbumActivityNavKey>(
        metadata = transparentMetadata()
    ) { args ->
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val message = if (result.resultCode != Activity.RESULT_OK) null else {
                result.data?.getStringExtra("message")
            }
            returnResult(LegacyAddToAlbumActivityNavKey.ADD_TO_ALBUM_RESULT, message)
        }

        LaunchedOnceEffect {
            val intent = Intent(context, AddToAlbumActivity::class.java).apply {
                val ids = args.photoIds.toTypedArray()
                putExtra("ids", ids)
                putExtra("type", args.viewType)
            }
            launcher.launch(intent)
        }
    }
}

fun EntryProviderScope<NavKey>.legacyMediaTimelinePhotoPreview(removeDestination: () -> Unit) {
    entry<MediaTimelinePhotoPreviewNavKey>(
        metadata = transparentMetadata()
    ) { args ->
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            val intent = ImagePreviewActivity.createIntent(
                context = context,
                imageSource = ImagePreviewFetcherSource.TIMELINE,
                menuOptionsSource = ImagePreviewMenuSource.TIMELINE,
                anchorImageNodeId = NodeId(args.id),
                params = mapOf(
                    TimelineImageNodeFetcher.TIMELINE_SORT_TYPE to args.sortType,
                    TimelineImageNodeFetcher.TIMELINE_FILTER_TYPE to args.filterType,
                    TimelineImageNodeFetcher.TIMELINE_MEDIA_SOURCE to args.mediaSource,
                ),
                enableAddToAlbum = true,
            )
            context.startActivity(intent)

            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

fun EntryProviderScope<NavKey>.legacySettingsCameraUploadsActivityNavKey(removeDestination: () -> Unit) {
    entry<LegacySettingsCameraUploadsActivityNavKey>(
        metadata = transparentMetadata()
    ) { args ->
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            val intent = Intent(context, SettingsCameraUploadsActivity::class.java).apply {
                putExtra(
                    INTENT_EXTRA_KEY_SHOW_HOW_TO_UPLOAD_PROMPT,
                    args.isShowHowToUploadPrompt
                )
            }
            context.startActivity(intent)
            // Immediately pop this destination from the back stack
            removeDestination()
        }
    }
}

fun EntryProviderScope<NavKey>.legacyImagePreview(
    removeDestination: () -> Unit,
) {
    entry<LegacyImagePreviewNavKey>(
        metadata = transparentMetadata()
    ) { args ->
        val context = LocalContext.current

        LaunchedEffect(Unit) {
            val intent = ImagePreviewActivity.createIntent(
                context = context,
                imageSource = ImagePreviewFetcherSource.DEFAULT,
                menuOptionsSource = ImagePreviewMenuSource.DEFAULT,
                anchorImageNodeId = NodeId(args.anchorImageId),
                params = mapOf(DefaultImageNodeFetcher.NODE_IDS to args.imageIds.toLongArray()),
            )

            context.startActivity(intent)
            removeDestination()
        }
    }
}
