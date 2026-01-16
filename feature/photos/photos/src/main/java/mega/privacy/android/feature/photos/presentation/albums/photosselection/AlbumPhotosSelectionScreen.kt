package mega.privacy.android.feature.photos.presentation.albums.photosselection

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.palm.composestateevents.EventEffect
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.BasicDialogRadioOption
import mega.android.core.ui.components.dialogs.BasicRadioDialog
import mega.android.core.ui.components.fab.MegaFab
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.feature.photos.R as featurePhotosR
import mega.privacy.android.feature.photos.model.AlbumFlow
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.model.TimelinePhotosSource
import mega.privacy.android.feature.photos.model.TimelinePhotosSource.ALL_PHOTOS
import mega.privacy.android.feature.photos.model.TimelinePhotosSource.CAMERA_UPLOAD
import mega.privacy.android.feature.photos.model.TimelinePhotosSource.CLOUD_DRIVE
import mega.privacy.android.feature.photos.presentation.albums.photosselection.AlbumPhotosSelectionViewModel.Companion.MAX_SELECTION_NUM
import mega.privacy.android.feature.photos.presentation.component.PhotosNodeGridView
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.AddItemsToExistingAlbumFABEvent
import mega.privacy.mobile.analytics.event.AddItemsToNewAlbumFABEvent
import mega.privacy.mobile.analytics.event.AlbumPhotosSelectionAllLocationsButtonEvent
import mega.privacy.mobile.analytics.event.AlbumPhotosSelectionCameraUploadsButtonEvent
import mega.privacy.mobile.analytics.event.AlbumPhotosSelectionCloudDriveButtonEvent
import mega.privacy.mobile.analytics.event.AlbumPhotosSelectionFilterMenuToolbarEvent
import mega.privacy.mobile.analytics.event.AlbumPhotosSelectionScreenEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumPhotosSelectionScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumPhotosSelectionViewModel =
        hiltViewModel<AlbumPhotosSelectionViewModel, AlbumPhotosSelectionViewModel.Factory>(
            creationCallback = { it.create(null, null) }
        ),
    onBackClicked: () -> Unit = {},
    onCompletion: (album: Album.UserAlbum, numCommittedPhotos: Int) -> Unit = { _, _ -> },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lazyGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    var showSelectLocationDialog by rememberSaveable { mutableStateOf(false) }
    var showMaxSelectionDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(AlbumPhotosSelectionScreenEvent)
    }

    LaunchedEffect(state.isInvalidAlbum) {
        if (state.isInvalidAlbum) {
            onBackClicked()
        }
    }

    EventEffect(
        event = state.photosSelectionCompletedEvent,
        onConsumed = viewModel::resetPhotosSelectionCompletedEvent,
        action = { commitedPhotos ->
            state.album?.let {
                onCompletion(it, commitedPhotos)
            }
        }
    )

    if (showSelectLocationDialog) {
        SelectLocationDialog(
            selectedLocation = state.selectedLocation,
            onLocationSelected = { location ->
                showSelectLocationDialog = false
                coroutineScope.launch { lazyGridState.scrollToItem(0) }

                viewModel.updateLocation(location)
            },
            onDialogDismissed = {
                showSelectLocationDialog = false
            },
        )
    }

    if (showMaxSelectionDialog) {
        MaxSelectionDialog { showMaxSelectionDialog = false }
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding(),
        topBar = {
            AlbumPhotosSelectionHeader(
                album = state.album,
                selectedLocation = state.selectedLocation,
                isLocationDetermined = state.isLocationDetermined,
                numSelectedPhotos = state.selectedPhotoIds.size,
                showFilterMenu = state.showFilterMenu,
                showSelectAll = !state.areAllNodesSelected,
                onBackClicked = {
                    if (state.selectedPhotoIds.isEmpty()) {
                        onBackClicked()
                    } else {
                        viewModel.clearSelection()
                    }
                },
                onFilterClicked = {
                    Analytics.tracker.trackEvent(AlbumPhotosSelectionFilterMenuToolbarEvent)
                    showSelectLocationDialog = true
                },
                onSelectAllClicked = viewModel::selectAllPhotos,
                onClearSelectionClicked = viewModel::clearSelection
            )
        },
        floatingActionButton = {
            val album = state.album
            val photos = state.photos
            val albumFlow = state.albumFlow
            val selectedPhotoIds = state.selectedPhotoIds

            if (album != null && photos.isNotEmpty() && (albumFlow == AlbumFlow.Creation || albumFlow == AlbumFlow.Addition && selectedPhotoIds.isNotEmpty())) {
                MegaFab(
                    painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.Check),
                    onClick = {
                        when (albumFlow) {
                            AlbumFlow.Creation -> Analytics.tracker.trackEvent(
                                AddItemsToNewAlbumFABEvent
                            )

                            AlbumFlow.Addition -> Analytics.tracker.trackEvent(
                                AddItemsToExistingAlbumFABEvent
                            )
                        }

                        viewModel.addPhotos(
                            album = album,
                            selectedPhotoIds = state.selectedPhotoIds,
                        )
                    },
                )
            }
        },
        content = { padding ->
            AlbumPhotosSelectionContent(
                state = state,
                selectPhoto = viewModel::selectPhoto,
                unselectPhoto = viewModel::unselectPhoto,
                showMaxSelectionDialog = { showMaxSelectionDialog = true },
                contentPadding = padding,
                lazyGridState = lazyGridState
            )
        },
    )
}

