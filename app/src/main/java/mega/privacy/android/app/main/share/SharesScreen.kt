package mega.privacy.android.app.main.share

import mega.privacy.android.icon.pack.R as IconR
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.AndroidFragment
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.shares.incoming.IncomingSharesComposeFragment
import mega.privacy.android.app.presentation.shares.incoming.model.IncomingSharesState
import mega.privacy.android.app.presentation.shares.links.LinksComposeFragment
import mega.privacy.android.app.presentation.shares.links.model.LinksUiState
import mega.privacy.android.app.presentation.shares.outgoing.OutgoingSharesComposeFragment
import mega.privacy.android.app.presentation.shares.outgoing.model.OutgoingSharesState
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_alpha_054_white_alpha_054

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SharesScreen(
    uiState: SharesUiState = SharesUiState(),
    incomingUiState: IncomingSharesState = IncomingSharesState(),
    outgoingUiState: OutgoingSharesState = OutgoingSharesState(),
    linksUiState: LinksUiState = LinksUiState(),
    onSearchClick: () -> Unit = {},
    onMoreClick: () -> Unit = {},
    onPageSelected: (SharesTab) -> Unit = {},
) {
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val view = LocalView.current
    val pagerState = rememberPagerState(initialPage = uiState.currentTab.position) { 3 }
    val isTabShown = when (uiState.currentTab) {
        SharesTab.INCOMING_TAB -> incomingUiState.isInRootLevel
        SharesTab.OUTGOING_TAB -> outgoingUiState.isInRootLevel
        SharesTab.LINKS_TAB -> linksUiState.isInRootLevel
        else -> true
    }
    val titleName = when (uiState.currentTab) {
        SharesTab.INCOMING_TAB -> incomingUiState.currentNodeName
        SharesTab.OUTGOING_TAB -> outgoingUiState.currentNodeName
        SharesTab.LINKS_TAB -> linksUiState.parentNode?.name
        else -> stringResource(R.string.title_shared_items)
    } ?: stringResource(R.string.title_shared_items)
    val actions = when (uiState.currentTab) {
        SharesTab.INCOMING_TAB -> {
            if (incomingUiState.isInRootLevel) {
                listOf(SharesActionMenu.Search)
            } else {
                listOf(SharesActionMenu.More, SharesActionMenu.Search)
            }
        }

        SharesTab.OUTGOING_TAB -> {
            if (outgoingUiState.isInRootLevel) {
                listOf(SharesActionMenu.Search)
            } else {
                listOf(SharesActionMenu.More, SharesActionMenu.Search)
            }
        }

        SharesTab.LINKS_TAB -> {
            if (linksUiState.isInRootLevel) {
                listOf(SharesActionMenu.Search)
            } else {
                listOf(SharesActionMenu.More, SharesActionMenu.Search)
            }
        }

        else -> emptyList()
    }

    LaunchedEffect(uiState.currentTab) {
        if (uiState.currentTab != SharesTab.NONE) {
            pagerState.animateScrollToPage(uiState.currentTab.position)
        }
    }
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (pagerState.currentPage != uiState.currentTab.position && !pagerState.isScrollInProgress) {
            onPageSelected(SharesTab.fromPosition(pagerState.currentPage))
        }
    }

    MegaScaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            MegaAppBar(
                appBarType = AppBarType.BACK_NAVIGATION,
                title = titleName,
                modifier = Modifier,
                actions = actions,
                onActionPressed = {
                    when (it) {
                        SharesActionMenu.More -> onMoreClick()
                        SharesActionMenu.Search -> onSearchClick()
                    }
                },
                onNavigationPressed = {
                    onBackPressedDispatcher?.onBackPressed()
                }
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isTabShown) {
                TabRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TAB_ROW_TEST_TAG),
                    selectedTabIndex = pagerState.currentPage,
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = colorResource(R.color.color_border_interactive),
                    indicator = { tabPositions ->
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[pagerState.currentPage])
                                .height(2.dp)
                                .background(color = colorResource(R.color.color_border_interactive))
                        )
                    }
                ) {
                    SharesTab.entries.filter { it != SharesTab.NONE }
                        .forEachIndexed { index, item ->
                            Tab(
                                text = {
                                    Text(
                                        text = stringResource(item.stringRes),
                                        style = MaterialTheme.typography.subtitle1,
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(id = item.getIcon(pagerState.currentPage == index)),
                                        contentDescription = "Tab Icon"
                                    )
                                },
                                selected = pagerState.currentPage == index,
                                unselectedContentColor = MaterialTheme.colors.grey_alpha_054_white_alpha_054,
                                onClick = {
                                    onPageSelected(item)
                                }
                            )
                        }
                }
            }

            MegaDivider(dividerType = DividerType.FullSize)

            // AndroidFragment can't be previewed
            if (!view.isInEditMode) {
                HorizontalPager(
                    modifier = Modifier.fillMaxWidth(),
                    state = pagerState,
                    beyondViewportPageCount = 3,
                    verticalAlignment = Alignment.Top,
                    userScrollEnabled = isTabShown,
                ) { page: Int ->
                    Column(
                        Modifier.fillMaxSize(),
                    ) {
                        when (page) {
                            SharesTab.INCOMING_TAB.position -> AndroidFragment(
                                IncomingSharesComposeFragment::class.java
                            )

                            SharesTab.OUTGOING_TAB.position -> AndroidFragment(
                                OutgoingSharesComposeFragment::class.java
                            )

                            SharesTab.LINKS_TAB.position -> AndroidFragment(
                                LinksComposeFragment::class.java
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.tabIndicatorOffset(
    currentTabPosition: TabPosition,
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "tabIndicatorOffset"
        value = currentTabPosition
    }
) {
    val currentTabWidth by animateDpAsState(
        targetValue = currentTabPosition.width,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = ""
    )
    val indicatorOffset by animateDpAsState(
        targetValue = currentTabPosition.left,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = ""
    )
    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
        .width(currentTabWidth)
}

private fun SharesTab.getIcon(isSelected: Boolean): Int {
    if (isSelected) {
        return when (this) {
            SharesTab.INCOMING_TAB -> IconR.drawable.ic_folder_incoming_medium_regular_solid
            SharesTab.OUTGOING_TAB -> IconR.drawable.ic_folder_outgoing_medium_regular_solid
            SharesTab.LINKS_TAB -> IconR.drawable.ic_link01_medium_regular_solid
            else -> throw IllegalArgumentException("Invalid SharesTab")
        }
    } else {
        return when (this) {
            SharesTab.INCOMING_TAB -> IconR.drawable.ic_folder_incoming_medium_regular_outline
            SharesTab.OUTGOING_TAB -> IconR.drawable.ic_folder_outgoing_medium_regular_outline
            SharesTab.LINKS_TAB -> IconR.drawable.ic_link01_medium_regular_outline
            else -> throw IllegalArgumentException("Invalid SharesTab")
        }
    }
}

private val SharesTab.stringRes: Int
    get() = when (this) {
        SharesTab.INCOMING_TAB -> R.string.tab_incoming_shares
        SharesTab.OUTGOING_TAB -> R.string.tab_outgoing_shares
        SharesTab.LINKS_TAB -> R.string.tab_links_shares
        else -> throw IllegalArgumentException("Invalid SharesTab")
    }

@Preview(showBackground = true)
@Composable
private fun SharesScreenPreview() {
    OriginalTempTheme(isSystemInDarkTheme()) {
        SharesScreen()
    }
}

/**
 * Tab row test tag
 */
const val TAB_ROW_TEST_TAG = "shares_screen:tab_row"