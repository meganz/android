@file:OptIn(ExperimentalMaterial3Api::class)

package mega.privacy.android.feature.photos.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.tabs.MegaScrollableTabRow
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.TabItems
import mega.android.core.ui.model.menu.MenuActionIconWithClick
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.photos.model.MediaAppBarAction
import mega.privacy.android.feature.photos.model.MediaAppBarAction.CameraUpload.CameraUploadStatus
import mega.privacy.android.feature.photos.model.MediaScreen
import mega.privacy.android.feature.photos.presentation.albums.AlbumsTabRoute
import mega.privacy.android.shared.resources.R as sharedResR

@Composable
fun MediaMainRoute() {
    MediaMainScreen()
}

@Composable
fun MediaMainScreen(
    modifier: Modifier = Modifier,
) {
    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MegaTopAppBar(
                navigationType = AppBarNavigationType.None,
                title = stringResource(sharedResR.string.media_feature_title),
                actions = buildList {
                    add(
                        MenuActionIconWithClick(
                            menuAction = MediaAppBarAction.CameraUpload(CameraUploadStatus.Default)
                        ) {
                            // Todo: Handle Camera Upload action click
                        }
                    )

                    add(
                        MenuActionIconWithClick(menuAction = MediaAppBarAction.Search) {
                            // Todo: Handle Search action click
                        }
                    )

                    add(
                        MenuActionIconWithClick(menuAction = MediaAppBarAction.More) {
                            // Todo: Handle More action click
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        MegaScrollableTabRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            beyondViewportPageCount = 1,
            hideTabs = false,
            pagerScrollEnabled = true,
        ) {
            MediaScreen.entries.forEach { tab ->
                with(tab) {
                    addTextTabWithScrollableContent(
                        tabItem = getTabItem(),
                        content = { _, modifier ->
                            MediaContent(modifier = Modifier.fillMaxSize())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaScreen.getTabItem() = when (this) {
    MediaScreen.Timeline -> TabItems(title = stringResource(sharedResR.string.media_timeline_tab_title))
    MediaScreen.Albums -> TabItems(title = stringResource(sharedResR.string.media_albums_tab_title))
    MediaScreen.Videos -> TabItems(title = stringResource(sharedResR.string.media_videos_tab_title))
    MediaScreen.Playlists -> TabItems(title = stringResource(sharedResR.string.media_playlists_tab_title))
}

@Composable
private fun MediaScreen.MediaContent(
    modifier: Modifier = Modifier,
) = when (this) {
    MediaScreen.Timeline -> {
        // Todo: Implement Timeline Screen
        Box(modifier) {
            MegaText("Timeline Screen - To be implemented")
        }
    }

    MediaScreen.Albums -> {
        AlbumsTabRoute(modifier = Modifier.fillMaxSize())
    }

    // Todo: Implement Videos and Playlists Screens
    else -> {
        Box(modifier) {
            MegaText("Video / Playlists Screen - To be implemented")
        }
    }
}

@CombinedThemePreviews
@Composable
fun PhotosMainScreenPreview() {
    AndroidThemeForPreviews {
        MediaMainScreen()
    }
}