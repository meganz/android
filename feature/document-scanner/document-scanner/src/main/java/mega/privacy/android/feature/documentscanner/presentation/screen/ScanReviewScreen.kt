package mega.privacy.android.feature.documentscanner.presentation.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.image.MegaIcon
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.IconColor
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.documentscanner.components.ScanPageIndicator
import mega.privacy.android.feature.documentscanner.components.ScanPagePreview
import mega.privacy.android.feature.documentscanner.components.ScanReviewThumbnail
import mega.privacy.android.feature.documentscanner.presentation.ScanReviewViewModel
import mega.privacy.android.feature.documentscanner.presentation.model.ReviewPageUiItem
import mega.privacy.android.icon.pack.IconPack
import kotlin.math.abs
import kotlin.math.roundToInt
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Page-review screen: a swipeable preview of the current page, with a thumbnail
 * strip below to jump between pages, drag to reorder, and long-press for actions.
 *
 * @param onBack Navigate back to the camera.
 */
@Composable
internal fun ScanReviewScreen(
    onBack: () -> Unit,
    onRetakePage: (String) -> Unit,
    viewModel: ScanReviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ScanReviewContent(
        pages = uiState.pages,
        onBack = onBack,
        onDeletePage = viewModel::onDeletePage,
        onReorder = viewModel::onReorder,
        onRetakePage = onRetakePage,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanReviewContent(
    pages: List<ReviewPageUiItem>,
    onBack: () -> Unit,
    onDeletePage: (String) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onRetakePage: (String) -> Unit,
) {
    var menuPageId by remember { mutableStateOf<String?>(null) }

    MegaScaffoldWithTopAppBarScrollBehavior(
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.document_scanner_review_title),
                navigationType = AppBarNavigationType.Back(onBack),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (pages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    MegaText(
                        text = stringResource(sharedR.string.document_scanner_review_empty),
                        textColor = TextColor.Secondary,
                        style = AppTheme.typography.bodyLarge,
                    )
                }
            } else {
                // A local, mutable copy of the order so the strip can reflow live during
                // a drag; the committed order (repo) is only updated on drop. Kept in sync
                // with the session order whenever a drag isn't in progress.
                val localPages = remember { pages.toMutableStateList() }
                var isDragging by remember { mutableStateOf(false) }
                LaunchedEffect(pages) {
                    if (!isDragging && localPages.toList() != pages) {
                        localPages.clear()
                        localPages.addAll(pages)
                    }
                }

                val pagerState = rememberPagerState(pageCount = { localPages.size })
                val scope = rememberCoroutineScope()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        pageSpacing = 16.dp,
                    ) { page ->
                        ScanPagePreview(imageUri = localPages[page].imageUri)
                    }
                    ScanPageIndicator(
                        currentPage = pagerState.currentPage.coerceIn(0, localPages.lastIndex) + 1,
                        totalPages = localPages.size,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp),
                    )
                }

                PageThumbnailStrip(
                    pages = localPages,
                    selectedIndex = pagerState.currentPage.coerceIn(0, localPages.lastIndex),
                    onSelect = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                    onDragStateChange = { isDragging = it },
                    onLiveMove = { from, to ->
                        // Keep the preview on the same page while the strip reflows.
                        val previewedId = localPages.getOrNull(pagerState.currentPage)?.id
                        localPages.add(to, localPages.removeAt(from))
                        previewedId?.let { id ->
                            val newIndex = localPages.indexOfFirst { it.id == id }
                            if (newIndex >= 0 && newIndex != pagerState.currentPage) {
                                scope.launch { pagerState.scrollToPage(newIndex) }
                            }
                        }
                    },
                    onCommit = onReorder,
                    onLongPress = { menuPageId = it },
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        }
    }

    menuPageId?.let { pageId ->
        val sheetState = rememberModalBottomSheetState()
        MegaModalBottomSheet(
            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
            sheetState = sheetState,
            onDismissRequest = { menuPageId = null },
        ) {
            SheetAction(
                icon = IconPack.Medium.Thin.Outline.Camera,
                text = stringResource(sharedR.string.document_scanner_review_retake_page),
                onClick = {
                    menuPageId = null
                    onRetakePage(pageId)
                },
            )
            SheetAction(
                icon = IconPack.Medium.Thin.Outline.Trash,
                text = stringResource(sharedR.string.document_scanner_review_delete_page),
                onClick = {
                    onDeletePage(pageId)
                    menuPageId = null
                },
            )
        }
    }
}