@Composable
private fun AlbumPhotosSelectionHeader(
    album: Album.UserAlbum?,
    selectedLocation: TimelinePhotosSource,
    isLocationDetermined: Boolean,
    numSelectedPhotos: Int,
    showFilterMenu: Boolean,
    showSelectAll: Boolean,
    onBackClicked: () -> Unit,
    onFilterClicked: () -> Unit,
    onSelectAllClicked: () -> Unit,
    onClearSelectionClicked: () -> Unit,
) {
    val context = LocalContext.current

    if (numSelectedPhotos > 0) {
        MegaTopAppBar(
            title = "$numSelectedPhotos",
            navigationType = AppBarNavigationType.Close(onClearSelectionClicked),
            actions = buildList {
                if (showSelectAll) {
                    add(
                        MenuActionWithClick(
                            menuAction = object : MenuActionWithIcon {
                                override val testTag: String
                                    get() = ALBUM_PHOTOS_SELECTION_SELECT_ALL_TEST_TAG

                                @Composable
                                override fun getDescription(): String =
                                    stringResource(sharedR.string.action_select_all)

                                @Composable
                                override fun getIconPainter(): Painter =
                                    rememberVectorPainter(IconPack.Medium.Thin.Outline.CheckStack)
                            },
                            onClick = onSelectAllClicked
                        )
                    )
                }
            }
        )
    } else {
        MegaTopAppBar(
            title = stringResource(
                id = sharedR.string.album_photos_selection_screen_title,
                album?.title.orEmpty()
            ),
            subtitle = selectedLocation.text(context).takeIf { isLocationDetermined },
            navigationType = AppBarNavigationType.Back(onBackClicked),
            actions = buildList {
                if (showFilterMenu) {
                    add(
                        MenuActionWithClick(
                            menuAction = object : MenuActionWithIcon {
                                override val testTag: String =
                                    ALBUM_PHOTOS_SELECTION_FILTER_TEST_TAG

                                @Composable
                                override fun getDescription(): String =
                                    stringResource(sharedR.string.general_action_filter)

                                @Composable
                                override fun getIconPainter(): Painter =
                                    rememberVectorPainter(IconPack.Medium.Thin.Outline.Filter)
                            },
                            onClick = onFilterClicked
                        )
                    )
                }
            }
        )
    }
}

@Composable
private fun AlbumPhotosSelectionContent(
    state: AlbumPhotosSelectionState,
    selectPhoto: (PhotoUiState) -> Unit,
    unselectPhoto: (PhotoUiState) -> Unit,
    showMaxSelectionDialog: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    lazyGridState: LazyGridState = rememberLazyGridState(),
) {
    if (state.photos.isEmpty() && !state.isLoading) {
        EmptyStateView(
            modifier = Modifier.fillMaxSize(),
            illustration = featurePhotosR.drawable.il_glass_image,
            description = SpannableText(
                stringResource(sharedR.string.album_photos_selection_empty_state_no_media_found)
            )
        )
    } else {
        PhotosNodeGridView(
            items = state.photosNodeContentTypes,
            gridSize = TimelineGridSize.Default,
            onGridSizeChange = {},
            onClick = { node ->
                if (node.photo.id in state.selectedPhotoIds) {
                    unselectPhoto(node.photo)
                } else {
                    if (state.selectedPhotoIds.size < MAX_SELECTION_NUM) {
                        selectPhoto(node.photo)
                    } else {
                        showMaxSelectionDialog()
                    }
                }
            },
            onLongClick = { node ->
                if (node.photo.id in state.selectedPhotoIds) {
                    unselectPhoto(node.photo)
                } else {
                    if (state.selectedPhotoIds.size < MAX_SELECTION_NUM) {
                        selectPhoto(node.photo)
                    } else {
                        showMaxSelectionDialog()
                    }
                }
            },
            lazyGridState = lazyGridState,
            contentPadding = contentPadding
        )
    }
}

