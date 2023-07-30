package mega.privacy.android.app.presentation.photos.mediadiscovery.view

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.PhotoDownloaderViewModel
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryGlobalStateViewModel
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryViewModel
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.model.PhotoDownload
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.model.UIPhoto
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.timeline.view.PhotosSkeletonView
import mega.privacy.android.app.presentation.photos.view.CardListView
import mega.privacy.android.app.presentation.photos.view.EmptyView
import mega.privacy.android.app.presentation.photos.view.FilterDialog
import mega.privacy.android.app.presentation.photos.view.PhotosGridView
import mega.privacy.android.app.presentation.photos.view.PhotosZoomGestureDetector
import mega.privacy.android.app.presentation.photos.view.SortByDialog
import mega.privacy.android.app.presentation.photos.view.TimeSwitchBar
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.grey_alpha_050_white_alpha_050
import mega.privacy.android.core.ui.theme.extensions.teal_300_teal_200
import mega.privacy.android.domain.entity.photos.Photo


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun MediaDiscoveryScreen(
    screenTitle: String? = null,
    viewModel: MediaDiscoveryViewModel = viewModel(),
    mediaDiscoveryGlobalStateViewModel: MediaDiscoveryGlobalStateViewModel = viewModel(),
    photoDownloaderViewModel: PhotoDownloaderViewModel = viewModel(),
    onBackClicked: () -> Unit,
    onPhotoClicked: (Photo) -> Unit,
    onPhotoLongPressed: (Photo) -> Unit,
    onImportClicked: () -> Unit,
    onSaveToDeviceClicked: () -> Unit,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val lazyGridState = rememberLazyGridState()

    var showSortByDialog by rememberSaveable { mutableStateOf(false) }
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }

    HandleSortByDialog(
        showDialog = showSortByDialog,
        currentSort = uiState.currentSort,
        onOptionSelected = {
            viewModel.setCurrentSort(it)
        },
        onDialogDismissed = {
            showSortByDialog = false
        },
    )

    HandleFilterDialog(
        showDialog = showFilterDialog,
        selectedOption = uiState.currentMediaType,
        onOptionSelected = {
            mediaDiscoveryGlobalStateViewModel.storeCurrentMediaType(it)
        },
        onDialogDismissed = {
            showFilterDialog = false
        }
    )

    Scaffold(
        topBar = {
            MDHeader(
                screenTitle = screenTitle,
                currentZoomLevel = uiState.currentZoomLevel,
                selectedTimeBarTab = uiState.selectedTimeBarTab,
                uiPhotos = uiState.uiPhotoList,
                numSelectedPhotos = uiState.selectedPhotoIds.size,
                showMoreMenu = showMoreMenu,
                showImportMenu = uiState.hasDbCredentials,
                onBackClicked = {
                    if (uiState.selectedPhotoIds.isEmpty()) {
                        onBackClicked()
                    } else {
                        viewModel.clearSelectedPhotos()
                    }
                },
                onImportClicked = {
                    onImportClicked()
                    showMoreMenu = false
                },
                onSaveToDeviceClicked = {
                    onSaveToDeviceClicked()
                    showMoreMenu = false
                },
                onSortByClicked = {
                    showMoreMenu = false
                    showSortByDialog = true
                },
                onFilterClicked = {
                    showMoreMenu = false
                    showFilterDialog = true
                },
                onZoomInClicked = {
                    mediaDiscoveryGlobalStateViewModel.zoomIn()
                },
                onZoomOutClicked = {
                    mediaDiscoveryGlobalStateViewModel.zoomOut()
                },
                onMoreClicked = {
                    showMoreMenu = true
                },
                onMoreDismissed = {
                    showMoreMenu = false
                },
                onSelectAllClicked = {
                    showMoreMenu = false
                    viewModel.selectAllPhotos()
                },
                onClearSelectionClicked = {
                    showMoreMenu = false
                    viewModel.clearSelectedPhotos()
                },
            )
        },
        content = {
            if (uiState.loadPhotosDone) {
                if (uiState.uiPhotoList.isNotEmpty()) {
                    MediaDiscoveryContent(
                        lazyGridState = lazyGridState,
                        uiPhotos = uiState.uiPhotoList,
                        yearsCardList = uiState.yearsCardList,
                        monthsCardList = uiState.monthsCardList,
                        daysCardList = uiState.daysCardList,
                        currentZoomLevel = uiState.currentZoomLevel,
                        selectedPhotoIds = uiState.selectedPhotoIds,
                        onPhotoDownload = photoDownloaderViewModel::downloadPublicNodePhoto,
                        onPhotoClicked = onPhotoClicked,
                        onPhotoLongPressed = onPhotoLongPressed,
                        onCardClicked = { dateCard ->
                            viewModel.onCardClick(dateCard)
                        },
                        selectedTimeBarTab = uiState.selectedTimeBarTab,
                        onTimeBarTabSelected = { timeBarTab ->
                            viewModel.onTimeBarTabSelected(timeBarTab = timeBarTab)
                        },
                        onZoomIn = mediaDiscoveryGlobalStateViewModel::zoomIn,
                        onZoomOut = mediaDiscoveryGlobalStateViewModel::zoomOut,
                    )
                } else {
                    EmptyView(uiState.currentMediaType)
                }
            } else {
                PhotosSkeletonView()
            }
        },
    )
}

