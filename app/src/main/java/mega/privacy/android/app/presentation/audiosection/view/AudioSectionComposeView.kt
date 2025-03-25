package mega.privacy.android.app.presentation.audiosection.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.audiosection.AudioSectionViewModel
import mega.privacy.android.app.presentation.audiosection.model.AudioUiEntity
import mega.privacy.android.app.presentation.search.view.LoadingStateView
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView
import mega.privacy.android.icon.pack.R as iconPackR

/**
 * The compose view for audio section
 */
@Composable
fun AudioSectionComposeView(
    viewModel: AudioSectionViewModel,
    modifier: Modifier = Modifier,
    onChangeViewTypeClick: () -> Unit = {},
    onSortOrderClick: () -> Unit = {},
    onMenuClick: (AudioUiEntity) -> Unit = {},
    onLongClick: (item: AudioUiEntity, index: Int) -> Unit = { _, _ -> },
) {

    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val progressBarShowing = uiState.progressBarShowing
    val items = uiState.allAudios
    val scrollToTop = uiState.scrollToTop

    LaunchedEffect(items) {
        if (scrollToTop) {
            if (uiState.currentViewType == ViewType.LIST)
                listState.scrollToItem(0)
            else
                gridState.scrollToItem(0)
        }
    }
    Box(modifier = modifier) {
        when {
            progressBarShowing -> LoadingStateView(uiState.currentViewType == ViewType.LIST)

            items.isEmpty() -> LegacyMegaEmptyView(
                modifier = Modifier,
                text = stringResource(id = R.string.homepage_empty_hint_audio),
                imagePainter = painterResource(id = iconPackR.drawable.ic_audio_glass)
            )

            else -> {
                AudiosView(
                    items = items,
                    shouldApplySensitiveMode = uiState.hiddenNodeEnabled
                            && uiState.accountType?.isPaid == true
                            && !uiState.isBusinessAccountExpired,
                    isListView = uiState.currentViewType == ViewType.LIST,
                    listState = listState,
                    gridState = gridState,
                    sortOrder = stringResource(
                        id = SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                            ?: R.string.sortby_name
                    ),
                    modifier = Modifier,
                    onChangeViewTypeClick = onChangeViewTypeClick,
                    onSortOrderClick = onSortOrderClick,
                    onClick = viewModel::onItemClicked,
                    onLongClick = onLongClick,
                    onMenuClick = onMenuClick,
                    inSelectionMode = uiState.isInSelection
                )
            }
        }
    }
}