@Composable
private fun PageThumbnailStrip(
    pages: List<ReviewPageUiItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    onLiveMove: (Int, Int) -> Unit,
    onCommit: (Int, Int) -> Unit,
    onLongPress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentPages by rememberUpdatedState(pages)
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var startIndex by remember { mutableIntStateOf(-1) }
    var moved by remember { mutableStateOf(false) }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(THUMB_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(pages, key = { _, page -> page.id }) { index, page ->
            val isDragging = draggingId == page.id
            ScanReviewThumbnail(
                thumbnailUri = page.thumbnailUri,
                pageNumber = index + 1,
                isSelected = index == selectedIndex,
                modifier = Modifier
                    // Always present so the chain stays structurally constant — a
                    // recomposition mid-drag (e.g. the preview following the dragged page)
                    // must not rebuild the pointerInput node and cancel the gesture. The
                    // dragged card follows the finger via translationX, so its own placement
                    // animation is disabled; neighbours still animate into the opening gap.
                    .animateItem(
                        placementSpec = if (isDragging) {
                            null
                        } else {
                            spring(stiffness = Spring.StiffnessMediumLow)
                        },
                    )
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer { translationX = if (isDragging) dragOffsetX else 0f }
                    .size(width = THUMB_WIDTH, height = THUMB_HEIGHT)
                    .pointerInput(page.id) {
                        val stride = (THUMB_WIDTH + THUMB_SPACING).toPx()
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingId = page.id
                                startIndex = currentPages.indexOfFirst { it.id == page.id }
                                dragOffsetX = 0f
                                moved = false
                                onDragStateChange(true)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetX += dragAmount.x
                                val current = currentPages.indexOfFirst { it.id == draggingId }
                                if (current >= 0) {
                                    val target = (current + (dragOffsetX / stride).roundToInt())
                                        .coerceIn(0, currentPages.lastIndex)
                                    if (target != current) {
                                        onLiveMove(current, target)
                                        // Keep the card under the finger after the slot shift.
                                        dragOffsetX -= (target - current) * stride
                                        moved = true
                                    }
                                }
                            },
                            onDragEnd = {
                                val finalIndex = currentPages.indexOfFirst { it.id == draggingId }
                                if (!moved && abs(dragOffsetX) < stride / 2f) {
                                    onLongPress(page.id)
                                } else if (finalIndex >= 0 && finalIndex != startIndex) {
                                    onCommit(startIndex, finalIndex)
                                }
                                draggingId = null
                                dragOffsetX = 0f
                                startIndex = -1
                                moved = false
                                onDragStateChange(false)
                            },
                            onDragCancel = {
                                draggingId = null
                                dragOffsetX = 0f
                                startIndex = -1
                                moved = false
                                onDragStateChange(false)
                            },
                        )
                    }
                    .clickable { onSelect(index) },
            )
        }
    }
}

@Composable
private fun SheetAction(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MegaIcon(
            imageVector = icon,
            contentDescription = null,
            tint = IconColor.Primary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        MegaText(
            text = text,
            textColor = TextColor.Primary,
            style = AppTheme.typography.bodyLarge,
        )
    }
}

private val THUMB_WIDTH = 56.dp
private val THUMB_HEIGHT = 72.dp
private val THUMB_SPACING = 8.dp