@Composable
private fun MDHeader(
    screenTitle: String? = null,
    currentZoomLevel: ZoomLevel = ZoomLevel.Grid_3,
    selectedTimeBarTab: TimeBarTab,
    uiPhotos: List<UIPhoto>,
    numSelectedPhotos: Int,
    showMoreMenu: Boolean,
    showImportMenu: Boolean,
    onBackClicked: () -> Unit,
    onImportClicked: () -> Unit,
    onSaveToDeviceClicked: () -> Unit,
    onSortByClicked: () -> Unit,
    onFilterClicked: () -> Unit,
    onZoomInClicked: () -> Unit,
    onZoomOutClicked: () -> Unit,
    onMoreClicked: () -> Unit,
    onMoreDismissed: () -> Unit,
    onSelectAllClicked: () -> Unit,
    onClearSelectionClicked: () -> Unit,
) {

    TopAppBar(
        title = {
            Column {
                if (numSelectedPhotos > 0) {
                    Text(
                        text = "$numSelectedPhotos",
                        color = tealIconTint(),
                        fontWeight = FontWeight.W500,
                        style = MaterialTheme.typography.subtitle1,
                    )
                } else {
                    Text(
                        text = screenTitle ?: "",
                        fontWeight = FontWeight.W500,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.subtitle1,
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    painter = if (numSelectedPhotos > 0)
                        painterResource(id = R.drawable.ic_arrow_back_white)
                    else painterResource(id = R.drawable.ic_close_white),
                    contentDescription = null,
                    tint = tealIconTint().takeIf {
                        numSelectedPhotos > 0
                    } ?: blackWhiteIconTint(),
                )
            }
        },
        actions = {
            if (selectedTimeBarTab == TimeBarTab.All) {
                if (numSelectedPhotos == 0) {
                    if (uiPhotos.isNotEmpty()) {
                        IconButton(onClick = onZoomOutClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_zoom_out),
                                contentDescription = null,
                                tint = blackWhiteIconTint().takeIf {
                                    isZoomOutValid(
                                        currentZoomLevel
                                    )
                                } ?: blackWhiteIconTint().copy(
                                    alpha = 0.5f
                                )
                            )
                        }

                        IconButton(onClick = onZoomInClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_zoom_in),
                                contentDescription = null,
                                tint = blackWhiteIconTint().takeIf {
                                    isZoomInValid(
                                        currentZoomLevel
                                    )
                                } ?: blackWhiteIconTint().copy(
                                    alpha = 0.5f
                                )
                            )
                        }
                    }
                } else {
                    IconButton(onClick = onSaveToDeviceClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download_white),
                            contentDescription = null,
                            tint = tealIconTint(),
                        )
                    }

                    if (showImportMenu) {
                        IconButton(onClick = onImportClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_import_to_cloud_white),
                                contentDescription = null,
                                tint = tealIconTint(),
                            )
                        }
                    }
                }

                IconButton(onClick = onMoreClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_dots_vertical_white),
                        contentDescription = null,
                        tint = tealIconTint().takeIf {
                            numSelectedPhotos > 0
                        } ?: blackWhiteIconTint(),
                    )
                }
            }

            DropdownMenu(expanded = showMoreMenu, onDismissRequest = onMoreDismissed) {

                if (numSelectedPhotos > 0) {
                    DropdownMenuItem(onClick = onSelectAllClicked) {
                        Text(text = stringResource(id = R.string.action_select_all))
                    }
                    DropdownMenuItem(onClick = onClearSelectionClicked) {
                        Text(text = stringResource(id = R.string.action_unselect_all))
                    }
                } else {
                    if (showImportMenu) {
                        DropdownMenuItem(onClick = onImportClicked) {
                            Text(text = stringResource(id = R.string.general_import))
                        }
                    }
                    DropdownMenuItem(
                        onClick = onSaveToDeviceClicked,
                        enabled = uiPhotos.isNotEmpty()
                    ) {
                        Text(
                            text = stringResource(id = R.string.general_save_to_device),
                            modifier = if (uiPhotos.isEmpty())
                                Modifier.alpha(0.5f)
                            else Modifier.alpha(1.0f),
                        )
                    }
                    DropdownMenuItem(
                        onClick = onSortByClicked,
                        enabled = uiPhotos.isNotEmpty()
                    ) {
                        Text(
                            text = stringResource(id = R.string.action_sort_by),
                            modifier = if (uiPhotos.isEmpty())
                                Modifier.alpha(0.5f)
                            else Modifier.alpha(1.0f),
                        )
                    }
                    DropdownMenuItem(onClick = onFilterClicked) {
                        Text(text = stringResource(id = R.string.photos_action_filter))
                    }
                }
            }
        },
        elevation = 0.dp,
    )
}

