package mega.privacy.android.feature.chat.giphy.view

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.SpannedText
import mega.android.core.ui.components.indicators.LargeSpinnerIndicator
import mega.android.core.ui.components.indicators.SmallSpinnerIndicator
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaSearchTopAppBar
import mega.android.core.ui.model.MegaSpanStyle
import mega.android.core.ui.model.SpanIndicator
import mega.android.core.ui.model.SpanStyleWithAnnotation
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.domain.entity.chat.Giphy
import mega.privacy.android.feature.chat.R
import mega.privacy.android.feature.chat.giphy.GiphyPickerViewModel
import mega.privacy.android.feature.chat.giphy.model.GiphyEmptyState
import mega.privacy.android.feature.chat.giphy.model.GiphyPickerUiState
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Screen to pick a GIF from GIPHY, showing trending GIFs or the ones matching a search.
 *
 * @param onGifSelected Called with the GIF the user tapped.
 * @param onBack Called when the user leaves the screen.
 */
@Composable
fun GiphyPickerScreen(
    onGifSelected: (Giphy) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<GiphyPickerViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GiphyPickerView(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onLoadMore = viewModel::onLoadMore,
        onGifSelected = onGifSelected,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun GiphyPickerView(
    uiState: GiphyPickerUiState,
    onQueryChanged: (String) -> Unit,
    onLoadMore: () -> Unit,
    onGifSelected: (Giphy) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialSearchingMode: Boolean = false,
) {
    var isSearching by rememberSaveable { mutableStateOf(initialSearchingMode) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val closeSearch = {
        isSearching = false
        onQueryChanged("")
    }

    BackHandler(enabled = isSearching, onBack = closeSearch)

    MegaScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MegaSearchTopAppBar(
                navigationType = AppBarNavigationType.Back(onBack),
                title = stringResource(sharedR.string.chat_giphy_picker_title),
                query = uiState.query,
                onQueryChanged = onQueryChanged,
                isSearchingMode = isSearching,
                onSearchingModeChanged = { searching ->
                    if (searching) isSearching = true else closeSearch()
                },
                searchPlaceholder = stringResource(sharedR.string.chat_giphy_picker_title),
                onSearchAction = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
            )
        },
        bottomBar = { PoweredByGiphyFooter() },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.emptyState != null -> GiphyEmptyView(
                    emptyState = uiState.emptyState,
                    modifier = Modifier.fillMaxSize(),
                )

                uiState.gifs.isEmpty() && uiState.isLoading -> LargeSpinnerIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag(GIPHY_PICKER_LOADING_TAG),
                )

                else -> GiphyGrid(
                    uiState = uiState,
                    onLoadMore = onLoadMore,
                    onGifSelected = onGifSelected,
                )
            }
        }
    }
}

