package mega.privacy.android.app.presentation.audiosection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.audiosection.model.AudioSectionState
import mega.privacy.android.app.presentation.audiosection.model.UIAudio
import mega.privacy.android.core.ui.controls.progressindicator.MegaCircularProgressIndicator
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.legacy.core.ui.controls.LegacyMegaEmptyView

/**
 * The compose view for audio section
 */
@Composable
fun AudioSectionComposeView(
    uiState: AudioSectionState,
    onChangeViewTypeClick: () -> Unit = {},
    onClick: (item: UIAudio, index: Int) -> Unit = { _, _ -> },
    onSortOrderClick: () -> Unit = {},
    onMenuClick: (UIAudio) -> Unit = {},
    onLongClick: (item: UIAudio, index: Int) -> Unit = { _, _ -> },
) {
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

    when {
        progressBarShowing -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp),
                contentAlignment = Alignment.TopCenter,
                content = {
                    MegaCircularProgressIndicator(
                        modifier = Modifier
                            .size(50.dp),
                        strokeWidth = 4.dp,
                    )
                },
            )
        }

        items.isEmpty() -> LegacyMegaEmptyView(
            modifier = Modifier,
            text = stringResource(id = R.string.homepage_empty_hint_audio),
            imagePainter = painterResource(id = R.drawable.ic_homepage_empty_audio)
        )

        else -> {
            AudiosView(
                items = items,
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
                onClick = onClick,
                onLongClick = onLongClick,
                onMenuClick = onMenuClick
            )
        }
    }
}