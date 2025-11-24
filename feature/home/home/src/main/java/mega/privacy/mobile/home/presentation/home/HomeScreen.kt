package mega.privacy.mobile.home.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.MegaOutlinedButton
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
import mega.privacy.android.core.nodecomponents.components.AddContentFab
import mega.privacy.android.core.nodecomponents.sheet.home.HomeFabOption
import mega.privacy.android.core.nodecomponents.sheet.home.HomeFabOptionsBottomSheetNavKey
import mega.privacy.android.core.sharedcomponents.extension.excludingBottomPadding
import mega.privacy.android.core.sharedcomponents.menu.CommonAppBarAction
import mega.privacy.android.core.transfers.widget.TransfersToolbarWidget
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.SearchNodeNavKey
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.home.presentation.configuration.HomeConfiguration
import mega.privacy.mobile.home.presentation.home.model.HomeUiState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    val fabOption by
    navigationHandler.monitorResult<HomeFabOption>(HomeFabOptionsBottomSheetNavKey.KEY)
        .collectAsStateWithLifecycle(null)

    LaunchedEffect(fabOption) {
        if (fabOption != null) {
            // Handle action and call
            navigationHandler.clearResult(HomeFabOptionsBottomSheetNavKey.KEY)
        }
    }

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            MegaTopAppBar(
                title = stringResource(sharedR.string.general_section_home),
                navigationType = AppBarNavigationType.None,
                trailingIcons = { TransfersToolbarWidget(navigationHandler) },
                actions = listOf(
                    MenuActionWithClick(CommonAppBarAction.Search) {
                        navigationHandler.navigate(
                            SearchNodeNavKey(
                                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                                parentHandle = -1L
                            )
                        )
                    }
                )
            )
        },
        floatingActionButton = {
            AddContentFab(
                visible = true,
                onClick = {
                    navigationHandler.navigate(HomeFabOptionsBottomSheetNavKey)
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
    ) { paddingValues ->
        when (state) {
            is HomeUiState.Data -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues.excludingBottomPadding()),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.widgets, key = { it.identifier }) { it ->
                        it.content(Modifier, navigationHandler::navigate, transferHandler)
                    }

                    item {
                        MegaOutlinedButton(
                            text = "Configure Widgets",
                            onClick = {
                                navigationHandler.navigate(HomeConfiguration)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    MegaText(text = "Home Screen Loading")
                }
            }
        }
    }
}