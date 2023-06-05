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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import mega.privacy.android.app.presentation.photos.view.CardListView
import mega.privacy.android.app.presentation.photos.view.FilterDialog
import mega.privacy.android.app.presentation.photos.view.PhotosGridView
import mega.privacy.android.app.presentation.photos.view.SortByDialog
import mega.privacy.android.app.presentation.photos.view.TimeSwitchBar
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.white
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
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val isLight = MaterialTheme.colors.isLight
    val lazyGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

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
                selectedTimeBarTab = uiState.selectedTimeBarTab,
                numSelectedPhotos = uiState.selectedPhotoIds.size,
                showMoreMenu = showMoreMenu,
                showSelectAllMenu = false,
                onBackClicked = {
                    if (uiState.selectedPhotoIds.isEmpty()) {
                        onBackClicked()
                    } else {
                        viewModel.clearSelectedPhotos()
                    }
                },
                onImportClicked = {
                    showMoreMenu = false
                },
                onSaveToDeviceClicked = {
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
            MediaDiscoveryContent(
                lazyGridState = lazyGridState,
                uiPhotos = uiState.uiPhotoList,
                yearsCardList = uiState.yearsCardList,
                monthsCardList = uiState.monthsCardList,
                daysCardList = uiState.daysCardList,
                currentZoomLevel = uiState.currentZoomLevel,
                selectedPhotoIds = uiState.selectedPhotoIds,
                onPhotoDownload = photoDownloaderViewModel::downloadPhoto,
                onPhotoClicked = onPhotoClicked,
                onPhotoLongPressed = onPhotoLongPressed,
                onCardClicked = { dateCard ->
                    viewModel.onCardClick(dateCard)
                },
                selectedTimeBarTab = uiState.selectedTimeBarTab,
                onTimeBarTabSelected = { timeBarTab ->
                    viewModel.onTimeBarTabSelected(timeBarTab = timeBarTab)
                }
            )
        },
    )
}

@Composable
private fun MDHeader(
    screenTitle: String? = null,
    selectedTimeBarTab: TimeBarTab,
    numSelectedPhotos: Int,
    showMoreMenu: Boolean,
    showSelectAllMenu: Boolean,
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
    val isLight = MaterialTheme.colors.isLight

    TopAppBar(
        title = {
            Column {
                if (numSelectedPhotos > 0) {
                    Text(
                        text = "$numSelectedPhotos",
                        color = teal_300,
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
                    painter = painterResource(id = R.drawable.ic_arrow_back_white),
                    contentDescription = null,
                    tint = teal_300.takeIf {
                        numSelectedPhotos > 0
                    } ?: (black.takeIf { isLight } ?: white),
                )
            }
        },
        actions = {
            if (selectedTimeBarTab == TimeBarTab.All) {
                if (numSelectedPhotos == 0) {
                    IconButton(onClick = onZoomOutClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_zoom_out),
                            contentDescription = null,
                            tint = black.takeIf { isLight } ?: white,
                        )
                    }

                    IconButton(onClick = onZoomInClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_zoom_in),
                            contentDescription = null,
                            tint = black.takeIf { isLight } ?: white,
                        )
                    }
                } else {
                    IconButton(onClick = onSaveToDeviceClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download_white),
                            contentDescription = null,
                            tint = teal_300,
                        )
                    }

                    IconButton(onClick = onImportClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_import_to_cloud_white),
                            contentDescription = null,
                            tint = teal_300,
                        )
                    }
                }

                IconButton(onClick = onMoreClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_dots_vertical_white),
                        contentDescription = null,
                        tint = teal_300.takeIf {
                            numSelectedPhotos > 0
                        } ?: (black.takeIf { isLight } ?: white),
                    )
                }
            }


            DropdownMenu(expanded = showMoreMenu, onDismissRequest = onMoreDismissed) {

                if (numSelectedPhotos > 0) {
                    if (showSelectAllMenu) {
                        DropdownMenuItem(onClick = onSelectAllClicked) {
                            Text(text = stringResource(id = R.string.action_select_all))
                        }
                    }
                    DropdownMenuItem(onClick = onClearSelectionClicked) {
                        Text(text = stringResource(id = R.string.action_unselect_all))
                    }
                } else {
                    DropdownMenuItem(onClick = onImportClicked) {
                        Text(text = stringResource(id = R.string.general_import))
                    }
                    DropdownMenuItem(onClick = onSaveToDeviceClicked) {
                        Text(text = stringResource(id = R.string.general_save_to_device))
                    }
                    DropdownMenuItem(onClick = onSortByClicked) {
                        Text(text = stringResource(id = R.string.action_sort_by))
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
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (selectedTimeBarTab == TimeBarTab.All) {
                PhotosGridView(
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