@Composable
private fun tealIconTint() = MaterialTheme.colors.teal_300_teal_200

@Composable
private fun greyColor() = MaterialTheme.colors.grey_alpha_050_white_alpha_050

@Composable
private fun blackWhiteIconTint() = MaterialTheme.colors.black_white

@Composable
private fun isZoomInValid(currentZoomLevel: ZoomLevel) =
    currentZoomLevel != ZoomLevel.values()
        .first()

@Composable
private fun isZoomOutValid(currentZoomLevel: ZoomLevel) =
    currentZoomLevel != ZoomLevel.values()
        .last()

@Composable
private fun MediaDiscoveryContent(
    currentZoomLevel: ZoomLevel = ZoomLevel.Grid_3,
    lazyGridState: LazyGridState,
    uiPhotos: List<UIPhoto>,
    selectedTimeBarTab: TimeBarTab = TimeBarTab.All,
    yearsCardList: List<DateCard> = emptyList(),
    monthsCardList: List<DateCard> = emptyList(),
    daysCardList: List<DateCard> = emptyList(),
    selectedPhotoIds: Set<Long>,
    onPhotoDownload: PhotoDownload,
    onPhotoClicked: (Photo) -> Unit,
    onPhotoLongPressed: (Photo) -> Unit,
    onCardClicked: (DateCard) -> Unit,
    onTimeBarTabSelected: (TimeBarTab) -> Unit = {},
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (selectedTimeBarTab == TimeBarTab.All) {
                PhotosGridView(
                    modifier = Modifier
                        .PhotosZoomGestureDetector(
                            onZoomIn = onZoomIn,
                            onZoomOut = onZoomOut,
                        ),
                    currentZoomLevel = currentZoomLevel,
                    photoDownland = onPhotoDownload,
                    lazyGridState = lazyGridState,
                    onClick = onPhotoClicked,
                    onLongPress = onPhotoLongPressed,
                    selectedPhotoIds = selectedPhotoIds,
                    uiPhotoList = uiPhotos,
                )
            } else {
                val dateCards = when (selectedTimeBarTab) {
                    TimeBarTab.Years -> yearsCardList
                    TimeBarTab.Months -> monthsCardList
                    TimeBarTab.Days -> daysCardList
                    else -> daysCardList
                }
                CardListView(
                    dateCards = dateCards,
                    photoDownload = onPhotoDownload,
                    onCardClick = onCardClicked,
                    state = lazyGridState,
                )
            }
        }
        if (selectedPhotoIds.isEmpty()) {
            TimeSwitchBar(
                selectedTimeBarTab = selectedTimeBarTab,
                onTimeBarTabSelected = onTimeBarTabSelected
            )
        }
    }
}

@Composable
private fun HandleFilterDialog(
    showDialog: Boolean,
    selectedOption: FilterMediaType,
    onOptionSelected: (FilterMediaType) -> Unit,
    onDialogDismissed: () -> Unit,
) {
    if (showDialog) {
        FilterDialog(
            onDialogDismissed = onDialogDismissed,
            selectedOption = selectedOption,
            onOptionSelected = onOptionSelected
        )
    }
}

@Composable
private fun HandleSortByDialog(
    showDialog: Boolean,
    currentSort: Sort,
    onOptionSelected: (Sort) -> Unit,
    onDialogDismissed: () -> Unit,
) {
    if (showDialog) {
        SortByDialog(
            onDialogDismissed = onDialogDismissed,
            selectedOption = currentSort,
            onOptionSelected = onOptionSelected
        )
    }
}