@Composable
private fun SelectLocationDialog(
    selectedLocation: TimelinePhotosSource,
    onLocationSelected: (TimelinePhotosSource) -> Unit,
    onDialogDismissed: () -> Unit,
) {
    val context = LocalContext.current
    val dialogOptions = remember {
        TimelinePhotosSource
            .entries
            .mapIndexed { index, source ->
                BasicDialogRadioOption(
                    ordinal = index,
                    text = source.text(context)
                )
            }.toImmutableList()
    }
    val selectedOption by remember(selectedLocation) {
        mutableStateOf(dialogOptions.find { it.ordinal == selectedLocation.ordinal })
    }

    BasicRadioDialog(
        title = SpannableText(text = stringResource(sharedR.string.general_action_filter)),
        options = dialogOptions,
        buttons = persistentListOf(
            BasicDialogButton(
                text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                onClick = onDialogDismissed
            )
        ),
        onDismissRequest = onDialogDismissed,
        onOptionSelected = { option ->
            val location = TimelinePhotosSource.entries[option.ordinal]
            locationFilterAnalytics(location)
            if (location == selectedLocation) {
                onDialogDismissed()
            } else {
                onLocationSelected(location)
            }
        },
        selectedOption = selectedOption,
    )
}

private fun TimelinePhotosSource.text(context: Context): String = when (this) {
    ALL_PHOTOS -> context.getString(sharedR.string.timeline_photos_source_all_locations)
    CLOUD_DRIVE -> context.getString(sharedR.string.general_section_cloud_drive)
    CAMERA_UPLOAD -> context.getString(sharedR.string.general_camera_uploads)
}

private fun locationFilterAnalytics(location: TimelinePhotosSource) =
    Analytics.tracker.trackEvent(
        when (location) {
            ALL_PHOTOS -> AlbumPhotosSelectionAllLocationsButtonEvent
            CLOUD_DRIVE -> AlbumPhotosSelectionCloudDriveButtonEvent
            CAMERA_UPLOAD -> AlbumPhotosSelectionCameraUploadsButtonEvent
        }
    )

@Composable
private fun MaxSelectionDialog(
    onDismiss: () -> Unit,
) {
    BasicDialog(
        title = stringResource(id = sharedR.string.album_photos_selection_limit_dialog_title),
        description = stringResource(
            id = sharedR.string.album_photos_selection_limit_dialog_description,
            MAX_SELECTION_NUM
        ),
        onDismiss = onDismiss,
        positiveButtonText = stringResource(id = sharedR.string.general_ok),
        onPositiveButtonClicked = onDismiss,
    )
}

@CombinedThemePreviews
@Composable
private fun MaxSelectionDialogPreview() {
    AndroidThemeForPreviews {
        MaxSelectionDialog { }
    }
}

@CombinedThemePreviews
@Composable
private fun SelectLocationDialogPreview() {
    AndroidThemeForPreviews {
        SelectLocationDialog(
            selectedLocation = ALL_PHOTOS,
            onLocationSelected = {},
            onDialogDismissed = {}
        )
    }
}

@CombinedThemePreviews
@Composable
private fun EmptyStatePreview() {
    AndroidThemeForPreviews {
        AlbumPhotosSelectionContent(
            state = AlbumPhotosSelectionState(photos = emptyList(), isLoading = false),
            selectPhoto = {},
            unselectPhoto = {},
            showMaxSelectionDialog = {}
        )
    }
}

internal const val ALBUM_PHOTOS_SELECTION_SELECT_ALL_TEST_TAG = "album_photos_selection:select_all"
internal const val ALBUM_PHOTOS_SELECTION_FILTER_TEST_TAG = "album_photos_selection:filter"