@Composable
private fun GiphyGrid(
    uiState: GiphyPickerUiState,
    onLoadMore: () -> Unit,
    onGifSelected: (Giphy) -> Unit,
) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val gridState = rememberLazyStaggeredGridState()

    LoadMoreEffect(
        gridState = gridState,
        canLoadMore = !uiState.isLoading && !uiState.isEndOfList,
        onLoadMore = onLoadMore,
    )

    LaunchedEffect(uiState.query) {
        gridState.scrollToItem(0)
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(if (isLandscape) LANDSCAPE_COLUMNS else PORTRAIT_COLUMNS),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .testTag(GIPHY_PICKER_GRID_TAG),
        contentPadding = PaddingValues(GIF_SPACING / 2),
        verticalItemSpacing = GIF_SPACING,
        horizontalArrangement = Arrangement.spacedBy(GIF_SPACING),
    ) {
        itemsIndexed(uiState.gifs) { index, gif ->
            GiphyGridItem(
                gif = gif,
                onClick = { onGifSelected(gif) },
                modifier = Modifier.testTag("$GIPHY_PICKER_ITEM_TAG$index"),
            )
        }

        if (uiState.isLoading || uiState.isEndOfList) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (uiState.isLoading) {
                        SmallSpinnerIndicator()
                    } else {
                        EndOfResultsText()
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadMoreEffect(
    gridState: LazyStaggeredGridState,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
) {
    val isLastItemVisible by remember(gridState) {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            layoutInfo.totalItemsCount > 0 &&
                    lastVisibleIndex >= layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(isLastItemVisible, canLoadMore) {
        if (isLastItemVisible && canLoadMore) onLoadMore()
    }
}

@Composable
private fun GiphyGridItem(
    gif: Giphy,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLoaded by remember(gif) { mutableStateOf(false) }

    AsyncImage(
        model = gif.webpSrc,
        contentDescription = gif.title,
        contentScale = ContentScale.Crop,
        onSuccess = { isLoaded = true },
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(gif.aspectRatio)
            .shimmerEffect(shape = RectangleShape, visible = !isLoaded)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun GiphyEmptyView(
    emptyState: GiphyEmptyState,
    modifier: Modifier = Modifier,
) {
    EmptyStateView(
        modifier = modifier.testTag(GIPHY_PICKER_EMPTY_TAG),
        illustration = R.drawable.ic_no_gifs,
        description = when (emptyState) {
            GiphyEmptyState.NoResults -> stringResource(sharedR.string.chat_giphy_picker_no_results)
            GiphyEmptyState.ServerDown -> stringResource(sharedR.string.chat_giphy_picker_server_down)
        },
        descriptionSpanStyles = mapOf(
            SpanIndicator('A') to SpanStyleWithAnnotation(
                megaSpanStyle = MegaSpanStyle.TextColorStyle(SpanStyle(), TextColor.Primary),
                annotation = null,
            ),
        ),
    )
}

@Composable
private fun EndOfResultsText() {
    SpannedText(
        value = stringResource(sharedR.string.chat_giphy_picker_end_of_results).uppercase(),
        spanStyles = mapOf(
            SpanIndicator('A') to MegaSpanStyle.TextColorStyle(SpanStyle(), TextColor.Primary),
        ),
        baseStyle = AppTheme.typography.labelLarge,
        baseTextColor = TextColor.Secondary,
        modifier = Modifier.testTag(GIPHY_PICKER_END_OF_RESULTS_TAG),
    )
}

@Composable
private fun PoweredByGiphyFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(FOOTER_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_powered_by_giphy),
            contentDescription = null,
        )
    }
}

private val Giphy.aspectRatio: Float
    get() = if (width > 0 && height > 0) width.toFloat() / height else 1f

private const val PORTRAIT_COLUMNS = 2
private const val LANDSCAPE_COLUMNS = 4
private const val LOAD_MORE_THRESHOLD = 4
private val GIF_SPACING = 4.dp
private val FOOTER_HEIGHT = 48.dp

internal const val GIPHY_PICKER_GRID_TAG = "giphy_picker:grid"
internal const val GIPHY_PICKER_ITEM_TAG = "giphy_picker:item_"
internal const val GIPHY_PICKER_LOADING_TAG = "giphy_picker:loading"
internal const val GIPHY_PICKER_EMPTY_TAG = "giphy_picker:empty"
internal const val GIPHY_PICKER_END_OF_RESULTS_TAG = "giphy_picker:end_of_results"

@CombinedThemePreviews
@Composable
private fun GiphyPickerViewPreview() {
    AndroidThemeForPreviews {
        GiphyPickerView(
            uiState = GiphyPickerUiState(
                gifs = List(6) {
                    Giphy(null, null, "GIF $it", 0, 0, 200, if (it % 2 == 0) 100 else 200)
                }.toImmutableList(),
                isLoading = false,
                isEndOfList = true,
            ),
            onQueryChanged = {},
            onLoadMore = {},
            onGifSelected = {},
            onBack = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun GiphyPickerViewEmptyPreview() {
    AndroidThemeForPreviews {
        GiphyPickerView(
            uiState = GiphyPickerUiState(
                query = "nothing",
                isLoading = false,
                emptyState = GiphyEmptyState.NoResults,
            ),
            onQueryChanged = {},
            onLoadMore = {},
            onGifSelected = {},
            onBack = {},
            initialSearchingMode = true,
        )
    }
}
