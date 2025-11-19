package mega.privacy.mobile.home.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.MegaOutlinedButton
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.menu.MenuActionWithClick
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
        }
    ) { paddingValues ->
        when (state) {
            is HomeUiState.Data -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 50.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.widgets, key = { it.identifier }) { it ->
                        it.content(Modifier, navigationHandler::navigate, transferHandler)
                        Spacer(modifier = Modifier.height(8.dp))
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